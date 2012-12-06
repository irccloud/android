package com.irccloud.android;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

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
	
	public void addEvent(int type, String nick, String old_nick, String hostmask, String from_mode, String msg) {
		addEvent(type, nick, old_nick, hostmask, from_mode, msg, 0, null);
	}
	
	public void addEvent(int type, String nick, String old_nick, String hostmask, String from_mode, String msg, int mode, String target_mode) {
		//Log.d("IRCCloud", "+++ Before: " + data.toString());
		CollapsedEvent e = null;
		
		if(type < TYPE_NICKCHANGE) {
			if(old_nick != null && type != TYPE_MODE) {
				e = findEvent(old_nick);
				if(e != null)
					e.nick = nick;
			}
			
			if(e == null)
				e = findEvent(nick);
			
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
	
	public CollapsedEvent findEvent(String nick) {
		for(CollapsedEvent e : data) {
			if(e.nick.equalsIgnoreCase(nick))
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
	
	public String getCollapsedMessage() {
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
				if(e.old_nick != null)
					message.append(" by ").append(formatNick(e.old_nick, e.from_mode));
				break;
			case TYPE_JOIN:
	    		message.append("→ <b>").append(formatNick(e.nick, e.from_mode)).append("</b>").append(was(e));
	    		message.append(" joined (").append(e.hostmask + ")");
				break;
			case TYPE_PART:
	    		message.append("← <b>").append(formatNick(e.nick, e.from_mode)).append("</b>").append(was(e));
	    		message.append(" left (").append(e.hostmask).append(")");
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
	    		break;
			case TYPE_POPOUT:
	    		message.append("↔ <b>").append(formatNick(e.nick, e.from_mode)).append("</b>").append(was(e));
	    		message.append(" nipped out");
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
				} else {
					message.append("<b>").append(formatNick(e.nick, (e.type == TYPE_MODE)?e.target_mode:e.from_mode)).append("</b>").append(was(e));
				}
				
				if(next == null || next.type != e.type) {
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
