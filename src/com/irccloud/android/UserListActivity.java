package com.irccloud.android;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;

public class UserListActivity extends BaseActivity implements UsersListFragment.OnUserSelectedListener {
	int cid;
	long bid;
	String channel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if(savedInstanceState != null && savedInstanceState.containsKey("cid")) {
        	cid = savedInstanceState.getInt("cid");
        	bid = savedInstanceState.getLong("bid");
        	channel = savedInstanceState.getString("channel");
        }
    }

    @Override
    public void onResume() {
    	super.onResume();
    	if(getIntent() != null && getIntent().hasExtra("cid")) {
	    	cid = getIntent().getIntExtra("cid", 0);
	    	bid = getIntent().getLongExtra("bid", 0);
	    	channel = getIntent().getStringExtra("name");
    	}
    	getSupportActionBar().setTitle(channel + " members");
    }
    
    @Override
    public void onSaveInstanceState(Bundle state) {
    	super.onSaveInstanceState(state);
    	state.putInt("cid", cid);
    	state.putLong("bid", bid);
    	state.putString("channel", channel);
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
