package com.irccloud.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class UsersDataSource {
	public class User {
		int cid;
		String channel;
		String nick;
		String old_nick = null;
		String hostmask;
		String mode;
		int away;
		String away_msg;
		int joined;
	}

	public class comparator implements Comparator<User> {
		public int compare(User u1, User u2) {
			return u1.nick.compareToIgnoreCase(u2.nick);
		}
	}
	
	private ArrayList<User> users;
	
	private static UsersDataSource instance = null;
	
	public static UsersDataSource getInstance() {
		if(instance == null)
			instance = new UsersDataSource();
		return instance;
	}

	public UsersDataSource() {
		users = new ArrayList<User>();
	}

	public synchronized void clear() {
		users.clear();
	}
	
	public synchronized User createUser(int cid, String channel, String nick, String hostmask, String mode, int away) {
		User u = findOldNickForHostmask(cid, hostmask);
		if(u == null)
			u = new User();
		else if(!u.nick.equals(nick))
			u.old_nick = u.nick;
		else
			u.old_nick = null;
		u.cid = cid;
		u.channel = channel;
		u.nick = nick;
		u.hostmask = hostmask;
		u.mode = mode;
		u.away = away;
		u.joined = 1;
		users.add(u);
		return u;
	}

	public synchronized void deleteUser(int cid, String channel, String nick) {
		User u = getUser(cid,channel,nick);
		if(u != null)
			users.remove(u);
	}

	public synchronized void deleteUsersForChannel(int cid, String channel) {
		ArrayList<User> usersToRemove = new ArrayList<User>();
		
		Iterator<User> i = users.iterator();
		while(i.hasNext()) {
			User u = i.next();
			if(u.cid == cid && u.channel.equals(channel))
				usersToRemove.add(u);
		}
		
		i=usersToRemove.iterator();
		while(i.hasNext()) {
			User u = i.next();
			users.remove(u);
		}
	}

	public synchronized void updateNick(int cid, String channel, String old_nick, String new_nick) {
		User u = getUser(cid,channel,old_nick);
		if(u != null) {
			u.nick = new_nick;
			u.old_nick = old_nick;
		}
	}
	
	public synchronized void updateAway(int cid, String channel, String nick, int away) {
		User u = getUser(cid,channel,nick);
		if(u != null)
			u.away = away;
	}
	
	public synchronized void updateAwayMsg(int cid, String nick, int away, String away_msg) {
		Iterator<User> i = users.iterator();
		while(i.hasNext()) {
			User u = i.next();
			if(u.cid == cid && u.nick.equals(nick)) {
				u.away = away;
				u.away_msg = away_msg;
			}
		}
	}
	
	public synchronized void updateHostmask(int cid, String channel, String nick, String hostmask) {
		User u = getUser(cid,channel,nick);
		if(u != null)
			u.hostmask = hostmask;
	}
	
	public synchronized void updateMode(int cid, String channel, String nick, String mode) {
		User u = getUser(cid,channel,nick);
		if(u != null)
			u.mode = mode;
	}
	
	public synchronized ArrayList<User> getUsersForChannel(int cid, String channel) {
		ArrayList<User> list = new ArrayList<User>();
		Iterator<User> i = users.iterator();
		while(i.hasNext()) {
			User u = i.next();
			if(u.cid == cid && u.channel.equals(channel) && u.joined == 1)
				list.add(u);
		}
		Collections.sort(list, new comparator());
		return list;
	}

	public synchronized User getUser(int cid, String channel, String nick) {
		Iterator<User> i = users.iterator();
		while(i.hasNext()) {
			User u = i.next();
			if(u.cid == cid && u.channel.equals(channel) && u.nick.equals(nick) && u.joined == 1)
				return u;
		}
		return null;
	}

	public synchronized User findOldNickForHostmask(int cid, String hostmask) {
		Iterator<User> i = users.iterator();
		while(i.hasNext()) {
			User u = i.next();
			if(u.cid == cid && u.hostmask.equals(hostmask) && u.joined == 0)
				return u;
		}
		return null;
	}

	public synchronized User getUser(int cid, String nick) {
		Iterator<User> i = users.iterator();
		while(i.hasNext()) {
			User u = i.next();
			if(u.cid == cid && u.nick.equals(nick) && u.joined == 1)
				return u;
		}
		return null;
	}
}
