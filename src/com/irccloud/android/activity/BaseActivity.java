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
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.irccloud.android.BackgroundTaskService;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.collection.EventsList;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.collection.ServersList;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.multiwindow.SMultiWindow;
import com.samsung.android.sdk.multiwindow.SMultiWindowActivity;

public class BaseActivity extends AppCompatActivity implements NetworkConnection.IRCEventHandler, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    NetworkConnection conn;
    private View dialogTextPrompt;
    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError;
    private static final int REQUEST_RESOLVE_ERROR = 1001;

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
        if(themeChanged)
            EventsList.getInstance().clearCaches();

        if (Build.VERSION.SDK_INT >= 21) {
            Bitmap cloud = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            if(cloud != null) {
                setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.app_name), cloud, ColorScheme.getInstance().navBarColor));
                cloud.recycle();
            }
            getWindow().setStatusBarColor(ColorScheme.getInstance().statusBarColor);
            getWindow().setNavigationBarColor(getResources().getColor(android.R.color.black));
        }
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

        if(ServersList.getInstance().count() == 0)
            NetworkConnection.getInstance().load();

        try {
            mMultiWindow = new SMultiWindow();
            mMultiWindow.initialize(this);
            mMultiWindowActivity = new SMultiWindowActivity(this);
        } catch (SsdkUnsupportedException e) {
        }
    }

    public boolean isMultiWindow() {
        return (mMultiWindowActivity != null && !mMultiWindowActivity.isNormalWindow());
    }

    public void makeMultiWindowIntent(Intent i) {
        if(mMultiWindowActivity != null)
            SMultiWindowActivity.makeMultiWindowIntent(i, SMultiWindowActivity.ZONE_A);
    }

    @Override
    protected void onStart() {
        super.onStart();
        BackgroundTaskService.cancelBacklogSync(this);
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("background_sync", true)) {
            BackgroundTaskService.scheduleBacklogSync(this);
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
                GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, REQUEST_RESOLVE_ERROR).show();
                mResolvingError = true;
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
        }
    }

    public View getDialogTextPrompt() {
        if (dialogTextPrompt == null)
            dialogTextPrompt = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_textprompt, null);
        if (dialogTextPrompt.getParent() != null)
            ((ViewGroup) dialogTextPrompt.getParent()).removeView(dialogTextPrompt);
        return dialogTextPrompt;
    }

    @Override
    public void onResume() {
        super.onResume();
        String session = getSharedPreferences("prefs", 0).getString("session_key", "");
        if (session != null && session.length() > 0) {
            conn = NetworkConnection.getInstance();
            conn.addHandler(this);
            if(conn.notifier) {
                android.util.Log.d("IRCCloud", "Upgrading notifier websocket");
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
            if (conn.getState() == NetworkConnection.STATE_DISCONNECTED || conn.getState() == NetworkConnection.STATE_DISCONNECTING)
                conn.connect();
            conn.registerForConnectivity();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (conn != null) {
            conn.removeHandler(this);
        }

        NetworkConnection.getInstance().unregisterForConnectivity();
    }

    public void onIRCEvent(int what, Object obj) {
        String message = "";
        final IRCCloudJSONObject o;

        switch (what) {
            case NetworkConnection.EVENT_BADCHANNELKEY:
                o = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Server server = ServersList.getInstance().getServer(o.cid());
                        AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
                        builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
                        View view = getDialogTextPrompt();
                        TextView prompt = (TextView) view.findViewById(R.id.prompt);
                        final EditText keyinput = (EditText) view.findViewById(R.id.textInput);
                        keyinput.setText("");
                        keyinput.setOnEditorActionListener(new OnEditorActionListener() {
                            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
                                    try {
                                        if (keyinput.getText() != null)
                                            conn.join(o.cid(), o.getString("chan"), keyinput.getText().toString());
                                    } catch (Exception e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
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
                            e.printStackTrace();
                        }
                        builder.setTitle(server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")");
                        builder.setView(view);
                        builder.setPositiveButton("Join", new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    conn.join(o.cid(), o.getString("chan"), keyinput.getText().toString());
                                } catch (Exception e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
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
                        dialog.show();
                    }
                });
                break;
            case NetworkConnection.EVENT_INVALIDNICK:
                o = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Server server = ServersList.getInstance().getServer(o.cid());
                        AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
                        builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
                        View view = getDialogTextPrompt();
                        TextView prompt = (TextView) view.findViewById(R.id.prompt);
                        final EditText nickinput = (EditText) view.findViewById(R.id.textInput);
                        nickinput.setText("");
                        nickinput.setOnEditorActionListener(new OnEditorActionListener() {
                            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
                                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
                                    try {
                                        conn.say(o.cid(), null, "/nick " + nickinput.getText().toString());
                                    } catch (Exception e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                    }
                                    ((AlertDialog) nickinput.getTag()).dismiss();
                                }
                                return true;
                            }
                        });
                        try {
                            String message = o.getString("invalid_nick") + " is not a valid nickname, try again";
                            if (server.isupport != null && server.isupport.has("NICKLEN"))
                                message += "\n(" + server.isupport.get("NICKLEN").asText() + " chars)";
                            message += ".";
                            prompt.setText(message);
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        builder.setTitle(server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")");
                        builder.setView(view);
                        builder.setPositiveButton("Change Nickname", new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    conn.say(o.cid(), null, "/nick " + nickinput.getText().toString());
                                } catch (Exception e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
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
                        nickinput.setTag(dialog);
                        dialog.setOwnerActivity(BaseActivity.this);
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        dialog.show();
                    }
                });
                break;
            case NetworkConnection.EVENT_ALERT:
                try {
                    o = (IRCCloudJSONObject) obj;
                    String type = o.type();

                    if (type.equalsIgnoreCase("invite_only_chan"))
                        showAlert(o.cid(), "You need an invitation to join " + o.getString("chan"));
                    else if (type.equalsIgnoreCase("channel_full"))
                        showAlert(o.cid(), o.getString("chan") + " isn't allowing any more members to join.");
                    else if (type.equalsIgnoreCase("banned_from_channel"))
                        showAlert(o.cid(), "You've been banned from " + o.getString("chan"));
                    else if (type.equalsIgnoreCase("invalid_nickchange"))
                        showAlert(o.cid(), o.getString("ban_channel") + ": " + o.getString("msg"));
                    else if (type.equalsIgnoreCase("no_messages_from_non_registered")) {
                        if (o.has("nick") && o.getString("nick").length() > 0)
                            showAlert(o.cid(), o.getString("nick") + ": " + o.getString("msg"));
                        else
                            showAlert(o.cid(), o.getString("msg"));
                    } else if (type.equalsIgnoreCase("not_registered")) {
                        String first = o.getString("first");
                        if (o.has("rest"))
                            first += " " + o.getString("rest");
                        showAlert(o.cid(), first + ": " + o.getString("msg"));
                    } else if (type.equalsIgnoreCase("too_many_channels"))
                        showAlert(o.cid(), "Couldn't join " + o.getString("chan") + ": " + o.getString("msg"));
                    else if (type.equalsIgnoreCase("too_many_targets"))
                        showAlert(o.cid(), o.getString("description") + ": " + o.getString("msg"));
                    else if (type.equalsIgnoreCase("no_such_server"))
                        showAlert(o.cid(), o.getString("server") + ": " + o.getString("msg"));
                    else if (type.equalsIgnoreCase("unknown_command"))
                        showAlert(o.cid(), "Unknown command: " + o.getString("command"));
                    else if (type.equalsIgnoreCase("help_not_found"))
                        showAlert(o.cid(), o.getString("topic") + ": " + o.getString("msg"));
                    else if (type.equalsIgnoreCase("accept_exists"))
                        showAlert(o.cid(), o.getString("nick") + " " + o.getString("msg"));
                    else if (type.equalsIgnoreCase("accept_not"))
                        showAlert(o.cid(), o.getString("nick") + " " + o.getString("msg"));
                    else if (type.equalsIgnoreCase("nick_collision"))
                        showAlert(o.cid(), o.getString("collision") + ": " + o.getString("msg"));
                    else if (type.equalsIgnoreCase("nick_too_fast"))
                        showAlert(o.cid(), o.getString("nick") + ": " + o.getString("msg"));
                    else if (type.equalsIgnoreCase("save_nick"))
                        showAlert(o.cid(), o.getString("nick") + ": " + o.getString("msg") + ": " + o.getString("new_nick"));
                    else if (type.equalsIgnoreCase("unknown_mode"))
                        showAlert(o.cid(), "Missing mode: " + o.getString("params"));
                    else if (type.equalsIgnoreCase("user_not_in_channel"))
                        showAlert(o.cid(), o.getString("nick") + " is not in " + o.getString("channel"));
                    else if (type.equalsIgnoreCase("need_more_params"))
                        showAlert(o.cid(), "Missing parameters for command: " + o.getString("command"));
                    else if (type.equalsIgnoreCase("chan_privs_needed"))
                        showAlert(o.cid(), o.getString("chan") + ": " + o.getString("msg"));
                    else if (type.equalsIgnoreCase("not_on_channel"))
                        showAlert(o.cid(), o.getString("channel") + ": " + o.getString("msg"));
                    else if (type.equalsIgnoreCase("ban_on_chan"))
                        showAlert(o.cid(), "You cannot change your nick to " + o.getString("proposed_nick") + " while banned on " + o.getString("channel"));
                    else if (type.equalsIgnoreCase("cannot_send_to_chan"))
                        showAlert(o.cid(), o.getString("channel") + ": " + o.getString("msg"));
                    else if (type.equalsIgnoreCase("user_on_channel"))
                        showAlert(o.cid(), o.getString("nick") + " is already a member of " + o.getString("channel"));
                    else if (type.equalsIgnoreCase("no_nick_given"))
                        showAlert(o.cid(), "No nickname given");
                    else if (type.equalsIgnoreCase("silence")) {
                        String mask = o.getString("usermask");
                        if (mask.startsWith("-"))
                            message = mask.substring(1) + " removed from silence list";
                        else if (mask.startsWith("+"))
                            message = mask.substring(1) + " added to silence list";
                        else
                            message = "Silence list change: " + mask;
                        showAlert(o.cid(), message);
                    } else if (type.equalsIgnoreCase("no_channel_topic"))
                        showAlert(o.cid(), o.getString("channel") + ": " + o.getString("msg"));
                    else if (type.equalsIgnoreCase("time")) {
                        message = o.getString("time_string");
                        if (o.has("time_stamp") && o.getString("time_stamp").length() > 0)
                            message += " (" + o.getString("time_stamp") + ")";
                        message += " — " + o.getString("time_server");
                        showAlert(o.cid(), message);
                    } else
                        showAlert(o.cid(), o.getString("msg"));
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onIRCRequestSucceeded(int reqid, IRCCloudJSONObject object) {
    }

    @Override
    public void onIRCRequestFailed(int reqid, IRCCloudJSONObject object) {
    }

    protected void showAlert(int cid, final String msg) {
        final Server server = ServersList.getInstance().getServer(cid);
        if (server != null)
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
                    builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
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
                    AlertDialog dialog = builder.create();
                    dialog.setOwnerActivity(BaseActivity.this);
                    dialog.show();
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
                builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
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
                dialog.show();
                break;
            case R.id.menu_settings:
                Intent i = new Intent(this, PreferencesActivity.class);
                startActivity(i);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
