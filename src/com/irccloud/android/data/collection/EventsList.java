/*
 * Copyright (c) 2015 IRCCloud, Ltd.
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

package com.irccloud.android.data.collection;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.data.model.Event;
import com.irccloud.android.fragment.MessageViewFragment;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

@SuppressLint("UseSparseArrays")
public class EventsList {
    private final HashMap<Integer, TreeMap<Long, Event>> events;
    private HashSet<Integer> loaded_bids = new HashSet<>();
    private static EventsList instance = null;
    private ColorScheme colorScheme = ColorScheme.getInstance();

    public synchronized static EventsList getInstance() {
        if (instance == null)
            instance = new EventsList();
        return instance;
    }

    public EventsList() {
        events = new HashMap<>(100);
    }

    public void load(int bid) {
        /*synchronized (events) {
            Cursor c = null;
            try {
                if(loaded_bids.contains(bid))
                    return;
                long start = System.currentTimeMillis();
                ModelAdapter<Event> modelAdapter = FlowManager.getModelAdapter(Event.class);
                if (events.containsKey(bid) && events.get(bid) != null && events.get(bid).size() > 0) {
                    c = new Select().from(Event.class).where(Condition.column(Event$Table.BID).is(bid)).and(Condition.column(Event$Table.EID).lessThan(events.get(bid).firstKey())).query();
                } else {
                    c = new Select().from(Event.class).where(Condition.column(Event$Table.BID).is(bid)).query();
                }

                if (c != null && c.moveToFirst()) {
                    android.util.Log.d("IRCCloud", "Loading events for bid" + bid);
                    do {
                        addEvent(modelAdapter.loadFromCursor(c));
                    } while (c.moveToNext());
                    long time = System.currentTimeMillis() - start;
                    android.util.Log.i("IRCCloud", "Loaded " + c.getCount() + " events in " + time + "ms");
                    loaded_bids.add(bid);
                }
            } catch (SQLiteException e) {
                NetworkConnection.printStackTraceToCrashlytics(e);
            } finally {
                if(c != null)
                    c.close();
            }
        }*/
    }

    public void save() {
        /*synchronized (events) {
            for (int bid : events.keySet()) {
                TreeMap<Long, Event> e = events.get(bid);
                if (e != null) {
                    TransactionManager.getInstance().saveOnSaveQueue(e.values());
                }
            }
        }*/
    }

    public void clear() {
        synchronized (events) {
            events.clear();
            loaded_bids.clear();
            //Delete.table(Event.class);
        }
    }

    public void addEvent(Event event) {
        synchronized (events) {
            if (!events.containsKey(event.bid) || events.get(event.bid) == null)
                events.put(event.bid, new TreeMap<Long, Event>());

            events.get(event.bid).put(event.eid, event);
        }
    }

    public interface Formatter {
        void format(IRCCloudJSONObject event, Event e, StringBuilder sb);
    }

    private HashMap<String, Formatter> formatterMap = new HashMap<String, Formatter>() {{
        put("socket_closed", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                e.from = "";
                e.row_type = MessageViewFragment.ROW_SOCKETCLOSED;
                e.color = colorScheme.timestampColor;
                e.linkify = false;
                if(event != null) {
                    if (event.has("pool_lost"))
                        e.msg = "Connection pool lost";
                    else if (event.has("server_ping_timeout"))
                        e.msg = "Server PING timed out";
                    else if (event.has("reason") && event.getString("reason").length() > 0)
                        e.msg = sb.append("Connection lost: ").append(reason(event.getString("reason"))).toString();
                    else if (event.has("abnormal"))
                        e.msg = "Connection closed unexpectedly";
                    else
                        e.msg = "";
                }
            }
        });

        put("user_channel_mode", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.target_mode = event.getString("newmode");
                    e.chan = event.getString("channel");
                }
            }
        });

        put("buffer_msg", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.target_mode = event.getString("statusmsg");
                }
            }
        });

        put("buffer_me_msg", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from_nick = event.getString("from");
                    if(event.has("display_name") && event.getString("display_name") != null && event.getString("display_name").length() > 0)
                        e.nick = event.getString("display_name");
                    else
                        e.nick = e.from_nick;
                    e.from = "";
                }
            }
        });

        put("nickname_in_use", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = event.getString("nick");
                }
                e.msg = "is already in use";
                e.color = colorScheme.networkErrorColor;
                e.bg_color = colorScheme.errorBackgroundColor;
            }
        });

        put("unhandled_line", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = "";
                    if (event.has("command"))
                        sb.append(event.getString("command")).append(" ");
                    if (event.has("raw"))
                        sb.append(event.getString("raw"));
                    else
                        sb.append(event.getString("msg"));
                    e.msg = sb.toString();
                }
                e.color = colorScheme.networkErrorColor;
                e.bg_color = colorScheme.errorBackgroundColor;
            }
        });
        put("unparsed_line", get("unhandled_line"));

        put("connecting_cancelled", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                e.from = "";
                if(event != null) {
                    if(event.has("sts"))
                        e.msg = "Upgrading connection security";
                    else
                        e.msg = "Cancelled";
                }
                e.color = colorScheme.networkErrorColor;
                e.bg_color = colorScheme.errorBackgroundColor;
            }
        });

        put("connecting_failed", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                e.row_type = MessageViewFragment.ROW_SOCKETCLOSED;
                e.color = colorScheme.timestampColor;
                e.from = "";
                e.linkify = false;
                if(event != null) {
                    String reason = reason(event.getString("reason"));
                    if (reason != null) {
                        if(reason.equals("ssl_verify_error")) {
                            String error = SSLreason(event.getJsonNode("ssl_verify_error"));
                            if(error != null && error.length() > 0)
                                e.msg = sb.append("Strict transport security error: ").append(error).toString();
                            else
                                e.msg = "Strict transport security error";
                        } else {
                            e.msg = sb.append("Failed to connect: ").append(reason).toString();
                        }
                    } else {
                        e.msg = "Failed to connect.";
                    }
                }
            }
        });

        put("quit_server", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                e.from = "";
                e.msg = "⇐ You disconnected";
                e.color = colorScheme.timestampColor;
                e.self = false;
            }
        });

        put("self_details", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                e.bg_color = colorScheme.statusBackgroundColor;
                e.linkify = false;
                if(event != null) {
                    e.from = "";
                    if(event.has("usermask") && event.has("user") && event.getString("user").length() > 0) {
                        e.msg = sb.append("<pre>Your hostmask: <b>").append(event.getString("usermask")).append("</b></pre>").toString();
                        sb.setLength(0);
                        if(event.has("server_realname")) {
                            Event e1 = new Event(e);
                            e1.eid++;
                            e1.msg = sb.append("<pre>Your name: <b>").append(event.getString("server_realname")).append("</b></pre>").toString();
                            e1.linkify = true;
                            addEvent(e1);
                        }
                    } else if(event.has("server_realname")) {
                        e.msg = sb.append("<pre>Your name: <b>").append(event.getString("server_realname")).append("</b></pre>").toString();
                        e.linkify = true;
                    }
                }
            }
        });

        put("myinfo", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = "";
                    sb.append("Host: ").append(event.getString("server")).append("\n");
                    sb.append("IRCd: ").append(event.getString("version")).append("\n");
                    sb.append("User modes: ").append(event.getString("user_modes")).append("\n");
                    sb.append("Channel modes: ").append(event.getString("channel_modes")).append("\n");
                    if (event.has("rest") && event.getString("rest").length() > 0)
                        sb.append("Parametric channel modes: ").append(event.getString("rest")).append("\n");
                    e.msg = "<pre>" + TextUtils.htmlEncode(sb.toString()) + "</pre>";
                }
                e.bg_color = colorScheme.statusBackgroundColor;
                e.linkify = false;
            }
        });

        put("user_mode", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = "";
                    e.msg = sb.append("<pre>Your user mode is: <b>+").append(event.getString("newmode")).append("</b></pre>").toString();
                }
                e.bg_color = colorScheme.statusBackgroundColor;
            }
        });

        put("your_unique_id", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = "";
                    e.msg = sb.append("<pre>Your unique ID is: <b>").append(event.getString("unique_id")).append("</b></pre>").toString();
                }
                e.bg_color = colorScheme.statusBackgroundColor;
            }
        });

        put("kill", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = "";
                    sb.append("You were killed");
                    if (event.has("from"))
                        sb.append(" by ").append(event.getString("from"));
                    if (event.has("killer_hostmask"))
                        sb.append(" (").append(event.getString("killer_hostmask")).append(")");
                    if (event.has("reason"))
                        sb.append(": ").append(TextUtils.htmlEncode(event.getString("reason")));
                    e.msg = sb.toString();
                }
                e.bg_color = colorScheme.statusBackgroundColor;
                e.linkify = false;
            }
        });

        put("banned", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = "";
                    sb.append("You were banned");
                    if (event.has("server"))
                        sb.append(" from ").append(event.getString("server"));
                    if (event.has("reason"))
                        sb.append(": ").append(TextUtils.htmlEncode(event.getString("reason")));
                    e.msg = sb.toString();
                }
                e.bg_color = colorScheme.statusBackgroundColor;
                e.linkify = false;
            }
        });

        put("channel_topic", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    if (event.has("author"))
                        e.from = event.getString("author");
                    else
                        e.from = event.getString("server");
                    if (event.getString("topic") != null && event.getString("topic").length() > 0)
                        e.msg = sb.append("set the topic: ").append(TextUtils.htmlEncode(event.getString("topic"))).toString();
                    else
                        e.msg = "cleared the topic";
                }
                e.bg_color = colorScheme.statusBackgroundColor;
            }
        });

        put("channel_mode", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.nick = e.from;
                    e.from = "";
                    e.msg = sb.append("Channel mode set to: <b>").append(event.getString("diff")).append("</b>").toString();
                }
                e.bg_color = colorScheme.statusBackgroundColor;
                e.linkify = false;
                e.self = false;
            }
        });

        put("channel_mode_is", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = "";
                    if (event.getString("diff") != null && event.getString("diff").length() > 0)
                        e.msg = sb.append("Channel mode is: <b>").append(event.getString("diff")).append("</b>").toString();
                    else
                        e.msg = "No channel mode";
                }
                e.bg_color = colorScheme.statusBackgroundColor;
                e.linkify = false;
            }
        });

        put("kicked_channel", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = "";
                    e.from_mode = event.getString("kicker_mode");
                    e.from_hostmask = event.getString("kicker_hostmask");
                    e.old_nick = event.getString("nick");
                    e.nick = event.getString("kicker");
                }
                e.color = colorScheme.timestampColor;
                e.linkify = false;
                if (e.self)
                    e.row_type = MessageViewFragment.ROW_SOCKETCLOSED;
            }
        });
        put("you_kicked_channel", get("kicked_channel"));

        put("channel_mode_list_change", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    boolean unknown = true;
                    JsonNode ops = event.getJsonObject("ops");
                    if (ops != null) {
                        JsonNode add = ops.get("add");
                        if (add != null && add.size() > 0) {
                            JsonNode op = add.get(0);
                            if (op.get("mode").asText().equals("b")) {
                                e.nick = e.from;
                                e.from = "";
                                e.msg = sb.append("banned <b>").append(op.get("param").asText()).append("</b> (<font color=#808080>+b</font>)").toString();
                                unknown = false;
                            } else if (op.get("mode").asText().equals("e")) {
                                e.nick = e.from;
                                e.from = "";
                                e.msg = sb.append("exempted <b>").append(op.get("param").asText()).append("</b> from bans (<font color=#808080>+e</font>)").toString();
                                unknown = false;
                            } else if (op.get("mode").asText().equals("q")) {
                                if (op.get("param").asText().contains("@") || op.get("param").asText().contains("$")) {
                                    e.nick = e.from;
                                    e.from = "";
                                    e.msg = sb.append("quieted <b>").append(op.get("param").asText()).append("</b> (<font color=#808080>+q</font>)").toString();
                                } else {
                                    e.type = "user_channel_mode";
                                    e.chan = event.getString("channel");
                                    e.nick = op.get("param").asText();
                                }
                                unknown = false;
                            } else if (op.get("mode").asText().equals("I")) {
                                e.nick = e.from;
                                e.from = "";
                                e.msg = sb.append("added <b>").append(op.get("param").asText()).append("</b> to the invite list (<font color=#808080>+I</font>)").toString();
                                unknown = false;
                            }
                        }
                        JsonNode remove = ops.get("remove");
                        if (remove != null && remove.size() > 0) {
                            JsonNode op = remove.get(0);
                            if (op.get("mode").asText().equals("b")) {
                                e.nick = e.from;
                                e.from = "";
                                e.msg = sb.append("un-banned <b>").append(op.get("param").asText()).append("</b> (<font color=#808080>-b</font>)").toString();
                                unknown = false;
                            } else if (op.get("mode").asText().equals("e")) {
                                e.nick = e.from;
                                e.from = "";
                                e.msg = sb.append("un-exempted <b>").append(op.get("param").asText()).append("</b> from bans (<font color=#808080>-e</font>)").toString();
                                unknown = false;
                            } else if (op.get("mode").asText().equals("q")) {
                                if (op.get("param").asText().contains("@") || op.get("param").asText().contains("$")) {
                                    e.nick = e.from;
                                    e.from = "";
                                    e.msg = sb.append("un-quieted <b>").append(op.get("param").asText()).append("</b> (<font color=#808080>-q</font>)").toString();
                                } else {
                                    e.type = "user_channel_mode";
                                    e.chan = event.getString("channel");
                                    e.nick = op.get("param").asText();
                                }
                                unknown = false;
                            } else if (op.get("mode").asText().equals("I")) {
                                e.nick = e.from;
                                e.from = "";
                                e.msg = sb.append("removed <b>").append(op.get("param").asText()).append("</b> from the invite list (<font color=#808080>-I</font>)").toString();
                                unknown = false;
                            }
                        }
                    }
                    if (unknown) {
                        e.nick = e.from;
                        e.from = "";
                        e.msg = sb.append("set channel mode: <b>").append(event.getString("diff")).append("</b>").toString();
                    }
                }
                e.bg_color = colorScheme.statusBackgroundColor;
                e.linkify = false;
                e.self = false;
            }
        });

        put("motd_response", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    JsonNode lines = event.getJsonNode("lines");
                    e.from = "";
                    if (lines != null) {
                        sb.append("<pre>");
                        if (event.has("start"))
                            sb.append(event.getString("start")).append("<br/>");
                        for (int i = 0; i < lines.size(); i++) {
                            sb.append(TextUtils.htmlEncode(lines.get(i).asText()).replace("  ", " &nbsp;")).append("<br/>");
                        }
                        sb.append("</pre>");
                        e.msg = sb.toString();
                    }
                }
                e.bg_color = colorScheme.selfBackgroundColor;
            }
        });
        put("server_motd", get("motd_response"));
        put("info_response", get("motd_response"));

        put("notice", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.chan = event.getString("target");
                    e.nick = event.getString("target");
                    e.target_mode = event.getString("statusmsg");
                    e.msg = sb.append("<pre>").append(e.msg).append("</pre>").toString();
                }
                e.bg_color = colorScheme.noticeBackgroundColor;
            }
        });

        put("buffer_msg", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.target_mode = event.getString("statusmsg");
                }
            }
        });

        put("newsflash", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.msg = sb.append("<pre>").append(e.msg).append("</pre>").toString();
                }
                e.bg_color = colorScheme.noticeBackgroundColor;
            }
        });

        put("invited", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = event.getString("inviter");
                    e.msg = sb.append("<pre>invited ").append(event.getString("invitee")).append(" to join ").append(event.getString("channel")).append("</pre>").toString();
                }
                e.bg_color = colorScheme.noticeBackgroundColor;
            }
        });

        put("generic_server_info", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.msg = sb.append("<pre>").append(e.msg).append("</pre>").toString();
                }
                e.bg_color = colorScheme.noticeBackgroundColor;
            }
        });

        put("rehashed_config", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.msg = sb.append("<pre>Rehashed config: ").append(event.getString("file")).append("(").append(e.msg).append(")</pre>").toString();
                }
                e.bg_color = colorScheme.noticeBackgroundColor;
            }
        });

        put("knock", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    if (e.nick != null && e.nick.length() > 0) {
                        sb.append("<pre>").append(e.msg);
                        e.from = e.nick;
                        if (e.hostmask != null && e.hostmask.length() > 0)
                            sb.append(" (").append(e.hostmask).append(")");
                    } else {
                        sb.append("<pre>").append(event.getString("userhost")).append(" ").append(e.msg);
                    }
                    sb.append("</pre>");
                    e.msg = sb.toString();
                }
                e.bg_color = colorScheme.noticeBackgroundColor;
            }
        });

        put("hidden_host_set", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                e.bg_color = colorScheme.statusBackgroundColor;
                e.linkify = false;
                if(event != null) {
                    e.from = "";
                    e.msg = sb.append("<b>").append(event.getString("hidden_host")).append("</b> ").append(e.msg).toString();
                }
            }
        });

        put("inviting_to_channel", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = "";
                    e.msg = sb.append("<pre>You invited ").append(event.getString("recipient")).append(" to join ").append(event.getString("channel")).append("</pre>").toString();
                }
                e.bg_color = colorScheme.noticeBackgroundColor;
            }
        });

        put("channel_invite", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.msg = sb.append("<pre>Invite to join ").append(event.getString("channel")).append("</pre>").toString();
                    e.old_nick = event.getString("channel");
                }
                e.bg_color = colorScheme.noticeBackgroundColor;
            }
        });

        put("callerid", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = e.nick;
                    e.msg = sb.append("<pre>").append(e.msg).append("</pre>").toString();
                    e.highlight = true;
                    e.linkify = false;
                    e.hostmask = event.getString("usermask");
                }
            }
        });

        put("target_callerid", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = event.getString("target_nick");
                    e.msg = sb.append("<pre>").append(e.msg).append("</pre>").toString();
                }
                e.color = colorScheme.networkErrorColor;
                e.bg_color = colorScheme.errorBackgroundColor;
            }
        });

        put("target_notified", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = event.getString("target_nick");
                    e.msg = sb.append("<pre>").append(e.msg).append("</pre>").toString();
                }
                e.color = colorScheme.networkErrorColor;
                e.bg_color = colorScheme.errorBackgroundColor;
            }
        });

        put("link_channel", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    if (event.has("invalid_chan")) {
                        if (event.has("valid_chan")) {
                            e.msg = sb.append(event.getString("invalid_chan")).append(" → ").append(event.getString("valid_chan")).append(" ").append(e.msg).toString();
                        } else {
                            e.msg = sb.append(event.getString("invalid_chan")).append(" ").append(e.msg).toString();
                        }
                    }
                }
                e.color = colorScheme.networkErrorColor;
                e.bg_color = colorScheme.errorBackgroundColor;
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
                "admin_info",
                "error"
        };
        Formatter statusFormatter = new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                e.bg_color = colorScheme.statusBackgroundColor;
                if(event != null) {
                    e.msg = sb.append("<pre>").append(e.msg).append("</pre>").toString();
                    e.from = "";
                    if (!e.type.equals("server_motd") && !e.type.equals("zurna_motd"))
                        e.linkify = false;
                }
            }
        };
        for (String status : statuses)
            put(status, statusFormatter);

        String[] stats = {
                "stats", "statslinkinfo", "statscommands", "statscline", "statsnline", "statsiline", "statskline", "statsqline", "statsyline", "statsbline", "statsgline", "statstline", "statseline", "statsvline", "statslline", "statsuptime", "statsoline", "statshline", "statssline", "statsuline", "statsdebug", "spamfilter", "endofstats"
        };
        Formatter statsFormatter = new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = "";
                    sb.append("<pre>");
                    if (event.has("parts") && event.getString("parts").length() > 0)
                        sb.append(event.getString("parts")).append(": ");
                    sb.append(e.msg).append("</pre>");
                    e.msg = sb.toString();
                }
                e.bg_color = colorScheme.statusBackgroundColor;
                e.linkify = false;
            }
        };
        for (String stat : stats)
            put(stat, statsFormatter);

        String[] caps = {
                "cap_ls", "cap_req", "cap_ack", "cap_raw", "cap_new", "cap_list", "cap_invalid"
        };
        Formatter capsFormatter = new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    sb.append("<pre>");
                    e.from = "";
                    e.linkify = false;
                    switch (e.type) {
                        case "cap_ls":
                            sb.append("<b>CAP</b> Server supports: ");
                            break;
                        case "cap_req":
                            sb.append("<b>CAP</b> Requesting: ");
                            break;
                        case "cap_ack":
                            sb.append("<b>CAP</b> Acknowledged: ");
                            break;
                        case "cap_nak":
                            sb.append("<b>CAP</b> Rejected: ");
                            break;
                        case "cap_raw":
                            sb.append("<b>CAP</b> ").append(event.getString("line"));
                            break;
                        case "cap_new":
                            sb.append("<b>CAP</b> Server added: ");
                            break;
                        case "cap_del":
                            sb.append("<b>CAP</b> Server removed: ");
                            break;
                        case "cap_list":
                            sb.append("<b>CAP</b> Enabled: ");
                            break;
                        case "cap_invalid":
                            sb.append("<b>CAP</b> ").append(event.getString("msg"));
                            break;
                    }
                    JsonNode caps = event.getJsonNode("caps");
                    if (caps != null) {
                        for (int i = 0; i < caps.size(); i++) {
                            if (i > 0)
                                sb.append(" | ");
                            sb.append(caps.get(i).asText());
                        }
                    }
                    sb.append("</pre>");
                    e.msg = sb.toString();
                }
                e.bg_color = colorScheme.statusBackgroundColor;
            }
        };
        for (String cap : caps)
            put(cap, capsFormatter);

        String[] helps = {
                "help_topics_start", "help_topics", "help_topics_end", "helphdr", "helpop", "helptlr", "helphlp", "helpfwd", "helpign"
        };
        Formatter helpsFormatter = new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                e.bg_color = colorScheme.statusBackgroundColor;
                if(event != null) {
                    e.msg = sb.append("<pre>").append(e.msg).append("</pre>").toString();
                    e.from = "";
                }
            }
        };
        for (String help : helps)
            put(help, helpsFormatter);

        String[] errors = {
                "too_fast", "sasl_fail", "sasl_too_long", "sasl_aborted", "sasl_already", "no_bots", "msg_services", "bad_ping", "not_for_halfops", "ambiguous_error_message", "list_syntax", "who_syntax"
        };
        Formatter errorFormatter = new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = "";
                }
                e.color = colorScheme.networkErrorColor;
                e.bg_color = colorScheme.errorBackgroundColor;
            }
        };
        for (String error : errors)
            put(error, errorFormatter);

        put("version", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = "";
                    e.msg = sb.append("<pre><b>").append(event.getString("server_version")).append("</b> ").append(event.getString("comments")).append("</pre>").toString();
                }
                e.bg_color = colorScheme.statusBackgroundColor;
                e.linkify = false;
            }
        });

        put("services_down", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = event.getString("services_name");
                }
                e.color = colorScheme.networkErrorColor;
                e.bg_color = colorScheme.errorBackgroundColor;
            }
        });

        put("unknown_umode", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = "";
                    if (event.has("flag"))
                        e.msg = sb.append("<b>").append(event.getString("flag")).append("</b> ").append(e.msg).toString();
                }
                e.color = colorScheme.networkErrorColor;
                e.bg_color = colorScheme.errorBackgroundColor;
            }
        });

        put("kill_deny", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = event.getString("channel");
                }
                e.color = colorScheme.networkErrorColor;
                e.bg_color = colorScheme.errorBackgroundColor;
            }
        });

        put("chan_own_priv_needed", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = event.getString("channel");
                }
                e.color = colorScheme.networkErrorColor;
                e.bg_color = colorScheme.errorBackgroundColor;
            }
        });

        put("chan_forbidden", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.from = event.getString("channel");
                }
                e.color = colorScheme.networkErrorColor;
                e.bg_color = colorScheme.errorBackgroundColor;
            }
        });

        put("list_usage", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.msg = sb.append("<pre>").append(e.msg).append("</pre>").toString();
                }
                e.bg_color = colorScheme.noticeBackgroundColor;
            }
        });

        put("time", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                e.bg_color = colorScheme.statusBackgroundColor;
                if(event != null) {
                    sb.append("<pre>").append(event.getString("time_string"));
                    if (event.has("time_stamp") && event.getString("time_stamp").length() > 0)
                        sb.append(" (").append(event.getString("time_stamp")).append(")");
                    sb.append(" — <b>").append(event.getString("time_server")).append("</b></pre>");
                    e.msg = sb.toString();
                    e.linkify = false;
                }
            }
        });

        put("watch_status", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                e.bg_color = colorScheme.statusBackgroundColor;
                if(event != null) {
                    e.from = event.getString("watch_nick");
                    e.msg = sb.append("<pre>").append(e.msg).append(" (").append(event.getString("username")).append("@").append(event.getString("userhost")).append(")</pre>").toString();
                    e.linkify = false;
                }
            }
        });

        put("sqline_nick", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                e.bg_color = colorScheme.statusBackgroundColor;
                if(event != null) {
                    e.from = event.getString("charset");
                    e.msg = sb.append("<pre>").append(e.msg).append("</pre>").toString();
                }
            }
        });

        put("you_parted_channel", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                e.row_type = MessageViewFragment.ROW_SOCKETCLOSED;
            }
        });


        put("user_chghost", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.msg = sb.append("changed host: ").append(event.getString("user")).append("@").append(event.getString("userhost")).append(" → ").append(event.getString("from_name")).append("@").append(event.getString("from_host")).toString();
                }
                e.color = colorScheme.collapsedRowTextColor;
                e.linkify = false;
            }
        });

        put("loaded_module", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                e.bg_color = colorScheme.statusBackgroundColor;
                e.linkify = false;
                if(event != null) {
                    e.msg = sb.append("<pre><b>").append(event.getString("module")).append("</b> ").append(e.msg).append("</pre>").toString();
                }
            }
        });

        put("unloaded_module", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                e.bg_color = colorScheme.statusBackgroundColor;
                e.linkify = false;
                if(event != null) {
                    e.msg = sb.append("<pre><b>").append(event.getString("module")).append("</b> ").append(e.msg).append("</pre>").toString();
                }
            }
        });

        put("invite_notify", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.msg = sb.append("invited ").append(event.getString("target")).append(" to join ").append(event.getString("channel")).toString();
                }
            }
        });

        put("channel_name_change", new Formatter() {
            @Override
            public void format(IRCCloudJSONObject event, Event e, StringBuilder sb) {
                if(event != null) {
                    e.msg = sb.append("renamed the channel: ").append(event.getString("old_name")).append(" → <b>").append(event.getString("new_name")).append("</b>").toString();
                }
                e.color = colorScheme.collapsedRowTextColor;
            }
        });

    }};

    private final StringBuilder eventStringBuilder = new StringBuilder(1024);
    public Event addEvent(IRCCloudJSONObject event) {
        synchronized (events) {
            if (!events.containsKey(event.bid()))
                events.put(event.bid(), new TreeMap<Long, Event>());

            Event e = getEvent(event.eid(), event.bid());
            if (e == null) {
                e = new Event();
                events.get(event.bid()).put(event.eid(), e);
            }
            e.cid = event.cid();
            e.bid = event.bid();
            e.eid = event.eid();
            e.type = event.type();
            e.color = colorScheme.messageTextColor;
            e.bg_color = colorScheme.contentBackgroundColor;
            e.msg = event.getString("msg");
            if(e.msg != null)
                e.msg = Normalizer.normalize(e.msg, Normalizer.Form.NFC);
            e.hostmask = event.getString("hostmask");
            if(event.has("from") && event.has("display_name"))
                e.from = event.getString("display_name");
            else
                e.from = event.getString("from");
            e.from_nick = event.getString("from");
            e.from_mode = event.getString("from_mode");
            e.from_realname = event.getString("from_realname");
            if(e.from_realname != null && e.from_realname.length() > 0 && e.from_realname.equals(e.from))
                e.from_realname = null;
            if(event.has("server_time"))
                e.server_time = event.getLong("server_time");
            else
                e.server_time = 0;

            e.chan = event.getString("chan");
            if (event.has("newnick"))
                e.nick = event.getString("newnick");
            else if (event.has("nick"))
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
            e.ops = event.getJsonNode("ops");
            e.entities = event.getJsonNode("entities");
            e.avatar = event.getString("avatar");
            e.avatar_url = event.getString("avatar_url");

            if((e.type.equals("buffer_msg") || e.type.equals("buffer_me_msg")) && (e.from == null || e.from.length() == 0))
                e.from = e.from_nick = e.server;

            if((e.from == null || e.from.length() == 0) && e.nick != null && e.nick.length() > 0 && event.has("display_name"))
                e.nick = event.getString("display_name");

            if (event.has("reqid"))
                e.reqid = event.getInt("reqid");
            else
                e.reqid = -1;

            if (e.from != null)
                e.from = TextUtils.htmlEncode(e.from);

            if (e.msg != null) {
                e.msg = TextUtils.htmlEncode(e.msg).replace("  ", "&nbsp; ");
                if(e.msg.startsWith(" "))
                    e.msg = "&nbsp;" + e.msg.substring(1);
            }

            synchronized (eventStringBuilder) {
                Formatter f = formatterMap.get(e.type);
                if (f != null)
                    f.format(event, e, eventStringBuilder);
                eventStringBuilder.setLength(0);
                if (event.has("value") && !event.type().startsWith("cap_")) {
                    e.msg = eventStringBuilder.append("<pre>").append(event.getString("value")).append(" ").append(e.msg).append("</pre>").toString();
                    eventStringBuilder.setLength(0);
                }
            }

            if (e.highlight)
                e.bg_color = colorScheme.highlightBackgroundColor;

            if (e.self && !e.type.equals("notice"))
                e.bg_color = colorScheme.selfBackgroundColor;

            if(e.from_nick == null && e.from != null && e.from.length() > 0)
                e.from_nick = e.from;

            return e;
        }
    }

    public int getSizeOfBuffer(int bid) {
        synchronized (events) {
            load(bid);
            TreeMap<Long, Event> buffer = events.get(bid);
            if(buffer != null)
                return buffer.size();
        }
        return 0;
    }

    public Event getEvent(long eid, int bid) {
        synchronized (events) {
            TreeMap<Long, Event> buffer = events.get(bid);
            if(buffer != null)
                return buffer.get(eid);
        }
        return null;
    }

    public void deleteEvent(long eid, int bid) {
        synchronized (events) {
            TreeMap<Long, Event> buffer = events.get(bid);
            if(buffer != null)
                buffer.remove(eid);
        }
        //new Delete().from(Event.class).where(Condition.column(Event$Table.BID).is(bid)).and(Condition.column(Event$Table.EID).is(eid)).queryClose();
    }

    public void deleteEventsForBuffer(int bid) {
        synchronized (events) {
            if (events.containsKey(bid) && events.get(bid) != null)
                events.remove(bid);
        }
        //new Delete().from(Event.class).where(Condition.column(Event$Table.BID).is(bid)).queryClose();
    }

    public TreeMap<Long, Event> getEventsForBuffer(int bid) {
        synchronized (events) {
            load(bid);
            if (events.containsKey(bid) && events.get(bid) != null) {
                return events.get(bid);
            }
        }
        return null;
    }

    public Long lastEidForBuffer(int bid) {
        synchronized (events) {
            if (events.containsKey(bid) && events.get(bid) != null && events.get(bid).size() > 0) {
                Long[] eids = events.get(bid).keySet().toArray(new Long[events.get(bid).keySet().size()]);
                if (eids.length > 0) {
                    int i = eids.length - 1;
                    long eid = eids[i];
                    while(events.get(bid).get(eid).pending && i > 0) {
                        eid = eids[--i];
                    }
                    return eid;
                }
            /*} else {
                Event e = new Select().from(Event.class).where(Condition.column(Event$Table.BID).is(bid)).orderBy(true, Event$Table.EID).limit(1).querySingle();
                if(e != null) {
                    return e.eid;
                }*/
            }
        }
        return 0L;
    }

    public void pruneEvents(int bid) {
        synchronized (events) {
            load(bid);
            TreeMap<Long, Event> e = events.get(bid);
            while (e != null && e.size() > 50 && e.firstKey() != null) {
                e.remove(e.firstKey());
            }
            /*if(e != null)
                new Delete().from(Event.class).where(Condition.column(Event$Table.BID).is(bid)).and(Condition.column(Event$Table.EID).lessThan(e.firstKey())).queryClose();*/
        }
    }

    public synchronized void clearCaches() {
        synchronized (events) {
            for (int bid : events.keySet()) {
                if (events.containsKey(bid) && events.get(bid) != null) {
                    for (Event e : events.get(bid).values()) {
                        e.color = colorScheme.messageTextColor;
                        e.bg_color = colorScheme.contentBackgroundColor;
                        e.timestamp = null;
                        e.html = null;
                        e.formatted = null;
                        e.formatted_nick = null;
                        e.formatted_realname = null;
                        synchronized (eventStringBuilder) {
                            Formatter f = formatterMap.get(e.type);
                            if (f != null)
                                f.format(null, e, eventStringBuilder);
                            eventStringBuilder.setLength(0);
                        }
                        if (e.highlight)
                            e.bg_color = colorScheme.highlightBackgroundColor;

                        if (e.self)
                            e.bg_color = colorScheme.selfBackgroundColor;
                    }
                }
            }
        }
    }

    public synchronized Event findPendingEventForReqid(int bid, int reqid) {
        synchronized (events) {
            load(bid);
            for (Event e : events.get(bid).values()) {
                if(e.reqid == reqid && (e.pending || e.failed))
                    return e;
            }
        }
        return null;
    }


    public synchronized void clearPendingEvents(int bid) {
        Event[] i;
        synchronized (events) {
            i = events.get(bid).values().toArray(new Event[events.get(bid).values().size()]);
        }
        for (Event e : i) {
            if(e.pending || e.failed) {
                if(e.expiration_timer != null) {
                    try {
                        e.expiration_timer.cancel();
                    } catch (Exception e1) {
                        //Timer already cancelled
                    }
                    e.expiration_timer = null;
                }
                deleteEvent(e.eid, e.bid);
            }
        }
    }

    private static final HashMap<String, String> REASONS = new HashMap<String, String>() {{
        put("pool_lost", "Connection pool failed");
        put("no_pool", "No available connection pools");
        put("enetdown", "Network down");
        put("etimedout", "Timed out");
        put("timeout", "Timed out");
        put("ehostunreach", "Host unreachable");
        put("econnrefused", "Connection refused");
        put("nxdomain", "Invalid hostname");
        put("server_ping_timeout", "PING timeout");
        put("ssl_certificate_error", "SSL certificate error");
        put("ssl_error", "SSL error");
        put("crash", "Connection crashed");
        put("networks", "You've exceeded the connection limit for free accounts.");
        put("passworded_servers", "You can't connect to passworded servers with free accounts.");
        put("unverified", "You can’t connect to external servers until you confirm your email address.");
    }};

    private static final HashMap<String, HashMap<String, String>> SSL_REASONS = new HashMap<String, HashMap<String, String>>() {{
        put("bad_cert", new HashMap<String, String>() {{
            put("unknown_ca", "Unknown certificate authority");
            put("selfsigned_peer", "Self signed certificate");
            put("cert_expired", "Certificate expired");
            put("invalid_issuer", "Invalid certificate issuer");
            put("invalid_signature", "Invalid certificate signature");
            put("name_not_permitted", "Invalid certificate alternative hostname");
            put("missing_basic_constraint", "Missing certificate basic contraints");
            put("invalid_key_usage", "Invalid certificate key usage");
        }});
        put("ssl_verify_hostname", new HashMap<String, String>() {{
            put("unable_to_match_altnames", "Certificate hostname mismatch");
            put("unable_to_match_common_name", "Certificate hostname mismatch");
            put("unable_to_decode_common_name", "Invalid certificate hostname");
        }});
    }};

    public static String reason(String reason) {
        if(reason != null && reason.length() > 0 && REASONS.containsKey(reason))
            return REASONS.get(reason);
        return reason;
    }

    public static String SSLreason(JsonNode info) {
        if(SSL_REASONS.containsKey(info.get("type").asText()) && SSL_REASONS.get(info.get("type").asText()).containsKey(info.get("error").asText())) {
            return SSL_REASONS.get(info.get("type").asText()).get(info.get("error").asText());
        }

        return info.get("type").asText() + ": " + info.get("error").asText();
    }
}
