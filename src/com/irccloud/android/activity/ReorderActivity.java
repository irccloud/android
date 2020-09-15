/*
 * Copyright (c) 2015 IRCCloud, Ltd.
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

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.fragment.app.FragmentTransaction;

import com.irccloud.android.R;
import com.irccloud.android.fragment.PinReorderFragment;
import com.irccloud.android.fragment.ServerReorderFragment;

public class ReorderActivity extends BaseActivity {
    private String title = "Connections";
    private boolean servers = true;
    private boolean pins = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reorder_servers);

        if(savedInstanceState != null && savedInstanceState.containsKey("title")) {
            title = savedInstanceState.getString("title");
            servers = savedInstanceState.getBoolean("servers");
            pins = savedInstanceState.getBoolean("pins");
        } else {
            if(getIntent() != null && getIntent().hasExtra("title"))
                title = getIntent().getStringExtra("title");
            servers = getIntent() != null && getIntent().getBooleanExtra("servers", false);
            pins = getIntent() != null && getIntent().getBooleanExtra("pins", false);
        }

        getSupportActionBar().setTitle(title);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setElevation(0);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if(servers) {
            final ServerReorderFragment newFragment = new ServerReorderFragment();
            ft.replace(R.id.reorderFragment, newFragment);
            ft.commit();
        } else if(pins) {
            final PinReorderFragment newFragment = new PinReorderFragment();
            ft.replace(R.id.reorderFragment, newFragment);
            ft.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
