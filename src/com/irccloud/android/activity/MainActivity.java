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

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.BuildConfig;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.ServersDataSource;

public class MainActivity extends FragmentActivity implements NetworkConnection.IRCEventHandler {
	private View login = null;
	private AutoCompleteTextView email;
	private EditText password;
    private EditText host;
    private EditText name;
	private Button loginBtn;
    private Button signupBtn;
    private Button nextBtn;
    private Button sendAccessLinkBtn;
	private NetworkConnection conn;
	private TextView errorMsg = null;
	private TextView connectingMsg = null;
    private TextView TOS = null;
    private TextView forgotPassword = null;
    private TextView enterpriseLearnMore = null;
    private TextView EnterYourEmail = null;
    private LinearLayout notAProblem = null;
	private ProgressBar progressBar = null;
	private static final Timer countdownTimer = new Timer("main-countdown-timer");
    private TimerTask countdownTimerTask = null;
	private String error = null;
	private View connecting = null;
    private LinearLayout loginHint = null;
    private LinearLayout signupHint = null;
    private LinearLayout enterpriseHint = null;
    private LinearLayout loginSignupHint = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		getWindow().setBackgroundDrawable(null);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.activity_main);

        connecting = findViewById(R.id.connecting);
		errorMsg = (TextView)findViewById(R.id.errorMsg);
		connectingMsg = (TextView)findViewById(R.id.connectingMsg);
		progressBar = (ProgressBar)findViewById(R.id.connectingProgress);

        loginHint = (LinearLayout)findViewById(R.id.loginHint);
        signupHint = (LinearLayout)findViewById(R.id.signupHint);

        login = findViewById(R.id.login);
        name = (EditText)findViewById(R.id.name);
        if(savedInstanceState != null && savedInstanceState.containsKey("name"))
            name.setText(savedInstanceState.getString("name"));
        email = (AutoCompleteTextView)findViewById(R.id.email);
        if(BuildConfig.ENTERPRISE)
            email.setHint(R.string.email_enterprise);
        ArrayList<String> accounts = new ArrayList<String>();
        AccountManager am = (AccountManager)getSystemService(Context.ACCOUNT_SERVICE);
        for(Account a : am.getAccounts()) {
        	if(a.name.contains("@") && !accounts.contains(a.name))
        		accounts.add(a.name);
        }
        if(accounts.size() > 0)
        	email.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, accounts.toArray(new String[accounts.size()])));

        if(savedInstanceState != null && savedInstanceState.containsKey("email"))
        	email.setText(savedInstanceState.getString("email"));
        
        password = (EditText)findViewById(R.id.password);
        password.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					new LoginTask().execute((Void)null);
					return true;
				}
				return false;
			}
        });
        if(savedInstanceState != null && savedInstanceState.containsKey("password"))
        	password.setText(savedInstanceState.getString("password"));

        host = (EditText)findViewById(R.id.host);
        if(BuildConfig.ENTERPRISE)
            host.setText(NetworkConnection.IRCCLOUD_HOST);
        else
            host.setVisibility(View.GONE);
        host.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    new LoginTask().execute((Void)null);
                    return true;
                }
                return false;
            }
        });
        if(savedInstanceState != null && savedInstanceState.containsKey("host"))
            host.setText(savedInstanceState.getString("host"));
        else
            host.setText(getSharedPreferences("prefs", 0).getString("host", BuildConfig.HOST));

        if(host.getText().toString().equals("api.irccloud.com") || host.getText().toString().equals("www.irccloud.com"))
            host.setText("");

        loginBtn = (Button)findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new LoginTask().execute((Void)null);
			}
        });
        loginBtn.setFocusable(true);
    	loginBtn.requestFocus();

        sendAccessLinkBtn = (Button)findViewById(R.id.sendAccessLink);
        nextBtn = (Button)findViewById(R.id.nextBtn);
        nextBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(host.getText().length() > 0) {
                    loginHintClickListener.onClick(view);
                }
            }
        });

        TOS = (TextView)findViewById(R.id.TOS);
        TOS.setMovementMethod(new LinkMovementMethod());

        forgotPassword = (TextView)findViewById(R.id.forgotPassword);
        forgotPassword.setOnClickListener(forgotPasswordClickListener);
        forgotPassword.setMovementMethod(new LinkMovementMethod() {
            public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
                forgotPasswordClickListener.onClick(widget);
                return false;
            }
        });

        enterpriseLearnMore = (TextView)findViewById(R.id.enterpriseLearnMore);
        enterpriseLearnMore.setMovementMethod(new LinkMovementMethod());
        enterpriseHint = (LinearLayout)findViewById(R.id.enterpriseHint);

        EnterYourEmail = (TextView)findViewById(R.id.enterYourEmail);

        signupHint.setOnClickListener(signupHintClickListener);
        loginHint.setOnClickListener(loginHintClickListener);

        signupBtn = (Button)findViewById(R.id.signupBtn);
        signupBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
            new LoginTask().execute((Void)null);
            }
        });

        TextView version = (TextView)findViewById(R.id.version);
        try {
            version.setText("Version " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			version.setVisibility(View.GONE);
		}

        Typeface LatoRegular = Typeface.createFromAsset(getAssets(), "Lato-Regular.ttf");
        Typeface LatoLightItalic = Typeface.createFromAsset(getAssets(), "Lato-LightItalic.ttf");

        for (int i=0; i < signupHint.getChildCount(); i++){
            View v = signupHint.getChildAt(i);
            if(v instanceof TextView) {
                ((TextView)v).setTypeface(LatoRegular);
            }
        }

        for (int i=0; i < loginHint.getChildCount(); i++){
            View v = loginHint.getChildAt(i);
            if(v instanceof TextView) {
                ((TextView)v).setTypeface(LatoRegular);
            }
        }

        LinearLayout IRCCloud = (LinearLayout)findViewById(R.id.IRCCloud);
        for (int i=0; i < IRCCloud.getChildCount(); i++){
            View v = IRCCloud.getChildAt(i);
            if(v instanceof TextView) {
                ((TextView)v).setTypeface(LatoRegular);
            }
        }

        notAProblem = (LinearLayout)findViewById(R.id.notAProblem);
        for (int i=0; i < notAProblem.getChildCount(); i++){
            View v = notAProblem.getChildAt(i);
            if(v instanceof TextView) {
                ((TextView)v).setTypeface((i==0)?LatoRegular:LatoLightItalic);
            }
        }

        loginSignupHint = (LinearLayout)findViewById(R.id.loginSignupHint);
        for (int i=0; i < loginSignupHint.getChildCount(); i++){
            View v = loginSignupHint.getChildAt(i);
            if(v instanceof TextView) {
                ((TextView)v).setTypeface(LatoRegular);
                ((TextView)v).setOnClickListener((i==0)?loginHintClickListener:signupHintClickListener);
            }
        }

        name.setTypeface(LatoRegular);
        email.setTypeface(LatoRegular);
        password.setTypeface(LatoRegular);
        host.setTypeface(LatoRegular);
        loginBtn.setTypeface(LatoRegular);
        signupBtn.setTypeface(LatoRegular);
        TOS.setTypeface(LatoRegular);
        EnterYourEmail.setTypeface(LatoRegular);

        if(BuildConfig.ENTERPRISE) {
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
            host.requestFocus();
        }

        if(savedInstanceState != null && savedInstanceState.containsKey("signup") && savedInstanceState.getBoolean("signup")) {
            signupHintClickListener.onClick(null);
        }

        if(savedInstanceState != null && savedInstanceState.containsKey("login") && savedInstanceState.getBoolean("login")) {
            loginHintClickListener.onClick(null);
        }

        if(savedInstanceState != null && savedInstanceState.containsKey("forgotPassword") && savedInstanceState.getBoolean("forgotPassword")) {
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
            signupHint.setVisibility(View.VISIBLE);
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
        }
    };

    @Override
    public void onSaveInstanceState(Bundle state) {
    	super.onSaveInstanceState(state);
    	if(login != null && login.getVisibility() == View.VISIBLE) {
	    	if(email != null)
	    		state.putString("email", email.getText().toString());
	    	if(password != null)
	    		state.putString("password", password.getText().toString());
            if(host != null)
                state.putString("host", host.getText().toString());
            if(name != null)
                state.putString("name", name.getText().toString());
            if(signupBtn != null)
                state.putBoolean("signup", signupBtn.getVisibility() == View.VISIBLE);
            if(loginBtn != null)
                state.putBoolean("login", loginBtn.getVisibility() == View.VISIBLE);
            if(sendAccessLinkBtn != null)
                state.putBoolean("forgotPassword", sendAccessLinkBtn.getVisibility() == View.VISIBLE);
    	}
    }

    @Override
    public void onPause() {
    	super.onPause();
    	if(conn != null)
    		conn.removeHandler(this);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if(prefs.getBoolean("screenlock", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

    	conn = NetworkConnection.getInstance();
    	if(conn.ready) {
            if(ServersDataSource.getInstance().count() > 0) {
                Intent i = new Intent(MainActivity.this, MessageActivity.class);
                if(getIntent() != null) {
                    if(getIntent().getData() != null)
                        i.setData(getIntent().getData());
                    if(getIntent().getExtras() != null)
                        i.putExtras(getIntent().getExtras());
                }
                startActivity(i);
            } else {
                startActivity(new Intent(MainActivity.this, EditConnectionActivity.class));
            }
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    		finish();
    	} else {
    		conn.addHandler(this);
    		if(getSharedPreferences("prefs", 0).contains("session_key")) {
    			connecting.setVisibility(View.VISIBLE);
    			login.setVisibility(View.GONE);
                signupHint.setVisibility(View.GONE);
                loginHint.setVisibility(View.GONE);
    			conn.connect(getSharedPreferences("prefs", 0).getString("session_key", ""));
    			updateReconnecting();
    		} else {
    			connecting.setVisibility(View.GONE);
    			login.setVisibility(View.VISIBLE);
                checkPlayServices();
    		}
    	}
    }

    private void updateReconnecting() {
		if(conn.getState() == NetworkConnection.STATE_CONNECTED) {
			connectingMsg.setText("Loading");
		} else if(conn.getState() == NetworkConnection.STATE_CONNECTING || conn.getReconnectTimestamp() > 0) {
			progressBar.setIndeterminate(true);
			if(conn.getState() == NetworkConnection.STATE_DISCONNECTED && conn.getReconnectTimestamp() > 0) {
	    		String plural = "";
	    		int seconds = (int)((conn.getReconnectTimestamp() - System.currentTimeMillis()) / 1000);
	    		if(seconds != 1)
	    			plural = "s";
	    		if(seconds < 1) {
	    			connectingMsg.setText("Connecting");
					errorMsg.setVisibility(View.GONE);
	    		} else {
                    connectingMsg.setText("Reconnecting in " + seconds + " second" + plural);
                    if(error != null) {
                        errorMsg.setText(error);
                        errorMsg.setVisibility(View.VISIBLE);
                    }
                }
                if(countdownTimerTask != null)
                    countdownTimerTask.cancel();
                countdownTimerTask =  new TimerTask(){
                    public void run() {
                        if(conn.getState() == NetworkConnection.STATE_DISCONNECTED) {
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

    private void launchMessageActivity() {
        if(ServersDataSource.getInstance().count() > 0) {
            Intent i = new Intent(MainActivity.this, MessageActivity.class);
            if(getIntent() != null) {
                if(getIntent().getData() != null)
                    i.setData(getIntent().getData());
                if(getIntent().getExtras() != null)
                    i.putExtras(getIntent().getExtras());
            }
            startActivity(i);
        } else {
            startActivity(new Intent(MainActivity.this, EditConnectionActivity.class));
        }
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        finish();
    }

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
				final float progress = (Float)obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(progressBar.getProgress() < progress) {
                            progressBar.setIndeterminate(false);
                            progressBar.setProgress((int)progress);
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
				if(conn.ready) {
                    if(conn.getUserInfo() != null && !conn.getUserInfo().verified) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setTitle("Confirm your Email");
                                builder.setMessage("You should shortly receive an email with a link to confirm your address.\n\nIf you’re still waiting for the email, you can send yourself another confirmation.");
                                builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            dialog.dismiss();
                                            launchMessageActivity();
                                        } catch (IllegalArgumentException e) {
                                        }
                                    }
                                });
                                builder.setNeutralButton("Send Again", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        try {
                                            conn.resend_verify_email();
                                            dialog.dismiss();
                                            Toast.makeText(MainActivity.this, "Resent Confirmation.", Toast.LENGTH_SHORT).show();
                                            launchMessageActivity();
                                        } catch (IllegalArgumentException e) {
                                        }
                                    }
                                });
                                AlertDialog dialog = builder.create();
                                dialog.setOwnerActivity(MainActivity.this);
                                dialog.show();
                            }
                        });
                    } else {
                        launchMessageActivity();
                    }
				}
				break;
			case NetworkConnection.EVENT_CONNECTIVITY:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateReconnecting();
                    }
                });
				break;
			case NetworkConnection.EVENT_FAILURE_MSG:
				final IRCCloudJSONObject o = (IRCCloudJSONObject)obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            error = o.getString("message");
                            if(error.equals("auth")) {
                                conn.logout();
                                connecting.setVisibility(View.GONE);
                                login.setVisibility(View.VISIBLE);
                                return;
                            }

                            if(error.equals("set_shard")) {
                                conn.disconnect();
                                conn.ready = false;
                                SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
                                editor.putString("session_key", o.getString("cookie"));
                                conn.connect(o.getString("cookie"));
                                editor.commit();
                                return;
                            }

                            if(error.equals("temp_unavailable"))
                                error = "Your account is temporarily unavailable";
                            updateReconnecting();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                break;
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
            if(BuildConfig.ENTERPRISE) {
                NetworkConnection.IRCCLOUD_HOST = host.getText().toString();
                if(NetworkConnection.IRCCLOUD_HOST.startsWith("http://"))
                    NetworkConnection.IRCCLOUD_HOST = NetworkConnection.IRCCLOUD_HOST.substring(7);
                if(NetworkConnection.IRCCLOUD_HOST.startsWith("https://"))
                    NetworkConnection.IRCCLOUD_HOST = NetworkConnection.IRCCLOUD_HOST.substring(8);
                if(NetworkConnection.IRCCLOUD_HOST.endsWith("/"))
                    NetworkConnection.IRCCLOUD_HOST = NetworkConnection.IRCCLOUD_HOST.substring(0, NetworkConnection.IRCCLOUD_HOST.length() - 1);
            }
            SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
            editor.putString("host", NetworkConnection.IRCCLOUD_HOST);
            editor.commit();
			loginBtn.setEnabled(false);
            signupBtn.setEnabled(false);
            if(name.getVisibility() == View.VISIBLE)
    			connectingMsg.setText("Creating Account");
            else
                connectingMsg.setText("Signing in");
			progressBar.setIndeterminate(true);
			connecting.setVisibility(View.VISIBLE);
            login.setVisibility(View.GONE);
		}
		
		@Override
		protected JSONObject doInBackground(Void... arg0) {
            if(name.getVisibility() == View.VISIBLE) {
                if (name.getText() != null && name.getText().length() > 0 && email.getText() != null && email.getText().length() > 0 && password.getText() != null && password.getText().length() > 0)
                    return NetworkConnection.getInstance().signup(name.getText().toString(), email.getText().toString(), password.getText().toString());
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
			if(result != null && result.has("session")) {
				try {
					SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
					editor.putString("session_key", result.getString("session"));
                    if(result.has("websocket_host")) {
                        NetworkConnection.IRCCLOUD_HOST = result.getString("websocket_host");
                        NetworkConnection.IRCCLOUD_PATH = result.getString("websocket_path");
                    }
                    editor.putString("host", NetworkConnection.IRCCLOUD_HOST);
                    editor.putString("path", NetworkConnection.IRCCLOUD_PATH);
					login.setVisibility(View.GONE);
                    signupHint.setVisibility(View.GONE);
                    loginHint.setVisibility(View.GONE);
					InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(password.getWindowToken(), 0);
					conn.addHandler(MainActivity.this);
					conn.connect(result.getString("session"));
					editor.commit();
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
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                if(name.getVisibility() == View.VISIBLE)
    				builder.setTitle("Sign Up Failed");
                else
                    builder.setTitle("Login Failed");
				String message = "Unable to connect to IRCCloud.  Please try again later.";
				if(result != null) {
					try {
                        if(result.has("message")) {
                            message = result.getString("message");
                            if(message.equalsIgnoreCase("auth") || message.equalsIgnoreCase("email") || message.equalsIgnoreCase("password") || message.equalsIgnoreCase("legacy_account"))
                                if(name.getVisibility() == View.VISIBLE)
                                    message = "Invalid email address or password.  Please try again.";
                                else
                                    message = "Incorrect username or password.  Please try again.";
                            else if(message.equals("json_error"))
                                message = "Invalid response received from the server.  Please try again shortly.";
                            else if(message.equals("invalid_response"))
                                message = "Unexpected response received from the server.  Check your network settings and try again shortly.";
                            else if(message.equals("empty_response"))
                                message = "The server did not respond.  Check your network settings and try again shortly.";
                            else if(message.equals("realname"))
                                message = "Please enter a valid name and try again.";
                            else if(message.equals("email_exists"))
                                message = "This email address is already in use, please sign in or try another.";
                            else if(message.equals("rate_limited"))
                                message = "Rate limited, please try again in a few minutes.";
                            else
                                message = "Error: " + message;
                        }
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else if((name.getVisibility() == View.VISIBLE && (name.getText() == null || name.getText().length() == 0)) || email.getText() == null || email.getText().length() == 0 || password.getText() == null || password.getText().length() == 0) {
                    if(name.getVisibility() == View.VISIBLE)
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
				AlertDialog dialog = builder.create();
				dialog.setOwnerActivity(MainActivity.this);
				try {
					dialog.show();
				} catch (WindowManager.BadTokenException e) {
				}
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
}
