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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class BuffersDataSource {
	public class Buffer {
        public int bid;
        public int cid;
        public long min_eid;
        public long last_seen_eid;
        public String name;
        public String type;
        public int archived;
        public int deferred;
        public int timeout;
        public String away_msg;
        public String draft;
        public String chan_types;
        public int valid;
        public boolean scrolledUp;
        public int scrollPosition;
        public int scrollPositionOffset;

        public String normalizedName() {
            if(chan_types == null || chan_types.length() < 2) {
                ServersDataSource.Server s = ServersDataSource.getInstance().getServer(cid);
                if(s != null && s.CHANTYPES != null && s.CHANTYPES.length() > 0)
                    chan_types = s.CHANTYPES;
                else
                    chan_types = "#";
            }
            return name.replaceAll("^["+chan_types+"]+","");
        }
	}

	public class comparator implements Comparator<Buffer> {
	    public int compare(Buffer b1, Buffer b2) {
	    	int joined1 = 1, joined2 = 1;
	    	ChannelsDataSource.Channel c = ChannelsDataSource.getInstance().getChannelForBuffer(b1.bid);
			if(c == null)
				joined1 = 0;
	    	c = ChannelsDataSource.getInstance().getChannelForBuffer(b2.bid);
			if(c == null)
				joined2 = 0;
	    	if(b1.type.equals("conversation") && b2.type.equals("channel"))
	    		return 1;
	    	else if(b1.type.equals("channel") && b2.type.equals("conversation"))
	    		return -1;
	    	else if(joined1 != joined2)
	    		return joined2 - joined1;
	    	else {
	    		return b1.normalizedName().compareToIgnoreCase(b2.normalizedName());
            }
	    }
	}
	
	private ArrayList<Buffer> buffers;
	private SparseArray<Buffer> buffers_indexed;

	private static BuffersDataSource instance = null;
    private boolean dirty = true;
	
	public synchronized static BuffersDataSource getInstance() {
		if(instance == null)
			instance = new BuffersDataSource();
		return instance;
	}

	public BuffersDataSource() {
		buffers = new ArrayList<Buffer>();
        buffers_indexed = new SparseArray<Buffer>();
	}

	public void clear() {
		buffers.clear();
        buffers_indexed.clear();
	}
	
	public int count() {
		return buffers.size();
	}
	
	public int firstBid() {
		if(buffers_indexed.size() > 0)
			return buffers_indexed.valueAt(0).bid;
		else
			return -1;
	}
	
	public synchronized Buffer createBuffer(int bid, int cid, long min_eid, long last_seen_eid, String name, String type, int archived, int deferred, int timeout) {
		Buffer b = getBuffer(bid);
		if(b == null) {
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
		return b;
	}

	public synchronized void updateLastSeenEid(int bid, long last_seen_eid) {
		Buffer b = getBuffer(bid);
		if(b != null && b.last_seen_eid < last_seen_eid)
			b.last_seen_eid = last_seen_eid;
	}
	
	public synchronized void updateArchived(int bid, int archived) {
		Buffer b = getBuffer(bid);
		if(b != null)
			b.archived = archived;
        dirty = true;
	}
	
	public synchronized void updateTimeout(int bid, int timeout) {
		Buffer b = getBuffer(bid);
		if(b != null)
			b.timeout = timeout;
	}
	
	public synchronized void updateName(int bid, String name) {
		Buffer b = getBuffer(bid);
		if(b != null)
			b.name = name;
        dirty = true;
	}
	
	public synchronized void updateAway(int cid, String nick, String away_msg) {
		Buffer b = getBufferByName(cid, nick);
		if(b != null) {
			b.away_msg = away_msg;
		}
	}

    public synchronized void updateDraft(int bid, String draft) {
        Buffer b = getBuffer(bid);
        if(b != null) {
            b.draft = draft;
        }
    }

	public synchronized void deleteAllDataForBuffer(int bid) {
		Buffer b = getBuffer(bid);
		if(b != null) {
			if(b.type.equalsIgnoreCase("channel")) {
				ChannelsDataSource.getInstance().deleteChannel(bid);
				UsersDataSource.getInstance().deleteUsersForBuffer(b.bid);
			}
			EventsDataSource.getInstance().deleteEventsForBuffer(bid);
		}
		buffers.remove(b);
        buffers_indexed.remove(bid);
        dirty = true;
	}

	public synchronized Buffer getBuffer(int bid) {
        return buffers_indexed.get(bid);
	}
	
	public synchronized Buffer getBufferByName(int cid, String name) {
        for(Buffer b : buffers) {
			if(b.cid == cid && b.name.equalsIgnoreCase(name))
				return b;
		}
		return null;
	}
	
	public synchronized ArrayList<Buffer> getBuffersForServer(int cid) {
		ArrayList<Buffer> list = new ArrayList<Buffer>();
        if(dirty) {
            Collections.sort(buffers, new comparator());
            dirty = false;
        }
        for(Buffer b : buffers) {
			if(b.cid == cid)
				list.add(b);
		}
		return list;
	}
	
	public synchronized ArrayList<Buffer> getBuffers() {
		ArrayList<Buffer> list = new ArrayList<Buffer>();
        for(Buffer b : buffers) {
            list.add(b);
		}
		return list;
	}

    public synchronized void invalidate() {
        for(Buffer b : buffers) {
            b.valid = 0;
        }
    }

    public synchronized void purgeInvalidBIDs() {
        ArrayList<Buffer> buffersToRemove = new ArrayList<Buffer>();
        Iterator<Buffer> i = buffers.iterator();
        while(i.hasNext()) {
            Buffer b = i.next();
            if(b.valid == 0)
                buffersToRemove.add(b);
        }
        i = buffersToRemove.iterator();
        while(i.hasNext()) {
            Buffer b = i.next();
            EventsDataSource.getInstance().deleteEventsForBuffer(b.bid);
            ChannelsDataSource.getInstance().deleteChannel(b.bid);
            UsersDataSource.getInstance().deleteUsersForBuffer(b.bid);
            buffers.remove(b);
            buffers_indexed.remove(b.bid);
            if(b.type.equalsIgnoreCase("console")) {
                ServersDataSource.getInstance().deleteServer(b.cid);
            }
        }
        dirty = true;
    }
}
