package com.irccloud.android;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
	public static final String TABLE_SERVERS = "servers";
	public static final String TABLE_BUFFERS = "buffers";
	public static final String TABLE_CHANNELS = "channels";
	public static final String TABLE_USERS = "users";
	public static final String TABLE_EVENTS = "events";

	private static final String DATABASE_NAME = "irc.db";
	private static final int DATABASE_VERSION = 1;

	private static DBHelper instance = null;
	
	public static DBHelper getInstance() {
		if(instance == null)
			instance = new DBHelper();
		return instance;
	}
	
	public DBHelper() {
		super(IRCCloudApplication.getInstance().getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL("create table " + TABLE_SERVERS + " ("
				+ "cid integer primary key, "
				+ "name text not null, "
				+ "hostname text not null, "
				+ "port integer not null, "
				+ "nick text not null, "
				+ "connected integer"
				+ ");");

		database.execSQL("create table " + TABLE_BUFFERS + " ("
				+ "bid integer primary key, "
				+ "cid integer not null, "
				+ "max_eid integer not null, "
				+ "last_seen_eid integer not null, "
				+ "name text not null, "
				+ "type text not null, "
				+ "hidden integer, "
				+ "joined integer"
				+ ");");
		
		database.execSQL("create table " + TABLE_CHANNELS + " ("
				+ "bid integer primary key, "
				+ "cid integer not null, "
				+ "name text not null, "
				+ "topic_text text not null, "
				+ "topic_time integer not null, "
				+ "topic_author text not null, "
				+ "mode text"
				+ ");");
		
		database.execSQL("create table " + TABLE_USERS + " ("
				+ "cid integer not null, "
				+ "channel text not null, "
				+ "nick text not null, "
				+ "hostmask text not null, "
				+ "mode text, "
				+ "away integer, "
				+ "PRIMARY KEY(cid,channel,nick));");
		
		database.execSQL("create table " + TABLE_EVENTS + " ("
				+ "eid integer primary key, "
				+ "cid integer not null, "
				+ "bid integer not null, "
				+ "type text not null, "
				+ "highlight integer not null, "
				+ "event text not null, "
				+ ");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVERS);
		onCreate(db);
	}
}
