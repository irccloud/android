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

import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.util.SparseArray;

import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.model.Channel;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.runtime.TransactionManager;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.ModelAdapter;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class BuffersList {
    public class comparator implements Comparator<Buffer> {
        public int compare(Buffer b1, Buffer b2) {
            if (b1.getCid() < b2.getCid())
                return -1;
            if (b1.getCid() > b2.getCid())
                return 1;
            if (b1.isConsole())
                return -1;
            if (b2.isConsole())
                return 1;
            if (b1.getBid() == b2.getBid())
                return 0;
            int joined1 = 1, joined2 = 1;
            Channel c = ChannelsList.getInstance().getChannelForBuffer(b1.getBid());
            if (c == null)
                joined1 = 0;
            c = ChannelsList.getInstance().getChannelForBuffer(b2.getBid());
            if (c == null)
                joined2 = 0;
            if (b1.isConversation() && b2.isChannel()) {
                return 1;
            } else if (b1.isChannel() && b2.isConversation()) {
                return -1;
            } else if (joined1 != joined2) {
                return joined2 - joined1;
            } else {
                if (collator.compare(b1.normalizedName(), b2.normalizedName()) == 0)
                    return (b1.getBid() < b2.getBid()) ? -1 : 1;
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
        buffers = new ArrayList<>();
        buffers_indexed = new SparseArray<>();
        collator = Collator.getInstance();
        collator.setStrength(Collator.SECONDARY);
    }

    public void clear() {
        buffers.clear();
        buffers_indexed.clear();
        Delete.table(Buffer.class);
    }

    public void load() {
        try {
            long start = System.currentTimeMillis();
            ModelAdapter<Buffer> modelAdapter = FlowManager.getModelAdapter(Buffer.class);
            Cursor c = new Select().all().from(Buffer.class).query();
            if (c != null && c.moveToFirst()) {
                buffers = new ArrayList<>(c.getCount());
                buffers_indexed = new SparseArray<>(c.getCount());
                do {
                    Buffer b = modelAdapter.loadFromCursor(c);
                    buffers.add(b);
                    buffers_indexed.put(b.getBid(), b);
                } while(c.moveToNext());
                c.close();
                long time = System.currentTimeMillis() - start;
                android.util.Log.i("IRCCloud", "Loaded " + c.getCount() + " buffers in " + time + "ms");
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
            return buffers_indexed.valueAt(0).getBid();
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
        b.setBid(bid);
        b.setCid(cid);
        b.setMin_eid(min_eid);
        b.setLast_seen_eid(last_seen_eid);
        b.setName(name);
        b.setType(type);
        b.setArchived(archived);
        b.setDeferred(deferred);
        b.setTimeout(timeout);
        b.setValid(1);
        if(EventsList.getInstance().lastEidForBuffer(bid) <= last_seen_eid) {
            b.setUnread(0);
            b.setHighlights(0);
        }
        dirty = true;
        return b;
    }

    public synchronized void deleteAllDataForBuffer(int bid) {
        Buffer b = getBuffer(bid);
        if (b != null) {
            if (b.isChannel()) {
                ChannelsList.getInstance().deleteChannel(bid);
                UsersList.getInstance().deleteUsersForBuffer(b.getBid());
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
            if (b.getCid() == cid && b.getName().equalsIgnoreCase(name))
                return b;
        }
        return null;
    }

    public synchronized ArrayList<Buffer> getBuffersForServer(int cid) {
        ArrayList<Buffer> list = new ArrayList<>();
        if (dirty) {
            try {
                Collections.sort(buffers, new comparator());
            } catch (Exception e) {
            }
            dirty = false;
        }
        for (Buffer b : buffers) {
            if (b.getCid() == cid)
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
            b.setValid(0);
        }
    }

    public synchronized void purgeInvalidBIDs() {
        ArrayList<Buffer> buffersToRemove = new ArrayList<Buffer>();
        Iterator<Buffer> i = buffers.iterator();
        while (i.hasNext()) {
            Buffer b = i.next();
            if (b.getValid() == 0)
                buffersToRemove.add(b);
        }
        i = buffersToRemove.iterator();
        while (i.hasNext()) {
            Buffer b = i.next();
            EventsList.getInstance().deleteEventsForBuffer(b.getBid());
            ChannelsList.getInstance().deleteChannel(b.getBid());
            UsersList.getInstance().deleteUsersForBuffer(b.getBid());
            buffers.remove(b);
            buffers_indexed.remove(b.getBid());
            if (b.isConsole()) {
                ServersList.getInstance().deleteServer(b.getCid());
            }
            b.delete();
        }
        dirty = true;
    }
}
