package com.irccloud.android;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockListFragment;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.irccloud.android.BuffersListFragment.OnBufferSelectedListener;

public class MessageViewFragment extends SherlockListFragment {
	private NetworkConnection conn;
	private TextView statusView;
	private View headerViewContainer;
	private View headerView;
	private TextView unreadTopLabel;
	private TextView unreadBottomLabel;
	private View unreadTopView;
	private View unreadBottomView;
	private TextView highlightsTopLabel;
	private TextView highlightsBottomLabel;
	private int cid = -1;
	public int bid = -1;
	private long last_seen_eid;
	private long min_eid;
	private long earliest_eid;
	private long backlog_eid = 0;
	private String name;
	private String type;
	private boolean firstScroll = true;
	private boolean requestingBacklog = false;
	private boolean shouldShowUnread = false;
	private float avgInsertTime = 0;
	private int newMsgs = 0;
	private long newMsgTime = 0;
	private int newHighlights = 0;
	private MessageViewListener mListener;
	private TextView errorMsg = null;
	private TextView connectingMsg = null;
	private ProgressBar progressBar = null;
	private Timer countdownTimer = null;
	private String error = null;
	private View connecting = null;
	private View awayView = null;
	private TextView awayTxt = null;
	private int savedScrollPos = -1;
	private int timestamp_width = -1;
	private View globalMsgView = null;
	private TextView globalMsg = null;
	
	public static final int ROW_MESSAGE = 0;
	public static final int ROW_TIMESTAMP = 1;
	public static final int ROW_BACKLOGMARKER = 2;
	public static final int ROW_SOCKETCLOSED = 3;
	public static final int ROW_LASTSEENEID = 4;
	private static final String TYPE_TIMESTAMP = "__timestamp__";
	private static final String TYPE_BACKLOGMARKER = "__backlog__";
	private static final String TYPE_LASTSEENEID = "__lastseeneid__";
	
	private MessageAdapter adapter;

	private long currentCollapsedEid = -1;
	private CollapsedEventsList collapsedEvents = new CollapsedEventsList();
	private int lastCollapsedDay = -1;
	private HashSet<Long> expandedSectionEids = new HashSet<Long>();
	private RefreshTask refreshTask = null;
	private HeartbeatTask heartbeatTask = null;
	private Ignore ignore = new Ignore();
	private Timer tapTimer = null;
	private ServersDataSource.Server mServer = null;
	private boolean longPressOverride = false;
	private LinkMovementMethodNoLongPress linkMovementMethodNoLongPress = new LinkMovementMethodNoLongPress();
	private boolean ready = false;
	
	private class LinkMovementMethodNoLongPress extends LinkMovementMethod {
		@Override
	    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
			if(longPressOverride) {
				longPressOverride = false;
				return false;
			} else {
				return super.onTouchEvent(widget, buffer, event);
			}
		}
	}
	
	private class MessageAdapter extends BaseAdapter {
		ArrayList<EventsDataSource.Event> data;
		private SherlockListFragment ctx;
		private long max_eid = 0;
		private long min_eid = 0;
		private int lastDay = -1;
		private int lastSeenEidMarkerPosition = -1;
		private int currentGroupPosition = -1;
		private TreeSet<Integer> unseenHighlightPositions;
		
		private class ViewHolder {
			int type;
			TextView timestamp;
			TextView message;
		}
	
		public MessageAdapter(SherlockListFragment context) {
			ctx = context;
			data = new ArrayList<EventsDataSource.Event>();
			unseenHighlightPositions = new TreeSet<Integer>(Collections.reverseOrder());
		}
		
		public void clear() {
			max_eid = 0;
			min_eid = 0;
			lastDay = -1;
			lastSeenEidMarkerPosition = -1;
			currentGroupPosition = -1;
			data.clear();
			unseenHighlightPositions.clear();
		}
		
		public void clearPending() {
			for(int i = 0; i < data.size(); i++) {
				if(data.get(i).reqid != -1 && data.get(i).color == R.color.timestamp) {
					data.remove(i);
					i--;
				}
			}
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
				if(data.get(i).row_type == ROW_BACKLOGMARKER) {
					return i;
				}
			}
			return -1;
		}

		public int insertLastSeenEIDMarker() {
			EventsDataSource.Event e = EventsDataSource.getInstance().new Event();
			e.type = TYPE_LASTSEENEID;
			e.row_type = ROW_LASTSEENEID;
			e.bg_color = R.drawable.socketclosed_bg;
			for(int i = 0; i < data.size(); i++) {
				if(data.get(i).row_type == ROW_LASTSEENEID) {
					data.remove(i);
				}
			}
			if(min_eid > 0 && last_seen_eid > 0 && min_eid >= last_seen_eid) {
				lastSeenEidMarkerPosition = 0;
			} else {
				for(int i = data.size() - 1; i >= 0; i--) {
					if(data.get(i).eid <= last_seen_eid) {
						lastSeenEidMarkerPosition = i;
						break;
					}
				}
				if(lastSeenEidMarkerPosition != data.size() - 1) {
					if(lastSeenEidMarkerPosition > 0 && data.get(lastSeenEidMarkerPosition - 1).row_type == ROW_TIMESTAMP)
						lastSeenEidMarkerPosition--;
					if(lastSeenEidMarkerPosition > 0)
						data.add(lastSeenEidMarkerPosition + 1, e);
				} else {
					lastSeenEidMarkerPosition = -1;
				}
			}
			return lastSeenEidMarkerPosition;
		}
		
		public void clearLastSeenEIDMarker() {
			for(int i = 0; i < data.size(); i++) {
				if(data.get(i).row_type == ROW_LASTSEENEID) {
					data.remove(i);
				}
			}
			lastSeenEidMarkerPosition = -1;
		}
		
		public int getLastSeenEIDPosition() {
			return lastSeenEidMarkerPosition;
		}
		
		public int getUnreadHighlightsAbovePosition(int pos) {
			int count = 0;

			Iterator<Integer> i = unseenHighlightPositions.iterator();
			while(i.hasNext()) {
				Integer p = i.next();
				if(p < pos)
					break;
				count++;
			}
			
			return unseenHighlightPositions.size() - count;
		}
		
		public synchronized void addItem(long eid, EventsDataSource.Event e) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(eid / 1000);
			int insert_pos = -1;
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
			e.timestamp = formatter.format(calendar.getTime());
			e.group_eid = currentCollapsedEid;
			if(e.group_msg != null && e.html == null)
				e.html = e.group_msg;

			if(e.html != null) {
				e.html = ColorFormatter.irc_to_html(e.html);
			}

			if(currentGroupPosition > 0 && eid == currentCollapsedEid && e.eid != eid) { //Shortcut for replacing the current group
				calendar.setTimeInMillis(e.eid / 1000);
				lastDay = calendar.get(Calendar.DAY_OF_YEAR);
				data.remove(currentGroupPosition);
				data.add(currentGroupPosition, e);
				insert_pos = currentGroupPosition;
			} else if(eid > max_eid || data.size() == 0 || eid > data.get(data.size()-1).eid) { //Message at the bottom
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
				if(data.size() > 1) {
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
					data.add(0, e);
					insert_pos = 0;
				}
			} else {
				int i = 0;
				for(EventsDataSource.Event e1 : data) {
					if(e1.row_type != ROW_TIMESTAMP && e1.eid > eid && e.eid == eid) { //Insert the message
						if(i > 0 && data.get(i-1).row_type != ROW_TIMESTAMP) {
							calendar.setTimeInMillis(data.get(i-1).eid / 1000);
							lastDay = calendar.get(Calendar.DAY_OF_YEAR);
							data.add(i, e);
							insert_pos = i;
							break;
						} else { //There was a date line above our insertion point
							calendar.setTimeInMillis(e1.eid / 1000);
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
					} else if(e1.row_type != ROW_TIMESTAMP && (e1.eid == eid || e1.group_eid == eid)) { //Replace the message
						lastDay = calendar.get(Calendar.DAY_OF_YEAR);
						data.remove(i);
						data.add(i, e);
						insert_pos = i;
						break;
					}
					i++;
				}
			}

			if(insert_pos == -1) {
				Log.e("IRCCloud", "Couldn't insert EID: " + eid + " MSG: " + e.html);
				return;
			}
			
			if(eid > last_seen_eid && e.highlight)
				unseenHighlightPositions.add(insert_pos);
			
			if(eid < min_eid || min_eid == 0)
				min_eid = eid;
			
			if(eid == currentCollapsedEid && e.eid == eid) {
				currentGroupPosition = insert_pos;
			} else if(currentCollapsedEid == -1) {
				currentGroupPosition = -1;
			}
			
			if(calendar.get(Calendar.DAY_OF_YEAR) != lastDay) {
				formatter.applyPattern("EEEE, MMMM dd, yyyy");
				EventsDataSource.Event d = EventsDataSource.getInstance().new Event();
				d.type = TYPE_TIMESTAMP;
				d.row_type = ROW_TIMESTAMP;
				d.eid = eid;
				d.timestamp = formatter.format(calendar.getTime());
				d.bg_color = R.drawable.row_timestamp_bg;
				data.add(insert_pos, d);
				lastDay = calendar.get(Calendar.DAY_OF_YEAR);
				if(currentGroupPosition > -1)
					currentGroupPosition++;
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
			EventsDataSource.Event e = data.get(position);
			View row = convertView;
			ViewHolder holder;

			if(row != null && ((ViewHolder)row.getTag()).type != e.row_type)
				row = null;
			
			if (row == null) {
				LayoutInflater inflater = ctx.getLayoutInflater(null);
				if(e.row_type == ROW_BACKLOGMARKER)
					row = inflater.inflate(R.layout.row_backlogmarker, null);
				else if(e.row_type == ROW_TIMESTAMP)
					row = inflater.inflate(R.layout.row_timestamp, null);
				else if(e.row_type == ROW_SOCKETCLOSED)
					row = inflater.inflate(R.layout.row_socketclosed, null);
				else if(e.row_type == ROW_LASTSEENEID)
					row = inflater.inflate(R.layout.row_lastseeneid, null);
				else
					row = inflater.inflate(R.layout.row_message, null);

				holder = new ViewHolder();
				holder.timestamp = (TextView) row.findViewById(R.id.timestamp);
				holder.message = (TextView) row.findViewById(R.id.message);
				holder.type = e.row_type;

				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}

			row.setOnClickListener(new OnItemClickListener(position));
			
			if(e.row_type == ROW_MESSAGE) {
				if(e.bg_color == R.color.message_bg)
					row.setBackgroundDrawable(null);
				else
					row.setBackgroundResource(e.bg_color);
			}

			if(holder.timestamp != null) {
				holder.timestamp.setText(e.timestamp);
				holder.timestamp.setMinWidth(timestamp_width);
			}
			if(e.row_type == ROW_SOCKETCLOSED) {
				if(e.msg.length() > 0) {
					holder.timestamp.setVisibility(View.VISIBLE);
					holder.message.setVisibility(View.VISIBLE);
				} else {
					holder.timestamp.setVisibility(View.GONE);
					holder.message.setVisibility(View.GONE);
				}
			}
			
			if(holder.message != null && e.html != null) {
				holder.message.setMovementMethod(linkMovementMethodNoLongPress);
				holder.message.setOnClickListener(new OnItemClickListener(position));
				if(e.msg != null && e.msg.startsWith("<pre>"))
					holder.message.setTypeface(Typeface.MONOSPACE);
				else
					holder.message.setTypeface(Typeface.DEFAULT);
				holder.message.setTextColor(getResources().getColorStateList(e.color));
				holder.message.setText(ColorFormatter.html_to_spanned(e.html, e.linkify, mServer));
			}
			
			return row;
		}
	}
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	final View v = inflater.inflate(R.layout.messageview, container, false);
        connecting = v.findViewById(R.id.connecting);
		errorMsg = (TextView)v.findViewById(R.id.errorMsg);
		connectingMsg = (TextView)v.findViewById(R.id.connectingMsg);
		progressBar = (ProgressBar)v.findViewById(R.id.connectingProgress);
    	statusView = (TextView)v.findViewById(R.id.statusView);
    	statusView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(mServer != null && mServer.status != null && mServer.status.equalsIgnoreCase("disconnected")) {
					conn.reconnect(cid);
				}
			}
    		
    	});
    	
    	awayView = v.findViewById(R.id.away);
    	awayView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				conn.back(cid);
			}
    		
    	});
    	awayTxt = (TextView)v.findViewById(R.id.awayTxt);
    	unreadBottomView = v.findViewById(R.id.unreadBottom);
    	unreadBottomView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getListView().setSelection(adapter.getCount() - 1);
			}
    		
    	});
    	unreadBottomLabel = (TextView)v.findViewById(R.id.unread);
    	highlightsBottomLabel = (TextView)v.findViewById(R.id.highlightsBottom);

    	unreadTopView = v.findViewById(R.id.unreadTop);
		unreadTopView.setVisibility(View.INVISIBLE);
    	unreadTopView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(adapter.getLastSeenEIDPosition() > 0) {
					Long e = adapter.data.get(adapter.data.size() - 1).eid;
					new HeartbeatTask().execute(e);
				}
				getListView().setSelection(adapter.getLastSeenEIDPosition());
				unreadTopView.setVisibility(View.GONE);
			}
    		
    	});
    	unreadTopLabel = (TextView)v.findViewById(R.id.unreadTopText);
    	highlightsTopLabel = (TextView)v.findViewById(R.id.highlightsTop);
    	Button b = (Button)v.findViewById(R.id.markAsRead);
    	b.setOnClickListener(new OnClickListener() {
    		@Override
    		public void onClick(View v) {
				unreadTopView.setVisibility(View.GONE);
				Long e = adapter.data.get(adapter.data.size() - 1).eid;
				new HeartbeatTask().execute(e);
    		}
    	});
    	globalMsgView = v.findViewById(R.id.globalMessageView);
    	globalMsg = (TextView)v.findViewById(R.id.globalMessageTxt);
    	b = (Button)v.findViewById(R.id.dismissGlobalMessage);
    	b.setOnClickListener(new OnClickListener() {
    		@Override
    		public void onClick(View v) {
    			if(conn != null)
    				conn.globalMsg = null;
    			update_global_msg();
    		}
    	});
    	((ListView)v.findViewById(android.R.id.list)).setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
				if(pos > 1) {
					longPressOverride = mListener.onMessageLongClicked(adapter.data.get(pos - 1));
					return longPressOverride;
				} else {
					return false;
				}
			}
    		
    	});
    	return v;
    }
	
    private OnScrollListener mOnScrollListener = new OnScrollListener() {
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if(!ready)
				return;
			
			if(headerView != null && min_eid > 0 && conn.ready) {
				if(firstVisibleItem == 0 && !requestingBacklog && headerView.getVisibility() == View.VISIBLE && bid != -1 && conn.getState() == NetworkConnection.STATE_CONNECTED) {
					requestingBacklog = true;
					conn.request_backlog(cid, bid, earliest_eid);
				}
			}
			
			if(unreadBottomView != null && adapter != null && adapter.data.size() > 0) {
				if(firstVisibleItem + visibleItemCount == totalItemCount) {
					unreadBottomView.setVisibility(View.GONE);
					if(unreadTopView.getVisibility() == View.GONE) {
						Long e = adapter.data.get(adapter.data.size() - 1).eid;
	    				if(heartbeatTask != null)
	    					heartbeatTask.cancel(true);
	    				heartbeatTask = new HeartbeatTask();
	    				heartbeatTask.execute(e);
					}
					newMsgs = 0;
					newMsgTime = 0;
					newHighlights = 0;
				}
			}
			if(firstVisibleItem + visibleItemCount < totalItemCount)
				shouldShowUnread = true;

			if(adapter != null && adapter.data.size() > 0 && unreadTopView != null && unreadTopView.getVisibility() == View.VISIBLE) {
				mUpdateTopUnreadRunnable.run();
				int markerPos = -1;
				if(adapter != null)
					markerPos = adapter.getLastSeenEIDPosition();
	    		if(markerPos > 0 && getListView().getFirstVisiblePosition() <= markerPos) {
	    			unreadTopView.setVisibility(View.GONE);
					Long e = adapter.data.get(adapter.data.size() - 1).eid;
    				if(heartbeatTask != null)
    					heartbeatTask.cancel(true);
    				heartbeatTask = new HeartbeatTask();
    				heartbeatTask.execute(e);
	    		}
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
		}
		
	};
    
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
        	backlog_eid = savedInstanceState.getLong("backlog_eid");
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
    	state.putLong("backlog_eid", backlog_eid);
    	//TODO: serialize the adapter data
    }
    
    @Override
    public void setArguments(Bundle args) {
    	if(tapTimer != null)
    		tapTimer.cancel();
    	tapTimer = null;
    	cid = args.getInt("cid", 0);
    	bid = args.getInt("bid", 0);
    	last_seen_eid = args.getLong("last_seen_eid", 0);
    	min_eid = args.getLong("min_eid", 0);
    	name = args.getString("name");
    	type = args.getString("type");
		firstScroll = true;
		requestingBacklog = false;
		shouldShowUnread = false;
		ready = false;
		avgInsertTime = 0;
		newMsgs = 0;
		newMsgTime = 0;
		newHighlights = 0;
		earliest_eid = 0;
		backlog_eid = 0;
		mServer = ServersDataSource.getInstance().getServer(cid);
    	if(mServer != null) {
    		ignore.setIgnores(mServer.ignores);
    		if(mServer.away != null && mServer.away.length() > 0) {
    			awayTxt.setText("Away (" + mServer.away + ")");
    			awayView.setVisibility(View.VISIBLE);
    		} else {
    			awayView.setVisibility(View.GONE);
    		}
			update_status(mServer.status, mServer.fail_info);
    	}
		if(unreadTopView != null)
			unreadTopView.setVisibility(View.INVISIBLE);
		if(headerView != null) {
			mHandler.postDelayed(new Runnable() {

				@Override
				public void run() {
					if(EventsDataSource.getInstance().getEventsForBuffer(bid) != null) {
						requestingBacklog = true;
			            if(refreshTask != null)
			            	refreshTask.cancel(true);
						refreshTask = new RefreshTask();
						refreshTask.execute((Void)null);
					} else {
						if(bid == -1 || min_eid == 0 || !conn.ready) {
							headerView.setVisibility(View.GONE);
						} else {
							headerView.setVisibility(View.VISIBLE);
						}
						adapter.clear();
						adapter.notifyDataSetInvalidated();
						mListener.onMessageViewReady();
						ready = true;
					}
				}
				
			}, 200);
		}
    }
    
    private synchronized void insertEvent(EventsDataSource.Event event, boolean backlog, boolean nextIsGrouped) {
		try {
    		long start = System.currentTimeMillis();
    		if(min_eid == 0)
    			min_eid = event.eid;
	    	if(event.eid == min_eid) {
	    		headerView.setVisibility(View.GONE);
	    	}
	    	if(event.eid < earliest_eid)
	    		earliest_eid = event.eid;
	    	
	    	String type = event.type;
	    	long eid = event.eid;
	    	
	    	if(type.startsWith("you_"))
	    		type = type.substring(4);
	    	
			if(type.equalsIgnoreCase("joined_channel") || type.equalsIgnoreCase("parted_channel") || type.equalsIgnoreCase("nickchange") || type.equalsIgnoreCase("quit") || type.equalsIgnoreCase("user_channel_mode")) {
				if(conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null) {
					JSONObject hiddenMap = null;
					if(this.type.equalsIgnoreCase("channel")) {
						if(conn.getUserInfo().prefs.has("channel-hideJoinPart"))
							hiddenMap = conn.getUserInfo().prefs.getJSONObject("channel-hideJoinPart");
					} else {
						if(conn.getUserInfo().prefs.has("buffer-hideJoinPart"))
							hiddenMap = conn.getUserInfo().prefs.getJSONObject("buffer-hideJoinPart");
					}
					
					if(hiddenMap != null && hiddenMap.has(String.valueOf(bid)) && hiddenMap.getBoolean(String.valueOf(bid))) {
			    		adapter.removeItem(event.eid);
				    	if(!backlog)
				    		adapter.notifyDataSetChanged();
				    	return;
					}
				}

				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(eid / 1000);

				if(currentCollapsedEid == -1 || calendar.get(Calendar.DAY_OF_YEAR) != lastCollapsedDay) {
					collapsedEvents.clear();
					currentCollapsedEid = eid;
					lastCollapsedDay = calendar.get(Calendar.DAY_OF_YEAR);
				}

				if(expandedSectionEids.contains(currentCollapsedEid))
					collapsedEvents.clear();
				
				event.color = R.color.timestamp;
				event.bg_color = R.color.message_bg;
				
				if(type.equalsIgnoreCase("joined_channel")) {
					collapsedEvents.addEvent(CollapsedEventsList.TYPE_JOIN, event.nick, null, event.hostmask, event.from_mode, null);
				} else if(type.equalsIgnoreCase("parted_channel")) {
					collapsedEvents.addEvent(CollapsedEventsList.TYPE_PART, event.nick, null, event.hostmask, event.from_mode, event.msg);
				} else if(type.equalsIgnoreCase("quit")) {
					collapsedEvents.addEvent(CollapsedEventsList.TYPE_QUIT, event.nick, null, event.hostmask, event.from_mode, event.msg);
				} else if(type.equalsIgnoreCase("nickchange")) {
					collapsedEvents.addEvent(CollapsedEventsList.TYPE_NICKCHANGE, event.nick, event.old_nick, null, event.from_mode, null);
				} else if(type.equalsIgnoreCase("user_channel_mode")) {
					boolean unknown = false;
					JsonObject ops = event.ops;
					if(ops != null) {
						JsonArray add = ops.getAsJsonArray("add");
						for(int i = 0; i < add.size(); i++) {
							JsonObject op = add.get(i).getAsJsonObject();
							if(op.get("mode").getAsString().equalsIgnoreCase("q"))
								collapsedEvents.addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), event.from, event.hostmask, event.from_mode, null, CollapsedEventsList.MODE_OWNER, event.target_mode);
							else if(op.get("mode").getAsString().equalsIgnoreCase("a"))
								collapsedEvents.addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), event.from, event.hostmask, event.from_mode, null, CollapsedEventsList.MODE_ADMIN, event.target_mode);
							else if(op.get("mode").getAsString().equalsIgnoreCase("o"))
								collapsedEvents.addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), event.from, event.hostmask, event.from_mode, null, CollapsedEventsList.MODE_OP, event.target_mode);
							else if(op.get("mode").getAsString().equalsIgnoreCase("h"))
								collapsedEvents.addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), event.from, event.hostmask, event.from_mode, null, CollapsedEventsList.MODE_HALFOP, event.target_mode);
							else if(op.get("mode").getAsString().equalsIgnoreCase("v"))
								collapsedEvents.addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), event.from, event.hostmask, event.from_mode, null, CollapsedEventsList.MODE_VOICE, event.target_mode);
							else
								unknown = true;
						}
						JsonArray remove = ops.getAsJsonArray("remove");
						for(int i = 0; i < remove.size(); i++) {
							JsonObject op = remove.get(i).getAsJsonObject();
							if(op.get("mode").getAsString().equalsIgnoreCase("q"))
								collapsedEvents.addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), event.from, event.hostmask, event.from_mode, null, CollapsedEventsList.MODE_DEOWNER, event.target_mode);
							else if(op.get("mode").getAsString().equalsIgnoreCase("a"))
								collapsedEvents.addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), event.from, event.hostmask, event.from_mode, null, CollapsedEventsList.MODE_DEADMIN, event.target_mode);
							else if(op.get("mode").getAsString().equalsIgnoreCase("o"))
								collapsedEvents.addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), event.from, event.hostmask, event.from_mode, null, CollapsedEventsList.MODE_DEOP, event.target_mode);
							else if(op.get("mode").getAsString().equalsIgnoreCase("h"))
								collapsedEvents.addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), event.from, event.hostmask, event.from_mode, null, CollapsedEventsList.MODE_DEHALFOP, event.target_mode);
							else if(op.get("mode").getAsString().equalsIgnoreCase("v"))
								collapsedEvents.addEvent(CollapsedEventsList.TYPE_MODE, op.get("param").getAsString(), event.from, event.hostmask, event.from_mode, null, CollapsedEventsList.MODE_DEVOICE, event.target_mode);
							else
								unknown = true;
						}
					}
					if(unknown) {
						collapsedEvents.clear();
					}
					event.color = R.color.row_message_label;
					event.bg_color = R.color.status_bg;
				}
				
				String msg = (nextIsGrouped && currentCollapsedEid != event.eid)?"":collapsedEvents.getCollapsedMessage();
				if(msg == null && type.equalsIgnoreCase("nickchange")) {
					msg = event.old_nick + " → <b>" + event.nick + "</b>";
				}
				if(msg == null && type.equalsIgnoreCase("user_channel_mode")) {
		    		msg = "<b>" + collapsedEvents.formatNick(event.from, event.from_mode) + "</b> set mode: <b>" + event.diff + " " + event.nick + "</b>";
		    		currentCollapsedEid = eid;
				}
				if(!expandedSectionEids.contains(currentCollapsedEid)) {
					if(eid != currentCollapsedEid) {
						msg = "[+] " + msg;
						event.color = R.color.timestamp;
						event.bg_color = R.color.message_bg;
					}
					eid = currentCollapsedEid;
				}
				event.group_msg = msg;
				event.html = null;
				event.linkify = false;
			} else {
				currentCollapsedEid = -1;
				collapsedEvents.clear();
	    		if(event.from != null)
	    			event.html = "<b>" + collapsedEvents.formatNick(event.from, event.from_mode) + "</b> " + event.msg;
	    		else
	    			event.html = event.msg;
			}

			if(event.from != null && event.hostmask != null && !type.equalsIgnoreCase("user_channel_mode") && !type.contains("kicked")) {
				String usermask = event.from + "!" + event.hostmask;
				if(ignore.match(usermask))
					return;
			}

			if(type.equalsIgnoreCase("channel_mode") && event.nick != null && event.nick.length() > 0) {
				event.html = event.msg + " by <b>" + collapsedEvents.formatNick(event.nick, event.from_mode) + "</b>";
			} else if(type.equalsIgnoreCase("buffer_me_msg")) {
				event.html = "— <i><b>" + collapsedEvents.formatNick(event.nick, event.from_mode) + "</b> " + event.msg + "</i>";
	    	} else if(type.equalsIgnoreCase("kicked_channel")) {
	    		event.html = "← ";
	    		if(event.type.startsWith("you_"))
	    			event.html += "You";
	    		else
	    			event.html += "<b>" + collapsedEvents.formatNick(event.old_nick, null) + "</b>";
	    		if(event.type.startsWith("you_"))
	    			event.html += " were";
	    		else
	    			event.html += " was";
	    		event.html += " kicked by <b>" + collapsedEvents.formatNick(event.nick, event.from_mode) + "</b> (" + event.hostmask + ")";
	    		if(event.msg != null && event.msg.length() > 0)
	    			event.html += ": " + event.msg;
	    	} else if(type.equalsIgnoreCase("callerid")) {
    			event.html = "<b>" + collapsedEvents.formatNick(event.from, event.from_mode) + "</b> ("+ event.hostmask + ") " + event.msg + " Tap to accept.";
			} else if(type.equalsIgnoreCase("channel_mode_list_change")) {
				if(event.from.length() == 0) {
					event.html = event.msg + "<b>" + collapsedEvents.formatNick(event.nick, event.from_mode) + "</b>";
				}
			}
			
	    	adapter.addItem(eid, event);

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
				if(event.highlight)
					newHighlights++;
	    		update_unread();
	    	}
	    	if(!backlog && !shouldShowUnread)
	    		getListView().setSelection(adapter.getCount() - 1);
	    	
	    	if(!backlog && event.highlight && !getActivity().getSharedPreferences("prefs", 0).getBoolean("mentionTip", false)) {
	    		Toast.makeText(getActivity(), "Double-tap a message to quickly reply to the sender", Toast.LENGTH_LONG).show();
	    		SharedPreferences.Editor editor = getActivity().getSharedPreferences("prefs", 0).edit();
	    		editor.putBoolean("mentionTip", true);
	    		editor.commit();
	    	}
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
    
    private class OnItemClickListener implements OnClickListener {
        private int pos;
        OnItemClickListener(int position){
            pos = position;
        }
        
        @Override
        public void onClick(View arg0) {
        	longPressOverride = false;
        	
	    	if(pos < 0 || pos >= adapter.data.size())
	    		return;

	    	if(adapter != null) {
	    		if(tapTimer != null) {
	    			tapTimer.cancel();
	    			tapTimer = null;
	    			mListener.onMessageDoubleClicked(adapter.data.get(pos));
	    		} else {
	    			tapTimer = new Timer();
	    			tapTimer.schedule(new TimerTask() {
	    				int position = pos;
	    				
						@Override
						public void run() {
				    		mHandler.post(new Runnable() {
								@Override
								public void run() {
									if(adapter != null && adapter.data != null && position < adapter.data.size()) {
								    	EventsDataSource.Event e = adapter.data.get(position);
								    	if(e != null && e.type.equals("channel_invite")) {
								    		conn.join(cid, e.old_nick, null);
								    	} else if(e != null && e.type.equals("callerid")) {
								    		conn.say(cid, null, "/accept " + e.from);
								    		BuffersDataSource b = BuffersDataSource.getInstance();
								    		BuffersDataSource.Buffer buffer = b.getBufferByName(cid, e.from);
								    		if(buffer != null) {
								    			mListener.onBufferSelected(buffer.cid, buffer.bid, buffer.name, buffer.last_seen_eid, buffer.min_eid, 
								    					buffer.type, 1, buffer.archived, "connected_ready");
								    		} else {
								    			mListener.onBufferSelected(cid, -1, e.from, 0, 0, "conversation", 1, 0, "connected_ready");
								    		}
								    	} else {
									    	long group = adapter.getGroupForPosition(position);
									    	if(expandedSectionEids.contains(group))
									    		expandedSectionEids.remove(group);
									    	else
									    		expandedSectionEids.add(group);
									        if(refreshTask != null)
									        	refreshTask.cancel(true);
											refreshTask = new RefreshTask();
											refreshTask.execute((Void)null);
								    	}
									}
								}
				    		});
			    			tapTimer = null;
						}
	    				
	    			}, 300);
	    		}
	    	}
    	}
    }
    
    @SuppressWarnings("unchecked")
	public void onResume() {
    	super.onResume();
    	longPressOverride = false;
    	ready = false;
        getListView().setStackFromBottom(true);
        getListView().requestFocus();
    	if(bid == -1 && cid != -1) {
    		BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBufferByName(cid, name);
    		if(b != null) {
    			bid = b.bid;
    		}
    	}
    	if(cid != -1) {
			mServer = ServersDataSource.getInstance().getServer(cid);
			if(mServer != null)
				update_status(mServer.status, mServer.fail_info);
    	}
		if(bid != -1) {
			BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBuffer(bid);
			if(b != null)
				last_seen_eid = b.last_seen_eid;
		}
    	if(getListView().getHeaderViewsCount() == 0) {
    		headerViewContainer = getLayoutInflater(null).inflate(R.layout.messageview_header, null);
    		headerView = headerViewContainer.findViewById(R.id.progress);
    		getListView().addHeaderView(headerViewContainer);
    	}
    	adapter = new MessageAdapter(this);
    	setListAdapter(adapter);
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
    	if(conn.getState() != NetworkConnection.STATE_CONNECTED || !NetworkConnection.getInstance().ready) {
			TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1, Animation.RELATIVE_TO_SELF, 0);
			anim.setDuration(200);
			anim.setFillAfter(true);
			connecting.startAnimation(anim);
			connecting.setVisibility(View.VISIBLE);
    	}
    	updateReconnecting();
    	update_global_msg();
    	if(mServer != null) {
    		ignore.setIgnores(mServer.ignores);
    		if(mServer.away != null && mServer.away.length() > 0) {
    			awayTxt.setText("Away (" + mServer.away + ")");
    			awayView.setVisibility(View.VISIBLE);
    		} else {
    			awayView.setVisibility(View.GONE);
    		}
    	}
    	if(bid != -1) {
    		TreeMap<Long,EventsDataSource.Event> events = EventsDataSource.getInstance().getEventsForBuffer((int)bid);
    		if(events != null && events.size() > 0) {
    			adapter.clearLastSeenEIDMarker();
    			events = (TreeMap<Long, EventsDataSource.Event>)events.clone();
	    		BuffersDataSource.Buffer buffer = BuffersDataSource.getInstance().getBuffer((int)bid);
	    		if(backlog_eid > 0) {
					EventsDataSource.Event backlogMarker = EventsDataSource.getInstance().new Event();
					backlogMarker.eid = backlog_eid;
					backlogMarker.type = TYPE_BACKLOGMARKER;
					backlogMarker.row_type = ROW_BACKLOGMARKER;
					backlogMarker.bg_color = R.color.message_bg;
					events.put(backlog_eid, backlogMarker);
	    		}
				refresh(events, buffer);
	    		adapter.notifyDataSetChanged();
	    		if(savedScrollPos > 0)
	    			getListView().setSelection(savedScrollPos);
	    		else
	    			getListView().setSelection(adapter.getCount() - 1);
	    		savedScrollPos = -1;
    		} else if(conn.getState() != NetworkConnection.STATE_CONNECTED || !conn.ready) {
    			headerView.setVisibility(View.GONE);
    		} else {
    			headerView.setVisibility(View.VISIBLE);
				ready = true;
    		}
    	} else {
    		if(cid == -1)
    			headerView.setVisibility(View.GONE);
    	}
		getListView().setOnScrollListener(mOnScrollListener);
    }
    
    private class HeartbeatTask extends AsyncTaskEx<Long, Void, Void> {

    	@Override
    	protected void onPreExecute() {
    		//Log.d("IRCCloud", "Heartbeat task created");
    	}
    	
		@Override
		protected Void doInBackground(Long... params) {
			Long eid = params[0];

			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
			}

			if(isCancelled())
				return null;
			
	    	if(eid > last_seen_eid && conn.getState() == NetworkConnection.STATE_CONNECTED) {
	    		if(getActivity() != null && getActivity().getIntent() != null)
	    			getActivity().getIntent().putExtra("last_seen_eid", eid);
	    		NetworkConnection.getInstance().heartbeat(bid, cid, bid, eid);
	    		last_seen_eid = eid;
	    		BuffersDataSource.getInstance().updateLastSeenEid(bid, eid);
	    	}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(!isCancelled())
				heartbeatTask = null;
		}
    }
    
	private class RefreshTask extends AsyncTaskEx<Void, Void, Void> {
		TreeMap<Long,EventsDataSource.Event> events;
		BuffersDataSource.Buffer buffer;
		
		@SuppressWarnings("unchecked")
		@Override
		protected Void doInBackground(Void... params) {
			buffer = BuffersDataSource.getInstance().getBuffer((int)bid);
			long time = System.currentTimeMillis();
			events = EventsDataSource.getInstance().getEventsForBuffer((int)bid);
			Log.i("IRCCloud", "Loaded data in " + (System.currentTimeMillis() - time) + "ms");
			if(!isCancelled() && events != null && events.size() > 0) {
    			events = (TreeMap<Long, EventsDataSource.Event>)events.clone();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if(isCancelled())
				return;
			
			if(events != null && events.size() > 0) {
				try {
					int oldPosition = getListView().getFirstVisiblePosition();
					if(adapter != null && adapter.data.size() > 0 && earliest_eid > events.firstKey()) {
						backlog_eid = adapter.getGroupForPosition(oldPosition) - 1;
						if(backlog_eid < 0) {
							backlog_eid = adapter.getItemId(oldPosition) - 1;
						}
						EventsDataSource.Event backlogMarker = EventsDataSource.getInstance().new Event();
						backlogMarker.eid = backlog_eid;
						backlogMarker.type = TYPE_BACKLOGMARKER;
						backlogMarker.row_type = ROW_BACKLOGMARKER;
						backlogMarker.html = "__backlog__";
						backlogMarker.bg_color = R.color.message_bg;
						events.put(backlog_eid, backlogMarker);
					}
					adapter.clear();
					refresh(events, buffer);
					int markerPos = adapter.getBacklogMarkerPosition();
					if(markerPos != -1 && requestingBacklog) {
						getListView().setSelectionFromTop(oldPosition + markerPos + 1, headerViewContainer.getHeight());
					}
				} catch (IllegalStateException e) {
					//The list view doesn't exist yet
					Log.e("IRCCloud", "Tried to refresh the message list, but it didn't exist.");
				}
			} else if(bid != -1 && min_eid > 0 && conn.ready) {
				headerView.setVisibility(View.VISIBLE);
				adapter.notifyDataSetInvalidated();
			}
			requestingBacklog = false;
			refreshTask = null;
		}
	}

	private void refresh(TreeMap<Long,EventsDataSource.Event> events, BuffersDataSource.Buffer buffer) {
		if(conn.getReconnectTimestamp() == 0)
			conn.cancel_idle_timer(); //This may take a while...
		collapsedEvents.clear();
		currentCollapsedEid = -1;
		
		if(conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null) {
			try {
				JSONObject prefs = conn.getUserInfo().prefs;
				timestamp_width = (int)getResources().getDimension(R.dimen.timestamp_base);
				if(prefs.has("time-seconds") && prefs.getBoolean("time-seconds"))
					timestamp_width += (int)getResources().getDimension(R.dimen.timestamp_seconds);
				if(!prefs.has("time-24hr") || !prefs.getBoolean("time-24hr"))
					timestamp_width += (int)getResources().getDimension(R.dimen.timestamp_ampm);
			} catch (Exception e) {
				
			}
		} else {
			timestamp_width = getResources().getDimensionPixelSize(R.dimen.timestamp_base) + getResources().getDimensionPixelSize(R.dimen.timestamp_ampm);
		}

		if(events == null || (events.size() == 0 && min_eid > 0)) {
			if(bid != -1) {
				requestingBacklog = true;
				conn.request_backlog(cid, bid, 0);
			} else {
	    		headerView.setVisibility(View.GONE);
			}
		} else if(events.size() > 0) {
			mServer = ServersDataSource.getInstance().getServer(cid);
	    	if(mServer != null)
	    		ignore.setIgnores(mServer.ignores);
	    	else
	    		ignore.setIgnores(null);
			earliest_eid = events.firstKey();
			if(events.firstKey() > min_eid && min_eid > 0) {
	    		headerView.setVisibility(View.VISIBLE);
			} else {
	    		headerView.setVisibility(View.GONE);
			}
	    	if(events.size() > 0) {
	    		avgInsertTime = 0;
	    		//Debug.startMethodTracing("refresh");
	    		long start = System.currentTimeMillis();
	    		Iterator<EventsDataSource.Event> i = events.values().iterator();
	    		EventsDataSource.Event next = i.next();
				Calendar calendar = Calendar.getInstance();
	    		while(next != null) {
	    			EventsDataSource.Event e = next;
	    			next = i.hasNext()?i.next():null;
	    			String type = (next == null)?"":next.type;

	    			if(next != null && currentCollapsedEid != -1 && !expandedSectionEids.contains(currentCollapsedEid) && (type.equalsIgnoreCase("joined_channel") || type.equalsIgnoreCase("parted_channel") || type.equalsIgnoreCase("nickchange") || type.equalsIgnoreCase("quit") || type.equalsIgnoreCase("user_channel_mode"))) {
						calendar.setTimeInMillis(next.eid / 1000);
	    				insertEvent(e, true, calendar.get(Calendar.DAY_OF_YEAR) == lastCollapsedDay);
	    			} else {
	    				insertEvent(e, true, false);
	    			}
	    		}
	    		if(adapter.getLastSeenEIDPosition() == -1)
	    			adapter.insertLastSeenEIDMarker();
	    		Log.i("IRCCloud", "Backlog rendering took: " + (System.currentTimeMillis() - start) + "ms");
	    		//Debug.stopMethodTracing();
	    		avgInsertTime = 0;
				adapter.notifyDataSetChanged();
	    	}
		}
		mHandler.removeCallbacks(mFirstScrollRunnable);
		mHandler.removeCallbacks(mUpdateTopUnreadRunnable);
		mHandler.post(mFirstScrollRunnable);
		mHandler.postDelayed(mUpdateTopUnreadRunnable, 100);
		if(conn.getReconnectTimestamp() == 0)
			conn.schedule_idle_timer();
	}

	private Runnable mFirstScrollRunnable = new Runnable() {
		@Override
		public void run() {
			if(adapter != null && adapter.data.size() > 0) {
				if(firstScroll) {
					try {
						getListView().setSelection(adapter.data.size() - 1);
						firstScroll = false;
					} catch (IllegalStateException e) {
						//The list view isn't ready yet
					}
				}
			}
		}
	};
	
	private Runnable mUpdateTopUnreadRunnable = new Runnable() {
		@Override
		public void run() {
			if(adapter != null) {
				try {
					int markerPos = adapter.getLastSeenEIDPosition();
		    		if(markerPos >= 0 && getListView().getFirstVisiblePosition() > (markerPos + 1)) {
		    			if(shouldTrackUnread()) {
		    				int highlights = adapter.getUnreadHighlightsAbovePosition(getListView().getFirstVisiblePosition());
		    				int count = (getListView().getFirstVisiblePosition() - markerPos - 1) - highlights;
		    				String txt = "";
		    				if(highlights > 0) {
			    				if(highlights == 1)
			    					txt = "mention";
			    				else if(highlights > 0)
			    					txt = "mentions";
			    				highlightsTopLabel.setText(String.valueOf(highlights));
		    					highlightsTopLabel.setVisibility(View.VISIBLE);
		    					
		    					if(count > 0)
		    						txt += " and ";
		    				} else {
		    					highlightsTopLabel.setVisibility(View.GONE);
		    				}
		    				if(count == 1)
		    					txt += count + " unread message";
		    				else if(count > 0)
		    					txt += count + " unread messages";
			    			unreadTopLabel.setText(txt);
			    			unreadTopView.setVisibility(View.VISIBLE);
		    			} else {
			    			unreadTopView.setVisibility(View.GONE);
		    			}
		    		} else {
		    			unreadTopView.setVisibility(View.GONE);
		    			if(adapter.data.size() > 0) {
			    			Long e = adapter.data.get(adapter.data.size() - 1).eid;
		    				if(heartbeatTask != null)
		    					heartbeatTask.cancel(true);
		    				heartbeatTask = new HeartbeatTask();
		    				heartbeatTask.execute(e);
		    			}
		    		}
		    		if(mServer != null)
		    			update_status(mServer.status, mServer.fail_info);
					if(mListener != null && !ready)
						mListener.onMessageViewReady();
					ready = true;
				} catch (IllegalStateException e) {
					//The list view wasn't on screen yet
				}
			}
		}
	};
	
	private boolean shouldTrackUnread() {
		if(conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null && conn.getUserInfo().prefs.has("channel-disableTrackUnread")) {
			try {
				JSONObject disabledMap = conn.getUserInfo().prefs.getJSONObject("channel-disableTrackUnread");
				if(disabledMap.has(String.valueOf(bid)) && disabledMap.getBoolean(String.valueOf(bid))) {
					return false;
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
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
			/*int minutes = (int)((System.currentTimeMillis() - newMsgTime)/60000);
			
			if(minutes < 1)
				unreadBottomLabel.setText("Less than a minute of chatter (");
			else if(minutes == 1)
				unreadBottomLabel.setText("1 minute of chatter (");
			else
				unreadBottomLabel.setText(minutes + " minutes of chatter (");
			if(newMsgs == 1)
				unreadBottomLabel.setText(unreadBottomLabel.getText() + "1 message)");
			else
				unreadBottomLabel.setText(unreadBottomLabel.getText() + (newMsgs + " messages)"));*/
			
			String txt = "";
			int msgCnt = newMsgs - newHighlights;
			if(newHighlights > 0) {
				if(newHighlights == 1)
					txt = "mention";
				else
					txt = "mentions";
				if(msgCnt > 0)
					txt += " and ";
				highlightsBottomLabel.setText(String.valueOf(newHighlights));
				highlightsBottomLabel.setVisibility(View.VISIBLE);
			} else {
				highlightsBottomLabel.setVisibility(View.GONE);
			}
			if(msgCnt == 1)
				txt += msgCnt + " unread message";
			else if(msgCnt > 0)
				txt += msgCnt + " unread messages";
			unreadBottomLabel.setText(txt);
			unreadBottomView.setVisibility(View.VISIBLE);
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
    		if(mServer != null && mServer.lag >= 2*1000*1000) {
	    		statusView.setVisibility(View.VISIBLE);
	    		statusView.setText("Slow ping response from " + mServer.hostname + " (" + (mServer.lag / 1000 / 1000) + "s)");
    		} else {
	    		statusView.setVisibility(View.GONE);
	    		statusView.setText("");
    		}
    	} else if(status.equals("quitting")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Disconnecting");
    		statusView.setTextColor(getResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("disconnected")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Disconnected. Tap to reconnect.");
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
    		statusView.setText("Connection temporarily unavailable");
    		statusView.setTextColor(getResources().getColor(R.color.status_fail_text));
    		statusView.setBackgroundResource(R.drawable.status_fail_bg);
    	} else if(status.equals("waiting_to_retry")) {
    		try {
	    		statusView.setVisibility(View.VISIBLE);
	    		long seconds = (fail_info.get("timestamp").getAsLong() + fail_info.get("retry_timeout").getAsInt()) - System.currentTimeMillis()/1000;
	    		if(seconds > 0) {
		    		String text = "Disconnected";
		    		if(fail_info.has("reason") && fail_info.get("reason").getAsString().length() > 0)
		    			text += ": " + fail_info.get("reason").getAsString() + ". ";
		    		else
		    			text += "; ";
		    		text += "Reconnecting in " + seconds + " seconds.";
		    		statusView.setText(text);
		    		statusView.setTextColor(getResources().getColor(R.color.status_fail_text));
		    		statusView.setBackgroundResource(R.drawable.status_fail_bg);
		    		statusRefreshRunnable = new StatusRefreshRunnable(status, fail_info);
	    		} else {
	        		statusView.setVisibility(View.VISIBLE);
	        		statusView.setText("Ready to connect, waiting our turn…");
	        		statusView.setTextColor(getResources().getColor(R.color.dark_blue));
	        		statusView.setBackgroundResource(R.drawable.background_blue);
	    		}
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

	private void update_global_msg() {
		if(globalMsgView != null) {
			if(conn != null && conn.globalMsg != null) {
				globalMsg.setText(conn.globalMsg);
				globalMsgView.setVisibility(View.VISIBLE);
			} else {
				globalMsgView.setVisibility(View.GONE);
			}
		}
	}
	
	@Override
    public void onPause() {
    	super.onPause();
		if(statusRefreshRunnable != null) {
			mHandler.removeCallbacks(statusRefreshRunnable);
			statusRefreshRunnable = null;
		}
    	if(conn != null)
    		conn.removeHandler(mHandler);
		TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1, Animation.RELATIVE_TO_SELF, -1);
		anim.setDuration(10);
		anim.setFillAfter(true);
		anim.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationEnd(Animation arg0) {
				connecting.setVisibility(View.GONE);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}

			@Override
			public void onAnimationStart(Animation animation) {
			}
			
		});
		connecting.startAnimation(anim);
		error = null;
		try {
			if(getListView().getLastVisiblePosition() < adapter.getCount())
				savedScrollPos = getListView().getFirstVisiblePosition();
			else
				savedScrollPos = -1;
			getListView().setOnScrollListener(null);
		} catch (Exception e) {
			savedScrollPos = -1;
		}
   	}
    
	private void updateReconnecting() {
		if(conn.getState() == NetworkConnection.STATE_CONNECTED) {
			connectingMsg.setText("Loading");
		} else if(conn.getState() == NetworkConnection.STATE_CONNECTING || conn.getReconnectTimestamp() > 0) {
			progressBar.setIndeterminate(true);
			if(connecting.getVisibility() == View.GONE) {
				TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1, Animation.RELATIVE_TO_SELF, 0);
				anim.setDuration(200);
				anim.setFillAfter(true);
				connecting.startAnimation(anim);
				connecting.setVisibility(View.VISIBLE);
			}
			if(conn.getState() == NetworkConnection.STATE_DISCONNECTED && conn.getReconnectTimestamp() > 0) {
	    		String plural = "";
	    		int seconds = (int)((conn.getReconnectTimestamp() - System.currentTimeMillis()) / 1000);
	    		if(seconds != 1)
	    			plural = "s";
	    		if(seconds < 1) {
	    			connectingMsg.setText("Connecting");
					errorMsg.setVisibility(View.GONE);
	    		} else if(seconds > 10 && error != null) {
	    			connectingMsg.setText("Reconnecting in " + seconds + " second" + plural);
					errorMsg.setText(error);
					errorMsg.setVisibility(View.VISIBLE);
	    		} else {
					connectingMsg.setText("Reconnecting in " + seconds + " second" + plural);
					errorMsg.setVisibility(View.GONE);
					error = null;
	    		}
				if(countdownTimer != null)
					countdownTimer.cancel();
				countdownTimer = new Timer();
				countdownTimer.schedule( new TimerTask(){
		             public void run() {
		    			 if(conn.getState() == NetworkConnection.STATE_DISCONNECTED) {
		    				 mHandler.post(new Runnable() {
								@Override
								public void run() {
				 					updateReconnecting();
								}
		    				 });
		    			 }
		    			 countdownTimer = null;
		             }
				}, 1000);
			} else {
				connectingMsg.setText("Connecting");
				error = null;
				errorMsg.setVisibility(View.GONE);
			}
    	} else {
			connectingMsg.setText("Offline");
			progressBar.setIndeterminate(false);
			progressBar.setProgress(0);
    	}
    }
    
	private final Handler mHandler = new Handler() {
		IRCCloudJSONObject e;
		
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_PROGRESS:
				float progress = (Float)msg.obj;
				progressBar.setIndeterminate(false);
				progressBar.setProgress((int)progress);
				break;
			case NetworkConnection.EVENT_BACKLOG_END:
				if(connecting.getVisibility() == View.VISIBLE) {
					TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1);
					anim.setDuration(200);
					anim.setFillAfter(true);
					anim.setAnimationListener(new AnimationListener() {

						@Override
						public void onAnimationEnd(Animation arg0) {
							connecting.setVisibility(View.GONE);
						}

						@Override
						public void onAnimationRepeat(Animation animation) {
						}

						@Override
						public void onAnimationStart(Animation animation) {
						}
						
					});
					connecting.startAnimation(anim);
					error = null;
				}
				if(bid != -1) {
					BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBuffer(bid);
					if(b != null)
						last_seen_eid = b.last_seen_eid;
				}
			case NetworkConnection.EVENT_CONNECTIVITY:
				updateReconnecting();
			case NetworkConnection.EVENT_USERINFO:
	            if(refreshTask != null)
	            	refreshTask.cancel(true);
				refreshTask = new RefreshTask();
				refreshTask.execute((Void)null);
				break;
			case NetworkConnection.EVENT_FAILURE_MSG:
				IRCCloudJSONObject o = (IRCCloudJSONObject)msg.obj;
				try {
					error = o.getString("message");
					if(error.equals("temp_unavailable"))
						error = "Your account is temporarily unavailable";
					updateReconnecting();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case NetworkConnection.EVENT_CONNECTIONLAG:
				try {
					IRCCloudJSONObject object = (IRCCloudJSONObject)msg.obj;
					if(mServer != null && object.cid() == cid) {
						update_status(mServer.status, mServer.fail_info);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case NetworkConnection.EVENT_STATUSCHANGED:
				try {
					IRCCloudJSONObject object = (IRCCloudJSONObject)msg.obj;
					if(object.cid() == cid) {
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
		            if(refreshTask != null)
		            	refreshTask.cancel(true);
					refreshTask = new RefreshTask();
					refreshTask.execute((Void)null);
				}
				break;
			case NetworkConnection.EVENT_SETIGNORES:
				e = (IRCCloudJSONObject)msg.obj;
				if(e.cid() == cid) {
		            if(refreshTask != null)
		            	refreshTask.cancel(true);
					refreshTask = new RefreshTask();
					refreshTask.execute((Void)null);
				}
				break;
			case NetworkConnection.EVENT_HEARTBEATECHO:
				if(adapter != null && adapter.data.size() > 0) {
					BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBuffer(bid);
					if(b != null && last_seen_eid != b.last_seen_eid) {
						last_seen_eid = b.last_seen_eid;
						if(last_seen_eid == adapter.data.get(adapter.data.size() - 1).eid || !shouldTrackUnread()) {
			    			unreadTopView.setVisibility(View.GONE);
			    		}
					}
				}
				break;
			case NetworkConnection.EVENT_CHANNELTOPIC:
			case NetworkConnection.EVENT_JOIN:
			case NetworkConnection.EVENT_PART:
			case NetworkConnection.EVENT_NICKCHANGE:
			case NetworkConnection.EVENT_QUIT:
			case NetworkConnection.EVENT_KICK:
			case NetworkConnection.EVENT_CHANNELMODE:
			case NetworkConnection.EVENT_SELFDETAILS:
			case NetworkConnection.EVENT_USERMODE:
			case NetworkConnection.EVENT_USERCHANNELMODE:
				e = (IRCCloudJSONObject)msg.obj;
				if(e.bid() == bid) {
					EventsDataSource.Event event = EventsDataSource.getInstance().getEvent(e.eid(), e.bid());
					insertEvent(event, false, false);
				}
				break;
			case NetworkConnection.EVENT_BUFFERMSG:
				EventsDataSource.Event event = (EventsDataSource.Event)msg.obj;
				if(event.bid == bid) {
					if(event.from != null && event.from.equals(name) && event.reqid == -1) {
						adapter.clearPending();
					} else if(event.reqid != -1) {
						for(int i = 0; i < adapter.data.size(); i++) {
							EventsDataSource.Event e = adapter.data.get(i);
							if(e.reqid == event.reqid && e.pending) {
								if(i > 1) {
									EventsDataSource.Event p = adapter.data.get(i-1);
									if(p.row_type == ROW_TIMESTAMP) {
										adapter.data.remove(p);
										i--;
									}
								}
								adapter.data.remove(e);
								i--;
							}
						}
					}
					insertEvent(event, false, false);
				}
				break;
			case NetworkConnection.EVENT_AWAY:
			case NetworkConnection.EVENT_SELFBACK:
				if(mServer != null) {
		    		if(mServer.away != null && mServer.away.length() > 0) {
		    			awayTxt.setText("Away (" + mServer.away + ")");
		    			awayView.setVisibility(View.VISIBLE);
		    		} else {
		    			awayView.setVisibility(View.GONE);
		    		}
				}
	    		break;
			case NetworkConnection.EVENT_GLOBALMSG:
				update_global_msg();
				break;
			default:
				break;
			}
		}
	};
	
	public interface MessageViewListener extends OnBufferSelectedListener {
		public void onMessageViewReady();
		public boolean onMessageLongClicked(EventsDataSource.Event event);
		public void onMessageDoubleClicked(EventsDataSource.Event event);
	}
}
