package com.irccloud.androidnative;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.TreeMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;

public class MessageViewFragment extends SherlockListFragment {
	private NetworkConnection conn;
	private TextView awayView;
	private TextView statusView;
	private View headerViewContainer;
	private View headerView;
	private TextView unreadView;
	private int cid;
	private int bid;
	private long last_seen_eid;
	private long min_eid;
	private long earliest_eid;
	private String name;
	private String type;
	private boolean firstScroll = true;
	private boolean requestingBacklog = false;
	private float avgInsertTime = 0;
	private int newMsgs = 0;
	private long newMsgTime = 0;
	
	private static final int TYPE_TIMESTAMP = 0;
	private static final int TYPE_MESSAGE = 1;
	private static final int TYPE_BACKLOGMARKER = 2;
	
	private MessageAdapter adapter;
	
	private class MessageAdapter extends BaseAdapter {
		ArrayList<MessageEntry> data;
		private SherlockListFragment ctx;
		private long max_eid = 0;
		private long min_eid = 0;
		private int lastDay;
		
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
		}

		public MessageAdapter(SherlockListFragment context) {
			ctx = context;
			data = new ArrayList<MessageEntry>();
		}
		
		public void removeItem(long eid) {
			for(int i = 0; i < data.size(); i++) {
				if(data.get(i).eid == eid) {
					data.remove(i);
					i--;
				}
			}
		}

		public int insertBacklogMarker(int position) {
			MessageEntry e = new MessageEntry();
			e.type = TYPE_BACKLOGMARKER;
			e.bg_color = R.color.message_bg;
			for(int i = 0; i < data.size(); i++) {
				if(data.get(i).type == TYPE_BACKLOGMARKER) {
					data.remove(i);
				}
			}
			if(position > 0 && data.get(position - 1).type == TYPE_TIMESTAMP)
				position--;
			data.add(position, e);
			return position;
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
			return position;
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
				holder.message.setText(Html.fromHtml(e.text));
			}
			
			return row;
		}
	}
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	final View v = inflater.inflate(R.layout.messageview, container, false);
    	awayView = (TextView)v.findViewById(R.id.topicView);
    	statusView = (TextView)v.findViewById(R.id.statusView);
    	unreadView = (TextView)v.findViewById(R.id.unread);
    	unreadView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				getListView().setSelection(adapter.getCount() - 1);
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
				if(unreadView != null) {
					if(firstVisibleItem + visibleItemCount == totalItemCount) {
						unreadView.setVisibility(View.GONE);
						if(newMsgs > 0) {
							Long e = adapter.data.get(adapter.data.size() - 1).eid;
							new HeartbeatTask().execute(e);
						}
						newMsgs = 0;
						newMsgTime = 0;
					}
				}
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
    public void onSaveInstanceState(Bundle state) {
    	super.onSaveInstanceState(state);
    	state.putInt("cid", cid);
    	state.putLong("bid", bid);
    	state.putLong("last_seen_eid", last_seen_eid);
    	state.putLong("min_eid", min_eid);
    	state.putString("name", name);
    	state.putString("type", type);
    	state.putBoolean("firstScroll", firstScroll);
    	//TODO: serialize the adapter data
    }

    
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	if(activity.getIntent() != null && activity.getIntent().hasExtra("cid")) {
	    	cid = activity.getIntent().getIntExtra("cid", 0);
	    	bid = activity.getIntent().getIntExtra("bid", 0);
	    	last_seen_eid = activity.getIntent().getLongExtra("last_seen_eid", 0);
	    	min_eid = activity.getIntent().getLongExtra("min_eid", 0);
	    	name = activity.getIntent().getStringExtra("name");
	    	type = activity.getIntent().getStringExtra("type");
    	}
    }

    private void insertEvent(IRCCloudJSONObject event, boolean backlog) {
		try {
    		long start = System.currentTimeMillis();
	    	if(event.eid() == min_eid)
	    		headerView.setVisibility(View.GONE);
	    	if(event.eid() < earliest_eid)
	    		earliest_eid = event.eid();
	    	
	    	int color = R.color.row_message_label;
	    	int bg_color = R.color.message_bg;
	    	
	    	String from = "";
	    	String msg = "";
	    	String type;
			type = event.getString("type");
	    	
			if(type.equalsIgnoreCase("joined_channel") || type.equalsIgnoreCase("parted_channel") ||type.equalsIgnoreCase("nickchange") || type.equalsIgnoreCase("quit")) {
				if(conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null) {
					JSONObject hiddenMap = conn.getUserInfo().prefs.getJSONObject("channel-hideJoinPart");
					if(hiddenMap.has(String.valueOf(bid)) && hiddenMap.getBoolean(String.valueOf(bid))) {
			    		adapter.removeItem(event.eid());
				    	if(!backlog)
				    		adapter.notifyDataSetChanged();
				    	return;
					}
				}
			}
			
	    	if(type.equalsIgnoreCase("buffer_me_msg")) {
				from = "* <i>" + event.getString("from") + "</i>";
				msg = "<i>" + event.getString("msg") + "</i>";
	    	} else if(type.equalsIgnoreCase("nickname_in_use")) {
	    		from = event.getString("nick");
	    		msg = "is already in use";
	    		bg_color = R.color.error;
	    	} else if(type.equalsIgnoreCase("connecting_failed")) {
	    		msg = "Failed to connect: " + event.getString("reason");
	    		bg_color = R.color.error;
	    	} else if(type.equalsIgnoreCase("socket_closed")) {
	    		msg = "===================="; //TODO: Add another row type for socket_closed events
	    	} else if(type.equalsIgnoreCase("self_details")) {
	    		msg = "Your hostmask: <b>" + event.getString("usermask") + "</b>";
	    		bg_color = R.color.dateline_bg;
	    	} else if(type.equalsIgnoreCase("myinfo")) {
	    		msg = "Host: " + event.getString("server") + "\n";
	    		msg += "IRCd: " + event.getString("version") + "\n";
	    		msg += "User modes: " + event.getString("user_modes") + "\n";
	    		msg += "Channel modes: " + event.getString("channel_modes") + "\n";
	    		bg_color = R.color.dateline_bg;
	    	} else if(type.equalsIgnoreCase("user_mode")) {
	    		msg = "Your user mode is: <b>" + event.getString("diff") + "</b>";
	    		bg_color = R.color.dateline_bg;
	    	} else if(type.equalsIgnoreCase("channel_topic")) {
	    		from = event.getString("author");
	    		msg = "set the topic: " + event.getString("topic");
	    		bg_color = R.color.dateline_bg;
	    	} else if(type.equalsIgnoreCase("channel_mode")) {
	    		msg = "Channel mode set to: <b>" + event.getString("diff") + "</b>";
	    		bg_color = R.color.dateline_bg;
	    	} else if(type.equalsIgnoreCase("channel_mode_is")) {
	    		msg = "Channel mode is: <b>" + event.getString("diff") + "</b>";
	    		bg_color = R.color.dateline_bg;
	    	} else if(type.equalsIgnoreCase("joined_channel") || type.equalsIgnoreCase("you_joined_channel")) {
	    		from = "-&gt; " + event.getString("nick");
	    		msg = "joined (" + event.getString("hostmask") + ")";
	    		color = R.color.timestamp;
	    	} else if(type.equalsIgnoreCase("parted_channel") || type.equalsIgnoreCase("you_parted_channel")) {
	    		from = "&lt;- " + event.getString("nick");
	    		msg = "left (" + event.getString("hostmask") + ")";
	    		color = R.color.timestamp;
	    	} else if(type.equalsIgnoreCase("kicked_channel") || type.equalsIgnoreCase("you_kicked_channel")) {
	    		from = "&lt;- " + event.getString("nick");
	    		msg = "was kicked by " + event.getString("kicker") + " (" + event.getString("kicker_hostmask") + ")";
	    		color = R.color.timestamp;
	    	} else if(type.equalsIgnoreCase("nickchange") || type.equalsIgnoreCase("you_nickchange")) {
	    		from = event.getString("oldnick");
	    		msg = "-&gt; " + event.getString("newnick");
	    		color = R.color.timestamp;
	    	} else if(type.equalsIgnoreCase("quit") || type.equalsIgnoreCase("quit_server")) {
	    		from = "&lt;= " + event.getString("nick");
	    		if(event.has("hostmask"))
	    			msg = "quit (" + event.getString("hostmask") + ") " + event.getString("msg");
	    		else
	    			msg = "quit: " + event.getString("msg");
	    		color = R.color.timestamp;
	    	} else if(type.equalsIgnoreCase("user_channel_mode")) {
	    		from = "+++ " + event.getString("from");
	    		msg = "set mode: <b>" + event.getString("diff") + " " + event.getString("nick") + "</b>";
	    	} else if(type.equalsIgnoreCase("channel_mode_list_change")) {
	    		from = "+++ " + event.getString("from");
	    		msg = "set mode: <b>" + event.getString("diff") + "</b>";
	    	} else if(type.equalsIgnoreCase("motd_response") || type.equalsIgnoreCase("server_motd")) {
	    	} else if(type.equalsIgnoreCase("inviting_to_channel")) {
	    		msg = "You invited " + event.getString("recipient") + " to join " + event.getString("channel");
	    		bg_color = R.color.notice;
	    	} else {
	    		if(event.has("from"))
	    			from = event.getString("from");
	    		if(event.has("msg"))
	    			msg = event.getString("msg");
	    	}
	    	
	    	if(event.has("value")) {
	    		msg = event.getString("value") + " " + msg;
	    	}
	    	
	    	from = TextUtils.htmlEncode(from);
	    	msg = TextUtils.htmlEncode(msg);
	    	
	    	if(from.length() > 0)
	    		msg = "<b>" + from + "</b> " + msg;
	    	
	    	if(event.has("highlight") && event.getBoolean("highlight"))
	    		bg_color = R.color.highlight;
	    	
	    	if(event.has("self") && event.getBoolean("self"))
	    		bg_color = R.color.self;
	    	
	    	adapter.addItem(TYPE_MESSAGE, event.getLong("eid"), msg, color, bg_color);
	    	if(!backlog)
	    		adapter.notifyDataSetChanged();
	    	long time = (System.currentTimeMillis() - start);
	    	if(avgInsertTime == 0)
	    		avgInsertTime = time;
	    	avgInsertTime += time;
	    	avgInsertTime /= 2.0;
	    	//Log.i("IRCCloud", "Average insert time: " + avgInsertTime);
	    	if(!backlog && getListView().getLastVisiblePosition() < (adapter.getCount() - 1)) {
	    		if(newMsgTime == 0)
	    			newMsgTime = System.currentTimeMillis();
	    		newMsgs++;
	    		update_unread();
	    	}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    public void onResume() {
    	super.onResume();
        getListView().setStackFromBottom(true);
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
    		UsersDataSource.User user = null;
			if(type.equalsIgnoreCase("conversation"))
				user = UsersDataSource.getInstance().getUser(cid, name);
			refresh(events, server, buffer, user);
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
	    	}
			return null;
		}
    }
    
	private class RefreshTask extends AsyncTask<Void, Void, Void> {
		TreeMap<Long,IRCCloudJSONObject> events;
		ServersDataSource.Server server;
		BuffersDataSource.Buffer buffer;
		UsersDataSource.User user = null;
		
		@Override
		protected Void doInBackground(Void... params) {
			buffer = BuffersDataSource.getInstance().getBuffer((int)bid);
			server = ServersDataSource.getInstance().getServer(cid);
			if(type.equalsIgnoreCase("conversation"))
				user = UsersDataSource.getInstance().getUser(cid, name);
			long time = System.currentTimeMillis();
			events = EventsDataSource.getInstance().getEventsForBuffer((int)bid);
			Log.i("IRCCloud", "Loaded data in " + (System.currentTimeMillis() - time) + "ms");
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if(events != null && events.size() > 0) {
				int oldSize = adapter.data.size();
				int oldPosition = getListView().getFirstVisiblePosition();
				refresh(events, server, buffer, user);
				if(oldSize > 1 && adapter.data.size() > oldSize && requestingBacklog) {
					int markerPos = adapter.insertBacklogMarker(adapter.data.size() - oldSize + 1);
					adapter.notifyDataSetChanged();
					getListView().setSelectionFromTop(oldPosition + markerPos + 1, headerViewContainer.getHeight());
				}
			}
			requestingBacklog = false;
		}
	}

	private void refresh(TreeMap<Long,IRCCloudJSONObject> events, ServersDataSource.Server server, BuffersDataSource.Buffer buffer, UsersDataSource.User user) {
		if(events == null || (events.size() == 0 && min_eid > 0)) {
			if(bid != -1) {
				requestingBacklog = true;
				conn.request_backlog(cid, bid, 0);
			}
		} else if(events.size() > 0){
			earliest_eid = events.firstKey();
			if(events.firstKey() > min_eid) {
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
	    		adapter.notifyDataSetChanged();
	    		Log.i("IRCCloud", "Backlog rendering took: " + (System.currentTimeMillis() - start) + "ms");
	    		new HeartbeatTask().execute(events.get(events.lastKey()).eid());
	    		avgInsertTime = 0;
	    	}
		}
    	if(type.equalsIgnoreCase("conversation") && buffer != null && buffer.away_msg != null && buffer.away_msg.length() > 0) {
    		awayView.setVisibility(View.VISIBLE);
    		awayView.setText("Away: " + buffer.away_msg);
    	} else if(type.equalsIgnoreCase("conversation") && user != null && user.away == 1) {
    		awayView.setVisibility(View.VISIBLE);
    		if(user.away_msg != null && user.away_msg.length() > 0)
    			awayView.setText("Away: " + user.away_msg);
    		else
	    		awayView.setText("Away");
    	} else {
    		awayView.setVisibility(View.GONE);
    		awayView.setText("");
    	}
    	try {
			update_status(server.status, new JSONObject(server.fail_info));
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		JSONObject fail_info;
		
		public StatusRefreshRunnable(String status, JSONObject fail_info) {
			this.status = status;
			this.fail_info = fail_info;
		}

		@Override
		public void run() {
			update_status(status, fail_info);
		}
	}
	
	StatusRefreshRunnable statusRefreshRunnable = null;
	
	private void update_status(String status, JSONObject fail_info) {
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
	    		long seconds = (fail_info.getLong("timestamp") + fail_info.getInt("retry_timeout")) - System.currentTimeMillis()/1000;
	    		statusView.setText("Disconnected: " + fail_info.getString("reason") + ". Reconnecting in " + seconds + " seconds.");
	    		statusView.setTextColor(getResources().getColor(R.color.status_fail_text));
	    		statusView.setBackgroundResource(R.drawable.status_fail_bg);
	    		statusRefreshRunnable = new StatusRefreshRunnable(status, fail_info);
	    		mHandler.postDelayed(statusRefreshRunnable, 500);
    		} catch (JSONException e) {
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
						update_status(object.getString("new_status"), object.getJSONObject("fail_info"));
					}
				} catch (JSONException e) {
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
			case NetworkConnection.EVENT_SELFBACK:
		    	try {
					e = (IRCCloudJSONObject)msg.obj;
					if(e.cid() == cid && e.getString("nick").equalsIgnoreCase(name)) {
			    		awayView.setVisibility(View.GONE);
						awayView.setText("");
					}
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
				break;
			case NetworkConnection.EVENT_AWAY:
		    	try {
					e = (IRCCloudJSONObject)msg.obj;
					if((e.bid() == bid || (e.type().equalsIgnoreCase("self_away") && e.cid() == cid)) && e.getString("nick").equalsIgnoreCase(name)) {
			    		awayView.setVisibility(View.VISIBLE);
						awayView.setText("Away: " + e.getString("msg"));
					}
				} catch (JSONException e1) {
					e1.printStackTrace();
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
					if(getListView().getLastVisiblePosition() == (adapter.getCount() - 1))
						new HeartbeatTask().execute(e.eid());
				}
				break;
			default:
				break;
			}
		}
	};
}
