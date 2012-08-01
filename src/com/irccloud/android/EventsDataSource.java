package com.irccloud.android;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class EventsDataSource {
	public class Event {
		long eid;
		long bid;
		int cid;
		String type;
		int highlight;
		JSONObject event;
	}

	private DBHelper dbHelper;
	private static EventsDataSource instance = null;
	
	public static EventsDataSource getInstance() {
		if(instance == null)
			instance = new EventsDataSource();
		return instance;
	}

	public EventsDataSource() {
		dbHelper = DBHelper.getInstance();
	}

	public Event createEvent(long eid, int bid, int cid, String type, int highlight, JSONObject event) {
		synchronized(dbHelper) {
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put("eid", eid);
			values.put("bid", bid);
			values.put("cid", cid);
			values.put("type", type);
			values.put("highlight", highlight);
			values.put("event", event.toString());
			db.insert(DBHelper.TABLE_EVENTS, null, values);
			Cursor cursor = db.query(DBHelper.TABLE_EVENTS, new String[] {"eid", "bid", "cid", "type", "highlight", "event"}, "eid = ? and bid = ?", new String[] {String.valueOf(eid), String.valueOf(bid)}, null, null, null);
			cursor.moveToFirst();
			Event newEvent = cursorToEvent(cursor);
			cursor.close();
			if(!DBHelper.getInstance().isBatch())
				db.close();
			return newEvent;
		}
	}

	public void deleteEvent(long eid, int bid) {
		synchronized(dbHelper) {
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			db.delete(DBHelper.TABLE_EVENTS, "eid = ? and bid = ?", new String[] {String.valueOf(eid), String.valueOf(bid)});
			if(!DBHelper.getInstance().isBatch())
				db.close();
		}
	}

	public synchronized ArrayList<Event> getEventsForBuffer(int bid) {
		synchronized(dbHelper) {
			ArrayList<Event> events = new ArrayList<Event>();
	
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			Cursor cursor = db.query(DBHelper.TABLE_EVENTS, new String[] {"eid", "bid", "cid", "type", "highlight", "event"}, "bid = ?", new String[] {String.valueOf(bid)}, null, null, "eid");
	
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				Event event = cursorToEvent(cursor);
				events.add(event);
				cursor.moveToNext();
			}
			// Make sure to close the cursor
			cursor.close();
			return events;
		}
	}

	public synchronized int getUnreadCountForBuffer(int bid, long last_seen_eid) {
		synchronized(dbHelper) {
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			Cursor cursor = db.query(DBHelper.TABLE_EVENTS, new String[] {"count() as count"}, "bid = ? and eid > ? and (type='buffer_msg' or type='buffer_me_msg' or type='notice' or type='channel_invite' or type='callerid')", new String[] {String.valueOf(bid), String.valueOf(last_seen_eid)}, null, null, null);
	
			cursor.moveToFirst();
			int count = cursor.getInt(0);
			cursor.close();
			return count;
		}
	}

	public synchronized int getHighlightCountForBuffer(int bid, long last_seen_eid) {
		synchronized(dbHelper) {
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			Cursor cursor = db.query(DBHelper.TABLE_EVENTS, new String[] {"count() as count"}, "bid = ? and eid > ? and highlight='1' and (type='buffer_msg' or type='buffer_me_msg' or type='notice' or type='channel_invite' or type='callerid')", new String[] {String.valueOf(bid), String.valueOf(last_seen_eid)}, null, null, null);
	
			cursor.moveToFirst();
			int count = cursor.getInt(0);
			cursor.close();
			return count;
		}
	}

	private Event cursorToEvent(Cursor cursor) {
		Event event = new Event();
		event.eid = cursor.getLong(cursor.getColumnIndex("eid"));
		event.bid = cursor.getInt(cursor.getColumnIndex("bid"));
		event.cid = cursor.getInt(cursor.getColumnIndex("cid"));
		event.type = cursor.getString(cursor.getColumnIndex("type"));
		event.highlight = cursor.getInt(cursor.getColumnIndex("highlight"));
		try {
			event.event = new JSONObject(cursor.getString(cursor.getColumnIndex("event")));
		} catch (JSONException e) {
			event.event = null;
		}
		return event;
	}
}
