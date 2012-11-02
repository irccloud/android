package com.irccloud.android;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@SuppressLint("UseSparseArrays")
public class EventsDataSource {

	public class Event {
		int cid;
		int bid;
		long eid;
		String timestamp;
		String type;
		String msg;
		String hostmask;
		String from;
		String from_mode;
		String nick;
		String old_nick;
		String server;
		String diff;
		String html;
		boolean highlight;
		boolean self;
		boolean to_chan;
		int color;
		int bg_color;
		JsonObject ops;
		long group_eid;
		int row_type;
		String group_msg;
		boolean linkify;
		String target_mode;
	}
	
	public class comparator implements Comparator<Event> {
		public int compare(Event e1, Event e2) {
			long l1 = e1.eid, l2 = e2.eid;
			if(l1 == l2)
				return 0;
			else if(l1 > l2)
				return 1;
			else return -1;
		}
	}
	
	private HashMap<Integer,TreeMap<Long, Event>> events;
	private static EventsDataSource instance = null;
	public long highest_eid = -1;
	
	public static EventsDataSource getInstance() {
		if(instance == null)
			instance = new EventsDataSource();
		return instance;
	}

	public EventsDataSource() {
		events = new HashMap<Integer,TreeMap<Long, Event>>();
	}

	public void clear() {
		synchronized(events) {
			events.clear();
		}
	}
	
	public Event addEvent(IRCCloudJSONObject event) {
		synchronized(events) {
			if(!events.containsKey(event.bid()))
				events.put(event.bid(), new TreeMap<Long,Event>());
			
			Event e = getEvent(event.eid(), event.bid());
			if(e == null) {
				e = new Event();
				events.get(event.bid()).put(event.eid(), e);
			}
			e.cid = event.cid();
			e.bid = event.bid();
			e.eid = event.eid();
			e.type = event.type();
			e.msg = event.getString("msg");
			e.hostmask = event.getString("hostmask");
			e.from = event.getString("from");
			e.from_mode = event.getString("from_mode");
			if(event.has("newnick"))
				e.nick = event.getString("newnick");
			else
				e.nick = event.getString("nick");
			e.old_nick = event.getString("oldnick");
			e.server = event.getString("server");
			e.diff = event.getString("diff");
			e.highlight = event.getBoolean("highlight");
			e.self = event.getBoolean("self");
			e.to_chan = event.getBoolean("to_chan");
			e.ops = event.getJsonObject("ops");
			e.color = R.color.row_message_label;
	    	e.bg_color = R.color.message_bg;
	    	e.row_type = 0;
	    	e.html = null;
	    	e.group_msg = null;
	    	e.linkify = true;
	    	e.target_mode = null;

			if(e.from != null)
				e.from = TextUtils.htmlEncode(e.from);
			
			if(e.msg != null)
				e.msg = TextUtils.htmlEncode(e.msg);
			
			if(e.type.equalsIgnoreCase("socket_closed")) {
				e.from = "";
				e.row_type = MessageViewFragment.ROW_SOCKETCLOSED;
				e.color = R.color.timestamp;
				if(event.has("pool_lost"))
					e.msg = "Connection pool lost";
				else if(event.has("server_ping_timeout"))
					e.msg = "Server PING timed out";
				else if(event.has("reason") && event.getString("reason").length() > 0)
					e.msg = "Connection lost: " + event.getString("reason");
				else
					e.msg = "Connection closed unexpectedly";
			} else if(e.type.equalsIgnoreCase("user_channel_mode")) {
				e.target_mode = event.getString("newmode");
			} else if(e.type.equalsIgnoreCase("buffer_me_msg")) {
				e.nick = e.from;
	    		e.from = "";
	    	} else if(e.type.equalsIgnoreCase("too_fast")) {
	    		e.from = "";
	    		e.bg_color = R.color.error;
	    	} else if(e.type.equalsIgnoreCase("no_bots")) {
	    		e.from = "";
	    		e.bg_color = R.color.error;
	    	} else if(e.type.equalsIgnoreCase("nickname_in_use")) {
	    		e.from = event.getString("nick");
	    		e.msg = "is already in use";
	    		e.bg_color = R.color.error;
	    	} else if(e.type.equalsIgnoreCase("unhandled_line") || e.type.equalsIgnoreCase("unparsed_line")) {
	    		e.from = "";
	    		e.msg = "";
	    		if(event.has("command"))
	    			e.msg = event.getString("command") + " ";
	    		if(event.has("raw"))
	    			e.msg += event.getString("raw");
	    		else
	    			e.msg += event.getString("msg");
	    		e.bg_color = R.color.error;
	    	} else if(e.type.equalsIgnoreCase("connecting_cancelled")) {
	    		e.from = "";
	    		e.msg = "Cancelled";
	    		e.bg_color = R.color.error;
	    	} else if(e.type.equalsIgnoreCase("connecting_failed")) {
				e.row_type = MessageViewFragment.ROW_SOCKETCLOSED;
				e.color = R.color.timestamp;
	    		e.from = "";
	    		e.msg = "Failed to connect: " + event.getString("reason");
	    	} else if(e.type.equalsIgnoreCase("quit_server")) {
	    		e.from = "";
	    		e.msg = "⇐ You disconnected";
	    		e.color = R.color.timestamp;
	    	} else if(e.type.equalsIgnoreCase("self_details")) {
	    		e.from = "";
	    		e.msg = "Your hostmask: <b>" + event.getString("usermask") + "</b>";
	    		e.bg_color = R.color.status_bg;
	    		e.linkify = false;
	    	} else if(e.type.equalsIgnoreCase("myinfo")) {
	    		e.from = "";
	    		e.msg = "Host: " + event.getString("server") + "\n";
	    		e.msg += "IRCd: " + event.getString("version") + "\n";
	    		e.msg += "User modes: " + event.getString("user_modes") + "\n";
	    		e.msg += "Channel modes: " + event.getString("channel_modes") + "\n";
	    		e.bg_color = R.color.status_bg;
	    		e.linkify = false;
	    	} else if(e.type.equalsIgnoreCase("wait")) {
	    		e.from = "";
	    		e.bg_color = R.color.status_bg;
	    	} else if(e.type.equalsIgnoreCase("user_mode")) {
	    		e.from = "";
	    		e.msg = "Your user mode is: <b>" + event.getString("diff") + "</b>";
	    		e.bg_color = R.color.status_bg;
	    	} else if(e.type.equalsIgnoreCase("your_unique_id")) {
	    		e.from = "";
	    		e.msg = "Your unique ID is: <b>" + event.getString("unique_id") + "</b>";
	    		e.bg_color = R.color.status_bg;
	    	} else if(e.type.equalsIgnoreCase("kill")) {
	    		e.from = "";
	    		e.msg = "You were killed";
	    		if(event.has("from"))
	    			e.msg += " by " + event.getString("from");
	    		if(event.has("killer_hostmask"))
	    			e.msg += " (" + event.getString("killer_hostmask") + ")";
	    		if(event.has("reason"))
	    			e.msg += ": " + event.getString("reason");
	    		e.bg_color = R.color.status_bg;
	    		e.linkify = false;
	    	} else if(e.type.equalsIgnoreCase("banned")) {
	    		e.from = "";
	    		e.msg = "You were banned";
	    		if(event.has("server"))
	    			e.msg += " from " + event.getString("server");
	    		if(event.has("reason"))
	    			e.msg += ": " + event.getString("reason");
	    		e.bg_color = R.color.status_bg;
	    		e.linkify = false;
	    	} else if(e.type.equalsIgnoreCase("channel_topic")) {
	    		e.from = event.getString("author");
	    		e.msg = "set the topic: " + event.getString("topic");
	    		e.bg_color = R.color.status_bg;
	    	} else if(e.type.equalsIgnoreCase("channel_mode")) {
	    		e.nick = e.from;
	    		e.from = "";
	    		e.msg = "Channel mode set to: <b>" + event.getString("diff") + "</b>";
	    		e.bg_color = R.color.status_bg;
	    	} else if(e.type.equalsIgnoreCase("channel_mode_is")) {
	    		e.from = "";
	    		if(event.getString("diff") != null && event.getString("diff").length() > 0)
	    			e.msg = "Channel mode is: <b>" + event.getString("diff") + "</b>";
	    		else
	    			e.msg = "No channel mode";
	    		e.bg_color = R.color.status_bg;
	    	} else if(e.type.equalsIgnoreCase("kicked_channel") || e.type.equalsIgnoreCase("you_kicked_channel")) {
	    		e.from = "";
	    		e.from_mode = null;
	    		e.old_nick = event.getString("nick");
	    		e.nick = event.getString("kicker");
	    		e.hostmask = event.getString("kicker_hostmask");
	    		e.color = R.color.timestamp;
	    		e.linkify = false;
	    	} else if(e.type.equalsIgnoreCase("channel_mode_list_change")) {
	    		boolean unknown = true;
	    		JsonObject ops = event.getJsonObject("ops");
	    		if(ops != null) {
	    			JsonArray add = ops.getAsJsonArray("add");
	    			if(add != null && add.size() > 0) {
	    				JsonObject op = add.get(0).getAsJsonObject();
	    				if(op.get("mode").getAsString().equalsIgnoreCase("b")) {
	    					e.nick = e.from;
	    					e.from = "";
	    					e.msg = "Channel ban set for <b>" + op.get("param").getAsString() + "</b> (+b) by ";
	    					unknown = false;
	    				}
	    			}
	    			JsonArray remove = ops.getAsJsonArray("remove");
	    			if(remove != null && remove.size() > 0) {
	    				JsonObject op = remove.get(0).getAsJsonObject();
	    				if(op.get("mode").getAsString().equalsIgnoreCase("b")) {
	    					e.nick = e.from;
	    					e.from = "";
	    					e.msg = "Channel ban removed for <b>" + op.get("param").getAsString() + "</b> (-b) by ";
	    					unknown = false;
	    				}
	    			}
	    		}
	    		if(unknown) {
	    			e.nick = e.from;
	    			e.from = "";
	    			e.msg = "Channel mode set to: <b>" + event.getString("diff") + "</b> by ";
	    		}
	    		e.bg_color = R.color.status_bg;
	    		e.linkify = false;
	    	} else if(e.type.equalsIgnoreCase("motd_response") || e.type.equalsIgnoreCase("server_motd")) {
	    		JsonArray lines = event.getJsonArray("lines");
    			e.from = "";
	    		if(lines != null) {
	    			e.msg = "<pre>";
	    			if(event.has("start"))
	    				e.msg += event.getString("start") + "<br/>";
	    			for(int i = 0; i < lines.size(); i++) {
	    				e.msg += TextUtils.htmlEncode(lines.get(i).getAsString()).replace("  ", " &nbsp;") + "<br/>";
	    			}
	    			e.msg += "</pre>";
	    		}
	    		e.bg_color = R.color.self;
	    	} else if(e.type.equalsIgnoreCase("notice")) {
	    		e.bg_color = R.color.notice;
	    	} else if(e.type.toLowerCase().startsWith("hidden_host_set")) {
	    		e.bg_color = R.color.status_bg;
	    		e.linkify = false;
	    		e.from = "";
	    		e.msg = "<b>" + event.getString("hidden_host") + "</b> " + e.msg;
	    	} else if(e.type.toLowerCase().startsWith("server_")) {
	    		e.bg_color = R.color.status_bg;
	    		e.linkify = false;
	    	} else if(e.type.equalsIgnoreCase("inviting_to_channel")) {
	    		e.from = "";
	    		e.msg = "You invited " + event.getString("recipient") + " to join " + event.getString("channel");
	    		e.bg_color = R.color.notice;
	    	} else if(e.type.equalsIgnoreCase("channel_invite")) {
	    		e.msg = "<pre>Invite to join " + event.getString("channel") + "</pre>";
	    		e.bg_color = R.color.highlight;
	    		e.old_nick = event.getString("channel");
	    		e.highlight = true;
	    	}
	    	
	    	if(event.has("value")) {
	    		e.msg = event.getString("value") + " " + e.msg;
	    	}

	    	if(event.has("highlight") && event.getBoolean("highlight"))
	    		e.bg_color = R.color.highlight;
	    	
	    	if(event.has("self") && event.getBoolean("self"))
	    		e.bg_color = R.color.self;
			
	    	if(e.msg != null && e.msg .length() > 0)
	    		e.msg = ColorFormatter.irc_to_html(e.msg);
	    	
			if(highest_eid < event.eid())
				highest_eid = event.eid();
			
			return e;
		}
	}

	public Event getEvent(long eid, int bid) {
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

	public TreeMap<Long,Event> getEventsForBuffer(int bid) {
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
				Iterator<Event> i = events.get(bid).values().iterator();
				while(i.hasNext()) {
					Event e = i.next();
					try {
						if(e.eid > last_seen_eid && isImportant(e, buffer_type))
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

	public boolean isImportant(Event e, String buffer_type) {
		if(e == null) {
			Log.w("IRCCloud", "isImportant: NULL event");
			return false;
		}
		String type = e.type;
		if(type == null) {
			Log.w("IRCCloud", "isImportant: NULL type");
			return false;
		}
		if (type.equals("notice") && buffer_type != null && buffer_type.equals("console")) {
			if (e.server != null || e.to_chan == true) {
				return false;
			}
		}
		return (type.equals("buffer_msg") ||
				type.equals("buffer_me_msg") ||
				type.equals("notice") ||
				type.equals("channel_invite") ||
				type.equals("callerid") ||
				type.equals("wallops"));
	}
	
	public synchronized int getHighlightCountForBuffer(int bid, long last_seen_eid, String buffer_type) {
		int count = 0;
		synchronized(events) {
			if(events.containsKey(bid)) {
				Iterator<Event> i = events.get(bid).values().iterator();
				while(i.hasNext()) {
					Event e = i.next();
					try {
						if(e.eid > last_seen_eid && isImportant(e, buffer_type) && (e.highlight || buffer_type.equalsIgnoreCase("conversation")))
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
