/*
 * Copyright (c) 2013 IRCCloud, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irccloud.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.irccloud.android.data.BuffersDataSource;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.ServersDataSource;
import com.irccloud.android.fragment.EditConnectionFragment;

public class EditConnectionActivity extends ActionBarActivity implements NetworkConnection.IRCEventHandler {
    int reqid = -1;
    int cidToOpen = -1;
    int cid = -1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_connection);
        
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar));
		getSupportActionBar().setCustomView(R.layout.actionbar_edit_connection);
        getSupportActionBar().setElevation(0);

		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        final EditConnectionFragment newFragment = new EditConnectionFragment();
        if(getIntent() != null && getIntent().hasExtra("cid")) {
        	newFragment.setCid(getIntent().getIntExtra("cid", -1));
            cid = getIntent().getIntExtra("cid", -1);
        }
        if(getIntent() != null && getIntent().hasExtra("hostname"))
        	newFragment.default_hostname = getIntent().getStringExtra("hostname");
        if(getIntent() != null && getIntent().hasExtra("channels"))
        	newFragment.default_channels = getIntent().getStringExtra("channels");
    	newFragment.default_port = getIntent().getIntExtra("port", 6667);
        ft.replace(R.id.EditConnectionFragment, newFragment);
        ft.commit();

        getSupportActionBar().getCustomView().findViewById(R.id.action_cancel).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
                if(ServersDataSource.getInstance().count() < 1) {
                    NetworkConnection.getInstance().logout();
                    Intent i = new Intent(EditConnectionActivity.this, LoginActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
				finish();
			}
        	
        });

        getSupportActionBar().getCustomView().findViewById(R.id.action_done).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				reqid = newFragment.save();
			}
        	
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        NetworkConnection.getInstance().addHandler(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        NetworkConnection.getInstance().removeHandler(this);
    }

    public void onIRCEvent(int what, Object o) {
        IRCCloudJSONObject obj;
        BuffersDataSource.Buffer buffer;
        switch (what) {
            case NetworkConnection.EVENT_MAKEBUFFER:
                buffer = (BuffersDataSource.Buffer)o;
                if(buffer.cid == cidToOpen) {
                    Intent i = new Intent(EditConnectionActivity.this, MainActivity.class);
                    i.putExtra("bid", buffer.bid);
                    startActivity(i);
                    finish();
                }
                break;
            case NetworkConnection.EVENT_SUCCESS:
                obj = (IRCCloudJSONObject)o;
                if(obj.getInt("_reqid") == reqid) {
                    if(cid != -1)
                        finish();
                    else
                        cidToOpen = obj.cid();
                }
                break;
            case NetworkConnection.EVENT_FAILURE_MSG:
                obj = (IRCCloudJSONObject)o;
                if(obj.getInt("_reqid") == reqid) {
                    final String message = obj.getString("message");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(message.equals("passworded_servers"))
                                Toast.makeText(EditConnectionActivity.this, "You can’t connect to passworded servers with free accounts.", Toast.LENGTH_SHORT).show();
                            else if(message.equals("networks"))
                                Toast.makeText(EditConnectionActivity.this, "You've exceeded the connection limit for free accounts.", Toast.LENGTH_SHORT).show();
                            else if(message.equals("unverified"))
                                Toast.makeText(EditConnectionActivity.this, "You can’t connect to external servers until you confirm your email address.", Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(EditConnectionActivity.this, "Unable to add connection: invalid " + message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;
            default:
                break;
        }
    }
}
