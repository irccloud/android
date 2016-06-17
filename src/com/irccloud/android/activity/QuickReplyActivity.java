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



import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
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

import com.fasterxml.jackson.databind.type.ArrayType;
import com.irccloud.android.CollapsedEventsList;
import com.irccloud.android.ColorFormatter;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudLinkMovementMethod;
import com.irccloud.android.data.collection.NotificationsList;
import com.irccloud.android.R;
import com.irccloud.android.RemoteInputService;
import com.irccloud.android.data.model.Notification;
import com.irccloud.android.data.model.Server;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class QuickReplyActivity extends AppCompatActivity {
    int cid, bid;
    String to;
    SimpleDateFormat formatter = new SimpleDateFormat("h:mm a");
    CollapsedEventsList collapsedEventsList = new CollapsedEventsList();
    boolean nickColors;
    Server server;
    int timestamp_width = -1;

    private class MessagesAdapter extends BaseAdapter {
        private class ViewHolder {
            TextView timestamp;
            TextView message;
        }

        private ArrayList<Notification> msgs = new ArrayList<>();

        public void loadMessages(int cid, int bid) {
            msgs.clear();

            List<Notification> notifications = NotificationsList.getInstance().getNotifications();

            for(Notification n : notifications) {
                if(n.cid == cid && n.bid == bid)
                    msgs.add(n);
            }

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return msgs.size();
        }

        @Override
        public Object getItem(int i) {
            return msgs.get(i);
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
                holder.timestamp = (TextView)row.findViewById(R.id.timestamp_right);
                holder.message = (TextView)row.findViewById(R.id.message);

                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }

            Notification msg = msgs.get(i);
            if(getWindowManager().getDefaultDisplay().getWidth() < 800) {
               holder.timestamp.setVisibility(View.GONE);
            } else {
               Calendar calendar = Calendar.getInstance();
               calendar.setTimeInMillis(msg.eid / 1000);

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
               holder.timestamp.setVisibility(View.VISIBLE);
               holder.timestamp.setMinWidth(timestamp_width);
               holder.timestamp.setText(formatter.format(calendar.getTime()));
               holder.timestamp.setTextColor(ColorScheme.getInstance().timestampColor);
            }
           String nick = msg.nick;
           if(nick == null)
               nick = NotificationsList.getInstance().getServerNick(msg.cid);
            holder.message.setText(ColorFormatter.html_to_spanned("<b>" + ColorFormatter.irc_to_html(collapsedEventsList.formatNick(nick, null, nickColors)) + "</b> " + msg.message, true, server));
            holder.message.setMovementMethod(IRCCloudLinkMovementMethod.getInstance());
            holder.message.setTextColor(ColorScheme.getInstance().messageTextColor);
            holder.message.setLinkTextColor(ColorScheme.getInstance().linkColor);
            return row;
        }
    }

    private MessagesAdapter adapter = new MessagesAdapter();

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        cid = intent.getIntExtra("cid", -1);
        bid = intent.getIntExtra("bid", -1);
        to = intent.getStringExtra("to");
        server = new Server();
        server.setCid(cid);

        setTitle("Reply to " + to + " (" + intent.getStringExtra("network") + ")");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ColorScheme.getDialogTheme(ColorScheme.getUserTheme()));
        ColorScheme.getInstance().setThemeFromContext(this, ColorScheme.getUserTheme());
        if (Build.VERSION.SDK_INT >= 21) {
            Bitmap cloud = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.app_name), cloud, ColorScheme.getInstance().navBarColor));
            getWindow().setStatusBarColor(ColorScheme.getInstance().statusBarColor);
            getWindow().setNavigationBarColor(getResources().getColor(android.R.color.black));
        }
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
                if (message.getText() != null && message.getText().length() > 0) {
                    Intent i = new Intent(RemoteInputService.ACTION_REPLY);
                    i.setComponent(new ComponentName(getPackageName(), RemoteInputService.class.getName()));
                    i.putExtras(getIntent());
                    i.putExtra("reply", message.getText().toString());
                    startService(i);
                    message.setText("");
                }
            }
        });
        send.setColorFilter(ColorScheme.getInstance().colorControlNormal, PorterDuff.Mode.SRC_ATOP);

        ListView listView = (ListView) findViewById(R.id.conversation);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
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
        NotificationsList.getInstance().excludeBid(bid);
        NotificationsList.getInstance().notificationAddedListener = new NotificationsList.NotificationAddedListener() {
            @Override
            public void onNotificationAdded(Notification notification) {
                if(notification.cid == cid && notification.bid == bid) {
                    findViewById(R.id.conversation).post(new Runnable() {
                        @Override
                        public void run() {
                            adapter.loadMessages(cid, bid);
                        }
                    });
                }
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        NotificationsList.getInstance().excludeBid(-1);
        NotificationsList.getInstance().notificationAddedListener = null;
    }
}
