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

import android.database.sqlite.SQLiteException;
import android.util.SparseArray;

import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.model.Channel;
import com.raizlabs.android.dbflow.runtime.TransactionManager;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Select;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class BuffersList {
    public class comparator implements Comparator<Buffer> {
        public int compare(Buffer b1, Buffer b2) {
            if (b1.cid < b2.cid)
                return -1;
            if (b1.cid > b2.cid)
                return 1;
            if (b1.type.equals("console"))
                return -1;
            if (b2.type.equals("console"))
                return 1;
            if (b1.bid == b2.bid)
                return 0;
            int joined1 = 1, joined2 = 1;
            Channel c = ChannelsList.getInstance().getChannelForBuffer(b1.bid);
            if (c == null)
                joined1 = 0;
            c = ChannelsList.getInstance().getChannelForBuffer(b2.bid);
            if (c == null)
                joined2 = 0;
            if (b1.type.equals("conversation") && b2.type.equals("channel")) {
                return 1;
            } else if (b1.type.equals("channel") && b2.type.equals("conversation")) {
                return -1;
            } else if (joined1 != joined2) {
                return joined2 - joined1;
            } else {
                if (collator.compare(b1.normalizedName(), b2.normalizedName()) == 0)
                    return (b1.bid < b2.bid) ? -1 : 1;
                return collator.compare(b1.normalizedName(), b2.normalizedName());
            }
        }
    }

    private ArrayList<Buffer> buffers;
    private SparseArray<Buffer> buffers_indexed;
    private Collator collator;

    private static BuffersList instance = null;
    public boolean dirty = true;

    public synchronized static BuffersList getInstance() {
        if (instance == null)
            instance = new BuffersList();
        return instance;
    }

    public BuffersList() {
        buffers = new ArrayList<>(100);
        buffers_indexed = new SparseArray<>(100);
        collator = Collator.getInstance();
        collator.setStrength(Collator.SECONDARY);
    }

    public void clear() {
        buffers.clear();
        buffers_indexed.clear();
        new Delete().from(Buffer.class).queryClose();
    }

    public void load() {
        try {
            List<Buffer> c = new Select().all().from(Buffer.class).queryList();
            if (c != null && !c.isEmpty()) {
                for (Buffer b : c) {
                    buffers.add(b);
                    buffers_indexed.put(b.bid, b);
                }
            }
        } catch (SQLiteException e) {
            buffers.clear();
            buffers_indexed.clear();
        }
    }

    public void save() {
        TransactionManager.getInstance().saveOnSaveQueue(buffers);
    }


    public int count() {
        return buffers.size();
    }

    public int firstBid() {
        if (buffers_indexed.size() > 0)
            return buffers_indexed.valueAt(0).bid;
        else
            return -1;
    }

    public synchronized Buffer createBuffer(int bid, int cid, long min_eid, long last_seen_eid, String name, String type, int archived, int deferred, int timeout) {
        Buffer b = getBuffer(bid);
        if (b == null) {
            b = new Buffer();
            buffers.add(b);
            buffers_indexed.put(bid, b);
        }
        b.bid = bid;
        b.cid = cid;
        b.min_eid = min_eid;
        b.last_seen_eid = last_seen_eid;
        b.name = name;
        b.type = type;
        b.archived = archived;
        b.deferred = deferred;
        b.timeout = timeout;
        b.valid = 1;
        dirty = true;
        b.unread = 0;
        b.highlights = 0;
        return b;
    }

    public synchronized void updateLastSeenEid(int bid, long last_seen_eid) {
        Buffer b = getBuffer(bid);
        if (b != null && b.last_seen_eid < last_seen_eid) {
            b.last_seen_eid = last_seen_eid;
            TransactionManager.getInstance().saveOnSaveQueue(b);
        }
    }

    public synchronized void updateArchived(int bid, int archived) {
        Buffer b = getBuffer(bid);
        if (b != null) {
            b.archived = archived;
            TransactionManager.getInstance().saveOnSaveQueue(b);
        }
        dirty = true;
    }

    public synchronized void updateTimeout(int bid, int timeout) {
        Buffer b = getBuffer(bid);
        if (b != null) {
            b.timeout = timeout;
            TransactionManager.getInstance().saveOnSaveQueue(b);
        }
    }

    public synchronized void updateName(int bid, String name) {
        Buffer b = getBuffer(bid);
        if (b != null) {
            b.name = name;
            TransactionManager.getInstance().saveOnSaveQueue(b);
        }
        dirty = true;
    }

    public synchronized void updateAway(int cid, String nick, String away_msg) {
        Buffer b = getBufferByName(cid, nick);
        if (b != null) {
            b.away_msg = away_msg;
            TransactionManager.getInstance().saveOnSaveQueue(b);
        }
    }

    public synchronized void updateDraft(int bid, String draft) {
        Buffer b = getBuffer(bid);
        if (b != null) {
            b.draft = draft;
            TransactionManager.getInstance().saveOnSaveQueue(b);
        }
    }

    public synchronized void deleteAllDataForBuffer(int bid) {
        Buffer b = getBuffer(bid);
        if (b != null) {
            if (b.type.equalsIgnoreCase("channel")) {
                ChannelsList.getInstance().deleteChannel(bid);
                UsersList.getInstance().deleteUsersForBuffer(b.bid);
            }
            EventsList.getInstance().deleteEventsForBuffer(bid);
            buffers.remove(b);
            b.delete();
        }
        buffers_indexed.remove(bid);
        dirty = true;
    }

    public synchronized Buffer getBuffer(int bid) {
        return buffers_indexed.get(bid);
    }

    public synchronized Buffer getBufferByName(int cid, String name) {
        for (Buffer b : buffers) {
            if (b.cid == cid && b.name.equalsIgnoreCase(name))
                return b;
        }
        return null;
    }

    public synchronized ArrayList<Buffer> getBuffersForServer(int cid) {
        ArrayList<Buffer> list = new ArrayList<Buffer>();
        if (dirty) {
            try {
                Collections.sort(buffers, new comparator());
            } catch (Exception e) {
            }
            dirty = false;
        }
        for (Buffer b : buffers) {
            if (b.cid == cid)
                list.add(b);
        }
        return list;
    }

    public synchronized ArrayList<Buffer> getBuffers() {
        ArrayList<Buffer> list = new ArrayList<Buffer>();
        for (Buffer b : buffers) {
            list.add(b);
        }
        return list;
    }

    public synchronized void invalidate() {
        for (Buffer b : buffers) {
            b.valid = 0;
        }
    }

    public synchronized void purgeInvalidBIDs() {
        ArrayList<Buffer> buffersToRemove = new ArrayList<Buffer>();
        Iterator<Buffer> i = buffers.iterator();
        while (i.hasNext()) {
            Buffer b = i.next();
            if (b.valid == 0)
                buffersToRemove.add(b);
        }
        i = buffersToRemove.iterator();
        while (i.hasNext()) {
            Buffer b = i.next();
            EventsList.getInstance().deleteEventsForBuffer(b.bid);
            ChannelsList.getInstance().deleteChannel(b.bid);
            UsersList.getInstance().deleteUsersForBuffer(b.bid);
            buffers.remove(b);
            buffers_indexed.remove(b.bid);
            if (b.type.equalsIgnoreCase("console")) {
                ServersList.getInstance().deleteServer(b.cid);
            }
            b.delete();
        }
        dirty = true;
    }
}
