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

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.irccloud.android.CollapsedEventsList;
import com.irccloud.android.ColorFormatter;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.Notifications;
import com.irccloud.android.R;
import com.irccloud.android.RemoteInputService;
import com.irccloud.android.data.ServersDataSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class QuickReplyActivity extends AppCompatActivity {
    int cid, bid;
    String to;
    SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
    CollapsedEventsList collapsedEventsList = new CollapsedEventsList();
    boolean nickColors;
    ServersDataSource.Server server;
    int timestamp_width = -1;

    private class MessagesAdapter extends BaseAdapter {
        private class ViewHolder {
            TextView timestamp;
            TextView message;
        }

        private JSONArray msgs = new JSONArray();

        public void loadMessages(int cid, int bid) {
            JSONArray notifications;

            try {
                notifications = new JSONArray(PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getString("notifications_json", "[]"));
            } catch (JSONException e) {
                notifications = new JSONArray();
            }

            msgs = new JSONArray();
            try {
                for(int i = 0; i < notifications.length(); i++) {
                    JSONObject n = notifications.getJSONObject(i);
                    if(n.getInt("cid") == cid && n.getInt("bid") == bid)
                        msgs.put(n);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return msgs.length();
        }

        @Override
        public Object getItem(int i) {
            try {
                return msgs.get(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

       @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View row = view;
            ViewHolder holder;

            if (row == null) {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.row_message, viewGroup, false);

                holder = new ViewHolder();
                holder.timestamp = (TextView)row.findViewById(R.id.timestamp);
                holder.message = (TextView)row.findViewById(R.id.message);

                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }

           try {
               JSONObject msg = msgs.getJSONObject(i);
               if(getWindowManager().getDefaultDisplay().getWidth() < 800) {
                   holder.timestamp.setVisibility(View.GONE);
               } else {
                   Calendar calendar = Calendar.getInstance();
                   calendar.setTimeInMillis(msg.getLong("eid") / 1000);

                   if (timestamp_width == -1) {
                       SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
                       String s = "88:88 88";
                       if (prefs.getBoolean("time-24hr", false)) {
                           if (prefs.getBoolean("time-seconds", false))
                               s = "88:88:88";
                           else
                               s = "88:88";
                       } else if (prefs.getBoolean("time-seconds", false)) {
                           s = "88:88:88 88";
                       }
                       timestamp_width = (int) holder.timestamp.getPaint().measureText(s);
                   }
                   holder.timestamp.setMinWidth(timestamp_width);
                   holder.timestamp.setText(formatter.format(calendar.getTime()));
               }
               holder.message.setText(ColorFormatter.html_to_spanned("<b>" + ColorFormatter.irc_to_html(collapsedEventsList.formatNick(msg.getString("nick"), null, nickColors)) + "</b> " + msg.getString("message"), true, server));
               holder.message.setMovementMethod(LinkMovementMethod.getInstance());
               holder.message.setLinkTextColor(getResources().getColor(R.color.linkColor));
           } catch (JSONException e) {
               e.printStackTrace();
           }
            return row;
        }
    }

    private MessagesAdapter adapter = new MessagesAdapter();

    private SharedPreferences.OnSharedPreferenceChangeListener prefslistener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            adapter.loadMessages(cid, bid);
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        cid = intent.getIntExtra("cid", -1);
        bid = intent.getIntExtra("bid", -1);
        to = intent.getStringExtra("to");
        server = new ServersDataSource.Server();
        server.cid = cid;

        setTitle("Reply to " + to + " (" + intent.getStringExtra("network") + ")");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_reply);

        if(getIntent().hasExtra("cid") && getIntent().hasExtra("bid")) {
            onNewIntent(getIntent());
        } else {
            finish();
            return;
        }

        final ImageButton send = (ImageButton) findViewById(R.id.sendBtn);
        final EditText message = (EditText) findViewById(R.id.messageTxt);
        message.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEND && message.getText() != null && message.getText().length() > 0)
                    send.performClick();
                return true;
            }
        });
        message.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && message.getText() != null && message.getText().length() > 0)
                    send.performClick();
                return false;
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(message.getText() != null && message.getText().length() > 0) {
                    Intent i = new Intent(RemoteInputService.ACTION_REPLY);
                    i.setComponent(new ComponentName(getPackageName(), RemoteInputService.class.getName()));
                    i.putExtras(getIntent());
                    i.putExtra("reply", message.getText().toString());
                    startService(i);
                    finish();
                }
            }
        });

        ListView listView = (ListView) findViewById(R.id.conversation);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
        prefs.registerOnSharedPreferenceChangeListener(prefslistener);
        nickColors = prefs.getBoolean("nick-colors", false);
        if (prefs.getBoolean("time-24hr", false)) {
            if (prefs.getBoolean("time-seconds", false))
                formatter = new SimpleDateFormat("H:mm:ss");
            else
                formatter = new SimpleDateFormat("H:mm");
        } else if (prefs.getBoolean("time-seconds", false)) {
            formatter = new SimpleDateFormat("h:mm:ss a");
        }
        adapter.loadMessages(cid, bid);

        NotificationManagerCompat.from(this).cancel(bid);
        Notifications.getInstance().excludeBid(bid);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).unregisterOnSharedPreferenceChangeListener(prefslistener);
        Notifications.getInstance().excludeBid(-1);
    }
}
