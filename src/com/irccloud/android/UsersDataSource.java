package com.irccloud.android;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class UsersDataSource {
	public class User {
		int cid;
		String channel;
		String nick;
		String hostmask;
		String mode;
		int away;
	}

	private DBHelper dbHelper;
	private static UsersDataSource instance = null;
	
	public static UsersDataSource getInstance() {
		if(instance == null)
			instance = new UsersDataSource();
		return instance;
	}

	public UsersDataSource() {
		dbHelper = DBHelper.getInstance();
	}

	public User createUser(int cid, String channel, String nick, String hostmask, String mode, int away) {
		SQLiteDatabase db = dbHelper.getSafeWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("cid", cid);
		values.put("channel", channel);
		values.put("nick", nick);
		values.put("hostmask", hostmask);
		values.put("mode",mode);
		values.put("away", away);
		db.insert(DBHelper.TABLE_USERS, null, values);
		Cursor cursor = db.query(DBHelper.TABLE_USERS, new String[] {"cid", "channel", "nick", "hostmask", "mode", "away"}, "cid = ? and channel = ? and nick = ?", new String[] {String.valueOf(cid), channel, nick}, null, null, null);
		cursor.moveToFirst();
		User newUser = cursorToUser(cursor);
		cursor.close();
		if(!DBHelper.getInstance().isBatch())
			db.close();
		dbHelper.releaseWriteableDatabase();
		return newUser;
	}

	public void deleteUser(int cid, String channel, String nick) {
		SQLiteDatabase db = dbHelper.getSafeWritableDatabase();
		db.delete(DBHelper.TABLE_USERS, "cid = ? and channel = ? and nick = ?", new String[] {String.valueOf(cid), channel, nick});
		if(!DBHelper.getInstance().isBatch())
			db.close();
		dbHelper.releaseWriteableDatabase();
	}

	public void deleteUsersForChannel(int cid, String channel) {
		SQLiteDatabase db = dbHelper.getSafeWritableDatabase();
		db.delete(DBHelper.TABLE_USERS, "cid = ? and channel = ?", new String[] {String.valueOf(cid), channel});
		if(!DBHelper.getInstance().isBatch())
			db.close();
		dbHelper.releaseWriteableDatabase();
	}

	public void updateNick(int cid, String channel, String old_nick, String new_nick) {
		SQLiteDatabase db = dbHelper.getSafeWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("nick", new_nick);
		db.update(DBHelper.TABLE_USERS, values, "cid = ? and channel = ? and nick = ?", new String[] {String.valueOf(cid), channel, old_nick});
		if(!DBHelper.getInstance().isBatch())
			db.close();
		dbHelper.releaseWriteableDatabase();
	}
	
	public void updateAway(int cid, String channel, String nick, int away) {
		SQLiteDatabase db = dbHelper.getSafeWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("away", away);
		db.update(DBHelper.TABLE_USERS, values, "cid = ? and channel = ? and nick = ?", new String[] {String.valueOf(cid), channel, nick});
		if(!DBHelper.getInstance().isBatch())
			db.close();
		dbHelper.releaseWriteableDatabase();
	}
	
	public void updateHostmask(int cid, String channel, String nick, String hostmask) {
		SQLiteDatabase db = dbHelper.getSafeWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("hostmask", hostmask);
		db.update(DBHelper.TABLE_USERS, values, "cid = ? and channel = ? and nick = ?", new String[] {String.valueOf(cid), channel, nick});
		if(!DBHelper.getInstance().isBatch())
			db.close();
		dbHelper.releaseWriteableDatabase();
	}
	
	public void updateMode(int cid, String channel, String nick, String mode) {
		SQLiteDatabase db = dbHelper.getSafeWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("mode", mode);
		db.update(DBHelper.TABLE_USERS, values, "cid = ? and channel = ? and nick = ?", new String[] {String.valueOf(cid), channel, nick});
		if(!DBHelper.getInstance().isBatch())
			db.close();
		dbHelper.releaseWriteableDatabase();
	}
	
	public synchronized ArrayList<User> getUsersForChannel(int cid, String channel) {
		ArrayList<User> users = new ArrayList<User>();

		SQLiteDatabase db = dbHelper.getSafeReadableDatabase();
		Cursor cursor = db.query(DBHelper.TABLE_USERS, new String[] {"cid", "channel", "nick", "hostmask", "mode", "away"}, "cid = ? and channel = ?", new String[] {String.valueOf(cid), channel}, null, null, "mode,nick");

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			User user = cursorToUser(cursor);
			users.add(user);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		db.close();
		dbHelper.releaseReadableDatabase();
		return users;
	}

	public synchronized User getUser(int cid, String channel, String nick) {
		User user = null;
		SQLiteDatabase db = dbHelper.getSafeReadableDatabase();
		Cursor cursor = db.query(DBHelper.TABLE_USERS, new String[] {"cid", "channel", "nick", "hostmask", "mode", "away"}, "cid = ? and channel = ? and nick = ?", new String[] {String.valueOf(cid), channel, nick}, null, null, "mode,nick");

		if(cursor.moveToFirst()) {
			user = cursorToUser(cursor);
		}
		// Make sure to close the cursor
		cursor.close();
		db.close();
		dbHelper.releaseReadableDatabase();
		return user;
	}

	private User cursorToUser(Cursor cursor) {
		User user = new User();
		user.cid = cursor.getInt(cursor.getColumnIndex("cid"));
		user.channel = cursor.getString(cursor.getColumnIndex("channel"));
		user.nick = cursor.getString(cursor.getColumnIndex("nick"));
		user.hostmask = cursor.getString(cursor.getColumnIndex("hostmask"));
		user.mode = cursor.getString(cursor.getColumnIndex("mode"));
		user.away = cursor.getInt(cursor.getColumnIndex("away"));
		return user;
	}
}
