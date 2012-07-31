package com.irccloud.android;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;

import android.os.Bundle;
import android.util.Log;

public class MessageActivity extends SherlockFragmentActivity {
	BuffersDataSource.Buffer buffer;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
    }

    @Override
    public void onResume() {
    	super.onResume();
    	buffer = BuffersDataSource.getInstance().getBuffer((int)getIntent().getLongExtra("bid", 0));
    	getSupportActionBar().setTitle(buffer.name);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_main, menu);

        return super.onCreateOptionsMenu(menu);
    }
}
