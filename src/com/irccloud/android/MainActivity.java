package com.irccloud.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONException;
import org.json.JSONObject;

import com.actionbarsherlock.app.SherlockActivity;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class MainActivity extends SherlockActivity {
	private View login = null;
	private AutoCompleteTextView email;
	private EditText password;
	private Button loginBtn;
	private NetworkConnection conn;
	private TextView errorMsg = null;
	private TextView connectingMsg = null;
	private ProgressBar progressBar = null;
	private Timer countdownTimer = null;
	private String error = null;
	private View connecting = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        if(getSupportActionBar() != null)
        	getSupportActionBar().hide();
        
        connecting = findViewById(R.id.connecting);
		errorMsg = (TextView)findViewById(R.id.errorMsg);
		connectingMsg = (TextView)findViewById(R.id.connectingMsg);
		progressBar = (ProgressBar)findViewById(R.id.connectingProgress);

        login = findViewById(R.id.login);
        email = (AutoCompleteTextView)findViewById(R.id.email);
        ArrayList<String> accounts = new ArrayList<String>();
        AccountManager am = (AccountManager)getSystemService(Context.ACCOUNT_SERVICE);
        for(Account a : am.getAccounts()) {
        	if(a.name.contains("@") && !accounts.contains(a.name))
        		accounts.add(a.name);
        }
        if(accounts.size() > 0)
        	email.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, accounts.toArray(new String[accounts.size()])));
        
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
        loginBtn = (Button)findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new LoginTask().execute((Void)null);
			}
        });
    }

    @Override
    public void onPause() {
    	super.onPause();
    	if(conn != null)
    		conn.removeHandler(mHandler);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	conn = NetworkConnection.getInstance();
    	if(conn.ready) {
    		Intent i = new Intent(MainActivity.this, MessageActivity.class);
    		if(getIntent() != null) {
    			if(getIntent().getData() != null)
    				i.setData(getIntent().getData());
    			if(getIntent().getExtras() != null)
    				i.putExtras(getIntent().getExtras());
    		}
    		startActivity(i);
    		finish();
    	} else {
    		conn.addHandler(mHandler);
    		if(getSharedPreferences("prefs", 0).contains("session_key")) {
    			connecting.setVisibility(View.VISIBLE);
    			login.setVisibility(View.GONE);
    			conn.connect(getSharedPreferences("prefs", 0).getString("session_key", ""));
    		} else {
    			connecting.setVisibility(View.GONE);
    			login.setVisibility(View.VISIBLE);
				email.requestFocus();
		    	getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
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
	    		} else if(seconds > 10 && error != null) {
	    			connectingMsg.setText("Reconnecting in " + seconds + " second" + plural);
					errorMsg.setText(error);
					errorMsg.setVisibility(View.VISIBLE);
	    		} else {
					connectingMsg.setText("Reconnecting in " + seconds + " second" + plural);
					errorMsg.setVisibility(View.GONE);
					error = null;
	    		}
				if(countdownTimer != null)
					countdownTimer.cancel();
				countdownTimer = new Timer();
				countdownTimer.schedule( new TimerTask(){
		             public void run() {
		    			 if(conn.getState() == NetworkConnection.STATE_DISCONNECTED) {
		    				 mHandler.post(new Runnable() {
								@Override
								public void run() {
				 					updateReconnecting();
								}
		    				 });
		    			 }
		    			 countdownTimer = null;
		             }
				}, 1000);
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
    
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_PROGRESS:
				float progress = (Float)msg.obj;
				progressBar.setIndeterminate(false);
				progressBar.setProgress((int)progress);
				break;
			case NetworkConnection.EVENT_BACKLOG_END:
	    		Intent i = new Intent(MainActivity.this, MessageActivity.class);
	    		if(getIntent() != null) {
	    			if(getIntent().getData() != null)
	    				i.setData(getIntent().getData());
	    			if(getIntent().getExtras() != null)
	    				i.putExtras(getIntent().getExtras());
	    		}
	    		startActivity(i);
	    		finish();
				break;
			case NetworkConnection.EVENT_CONNECTIVITY:
				updateReconnecting();
				break;
			case NetworkConnection.EVENT_FAILURE_MSG:
				IRCCloudJSONObject o = (IRCCloudJSONObject)msg.obj;
				try {
					error = o.getString("message");
					if(error.equals("temp_unavailable"))
						error = "Your account is temporarily unavailable";
					updateReconnecting();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
		}
	};
    
    private class LoginTask extends AsyncTaskEx<Void, Void, JSONObject> {
		@Override
		public void onPreExecute() {
			email.setEnabled(false);
			password.setEnabled(false);
			loginBtn.setEnabled(false);
			connectingMsg.setText("Authenticating");
			progressBar.setIndeterminate(true);
			login.setVisibility(View.GONE);
			connecting.setVisibility(View.VISIBLE);
		}
		
		@Override
		protected JSONObject doInBackground(Void... arg0) {
			try {
				return NetworkConnection.getInstance().login(email.getText().toString(), password.getText().toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void onPostExecute(JSONObject result) {
			email.setEnabled(true);
			password.setEnabled(true);
			loginBtn.setEnabled(true);

			if(result != null && result.has("session")) {
				try {
					SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
					editor.putString("session_key", result.getString("session"));
					login.setVisibility(View.GONE);
					connecting.setVisibility(View.VISIBLE);
					conn.addHandler(mHandler);
					conn.connect(result.getString("session"));
					editor.commit();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				login.setVisibility(View.VISIBLE);
				connecting.setVisibility(View.GONE);
				AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				builder.setTitle("Login Failed");
				String message = "Unable to connect to IRCCloud.  Please try again shortly.";
				if(result != null) {
					try {
						message = result.getString("message");
						if(message.equalsIgnoreCase("auth") || message.equalsIgnoreCase("password"))
							message = "Incorrect username or password.  Please try again.";
						else if(message.equalsIgnoreCase("legacy_account"))
							message = "Your account hasn't been migrated yet.  Please try again shortly.";
						else
							message = "Error: " + message;
					} catch (JSONException e) {
						e.printStackTrace();
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
				dialog.setOwnerActivity(MainActivity.this);
				dialog.show();
			}
		}
    }
}
