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
import android.os.Build;
import android.text.Spanned;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.ColorFormatter;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.Ignore;
import com.irccloud.android.R;
import com.irccloud.android.fragment.MessageViewFragment;

@SuppressLint("UseSparseArrays")
public class EventsDataSource {

	public static class Event {
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
        public JsonNode ops;
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
        public String contentDescription;

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

        public synchronized boolean isImportant(String buffer_type) {
            if(self)
                return false;
            if(type == null) {
                return false;
            }

            Ignore ignore = new Ignore();
            ServersDataSource.Server s = ServersDataSource.getInstance().getServer(cid);
            if(s != null) {
                ignore.setIgnores(s.ignores);
                String from = this.from;
                if(from == null || from.length() == 0)
                    from = this.nick;
                if(ignore.match(from + "!" + hostmask))
                    return false;
            }

            if (type.equals("notice") || type.equalsIgnoreCase("channel_invite") ) {
                // Notices sent from the server (with no nick sender) aren't important
                // e.g. *** Looking up your hostname...
                if (from == null || from.length() == 0)
                    return false;

                // Notices and invites sent to a buffer shouldn't notify in the server buffer
                if(buffer_type.equalsIgnoreCase("console") && (to_chan || to_buffer))
                    return false;
            }
            return (type.equals("buffer_msg") ||
                    type.equals("buffer_me_msg") ||
                    type.equals("notice") ||
                    type.equals("channel_invite") ||
                    type.equals("callerid") ||
                    type.equals("wallops"));
        }

        public synchronized void set(IRCCloudJSONObject event) {
            cid = event.cid();
            bid = event.bid();
            eid = event.eid();
            type = event.type();
            msg = event.getString("msg");
            hostmask = event.getString("hostmask");
            from = event.getString("from");
            from_mode = event.getString("from_mode");
            chan = event.getString("chan");
            if(event.has("newnick"))
                nick = event.getString("newnick");
            else if(event.has("nick"))
                nick = event.getString("nick");
            else
                nick = null;
            old_nick = event.getString("oldnick");
            server = event.getString("server");
            diff = event.getString("diff");
            highlight = event.getBoolean("highlight");
            self = event.getBoolean("self");
            to_chan = event.getBoolean("to_chan");
            to_buffer = event.getBoolean("to_buffer");
            ops = event.getJsonNode("ops");
            color = R.color.row_message_label;
            bg_color = R.color.message_bg;
            row_type = 0;
            html = null;
            group_msg = null;
            linkify = true;
            target_mode = null;
            pending = false;
            failed = false;
            command = null;
            day = -1;

            if(event.has("reqid"))
                reqid = event.getInt("reqid");
            else
                reqid = -1;

            if(from != null)
                from = TextUtils.htmlEncode(from);

            if(msg != null)
                msg = TextUtils.htmlEncode(msg);

            if(event.has("value") && !event.type().startsWith("cap_")) {
                msg = "<pre>" + event.getString("value") + " " + msg + "</pre>";
            }

            if(highlight)
                bg_color = R.color.highlight;

            if(self)
                bg_color = R.color.self;
        }
    }
	
	private final HashMap<Integer,TreeMap<Long, Event>> events = new HashMap<Integer,TreeMap<Long, Event>>();;
    private final ArrayList<Event> search = new ArrayList<Event>();
	private static EventsDataSource instance = null;
	public long highest_eid = -1;
	
	public synchronized static EventsDataSource getInstance() {
		if(instance == null)
			instance = new EventsDataSource();
		return instance;
	}

	public void clear() {
		synchronized(events) {
			events.clear();
			highest_eid = -1;
		}
	}

    public ArrayList<Event> getSearch() {
        return search;
    }

    public void clearSearch() {
        search.clear();
    }

	public void addEvent(Event event) {
		synchronized(events) {
			if(!events.containsKey(event.bid))
				events.put(event.bid, new TreeMap<Long,Event>());
			
			events.get(event.bid).put(event.eid, event);
		}
	}

    public interface Formatter {
        public void format(IRCCloudJSONObject event, Event e);
    }

    private HashMap<String, Formatter> formatterMap = new HashMap<String, Formatter>() {{
        put("socket_closed", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                e.row_type = MessageViewFragment.ROW_SOCKETCLOSED;
                e.color = R.color.timestamp;
                e.linkify = false;
                if(event.has("pool_lost"))
                    e.msg = "Connection pool lost";
                else if(event.has("server_ping_timeout"))
                    e.msg = "Server PING timed out";
                else if(event.has("reason") && event.getString("reason").length() > 0)
                    e.msg = "Connection lost: " + reason(event.getString("reason"));
                else if(event.has("abnormal"))
                    e.msg = "Connection closed unexpectedly";
                else
                    e.msg = "";
            }
        });

        put("user_channel_mode", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.target_mode = event.getString("newmode");
                e.chan = event.getString("channel");
            }
        });

        put("buffer_me_msg", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.nick = e.from;
                e.from = "";
            }
        });

        put("nickname_in_use", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = event.getString("nick");
                e.msg = "is already in use";
                e.bg_color = R.color.error;
            }
        });

        put("unhandled_line", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                e.msg = "";
                if(event.has("command"))
                    e.msg = event.getString("command") + " ";
                if(event.has("raw"))
                    e.msg += event.getString("raw");
                else
                    e.msg += event.getString("msg");
                e.bg_color = R.color.error;
            }
        });
        put("unparsed_line", get("unhandled_line"));

        put("connecting_cancelled", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                e.msg = "Cancelled";
                e.bg_color = R.color.error;
            }
        });

        put("connecting_failed", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.row_type = MessageViewFragment.ROW_SOCKETCLOSED;
                e.color = R.color.timestamp;
                e.from = "";
                e.linkify = false;
                String reason = reason(event.getString("reason"));
                if(reason != null) {
                    e.msg = "Failed to connect: " + reason;
                } else {
                    e.msg = "Failed to connect.";
                }
            }
        });

        put("quit_server", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                e.msg = "⇐ You disconnected";
                e.color = R.color.timestamp;
                e.self = false;
            }
        });

        put("self_details", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                e.msg = "<pre>Your hostmask: <b>" + event.getString("usermask") + "</b></pre>";
                e.bg_color = R.color.status_bg;
                e.linkify = false;
            }
        });

        put("myinfo", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                e.msg = "Host: " + event.getString("server") + "\n";
                e.msg += "IRCd: " + event.getString("version") + "\n";
                e.msg += "User modes: " + event.getString("user_modes") + "\n";
                e.msg += "Channel modes: " + event.getString("channel_modes") + "\n";
                if(event.has("rest") && event.getString("rest").length() > 0)
                    e.msg += "Parametric channel modes: " + event.getString("rest") + "\n";
                e.msg = "<pre>" + TextUtils.htmlEncode(e.msg) + "</pre>";
                e.bg_color = R.color.status_bg;
                e.linkify = false;
            }
        });

        put("user_mode", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                e.msg = "<pre>Your user mode is: <b>+" + event.getString("newmode") + "</b></pre>";
                e.bg_color = R.color.status_bg;
            }
        });

        put("your_unique_id", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                e.msg = "<pre>Your unique ID is: <b>" + event.getString("unique_id") + "</b></pre>";
                e.bg_color = R.color.status_bg;
            }
        });

        put("kill", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
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
            }
        });

        put("banned", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                e.msg = "You were banned";
                if(event.has("server"))
                    e.msg += " from " + event.getString("server");
                if(event.has("reason"))
                    e.msg += ": " + TextUtils.htmlEncode(event.getString("reason"));
                e.bg_color = R.color.status_bg;
                e.linkify = false;
            }
        });

        put("channel_topic", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                if(event.has("author"))
                    e.from = event.getString("author");
                else
                    e.from = event.getString("server");
                if(event.getString("topic") != null && event.getString("topic").length() > 0)
                    e.msg = "set the topic: " + TextUtils.htmlEncode(event.getString("topic"));
                else
                    e.msg = "cleared the topic";
                e.bg_color = R.color.status_bg;
            }
        });

        put("channel_mode", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.nick = e.from;
                e.from = "";
                e.msg = "Channel mode set to: <b>" + event.getString("diff") + "</b>";
                e.bg_color = R.color.status_bg;
                e.linkify = false;
            }
        });

        put("channel_mode_is", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                if(event.getString("diff") != null && event.getString("diff").length() > 0)
                    e.msg = "Channel mode is: <b>" + event.getString("diff") + "</b>";
                else
                    e.msg = "No channel mode";
                e.bg_color = R.color.status_bg;
                e.linkify = false;
            }
        });

        put("kicked_channel", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                e.from_mode = null;
                e.old_nick = event.getString("nick");
                e.nick = event.getString("kicker");
                e.hostmask = event.getString("kicker_hostmask");
                e.color = R.color.timestamp;
                e.linkify = false;
                if(e.self)
                    e.row_type = MessageViewFragment.ROW_SOCKETCLOSED;
            }
        });
        put("you_kicked_channel", get("kicked_channel"));

        put("channel_mode_list_change", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                boolean unknown = true;
                JsonNode ops = event.getJsonObject("ops");
                if(ops != null) {
                    JsonNode add = ops.get("add");
                    if(add != null && add.size() > 0) {
                        JsonNode op = add.get(0);
                        if(op.get("mode").asText().equals("b")) {
                            e.nick = e.from;
                            e.from = "";
                            e.msg = "banned <b>" + op.get("param").asText() + "</b> (<font color=#808080>+b</font>)";
                            unknown = false;
                        } else if(op.get("mode").asText().equals("e")) {
                            e.nick = e.from;
                            e.from = "";
                            e.msg = "exempted <b>" + op.get("param").asText() + "</b> from bans (<font color=#808080>+e</font>)";
                            unknown = false;
                        } else if(op.get("mode").asText().equals("q")) {
                            e.nick = e.from;
                            e.from = "";
                            e.msg = "quieted <b>" + op.get("param").asText() + "</b> (<font color=#808080>+q</font>)";
                            unknown = false;
                        } else if(op.get("mode").asText().equals("I")) {
                            e.nick = e.from;
                            e.from = "";
                            e.msg = "added <b>" + op.get("param").asText() + "</b> to the invite list (<font color=#808080>+I</font>)";
                            unknown = false;
                        }
                    }
                    JsonNode remove = ops.get("remove");
                    if(remove != null && remove.size() > 0) {
                        JsonNode op = remove.get(0);
                        if(op.get("mode").asText().equals("b")) {
                            e.nick = e.from;
                            e.from = "";
                            e.msg = "un-banned <b>" + op.get("param").asText() + "</b> (<font color=#808080>-b</font>)";
                            unknown = false;
                        } else if(op.get("mode").asText().equals("e")) {
                            e.nick = e.from;
                            e.from = "";
                            e.msg = "un-exempted <b>" + op.get("param").asText() + "</b> from bans (<font color=#808080>-e</font>)";
                            unknown = false;
                        } else if(op.get("mode").asText().equals("q")) {
                            e.nick = e.from;
                            e.from = "";
                            e.msg = "un-quieted <b>" + op.get("param").asText() + "</b> (<font color=#808080>-q</font>)";
                            unknown = false;
                        } else if(op.get("mode").asText().equals("I")) {
                            e.nick = e.from;
                            e.from = "";
                            e.msg = "removed <b>" + op.get("param").asText() + "</b> from the invite list (<font color=#808080>-I</font>)";
                            unknown = false;
                        }
                    }
                }
                if(unknown) {
                    e.nick = e.from;
                    e.from = "";
                    e.msg = "set channel mode: <b>" + event.getString("diff") + "</b>";
                }
                e.bg_color = R.color.status_bg;
                e.linkify = false;
                e.self = false;
            }
        });

        put("motd_response", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                JsonNode lines = event.getJsonNode("lines");
                e.from = "";
                if(lines != null) {
                    StringBuilder builder = new StringBuilder("<pre>");
                    if(event.has("start"))
                        builder.append(event.getString("start")).append("<br/>");
                    for(int i = 0; i < lines.size(); i++) {
                        builder.append(TextUtils.htmlEncode(lines.get(i).asText()).replace("  ", " &nbsp;")).append("<br/>");
                    }
                    builder.append("</pre>");
                    e.msg = builder.toString();
                }
                e.bg_color = R.color.self;
            }
        });
        put("server_motd", get("motd_response"));
        put("info_response", get("motd_response"));

        put("notice", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.chan = event.getString("target");
                e.msg = "<pre>" + e.msg.replace("  ", " &nbsp;") + "</pre>";
                e.bg_color = R.color.notice;
            }
        });

        put("newsflash", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.msg = "<pre>" + e.msg + "</pre>";
                e.bg_color = R.color.notice;
            }
        });

        put("invited", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = event.getString("inviter");
                e.msg = "<pre>invited " + event.getString("invitee") + " to join " + event.getString("channel") + "</pre>";
                e.bg_color = R.color.notice;
            }
        });

        put("generic_server_info", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.msg = "<pre>" + e.msg + "</pre>";
                e.bg_color = R.color.notice;
            }
        });

        put("rehashed_config", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.msg = "Rehashed config: " + event.getString("file") + "(" + e.msg + ")";
                e.msg = "<pre>" + e.msg + "</pre>";
                e.bg_color = R.color.notice;
            }
        });

        put("knock", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                if(e.nick != null && e.nick.length() > 0) {
                    e.from = e.nick;
                    if(e.hostmask != null && e.hostmask.length() > 0)
                        e.msg += " (" + e.hostmask + ")";
                } else {
                    e.msg = event.getString("userhost") + " " + e.msg;
                }
                e.msg = "<pre>" + e.msg + "</pre>";
                e.bg_color = R.color.notice;
            }
        });

        put("hidden_host_set", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.bg_color = R.color.status_bg;
                e.linkify = false;
                e.from = "";
                e.msg = "<b>" + event.getString("hidden_host") + "</b> " + e.msg;
            }
        });

        put("inviting_to_channel", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                e.msg = "You invited " + event.getString("recipient") + " to join " + event.getString("channel");
                e.bg_color = R.color.notice;
            }
        });

        put("channel_invite", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.msg = "<pre>Invite to join " + event.getString("channel") + "</pre>";
                e.old_nick = event.getString("channel");
                e.bg_color = R.color.notice;
            }
        });

        put("callerid", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = e.nick;
                e.msg = "<pre>" + e.msg + "</pre>";
                e.highlight = true;
                e.linkify = false;
                e.hostmask = event.getString("usermask");
            }
        });

        put("target_callerid", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = event.getString("target_nick");
                e.msg = "<pre>" + e.msg + "</pre>";
                e.bg_color = R.color.error;
            }
        });

        put("target_notified", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = event.getString("target_nick");
                e.msg = "<pre>" + e.msg + "</pre>";
                e.bg_color = R.color.error;
            }
        });

        put("link_channel", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                if(event.has("invalid_chan")) {
                    if(event.has("valid_chan")) {
                        e.msg = event.getString("invalid_chan") + " → " + event.getString("valid_chan") + " " + e.msg;
                    } else {
                        e.msg = event.getString("invalid_chan") + " " + e.msg;
                    }
                }
                e.bg_color = R.color.error;
            }
        });

        String[] statuses = {
                "server_motdstart",
                "server_welcome",
                "server_motd",
                "server_endofmotd",
                "server_nomotd",
                "server_luserclient",
                "server_luserop",
                "server_luserconns",
                "server_luserme",
                "server_n_local",
                "server_luserchannels",
                "server_n_global",
                "server_yourhost",
                "server_created",
                "server_luserunknown",
                "server_snomask",
                "starircd_welcome",
                "zurna_motd",
                "wait",
                "logged_in_as",
                "btn_metadata_set",
                "sasl_success",
                "you_are_operator",
                "codepage",
                "logged_out",
                "nick_locked",
                "text",
                "admin_info"
        };
        Formatter statusFormatter = new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.bg_color = R.color.status_bg;
                e.msg = "<pre>" + e.msg + "</pre>";
                e.from = "";
                if(!e.type.equals("server_motd") && !e.type.equals("zurna_motd"))
                    e.linkify = false;
            }
        };
        for(String status : statuses)
            put(status, statusFormatter);

        String[] stats = {
                "stats", "statslinkinfo", "statscommands", "statscline", "statsnline", "statsiline", "statskline", "statsqline", "statsyline", "statsbline", "statsgline", "statstline", "statseline", "statsvline", "statslline", "statsuptime", "statsoline", "statshline", "statssline", "statsuline", "statsdebug", "endofstats"
        };
        Formatter statsFormatter = new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                if(event.has("parts") && event.getString("parts").length() > 0)
                    e.msg = event.getString("parts") + ": " + e.msg;
                e.bg_color = R.color.status_bg;
                e.linkify = false;
                e.msg = "<pre>" + e.msg + "</pre>";
            }
        };
        for(String stat : stats)
            put(stat, statsFormatter);

        String[] caps = {
                "cap_ls", "cap_req", "cap_ack", "cap_raw"
        };
        Formatter capsFormatter = new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                e.bg_color = R.color.status_bg;
                e.linkify = false;
                if(e.type.equals("cap_ls"))
                    e.msg = "<b>CAP</b> Server supports: ";
                else if(e.type.equals("cap_req"))
                    e.msg = "<b>CAP</b> Requesting: ";
                else if(e.type.equals("cap_ack"))
                    e.msg = "<b>CAP</b> Acknowledged: ";
                else if(e.type.equals("cap_raw"))
                    e.msg = "<b>CAP</b> " + event.getString("line");
                JsonNode caps = event.getJsonNode("caps");
                if(caps != null) {
                    for (int i = 0; i < caps.size(); i++) {
                        if (i > 0)
                            e.msg += " | ";
                        e.msg += caps.get(i).asText();
                    }
                }
                e.msg = "<pre>" + e.msg + "</pre>";
            }
        };
        for(String cap : caps)
            put(cap, capsFormatter);

        String[] helps = {
                "help_topics_start", "help_topics", "help_topics_end", "helphdr", "helpop", "helptlr", "helphlp", "helpfwd", "helpign"
        };
        Formatter helpsFormatter = new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.bg_color = R.color.status_bg;
                e.msg = "<pre>" + e.msg + "</pre>";
                e.from = "";
            }
        };
        for(String help : helps)
            put(help, helpsFormatter);

        String[] errors = {
                "too_fast", "sasl_fail", "sasl_too_long", "sasl_aborted", "sasl_already", "no_bots", "msg_services", "bad_ping", "not_for_halfops", "ambiguous_error_message", "list_syntax", "who_syntax"
        };
        Formatter errorFormatter = new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                e.bg_color = R.color.error;
            }
        };
        for(String error : errors)
            put(error, errorFormatter);

        put("version", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                e.msg = "<pre><b>" + event.getString("server_version") + "</b> " + event.getString("comments") + "</pre>";
                e.bg_color = R.color.status_bg;
                e.linkify = false;
            }
        });

        put("services_down", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = event.getString("services_name");
                e.bg_color = R.color.error;
            }
        });

        put("unknown_umode", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = "";
                if(event.has("flag"))
                    e.msg = "<b>" + event.getString("flag") + "</b> " + e.msg;
                e.bg_color = R.color.error;
            }
        });

        put("kill_deny", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = event.getString("channel");
                e.bg_color = R.color.error;
            }
        });

        put("chan_own_priv_needed", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = event.getString("channel");
                e.bg_color = R.color.error;
            }
        });

        put("chan_forbidden", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.from = event.getString("channel");
                e.bg_color = R.color.error;
            }
        });

        put("list_usage", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.msg = "<pre>" + e.msg + "</pre>";
                e.bg_color = R.color.notice;
            }
        });

        put("time", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.bg_color = R.color.status_bg;
                e.msg = "<pre>" + event.getString("time_string");
                if(event.has("time_stamp") && event.getString("time_stamp").length() > 0)
                    e.msg += " (" + event.getString("time_stamp") + ")";
                e.msg += " — <b>" + event.getString("time_server") + "</b></pre>";
                e.linkify = false;
            }
        });

        put("watch_status", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.bg_color = R.color.status_bg;
                e.from = event.getString("watch_nick");
                e.msg = "<pre>" + e.msg + " (" + event.getString("username") + "@" + event.getString("userhost") + ")</pre>";
                e.linkify = false;
            }
        });

        put("sqline_nick", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e) {
                e.bg_color = R.color.status_bg;
                e.from = event.getString("charset");
                e.msg = "<pre>" + e.msg + "</pre>";
            }
        });

    }};
    
    private String reason(String reason) {
        if(reason != null) {
            if(reason.equalsIgnoreCase("pool_lost")) {
                return "Connection pool failed";
            } else if(reason.equalsIgnoreCase("no_pool")) {
                return "No available connection pools";
            } else if(reason.equalsIgnoreCase("enetdown")) {
                return "Network down";
            } else if(reason.equalsIgnoreCase("etimedout") || reason.equalsIgnoreCase("timeout")) {
                return "Timed out";
            } else if(reason.equalsIgnoreCase("ehostunreach")) {
                return "Host unreachable";
            } else if(reason.equalsIgnoreCase("econnrefused")) {
                return "Connection refused";
            } else if(reason.equalsIgnoreCase("nxdomain")) {
                return "Invalid hostname";
            } else if(reason.equalsIgnoreCase("server_ping_timeout")) {
                return "PING timeout";
            } else if(reason.equalsIgnoreCase("ssl_certificate_error")) {
                return "SSL certificate error";
            } else if(reason.equalsIgnoreCase("ssl_error")) {
                return "SSL error";
            } else if(reason.equalsIgnoreCase("crash")) {
                return "Connection crashed";
            }
        }
        return reason;
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

            e.set(event);

            Formatter f = formatterMap.get(e.type);
            if(f != null)
                f.format(event, e);

			if(highest_eid < event.eid())
				highest_eid = event.eid();

			return e;
		}
	}

    public void addSearchResult(IRCCloudJSONObject event) {
        synchronized (search) {
            Event e = new Event();
            e.set(event);
            Formatter f = formatterMap.get(e.type);
            if(f != null)
                f.format(event, e);
            search.add(e);
        }
    }

    public int getSizeOfBuffer(int bid) {
        synchronized(events) {
            if(events.containsKey(bid))
                return events.get(bid).size();
        }
        return 0;
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

    public Long lastEidForBuffer(int bid) {
        synchronized(events) {
            if(events.containsKey(bid)) {
                Long[] eids = events.get(bid).keySet().toArray(new Long[events.get(bid).keySet().size()]);
                if(eids.length > 0)
                    return eids[eids.length - 1];
            }
        }
        return 0L;
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

    public void pruneEvents(int bid) {
        synchronized(events) {
            TreeMap<Long,Event> e = events.get(bid);
            while(e != null && e.size() > 50 && e.firstKey() != null) {
                e.remove(e.firstKey());
            }
        }
    }

    public synchronized void clearCaches() {
        synchronized(events) {
            for(int bid : events.keySet()) {
                if(events.containsKey(bid)) {
                    for(Event e : events.get(bid).values()) {
                        e.timestamp = null;
                        e.html = null;
                        e.formatted = null;
                    }
                }
            }
        }
    }
}
