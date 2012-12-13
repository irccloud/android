package com.irccloud.android;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.View.OnClickListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class EditConnectionActivity extends SherlockFragmentActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_connection);
        
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.heading_bg_blue));
		getSupportActionBar().setCustomView(R.layout.actionbar_edit_connection);

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final EditConnectionFragment newFragment = new EditConnectionFragment();
        if(getIntent() != null && getIntent().hasExtra("cid"))
        	newFragment.setCid(getIntent().getIntExtra("cid", -1));
        ft.add(R.id.EditConnectionFragment, newFragment);
        ft.commit();

        getSupportActionBar().getCustomView().findViewById(R.id.action_cancel).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
        	
        });

        getSupportActionBar().getCustomView().findViewById(R.id.action_done).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				newFragment.save();
				finish();
			}
        	
        });

    }
}
