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

package com.irccloud.android;

import android.util.SparseArray;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;

public class ChannelsDataSource {
    public class Mode {
        String mode;
        String param;
    }

	public class Channel {
		int cid;
		int bid;
		String name;
		String topic_text;
		long topic_time;
		String topic_author;
		String type;
		String mode;
        ArrayList<Mode> modes;
		long timestamp;
		String url;
        int valid;
        boolean key;

        public synchronized void addMode(String mode, String param, boolean init) {
            if(!init)
                removeMode(mode);
            if(mode.equalsIgnoreCase("k"))
                key = true;
            Mode m = new Mode();
            m.mode = mode;
            m.param = param;
            modes.add(m);
        }

        public synchronized void removeMode(String mode) {
            if(mode.equalsIgnoreCase("k"))
                key = false;
            Iterator<Mode> i = modes.iterator();
            while(i.hasNext()) {
                Mode m = i.next();
                if(m.mode.equalsIgnoreCase(mode)) {
                    modes.remove(m);
                    return;
                }
            }
        }

        public synchronized boolean hasMode(String mode) {
            Iterator<Mode> i = modes.iterator();
            while(i.hasNext()) {
                Mode m = i.next();
                if(m.mode.equalsIgnoreCase(mode)) {
                    return true;
                }
            }
            return false;
        }
    }
	
	private SparseArray<Channel> channels;

	private static ChannelsDataSource instance = null;
	
	public static ChannelsDataSource getInstance() {
		if(instance == null)
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
		if(c == null) {
			c = new Channel();
			channels.put(bid, c);
		}
		c.cid = cid;
		c.bid = bid;
		c.name = name;
		c.topic_author = topic_author;
		c.topic_text = topic_text;
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
		if(c != null) {
			c.topic_text = topic_text;
			c.topic_time = topic_time;
			c.topic_author = topic_author;
		}
	}
	
	public synchronized void updateMode(int bid, String mode, JsonObject ops, boolean init) {
		Channel c = getChannelForBuffer(bid);
		if(c != null) {
            c.key = false;
            JsonArray add = ops.get("add").getAsJsonArray();
            for(int i = 0; i < add.size(); i++) {
                JsonObject m = add.get(i).getAsJsonObject();
                c.addMode(m.get("mode").getAsString(), m.get("param").getAsString(), init);
            }
            JsonArray remove = ops.get("remove").getAsJsonArray();
            for(int i = 0; i < remove.size(); i++) {
                JsonObject m = remove.get(i).getAsJsonObject();
                c.removeMode(m.get("mode").getAsString());
            }
			c.mode = mode;
		}
	}
	
	public synchronized void updateURL(int bid, String url) {
		Channel c = getChannelForBuffer(bid);
		if(c != null) {
			c.url = url;
		}
	}
	
	public synchronized void updateTimestamp(int bid, long timestamp) {
		Channel c = getChannelForBuffer(bid);
		if(c != null) {
			c.timestamp = timestamp;
		}
	}
	
	public synchronized Channel getChannelForBuffer(int bid) {
        return channels.get(bid);
	}

    public synchronized void invalidate() {
        for(int i = 0; i < channels.size(); i++) {
            Channel c = channels.valueAt(i);
            c.valid = 0;
        }
    }

    public synchronized void purgeInvalidChannels() {
        ArrayList<Channel> channelsToRemove = new ArrayList<Channel>();
        for(int i = 0; i < channels.size(); i++) {
            Channel c = channels.valueAt(i);
            if(c.valid == 0)
                channelsToRemove.add(c);
        }
        Iterator<Channel> i = channelsToRemove.iterator();
        while(i.hasNext()) {
            Channel c = i.next();
            UsersDataSource.getInstance().deleteUsersForBuffer(c.cid, c.bid);
            channels.remove(c.bid);
        }
    }
}
