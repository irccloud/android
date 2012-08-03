package com.irccloud.android;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;

public class UserListActivity extends SherlockFragmentActivity implements UsersListFragment.OnUserSelectedListener {
	int cid;
	long bid;
	String channel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onResume() {
    	super.onResume();
    	cid = getIntent().getIntExtra("cid", 0);
    	bid = getIntent().getLongExtra("bid", 0);
    	channel = getIntent().getStringExtra("name");
    	getSupportActionBar().setTitle(channel);
    	String session = getSharedPreferences("prefs", 0).getString("session_key", "");
    	if(session != null && session.length() > 0) {
	    	NetworkConnection conn = NetworkConnection.getInstance();
	    	if(conn.getState() == NetworkConnection.STATE_DISCONNECTED)
	    		conn.connect(session);
    	} else {
    		Intent i = new Intent(this, LoginActivity.class);
    		startActivity(i);
    		finish();
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_user_list, menu);

        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed
                // in the Action Bar.
                Intent parentActivityIntent = new Intent(this, MessageActivity.class);
                parentActivityIntent.putExtra("cid", cid);
                parentActivityIntent.putExtra("bid", bid);
                parentActivityIntent.putExtra("name", channel);
                parentActivityIntent.putExtra("type", "channel");
                parentActivityIntent.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(parentActivityIntent);
                finish();
                return true;
            case R.id.menu_userlist:
            	break;
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void onUserSelected(int cid, String channel, String name) {
		// TODO Auto-generated method stub
		
	}
}
