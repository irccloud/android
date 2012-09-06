package com.irccloud.android;

import org.json.JSONException;
import org.json.JSONObject;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class MessageActivity extends UserListActivity {
	int cid;
	int bid;
	String name;
	String type;
	TextView messageTxt;
	View sendBtn;
	int joined;
	int archived;
	String status;
	
	NetworkConnection conn;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        messageTxt = (TextView)findViewById(R.id.messageTxt);
        messageTxt.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
         	   if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
         		   new SendTask().execute((Void)null);
         	   }
         	   return true;
           	}
        });
        sendBtn = findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new SendTask().execute((Void)null);
			}
        });
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        if(savedInstanceState != null && savedInstanceState.containsKey("cid")) {
        	cid = savedInstanceState.getInt("cid");
        	bid = savedInstanceState.getInt("bid");
        	name = savedInstanceState.getString("name");
        	type = savedInstanceState.getString("type");
        	joined = savedInstanceState.getInt("joined");
        	archived = savedInstanceState.getInt("archived");
        	status = savedInstanceState.getString("status");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
    	super.onSaveInstanceState(state);
    	state.putInt("cid", cid);
    	state.putInt("bid", bid);
    	state.putString("name", name);
    	state.putString("type", type);
    	state.putInt("joined", joined);
    	state.putInt("archived", archived);
    	state.putString("status", status);
    }
    
    private class SendTask extends AsyncTask<Void, Void, Void> {
    	@Override
    	protected void onPreExecute() {
    		sendBtn.setEnabled(false);
    	}
    	
		@Override
		protected Void doInBackground(Void... arg0) {
			if(conn.getState() == NetworkConnection.STATE_CONNECTED) {
				conn.say(cid, name, messageTxt.getText().toString());
			}
			return null;
		}
    	
		@Override
		protected void onPostExecute(Void result) {
			messageTxt.setText("");
    		sendBtn.setEnabled(true);
		}
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	if(getIntent() != null && getIntent().hasExtra("cid")) {
	    	cid = getIntent().getIntExtra("cid", 0);
	    	bid = getIntent().getIntExtra("bid", 0);
	    	name = getIntent().getStringExtra("name");
	    	type = getIntent().getStringExtra("type");
	    	joined = getIntent().getIntExtra("joined", 0);
	    	archived = getIntent().getIntExtra("archived", 0);
	    	status = getIntent().getStringExtra("status");
    	}
    	if(bid == -1) {
    		BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBufferByName(cid, name);
    		if(b != null) {
    			bid = b.bid;
    		}
    	}
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
    	updateUsersListFragmentVisibility();
    	getSupportActionBar().setTitle(name);
    	invalidateOptionsMenu();
    }

    @Override
    public void onPause() {
    	super.onPause();
    	if(conn != null)
    		conn.removeHandler(mHandler);
    }

    private void updateUsersListFragmentVisibility() {
    	boolean hide = false;
		View v = findViewById(R.id.usersListFragment);
		if(v != null) {
			try {
				JSONObject hiddenMap = conn.getUserInfo().prefs.getJSONObject("channel-hiddenMembers");
				if(hiddenMap.has(String.valueOf(bid)) && hiddenMap.getBoolean(String.valueOf(bid)))
					hide = true;
			} catch (JSONException e) {
			}
	    	if(hide || !type.equalsIgnoreCase("channel"))
	    		v.setVisibility(View.GONE);
		}
    }
    
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			Integer event_bid = 0;
			IRCCloudJSONObject event = null;
			switch (msg.what) {
			case NetworkConnection.EVENT_USERINFO:
		    	updateUsersListFragmentVisibility();
				invalidateOptionsMenu();
				break;
			case NetworkConnection.EVENT_STATUSCHANGED:
				try {
					IRCCloudJSONObject o = (IRCCloudJSONObject)msg.obj;
					if(o.cid() == cid) {
						status = o.getString("new_status");
						invalidateOptionsMenu();
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case NetworkConnection.EVENT_MAKESERVER:
				ServersDataSource.Server server = (ServersDataSource.Server)msg.obj;
				if(server.cid == cid) {
					status = server.status;
					invalidateOptionsMenu();
				}
				break;
			case NetworkConnection.EVENT_MAKEBUFFER:
				BuffersDataSource.Buffer buffer = (BuffersDataSource.Buffer)msg.obj;
				if(bid == -1 && buffer.cid == cid && buffer.name.equalsIgnoreCase(name)) {
					Log.i("IRCCloud", "Got my new buffer id: " + buffer.bid);
					bid = buffer.bid;
				}
				break;
			case NetworkConnection.EVENT_BUFFERARCHIVED:
				event_bid = (Integer)msg.obj;
				if(event_bid == bid) {
					archived = 1;
					invalidateOptionsMenu();
				}
				break;
			case NetworkConnection.EVENT_BUFFERUNARCHIVED:
				event_bid = (Integer)msg.obj;
				if(event_bid == bid) {
					archived = 0;
					invalidateOptionsMenu();
				}
				break;
			case NetworkConnection.EVENT_JOIN:
				event = (IRCCloudJSONObject)msg.obj;
				if(event.bid() == bid && event.type().equalsIgnoreCase("you_joined_channel")) {
					joined = 1;
					invalidateOptionsMenu();
				}
				break;
			case NetworkConnection.EVENT_PART:
				event = (IRCCloudJSONObject)msg.obj;
				if(event.bid() == bid && event.type().equalsIgnoreCase("you_parted_channel")) {
					joined = 0;
					invalidateOptionsMenu();
				}
				break;
			case NetworkConnection.EVENT_CHANNELINIT:
				ChannelsDataSource.Channel channel = (ChannelsDataSource.Channel)msg.obj;
				if(channel.bid == bid) {
					joined = 1;
					archived = 0;
					invalidateOptionsMenu();
				}
			default:
				break;
			}
		}
	};
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	if(type.equalsIgnoreCase("channel")) {
    		getSupportMenuInflater().inflate(R.menu.activity_message_channel_userlist, menu);
    		getSupportMenuInflater().inflate(R.menu.activity_message_channel, menu);
    	} else if(type.equalsIgnoreCase("conversation"))
    		getSupportMenuInflater().inflate(R.menu.activity_message_conversation, menu);
    	else if(type.equalsIgnoreCase("console"))
    		getSupportMenuInflater().inflate(R.menu.activity_message_console, menu);

    	getSupportMenuInflater().inflate(R.menu.activity_message_archive, menu);

        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
    	if(archived == 0) {
    		menu.findItem(R.id.menu_archive).setTitle(R.string.menu_archive);
    	} else {
    		menu.findItem(R.id.menu_archive).setTitle(R.string.menu_unarchive);
    	}
    	if(type.equalsIgnoreCase("channel")) {
        	if(joined == 0) {
        		menu.findItem(R.id.menu_leave).setTitle(R.string.menu_rejoin);
        		menu.findItem(R.id.menu_archive).setVisible(true);
        		menu.findItem(R.id.menu_archive).setEnabled(true);
        		menu.findItem(R.id.menu_delete).setVisible(true);
        		menu.findItem(R.id.menu_delete).setEnabled(true);
        		if(menu.findItem(R.id.menu_userlist) != null)
        			menu.findItem(R.id.menu_userlist).setEnabled(false);
        	} else {
        		menu.findItem(R.id.menu_leave).setTitle(R.string.menu_leave);
        		menu.findItem(R.id.menu_archive).setVisible(false);
        		menu.findItem(R.id.menu_archive).setEnabled(false);
        		menu.findItem(R.id.menu_delete).setVisible(false);
        		menu.findItem(R.id.menu_delete).setEnabled(false);
        		if(menu.findItem(R.id.menu_userlist) != null) {
	        		boolean hide = false;
	        		try {
	        			if(conn != null && conn.getUserInfo().prefs != null) {
							JSONObject hiddenMap = conn.getUserInfo().prefs.getJSONObject("channel-hiddenMembers");
							if(hiddenMap.has(String.valueOf(bid)) && hiddenMap.getBoolean(String.valueOf(bid)))
								hide = true;
	        			}
					} catch (JSONException e) {
					}
					if(hide) {
		        		menu.findItem(R.id.menu_userlist).setEnabled(false);
		        		menu.findItem(R.id.menu_userlist).setVisible(false);
					} else {
		        		menu.findItem(R.id.menu_userlist).setEnabled(true);
		        		menu.findItem(R.id.menu_userlist).setVisible(true);
					}
        		}
        	}
    	} else if(type.equalsIgnoreCase("console")) {
    		menu.findItem(R.id.menu_archive).setVisible(false);
    		menu.findItem(R.id.menu_archive).setEnabled(false);
    		Log.i("IRCCloud", "Status: " + status);
    		if(status != null && status.contains("connected") && !status.startsWith("dis")) {
    			menu.findItem(R.id.menu_disconnect).setTitle(R.string.menu_disconnect);
        		menu.findItem(R.id.menu_delete).setVisible(false);
        		menu.findItem(R.id.menu_delete).setEnabled(false);
    		} else {
    			menu.findItem(R.id.menu_disconnect).setTitle(R.string.menu_reconnect);
        		menu.findItem(R.id.menu_delete).setVisible(true);
        		menu.findItem(R.id.menu_delete).setEnabled(true);
    		}
    	}
    	return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Intent intent;
    	
        switch (item.getItemId()) {
            case R.id.menu_userlist:
            	intent = new Intent(this, UserListActivity.class);
            	intent.putExtra("cid", cid);
            	intent.putExtra("bid", bid);
            	intent.putExtra("name", name);
            	startActivity(intent);
            	return true;
            case R.id.menu_ignore_list:
                intent = new Intent(this, IgnoreListActivity.class);
                intent.putExtra("cid", cid);
                startActivity(intent);
                return true;
            case R.id.menu_leave:
            	if(joined == 0)
            		conn.join(cid, name, "");
            	else
            		conn.part(cid, name, "");
            	return true;
            case R.id.menu_archive:
            	if(archived == 0)
            		conn.archiveBuffer(cid, bid);
            	else
            		conn.unarchiveBuffer(cid, bid);
            	return true;
            case R.id.menu_delete:
            	if(type.equalsIgnoreCase("console")) {
            		conn.deleteServer(cid);
            	} else {
                	conn.deleteBuffer(cid, bid);
            	}
            	return true;
            case R.id.menu_editconnection:
                intent = new Intent(this, EditConnectionActivity.class);
                intent.putExtra("cid", cid);
                startActivity(intent);
            	return true;
            case R.id.menu_disconnect:
        		if(status != null && status.contains("connected") && !status.startsWith("dis")) {
        			conn.disconnect(cid, "");
        		} else {
        			conn.reconnect(cid);
        		}
        }
        return super.onOptionsItemSelected(item);
    }
}
