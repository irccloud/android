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

import android.util.SparseArray;

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.ColorFormatter;

import java.util.ArrayList;

public class ChannelsDataSource {
    public static class Mode {
        public String mode;
        public String param;
    }

    public static class Channel {
        public int cid;
        public int bid;
        public String name;
        public String topic_text;
        public long topic_time;
        public String topic_author;
        public String type;
        public String mode;
        public ArrayList<Mode> modes;
        public long timestamp;
        public String url;
        public int valid;
        public boolean key;

        public synchronized void addMode(String mode, String param, boolean init) {
            if (!init)
                removeMode(mode);
            if (mode.equals("k"))
                key = true;
            Mode m = new Mode();
            m.mode = mode;
            m.param = param;
            modes.add(m);
        }

        public synchronized void removeMode(String mode) {
            if (mode.equals("k"))
                key = false;
            for (Mode m : modes) {
                if (m.mode.equals(mode)) {
                    modes.remove(m);
                    return;
                }
            }
        }

        public synchronized boolean hasMode(String mode) {
            for (Mode m : modes) {
                if (m.mode.equals(mode)) {
                    return true;
                }
            }
            return false;
        }

        public synchronized String paramForMode(String mode) {
            for (Mode m : modes) {
                if (m.mode.equals(mode)) {
                    return m.param;
                }
            }
            return null;
        }
    }

    private SparseArray<Channel> channels;

    private static ChannelsDataSource instance = null;

    public synchronized static ChannelsDataSource getInstance() {
        if (instance == null)
            instance = new ChannelsDataSource();
        return instance;
    }

    public ChannelsDataSource() {
        channels = new SparseArray<Channel>();
    }

    public synchronized void clear() {
        channels.clear();
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
        c.valid = 1;
        c.key = false;
        c.mode = "";
        c.modes = new ArrayList<Mode>();
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
        }
    }

    public synchronized void updateURL(int bid, String url) {
        Channel c = getChannelForBuffer(bid);
        if (c != null) {
            c.url = url;
        }
    }

    public synchronized void updateTimestamp(int bid, long timestamp) {
        Channel c = getChannelForBuffer(bid);
        if (c != null) {
            c.timestamp = timestamp;
        }
    }

    public synchronized Channel getChannelForBuffer(int bid) {
        return channels.get(bid);
    }

    public synchronized ArrayList<Channel> getChannels() {
        ArrayList<Channel> list = new ArrayList<Channel>();
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
        ArrayList<Channel> channelsToRemove = new ArrayList<Channel>();
        for (int i = 0; i < channels.size(); i++) {
            Channel c = channels.valueAt(i);
            if (c.valid == 0)
                channelsToRemove.add(c);
        }
        for (Channel c : channelsToRemove) {
            UsersDataSource.getInstance().deleteUsersForBuffer(c.bid);
            channels.remove(c.bid);
        }
    }
}
