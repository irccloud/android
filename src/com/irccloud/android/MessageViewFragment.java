package com.irccloud.android;

import java.util.ArrayList;

import org.json.JSONException;

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
import android.webkit.WebView;
import android.webkit.WebChromeClient;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.irccloud.android.EventsDataSource.Event;

public class MessageViewFragment extends SherlockFragment {
	private NetworkConnection conn;
	private WebView webView;
	private TextView topicView;
	private int cid;
	private long bid;
	private long last_seen_eid;
	private long min_eid;
	private long earliest_eid;
	private String name;
	
	public class JavaScriptInterface {
		public void requestBacklog() {
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
	}
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View v = inflater.inflate(R.layout.messageview, container, false);
    	webView = (WebView)v.findViewById(R.id.messageview);
    	webView.getSettings().setJavaScriptEnabled(true);
    	webView.addJavascriptInterface(new JavaScriptInterface(), "Android");
    	webView.setWebChromeClient(new WebChromeClient() {
    		  public void onConsoleMessage(String message, int lineNumber, String sourceID) {
    		    Log.d("IRCCloud", message + " -- From line "
    		                         + lineNumber + " of "
    		                         + sourceID);
    		  }
    		});
    	webView.loadUrl("file:///android_asset/messageview.html");
    	topicView = (TextView)v.findViewById(R.id.topicView);
    	return v;
    }
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState.containsKey("cid")) {
        	cid = savedInstanceState.getInt("cid");
        	bid = savedInstanceState.getLong("bid");
        	name = savedInstanceState.getString("name");
        	last_seen_eid = savedInstanceState.getLong("last_seen_eid");
        	min_eid = savedInstanceState.getLong("min_eid");
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
    }

    
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	if(activity.getIntent() != null && activity.getIntent().hasExtra("cid")) {
	    	cid = activity.getIntent().getIntExtra("cid", 0);
	    	bid = activity.getIntent().getLongExtra("bid", 0);
	    	last_seen_eid = activity.getIntent().getLongExtra("last_seen_eid", 0);
	    	min_eid = activity.getIntent().getLongExtra("min_eid", 0);
	    	name = activity.getIntent().getStringExtra("name");
    	}
    }

    private void insertEvent(EventsDataSource.Event event) {
    	if(event.eid == min_eid)
	    	webView.loadUrl("javascript:hideBacklogBtn()");
    	if(event.eid < earliest_eid)
    		earliest_eid = event.eid;
    	webView.loadUrl("javascript:appendEvent(("+event.event.toString()+"))");
    }
    
    public void onResume() {
    	super.onResume();
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
    	if(bid != -1)
    		new RefreshTask().execute((Void)null);
    }
    
    private class HeartbeatTask extends AsyncTask<EventsDataSource.Event, Void, Void> {

		@Override
		protected Void doInBackground(Event... params) {
			Event e = params[0];
			
	    	if(e.eid > last_seen_eid) {
	    		getActivity().getIntent().putExtra("last_seen_eid", e.eid);
	    		NetworkConnection.getInstance().heartbeat(bid, e.cid, e.bid, e.eid);
	    		last_seen_eid = e.eid;
	    	}
			return null;
		}
    }
    
	private class RefreshTask extends AsyncTask<Void, Void, Void> {
		ArrayList<EventsDataSource.Event> events;
		ChannelsDataSource.Channel channel;
		
		@Override
		protected Void doInBackground(Void... params) {
			events = EventsDataSource.getInstance().getEventsForBuffer((int)bid);
			channel = ChannelsDataSource.getInstance().getChannelForBuffer(bid);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if(events.size() == 0 && min_eid > 0) {
				conn.request_backlog(cid, bid, 0);
			} else {
		    	for(int i = 0; i < events.size(); i++) {
		    		if(i == 0) {
		    			earliest_eid = events.get(i).eid;
		    			if(events.get(i).eid > min_eid)
		    		    	webView.loadUrl("javascript:showBacklogBtn()");
		    		}
		    		insertEvent(events.get(i));
		    	}
		    	if(events.size() > 0)
		    		new HeartbeatTask().execute(events.get(events.size()-1));
		    	if(channel != null && channel.topic_text != null && channel.topic_text.length() > 0) {
		    		topicView.setVisibility(View.VISIBLE);
		    		topicView.setText(channel.topic_text);
		    	} else {
		    		topicView.setVisibility(View.GONE);
		    		topicView.setText("");
		    	}
			}
		}
	}
    
    public void onPause() {
    	super.onPause();
    	if(conn != null)
    		conn.removeHandler(mHandler);
    	}
    
	private final Handler mHandler = new Handler() {
		EventsDataSource.Event e;
		
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_MAKEBUFFER:
				BuffersDataSource.Buffer buffer = (BuffersDataSource.Buffer)msg.obj;
				if(bid == -1 && buffer.cid == cid && buffer.name.equalsIgnoreCase(name)) {
					bid = buffer.bid;
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
			case NetworkConnection.EVENT_BACKLOG_END:
				new RefreshTask().execute((Void)null);
				break;
			case NetworkConnection.EVENT_CHANNELTOPIC:
		    	try {
					e = (EventsDataSource.Event)msg.obj;
		    		topicView.setVisibility(View.VISIBLE);
					topicView.setText(e.event.getString("topic"));
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
			case NetworkConnection.EVENT_JOIN:
			case NetworkConnection.EVENT_PART:
			case NetworkConnection.EVENT_NICKCHANGE:
			case NetworkConnection.EVENT_QUIT:
			case NetworkConnection.EVENT_BUFFERMSG:
			case NetworkConnection.EVENT_USERCHANNELMODE:
				e = (EventsDataSource.Event)msg.obj;
				if(e.bid == bid) {
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
