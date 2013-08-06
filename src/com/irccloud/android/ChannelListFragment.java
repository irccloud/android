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

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ListFragment;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ChannelListFragment extends ListFragment {
	ArrayList<ChannelsAdapter.Channel> channels;
	ChannelsAdapter adapter;
	NetworkConnection conn;
	ListView listView;
	TextView empty;
	ServersDataSource.Server server;
	
	private class ChannelsAdapter extends BaseAdapter {
		private ListFragment ctx;
		
		private class ViewHolder {
			TextView channel;
			TextView topic;
		}

		private class Channel {
			Spanned channel;
			Spanned topic;
		}
		
		public ChannelsAdapter(ListFragment context) {
			ctx = context;
		}
		
		public void set(JsonArray json) {
			channels = new ArrayList<Channel>(json.size());
			
			for(int i = 0; i < json.size(); i++) {
				Channel c = new Channel();
				JsonObject o = json.get(i).getAsJsonObject();
				String channel = o.get("name").getAsString() + " (" + o.get("num_members").getAsInt() + " member";
				if(o.get("num_members").getAsInt() != 1)
					channel += "s";
				channel += ")";
				c.channel = ColorFormatter.html_to_spanned(channel, true, server);
				
				String topic = o.get("topic").getAsString();
				if(topic.length() > 0) {
					c.topic = ColorFormatter.html_to_spanned(topic, true, server);
				} else {
					c.topic = null;
				}
				channels.add(c);
			}
		}
		
		@Override
		public int getCount() {
			if(channels == null)
				return 0;
			else
				return channels.size();
		}

		@Override
		public Object getItem(int pos) {
			try {
				return channels.get(pos);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public long getItemId(int pos) {
			return pos;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			ViewHolder holder;

			if (row == null) {
				LayoutInflater inflater = ctx.getLayoutInflater(null);
				row = inflater.inflate(R.layout.row_channel, null);

				holder = new ViewHolder();
				holder.channel = (TextView) row.findViewById(R.id.channel);
				holder.topic = (TextView) row.findViewById(R.id.topic);

				holder.channel.setMovementMethod(LinkMovementMethod.getInstance());
				holder.topic.setMovementMethod(LinkMovementMethod.getInstance());

				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}
			
			try {
				Channel c = channels.get(position);
				holder.channel.setText(c.channel);
				if(c.topic != null && c.topic.length() > 0) {
					holder.topic.setText(c.topic);
					holder.topic.setVisibility(View.VISIBLE);
				} else {
					holder.topic.setText("");
					holder.topic.setVisibility(View.GONE);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return row;
		}
	}
	
	@Override
    public View onCreateView(LayoutInflater i, ViewGroup container, Bundle savedInstanceState) {
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);

    	Context ctx = getActivity();
		if(Build.VERSION.SDK_INT < 11)
			ctx = new ContextThemeWrapper(ctx, android.R.style.Theme_Dialog);

		if(ctx == null)
			return null;
		
		LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View v = inflater.inflate(R.layout.ignorelist, null);
    	listView = (ListView)v.findViewById(android.R.id.list);
    	empty = (TextView)v.findViewById(android.R.id.empty);
    	empty.setText("Loading channel list…");
    	listView.setEmptyView(empty);
    	return v;
    }

    @Override
    public void setArguments(Bundle args) {
    	server = ServersDataSource.getInstance().getServer(args.getInt("cid", -1));
    	channels = null;
    	if(listView != null) {
        	empty.setText("Loading channel list…");
        	adapter = new ChannelsAdapter(this);
        	listView.setAdapter(adapter);
    	}
    }
    
    public void onResume() {
    	super.onResume();
    	if(conn == null) {
	    	conn = NetworkConnection.getInstance();
	    	conn.addHandler(mHandler);
    	}
    	
    	if(adapter == null) {
        	adapter = new ChannelsAdapter(this);
        	listView.setAdapter(adapter);
    	}
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	if(conn != null)
    		conn.removeHandler(mHandler);
    	conn = null;
    }
    
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			IRCCloudJSONObject o;
			switch (msg.what) {
			case NetworkConnection.EVENT_LISTRESPONSE:
				o = (IRCCloudJSONObject)msg.obj;
				adapter.set(o.getJsonArray("channels"));
				adapter.notifyDataSetChanged();
				break;
			case NetworkConnection.EVENT_LISTRESPONSETOOMANY:
				empty.setText("Too many channels to list");
				break;
			default:
				break;
			}
		}
	};
}
