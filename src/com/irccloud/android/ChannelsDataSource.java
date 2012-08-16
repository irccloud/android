package com.irccloud.android;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class ChannelsDataSource {
	public class Channel {
		int cid;
		long bid;
		String name;
		String topic_text;
		long topic_time;
		String topic_author;
		String type;
		String mode;
	}

	private DBHelper dbHelper;
	private static ChannelsDataSource instance = null;
	
	public static ChannelsDataSource getInstance() {
		if(instance == null)
			instance = new ChannelsDataSource();
		return instance;
	}

	public ChannelsDataSource() {
		dbHelper = DBHelper.getInstance();
	}

	public Channel createChannel(int cid, long bid, String name, String topic_text, long topic_time, String topic_author, String type, String mode) {
		SQLiteDatabase db = dbHelper.getSafeWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("cid", cid);
		values.put("bid", bid);
		values.put("name", name);
		values.put("topic_text", topic_text);
		values.put("topic_time", topic_time);
		values.put("topic_author",topic_author);
		values.put("type", type);
		values.put("mode", mode);
		db.insert(DBHelper.TABLE_CHANNELS, null, values);
		Cursor cursor = db.query(DBHelper.TABLE_CHANNELS, new String[] {"cid", "bid", "name", "topic_text", "topic_time", "topic_author", "type", "mode"}, "bid = ?", new String[] {String.valueOf(bid)}, null, null, null);
		cursor.moveToFirst();
		Channel newChannel = cursorToChannel(cursor);
		cursor.close();
		if(!DBHelper.getInstance().isBatch())
			db.close();
		dbHelper.releaseWriteableDatabase();
		return newChannel;
	}

	public void deleteChannel(long bid) {
		SQLiteDatabase db = dbHelper.getSafeWritableDatabase();
		db.delete(DBHelper.TABLE_CHANNELS, "bid = ?", new String[] {String.valueOf(bid)});
		if(!DBHelper.getInstance().isBatch())
			db.close();
		dbHelper.releaseWriteableDatabase();
	}

	public void updateTopic(long bid, String topic_text, long topic_time, String topic_author) {
		SQLiteDatabase db = dbHelper.getSafeWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("topic_text", topic_text);
		values.put("topic_time", topic_time);
		values.put("topic_author", topic_author);
		db.update(DBHelper.TABLE_CHANNELS, values, "bid = ?", new String[] {String.valueOf(bid)});
		if(!DBHelper.getInstance().isBatch())
			db.close();
		dbHelper.releaseWriteableDatabase();
	}
	
	public synchronized Channel getChannelForBuffer(long bid) {
		Channel channel = null;
		SQLiteDatabase db = dbHelper.getSafeReadableDatabase();
		Cursor cursor = db.query(DBHelper.TABLE_CHANNELS, new String[] {"cid", "bid", "name", "topic_text", "topic_time", "topic_author", "type", "mode"}, "bid = ?", new String[] {String.valueOf(bid)}, null, null, null);
		if(cursor.moveToFirst())
			channel = cursorToChannel(cursor);
		// Make sure to close the cursor
		cursor.close();
		db.close();
		dbHelper.releaseReadableDatabase();
		return channel;
	}

	private Channel cursorToChannel(Cursor cursor) {
		Channel channel = new Channel();
		channel.cid = cursor.getInt(cursor.getColumnIndex("cid"));
		channel.bid = cursor.getLong(cursor.getColumnIndex("bid"));
		channel.name = cursor.getString(cursor.getColumnIndex("name"));
		channel.topic_text = cursor.getString(cursor.getColumnIndex("topic_text"));
		channel.topic_time = cursor.getLong(cursor.getColumnIndex("topic_time"));
		channel.topic_author = cursor.getString(cursor.getColumnIndex("topic_author"));
		channel.type = cursor.getString(cursor.getColumnIndex("type"));
		channel.mode = cursor.getString(cursor.getColumnIndex("mode"));
		return channel;
	}
}
