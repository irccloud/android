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

import android.annotation.SuppressLint;
import android.text.Spanned;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.Ignore;
import com.irccloud.android.R;
import com.irccloud.android.fragment.MessageViewFragment;

@SuppressLint("UseSparseArrays")
public class EventsDataSource {

	public class Event {
		public int cid;
        public int bid;
        public long eid;
        public String timestamp;
        public String type;
        public String msg;
        public String hostmask;
        public String from;
        public String from_mode;
        public String nick;
		public String old_nick;
        public String server;
        public String diff;
        public String html;
        public String chan;
        public boolean highlight;
        public boolean self;
        public boolean to_chan;
        public boolean to_buffer;
        public int color;
        public int bg_color;
        public JsonObject ops;
        public long group_eid;
        public int row_type;
        public String group_msg;
        public boolean linkify;
        public String target_mode;
        public int reqid;
        public boolean pending;
        public boolean failed;
        public String command;
        public int day;
        public Spanned formatted;

        public String toString() {
            return "{"+
                    "cid: " + cid +
                    " bid: " + bid +
                    " eid: " + eid +
                    " type: " + type +
                    " timestamp: " + timestamp +
                    " from: " + from +
                    " msg: " + msg +
                    " html: " + html +
                    " group_eid: " + group_eid +
                    " group_msg: " + group_msg +
                    " pending: " + pending +
                    "}";
        }
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
			highest_eid = -1;
		}
	}
	
	public void addEvent(Event event) {
		synchronized(events) {
			if(!events.containsKey(event.bid))
				events.put(event.bid, new TreeMap<Long,Event>());
			
			events.get(event.bid).put(event.eid, event);
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
            e.chan = event.getString("chan");
			if(event.has("newnick"))
				e.nick = event.getString("newnick");
			else if(event.has("nick"))
				e.nick = event.getString("nick");
			else
				e.nick = null;
			e.old_nick = event.getString("oldnick");
			e.server = event.getString("server");
			e.diff = event.getString("diff");
			e.highlight = event.getBoolean("highlight");
			e.self = event.getBoolean("self");
			e.to_chan = event.getBoolean("to_chan");
            e.to_buffer = event.getBoolean("to_buffer");
			e.ops = event.getJsonObject("ops");
			e.color = R.color.row_message_label;
	    	e.bg_color = R.color.message_bg;
	    	e.row_type = 0;
	    	e.html = null;
	    	e.group_msg = null;
	    	e.linkify = true;
	    	e.target_mode = null;
	    	e.pending = false;
            e.failed = false;
            e.command = null;
            e.day = -1;

	    	if(event.has("reqid"))
	    		e.reqid = event.getInt("reqid");
	    	else
	    		e.reqid = -1;

			if(e.from != null)
				e.from = TextUtils.htmlEncode(e.from);
			
			if(e.msg != null)
				e.msg = TextUtils.htmlEncode(e.msg);
			
			if(e.type.equals("socket_closed")) {
				e.from = "";
				e.row_type = MessageViewFragment.ROW_SOCKETCLOSED;
				e.color = R.color.timestamp;
                e.linkify = false;
				if(event.has("pool_lost"))
					e.msg = "Connection pool lost";
				else if(event.has("server_ping_timeout"))
					e.msg = "Server PING timed out";
				else if(event.has("reason") && event.getString("reason").length() > 0)
					e.msg = "Connection lost: " + event.getString("reason");
				else if(event.has("abnormal"))
					e.msg = "Connection closed unexpectedly";
				else
					e.msg = "";
			} else if(e.type.equals("user_channel_mode")) {
				e.target_mode = event.getString("newmode");
                e.chan = event.getString("channel");
			} else if(e.type.equals("buffer_me_msg")) {
				e.nick = e.from;
	    		e.from = "";
	    	} else if(e.type.equals("too_fast") || e.type.equals("sasl_fail") || e.type.equals("sasl_too_long") || e.type.equals("sasl_aborted") || e.type.equals("sasl_already")) {
	    		e.from = "";
	    		e.bg_color = R.color.error;
	    	} else if(e.type.equals("no_bots")) {
	    		e.from = "";
	    		e.bg_color = R.color.error;
	    	} else if(e.type.equals("nickname_in_use")) {
	    		e.from = event.getString("nick");
	    		e.msg = "is already in use";
	    		e.bg_color = R.color.error;
	    	} else if(e.type.equals("unhandled_line") || e.type.equals("unparsed_line")) {
	    		e.from = "";
	    		e.msg = "";
	    		if(event.has("command"))
	    			e.msg = event.getString("command") + " ";
	    		if(event.has("raw"))
	    			e.msg += event.getString("raw");
	    		else
	    			e.msg += event.getString("msg");
	    		e.bg_color = R.color.error;
	    	} else if(e.type.equals("connecting_cancelled")) {
	    		e.from = "";
	    		e.msg = "Cancelled";
	    		e.bg_color = R.color.error;
	    	} else if(e.type.equals("msg_services")) {
	    		e.from = "";
	    		e.bg_color = R.color.error;
	    	} else if(e.type.equals("connecting_failed")) {
				e.row_type = MessageViewFragment.ROW_SOCKETCLOSED;
				e.color = R.color.timestamp;
	    		e.from = "";
                e.linkify = false;
                String reason = event.getString("reason");
                if(reason != null) {
                    if(reason.equalsIgnoreCase("pool_lost")) {
                        reason = "Connection pool failed";
                    } else if(reason.equalsIgnoreCase("no_pool")) {
                        reason = "No available connection pools";
                    } else if(reason.equalsIgnoreCase("enetdown")) {
                        reason = "Network down";
                    } else if(reason.equalsIgnoreCase("etimedout") || reason.equalsIgnoreCase("timeout")) {
                        reason = "Timed out";
                    } else if(reason.equalsIgnoreCase("ehostunreach")) {
                        reason = "Host unreachable";
                    } else if(reason.equalsIgnoreCase("econnrefused")) {
                        reason = "Connection refused";
                    } else if(reason.equalsIgnoreCase("nxdomain")) {
                        reason = "Invalid hostname";
                    } else if(reason.equalsIgnoreCase("server_ping_timeout")) {
                        reason = "PING timeout";
                    } else if(reason.equalsIgnoreCase("ssl_certificate_error")) {
                        reason = "SSL certificate error";
                    } else if(reason.equalsIgnoreCase("ssl_error")) {
                        reason = "SSL error";
                    } else if(reason.equalsIgnoreCase("crash")) {
                        reason = "Connection crashed";
                    }
                    e.msg = "Failed to connect: " + reason;
                } else {
                    e.msg = "Failed to connect.";
                }
	    	} else if(e.type.equals("quit_server")) {
	    		e.from = "";
	    		e.msg = "⇐ You disconnected";
	    		e.color = R.color.timestamp;
	    	} else if(e.type.equals("self_details")) {
	    		e.from = "";
	    		e.msg = "Your hostmask: <b>" + event.getString("usermask") + "</b>";
	    		e.bg_color = R.color.status_bg;
	    		e.linkify = false;
	    	} else if(e.type.equals("myinfo")) {
	    		e.from = "";
	    		e.msg = "Host: " + event.getString("server") + "\n";
	    		e.msg += "IRCd: " + event.getString("version") + "\n";
	    		e.msg += "User modes: " + event.getString("user_modes") + "\n";
	    		e.msg += "Channel modes: " + event.getString("channel_modes") + "\n";
	    		e.msg = TextUtils.htmlEncode(e.msg);
	    		e.bg_color = R.color.status_bg;
	    		e.linkify = false;
	    	} else if(e.type.equals("wait")) {
	    		e.from = "";
	    		e.bg_color = R.color.status_bg;
	    	} else if(e.type.equals("user_mode")) {
	    		e.from = "";
	    		e.msg = "Your user mode is: <b>+" + event.getString("newmode") + "</b>";
	    		e.bg_color = R.color.status_bg;
	    	} else if(e.type.equals("your_unique_id")) {
	    		e.from = "";
	    		e.msg = "Your unique ID is: <b>" + event.getString("unique_id") + "</b>";
	    		e.bg_color = R.color.status_bg;
	    	} else if(e.type.startsWith("stats")) {
	    		e.from = "";
	    		if(event.has("parts") && event.getString("parts").length() > 0)
	    			e.msg = event.getString("parts") + ": " + e.msg;
	    		e.bg_color = R.color.status_bg;
	    		e.linkify = false;
	    	} else if(e.type.equals("endofstats")) {
	    		e.from = "";
	    		e.msg = event.getString("parts") + ": " + e.msg;
	    		e.bg_color = R.color.status_bg;
	    	} else if(e.type.equals("kill")) {
	    		e.from = "";
	    		e.msg = "You were killed";
	    		if(event.has("from"))
	    			e.msg += " by " + event.getString("from");
	    		if(event.has("killer_hostmask"))
	    			e.msg += " (" + event.getString("killer_hostmask") + ")";
	    		if(event.has("reason"))
	    			e.msg += ": " + TextUtils.htmlEncode(event.getString("reason"));
	    		e.bg_color = R.color.status_bg;
	    		e.linkify = false;
	    	} else if(e.type.equals("banned")) {
	    		e.from = "";
	    		e.msg = "You were banned";
	    		if(event.has("server"))
	    			e.msg += " from " + event.getString("server");
	    		if(event.has("reason"))
	    			e.msg += ": " + TextUtils.htmlEncode(event.getString("reason"));
	    		e.bg_color = R.color.status_bg;
	    		e.linkify = false;
	    	} else if(e.type.equals("channel_topic")) {
	    		e.from = event.getString("author");
	    		e.msg = "set the topic: " + TextUtils.htmlEncode(event.getString("topic"));
	    		e.bg_color = R.color.status_bg;
	    	} else if(e.type.equals("channel_mode")) {
	    		e.nick = e.from;
	    		e.from = "";
	    		e.msg = "Channel mode set to: <b>" + event.getString("diff") + "</b>";
	    		e.bg_color = R.color.status_bg;
                e.linkify = false;
	    	} else if(e.type.equals("channel_mode_is")) {
	    		e.from = "";
	    		if(event.getString("diff") != null && event.getString("diff").length() > 0)
	    			e.msg = "Channel mode is: <b>" + event.getString("diff") + "</b>";
	    		else
	    			e.msg = "No channel mode";
	    		e.bg_color = R.color.status_bg;
                e.linkify = false;
	    	} else if(e.type.equals("kicked_channel") || e.type.equals("you_kicked_channel")) {
	    		e.from = "";
	    		e.from_mode = null;
	    		e.old_nick = event.getString("nick");
	    		e.nick = event.getString("kicker");
	    		e.hostmask = event.getString("kicker_hostmask");
	    		e.color = R.color.timestamp;
	    		e.linkify = false;
	    	} else if(e.type.equals("channel_mode_list_change")) {
	    		boolean unknown = true;
	    		JsonObject ops = event.getJsonObject("ops");
	    		if(ops != null) {
	    			JsonArray add = ops.getAsJsonArray("add");
	    			if(add != null && add.size() > 0) {
	    				JsonObject op = add.get(0).getAsJsonObject();
	    				if(op.get("mode").getAsString().equalsIgnoreCase("b")) {
	    					e.nick = e.from;
	    					e.from = "";
	    					e.msg = "Channel ban set for <b>" + op.get("param").getAsString() + "</b> (+b)";
	    					unknown = false;
	    				}
	    			}
	    			JsonArray remove = ops.getAsJsonArray("remove");
	    			if(remove != null && remove.size() > 0) {
	    				JsonObject op = remove.get(0).getAsJsonObject();
	    				if(op.get("mode").getAsString().equalsIgnoreCase("b")) {
	    					e.nick = e.from;
	    					e.from = "";
	    					e.msg = "Channel ban removed for <b>" + op.get("param").getAsString() + "</b> (-b)";
	    					unknown = false;
	    				}
	    			}
	    		}
	    		if(unknown) {
	    			e.nick = e.from;
	    			e.from = "";
	    			e.msg = "Channel mode set to: <b>" + event.getString("diff") + "</b>";
	    		}
	    		e.bg_color = R.color.status_bg;
	    		e.linkify = false;
	    	} else if(e.type.equals("motd_response") || e.type.equals("server_motd")) {
	    		JsonArray lines = event.getJsonArray("lines");
    			e.from = "";
	    		if(lines != null) {
	    			StringBuilder builder = new StringBuilder("<pre>");
	    			if(event.has("start"))
	    				builder.append(event.getString("start") + "<br/>");
	    			for(int i = 0; i < lines.size(); i++) {
	    				builder.append(TextUtils.htmlEncode(lines.get(i).getAsString()).replace("  ", " &nbsp;") + "<br/>");
	    			}
	    			builder.append("</pre>");
	    			e.msg = builder.toString();
	    		}
	    		e.bg_color = R.color.self;
            } else if(e.type.equals("server_nomotd")) {
                e.bg_color = R.color.status_bg;
                e.linkify = false;
                e.from = "";
            } else if(e.type.equals("helptlr")) {
                e.bg_color = R.color.status_bg;
                e.msg = "<pre>" + e.msg + "</pre>";
                e.from = "";
	    	} else if(e.type.equals("notice")) {
                e.chan = event.getString("target");
   	    		e.msg = "<pre>" + e.msg.replace("  ", " &nbsp;") + "</pre>";
	    		e.bg_color = R.color.notice;
	    	} else if(e.type.toLowerCase().startsWith("hidden_host_set")) {
	    		e.bg_color = R.color.status_bg;
	    		e.linkify = false;
	    		e.from = "";
	    		e.msg = "<b>" + event.getString("hidden_host") + "</b> " + e.msg;
	    	} else if(e.type.toLowerCase().startsWith("server_") || e.type.equals("logged_in_as") || e.type.equals("btn_metadata_set") || e.type.equals("sasl_success") || e.type.equals("you_are_operator")) {
	    		e.bg_color = R.color.status_bg;
	    		e.linkify = false;
            } else if(e.type.startsWith("cap_")) {
                e.from = "CAP";
                e.bg_color = R.color.status_bg;
                e.linkify = false;
                if(e.type.equals("cap_ls"))
                    e.msg = "Server supports: ";
                else if(e.type.equals("cap_req"))
                    e.msg = "Requesting: ";
                else if(e.type.equals("cap_ack"))
                    e.msg = "Acknowledged: ";
                JsonArray caps = event.getJsonArray("caps");
                for(int i = 0; i < caps.size(); i++) {
                    if(i > 0)
                        e.msg += " | ";
                    e.msg += caps.get(i).getAsString();
                }
	    	} else if(e.type.equals("inviting_to_channel")) {
	    		e.from = "";
	    		e.msg = "You invited " + event.getString("recipient") + " to join " + event.getString("channel");
	    		e.bg_color = R.color.notice;
	    	} else if(e.type.equals("channel_invite")) {
	    		e.msg = "<pre>Invite to join " + event.getString("channel") + "</pre>";
	    		e.old_nick = event.getString("channel");
                e.bg_color = R.color.notice;
	    	} else if(e.type.equals("callerid")) {
	    		e.from = e.nick;
	    		e.msg = "<pre>" + e.msg + "</pre>";
	    		e.highlight = true;
	    		e.linkify = false;
	    		e.hostmask = event.getString("usermask");
	    	} else if(e.type.equals("target_callerid")) {
	    		e.from = event.getString("target_nick");
	    		e.msg = "<pre>" + e.msg + "</pre>";
	    		e.bg_color = R.color.error;
	    	} else if(e.type.equals("target_notified")) {
	    		e.from = event.getString("target_nick");
	    		e.msg = "<pre>" + e.msg + "</pre>";
	    		e.bg_color = R.color.error;
	    	} else if(e.type.equals("link_channel")) {
	    		e.from = "";
	    		e.msg = "<pre>You tried to join " + event.getString("invalid_chan") + " but were forwarded to " + event.getString("valid_chan") + "</pre>";
	    		e.bg_color = R.color.error;
	    	}
	    	
	    	if(event.has("value") && !event.type().startsWith("cap_")) {
	    		e.msg = event.getString("value") + " " + e.msg;
	    	}

	    	if(e.highlight)
	    		e.bg_color = R.color.highlight;
	    	
	    	if(e.self)
	    		e.bg_color = R.color.self;
			
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

	public void pruneEvents(int bid, long min_eid) {
		synchronized(events) {
			if(events.containsKey(bid)) {
				ArrayList<Event> eventsToDelete = new ArrayList<Event>();
				for(Event e : events.get(bid).values()) {
					if(e.eid < min_eid)
						eventsToDelete.add(e);
					else
						break;
				}
				for(Event e : eventsToDelete) {
					events.get(bid).remove(e.eid);
				}
			}
		}
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
			return false;
		}
		if(e.self)
			return false;
		String type = e.type;
		if(type == null) {
			return false;
		}

        Ignore ignore = new Ignore();
        ServersDataSource.Server s = ServersDataSource.getInstance().getServer(e.cid);
        if(s != null) {
            ignore.setIgnores(s.ignores);
            String from = e.from;
            if(from == null || from.length() == 0)
                from = e.nick;
            if(ignore.match(from + "!" + e.hostmask))
                return false;
        }

		if (type.equals("notice") || type.equalsIgnoreCase("channel_invite") ) {
            // Notices sent from the server (with no nick sender) aren't important
            // e.g. *** Looking up your hostname...
			if (e.from == null || e.from.length() == 0)
                return false;

            // Notices and invites sent to a buffer shouldn't notify in the server buffer
            if(buffer_type.equalsIgnoreCase("console") && (e.to_chan || e.to_buffer))
                return false;
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

    public synchronized void clearCacheForBuffer(int bid) {
        synchronized(events) {
            if(events.containsKey(bid)) {
                Iterator<Event> i = events.get(bid).values().iterator();
                while(i.hasNext()) {
                    Event e = i.next();
                    e.timestamp = null;
                    e.html = null;
                    e.formatted = null;
                }
            }
        }
    }
}
