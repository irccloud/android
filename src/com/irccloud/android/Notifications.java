package com.irccloud.android;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import com.jakewharton.notificationcompat2.NotificationCompat2;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;

public class Notifications extends SQLiteOpenHelper {
	public class Notification {
		int cid;
		int bid;
		long eid;
		String nick;
		String message;
		String network;
		String chan;
		String buffer_type;
		String message_type;
		
		public String toString() {
			return "{cid: " + cid + ", bid: " + bid + ", eid: " + eid + ", nick: " + nick + ", message: " + message + ", network: " + network + "}";
		}
	}
	
	public static final String TABLE_NOTIFICATIONS = "notifications";
	public static final String TABLE_NETWORKS = "networks";

	private static final String DATABASE_NAME = "notifications.db";
	private static final int DATABASE_VERSION = 2;
	private static final int NOTIFY_ID = 1;

	private static Notifications instance = null;
	private SQLiteDatabase batchDb;
	
	private Semaphore readSemaphore = new Semaphore(1);
	private Semaphore writeSemaphore = new Semaphore(1);
	
	public int excludeBid = -1;
	
	public static Notifications getInstance() {
		if(instance == null)
			instance = new Notifications();
		return instance;
	}
	
	public Notifications() {
		super(IRCCloudApplication.getInstance().getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
	}

	public void clear() {
		try {
			readSemaphore.acquire();
			SQLiteDatabase db = getSafeWritableDatabase();
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATIONS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NETWORKS);
			onCreate(db);
			db.close();
			writeSemaphore.release();
			readSemaphore.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public SQLiteDatabase getSafeWritableDatabase() {
		try {
			writeSemaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
		if(batchDb != null)
			return batchDb;
		else {
			SQLiteDatabase d = getWritableDatabase();
			d.execSQL("PRAGMA synchronous = OFF");
			return d;
		}
	}

	public SQLiteDatabase getSafeReadableDatabase() {
		try {
			readSemaphore.acquire();
			writeSemaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
		SQLiteDatabase d = getReadableDatabase();
		d.execSQL("PRAGMA synchronous = OFF");
		return d;
	}
	
	public void releaseWriteableDatabase() {
		writeSemaphore.release();
	}
	
	public void releaseReadableDatabase() {
		writeSemaphore.release();
		readSemaphore.release();
	}
	
	public void beginBatch() {
		try {
			readSemaphore.acquire();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		Log.d("IRCCloud", "+++ Starting batch transactions");
		batchDb = getWritableDatabase();
		batchDb.execSQL("PRAGMA synchronous = OFF");
		batchDb.beginTransaction();
	}
	
	public void endBatch() {
		Log.d("IRCCloud", "--- Batch transactions finished");
		batchDb.setTransactionSuccessful();
		batchDb.endTransaction();
		batchDb.close();
		batchDb = null;
		readSemaphore.release();
	}
	
	public boolean isBatch() {
		return(batchDb != null);
	}
	
	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL("create table " + TABLE_NOTIFICATIONS + " ("
				+ "eid integer not null, "
				+ "cid integer not null, "
				+ "bid integer not null, "
				+ "nick text not null, "
				+ "message text not null, "
				+ "chan text, "
				+ "buffer_type text not null, "
				+ "message_type text not null, "
				+ "PRIMARY KEY(eid,bid));");
		database.execSQL("create table " + TABLE_NETWORKS + " ("
				+ "cid integer not null, "
				+ "network text, "
				+ "PRIMARY KEY(cid));");
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db,oldVersion,newVersion);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATIONS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NETWORKS);
		onCreate(db);
	}
	
	public void addNetwork(int cid, String network) {
		SQLiteDatabase db = getSafeWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("cid", cid);
		values.put("network", network);
		db.insert(TABLE_NETWORKS, null, values);
		if(!isBatch())
			db.close();
		releaseWriteableDatabase();
	}

	public void deleteNetwork(int cid) {
		SQLiteDatabase db = getSafeWritableDatabase();
		db.delete(TABLE_NETWORKS, "cid = ?", new String[] {String.valueOf(cid)});
		if(!isBatch())
			db.close();
		releaseWriteableDatabase();
	}
	
	public void addNotification(int cid, int bid, long eid, String from, String message, String chan, String buffer_type, String message_type) {
		SQLiteDatabase db = getSafeWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("bid", bid);
		values.put("cid", cid);
		values.put("eid", eid);
		values.put("nick", from);
		values.put("message", message);
		values.put("chan", chan);
		values.put("buffer_type", buffer_type);
		values.put("message_type", message_type);
		db.insert(TABLE_NOTIFICATIONS, null, values);
		if(!isBatch())
			db.close();
		releaseWriteableDatabase();
	}

	public void deleteNotification(int cid, int bid, long eid) {
		SQLiteDatabase db = getSafeWritableDatabase();
		db.delete(TABLE_NOTIFICATIONS, "cid = ? and bid = ? and eid = ?", new String[] {String.valueOf(cid), String.valueOf(bid), String.valueOf(eid)});
		if(!isBatch())
			db.close();
		releaseWriteableDatabase();
	}
	
	public void deleteOldNotifications(int bid, long last_seen_eid) {
		SQLiteDatabase db = getSafeWritableDatabase();
		db.delete(TABLE_NOTIFICATIONS, "bid = ? and eid <= ?", new String[] {String.valueOf(bid), String.valueOf(last_seen_eid)});
		if(!isBatch())
			db.close();
		releaseWriteableDatabase();
	}
	
	public ArrayList<Notification> getNotifications() {
		ArrayList<Notification> notifications = new ArrayList<Notification>();

		SQLiteDatabase db = getSafeReadableDatabase();
		Cursor cursor = db.query(TABLE_NOTIFICATIONS + "," + TABLE_NETWORKS, new String[] {"bid", TABLE_NETWORKS + ".cid AS cid", "eid", "network", "nick", "message", "chan", "buffer_type", "message_type"}, TABLE_NETWORKS + ".cid = " + TABLE_NOTIFICATIONS + ".cid and bid != " + excludeBid, null, null, null, "cid,chan,eid");

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Notification n = cursorToNotification(cursor);
			notifications.add(n);
			cursor.moveToNext();
		}
		cursor.close();
		db.close();
		releaseReadableDatabase();
		return notifications;
	}
	
	public void showNotifications(String ticker) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
        NotificationManager nm = (NotificationManager)IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		ArrayList<Notification> notifications = getNotifications();
		Intent i = new Intent(IRCCloudApplication.getInstance().getApplicationContext(), MainActivity.class);
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		
		if(notifications.size() > 0 && prefs.getBoolean("notify", true)) {
			i.putExtra("bid", notifications.get(0).bid);
			int lastcid = -1;
			int lastbid = -1;
			int count = 0;
			String lastChan = "";
			String from = "";
			for(int j = 0; j < notifications.size(); j++) {
				String chan = notifications.get(j).chan;
				if(!lastChan.equals(chan)) {
					if(from.length() > 0) {
						from += " (" + (count+1) + "), ";
					}
					from += chan;
					lastChan = chan;
					count = 0;
				} else {
					count++;
				}
			}
			from += " (" + (count+1) + ")";
			count = 0;
			NotificationCompat2.Builder builder = new NotificationCompat2.Builder(IRCCloudApplication.getInstance().getApplicationContext())
	         .setContentTitle(notifications.size() + " unread highlight(s)")
	         .setTicker(ticker)
	         .setContentText(from)
	         .setNumber(notifications.size())
	         .setLights(0xFF0000FF, 500, 1000)
	         .setContentIntent(PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT))
	         .setSmallIcon(R.drawable.ic_launcher);
			if(ticker != null) {
				if(prefs.getBoolean("notify_vibrate", true))
					builder.setDefaults(android.app.Notification.DEFAULT_VIBRATE);
				if(prefs.getBoolean("notify_sound", true))
					builder.setSound(Uri.parse("android.resource://com.irccloud.android/"+R.raw.digit));
			}
	        NotificationCompat2.InboxStyle inbox = new NotificationCompat2.InboxStyle(builder);
	        for(Notification n : notifications) {
	    		if(++count > 4)
	    			break;
	        	if(n.cid != lastcid) {
	        		inbox.addLine(Html.fromHtml("<b>" + n.network.toUpperCase() + "</b>"));
	        		lastcid = n.cid;
	        	}
	        	if(n.bid != lastbid) {
	        		inbox.addLine(Html.fromHtml("&nbsp;- <b>" + n.chan + "</b>"));
	        		lastbid = n.bid;
	        	}
	        	if(n.buffer_type.equals("conversation"))
	        		inbox.addLine(Html.fromHtml("&nbsp;&nbsp;&nbsp;&nbsp;¥ " + n.message));
	        	else
		    		inbox.addLine(Html.fromHtml("&nbsp;&nbsp;&nbsp;&nbsp;¥ <b>&lt;" + n.nick + "&gt;</b> " + n.message));
	        }
	        if(count > 4)
	        	inbox.setSummaryText("+" + (notifications.size() - count + 1) + " more");
	        nm.notify(NOTIFY_ID, inbox.build());
		} else {
			nm.cancel(NOTIFY_ID);
		}
	}
	
	private Notification cursorToNotification(Cursor cursor) {
		Notification notification = new Notification();
		notification.bid = cursor.getInt(cursor.getColumnIndex("bid"));
		notification.cid = cursor.getInt(cursor.getColumnIndex("cid"));
		notification.eid = cursor.getLong(cursor.getColumnIndex("eid"));
		notification.nick = cursor.getString(cursor.getColumnIndex("nick"));
		notification.message = cursor.getString(cursor.getColumnIndex("message"));
		notification.network = cursor.getString(cursor.getColumnIndex("network"));
		notification.chan = cursor.getString(cursor.getColumnIndex("chan"));
		notification.buffer_type = cursor.getString(cursor.getColumnIndex("buffer_type"));
		notification.message_type = cursor.getString(cursor.getColumnIndex("message_type"));
		return notification;
	}
}