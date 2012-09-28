package com.irccloud.android;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;

@SuppressLint("SetJavaScriptEnabled")
public class MessageViewFragment extends SherlockFragment {
	private NetworkConnection conn;
	private WebView webView;
	private TextView awayView;
	private TextView statusView;
	private int cid;
	private int bid;
	private long last_seen_eid;
	private long min_eid;
	private long earliest_eid;
	private String name;
	private String type;
	private boolean firstScroll = true;
	
	private final Semaphore webviewLock = new Semaphore(1);
	
	public class JavaScriptInterface {
		public boolean loadingMoreBacklog = false;
		public TreeMap<Long,IRCCloudJSONObject> incomingBacklog;
		
		public void requestBacklog() {
			loadingMoreBacklog = true;
			BaseActivity a = (BaseActivity) getActivity();
			a.setSupportProgressBarIndeterminate(true);
			conn.request_backlog(cid, bid, earliest_eid);
		}

		public void log(String msg) {
			Log.i("IRCCloud", msg);
		}
		
	    public void showToast(String toast) {
	        Toast.makeText(getActivity(), toast, Toast.LENGTH_SHORT).show();
	    }
	    
	    public String getIgnores() {
	    	return ServersDataSource.getInstance().getServer(cid).ignores.toString();
	    }
	    
	    public String getPrefs() {
	    	if(NetworkConnection.getInstance().getUserInfo().prefs == null)
	    		return "{}";
	    	else
	    		return NetworkConnection.getInstance().getUserInfo().prefs.toString();
	    }
	    
	    public long getBid() {
	    	return bid;
	    }
	    
	    public String getIncomingBacklog() {
	    	JSONArray array = new JSONArray();
	    	if(incomingBacklog != null) {
	    		Iterator<IRCCloudJSONObject> i = incomingBacklog.values().iterator();
	    		while(i.hasNext()) {
	    			array.put(i.next().getObject());
	    		}
	    	}
	    	incomingBacklog = null;
	    	return array.toString();
	    }
	    
	    public void backlogComplete() {
	    	if(loadingMoreBacklog) {
    			mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
		    			webView.loadUrl("javascript:scrollToBacklogBottom()");
					}
    			}, 100);
	    		loadingMoreBacklog = false;
	    	} else if(firstScroll) {
    			mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
		    			webView.loadUrl("javascript:window.scrollTo(0, document.body.scrollHeight)");
					}
    			}, 100);
	    		firstScroll = false;
	    	}
	    }
	}
	
	private JavaScriptInterface jsInterface = new JavaScriptInterface();
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	final View v = inflater.inflate(R.layout.messageview, container, false);
    	v.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			int lastHeightDiff = 0;

			@Override
    		public void onGlobalLayout() {
				int heightDiff = v.getRootView().getHeight() - v.getHeight();
				if(heightDiff != lastHeightDiff) {
					final int delta = heightDiff - lastHeightDiff;
					lastHeightDiff = heightDiff;
	    			mHandler.postDelayed(new Runnable() {
	    				
						@Override
						public void run() {
							if(webviewLock.tryAcquire()) {
				    			webView.loadUrl("javascript:scrollToBottom("+delta+")");
				    			webView.invalidate();
				    	    	webviewLock.release();
							}
						}
	    			}, 250);
				}
    		}
   		}); 
    	webView = (WebView)v.findViewById(R.id.messageview);
    	webView.getSettings().setJavaScriptEnabled(true);
    	webView.addJavascriptInterface(jsInterface, "Android");
    	webView.setWebChromeClient(new WebChromeClient() {
    		  public void onConsoleMessage(String message, int lineNumber, String sourceID) {
    		    Log.d("IRCCloud", message + " -- From line "
    		                         + lineNumber + " of "
    		                         + sourceID);
    		  }
    		});
    	try {
    		Log.i("IRCCloud", "Acquire: onCreate");
			webviewLock.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	webView.loadUrl("file:///android_asset/messageview.html");
    	webView.setWebViewClient(new WebViewClient() {
    		public void onPageFinished(WebView view, String url) {
    			Log.i("IRCCloud", "Page loaded, releasing lock!");
    			webviewLock.release();
    		}
    	});
    	awayView = (TextView)v.findViewById(R.id.topicView);
    	statusView = (TextView)v.findViewById(R.id.statusView);
    	return v;
    }
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState.containsKey("cid")) {
        	cid = savedInstanceState.getInt("cid");
        	bid = savedInstanceState.getInt("bid");
        	name = savedInstanceState.getString("name");
        	last_seen_eid = savedInstanceState.getLong("last_seen_eid");
        	min_eid = savedInstanceState.getLong("min_eid");
        	type = savedInstanceState.getString("type");
        	firstScroll = savedInstanceState.getBoolean("firstScroll");
        }
    }
    
    @Override
    public void onSaveInstanceState(Bundle state) {
    	super.onSaveInstanceState(state);
    	state.putInt("cid", cid);
    	state.putLong("bid", bid);
    	state.putLong("last_seen_eid", last_seen_eid);
    	state.putLong("min_eid", min_eid);
    	state.putString("name", name);
    	state.putString("type", type);
    	state.putBoolean("firstScroll", firstScroll);
    }

    
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	if(activity.getIntent() != null && activity.getIntent().hasExtra("cid")) {
	    	cid = activity.getIntent().getIntExtra("cid", 0);
	    	bid = activity.getIntent().getIntExtra("bid", 0);
	    	last_seen_eid = activity.getIntent().getLongExtra("last_seen_eid", 0);
	    	min_eid = activity.getIntent().getLongExtra("min_eid", 0);
	    	name = activity.getIntent().getStringExtra("name");
	    	type = activity.getIntent().getStringExtra("type");
    	}
    }

    private void insertEvent(IRCCloudJSONObject event) {
    	if(webviewLock.tryAcquire()) {
	    	if(event.eid() == min_eid)
		    	webView.loadUrl("javascript:hideBacklogBtn()");
	    	if(event.eid() < earliest_eid)
	    		earliest_eid = event.eid();
	    	webView.loadUrl("javascript:appendEvent(("+event.toString()+"))");
	    	webviewLock.release();
    	}
    }
    
    public void onResume() {
    	super.onResume();
    	if(bid == -1) {
    		BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBufferByName(cid, name);
    		if(b != null) {
    			bid = b.bid;
    		}
    	}
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
    	if(bid != -1)
    		new RefreshTask().execute((Void)null);
    }
    
    private class HeartbeatTask extends AsyncTask<IRCCloudJSONObject, Void, Void> {

		@Override
		protected Void doInBackground(IRCCloudJSONObject... params) {
			IRCCloudJSONObject e = params[0];
			
	    	if(e.eid() > last_seen_eid) {
	    		getActivity().getIntent().putExtra("last_seen_eid", e.eid());
	    		NetworkConnection.getInstance().heartbeat(bid, e.cid(), e.bid(), e.eid());
	    		last_seen_eid = e.eid();
	    	}
			return null;
		}
    }
    
	private class RefreshTask extends AsyncTask<Void, Void, Void> {
		TreeMap<Long,IRCCloudJSONObject> events;
		ServersDataSource.Server server;
		BuffersDataSource.Buffer buffer;
		UsersDataSource.User user = null;
		
		@Override
		protected Void doInBackground(Void... params) {
	    	try {
	    		Log.i("IRCCloud", "Waiting for the initial page to load");
				webviewLock.acquire();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			webviewLock.release();
			buffer = BuffersDataSource.getInstance().getBuffer((int)bid);
			server = ServersDataSource.getInstance().getServer(cid);
			if(type.equalsIgnoreCase("conversation"))
				user = UsersDataSource.getInstance().getUser(cid, name);
			long time = System.currentTimeMillis();
			events = EventsDataSource.getInstance().getEventsForBuffer((int)bid);
			Log.i("IRCCloud", "Loaded data in " + (System.currentTimeMillis() - time) + "ms");
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if(events == null || (events.size() == 0 && min_eid > 0)) {
				if(bid != -1)
					conn.request_backlog(cid, bid, 0);
			} else if(events.size() > 0){
    			earliest_eid = events.firstKey();
    			if(events.firstKey() > min_eid)
    		    	webView.loadUrl("javascript:showBacklogBtn()");
    			else
    		    	webView.loadUrl("javascript:hideBacklogBtn()");
    			jsInterface.incomingBacklog = events;
		    	webView.loadUrl("javascript:appendBacklog()");
		    	if(events.size() > 0)
		    		new HeartbeatTask().execute(events.get(events.lastKey()));
			}
	    	if(type.equalsIgnoreCase("conversation") && buffer != null && buffer.away_msg != null && buffer.away_msg.length() > 0) {
	    		awayView.setVisibility(View.VISIBLE);
	    		awayView.setText("Away: " + buffer.away_msg);
	    	} else if(type.equalsIgnoreCase("conversation") && user != null && user.away == 1) {
	    		awayView.setVisibility(View.VISIBLE);
	    		if(user.away_msg != null && user.away_msg.length() > 0)
	    			awayView.setText("Away: " + user.away_msg);
	    		else
		    		awayView.setText("Away");
	    	} else {
	    		awayView.setVisibility(View.GONE);
	    		awayView.setText("");
	    	}
	    	try {
				update_status(server.status, new JSONObject(server.fail_info));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private class StatusRefreshRunnable implements Runnable {
		String status;
		JSONObject fail_info;
		
		public StatusRefreshRunnable(String status, JSONObject fail_info) {
			this.status = status;
			this.fail_info = fail_info;
		}

		@Override
		public void run() {
			update_status(status, fail_info);
		}
	}
	
	StatusRefreshRunnable statusRefreshRunnable = null;
	
	private void update_status(String status, JSONObject fail_info) {
		if(statusRefreshRunnable != null) {
			mHandler.removeCallbacks(statusRefreshRunnable);
			statusRefreshRunnable = null;
		}
		
    	if(status.equals("connected_ready")) {
    		statusView.setVisibility(View.GONE);
    		statusView.setText("");
    	} else if(status.equals("quitting")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Disconnecting");
    		statusView.setTextColor(getResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("disconnected")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Disconnected");
    		statusView.setTextColor(getResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("queued")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Connection queued");
    		statusView.setTextColor(getResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("connecting")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Connecting");
    		statusView.setTextColor(getResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("connected")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Connected");
    		statusView.setTextColor(getResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("connected_joining")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Connected: Joining Channels");
    		statusView.setTextColor(getResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	} else if(status.equals("pool_unavailable")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Disconnected: Connection temporarily unavailable");
    		statusView.setTextColor(getResources().getColor(R.color.status_fail_text));
    		statusView.setBackgroundResource(R.drawable.status_fail_bg);
    	} else if(status.equals("waiting_to_retry")) {
    		try {
	    		statusView.setVisibility(View.VISIBLE);
	    		long seconds = (fail_info.getLong("timestamp") + fail_info.getInt("retry_timeout")) - System.currentTimeMillis()/1000;
	    		statusView.setText("Disconnected: " + fail_info.getString("reason") + ". Reconnecting in " + seconds + " seconds.");
	    		statusView.setTextColor(getResources().getColor(R.color.status_fail_text));
	    		statusView.setBackgroundResource(R.drawable.status_fail_bg);
	    		statusRefreshRunnable = new StatusRefreshRunnable(status, fail_info);
	    		mHandler.postDelayed(statusRefreshRunnable, 500);
    		} catch (JSONException e) {
    			e.printStackTrace();
    		}
    	} else if(status.equals("ip_retry")) {
    		statusView.setVisibility(View.VISIBLE);
    		statusView.setText("Trying another IP address");
    		statusView.setTextColor(getResources().getColor(R.color.dark_blue));
    		statusView.setBackgroundResource(R.drawable.background_blue);
    	}
	}
	
    public void onPause() {
    	super.onPause();
		if(statusRefreshRunnable != null) {
			mHandler.removeCallbacks(statusRefreshRunnable);
			statusRefreshRunnable = null;
		}
    	if(conn != null)
    		conn.removeHandler(mHandler);
   	}
    
	@SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		IRCCloudJSONObject e;
		
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_USERINFO:
				new RefreshTask().execute((Void)null);
				break;
			case NetworkConnection.EVENT_STATUSCHANGED:
				try {
					IRCCloudJSONObject object = (IRCCloudJSONObject)msg.obj;
					if(object.getInt("cid") == cid) {
						update_status(object.getString("new_status"), object.getJSONObject("fail_info"));
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case NetworkConnection.EVENT_MAKEBUFFER:
				BuffersDataSource.Buffer buffer = (BuffersDataSource.Buffer)msg.obj;
				if(bid == -1 && buffer.cid == cid && buffer.name.equalsIgnoreCase(name)) {
					bid = buffer.bid;
		    		new RefreshTask().execute((Void)null);
				}
				break;
			case NetworkConnection.EVENT_DELETEBUFFER:
				if((Integer)msg.obj == bid) {
	                Intent parentActivityIntent = new Intent(getActivity(), MainActivity.class);
	                parentActivityIntent.addFlags(
	                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
	                        Intent.FLAG_ACTIVITY_NEW_TASK);
	                getActivity().startActivity(parentActivityIntent);
	                getActivity().finish();
				}
				break;
			case NetworkConnection.EVENT_SETIGNORES:
				e = (IRCCloudJSONObject)msg.obj;
				if(e.cid() == cid) {
					new RefreshTask().execute((Void)null);
				}
				break;
			case NetworkConnection.EVENT_BACKLOG_END:
				new RefreshTask().execute((Void)null);
				break;
			case NetworkConnection.EVENT_SELFBACK:
		    	try {
					e = (IRCCloudJSONObject)msg.obj;
					if(e.cid() == cid && e.getString("nick").equalsIgnoreCase(name)) {
			    		awayView.setVisibility(View.GONE);
						awayView.setText("");
					}
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
				break;
			case NetworkConnection.EVENT_AWAY:
		    	try {
					e = (IRCCloudJSONObject)msg.obj;
					if((e.bid() == bid || (e.type().equalsIgnoreCase("self_away") && e.cid() == cid)) && e.getString("nick").equalsIgnoreCase(name)) {
			    		awayView.setVisibility(View.VISIBLE);
						awayView.setText("Away: " + e.getString("msg"));
					}
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
				break;
			case NetworkConnection.EVENT_CHANNELTOPIC:
			case NetworkConnection.EVENT_JOIN:
			case NetworkConnection.EVENT_PART:
			case NetworkConnection.EVENT_NICKCHANGE:
			case NetworkConnection.EVENT_QUIT:
			case NetworkConnection.EVENT_BUFFERMSG:
			case NetworkConnection.EVENT_USERCHANNELMODE:
			case NetworkConnection.EVENT_KICK:
			case NetworkConnection.EVENT_CHANNELMODE:
			case NetworkConnection.EVENT_SELFDETAILS:
			case NetworkConnection.EVENT_USERMODE:
				e = (IRCCloudJSONObject)msg.obj;
				if(e.bid() == bid) {
					insertEvent(e);
					new HeartbeatTask().execute(e);
				}
				break;
			default:
				break;
			}
		}
	};
}
