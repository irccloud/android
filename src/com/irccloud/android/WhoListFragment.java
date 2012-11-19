package com.irccloud.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.google.gson.JsonArray;

public class WhoListFragment extends SherlockDialogFragment {
	JsonArray users;
	UsersAdapter adapter;
	NetworkConnection conn;
	ListView listView;
	IRCCloudJSONObject event;
	
	private class UsersAdapter extends BaseAdapter {
		private SherlockDialogFragment ctx;
		
		private class ViewHolder {
			int position;
			TextView nick;
			TextView name;
			TextView server;
			TextView mask;
		}
	
		public UsersAdapter(SherlockDialogFragment context) {
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
				holder.nick.setText(users.get(position).getAsJsonObject().get("nick").getAsString());
				holder.name.setText(" (" + users.get(position).getAsJsonObject().get("realname").getAsString() + ")");
				holder.server.setText("Connected via " + users.get(position).getAsJsonObject().get("ircserver").getAsString());
				holder.mask.setText(users.get(position).getAsJsonObject().get("usermask").getAsString());
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
        	users = event.getJsonArray("users");
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
    	users = event.getJsonArray("users");
    	if(listView != null) {
    		if(adapter == null) {
	        	adapter = new UsersAdapter(this);
	        	listView.setAdapter(adapter);
    		} else {
    			adapter.notifyDataSetChanged();
    		}
    	}
    }
    
    public void onResume() {
    	super.onResume();
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
    	
    	if(users != null) {
        	adapter = new UsersAdapter(this);
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
			default:
				break;
			}
		}
	};
}
