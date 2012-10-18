package com.irccloud.android;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.actionbarsherlock.app.SherlockActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class LoginActivity extends SherlockActivity {
	EditText email;
	EditText password;
	Button loginBtn;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.heading_bg_blue));
        
        email = (EditText)findViewById(R.id.email);
        password = (EditText)findViewById(R.id.password);
        password.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					new LoginTask().execute((Void)null);
				}
				return true;
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
    public void onResume() {
    	super.onResume();
    	email.requestFocus();
    	getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
    
    private class LoginTask extends AsyncTask<Void, Void, JSONObject> {
		@Override
		public void onPreExecute() {
			email.setEnabled(false);
			password.setEnabled(false);
			loginBtn.setEnabled(false);
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
				SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
				try {
					editor.putString("session_key", result.getString("session"));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				editor.commit();
	    		Intent i = new Intent(LoginActivity.this, MainActivity.class);
	    		startActivity(i);
	    		finish();
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
				builder.setTitle("Login Failed");
				String message = "Unable to connect to IRCCloud.  Please try again shortly.";
				if(result != null) {
					try {
						message = result.getString("message");
						if(message.equalsIgnoreCase("auth"))
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
				dialog.setOwnerActivity(LoginActivity.this);
				dialog.show();
			}
		}
    }
}
