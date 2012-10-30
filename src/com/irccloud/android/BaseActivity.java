package com.irccloud.android;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.google.android.gcm.GCMRegistrar;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class BaseActivity extends SherlockFragmentActivity {
	NetworkConnection conn;
	private int lastState;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        lastState = -1;
    }

    protected void setLoadingIndicator(boolean state) {
    	//We toggle this to work around a bug where the bar wont redraw properly after setting the background drawable
   		setSupportProgressBarIndeterminateVisibility(!state);
   		setSupportProgressBarIndeterminateVisibility(state);
    }
    
    @Override
    public void onResume() {
    	super.onResume();
		setLoadingIndicator(false);
    	String session = getSharedPreferences("prefs", 0).getString("session_key", "");
    	if(session != null && session.length() > 0) {
	    	conn = NetworkConnection.getInstance();
	    	conn.addHandler(mHandler);
	    	if(conn.getState() == NetworkConnection.STATE_DISCONNECTED)
	    		conn.connect(session);
			if(NetworkConnection.getInstance().getState() != NetworkConnection.STATE_CONNECTED) {
				if(lastState != 1) {
					setLoadingIndicator(true);
					getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.disconnected_yellow));
					lastState = 1;
				}
			} else {
				if(lastState != 2) {
					setLoadingIndicator(false);
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
    
    @SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
    	String bufferToOpen = null;
    	int cidToOpen = -1;
    	LayoutInflater inflater;
    	View view;
    	TextView prompt;
		AlertDialog dialog;
    	
		public void handleMessage(Message msg) {
			final IRCCloudJSONObject o;
			final BuffersDataSource.Buffer b;
			ServersDataSource s;
			ServersDataSource.Server server;
			AlertDialog.Builder builder;
			
			switch (msg.what) {
			case NetworkConnection.EVENT_CONNECTIVITY:
				Log.i("IRCCloud", "New connection state: " + NetworkConnection.getInstance().getState());
				if(NetworkConnection.getInstance().getState() != NetworkConnection.STATE_CONNECTED) {
					if(lastState != 1) {
						setLoadingIndicator(true);
						getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.disconnected_yellow));
						lastState = 1;
					}
				} else {
					if(lastState != 2) {
						setLoadingIndicator(false);
			    		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.heading_bg_blue));
						lastState = 2;
					}
				}
				break;
			case NetworkConnection.EVENT_BACKLOG_START:
                setLoadingIndicator(true);
				break;
			case NetworkConnection.EVENT_BACKLOG_END:
                setLoadingIndicator(false);
				break;
			case NetworkConnection.EVENT_MAKEBUFFER:
				b = (BuffersDataSource.Buffer)msg.obj;
				if(cidToOpen == b.cid && b.name.equalsIgnoreCase(bufferToOpen) && !bufferToOpen.equalsIgnoreCase(getSupportActionBar().getTitle().toString())) {
		    		Intent i = new Intent(BaseActivity.this, MessageActivity.class);
		    		i.putExtra("cid", b.cid);
		    		i.putExtra("bid", b.bid);
		    		i.putExtra("last_seen_eid", b.last_seen_eid);
		    		i.putExtra("min_eid", b.min_eid);
		    		i.putExtra("type", b.type);
		    		i.putExtra("name", b.name);
		    		i.putExtra("joined", 1);
		    		i.putExtra("archived", 0);
		    		i.putExtra("status", "connected_ready");
		    		startActivity(i);
		    		bufferToOpen = null;
		    		cidToOpen = -1;
				}
				break;
			case NetworkConnection.EVENT_OPENBUFFER:
				o = (IRCCloudJSONObject)msg.obj;
				try {
					bufferToOpen = o.getString("name");
					cidToOpen = o.cid();
					b = BuffersDataSource.getInstance().getBufferByName(cidToOpen, bufferToOpen);
					if(b != null && !bufferToOpen.equalsIgnoreCase(getSupportActionBar().getTitle().toString())) {
			    		Intent i = new Intent(BaseActivity.this, MessageActivity.class);
			    		i.putExtra("cid", b.cid);
			    		i.putExtra("bid", b.bid);
			    		i.putExtra("last_seen_eid", b.last_seen_eid);
			    		i.putExtra("min_eid", b.min_eid);
			    		i.putExtra("type", b.type);
			    		i.putExtra("name", b.name);
			    		i.putExtra("joined", 1);
			    		i.putExtra("archived", 0);
			    		i.putExtra("status", "connected_ready");
			    		startActivity(i);
			    		bufferToOpen = null;
			    		cidToOpen = -1;
					}
				} catch (Exception e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				}
				break;
			case NetworkConnection.EVENT_NOSUCHNICK:
	        	try {
					o = (IRCCloudJSONObject)msg.obj;
		    		s = ServersDataSource.getInstance();
		    		server = s.getServer(o.cid());
		    		builder = new AlertDialog.Builder(BaseActivity.this);
		        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
						builder.setMessage("No such nickname: " + o.getString("nick") + " on " + server.name + " (" + server.hostname + ":" + (server.port) + "). Please try again.");
		    		builder.setNegativeButton("Ok", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
		    		});
		    		dialog = builder.create();
		    		dialog.setOwnerActivity(BaseActivity.this);
		    		dialog.show();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	    		break;
			case NetworkConnection.EVENT_NOSUCHCHANNEL:
	        	try {
					o = (IRCCloudJSONObject)msg.obj;
		    		s = ServersDataSource.getInstance();
		    		server = s.getServer(o.cid());
		    		builder = new AlertDialog.Builder(BaseActivity.this);
		        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
						builder.setMessage("No such channel: " + o.getString("chan") + " on " + server.name + " (" + server.hostname + ":" + (server.port) + "). Please try again.");
		    		builder.setNegativeButton("Ok", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
		    		});
		    		dialog = builder.create();
		    		dialog.setOwnerActivity(BaseActivity.this);
		    		dialog.show();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	    		break;
			case NetworkConnection.EVENT_TOOMANYCHANNELS:
	        	try {
					o = (IRCCloudJSONObject)msg.obj;
		    		s = ServersDataSource.getInstance();
		    		server = s.getServer(o.cid());
		    		builder = new AlertDialog.Builder(BaseActivity.this);
		        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
						builder.setMessage("Couldn't join " + o.getString("chan") + ". You have joined too many channels.");
		    		builder.setNegativeButton("Ok", new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
		    		});
		    		dialog = builder.create();
		    		dialog.setOwnerActivity(BaseActivity.this);
		    		dialog.show();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	    		break;
			case NetworkConnection.EVENT_BADCHANNELKEY:
				o = (IRCCloudJSONObject)msg.obj;
	    		s = ServersDataSource.getInstance();
	    		server = s.getServer(o.cid());
	    		builder = new AlertDialog.Builder(BaseActivity.this);
	    		inflater = getLayoutInflater();
	        	view = inflater.inflate(R.layout.dialog_textprompt,null);
	        	prompt = (TextView)view.findViewById(R.id.prompt);
	        	final EditText keyinput = (EditText)view.findViewById(R.id.textInput);
	        	keyinput.setOnEditorActionListener(new OnEditorActionListener() {
	                public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
		              	   if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
								try {
									conn.join(o.cid(), o.getString("chan"), keyinput.getText().toString());
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								dialog.dismiss();
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
	        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
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
	    		dialog = builder.create();
	    		dialog.setOwnerActivity(BaseActivity.this);
	    		dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	    		dialog.show();
	    		break;
			case NetworkConnection.EVENT_INVALIDNICK:
				o = (IRCCloudJSONObject)msg.obj;
	    		s = ServersDataSource.getInstance();
	    		server = s.getServer(o.cid());
	    		builder = new AlertDialog.Builder(BaseActivity.this);
	    		inflater = getLayoutInflater();
	        	view = inflater.inflate(R.layout.dialog_textprompt,null);
	        	prompt = (TextView)view.findViewById(R.id.prompt);
	        	final EditText nickinput = (EditText)view.findViewById(R.id.textInput);
	        	nickinput.setOnEditorActionListener(new OnEditorActionListener() {
	                public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
	              	   if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
							try {
								conn.say(o.cid(), null, "/nick " + nickinput.getText().toString());
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							dialog.dismiss();
	              	   }
	              	   return true;
                	}
	             });
	        	try {
	        		Log.i("IRCCloud", server.isupport.toString());
	        		String message = o.getString("invalid_nick") + " is not a valid nickname, try again";
	        		if(server.isupport != null && server.isupport.has("NICKLEN"))
	        			message += " (" + server.isupport.get("NICKLEN").getAsString() + " chars)";
	        		message += ".";
					prompt.setText(message);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
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
	    		dialog = builder.create();
	    		dialog.setOwnerActivity(BaseActivity.this);
	    		dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	    		dialog.show();
	    		break;
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
    	Intent i;
    	
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
        		i = new Intent(this, LoginActivity.class);
        		i.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK);
        		startActivity(i);
        		finish();
                GCMRegistrar.checkDevice(this);
                GCMRegistrar.checkManifest(this);
                GCMRegistrar.unregister(this);
            	break;
            case R.id.menu_settings:
        		i = new Intent(this, PreferencesActivity.class);
        		startActivity(i);
            	break;
        }
        return super.onOptionsItemSelected(item);
    }
}
