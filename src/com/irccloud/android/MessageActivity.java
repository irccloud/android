package com.irccloud.android;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class MessageActivity extends SherlockFragmentActivity {
	int cid;
	long bid;
	String name;
	TextView messageTxt;
	Button sendBtn;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        messageTxt = (TextView)findViewById(R.id.messageTxt);
        messageTxt.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
         	   if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
         		   new SendTask().execute((Void)null);
         	   }
         	   return true;
           	}
        });
        sendBtn = (Button)findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new SendTask().execute((Void)null);
			}
        });
    }

    private class SendTask extends AsyncTask<Void, Void, Void> {
    	@Override
    	protected void onPreExecute() {
    		sendBtn.setEnabled(false);
    	}
    	
		@Override
		protected Void doInBackground(Void... arg0) {
			NetworkConnection conn = NetworkConnection.getInstance();
			if(conn.getState() == NetworkConnection.STATE_CONNECTED) {
				conn.say(cid, name, messageTxt.getText().toString());
			}
			return null;
		}
    	
		@Override
		protected void onPostExecute(Void result) {
			messageTxt.setText("");
    		sendBtn.setEnabled(true);
		}
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	cid = getIntent().getIntExtra("cid", 0);
    	bid = getIntent().getLongExtra("bid", 0);
    	name = getIntent().getStringExtra("name");
    	getSupportActionBar().setTitle(name);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_main, menu);

        return super.onCreateOptionsMenu(menu);
    }
}
