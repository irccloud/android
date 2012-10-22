package com.irccloud.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class CollapsedEventsList {
	public static final int TYPE_JOIN = 0;
	public static final int TYPE_PART = 1;
	public static final int TYPE_QUIT = 2;
	public static final int TYPE_POPIN = 3;
	public static final int TYPE_POPOUT = 4;
	public static final int TYPE_NICKCHANGE = 5;
	
	public class CollapsedEvent {
		int type;
		String nick;
		String old_nick;
		String hostmask;
		String msg;
		
		public String toString() {
			return "{type: " + type + ", nick: " + nick + ", old_nick: " + old_nick + ", hostmask: " + hostmask + ", msg: " + msg + "}";
		}
	}
	
	public class comparator implements Comparator<CollapsedEvent> {
		public int compare(CollapsedEvent e1, CollapsedEvent e2) {
			if(e1.type == e2.type)
				return e2.nick.compareToIgnoreCase(e1.nick);
			else if(e1.type > e2.type)
				return 1;
			else
				return -1;
		}
	}
	
	private ArrayList<CollapsedEvent> data = new ArrayList<CollapsedEvent>();
	
	public void addEvent(int type, String nick, String old_nick, String hostmask, String msg) {
		//Log.d("IRCCloud", "+++ Before: " + data.toString());
		CollapsedEvent e = null;
		
		if(type < TYPE_NICKCHANGE) {
			if(old_nick != null) {
				e = findJoinPartQuit(old_nick);
				if(e != null)
					e.nick = nick;
			}
			
			if(e == null)
				e = findJoinPartQuit(nick);
			
			if(e == null) {
				e = new CollapsedEvent();
				e.type = type;
				e.nick = nick;
				e.old_nick = old_nick;
				e.hostmask = hostmask;
				e.msg = msg;
				data.add(e);
			} else {
				if(type == TYPE_JOIN)
					e.type = TYPE_POPOUT;
				else
					e.type = TYPE_POPIN;
			}
		} else {
			if(type == TYPE_NICKCHANGE) {
				for(CollapsedEvent e1 : data) {
					if(e1.type == TYPE_NICKCHANGE && e1.nick.equalsIgnoreCase(old_nick)) {
						if(e1.old_nick.equalsIgnoreCase(nick))
							data.remove(e1);
						else
							e1.nick = nick;
						//Log.d("IRCCloud", "--- After: " + data.toString());
						return;
					}
					if((e1.type == TYPE_JOIN || e1.type == TYPE_POPOUT) && e1.nick.equalsIgnoreCase(old_nick)) {
						e1.old_nick = old_nick;
						e1.nick = nick;
						return;
					}
				}
				e = new CollapsedEvent();
				e.type = type;
				e.nick = nick;
				e.old_nick = old_nick;
				e.hostmask = hostmask;
				e.msg = msg;
				data.add(e);
			} else {
				e = new CollapsedEvent();
				e.type = type;
				e.nick = nick;
				e.old_nick = old_nick;
				e.hostmask = hostmask;
				e.msg = msg;
				data.add(e);
			}
		}
		//Log.d("IRCCloud", "--- After: " + data.toString());
	}
	
	public CollapsedEvent findJoinPartQuit(String nick) {
		for(CollapsedEvent e : data) {
			if(e.type < TYPE_NICKCHANGE && e.nick.equalsIgnoreCase(nick))
				return e;
		}
		return null;
	}
	
	public String getCollapsedMessage() {
		String message = "";

		if(data.size() == 0)
			return null;
		
		if(data.size() == 1) {
			CollapsedEvent e = data.get(0);
			switch(e.type) {
			case TYPE_JOIN:
	    		message = "→ <b>" + e.nick + "</b>";
	    		if(e.old_nick != null)
	    			message += " (was " + e.old_nick + ")";
	    		message += " joined (" + e.hostmask + ")";
				break;
			case TYPE_PART:
	    		message = "← <b>" + e.nick + "</b>";
	    		message += " left (" + e.hostmask + ")";
				break;
			case TYPE_QUIT:
	    		message = "⇐ <b>" + e.nick + "</b>";
	    		if(e.hostmask != null)
	    			message += " quit (" + e.hostmask + ") " + e.msg;
	    		else
	    			message += " quit: " + e.msg;
				break;
			case TYPE_NICKCHANGE:
	    		message = e.old_nick + " → <b>" + e.nick + "</b>";
				break;
			case TYPE_POPIN:
	    		message = "↔ <b>" + e.nick + "</b>";
	    		if(e.old_nick != null)
	    			message += " (was " + e.old_nick + ")";
	    		message += " popped in";
	    		break;
			case TYPE_POPOUT:
	    		message = "↔ <b>" + e.nick + "</b>";
	    		if(e.old_nick != null)
	    			message += " (was " + e.old_nick + ")";
	    		message += " nipped out";
	    		break;
			}
		} else {
			Collections.sort(data, new comparator());
			Iterator<CollapsedEvent> i = data.iterator();
			CollapsedEvent last = null;
			CollapsedEvent next = i.next();
			CollapsedEvent e;
			while(next != null) {
				e = next;
				
				if(i.hasNext())
					next = i.next();
				else
					next = null;
				
				if(message.length() > 0 && e.type < TYPE_NICKCHANGE && ((next == null || next.type != e.type) && last != null && last.type == e.type))
					message += "and ";
				
				if(last == null || last.type != e.type) {
					switch(e.type) {
					case TYPE_JOIN:
						message += "→ ";
						break;
					case TYPE_PART:
						message += "← ";
						break;
					case TYPE_QUIT:
						message += "⇐ ";
						break;
					case TYPE_NICKCHANGE:
						if(message.length() > 0)
							message += "• ";
						break;
					case TYPE_POPIN:
					case TYPE_POPOUT:
						message += "↔ ";
						break;
					}
				}

				if(e.type == TYPE_NICKCHANGE)
					message += e.old_nick + " → <b>" + e.nick + "</b>";
				else if(e.old_nick != null)
					message += "<b>" + e.nick + "</b> (was " + e.old_nick +")";
				else
					message += "<b>" + e.nick + "</b>";
				
				if(next == null || next.type != e.type) {
					switch(e.type) {
					case TYPE_JOIN:
						message += " joined";
						break;
					case TYPE_PART:
						message += " left";
						break;
					case TYPE_QUIT:
						message += " quit";
						break;
					case TYPE_POPIN:
						message += " popped in";
						break;
					case TYPE_POPOUT:
						message += " nipped out";
						break;
					}
				}

				if(next != null && next.type == e.type)
					message += ", ";
				else if(next != null)
					message += " ";
				
				last = e;
			}
		}
		return message;
	}
	
	public void clear() {
		data.clear();
	}
}
