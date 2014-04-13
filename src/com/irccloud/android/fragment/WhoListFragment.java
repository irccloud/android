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

package com.irccloud.android.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.ColorFormatter;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;

public class WhoListFragment extends DialogFragment {
	JsonNode users;
	UsersAdapter adapter;
	NetworkConnection conn;
	ListView listView;
	IRCCloudJSONObject event;
	
	private class UsersAdapter extends BaseAdapter {
		private DialogFragment ctx;
		
		private class ViewHolder {
			int position;
			TextView nick;
			TextView name;
			TextView server;
			TextView mask;
		}
	
		public UsersAdapter(DialogFragment context) {
			ctx = context;
		}
		
		@Override
		public int getCount() {
			return users.size();
		}

		@Override
		public Object getItem(int pos) {
			try {
				return users.get(pos);
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
				row = inflater.inflate(R.layout.row_who, null);

				holder = new ViewHolder();
				holder.position = position;
				holder.nick = (TextView) row.findViewById(R.id.nick);
				holder.name = (TextView) row.findViewById(R.id.name);
				holder.server = (TextView) row.findViewById(R.id.server);
				holder.mask = (TextView) row.findViewById(R.id.mask);

				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}
			
			try {
				holder.nick.setText(users.get(position).get("nick").asText());
				holder.name.setText(ColorFormatter.html_to_spanned("&nbsp;(" + ColorFormatter.irc_to_html(TextUtils.htmlEncode(users.get(position).get("realname").asText())) + ")"));
				holder.server.setText("Connected via " + users.get(position).get("ircserver").asText());
				holder.mask.setText(users.get(position).get("usermask").asText());
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
        if(savedInstanceState != null && savedInstanceState.containsKey("event")) {
        	event = new IRCCloudJSONObject(savedInstanceState.getString("event"));
        	users = event.getJsonNode("users");
        	adapter = new UsersAdapter(this);
        	listView.setAdapter(adapter);
        }
    	Dialog d = new AlertDialog.Builder(ctx)
        .setTitle("WHO response for " + event.getString("subject"))
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
    	users = event.getJsonNode("users");
    	if(getActivity() != null && listView != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(adapter == null) {
                        adapter = new UsersAdapter(WhoListFragment.this);
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

    	if(users != null) {
        	adapter = new UsersAdapter(this);
        	listView.setAdapter(adapter);
    	}
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    }
}
