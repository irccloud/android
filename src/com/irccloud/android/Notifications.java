package com.irccloud.android;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import com.jakewharton.notificationcompat2.NotificationCompat2;

import android.annotation.SuppressLint;
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
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

public class Notifications extends SQLiteOpenHelper {
	public class Notification {
		public int cid;
		public int bid;
		public long eid;
		public String nick;
		public String message;
		public String network;
		public String chan;
		public String buffer_type;
		public String message_type;
		
		public String toString() {
			return "{cid: " + cid + ", bid: " + bid + ", eid: " + eid + ", nick: " + nick + ", message: " + message + ", network: " + network + "}";
		}
	}
	
	public class Network {
		int cid;
		String name;
	}
	
	public static final String TABLE_NOTIFICATIONS = "notifications";
	public static final String TABLE_NETWORKS = "networks";
	public static final String TABLE_LAST_SEEN_EIDS = "last_seen_eids";
	public static final String TABLE_DISMISSED_EIDS = "dismissed_eids";

	private static final String DATABASE_NAME = "notifications.db";
	private static final int DATABASE_VERSION = 1;

	private static Notifications instance = null;
	private SQLiteDatabase batchDb;
	
	private Semaphore readSemaphore = new Semaphore(1);
	private Semaphore writeSemaphore = new Semaphore(1);
	
	private int excludeBid = -1;
	
	public static Notifications getInstance() {
		if(instance == null)
			instance = new Notifications();
		return instance;
	}
	
	public Notifications() {
		super(IRCCloudApplication.getInstance().getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
	}

	public void clearDismissed() {
		SQLiteDatabase db = getSafeWritableDatabase();
		db.delete(TABLE_DISMISSED_EIDS, null, null);
		if(!isBatch())
			db.close();
		releaseWriteableDatabase();
	}
	
	public void clear() {
		try {
	        NotificationManager nm = (NotificationManager)IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
			ArrayList<Notification> notifications = getOtherNotifications();
			
			if(notifications.size() > 0) {
		        for(Notification n : notifications) {
	        		nm.cancel((int)(n.eid/1000));
	        		nm.cancel(n.bid);
		        }
			}
			readSemaphore.acquire();
			SQLiteDatabase db = getSafeWritableDatabase();
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATIONS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_NETWORKS);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE_LAST_SEEN_EIDS);
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
		database.execSQL("create table " + TABLE_LAST_SEEN_EIDS + " ("
				+ "bid integer not null, "
				+ "eid integer, "
				+ "PRIMARY KEY(bid));");
		database.execSQL("create table if not exists " + TABLE_DISMISSED_EIDS + " ("
				+ "bid integer not null, "
				+ "eid integer, "
				+ "PRIMARY KEY(bid,eid));");
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db,oldVersion,newVersion);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATIONS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_NETWORKS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_LAST_SEEN_EIDS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_DISMISSED_EIDS);
		onCreate(db);
	}
	
	public long getLastSeenEid(int bid) {
		long eid = -1;

		SQLiteDatabase db;
		if(isBatch())
			db = batchDb;
		else
			db = getSafeReadableDatabase();
		Cursor cursor = db.query(TABLE_LAST_SEEN_EIDS, new String[] {"eid"}, "bid = " + bid, null, null, null, null);

		if(cursor.moveToFirst()) {
			eid = cursor.getLong(cursor.getColumnIndex("eid"));
		}
		cursor.close();
		if(!isBatch()) {
			db.close();
			releaseReadableDatabase();
		}
		
		return eid;
	}
	
	public synchronized void updateLastSeenEid(int bid, long eid) {
		long last_eid = getLastSeenEid(bid);
		SQLiteDatabase db = getSafeWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("bid", bid);
		values.put("eid", eid);
		if(last_eid > 0)
			db.update(TABLE_LAST_SEEN_EIDS, values, "bid = " + bid, null);
		else
			db.insert(TABLE_LAST_SEEN_EIDS, null, values);
		if(!isBatch())
			db.close();
		releaseWriteableDatabase();
	}

	public boolean isDismissed(int bid, long eid) {
		boolean result = false;
		SQLiteDatabase db;
		if(isBatch())
			db = batchDb;
		else
			db = getSafeReadableDatabase();
		Cursor cursor = db.query(TABLE_DISMISSED_EIDS, new String[] {"eid"}, "bid = " + bid + " and eid = " + eid, null, null, null, null);

		if(cursor.moveToFirst()) {
			result = true;
		}
		cursor.close();
		if(!isBatch()) {
			db.close();
			releaseReadableDatabase();
		}
		
		return result;
	}
	
	public void dismiss(int bid, long eid) {
		SQLiteDatabase db = getSafeWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("bid", bid);
		values.put("eid", eid);
		db.insert(TABLE_DISMISSED_EIDS, null, values);
		db.delete(TABLE_NOTIFICATIONS, "bid = ? and eid = ?", new String[] {String.valueOf(bid), String.valueOf(eid)});
		if(!isBatch())
			db.close();
		releaseWriteableDatabase();
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
		if(isDismissed(bid, eid)) {
			Log.d("IRCCloud", "This notification's EID has been dismissed, skipping...");
			return;
		}
		long last_eid = getLastSeenEid(bid);
		if(eid <= last_eid) {
			Log.d("IRCCloud", "This notification's EID has already been seen, skipping...");
			return;
		}
		Log.d("IRCCloud", "Adding notification: "
				+ "cid: " + cid + " "
				+ "bid: " + bid + " "
				+ "eid: " + eid + " "
				+ "from: " + from + " "
				+ "message: " + message + " "
				+ "chan: " + chan + " "
				+ "buffer_type: " + buffer_type + " "
				+ "message_type: " + message_type + " "
				);
		Network network = getNetwork(cid);
		if(network != null)
			Log.d("IRCCloud", "Name for network: " + network.name);
		else
			Log.w("IRCCloud", "No network name!");
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
        NotificationManager nm = (NotificationManager)IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		ArrayList<Notification> notifications = getOtherNotifications();
		
		if(notifications.size() > 0) {
	        for(Notification n : notifications) {
	        	if(n.bid == bid && n.eid <= last_seen_eid)
	        		nm.cancel((int)(n.eid/1000));
	        }
		}
		nm.cancel(bid);
		SQLiteDatabase db = getSafeWritableDatabase();
		db.delete(TABLE_NOTIFICATIONS, "bid = ? and eid <= ?", new String[] {String.valueOf(bid), String.valueOf(last_seen_eid)});
		db.delete(TABLE_DISMISSED_EIDS, "bid = ? and eid <= ?", new String[] {String.valueOf(bid), String.valueOf(last_seen_eid)});
		if(!isBatch())
			db.close();
		releaseWriteableDatabase();
	}
	
	public void deleteNotificationsForBid(int bid) {
        NotificationManager nm = (NotificationManager)IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		ArrayList<Notification> notifications = getOtherNotifications();
		
		if(notifications.size() > 0) {
	        for(Notification n : notifications) {
	        	if(n.bid == bid)
	        		nm.cancel((int)(n.eid/1000));
	        }
		}
		SQLiteDatabase db = getSafeWritableDatabase();
		db.delete(TABLE_NOTIFICATIONS, "bid = ?", new String[] {String.valueOf(bid)});
		db.delete(TABLE_DISMISSED_EIDS, "bid = ?", new String[] {String.valueOf(bid)});
		if(!isBatch())
			db.close();
		releaseWriteableDatabase();
	}
	
	public ArrayList<Notification> getMessageNotifications() {
		Log.d("IRCCloud", "+++ Begin message notifications");
		ArrayList<Notification> notifications = new ArrayList<Notification>();

		SQLiteDatabase db = getSafeReadableDatabase();
		Cursor cursor = db.query(TABLE_NOTIFICATIONS + " INNER JOIN " + TABLE_NETWORKS + " ON " + TABLE_NOTIFICATIONS + ".cid=" + TABLE_NETWORKS +".cid",
				new String[] {"bid", TABLE_NETWORKS + ".cid AS cid", "eid", "network", "nick", "message", "chan", "buffer_type", "message_type"}, "bid != " + excludeBid + " and (message_type='buffer_msg' or message_type='buffer_me_msg' or message_type='notice' or message_type='wallops')", null, null, null, "cid,chan,eid");

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Notification n = cursorToNotification(cursor);
			Log.d("IRCCloud", "Notification: " + n.toString());
			notifications.add(n);
			cursor.moveToNext();
		}
		cursor.close();
		db.close();
		releaseReadableDatabase();
		Log.d("IRCCloud", "--- End message notifications");
		return notifications;
	}
	
	public ArrayList<Notification> getOtherNotifications() {
		ArrayList<Notification> notifications = new ArrayList<Notification>();

		SQLiteDatabase db = getSafeReadableDatabase();
		Cursor cursor = db.query(TABLE_NOTIFICATIONS + "," + TABLE_NETWORKS, new String[] {"bid", TABLE_NETWORKS + ".cid AS cid", "eid", "network", "nick", "message", "chan", "buffer_type", "message_type"}, TABLE_NETWORKS + ".cid = " + TABLE_NOTIFICATIONS + ".cid and bid != " + excludeBid + " and (message_type='channel_invite' or message_type='callerid')", null, null, null, "cid,chan,eid");

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
	
	public Network getNetwork(int cid) {
		Network n = null;
		SQLiteDatabase db;
		if(isBatch())
			db = batchDb;
		else
			db = getSafeReadableDatabase();
		Cursor cursor = db.query(TABLE_NETWORKS, new String[] {"cid", "network"}, "cid = " + cid, null, null, null, null);

		cursor.moveToFirst();
		
		if(!cursor.isAfterLast()) {
			n = cursorToNetwork(cursor);
		}
		cursor.close();
		if(!isBatch()) {
			db.close();
			releaseReadableDatabase();
		}
		return n;
	}
	
	public Notification getNotification(long eid) {
		Notification n = null;
		SQLiteDatabase db;
		if(isBatch())
			db = batchDb;
		else
			db = getSafeReadableDatabase();
		Cursor cursor = db.query(TABLE_NOTIFICATIONS + "," + TABLE_NETWORKS, new String[] {"bid", TABLE_NETWORKS + ".cid AS cid", "eid", "network", "nick", "message", "chan", "buffer_type", "message_type"}, TABLE_NETWORKS + ".cid = " + TABLE_NOTIFICATIONS + ".cid and eid = " + eid, null, null, null, null);

		if(cursor.moveToFirst()) {
			n = cursorToNotification(cursor);
		}
		cursor.close();
		if(!isBatch()) {
			db.close();
			releaseReadableDatabase();
		}
		return n;
	}
	
	public void excludeBid(int bid) {
		excludeBid = -1;
        NotificationManager nm = (NotificationManager)IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		ArrayList<Notification> notifications = getOtherNotifications();
		
		if(notifications.size() > 0) {
	        for(Notification n : notifications) {
	        	if(n.bid == bid)
	        		nm.cancel((int)(n.eid/1000));
	        }
		}
		nm.cancel(bid);
		excludeBid = bid;
	}
	
	public void showNotifications(String ticker) {
		if(ServersDataSource.getInstance().count() > 0) {
			ArrayList<Notification> notifications = getMessageNotifications();
			for(Notification n : notifications) {
				if(ServersDataSource.getInstance().getServer(n.cid) == null
						|| BuffersDataSource.getInstance().getBuffer(n.bid) == null
						|| EventsDataSource.getInstance().getEvent(n.eid, n.bid) == null) {
					Log.d("IRCCloud", "Removing stale notification: " + n.toString());
					deleteNotification(n.cid, n.bid, n.eid);
				}
				if(isDismissed(n.bid, n.eid)) {
					Log.d("IRCCloud", "Removing dismissed notification: " + n.toString());
					deleteNotification(n.cid, n.bid, n.eid);
				}
			}
		}
		
		showMessageNotifications(ticker);
		showOtherNotifications();
	}
	
	private void showOtherNotifications() {
		String title = "";
		String text = "";
		String ticker = null;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
        NotificationManager nm = (NotificationManager)IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		ArrayList<Notification> notifications = getOtherNotifications();
		
		if(notifications.size() > 0 && prefs.getBoolean("notify", true)) {
	        for(Notification n : notifications) {
				if(n.message_type.equals("callerid")) {
					title = "Callerid: " + n.nick + " (" + n.network + ")";
					text = n.nick + " " + n.message;
					ticker = n.nick + " " + n.message;
				} else {
					title = n.nick + " (" + n.network + ")";
					text = n.message;
					ticker = n.message;
				}
		        nm.notify((int)(n.eid/1000), buildNotification(ticker, n.bid, new long[] {n.eid}, title, text, Html.fromHtml(text), 1));
	        }
		}
	}
	
	@SuppressLint("NewApi")
	private android.app.Notification buildNotification(String ticker, int bid, long[] eids, String title, String text, Spanned big_text, int count) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());

		NotificationCompat2.Builder builder = new NotificationCompat2.Builder(IRCCloudApplication.getInstance().getApplicationContext())
        .setTicker(ticker)
		.setContentTitle(title)
		.setContentText(text)
        .setLights(0xFF0000FF, 500, 1000)
        .setSmallIcon(R.drawable.ic_launcher);

		if(ticker != null) {
			if(prefs.getBoolean("notify_vibrate", true))
				builder.setDefaults(android.app.Notification.DEFAULT_VIBRATE);
			if(prefs.getBoolean("notify_sound", true))
				builder.setSound(Uri.parse("android.resource://com.irccloud.android/"+R.raw.digit));
		}

		if(count == 1)
			builder.setOnlyAlertOnce(true);
		
		Intent i = new Intent(IRCCloudApplication.getInstance().getApplicationContext(), MessageActivity.class);
		i.putExtra("bid", bid);
		i.setData(Uri.parse("bid://" + bid));
    	Intent dismiss = new Intent("com.irccloud.android.DISMISS_NOTIFICATION");
		dismiss.setData(Uri.parse("irccloud-dismiss://" + bid));
    	dismiss.putExtra("bid", bid);
		dismiss.putExtra("eids", eids);
        builder.setContentIntent(PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT));
		builder.setDeleteIntent(PendingIntent.getBroadcast(IRCCloudApplication.getInstance().getApplicationContext(), 0, dismiss, PendingIntent.FLAG_UPDATE_CURRENT));

		android.app.Notification notification = builder.build();
		if(Build.VERSION.SDK_INT >= 16 && big_text != null) {
			RemoteViews contentView = new RemoteViews("com.irccloud.android", R.layout.notification);
			contentView.setTextViewText(R.id.title, title);
			contentView.setTextViewText(R.id.text, big_text);
			if(count > 4) {
				contentView.setViewVisibility(R.id.divider, View.VISIBLE);
				contentView.setViewVisibility(R.id.more, View.VISIBLE);
				contentView.setTextViewText(R.id.more, "+" + (count - 4) + " more");
			} else {
				contentView.setViewVisibility(R.id.divider, View.GONE);
				contentView.setViewVisibility(R.id.more, View.GONE);
			}
			notification.bigContentView = contentView;
		}
		
		return notification;
	}
	
	private void showMessageNotifications(String ticker) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
		String title = "";
		String text = "";
        NotificationManager nm = (NotificationManager)IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
		ArrayList<Notification> notifications = getMessageNotifications();

		if(notifications.size() > 0 && prefs.getBoolean("notify", true)) {
			if(notifications.size() == 1) {
				Notification n = notifications.get(0);
				title = n.nick;
				if(!n.buffer_type.equals("conversation") && !n.message_type.equals("wallops") && n.chan.length() > 0)
					title += " in " + n.chan;
				title += " (" + n.network + ")";
				if(n.message_type.equals("buffer_me_msg"))
					text = "Ñ " + n.message;
				else
					text = n.message;
		        nm.notify(n.bid, buildNotification(ticker, n.bid, new long[] {n.eid}, title, text, Html.fromHtml(text), 1));
			} else {
				int lastbid = notifications.get(0).bid;
				int count = 0;
				long[] eids = new long[notifications.size()];
				Notification last = null;
				count = 0;
        		title = notifications.get(0).chan + " (" + notifications.get(0).network + ")";
		        for(Notification n : notifications) {
		        	if(n.bid != lastbid) {
						if(count == 1) {
							title = last.nick;
							if(!last.buffer_type.equals("conversation") && !last.message_type.equals("wallops") && last.chan.length() > 0)
								title += " in " + last.chan;
							title += " (" + last.network + ")";
					        nm.notify(lastbid, buildNotification(ticker, lastbid, eids, title, Html.fromHtml(text).toString(), Html.fromHtml(text), count));
						} else {
					        nm.notify(lastbid, buildNotification(ticker, lastbid, eids, title, count + " unread highlight(s)", Html.fromHtml(text), count));
						}
		        		lastbid = n.bid;
		        		title = n.chan + " (" + n.network + ")";
		        		text = "";
						count = 0;
						eids = new long[notifications.size()];
		        	}
		        	if(count < 4) {
		        		if(text.length() > 0)
		        			text += "<br/>";
			        	if(n.buffer_type.equals("conversation") && n.message_type.equals("buffer_me_msg"))
			        		text += "Ñ " + n.message;
			        	else if(n.buffer_type.equals("conversation"))
			        		text += n.message;
			        	else if(n.message_type.equals("buffer_me_msg"))
				    		text += "<b>Ñ " + n.nick + "</b> " + n.message;
			        	else
				    		text += "<b>" + n.nick + "</b> " + n.message;
		        	}
		        	eids[count++] = n.eid;
		        	last = n;
		        }
				if(count == 1) {
					title = last.nick;
					if(!last.buffer_type.equals("conversation") && !last.message_type.equals("wallops") && last.chan.length() > 0)
						title += " in " + last.chan;
					title += " (" + last.network + ")";
			        nm.notify(lastbid, buildNotification(ticker, lastbid, eids, title, Html.fromHtml(text).toString(), Html.fromHtml(text), count));
				} else {
			        nm.notify(lastbid, buildNotification(ticker, lastbid, eids, title, count + " unread highlight(s)", Html.fromHtml(text), count));
				}
			}
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

	private Network cursorToNetwork(Cursor cursor) {
		Network network = new Network();
		network.name = cursor.getString(cursor.getColumnIndex("network"));
		network.cid = cursor.getInt(cursor.getColumnIndex("cid"));
		return network;
	}
}