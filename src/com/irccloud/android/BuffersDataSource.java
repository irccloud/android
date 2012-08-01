package com.irccloud.android;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class BuffersDataSource {
	public class Buffer {
		int bid;
		int cid;
		long max_eid;
		long last_seen_eid;
		String name;
		String type;
		int hidden;
		int joined;
	}

	private DBHelper dbHelper;
	private static BuffersDataSource instance = null;
	
	public static BuffersDataSource getInstance() {
		if(instance == null)
			instance = new BuffersDataSource();
		return instance;
	}

	public BuffersDataSource() {
		dbHelper = DBHelper.getInstance();
	}

	public Buffer createBuffer(int bid, int cid, long max_eid, long last_seen_eid, String name, String type, int hidden, int joined) {
		synchronized(dbHelper) {
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put("bid", bid);
			values.put("cid", cid);
			values.put("max_eid", max_eid);
			values.put("last_seen_eid", last_seen_eid);
			values.put("name", name);
			values.put("type", type);
			values.put("hidden", hidden);
			values.put("joined", joined);
			db.insert(DBHelper.TABLE_BUFFERS, null, values);
			Cursor cursor = db.query(DBHelper.TABLE_BUFFERS, new String[] {"bid", "cid", "max_eid", "last_seen_eid", "name", "type", "hidden", "joined"}, "bid = ?", new String[] {String.valueOf(bid)}, null, null, null);
			cursor.moveToFirst();
			Buffer newBuffer = cursorToBuffer(cursor);
			cursor.close();
			if(!DBHelper.getInstance().isBatch())
				db.close();
			return newBuffer;
		}
	}

	public void deleteBuffer(int bid) {
		synchronized(dbHelper) {
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			db.delete(DBHelper.TABLE_BUFFERS, "bid = ?", new String[] {String.valueOf(bid)});
			if(!DBHelper.getInstance().isBatch())
				db.close();
		}
	}

	public synchronized Buffer getBuffer(int bid) {
		synchronized(dbHelper) {
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			Cursor cursor = db.query(DBHelper.TABLE_BUFFERS, new String[] {"bid", "cid", "max_eid", "last_seen_eid", "name", "type", "hidden", "joined"}, "bid = ?", new String[] {String.valueOf(bid)}, null, null, null);
	
			cursor.moveToFirst();
			Buffer buffer = cursorToBuffer(cursor);
			cursor.close();
			db.close();
			return buffer;
		}
	}
	
	public synchronized ArrayList<Buffer> getBuffersForServer(int cid) {
		synchronized(dbHelper) {
			ArrayList<Buffer> buffers = new ArrayList<Buffer>();
	
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			Cursor cursor = db.query(DBHelper.TABLE_BUFFERS, new String[] {"bid", "cid", "max_eid", "last_seen_eid", "name", "type", "hidden", "joined"}, "cid = ?", new String[] {String.valueOf(cid)}, null, null, "type");
	
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				Buffer buffer = cursorToBuffer(cursor);
				buffers.add(buffer);
				cursor.moveToNext();
			}
			// Make sure to close the cursor
			cursor.close();
			db.close();
			return buffers;
		}
	}

	private Buffer cursorToBuffer(Cursor cursor) {
		Buffer buffer = new Buffer();
		buffer.bid = cursor.getInt(cursor.getColumnIndex("bid"));
		buffer.cid = cursor.getInt(cursor.getColumnIndex("cid"));
		buffer.max_eid = cursor.getLong(cursor.getColumnIndex("max_eid"));
		buffer.last_seen_eid = cursor.getLong(cursor.getColumnIndex("last_seen_eid"));
		buffer.name = cursor.getString(cursor.getColumnIndex("name"));
		buffer.type = cursor.getString(cursor.getColumnIndex("type"));
		buffer.hidden = cursor.getInt(cursor.getColumnIndex("hidden"));
		buffer.joined = cursor.getInt(cursor.getColumnIndex("joined"));
		return buffer;
	}
}
