package com.irccloud.android;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;

public class BuffersListFragment extends SherlockListFragment {
	private static final int TYPE_SERVER = 0;
	private static final int TYPE_CHANNEL = 1;
	private static final int TYPE_CONVERSATION = 2;
	
	NetworkConnection conn;
	BufferListAdapter adapter;
	
	boolean isBacklog = false;
	
	private class BufferListAdapter extends BaseAdapter {
		ArrayList<BufferListEntry> data;
		private SherlockListFragment ctx;
		
		private class ViewHolder {
			int type;
			TextView label;
		}
	
		private class BufferListEntry {
			int cid;
			int bid;
			int type;
			boolean unread;
			String name;
		}

		public BufferListAdapter(SherlockListFragment context) {
			ctx = context;
			data = new ArrayList<BufferListEntry>();
		}
		
		public void clear() {
			data = new ArrayList<BufferListEntry>();
		}
		
		public void addItem(int cid, int bid, int type, String name, boolean unread) {
			BufferListEntry e = new BufferListEntry();
			e.cid = cid;
			e.bid = bid;
			e.type = type;
			e.name = name;
			e.unread = unread;
			data.add(e);
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
				holder.type = e.type;

				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}

			holder.label.setText(e.name);
			if(e.unread) {
				row.setBackgroundColor(0xFF0000FF);
			} else {
				row.setBackgroundColor(0x00000000);
			}
			
			return row;
		}
	}
	
	public void refresh() {
		adapter.clear();
		ArrayList<ServersDataSource.Server> servers = ServersDataSource.getInstance().getServers();
		for(int i = 0; i < servers.size(); i++) {
			ServersDataSource.Server s = servers.get(i);
			ArrayList<BuffersDataSource.Buffer> buffers = BuffersDataSource.getInstance().getBuffersForServer(s.cid);
			for(int j = 0; j < buffers.size(); j++) {
				BuffersDataSource.Buffer b = buffers.get(j);
				if(b.type.equalsIgnoreCase("console")) {
					adapter.addItem(b.cid, b.bid, TYPE_SERVER, s.name, false);
					break;
				}
			}
			for(int j = 0; j < buffers.size(); j++) {
				BuffersDataSource.Buffer b = buffers.get(j);
				int type = -1;
				if(b.type.equalsIgnoreCase("channel"))
					type = TYPE_CHANNEL;
				else if(b.type.equalsIgnoreCase("conversation"))
					type = TYPE_CONVERSATION;
				if(type > 0 && b.hidden == 0)
					adapter.addItem(b.cid, b.bid, type, b.name, false);
			}
		}
		adapter.notifyDataSetChanged();
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new BufferListAdapter(this);
        setListAdapter(adapter);
    }
    
    public void onResume() {
    	super.onResume();
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
    	refresh();
    }
    
    public void onPause() {
    	super.onPause();
    	if(conn != null)
    		conn.removeHandler(mHandler);
    	}
    
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_BACKLOG_START:
				isBacklog = true;
				break;
			case NetworkConnection.EVENT_BACKLOG_END:
				isBacklog = false;
				refresh();
				break;
			case NetworkConnection.EVENT_MAKESERVER:
			case NetworkConnection.EVENT_MAKEBUFFER:
			case NetworkConnection.EVENT_DELETEBUFFER:
				if(!isBacklog)
					refresh();
				break;
			default:
				break;
			}
		}
	};
}
