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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.material.snackbar.Snackbar;
import com.irccloud.android.BuildConfig;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.IRCCloudLog;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.collection.AvatarsList;
import com.irccloud.android.data.collection.EventsList;
import com.irccloud.android.data.collection.ImageList;
import com.irccloud.android.data.collection.LogExportsList;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.data.model.LogExport;
import com.irccloud.android.data.model.Server;
import com.samsung.android.sdk.multiwindow.SMultiWindow;
import com.samsung.android.sdk.multiwindow.SMultiWindowActivity;

import java.io.File;

public class BaseActivity extends AppCompatActivity implements NetworkConnection.IRCEventHandler {
    NetworkConnection conn;
    private View dialogTextPrompt;

    private SMultiWindow mMultiWindow = null;
    private SMultiWindowActivity mMultiWindowActivity = null;

    private BroadcastReceiver powerSaverListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!ColorScheme.getInstance().theme.equals(ColorScheme.getUserTheme()))
                recreate();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean themeChanged = false;
        String theme = PreferenceManager.getDefaultSharedPreferences(this).getString("theme", ColorScheme.defaultTheme());
        if(theme.equals("auto"))
            AppCompatDelegate.setDefaultNightMode((Build.VERSION.SDK_INT < 29) ? AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY : AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        else if(theme.equals("dawn"))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        theme = ColorScheme.getUserTheme();
        if(ColorScheme.getInstance().theme == null || !ColorScheme.getInstance().theme.equals(theme)) {
            themeChanged = true;
        }
        setTheme(ColorScheme.getTheme(theme, true));
        ColorScheme.getInstance().setThemeFromContext(this, theme);
        if(themeChanged) {
            EventsList.getInstance().clearCaches();
            AvatarsList.getInstance().clear();
        }
        Bitmap cloud = BitmapFactory.decodeResource(getResources(), R.drawable.splash_logo);
        if(cloud != null) {
            setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.app_name), cloud, ColorScheme.getInstance().navBarColor));
        }
        getWindow().setStatusBarColor(ColorScheme.getInstance().statusBarColor);
        getWindow().setNavigationBarColor(getResources().getColor(android.R.color.black));
        if(ColorScheme.getInstance().windowBackgroundDrawable != 0)
            getWindow().setBackgroundDrawableResource(ColorScheme.getInstance().windowBackgroundDrawable);

        if(theme.equals("dawn"))
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        else
            getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() &~ View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        conn = NetworkConnection.getInstance();
        conn.addHandler(this);
        if(ServersList.getInstance().count() == 0)
            NetworkConnection.getInstance().load();

        try {
            mMultiWindow = new SMultiWindow();
            mMultiWindow.initialize(this);
            mMultiWindowActivity = new SMultiWindowActivity(this);
        } catch (Exception e) {
            mMultiWindow = null;
            mMultiWindowActivity = null;
        } catch (Error e) {
            mMultiWindow = null;
            mMultiWindowActivity = null;
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        registerReceiver(powerSaverListener, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (conn != null) {
            conn.removeHandler(this);
        }
        unregisterReceiver(powerSaverListener);
    }

    public boolean isMultiWindow() {
        return isInMultiWindowMode();
    }

    public View getDialogTextPrompt() {
        if (dialogTextPrompt == null)
            dialogTextPrompt = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_textprompt, null);
        if (dialogTextPrompt.getParent() != null)
            ((ViewGroup) dialogTextPrompt.getParent()).removeView(dialogTextPrompt);
        return dialogTextPrompt;
    }

    protected boolean finished = false;

    @Override
    protected void onPause() {
        super.onPause();
        IRCCloudApplication.getInstance().onPause(this);
        if(isFinishing())
            finished = true;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        finished = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        IRCCloudApplication.getInstance().onResume(this);
        finished = false;
        File f = new File(getFilesDir(), "log.txt");
        if(f.exists()) {
            android.util.Log.d("IRCCloud", "Removing stale log file");
            f.delete();
        }
        new ImageListPruneTask().execute((Void)null);
        if ((NetworkConnection.getInstance().session != null && NetworkConnection.getInstance().session.length() > 0) || BuildConfig.MOCK_DATA) {
            if(conn.notifier) {
                IRCCloudLog.Log(Log.INFO, "IRCCloud", "Upgrading notifier websocket");
                conn.upgrade();
            }
        } else {
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(conn != null) {
            IRCCloudLog.Log(Log.INFO, "IRCCloud", "App resumed, websocket state: " + conn.getState());
            if(conn.getState() == NetworkConnection.STATE_CONNECTING)
                conn.disconnect();

            if(conn.getState() == NetworkConnection.STATE_DISCONNECTED || conn.getState() == NetworkConnection.STATE_DISCONNECTING)
                conn.connect(true);
        }
    }

    public void onIRCEvent(int what, Object obj) {
        String message = "";
        final IRCCloudJSONObject o;

        switch (what) {
            case NetworkConnection.EVENT_LOGEXPORTFINISHED:
                o = (IRCCloudJSONObject) obj;
                JsonNode export = o.getJsonNode("export");
                LogExport e = LogExportsList.getInstance().get(export.get("id").asInt());
                if(e != null) {
                    e.setFile_name(export.get("file_name").asText());
                    e.setRedirect_url(export.get("redirect_url").asText());
                    e.setFinish_date(export.get("finishdate").asLong());
                    e.setExpiry_date(export.get("expirydate").asLong());
                } else {
                    e = LogExportsList.getInstance().create(export);
                }
                IRCCloudDatabase.getInstance().LogExportsDao().insert(e);
                final LogExport e1 = e;
                Snackbar.make(findViewById(android.R.id.content), "Logs for " + e.getName() + " are now available", Snackbar.LENGTH_LONG)
                        .setAction("Download", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                e1.download();
                            }
                        })
                        .show();
                break;
            case NetworkConnection.EVENT_BADCHANNELKEY:
                o = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Server server = ServersList.getInstance().getServer(o.cid());
                        AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
                        View view = getDialogTextPrompt();
                        TextView prompt = view.findViewById(R.id.prompt);
                        final EditText keyinput = view.findViewById(R.id.textInput);
                        keyinput.setText("");
                        keyinput.setOnEditorActionListener(new OnEditorActionListener() {
                            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
                                    try {
                                        if (keyinput.getText() != null)
                                            conn.join(o.cid(), o.getString("chan"), keyinput.getText().toString(), null);
                                    } catch (Exception e) {
                                        // TODO Auto-generated catch block
                                        NetworkConnection.printStackTraceToCrashlytics(e);
                                    }
                                    ((AlertDialog) keyinput.getTag()).dismiss();
                                }
                                return true;
                            }
                        });
                        try {
                            prompt.setText("Password for " + o.getString("chan"));
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            NetworkConnection.printStackTraceToCrashlytics(e);
                        }
                        builder.setTitle(server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")");
                        builder.setView(view);
                        builder.setPositiveButton("Join", new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    conn.join(o.cid(), o.getString("chan"), keyinput.getText().toString(), null);
                                } catch (Exception e) {
                                    // TODO Auto-generated catch block
                                    NetworkConnection.printStackTraceToCrashlytics(e);
                                }
                                dialog.dismiss();
                            }
                        });
                        builder.setNegativeButton("Cancel", new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        AlertDialog dialog = builder.create();
                        keyinput.setTag(dialog);
                        dialog.setOwnerActivity(BaseActivity.this);
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        if(!isFinishing())
                            dialog.show();
                    }
                });
                break;
            case NetworkConnection.EVENT_ALERT:
                try {
                    o = (IRCCloudJSONObject) obj;

                    switch(o.type()) {
                        case "help":
                            break;
                        case "stats":
                            break;
                        case "invite_only_chan":
                            showAlert(o.cid(), "You need an invitation to join " + o.getString("chan"));
                            break;
                        case "channel_full":
                            showAlert(o.cid(), o.getString("chan") + " isn't allowing any more members to join.");
                            break;
                        case "banned_from_channel":
                            showAlert(o.cid(), "You've been banned from " + o.getString("chan"));
                            break;
                        case "invalid_nickchange":
                            showAlert(o.cid(), o.getString("ban_channel") + ": " + o.getString("msg"));
                            break;
                        case "no_messages_from_non_registered":
                            if (o.has("nick") && o.getString("nick").length() > 0)
                                showAlert(o.cid(), o.getString("nick") + ": " + o.getString("msg"));
                            else
                                showAlert(o.cid(), o.getString("msg"));
                            break;
                        case "not_registered":
                            String first = o.getString("first");
                            if (o.has("rest"))
                                first += " " + o.getString("rest");
                            showAlert(o.cid(), first + ": " + o.getString("msg"));
                            break;
                        case "too_many_channels":
                            showAlert(o.cid(), "Couldn't join " + o.getString("chan") + ": " + o.getString("msg"));
                            break;
                        case "too_many_targets":
                            showAlert(o.cid(), o.getString("description") + ": " + o.getString("msg"));
                            break;
                        case "no_such_server":
                            showAlert(o.cid(), o.getString("server") + ": " + o.getString("msg"));
                            break;
                        case "unknown_command":
                            if(o.has("msg") && o.getString("msg").length() > 0)
                                showAlert(o.cid(), o.getString("msg") + ": " + o.getString("command"));
                            else
                                showAlert(o.cid(), "Unknown command: " + o.getString("command"));
                            break;
                        case "help_not_found":
                            showAlert(o.cid(), o.getString("topic") + ": " + o.getString("msg"));
                            break;
                        case "accept_exists":
                            showAlert(o.cid(), o.getString("nick") + " " + o.getString("msg"));
                            break;
                        case "accept_not":
                            showAlert(o.cid(), o.getString("nick") + " " + o.getString("msg"));
                            break;
                        case "nick_collision":
                            showAlert(o.cid(), o.getString("collision") + ": " + o.getString("msg"));
                            break;
                        case "nick_too_fast":
                            showAlert(o.cid(), o.getString("nick") + ": " + o.getString("msg"));
                            break;
                        case "save_nick":
                            showAlert(o.cid(), o.getString("nick") + ": " + o.getString("msg") + ": " + o.getString("new_nick"));
                            break;
                        case "unknown_mode":
                            showAlert(o.cid(), "Missing mode: " + o.getString("params"));
                            break;
                        case "user_not_in_channel":
                            showAlert(o.cid(), o.getString("nick") + " is not in " + o.getString("channel"));
                            break;
                        case "need_more_params":
                            showAlert(o.cid(), "Missing parameters for command: " + o.getString("command"));
                            break;
                        case "chan_privs_needed":
                            showAlert(o.cid(), o.getString("chan") + ": " + o.getString("msg"));
                            break;
                        case "not_on_channel":
                            showAlert(o.cid(), o.getString("channel") + ": " + o.getString("msg"));
                            break;
                        case "ban_on_chan":
                            showAlert(o.cid(), "You cannot change your nick to " + o.getString("proposed_nick") + " while banned on " + o.getString("channel"));
                            break;
                        case "cannot_send_to_chan":
                            showAlert(o.cid(), o.getString("channel") + ": " + o.getString("msg"));
                            break;
                        case "cant_send_to_user":
                            showAlert(o.cid(), o.getString("nick") + ": " + o.getString("msg"));
                            break;
                        case "user_on_channel":
                            showAlert(o.cid(), o.getString("nick") + " is already a member of " + o.getString("channel"));
                            break;
                        case "no_nick_given":
                            showAlert(o.cid(), "No nickname given");
                            break;
                        case "nickname_in_use":
                            showAlert(o.cid(), o.getString("nick") + " is already in use");
                            break;
                        case "silence":
                            String mask = o.getString("usermask");
                            if (mask.startsWith("-"))
                                message = mask.substring(1) + " removed from silence list";
                            else if (mask.startsWith("+"))
                                message = mask.substring(1) + " added to silence list";
                            else
                                message = "Silence list change: " + mask;
                            showAlert(o.cid(), message);
                            break;
                        case "no_channel_topic":
                            showAlert(o.cid(), o.getString("channel") + ": " + o.getString("msg"));
                            break;
                        case "time":
                            message = o.getString("time_string");
                            if (o.has("time_stamp") && o.getString("time_stamp").length() > 0)
                                message += " (" + o.getString("time_stamp") + ")";
                            message += " — " + o.getString("time_server");
                            showAlert(o.cid(), message);
                            break;
                        case "blocked_channel":
                            showAlert(o.cid(), "This channel is blocked, you have been disconnected");
                            break;
                        case "unknown_error":
                            showAlert(o.cid(), "Unknown error: [" + o.getString("command") + "] " + o.getString("msg"));
                            break;
                        case "pong":
                            if(o.has("origin") && o.getString("origin").length() > 0)
                                showAlert(o.cid(), "PONG from: " + o.getString("origin") + ": " + o.getString("msg"));
                            else
                                showAlert(o.cid(), "PONG: " + o.getString("msg"));
                            break;
                        case "monitor_full":
                            showAlert(o.cid(), o.getString("targets") + ": " + o.getString("msg"));
                            break;
                        case "mlock_restricted":
                            showAlert(o.cid(), o.getString("channel") + ": " + o.getString("msg") + "\nMLOCK: " + o.getString("mlock") + "\nRequested mode change: " + o.getString("mode_change"));
                            break;
                        case "cannot_do_cmd":
                            if(o.has("cmd") && o.getString("cmd").length() > 0)
                                showAlert(o.cid(), o.getString("cmd") + ": " + o.getString("msg"));
                            else
                                showAlert(o.cid(), o.getString("msg"));
                            break;
                        case "cannot_change_chan_mode":
                            if(o.has("mode") && o.getString("mode").length() > 0)
                                showAlert(o.cid(), "You can't change channel mode: " + o.getString("mode") + "; " + o.getString("msg"));
                            else
                                showAlert(o.cid(), "You can't change channel mode; " + o.getString("msg"));
                            break;
                        case "metadata_limit":
                        case "metadata_targetinvalid":
                            showAlert(o.cid(), o.getString("msg") + ": " + o.getString("target"));
                            break;
                        case "metadata_nomatchingkey":
                        case "metadata_keynotset":
                        case "metadata_keynopermission":
                            showAlert(o.cid(), o.getString("msg") + ": " + o.getString("key") + " for target " + o.getString("target"));
                            break;
                        case "metadata_keyinvalid":
                            showAlert(o.cid(), "Invalid metadata for key: " + o.getString("key"));
                            break;
                        case "metadata_toomanysubs":
                            showAlert(o.cid(), "Metadata key subscription limit reached, keys after and including '" + o.getString("key") + "' are not subscribed");
                            break;
                        case "fail":
                            showAlert(o.cid(), "FAIL: " + o.getString("command") + ": " + o.getString("code") + ": " + o.getString("description") + ": " + o.getString("context"));
                            break;
                        default:
                            if(o.has("message") && o.getString("message").equals("invalid_nick"))
                                showAlert(-1, "Invalid nickname");
                            else
                                showAlert(o.cid(), o.getString("msg"));
                        }
                    } catch (Exception e2) {
                    NetworkConnection.printStackTraceToCrashlytics(e2);
                }
                break;
            default:
                break;
        }
    }

    protected void showAlert(int cid, final String msg) {
        final Server server = ServersList.getInstance().getServer(cid);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
                if(server != null)
                    builder.setTitle(server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")");
                builder.setMessage(msg);
                builder.setNegativeButton("Ok", new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            dialog.dismiss();
                        } catch (IllegalArgumentException e) {
                        }
                    }
                });
                if (!isFinishing()) {
                    AlertDialog dialog = builder.create();
                    dialog.setOwnerActivity(BaseActivity.this);
                    dialog.show();
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_base, menu);
        setMenuColorFilter(menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void setMenuColorFilter(final Menu menu) {
        for(int i = 0; i < menu.size(); i++) {
            MenuItem menuItem = menu.getItem(i);
            Drawable d = menuItem.getIcon();
            if(d != null) {
                d.mutate();
                d.setColorFilter(ColorScheme.getInstance().navBarSubheadingColor, PorterDuff.Mode.SRC_ATOP);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Logout");
                builder.setMessage("Would you like to logout of IRCCloud?");

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setPositiveButton("Logout", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        conn.logout();
                        IRCCloudLog.Log("LOGOUT: Logout menu item selected");
                        Intent i = new Intent(BaseActivity.this, LoginActivity.class);
                        i.addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                        Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        finish();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.setOwnerActivity(this);
                if(!isFinishing())
                    dialog.show();
                break;
            case R.id.menu_settings:
                Intent i = new Intent(this, PreferencesActivity.class);
                startActivity(i);
                break;
            case R.id.menu_feedback:
                NetworkConnection.sendFeedbackReport(this);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private class ImageListPruneTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            ImageList.getInstance().prune();
            return null;
        }
    }
}
