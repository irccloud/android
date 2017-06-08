/*
 * Copyright (c) 2015 IRCCloud, Ltd.
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

package com.irccloud.android.data.collection;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;

import com.irccloud.android.AlphanumComparator;
import com.irccloud.android.data.model.User;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.runtime.TransactionManager;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.ModelAdapter;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

@SuppressLint("UseSparseArrays")
public class UsersList {
    private final HashMap<Integer, TreeMap<String, User>> users;
    private HashSet<Integer> loaded_bids = new HashSet<>();
    private Collator collator;

    private static UsersList instance = null;

    public synchronized static UsersList getInstance() {
        if (instance == null)
            instance = new UsersList();
        return instance;
    }

    public UsersList() {
        users = new HashMap<>(1000);
        collator = Collator.getInstance();
        collator.setStrength(Collator.SECONDARY);
        comparator = new AlphanumComparator(collator);
    }

    public synchronized void clear() {
        users.clear();
        //Delete.table(User.class);
    }

    public void load(int bid) {
        /*synchronized (users) {
            Cursor c = new Select().from(User.class).where(Condition.column(User$Table.BID).is(bid)).query();
            try {
                if(loaded_bids.contains(bid))
                    return;
                long start = System.currentTimeMillis();
                ModelAdapter<User> modelAdapter = FlowManager.getModelAdapter(User.class);
                if (c != null && c.moveToFirst()) {
                    android.util.Log.d("IRCCloud", "Loading users for bid" + bid);
                    do {
                        User e = modelAdapter.loadFromCursor(c);
                        if (!users.containsKey(e.bid) || users.get(e.bid) == null)
                            users.put(e.bid, new TreeMap<String, User>(comparator));
                        users.get(e.bid).put(e.nick.toLowerCase(), e);
                    } while (c.moveToNext());
                    long time = System.currentTimeMillis() - start;
                    android.util.Log.i("IRCCloud", "Loaded " + c.getCount() + " users in " + time + "ms");
                    loaded_bids.add(bid);
                }
            } catch (SQLiteException e) {
                NetworkConnection.printStackTraceToCrashlytics(e);
            } finally {
                if(c != null)
                    c.close();
            }
        }*/
    }

    public void save() {
        /*for (int bid : users.keySet()) {
            TreeMap<String, User> e = users.get(bid);
            if(e != null) {
                TransactionManager.getInstance().saveOnSaveQueue(e.values());
            }
        }*/
    }

    public synchronized User createUser(int cid, int bid, String nick, String hostmask, String mode, String ircserver, int away) {
        return createUser(cid, bid, nick, hostmask, mode, ircserver, away, true);
    }

    public synchronized User createUser(int cid, int bid, String nick, String hostmask, String mode, String ircserver, int away, boolean find_old) {
        User u = null;
        if (find_old)
            u = getUser(bid, nick);

        if (u == null) {
            u = new User();

            TreeMap<String, User> usersList = users.get(bid);
            if (users.get(bid) == null) {
                usersList = new TreeMap<>(comparator);
                users.put(bid, usersList);
            }
            usersList.put(nick.toLowerCase(), u);
        }
        u.cid = cid;
        u.bid = bid;
        u.nick = nick;
        u.nick_lowercase = nick.toLowerCase();
        u.hostmask = hostmask;
        u.mode = mode;
        u.ircserver = ircserver;
        u.away = away;
        u.joined = 1;
        return u;
    }

    private AlphanumComparator comparator;

    public synchronized void deleteUser(int bid, String nick) {
        User u = getUser(bid, nick);
        /*if(u != null)
            u.delete();*/
        if (users.containsKey(bid) && users.get(bid) != null)
            users.get(bid).remove(nick.toLowerCase());
    }

    public synchronized void deleteUsersForBuffer(int bid) {
        users.remove(bid);
        //new Delete().from(User.class).where(Condition.column(User$Table.BID).is(bid)).queryClose();
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

    public synchronized void updateAway(int cid, String nick, int away) {
        for (Integer bid : users.keySet()) {
            if (users.get(bid) != null && users.get(bid).containsKey(nick.toLowerCase()) && users.get(bid).get(nick.toLowerCase()).cid == cid) {
                User u = users.get(bid).get(nick.toLowerCase());
                if (u != null) {
                    u.away = away;
                }
            }
        }
    }

    public synchronized void updateHostmask(int bid, String nick, String hostmask) {
        User u = getUser(bid, nick);
        if (u != null) {
            u.hostmask = hostmask;
        }
    }

    public synchronized void updateMode(int bid, String nick, String mode) {
        User u = getUser(bid, nick);
        if (u != null) {
            u.mode = mode;
        }
    }

    public synchronized void updateAwayMsg(int cid, String nick, int away, String away_msg) {
        for (Integer bid : users.keySet()) {
            if (users.get(bid) != null && users.get(bid).containsKey(nick.toLowerCase()) && users.get(bid).get(nick.toLowerCase()).cid == cid) {
                User u = users.get(bid).get(nick.toLowerCase());
                if (u != null) {
                    u.away = away;
                    u.away_msg = away_msg;
                }
            }
        }
    }

    public synchronized ArrayList<User> getUsersForBuffer(int bid) {
        load(bid);
        ArrayList<User> list = new ArrayList<>();
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
        load(bid);
        if (nick != null && users.containsKey(bid) && users.get(bid) != null && users.get(bid).containsKey(nick.toLowerCase())) {
            return users.get(bid).get(nick.toLowerCase());
        }
        return null;
    }

    public synchronized User findUserOnConnection(int cid, String nick) {
        //return new Select().from(User.class).where(Condition.column(User$Table.CID).is(cid)).and(Condition.column(User$Table.NICK_LOWERCASE).is(nick.toLowerCase())).querySingle();
        for (Integer bid : users.keySet()) {
            if (users.get(bid) != null && users.get(bid).containsKey(nick.toLowerCase()) && users.get(bid).get(nick.toLowerCase()).cid == cid)
                return users.get(bid).get(nick.toLowerCase());
        }
        return null;
    }
}
