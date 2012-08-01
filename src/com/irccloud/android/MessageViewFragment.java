package com.irccloud.android;

import java.util.ArrayList;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.SherlockFragment;
import com.irccloud.android.EventsDataSource.Event;

public class MessageViewFragment extends SherlockFragment {
	private NetworkConnection conn;
	private WebView webView;
	private long bid;
	private long last_seen_eid;
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View v = inflater.inflate(R.layout.messageview, container, false);
    	webView = (WebView)v.findViewById(R.id.messageview);
    	webView.getSettings().setJavaScriptEnabled(true);
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
			
			Log.d("IRCCloud", "Comparing: " + e.eid + " and " + last_seen_eid);
			
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
		
		@Override
		protected Void doInBackground(Void... params) {
			events = EventsDataSource.getInstance().getEventsForBuffer((int)bid);
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
		}
	}
    
    public void onPause() {
    	super.onPause();
    	if(conn != null)
    		conn.removeHandler(mHandler);
    	}
    
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_BACKLOG_END:
				new RefreshTask().execute((Void)null);
				break;
			case NetworkConnection.EVENT_BUFFERMSG:
				EventsDataSource.Event e = (EventsDataSource.Event)msg.obj;
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
