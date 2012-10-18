package com.irccloud.androidnative;

import android.annotation.SuppressLint;
import android.util.Log;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

@SuppressLint("UseSparseArrays")
public class EventsDataSource {

	public class comparator implements Comparator<IRCCloudJSONObject> {
		public int compare(IRCCloudJSONObject e1, IRCCloudJSONObject e2) {
			long l1 = e1.eid(), l2 = e2.eid();
			if(l1 == l2)
				return 0;
			else if(l1 > l2)
				return 1;
			else return -1;
		}
	}
	
	private HashMap<Integer,TreeMap<Long, IRCCloudJSONObject>> events;
	private static EventsDataSource instance = null;
	public long highest_eid = -1;
	
	public static EventsDataSource getInstance() {
		if(instance == null)
			instance = new EventsDataSource();
		return instance;
	}

	public EventsDataSource() {
		events = new HashMap<Integer,TreeMap<Long, IRCCloudJSONObject>>();
	}

	public void clear() {
		synchronized(events) {
			events.clear();
		}
	}
	
	public void addEvent(IRCCloudJSONObject event) {
		synchronized(events) {
			if(!events.containsKey(event.bid()))
				events.put(event.bid(), new TreeMap<Long,IRCCloudJSONObject>());
			events.get(event.bid()).put(event.eid(), event);
			if(highest_eid < event.eid())
				highest_eid = event.eid();
		}
	}

	public IRCCloudJSONObject getEvent(long eid, int bid) {
		synchronized(events) {
			if(events.containsKey(bid))
				return events.get(bid).get(eid);
		}
		return null;
	}
	
	public void deleteEvent(long eid, int bid) {
		synchronized(events) {
			if(events.containsKey(bid) && events.get(bid).containsKey(eid))
				events.get(bid).remove(eid);
		}
	}

	public void deleteEventsForBuffer(int bid) {
		synchronized(events) {
			if(events.containsKey(bid))
				events.remove(bid);
		}
	}

	public TreeMap<Long,IRCCloudJSONObject> getEventsForBuffer(int bid) {
		synchronized(events) {
			if(events.containsKey(bid)) {
				return events.get(bid);
			}
		}
		return null;
	}

	public int getUnreadCountForBuffer(int bid, long last_seen_eid, String buffer_type) {
		int count = 0;
		synchronized(events) {
			if(events.containsKey(bid)) {
				Iterator<IRCCloudJSONObject> i = events.get(bid).values().iterator();
				while(i.hasNext()) {
					IRCCloudJSONObject e = i.next();
					try {
						if(e.eid() > last_seen_eid && isImportant(e, buffer_type))
							count++;
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}
		return count;
	}

	private boolean isImportant(IRCCloudJSONObject e, String buffer_type) {
		if(e == null) {
			Log.w("IRCCloud", "isImportant: NULL event");
			return false;
		}
		String type = e.type();
		if(type == null) {
			Log.w("IRCCloud", "isImportant: NULL type");
			return false;
		}
		return (type.equals("buffer_msg") || type.equals("buffer_me_msg") 
				|| (type.equals("notice") && buffer_type != null && (!buffer_type.equals("console") || !(e.has("server") && e.getString("server") != null && !e.getString("server").equals("undefined")))));
	}
	
	public synchronized int getHighlightCountForBuffer(int bid, long last_seen_eid, String buffer_type) {
		int count = 0;
		synchronized(events) {
			if(events.containsKey(bid)) {
				Iterator<IRCCloudJSONObject> i = events.get(bid).values().iterator();
				while(i.hasNext()) {
					IRCCloudJSONObject e = i.next();
					try {
						if(e.eid() > last_seen_eid && isImportant(e, buffer_type) && (e.highlight() || buffer_type.equalsIgnoreCase("conversation")))
							count++;
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}
		return count;
	}
}
