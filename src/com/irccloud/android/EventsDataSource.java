package com.irccloud.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import org.json.JSONObject;

public class EventsDataSource {
	public class Event {
		long eid;
		long bid;
		int cid;
		String type;
		int highlight;
		JSONObject event;
	}

	public class comparator implements Comparator<Event> {
		public int compare(Event e1, Event e2) {
			return (int)(e1.eid - e2.eid);
		}
	}
	
	private ArrayList<Event> events;
	
	private static EventsDataSource instance = null;
	
	public static EventsDataSource getInstance() {
		if(instance == null)
			instance = new EventsDataSource();
		return instance;
	}

	public EventsDataSource() {
		events = new ArrayList<Event>();
	}

	public void clear() {
		events.clear();
	}
	
	public Event createEvent(long eid, int bid, int cid, String type, int highlight, JSONObject event) {
		Event e = new Event();
		e.eid = eid;
		e.bid = bid;
		e.cid = cid;
		e.type = type;
		e.highlight = highlight;
		e.event = event;
		events.add(e);
		return e;
	}

	public Event getEvent(long eid, int bid) {
		Iterator<Event> i = events.iterator();
		while(i.hasNext()) {
			Event e = i.next();
			if(e.eid == eid && e.bid == bid)
				return e;
		}
		return null;
	}
	
	public void deleteEvent(long eid, int bid) {
		Event e = getEvent(eid, bid);
		if(e != null)
			events.remove(e);
	}

	public void deleteEventsForServer(int cid) {
		ArrayList<Event> eventsToRemove = new ArrayList<Event>();
		
		Iterator<Event> i = events.iterator();
		while(i.hasNext()) {
			Event e = i.next();
			if(e.cid == cid)
				eventsToRemove.add(e);
		}
		
		i=eventsToRemove.iterator();
		while(i.hasNext()) {
			Event e = i.next();
			events.remove(e);
		}
	}

	public void deleteEventsForBuffer(int bid) {
		ArrayList<Event> eventsToRemove = new ArrayList<Event>();
		
		Iterator<Event> i = events.iterator();
		while(i.hasNext()) {
			Event e = i.next();
			if(e.bid == bid)
				eventsToRemove.add(e);
		}
		
		i=eventsToRemove.iterator();
		while(i.hasNext()) {
			Event e = i.next();
			events.remove(e);
		}
	}

	public ArrayList<Event> getEventsForBuffer(int bid) {
		ArrayList<Event> list = new ArrayList<Event>();
		Iterator<Event> i = events.iterator();
		while(i.hasNext()) {
			Event e = i.next();
			if(e.bid == bid)
				list.add(e);
		}
		Collections.sort(list, new comparator());
		return list;
	}

	public int getUnreadCountForBuffer(long bid, long last_seen_eid) {
		int count = 0;
		Iterator<Event> i = events.iterator();
		while(i.hasNext()) {
			Event e = i.next();
			if(e.bid == bid && e.eid > last_seen_eid && (e.type.equals("buffer_msg") || e.type.equals("buffer_me_msg") ||e.type.equals("notice")))
				count++;
		}
		return count;
	}

	public int getHighlightCountForBuffer(long bid, long last_seen_eid) {
		int count = 0;
		Iterator<Event> i = events.iterator();
		while(i.hasNext()) {
			Event e = i.next();
			if(e.bid == bid && e.eid > last_seen_eid && e.highlight == 1)
				count++;
		}
		return count;
	}
}
