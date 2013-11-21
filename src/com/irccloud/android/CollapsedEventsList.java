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
    public static final int TYPE_NETSPLIT = -1;
	public static final int TYPE_JOIN = 0;
	public static final int TYPE_PART = 1;
	public static final int TYPE_QUIT = 2;
	public static final int TYPE_MODE = 3;
	public static final int TYPE_POPIN = 4;
	public static final int TYPE_POPOUT = 5;
	public static final int TYPE_NICKCHANGE = 6;

	public static final int MODE_OWNER = 0;
	public static final int MODE_ADMIN = 1;
	public static final int MODE_OP = 2;
	public static final int MODE_HALFOP = 3;
	public static final int MODE_VOICE = 4;

    public static final int MODE_DEOWNER = 5;
    public static final int MODE_DEADMIN = 6;
    public static final int MODE_DEOP = 7;
    public static final int MODE_DEHALFOP = 8;
    public static final int MODE_DEVOICE = 9;

    public static final int MODE_COUNT = 10;

	public class CollapsedEvent {
		int type;
        boolean modes[] = new boolean[MODE_COUNT];
		String nick;
		String old_nick;
		String hostmask;
		String msg;
		String from_mode;
        String from_nick;
		String target_mode;
        String chan;
        boolean netsplit;
		
		public String toString() {
			return "{type: " + type + ", nick: " + nick + ", old_nick: " + old_nick + ", hostmask: " + hostmask + ", msg: " + msg + "netsplit: " + netsplit + "}";
		}

        public int modeCount() {
            int count = 0;
            for(int i = 0; i < MODE_COUNT; i++) {
                if(modes[i])
                    count++;
            }
            return count;
        }

        public boolean addMode(String mode) {
            if(mode.equalsIgnoreCase("q")) {
                if(modes[MODE_DEOWNER])
                    modes[MODE_DEOWNER] = false;
                else
                    modes[MODE_OWNER] = true;
            } else if(mode.equalsIgnoreCase("a")) {
                if(modes[MODE_DEADMIN])
                    modes[MODE_DEADMIN] = false;
                else
                    modes[MODE_ADMIN] = true;
            } else if(mode.equalsIgnoreCase("o")) {
                if(modes[MODE_DEOP])
                    modes[MODE_DEOP] = false;
                else
                    modes[MODE_OP] = true;
            } else if(mode.equalsIgnoreCase("h")) {
                if(modes[MODE_DEHALFOP])
                    modes[MODE_DEHALFOP] = false;
                else
                    modes[MODE_HALFOP] = true;
            } else if(mode.equalsIgnoreCase("v")) {
                if(modes[MODE_DEVOICE])
                    modes[MODE_DEVOICE] = false;
                else
                    modes[MODE_VOICE] = true;
            } else {
                return false;
            }
            if(modeCount() == 0)
                return addMode(mode);
            return true;
        }

        public boolean removeMode(String mode) {
            if(mode.equalsIgnoreCase("q")) {
                if(modes[MODE_OWNER])
                    modes[MODE_OWNER] = false;
                else
                    modes[MODE_DEOWNER] = true;
            } else if(mode.equalsIgnoreCase("a")) {
                if(modes[MODE_ADMIN])
                    modes[MODE_ADMIN] = false;
                else
                    modes[MODE_DEADMIN] = true;
            } else if(mode.equalsIgnoreCase("o")) {
                if(modes[MODE_OP])
                    modes[MODE_OP] = false;
                else
                    modes[MODE_DEOP] = true;
            } else if(mode.equalsIgnoreCase("h")) {
                if(modes[MODE_HALFOP])
                    modes[MODE_HALFOP] = false;
                else
                    modes[MODE_DEHALFOP] = true;
            } else if(mode.equalsIgnoreCase("v")) {
                if(modes[MODE_VOICE])
                    modes[MODE_VOICE] = false;
                else
                    modes[MODE_DEVOICE] = true;
            } else {
                return false;
            }
            if(modeCount() == 0)
                return addMode(mode);
            return true;
        }

        public String getModes(boolean showSymbol) {
            final String[] mode_msgs = {
                "promoted to owner",
                "promoted to admin",
                "opped",
                "halfopped",
                "voiced",
                "demoted from owner",
                "demoted from admin",
                "de-opped",
                "de-halfopped",
                "de-voiced",
            };
            final String[] mode_modes = {
                "+q", "+a", "+o", "+h", "+v", "-q", "-a", "-o", "-h", "-v"
            };
            final String[] mode_colors = {
                "E7AA00",
                "6500A5",
                "BA1719",
                "B55900",
                "25B100"
            };

            String output = null;
            if(modeCount() > 0) {
                output = "";

                for(int i = 0; i < MODE_COUNT; i++) {
                    if(modes[i]) {
                        if(output.length() > 0)
                            output += ", ";
                        output += mode_msgs[i];
                        if(showSymbol) {
                            output += " (\u0004" + mode_colors[i%5] + mode_modes[i] + "\u000f)";
                        }
                    }
                }
            }

            return output;
        }
    }
	
	public class comparator implements Comparator<CollapsedEvent> {
		public int compare(CollapsedEvent e1, CollapsedEvent e2) {
			if(e1.type == e2.type) {
                if(e1.type == TYPE_NETSPLIT)
    				return e1.msg.compareToIgnoreCase(e2.msg);
                else
                    return e1.nick.compareToIgnoreCase(e2.nick);
            } else if(e1.type > e2.type) {
				return 1;
            } else {
				return -1;
            }
		}
	}
	
	private ArrayList<CollapsedEvent> data = new ArrayList<CollapsedEvent>();

    public String toString() {
        String out = "CollapsedEventsList {\n";
        for(int i = 0; i < data.size(); i++) {
            out += "\t" + data.get(i).toString() + "\n";
        }
        out += "}";
        return out;
    }

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
            JsonObject ops = event.ops;
            if(ops != null) {
                CollapsedEvent e = findEvent(event.nick, event.chan);
                if(e == null) {
                    e = new CollapsedEvent();
                    e.type = TYPE_MODE;
                    e.hostmask = event.hostmask;
                    e.target_mode = event.target_mode;
                    e.nick = event.nick;
                    e.chan = event.chan;
                    data.add(e);
                }
                JsonArray add = ops.getAsJsonArray("add");
                for(int i = 0; i < add.size(); i++) {
                    JsonObject op = add.get(i).getAsJsonObject();
                    if(!e.addMode(op.get("mode").getAsString()))
                        return false;
                    if(e.type == TYPE_MODE) {
                        if(event.from != null && event.from.length() > 0) {
                            e.from_nick = event.from;
                            e.from_mode = event.from_mode;
                        } else {
                            e.from_nick = event.server;
                            e.from_mode = "__the_server__";
                        }
                    } else {
                        e.from_mode = event.target_mode;
                    }
                }
                JsonArray remove = ops.getAsJsonArray("remove");
                for(int i = 0; i < remove.size(); i++) {
                    JsonObject op = remove.get(i).getAsJsonObject();
                    if(!e.removeMode(op.get("mode").getAsString()))
                        return false;
                    if(e.type == TYPE_MODE) {
                        if(event.from != null && event.from.length() > 0) {
                            e.from_nick = event.from;
                            e.from_mode = event.from_mode;
                        } else {
                            e.from_nick = event.server;
                            e.from_mode = "__the_server__";
                        }
                    } else {
                        e.from_mode = event.target_mode;
                    }
                }
            }
        }
        return true;
    }

	public void addEvent(int type, String nick, String old_nick, String hostmask, String from_mode, String msg, String chan) {
		addEvent(type, nick, old_nick, hostmask, from_mode, msg, null, chan);
	}
	
	public void addEvent(int type, String nick, String old_nick, String hostmask, String from_mode, String msg, String target_mode, String chan) {
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

        if(e != null && type == TYPE_QUIT && msg != null) {
            if(msg.matches("(?:[^\\s:\\/.]+\\.)+[a-z]{2,} (?:[^\\s:\\/.]+\\.)+[a-z]{2,}")) {
                String[] parts = msg.split(" ");
                if(parts.length > 1 && !parts[0].equals(parts[1])) {
                    e.netsplit = true;
                    boolean found = false;
                    for(CollapsedEvent c : data) {
                        if(c.type == TYPE_NETSPLIT && c.msg.equalsIgnoreCase(msg))
                            found = true;
                    }
                    if(!found) {
                        CollapsedEvent c = new CollapsedEvent();
                        c.type = TYPE_NETSPLIT;
                        c.msg = msg;
                        data.add(c);
                    }
                }
            }
        }
	}
	
	public CollapsedEvent findEvent(String nick, String chan) {
		for(CollapsedEvent e : data) {
			if(e.nick != null && (e.nick.equalsIgnoreCase(nick) && (e.chan == null || e.chan.equalsIgnoreCase(chan))))
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
		String modes = e.getModes(false);

		if(e.old_nick != null && e.type != TYPE_MODE)
			was.append("was ").append(e.old_nick);
		if(modes != null && modes.length() > 0) {
			if(was.length() > 0)
				was.append("; ");
            was.append(modes);
		}
		
		if(was.length() > 0)
			was.insert(0, " (").append(")");
		
		return was.toString();
	}
	
	public String getCollapsedMessage(boolean showChan) {
		StringBuilder message = new StringBuilder();

		if(data.size() == 0)
			return null;
		
		if(data.size() == 1 && data.get(0).modeCount() < 2) {
			CollapsedEvent e = data.get(0);
			switch(e.type) {
            case TYPE_NETSPLIT:
                message.append(e.msg.replace(" ", " ↮ "));
                break;
			case TYPE_MODE:
				message.append("<b>").append(formatNick(e.nick, e.target_mode)).append("</b> was " + e.getModes(true));
				if(e.from_nick != null) {
                    if(e.from_mode != null && e.from_mode.equalsIgnoreCase("__the_server__"))
    					message.append(" by the server <b>").append(e.from_nick).append("</b>");
                    else
                        message.append(" by ").append(formatNick(e.from_nick, e.from_mode));
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

                do {
                    if(i.hasNext())
                        next = i.next();
                    else
                        next = null;
                } while(next != null && next.netsplit);
				
				if(message.length() > 0 && e.type < TYPE_NICKCHANGE && ((next == null || next.type != e.type) && last != null && last.type == e.type)) {
					if(groupcount == 1)
						message.delete(message.length() - 2, message.length()).append(" ");
					message.append("and ");
				}
				
				if(last == null || last.type != e.type) {
					switch(e.type) {
                    case TYPE_NETSPLIT:
                        break;
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
                } else if(e.type == TYPE_NETSPLIT) {
                    message.append(e.msg.replace(" ", " ↮ "));
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
				} else if(showChan && e.type != TYPE_NETSPLIT) {
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
