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

import android.annotation.SuppressLint;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

@SuppressLint("UseSparseArrays")
public class UsersDataSource {
    public static class User {
        public int cid;
        public int bid;
        public String nick;
        public String old_nick = null;
        public String nick_lowercase;
        public String hostmask;
        public String mode;
        public int away;
        public String away_msg;
        public int joined;
        public long last_mention = -1;
    }

    private HashMap<Integer, TreeMap<String, User>> users;
    private Collator collator;

    private static UsersDataSource instance = null;

    public synchronized static UsersDataSource getInstance() {
        if (instance == null)
            instance = new UsersDataSource();
        return instance;
    }

    public UsersDataSource() {
        users = new HashMap<Integer, TreeMap<String, User>>();
        collator = Collator.getInstance();
        collator.setStrength(Collator.SECONDARY);
    }

    public synchronized void clear() {
        users.clear();
    }

    public synchronized User createUser(int cid, int bid, String nick, String hostmask, String mode, int away) {
        return createUser(cid, bid, nick, hostmask, mode, away, true);
    }

    public synchronized User createUser(int cid, int bid, String nick, String hostmask, String mode, int away, boolean find_old) {
        User u = null;
        if (find_old)
            u = getUser(bid, nick);

        if (u == null) {
            u = new User();

            if (!users.containsKey(bid) || users.get(bid) == null)
                users.put(bid, new TreeMap<String, User>(comparator));
            users.get(bid).put(nick.toLowerCase(), u);
        }
        u.cid = cid;
        u.bid = bid;
        u.nick = nick;
        u.nick_lowercase = nick.toLowerCase();
        u.hostmask = hostmask;
        u.mode = mode;
        u.away = away;
        u.joined = 1;
        return u;
    }

    private Comparator<String> comparator = new Comparator<String>() {
        public int compare(String o1, String o2) {
            return collator.compare(o1, o2);
        }
    };

    public synchronized void deleteUser(int bid, String nick) {
        if (users.containsKey(bid) && users.get(bid) != null)
            users.get(bid).remove(nick.toLowerCase());
    }

    public synchronized void deleteUsersForBuffer(int bid) {
        users.remove(bid);
    }

    public synchronized void updateNick(int bid, String old_nick, String new_nick) {
        User u = getUser(bid, old_nick);
        if (u != null) {
            u.nick = new_nick;
            u.nick_lowercase = new_nick.toLowerCase();
            u.old_nick = old_nick;
            users.get(bid).remove(old_nick.toLowerCase());
            users.get(bid).put(new_nick.toLowerCase(), u);
        }
    }

    public synchronized void updateAway(int bid, String nick, int away) {
        User u = getUser(bid, nick);
        if (u != null)
            u.away = away;
    }

    public synchronized void updateHostmask(int bid, String nick, String hostmask) {
        User u = getUser(bid, nick);
        if (u != null)
            u.hostmask = hostmask;
    }

    public synchronized void updateMode(int bid, String nick, String mode) {
        User u = getUser(bid, nick);
        if (u != null)
            u.mode = mode;
    }

    public synchronized void updateAwayMsg(int bid, String nick, int away, String away_msg) {
        User u = getUser(bid, nick);
        if (u != null) {
            u.away = away;
            u.away_msg = away_msg;
        }
    }

    public synchronized ArrayList<User> getUsersForBuffer(int bid) {
        ArrayList<User> list = new ArrayList<User>();
        if (users.containsKey(bid) && users.get(bid) != null) {
            for (User u : users.get(bid).values()) {
                list.add(u);
            }
        }
        return list;
    }

    public synchronized User getUser(int bid, String nick) {
        if (nick != null && users.containsKey(bid) && users.get(bid) != null && users.get(bid).containsKey(nick.toLowerCase())) {
            return users.get(bid).get(nick.toLowerCase());
        }
        return null;
    }

    public synchronized User findUserOnConnection(int cid, String nick) {
        for (Integer bid : users.keySet()) {
            if (users.get(bid).containsKey(nick.toLowerCase()) && users.get(bid) != null && users.get(bid).get(nick.toLowerCase()).cid == cid)
                return users.get(bid).get(nick.toLowerCase());
        }
        return null;
    }
}
