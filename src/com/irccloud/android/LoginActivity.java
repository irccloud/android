package com.irccloud.android;

import java.io.IOException;

import com.actionbarsherlock.app.SherlockActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

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
        loginBtn = (Button)findViewById(R.id.loginBtn);
        loginBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new LoginTask().execute((Void)null);
			}
        });
    }

    private class LoginTask extends AsyncTask<Void, Void, String> {
		@Override
		public void onPreExecute() {
			email.setEnabled(false);
			password.setEnabled(false);
			loginBtn.setEnabled(false);
		}
		
		@Override
		protected String doInBackground(Void... arg0) {
			try {
				return NetworkConnection.getInstance().login(email.getText().toString(), password.getText().toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void onPostExecute(String result) {
			email.setEnabled(true);
			password.setEnabled(true);
			loginBtn.setEnabled(true);

			if(result != null && result.length() > 0) {
				SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
				editor.putString("session_key", result);
				editor.commit();
	    		Intent i = new Intent(LoginActivity.this, MainActivity.class);
	    		startActivity(i);
	    		finish();
			}
		}
    }
}
