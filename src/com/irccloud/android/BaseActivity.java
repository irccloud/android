package com.irccloud.android;

import org.json.JSONException;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class BaseActivity extends SherlockFragmentActivity {
	NetworkConnection conn;
	private int lastState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        lastState = -1;
    }

    @Override
    public void onResume() {
    	super.onResume();
		setSupportProgressBarIndeterminateVisibility(false);
    	String session = getSharedPreferences("prefs", 0).getString("session_key", "");
    	if(session != null && session.length() > 0) {
	    	conn = NetworkConnection.getInstance();
	    	conn.addHandler(mHandler);
	    	if(conn.getState() == NetworkConnection.STATE_DISCONNECTED)
	    		conn.connect(session);
			if(NetworkConnection.getInstance().getState() != NetworkConnection.STATE_CONNECTED) {
				if(lastState != 1) {
					setSupportProgressBarIndeterminateVisibility(true);
					getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.disconnected_yellow));
					lastState = 1;
				}
			} else {
				if(lastState != 2) {
					setSupportProgressBarIndeterminateVisibility(false);
					getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.heading_bg_blue));
					lastState = 2;
				}
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
    public void onPause() {
    	super.onPause();

    	if(conn != null) {
        	conn.removeHandler(mHandler);
    	}
    }
    
    private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_CONNECTIVITY:
				Log.i("IRCCloud", "New connection state: " + NetworkConnection.getInstance().getState());
				if(NetworkConnection.getInstance().getState() != NetworkConnection.STATE_CONNECTED) {
					if(lastState != 1) {
						setSupportProgressBarIndeterminateVisibility(true);
						getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.disconnected_yellow));
						lastState = 1;
					}
				} else {
					if(lastState != 2) {
						setSupportProgressBarIndeterminateVisibility(false);
						getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.heading_bg_blue));
						lastState = 2;
					}
				}
				break;
			case NetworkConnection.EVENT_BACKLOG_START:
                setSupportProgressBarIndeterminateVisibility(true);
				break;
			case NetworkConnection.EVENT_BACKLOG_END:
                setSupportProgressBarIndeterminateVisibility(false);
				break;
			case NetworkConnection.EVENT_BADCHANNELKEY:
				final IRCCloudJSONObject o = (IRCCloudJSONObject)msg.obj;
	    		ServersDataSource s = ServersDataSource.getInstance();
	    		ServersDataSource.Server server = s.getServer(o.cid());
	    		AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
	    		LayoutInflater inflater = getLayoutInflater();
	        	View view = inflater.inflate(R.layout.dialog_textprompt,null);
	        	TextView prompt = (TextView)view.findViewById(R.id.prompt);
	        	final EditText input = (EditText)view.findViewById(R.id.textInput);
	        	try {
					prompt.setText("Password for " + o.getString("chan"));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
	    		builder.setView(view);
	    		builder.setPositiveButton("Join", new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try {
							conn.join(o.cid(), o.getString("chan"), input.getText().toString());
						} catch (JSONException e) {
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
	    		dialog.setOwnerActivity(BaseActivity.this);
	    		dialog.show();
			default:
				break;
			}
		}
	};
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_base, menu);

        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_logout:
            	conn.disconnect();
				SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
				editor.remove("session_key");
				editor.commit();
				ServersDataSource.getInstance().clear();
				BuffersDataSource.getInstance().clear();
				ChannelsDataSource.getInstance().clear();
				UsersDataSource.getInstance().clear();
				EventsDataSource.getInstance().clear();
        		Intent i = new Intent(this, LoginActivity.class);
        		i.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK);
        		startActivity(i);
        		finish();
            	break;
        }
        return super.onOptionsItemSelected(item);
    }
}
