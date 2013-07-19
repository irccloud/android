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

import java.util.ArrayList;
import java.util.Iterator;

public class ChannelsDataSource {
	public class Channel {
		int cid;
		long bid;
		String name;
		String topic_text;
		long topic_time;
		String topic_author;
		String type;
		String mode;
		long timestamp;
		String url;
	}
	
	private ArrayList<Channel> channels;

	private static ChannelsDataSource instance = null;
	
	public static ChannelsDataSource getInstance() {
		if(instance == null)
			instance = new ChannelsDataSource();
		return instance;
	}

	public ChannelsDataSource() {
		channels = new ArrayList<Channel>();
	}

	public synchronized void clear() {
		channels.clear();
	}
	
	public synchronized Channel createChannel(int cid, long bid, String name, String topic_text, long topic_time, String topic_author, String type, String mode, long timestamp) {
		Channel c = getChannelForBuffer(bid);
		if(c == null) {
			c = new Channel();
			channels.add(c);
		}
		c.cid = cid;
		c.bid = bid;
		c.name = name;
		c.topic_author = topic_author;
		c.topic_text = topic_text;
		c.topic_time = topic_time;
		c.type = type;
		c.mode = mode;
		c.timestamp = timestamp;
		return c;
	}

	public synchronized void deleteChannel(long bid) {
		Channel c = getChannelForBuffer(bid);
		if(c != null)
			channels.remove(c);
	}

	public synchronized void updateTopic(long bid, String topic_text, long topic_time, String topic_author) {
		Channel c = getChannelForBuffer(bid);
		if(c != null) {
			c.topic_text = topic_text;
			c.topic_time = topic_time;
			c.topic_author = topic_author;
		}
	}
	
	public synchronized void updateMode(long bid, String mode) {
		Channel c = getChannelForBuffer(bid);
		if(c != null) {
			c.mode = mode;
		}
	}
	
	public synchronized void updateURL(long bid, String url) {
		Channel c = getChannelForBuffer(bid);
		if(c != null) {
			c.url = url;
		}
	}
	
	public synchronized void updateTimestamp(long bid, long timestamp) {
		Channel c = getChannelForBuffer(bid);
		if(c != null) {
			c.timestamp = timestamp;
		}
	}
	
	public synchronized Channel getChannelForBuffer(long bid) {
		Iterator<Channel> i = channels.iterator();
		while(i.hasNext()) {
			Channel c = i.next();
			if(c.bid == bid)
				return c;
		}
		return null;
	}
}
