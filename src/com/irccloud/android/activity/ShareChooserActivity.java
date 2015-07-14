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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.fragment.BuffersListFragment;

import java.util.Timer;
import java.util.TimerTask;

public class ShareChooserActivity extends FragmentActivity implements NetworkConnection.IRCEventHandler, BuffersListFragment.OnBufferSelectedListener {
    private TextView errorMsg = null;
    private TextView connectingMsg = null;
    private ProgressBar progressBar = null;
    private static Timer countdownTimer = null;
    private TimerTask countdownTimerTask = null;
    private String error = null;
    private View connecting = null;
    private View buffersList = null;
    private NetworkConnection conn = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        countdownTimer = new Timer("share-chooser-countdown-timer");
        if (Build.VERSION.SDK_INT >= 21) {
            Bitmap cloud = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.app_name), cloud, 0xFFF2F7FC));
            cloud.recycle();
        }
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_share_chooser);
        connecting = findViewById(R.id.connecting);
        errorMsg = (TextView) findViewById(R.id.errorMsg);
        connectingMsg = (TextView) findViewById(R.id.connectingMsg);
        progressBar = (ProgressBar) findViewById(R.id.connectingProgress);

        BuffersListFragment f = (BuffersListFragment) getSupportFragmentManager().findFragmentById(R.id.BuffersList);
        f.readOnly = true;
        buffersList = f.getView();
        buffersList.setVisibility(View.GONE);

        Typeface LatoRegular = Typeface.createFromAsset(getAssets(), "Lato-Regular.ttf");

        LinearLayout IRCCloud = (LinearLayout) findViewById(R.id.IRCCloud);
        for (int i = 0; i < IRCCloud.getChildCount(); i++) {
            View v = IRCCloud.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setTypeface(LatoRegular);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String session = getSharedPreferences("prefs", 0).getString("session_key", "");
        if (session != null && session.length() > 0) {
            conn = NetworkConnection.getInstance();
            conn.addHandler(this);
            if (conn.getState() == NetworkConnection.STATE_DISCONNECTED || conn.getState() == NetworkConnection.STATE_DISCONNECTING) {
                conn.connect(session);
            } else {
                connecting.setVisibility(View.GONE);
                buffersList.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(this, "You must login to the IRCCloud app before sharing", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (conn != null)
            conn.removeHandler(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
    }

    private void updateReconnecting() {
        if (conn.getState() == NetworkConnection.STATE_CONNECTED) {
            connectingMsg.setText("Loading");
        } else if (conn.getState() == NetworkConnection.STATE_CONNECTING || conn.getReconnectTimestamp() > 0) {
            progressBar.setIndeterminate(true);
            if (conn.getState() == NetworkConnection.STATE_DISCONNECTED && conn.getReconnectTimestamp() > 0) {
                String plural = "";
                int seconds = (int) ((conn.getReconnectTimestamp() - System.currentTimeMillis()) / 1000);
                if (seconds != 1)
                    plural = "s";
                if (seconds < 1) {
                    connectingMsg.setText("Connecting");
                    errorMsg.setVisibility(View.GONE);
                } else {
                    connectingMsg.setText("Reconnecting in " + seconds + " second" + plural);
                    if (error != null) {
                        errorMsg.setText(error);
                        errorMsg.setVisibility(View.VISIBLE);
                    }
                }
                if(countdownTimer != null) {
                    if (countdownTimerTask != null)
                        countdownTimerTask.cancel();
                    countdownTimerTask = new TimerTask() {
                        public void run() {
                            if (conn.getState() == NetworkConnection.STATE_DISCONNECTED) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateReconnecting();
                                    }
                                });
                            }
                        }
                    };
                    countdownTimer.schedule(countdownTimerTask, 1000);
                }
            } else {
                connectingMsg.setText("Connecting");
                error = null;
                errorMsg.setVisibility(View.GONE);
            }
        } else {
            connectingMsg.setText("Offline");
            progressBar.setIndeterminate(false);
            progressBar.setProgress(0);
        }
    }

    @Override
    public void onIRCEvent(int what, final Object obj) {
        switch (what) {
            case NetworkConnection.EVENT_DEBUG:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        errorMsg.setVisibility(View.VISIBLE);
                        errorMsg.setText(obj.toString());
                    }
                });
                break;
            case NetworkConnection.EVENT_PROGRESS:
                final float progress = (Float) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressBar.getProgress() < progress) {
                            progressBar.setIndeterminate(false);
                            progressBar.setProgress((int) progress);
                        }
                    }
                });
                break;
            case NetworkConnection.EVENT_BACKLOG_START:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(0);
                    }
                });
                break;
            case NetworkConnection.EVENT_BACKLOG_END:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connecting.setVisibility(View.GONE);
                        buffersList.setVisibility(View.VISIBLE);
                    }
                });
                break;
            case NetworkConnection.EVENT_CONNECTIVITY:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateReconnecting();
                    }
                });
                break;
        }
    }

    @Override
    public void onIRCRequestSucceeded(int reqid, IRCCloudJSONObject object) {

    }

    @Override
    public void onIRCRequestFailed(int reqid, final IRCCloudJSONObject o) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    error = o.getString("message");
                    if (error.equals("auth")) {
                        conn.logout();
                        finish();
                        return;
                    }

                    if (error.equals("set_shard")) {
                        conn.disconnect();
                        conn.ready = false;
                        SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
                        editor.putString("session_key", o.getString("cookie"));
                        conn.connect(o.getString("cookie"));
                        editor.commit();
                        return;
                    }

                    if (error.equals("temp_unavailable"))
                        error = "Your account is temporarily unavailable";
                    updateReconnecting();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onBufferSelected(int bid) {
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        i.putExtra("bid", bid);
        if (getIntent() != null && getIntent().getData() != null)
            i.setData(getIntent().getData());
        if (getIntent() != null && getIntent().getExtras() != null) {
            for(String key : getIntent().getExtras().keySet()) {
                try {
                    if(key.equals(Intent.EXTRA_STREAM))
                        i.putExtra(key, getIntent().getParcelableExtra(key));
                    else
                        i.putExtra(key, getIntent().getStringExtra(key));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        startActivity(i);
        finish();
    }

    @Override
    public boolean onBufferLongClicked(Buffer b) {
        return false;
    }

    @Override
    public void addButtonPressed(int cid) {

    }

    @Override
    public void addNetwork() {

    }

    @Override
    public void reorder() {

    }
}
