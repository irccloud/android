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
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.irccloud.android.BackgroundTaskWorker;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.collection.AvatarsList;
import com.irccloud.android.data.collection.EventsList;
import com.irccloud.android.data.collection.ImageList;
import com.irccloud.android.data.collection.LogExportsList;
import com.irccloud.android.data.model.LogExport;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.collection.ServersList;
import com.samsung.android.sdk.multiwindow.SMultiWindow;
import com.samsung.android.sdk.multiwindow.SMultiWindowActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class BaseActivity extends AppCompatActivity implements NetworkConnection.IRCEventHandler, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    NetworkConnection conn;
    private View dialogTextPrompt;
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError;
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final int REQUEST_SEND_FEEDBACK = 1002;
    private static final String LOG_FILENAME = "log.txt";

    private SMultiWindow mMultiWindow = null;
    private SMultiWindowActivity mMultiWindowActivity = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean themeChanged = false;
        String theme = ColorScheme.getUserTheme();
        if(ColorScheme.getInstance().theme == null || !ColorScheme.getInstance().theme.equals(theme)) {
            themeChanged = true;
        }
        setTheme(ColorScheme.getTheme(theme, true));
        ColorScheme.getInstance().setThemeFromContext(this, theme);
        if(themeChanged) {
            EventsList.getInstance().clearCaches();
            AvatarsList.getInstance().clear();
        }
        if (Build.VERSION.SDK_INT >= 21) {
            Bitmap cloud = BitmapFactory.decodeResource(getResources(), R.drawable.splash_logo);
            if(cloud != null) {
                setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.app_name), cloud, ColorScheme.getInstance().navBarColor));
            }
            getWindow().setStatusBarColor(ColorScheme.getInstance().statusBarColor);
            getWindow().setNavigationBarColor(getResources().getColor(android.R.color.black));
        }
        if(ColorScheme.getInstance().windowBackgroundDrawable != 0)
            getWindow().setBackgroundDrawableResource(ColorScheme.getInstance().windowBackgroundDrawable);
        if(Build.VERSION.SDK_INT >= 23) {
            if(theme.equals("dawn"))
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            else
                getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() &~ View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.CREDENTIALS_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (conn != null) {
            conn.removeHandler(this);
        }
    }

    public boolean isMultiWindow() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            return (mMultiWindowActivity != null && !mMultiWindowActivity.isNormalWindow());
        else
            return isInMultiWindowMode();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        try {
            super.onStop();
        } catch (IllegalStateException e) {
            //Android Support Library bug
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            if (GooglePlayServicesUtil.isUserRecoverableError(result.getErrorCode())) {
                try {
                    GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, REQUEST_RESOLVE_ERROR).show();
                    mResolvingError = true;
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        } else if(requestCode == REQUEST_SEND_FEEDBACK) {
            if(getFileStreamPath(LOG_FILENAME).exists()) {
                android.util.Log.d("IRCCloud", "Removing stale log file");
                getFileStreamPath(LOG_FILENAME).delete();
            }
        }
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
        File f = new File(getFilesDir(), LOG_FILENAME);
        if(f.exists()) {
            android.util.Log.d("IRCCloud", "Removing stale log file");
            f.delete();
        }
        new ImageListPruneTask().execute((Void)null);
        String session = getSharedPreferences("prefs", 0).getString("session_key", "");
        if (session.length() > 0) {
            if(conn.notifier) {
                Crashlytics.log(Log.INFO, "IRCCloud", "Upgrading notifier websocket");
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
        if(conn != null){
            Crashlytics.log(Log.INFO, "IRCCloud", "App resumed, websocket state: " + conn.getState());
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
                        if(mGoogleApiClient.isConnected()) {
                            Auth.CredentialsApi.disableAutoSignIn(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                                @Override
                                public void onResult(Status status) {
                                    Intent i = new Intent(BaseActivity.this, LoginActivity.class);
                                    i.addFlags(
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                                    Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(i);
                                    finish();
                                }
                            });
                        } else {
                            Intent i = new Intent(BaseActivity.this, LoginActivity.class);
                            i.addFlags(
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                            Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                            finish();
                        }
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
                try {
                    String bugReport = "Briefly describe the issue below:\n\n\n\n\n" +
                        "===========\n" +
                        "UID: " + PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getString("uid", "") + "\n" +
                        "App version: " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName + " (" + getPackageManager().getPackageInfo(getPackageName(), 0).versionCode + ")\n" +
                        "Device: " + Build.MODEL + "\n" +
                        "Android version: " + Build.VERSION.RELEASE + "\n" +
                        "Firmware fingerprint: " + Build.FINGERPRINT + "\n";

                    File logsDir = new File(getFilesDir(),".Fabric/com.crashlytics.sdk.android.crashlytics-core/log-files/");
                    File log = null;
                    File output = null;
                    if(logsDir.exists()) {
                        long max = Long.MIN_VALUE;
                        for (File f : logsDir.listFiles()) {
                            if (f.lastModified() > max) {
                                max = f.lastModified();
                                log = f;
                            }
                        }

                        if (log != null) {
                            File f = new File(getFilesDir(), "logs");
                            f.mkdirs();
                            output = new File(f, LOG_FILENAME);
                            byte[] b = new byte[1];

                            FileOutputStream out = new FileOutputStream(output);
                            FileInputStream is = new FileInputStream(log);
                            is.skip(5);

                            while (is.available() > 0 && is.read(b, 0, 1) > 0) {
                                if (b[0] == ' ') {
                                    while (is.available() > 0 && is.read(b, 0, 1) > 0) {
                                        out.write(b);
                                        if (b[0] == '\n')
                                            break;
                                    }
                                }
                            }
                            is.close();
                            out.close();
                        }
                    }

                    Intent email = new Intent(Intent.ACTION_SEND);
                    email.setData(Uri.parse("mailto:"));
                    email.setType("message/rfc822");
                    email.putExtra(Intent.EXTRA_EMAIL, new String[]{"IRCCloud Team <team@irccloud.com>"});
                    email.putExtra(Intent.EXTRA_TEXT, bugReport);
                    email.putExtra(Intent.EXTRA_SUBJECT, "IRCCloud for Android");
                    if(log != null) {
                        email.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", output));
                    }
                    startActivityForResult(Intent.createChooser(email, "Send Feedback:"), 0);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to generate email report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Crashlytics.logException(e);
                    NetworkConnection.printStackTraceToCrashlytics(e);
                }
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
