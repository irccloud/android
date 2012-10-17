package com.irccloud.androidnative;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.XMLReader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MessageViewFragment extends SherlockListFragment {
	private NetworkConnection conn;
	private TextView statusView;
	private View headerViewContainer;
	private View headerView;
	private TextView unreadView;
	private TextView unreadTopLabel;
	private View unreadTopView;
	private int cid;
	private int bid;
	private long last_seen_eid;
	private long min_eid;
	private long earliest_eid;
	private String name;
	private String type;
	private boolean firstScroll = true;
	private boolean requestingBacklog = false;
	private boolean shouldShowUnread = false;
	private float avgInsertTime = 0;
	private int newMsgs = 0;
	private long newMsgTime = 0;
	private MessageViewListener mListener;
	
	private static final int TYPE_TIMESTAMP = 0;
	private static final int TYPE_MESSAGE = 1;
	private static final int TYPE_BACKLOGMARKER = 2;
	private static final int TYPE_SOCKETCLOSED = 3;
	private static final int TYPE_LASTSEENEID = 4;
	
	private static final String[] COLOR_MAP = {
		"FFFFFF", //white
		"000000", //black
		"000080", //navy
		"008000", //green
		"FF0000", //red
		"800000", //maroon
		"800080", //purple
		"FFA500", //orange
		"FFFF00", //yellow
		"00FF00", //lime
		"008080", //teal
		"00FFFF", //cyan
		"0000FF", //blue
		"FF00FF", //magenta
		"808080", //grey
		"C0C0C0", //silver
	};
	
	private MessageAdapter adapter;

	private long currentCollapsedEid = -1;
	private CollapsedEventsList collapsedEvents = null;
	private int lastCollapsedDay = -1;
	private HashSet<Long> expandedSectionEids = new HashSet<Long>();

	private class MessageAdapter extends BaseAdapter {
		ArrayList<MessageEntry> data;
		private SherlockListFragment ctx;
		private long max_eid = 0;
		private long min_eid = 0;
		private int lastDay;
		private int lastSeenEidMarkerPosition = -1;
		
		private class ViewHolder {
			int type;
			TextView timestamp;
			TextView message;
		}
	
		private class MessageEntry {
			int type;
			long eid;
			String timestamp;
			String text;
			int color;
			int bg_color;
			long group_eid;
		}

		public MessageAdapter(SherlockListFragment context) {
			ctx = context;
			data = new ArrayList<MessageEntry>();
		}
		
		public void clear() {
			max_eid = 0;
			min_eid = 0;
			lastSeenEidMarkerPosition = -1;
			data.clear();
		}
		
		public void removeItem(long eid) {
			for(int i = 0; i < data.size(); i++) {
				if(data.get(i).eid == eid) {
					data.remove(i);
					i--;
				}
			}
		}

		public int getBacklogMarkerPosition() {
			for(int i = 0; i < data.size(); i++) {
				if(data.get(i).type == TYPE_BACKLOGMARKER) {
					return i;
				}
			}
			return -1;
		}

		public int insertLastSeenEIDMarker() {
			MessageEntry e = new MessageEntry();
			e.type = TYPE_LASTSEENEID;
			e.bg_color = R.drawable.socketclosed_bg;
			for(int i = 0; i < data.size(); i++) {
				if(data.get(i).type == TYPE_LASTSEENEID) {
					data.remove(i);
				}
			}
			for(int i = data.size() - 1; i >= 0; i--) {
				if(data.get(i).eid <= last_seen_eid) {
					lastSeenEidMarkerPosition = i;
					break;
				}
			}
			if(lastSeenEidMarkerPosition != data.size() - 1) {
				if(lastSeenEidMarkerPosition > 0 && data.get(lastSeenEidMarkerPosition - 1).type == TYPE_TIMESTAMP)
					lastSeenEidMarkerPosition--;
				if(lastSeenEidMarkerPosition > 0)
					data.add(lastSeenEidMarkerPosition + 1, e);
			} else {
				lastSeenEidMarkerPosition = -1;
			}
			return lastSeenEidMarkerPosition;
		}
		
		public int getLastSeenEIDPosition() {
			return lastSeenEidMarkerPosition;
		}
		
		public void addItem(int type, long eid, String text, int color, int bg_color) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(eid / 1000);
			int insert_pos = 0;
			MessageEntry e = new MessageEntry();
			SimpleDateFormat formatter;
			formatter = new SimpleDateFormat("h:mm a");
			if(conn.getUserInfo() != null && conn.getUserInfo().prefs != null) {
				try {
					JSONObject prefs = conn.getUserInfo().prefs;
					if(prefs.has("time-24hr") && prefs.getBoolean("time-24hr")) {
						if(prefs.has("time-seconds") && prefs.getBoolean("time-seconds"))
							formatter = new SimpleDateFormat("H:mm:ss");
						else
							formatter = new SimpleDateFormat("H:mm");
					} else if(prefs.has("time-seconds") && prefs.getBoolean("time-seconds")) {
						formatter = new SimpleDateFormat("h:mm:ss a");
					}
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
			}
			e.type = type;
			e.eid = eid;
			e.timestamp = formatter.format(calendar.getTime());
			e.text = text;
			e.color = color;
			e.bg_color = bg_color;
			e.group_eid = currentCollapsedEid;
			
			if(eid > max_eid) { //Message at the bottom
				if(data.size() > 0) {
					calendar.setTimeInMillis(data.get(data.size()-1).eid / 1000);
					lastDay = calendar.get(Calendar.DAY_OF_YEAR);
					calendar.setTimeInMillis(eid/1000);
				} else {
					lastDay = 0;
				}
				max_eid = eid;
				data.add(e);
				insert_pos = data.size() - 1;
			} else if(min_eid > eid) { //Message goes on top
				calendar.setTimeInMillis(data.get(1).eid / 1000);
				lastDay = calendar.get(Calendar.DAY_OF_YEAR);
				calendar.setTimeInMillis(eid/1000);
				if(calendar.get(Calendar.DAY_OF_YEAR) != lastDay) { //Insert above the dateline
					data.add(0, e);
					insert_pos = 0;
				} else { //Insert below the dateline
					data.add(1, e);
					insert_pos = 1;
				}
			} else {
				for(int i = 0; i < data.size(); i++) {
					if(data.get(i).type == TYPE_MESSAGE && data.get(i).eid > eid) { //Insert the message
						if(i > 0 && data.get(i-1).type == TYPE_MESSAGE) {
							calendar.setTimeInMillis(data.get(i-1).eid / 1000);
							lastDay = calendar.get(Calendar.DAY_OF_YEAR);
							data.add(i, e);
							insert_pos = i;
							break;
						} else { //There was a date line above our insertion point
							calendar.setTimeInMillis(data.get(i).eid / 1000);
							lastDay = calendar.get(Calendar.DAY_OF_YEAR);
							calendar.setTimeInMillis(eid/1000);
							if(calendar.get(Calendar.DAY_OF_YEAR) != lastDay) { //Insert above the dateline
								if(i > 1) {
									calendar.setTimeInMillis(data.get(i-2).eid / 1000);
									lastDay = calendar.get(Calendar.DAY_OF_YEAR);
								} else {
									//We're above the first dateline, so we'll need to put a new one on top!
									lastDay = 0;
								}
								calendar.setTimeInMillis(eid/1000);
								data.add(i-1, e);
								insert_pos = i-1;
							} else { //Insert below the dateline
								data.add(i, e);
								insert_pos = i;
							}
							break;
						}
					} else if(data.get(i).type == TYPE_MESSAGE && data.get(i).eid == eid) { //Replace the message
						lastDay = calendar.get(Calendar.DAY_OF_YEAR);
						data.remove(i);
						data.add(i, e);
						insert_pos = i;
						break;
					}
				}
			}
			
			if(calendar.get(Calendar.DAY_OF_YEAR) != lastDay) {
				formatter.applyPattern("EEEE, MMMM dd, yyyy");
				MessageEntry d = new MessageEntry();
				d.type = TYPE_TIMESTAMP;
				d.eid = eid;
				d.timestamp = formatter.format(calendar.getTime());
				d.bg_color = R.color.dateline_bg;
				data.add(insert_pos, d);
				lastDay = calendar.get(Calendar.DAY_OF_YEAR);
			}
		}
		
		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public Object getItem(int position) {
			return data.get(position);
		}

		@Override
		public long getItemId(int position) {
			return data.get(position).eid;
		}
		
		public long getGroupForPosition(int position) {
			return data.get(position).group_eid;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			MessageEntry e = data.get(position);
			View row = convertView;
			ViewHolder holder;

			if(row != null && ((ViewHolder)row.getTag()).type != e.type)
				row = null;
			
			if (row == null) {
				LayoutInflater inflater = ctx.getLayoutInflater(null);
				if(e.type == TYPE_BACKLOGMARKER)
					row = inflater.inflate(R.layout.row_backlogmarker, null);
				else if(e.type == TYPE_TIMESTAMP)
					row = inflater.inflate(R.layout.row_timestamp, null);
				else if(e.type == TYPE_SOCKETCLOSED)
					row = inflater.inflate(R.layout.row_socketclosed, null);
				else if(e.type == TYPE_LASTSEENEID)
					row = inflater.inflate(R.layout.row_lastseeneid, null);
				else
					row = inflater.inflate(R.layout.row_message, null);

				holder = new ViewHolder();
				holder.timestamp = (TextView) row.findViewById(R.id.timestamp);
				holder.message = (TextView) row.findViewById(R.id.message);
				holder.type = e.type;

				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}

			row.setBackgroundResource(e.bg_color);
			if(holder.timestamp != null)
				holder.timestamp.setText(e.timestamp);
			if(holder.message != null) {
				holder.message.setTextColor(getResources().getColorStateList(e.color));
				holder.message.setText(Html.fromHtml(e.text, null, new Html.TagHandler() {
					@Override
					public void handleTag(boolean opening, String tag, Editable output, XMLReader xmlReader) {
						int len = output.length();
						if(tag.startsWith("_bg")) {
							String rgb = "#" + tag.substring(3);
					        if(opening) {
					            output.setSpan(new BackgroundColorSpan(Color.parseColor(rgb)), len, len, Spannable.SPAN_MARK_MARK);
					        } else {
					            Object obj = getLast(output, BackgroundColorSpan.class);
					            int where = output.getSpanStart(obj);
	
					            output.removeSpan(obj);
	
					            if (where != len) {
					                output.setSpan(new BackgroundColorSpan(Color.parseColor(rgb)), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					            }
					        }
						}
					}
					
					private Object getLast(Editable text, Class kind) {
				        Object[] objs = text.getSpans(0, text.length(), kind);

				        if (objs.length == 0) {
				            return null;
				        } else {
				            for(int i = objs.length;i>0;i--) {
				                if(text.getSpanFlags(objs[i-1]) == Spannable.SPAN_MARK_MARK) {
				                    return objs[i-1];
				                }
				            }
				            return null;
				        }
				    }
				}));
			}
			
			return row;
		}
	}
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	final View v = inflater.inflate(R.layout.messageview, container, false);
    	statusView = (TextView)v.findViewById(R.id.statusView);
    	unreadView = (TextView)v.findViewById(R.id.unread);
    	unreadView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getListView().setSelection(adapter.getCount() - 1);
			}
    		
    	});
    	unreadTopView = v.findViewById(R.id.unreadTop);
    	unreadTopView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				unreadTopView.setVisibility(View.GONE);
				getListView().setSelection(adapter.getLastSeenEIDPosition());
				Long e = adapter.data.get(adapter.data.size() - 1).eid;
				new HeartbeatTask().execute(e);
			}
    		
    	});
    	unreadTopLabel = (TextView)v.findViewById(R.id.unreadTopText);
    	Button b = (Button)v.findViewById(R.id.markAsRead);
    	b.setOnClickListener(new OnClickListener() {
    		@Override
    		public void onClick(View v) {
				unreadTopView.setVisibility(View.GONE);
				Long e = adapter.data.get(adapter.data.size() - 1).eid;
				new HeartbeatTask().execute(e);
    		}
    	});
    	((ListView)v.findViewById(android.R.id.list)).setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				if(headerView != null) {
					if(firstVisibleItem == 0 && !requestingBacklog && headerView.getVisibility() == View.VISIBLE) {
						requestingBacklog = true;
						conn.request_backlog(cid, bid, earliest_eid);
					}
				}
				int markerPos = -1;
				if(adapter != null)
					markerPos = adapter.getLastSeenEIDPosition();
				
				if(unreadView != null) {
					if(firstVisibleItem + visibleItemCount == totalItemCount) {
						unreadView.setVisibility(View.GONE);
						if(newMsgs > 0 && (markerPos == -1 || getListView().getFirstVisiblePosition() <= markerPos)) {
							Long e = adapter.data.get(adapter.data.size() - 1).eid;
							new HeartbeatTask().execute(e);
						}
						newMsgs = 0;
						newMsgTime = 0;
					}
				}
				if(firstVisibleItem + visibleItemCount < totalItemCount)
					shouldShowUnread = true;

				if(adapter != null && unreadTopView != null && unreadTopView.getVisibility() == View.VISIBLE) {
		    		if(markerPos > 0 && getListView().getFirstVisiblePosition() <= markerPos) {
		    			unreadTopView.setVisibility(View.GONE);
						Long e = adapter.data.get(adapter.data.size() - 1).eid;
						new HeartbeatTask().execute(e);
		    		}
				}
				firstScroll = false;
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}
    		
    	});
    	return v;
    }
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState.containsKey("cid")) {
        	cid = savedInstanceState.getInt("cid");
        	bid = savedInstanceState.getInt("bid");
        	name = savedInstanceState.getString("name");
        	last_seen_eid = savedInstanceState.getLong("last_seen_eid");
        	min_eid = savedInstanceState.getLong("min_eid");
        	type = savedInstanceState.getString("type");
        	firstScroll = savedInstanceState.getBoolean("firstScroll");
        	//TODO: serialize the adapter data
        }
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (MessageViewListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement MessageViewListener");
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle state) {
    	super.onSaveInstanceState(state);
    	state.putInt("cid", cid);
    	state.putInt("bid", bid);
    	state.putLong("last_seen_eid", last_seen_eid);
    	state.putLong("min_eid", min_eid);
    	state.putString("name", name);
    	state.putString("type", type);
    	state.putBoolean("firstScroll", firstScroll);
    	//TODO: serialize the adapter data
    }
    
    @Override
    public void setArguments(Bundle args) {
    	cid = args.getInt("cid", 0);
    	bid = args.getInt("bid", 0);
    	last_seen_eid = args.getLong("last_seen_eid", 0);
    	min_eid = args.getLong("min_eid", 0);
    	name = args.getString("name");
    	type = args.getString("type");
		firstScroll = true;
		requestingBacklog = false;
		shouldShowUnread = false;
		avgInsertTime = 0;
		newMsgs = 0;
		newMsgTime = 0;
		earliest_eid = 0;
		if(headerView != null) {
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					if(EventsDataSource.getInstance().getEventsForBuffer(bid) != null) {
						requestingBacklog = true;
						new RefreshTask().execute((Void)null);
					} else {
						headerView.setVisibility(View.VISIBLE);
						adapter.clear();
						adapter.notifyDataSetInvalidated();
						mListener.onMessageViewReady();
					}
				}
				
			}, 100);
		}
    }
    
    private void insertEvent(IRCCloudJSONObject event, boolean backlog) {
		try {
    		long start = System.currentTimeMillis();
    		if(min_eid == 0)
    			min_eid = event.eid();
	    	if(event.eid() == min_eid)
	    		headerView.setVisibility(View.GONE);
	    	if(event.eid() < earliest_eid)
	    		earliest_eid = event.eid();
	    	
	    	int color = R.color.row_message_label;
	    	int bg_color = R.color.message_bg;
	    	
	    	String from = "";
	    	String msg = "";
	    	String type = event.getString("type");
	    	long eid = event.getLong("eid");

	    	if(event.has("from"))
				from = TextUtils.htmlEncode(event.getString("from"));
	    	
			if(event.has("msg"))
				msg = TextUtils.htmlEncode(event.getString("msg"));

	    	if(type.startsWith("you_"))
	    		type = type.substring(4);
	    	
			if(type.equalsIgnoreCase("joined_channel") || type.equalsIgnoreCase("parted_channel") || type.equalsIgnoreCase("nickchange") || type.equalsIgnoreCase("quit") || type.equalsIgnoreCase("quit_channel")) {
				if(conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null) {
					JSONObject hiddenMap = conn.getUserInfo().prefs.getJSONObject("channel-hideJoinPart");
					if(hiddenMap.has(String.valueOf(bid)) && hiddenMap.getBoolean(String.valueOf(bid))) {
			    		adapter.removeItem(event.eid());
				    	if(!backlog)
				    		adapter.notifyDataSetChanged();
				    	return;
					}
				}

				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(eid / 1000);

				if(currentCollapsedEid == -1 || calendar.get(Calendar.DAY_OF_YEAR) != lastCollapsedDay) {
					currentCollapsedEid = eid;
					lastCollapsedDay = calendar.get(Calendar.DAY_OF_YEAR);
					collapsedEvents = null;
				}

				if(collapsedEvents == null || expandedSectionEids.contains(currentCollapsedEid))
					collapsedEvents = new CollapsedEventsList();
				
				if(type.equalsIgnoreCase("joined_channel")) {
					UsersDataSource.User user = UsersDataSource.getInstance().getUser(cid, name, event.getString("nick"));
					collapsedEvents.addEvent(CollapsedEventsList.TYPE_JOIN, event.getString("nick"), (user==null)?null:user.old_nick, event.getString("hostmask"), null);
				} else if(type.equalsIgnoreCase("parted_channel")) {
					collapsedEvents.addEvent(CollapsedEventsList.TYPE_PART, event.getString("nick"), null, event.getString("hostmask"), null);
				} else if(type.equalsIgnoreCase("quit_channel") || type.equalsIgnoreCase("quit")) {
					collapsedEvents.addEvent(CollapsedEventsList.TYPE_QUIT, event.getString("nick"), null, event.getString("hostmask"), event.getString("msg"));
				} else if(type.equalsIgnoreCase("nickchange")) {
					collapsedEvents.addEvent(CollapsedEventsList.TYPE_NICKCHANGE, event.getString("newnick"), event.getString("oldnick"), null, null);
				}
				
				from = "";
				msg = collapsedEvents.getCollapsedMessage();
				if(msg == null && type.equalsIgnoreCase("nickchange"))
					msg = event.getString("newnick") + " → <b>" + event.getString("newnick") + "</b>";
				if(!expandedSectionEids.contains(currentCollapsedEid)) {
					if(eid != currentCollapsedEid)
						msg = "[+] " + msg;
					eid = currentCollapsedEid;
				}
				color = R.color.timestamp;
			} else {
				currentCollapsedEid = -1;
				collapsedEvents = null;
			}

			if(type.equalsIgnoreCase("socket_closed")) {
		    	adapter.addItem(TYPE_SOCKETCLOSED, eid, "", color, R.drawable.socketclosed_bg);
			} else if(type.equalsIgnoreCase("__backlog_marker__")) {
		    	adapter.addItem(TYPE_BACKLOGMARKER, eid, "", color, R.color.message_bg);
			} else {
		    	if(type.equalsIgnoreCase("buffer_me_msg")) {
					from = "* <i>" + from + "</i>";
					msg = "<i>" + msg + "</i>";
		    	} else if(type.equalsIgnoreCase("nickname_in_use")) {
		    		from = event.getString("nick");
		    		msg = "is already in use";
		    		bg_color = R.color.error;
		    	} else if(type.equalsIgnoreCase("connecting_failed")) {
		    		from = "";
		    		msg = "Failed to connect: " + event.getString("reason");
		    		bg_color = R.color.error;
		    	} else if(type.equalsIgnoreCase("self_details")) {
		    		from = "";
		    		msg = "Your hostmask: <b>" + event.getString("usermask") + "</b>";
		    		bg_color = R.color.dateline_bg;
		    	} else if(type.equalsIgnoreCase("myinfo")) {
		    		from = "";
		    		msg = "Host: " + event.getString("server") + "\n";
		    		msg += "IRCd: " + event.getString("version") + "\n";
		    		msg += "User modes: " + event.getString("user_modes") + "\n";
		    		msg += "Channel modes: " + event.getString("channel_modes") + "\n";
		    		bg_color = R.color.dateline_bg;
		    	} else if(type.equalsIgnoreCase("user_mode")) {
		    		from = "";
		    		msg = "Your user mode is: <b>" + event.getString("diff") + "</b>";
		    		bg_color = R.color.dateline_bg;
		    	} else if(type.equalsIgnoreCase("channel_topic")) {
		    		from = event.getString("author");
		    		msg = "set the topic: " + event.getString("topic");
		    		bg_color = R.color.dateline_bg;
		    	} else if(type.equalsIgnoreCase("channel_mode")) {
		    		from = "";
		    		msg = "Channel mode set to: <b>" + event.getString("diff") + "</b>";
		    		bg_color = R.color.dateline_bg;
		    	} else if(type.equalsIgnoreCase("channel_mode_is")) {
		    		from = "";
		    		msg = "Channel mode is: <b>" + event.getString("diff") + "</b>";
		    		bg_color = R.color.dateline_bg;
		    	} else if(type.equalsIgnoreCase("kicked_channel") || type.equalsIgnoreCase("you_kicked_channel")) {
		    		from = "← " + event.getString("nick");
		    		msg = "was kicked by " + event.getString("kicker") + " (" + event.getString("kicker_hostmask") + ")";
		    		color = R.color.timestamp;
		    	} else if(type.equalsIgnoreCase("user_channel_mode")) {
		    		from = "+++ " + from;
		    		msg = "set mode: <b>" + event.getString("diff") + " " + event.getString("nick") + "</b>";
		    	} else if(type.equalsIgnoreCase("channel_mode_list_change")) {
		    		from = "+++ " + from;
		    		msg = "set mode: <b>" + event.getString("diff") + "</b>";
		    	} else if(type.equalsIgnoreCase("motd_response") || type.equalsIgnoreCase("server_motd")) {
		    		//TODO: parse the MOTD lines
		    	} else if(type.equalsIgnoreCase("inviting_to_channel")) {
		    		from = "";
		    		msg = "You invited " + event.getString("recipient") + " to join " + event.getString("channel");
		    		bg_color = R.color.notice;
		    	}
		    	
		    	if(event.has("value")) {
		    		msg = event.getString("value") + " " + msg;
		    	}
	
		    	if(msg.length() > 0) {
		    		int pos=0;
		    		boolean bold=false, underline=false, italics=false;
		    		String fg="", bg="";
		    		
		    		while(pos < msg.length()) {
		    			if(msg.charAt(pos) == 2) {
		    				String html = "";
		    				if(bold)
		    					html += "</b>";
		    				else
		    					html += "<b>";
		    				bold = !bold;
		    				msg = removeCharAtIndex(msg, pos);
							msg = insertAtIndex(msg, pos, html);
		    			} else if(msg.charAt(pos) == 16 || msg.charAt(pos) == 29) {
		    				String html = "";
		    				if(italics)
		    					html += "</i>";
		    				else
		    					html += "<i>";
		    				italics = !italics;
		    				msg = removeCharAtIndex(msg, pos);
							msg = insertAtIndex(msg, pos, html);
		    			} else if(msg.charAt(pos) == 31) {
		    				String html = "";
		    				if(underline)
		    					html += "</u>";
		    				else
		    					html += "<u>";
		    				underline = !underline;
		    				msg = removeCharAtIndex(msg, pos);
							msg = insertAtIndex(msg, pos, html);
		    			} else if(msg.charAt(pos) == 3) {
		    				String new_fg="", new_bg="";
		    				String v = "";
		    				msg = removeCharAtIndex(msg, pos);
		    				while(msg.charAt(pos) >= '0' && msg.charAt(pos) <= '9') {
		    					v += msg.charAt(pos);
			    				msg = removeCharAtIndex(msg, pos);
		    				}
		    				if(v.length() > 0) {
		    					if(v.length() < 3)
		    						new_fg = COLOR_MAP[Integer.parseInt(v)];
		    					else
			    					new_fg = v;
		    				}
		    				v="";
		    				if(msg.charAt(pos) == ',') {
			    				msg = removeCharAtIndex(msg, pos);
		    					if(new_fg.length() == 0)
		    						new_fg = "clear";
		    					new_bg = "clear";
			    				while(msg.charAt(pos) >= '0' && msg.charAt(pos) <= '9') {
			    					v += msg.charAt(pos);
				    				msg = removeCharAtIndex(msg, pos);
			    				}
			    				if(v.length() > 0) {
			    					if(v.length() < 3)
			    						new_bg = COLOR_MAP[Integer.parseInt(v)];
			    					else
				    					new_bg = v;
			    				}
		    				}
							String html = "";
							if(new_fg.length() == 0 && new_bg.length() == 0) {
								new_fg = "clear";
								new_bg = "clear";
							}
							if(new_fg.length() > 0 && !new_fg.equals(fg) && fg.length() > 0) {
								html += "</font>";
							}
							if(new_bg.length() > 0 && !new_bg.equals(bg) && bg.length() > 0) {
								html += "</_bg" + bg + ">";
							}
		    				if(new_bg.length() > 0) {
		    					if(!new_bg.equals(bg)) {
		    						if(new_bg.equals("clear")) {
		    							bg = "";
		    						} else {
			    						html += "<_bg" + new_bg + ">";
			    						bg = new_bg;
		    						}
		    					}
		    				}
		    				if(new_fg.length() > 0) {
		    					if(!new_fg.equals(fg)) {
		    						if(new_fg.equals("clear")) {
		    							fg = "";
		    						} else {
			    						html += "<font color=\"#" + new_fg + "\">";
			    						fg = new_fg;
		    						}
		    					}
		    				}
							msg = insertAtIndex(msg, pos, html);
		    			}
		    			pos++;
		    		}
		    		if(fg.length() > 0) {
		    			msg += "</font>";
		    		}
		    		if(bg.length() > 0) {
		    			msg += "</_bg" + bg + ">";
		    		}
					if(bold)
						msg += "</b>";
					if(underline)
						msg += "</u>";
					if(italics)
						msg += "</i>";
		    	}
		    	
		    	if(from.length() > 0)
		    		msg = "<b>" + from + "</b> " + msg;
		    	
		    	if(event.has("highlight") && event.getBoolean("highlight"))
		    		bg_color = R.color.highlight;
		    	
		    	if(event.has("self") && event.getBoolean("self"))
		    		bg_color = R.color.self;
		    	
		    	adapter.addItem(TYPE_MESSAGE, eid, msg, color, bg_color);
			}
	    	if(!backlog)
	    		adapter.notifyDataSetChanged();
	    	long time = (System.currentTimeMillis() - start);
	    	if(avgInsertTime == 0)
	    		avgInsertTime = time;
	    	avgInsertTime += time;
	    	avgInsertTime /= 2.0;
	    	//Log.i("IRCCloud", "Average insert time: " + avgInsertTime);
	    	if(getListView().getLastVisiblePosition() >= (adapter.getCount() - 1)) {
	    		shouldShowUnread = false;
	    	}
	    	if(!backlog && shouldShowUnread) {
	    		if(newMsgTime == 0)
	    			newMsgTime = System.currentTimeMillis();
	    		newMsgs++;
	    		update_unread();
	    	}
	    	if(!backlog && !shouldShowUnread)
	    		getListView().setSelection(adapter.getCount() - 1);
	    	
	    	if(!backlog) {
				int markerPos = adapter.getLastSeenEIDPosition();
	    		if(markerPos > 0 && getListView().getFirstVisiblePosition() > markerPos) {
	    			unreadTopLabel.setText((getListView().getFirstVisiblePosition() - markerPos) + " unread messages");
	    		}
	    	}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public String insertAtIndex(String input, int index, String text) {
    	String head = input.substring(0, index);
    	String tail = input.substring(index, input.length());
    	return head + text + tail;
    }
    
    public String removeCharAtIndex(String input, int index) {
    	return input.substring(0, index) + input.substring(index+1, input.length());
    }
    
    public void onListItemClick(ListView l, View v, int position, long id) {
    	long group = adapter.getGroupForPosition(position-1);
    	if(expandedSectionEids.contains(group))
    		expandedSectionEids.remove(group);
    	else
    		expandedSectionEids.add(group);
    	new RefreshTask().execute((Void)null);
    }
    
    public void onResume() {
    	super.onResume();
        getListView().setStackFromBottom(true);
        getListView().requestFocus();
    	if(bid == -1) {
    		BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBufferByName(cid, name);
    		if(b != null) {
    			bid = b.bid;
    		}
    	}
    	headerViewContainer = getLayoutInflater(null).inflate(R.layout.messageview_header, null);
    	if(getListView().getHeaderViewsCount() == 0)
    		getListView().addHeaderView(headerViewContainer);
    	headerView = headerViewContainer.findViewById(R.id.progress);
    	adapter = new MessageAdapter(this);
    	setListAdapter(adapter);
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
    	if(bid != -1) {
    		TreeMap<Long,IRCCloudJSONObject> events = EventsDataSource.getInstance().getEventsForBuffer((int)bid);
    		ServersDataSource.Server server = ServersDataSource.getInstance().getServer(cid);
    		BuffersDataSource.Buffer buffer = BuffersDataSource.getInstance().getBuffer((int)bid);
			refresh(events, server, buffer);
			getListView().setSelection(adapter.getCount() - 1);
    	}
    }
    
    private class HeartbeatTask extends AsyncTask<Long, Void, Void> {

		@Override
		protected Void doInBackground(Long... params) {
			Long eid = params[0];
			
	    	if(eid > last_seen_eid && conn.getState() == NetworkConnection.STATE_CONNECTED) {
	    		if(getActivity() != null && getActivity().getIntent() != null)
	    			getActivity().getIntent().putExtra("last_seen_eid", eid);
	    		NetworkConnection.getInstance().heartbeat(bid, cid, bid, eid);
	    		last_seen_eid = eid;
	    		BuffersDataSource.getInstance().updateLastSeenEid(bid, eid);
	    	}
			return null;
		}
    }
    
	private class RefreshTask extends AsyncTaskEx<Void, Void, Void> {
		TreeMap<Long,IRCCloudJSONObject> events;
		ServersDataSource.Server server;
		BuffersDataSource.Buffer buffer;
		
		@Override
		protected Void doInBackground(Void... params) {
			buffer = BuffersDataSource.getInstance().getBuffer((int)bid);
			server = ServersDataSource.getInstance().getServer(cid);
			long time = System.currentTimeMillis();
			events = (TreeMap<Long, IRCCloudJSONObject>) EventsDataSource.getInstance().getEventsForBuffer((int)bid).clone();
			Log.i("IRCCloud", "Loaded data in " + (System.currentTimeMillis() - time) + "ms");
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if(events != null && events.size() > 0) {
				try {
					int oldSize = adapter.data.size();
					int oldPosition = getListView().getFirstVisiblePosition();
					if(oldSize > 1 && requestingBacklog && !firstScroll) {
						long oldEid = adapter.getItemId(oldPosition);
						IRCCloudJSONObject backlogMarker = new IRCCloudJSONObject();
						backlogMarker.getObject().addProperty("cid", cid);
						backlogMarker.getObject().addProperty("bid", bid);
						backlogMarker.getObject().addProperty("eid", oldEid - 1);
						backlogMarker.getObject().addProperty("type", "__backlog_marker__");
						events.put(oldEid - 1, backlogMarker);
					}
					adapter.clear();
					refresh(events, server, buffer);
					int markerPos = adapter.getBacklogMarkerPosition();
					if(firstScroll) {
						getListView().setSelection(adapter.data.size() - 1);
						firstScroll = false;
					} else if(markerPos != -1 && oldSize > 1 && adapter.data.size() > oldSize && requestingBacklog) {
						adapter.notifyDataSetChanged();
						getListView().setSelectionFromTop(oldPosition + markerPos + 1, headerViewContainer.getHeight());
					}
				} catch (IllegalStateException e) {
					//The list view doesn't exist yet
					Log.e("IRCCloud", "Tried to refresh the message list, but it didn't exist.");
				}
			}
			requestingBacklog = false;
		}
	}

	private void refresh(TreeMap<Long,IRCCloudJSONObject> events, ServersDataSource.Server server, BuffersDataSource.Buffer buffer) {
		if(events == null || (events.size() == 0 && min_eid > 0)) {
			if(bid != -1) {
				requestingBacklog = true;
				conn.request_backlog(cid, bid, 0);
			} else {
	    		headerView.setVisibility(View.GONE);
			}
		} else if(events.size() > 0){
			earliest_eid = events.firstKey();
			if(events.firstKey() > min_eid && min_eid > 0) {
	    		headerView.setVisibility(View.VISIBLE);
			} else {
	    		headerView.setVisibility(View.GONE);
			}
	    	if(events.size() > 0) {
	    		avgInsertTime = 0;
	    		long start = System.currentTimeMillis();
	    		Iterator<IRCCloudJSONObject> i = events.values().iterator();
	    		while(i.hasNext()) {
	    			insertEvent(i.next(), true);
	    		}
	    		if(adapter.getLastSeenEIDPosition() == -1)
	    			adapter.insertLastSeenEIDMarker();
	    		adapter.notifyDataSetChanged();
	    		Log.i("IRCCloud", "Backlog rendering took: " + (System.currentTimeMillis() - start) + "ms");
	    		avgInsertTime = 0;
	    		final long lastEid = events.get(events.lastKey()).eid();
	    		mHandler.post(new Runnable() {
					@Override
					public void run() {
						if(adapter != null) {
							int markerPos = adapter.getLastSeenEIDPosition();
				    		if(markerPos > 0 && getListView().getFirstVisiblePosition() > markerPos) {
				    			unreadTopLabel.setText((getListView().getFirstVisiblePosition() - markerPos) + " unread messages");
				    			unreadTopView.setVisibility(View.VISIBLE);
				    		} else {
				    			unreadTopView.setVisibility(View.GONE);
				    			new HeartbeatTask().execute(lastEid);
				    		}
						}
					}
	    		});
	    	}
		}
    	try {
			update_status(server.status, new JsonParser().parse(server.fail_info).getAsJsonObject());
		} catch (Exception e) {
			e.printStackTrace();
		}
		mListener.onMessageViewReady();
	}

	private class UnreadRefreshRunnable implements Runnable {
		@Override
		public void run() {
			update_unread();
		}
	}
	
	UnreadRefreshRunnable unreadRefreshRunnable = null;

	private void update_unread() {
		if(unreadRefreshRunnable != null) {
			mHandler.removeCallbacks(unreadRefreshRunnable);
			unreadRefreshRunnable = null;
		}
		
		if(newMsgs > 0) {
			int minutes = (int)((System.currentTimeMillis() - newMsgTime)/60000);
			
			if(minutes < 1)
				unreadView.setText("Less than a minute of chatter (");
			else if(minutes == 1)
				unreadView.setText("1 minute of chatter (");
			else
				unreadView.setText(minutes + " minutes of chatter (");
			if(newMsgs == 1)
				unreadView.setText(unreadView.getText() + "1 message)");
			else
				unreadView.setText(unreadView.getText() + (newMsgs + " messages)"));
			unreadView.setVisibility(View.VISIBLE);
			unreadRefreshRunnable = new UnreadRefreshRunnable();
			mHandler.postDelayed(unreadRefreshRunnable, 10000);
		}
	}
	
	private class StatusRefreshRunnable implements Runnable {
		String status;
		JsonObject fail_info;
		
		public StatusRefreshRunnable(String status, JsonObject fail_info) {
			this.status = status;
			this.fail_info = fail_info;
		}

		@Override
		public void run() {
			update_status(status, fail_info);
		}
	}
	
	StatusRefreshRunnable statusRefreshRunnable = null;
	
	private void update_status(String status, JsonObject fail_info) {
		if(statusRefreshRunnable != null) {
			mHandler.removeCallbacks(statusRefreshRunnable);
			statusRefreshRunnable = null;
		}
		
    	if(status.equals("connected_ready")) {
    		statusView.setVisibility(View.GONE);
    		statusView.setText("");
    	} else if(status.equals("quitting")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Disconnecting");
    		statusView.setTextColor(getResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("disconnected")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Disconnected");
    		statusView.setTextColor(getResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("queued")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Connection queued");
    		statusView.setTextColor(getResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("connecting")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Connecting");
    		statusView.setTextColor(getResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("connected")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Connected");
    		statusView.setTextColor(getResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("connected_joining")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Connected: Joining Channels");
    		statusView.setTextColor(getResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("pool_unavailable")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Disconnected: Connection temporarily unavailable");
    		statusView.setTextColor(getResources().getColor(R.color.status_fail_text));
    		statusView.setBackgroundResource(R.drawable.status_fail_bg);
    	} else if(status.equals("waiting_to_retry")) {
    		try {
	    		statusView.setVisibility(View.VISIBLE);
	    		long seconds = (fail_info.get("timestamp").getAsLong() + fail_info.get("retry_timeout").getAsInt()) - System.currentTimeMillis()/1000;
	    		statusView.setText("Disconnected: " + fail_info.get("reason").getAsString() + ". Reconnecting in " + seconds + " seconds.");
	    		statusView.setTextColor(getResources().getColor(R.color.status_fail_text));
	    		statusView.setBackgroundResource(R.drawable.status_fail_bg);
	    		statusRefreshRunnable = new StatusRefreshRunnable(status, fail_info);
	    		mHandler.postDelayed(statusRefreshRunnable, 500);
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    	} else if(status.equals("ip_retry")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Trying another IP address");
    		statusView.setTextColor(getResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	}
	}
	
    public void onPause() {
    	super.onPause();
		if(statusRefreshRunnable != null) {
			mHandler.removeCallbacks(statusRefreshRunnable);
			statusRefreshRunnable = null;
		}
    	if(conn != null)
    		conn.removeHandler(mHandler);
   	}
    
	private final Handler mHandler = new Handler() {
		IRCCloudJSONObject e;
		
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_USERINFO:
				new RefreshTask().execute((Void)null);
				break;
			case NetworkConnection.EVENT_STATUSCHANGED:
				try {
					IRCCloudJSONObject object = (IRCCloudJSONObject)msg.obj;
					if(object.getInt("cid") == cid) {
						update_status(object.getString("new_status"), object.getJsonObject("fail_info"));
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case NetworkConnection.EVENT_MAKEBUFFER:
				BuffersDataSource.Buffer buffer = (BuffersDataSource.Buffer)msg.obj;
				if(bid == -1 && buffer.cid == cid && buffer.name.equalsIgnoreCase(name)) {
					bid = buffer.bid;
		    		new RefreshTask().execute((Void)null);
				}
				break;
			case NetworkConnection.EVENT_DELETEBUFFER:
				if((Integer)msg.obj == bid) {
	                Intent parentActivityIntent = new Intent(getActivity(), MainActivity.class);
	                parentActivityIntent.addFlags(
	                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
	                        Intent.FLAG_ACTIVITY_NEW_TASK);
	                getActivity().startActivity(parentActivityIntent);
	                getActivity().finish();
				}
				break;
			case NetworkConnection.EVENT_SETIGNORES:
				e = (IRCCloudJSONObject)msg.obj;
				if(e.cid() == cid) {
					new RefreshTask().execute((Void)null);
				}
				break;
			case NetworkConnection.EVENT_BACKLOG_END:
				new RefreshTask().execute((Void)null);
				break;
			case NetworkConnection.EVENT_HEARTBEATECHO:
				if(adapter != null) {
					BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBuffer(bid);
					if(last_seen_eid != b.last_seen_eid) {
						last_seen_eid = b.last_seen_eid;
						int markerPos = adapter.insertLastSeenEIDMarker();
			    		if(markerPos > 0 && getListView().getFirstVisiblePosition() > markerPos) {
			    			unreadTopLabel.setText((getListView().getFirstVisiblePosition() - markerPos) + " unread messages");
			    			unreadTopView.setVisibility(View.VISIBLE);
			    		} else {
			    			unreadTopView.setVisibility(View.GONE);
			    		}
			    		adapter.notifyDataSetChanged();
					}
				}
				break;
			case NetworkConnection.EVENT_CHANNELTOPIC:
			case NetworkConnection.EVENT_JOIN:
			case NetworkConnection.EVENT_PART:
			case NetworkConnection.EVENT_NICKCHANGE:
			case NetworkConnection.EVENT_QUIT:
			case NetworkConnection.EVENT_BUFFERMSG:
			case NetworkConnection.EVENT_USERCHANNELMODE:
			case NetworkConnection.EVENT_KICK:
			case NetworkConnection.EVENT_CHANNELMODE:
			case NetworkConnection.EVENT_SELFDETAILS:
			case NetworkConnection.EVENT_USERMODE:
				e = (IRCCloudJSONObject)msg.obj;
				if(e.bid() == bid) {
					insertEvent(e, false);
				}
				break;
			default:
				break;
			}
		}
	};
	
	public interface MessageViewListener {
		public void onMessageViewReady();
	}
}
