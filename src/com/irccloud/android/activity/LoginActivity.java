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
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;

import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.BuildConfig;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudLinkMovementMethod;
import com.irccloud.android.IRCCloudLog;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class LoginActivity extends FragmentActivity {
    private View login = null;
    private View loading = null;
    private EditText email;
    private EditText password;
    private EditText host;
    private EditText name;
    private Button loginBtn;
    private Button signupBtn;
    private Button nextBtn;
    private Button sendAccessLinkBtn;
    private TextView connectingMsg = null;
    private TextView TOS = null;
    private TextView forgotPassword = null;
    private TextView enterpriseLearnMore = null;
    private TextView EnterYourEmail = null;
    private TextView hostHint = null;
    private LinearLayout notAProblem = null;
    private ProgressBar progressBar = null;
    private View connecting = null;
    private LinearLayout loginHint = null;
    private LinearLayout signupHint = null;
    private LinearLayout enterpriseHint = null;
    private LinearLayout loginSignupHint = null;

    private String auth_url = null;

    ActivityResultLauncher<Intent> samlLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult o) {
            if (o.getResultCode() == RESULT_OK) {
                final Intent i = new Intent(LoginActivity.this, MainActivity.class);
                i.putExtra("nosplash", true);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                if (getIntent() != null) {
                    if (getIntent().getData() != null)
                        i.setData(getIntent().getData());
                    if (getIntent().getExtras() != null)
                        i.putExtras(getIntent().getExtras());
                }

                startActivity(i);
                finish();
            } else {
                NetworkConnection.IRCCLOUD_HOST = null;
                name.setVisibility(View.GONE);
                email.setVisibility(View.GONE);
                password.setVisibility(View.GONE);
                loginBtn.setVisibility(View.GONE);
                signupBtn.setVisibility(View.GONE);
                TOS.setVisibility(View.GONE);
                signupHint.setVisibility(View.GONE);
                loginHint.setVisibility(View.GONE);
                forgotPassword.setVisibility(View.GONE);
                loginSignupHint.setVisibility(View.GONE);
                EnterYourEmail.setVisibility(View.GONE);
                sendAccessLinkBtn.setVisibility(View.GONE);
                notAProblem.setVisibility(View.GONE);
                enterpriseLearnMore.setVisibility(View.VISIBLE);
                enterpriseHint.setVisibility(View.VISIBLE);
                host.setVisibility(View.VISIBLE);
                nextBtn.setVisibility(View.VISIBLE);
                hostHint.setVisibility(View.VISIBLE);
                host.requestFocus();
                ((TextView) findViewById(R.id.enterpriseHintText)).setText("Enterprise Edition");
            }
        }
    });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bitmap cloud = BitmapFactory.decodeResource(getResources(), R.drawable.splash_logo);
        setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.app_name), cloud, 0xff0b2e60));

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        try {
            setContentView(R.layout.activity_login);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to load resources required for this device configuration.  Please reinstall the app from the Play Store or install the correct APKs for this device configuration.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loading = findViewById(R.id.loading);

        connecting = findViewById(R.id.connecting);
        connectingMsg = findViewById(R.id.connectingMsg);
        progressBar = findViewById(R.id.connectingProgress);

        loginHint = findViewById(R.id.loginHint);
        signupHint = findViewById(R.id.signupHint);
        hostHint = findViewById(R.id.hostHint);

        login = findViewById(R.id.login);
        name = findViewById(R.id.name);
        if (savedInstanceState != null && savedInstanceState.containsKey("name"))
            name.setText(savedInstanceState.getString("name"));
        email = findViewById(R.id.email);
        if (BuildConfig.ENTERPRISE)
            email.setHint(R.string.email_enterprise);

        if (savedInstanceState != null && savedInstanceState.containsKey("email"))
            email.setText(savedInstanceState.getString("email"));

        password = findViewById(R.id.password);
        password.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    login.post(new Runnable() {
                        @Override
                        public void run() {
                            login();
                        }
                    });
                    return true;
                }
                return false;
            }
        });
        if (savedInstanceState != null && savedInstanceState.containsKey("password"))
            password.setText(savedInstanceState.getString("password"));

        host = findViewById(R.id.host);
        if (BuildConfig.ENTERPRISE)
            host.setText(NetworkConnection.IRCCLOUD_HOST);
        else
            host.setVisibility(View.GONE);
        host.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    login.post(new Runnable() {
                        @Override
                        public void run() {
                            login();
                        }
                    });
                    return true;
                }
                return false;
            }
        });
        if (savedInstanceState != null && savedInstanceState.containsKey("host"))
            host.setText(savedInstanceState.getString("host"));
        else
            host.setText(getSharedPreferences("prefs", 0).getString("host", BuildConfig.HOST));

        if (host.getText().toString().equals("api.irccloud.com") || host.getText().toString().equals("www.irccloud.com"))
            host.setText("");

        loginBtn = findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                login.post(new Runnable() {
                    @Override
                    public void run() {
                        if(auth_url != null) {
                            Intent i = new Intent(LoginActivity.this, SAMLAuthActivity.class);
                            i.putExtra("auth_url", auth_url);
                            i.putExtra("title", loginBtn.getText().toString());
                            samlLauncher.launch(i);
                        } else {
                            login();
                        }
                    }
                });
            }
        });
        loginBtn.setFocusable(true);
        loginBtn.requestFocus();

        sendAccessLinkBtn = findViewById(R.id.sendAccessLink);
        sendAccessLinkBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new ResetPasswordTask().execute((Void) null);
            }
        });

        nextBtn = findViewById(R.id.nextBtn);
        nextBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (host.getText().length() > 0) {
                    NetworkConnection.IRCCLOUD_HOST = host.getText().toString();
                    trimHost();

                    new EnterpriseConfigTask().execute((Void) null);
                }
            }
        });

        TOS = findViewById(R.id.TOS);
        TOS.setMovementMethod(IRCCloudLinkMovementMethod.getInstance());

        forgotPassword = findViewById(R.id.forgotPassword);
        forgotPassword.setOnClickListener(forgotPasswordClickListener);

        enterpriseLearnMore = findViewById(R.id.enterpriseLearnMore);
        enterpriseLearnMore.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPackageInstalled("com.irccloud.android", LoginActivity.this)) {
                    startActivity(getPackageManager().getLaunchIntentForPackage("com.irccloud.android"));
                } else {
                    try {
                        IRCCloudLinkMovementMethod.launchBrowser(Uri.parse("market://details?id=com.irccloud.android"),LoginActivity.this);
                    } catch (Exception e) {
                        IRCCloudLinkMovementMethod.launchBrowser(Uri.parse("https://play.google.com/store/apps/details?id=com.irccloud.android"),LoginActivity.this);
                    }
                }
            }

            private boolean isPackageInstalled(String packagename, Context context) {
                PackageManager pm = context.getPackageManager();
                try {
                    pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
                    return true;
                } catch (NameNotFoundException e) {
                    return false;
                }
            }
        });
        enterpriseHint = findViewById(R.id.enterpriseHint);

        EnterYourEmail = findViewById(R.id.enterYourEmail);

        signupHint.setOnClickListener(signupHintClickListener);
        loginHint.setOnClickListener(loginHintClickListener);

        signupBtn = findViewById(R.id.signupBtn);
        signupBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                login();
            }
        });

        TextView version = findViewById(R.id.version);
        try {
            version.setText("Version " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (NameNotFoundException e) {
            version.setVisibility(View.GONE);
        }

        Typeface SourceSansProRegular = ResourcesCompat.getFont(IRCCloudApplication.getInstance().getApplicationContext(), R.font.sourcesansproregular);
        Typeface SourceSansProLightItalic = ResourcesCompat.getFont(IRCCloudApplication.getInstance().getApplicationContext(), R.font.sourcesansprolightit);

        for (int i = 0; i < signupHint.getChildCount(); i++) {
            View v = signupHint.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setTypeface(SourceSansProRegular);
            }
        }

        for (int i = 0; i < loginHint.getChildCount(); i++) {
            View v = loginHint.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setTypeface(SourceSansProRegular);
            }
        }

        LinearLayout IRCCloud = findViewById(R.id.IRCCloud);
        for (int i = 0; i < IRCCloud.getChildCount(); i++) {
            View v = IRCCloud.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setTypeface(SourceSansProRegular);
            }
        }

        notAProblem = findViewById(R.id.notAProblem);
        for (int i = 0; i < notAProblem.getChildCount(); i++) {
            View v = notAProblem.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setTypeface((i == 0) ? SourceSansProRegular : SourceSansProLightItalic);
            }
        }

        loginSignupHint = findViewById(R.id.loginSignupHint);
        for (int i = 0; i < loginSignupHint.getChildCount(); i++) {
            View v = loginSignupHint.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setTypeface(SourceSansProRegular);
                v.setOnClickListener((i == 0) ? loginHintClickListener : signupHintClickListener);
            }
        }

        name.setTypeface(SourceSansProRegular);
        email.setTypeface(SourceSansProRegular);
        password.setTypeface(SourceSansProRegular);
        host.setTypeface(SourceSansProRegular);
        loginBtn.setTypeface(SourceSansProRegular);
        signupBtn.setTypeface(SourceSansProRegular);
        TOS.setTypeface(SourceSansProRegular);
        EnterYourEmail.setTypeface(SourceSansProRegular);
        hostHint.setTypeface(SourceSansProLightItalic);

        if (BuildConfig.ENTERPRISE) {
            name.setVisibility(View.GONE);
            email.setVisibility(View.GONE);
            password.setVisibility(View.GONE);
            loginBtn.setVisibility(View.GONE);
            signupBtn.setVisibility(View.GONE);
            TOS.setVisibility(View.GONE);
            signupHint.setVisibility(View.GONE);
            loginHint.setVisibility(View.GONE);
            forgotPassword.setVisibility(View.GONE);
            loginSignupHint.setVisibility(View.GONE);
            EnterYourEmail.setVisibility(View.GONE);
            sendAccessLinkBtn.setVisibility(View.GONE);
            notAProblem.setVisibility(View.GONE);
            enterpriseLearnMore.setVisibility(View.VISIBLE);
            enterpriseHint.setVisibility(View.VISIBLE);
            host.setVisibility(View.VISIBLE);
            nextBtn.setVisibility(View.VISIBLE);
            hostHint.setVisibility(View.VISIBLE);
            host.requestFocus();
        }

        if (savedInstanceState != null && savedInstanceState.containsKey("signup") && savedInstanceState.getBoolean("signup")) {
            signupHintClickListener.onClick(null);
        }

        if (savedInstanceState != null && savedInstanceState.containsKey("login") && savedInstanceState.getBoolean("login")) {
            loginHintClickListener.onClick(null);
        }

        if (savedInstanceState != null && savedInstanceState.containsKey("forgotPassword") && savedInstanceState.getBoolean("forgotPassword")) {
            forgotPasswordClickListener.onClick(null);
        }
    }

    private OnClickListener signupHintClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            name.setVisibility(View.VISIBLE);
            email.setVisibility(View.VISIBLE);
            password.setVisibility(View.VISIBLE);
            loginBtn.setVisibility(View.GONE);
            signupBtn.setVisibility(View.VISIBLE);
            name.requestFocus();
            TOS.setVisibility(View.VISIBLE);
            signupHint.setVisibility(View.GONE);
            loginHint.setVisibility(View.VISIBLE);
            forgotPassword.setVisibility(View.GONE);
            loginSignupHint.setVisibility(View.GONE);
            EnterYourEmail.setVisibility(View.GONE);
            sendAccessLinkBtn.setVisibility(View.GONE);
            notAProblem.setVisibility(View.GONE);
            email.setBackgroundResource(R.drawable.login_mid_input);
            host.setVisibility(View.GONE);
            nextBtn.setVisibility(View.GONE);
            enterpriseLearnMore.setVisibility(View.GONE);
            enterpriseHint.setVisibility(View.GONE);
            hostHint.setVisibility(View.GONE);
        }
    };

    private OnClickListener loginHintClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            name.setVisibility(View.GONE);
            if(auth_url != null) {
                email.setVisibility(View.GONE);
                password.setVisibility(View.GONE);
                forgotPassword.setVisibility(View.GONE);
            } else {
                email.setVisibility(View.VISIBLE);
                password.setVisibility(View.VISIBLE);
                forgotPassword.setVisibility(View.VISIBLE);
                email.requestFocus();
            }
            loginBtn.setVisibility(View.VISIBLE);
            signupBtn.setVisibility(View.GONE);
            TOS.setVisibility(View.GONE);
            loginHint.setVisibility(View.GONE);
            loginSignupHint.setVisibility(View.GONE);
            EnterYourEmail.setVisibility(View.GONE);
            sendAccessLinkBtn.setVisibility(View.GONE);
            notAProblem.setVisibility(View.GONE);
            email.setBackgroundResource(R.drawable.login_top_input);
            host.setVisibility(View.GONE);
            nextBtn.setVisibility(View.GONE);
            enterpriseLearnMore.setVisibility(View.GONE);
            enterpriseHint.setVisibility(View.GONE);
            hostHint.setVisibility(View.GONE);
            if (loginSignupHint.getChildAt(1).getVisibility() == View.VISIBLE)
                signupHint.setVisibility(View.VISIBLE);
            else
                enterpriseHint.setVisibility(View.VISIBLE);
        }
    };

    private OnClickListener forgotPasswordClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            loginHint.setVisibility(View.GONE);
            signupHint.setVisibility(View.GONE);
            loginSignupHint.setVisibility(View.VISIBLE);
            notAProblem.setVisibility(View.VISIBLE);
            password.setVisibility(View.GONE);
            loginBtn.setVisibility(View.GONE);
            signupBtn.setVisibility(View.GONE);
            TOS.setVisibility(View.GONE);
            sendAccessLinkBtn.setVisibility(View.VISIBLE);
            EnterYourEmail.setVisibility(View.VISIBLE);
            forgotPassword.setVisibility(View.GONE);
            name.setVisibility(View.GONE);
            email.setBackgroundResource(R.drawable.login_only_input);
            host.setVisibility(View.GONE);
            nextBtn.setVisibility(View.GONE);
            enterpriseLearnMore.setVisibility(View.GONE);
            enterpriseHint.setVisibility(View.GONE);
            hostHint.setVisibility(View.GONE);
        }
    };

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (login != null && login.getVisibility() == View.VISIBLE) {
            if (email != null)
                state.putString("email", email.getText().toString());
            if (password != null)
                state.putString("password", password.getText().toString());
            if (host != null)
                state.putString("host", host.getText().toString());
            if (name != null)
                state.putString("name", name.getText().toString());
            if (signupBtn != null)
                state.putBoolean("signup", signupBtn.getVisibility() == View.VISIBLE);
            if (loginBtn != null)
                state.putBoolean("login", loginBtn.getVisibility() == View.VISIBLE);
            if (sendAccessLinkBtn != null)
                state.putBoolean("forgotPassword", sendAccessLinkBtn.getVisibility() == View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("screenlock", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        login_or_connect();
    }

    private void login_or_connect() {
        if (NetworkConnection.IRCCLOUD_HOST != null && NetworkConnection.IRCCLOUD_HOST.length() > 0 && getIntent() != null && getIntent().getData() != null && getIntent().getData().getPath().endsWith("/access-link")) {
            NetworkConnection.getInstance().logout();
            IRCCloudLog.Log("LOGOUT: Access Link launched");
            new AccessLinkTask().execute("https://" + NetworkConnection.IRCCLOUD_HOST + "/chat/access-link?" + getIntent().getData().getEncodedQuery().replace("&mobile=1", "") + "&format=json");
            setIntent(new Intent(this, LoginActivity.class));
        } else if (NetworkConnection.getInstance().session != null && NetworkConnection.getInstance().session.length() > 0) {
            Intent i = new Intent(LoginActivity.this, MainActivity.class);
            i.putExtra("nosplash", true);
            if (getIntent() != null) {
                if (getIntent().getData() != null)
                    i.setData(getIntent().getData());
                if (getIntent().getExtras() != null)
                    i.putExtras(getIntent().getExtras());
            }
            startActivity(i);
            finish();
        } else {
            if (loading != null)
                loading.setVisibility(View.GONE);
            if (connecting != null)
                connecting.setVisibility(View.GONE);
            if (login != null)
                login.setVisibility(View.VISIBLE);
        }
    }

    private void login() {
        if(!BuildConfig.ENTERPRISE)
            NetworkConnection.IRCCLOUD_HOST = BuildConfig.HOST;
        LoginTask task = new LoginTask();
        task.onPreExecute();
        NetworkConnection.getInstance().fetchConfig(new NetworkConnection.ConfigCallback() {
            @Override
            public void onConfig(JSONObject config) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(config != null) {
                            task.execute((Void) null);
                        } else {
                            try {
                                JSONObject result = new JSONObject();
                                result.put("message", "config");
                                task.onPostExecute(result);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });
    }

    private class LoginTask extends AsyncTaskEx<Void, Void, JSONObject> {
        @Override
        public void onPreExecute() {
            name.setEnabled(false);
            email.setEnabled(false);
            password.setEnabled(false);
            host.setEnabled(false);
            loginHint.setEnabled(false);
            signupHint.setEnabled(false);
            SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
            editor.putString("host", NetworkConnection.IRCCLOUD_HOST);
            editor.apply();
            loginBtn.setEnabled(false);
            signupBtn.setEnabled(false);
            if (name.getVisibility() == View.VISIBLE)
                connectingMsg.setText("Creating Account");
            else
                connectingMsg.setText("Signing in");
            progressBar.setIndeterminate(true);
            connecting.setVisibility(View.VISIBLE);
            login.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
        }

        @Override
        protected JSONObject doInBackground(Void... arg0) {
            if (name.getVisibility() == View.VISIBLE) {
                if (name.getText() != null && name.getText().length() > 0 && email.getText() != null && email.getText().length() > 0 && password.getText() != null && password.getText().length() > 0)
                    return NetworkConnection.getInstance().signup(name.getText().toString().trim(), email.getText().toString().trim(), password.getText().toString());
                else
                    return null;
            } else {
                if (email.getText() != null && email.getText().length() > 0 && password.getText() != null && password.getText().length() > 0)
                    return NetworkConnection.getInstance().login(email.getText().toString().trim(), password.getText().toString());
                else
                    return null;
            }
        }

        @Override
        public void onPostExecute(JSONObject result) {
            if (result != null && result.has("session")) {
                try {
                    NetworkConnection.getInstance().set_session(result.getString("session"));
                    if (result.has("websocket_path")) {
                        NetworkConnection.set_api_path(result.getString("websocket_path"));
                    }

                    if (result.has("api_host")) {
                        NetworkConnection.set_api_host(result.getString("api_host"));
                    }

                    final Intent i = new Intent(LoginActivity.this, MainActivity.class);
                    i.putExtra("nosplash", true);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (getIntent() != null) {
                        if (getIntent().getData() != null)
                            i.setData(getIntent().getData());
                        if (getIntent().getExtras() != null)
                            i.putExtras(getIntent().getExtras());
                    }

                    startActivity(i);
                    finish();
                } catch (JSONException e) {
                    NetworkConnection.printStackTraceToCrashlytics(e);
                }
            } else {
                name.setEnabled(true);
                email.setEnabled(true);
                password.setEnabled(true);
                host.setEnabled(true);
                loginBtn.setEnabled(true);
                signupBtn.setEnabled(true);
                loginHint.setEnabled(true);
                signupHint.setEnabled(true);
                connecting.setVisibility(View.GONE);
                login.setVisibility(View.VISIBLE);
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                if (name.getVisibility() == View.VISIBLE)
                    builder.setTitle("Sign Up Failed");
                else
                    builder.setTitle("Login Failed");
                String message = "Unable to connect to IRCCloud.  Please try again later.";
                if (result != null) {
                    try {
                        if (result.has("message")) {
                            IRCCloudLog.Log(Log.ERROR, "IRCCloud", "Failure: " + result.getString("message"));
                            message = result.getString("message");
                            if (message.equalsIgnoreCase("auth") || message.equalsIgnoreCase("email") || message.equalsIgnoreCase("password") || message.equalsIgnoreCase("legacy_account"))
                                if (name.getVisibility() == View.VISIBLE)
                                    message = "Invalid email address or password.  Please try again.";
                                else
                                    message = "Incorrect username or password.  Please try again.";
                            else if (message.equals("json_error"))
                                message = "Invalid response received from the server.  Please try again shortly.";
                            else if (message.equals("invalid_response"))
                                message = "Unexpected response received from the server.  Check your network settings and try again shortly.";
                            else if (message.equals("empty_response"))
                                message = "The server did not respond.  Check your network settings and try again shortly.";
                            else if (message.equals("realname"))
                                message = "Please enter a valid name and try again.";
                            else if (message.equals("email_exists"))
                                message = "This email address is already in use, please sign in or try another.";
                            else if (message.equals("rate_limited"))
                                message = "Rate limited, please try again in a few minutes.";
                            else if (message.equals("password_error"))
                                message = "Invalid password, try again.";
                            else if (message.equals("banned") || message.equals("ip_banned"))
                                message = "Signup server unavailable, please try again later.";
                            else if (message.equals("bad_email"))
                                message = "No signups allowed from that domain.";
                            else if (message.equals("tor_blocked"))
                                message = "No signups allowed from TOR exit nodes";
                            else if (message.equals("signup_ip_blocked"))
                                message = "Your IP address has been blacklisted";
                            else if (message.equals("config"))
                                message = "Unable to fetch configuration. Please try again shortly.";
                            else
                                message = "Error: " + message;
                        }
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        NetworkConnection.printStackTraceToCrashlytics(e);
                    }
                } else if ((name.getVisibility() == View.VISIBLE && (name.getText() == null || name.getText().length() == 0)) || email.getText() == null || email.getText().length() == 0 || password.getText() == null || password.getText().length() == 0) {
                    if (name.getVisibility() == View.VISIBLE)
                        message = "Please enter your name, email address, and password.";
                    else
                        message = "Please enter your username and password.";
                }
                builder.setMessage(message);
                builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setNeutralButton("Send Feedback", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        NetworkConnection.sendFeedbackReport(LoginActivity.this);
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.setOwnerActivity(LoginActivity.this);
                if(!isFinishing())
                    dialog.show();
            }
        }
    }

    private class AccessLinkTask extends AsyncTaskEx<String, Void, JSONObject> {
        @Override
        public void onPreExecute() {
            name.setEnabled(false);
            email.setEnabled(false);
            password.setEnabled(false);
            host.setEnabled(false);
            loginHint.setEnabled(false);
            signupHint.setEnabled(false);
            loginBtn.setEnabled(false);
            signupBtn.setEnabled(false);
            connectingMsg.setText("Signing in");
            progressBar.setIndeterminate(true);
            connecting.setVisibility(View.VISIBLE);
            login.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
        }

        @Override
        protected JSONObject doInBackground(String... arg0) {
            try {
                return NetworkConnection.getInstance().fetchJSON(arg0[0]);
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public void onPostExecute(JSONObject result) {
            if (result != null && result.has("session")) {
                try {
                    NetworkConnection.getInstance().set_session(result.getString("session"));
                    if (result.has("websocket_path")) {
                        NetworkConnection.set_api_path(result.getString("websocket_path"));
                    }

                    if (result.has("api_host")) {
                        NetworkConnection.set_api_host(result.getString("api_host"));
                    }

                    Intent i = new Intent(LoginActivity.this, MainActivity.class);
                    i.putExtra("nosplash", true);
                    if (getIntent() != null) {
                        if (getIntent().getData() != null)
                            i.setData(getIntent().getData());
                        if (getIntent().getExtras() != null)
                            i.putExtras(getIntent().getExtras());
                    }
                    startActivity(i);
                    finish();
                } catch (JSONException e) {
                    NetworkConnection.printStackTraceToCrashlytics(e);
                }
            } else {
                name.setEnabled(true);
                email.setEnabled(true);
                password.setEnabled(true);
                host.setEnabled(true);
                loginBtn.setEnabled(true);
                signupBtn.setEnabled(true);
                loginHint.setEnabled(true);
                signupHint.setEnabled(true);
                connecting.setVisibility(View.GONE);
                login.setVisibility(View.VISIBLE);
                AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                builder.setTitle("Login Failed");
                builder.setMessage("Invalid access link");
                builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.setOwnerActivity(LoginActivity.this);
                if(!isFinishing())
                    dialog.show();
            }
        }
    }

    private class ResetPasswordTask extends AsyncTaskEx<Void, Void, JSONObject> {
        @Override
        public void onPreExecute() {
            email.setEnabled(false);
            sendAccessLinkBtn.setEnabled(false);
            connectingMsg.setText("Requesting Access Link");
            progressBar.setIndeterminate(true);
            connecting.setVisibility(View.VISIBLE);
            login.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
        }

        @Override
        protected JSONObject doInBackground(Void... arg0) {
            return NetworkConnection.getInstance().request_password(email.getText().toString().trim());
        }

        @Override
        public void onPostExecute(JSONObject result) {
            email.setEnabled(true);
            sendAccessLinkBtn.setEnabled(true);
            progressBar.setIndeterminate(false);
            connecting.setVisibility(View.GONE);
            login.setVisibility(View.VISIBLE);
            try {
                if (result != null && result.has("success") && result.getBoolean("success")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                    builder.setTitle("Access Link");
                    builder.setMessage("We've sent you an access link.  Check your email and follow the instructions to sign in.");
                    builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            loginHintClickListener.onClick(null);
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.setOwnerActivity(LoginActivity.this);
                    if(!isFinishing())
                        dialog.show();
                    return;
                }
            } catch (JSONException e) {
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setTitle("Request Failed");
            builder.setMessage("Unable to request an access link.  Please try again later.");
            builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.setOwnerActivity(LoginActivity.this);
            if(!isFinishing())
                dialog.show();
        }
    }

    private void trimHost() {
        if (NetworkConnection.IRCCLOUD_HOST.startsWith("http://"))
            NetworkConnection.IRCCLOUD_HOST = NetworkConnection.IRCCLOUD_HOST.substring(7);
        if (NetworkConnection.IRCCLOUD_HOST.startsWith("https://"))
            NetworkConnection.IRCCLOUD_HOST = NetworkConnection.IRCCLOUD_HOST.substring(8);
        if (NetworkConnection.IRCCLOUD_HOST.endsWith("/"))
            NetworkConnection.IRCCLOUD_HOST = NetworkConnection.IRCCLOUD_HOST.substring(0, NetworkConnection.IRCCLOUD_HOST.length() - 1);
    }

    private class EnterpriseConfigTask extends AsyncTaskEx<Void, Void, JSONObject> {
        @Override
        public void onPreExecute() {
            host.setEnabled(false);
            connectingMsg.setText("Connecting");
            progressBar.setIndeterminate(true);
            connecting.setVisibility(View.VISIBLE);
            login.setVisibility(View.GONE);
            loading.setVisibility(View.GONE);
        }

        @Override
        protected JSONObject doInBackground(Void... arg0) {
            try {
                return NetworkConnection.getInstance().fetchJSON("https://" + NetworkConnection.IRCCLOUD_HOST + "/config");
            } catch (IOException e) {
                NetworkConnection.printStackTraceToCrashlytics(e);
                return null;
            }
        }

        @Override
        public void onPostExecute(JSONObject result) {
            host.setEnabled(true);
            progressBar.setIndeterminate(false);
            connecting.setVisibility(View.GONE);
            login.setVisibility(View.VISIBLE);
            try {
                if (result != null) {
                    NetworkConnection.IRCCLOUD_HOST = result.getString("api_host");
                    trimHost();

                    SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
                    editor.putString("host", NetworkConnection.IRCCLOUD_HOST);
                    editor.apply();

                    if (result.getString("auth_mechanism").equals("internal")) {
                        loginSignupHint.getChildAt(1).setVisibility(View.VISIBLE);
                        auth_url = null;
                        loginBtn.setText("Login");
                    } else if (result.getString("auth_mechanism").equals("saml")) {
                        loginBtn.setText("Login with " + result.getString("saml_provider"));
                        auth_url = "https://" + NetworkConnection.IRCCLOUD_HOST + "/saml/auth";
                        loginSignupHint.getChildAt(1).setVisibility(View.GONE);
                    } else {
                        loginSignupHint.getChildAt(1).setVisibility(View.GONE);
                        auth_url = null;
                        loginBtn.setText("Login");
                    }

                    if (result.get("enterprise") instanceof JSONObject && ((JSONObject) result.get("enterprise")).has("fullname"))
                        ((TextView) findViewById(R.id.enterpriseHintText)).setText(((JSONObject) result.get("enterprise")).getString("fullname"));

                    if (NetworkConnection.IRCCLOUD_HOST != null && NetworkConnection.IRCCLOUD_HOST.length() > 0 && getIntent() != null && getIntent().getData() != null && getIntent().getData().getPath().endsWith("/access-link")) {
                        NetworkConnection.getInstance().logout();
                        IRCCloudLog.Log("LOGOUT: Access Link launched");
                        new AccessLinkTask().execute("https://" + NetworkConnection.IRCCLOUD_HOST + "/chat/access-link?" + getIntent().getData().getEncodedQuery().replace("&mobile=1", "") + "&format=json");
                        setIntent(new Intent(LoginActivity.this, LoginActivity.class));
                    } else {
                        loginHintClickListener.onClick(null);
                        loading.setVisibility(View.VISIBLE);
                        connecting.setVisibility(View.GONE);
                        login.setVisibility(View.GONE);
                        login_or_connect();
                    }
                    return;
                }
            } catch (JSONException e) {
                NetworkConnection.printStackTraceToCrashlytics(e);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setTitle("Connection Failed");
            builder.setMessage("Please check your host and try again shortly, or contact your system administrator for assistance.");
            builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.setOwnerActivity(LoginActivity.this);
            if(!isFinishing())
                dialog.show();
        }
    }
}
