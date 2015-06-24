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

import android.app.ActivityManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.github.fge.uritemplate.URITemplate;
import com.github.fge.uritemplate.vars.VariableMap;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;

import org.json.JSONException;
import org.json.JSONObject;

public class PastebinEditorActivity extends AppCompatActivity implements NetworkConnection.IRCEventHandler {

    private class FetchPastebinTask extends AsyncTaskEx<Void, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                URITemplate uri_template = new URITemplate(NetworkConnection.getInstance().config.getString("pastebin_uri_template"));
                return NetworkConnection.getInstance().fetchJSON(uri_template.toString(VariableMap.newBuilder().addScalarValue("id", pasteID).addScalarValue("type", "json").freeze()));
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject o) {
            if (o != null) {
                try {
                    paste.setText(o.getString("body"));
                    filename.setText(o.getString("name"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private TabLayout tabHost;
    private EditText paste;
    private EditText filename;
    private EditText message;
    private TextView messages_count;
    private Toolbar toolbar;
    private int pastereqid = -1;
    private String pastecontents;
    private String pasteID;
    private String url;
    private int current_tab = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            Bitmap cloud = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.app_name), cloud, 0xFFF2F7FC));
            cloud.recycle();
        }
        setContentView(R.layout.activity_pastebineditor);

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            if(!getWindow().isFloating()) {
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        paste = (EditText) findViewById(R.id.paste);
        filename = (EditText) findViewById(R.id.filename);
        message = (EditText) findViewById(R.id.message);
        messages_count = (TextView) findViewById(R.id.messages_count);

        if (savedInstanceState != null && savedInstanceState.containsKey("message"))
            message.setText(savedInstanceState.getString("message"));

        if (savedInstanceState != null && savedInstanceState.containsKey("paste_id"))
            pasteID = savedInstanceState.getString("paste_id");
        else if (getIntent() != null && getIntent().hasExtra("paste_id"))
            pasteID = getIntent().getStringExtra("paste_id");

        if (savedInstanceState != null && savedInstanceState.containsKey("paste_contents"))
            pastecontents = savedInstanceState.getString("paste_contents");
        else if (getIntent() != null && getIntent().hasExtra("paste_contents"))
            pastecontents = getIntent().getStringExtra("paste_contents");
        paste.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                int count = 0;
                String lines[] = editable.toString().split("\n");
                for(String line : lines) {
                    count += Math.ceil(line.length() / 1080.0f);
                }
                messages_count.setText("Text will be sent as " + count + " message" + (count==1?"":"s"));
            }
        });
        paste.setText(pastecontents);

        if (savedInstanceState != null && savedInstanceState.containsKey("filename"))
            filename.setText(savedInstanceState.getString("filename"));
        else if (getIntent() != null && getIntent().hasExtra("filename"))
            filename.setText(getIntent().getStringExtra("filename"));

        tabHost = (TabLayout) findViewById(android.R.id.tabhost);
        ViewCompat.setElevation(toolbar, ViewCompat.getElevation(tabHost));

        if (pasteID != null) {
            tabHost.setVisibility(View.GONE);
            message.setVisibility(View.GONE);
            findViewById(R.id.message_heading).setVisibility(View.GONE);
        } else {
            tabHost.setTabGravity(TabLayout.GRAVITY_FILL);
            tabHost.setTabMode(TabLayout.MODE_FIXED);
            tabHost.addTab(tabHost.newTab().setText("Pastebin"));
            tabHost.addTab(tabHost.newTab().setText("Messages"));
            tabHost.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    current_tab = tab.getPosition();
                    if (current_tab == 0) {
                        filename.setVisibility(View.VISIBLE);
                        message.setVisibility(View.VISIBLE);
                        messages_count.setVisibility(View.GONE);
                        findViewById(R.id.filename_heading).setVisibility(View.VISIBLE);
                        findViewById(R.id.message_heading).setVisibility(View.VISIBLE);
                    } else {
                        filename.setVisibility(View.GONE);
                        message.setVisibility(View.GONE);
                        messages_count.setVisibility(View.VISIBLE);
                        findViewById(R.id.filename_heading).setVisibility(View.GONE);
                        findViewById(R.id.message_heading).setVisibility(View.GONE);
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });
            if (savedInstanceState != null && savedInstanceState.containsKey("tab"))
                tabHost.getTabAt(savedInstanceState.getInt("tab")).select();
        }

        NetworkConnection.getInstance().addHandler(this);
        if (pasteID != null && (pastecontents == null || pastecontents.length() == 0)) {
            new FetchPastebinTask().execute((Void) null);
        }

        if(pasteID != null) {
            setTitle(R.string.title_activity_pastebin_editor_edit);
            toolbar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar));
        } else {
            setTitle(R.string.title_activity_pastebin_editor);
        }

        supportInvalidateOptionsMenu();

        result(RESULT_CANCELED);
    }

    @Override
    protected void onDestroy() {
        NetworkConnection.getInstance().removeHandler(this);
        super.onDestroy();
    }

    private String extension() {
        try {
            if (filename != null && filename.getText() != null && filename.getText().length() > 0) {
                String file = filename.getText().toString();
                if (file.contains(".")) {
                    String extension = file.substring(file.lastIndexOf(".") + 1);
                    if (extension.length() > 0)
                        return extension;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "txt";
    }

    @Override
    public void onIRCEvent(final int msg, Object obj) {
        final IRCCloudJSONObject event;
        switch (msg) {
            case NetworkConnection.EVENT_FAILURE_MSG:
                event = (IRCCloudJSONObject) obj;
                if (event != null && event.has("_reqid")) {
                    Log.e("IRCCloud", "Pastebin Error: " + obj.toString());
                    int reqid = event.getInt("_reqid");
                    if (reqid == pastereqid) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(PastebinEditorActivity.this, "Unable to save pastebin, please try again shortly.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
                break;
            case NetworkConnection.EVENT_SUCCESS:
                event = (IRCCloudJSONObject) obj;
                if (event != null && event.has("_reqid")) {
                    int reqid = event.getInt("_reqid");
                    if (reqid == pastereqid) {
                        url = event.getString("url");
                        result(RESULT_OK);
                        finish();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_pastebineditor, menu);
        if(pasteID != null) {
            menu.findItem(R.id.action_save).setVisible(true);
            menu.findItem(R.id.action_send).setVisible(false);
        } else {
            menu.findItem(R.id.action_save).setVisible(false);
            menu.findItem(R.id.action_send).setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_save:
                pastecontents = paste.getText().toString();
                pastereqid = NetworkConnection.getInstance().edit_paste(pasteID, filename.getText().toString(), extension(), pastecontents);
                break;
            case R.id.action_send:
                pastecontents = paste.getText().toString();
                if(current_tab == 0) {
                    pastereqid = NetworkConnection.getInstance().paste(filename.getText().toString(), extension(), pastecontents);
                } else {
                    result(RESULT_OK);
                    finish();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void result(int resultCode) {
        Intent data = new Intent();
        data.putExtra("paste_contents", pastecontents);
        data.putExtra("paste_id", pasteID);
        data.putExtra("message", message.getText().toString());
        data.putExtra("url", url);
        setResult(resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("paste_contents", paste.getText().toString());
        outState.putString("paste_id", pasteID);
        outState.putString("message", message.getText().toString());
        outState.putString("filename", filename.getText().toString());
        outState.putInt("tab", current_tab);
    }
}