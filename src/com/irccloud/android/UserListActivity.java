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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

	@Override
	public void onUserSelected(int cid, String channel, String name) {
		// TODO Auto-generated method stub
		
	}
}
