package com.irccloud.android;

import java.util.ArrayList;

import org.json.JSONException;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;

public class UsersListFragment extends SherlockListFragment {
	private static final int TYPE_HEADING = 0;
	private static final int TYPE_USER = 1;
	
	NetworkConnection conn;
	UserListAdapter adapter;
	OnUserSelectedListener mListener;
	int cid = -1;
	String channel;
	View view;
	RefreshTask refreshTask = null;
	
	private class UserListAdapter extends BaseAdapter {
		ArrayList<UserListEntry> data;
		private SherlockListFragment ctx;
		
		private class ViewHolder {
			int type;
			TextView label;
			TextView count;
		}
	
		private class UserListEntry {
			int type;
			String text;
			String count;
			int color;
			int bg_color;
			boolean away;
		}

		public UserListAdapter(SherlockListFragment context) {
			ctx = context;
			data = new ArrayList<UserListEntry>();
		}
		
		public void setItems(ArrayList<UserListEntry> items) {
			data = items;
		}
		
		public UserListEntry buildItem(int type, String text, String count, int color, int bg_color, boolean away) {
			UserListEntry e = new UserListEntry();
			e.type = type;
			e.text = text;
			e.count = count;
			e.color = color;
			e.bg_color = bg_color;
			e.away = away;
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
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			UserListEntry e = data.get(position);
			View row = convertView;
			ViewHolder holder;

			if(row != null && ((ViewHolder)row.getTag()).type != e.type)
				row = null;
			
			if (row == null) {
				LayoutInflater inflater = ctx.getLayoutInflater(null);
				if(e.type == TYPE_HEADING)
					row = inflater.inflate(R.layout.row_usergroup, null);
				else
					row = inflater.inflate(R.layout.row_user, null);

				holder = new ViewHolder();
				holder.label = (TextView) row.findViewById(R.id.label);
				holder.count = (TextView) row.findViewById(R.id.count);
				holder.type = e.type;

				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}

			holder.label.setText(e.text);
			if(e.type == TYPE_USER && e.away) {
				holder.label.setTextColor(getResources().getColorStateList(R.color.row_user_away));
			} else {
				holder.label.setTextColor(getResources().getColorStateList(e.color));
			}
			if(NetworkConnection.getInstance().getState() != NetworkConnection.STATE_CONNECTED)
				row.setBackgroundResource(R.drawable.row_disconnected_bg);
			else
				row.setBackgroundResource(e.bg_color);
				
			if(holder.count != null) {
				if(e.count != null) {
					holder.count.setVisibility(View.VISIBLE);
					holder.count.setText(e.count);
					holder.count.setTextColor(getResources().getColorStateList(e.color));
				} else {
					holder.count.setVisibility(View.GONE);
					holder.count.setText("");
				}
			}
			
			return row;
		}
	}

	private void addUsersFromList(ArrayList<UserListAdapter.UserListEntry> entries, ArrayList<UsersDataSource.User> users, String heading, String symbol, int heading_color, int bg_color) {
		if(users.size() > 0) {
			entries.add(adapter.buildItem(TYPE_HEADING, heading, users.size() > 0?symbol + String.valueOf(users.size()):null, heading_color, bg_color, false));
			for(int i = 0; i < users.size(); i++) {
				UsersDataSource.User user = users.get(i);
				entries.add(adapter.buildItem(TYPE_USER, user.nick, null, R.color.row_user, bg_color, user.away > 0));
			}
		}
	}
	
	private void refresh(ArrayList<UsersDataSource.User> users) {
		ArrayList<UserListAdapter.UserListEntry> entries = new ArrayList<UserListAdapter.UserListEntry>();
		ArrayList<UsersDataSource.User> owners = new ArrayList<UsersDataSource.User>();
		ArrayList<UsersDataSource.User> admins = new ArrayList<UsersDataSource.User>();
		ArrayList<UsersDataSource.User> ops = new ArrayList<UsersDataSource.User>();
		ArrayList<UsersDataSource.User> halfops = new ArrayList<UsersDataSource.User>();
		ArrayList<UsersDataSource.User> voiced = new ArrayList<UsersDataSource.User>();
		ArrayList<UsersDataSource.User> members = new ArrayList<UsersDataSource.User>();
		boolean showSymbol = false;
		try {
			if(conn.getUserInfo() != null && conn.getUserInfo().prefs != null)
			showSymbol = conn.getUserInfo().prefs.getBoolean("mode-showsymbol");
		} catch (JSONException e) {
		}
		
		if(adapter == null) {
			adapter = new UserListAdapter(UsersListFragment.this);
		}

		for(int i = 0; i < users.size(); i++) {
			UsersDataSource.User user = users.get(i);
			if(user.mode.contains("q")) {
				owners.add(user);
			} else if(user.mode.contains("a")) {
				admins.add(user);
			} else if(user.mode.contains("o")) {
				ops.add(user);
			} else if(user.mode.contains("h")) {
				halfops.add(user);
			} else if(user.mode.contains("v")) {
				voiced.add(user);
			} else {
				members.add(user);
			}
		}
		
		if(owners.size() > 0) {
			addUsersFromList(entries, owners, "OWNERS", (showSymbol?"~ ":"¥ "), R.color.heading_owner, R.drawable.row_owners_bg);
		}
		
		if(admins.size() > 0) {
			addUsersFromList(entries, admins, "ADMINS", (showSymbol?"& ":"¥ "), R.color.heading_admin, R.drawable.row_admins_bg);
		}
		
		if(ops.size() > 0) {
			addUsersFromList(entries, ops, "OPERATORS", (showSymbol?"@ ":"¥ "), R.color.heading_operators, R.drawable.row_operator_bg);
		}
		
		if(halfops.size() > 0) {
			addUsersFromList(entries, halfops, "HALFOPS", (showSymbol?"% ":"¥ "), R.color.heading_halfop, R.drawable.row_halfops_bg);
		}
		
		if(voiced.size() > 0) {
			addUsersFromList(entries, voiced, "VOICED", (showSymbol?"+ ":"¥ "), R.color.heading_voiced, R.drawable.row_voiced_bg);
		}
		
		if(members.size() > 0) {
			addUsersFromList(entries, members, "MEMBERS", "", R.color.heading_members, R.drawable.row_members_bg);
		}

		adapter.setItems(entries);
		
		if(getListAdapter() == null && entries.size() > 0)
			setListAdapter(adapter);
		else
			adapter.notifyDataSetChanged();
	}
	
	private class RefreshTask extends AsyncTaskEx<Void, Void, Void> {
		ArrayList<UsersDataSource.User> users;
		
		@Override
		protected Void doInBackground(Void... params) {
			users = UsersDataSource.getInstance().getUsersForChannel(cid, channel);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(!isCancelled()) {
				refresh(users);
				refreshTask = null;
			}
		}
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if(savedInstanceState != null && savedInstanceState.containsKey("cid")) {
        	cid = savedInstanceState.getInt("cid");
        	channel = savedInstanceState.getString("channel");
        }
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
	        Bundle savedInstanceState) {
		view = inflater.inflate(R.layout.userslist, null);
		return view;
	}

	public void onResume() {
    	super.onResume();
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
		if(NetworkConnection.getInstance().getState() != NetworkConnection.STATE_CONNECTED) {
			view.setBackgroundResource(R.drawable.disconnected_yellow);
		} else {
			view.setBackgroundResource(R.drawable.background_blue);
		}
    	ArrayList<UsersDataSource.User> users = UsersDataSource.getInstance().getUsersForChannel(cid, channel);
    	refresh(users);
    }
    
    @Override
    public void setArguments(Bundle args) {
    	cid = args.getInt("cid", 0);
    	channel = args.getString("name");
    	
    	mHandler.postDelayed(new Runnable() {

			@Override
			public void run() {
		    	ArrayList<UsersDataSource.User> users = UsersDataSource.getInstance().getUsersForChannel(cid, channel);
		    	refresh(users);
		    	try {
			    	if(getListView() != null)
			    		getListView().setSelection(0);
		    	} catch (Exception e) { //Sometimes the list view isn't available yet
		    	}
			}
    		
    	}, 100);
    }
	
    public void onPause() {
    	super.onPause();
    	if(conn != null)
    		conn.removeHandler(mHandler);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnUserSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnUserSelectedListener");
        }
        if(cid == -1) {
	        cid = activity.getIntent().getIntExtra("cid", 0);
	        channel = activity.getIntent().getStringExtra("name");
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle state) {
    	super.onSaveInstanceState(state);
    	state.putInt("cid", cid);
    	state.putString("channel", channel);
    }
    
    public void onListItemClick(ListView l, View v, int position, long id) {
    	UserListAdapter.UserListEntry e = (UserListAdapter.UserListEntry)adapter.getItem(position);
    	if(e.type == TYPE_USER)
    		mListener.onUserSelected(cid, channel, e.text);
    }
    
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_CONNECTIVITY:
				if(NetworkConnection.getInstance().getState() != NetworkConnection.STATE_CONNECTED) {
					view.setBackgroundResource(R.drawable.disconnected_yellow);
				} else {
					view.setBackgroundResource(R.drawable.background_blue);
				}
				if(adapter != null)
					adapter.notifyDataSetChanged();
				break;
			case NetworkConnection.EVENT_USERINFO:
			case NetworkConnection.EVENT_JOIN:
			case NetworkConnection.EVENT_PART:
			case NetworkConnection.EVENT_QUIT:
			case NetworkConnection.EVENT_NICKCHANGE:
			case NetworkConnection.EVENT_MEMBERUPDATES:
			case NetworkConnection.EVENT_USERCHANNELMODE:
			case NetworkConnection.EVENT_KICK:
			case NetworkConnection.EVENT_BACKLOG_END:
	            if(refreshTask != null)
	            	refreshTask.cancel(true);
				refreshTask = new RefreshTask();
				refreshTask.execute((Void)null);
				break;
			default:
				break;
			}
		}
	};
	
	public interface OnUserSelectedListener {
		public void onUserSelected(int cid, String channel, String name);
	}
}
