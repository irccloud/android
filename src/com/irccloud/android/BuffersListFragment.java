package com.irccloud.android;

import java.io.Serializable;
import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;

public class BuffersListFragment extends SherlockListFragment {
	private static final int TYPE_SERVER = 0;
	private static final int TYPE_CHANNEL = 1;
	private static final int TYPE_CONVERSATION = 2;
	
	NetworkConnection conn;
	BufferListAdapter adapter;
	OnBufferSelectedListener mListener;
	
	private static class BufferListEntry implements Serializable {
		private static final long serialVersionUID = 1848168221883194026L;
		int cid;
		long bid;
		int type;
		int unread;
		int highlights;
		int key;
		long last_seen_eid;
		long min_eid;
		int joined;
		int archived;
		String name;
	}

	private class BufferListAdapter extends BaseAdapter {
		ArrayList<BufferListEntry> data;
		private SherlockListFragment ctx;
		
		private class ViewHolder {
			int type;
			TextView label;
			TextView highlights;
			LinearLayout unread;
			ImageView key;
		}
	
		public BufferListAdapter(SherlockListFragment context) {
			ctx = context;
			data = new ArrayList<BufferListEntry>();
		}
		
		public void setItems(ArrayList<BufferListEntry> items) {
			data = items;
		}
		
		public BufferListEntry buildItem(int cid, long bid, int type, String name, int key, int unread, int highlights, long last_seen_eid, long min_eid, int joined, int archived) {
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
				holder.key = (ImageView) row.findViewById(R.id.key);
				holder.type = e.type;

				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}

			holder.label.setText(e.name);
			if(e.unread > 0) {
				holder.label.setTypeface(null, Typeface.BOLD);
				holder.label.setTextColor(getResources().getColorStateList(R.color.row_label_unread));
				holder.unread.setBackgroundResource(R.drawable.selected_blue);
			} else {
				holder.label.setTypeface(null);
				holder.label.setTextColor(getResources().getColorStateList(R.color.row_label));
				holder.unread.setBackgroundResource(R.drawable.background_blue);
			}

			if(holder.key != null) {
				if(e.key > 0) {
					holder.key.setVisibility(View.VISIBLE);
				} else {
					holder.key.setVisibility(View.INVISIBLE);
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

			for(int i = 0; i < servers.size(); i++) {
				ServersDataSource.Server s = servers.get(i);
				ArrayList<BuffersDataSource.Buffer> buffers = BuffersDataSource.getInstance().getBuffersForServer(s.cid);
				for(int j = 0; j < buffers.size(); j++) {
					BuffersDataSource.Buffer b = buffers.get(j);
					if(b.type.equalsIgnoreCase("console")) {
						int unread = EventsDataSource.getInstance().getUnreadCountForBuffer(b.bid, b.last_seen_eid);
						int highlights = EventsDataSource.getInstance().getHighlightCountForBuffer(b.bid, b.last_seen_eid);
						entries.add(adapter.buildItem(b.cid, b.bid, TYPE_SERVER, s.name, 0, unread, highlights, b.last_seen_eid, b.min_eid, 1, b.archived));
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
						int unread = EventsDataSource.getInstance().getUnreadCountForBuffer(b.bid, b.last_seen_eid);
						int highlights = EventsDataSource.getInstance().getHighlightCountForBuffer(b.bid, b.last_seen_eid);
						entries.add(adapter.buildItem(b.cid, b.bid, type, b.name, key, unread, highlights, b.last_seen_eid, b.min_eid, joined, b.archived));
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
		}
	}
	
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
		View view = inflater.inflate(R.layout.bufferslist, null);
		return view;
	}
	
    @Override
    public void onSaveInstanceState(Bundle state) {
    	if(adapter.data.size() > 0)
    		state.putSerializable("data", adapter.data);
    }
	
    public void onResume() {
    	super.onResume();
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
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
    	mListener.onBufferSelected(e.cid, e.bid, e.name, e.last_seen_eid, e.min_eid, type, e.joined, e.archived);
    }
    
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_BACKLOG_END:
			case NetworkConnection.EVENT_MAKESERVER:
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
	
	public interface OnBufferSelectedListener {
		public void onBufferSelected(int cid, long bid, String name, long last_seen_eid, long min_eid, String type, int joined, int archived);
	}
}
