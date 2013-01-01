package com.irccloud.android;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.google.android.gcm.GCMRegistrar;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
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

    @Override
    public void onResume() {
    	super.onResume();
    	String session = getSharedPreferences("prefs", 0).getString("session_key", "");
    	if(session != null && session.length() > 0) {
	    	conn = NetworkConnection.getInstance();
	    	if(conn.ready) {
		    	conn.addHandler(mHandler);
		    	if(conn.getState() == NetworkConnection.STATE_DISCONNECTED || conn.getState() == NetworkConnection.STATE_DISCONNECTING)
		    		conn.connect(session);
	    	}
    	} else {
    		Intent i = new Intent(this, MainActivity.class);
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
    	LayoutInflater inflater;
    	View view;
    	TextView prompt;
		AlertDialog dialog;
    	String message = "";
		
		public void handleMessage(Message msg) {
			final IRCCloudJSONObject o;
			ServersDataSource s;
			ServersDataSource.Server server;
			AlertDialog.Builder builder;
			
			switch (msg.what) {
			case NetworkConnection.EVENT_CONNECTIVITY:
				Log.i("IRCCloud", "New connection state: " + NetworkConnection.getInstance().getState());
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
			case NetworkConnection.EVENT_ALERT:
	        	try {
					o = (IRCCloudJSONObject)msg.obj;
	        		String type = o.type();
	        		
	        		if(type.equalsIgnoreCase("invite_only_chan"))
	        			showAlert(o.cid(), "You need an invitation to join " + o.getString("chan"));
	        		else if(type.equalsIgnoreCase("channel_full"))
	        			showAlert(o.cid(), o.getString("chan") + " isn't allowing any more members to join.");
	        		else if(type.equalsIgnoreCase("banned_from_channel"))
	        			showAlert(o.cid(), "You've been banned from " + o.getString("chan"));
	        		else if(type.equalsIgnoreCase("invalid_nickchange"))
	        			showAlert(o.cid(), o.getString("ban_channel") + ": " + o.getString("msg"));
	        		else if(type.equalsIgnoreCase("no_messages_from_non_registered")) {
	        			if(o.has("nick") && o.getString("nick").length() > 0)
	        				showAlert(o.cid(), o.getString("nick") + ": " + o.getString("msg"));
	        			else
	        				showAlert(o.cid(), o.getString("msg"));
	        		} else if(type.equalsIgnoreCase("not_registered")) {
	        			String first = o.getString("first");
	        			if(o.has("rest"))
	        				first += " " + o.getString("rest");
	        			showAlert(o.cid(), first + ": " + o.getString("msg"));
	        		} else if(type.equalsIgnoreCase("too_many_channels"))
	        			showAlert(o.cid(), "Couldn't join " + o.getString("chan") + ": " + o.getString("msg"));
	        		else if(type.equalsIgnoreCase("too_many_targets"))
	        			showAlert(o.cid(), o.getString("description") + ": " + o.getString("msg"));
	        		else if(type.equalsIgnoreCase("no_such_server"))
	        			showAlert(o.cid(), o.getString("server") + ": " + o.getString("msg"));
	        		else if(type.equalsIgnoreCase("unknown_command"))
	        			showAlert(o.cid(), "Unknown command: " + o.getString("command"));
	        		else if(type.equalsIgnoreCase("help_not_found"))
	        			showAlert(o.cid(), o.getString("topic") + ": " + o.getString("msg"));
	        		else if(type.equalsIgnoreCase("accept_exists"))
						showAlert(o.cid(), o.getString("nick") + " " + o.getString("msg"));
	        		else if(type.equalsIgnoreCase("accept_not"))
						showAlert(o.cid(), o.getString("nick") + " " + o.getString("msg"));
	        		else if(type.equalsIgnoreCase("nick_collision"))
						showAlert(o.cid(), o.getString("collision") + ": " + o.getString("msg"));
	        		else if(type.equalsIgnoreCase("nick_too_fast"))
						showAlert(o.cid(), o.getString("nick") + ": " + o.getString("msg"));
	        		else if(type.equalsIgnoreCase("save_nick"))
						showAlert(o.cid(), o.getString("nick") + ": " + o.getString("msg") + ": " + o.getString("new_nick"));
	        		else if(type.equalsIgnoreCase("unknown_mode"))
						showAlert(o.cid(), "Missing mode: " + o.getString("params"));
	        		else if(type.equalsIgnoreCase("user_not_in_channel"))
						showAlert(o.cid(), o.getString("nick") + " is not in " + o.getString("channel"));
	        		else if(type.equalsIgnoreCase("need_more_params"))
						showAlert(o.cid(), "Missing parameters for command: " + o.getString("command"));
	        		else if(type.equalsIgnoreCase("chan_privs_needed"))
						showAlert(o.cid(), o.getString("chan") + ": " + o.getString("msg"));
	        		else if(type.equalsIgnoreCase("not_on_channel"))
						showAlert(o.cid(), o.getString("channel") + ": " + o.getString("msg"));
	        		else if(type.equalsIgnoreCase("ban_on_chan"))
						showAlert(o.cid(), "You cannot change your nick to " + o.getString("proposed_nick") + " while banned on " + o.getString("channel"));
	        		else if(type.equalsIgnoreCase("cannot_send_to_chan"))
						showAlert(o.cid(), o.getString("channel") + ": " + o.getString("msg"));
	        		else if(type.equalsIgnoreCase("user_on_channel"))
						showAlert(o.cid(), o.getString("nick") + " is already a member of " + o.getString("channel"));
	        		else if(type.equalsIgnoreCase("no_nick_given"))
						showAlert(o.cid(), "No nickname given");
	        		else if(type.equalsIgnoreCase("silence")) {
	        			String mask = o.getString("usermask");
	        			if(mask.startsWith("-"))
	        				message = mask.substring(1) + " removed from silence list";
	        			else if(mask.startsWith("+"))
	        				message = mask.substring(1) + " added to silence list";
	        			else
	        				message = "Silence list change: " + mask;
						showAlert(o.cid(), message);
	        		} else if(type.equalsIgnoreCase("no_channel_topic"))
						showAlert(o.cid(), o.getString("channel") + ": " + o.getString("msg"));
	        		else
	        			showAlert(o.cid(), o.getString("msg"));
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
	    		break;
			default:
				break;
			}
		}
	};
    
	private void showAlert(int cid, String msg) {
		ServersDataSource.Server server = ServersDataSource.getInstance().getServer(cid);
		AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.this);
    	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
		builder.setMessage(msg);
		builder.setNegativeButton("Ok", new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog dialog = builder.create();
		dialog.setOwnerActivity(BaseActivity.this);
		dialog.show();
	}
	
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
            	conn.ready = false;
				SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
        		try {
	                GCMRegistrar.checkDevice(this);
	                GCMRegistrar.checkManifest(this);
					if(GCMRegistrar.isRegistered(this)) {
						//Store the old session key so GCM can unregister later
						editor.putString(GCMRegistrar.getRegistrationId(this), getSharedPreferences("prefs", 0).getString("session_key", ""));
		                GCMRegistrar.unregister(this);
					}
        		} catch (Exception e) {
        			//GCM might not be available on the device
        		}
				editor.remove("session_key");
				editor.remove("gcm_registered");
				editor.remove("mentionTip");
				editor.commit();
				ServersDataSource.getInstance().clear();
				BuffersDataSource.getInstance().clear();
				ChannelsDataSource.getInstance().clear();
				UsersDataSource.getInstance().clear();
				EventsDataSource.getInstance().clear();
				Notifications.getInstance().clear();
        		i = new Intent(this, MainActivity.class);
        		i.addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_NEW_TASK);
        		startActivity(i);
        		finish();
            	break;
            case R.id.menu_settings:
        		i = new Intent(this, PreferencesActivity.class);
        		startActivity(i);
            	break;
        }
        return super.onOptionsItemSelected(item);
    }
}
