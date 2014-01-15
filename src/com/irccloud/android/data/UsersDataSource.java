/*
 * Copyright (c) 2013 IRCCloud, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irccloud.android.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

public class UsersDataSource {
	public class User {
		public int cid;
        public int bid;
        public String nick;
        public String old_nick = null;
        public String hostmask;
        public String mode;
        public int away;
        public String away_msg;
        public int joined;
        public long last_mention = -1;
	}

	private HashMap<Integer,TreeMap<String,User>> users;
	
	private static UsersDataSource instance = null;
	
	public static UsersDataSource getInstance() {
		if(instance == null)
			instance = new UsersDataSource();
		return instance;
	}

	public UsersDataSource() {
		users = new HashMap<Integer,TreeMap<String,User>>();
	}

	public synchronized void clear() {
		users.clear();
	}
	
	public synchronized User createUser(int cid, int bid, String nick, String hostmask, String mode, int away) {
		return createUser(cid, bid, nick, hostmask, mode, away, true);
	}

	public synchronized User createUser(int cid, int bid, String nick, String hostmask, String mode, int away, boolean find_old) {
		User u = null;
		if(find_old)
			u = getUser(cid, bid, nick);
		
		if(u == null) {
			if(u == null)
				u = new User();
			else if(!u.nick.equals(nick))
				u.old_nick = u.nick;
			else
				u.old_nick = null;

            if(!users.containsKey(bid))
                users.put(bid, new TreeMap<String,User>());
			users.get(bid).put(nick.toLowerCase(),u);
		}
		u.cid = cid;
		u.bid = bid;
		u.nick = nick;
		u.hostmask = hostmask;
		u.mode = mode;
		u.away = away;
		u.joined = 1;
		return u;
	}

	public synchronized void deleteUser(int cid, int bid, String nick) {
        if(users.containsKey(bid))
            users.get(bid).remove(nick.toLowerCase());
	}

	public synchronized void deleteUsersForBuffer(int cid, int bid) {
        users.remove(bid);
	}

	public synchronized void updateNick(int cid, int bid, String old_nick, String new_nick) {
		User u = getUser(cid,bid,old_nick);
		if(u != null) {
			u.nick = new_nick;
			u.old_nick = old_nick;
            users.get(bid).remove(old_nick.toLowerCase());
            users.get(bid).put(new_nick.toLowerCase(), u);
		}
	}
	
	public synchronized void updateAway(int cid, int bid, String nick, int away) {
		User u = getUser(cid,bid,nick);
		if(u != null)
			u.away = away;
	}
	
	public synchronized void updateHostmask(int cid, int bid, String nick, String hostmask) {
		User u = getUser(cid,bid,nick);
		if(u != null)
			u.hostmask = hostmask;
	}
	
	public synchronized void updateMode(int cid, int bid, String nick, String mode) {
		User u = getUser(cid,bid,nick);
		if(u != null)
			u.mode = mode;
	}

    public synchronized void updateAwayMsg(int cid, int bid, String nick, int away, String away_msg) {
        User u = getUser(cid,bid,nick);
        u.away = away;
        u.away_msg = away_msg;
    }

    public synchronized ArrayList<User> getUsersForBuffer(int cid, int bid) {
		ArrayList<User> list = new ArrayList<User>();
        if(users.containsKey(bid)) {
            Iterator<User> i = users.get(bid).values().iterator();
            while(i.hasNext()) {
                User u = i.next();
                list.add(u);
            }
        }
		return list;
	}

	public synchronized User getUser(int cid, int bid, String nick) {
        if(users.containsKey(bid) && users.get(bid).containsKey(nick.toLowerCase())) {
            return users.get(bid).get(nick.toLowerCase());
        }
		return null;
	}
}
