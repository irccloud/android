package com.irccloud.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.format.DateUtils;
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

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.google.gson.JsonArray;

public class BanListFragment extends SherlockDialogFragment {
	JsonArray bans;
	int cid;
	int bid;
	IRCCloudJSONObject event;
	BansAdapter adapter;
	NetworkConnection conn;
	ListView listView;
	boolean canUnBan = false;
	
	private class BansAdapter extends BaseAdapter {
		private SherlockDialogFragment ctx;
		
		private class ViewHolder {
			int position;
			TextView mask;
			TextView setBy;
			Button removeBtn;
		}
	
		public BansAdapter(SherlockDialogFragment context) {
			ctx = context;
		}
		
		@Override
		public int getCount() {
			return bans.size();
		}

		@Override
		public Object getItem(int pos) {
			try {
				return bans.get(pos);
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
					conn.mode(cid, event.getString("channel"), "-b " + bans.get(position).getAsJsonObject().get("mask").getAsString());
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
				row = inflater.inflate(R.layout.row_banlist, null);

				holder = new ViewHolder();
				holder.position = position;
				holder.mask = (TextView) row.findViewById(R.id.mask);
				holder.setBy = (TextView) row.findViewById(R.id.setBy);
				holder.removeBtn = (Button) row.findViewById(R.id.removeBtn);

				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}
			
			try {
				holder.mask.setText(Html.fromHtml(bans.get(position).getAsJsonObject().get("mask").getAsString()));
				holder.setBy.setText("Set " + DateUtils.getRelativeTimeSpanString(bans.get(position).getAsJsonObject().get("time").getAsLong() * 1000L, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)
						+ " by " + bans.get(position).getAsJsonObject().get("usermask").getAsString());
				if(canUnBan) {
					holder.removeBtn.setVisibility(View.VISIBLE);
					holder.removeBtn.setOnClickListener(removeClickListener);
					holder.removeBtn.setTag(position);
				} else {
					holder.removeBtn.setVisibility(View.GONE);
				}
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
    	empty.setText("No bans in effect.  You can ban people from a menu on tapping their nickname, long-pressing a message, or by using /ban.");
    	listView.setEmptyView(empty);
        if(savedInstanceState != null && savedInstanceState.containsKey("cid")) {
        	cid = savedInstanceState.getInt("cid");
        	bid = savedInstanceState.getInt("bid");
        	event = new IRCCloudJSONObject(savedInstanceState.getString("event"));
        	bans = event.getJsonArray("bans");
        	adapter = new BansAdapter(this);
        	listView.setAdapter(adapter);
        }
    	Dialog d = new AlertDialog.Builder(ctx)
        .setTitle("Ban list for " + event.getString("channel"))
        .setView(v)
        .setPositiveButton("Add Ban Mask", new AddClickListener())
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
        	prompt.setText("Ban this hostmask");
        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
    		builder.setView(view);
    		builder.setPositiveButton("Ban", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					conn.mode(cid, event.getString("channel"), "+b " + input.getText().toString());
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
    	state.putInt("bid", bid);
    	state.putString("event", event.toString());
    }
	
    @Override
    public void setArguments(Bundle args) {
    	cid = args.getInt("cid", 0);
    	bid = args.getInt("bid", 0);
    	event = new IRCCloudJSONObject(args.getString("event"));
    	bans = event.getJsonArray("bans");
    	if(cid > 0 && listView != null) {
    		if(adapter == null) {
	        	adapter = new BansAdapter(this);
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
    	
    	if(cid > 0) {
        	adapter = new BansAdapter(this);
        	listView.setAdapter(adapter);
    	}
		UsersDataSource.User self_user = UsersDataSource.getInstance().getUser(cid, event.getString("channel"), ServersDataSource.getInstance().getServer(cid).nick);
		if(self_user != null && (self_user.mode.contains("q") || self_user.mode.contains("a") || self_user.mode.contains("o")))
			canUnBan = true;
		else
			canUnBan = false;
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
			case NetworkConnection.EVENT_USERCHANNELMODE:
				UsersDataSource.User self_user = UsersDataSource.getInstance().getUser(cid, event.getString("channel"), ServersDataSource.getInstance().getServer(cid).nick);
				if(self_user != null && (self_user.mode.contains("q") || self_user.mode.contains("a") || self_user.mode.contains("o")))
					canUnBan = true;
				else
					canUnBan = false;
				if(adapter != null)
					adapter.notifyDataSetChanged();
				break;
			case NetworkConnection.EVENT_BUFFERMSG:
				EventsDataSource.Event e = (EventsDataSource.Event)msg.obj;
				if(e.bid == bid && e.type.equals("channel_mode_list_change"))
					conn.mode(cid, event.getString("channel"), "b");
				break;
			default:
				break;
			}
		}
	};
}
