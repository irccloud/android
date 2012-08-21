package com.irccloud.android;

import java.util.ArrayList;
import java.util.Iterator;

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
	
	public Server createServer(int cid, String name, String hostname, int port, String nick, String status, long lag, int ssl, String realname, String server_pass, String nickserv_pass, String join_commands, String fail_info, String away) {
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
		servers.add(s);
		return s;
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
