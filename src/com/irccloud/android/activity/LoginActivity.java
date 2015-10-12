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

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
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
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.LoginEvent;
import com.crashlytics.android.answers.SignUpEvent;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.BackgroundTaskService;
import com.irccloud.android.BuildConfig;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class LoginActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private View login = null;
    private View loading = null;
    private AutoCompleteTextView email;
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

    private String impression_id = null;
    private GoogleApiClient mGoogleApiClient;

    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final int REQUEST_RESOLVE_CREDENTIALS = 1002;
    private static final int REQUEST_RESOLVE_SAVE_CREDENTIALS = 1003;
    private boolean mResolvingError = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            Bitmap cloud = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.app_name), cloud, 0xff0b2e60));
            cloud.recycle();
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_login);

        loading = findViewById(R.id.loading);

        connecting = findViewById(R.id.connecting);
        connectingMsg = (TextView) findViewById(R.id.connectingMsg);
        progressBar = (ProgressBar) findViewById(R.id.connectingProgress);

        loginHint = (LinearLayout) findViewById(R.id.loginHint);
        signupHint = (LinearLayout) findViewById(R.id.signupHint);
        hostHint = (TextView) findViewById(R.id.hostHint);

        login = findViewById(R.id.login);
        name = (EditText) findViewById(R.id.name);
        if (savedInstanceState != null && savedInstanceState.containsKey("name"))
            name.setText(savedInstanceState.getString("name"));
        email = (AutoCompleteTextView) findViewById(R.id.email);
        if (BuildConfig.ENTERPRISE)
            email.setHint(R.string.email_enterprise);

        ArrayList<String> accounts = new ArrayList<String>();
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED) {
            AccountManager am = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
            for (Account a : am.getAccounts()) {
                if (a.name.contains("@") && !accounts.contains(a.name))
                    accounts.add(a.name);
            }
        }
        if (accounts.size() > 0)
            email.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, accounts.toArray(new String[accounts.size()])));

        if (savedInstanceState != null && savedInstanceState.containsKey("email"))
            email.setText(savedInstanceState.getString("email"));

        password = (EditText) findViewById(R.id.password);
        password.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    new LoginTask().execute((Void) null);
                    return true;
                }
                return false;
            }
        });
        if (savedInstanceState != null && savedInstanceState.containsKey("password"))
            password.setText(savedInstanceState.getString("password"));

        host = (EditText) findViewById(R.id.host);
        if (BuildConfig.ENTERPRISE)
            host.setText(NetworkConnection.IRCCLOUD_HOST);
        else
            host.setVisibility(View.GONE);
        host.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    new LoginTask().execute((Void) null);
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

        loginBtn = (Button) findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new LoginTask().execute((Void) null);
            }
        });
        loginBtn.setFocusable(true);
        loginBtn.requestFocus();

        sendAccessLinkBtn = (Button) findViewById(R.id.sendAccessLink);
        sendAccessLinkBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new ResetPasswordTask().execute((Void) null);
            }
        });

        nextBtn = (Button) findViewById(R.id.nextBtn);
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

        TOS = (TextView) findViewById(R.id.TOS);
        TOS.setMovementMethod(new LinkMovementMethod());

        forgotPassword = (TextView) findViewById(R.id.forgotPassword);
        forgotPassword.setOnClickListener(forgotPasswordClickListener);

        enterpriseLearnMore = (TextView) findViewById(R.id.enterpriseLearnMore);
        enterpriseLearnMore.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPackageInstalled("com.irccloud.android", LoginActivity.this)) {
                    startActivity(getPackageManager().getLaunchIntentForPackage("com.irccloud.android"));
                } else {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.irccloud.android")));
                    } catch (Exception e) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=com.irccloud.android")));
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
        enterpriseHint = (LinearLayout) findViewById(R.id.enterpriseHint);

        EnterYourEmail = (TextView) findViewById(R.id.enterYourEmail);

        signupHint.setOnClickListener(signupHintClickListener);
        loginHint.setOnClickListener(loginHintClickListener);

        signupBtn = (Button) findViewById(R.id.signupBtn);
        signupBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                new LoginTask().execute((Void) null);
            }
        });

        TextView version = (TextView) findViewById(R.id.version);
        try {
            version.setText("Version " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (NameNotFoundException e) {
            version.setVisibility(View.GONE);
        }

        Typeface SourceSansProRegular = Typeface.createFromAsset(getAssets(), "SourceSansPro-Regular.otf");
        Typeface SourceSansProLightItalic = Typeface.createFromAsset(getAssets(), "SourceSansPro-LightIt.otf");

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

        LinearLayout IRCCloud = (LinearLayout) findViewById(R.id.IRCCloud);
        for (int i = 0; i < IRCCloud.getChildCount(); i++) {
            View v = IRCCloud.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setTypeface(SourceSansProRegular);
            }
        }

        notAProblem = (LinearLayout) findViewById(R.id.notAProblem);
        for (int i = 0; i < notAProblem.getChildCount(); i++) {
            View v = notAProblem.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setTypeface((i == 0) ? SourceSansProRegular : SourceSansProLightItalic);
            }
        }

        loginSignupHint = (LinearLayout) findViewById(R.id.loginSignupHint);
        for (int i = 0; i < loginSignupHint.getChildCount(); i++) {
            View v = loginSignupHint.getChildAt(i);
            if (v instanceof TextView) {
                ((TextView) v).setTypeface(SourceSansProRegular);
                ((TextView) v).setOnClickListener((i == 0) ? loginHintClickListener : signupHintClickListener);
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

        mResolvingError = savedInstanceState != null && savedInstanceState.getBoolean("resolving_error", false);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.CREDENTIALS_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
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
            email.setVisibility(View.VISIBLE);
            password.setVisibility(View.VISIBLE);
            loginBtn.setVisibility(View.VISIBLE);
            signupBtn.setVisibility(View.GONE);
            email.requestFocus();
            TOS.setVisibility(View.GONE);
            loginHint.setVisibility(View.GONE);
            forgotPassword.setVisibility(View.VISIBLE);
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
        state.putBoolean("resolving_error", mResolvingError);
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
    }

    private void login_or_connect() {
        if (NetworkConnection.IRCCLOUD_HOST != null && NetworkConnection.IRCCLOUD_HOST.length() > 0 && getIntent() != null && getIntent().getData() != null && getIntent().getData().getPath().endsWith("/access-link")) {
            NetworkConnection.getInstance().logout();
            new AccessLinkTask().execute("https://" + NetworkConnection.IRCCLOUD_HOST + "/chat/access-link?" + getIntent().getData().getEncodedQuery().replace("&mobile=1", "") + "&format=json");
            setIntent(new Intent(this, LoginActivity.class));
        } else if (getIntent() != null && getIntent().getData() != null && getIntent().getData().getHost().equals("referral")) {
            new ImpressionTask().execute(getIntent().getDataString().substring(getIntent().getData().getScheme().length() + getIntent().getData().getHost().length() + 4));
            if (getSharedPreferences("prefs", 0).contains("session_key")) {
                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                i.putExtra("nosplash", true);
                startActivity(i);
                finish();
            }
        } else if (getSharedPreferences("prefs", 0).contains("session_key")) {
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
            if (host.getVisibility() == View.GONE && mGoogleApiClient.isConnected()) {
                Log.e("IRCCloud", "Play Services connected");
                CredentialRequest request = new CredentialRequest.Builder()
                        .setAccountTypes("https://" + NetworkConnection.IRCCLOUD_HOST)
                        .setSupportsPasswordLogin(true)
                        .build();

                Auth.CredentialsApi.request(mGoogleApiClient, request).setResultCallback(new ResultCallback<CredentialRequestResult>() {
                    @Override
                    public void onResult(CredentialRequestResult result) {
                        if (result.getStatus().isSuccess()) {
                            Log.e("IRCCloud", "Credentials request succeeded");
                            email.setText(result.getCredential().getId());
                            password.setText(result.getCredential().getPassword());
                            loginHintClickListener.onClick(null);
                            new LoginTask().execute((Void) null);
                        } else if (result.getStatus().getStatusCode() == CommonStatusCodes.SIGN_IN_REQUIRED) {
                            Log.e("IRCCloud", "Credentials request sign in");
                            loading.setVisibility(View.GONE);
                            connecting.setVisibility(View.GONE);
                            login.setVisibility(View.VISIBLE);
                        } else if (result.getStatus().hasResolution()) {
                            Log.e("IRCCloud", "Credentials request requires resolution");
                            try {
                                startIntentSenderForResult(result.getStatus().getResolution().getIntentSender(), REQUEST_RESOLVE_CREDENTIALS, null, 0, 0, 0);
                            } catch (IntentSender.SendIntentException e) {
                                e.printStackTrace();
                                loading.setVisibility(View.GONE);
                                connecting.setVisibility(View.GONE);
                                login.setVisibility(View.VISIBLE);
                            }
                        } else {
                            Log.e("IRCCloud", "Credentials request failed");
                            loading.setVisibility(View.GONE);
                            connecting.setVisibility(View.GONE);
                            login.setVisibility(View.VISIBLE);
                        }
                    }
                });
            } else {
                loading.setVisibility(View.GONE);
                connecting.setVisibility(View.GONE);
                login.setVisibility(View.VISIBLE);
            }
        }
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
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        login_or_connect();
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
            login_or_connect();
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
        } else if (requestCode == REQUEST_RESOLVE_CREDENTIALS) {
            if (resultCode == RESULT_OK && data.hasExtra(Credential.EXTRA_KEY)) {
                Credential c = data.getParcelableExtra(Credential.EXTRA_KEY);
                name.setText(c.getName());
                email.setText(c.getId());
                password.setText(c.getPassword());
                loading.setVisibility(View.GONE);
                login.setVisibility(View.VISIBLE);
                loginHintClickListener.onClick(null);
                new LoginTask().execute((Void) null);
            } else {
                loading.setVisibility(View.GONE);
                login.setVisibility(View.VISIBLE);
            }
        } else if (requestCode == REQUEST_RESOLVE_SAVE_CREDENTIALS) {
            if (resultCode == RESULT_OK) {
                Log.e("IRCCloud", "Credentials result: OK");
            }
            Intent i = new Intent(LoginActivity.this, MainActivity.class);
            i.putExtra("nosplash", true);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            if (getIntent() != null) {
                if (getIntent().getData() != null)
                    i.setData(getIntent().getData());
                if (getIntent().getExtras() != null)
                    i.putExtras(getIntent().getExtras());
            }
            startActivity(i);
            finish();
        }
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
            editor.commit();
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
            try {
                if (!BuildConfig.ENTERPRISE)
                    NetworkConnection.IRCCLOUD_HOST = BuildConfig.HOST;
                JSONObject config = NetworkConnection.getInstance().fetchConfig();
                NetworkConnection.IRCCLOUD_HOST = config.getString("api_host");
                trimHost();
            } catch (Exception e) {
                return null;
            }
            if (name.getVisibility() == View.VISIBLE) {
                if (name.getText() != null && name.getText().length() > 0 && email.getText() != null && email.getText().length() > 0 && password.getText() != null && password.getText().length() > 0)
                    return NetworkConnection.getInstance().signup(name.getText().toString(), email.getText().toString(), password.getText().toString(), (impression_id != null) ? impression_id : "");
                else
                    return null;
            } else {
                if (email.getText() != null && email.getText().length() > 0 && password.getText() != null && password.getText().length() > 0)
                    return NetworkConnection.getInstance().login(email.getText().toString(), password.getText().toString());
                else
                    return null;
            }
        }

        @Override
        public void onPostExecute(JSONObject result) {
            if (result != null && result.has("session")) {
                try {
                    NetworkConnection.getInstance().session = result.getString("session");
                    SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
                    editor.putString("session_key", result.getString("session"));
                    if (result.has("websocket_host")) {
                        NetworkConnection.IRCCLOUD_HOST = result.getString("websocket_host");
                        NetworkConnection.IRCCLOUD_PATH = result.getString("websocket_path");
                    }
                    editor.putString("host", NetworkConnection.IRCCLOUD_HOST);
                    editor.putString("path", NetworkConnection.IRCCLOUD_PATH);
                    editor.commit();

                    final Intent i = new Intent(LoginActivity.this, MainActivity.class);
                    i.putExtra("nosplash", true);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    if (getIntent() != null) {
                        if (getIntent().getData() != null)
                            i.setData(getIntent().getData());
                        if (getIntent().getExtras() != null)
                            i.putExtras(getIntent().getExtras());
                    }

                    if (mGoogleApiClient.isConnected()) {
                        Credential.Builder builder = new Credential.Builder(email.getText().toString()).setPassword(password.getText().toString());
                        if (name.getText() != null && name.getText().length() > 0)
                            builder.setName(name.getText().toString());
                        Auth.CredentialsApi.save(mGoogleApiClient, builder.build()).setResultCallback(new ResultCallback<com.google.android.gms.common.api.Status>() {
                            @Override
                            public void onResult(com.google.android.gms.common.api.Status status) {
                                if (status.isSuccess()) {
                                    Log.e("IRCCloud", "Credentials saved");
                                    startActivity(i);
                                    finish();
                                } else if (status.hasResolution()) {
                                    Log.e("IRCCloud", "Credentials require resolution");
                                    try {
                                        startIntentSenderForResult(status.getResolution().getIntentSender(), REQUEST_RESOLVE_SAVE_CREDENTIALS, null, 0, 0, 0);
                                    } catch (IntentSender.SendIntentException e) {
                                        e.printStackTrace();
                                        startActivity(i);
                                        finish();
                                    }
                                } else {
                                    Log.e("IRCCloud", "Credentials request failed");
                                    startActivity(i);
                                    finish();
                                }
                            }
                        });
                        if(BuildConfig.GCM_ID.length() > 0)
                            BackgroundTaskService.registerGCM(LoginActivity.this);
                    } else {
                        startActivity(i);
                        finish();
                    }

                    if (!BuildConfig.ENTERPRISE) {
                        if (name.getVisibility() == View.VISIBLE)
                            Answers.getInstance().logSignUp(new SignUpEvent().putMethod("email").putSuccess(true));
                        else
                            Answers.getInstance().logLogin(new LoginEvent().putMethod("email").putSuccess(true));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
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
                builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
                if (name.getVisibility() == View.VISIBLE)
                    builder.setTitle("Sign Up Failed");
                else
                    builder.setTitle("Login Failed");
                String message = "Unable to connect to IRCCloud.  Please try again later.";
                if (result != null) {
                    try {
                        if (result.has("message")) {
                            if (!BuildConfig.ENTERPRISE) {
                                if (name.getVisibility() == View.VISIBLE)
                                    Answers.getInstance().logSignUp(new SignUpEvent().putMethod("email").putSuccess(false).putCustomAttribute("Failure", result.getString("message")));
                                else
                                    Answers.getInstance().logLogin(new LoginEvent().putMethod("email").putSuccess(false).putCustomAttribute("Failure", result.getString("message")));
                            }
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
                            else
                                message = "Error: " + message;
                        }

                        if (mGoogleApiClient.isConnected()) {
                            Auth.CredentialsApi.delete(mGoogleApiClient, new Credential.Builder(email.getText().toString()).setPassword(password.getText().toString()).build()).setResultCallback(new ResultCallback<com.google.android.gms.common.api.Status>() {
                                @Override
                                public void onResult(com.google.android.gms.common.api.Status status) {
                                }
                            });
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if ((name.getVisibility() == View.VISIBLE && (name.getText() == null || name.getText().length() == 0)) || email.getText() == null || email.getText().length() == 0 || password.getText() == null || password.getText().length() == 0) {
                    if (name.getVisibility() == View.VISIBLE)
                        message = "Please enter your name, email address, and password.";
                    else
                        message = "Please enter your username and password.";
                } else {
                    if (!BuildConfig.ENTERPRISE) {
                        if (name.getVisibility() == View.VISIBLE)
                            Answers.getInstance().logSignUp(new SignUpEvent().putMethod("email").putSuccess(false));
                        else
                            Answers.getInstance().logLogin(new LoginEvent().putMethod("email").putSuccess(false));
                    }
                }
                builder.setMessage(message);
                builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.setOwnerActivity(LoginActivity.this);
                try {
                    dialog.show();
                } catch (WindowManager.BadTokenException e) {
                }
            }
        }
    }

    private class ImpressionTask extends AsyncTaskEx<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... arg0) {
            try {
                return NetworkConnection.getInstance().impression(AdvertisingIdClient.getAdvertisingIdInfo(LoginActivity.this).getId(), arg0[0], getSharedPreferences("prefs", 0).getString("session_key", ""));
            } catch (IOException e) {
            } catch (GooglePlayServicesNotAvailableException e) {
                e.printStackTrace();
            } catch (GooglePlayServicesRepairableException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void onPostExecute(JSONObject result) {
            if (result != null && result.has("success")) {
                try {
                    if (result.getBoolean("success")) {
                        impression_id = result.getString("id");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
                    SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
                    editor.putString("session_key", result.getString("session"));
                    if (result.has("websocket_host")) {
                        NetworkConnection.IRCCLOUD_HOST = result.getString("websocket_host");
                        NetworkConnection.IRCCLOUD_PATH = result.getString("websocket_path");
                    }
                    editor.putString("host", NetworkConnection.IRCCLOUD_HOST);
                    editor.putString("path", NetworkConnection.IRCCLOUD_PATH);
                    editor.commit();
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
                    if (!BuildConfig.ENTERPRISE)
                        Answers.getInstance().logLogin(new LoginEvent().putMethod("access-link").putSuccess(true));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                if (!BuildConfig.ENTERPRISE)
                    Answers.getInstance().logLogin(new LoginEvent().putMethod("access-link").putSuccess(false));
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
                builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
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
                try {
                    dialog.show();
                } catch (WindowManager.BadTokenException e) {
                }
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
            return NetworkConnection.getInstance().request_password(email.getText().toString());
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
                    builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
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
                    try {
                        dialog.show();
                    } catch (WindowManager.BadTokenException e) {
                    }
                    return;
                }
            } catch (JSONException e) {
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
            builder.setTitle("Reset Failed");
            builder.setMessage("Unable to request an access link.  Please try again later.");
            builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.setOwnerActivity(LoginActivity.this);
            try {
                dialog.show();
            } catch (WindowManager.BadTokenException e) {
            }
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
                e.printStackTrace();
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
                    editor.commit();

                    if (result.getString("auth_mechanism").equals("internal"))
                        loginSignupHint.getChildAt(1).setVisibility(View.VISIBLE);
                    else
                        loginSignupHint.getChildAt(1).setVisibility(View.GONE);

                    if (result.get("enterprise") instanceof JSONObject && ((JSONObject) result.get("enterprise")).has("fullname"))
                        ((TextView) findViewById(R.id.enterpriseHintText)).setText(((JSONObject) result.get("enterprise")).getString("fullname"));

                    if (NetworkConnection.IRCCLOUD_HOST != null && NetworkConnection.IRCCLOUD_HOST.length() > 0 && getIntent() != null && getIntent().getData() != null && getIntent().getData().getPath().endsWith("/access-link")) {
                        NetworkConnection.getInstance().logout();
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
                e.printStackTrace();
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
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
            try {
                dialog.show();
            } catch (WindowManager.BadTokenException e) {
            }
        }
    }
}
