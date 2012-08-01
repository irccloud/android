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
		int connected;
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

	public Server createServer(int cid, String name, String hostname, int port, String nick, int connected) {
		synchronized(dbHelper) {
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			ContentValues values = new ContentValues();
			values.put("cid", cid);
			values.put("name", name);
			values.put("hostname", hostname);
			values.put("port", port);
			values.put("nick", nick);
			values.put("connected", connected);
			db.insert(DBHelper.TABLE_SERVERS, null, values);
			Cursor cursor = db.query(DBHelper.TABLE_SERVERS, new String[] {"cid", "name", "hostname", "port", "nick", "connected"}, "cid = ?", new String[] {String.valueOf(cid)}, null, null, null);
			cursor.moveToFirst();
			Server newServer = cursorToServer(cursor);
			cursor.close();
			if(!DBHelper.getInstance().isBatch())
				db.close();
			return newServer;
		}
	}

	public void deleteServer(int cid) {
		synchronized(dbHelper) {
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			db.delete(DBHelper.TABLE_SERVERS, "cid = ?", new String[] {String.valueOf(cid)});
			if(!DBHelper.getInstance().isBatch())
				db.close();
		}
	}

	public synchronized ArrayList<Server> getServers() {
		synchronized(dbHelper) {
			ArrayList<Server> servers = new ArrayList<Server>();
	
			SQLiteDatabase db = dbHelper.getReadableDatabase();
			Cursor cursor = db.query(DBHelper.TABLE_SERVERS, new String[] {"cid", "name", "hostname", "port", "nick", "connected"}, null, null, null, null, "cid");
	
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				Server server = cursorToServer(cursor);
				servers.add(server);
				cursor.moveToNext();
			}
			// Make sure to close the cursor
			cursor.close();
			db.close();
			return servers;
		}
	}

	private Server cursorToServer(Cursor cursor) {
		Server server = new Server();
		server.cid = cursor.getInt(cursor.getColumnIndex("cid"));
		server.name = cursor.getString(cursor.getColumnIndex("name"));
		server.hostname = cursor.getString(cursor.getColumnIndex("hostname"));
		server.port = cursor.getInt(cursor.getColumnIndex("port"));
		server.nick = cursor.getString(cursor.getColumnIndex("nick"));
		server.connected = cursor.getInt(cursor.getColumnIndex("connected"));
		return server;
	}
}
