package com.irccloud.android;

import org.json.JSONException;
import org.json.JSONObject;

import com.actionbarsherlock.internal.nineoldandroids.animation.Animator;
import com.actionbarsherlock.internal.nineoldandroids.animation.Animator.AnimatorListener;
import com.actionbarsherlock.internal.nineoldandroids.animation.AnimatorSet;
import com.actionbarsherlock.internal.nineoldandroids.animation.ObjectAnimator;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class MessageActivity extends BaseActivity  implements UsersListFragment.OnUserSelectedListener {
	int cid;
	int bid;
	String name;
	String type;
	TextView messageTxt;
	View sendBtn;
	int joined;
	int archived;
	String status;
	UsersDataSource.User selected_user;
	View userListView;

	NetworkConnection conn;
	
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
        sendBtn = findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new SendTask().execute((Void)null);
			}
        });
        userListView = findViewById(R.id.usersListFragment);
		userListView.setVisibility(View.INVISIBLE);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        if(savedInstanceState != null && savedInstanceState.containsKey("cid")) {
        	cid = savedInstanceState.getInt("cid");
        	bid = savedInstanceState.getInt("bid");
        	name = savedInstanceState.getString("name");
        	type = savedInstanceState.getString("type");
        	joined = savedInstanceState.getInt("joined");
        	archived = savedInstanceState.getInt("archived");
        	status = savedInstanceState.getString("status");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
    	super.onSaveInstanceState(state);
    	state.putInt("cid", cid);
    	state.putInt("bid", bid);
    	state.putString("name", name);
    	state.putString("type", type);
    	state.putInt("joined", joined);
    	state.putInt("archived", archived);
    	state.putString("status", status);
    }
    
    private class SendTask extends AsyncTask<Void, Void, Void> {
    	@Override
    	protected void onPreExecute() {
    		sendBtn.setEnabled(false);
    	}
    	
		@Override
		protected Void doInBackground(Void... arg0) {
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
    	if(getIntent() != null && getIntent().hasExtra("cid")) {
	    	cid = getIntent().getIntExtra("cid", 0);
	    	bid = getIntent().getIntExtra("bid", 0);
	    	name = getIntent().getStringExtra("name");
	    	type = getIntent().getStringExtra("type");
	    	joined = getIntent().getIntExtra("joined", 0);
	    	archived = getIntent().getIntExtra("archived", 0);
	    	status = getIntent().getStringExtra("status");
    	}
    	if(bid == -1) {
    		BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBufferByName(cid, name);
    		if(b != null) {
    			bid = b.bid;
    		}
    	}
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
    	updateUsersListFragmentVisibility();
    	getSupportActionBar().setTitle(name);
    	invalidateOptionsMenu();
    }

    @Override
    public void onPause() {
    	super.onPause();
    	if(conn != null)
    		conn.removeHandler(mHandler);
    }

    private void updateUsersListFragmentVisibility() {
    	boolean hide = false;
		View v = findViewById(R.id.usersListFragment);
		if(v != null) {
			try {
				JSONObject hiddenMap = conn.getUserInfo().prefs.getJSONObject("channel-hiddenMembers");
				if(hiddenMap.has(String.valueOf(bid)) && hiddenMap.getBoolean(String.valueOf(bid)))
					hide = true;
			} catch (JSONException e) {
			}
	    	if(hide || !type.equalsIgnoreCase("channel"))
	    		v.setVisibility(View.GONE);
		}
    }
    
	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			Integer event_bid = 0;
			IRCCloudJSONObject event = null;
			switch (msg.what) {
			case NetworkConnection.EVENT_USERINFO:
		    	updateUsersListFragmentVisibility();
				invalidateOptionsMenu();
				break;
			case NetworkConnection.EVENT_STATUSCHANGED:
				try {
					IRCCloudJSONObject o = (IRCCloudJSONObject)msg.obj;
					if(o.cid() == cid) {
						status = o.getString("new_status");
						invalidateOptionsMenu();
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case NetworkConnection.EVENT_MAKESERVER:
				ServersDataSource.Server server = (ServersDataSource.Server)msg.obj;
				if(server.cid == cid) {
					status = server.status;
					invalidateOptionsMenu();
				}
				break;
			case NetworkConnection.EVENT_MAKEBUFFER:
				BuffersDataSource.Buffer buffer = (BuffersDataSource.Buffer)msg.obj;
				if(bid == -1 && buffer.cid == cid && buffer.name.equalsIgnoreCase(name)) {
					Log.i("IRCCloud", "Got my new buffer id: " + buffer.bid);
					bid = buffer.bid;
				}
				break;
			case NetworkConnection.EVENT_BUFFERARCHIVED:
				event_bid = (Integer)msg.obj;
				if(event_bid == bid) {
					archived = 1;
					invalidateOptionsMenu();
				}
				break;
			case NetworkConnection.EVENT_BUFFERUNARCHIVED:
				event_bid = (Integer)msg.obj;
				if(event_bid == bid) {
					archived = 0;
					invalidateOptionsMenu();
				}
				break;
			case NetworkConnection.EVENT_JOIN:
				event = (IRCCloudJSONObject)msg.obj;
				if(event.bid() == bid && event.type().equalsIgnoreCase("you_joined_channel")) {
					joined = 1;
					invalidateOptionsMenu();
				}
				break;
			case NetworkConnection.EVENT_PART:
				event = (IRCCloudJSONObject)msg.obj;
				if(event.bid() == bid && event.type().equalsIgnoreCase("you_parted_channel")) {
					joined = 0;
					invalidateOptionsMenu();
				}
				break;
			case NetworkConnection.EVENT_CHANNELINIT:
				ChannelsDataSource.Channel channel = (ChannelsDataSource.Channel)msg.obj;
				if(channel.bid == bid) {
					joined = 1;
					archived = 0;
					invalidateOptionsMenu();
				}
			default:
				break;
			}
		}
	};
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	if(type.equalsIgnoreCase("channel")) {
    		getSupportMenuInflater().inflate(R.menu.activity_message_channel_userlist, menu);
    		getSupportMenuInflater().inflate(R.menu.activity_message_channel, menu);
    	} else if(type.equalsIgnoreCase("conversation"))
    		getSupportMenuInflater().inflate(R.menu.activity_message_conversation, menu);
    	else if(type.equalsIgnoreCase("console"))
    		getSupportMenuInflater().inflate(R.menu.activity_message_console, menu);

    	getSupportMenuInflater().inflate(R.menu.activity_message_archive, menu);

        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
    	if(archived == 0) {
    		menu.findItem(R.id.menu_archive).setTitle(R.string.menu_archive);
    	} else {
    		menu.findItem(R.id.menu_archive).setTitle(R.string.menu_unarchive);
    	}
    	if(type.equalsIgnoreCase("channel")) {
        	if(joined == 0) {
        		menu.findItem(R.id.menu_leave).setTitle(R.string.menu_rejoin);
        		menu.findItem(R.id.menu_archive).setVisible(true);
        		menu.findItem(R.id.menu_archive).setEnabled(true);
        		menu.findItem(R.id.menu_delete).setVisible(true);
        		menu.findItem(R.id.menu_delete).setEnabled(true);
        		menu.findItem(R.id.menu_topic).setVisible(false);
        		menu.findItem(R.id.menu_topic).setEnabled(false);
        		if(menu.findItem(R.id.menu_userlist) != null) {
        			menu.findItem(R.id.menu_userlist).setEnabled(false);
        			menu.findItem(R.id.menu_userlist).setVisible(false);
        		}
        	} else {
        		menu.findItem(R.id.menu_leave).setTitle(R.string.menu_leave);
        		menu.findItem(R.id.menu_archive).setVisible(false);
        		menu.findItem(R.id.menu_archive).setEnabled(false);
        		menu.findItem(R.id.menu_delete).setVisible(false);
        		menu.findItem(R.id.menu_delete).setEnabled(false);
        		menu.findItem(R.id.menu_topic).setVisible(true);
        		menu.findItem(R.id.menu_topic).setEnabled(true);
        		if(menu.findItem(R.id.menu_userlist) != null) {
	        		boolean hide = false;
	        		try {
	        			if(conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null) {
							JSONObject hiddenMap = conn.getUserInfo().prefs.getJSONObject("channel-hiddenMembers");
							if(hiddenMap.has(String.valueOf(bid)) && hiddenMap.getBoolean(String.valueOf(bid)))
								hide = true;
	        			}
					} catch (JSONException e) {
					}
					if(hide) {
		        		menu.findItem(R.id.menu_userlist).setEnabled(false);
		        		menu.findItem(R.id.menu_userlist).setVisible(false);
					} else {
		        		menu.findItem(R.id.menu_userlist).setEnabled(true);
		        		menu.findItem(R.id.menu_userlist).setVisible(true);
					}
        		}
        	}
    	} else if(type.equalsIgnoreCase("console")) {
    		menu.findItem(R.id.menu_archive).setVisible(false);
    		menu.findItem(R.id.menu_archive).setEnabled(false);
    		Log.i("IRCCloud", "Status: " + status);
    		if(status != null && status.contains("connected") && !status.startsWith("dis")) {
    			menu.findItem(R.id.menu_disconnect).setTitle(R.string.menu_disconnect);
        		menu.findItem(R.id.menu_delete).setVisible(false);
        		menu.findItem(R.id.menu_delete).setEnabled(false);
    		} else {
    			menu.findItem(R.id.menu_disconnect).setTitle(R.string.menu_reconnect);
        		menu.findItem(R.id.menu_delete).setVisible(true);
        		menu.findItem(R.id.menu_delete).setEnabled(true);
    		}
    	}
    	return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	Intent intent;
    	AlertDialog.Builder builder;
    	AlertDialog dialog;
    	
        switch (item.getItemId()) {
            case R.id.menu_userlist:
            	if(userListView.getVisibility() == View.INVISIBLE) {
            		userListView.setVisibility(View.VISIBLE);
            		ObjectAnimator animX = ObjectAnimator.ofFloat(userListView, "x", findViewById(R.id.frame).getWidth(), findViewById(R.id.frame).getWidth() - userListView.getWidth());
            		ObjectAnimator animAlpha = ObjectAnimator.ofFloat(userListView, "alpha", 0f, 1f);
            		AnimatorSet animSet = new AnimatorSet();
            		animSet.playTogether(animX, animAlpha);
            		animSet.addListener(new AnimatorListener() {
						@Override
						public void onAnimationStart(Animator animation) {
						}

						@Override
						public void onAnimationEnd(Animator animation) {
						}

						@Override
						public void onAnimationCancel(Animator animation) {
						}

						@Override
						public void onAnimationRepeat(Animator animation) {
						}
            		});
            		animSet.start();
            	} else {
            		ObjectAnimator animX = ObjectAnimator.ofFloat(userListView, "x", findViewById(R.id.frame).getWidth() - userListView.getWidth(), findViewById(R.id.frame).getWidth());
            		ObjectAnimator animAlpha = ObjectAnimator.ofFloat(userListView, "alpha", 1f, 0f);
            		AnimatorSet animSet = new AnimatorSet();
            		animSet.playTogether(animX, animAlpha);
            		animSet.addListener(new AnimatorListener() {
						@Override
						public void onAnimationStart(Animator animation) {
						}

						@Override
						public void onAnimationEnd(Animator animation) {
		            		userListView.setVisibility(View.INVISIBLE);
						}

						@Override
						public void onAnimationCancel(Animator animation) {
						}

						@Override
						public void onAnimationRepeat(Animator animation) {
						}
            		});
            		animSet.start();
            	}
            	return true;
            case R.id.menu_ignore_list:
                intent = new Intent(this, IgnoreListActivity.class);
                intent.putExtra("cid", cid);
                startActivity(intent);
                return true;
            case R.id.menu_leave:
            	if(joined == 0)
            		conn.join(cid, name, "");
            	else
            		conn.part(cid, name, "");
            	return true;
            case R.id.menu_archive:
            	if(archived == 0)
            		conn.archiveBuffer(cid, bid);
            	else
            		conn.unarchiveBuffer(cid, bid);
            	return true;
            case R.id.menu_delete:
            	builder = new AlertDialog.Builder(MessageActivity.this);
            	
            	if(type.equalsIgnoreCase("console"))
            		builder.setTitle("Delete Connection");
            	else
            		builder.setTitle("Delete History");
            	
            	if(type.equalsIgnoreCase("console"))
            		builder.setMessage("Are you sure you want to remove this connection?");
            	else if(type.equalsIgnoreCase("channel"))
            		builder.setMessage("Are you sure you want to clear your history in " + name + "?");
            	else
            		builder.setMessage("Are you sure you want to clear your history with " + name + "?");
            	
            	builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
            	});
            	builder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
		            	if(type.equalsIgnoreCase("console")) {
		            		conn.deleteServer(cid);
		            	} else {
		                	conn.deleteBuffer(cid, bid);
		            	}
						dialog.dismiss();
					}
            	});
	    		dialog = builder.create();
	    		dialog.setOwnerActivity(MessageActivity.this);
	    		dialog.show();
            	return true;
            case R.id.menu_editconnection:
                intent = new Intent(this, EditConnectionActivity.class);
                intent.putExtra("cid", cid);
                startActivity(intent);
            	return true;
            case R.id.menu_disconnect:
        		if(status != null && status.contains("connected") && !status.startsWith("dis")) {
        			conn.disconnect(cid, "");
        		} else {
        			conn.reconnect(cid);
        		}
        		return true;
            case R.id.menu_topic:
            	ChannelsDataSource.Channel c = ChannelsDataSource.getInstance().getChannelForBuffer(bid);
            	if(c != null) {
	            	builder = new AlertDialog.Builder(MessageActivity.this);
	            	builder.setTitle("Channel Topic");
	            	if(c.topic_text.length() > 0) {
	            		final SpannableString s = new SpannableString(c.topic_text);
	            		Linkify.addLinks(s, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
	            		builder.setMessage(s);
	            	} else
	            		builder.setMessage("No topic set.");
	            	builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
	            	});
	            	builder.setNeutralButton("Edit", new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							editTopic();
						}
	            	});
		    		dialog = builder.create();
		    		dialog.setOwnerActivity(MessageActivity.this);
		    		dialog.show();
		    		((TextView)dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
            	}
            	return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    void editTopic() {
    	ChannelsDataSource.Channel c = ChannelsDataSource.getInstance().getChannelForBuffer(bid);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
    	View view = inflater.inflate(R.layout.dialog_textprompt,null);
    	TextView prompt = (TextView)view.findViewById(R.id.prompt);
    	final EditText input = (EditText)view.findViewById(R.id.textInput);
    	input.setText(c.topic_text);
    	prompt.setVisibility(View.GONE);
    	builder.setTitle("Channel Topic");
		builder.setView(view);
		builder.setPositiveButton("Set Topic", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				conn.say(cid, name, "/topic " + input.getText().toString());
				dialog.dismiss();
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog dialog = builder.create();
		dialog.setOwnerActivity(this);
		dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		dialog.show();
    }
    
	@Override
	public void onUserSelected(int c, String chan, String n) {
		UsersDataSource u = UsersDataSource.getInstance();
		selected_user = u.getUser(cid, chan, n);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

   		final CharSequence[] items = {"Open", "Invite to a channel...", "Ignore", "Op", "Kick...", "Ban..."};

		if(selected_user.mode.contains("o") || selected_user.mode.contains("O"))
			items[3] = "Deop";
		
		builder.setTitle(selected_user.nick + "\n(" + selected_user.hostmask + ")");
		builder.setItems(items, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialogInterface, int item) {
	    		AlertDialog.Builder builder = new AlertDialog.Builder(MessageActivity.this);
	    		LayoutInflater inflater = getLayoutInflater();
	    		ServersDataSource s = ServersDataSource.getInstance();
	    		ServersDataSource.Server server = s.getServer(cid);
	    		View view;
	    		final TextView prompt;
	    		final EditText input;
	    		AlertDialog dialog;
	    		
	    		switch(item) {
		    	case 0:
		    		BuffersDataSource b = BuffersDataSource.getInstance();
		    		BuffersDataSource.Buffer buffer = b.getBufferByName(cid, selected_user.nick);
		    		Intent i = new Intent(MessageActivity.this, MessageActivity.class);
		    		if(buffer != null) {
			    		i.putExtra("cid", buffer.cid);
			    		i.putExtra("bid", buffer.bid);
			    		i.putExtra("name", buffer.name);
			    		i.putExtra("last_seen_eid", buffer.last_seen_eid);
			    		i.putExtra("min_eid", buffer.min_eid);
			    		i.putExtra("type", buffer.type);
			    		i.putExtra("joined", 1);
			    		i.putExtra("archived", buffer.archived);
			    		i.putExtra("status", status);
		    		} else {
			    		i.putExtra("cid", cid);
			    		i.putExtra("bid", -1);
			    		i.putExtra("name", selected_user.nick);
			    		i.putExtra("last_seen_eid", 0L);
			    		i.putExtra("min_eid", 0L);
			    		i.putExtra("type", "conversation");
			    		i.putExtra("joined", 1);
			    		i.putExtra("archived", 0);
			    		i.putExtra("status", status);
		    		}
		    		startActivity(i);
		    		break;
		    	case 1:
		        	view = inflater.inflate(R.layout.dialog_textprompt,null);
		        	prompt = (TextView)view.findViewById(R.id.prompt);
		        	input = (EditText)view.findViewById(R.id.textInput);
		        	prompt.setText("Invite " + selected_user.nick + " to a channel");
		        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
		    		builder.setView(view);
		    		builder.setPositiveButton("Invite", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							conn.invite(cid, input.getText().toString(), selected_user.nick);
							dialog.dismiss();
						}
		    		});
		    		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
		    		});
		    		dialog = builder.create();
		    		dialog.setOwnerActivity(MessageActivity.this);
		    		dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		    		dialog.show();
		    		break;
		    	case 2:
		        	view = inflater.inflate(R.layout.dialog_textprompt,null);
		        	prompt = (TextView)view.findViewById(R.id.prompt);
		        	input = (EditText)view.findViewById(R.id.textInput);
		        	input.setText("*!"+selected_user.hostmask);
		        	prompt.setText("Ignore messages for " + selected_user.nick + " at this hostmask");
		        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
		    		builder.setView(view);
		    		builder.setPositiveButton("Ignore", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							conn.ignore(cid, input.getText().toString());
							dialog.dismiss();
						}
		    		});
		    		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
		    		});
		    		dialog = builder.create();
		    		dialog.setOwnerActivity(MessageActivity.this);
		    		dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		    		dialog.show();
		    		break;
		    	case 3:
		    		if(selected_user.mode.contains("o") || selected_user.mode.contains("O"))
		    			conn.mode(cid, name, "-o " + selected_user.nick);
		    		else
		    			conn.mode(cid, name, "+o " + selected_user.nick);
		    		break;
		    	case 4:
		        	view = inflater.inflate(R.layout.dialog_textprompt,null);
		        	prompt = (TextView)view.findViewById(R.id.prompt);
		        	input = (EditText)view.findViewById(R.id.textInput);
		        	prompt.setText("Give a reason for kicking");
		        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
		    		builder.setView(view);
		    		builder.setPositiveButton("Kick", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							conn.kick(cid, name, selected_user.nick, input.getText().toString());
							dialog.dismiss();
						}
		    		});
		    		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
		    		});
		    		dialog = builder.create();
		    		dialog.setOwnerActivity(MessageActivity.this);
		    		dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		    		dialog.show();
		    		break;
		    	case 5:
		        	view = inflater.inflate(R.layout.dialog_textprompt,null);
		        	prompt = (TextView)view.findViewById(R.id.prompt);
		        	input = (EditText)view.findViewById(R.id.textInput);
		        	input.setText("*!"+selected_user.hostmask);
		        	prompt.setText("Add a banmask for " + selected_user.nick);
		        	builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
		    		builder.setView(view);
		    		builder.setPositiveButton("Ban", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							conn.mode(cid, name, "+b " + input.getText().toString());
							dialog.dismiss();
						}
		    		});
		    		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
		    		});
		    		dialog = builder.create();
		    		dialog.setOwnerActivity(MessageActivity.this);
		    		dialog.getWindow().setSoftInputMode (WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		    		dialog.show();
		    		break;
		    	}
		    	dialogInterface.dismiss();
		    }
		});
		
		AlertDialog dialog = builder.create();
		dialog.setOwnerActivity(this);
		dialog.show();
    }
}
