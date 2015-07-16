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

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.ColorFormatter;
import com.irccloud.android.data.model.Channel;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.runtime.TransactionManager;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.ModelAdapter;

import java.util.ArrayList;

public class ChannelsList {
    private SparseArray<Channel> channels;

    private static ChannelsList instance = null;

    public synchronized static ChannelsList getInstance() {
        if (instance == null)
            instance = new ChannelsList();
        return instance;
    }

    public ChannelsList() {
        channels = new SparseArray<>();
    }

    public synchronized void clear() {
        channels.clear();
        Delete.table(Channel.class);
    }

    public void load() {
        try {
            long start = System.currentTimeMillis();
            ModelAdapter<Channel> modelAdapter = FlowManager.getModelAdapter(Channel.class);
            Cursor c = new Select().all().from(Channel.class).query();
            if (c != null && c.moveToFirst()) {
                channels = new SparseArray<>(c.getCount());
                do {
                    Channel s = modelAdapter.loadFromCursor(c);
                    channels.put(s.bid, s);
                    updateMode(s.bid, s.mode, s.ops, true);
                } while(c.moveToNext());
                c.close();
                long time = System.currentTimeMillis() - start;
                android.util.Log.i("IRCCloud", "Loaded " + c.getCount() + " channels in " + time + "ms");
            }
        } catch (SQLiteException e) {
            channels.clear();
        }
    }

    public void save() {
        ArrayList<Channel> s = new ArrayList<>(channels.size());
        for (int i = 0; i < channels.size(); i++) {
            s.add(channels.valueAt(i));
        }
        TransactionManager.getInstance().saveOnSaveQueue(s);
    }

    public synchronized Channel createChannel(int cid, int bid, String name, String topic_text, long topic_time, String topic_author, String type, long timestamp) {
        Channel c = getChannelForBuffer(bid);
        if (c == null) {
            c = new Channel();
            channels.put(bid, c);
        }
        c.cid = cid;
        c.bid = bid;
        c.name = name;
        c.topic_author = topic_author;
        c.topic_text = ColorFormatter.emojify(topic_text);
        c.topic_time = topic_time;
        c.type = type;
        c.timestamp = timestamp;
        c.key = false;
        c.mode = "";
        c.valid = 1;
        TransactionManager.getInstance().saveOnSaveQueue(c);
        return c;
    }

    public synchronized void deleteChannel(int bid) {
        channels.remove(bid);
    }

    public synchronized void updateTopic(int bid, String topic_text, long topic_time, String topic_author) {
        Channel c = getChannelForBuffer(bid);
        if (c != null) {
            c.topic_text = ColorFormatter.emojify(topic_text);
            c.topic_time = topic_time;
            c.topic_author = topic_author;
            TransactionManager.getInstance().saveOnSaveQueue(c);
        }
    }

    public synchronized void updateMode(int bid, String mode, JsonNode ops, boolean init) {
        Channel c = getChannelForBuffer(bid);
        if (c != null) {
            c.key = false;
            JsonNode add = ops.get("add");
            for (int i = 0; i < add.size(); i++) {
                JsonNode m = add.get(i);
                c.addMode(m.get("mode").asText(), m.get("param").asText(), init);
            }
            JsonNode remove = ops.get("remove");
            for (int i = 0; i < remove.size(); i++) {
                JsonNode m = remove.get(i);
                c.removeMode(m.get("mode").asText());
            }
            c.mode = mode;
            c.ops = ops;
            TransactionManager.getInstance().saveOnSaveQueue(c);
        }
    }

    public synchronized void updateURL(int bid, String url) {
        Channel c = getChannelForBuffer(bid);
        if (c != null) {
            c.url = url;
            TransactionManager.getInstance().saveOnSaveQueue(c);
        }
    }

    public synchronized void updateTimestamp(int bid, long timestamp) {
        Channel c = getChannelForBuffer(bid);
        if (c != null) {
            c.timestamp = timestamp;
            TransactionManager.getInstance().saveOnSaveQueue(c);
        }
    }

    public synchronized Channel getChannelForBuffer(int bid) {
        return channels.get(bid);
    }

    public synchronized ArrayList<Channel> getChannels() {
        ArrayList<Channel> list = new ArrayList<>();
        for (int i = 0; i < channels.size(); i++) {
            Channel c = channels.valueAt(i);
            list.add(c);
        }
        return list;
    }

    public synchronized void invalidate() {
        for (int i = 0; i < channels.size(); i++) {
            Channel c = channels.valueAt(i);
            c.valid = 0;
        }
    }

    public synchronized void purgeInvalidChannels() {
        ArrayList<Channel> channelsToRemove = new ArrayList<>();
        for (int i = 0; i < channels.size(); i++) {
            Channel c = channels.valueAt(i);
            if (c.valid == 0)
                channelsToRemove.add(c);
        }
        for (Channel c : channelsToRemove) {
            android.util.Log.e("IRCCloud", "Removing invalid channel: " + c.name);
            UsersList.getInstance().deleteUsersForBuffer(c.bid);
            channels.remove(c.bid);
            c.delete();
        }
    }
}
