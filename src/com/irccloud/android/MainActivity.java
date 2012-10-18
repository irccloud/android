package com.irccloud.android;

import java.util.Timer;
import java.util.TimerTask;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends BaseActivity implements BuffersListFragment.OnBufferSelectedListener {
	private NetworkConnection conn;
	private TextView errorMsg;
	private Timer countdownTimer = null;
	private String error = null;
	private View connecting = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.drawable.logo);
        connecting = findViewById(R.id.connecting);
		errorMsg = (TextView)findViewById(R.id.errorMsg);
	}

    @Override
    protected void setLoadingIndicator(boolean state) {
		super.setLoadingIndicator(false);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
		if(conn.getState() != NetworkConnection.STATE_CONNECTED) {
			connecting.setBackgroundResource(R.drawable.disconnected_yellow);
		} else {
			connecting.setBackgroundResource(R.drawable.background_blue);
		}

    	launchLastChannel();
    }
    
    @Override
    public void onPause() {
    	super.onPause();

    	if(conn != null) {
        	conn.removeHandler(mHandler);
    	}
    }
    
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_CONNECTIVITY:
				if(conn.getState() != NetworkConnection.STATE_CONNECTED) {
					connecting.setBackgroundResource(R.drawable.disconnected_yellow);
				} else {
					connecting.setBackgroundResource(R.drawable.background_blue);
					errorMsg.setText("Loading");
					error = null;
				}
				if(conn.getState() == NetworkConnection.STATE_CONNECTING) {
					errorMsg.setText("Connecting");
					error = null;
				}
				else if(conn.getState() == NetworkConnection.STATE_DISCONNECTED)
					updateReconnecting();
				break;
			case NetworkConnection.EVENT_FAILURE_MSG:
				IRCCloudJSONObject o = (IRCCloudJSONObject)msg.obj;
				if(conn.getState() != NetworkConnection.STATE_CONNECTED) {
					try {
						error = o.getString("message");
						if(error.equals("temp_unavailable"))
							error = "Your account is temporarily unavailable";
						updateReconnecting();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				break;
			case NetworkConnection.EVENT_USERINFO:
				Log.i("IRCCloud", "User info updated!  Hello, " + ((NetworkConnection.UserInfo)msg.obj).name);
				break;
			case NetworkConnection.EVENT_BACKLOG_END:
				launchLastChannel();
				break;
			default:
				break;
			}
		}
	};

	private void launchLastChannel() {
		if(conn != null && conn.getState() == NetworkConnection.STATE_CONNECTED && conn.getUserInfo() != null) {
			int bid = conn.getUserInfo().last_selected_bid;
			BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBuffer(bid);
			if(b != null) {
				ServersDataSource.Server s = ServersDataSource.getInstance().getServer(b.cid);
				int joined = 1;
				if(b.type.equalsIgnoreCase("channel")) {
					ChannelsDataSource.Channel c = ChannelsDataSource.getInstance().getChannelForBuffer(b.bid);
					if(c == null)
						joined = 0;
				}
				if(b.type.equalsIgnoreCase("console"))
					b.name = s.name;
				Intent i = new Intent(MainActivity.this, MessageActivity.class);
				i.putExtra("cid", b.cid);
				i.putExtra("bid", b.bid);
				i.putExtra("name", b.name);
				i.putExtra("last_seen_eid", b.last_seen_eid);
				i.putExtra("min_eid", b.min_eid);
				i.putExtra("type", b.type);
				i.putExtra("joined", joined);
				i.putExtra("archived", b.archived);
				i.putExtra("status", s.status);
				startActivity(i);
				finish();
			}
		}
	}
	
	private void updateReconnecting() {
    	if(conn.getReconnectTimestamp() > 0) {
    		String plural = "";
    		int seconds = (int)((conn.getReconnectTimestamp() - System.currentTimeMillis()) / 1000);
    		if(seconds != 1)
    			plural = "s";
    		if(seconds < 1)
    			errorMsg.setText("Connecting");
    		else if(seconds > 10 && error != null)
				errorMsg.setText(error +"\n\nReconnecting in\n" + seconds + " second" + plural);
			else
				errorMsg.setText("Reconnecting in\n" + seconds + " second" + plural);
			if(countdownTimer != null)
				countdownTimer.cancel();
			countdownTimer = new Timer();
			countdownTimer.schedule( new TimerTask(){
	             public void run() {
	    			 if(conn.getState() == NetworkConnection.STATE_DISCONNECTED) {
	    				 mHandler.post(new Runnable() {
							@Override
							public void run() {
			 					updateReconnecting();
							}
	    				 });
	    			 }
	    			 countdownTimer = null;
	             }
			}, 1000);
    	} else {
			errorMsg.setText("Offline");
    	}
    }
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_add_network:
        	EditConnectionFragment newFragment = new EditConnectionFragment();
            newFragment.show(getSupportFragmentManager(), "dialog");
            break;
        }
        return super.onOptionsItemSelected(item);
    }
    
	@Override
	public void onBufferSelected(int cid, int bid, String name, long last_seen_eid, long min_eid, String type, int joined, int archived, String status) {
	}
}
