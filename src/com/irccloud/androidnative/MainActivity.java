package com.irccloud.androidnative;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class MainActivity extends BaseActivity implements BuffersListFragment.OnBufferSelectedListener {
	NetworkConnection conn;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        
        View v = getLayoutInflater().inflate(R.layout.actionbar_bufferslist, null);
        v.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				BuffersListFragment f = (BuffersListFragment)getSupportFragmentManager().findFragmentById(R.id.BuffersList);
				f.scrollToTop();
			}
        });
        getSupportActionBar().setCustomView(v);
	}

    @Override
    protected void setLoadingIndicator(boolean state) {
		BuffersListFragment f = (BuffersListFragment)getSupportFragmentManager().findFragmentById(R.id.BuffersList);
		if(f != null && f.getConnectingVisibility() == View.GONE)
	    	super.setLoadingIndicator(state);
		else
			super.setLoadingIndicator(false);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
    }
    
    @Override
    public void onPause() {
    	super.onPause();

    	if(conn != null) {
        	conn.removeHandler(mHandler);
    	}
    }
    
	static private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
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
		Intent i = new Intent(this, MessageActivity.class);
		i.putExtra("cid", cid);
		i.putExtra("bid", bid);
		i.putExtra("name", name);
		i.putExtra("last_seen_eid", last_seen_eid);
		i.putExtra("min_eid", min_eid);
		i.putExtra("type", type);
		i.putExtra("joined", joined);
		i.putExtra("archived", archived);
		i.putExtra("status", status);
		startActivity(i);
	}
}
