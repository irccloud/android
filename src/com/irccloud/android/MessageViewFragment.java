package com.irccloud.android;

import java.util.ArrayList;

import org.json.JSONException;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.irccloud.android.EventsDataSource.Event;

public class MessageViewFragment extends SherlockFragment {
	private NetworkConnection conn;
	private WebView webView;
	private TextView topicView;
	private long bid;
	private long last_seen_eid;
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View v = inflater.inflate(R.layout.messageview, container, false);
    	webView = (WebView)v.findViewById(R.id.messageview);
    	webView.getSettings().setJavaScriptEnabled(true);
    	topicView = (TextView)v.findViewById(R.id.topicView);
    	return v;
    }
	
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	bid = activity.getIntent().getLongExtra("bid", 0);
    	last_seen_eid = activity.getIntent().getLongExtra("last_seen_eid", 0);
    }

    private void insertEvent(EventsDataSource.Event event) {
    	webView.loadUrl("javascript:appendEvent(("+event.event.toString()+"))");
    }
    
    public void onResume() {
    	super.onResume();
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
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
	    	webView.setWebViewClient(new WebViewClient() {
	    	    @Override  
	    	    public void onPageFinished(WebView view, String url) {
	    	    	webView.setWebViewClient(null);
	    	    	for(int i = 0; i < events.size(); i++) {
	    	    		insertEvent(events.get(i));
	    	    	}
	    	    	if(events.size() > 0)
	    	    		new HeartbeatTask().execute(events.get(events.size()-1));
	    	    }
	    	});
	    	webView.loadUrl("file:///android_asset/messageview.html");
	    	if(channel != null && channel.topic_text != null && channel.topic_text.length() > 0) {
	    		topicView.setVisibility(View.VISIBLE);
	    		topicView.setText(channel.topic_text);
	    	} else {
	    		topicView.setVisibility(View.GONE);
	    		topicView.setText("");
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
			case NetworkConnection.EVENT_BUFFERMSG:
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
