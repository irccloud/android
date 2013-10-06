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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.json.JSONException;

public class CollapsedEventsList {
	public static final int TYPE_JOIN = 0;
	public static final int TYPE_PART = 1;
	public static final int TYPE_QUIT = 2;
	public static final int TYPE_MODE = 3;
	public static final int TYPE_POPIN = 4;
	public static final int TYPE_POPOUT = 5;
	public static final int TYPE_NICKCHANGE = 6;

	public static final int MODE_OWNER = 1;
	public static final int MODE_DEOWNER = 2;
	public static final int MODE_ADMIN = 3;
	public static final int MODE_DEADMIN = 4;
	public static final int MODE_OP = 5;
	public static final int MODE_DEOP = 6;
	public static final int MODE_HALFOP = 7;
	public static final int MODE_DEHALFOP = 8;
	public static final int MODE_VOICE = 9;
	public static final int MODE_DEVOICE = 10;
	
	public class CollapsedEvent {
		int type;
		int mode = 0;
		String nick;
		String old_nick;
		String hostmask;
		String msg;
		String from_mode;
		String target_mode;
        String chan;
		
		public String toString() {
			return "{type: " + type + ", nick: " + nick + ", old_nick: " + old_nick + ", hostmask: " + hostmask + ", msg: " + msg + "}";
		}
	}
	
	public class comparator implements Comparator<CollapsedEvent> {
		public int compare(CollapsedEvent e1, CollapsedEvent e2) {
			if(e1.type == e2.type)
				return e1.nick.compareToIgnoreCase(e2.nick);
			else if(e1.type > e2.type)
				return 1;
			else
				return -1;
		}
	}
	
	private ArrayList<CollapsedEvent> data = new ArrayList<CollapsedEvent>();

    public boolean addEvent(EventsDataSource.Event event) {
        String type = event.type;
        if(type.startsWith("you_"))
            type = type.substring(4);

        if(type.equalsIgnoreCase("joined_channel")) {
            addEvent(CollapsedEventsList.TYPE_JOIN, event.nick, null, event.hostmask, event.from_mode, null, event.chan);
        } else if(type.equalsIgnoreCase("parted_channel")) {
            addEvent(CollapsedEventsList.TYPE_PART, event.nick, null, event.hostmask, event.from_mode, event.msg, event.chan);
        } else if(type.equalsIgnoreCase("quit")) {
            addEvent(CollapsedEventsList.TYPE_QUIT, event.nick, null, event.hostmask, event.from_mode, event.msg, event.chan);
        } else if(type.equalsIgnoreCase("nickchange")) {
            addEvent(CollapsedEventsList.TYPE_NICKCHANGE, event.nick, event.old_nick, null, event.from_mode, null, event.chan);
        } else if(type.equalsIgnoreCase("user_channel_mode")) {
            String from = event.from;
            String from_mode = event.from_mode;
            if(from == null || from.length() == 0) {
                from = event.server;
                if(from != null && from.length() > 0)
                    from_mode = "__the_server__";
            }
            JsonObject ops = event.ops;
            if(ops != null) {
                JsonArray add = ops.getAsJsonArray("add");
                for(int i = 0; i < add.size(); i++) {
                    JsonObject op = add.get(i).getAsJsonObject();
                    if(op.get("mode").getAsString().equalsIgnoreCase("q"))
                        addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), from, event.hostmask, from_mode, null, CollapsedEventsList.MODE_OWNER, event.target_mode, event.chan);
                    else if(op.get("mode").getAsString().equalsIgnoreCase("a"))
                        addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), from, event.hostmask, from_mode, null, CollapsedEventsList.MODE_ADMIN, event.target_mode, event.chan);
                    else if(op.get("mode").getAsString().equalsIgnoreCase("o"))
                        addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), from, event.hostmask, from_mode, null, CollapsedEventsList.MODE_OP, event.target_mode, event.chan);
                    else if(op.get("mode").getAsString().equalsIgnoreCase("h"))
                        addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), from, event.hostmask, from_mode, null, CollapsedEventsList.MODE_HALFOP, event.target_mode, event.chan);
                    else if(op.get("mode").getAsString().equalsIgnoreCase("v"))
                        addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), from, event.hostmask, from_mode, null, CollapsedEventsList.MODE_VOICE, event.target_mode, event.chan);
                    else
                        return false;
                }
                JsonArray remove = ops.getAsJsonArray("remove");
                for(int i = 0; i < remove.size(); i++) {
                    JsonObject op = remove.get(i).getAsJsonObject();
                    if(op.get("mode").getAsString().equalsIgnoreCase("q"))
                        addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), from, event.hostmask, from_mode, null, CollapsedEventsList.MODE_DEOWNER, event.target_mode, event.chan);
                    else if(op.get("mode").getAsString().equalsIgnoreCase("a"))
                        addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), from, event.hostmask, from_mode, null, CollapsedEventsList.MODE_DEADMIN, event.target_mode, event.chan);
                    else if(op.get("mode").getAsString().equalsIgnoreCase("o"))
                        addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), from, event.hostmask, from_mode, null, CollapsedEventsList.MODE_DEOP, event.target_mode, event.chan);
                    else if(op.get("mode").getAsString().equalsIgnoreCase("h"))
                        addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), from, event.hostmask, from_mode, null, CollapsedEventsList.MODE_DEHALFOP, event.target_mode, event.chan);
                    else if(op.get("mode").getAsString().equalsIgnoreCase("v"))
                        addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), from, event.hostmask, from_mode, null, CollapsedEventsList.MODE_DEVOICE, event.target_mode, event.chan);
                    else
                        return false;
                }
            }
        }
        return true;
    }

	public void addEvent(int type, String nick, String old_nick, String hostmask, String from_mode, String msg, String chan) {
		addEvent(type, nick, old_nick, hostmask, from_mode, msg, 0, null, chan);
	}
	
	public void addEvent(int type, String nick, String old_nick, String hostmask, String from_mode, String msg, int mode, String target_mode, String chan) {
		CollapsedEvent e = null;
		
		if(type < TYPE_NICKCHANGE) {
			if(old_nick != null && type != TYPE_MODE) {
				e = findEvent(old_nick, chan);
				if(e != null)
					e.nick = nick;
			}
			
			if(e == null)
				e = findEvent(nick, chan);
			
			if(e == null) {
				e = new CollapsedEvent();
				e.type = type;
				e.nick = nick;
				e.old_nick = old_nick;
				e.hostmask = hostmask;
				e.from_mode = from_mode;
				e.msg = msg;
				e.mode = mode;
				e.target_mode = target_mode;
                e.chan = chan;
				data.add(e);
			} else {
				if(e.type == TYPE_MODE) {
					e.type = type;
					e.msg = msg;
					e.old_nick = old_nick;
					if(from_mode != null)
						e.from_mode = from_mode;
					if(target_mode != null)
						e.target_mode = target_mode;
				} else if(type == TYPE_MODE) {
					e.from_mode = target_mode;
				} else if(e.type == type) {
				} else if(type == TYPE_JOIN) {
					e.type = TYPE_POPOUT;
					e.from_mode = from_mode;
				} else if(e.type == TYPE_POPOUT) {
					e.type = type;
				} else {
					e.type = TYPE_POPIN;
				}
				if(mode > 0)
					e.mode = mode;
			}
		} else {
			if(type == TYPE_NICKCHANGE) {
				for(CollapsedEvent e1 : data) {
					if(e1.type == TYPE_NICKCHANGE && e1.nick.equalsIgnoreCase(old_nick)) {
						if(e1.old_nick.equalsIgnoreCase(nick))
							data.remove(e1);
						else
							e1.nick = nick;
						return;
					}
					if((e1.type == TYPE_JOIN || e1.type == TYPE_POPOUT) && e1.nick.equalsIgnoreCase(old_nick)) {
						e1.old_nick = old_nick;
						e1.nick = nick;
						return;
					}
					if((e1.type == TYPE_QUIT || e1.type == TYPE_PART) && e1.nick.equalsIgnoreCase(nick)) {
						e1.type = TYPE_POPOUT;
						for(CollapsedEvent e2 : data) {
							if(e2.type == TYPE_JOIN && e2.nick.equalsIgnoreCase(old_nick)) {
								data.remove(e2);
								break;
							}
						}
						return;
					}
				}
				e = new CollapsedEvent();
				e.type = type;
				e.nick = nick;
				e.old_nick = old_nick;
				e.hostmask = hostmask;
				e.msg = msg;
                e.chan = chan;
				data.add(e);
			} else {
				e = new CollapsedEvent();
				e.type = type;
				e.nick = nick;
				e.old_nick = old_nick;
				e.hostmask = hostmask;
				e.msg = msg;
                e.chan = chan;
				data.add(e);
			}
		}
	}
	
	public CollapsedEvent findEvent(String nick, String chan) {
		for(CollapsedEvent e : data) {
			if(e.nick.equalsIgnoreCase(nick) && (e.chan == null || e.chan.equalsIgnoreCase(chan)))
				return e;
		}
		return null;
	}
	
	public String formatNick(String nick, String from_mode) {
		StringBuilder output = new StringBuilder();
		boolean showSymbol = false;
		try {
			if(NetworkConnection.getInstance().getUserInfo() != null && NetworkConnection.getInstance().getUserInfo().prefs != null)
			showSymbol = NetworkConnection.getInstance().getUserInfo().prefs.getBoolean("mode-showsymbol");
		} catch (JSONException e) {
		}
		String mode = "";
		if(from_mode != null) {
			mode = from_mode;
		}
		if(mode != null && mode.length() > 0) {
			if(showSymbol) {
				if(mode.contains("q"))
					output.append("\u0004E7AA00\u0002~\u000f ");
				else if(mode.contains("a"))
					output.append("\u00046500A5\u0002&amp;\u000f ");
				else if(mode.contains("o"))
					output.append("\u0004BA1719\u0002@\u000f ");
				else if(mode.contains("h"))
					output.append("\u0004B55900\u0002%\u000f ");
				else if(mode.contains("v"))
					output.append("\u000425B100\u0002+\u000f ");
			} else {
				if(mode.contains("q"))
					output.append("\u0004E7AA00\u0002•\u000f ");
				else if(mode.contains("a"))
					output.append("\u00046500A5\u0002•\u000f ");
				else if(mode.contains("o"))
					output.append("\u0004BA1719\u0002•\u000f ");
				else if(mode.contains("h"))
					output.append("\u0004B55900\u0002•\u000f ");
				else if(mode.contains("v"))
					output.append("\u000425B100\u0002•\u000f ");
			}
		}
		
		output.append(nick);
		return output.toString();
	}
	
	private String was(CollapsedEvent e) {
		StringBuilder was = new StringBuilder();
		
		if(e.old_nick != null && e.type != TYPE_MODE)
			was.append("was ").append(e.old_nick);
		if(e.mode > 0) {
			if(was.length() > 0)
				was.append("; ");
			switch(e.mode) {
			case MODE_OWNER:
				was.append("promoted to owner");
				break;
			case MODE_DEOWNER:
				was.append("demoted from owner");
				break;
			case MODE_ADMIN:
				was.append("promoted to admin");
				break;
			case MODE_DEADMIN:
				was.append("demoted from admin");
				break;
			case MODE_OP:
				was.append("opped");
				break;
			case MODE_DEOP:
				was.append("de-opped");
				break;
			case MODE_HALFOP:
				was.append("halfopped");
				break;
			case MODE_DEHALFOP:
				was.append("de-halfopped");
				break;
			case MODE_VOICE:
				was.append("voiced");
				break;
			case MODE_DEVOICE:
				was.append("de-voiced");
				break;
			}
		}
		
		if(was.length() > 0)
			was.insert(0, " (").append(")");
		
		return was.toString();
	}
	
	public String getCollapsedMessage(boolean showChan) {
		StringBuilder message = new StringBuilder();

		if(data.size() == 0)
			return null;
		
		if(data.size() == 1) {
			CollapsedEvent e = data.get(0);
			switch(e.type) {
			case TYPE_MODE:
				message.append("<b>").append(formatNick(e.nick, e.target_mode)).append("</b> was ");
				switch(e.mode) {
					case MODE_OWNER:
						message.append("promoted to owner (\u0004E7AA00+q\u000f)");
						break;
					case MODE_DEOWNER:
						message.append("demoted from owner (\u0004E7AA00-q\u000f)");
						break;
					case MODE_ADMIN:
						message.append("promoted to admin (\u00046500A5+a\u000f)");
						break;
					case MODE_DEADMIN:
						message.append("demoted from admin (\u00046500A5-a\u000f)");
						break;
					case MODE_OP:
						message.append("opped (\u0004BA1719+o\u000f)");
						break;
					case MODE_DEOP:
						message.append("de-opped (\u0004BA1719-o\u000f)");
						break;
					case MODE_HALFOP:
						message.append("halfopped (\u0004B55900+h\u000f)");
						break;
					case MODE_DEHALFOP:
						message.append("de-halfopped (\u0004B55900-h\u000f)");
						break;
					case MODE_VOICE:
						message.append("voiced (\u000425B100+v\u000f)");
						break;
					case MODE_DEVOICE:
						message.append("de-voiced (\u000425B100-v\u000f)");
						break;
				}
				if(e.old_nick != null) {
                    if(e.from_mode.equalsIgnoreCase("__the_server__"))
    					message.append(" by the server <b>").append(e.old_nick).append("</b>");
                    else
                        message.append(" by ").append(formatNick(e.old_nick, e.from_mode));
                }
				break;
			case TYPE_JOIN:
	    		message.append("→ <b>").append(formatNick(e.nick, e.from_mode)).append("</b>").append(was(e));
	    		message.append(" joined");
                if(showChan)
                    message.append(" " + e.chan);
                message.append(" (").append(e.hostmask + ")");
				break;
			case TYPE_PART:
	    		message.append("← <b>").append(formatNick(e.nick, e.from_mode)).append("</b>").append(was(e));
	    		message.append(" left");
                if(showChan)
                    message.append(" " + e.chan);
                message.append(" (").append(e.hostmask + ")");
	    		if(e.msg != null && e.msg.length() > 0)
	    			message.append(": ").append(e.msg);
				break;
			case TYPE_QUIT:
	    		message.append("⇐ <b>").append(formatNick(e.nick, e.from_mode)).append("</b>").append(was(e));
	    		if(e.hostmask != null)
	    			message.append(" quit (").append(e.hostmask).append(") ").append(e.msg);
	    		else
	    			message.append(" quit: ").append(e.msg);
				break;
			case TYPE_NICKCHANGE:
	    		message.append(e.old_nick).append(" → <b>").append(formatNick(e.nick, e.from_mode)).append("</b>");
				break;
			case TYPE_POPIN:
	    		message.append("↔ <b>").append(formatNick(e.nick, e.from_mode)).append("</b>").append(was(e));
	    		message.append(" popped in");
                if(showChan)
                    message.append(" " + e.chan);
	    		break;
			case TYPE_POPOUT:
	    		message.append("↔ <b>").append(formatNick(e.nick, e.from_mode)).append("</b>").append(was(e));
	    		message.append(" nipped out");
                if(showChan)
                    message.append(" " + e.chan);
	    		break;
			}
		} else {
			Collections.sort(data, new comparator());
			Iterator<CollapsedEvent> i = data.iterator();
			CollapsedEvent last = null;
			CollapsedEvent next = i.next();
			CollapsedEvent e;
			int groupcount = 0;
			
			while(next != null) {
				e = next;
				
				if(i.hasNext())
					next = i.next();
				else
					next = null;
				
				if(message.length() > 0 && e.type < TYPE_NICKCHANGE && ((next == null || next.type != e.type) && last != null && last.type == e.type)) {
					if(groupcount == 1)
						message.delete(message.length() - 2, message.length()).append(" ");
					message.append("and ");
				}
				
				if(last == null || last.type != e.type) {
					switch(e.type) {
					case TYPE_MODE:
						if(message.length() > 0)
							message.append("• ");
						message.append("mode: ");
						break;
					case TYPE_JOIN:
						message.append("→ ");
						break;
					case TYPE_PART:
						message.append("← ");
						break;
					case TYPE_QUIT:
						message.append("⇐ ");
						break;
					case TYPE_NICKCHANGE:
						if(message.length() > 0)
							message.append("• ");
						break;
					case TYPE_POPIN:
					case TYPE_POPOUT:
						message.append("↔ ");
						break;
					}
				}

				if(e.type == TYPE_NICKCHANGE) {
					message.append(e.old_nick).append(" → <b>").append(formatNick(e.nick, e.from_mode)).append("</b>");
					String old_nick = e.old_nick;
					e.old_nick = null;
					message.append(was(e));
					e.old_nick = old_nick;
				} else if(!showChan) {
					message.append("<b>").append(formatNick(e.nick, (e.type == TYPE_MODE)?e.target_mode:e.from_mode)).append("</b>").append(was(e));
				}
				
				if((next == null || next.type != e.type) && !showChan) {
					switch(e.type) {
					case TYPE_JOIN:
						message.append(" joined");
						break;
					case TYPE_PART:
						message.append(" left");
						break;
					case TYPE_QUIT:
						message.append(" quit");
						break;
					case TYPE_POPIN:
						message.append(" popped in");
						break;
					case TYPE_POPOUT:
						message.append(" nipped out");
						break;
					}
				} else if(showChan) {
                    if(groupcount == 0) {
                        message.append("<b>").append(formatNick(e.nick, (e.type == TYPE_MODE)?e.target_mode:e.from_mode)).append("</b>").append(was(e));
                        switch(e.type) {
                            case TYPE_JOIN:
                                message.append(" joined ");
                                break;
                            case TYPE_PART:
                                message.append(" left ");
                                break;
                            case TYPE_QUIT:
                                message.append(" quit ");
                                break;
                            case TYPE_POPIN:
                                message.append(" popped in ");
                                break;
                            case TYPE_POPOUT:
                                message.append(" nipped out ");
                                break;
                        }
                    }
                    if(e.type != TYPE_QUIT)
                        message.append(e.chan);
                }

				if(next != null && next.type == e.type) {
					message.append(", ");
					groupcount++;
				} else if(next != null) {
					message.append(" ");
					groupcount = 0;
				}
				
				last = e;
			}
		}
		return message.toString();
	}
	
	public void clear() {
		data.clear();
	}
}
