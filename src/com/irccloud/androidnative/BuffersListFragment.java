package com.irccloud.androidnative;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;

public class BuffersListFragment extends SherlockListFragment {
	private static final int TYPE_SERVER = 0;
	private static final int TYPE_CHANNEL = 1;
	private static final int TYPE_CONVERSATION = 2;
	private static final int TYPE_ARCHIVES_HEADER = 3;
	
	NetworkConnection conn;
	BufferListAdapter adapter;
	OnBufferSelectedListener mListener;
	View view;
	TextView errorMsg;
	ListView listView = null;
	RelativeLayout connecting = null;
	LinearLayout topUnreadIndicator = null;
	LinearLayout topUnreadIndicatorColor = null;
	LinearLayout bottomUnreadIndicator = null;
	LinearLayout bottomUnreadIndicatorColor = null;
	String error = null;
	private Timer countdownTimer = null;
	
	int firstUnreadPosition = -1;
	int lastUnreadPosition= -1;
	int firstHighlightPosition = -1;
	int lastHighlightPosition= -1;
	
	SparseBooleanArray mExpandArchives = new SparseBooleanArray();
	
	private static class BufferListEntry implements Serializable {
		private static final long serialVersionUID = 1848168221883194027L;
		int cid;
		int bid;
		int type;
		int unread;
		int highlights;
		int key;
		long last_seen_eid;
		long min_eid;
		int joined;
		int archived;
		String name;
		String status;
	}

	private class BufferListAdapter extends BaseAdapter {
		ArrayList<BufferListEntry> data;
		private SherlockListFragment ctx;
		int progressRow = -1;
		
		private class ViewHolder {
			int type;
			TextView label;
			TextView highlights;
			LinearLayout unread;
			LinearLayout groupbg;
			LinearLayout bufferbg;
			ImageView key;
			ProgressBar progress;
			ImageButton addBtn;
		}

		public void showProgress(int row) {
			progressRow = row;
			notifyDataSetChanged();
		}
		
		public BufferListAdapter(SherlockListFragment context) {
			ctx = context;
			data = new ArrayList<BufferListEntry>();
		}
		
		public void setItems(ArrayList<BufferListEntry> items) {
			data = items;
		}
		
		int unreadPositionAbove(int pos) {
			for(int i = pos-1; i >= 0; i--) {
				BufferListEntry e = data.get(i);
				if(e.unread > 0)
					return i;
			}
			return 0;
		}
		
		int unreadPositionBelow(int pos) {
			for(int i = pos; i < data.size(); i++) {
				BufferListEntry e = data.get(i);
				if(e.unread > 0)
					return i;
			}
			return data.size() - 1;
		}
		
		public BufferListEntry buildItem(int cid, int bid, int type, String name, int key, int unread, int highlights, long last_seen_eid, long min_eid, int joined, int archived, String status) {
			BufferListEntry e = new BufferListEntry();
			e.cid = cid;
			e.bid = bid;
			e.type = type;
			e.name = name;
			e.key = key;
			e.unread = unread;
			e.highlights = highlights;
			e.last_seen_eid = last_seen_eid;
			e.min_eid = min_eid;
			e.joined = joined;
			e.archived = archived;
			e.status = status;
			return e;
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
			BufferListEntry e = data.get(position);
			return e.bid;
		}

		@SuppressWarnings("deprecation")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			BufferListEntry e = data.get(position);
			View row = convertView;
			ViewHolder holder;

			if(row != null && ((ViewHolder)row.getTag()).type != e.type)
				row = null;
			
			if (row == null) {
				LayoutInflater inflater = ctx.getLayoutInflater(null);
				if(e.type == TYPE_SERVER)
					row = inflater.inflate(R.layout.row_buffergroup, null);
				else
					row = inflater.inflate(R.layout.row_buffer, null);

				holder = new ViewHolder();
				holder.label = (TextView) row.findViewById(R.id.label);
				holder.highlights = (TextView) row.findViewById(R.id.highlights);
				holder.unread = (LinearLayout) row.findViewById(R.id.unread);
				holder.groupbg = (LinearLayout) row.findViewById(R.id.groupbg);
				holder.bufferbg = (LinearLayout) row.findViewById(R.id.bufferbg);
				holder.key = (ImageView) row.findViewById(R.id.key);
				holder.progress = (ProgressBar) row.findViewById(R.id.progressBar);
				holder.addBtn = (ImageButton) row.findViewById(R.id.addBtn);
				holder.type = e.type;

				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}

			holder.label.setText(e.name);
			if(e.type == TYPE_ARCHIVES_HEADER) {
				holder.label.setTypeface(null);
				holder.label.setTextColor(getResources().getColorStateList(R.color.row_label_archives_heading));
				holder.unread.setBackgroundDrawable(null);
				if(mExpandArchives.get(e.cid, false)) {
					holder.bufferbg.setBackgroundResource(R.drawable.row_buffer_bg_archived);
					holder.bufferbg.setSelected(true);
				} else {
					holder.bufferbg.setBackgroundResource(R.drawable.row_buffer_bg);
					holder.bufferbg.setSelected(false);
				}
			} else if(e.archived == 1 && holder.bufferbg != null) {
				holder.label.setTypeface(null);
				holder.label.setTextColor(getResources().getColorStateList(R.color.row_label_archived));
				holder.bufferbg.setBackgroundResource(R.drawable.row_buffer_bg_archived);
				holder.unread.setBackgroundDrawable(null);
			} else if((e.type == TYPE_CHANNEL && e.joined == 0) || !e.status.equals("connected_ready")) {
				holder.label.setTypeface(null);
				holder.label.setTextColor(getResources().getColorStateList(R.color.row_label_inactive));
				holder.unread.setBackgroundDrawable(null);
				if(holder.bufferbg != null)
					holder.bufferbg.setBackgroundResource(R.drawable.row_buffer_bg);
			} else if(e.unread > 0) {
				holder.label.setTypeface(null, Typeface.BOLD);
				holder.label.setTextColor(getResources().getColorStateList(R.color.row_label_unread));
				holder.unread.setBackgroundResource(R.drawable.selected_blue);
				if(holder.bufferbg != null)
					holder.bufferbg.setBackgroundResource(R.drawable.row_buffer_bg);
			} else {
				holder.label.setTypeface(null);
				holder.label.setTextColor(getResources().getColorStateList(R.color.row_label));
				holder.unread.setBackgroundDrawable(null);
				if(holder.bufferbg != null)
					holder.bufferbg.setBackgroundResource(R.drawable.row_buffer_bg);
			}

			if(conn.getState() != NetworkConnection.STATE_CONNECTED)
				row.setBackgroundResource(R.drawable.disconnected_yellow);
			else
				row.setBackgroundResource(R.drawable.bg);
			
			if(holder.key != null) {
				if(e.key > 0) {
					holder.key.setVisibility(View.VISIBLE);
				} else {
					holder.key.setVisibility(View.INVISIBLE);
				}
			}
			
			if(holder.progress != null) {
				if(progressRow == position || (e.type == TYPE_SERVER && !(e.status.equals("connected_ready") || e.status.equals("quitting") || e.status.equals("disconnected")))) {
					holder.progress.setVisibility(View.VISIBLE);
				} else {
					holder.progress.setVisibility(View.GONE);
				}
			}
			
			if(holder.groupbg != null) {
				if(e.status.equals("waiting_to_retry") || e.status.equals("pool_unavailable")) {
					holder.groupbg.setBackgroundResource(R.drawable.operator_bg_red);
					holder.label.setTextColor(getResources().getColorStateList(R.color.heading_operators));
				} else {
					holder.groupbg.setBackgroundResource(R.drawable.row_buffergroup_bg);
				}
			}
			
			if(holder.highlights != null) {
				if(e.highlights > 0) {
					holder.highlights.setVisibility(View.VISIBLE);
					holder.highlights.setText("(" + e.highlights + ")");
				} else {
					holder.highlights.setVisibility(View.GONE);
					holder.highlights.setText("");
				}
			}
			
			if(holder.addBtn != null) {
				holder.addBtn.setTag(e);
				holder.addBtn.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						BufferListEntry e = (BufferListEntry)v.getTag();
			        	AddChannelFragment newFragment = new AddChannelFragment();
			        	newFragment.setDefaultCid(e.cid);
			            newFragment.show(getActivity().getSupportFragmentManager(), "dialog");
					}
				});
			}
			
			return row;
		}
	}
	
	private class RefreshTask extends AsyncTask<Void, Void, Void> {
		ArrayList<BufferListEntry> entries = new ArrayList<BufferListEntry>();
		
		@Override
		protected Void doInBackground(Void... params) {
			ArrayList<ServersDataSource.Server> servers = ServersDataSource.getInstance().getServers();
			if(adapter == null) {
				adapter = new BufferListAdapter(BuffersListFragment.this);
			}

			firstUnreadPosition = -1;
			lastUnreadPosition = -1;
			firstHighlightPosition = -1;
			lastHighlightPosition = -1;
			int position = 0;
			
			for(int i = 0; i < servers.size(); i++) {
				ServersDataSource.Server s = servers.get(i);
				ArrayList<BuffersDataSource.Buffer> buffers = BuffersDataSource.getInstance().getBuffersForServer(s.cid);
				for(int j = 0; j < buffers.size(); j++) {
					BuffersDataSource.Buffer b = buffers.get(j);
					if(b.type.equalsIgnoreCase("console")) {
						int unread = EventsDataSource.getInstance().getUnreadCountForBuffer(b.bid, b.last_seen_eid, b.type);
						int highlights = EventsDataSource.getInstance().getHighlightCountForBuffer(b.bid, b.last_seen_eid);
						if(s.name.length() == 0)
							s.name = s.hostname;
						entries.add(adapter.buildItem(b.cid, b.bid, TYPE_SERVER, s.name, 0, unread, highlights, b.last_seen_eid, b.min_eid, 1, b.archived, s.status));
						if(unread > 0 && firstUnreadPosition == -1)
							firstUnreadPosition = position;
						if(unread > 0 && (lastUnreadPosition == -1 || lastUnreadPosition < position))
							lastUnreadPosition = position;
						if(highlights > 0 && firstHighlightPosition == -1)
							firstHighlightPosition = position;
						if(highlights > 0 && (lastHighlightPosition == -1 || lastHighlightPosition < position))
							lastHighlightPosition = position;
						position++;
						break;
					}
				}
				for(int j = 0; j < buffers.size(); j++) {
					BuffersDataSource.Buffer b = buffers.get(j);
					int type = -1;
					int key = 0;
					int joined = 1;
					if(b.type.equalsIgnoreCase("channel")) {
						type = TYPE_CHANNEL;
						ChannelsDataSource.Channel c = ChannelsDataSource.getInstance().getChannelForBuffer(b.bid);
						if(c == null)
							joined = 0;
						if(c != null && c.mode != null && c.mode.contains("k"))
							key = 1;
					}
					else if(b.type.equalsIgnoreCase("conversation"))
						type = TYPE_CONVERSATION;
					if(type > 0 && b.archived == 0) {
						int unread = EventsDataSource.getInstance().getUnreadCountForBuffer(b.bid, b.last_seen_eid, b.type);
						int highlights = EventsDataSource.getInstance().getHighlightCountForBuffer(b.bid, b.last_seen_eid);
						if(conn.getUserInfo() != null && conn.getUserInfo().prefs != null && conn.getUserInfo().prefs.has("channel-disableTrackUnread")) {
							try {
								JSONObject disabledMap = conn.getUserInfo().prefs.getJSONObject("channel-disableTrackUnread");
								if(disabledMap.has(String.valueOf(b.bid)) && disabledMap.getBoolean(String.valueOf(b.bid)))
									unread = 0;
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						entries.add(adapter.buildItem(b.cid, b.bid, type, b.name, key, unread, highlights, b.last_seen_eid, b.min_eid, joined, b.archived, s.status));
						if(unread > 0 && firstUnreadPosition == -1)
							firstUnreadPosition = position;
						if(unread > 0 && (lastUnreadPosition == -1 || lastUnreadPosition < position))
							lastUnreadPosition = position;
						if(highlights > 0 && firstHighlightPosition == -1)
							firstHighlightPosition = position;
						if(highlights > 0 && (lastHighlightPosition == -1 || lastHighlightPosition < position))
							lastHighlightPosition = position;
						position++;
					}
				}
				entries.add(adapter.buildItem(s.cid, 0, TYPE_ARCHIVES_HEADER, "Archives", 0, 0, 0, 0, 0, 0, 1, s.status));
				position++;
				if(mExpandArchives.get(s.cid, false)) {
					for(int j = 0; j < buffers.size(); j++) {
						BuffersDataSource.Buffer b = buffers.get(j);
						int type = -1;
						if(b.archived == 1) {
							if(b.type.equalsIgnoreCase("channel"))
								type = TYPE_CHANNEL;
							else if(b.type.equalsIgnoreCase("conversation"))
								type = TYPE_CONVERSATION;
							
							if(type > 0) {
								entries.add(adapter.buildItem(b.cid, b.bid, type, b.name, 0, 0, 0, b.last_seen_eid, b.min_eid, 0, b.archived, s.status));
								position++;
							}
						}
					}
				}
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			adapter.setItems(entries);
			
			if(getListAdapter() == null && entries.size() > 0)
				setListAdapter(adapter);
			else
				adapter.notifyDataSetChanged();
			
			if(entries.size() > 0 && connecting != null) {
				connecting.setVisibility(View.GONE);
			}
			
			if(listView != null)
				updateUnreadIndicators(listView.getFirstVisiblePosition(), listView.getLastVisiblePosition());
			else //The activity view isn't ready yet, try again
				new RefreshTask().execute((Void)null);
		}
	}

	private void updateUnreadIndicators(int first, int last) {
		if(topUnreadIndicator != null) {
			if(firstUnreadPosition != -1 && first >= firstUnreadPosition) {
				topUnreadIndicator.setVisibility(View.VISIBLE);
				topUnreadIndicatorColor.setBackgroundResource(R.drawable.selected_blue);
			} else {
				topUnreadIndicator.setVisibility(View.GONE);
			}
			if((lastHighlightPosition != -1 && first >= lastHighlightPosition) ||
					(firstHighlightPosition != -1 && first >= firstHighlightPosition)) {
				topUnreadIndicator.setVisibility(View.VISIBLE);
				topUnreadIndicatorColor.setBackgroundResource(R.drawable.highlight_red);
			}
		}
		if(bottomUnreadIndicator != null) {
			if(lastUnreadPosition != -1 && last <= lastUnreadPosition) {
				bottomUnreadIndicator.setVisibility(View.VISIBLE);
				bottomUnreadIndicatorColor.setBackgroundResource(R.drawable.selected_blue);
			} else {
				bottomUnreadIndicator.setVisibility(View.GONE);
			}
			if((firstHighlightPosition != -1 && last <= firstHighlightPosition) ||
					(lastHighlightPosition != -1 && last <= lastHighlightPosition)) {
				bottomUnreadIndicator.setVisibility(View.VISIBLE);
				bottomUnreadIndicatorColor.setBackgroundResource(R.drawable.highlight_red);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState.containsKey("data")) {
        	adapter = new BufferListAdapter(this);
        	adapter.setItems((ArrayList<BufferListEntry>) savedInstanceState.getSerializable("data"));
        	setListAdapter(adapter);
        }
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
	        Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.bufferslist, null);
		errorMsg = (TextView)view.findViewById(R.id.errorMsg);
		connecting = (RelativeLayout)view.findViewById(R.id.connecting);
		topUnreadIndicator = (LinearLayout)view.findViewById(R.id.topUnreadIndicator);
		topUnreadIndicator.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				int scrollTo = adapter.unreadPositionAbove(getListView().getFirstVisiblePosition());
				if(scrollTo > 0)
					getListView().setSelection(scrollTo-1);
				else
					getListView().setSelection(0);

				updateUnreadIndicators(getListView().getFirstVisiblePosition(), getListView().getLastVisiblePosition());
			}
			
		});
		topUnreadIndicatorColor = (LinearLayout)view.findViewById(R.id.topUnreadIndicatorColor);
		bottomUnreadIndicator = (LinearLayout)view.findViewById(R.id.bottomUnreadIndicator);
		bottomUnreadIndicator.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				int offset = getListView().getLastVisiblePosition() - getListView().getFirstVisiblePosition();
				int scrollTo = adapter.unreadPositionBelow(getListView().getLastVisiblePosition()) - offset + 2;
				if(scrollTo < adapter.getCount())
					getListView().setSelection(scrollTo);
				else
					getListView().setSelection(adapter.getCount() - 1);
				
				updateUnreadIndicators(getListView().getFirstVisiblePosition(), getListView().getLastVisiblePosition());
			}
			
		});
		bottomUnreadIndicatorColor = (LinearLayout)view.findViewById(R.id.bottomUnreadIndicatorColor);
		listView = (ListView)view.findViewById(android.R.id.list);
		listView.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				updateUnreadIndicators(firstVisibleItem, firstVisibleItem+visibleItemCount-1);
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}
		});
		return view;
	}
	
    @Override
    public void onSaveInstanceState(Bundle state) {
    	if(adapter != null && adapter.data != null && adapter.data.size() > 0) {
    		state.putSerializable("data", adapter.data);
    	}
    }
	
    public void onResume() {
    	super.onResume();
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
		if(conn.getState() != NetworkConnection.STATE_CONNECTED) {
			view.setBackgroundResource(R.drawable.disconnected_yellow);
		} else {
			view.setBackgroundResource(R.drawable.background_blue);
			connecting.setVisibility(View.GONE);
			getListView().setVisibility(View.VISIBLE);
		}
		if(adapter != null)
			adapter.showProgress(-1);
    	new RefreshTask().execute((Void)null);
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	if(conn != null)
    		conn.removeHandler(mHandler);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnBufferSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnBufferSelectedListener");
        }
    }
    
    public void onListItemClick(ListView l, View v, int position, long id) {
    	BufferListEntry e = (BufferListEntry)adapter.getItem(position);
    	String type = null;
    	switch(e.type) {
    	case TYPE_ARCHIVES_HEADER:
    		mExpandArchives.put(e.cid, !mExpandArchives.get(e.cid, false));
	    	new RefreshTask().execute((Void)null);
    		return;
    	case TYPE_SERVER:
    		type = "console";
    		break;
    	case TYPE_CHANNEL:
    		type = "channel";
    		break;
    	case TYPE_CONVERSATION:
    		type = "conversation";
    		break;
    	}
    	adapter.showProgress(position);
    	mListener.onBufferSelected(e.cid, e.bid, e.name, e.last_seen_eid, e.min_eid, type, e.joined, e.archived, e.status);
    }
    
	private void updateReconnecting() {
    	if(conn.getReconnectTimestamp() > 0) {
    		String plural = "";
    		int seconds = (int)((conn.getReconnectTimestamp() - System.currentTimeMillis()) / 1000);
    		if(seconds != 1)
    			plural = "s";
    		if(seconds < 1)
    			errorMsg.setText("Connecting");
    		else if(seconds > 10 && error != null)
				errorMsg.setText(error +"\n\nReconnecting in\n" + seconds + " second" + plural);
			else
				errorMsg.setText("Reconnecting in\n" + seconds + " second" + plural);
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
			errorMsg.setText("Offline");
    	}
    }
    
    @SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_CONNECTIVITY:
				if(conn.getState() != NetworkConnection.STATE_CONNECTED) {
					view.setBackgroundResource(R.drawable.disconnected_yellow);
				} else {
					view.setBackgroundResource(R.drawable.background_blue);
					errorMsg.setText("Loading");
					error = null;
				}
				if(conn.getState() == NetworkConnection.STATE_CONNECTING) {
					errorMsg.setText("Connecting");
					error = null;
				}
				else if(conn.getState() == NetworkConnection.STATE_DISCONNECTED)
					updateReconnecting();
				if(adapter != null)
					adapter.notifyDataSetChanged();
				break;
			case NetworkConnection.EVENT_FAILURE_MSG:
				IRCCloudJSONObject o = (IRCCloudJSONObject)msg.obj;
				if(conn.getState() != NetworkConnection.STATE_CONNECTED) {
					try {
						error = o.getString("message");
						if(error.equals("temp_unavailable"))
							error = "Your account is temporarily unavailable";
						updateReconnecting();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				break;
			case NetworkConnection.EVENT_BACKLOG_END:
			case NetworkConnection.EVENT_USERINFO:
			case NetworkConnection.EVENT_MAKESERVER:
			case NetworkConnection.EVENT_STATUSCHANGED:
			case NetworkConnection.EVENT_CONNECTIONDELETED:
			case NetworkConnection.EVENT_MAKEBUFFER:
			case NetworkConnection.EVENT_DELETEBUFFER:
			case NetworkConnection.EVENT_BUFFERMSG:
			case NetworkConnection.EVENT_HEARTBEATECHO:
			case NetworkConnection.EVENT_BUFFERARCHIVED:
			case NetworkConnection.EVENT_BUFFERUNARCHIVED:
			case NetworkConnection.EVENT_RENAMECONVERSATION:
			case NetworkConnection.EVENT_PART:
		    	new RefreshTask().execute((Void)null);
				break;
			default:
				break;
			}
		}
	};
	
	public void scrollToTop() {
		if(listView != null) {
			listView.smoothScrollToPosition(0);
		}
	}
	
	public interface OnBufferSelectedListener {
		public void onBufferSelected(int cid, int bid, String name, long last_seen_eid, long min_eid, String type, int joined, int archived, String status);
	}
}
