package com.irccloud.android;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BaseActivity extends SherlockFragmentActivity {
	NetworkConnection conn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
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
				setSupportProgressBarIndeterminateVisibility(true);
				getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.disconnected_yellow));
			} else {
				setSupportProgressBarIndeterminateVisibility(false);
				getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.heading_bg_blue));
			}
    	} else {
    		Intent i = new Intent(this, LoginActivity.class);
    		startActivity(i);
    		finish();
    	}
    }

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
					setSupportProgressBarIndeterminateVisibility(true);
					getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.disconnected_yellow));
				} else {
					setSupportProgressBarIndeterminateVisibility(false);
					getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.heading_bg_blue));
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
        getSupportMenuInflater().inflate(R.menu.activity_user_list, menu);

        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_userlist:
            	break;
        }
        return super.onOptionsItemSelected(item);
    }
}
