package com.irccloud.android;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
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
	ListView listView = null;
	LinearLayout topUnreadIndicator = null;
	LinearLayout topUnreadIndicatorColor = null;
	LinearLayout bottomUnreadIndicator = null;
	LinearLayout bottomUnreadIndicatorColor = null;
	int selected_bid = -1;
	RefreshTask refreshTask = null;
	private boolean ready = false;
	
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
		int timeout;
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
		
		public int positionForBid(int bid) {
			for(int i = 0; i < data.size(); i++) {
				BufferListEntry e = data.get(i);
				if(e.bid == bid)
					return i;
			}
			return -1;
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
		
		public BufferListEntry buildItem(int cid, int bid, int type, String name, int key, int unread, int highlights, long last_seen_eid, long min_eid, int joined, int archived, String status, int timeout) {
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
			e.timeout = timeout;
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
			} else if(e.unread > 0 || selected_bid == e.bid) {
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

			if(holder.key != null) {
				if(e.key > 0) {
					holder.key.setVisibility(View.VISIBLE);
				} else {
					holder.key.setVisibility(View.INVISIBLE);
				}
			}
			
			if(holder.progress != null) {
				if(progressRow == position || e.timeout > 0 || (e.type == TYPE_SERVER && !(e.status.equals("connected_ready") || e.status.equals("quitting") || e.status.equals("disconnected")))) {
					if(selected_bid == -1 || progressRow != position) {
						holder.progress.setVisibility(View.VISIBLE);
						if(holder.bufferbg != null)
							holder.bufferbg.setSelected(false);
						if(holder.groupbg != null)
							holder.groupbg.setSelected(false);
					} else {
						if(holder.bufferbg != null)
							holder.bufferbg.setSelected(true);
						if(holder.groupbg != null)
							holder.groupbg.setSelected(true);
						holder.progress.setVisibility(View.GONE);
					}
				} else if(e.type != TYPE_ARCHIVES_HEADER) {
					holder.progress.setVisibility(View.GONE);
					if(holder.bufferbg != null)
						holder.bufferbg.setSelected(false);
					if(holder.groupbg != null)
						holder.groupbg.setSelected(false);
				}
			}
			
			if(holder.groupbg != null) {
				if(e.status.equals("waiting_to_retry") || e.status.equals("pool_unavailable")) {
					holder.groupbg.setBackgroundResource(R.drawable.row_connecting_bg);
					holder.label.setTextColor(getResources().getColorStateList(R.color.row_label_disconnected));
				} else {
					holder.groupbg.setBackgroundResource(R.drawable.row_buffergroup_bg);
                    if(e.status.equals("connected_ready"))
    					holder.label.setTextColor(getResources().getColorStateList(R.color.row_label));
                    else
                        holder.label.setTextColor(getResources().getColorStateList(R.color.row_label_inactive));
				}
			}
			
			if(holder.highlights != null) {
				if(e.highlights > 0) {
					holder.highlights.setVisibility(View.VISIBLE);
					holder.highlights.setText(String.valueOf(e.highlights));
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
			            mListener.addButtonPressed(e.cid);
					}
				});
			}
			
			return row;
		}
	}
	
	private class RefreshTask extends AsyncTaskEx<Void, Void, Void> {
		ArrayList<BufferListEntry> entries = new ArrayList<BufferListEntry>();
		
		@Override
		protected Void doInBackground(Void... params) {
			if(!ready || isCancelled())
				return null;
			
			ArrayList<ServersDataSource.Server> servers = ServersDataSource.getInstance().getServers();
			if(adapter == null) {
				adapter = new BufferListAdapter(BuffersListFragment.this);
			}

			firstUnreadPosition = -1;
			lastUnreadPosition = -1;
			firstHighlightPosition = -1;
			lastHighlightPosition = -1;
			int position = 0;
			
			JSONObject channelDisabledMap = null;
			JSONObject bufferDisabledMap = null;
			if(conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null) {
				try {
					if(conn.getUserInfo().prefs.has("channel-disableTrackUnread"))
						channelDisabledMap = conn.getUserInfo().prefs.getJSONObject("channel-disableTrackUnread");
					if(conn.getUserInfo().prefs.has("buffer-disableTrackUnread"))
						bufferDisabledMap = conn.getUserInfo().prefs.getJSONObject("buffer-disableTrackUnread");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			for(int i = 0; i < servers.size(); i++) {
				if(isCancelled())
					return null;

				int archiveCount = 0;
				ServersDataSource.Server s = servers.get(i);
				ArrayList<BuffersDataSource.Buffer> buffers = BuffersDataSource.getInstance().getBuffersForServer(s.cid);
				for(int j = 0; j < buffers.size(); j++) {
					if(isCancelled())
						return null;

					BuffersDataSource.Buffer b = buffers.get(j);
					if(b.type.equalsIgnoreCase("console")) {
						int unread = 0;
						int highlights = 0;
						if(conn.getState() == NetworkConnection.STATE_CONNECTED && conn.ready) {
							unread = EventsDataSource.getInstance().getUnreadCountForBuffer(b.bid, b.last_seen_eid, b.type);
							highlights = EventsDataSource.getInstance().getHighlightCountForBuffer(b.bid, b.last_seen_eid, b.type);
						}
						if(s.name.length() == 0)
							s.name = s.hostname;
						try {
							if(b.bid == selected_bid || (bufferDisabledMap != null && bufferDisabledMap.has(String.valueOf(b.bid)) && bufferDisabledMap.getBoolean(String.valueOf(b.bid))))
								unread = 0;
						} catch (JSONException e) {
							e.printStackTrace();
						}
						entries.add(adapter.buildItem(b.cid, b.bid, TYPE_SERVER, s.name, 0, unread, highlights, b.last_seen_eid, b.min_eid, 1, b.archived, s.status, 0));
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
					if(isCancelled())
						return null;

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
						int unread = 0;
						int highlights = 0;
						if(conn.getState() == NetworkConnection.STATE_CONNECTED && conn.ready) {
							unread = EventsDataSource.getInstance().getUnreadCountForBuffer(b.bid, b.last_seen_eid, b.type);
							highlights = EventsDataSource.getInstance().getHighlightCountForBuffer(b.bid, b.last_seen_eid, b.type);
						}
						try {
							if(b.type.equalsIgnoreCase("channel")) {
								if(b.bid == selected_bid || (channelDisabledMap != null && channelDisabledMap.has(String.valueOf(b.bid)) && channelDisabledMap.getBoolean(String.valueOf(b.bid))))
									unread = 0;
							} else {
								if(b.bid == selected_bid || (bufferDisabledMap != null && bufferDisabledMap.has(String.valueOf(b.bid)) && bufferDisabledMap.getBoolean(String.valueOf(b.bid))))
									unread = 0;
								if(b.type.equalsIgnoreCase("conversation") && (bufferDisabledMap != null && bufferDisabledMap.has(String.valueOf(b.bid)) && bufferDisabledMap.getBoolean(String.valueOf(b.bid))))
									highlights = 0;
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
						entries.add(adapter.buildItem(b.cid, b.bid, type, b.name, key, unread, highlights, b.last_seen_eid, b.min_eid, joined, b.archived, s.status, b.timeout));
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
					if(type > 0 && b.archived > 0) {
						archiveCount++;
					}
				}
				if(archiveCount > 0) {
					entries.add(adapter.buildItem(s.cid, 0, TYPE_ARCHIVES_HEADER, "Archives", 0, 0, 0, 0, 0, 0, 1, s.status, 0));
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
									entries.add(adapter.buildItem(b.cid, b.bid, type, b.name, 0, 0, 0, b.last_seen_eid, b.min_eid, 0, b.archived, s.status, 0));
									position++;
								}
							}
						}
					}
				}
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(isCancelled())
				return;
			
			refreshTask = null;

			if(adapter == null)
				return;
			
			adapter.setItems(entries);
			
			if(getListAdapter() == null && entries.size() > 0) {
				setListAdapter(adapter);
			} else
				adapter.notifyDataSetChanged();
			
			if(listView != null)
				updateUnreadIndicators(listView.getFirstVisiblePosition(), listView.getLastVisiblePosition());
			else {//The activity view isn't ready yet, try again
				refreshTask = new RefreshTask();
				refreshTask.execute((Void)null);
			}
			
			if(selected_bid > 0)
				adapter.showProgress(adapter.positionForBid(selected_bid));
		}
	}

	public void setSelectedBid(int bid) {
		selected_bid = bid;
		if(adapter != null)
			adapter.showProgress(adapter.positionForBid(bid));
	}
	
	private void updateUnreadIndicators(int first, int last) {
		if(topUnreadIndicator != null) {
			if(firstUnreadPosition != -1 && first > firstUnreadPosition) {
				topUnreadIndicator.setVisibility(View.VISIBLE);
				topUnreadIndicatorColor.setBackgroundResource(R.drawable.selected_blue);
			} else {
				topUnreadIndicator.setVisibility(View.GONE);
			}
			if((lastHighlightPosition != -1 && first > lastHighlightPosition) ||
					(firstHighlightPosition != -1 && first > firstHighlightPosition)) {
				topUnreadIndicator.setVisibility(View.VISIBLE);
				topUnreadIndicatorColor.setBackgroundResource(R.drawable.highlight_red);
			}
		}
		if(bottomUnreadIndicator != null) {
			if(lastUnreadPosition != -1 && last < lastUnreadPosition) {
				bottomUnreadIndicator.setVisibility(View.VISIBLE);
				bottomUnreadIndicatorColor.setBackgroundResource(R.drawable.selected_blue);
			} else {
				bottomUnreadIndicator.setVisibility(View.GONE);
			}
			if((firstHighlightPosition != -1 && last < firstHighlightPosition) ||
					(lastHighlightPosition != -1 && last < lastHighlightPosition)) {
				bottomUnreadIndicator.setVisibility(View.VISIBLE);
				bottomUnreadIndicatorColor.setBackgroundResource(R.drawable.highlight_red);
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
	        Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.bufferslist, null);
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
    	listView.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
				return mListener.onBufferLongClicked(BuffersDataSource.getInstance().getBuffer(adapter.data.get(pos).bid));
			}
    		
    	});

    	ready = NetworkConnection.getInstance().ready;
    	
    	if(ready && savedInstanceState != null && savedInstanceState.containsKey("data")) {
    		ArrayList<Integer> expandedArchives = savedInstanceState.getIntegerArrayList("expandedArchives");
    		Iterator<Integer> i = expandedArchives.iterator();
    		while(i.hasNext()) {
    			Integer cid = i.next();
    			mExpandArchives.put(cid, true);
    		}
        	adapter = new BufferListAdapter(this);
        	adapter.setItems((ArrayList<BufferListEntry>) savedInstanceState.getSerializable("data"));
        	setListAdapter(adapter);
        	listView.setSelection(savedInstanceState.getInt("scrollPosition"));
        	if(selected_bid > 0)
        		adapter.showProgress(adapter.positionForBid(selected_bid));
        }
		return view;
	}
	
    @Override
    public void onSaveInstanceState(Bundle state) {
    	if(adapter != null && adapter.data != null && adapter.data.size() > 0) {
    		ArrayList<Integer> expandedArchives = new ArrayList<Integer>();
    		ArrayList<ServersDataSource.Server> servers = ServersDataSource.getInstance().getServers();
    		Iterator<ServersDataSource.Server> i = servers.iterator();
    		while(i.hasNext()) {
    			ServersDataSource.Server s = i.next();
    			if(mExpandArchives.get(s.cid, false))
    				expandedArchives.add(s.cid);
    		}
    		state.putSerializable("data", adapter.data);
    		state.putIntegerArrayList("expandedArchives", expandedArchives);
    		if(listView != null)
    			state.putInt("scrollPosition", listView.getFirstVisiblePosition());
    	}
    }
	
    public void onResume() {
    	super.onResume();
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
    	ready = conn.ready;
		if(adapter != null)
			adapter.showProgress(-1);

		if(refreshTask != null)
        	refreshTask.cancel(true);
		refreshTask = new RefreshTask();
		refreshTask.execute((Void)null);
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
            if(refreshTask != null)
            	refreshTask.cancel(true);
			refreshTask = new RefreshTask();
			refreshTask.execute((Void)null);
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
    
    
    @SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_PROGRESS:
				break;
			case NetworkConnection.EVENT_CONNECTIVITY:
				if(adapter != null)
					adapter.notifyDataSetChanged();
				break;
			case NetworkConnection.EVENT_BACKLOG_START:
	            if(refreshTask != null)
	            	refreshTask.cancel(true);
	            break;
			case NetworkConnection.EVENT_BACKLOG_END:
				ready = true;
			default:
	            if(refreshTask != null)
	            	refreshTask.cancel(true);
				refreshTask = new RefreshTask();
				refreshTask.execute((Void)null);
				break;
			}
		}
	};
	
	public interface OnBufferSelectedListener {
		public void onBufferSelected(int cid, int bid, String name, long last_seen_eid, long min_eid, String type, int joined, int archived, String status);
		public boolean onBufferLongClicked(BuffersDataSource.Buffer b);
		public void addButtonPressed(int cid);
	}
}
