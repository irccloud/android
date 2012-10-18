package com.irccloud.android;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

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
		String away;
		String usermask;
		String mode;
		JsonObject isupport;
		JsonArray ignores;
	}

	private ArrayList<Server> servers;
	
	private static ServersDataSource instance = null;
	
	public static ServersDataSource getInstance() {
		if(instance == null)
			instance = new ServersDataSource();
		return instance;
	}

	public ServersDataSource() {
		servers = new ArrayList<Server>();
	}

	public void clear() {
		servers.clear();
	}
	
	public Server createServer(int cid, String name, String hostname, int port, String nick, String status, long lag, int ssl, String realname, String server_pass, String nickserv_pass, String join_commands, String fail_info, String away, JsonArray ignores) {
		Server s = new Server();
		s.cid = cid;
		s.name = name;
		s.hostname = hostname;
		s.port = port;
		s.nick = nick;
		s.status = status;
		s.lag = lag;
		s.ssl = ssl;
		s.realname = realname;
		s.server_pass = server_pass;
		s.nickserv_pass = nickserv_pass;
		s.join_commands = join_commands;
		s.fail_info = fail_info;
		s.away = away;
		s.usermask = "";
		s.mode = "";
		s.ignores = ignores;
		servers.add(s);
		return s;
	}
	
	int count() {
		return servers.size();
	}
	
	public void updateLag(int cid, long lag) {
		Server s = getServer(cid);
		if(s != null) {
			s.lag = lag;
		}
	}

	public void updateStatus(int cid, String status, String fail_info) {
		Server s = getServer(cid);
		if(s != null) {
			s.status = status;
			s.fail_info = fail_info;
		}
	}

	public void updateAway(int cid, String away) {
		Server s = getServer(cid);
		if(s != null) {
			s.away = away;
		}
	}

	public void updateUsermask(int cid, String usermask) {
		Server s = getServer(cid);
		if(s != null) {
			s.usermask = usermask;
		}
	}

	public void updateMode(int cid, String mode) {
		Server s = getServer(cid);
		if(s != null) {
			s.mode = mode;
		}
	}

	public void updateIsupport(int cid, JsonObject params) {
		Server s = getServer(cid);
		if(s != null) {
			s.isupport = params;
		}
	}

	public void updateIgnores(int cid, JsonArray ignores) {
		Server s = getServer(cid);
		if(s != null) {
			s.ignores = ignores;
		}
	}

	public void deleteServer(int cid) {
		Server s = getServer(cid);
		if(s != null) {
			servers.remove(s);
		}
	}

	public void deleteAllDataForServer(int cid) {
		Server s = getServer(cid);
		if(s != null) {
			ArrayList<BuffersDataSource.Buffer> buffersToRemove = new ArrayList<BuffersDataSource.Buffer>();
			
			Iterator<BuffersDataSource.Buffer> i = BuffersDataSource.getInstance().getBuffersForServer(cid).iterator();
			while(i.hasNext()) {
				BuffersDataSource.Buffer b = i.next();
				buffersToRemove.add(b);
			}
			
			i=buffersToRemove.iterator();
			while(i.hasNext()) {
				BuffersDataSource.Buffer b = i.next();
				BuffersDataSource.getInstance().deleteAllDataForBuffer(b.bid);
			}
			servers.remove(s);
		}
	}

	public synchronized ArrayList<Server> getServers() {
		return servers;
	}

	public Server getServer(int cid) {
		Iterator<Server> i = servers.iterator();
		while(i.hasNext()) {
			Server s = i.next();
			if(s.cid == cid)
				return s;
		}
		return null;
	}
}
