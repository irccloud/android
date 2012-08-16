package com.irccloud.android;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class ServersDataSource {
	public class Server {
		int cid;
		String name;
		String hostname;
		int port;
		String nick;
		String status;
		long lag;
		int ssl;
		String realname;
		String server_pass;
		String nickserv_pass;
		String join_commands;
		String fail_info;
		int away;
	}

	private DBHelper dbHelper;
	private static ServersDataSource instance = null;
	
	public static ServersDataSource getInstance() {
		if(instance == null)
			instance = new ServersDataSource();
		return instance;
	}

	public ServersDataSource() {
		dbHelper = DBHelper.getInstance();
	}

	public Server createServer(int cid, String name, String hostname, int port, String nick, String status, long lag, int ssl, String realname, String server_pass, String nickserv_pass, String join_commands, String fail_info, int away) {
		SQLiteDatabase db = dbHelper.getSafeWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("cid", cid);
		values.put("name", name);
		values.put("hostname", hostname);
		values.put("port", port);
		values.put("nick", nick);
		values.put("status", status);
		values.put("lag",lag);
		values.put("ssl",ssl);
		values.put("realname",realname);
		values.put("server_pass",server_pass);
		values.put("nickserv_pass",nickserv_pass);
		values.put("join_commands",join_commands);
		values.put("fail_info",fail_info);
		values.put("away",away);
		db.insert(DBHelper.TABLE_SERVERS, null, values);
		Cursor cursor = db.query(DBHelper.TABLE_SERVERS, new String[] {"cid", "name", "hostname", "port", "nick", "status", "lag", "ssl", "realname", "server_pass", "nickserv_pass", "join_commands", "fail_info", "away"}, "cid = ?", new String[] {String.valueOf(cid)}, null, null, null);
		cursor.moveToFirst();
		Server newServer = cursorToServer(cursor);
		cursor.close();
		if(!DBHelper.getInstance().isBatch())
			db.close();
		dbHelper.releaseWriteableDatabase();
		return newServer;
	}
	
	public void updateLag(int cid, long lag) {
		SQLiteDatabase db = dbHelper.getSafeWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("lag", lag);
		db.update(DBHelper.TABLE_SERVERS, values, "cid = ?", new String[] {String.valueOf(cid)});
		if(!DBHelper.getInstance().isBatch())
			db.close();
		dbHelper.releaseWriteableDatabase();
	}

	public void updateStatus(int cid, String status, String fail_info) {
		SQLiteDatabase db = dbHelper.getSafeWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("status", status);
		values.put("fail_info", fail_info);
		db.update(DBHelper.TABLE_SERVERS, values, "cid = ?", new String[] {String.valueOf(cid)});
		if(!DBHelper.getInstance().isBatch())
			db.close();
		dbHelper.releaseWriteableDatabase();
	}

	public void deleteServer(int cid) {
		SQLiteDatabase db = dbHelper.getSafeWritableDatabase();
		db.delete(DBHelper.TABLE_SERVERS, "cid = ?", new String[] {String.valueOf(cid)});
		if(!DBHelper.getInstance().isBatch())
			db.close();
		dbHelper.releaseWriteableDatabase();
	}

	public void deleteAllDataForServer(int cid) {
		SQLiteDatabase db = dbHelper.getSafeWritableDatabase();
		db.delete(DBHelper.TABLE_SERVERS, "cid = ?", new String[] {String.valueOf(cid)});
		db.delete(DBHelper.TABLE_BUFFERS, "cid = ?", new String[] {String.valueOf(cid)});
		db.delete(DBHelper.TABLE_CHANNELS, "cid = ?", new String[] {String.valueOf(cid)});
		db.delete(DBHelper.TABLE_USERS, "cid = ?", new String[] {String.valueOf(cid)});
		db.delete(DBHelper.TABLE_EVENTS, "cid = ?", new String[] {String.valueOf(cid)});
		if(!DBHelper.getInstance().isBatch())
			db.close();
		dbHelper.releaseWriteableDatabase();
	}

	public synchronized ArrayList<Server> getServers() {
		ArrayList<Server> servers = new ArrayList<Server>();

		SQLiteDatabase db = dbHelper.getSafeReadableDatabase();
		Cursor cursor = db.query(DBHelper.TABLE_SERVERS, new String[] {"cid", "name", "hostname", "port", "nick", "status", "lag", "ssl", "realname", "server_pass", "nickserv_pass", "join_commands", "fail_info", "away"}, null, null, null, null, "cid");

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Server server = cursorToServer(cursor);
			servers.add(server);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		db.close();
		dbHelper.releaseReadableDatabase();
		return servers;
	}

	public synchronized Server getServer(int cid) {
		Server server = null;
		SQLiteDatabase db = dbHelper.getSafeReadableDatabase();
		Cursor cursor = db.query(DBHelper.TABLE_SERVERS, new String[] {"cid", "name", "hostname", "port", "nick", "status", "lag", "ssl", "realname", "server_pass", "nickserv_pass", "join_commands", "fail_info", "away"}, "cid = ?", new String[] {String.valueOf(cid)}, null, null, "cid");

		if(cursor.moveToFirst()) {
			server = cursorToServer(cursor);
		}
		// Make sure to close the cursor
		cursor.close();
		db.close();
		dbHelper.releaseReadableDatabase();
		return server;
	}

	private Server cursorToServer(Cursor cursor) {
		Server server = new Server();
		server.cid = cursor.getInt(cursor.getColumnIndex("cid"));
		server.name = cursor.getString(cursor.getColumnIndex("name"));
		server.hostname = cursor.getString(cursor.getColumnIndex("hostname"));
		server.port = cursor.getInt(cursor.getColumnIndex("port"));
		server.nick = cursor.getString(cursor.getColumnIndex("nick"));
		server.realname = cursor.getString(cursor.getColumnIndex("realname"));
		server.lag = cursor.getLong(cursor.getColumnIndex("lag"));
		server.status = cursor.getString(cursor.getColumnIndex("status"));
		server.ssl = cursor.getInt(cursor.getColumnIndex("ssl"));
		server.server_pass = cursor.getString(cursor.getColumnIndex("server_pass"));
		server.nickserv_pass = cursor.getString(cursor.getColumnIndex("nickserv_pass"));
		server.join_commands = cursor.getString(cursor.getColumnIndex("join_commands"));
		server.fail_info = cursor.getString(cursor.getColumnIndex("fail_info"));
		server.away = cursor.getInt(cursor.getColumnIndex("away"));
		return server;
	}
}
