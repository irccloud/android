package com.irccloud.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class BuffersDataSource {
	public class Buffer {
		int bid;
		int cid;
		long min_eid;
		long last_seen_eid;
		String name;
		String type;
		int archived;
		int deferred;
		String away_msg;
	}

	public class comparator implements Comparator<Buffer> {
	    public int compare(Buffer b1, Buffer b2) {
	    	return b1.name.compareToIgnoreCase(b2.name);
	    }
	}
	
	private ArrayList<Buffer> buffers;
	
	private static BuffersDataSource instance = null;
	
	public static BuffersDataSource getInstance() {
		if(instance == null)
			instance = new BuffersDataSource();
		return instance;
	}

	public BuffersDataSource() {
		buffers = new ArrayList<Buffer>();
	}

	public void clear() {
		buffers.clear();
	}
	
	public synchronized Buffer createBuffer(int bid, int cid, long min_eid, long last_seen_eid, String name, String type, int archived, int deferred) {
		Buffer b = new Buffer();
		b.bid = bid;
		b.cid = cid;
		b.min_eid = min_eid;
		b.last_seen_eid = last_seen_eid;
		b.name = name;
		b.type = type;
		b.archived = archived;
		b.deferred = deferred;
		buffers.add(b);
		return b;
	}

	public synchronized void updateLastSeenEid(int bid, long last_seen_eid) {
		Buffer b = getBuffer(bid);
		if(b != null)
			b.last_seen_eid = last_seen_eid;
	}
	
	public synchronized void updateArchived(int bid, int archived) {
		Buffer b = getBuffer(bid);
		if(b != null)
			b.archived = archived;
	}
	
	public synchronized void updateName(int bid, String name) {
		Buffer b = getBuffer(bid);
		if(b != null)
			b.name = name;
	}
	
	public synchronized void updateAway(int bid, String away_msg) {
		Buffer b = getBuffer(bid);
		if(b != null) {
			b.away_msg = away_msg;
		}
	}
	
	public synchronized void deleteBuffer(int bid) {
		Buffer b = getBuffer(bid);
		if(b != null)
			buffers.remove(b);
	}

	public synchronized void deleteAllDataForBuffer(int bid) {
		Buffer b = getBuffer(bid);
		if(b != null) {
			if(b.type.equalsIgnoreCase("channel")) {
				ChannelsDataSource.getInstance().deleteChannel(bid);
				UsersDataSource.getInstance().deleteUsersForChannel(b.cid, b.name);
			}
			EventsDataSource.getInstance().deleteEventsForBuffer(bid);
		}
		buffers.remove(b);
	}

	public synchronized Buffer getBuffer(int bid) {
		Iterator<Buffer> i = buffers.iterator();
		while(i.hasNext()) {
			Buffer b = i.next();
			if(b.bid == bid)
				return b;
		}
		return null;
	}
	
	public synchronized Buffer getBufferByName(int cid, String name) {
		Iterator<Buffer> i = buffers.iterator();
		while(i.hasNext()) {
			Buffer b = i.next();
			if(b.name.equals(name))
				return b;
		}
		return null;
	}
	
	public synchronized ArrayList<Buffer> getBuffersForServer(int cid) {
		ArrayList<Buffer> list = new ArrayList<Buffer>();
		Iterator<Buffer> i = buffers.iterator();
		while(i.hasNext()) {
			Buffer b = i.next();
			if(b.cid == cid)
				list.add(b);
		}
		Collections.sort(list, new comparator());
		return list;
	}
}
