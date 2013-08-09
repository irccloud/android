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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.JsonArray;

public class IgnoreListFragment extends DialogFragment {
	JsonArray ignores;
	int cid;
	IgnoresAdapter adapter;
	NetworkConnection conn;
	ListView listView;
	
	private class IgnoresAdapter extends BaseAdapter {
		private DialogFragment ctx;
		
		private class ViewHolder {
			int position;
			TextView label;
			Button removeBtn;
		}
	
		public IgnoresAdapter(DialogFragment context) {
			ctx = context;
		}
		
		@Override
		public int getCount() {
			return ignores.size();
		}

		@Override
		public Object getItem(int pos) {
			try {
				return ignores.get(pos);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public long getItemId(int pos) {
			return pos;
		}

		OnClickListener removeClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Integer position = (Integer)v.getTag();
				try {
					conn.unignore(cid, ignores.get(position).getAsString());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			ViewHolder holder;

			if(row != null && ((ViewHolder)row.getTag()).position != position)
				row = null;
			
			if (row == null) {
				LayoutInflater inflater = ctx.getLayoutInflater(null);
				row = inflater.inflate(R.layout.row_hostmask, null);

				holder = new ViewHolder();
				holder.position = position;
				holder.label = (TextView) row.findViewById(R.id.label);
				holder.removeBtn = (Button) row.findViewById(R.id.removeBtn);

				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}
			
			try {
				holder.label.setText(ignores.get(position).toString());
				holder.removeBtn.setOnClickListener(removeClickListener);
				holder.removeBtn.setTag(position);
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
    	empty.setText("You're not ignoring anyone at the moment.  You can ignore someone by tapping their nickname in the user list, long-pressing a message, or by using /ignore.");
    	listView.setEmptyView(empty);
        if(savedInstanceState != null && savedInstanceState.containsKey("cid")) {
        	cid = savedInstanceState.getInt("cid");
        	ignores = ServersDataSource.getInstance().getServer(cid).raw_ignores;
        	adapter = new IgnoresAdapter(this);
        	listView.setAdapter(adapter);
        }
    	Dialog d = new AlertDialog.Builder(ctx)
        .setTitle("Ignore list for " + ServersDataSource.getInstance().getServer(cid).name)
        .setView(v)
        .setPositiveButton("Add Ignore Mask", new AddClickListener())
        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
        })
        .create();
	    d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    	return d;
    }

    class AddClickListener implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface d, int which) {
			Context ctx = getActivity();
			
			if(Build.VERSION.SDK_INT < 11)
				ctx = new ContextThemeWrapper(ctx, android.R.style.Theme_Dialog);
    		ServersDataSource s = ServersDataSource.getInstance();
    		ServersDataSource.Server server = s.getServer(cid);
    		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
    		LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	View view = inflater.inflate(R.layout.dialog_textprompt,null);
        	TextView prompt = (TextView)view.findViewById(R.id.prompt);
        	final EditText input = (EditText)view.findViewById(R.id.textInput);
        	input.setHint("nickname!user@host.name");
        	prompt.setText("Ignore messages from this hostmask");
        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
    		builder.setView(view);
    		builder.setPositiveButton("Ignore", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					conn.ignore(cid, input.getText().toString());
					dialog.dismiss();
				}
    		});
    		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
    		});
    		AlertDialog dialog = builder.create();
    		dialog.setOwnerActivity(getActivity());
    		dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    		dialog.show();
		}
    }

	
    @Override
    public void onSaveInstanceState(Bundle state) {
    	state.putInt("cid", cid);
    }
	
    @Override
    public void setArguments(Bundle args) {
    	cid = args.getInt("cid", 0);
    	if(cid > 0 && listView != null) {
        	ignores = ServersDataSource.getInstance().getServer(cid).raw_ignores;
        	adapter = new IgnoresAdapter(this);
        	listView.setAdapter(adapter);
    	}
    }
    
    public void onResume() {
    	super.onResume();
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
    	
    	if(ignores == null && cid > 0) {
            ignores = ServersDataSource.getInstance().getServer(cid).raw_ignores;
        	adapter = new IgnoresAdapter(this);
        	listView.setAdapter(adapter);
    	}
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	if(conn != null)
    		conn.removeHandler(mHandler);
    }
    
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_MAKESERVER:
				ServersDataSource.Server s = (ServersDataSource.Server)msg.obj;
				if(s.cid == cid) {
                    ignores = ServersDataSource.getInstance().getServer(cid).raw_ignores;
		        	adapter.notifyDataSetChanged();
				}
				break;
			case NetworkConnection.EVENT_SETIGNORES:
				IRCCloudJSONObject o = (IRCCloudJSONObject)msg.obj;
				if(o.cid() == cid) {
                    ignores = ServersDataSource.getInstance().getServer(cid).raw_ignores;
		        	adapter.notifyDataSetChanged();
				}
				break;
			default:
				break;
			}
		}
	};
}
