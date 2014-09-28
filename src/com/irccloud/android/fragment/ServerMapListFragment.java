/*
 * Copyright (c) 2014 IRCCloud, Ltd.
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

package com.irccloud.android.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;

public class ServerMapListFragment extends DialogFragment {
	JsonNode servers;
	ServersAdapter adapter;
	NetworkConnection conn;
	ListView listView;
	IRCCloudJSONObject event;
	
	private class ServersAdapter extends BaseAdapter {
		private DialogFragment ctx;

		private class ViewHolder {
			int position;
			TextView server;
		}

		public ServersAdapter(DialogFragment context) {
			ctx = context;
		}

		@Override
		public int getCount() {
			return servers.size();
		}

		@Override
		public Object getItem(int pos) {
			try {
				return servers.get(pos);
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

			if(row != null && ((ViewHolder)row.getTag()).position != position)
				row = null;

			if (row == null) {
				LayoutInflater inflater = ctx.getLayoutInflater(null);
				row = inflater.inflate(R.layout.row_servermaplist, null);

				holder = new ViewHolder();
				holder.position = position;
				holder.server = (TextView) row.findViewById(R.id.server);
				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}

			try {
				holder.server.setText(servers.get(position).asText());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return row;
		}
	}

	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		Context ctx = getActivity();
		if(Build.VERSION.SDK_INT < 11)
			ctx = new ContextThemeWrapper(ctx, android.R.style.Theme_Dialog);

		LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View v = inflater.inflate(R.layout.ignorelist, null);
    	listView = (ListView)v.findViewById(android.R.id.list);
    	TextView empty = (TextView)v.findViewById(android.R.id.empty);
    	empty.setText("No results found.");
    	listView.setEmptyView(empty);
        listView.setDividerHeight(0);
        if(savedInstanceState != null && savedInstanceState.containsKey("event")) {
        	event = new IRCCloudJSONObject(savedInstanceState.getString("event"));
        	servers = event.getJsonNode("servers");
        	adapter = new ServersAdapter(this);
        	listView.setAdapter(adapter);
        }
    	Dialog d = new AlertDialog.Builder(ctx)
        .setTitle("Server Map")
        .setView(v)
        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
        })
        .create();
    	return d;
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
    	state.putString("event", event.toString());
    }
	
    @Override
    public void setArguments(Bundle args) {
    	event = new IRCCloudJSONObject(args.getString("event"));
    	servers = event.getJsonNode("servers");
    	if(getActivity() != null && listView != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(adapter == null) {
                        adapter = new ServersAdapter(ServerMapListFragment.this);
                        listView.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                }
            });
    	}
    }
    
    public void onResume() {
    	super.onResume();
    	conn = NetworkConnection.getInstance();

    	if(servers != null) {
        	adapter = new ServersAdapter(this);
        	listView.setAdapter(adapter);
    	}
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    }
}
