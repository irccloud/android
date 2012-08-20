package com.irccloud.android;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BaseActivity extends SherlockFragmentActivity {
	NetworkConnection conn;
	private int lastState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        lastState = -1;
    }

    @Override
    public void onResume() {
    	super.onResume();
		setSupportProgressBarIndeterminateVisibility(false);
    	String session = getSharedPreferences("prefs", 0).getString("session_key", "");
    	if(session != null && session.length() > 0) {
	    	conn = NetworkConnection.getInstance();
	    	conn.addHandler(mHandler);
	    	if(conn.getState() == NetworkConnection.STATE_DISCONNECTED)
	    		conn.connect(session);
			if(NetworkConnection.getInstance().getState() != NetworkConnection.STATE_CONNECTED) {
				if(lastState != 1) {
					setSupportProgressBarIndeterminateVisibility(true);
					getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.disconnected_yellow));
					lastState = 1;
				}
			} else {
				if(lastState != 2) {
					setSupportProgressBarIndeterminateVisibility(false);
					getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.heading_bg_blue));
					lastState = 2;
				}
			}
			if(DBHelper.getInstance().isBatch())
				setSupportProgressBarIndeterminateVisibility(true);
    	} else {
    		Intent i = new Intent(this, LoginActivity.class);
    		i.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                    Intent.FLAG_ACTIVITY_NEW_TASK);
    		startActivity(i);
    		finish();
    	}
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
				Log.i("IRCCloud", "New connection state: " + NetworkConnection.getInstance().getState());
				if(NetworkConnection.getInstance().getState() != NetworkConnection.STATE_CONNECTED) {
					if(lastState != 1) {
						setSupportProgressBarIndeterminateVisibility(true);
						getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.disconnected_yellow));
						lastState = 1;
					}
				} else {
					if(lastState != 2) {
						setSupportProgressBarIndeterminateVisibility(false);
						getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.heading_bg_blue));
						lastState = 2;
					}
				}
				break;
			case NetworkConnection.EVENT_BACKLOG_START:
                setSupportProgressBarIndeterminateVisibility(true);
				break;
			case NetworkConnection.EVENT_BACKLOG_END:
                setSupportProgressBarIndeterminateVisibility(false);
				break;
			default:
				break;
			}
		}
	};
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_base, menu);

        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
            	conn.disconnect();
				SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
				editor.remove("session_key");
				editor.commit();
            	DBHelper.getInstance().clear();
        		Intent i = new Intent(this, LoginActivity.class);
        		i.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK);
        		startActivity(i);
        		finish();
            	break;
        }
        return super.onOptionsItemSelected(item);
    }
}
