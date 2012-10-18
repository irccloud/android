package com.irccloud.android;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

public class IgnoreListActivity extends BaseActivity {
	int cid;
	NetworkConnection conn;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ignore_list);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if(savedInstanceState != null && savedInstanceState.containsKey("cid")) {
        	cid = savedInstanceState.getInt("cid");
        }
    }

    @Override
    public void onResume() {
    	super.onResume();
    	if(getIntent() != null && getIntent().hasExtra("cid")) {
	    	cid = getIntent().getIntExtra("cid", 0);
    	}
    	getSupportActionBar().setTitle("Ignore list for " + ServersDataSource.getInstance().getServer(cid).name);
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
    }

    @Override
    public void onPause() {
    	super.onPause();
    	if(conn != null)
    		conn.removeHandler(mHandler);
    }
    
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_CONNECTIONDELETED:
				Integer id = (Integer)msg.obj;
				if(id == cid) {
	                Intent parentActivityIntent = new Intent(IgnoreListActivity.this, MainActivity.class);
	                parentActivityIntent.addFlags(
	                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
	                        Intent.FLAG_ACTIVITY_NEW_TASK);
	                startActivity(parentActivityIntent);
	                finish();
				}
				break;
			}
		}
	};
    
    @Override
    public void onSaveInstanceState(Bundle state) {
    	super.onSaveInstanceState(state);
    	state.putInt("cid", cid);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getSupportMenuInflater().inflate(R.menu.activity_ignore_list, menu);

        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_add:
	    		ServersDataSource s = ServersDataSource.getInstance();
	    		ServersDataSource.Server server = s.getServer(cid);
	    		AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    		LayoutInflater inflater = getLayoutInflater();
	        	View view = inflater.inflate(R.layout.dialog_textprompt,null);
	        	TextView prompt = (TextView)view.findViewById(R.id.prompt);
	        	final EditText input = (EditText)view.findViewById(R.id.textInput);
	        	input.setHint("nickname!user@host.name");
	        	prompt.setText("Ignore messages from this hostmask");
	        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
	    		builder.setView(view);
	    		builder.setPositiveButton("Ignore", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						conn.ignore(cid, input.getText().toString());
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
	    		dialog.setOwnerActivity(this);
	    		dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	    		dialog.show();
	    		break;
        }
        return super.onOptionsItemSelected(item);
    }
}
