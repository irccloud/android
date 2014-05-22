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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.os.Debug;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.widget.DrawerLayout;
import android.view.*;
import org.json.JSONException;
import org.json.JSONObject;

import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.irccloud.android.ActionEditText;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.BuildConfig;
import com.irccloud.android.data.BuffersDataSource;
import com.irccloud.android.data.ChannelsDataSource;
import com.irccloud.android.ColorFormatter;
import com.irccloud.android.data.EventsDataSource;
import com.irccloud.android.GCMIntentService;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.Notifications;
import com.irccloud.android.R;
import com.irccloud.android.data.ServersDataSource;
import com.irccloud.android.data.UsersDataSource;
import com.irccloud.android.fragment.AcceptListFragment;
import com.irccloud.android.fragment.BanListFragment;
import com.irccloud.android.fragment.BufferOptionsFragment;
import com.irccloud.android.fragment.BuffersListFragment;
import com.irccloud.android.fragment.ChannelListFragment;
import com.irccloud.android.fragment.ChannelOptionsFragment;
import com.irccloud.android.fragment.EditConnectionFragment;
import com.irccloud.android.fragment.IgnoreListFragment;
import com.irccloud.android.fragment.MessageViewFragment;
import com.irccloud.android.fragment.NamesListFragment;
import com.irccloud.android.fragment.NickservFragment;
import com.irccloud.android.fragment.ServerReorderFragment;
import com.irccloud.android.fragment.UsersListFragment;
import com.irccloud.android.fragment.WhoListFragment;
import com.irccloud.android.fragment.WhoisFragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.TextView.OnEditorActionListener;

public class MessageActivity extends BaseActivity implements UsersListFragment.OnUserSelectedListener, BuffersListFragment.OnBufferSelectedListener, MessageViewFragment.MessageViewListener, NetworkConnection.IRCEventHandler {
    BuffersDataSource.Buffer buffer;
    ServersDataSource.Server server;
	ActionEditText messageTxt;
	View sendBtn;
	UsersDataSource.User selected_user;
	View userListView;
	View buffersListView;
	TextView title;
	TextView subtitle;
	ImageView key;
	LinearLayout messageContainer;
    DrawerLayout drawerLayout;
	NetworkConnection conn;
	private boolean shouldFadeIn = false;
	ImageView upView;
	private RefreshUpIndicatorTask refreshUpIndicatorTask = null;
	private ShowNotificationsTask showNotificationsTask = null;
	private ArrayList<Integer> backStack = new ArrayList<Integer>();
	PowerManager.WakeLock screenLock = null;
	private int launchBid = -1;
	private Uri launchURI = null;
	private AlertDialog channelsListDialog;
    String bufferToOpen = null;
    int cidToOpen = -1;
    private Uri imageCaptureURI = null;

    private class SuggestionsAdapter extends ArrayAdapter<String> {
        public SuggestionsAdapter() {
            super(MessageActivity.this, R.layout.row_suggestion);
            setNotifyOnChange(false);
        }
        public int activePos = -1;

        @Override
        public void clear() {
            super.clear();
            activePos = -1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView v = (TextView)super.getView(position, convertView, parent);

            if(position == activePos) {
                v.setTextColor(0xffffffff);
                v.setBackgroundResource(R.drawable.selected_blue);
            } else {
                v.setTextColor(getResources().getColor(R.color.row_label));
                v.setBackgroundResource(R.drawable.row_bg_blue);
            }

            //This will prevent GridView from stealing focus from the EditText by bypassing the check on line 1397 of GridView.java in the Android Source
            v.setSelected(true);
            return v;
        }
    };
    private SuggestionsAdapter suggestionsAdapter;
    private View suggestionsContainer;
    private GridView suggestions;
    private Timer suggestionsTimer = new Timer("suggestions-timer");
    private TimerTask suggestionsTimerTask = null;
    private ArrayList<UsersDataSource.User> sortedUsers = null;
    private ArrayList<ChannelsDataSource.Channel> sortedChannels = null;
    private ImgurUploadTask imgurTask = null;

    private HashMap<Integer, EventsDataSource.Event> pendingEvents = new HashMap<Integer, EventsDataSource.Event>();
	
    @SuppressLint("NewApi")
	@SuppressWarnings({ "deprecation", "unchecked" })
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		getWindow().setBackgroundDrawable(null);
        setContentView(R.layout.activity_message);
        suggestionsAdapter = new SuggestionsAdapter();
        buffersListView = findViewById(R.id.BuffersList);
        messageContainer = (LinearLayout)findViewById(R.id.messageContainer);
        drawerLayout = (DrawerLayout)findViewById(R.id.drawerLayout);
        
        messageTxt = (ActionEditText)findViewById(R.id.messageTxt);
		messageTxt.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if(sendBtn.isEnabled() && NetworkConnection.getInstance().getState() == NetworkConnection.STATE_CONNECTED && event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && messageTxt.getText() != null && messageTxt.getText().length() > 0) {
                    sendBtn.setEnabled(false);
	         		new SendTask().execute((Void)null);
                } else if(keyCode == KeyEvent.KEYCODE_TAB) {
                    if(event.getAction() == KeyEvent.ACTION_DOWN)
                        nextSuggestion();
                    return true;
                }
				return false;
			}
		});
		messageTxt.setOnFocusChangeListener(new OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if(drawerLayout != null && v == messageTxt && hasFocus) {
                    drawerLayout.closeDrawers();
                    if(getSupportFragmentManager().findFragmentById(R.id.usersListFragment2) == null)
    		        	upView.setVisibility(View.VISIBLE);
                    update_suggestions(false);
				} else if(!hasFocus) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            suggestionsContainer.setVisibility(View.INVISIBLE);
                        }
                    });
                }
			}
		});
		messageTxt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout != null) {
                    drawerLayout.closeDrawers();
                    if(getSupportFragmentManager().findFragmentById(R.id.usersListFragment2) == null)
                        upView.setVisibility(View.VISIBLE);
                }
            }
        });
        messageTxt.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (sendBtn.isEnabled() && NetworkConnection.getInstance().getState() == NetworkConnection.STATE_CONNECTED && actionId == EditorInfo.IME_ACTION_SEND && messageTxt.getText() != null && messageTxt.getText().length() > 0) {
                    sendBtn.setEnabled(false);
                    new SendTask().execute((Void) null);
                }
                return true;
            }
        });
        messageTxt.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                Object[] spans = s.getSpans(0, s.length(), Object.class);
                for (Object o : spans) {
                    if (((s.getSpanFlags(o) & Spanned.SPAN_COMPOSING) != Spanned.SPAN_COMPOSING) && (o.getClass() == StyleSpan.class || o.getClass() == ForegroundColorSpan.class || o.getClass() == BackgroundColorSpan.class || o.getClass() == UnderlineSpan.class)) {
                        s.removeSpan(o);
                    }
                }
                if (s.length() > 0 && NetworkConnection.getInstance().getState() == NetworkConnection.STATE_CONNECTED) {
                    sendBtn.setEnabled(true);
                    if (Build.VERSION.SDK_INT >= 11)
                        sendBtn.setAlpha(1);
                } else {
                    sendBtn.setEnabled(false);
                    if (Build.VERSION.SDK_INT >= 11)
                        sendBtn.setAlpha(0.5f);
                }
                String text = s.toString();
                if (text.endsWith("\t")) { //Workaround for Swype
                    text = text.substring(0, text.length() - 1);
                    messageTxt.setText(text);
                    nextSuggestion();
                } else if(suggestionsContainer != null && suggestionsContainer.getVisibility() == View.VISIBLE) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update_suggestions(false);
                        }
                    });
                } else {
                        if(suggestionsTimerTask != null)
                            suggestionsTimerTask.cancel();
                    suggestionsTimerTask = new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    update_suggestions(false);
                                }
                            });
                        }
                    };
                    suggestionsTimer.schedule(suggestionsTimerTask, 250);
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        sendBtn = findViewById(R.id.sendBtn);
        sendBtn.setFocusable(false);
        sendBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (NetworkConnection.getInstance().getState() == NetworkConnection.STATE_CONNECTED)
                    new SendTask().execute((Void) null);
            }
        });

        View photoBtn = findViewById(R.id.photoBtn);
        if(photoBtn != null) {
            if (Build.VERSION.SDK_INT < 11) {
                photoBtn.setVisibility(View.GONE);
            } else {
                photoBtn.setFocusable(false);
                photoBtn.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        insertPhoto();
                    }
                });
            }
        }
        userListView = findViewById(R.id.usersListFragment);
        
        getSupportActionBar().setLogo(R.drawable.logo);
        getSupportActionBar().setHomeButtonEnabled(false);
       	getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        
        View v = getLayoutInflater().inflate(R.layout.actionbar_messageview, null);
        v.findViewById(R.id.actionTitleArea).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                show_topic_popup();
            }
        });

        upView = (ImageView)v.findViewById(R.id.upIndicator);
        if(drawerLayout != null && findViewById(R.id.usersListFragment2) == null) {
            drawerLayout.setDrawerListener(mDrawerListener);
        	upView.setVisibility(View.VISIBLE);
        	upView.setOnClickListener(upClickListener);
	        ImageView icon = (ImageView)v.findViewById(R.id.upIcon);
	        icon.setOnClickListener(upClickListener);
	        if(refreshUpIndicatorTask != null)
	        	refreshUpIndicatorTask.cancel(true);
	        refreshUpIndicatorTask = new RefreshUpIndicatorTask();
	        refreshUpIndicatorTask.execute((Void)null);
        } else {
        	upView.setVisibility(View.INVISIBLE);
        }
		messageTxt.setDrawerLayout(drawerLayout, upView);

        title = (TextView)v.findViewById(R.id.title);
        subtitle = (TextView)v.findViewById(R.id.subtitle);
        key = (ImageView)v.findViewById(R.id.key);
        getSupportActionBar().setCustomView(v);
        
        if(savedInstanceState != null && savedInstanceState.containsKey("cid")) {
        	server = ServersDataSource.getInstance().getServer(savedInstanceState.getInt("cid"));
        	buffer = BuffersDataSource.getInstance().getBuffer(savedInstanceState.getInt("bid"));
        	backStack = (ArrayList<Integer>) savedInstanceState.getSerializable("backStack");
        }
        if(getSharedPreferences("prefs", 0).contains("session_key") && BuildConfig.GCM_ID.length() > 0 && checkPlayServices()) {
            final String regId = GCMIntentService.getRegistrationId(this);
            if (regId.equals("") || !getSharedPreferences("prefs", 0).contains("gcm_registered")) {
                GCMIntentService.scheduleRegisterTimer(100);
            }
        }

        if(savedInstanceState != null && savedInstanceState.containsKey("imagecaptureuri"))
            imageCaptureURI = Uri.parse(savedInstanceState.getString("imagecaptureuri"));
        else
            imageCaptureURI = null;

        imgurTask = (ImgurUploadTask)getLastCustomNonConfigurationInstance();
    }

    private void show_topic_popup() {
        ChannelsDataSource.Channel c = ChannelsDataSource.getInstance().getChannelForBuffer(buffer.bid);
        if (c != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
            builder.setTitle("Channel Topic");
            if (c.topic_text.length() > 0) {
                String author = "";
                if(c.topic_author != null && c.topic_author.length() > 0) {
                    author = "<br/>— Set by " + c.topic_author;
                    if(c.topic_time > 0) {
                        author += " on " + DateFormat.getDateTimeInstance().format(new Date(c.topic_time * 1000));
                    }
                }
                builder.setMessage(ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(TextUtils.htmlEncode(c.topic_text)) + author, true, server));
            } else
                builder.setMessage("No topic set.");
            builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            boolean canEditTopic;
            if (c.mode.contains("t")) {
                UsersDataSource.User self_user = UsersDataSource.getInstance().getUser(buffer.bid, server.nick);
                canEditTopic = (self_user != null && (self_user.mode.contains(server!=null?server.MODE_OWNER:"q") || self_user.mode.contains(server!=null?server.MODE_ADMIN:"a") || self_user.mode.contains(server!=null?server.MODE_OP:"o")));
            } else {
                canEditTopic = true;
            }

            if (canEditTopic) {
                builder.setNeutralButton("Edit", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        editTopic();
                    }
                });
            }
            final AlertDialog dialog = builder.create();
            dialog.setOwnerActivity(MessageActivity.this);
            dialog.show();
            ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
            dialog.findViewById(android.R.id.message).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }

            });
        } else if (buffer != null && buffer.type.equals("channel") && buffer.archived == 0 && title.getText() != null && subtitle.getText() != null && subtitle.getText().length() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
            builder.setTitle(title.getText().toString());
            final SpannableString s = new SpannableString(subtitle.getText().toString());
            Linkify.addLinks(s, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
            builder.setMessage(s);
            builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.setOwnerActivity(MessageActivity.this);
            dialog.show();
            ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private void update_suggestions(boolean force) {
        if(suggestionsContainer != null && messageTxt != null && messageTxt.getText() != null) {
            String text = messageTxt.getText().toString().toLowerCase();
            if(text.lastIndexOf(' ') > 0 && text.lastIndexOf(' ') < text.length() - 1) {
                text = text.substring(text.lastIndexOf(' ') + 1);
            }
            if(text.endsWith(":"))
                text = text.substring(0, text.length() - 1);
            ArrayList<String> sugs = new ArrayList<String>();
            if(text.length() > 1 || force || (text.length() > 0 && suggestionsAdapter.activePos != -1)) {
                if(sortedChannels == null) {
                    sortedChannels = ChannelsDataSource.getInstance().getChannels();
                    Collections.sort(sortedChannels, new Comparator<ChannelsDataSource.Channel>() {
                        @Override
                        public int compare(ChannelsDataSource.Channel lhs, ChannelsDataSource.Channel rhs) {
                            return lhs.name.compareTo(rhs.name);
                        }
                    });
                }

                if(buffer != null && messageTxt.getText().length() > 0 && buffer.type.equals("channel") && buffer.name.toLowerCase().startsWith(text))
                    sugs.add(buffer.name);
                for(ChannelsDataSource.Channel channel : sortedChannels) {
                    if(text.length() > 0 && text.charAt(0) == channel.name.charAt(0) && channel.name.toLowerCase().startsWith(text) && !channel.name.equalsIgnoreCase(buffer.name))
                        sugs.add(channel.name);
                }

                if(sortedUsers == null && buffer != null) {
                    sortedUsers = UsersDataSource.getInstance().getUsersForBuffer(buffer.bid);
                    Collections.sort(sortedUsers, new Comparator<UsersDataSource.User>() {
                        @Override
                        public int compare(UsersDataSource.User lhs, UsersDataSource.User rhs) {
                            if(lhs.last_mention > rhs.last_mention)
                                return -1;
                            if(lhs.last_mention < rhs.last_mention)
                                return 1;
                            return lhs.nick.compareToIgnoreCase(rhs.nick);
                        }
                    });
                }
                if(sortedUsers != null) {
                    for (UsersDataSource.User user : sortedUsers) {
                        if (user.nick.toLowerCase().startsWith(text))
                            sugs.add(user.nick);
                    }
                }
            }
            if(sugs.size() > 0) {
                if(suggestionsAdapter.activePos == -1) {
                    suggestionsAdapter.clear();
                    for(String s : sugs) {
                        suggestionsAdapter.add(s);
                    }
                    suggestionsAdapter.notifyDataSetChanged();
                    suggestions.smoothScrollToPosition(0);
                }
                if(suggestionsContainer.getVisibility() == View.INVISIBLE) {
                    AlphaAnimation anim = new AlphaAnimation(0, 1);
                    anim.setDuration(250);
                    anim.setFillAfter(true);
                    suggestionsContainer.startAnimation(anim);
                    suggestionsContainer.setVisibility(View.VISIBLE);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (suggestionsContainer.getHeight() < 48) {
                                getSupportActionBar().hide();
                            }
                        }
                    });
                }
            } else {
                if(suggestionsContainer.getVisibility() == View.VISIBLE) {
                    AlphaAnimation anim = new AlphaAnimation(1, 0);
                    anim.setDuration(250);
                    anim.setFillAfter(true);
                    anim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            suggestionsContainer.setVisibility(View.INVISIBLE);
                            suggestionsAdapter.clear();
                            suggestionsAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    suggestionsContainer.startAnimation(anim);
                    sortedUsers = null;
                    sortedChannels = null;
                    if(!getSupportActionBar().isShowing())
                        getSupportActionBar().show();
                }
            }
        }
    }

    private void nextSuggestion() {
        if(suggestionsAdapter.getCount() == 0)
            update_suggestions(true);

        if(suggestionsAdapter.getCount() > 0) {
            if(suggestionsAdapter.activePos < 0 || suggestionsAdapter.activePos >= suggestionsAdapter.getCount() - 1) {
                suggestionsAdapter.activePos = 0;
            } else {
                suggestionsAdapter.activePos++;
            }
            suggestionsAdapter.notifyDataSetChanged();
            suggestions.smoothScrollToPosition(suggestionsAdapter.activePos);

            String nick = suggestionsAdapter.getItem(suggestionsAdapter.activePos);
            String text = messageTxt.getText().toString();

            if(text.lastIndexOf(' ') > 0) {
                messageTxt.setText(text.substring(0,text.lastIndexOf(' ') + 1) + nick);
            } else {
                if(nick.startsWith("#"))
                    messageTxt.setText(nick);
                else
                    messageTxt.setText(nick + ":");
            }
            messageTxt.setSelection(messageTxt.getText().length());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
    	super.onSaveInstanceState(state);
        if(server != null)
        	state.putInt("cid", server.cid);
        if(buffer != null) {
            state.putInt("bid", buffer.bid);
            if(messageTxt != null && messageTxt.getText() != null)
                buffer.draft = messageTxt.getText().toString();
            else
                buffer.draft = null;
        }
    	state.putSerializable("backStack", backStack);
        if(imageCaptureURI != null)
            state.putString("imagecaptureuri", imageCaptureURI.toString());
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) { //Back key pressed
        	if(drawerLayout != null && (drawerLayout.isDrawerOpen(Gravity.LEFT) || drawerLayout.isDrawerOpen(Gravity.RIGHT))) {
                drawerLayout.closeDrawers();
                if(getSupportFragmentManager().findFragmentById(R.id.usersListFragment2) == null)
    	        	upView.setVisibility(View.VISIBLE);
	        	return true;
        	}
            while(backStack != null && backStack.size() > 0) {
        		Integer bid = backStack.get(0);
        		backStack.remove(0);
        		BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBuffer(bid);
        		if(b != null) {
                    onBufferSelected(bid);
	    			if(backStack.size() > 0)
	    				backStack.remove(0);
                    return true;
        		}
        	}
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private class SendTask extends AsyncTaskEx<Void, Void, Void> {
    	EventsDataSource.Event e = null;
    	
    	@Override
    	protected void onPreExecute() {
			if(conn != null && conn.getState() == NetworkConnection.STATE_CONNECTED && messageTxt.getText() != null && messageTxt.getText().length() > 0 && buffer != null && server != null) {
                sendBtn.setEnabled(false);
                if(Build.VERSION.SDK_INT >= 11)
                    sendBtn.setAlpha(0.5f);
                UsersDataSource.User u = UsersDataSource.getInstance().getUser(buffer.bid, server.nick);
                e = EventsDataSource.getInstance().new Event();
                e.command = messageTxt.getText().toString();
                e.cid = buffer.cid;
                e.bid = buffer.bid;
                e.eid = (System.currentTimeMillis() + conn.clockOffset + 5000) * 1000L;
                e.self = true;
                e.from = server.nick;
                e.nick = server.nick;
                if(!buffer.type.equals("console"))
                    e.chan = buffer.name;
                if(u != null)
                    e.from_mode = u.mode;
                String msg = messageTxt.getText().toString();
                if(msg.startsWith("//"))
                    msg = msg.substring(1);
                else if(msg.startsWith("/") && !msg.startsWith("/me "))
                    msg = null;
                e.msg = msg;
                if(msg != null && msg.toLowerCase().startsWith("/me ")) {
                    e.type = "buffer_me_msg";
                    e.msg = msg.substring(4);
                } else {
                    e.type = "buffer_msg";
                }
                e.color = R.color.timestamp;
                if(title.getText() != null && title.getText().equals(server.nick))
                    e.bg_color = R.color.message_bg;
                else
                    e.bg_color = R.color.self;
                e.row_type = 0;
                e.html = null;
                e.group_msg = null;
                e.linkify = true;
                e.target_mode = null;
                e.highlight = false;
                e.reqid = -1;
                e.pending = true;
                if(e.msg != null) {
                    e.msg = TextUtils.htmlEncode(e.msg);
                    EventsDataSource.getInstance().addEvent(e);
                    conn.notifyHandlers(NetworkConnection.EVENT_BUFFERMSG, e, MessageActivity.this);
                }
			}
    	}
    	
		@Override
		protected Void doInBackground(Void... arg0) {
            if(BuildConfig.DEBUG && e != null && e.command != null) {
                if(e.command.equals("/starttrace") || e.command.equals("/stoptrace") || e.command.equals("/crash")) {
                    e.reqid = -2;
                    return null;
                }
            }
            if(e != null && conn != null && conn.getState() == NetworkConnection.STATE_CONNECTED && messageTxt.getText() != null && messageTxt.getText().length() > 0) {
                e.reqid = conn.say(e.cid, e.chan, e.command);
                if(e.msg != null)
                    pendingEvents.put(e.reqid, e);
            }
			return null;
		}
    	
		@Override
		protected void onPostExecute(Void result) {
            if(BuildConfig.DEBUG) {
                if(messageTxt.getText().toString().equals("/starttrace")) {
                    Debug.startMethodTracing("irccloud");
                    showAlert(e.cid, "Method tracing started");
                } else if(messageTxt.getText().toString().equals("/stoptrace")) {
                    Debug.stopMethodTracing();
                    showAlert(e.cid, "Method tracing finished");
                } else if(messageTxt.getText().toString().equals("/crash")) {
                    Crashlytics.getInstance().crash();
                }
            }
            if(e != null && e.reqid != -1) {
				messageTxt.setText("");
                BuffersDataSource.getInstance().updateDraft(e.bid, null);
			} else {
                sendBtn.setEnabled(true);
                if(Build.VERSION.SDK_INT >= 11)
                    sendBtn.setAlpha(1);
            }
		}
    }
    
    private class RefreshUpIndicatorTask extends AsyncTaskEx<Void, Void, Void> {
		int unread = 0;
		int highlights = 0;
    	
		@Override
		protected Void doInBackground(Void... arg0) {
            if(drawerLayout != null) {
                JSONObject channelDisabledMap = null;
                JSONObject bufferDisabledMap = null;
                if(NetworkConnection.getInstance().getUserInfo() != null && NetworkConnection.getInstance().getUserInfo().prefs != null) {
                    try {
                        if(NetworkConnection.getInstance().getUserInfo().prefs.has("channel-disableTrackUnread"))
                            channelDisabledMap = NetworkConnection.getInstance().getUserInfo().prefs.getJSONObject("channel-disableTrackUnread");
                        if(NetworkConnection.getInstance().getUserInfo().prefs.has("buffer-disableTrackUnread"))
                            bufferDisabledMap = NetworkConnection.getInstance().getUserInfo().prefs.getJSONObject("buffer-disableTrackUnread");
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                ArrayList<BuffersDataSource.Buffer> buffers = BuffersDataSource.getInstance().getBuffers();
                for(int j = 0; j < buffers.size(); j++) {
                    BuffersDataSource.Buffer b = buffers.get(j);
                    if(buffer == null || b.bid != buffer.bid) {
                        if(unread == 0) {
                            int u = 0;
                            try {
                                u = EventsDataSource.getInstance().getUnreadStateForBuffer(b.bid, b.last_seen_eid, b.type);
                                if(b.type.equalsIgnoreCase("channel") && channelDisabledMap != null && channelDisabledMap.has(String.valueOf(b.bid)) && channelDisabledMap.getBoolean(String.valueOf(b.bid)))
                                    u = 0;
                                else if(bufferDisabledMap != null && bufferDisabledMap.has(String.valueOf(b.bid)) && bufferDisabledMap.getBoolean(String.valueOf(b.bid)))
                                    u = 0;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            unread += u;
                        }
                        if(highlights == 0) {
                            try {
                                if(!b.type.equalsIgnoreCase("conversation") || bufferDisabledMap == null || !bufferDisabledMap.has(String.valueOf(b.bid)) || !bufferDisabledMap.getBoolean(String.valueOf(b.bid))) {
                                    highlights += EventsDataSource.getInstance().getHighlightStateForBuffer(b.bid, b.last_seen_eid, b.type);
                                }
                            } catch (JSONException e) {
                            }
                        }
                        if(highlights > 0)
                            break;
                    }
                }
            }
			return null;
		}
    	
		@Override
		protected void onPostExecute(Void result) {
			if(!isCancelled() && drawerLayout != null) {
				if(highlights > 0) {
                    mDrawerListener.setUpDrawable(getResources().getDrawable(R.drawable.ic_navigation_drawer_highlight));
                    upView.setTag(R.drawable.ic_navigation_drawer_highlight);
				} else if(unread > 0) {
                    mDrawerListener.setUpDrawable(getResources().getDrawable(R.drawable.ic_navigation_drawer_unread));
                    upView.setTag(R.drawable.ic_navigation_drawer_unread);
				} else {
                    mDrawerListener.setUpDrawable(getResources().getDrawable(R.drawable.ic_navigation_drawer));
                    upView.setTag(R.drawable.ic_navigation_drawer);
				}
				refreshUpIndicatorTask = null;
			}
		}
    }
    
    private class ShowNotificationsTask extends AsyncTaskEx<Integer, Void, Void> {

		@Override
		protected Void doInBackground(Integer... params) {
	    	Notifications.getInstance().excludeBid(params[0]);
	    	if(params[0] > 0)
	    		Notifications.getInstance().showNotifications(null);
	    	showNotificationsTask = null;
			return null;
		}
    }
    
    private void setFromIntent(Intent intent) {
    	launchBid = -1;
    	launchURI = null;

        if(NetworkConnection.getInstance().ready)
        	setIntent(null);
    	
    	if(intent.hasExtra("bid")) {
    		int new_bid = intent.getIntExtra("bid", 0);
    		if(NetworkConnection.getInstance().ready && NetworkConnection.getInstance().getState() == NetworkConnection.STATE_CONNECTED && BuffersDataSource.getInstance().getBuffer(new_bid) == null) {
    			Crashlytics.log(Log.WARN, "IRCCloud", "Invalid bid requested by launch intent: " + new_bid);
    			Notifications.getInstance().deleteNotificationsForBid(new_bid);
    			if(showNotificationsTask != null)
    				showNotificationsTask.cancel(true);
    			showNotificationsTask = new ShowNotificationsTask();
    			showNotificationsTask.execute(new_bid);
    			return;
    		} else if(BuffersDataSource.getInstance().getBuffer(new_bid) != null) {
                Crashlytics.log(Log.DEBUG, "IRCCloud", "Found BID, switching buffers");
    	    	if(buffer != null && buffer.bid != new_bid)
    	    		backStack.add(0, buffer.bid);
                buffer = BuffersDataSource.getInstance().getBuffer(new_bid);
                server = ServersDataSource.getInstance().getServer(buffer.cid);
    		} else {
                Crashlytics.log(Log.DEBUG, "IRCCloud", "BID not found, will try after reconnecting");
                launchBid = new_bid;
            }
    	}
    	
    	if(intent.getData() != null && intent.getData().getScheme() != null && intent.getData().getScheme().startsWith("irc")) {
    		if(open_uri(intent.getData()))
    			return;
    		else
    			launchURI = intent.getData();
    	} else if(intent.hasExtra("cid")) {
	    	if(buffer == null) {
	    		buffer = BuffersDataSource.getInstance().getBufferByName(intent.getIntExtra("cid", 0), intent.getStringExtra("name"));
	    		if(buffer != null) {
                    server = ServersDataSource.getInstance().getServer(intent.getIntExtra("cid", 0));
	    		}
	    	}
    	}

        if(buffer == null)
            server = null;

        if(intent.hasExtra("reply")) {
            if(NetworkConnection.getInstance().getState() == NetworkConnection.STATE_CONNECTED) {
                NetworkConnection.getInstance().say(intent.getIntExtra("cid", -1), intent.getStringExtra("to"), (intent.hasExtra("nick")?intent.getStringExtra("nick") + ": ":"") + intent.getStringExtra("reply"));
            } else {
                NetworkConnection.getInstance().incoming_reply_cid = intent.getIntExtra("cid", -1);
                NetworkConnection.getInstance().incoming_reply_to = intent.getStringExtra("to");
                NetworkConnection.getInstance().incoming_reply_msg = (intent.hasExtra("nick")?intent.getStringExtra("nick") + ": ":"") + intent.getStringExtra("reply");
            }
        }

        if(buffer == null) {
			launchBid = intent.getIntExtra("bid", -1);
    	} else {
            onBufferSelected(buffer.bid);
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
    	if(intent != null) {
            Crashlytics.log(Log.DEBUG, "IRCCloud", "Got new launch intent");
    		setFromIntent(intent);
    	}
    }

    @SuppressLint("NewApi")
	@Override
    public void onResume() {
        Crashlytics.log(Log.DEBUG, "IRCCloud", "Resuming app");
    	conn = NetworkConnection.getInstance();
    	if(!conn.ready) {
        	super.onResume();
            Crashlytics.log(Log.DEBUG, "IRCCloud", "App was resumed but data has been lost, returning to splash screen");
    		Intent i = new Intent(this, MainActivity.class);
    		if(getIntent() != null) {
    			if(getIntent().getData() != null)
    				i.setData(getIntent().getData());
    			if(getIntent().getExtras() != null)
    				i.putExtras(getIntent().getExtras());
    		}
    		startActivity(i);
    		finish();
    		return;
    	}
    	conn.addHandler(this);

    	super.onResume();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	if(prefs.getBoolean("screenlock", false)) {
    		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	} else {
    		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	}
    	
    	if(conn.getState() != NetworkConnection.STATE_CONNECTED) {
    		if(drawerLayout != null && !NetworkConnection.getInstance().ready) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            	upView.setVisibility(View.INVISIBLE);
    		}
    		sendBtn.setEnabled(false);
       		if(Build.VERSION.SDK_INT >= 11)
       			sendBtn.setAlpha(0.5f);
    	} else {
    		if(drawerLayout != null) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                if(getSupportFragmentManager().findFragmentById(R.id.usersListFragment2) == null)
                	upView.setVisibility(View.VISIBLE);
    		}
    		if(messageTxt.getText() != null && messageTxt.getText().length() > 0) {
    			sendBtn.setEnabled(true);
           		if(Build.VERSION.SDK_INT >= 11)
           			sendBtn.setAlpha(1);
    		}
    	}

    	if(server == null || launchURI != null || (getIntent() != null && (getIntent().hasExtra("bid") || getIntent().getData() != null))) {
    		if(getIntent() != null && (getIntent().hasExtra("bid") || getIntent().getData() != null)) {
                Crashlytics.log(Log.DEBUG, "IRCCloud", "Launch intent contains a BID or URL");
	    		setFromIntent(getIntent());
	    	} else if(conn.getState() == NetworkConnection.STATE_CONNECTED && conn.getUserInfo() != null && conn.ready) {
	    		if(launchURI == null || !open_uri(launchURI)) {
		    		if(!open_bid(conn.getUserInfo().last_selected_bid)) {
		    			if(!open_bid(BuffersDataSource.getInstance().firstBid())) {
		    				if(drawerLayout != null && NetworkConnection.getInstance().ready)
                                drawerLayout.openDrawer(Gravity.LEFT);
                        }
		    		}
	    		}
	    	}
    	} else if(buffer != null) {
            int bid = buffer.bid;
            onBufferSelected(bid);
        }

    	updateUsersListFragmentVisibility();
    	update_subtitle();

        suggestions = ((MessageViewFragment)getSupportFragmentManager().findFragmentById(R.id.messageViewFragment)).suggestions;
        suggestions.setAdapter(suggestionsAdapter);
        suggestions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String nick = suggestionsAdapter.getItem(position);
                String text = messageTxt.getText().toString();

                if(text.lastIndexOf(' ') > 0) {
                    messageTxt.setText(text.substring(0, text.lastIndexOf(' ') + 1) + nick + " ");
                } else {
                    if(nick.startsWith("#"))
                        messageTxt.setText(nick + " ");
                    else
                        messageTxt.setText(nick + ": ");
                }
                messageTxt.setSelection(messageTxt.getText().length());
            }
        });
        suggestionsContainer = ((MessageViewFragment)getSupportFragmentManager().findFragmentById(R.id.messageViewFragment)).suggestionsContainer;
        update_suggestions(false);

        if(refreshUpIndicatorTask != null)
        	refreshUpIndicatorTask.cancel(true);
        refreshUpIndicatorTask = new RefreshUpIndicatorTask();
        refreshUpIndicatorTask.execute((Void)null);

    	supportInvalidateOptionsMenu();
    	
    	if(NetworkConnection.getInstance().ready && buffer != null) {
            try {
                if (showNotificationsTask != null)
                    showNotificationsTask.cancel(true);
            } catch (Exception e) {
            }
			showNotificationsTask = new ShowNotificationsTask();
			showNotificationsTask.execute(buffer.bid);
    	}
   		sendBtn.setEnabled(messageTxt.getText().length() > 0);
   		if(Build.VERSION.SDK_INT >= 11 && messageTxt.getText().length() == 0)
   			sendBtn.setAlpha(0.5f);

        if(drawerLayout != null)
            drawerLayout.closeDrawers();

        if(imgurTask != null)
            imgurTask.setActivity(this);
    }

    @Override
    public void onPause() {
    	super.onPause();
        if(imgurTask != null)
            imgurTask.setActivity(null);
		if(showNotificationsTask != null)
			showNotificationsTask.cancel(true);
		showNotificationsTask = new ShowNotificationsTask();
		showNotificationsTask.execute(-1);
		if(channelsListDialog != null)
			channelsListDialog.dismiss();
    	if(conn != null)
    		conn.removeHandler(this);
        suggestionsAdapter.clear();
    	conn = null;
    }

    public Object onRetainCustomNonConfigurationInstance () {
        return imgurTask;
    }

    private boolean open_uri(Uri uri) {
		if(uri != null && conn != null && conn.ready) {
			launchURI = null;
    		ServersDataSource.Server s = null;
            try {
                if(Integer.parseInt(uri.getHost()) > 0) {
                    s = ServersDataSource.getInstance().getServer(Integer.parseInt(uri.getHost()));
                }
            } catch (NumberFormatException e) {

            }
            if(s == null) {
                if(uri.getPort() > 0)
                    s = ServersDataSource.getInstance().getServer(uri.getHost(), uri.getPort());
                else if(uri.getScheme() != null && uri.getScheme().equalsIgnoreCase("ircs"))
                    s = ServersDataSource.getInstance().getServer(uri.getHost(), true);
                else
                    s = ServersDataSource.getInstance().getServer(uri.getHost());
            }

    		if(s != null) {
    			if(uri.getPath() != null && uri.getPath().length() > 1) {
	    			String key = null;
	    			String channel = uri.getPath().substring(1);
	    			if(channel.contains(",")) {
	    				key = channel.substring(channel.indexOf(",") + 1);
	    				channel = channel.substring(0, channel.indexOf(","));
	    			}
	    			BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBufferByName(s.cid, channel);
	    			if(b != null) {
                        server = null;
	    				return open_bid(b.bid);
                    } else {
	    				conn.join(s.cid, channel, key);
                    }
	    			return true;
    			} else {
	    			BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBufferByName(s.cid, "*");
	    			if(b != null)
	    				return open_bid(b.bid);
    			}
    		} else {
				if(getWindowManager().getDefaultDisplay().getWidth() < 800) {
					Intent i = new Intent(this, EditConnectionActivity.class);
					i.putExtra("hostname", uri.getHost());
					if(uri.getPort() > 0)
						i.putExtra("port", uri.getPort());
	    			else if(uri.getScheme().equalsIgnoreCase("ircs"))
	    				i.putExtra("port", 6697);
	    			if(uri.getPath() != null && uri.getPath().length() > 1)
	    				i.putExtra("channels", uri.getPath().substring(1).replace(",", " "));
					startActivity(i);
				} else {
		        	EditConnectionFragment connFragment = new EditConnectionFragment();
		        	connFragment.default_hostname = uri.getHost();
	    			if(uri.getPort() > 0)
	    				connFragment.default_port = uri.getPort();
	    			else if(uri.getScheme().equalsIgnoreCase("ircs"))
	    				connFragment.default_port = 6697;
	    			if(uri.getPath() != null && uri.getPath().length() > 1)
	    				connFragment.default_channels = uri.getPath().substring(1).replace(",", " ");
		            connFragment.show(getSupportFragmentManager(), "addnetwork");
				}
	            return true;
    		}
		}
		return false;
    }
    
    private boolean open_bid(int bid) {
		if(BuffersDataSource.getInstance().getBuffer(bid) != null) {
			onBufferSelected(bid);
            if(bid == launchBid)
                launchBid = -1;
			return true;
		}
		Log.w("IRCCloud", "Requested BID not found");
		return false;
    }
    
    private void update_subtitle() {
        if(server == null || buffer == null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowCustomEnabled(false);
            title.setText(null);
            getSupportActionBar().setTitle(null);
            subtitle.setVisibility(View.GONE);
        } else if(!NetworkConnection.getInstance().ready) {
           	getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowCustomEnabled(false);
        } else {
           	getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setDisplayShowCustomEnabled(true);

            if(buffer.type.equals("console")) {
                if(server.name.length() > 0) {
                    title.setText(server.name);
                    getSupportActionBar().setTitle(server.name);
                } else {
                    title.setText(server.hostname);
                    getSupportActionBar().setTitle(server.hostname);
                }
            } else {
                title.setText(buffer.name);
                getSupportActionBar().setTitle(buffer.name);
            }

            if(buffer.archived > 0 && !buffer.type.equals("console")) {
	    		subtitle.setVisibility(View.VISIBLE);
	    		subtitle.setText("(archived)");
                if(buffer.type.equals("conversation")) {
                    title.setContentDescription("Conversation with " + title.getText());
                } else if(buffer.type.equals("channel")) {
                    title.setContentDescription("Channel " + buffer.normalizedName());
                }
	    	} else {
	    		if(buffer.type.equals("conversation")) {
                    title.setContentDescription("Conversation with " + title.getText());
	    			if(buffer.away_msg != null && buffer.away_msg.length() > 0) {
		        		subtitle.setVisibility(View.VISIBLE);
	    				if(buffer.away_msg != null && buffer.away_msg.length() > 0) {
	    					subtitle.setText("Away: " + ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(TextUtils.htmlEncode(buffer.away_msg))).toString());
	    				} else {
	    					subtitle.setText("Away");
	    				}
	    			} else {
                        UsersDataSource.User u = UsersDataSource.getInstance().findUserOnConnection(buffer.cid, buffer.name);
                        if(u != null && u.away > 0) {
                            subtitle.setVisibility(View.VISIBLE);
                            if(u.away_msg != null && u.away_msg.length() > 0) {
                                subtitle.setText("Away: " + ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(TextUtils.htmlEncode(u.away_msg))).toString());
                            } else {
                                subtitle.setText("Away");
                            }
                        } else {
    		        		subtitle.setVisibility(View.GONE);
                        }
	    			}
	        		key.setVisibility(View.GONE);
	    		} else if(buffer.type.equals("channel")) {
                    title.setContentDescription("Channel " + buffer.normalizedName() + ". Double-tap to view or edit the topic.");
		        	ChannelsDataSource.Channel c = ChannelsDataSource.getInstance().getChannelForBuffer(buffer.bid);
		        	if(c != null && c.topic_text.length() > 0) {
		        		subtitle.setVisibility(View.VISIBLE);
		        		subtitle.setText(ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(TextUtils.htmlEncode(c.topic_text)), false, null).toString());
                        subtitle.setContentDescription(".");
		        	} else {
		        		subtitle.setVisibility(View.GONE);
		        	}
		        	if(c != null && c.key) {
                        key.setImageResource(R.drawable.lock);
		        		key.setVisibility(View.VISIBLE);
		        	} else {
		        		key.setVisibility(View.GONE);
		        	}
	    		} else if(buffer.type.equals("console")) {
                    subtitle.setVisibility(View.VISIBLE);
                    subtitle.setText(server.hostname + ":" + server.port);
                    title.setContentDescription("Network " + server.name);
                    subtitle.setContentDescription(".");
                    if(server.ssl > 0)
                        key.setImageResource(R.drawable.world_shield);
                    else
                        key.setImageResource(R.drawable.world);
	        		key.setVisibility(View.VISIBLE);
	    		}
	    	}
    	}
    	supportInvalidateOptionsMenu();
    }
    
    private void updateUsersListFragmentVisibility() {
    	boolean hide = true;
		if(userListView != null) {
            ChannelsDataSource.Channel c = null;
            if(buffer != null && buffer.type.equals("channel")) {
                c = ChannelsDataSource.getInstance().getChannelForBuffer(buffer.bid);
                if(c != null)
                    hide = false;
            }
			try {
				if(conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null && getSupportFragmentManager().findFragmentById(R.id.usersListFragment2) != null) {
					JSONObject hiddenMap = conn.getUserInfo().prefs.getJSONObject("channel-hiddenMembers");
					if(hiddenMap.has(String.valueOf(buffer.bid)) && hiddenMap.getBoolean(String.valueOf(buffer.bid)))
						hide = true;
				}
			} catch (Exception e) {
			}
	    	if(hide) {
	    		userListView.setVisibility(View.GONE);
                if(drawerLayout != null) {
                    if(getSupportFragmentManager().findFragmentById(R.id.usersListFragment2) != null && c != null)
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
                    else
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
                }
            } else {
	    		userListView.setVisibility(View.VISIBLE);
                if(drawerLayout != null)
                    if(getSupportFragmentManager().findFragmentById(R.id.usersListFragment2) != null)
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
                    else
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
            }
		}
    }

    public void onIRCEvent(int what, Object obj) {
        super.onIRCEvent(what, obj);
        Integer event_bid = 0;
        final IRCCloudJSONObject event;
        switch (what) {
            case NetworkConnection.EVENT_RENAMECONVERSATION:
                if(buffer != null && (Integer)obj == buffer.bid) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update_subtitle();
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_CHANNELTOPICIS:
                event = (IRCCloudJSONObject)obj;
                if(buffer != null && buffer.cid == event.cid() && buffer.name.equalsIgnoreCase(event.getString("chan"))) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update_subtitle();
                            show_topic_popup();
                        }
                    });
                }
                break;
			case NetworkConnection.EVENT_LINKCHANNEL:
				event = (IRCCloudJSONObject)obj;
				if(event != null && cidToOpen == event.cid() && event.has("invalid_chan") && event.has("valid_chan") && event.getString("invalid_chan").equalsIgnoreCase(bufferToOpen)) {
					bufferToOpen = event.getString("valid_chan");
					obj = BuffersDataSource.getInstance().getBuffer(event.bid());
				} else {
                    bufferToOpen = null;
					return;
				}
			case NetworkConnection.EVENT_MAKEBUFFER:
				BuffersDataSource.Buffer b = (BuffersDataSource.Buffer)obj;
				if(cidToOpen == b.cid && (bufferToOpen == null || (b.name.equalsIgnoreCase(bufferToOpen) && (buffer == null || !bufferToOpen.equalsIgnoreCase(buffer.name))))) {
                    server = null;
                    final int bid = b.bid;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onBufferSelected(bid);
                        }
                    });
		    		bufferToOpen = null;
		    		cidToOpen = -1;
                }
				break;
			case NetworkConnection.EVENT_OPENBUFFER:
				event = (IRCCloudJSONObject)obj;
				try {
					bufferToOpen = event.getString("name");
					cidToOpen = event.cid();
					b = BuffersDataSource.getInstance().getBufferByName(cidToOpen, bufferToOpen);
					if(b != null && !bufferToOpen.equalsIgnoreCase(buffer.name)) {
                        server = null;
                        bufferToOpen = null;
                        cidToOpen = -1;
                        final int bid = b.bid;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onBufferSelected(bid);
                            }
                        });
					}
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				break;
            case NetworkConnection.EVENT_CONNECTIVITY:
				if(conn != null) {
					if(conn.getState() == NetworkConnection.STATE_CONNECTED) {
						for(EventsDataSource.Event e : pendingEvents.values()) {
							EventsDataSource.getInstance().deleteEvent(e.eid, e.bid);
						}
						pendingEvents.clear();
			    		if(drawerLayout != null && NetworkConnection.getInstance().ready) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                                    if(getSupportFragmentManager().findFragmentById(R.id.usersListFragment2) == null)
                                        upView.setVisibility(View.VISIBLE);
                                    updateUsersListFragmentVisibility();
                                }
                            });
			    		}
			    		if(server != null && messageTxt.getText() != null && messageTxt.getText().length() > 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    sendBtn.setEnabled(true);
                                    if(Build.VERSION.SDK_INT >= 11)
                                        sendBtn.setAlpha(1);
                                }
                            });
			    		}
					} else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(drawerLayout != null && !NetworkConnection.getInstance().ready) {
                                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                                    if(getSupportFragmentManager().findFragmentById(R.id.usersListFragment2) == null)
                                        upView.setVisibility(View.INVISIBLE);
                                }
                                sendBtn.setEnabled(false);
                                if(Build.VERSION.SDK_INT >= 11)
                                    sendBtn.setAlpha(0.5f);
                            }
                        });
					}
				}
				break;
			case NetworkConnection.EVENT_BANLIST:
				event = (IRCCloudJSONObject)obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(event != null && event.getString("channel").equalsIgnoreCase(buffer.name)) {
                            Bundle args = new Bundle();
                            args.putInt("cid", buffer.cid);
                            args.putInt("bid", buffer.bid);
                            args.putString("event", event.toString());
                            BanListFragment banList = (BanListFragment)getSupportFragmentManager().findFragmentByTag("banlist");
                            if(banList == null) {
                                banList = new BanListFragment();
                                banList.setArguments(args);
                                try {
                                    banList.show(getSupportFragmentManager(), "banlist");
                                } catch (IllegalStateException e) {
                                    //App lost focus already
                                }
                            } else {
                                banList.setArguments(args);
                            }
                        }
                    }
                });
	            break;
			case NetworkConnection.EVENT_ACCEPTLIST:
				event = (IRCCloudJSONObject)obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(event != null && event.cid() == buffer.cid) {
                            Bundle args = new Bundle();
                            args.putInt("cid", buffer.cid);
                            args.putString("event", event.toString());
                            AcceptListFragment acceptList = (AcceptListFragment)getSupportFragmentManager().findFragmentByTag("acceptlist");
                            if(acceptList == null) {
                                acceptList = new AcceptListFragment();
                                acceptList.setArguments(args);
                                try {
                                    acceptList.show(getSupportFragmentManager(), "acceptlist");
                                } catch (IllegalStateException e) {
                                    //App lost focus already
                                }
                            } else {
                                acceptList.setArguments(args);
                            }
                        }
                    }
                });
	            break;
			case NetworkConnection.EVENT_WHOLIST:
				event = (IRCCloudJSONObject)obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Bundle args = new Bundle();
                        args.putString("event", event.toString());
                        WhoListFragment whoList = (WhoListFragment)getSupportFragmentManager().findFragmentByTag("wholist");
                        if(whoList == null) {
                            whoList = new WhoListFragment();
                            whoList.setArguments(args);
                            try {
                                whoList.show(getSupportFragmentManager(), "wholist");
                            } catch (IllegalStateException e) {
                                //App lost focus already
                            }
                        } else {
                            whoList.setArguments(args);
                        }
                    }
                });
	            break;
            case NetworkConnection.EVENT_NAMESLIST:
                event = (IRCCloudJSONObject)obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Bundle args = new Bundle();
                        args.putString("event", event.toString());
                        NamesListFragment namesList = (NamesListFragment)getSupportFragmentManager().findFragmentByTag("nameslist");
                        if(namesList == null) {
                            namesList = new NamesListFragment();
                            namesList.setArguments(args);
                            try {
                                namesList.show(getSupportFragmentManager(), "nameslist");
                            } catch (IllegalStateException e) {
                                //App lost focus already
                            }
                        } else {
                            namesList.setArguments(args);
                        }
                    }
                });
                break;
			case NetworkConnection.EVENT_WHOIS:
				event = (IRCCloudJSONObject)obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Bundle args = new Bundle();
                        args.putString("event", event.toString());
                        WhoisFragment whois = (WhoisFragment)getSupportFragmentManager().findFragmentByTag("whois");
                        if(whois == null) {
                            whois = new WhoisFragment();
                            whois.setArguments(args);
                            try {
                                whois.show(getSupportFragmentManager(), "whois");
                            } catch (IllegalStateException e) {
                                //App lost focus already
                            }
                        } else {
                            whois.setArguments(args);
                        }
                    }
                });
	            break;
			case NetworkConnection.EVENT_LISTRESPONSEFETCHING:
				event = (IRCCloudJSONObject)obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String dialogtitle = "List of channels on " + ServersDataSource.getInstance().getServer(event.cid()).hostname;
                        if(channelsListDialog == null) {
                            Context ctx = MessageActivity.this;
                            if(Build.VERSION.SDK_INT < 11)
                                ctx = new ContextThemeWrapper(ctx, android.R.style.Theme_Dialog);
                            final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                            builder.setView(getLayoutInflater().inflate(R.layout.dialog_channelslist, null));
                            builder.setTitle(dialogtitle);
                            builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            channelsListDialog = builder.create();
                            channelsListDialog.setOwnerActivity(MessageActivity.this);
                        } else {
                            channelsListDialog.setTitle(dialogtitle);
                        }
                        try {
                            channelsListDialog.show();
                        } catch (IllegalStateException e) {
                            //App lost focus already
                        }
                        ChannelListFragment channels = (ChannelListFragment)getSupportFragmentManager().findFragmentById(R.id.channelListFragment);
                        Bundle args = new Bundle();
                        args.putInt("cid", event.cid());
                        channels.setArguments(args);
                    }
                });
	            break;
			case NetworkConnection.EVENT_USERINFO:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateUsersListFragmentVisibility();
                        supportInvalidateOptionsMenu();
                        if (refreshUpIndicatorTask != null)
                            refreshUpIndicatorTask.cancel(true);
                        refreshUpIndicatorTask = new RefreshUpIndicatorTask();
                        refreshUpIndicatorTask.execute((Void) null);
                    }
                });
		        if(launchBid == -1 && server == null && conn != null && conn.getUserInfo() != null)
		        	launchBid = conn.getUserInfo().last_selected_bid;
				break;
			case NetworkConnection.EVENT_STATUSCHANGED:
				try {
					event = (IRCCloudJSONObject)obj;
					if(event != null && server != null && event.cid() == server.cid) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                supportInvalidateOptionsMenu();
                            }
                        });
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				break;
			case NetworkConnection.EVENT_MAKESERVER:
				ServersDataSource.Server s = (ServersDataSource.Server)obj;
				if(server != null && s != null && s.cid == server.cid) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            supportInvalidateOptionsMenu();
                            update_subtitle();
                        }
                    });
				} else {
                    cidToOpen = s.cid;
                    bufferToOpen = "*";
                }
				break;
			case NetworkConnection.EVENT_BUFFERARCHIVED:
            case NetworkConnection.EVENT_BUFFERUNARCHIVED:
				event_bid = (Integer)obj;
				if(buffer != null && event_bid == buffer.bid) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update_subtitle();
                        }
                    });
				}
                if(refreshUpIndicatorTask != null)
                    refreshUpIndicatorTask.cancel(true);
                refreshUpIndicatorTask = new RefreshUpIndicatorTask();
                refreshUpIndicatorTask.execute((Void)null);
				break;
			case NetworkConnection.EVENT_JOIN:
				event = (IRCCloudJSONObject)obj;
				if(event != null && buffer != null && event.bid() == buffer.bid && event.type().equals("you_joined_channel")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            supportInvalidateOptionsMenu();
                            updateUsersListFragmentVisibility();
                        }
                    });
				}
				break;
			case NetworkConnection.EVENT_PART:
            case NetworkConnection.EVENT_KICK:
				event = (IRCCloudJSONObject)obj;
				if(event != null && buffer != null && event.bid() == buffer.bid && event.type().toLowerCase().startsWith("you_")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            supportInvalidateOptionsMenu();
                            updateUsersListFragmentVisibility();
                        }
                    });
				}
				break;
			case NetworkConnection.EVENT_CHANNELINIT:
				ChannelsDataSource.Channel channel = (ChannelsDataSource.Channel)obj;
				if(channel != null && buffer != null && channel.bid == buffer.bid) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update_subtitle();
                            supportInvalidateOptionsMenu();
                            updateUsersListFragmentVisibility();
                        }
                    });
				}
				break;
            case NetworkConnection.EVENT_BACKLOG_END:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(drawerLayout != null) {
                            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                            if(getSupportFragmentManager().findFragmentById(R.id.usersListFragment2) == null)
                                upView.setVisibility(View.VISIBLE);
                            updateUsersListFragmentVisibility();
                        }
                        if(server == null || launchURI != null || launchBid != -1) {
                            Crashlytics.log(Log.DEBUG, "IRCCloud", "Backlog loaded and we're waiting for a buffer, switching now");
                            if(launchURI == null || !open_uri(launchURI)) {
                                if(launchBid == -1 || !open_bid(launchBid)) {
                                    if(conn == null || conn.getUserInfo() == null || !open_bid(conn.getUserInfo().last_selected_bid)) {
                                        if(!open_bid(BuffersDataSource.getInstance().firstBid())) {
                                            if(drawerLayout != null && NetworkConnection.getInstance().ready) {
                                                drawerLayout.openDrawer(Gravity.LEFT);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        update_subtitle();
                        if(refreshUpIndicatorTask != null)
                            refreshUpIndicatorTask.cancel(true);
                        refreshUpIndicatorTask = new RefreshUpIndicatorTask();
                        refreshUpIndicatorTask.execute((Void)null);
                    }
                });
                //TODO: prune and pop the back stack if the current BID has disappeared
                break;
			case NetworkConnection.EVENT_CONNECTIONDELETED:
			case NetworkConnection.EVENT_DELETEBUFFER:
				Integer id = (Integer)obj;
				if(what==NetworkConnection.EVENT_DELETEBUFFER) {
                    synchronized (backStack) {
                        for(int i = 0; i < backStack.size(); i++) {
                            if(backStack.get(i).equals(id)) {
                                backStack.remove(i);
                                i--;
                            }
                        }
                    }
				}
				if(buffer != null && id == ((what==NetworkConnection.EVENT_CONNECTIONDELETED)?buffer.cid:buffer.bid)) {
                    synchronized (backStack) {
                        while(backStack != null && backStack.size() > 0) {
                            final Integer bid = backStack.get(0);
                            backStack.remove(0);
                            b = BuffersDataSource.getInstance().getBuffer(bid);
                            if(b != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        onBufferSelected(bid);
                                        if(backStack.size() > 0)
                                            backStack.remove(0);
                                    }
                                });
                                return;
                            }
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(BuffersDataSource.getInstance().count() == 0) {
                                startActivity(new Intent(MessageActivity.this, EditConnectionActivity.class));
                                finish();
                            } else {
                                if(!open_bid(BuffersDataSource.getInstance().firstBid()))
                                    finish();
                            }
                        }
                    });
				}
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (refreshUpIndicatorTask != null)
                            refreshUpIndicatorTask.cancel(true);
                        refreshUpIndicatorTask = new RefreshUpIndicatorTask();
                        refreshUpIndicatorTask.execute((Void) null);
                    }
                });
				break;
            case NetworkConnection.EVENT_CHANNELMODE:
                event = (IRCCloudJSONObject)obj;
                if(event != null && buffer != null && event.bid() == buffer.bid) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update_subtitle();
                        }
                    });
                }
                break;
			case NetworkConnection.EVENT_CHANNELTOPIC:
				event = (IRCCloudJSONObject)obj;
                if(event != null && buffer != null && event.bid() == buffer.bid) {
                    final String topic = event.getString("topic");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if(topic.length() > 0) {
                                    subtitle.setVisibility(View.VISIBLE);
                                    subtitle.setText(topic);
                                } else {
                                    subtitle.setVisibility(View.GONE);
                                }
                            } catch (Exception e1) {
                                subtitle.setVisibility(View.GONE);
                                e1.printStackTrace();
                            }
                        }
                    });
				}
				break;
			case NetworkConnection.EVENT_SELFBACK:
		    	try {
					event = (IRCCloudJSONObject)obj;
					if(event != null && buffer != null && event.cid() == buffer.cid && event.getString("nick").equalsIgnoreCase(buffer.name)) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                subtitle.setVisibility(View.GONE);
                                subtitle.setText("");
                            }
                        });
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				break;
			case NetworkConnection.EVENT_AWAY:
		    	try {
					event = (IRCCloudJSONObject)obj;
					if(event != null && buffer != null && event.cid() == buffer.cid && event.getString("nick").equalsIgnoreCase(buffer.name)) {
                        final String away = ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html("Away: " + (event.has("away_msg")?event.getString("away_msg"):event.getString("msg")))).toString();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                subtitle.setVisibility(View.VISIBLE);
                                subtitle.setText(away);
                            }
                        });
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				break;
			case NetworkConnection.EVENT_HEARTBEATECHO:
                boolean shouldRefresh = false;
                event = (IRCCloudJSONObject)obj;
                JsonNode seenEids = event.getJsonNode("seenEids");
                Iterator<Map.Entry<String, JsonNode>> iterator = seenEids.fields();
                while(iterator.hasNext()) {
                    Map.Entry<String, JsonNode> entry = iterator.next();
                    JsonNode eids = entry.getValue();
                    Iterator<Map.Entry<String, JsonNode>> j = eids.fields();
                    while(j.hasNext()) {
                        Map.Entry<String, JsonNode> eidentry = j.next();
                        Integer bid = Integer.valueOf(eidentry.getKey());
                        if(buffer != null && bid != buffer.bid) {
                            shouldRefresh = true;
                        }
                    }
                }
                if(shouldRefresh) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (refreshUpIndicatorTask != null)
                                refreshUpIndicatorTask.cancel(true);
                            refreshUpIndicatorTask = new RefreshUpIndicatorTask();
                            refreshUpIndicatorTask.execute((Void) null);
                        }
                    });
                }
				break;
			case NetworkConnection.EVENT_FAILURE_MSG:
				event = (IRCCloudJSONObject)obj;
				if(event != null && event.has("_reqid")) {
					int reqid = event.getInt("_reqid");
					if(pendingEvents.containsKey(reqid)) {
						EventsDataSource.Event e = pendingEvents.get(reqid);
						EventsDataSource.getInstance().deleteEvent(e.eid, e.bid);
						pendingEvents.remove(event.getInt("_reqid"));
                        e.failed = true;
                        e.bg_color = R.color.error;
						conn.notifyHandlers(NetworkConnection.EVENT_BUFFERMSG, e);
					}
				} else {
                    if(event.getString("message").equalsIgnoreCase("auth")) {
                        conn.logout();
                        Intent i = new Intent(MessageActivity.this, MainActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        finish();
                    }
                    if(event.getString("message").equalsIgnoreCase("set_shard")) {
                        NetworkConnection.getInstance().disconnect();
                        NetworkConnection.getInstance().ready = false;
                        SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
                        editor.putString("session_key", event.getString("cookie"));
                        NetworkConnection.getInstance().connect(event.getString("cookie"));
                        editor.commit();
                    }
                }
				break;
			case NetworkConnection.EVENT_BUFFERMSG:
				try {
					EventsDataSource.Event e = (EventsDataSource.Event)obj;
                    if(e != null && buffer != null) {
                        if(e.bid != buffer.bid && upView != null) {
                            BuffersDataSource.Buffer buf = BuffersDataSource.getInstance().getBuffer(e.bid);
                            if(e.isImportant(buf.type)) {
                                if(!upView.getTag().equals(R.drawable.ic_navigation_drawer_highlight) && (e.highlight || buf.type.equals("conversation"))) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mDrawerListener.setUpDrawable(getResources().getDrawable(R.drawable.ic_navigation_drawer_highlight));
                                            upView.setTag(R.drawable.ic_navigation_drawer_highlight);
                                        }
                                    });
                                } else if(upView.getTag().equals(R.drawable.ic_navigation_drawer)) {
                                    JSONObject channelDisabledMap = null;
                                    JSONObject bufferDisabledMap = null;
                                    if(NetworkConnection.getInstance().getUserInfo() != null && NetworkConnection.getInstance().getUserInfo().prefs != null) {
                                        try {
                                            if(NetworkConnection.getInstance().getUserInfo().prefs.has("channel-disableTrackUnread"))
                                                channelDisabledMap = NetworkConnection.getInstance().getUserInfo().prefs.getJSONObject("channel-disableTrackUnread");
                                            if(NetworkConnection.getInstance().getUserInfo().prefs.has("buffer-disableTrackUnread"))
                                                bufferDisabledMap = NetworkConnection.getInstance().getUserInfo().prefs.getJSONObject("buffer-disableTrackUnread");
                                        } catch (Exception e1) {
                                            // TODO Auto-generated catch block
                                            e1.printStackTrace();
                                        }
                                    }
                                    if(buf.type.equalsIgnoreCase("channel") && channelDisabledMap != null && channelDisabledMap.has(String.valueOf(buf.bid)) && channelDisabledMap.getBoolean(String.valueOf(buf.bid)))
                                        break;
                                    else if(bufferDisabledMap != null && bufferDisabledMap.has(String.valueOf(buf.bid)) && bufferDisabledMap.getBoolean(String.valueOf(buf.bid)))
                                        break;
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mDrawerListener.setUpDrawable(getResources().getDrawable(R.drawable.ic_navigation_drawer_unread));
                                            upView.setTag(R.drawable.ic_navigation_drawer_unread);
                                        }
                                    });
                                }
                            }
                        }
                        if(e.from.equalsIgnoreCase(buffer.name)) {
                            for(EventsDataSource.Event e1 : pendingEvents.values()) {
                                EventsDataSource.getInstance().deleteEvent(e1.eid, e1.bid);
                            }
                            pendingEvents.clear();
                        } else if(pendingEvents.containsKey(e.reqid)) {
                            e = pendingEvents.get(e.reqid);
                            EventsDataSource.getInstance().deleteEvent(e.eid, e.bid);
                            pendingEvents.remove(e.reqid);
                        }
                    }
				} catch (Exception e1) {
				}
				break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	if(buffer != null && buffer.type != null && NetworkConnection.getInstance().ready) {
	    	if(buffer.type.equals("channel")) {
	    		getMenuInflater().inflate(R.menu.activity_message_channel_userlist, menu);
	    		getMenuInflater().inflate(R.menu.activity_message_channel, menu);
	    	} else if(buffer.type.equals("conversation"))
	    		getMenuInflater().inflate(R.menu.activity_message_conversation, menu);
	    	else if(buffer.type.equals("console"))
	    		getMenuInflater().inflate(R.menu.activity_message_console, menu);
	
	    	getMenuInflater().inflate(R.menu.activity_message_archive, menu);
    	}
    	getMenuInflater().inflate(R.menu.activity_main, menu);

        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
    	if(menu != null && buffer != null && buffer.type != null && NetworkConnection.getInstance().ready) {
            if(Build.VERSION.SDK_INT >= 11 && menu.findItem(R.id.menu_photo) != null)
                menu.findItem(R.id.menu_photo).setVisible(false);

        	if(buffer.archived == 0) {
                menu.findItem(R.id.menu_archive).setTitle(R.string.menu_archive);
        	} else {
        		menu.findItem(R.id.menu_archive).setTitle(R.string.menu_unarchive);
        	}
	    	if(buffer.type.equals("channel")) {
	        	if(ChannelsDataSource.getInstance().getChannelForBuffer(buffer.bid) == null) {
	        		menu.findItem(R.id.menu_leave).setTitle(R.string.menu_rejoin);
	        		menu.findItem(R.id.menu_archive).setVisible(true);
	        		menu.findItem(R.id.menu_archive).setEnabled(true);
	        		menu.findItem(R.id.menu_delete).setVisible(true);
	        		menu.findItem(R.id.menu_delete).setEnabled(true);
	        		if(menu.findItem(R.id.menu_userlist) != null) {
	        			menu.findItem(R.id.menu_userlist).setEnabled(false);
	        			menu.findItem(R.id.menu_userlist).setVisible(false);
	        		}
	        		menu.findItem(R.id.menu_ban_list).setVisible(false);
	        		menu.findItem(R.id.menu_ban_list).setEnabled(false);
	        	} else {
	        		menu.findItem(R.id.menu_leave).setTitle(R.string.menu_leave);
	        		menu.findItem(R.id.menu_archive).setVisible(false);
	        		menu.findItem(R.id.menu_archive).setEnabled(false);
	        		menu.findItem(R.id.menu_delete).setVisible(false);
	        		menu.findItem(R.id.menu_delete).setEnabled(false);
	        		menu.findItem(R.id.menu_ban_list).setVisible(true);
	        		menu.findItem(R.id.menu_ban_list).setEnabled(true);
	        		if(menu.findItem(R.id.menu_userlist) != null && findViewById(R.id.usersListFragment2) != null) {
		        		boolean hide = true;
		        		try {
		        			if(conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null) {
								JSONObject hiddenMap = conn.getUserInfo().prefs.getJSONObject("channel-hiddenMembers");
								if(hiddenMap.has(String.valueOf(buffer.bid)) && hiddenMap.getBoolean(String.valueOf(buffer.bid)))
									hide = false;
		        			}
						} catch (JSONException e) {
						}
						if(hide) {
			        		menu.findItem(R.id.menu_userlist).setEnabled(false);
			        		menu.findItem(R.id.menu_userlist).setVisible(false);
						} else {
			        		menu.findItem(R.id.menu_userlist).setEnabled(true);
			        		menu.findItem(R.id.menu_userlist).setVisible(true);
						}
	        		}
	        	}
	    	} else if(buffer.type.equals("console")) {
	    		menu.findItem(R.id.menu_archive).setVisible(false);
	    		menu.findItem(R.id.menu_archive).setEnabled(false);
	    		if(server != null && server.status != null && (server.status.equalsIgnoreCase("waiting_to_retry") || (server.status.contains("connected") && !server.status.startsWith("dis")))) {
                    if(menu.findItem(R.id.menu_disconnect) != null)
    	    			menu.findItem(R.id.menu_disconnect).setTitle(R.string.menu_disconnect);
	        		menu.findItem(R.id.menu_delete).setVisible(false);
	        		menu.findItem(R.id.menu_delete).setEnabled(false);
	    		} else {
                    if(menu.findItem(R.id.menu_disconnect) != null)
    	    			menu.findItem(R.id.menu_disconnect).setTitle(R.string.menu_reconnect);
	        		menu.findItem(R.id.menu_delete).setVisible(true);
	        		menu.findItem(R.id.menu_delete).setEnabled(true);
	    		}
	    	}
    	}
    	return super.onPrepareOptionsMenu(menu);
    }

    private class ToggleListener implements DrawerLayout.DrawerListener  {
        private SlideDrawable mSlider = null;

        @Override
        public void onDrawerSlide(View view, float slideOffset) {
            if(view != null && mSlider != null && ((DrawerLayout.LayoutParams)view.getLayoutParams()).gravity == Gravity.LEFT) {
                float glyphOffset = mSlider.getOffset();
                if (slideOffset > 0.5f) {
                    glyphOffset = Math.max(glyphOffset, Math.max(0.f, slideOffset - 0.5f) * 2);
                } else {
                    glyphOffset = Math.min(glyphOffset, slideOffset * 2);
                }
                mSlider.setOffset(glyphOffset);
            }
        }

        public void setUpDrawable(Drawable d) {
            mSlider = new SlideDrawable(d);
            mSlider.setOffsetBy(1.f / 3);
            upView.setImageDrawable(mSlider);
            if(drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                mSlider.setOffset(1.f);
            } else {
                mSlider.setOffset(0.f);
            }
        }

        @Override
        public void onDrawerOpened(View view) {
            if(((DrawerLayout.LayoutParams)view.getLayoutParams()).gravity == Gravity.LEFT) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
                if(mSlider != null)
                    mSlider.setOffset(1.f);
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
            }
            if(getCurrentFocus() != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        }

        @Override
        public void onDrawerClosed(View view) {
            if(((DrawerLayout.LayoutParams)view.getLayoutParams()).gravity == Gravity.LEFT) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                if(mSlider != null)
                    mSlider.setOffset(0.f);
                updateUsersListFragmentVisibility();
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.LEFT);
            }
        }

        @Override
        public void onDrawerStateChanged(int i) {

        }

        class SlideDrawable extends Drawable implements Drawable.Callback {
            private Drawable mWrapped;
            private float mOffset;
            private float mOffsetBy;

            private final Rect mTmpRect = new Rect();

            public SlideDrawable(Drawable wrapped) {
                mWrapped = wrapped;
            }

            public void setOffset(float offset) {
                mOffset = offset;
                invalidateSelf();
            }

            public float getOffset() {
                return mOffset;
            }

            public void setOffsetBy(float offsetBy) {
                mOffsetBy = offsetBy;
                invalidateSelf();
            }

            @Override
            public void draw(Canvas canvas) {
                mWrapped.copyBounds(mTmpRect);
                canvas.save();
                canvas.translate(mOffsetBy * mTmpRect.width() * -mOffset, 0);
                mWrapped.draw(canvas);
                canvas.restore();
            }

            @Override
            public void setChangingConfigurations(int configs) {
                mWrapped.setChangingConfigurations(configs);
            }

            @Override
            public int getChangingConfigurations() {
                return mWrapped.getChangingConfigurations();
            }

            @Override
            public void setDither(boolean dither) {
                mWrapped.setDither(dither);
            }

            @Override
            public void setFilterBitmap(boolean filter) {
                mWrapped.setFilterBitmap(filter);
            }

            @Override
            public void setAlpha(int alpha) {
                mWrapped.setAlpha(alpha);
            }

            @Override
            public void setColorFilter(ColorFilter cf) {
                mWrapped.setColorFilter(cf);
            }

            @Override
            public void setColorFilter(int color, PorterDuff.Mode mode) {
                mWrapped.setColorFilter(color, mode);
            }

            @Override
            public void clearColorFilter() {
                mWrapped.clearColorFilter();
            }

            @Override
            public boolean isStateful() {
                return mWrapped.isStateful();
            }

            @Override
            public boolean setState(int[] stateSet) {
                return mWrapped.setState(stateSet);
            }

            @Override
            public int[] getState() {
                return mWrapped.getState();
            }

            @Override
            public Drawable getCurrent() {
                return mWrapped.getCurrent();
            }

            @Override
            public boolean setVisible(boolean visible, boolean restart) {
                return super.setVisible(visible, restart);
            }

            @Override
            public int getOpacity() {
                return mWrapped.getOpacity();
            }

            @Override
            public Region getTransparentRegion() {
                return mWrapped.getTransparentRegion();
            }

            @Override
            protected boolean onStateChange(int[] state) {
                mWrapped.setState(state);
                return super.onStateChange(state);
            }

            @Override
            protected void onBoundsChange(Rect bounds) {
                super.onBoundsChange(bounds);
                mWrapped.setBounds(bounds);
            }

            @Override
            public int getIntrinsicWidth() {
                return mWrapped.getIntrinsicWidth();
            }

            @Override
            public int getIntrinsicHeight() {
                return mWrapped.getIntrinsicHeight();
            }

            @Override
            public int getMinimumWidth() {
                return mWrapped.getMinimumWidth();
            }

            @Override
            public int getMinimumHeight() {
                return mWrapped.getMinimumHeight();
            }

            @Override
            public boolean getPadding(Rect padding) {
                return mWrapped.getPadding(padding);
            }

            @Override
            public ConstantState getConstantState() {
                return super.getConstantState();
            }

            @Override
            public void invalidateDrawable(Drawable who) {
                if (who == mWrapped) {
                    invalidateSelf();
                }
            }

            @Override
            public void scheduleDrawable(Drawable who, Runnable what, long when) {
                if (who == mWrapped) {
                    scheduleSelf(what, when);
                }
            }

            @Override
            public void unscheduleDrawable(Drawable who, Runnable what) {
                if (who == mWrapped) {
                    unscheduleSelf(what);
                }
            }
        }
    };

    private ToggleListener mDrawerListener = new ToggleListener();

    private OnClickListener upClickListener = new OnClickListener() {

		@Override
		public void onClick(View arg0) {
        	if(drawerLayout != null) {
	        	if(drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                    drawerLayout.closeDrawers();
	        	} else if(upView.getVisibility() == View.VISIBLE) {
                    if(drawerLayout.isDrawerOpen(Gravity.RIGHT))
                        drawerLayout.closeDrawers();
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.LEFT);
                    drawerLayout.openDrawer(Gravity.LEFT);
	        	}
		    	if(!getSharedPreferences("prefs", 0).getBoolean("bufferSwipeTip", false)) {
		    		Toast.makeText(MessageActivity.this, "Drag from the edge of the screen to quickly open and close channels and conversations list", Toast.LENGTH_LONG).show();
		    		SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
		    		editor.putBoolean("bufferSwipeTip", true);
		    		editor.commit();
		    	}
        	}
		}
    	
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            if (imageCaptureURI != null) {
                new ImgurRefreshTask(imageCaptureURI).execute((Void) null);
            }
        } else if (requestCode == 2 && resultCode == RESULT_OK) {
            Uri selectedImage = imageReturnedIntent.getData();
            if (selectedImage != null) {
                new ImgurRefreshTask(selectedImage).execute((Void) null);
            }
        }
    }

    private void insertPhoto() {
        AlertDialog.Builder builder;
        AlertDialog dialog;
        builder = new AlertDialog.Builder(this);
        builder.setItems(new String[] {"Take a Photo", "Choose Existing"}, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == 0) {
                    try {
                        imageCaptureURI = Uri.fromFile(File.createTempFile("irccloudcapture", ".jpg", Environment.getExternalStorageDirectory()));
                        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageCaptureURI);
                        startActivityForResult(i, 1);
                    } catch (IOException e) {
                    }
                } else {
                    Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(i, 2);
                }
                dialog.dismiss();
            }
        });
        dialog = builder.create();
        dialog.setOwnerActivity(MessageActivity.this);
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	AlertDialog.Builder builder;
    	AlertDialog dialog;
    	
        switch (item.getItemId()) {
            case R.id.menu_photo:
                insertPhoto();
                break;
	        case R.id.menu_whois:
	        	conn.whois(buffer.cid, buffer.name, null);
	        	break;
	        case R.id.menu_identify:
	        	NickservFragment nsFragment = new NickservFragment();
	        	nsFragment.setCid(buffer.cid);
	            nsFragment.show(getSupportFragmentManager(), "nickserv");
	            break;
	        case R.id.menu_add_network:
                addNetwork();
	            break;
	        case R.id.menu_channel_options:
	        	ChannelOptionsFragment newFragment = new ChannelOptionsFragment(buffer.cid, buffer.bid);
	            newFragment.show(getSupportFragmentManager(), "channeloptions");
	        	break;
	        case R.id.menu_buffer_options:
	        	BufferOptionsFragment bufferFragment = new BufferOptionsFragment(buffer.cid, buffer.bid, buffer.type);
	        	bufferFragment.show(getSupportFragmentManager(), "bufferoptions");
	        	break;
            case R.id.menu_userlist:
            	if(drawerLayout != null) {
		        	if(drawerLayout.isDrawerOpen(Gravity.RIGHT)) {
	        			drawerLayout.closeDrawers();
		        	} else {
                        if(getSupportFragmentManager().findFragmentById(R.id.usersListFragment2) == null)
                            drawerLayout.closeDrawer(Gravity.LEFT);
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
                        drawerLayout.openDrawer(Gravity.RIGHT);
		        	}
                    if(getSupportFragmentManager().findFragmentById(R.id.usersListFragment2) == null)
    		        	upView.setVisibility(View.VISIBLE);
			    	if(!getSharedPreferences("prefs", 0).getBoolean("userSwipeTip", false)) {
			    		Toast.makeText(this, "Drag from the edge of the screen to quickly open and close the user list", Toast.LENGTH_LONG).show();
			    		SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
			    		editor.putBoolean("userSwipeTip", true);
			    		editor.commit();
			    	}
            	}
            	return true;
            case R.id.menu_ignore_list:
            	Bundle args = new Bundle();
            	args.putInt("cid", buffer.cid);
	        	IgnoreListFragment ignoreList = new IgnoreListFragment();
	        	ignoreList.setArguments(args);
	            ignoreList.show(getSupportFragmentManager(), "ignorelist");
                return true;
            case R.id.menu_ban_list:
            	conn.mode(buffer.cid, buffer.name, "b");
                return true;
            case R.id.menu_leave:
            	if(ChannelsDataSource.getInstance().getChannelForBuffer(buffer.bid) == null)
            		conn.join(buffer.cid, buffer.name, null);
            	else
            		conn.part(buffer.cid, buffer.name, null);
            	return true;
            case R.id.menu_archive:
            	if(buffer.archived == 0)
            		conn.archiveBuffer(buffer.cid, buffer.bid);
            	else
            		conn.unarchiveBuffer(buffer.cid, buffer.bid);
            	return true;
            case R.id.menu_delete:
            	builder = new AlertDialog.Builder(MessageActivity.this);
            	
            	if(buffer.type.equals("console"))
            		builder.setTitle("Delete Connection");
            	else
            		builder.setTitle("Delete History");
            	
            	if(buffer.type.equalsIgnoreCase("console"))
            		builder.setMessage("Are you sure you want to remove this connection?");
            	else if(buffer.type.equalsIgnoreCase("channel"))
            		builder.setMessage("Are you sure you want to clear your history in " + buffer.name + "?");
            	else
            		builder.setMessage("Are you sure you want to clear your history with " + buffer.name + "?");
            	
            	builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
            	});
            	builder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
		            	if(buffer.type.equals("console")) {
		            		NetworkConnection.getInstance().deleteServer(buffer.cid);
		            	} else {
		                	NetworkConnection.getInstance().deleteBuffer(buffer.cid, buffer.bid);
		            	}
						dialog.dismiss();
					}
            	});
	    		dialog = builder.create();
	    		dialog.setOwnerActivity(MessageActivity.this);
	    		dialog.show();
            	return true;
            case R.id.menu_editconnection:
				if(getWindowManager().getDefaultDisplay().getWidth() < 800) {
					Intent i = new Intent(this, EditConnectionActivity.class);
					i.putExtra("cid", buffer.cid);
					startActivity(i);
				} else {
		        	EditConnectionFragment editFragment = new EditConnectionFragment();
		        	editFragment.setCid(buffer.cid);
		            editFragment.show(getSupportFragmentManager(), "editconnection");
				}
            	return true;
            case R.id.menu_disconnect:
                if(server != null && server.status != null && (server.status.equalsIgnoreCase("waiting_to_retry")) || (server.status.contains("connected") && !server.status.startsWith("dis"))) {
        			conn.disconnect(buffer.cid, null);
        		} else {
        			conn.reconnect(buffer.cid);
        		}
        		return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    void editTopic() {
    	ChannelsDataSource.Channel c = ChannelsDataSource.getInstance().getChannelForBuffer(buffer.bid);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	View view = getDialogTextPrompt();
    	TextView prompt = (TextView)view.findViewById(R.id.prompt);
    	final EditText input = (EditText)view.findViewById(R.id.textInput);
    	input.setText(c.topic_text);
    	prompt.setVisibility(View.GONE);
    	builder.setTitle("Channel Topic");
		builder.setView(view);
		builder.setPositiveButton("Set Topic", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				conn.topic(buffer.cid, buffer.name, input.getText().toString());
				dialog.dismiss();
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog dialog = builder.create();
		dialog.setOwnerActivity(this);
		dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		dialog.show();
    }
    
	@Override
	public void onMessageDoubleClicked(EventsDataSource.Event event) {
		if(event == null)
			return;
		
		String from = event.from;
		if(from == null || from.length() == 0)
			from = event.nick;
		
		onUserDoubleClicked(from);
	}

	@Override
	public void onUserDoubleClicked(String from) {
		if(messageTxt == null || from == null || from.length() == 0)
			return;

    	if(!getSharedPreferences("prefs", 0).getBoolean("mentionTip", false)) {
    		SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
    		editor.putBoolean("mentionTip", true);
    		editor.commit();
    	}
		
		if(drawerLayout != null)
			drawerLayout.closeDrawers();
		
		if(messageTxt.getText().length() == 0) {
			messageTxt.append(from + ": ");
		} else {
			int oldPosition = messageTxt.getSelectionStart();
			String text = messageTxt.getText().toString();
			int start = oldPosition - 1;
			if(start > 0 && text.charAt(start) == ' ')
				start--;
			while(start > 0 && text.charAt(start) != ' ')
				start--;
			int match = text.indexOf(from, start);
			int end = oldPosition + from.length();
			if(end > text.length() - 1)
				end = text.length() - 1;
			if(match >= 0 && match < end) {
				String newtext = "";
				if(match > 1 && text.charAt(match - 1) == ' ')
					newtext = text.substring(0, match - 1);
				else
					newtext = text.substring(0, match);
				if(match+from.length() < text.length() && text.charAt(match+from.length()) == ':' &&
						match+from.length()+1 < text.length() && text.charAt(match+from.length()+1) == ' ') {
					if(match+from.length()+2 < text.length())
						newtext += text.substring(match+from.length()+2, text.length());
				} else if(match+from.length() < text.length()) {
					newtext += text.substring(match+from.length(), text.length());
				}
				if(newtext.endsWith(" "))
					newtext = newtext.substring(0, newtext.length() - 1);
				if(newtext.equals(":"))
					newtext = "";
				messageTxt.setText(newtext);
				if(match < newtext.length())
					messageTxt.setSelection(match);
				else
					messageTxt.setSelection(newtext.length());
			} else {
				if(oldPosition == text.length() - 1) {
					text += " " + from;
				} else {
					String newtext = text.substring(0, oldPosition);
					if(!newtext.endsWith(" "))
						from = " " + from;
					if(!text.substring(oldPosition, text.length()).startsWith(" "))
						from += " ";
					newtext += from;
					newtext += text.substring(oldPosition, text.length());
					if(newtext.endsWith(" "))
						newtext = newtext.substring(0, newtext.length() - 1);
					text = newtext;
				}
				messageTxt.setText(text);
				if(text.length() > 0) {
					if(oldPosition + from.length() + 2 < text.length())
						messageTxt.setSelection(oldPosition + from.length());
					else
						messageTxt.setSelection(text.length());
				}
			}
		}
		messageTxt.requestFocus();
		InputMethodManager keyboard = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        keyboard.showSoftInput(messageTxt, 0);
	}
	
	@Override
	public boolean onBufferLongClicked(final BuffersDataSource.Buffer b) {
   		if(b == null)
			return false;

   		ArrayList<String> itemList = new ArrayList<String>();
   		final String[] items;
		ServersDataSource.Server s = ServersDataSource.getInstance().getServer(b.cid);

		if(buffer == null || b.bid != buffer.bid)
			itemList.add("Open");
		
		if(ChannelsDataSource.getInstance().getChannelForBuffer(b.bid) != null) {
			itemList.add("Leave");
			itemList.add("Display Options…");
		} else {
			if(b.type.equalsIgnoreCase("channel"))
				itemList.add("Join");
			else if(b.type.equalsIgnoreCase("console")) {
				if(s.status.equalsIgnoreCase("waiting_to_retry") || (s.status.contains("connected") && !s.status.startsWith("dis"))) {
					itemList.add("Disconnect");
				} else {
					itemList.add("Connect");
					itemList.add("Delete");
				}
				itemList.add("Edit Connection…");
			}
			if(!b.type.equalsIgnoreCase("console")) {
				if(b.archived == 0)
					itemList.add("Archive");
				else
					itemList.add("Unarchive");
				itemList.add("Delete");
			}
			if(!b.type.equalsIgnoreCase("channel")) {
				itemList.add("Display Options…");
			}
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if(b.type.equalsIgnoreCase("console"))
			builder.setTitle(s.name);
		else
			builder.setTitle(b.name);
		items = itemList.toArray(new String[itemList.size()]);
		builder.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialogInterface, int item) {
	    		AlertDialog.Builder builder;
	    		AlertDialog dialog;

	    		if(items[item].equals("Open")) {
                    onBufferSelected(b.bid);
	    		} else if(items[item].equals("Join")) {
	    			conn.join(b.cid, b.name, null);
	    		} else if(items[item].equals("Leave")) {
	    			conn.part(b.cid, b.name, null);
	    		} else if(items[item].equals("Archive")) {
	    			conn.archiveBuffer(b.cid, b.bid);
	    		} else if(items[item].equals("Unarchive")) {
	    			conn.unarchiveBuffer(b.cid, b.bid);
	    		} else if(items[item].equals("Connect")) {
	    			conn.reconnect(b.cid);
	    		} else if(items[item].equals("Disconnect")) {
	    			conn.disconnect(b.cid, null);
	    		} else if(items[item].equals("Display Options…")) {
	    			if(buffer.type.equals("channel")) {
			        	ChannelOptionsFragment newFragment = new ChannelOptionsFragment(b.cid, b.bid);
			            newFragment.show(getSupportFragmentManager(), "channeloptions");
	    			} else {
			        	BufferOptionsFragment newFragment = new BufferOptionsFragment(b.cid, b.bid, b.type);
			            newFragment.show(getSupportFragmentManager(), "bufferoptions");
	    			}
	    		} else if(items[item].equals("Edit Connection…")) {
                    if(getWindowManager().getDefaultDisplay().getWidth() < 800) {
                        Intent i = new Intent(MessageActivity.this, EditConnectionActivity.class);
                        i.putExtra("cid", b.cid);
                        startActivity(i);
                    } else {
                        EditConnectionFragment editFragment = new EditConnectionFragment();
                        editFragment.setCid(b.cid);
                        editFragment.show(getSupportFragmentManager(), "editconnection");
                    }
	    		} else if(items[item].equals("Delete")) {
	            	builder = new AlertDialog.Builder(MessageActivity.this);
	            	
	            	if(b.type.equalsIgnoreCase("console"))
	            		builder.setTitle("Delete Connection");
	            	else
	            		builder.setTitle("Delete History");
	            	
	            	if(b.type.equalsIgnoreCase("console"))
	            		builder.setMessage("Are you sure you want to remove this connection?");
	            	else if(b.type.equalsIgnoreCase("channel"))
	            		builder.setMessage("Are you sure you want to clear your history in " + b.name + "?");
	            	else
	            		builder.setMessage("Are you sure you want to clear your history with " + b.name + "?");
	            	
	            	builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
	            	});
	            	builder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
			            	if(b.type.equalsIgnoreCase("console")) {
			            		conn.deleteServer(b.cid);
			            	} else {
			                	conn.deleteBuffer(b.cid, b.bid);
			            	}
							dialog.dismiss();
						}
	            	});
		    		dialog = builder.create();
		    		dialog.setOwnerActivity(MessageActivity.this);
		    		dialog.show();
	    		}
		    }
		});
		
		AlertDialog dialog = builder.create();
		dialog.setOwnerActivity(this);
		dialog.show();
		return true;
	}
	
	@Override
	public boolean onMessageLongClicked(EventsDataSource.Event event) {
		String from = event.from;
		if(from == null || from.length() == 0)
			from = event.nick;

		UsersDataSource.User user = UsersDataSource.getInstance().getUser(buffer.bid, from);

		if(user == null && from != null && event.hostmask != null) {
			user = UsersDataSource.getInstance().new User();
			user.nick = from;
			user.hostmask = event.hostmask;
			user.mode = "";
		}
		
		if(user == null && event.html == null)
			return false;
		
		if(event.html != null) {
            String html = event.html;
            if(user != null) {
                if(html.startsWith("<b>")) {
                    String nick = event.html.substring(0, event.html.indexOf("</b>"));
                    if(!nick.contains(user.nick) && event.html.indexOf("</b>", nick.length() + 4) > 0)
                        nick = event.html.substring(0, event.html.indexOf("</b>", nick.length() + 4));
                    if(nick.contains(user.nick + "<")) {
                        html = html.substring(nick.length());
                        nick = "<b>&lt;" + nick.replace("</b> <font", "</b><font").substring(3);
                        html = nick + "&gt;" + html;
                    }
                }
            }
			showUserPopup(user, ColorFormatter.html_to_spanned(event.timestamp + " " + html));
        } else {
			showUserPopup(user, null);
        }
		return true;
    }
    
	@Override
	public void onUserSelected(int c, String chan, String nick) {
		UsersDataSource u = UsersDataSource.getInstance();
        showUserPopup(u.getUser(buffer.bid, nick), null);
	}
	
	@SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
	private void showUserPopup(UsersDataSource.User user, Spanned message) {
		ArrayList<String> itemList = new ArrayList<String>();
   		final String[] items;
   		final Spanned text_to_copy = message;
		selected_user = user;

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		if(message != null)
			itemList.add("Copy Message");
		
		if(selected_user != null) {
			itemList.add("Whois…");
			itemList.add("Send a message");
			itemList.add("Mention");
			itemList.add("Invite to a channel…");
			itemList.add("Ignore");
			if(buffer.type.equalsIgnoreCase("channel")) {
				UsersDataSource.User self_user = UsersDataSource.getInstance().getUser(buffer.bid, server.nick);
				if(self_user != null && self_user.mode != null) {
					if(self_user.mode.contains(server!=null?server.MODE_OWNER:"q") || self_user.mode.contains(server!=null?server.MODE_ADMIN:"a") || self_user.mode.contains(server!=null?server.MODE_OP:"o")) {
						if(selected_user.mode.contains(server!=null?server.MODE_OP:"o"))
							itemList.add("Deop");
						else
							itemList.add("Op");
					}
					if(self_user.mode.contains(server!=null?server.MODE_OWNER:"q") || self_user.mode.contains(server!=null?server.MODE_ADMIN:"a") || self_user.mode.contains(server!=null?server.MODE_OP:"o") || self_user.mode.contains(server!=null?server.MODE_HALFOP:"h")) {
						itemList.add("Kick…");
						itemList.add("Ban…");
					}
				}
			}
            itemList.add("Copy Hostmask");
		}

		items = itemList.toArray(new String[itemList.size()]);
		
		if(selected_user != null)
            if(selected_user.hostmask != null && selected_user.hostmask.length() > 0)
    			builder.setTitle(selected_user.nick + "\n(" + selected_user.hostmask + ")");
            else
                builder.setTitle(selected_user.nick);
		else
			builder.setTitle("Message");
		
		builder.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialogInterface, int item) {
                if(conn == null || buffer == null)
                    return;

	    		AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
	    		View view;
	    		final TextView prompt;
	    		final EditText input;
	    		AlertDialog dialog;

	    		if(items[item].equals("Copy Message")) {
	    			if(Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
						android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
	    			    clipboard.setText(text_to_copy);
	    			} else {
	    			    @SuppressLint("ServiceCast") android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
	    			    android.content.ClipData clip = android.content.ClipData.newPlainText("IRCCloud Message",text_to_copy);
	    			    clipboard.setPrimaryClip(clip);
	    			}
                    Toast.makeText(MessageActivity.this, "Message copied to clipboard", Toast.LENGTH_SHORT).show();
                } else if(items[item].equals("Copy Hostmask")) {
                    if(Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText(selected_user.nick + "!" + selected_user.hostmask);
                    } else {
                        @SuppressLint("ServiceCast") android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Hostmask",selected_user.nick + "!" + selected_user.hostmask);
                        clipboard.setPrimaryClip(clip);
                    }
                    Toast.makeText(MessageActivity.this, "Hostmask copied to clipboard", Toast.LENGTH_SHORT).show();
	    		} else if(items[item].equals("Whois…")) {
	    			conn.whois(buffer.cid, selected_user.nick, null);
	    		} else if(items[item].equals("Send a message")) {
                    conn.say(buffer.cid, null, "/query " + selected_user.nick);
	    		} else if(items[item].equals("Mention")) {
			    	if(!getSharedPreferences("prefs", 0).getBoolean("mentionTip", false)) {
			    		Toast.makeText(MessageActivity.this, "Double-tap a message to quickly reply to the sender", Toast.LENGTH_LONG).show();
			    		SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
			    		editor.putBoolean("mentionTip", true);
			    		editor.commit();
			    	}
	    			onUserDoubleClicked(selected_user.nick);
	    		} else if(items[item].equals("Invite to a channel…")) {
		        	view = getDialogTextPrompt();
		        	prompt = (TextView)view.findViewById(R.id.prompt);
		        	input = (EditText)view.findViewById(R.id.textInput);
                    input.setText("");
		        	prompt.setText("Invite " + selected_user.nick + " to a channel");
		        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
		    		builder.setView(view);
		    		builder.setPositiveButton("Invite", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							conn.invite(buffer.cid, input.getText().toString(), selected_user.nick);
							dialog.dismiss();
						}
		    		});
		    		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
		    		});
		    		dialog = builder.create();
		    		dialog.setOwnerActivity(MessageActivity.this);
		    		dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		    		dialog.show();
	    		} else if(items[item].equals("Ignore")) {
		        	view = getDialogTextPrompt();
		        	prompt = (TextView)view.findViewById(R.id.prompt);
		        	input = (EditText)view.findViewById(R.id.textInput);
		        	input.setText("*!"+selected_user.hostmask);
		        	prompt.setText("Ignore messages for " + selected_user.nick + " at this hostmask");
		        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
		    		builder.setView(view);
		    		builder.setPositiveButton("Ignore", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							conn.ignore(buffer.cid, input.getText().toString());
							dialog.dismiss();
						}
		    		});
		    		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
		    		});
		    		dialog = builder.create();
		    		dialog.setOwnerActivity(MessageActivity.this);
		    		dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		    		dialog.show();
	    		} else if(items[item].equals("Op")) {
	    			conn.mode(buffer.cid, buffer.name, "+" + (server!=null?server.MODE_OP:"o") + " " + selected_user.nick);
	    		} else if(items[item].equals("Deop")) {
                    conn.mode(buffer.cid, buffer.name, "-" + (server!=null?server.MODE_OP:"o") + " " + selected_user.nick);
	    		} else if(items[item].equals("Kick…")) {
		        	view = getDialogTextPrompt();
		        	prompt = (TextView)view.findViewById(R.id.prompt);
		        	input = (EditText)view.findViewById(R.id.textInput);
                    input.setText("");
		        	prompt.setText("Give a reason for kicking");
		        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
		    		builder.setView(view);
		    		builder.setPositiveButton("Kick", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							conn.kick(buffer.cid, buffer.name, selected_user.nick, input.getText().toString());
							dialog.dismiss();
						}
		    		});
		    		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
		    		});
		    		dialog = builder.create();
		    		dialog.setOwnerActivity(MessageActivity.this);
		    		dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		    		dialog.show();
	    		} else if(items[item].equals("Ban…")) {
		        	view = getDialogTextPrompt();
		        	prompt = (TextView)view.findViewById(R.id.prompt);
		        	input = (EditText)view.findViewById(R.id.textInput);
		        	input.setText("*!"+selected_user.hostmask);
		        	prompt.setText("Add a banmask for " + selected_user.nick);
		        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
		    		builder.setView(view);
		    		builder.setPositiveButton("Ban", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							conn.mode(buffer.cid, buffer.name, "+b " + input.getText().toString());
							dialog.dismiss();
						}
		    		});
		    		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
		    		});
		    		dialog = builder.create();
		    		dialog.setOwnerActivity(MessageActivity.this);
		    		dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		    		dialog.show();
	    		}
		    	dialogInterface.dismiss();
		    }
		});
		
		AlertDialog dialog = builder.create();
		dialog.setOwnerActivity(this);
		dialog.show();
    }

	@Override
	public void onBufferSelected(int bid) {
        if(suggestionsTimerTask != null)
            suggestionsTimerTask.cancel();
        sortedChannels = null;
        sortedUsers = null;

		if(drawerLayout != null) {
            drawerLayout.closeDrawers();
            if(getSupportFragmentManager().findFragmentById(R.id.usersListFragment2) == null)
                upView.setVisibility(View.VISIBLE);
		}
        if(bid != -1 && conn != null && conn.getUserInfo() != null) {
            conn.getUserInfo().last_selected_bid = bid;
        }
        for(int i = 0; i < backStack.size(); i++) {
            if(buffer != null && backStack.get(i) == buffer.bid)
                backStack.remove(i);
        }
        if(buffer != null && buffer.bid >= 0 && bid != buffer.bid) {
            backStack.add(0, buffer.bid);
            buffer.draft = messageTxt.getText().toString();
        }
        if(buffer == null || buffer.bid == -1 || buffer.cid == -1 || buffer.bid == bid)
            shouldFadeIn = false;
        else
            shouldFadeIn = true;
        buffer = BuffersDataSource.getInstance().getBuffer(bid);
        if(buffer != null) {
            Crashlytics.log(Log.DEBUG, "IRCCloud", "Buffer selected: cid" + buffer.cid + " bid" + bid + " shouldFadeIn: " + shouldFadeIn);
            server = ServersDataSource.getInstance().getServer(buffer.cid);

            try {
                TreeMap<Long, EventsDataSource.Event> events = EventsDataSource.getInstance().getEventsForBuffer(buffer.bid);
                if (events != null) {
                    events = (TreeMap<Long, EventsDataSource.Event>) events.clone();
                    for (EventsDataSource.Event e : events.values()) {
                        if (e.highlight && e.from != null) {
                            UsersDataSource.User u = UsersDataSource.getInstance().getUser(buffer.bid, e.from);
                            if (u != null && u.last_mention < e.eid)
                                u.last_mention = e.eid;
                        }
                    }
                }
            } catch (Exception e) {
                Crashlytics.logException(e);
            }
        } else {
            Crashlytics.log(Log.DEBUG, "IRCCloud", "Buffer selected but not found: bid" + bid + " shouldFadeIn: " + shouldFadeIn);
            server = null;
        }
        update_subtitle();
        final Bundle b = new Bundle();
        if(buffer != null)
            b.putInt("cid", buffer.cid);
        b.putInt("bid", bid);
        b.putBoolean("fade", shouldFadeIn);
        BuffersListFragment blf = (BuffersListFragment)getSupportFragmentManager().findFragmentById(R.id.BuffersList);
        final MessageViewFragment mvf = (MessageViewFragment)getSupportFragmentManager().findFragmentById(R.id.messageViewFragment);
        UsersListFragment ulf = (UsersListFragment)getSupportFragmentManager().findFragmentById(R.id.usersListFragment);
        UsersListFragment ulf2 = (UsersListFragment)getSupportFragmentManager().findFragmentById(R.id.usersListFragment2);
        if(mvf != null)
            mvf.ready = false;
        if(blf != null)
            blf.setSelectedBid(bid);
        if(ulf != null)
            ulf.setArguments(b);
        if(ulf2 != null)
            ulf2.setArguments(b);

        if(shouldFadeIn) {
            Crashlytics.log(Log.DEBUG, "IRCCloud", "Fade Out");
            AlphaAnimation anim = new AlphaAnimation(1, 0);
            anim.setDuration(150);
            anim.setFillAfter(true);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if(mvf != null)
                        mvf.setArguments(b);
                    messageTxt.setText("");
                    if(buffer != null && buffer.draft != null)
                        messageTxt.append(buffer.draft);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            try {
                mvf.showSpinner(true);
                mvf.getListView().startAnimation(anim);
                ulf.getListView().startAnimation(anim);
            } catch (Exception e) {

            }
        } else {
            if(mvf != null)
                mvf.setArguments(b);
            messageTxt.setText("");
            if(buffer != null && buffer.draft != null)
                messageTxt.append(buffer.draft);
        }

        updateUsersListFragmentVisibility();
        supportInvalidateOptionsMenu();
        if(showNotificationsTask != null)
            showNotificationsTask.cancel(true);
        showNotificationsTask = new ShowNotificationsTask();
        showNotificationsTask.execute(bid);
        if(upView != null)
            new RefreshUpIndicatorTask().execute((Void)null);
		if(buffer != null && buffer.cid != -1) {
			if(drawerLayout != null)
				drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.LEFT);
		}
        update_suggestions(false);
	}

	@Override
	public void onMessageViewReady() {
		if(shouldFadeIn) {
            Crashlytics.log(Log.DEBUG, "IRCCloud", "Fade In");
            MessageViewFragment mvf = (MessageViewFragment)getSupportFragmentManager().findFragmentById(R.id.messageViewFragment);
	    	UsersListFragment ulf = (UsersListFragment)getSupportFragmentManager().findFragmentById(R.id.usersListFragment);
	    	AlphaAnimation anim = new AlphaAnimation(0, 1);
			anim.setDuration(150);
			anim.setFillAfter(true);
			if(mvf != null && mvf.getListView() != null) {
                if(mvf.buffer != buffer && buffer != null) {
                    Bundle b = new Bundle();
                    b.putInt("cid", buffer.cid);
                    b.putInt("bid", buffer.bid);
                    b.putBoolean("fade", false);
                    mvf.setArguments(b);
                }
                mvf.showSpinner(false);
				mvf.getListView().startAnimation(anim);
            }
			if(ulf != null && ulf.getListView() != null)
				ulf.getListView().startAnimation(anim);
			shouldFadeIn = false;
        }
	}

	@Override
	public void addButtonPressed(int cid) {
        if(drawerLayout != null) {
            drawerLayout.closeDrawers();
            if(getSupportFragmentManager().findFragmentById(R.id.usersListFragment2) == null)
                upView.setVisibility(View.VISIBLE);
        }
	}

    @Override
    public void addNetwork() {
        if(drawerLayout != null) {
            drawerLayout.closeDrawers();
            if(getSupportFragmentManager().findFragmentById(R.id.usersListFragment2) == null)
                upView.setVisibility(View.VISIBLE);
        }
        if(getWindowManager().getDefaultDisplay().getWidth() < 800) {
            Intent i = new Intent(this, EditConnectionActivity.class);
            startActivity(i);
        } else {
            EditConnectionFragment connFragment = new EditConnectionFragment();
            connFragment.show(getSupportFragmentManager(), "addnetwork");
        }
    }

    @Override
    public void reorder() {
        if(drawerLayout != null) {
            drawerLayout.closeDrawers();
            if(getSupportFragmentManager().findFragmentById(R.id.usersListFragment2) == null)
                upView.setVisibility(View.VISIBLE);
        }
        if(getWindowManager().getDefaultDisplay().getWidth() < 800) {
            Intent i = new Intent(this, ServerReorderActivity.class);
            startActivity(i);
        } else {
            try {
                ServerReorderFragment fragment = new ServerReorderFragment();
                fragment.show(getSupportFragmentManager(), "reorder");
            } catch (IllegalStateException e) {
                Intent i = new Intent(this, ServerReorderActivity.class);
                startActivity(i);
            }
        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, 9000).show();
            }
            return false;
        }
        return true;
    }

    public class ImgurRefreshTask extends AsyncTaskEx<Void, Void, JSONObject> {
        private final String REFRESH_URL = "https://api.imgur.com/oauth2/token";
        private Uri mImageUri;  // local Uri to upload

        public ImgurRefreshTask(Uri imageUri) {
            mImageUri = imageUri;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                JSONObject o = NetworkConnection.getInstance().fetchJSON(REFRESH_URL,
                        "client_id="+BuildConfig.IMGUR_KEY
                        +"&client_secret="+BuildConfig.IMGUR_SECRET
                        +"&grant_type=refresh_token"
                        +"&refresh_token=" + getSharedPreferences("prefs", 0).getString("imgur_refresh_token", "")
                );
                return o;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject o) {
            try {
                if(o == null || (o.has("success") && !o.getBoolean("success"))) {
                    startActivity(new Intent(MessageActivity.this, ImgurAuthActivity.class));
                } else {
                    SharedPreferences.Editor prefs = getSharedPreferences("prefs", 0).edit();
                    Iterator<String> i = o.keys();
                    while(i.hasNext()) {
                        String k = i.next();
                        prefs.putString("imgur_" + k, o.getString(k));
                    }
                    prefs.commit();
                    if(mImageUri != null) {
                        imgurTask = new ImgurUploadTask(mImageUri);
                        imgurTask.execute((Void) null);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    public class ImgurUploadTask extends AsyncTaskEx<Void, Float, String> {
        private final String UPLOAD_URL = "https://api.imgur.com/3/image";
        private Uri mImageUri;  // local Uri to upload
        private int total = 0;
        public Activity activity;
        private View connecting;
        private TextView connectingMsg;
        private ProgressBar progressBar;
        private String error;
        private BuffersDataSource.Buffer mBuffer;

        public ImgurUploadTask(Uri imageUri) {
            mImageUri = imageUri;
            mBuffer = buffer;
            setActivity(MessageActivity.this);
        }

        @Override
        protected String doInBackground(Void... params) {
            InputStream imageIn;
            try {
                while(activity == null)
                    Thread.sleep(100);
                imageIn = activity.getContentResolver().openInputStream(mImageUri);
                total = imageIn.available();
            } catch (Exception e) {
                Log.e("IRCCloud", "could not open InputStream", e);
                return null;
            }

            HttpURLConnection conn = null;
            InputStream responseIn = null;

            try {
                conn = (HttpURLConnection) new URL(UPLOAD_URL).openConnection();
                conn.setDoOutput(true);
                if(getSharedPreferences("prefs", 0).contains("imgur_access_token")) {
                    conn.setRequestProperty("Authorization", "Bearer " + getSharedPreferences("prefs", 0).getString("imgur_access_token", ""));
                } else {
                    conn.setRequestProperty("Authorization", "Client-ID " + BuildConfig.IMGUR_KEY);
                }

                OutputStream out = conn.getOutputStream();
                copy(imageIn, out);
                out.flush();
                out.close();
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    responseIn = conn.getInputStream();
                    return onInput(responseIn);
                }
                else {
                    Log.i("IRCCloud", "responseCode=" + conn.getResponseCode());
                    responseIn = conn.getErrorStream();
                    StringBuilder sb = new StringBuilder();
                    Scanner scanner = new Scanner(responseIn);
                    while (scanner.hasNext()) {
                        sb.append(scanner.next());
                    }
                    error = sb.toString();
                    Log.i("IRCCloud", "error response: " + sb.toString());
                    return null;
                }
            } catch (Exception ex) {
                Log.e("IRCCloud", "Error during POST", ex);
                return null;
            } finally {
                try {
                    responseIn.close();
                } catch (Exception ignore) {}
                try {
                    conn.disconnect();
                } catch (Exception ignore) {}
                try {
                    imageIn.close();
                } catch (Exception ignore) {}
            }
        }

        public void setActivity(Activity a) {
            activity = a;
            if(a != null) {
                connecting = a.findViewById(R.id.connecting);
                connectingMsg = (TextView)a.findViewById(R.id.connectingMsg);
                progressBar = (ProgressBar)a.findViewById(R.id.connectingProgress);
                if(total > 0) {
                    if (connecting.getVisibility() == View.GONE) {
                        TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1, Animation.RELATIVE_TO_SELF, 0);
                        anim.setDuration(200);
                        anim.setFillAfter(true);
                        connecting.startAnimation(anim);
                        connecting.setVisibility(View.VISIBLE);
                        connectingMsg.setText("Uploading");
                        progressBar.setProgress(0);
                        progressBar.setIndeterminate(true);
                    }
                }
            }
        }

        @Override
        protected void onProgressUpdate(Float... values) {
            if(activity != null) {
                try {
                    if (connecting.getVisibility() == View.GONE) {
                        TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1, Animation.RELATIVE_TO_SELF, 0);
                        anim.setDuration(200);
                        anim.setFillAfter(true);
                        connecting.startAnimation(anim);
                        connecting.setVisibility(View.VISIBLE);
                        connectingMsg.setText("Uploading");
                        progressBar.setProgress(0);
                        progressBar.setIndeterminate(true);
                    }
                    if (values[0] < 1.0f) {
                        progressBar.setIndeterminate(false);
                        progressBar.setProgress((int) (values[0] * 1000));
                    } else {
                        progressBar.setIndeterminate(true);
                    }
                } catch (Exception e) {
                }
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if(mImageUri.toString().contains("irccloudcapture") && s != null && s.length() > 0) {
                try {
                    new File(new URI(mImageUri.toString())).delete();
                } catch (Exception e) {
                }
            }
            if(activity != null) {
                TranslateAnimation anim = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, -1);
                anim.setDuration(200);
                anim.setFillAfter(true);
                anim.setAnimationListener(new Animation.AnimationListener() {

                    @Override
                    public void onAnimationEnd(Animation arg0) {
                        connecting.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                });
                connecting.startAnimation(anim);
            }
            setText(s);
        }

        private void setText(final String s) {
            //If the user rotated the screen, we might not be attached to an activity yet.  Keep trying until we reattach
            if(s == null) {
                try {
                    JSONObject root = new JSONObject(error);
                    if (root.has("status") && root.getInt("status") == 403) {
                        new ImgurRefreshTask(mImageUri).execute((Void) null);
                        return;
                    }
                } catch (JSONException e) {
                }
            }
            if(activity != null) {
                Log.e("IRCCloud", "Upload finished");
                if (s != null) {
                    if(mBuffer != null) {
                        if (mBuffer.draft == null)
                            mBuffer.draft = "";
                        if (mBuffer.draft.length() > 0 && !mBuffer.draft.endsWith(" "))
                            mBuffer.draft += " ";
                        mBuffer.draft += s;
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ActionEditText messageTxt = (ActionEditText) activity.findViewById(R.id.messageTxt);
                            String txt = messageTxt.getText().toString();
                            if (txt.length() > 0 && !txt.endsWith(" "))
                                txt += " ";
                            txt += s.replace("http://", "https://");
                            messageTxt.setText(txt);
                        }
                    });
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle("Upload Failed");
                    builder.setMessage("Unable to upload photo to imgur.  Please try again. " + ((error != null) ? error : ""));
                    builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.setOwnerActivity(activity);
                    dialog.show();
                }
                imgurTask = null;
            } else if(mBuffer != null && s != null) {
                Log.e("IRCCloud", "Upload finished, updating draft");
                if (mBuffer.draft == null)
                    mBuffer.draft = "";
                if (mBuffer.draft.length() > 0 && !mBuffer.draft.endsWith(" "))
                    mBuffer.draft += " ";
                mBuffer.draft += s;
            } else {
                Log.e("IRCCloud", "Upload finished, waiting for new activity reference");
                suggestionsTimer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        setText(s);
                    }
                }, 500);
            }
        }

        private int copy(InputStream input, OutputStream output) throws IOException {
            byte[] buffer = new byte[8192];
            int count = 0;
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
                publishProgress((float)count / (float)total);
            }
            return count;
        }

        protected String onInput(InputStream in) throws Exception {
            StringBuilder sb = new StringBuilder();
            Scanner scanner = new Scanner(in);
            while (scanner.hasNext()) {
                sb.append(scanner.next());
            }

            Log.d("IRCCloud", sb.toString());

            JSONObject root = new JSONObject(sb.toString());
            error = sb.toString();
            total = 0;
            return root.getJSONObject("data").getString("link");
        }
    }
}
