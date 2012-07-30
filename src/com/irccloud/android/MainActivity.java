package com.irccloud.android;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class MainActivity extends SherlockFragmentActivity {
	NetworkConnection conn;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onResume() {
    	super.onResume();
    	
    	String session = getSharedPreferences("prefs", 0).getString("session_key", "");
    	if(session != null && session.length() > 0) {
	    	conn = NetworkConnection.getInstance();
	    	conn.addHandler(mHandler);
	    	
	    	if(conn.getState() == NetworkConnection.STATE_DISCONNECTED)
	    		conn.connect(session);
    	} else {
    		Intent i = new Intent(this, LoginActivity.class);
    		startActivity(i);
    		finish();
    	}
    }
    
    public void onPause() {
    	super.onPause();

    	if(conn != null) {
    		conn.disconnect();
        	conn.removeHandler(mHandler);
    	}
    }
    
	static private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_CONNECTIVITY:
				Log.i("IRCCloud", "New connection state: " + NetworkConnection.getInstance().getState());
				break;
			case NetworkConnection.EVENT_USERINFO:
				Log.i("IRCCloud", "User info updated!  Hello, " + ((NetworkConnection.UserInfo)msg.obj).name);
				break;
			default:
				break;
			}
		}
	};
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_main, menu);

        return super.onCreateOptionsMenu(menu);
    }
}
