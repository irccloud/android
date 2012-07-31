package com.irccloud.android;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragment;

public class MessageViewFragment extends SherlockFragment {
	NetworkConnection conn;
	WebView webView;
	private BuffersDataSource.Buffer buffer;
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View v = inflater.inflate(R.layout.messageview, container, false);
    	webView = (WebView)v.findViewById(R.id.messageview);
    	webView.getSettings().setJavaScriptEnabled(true);
    	return v;
    }
	
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	buffer = BuffersDataSource.getInstance().getBuffer((int)activity.getIntent().getLongExtra("bid", 0));
    }

    private void insertEvent(EventsDataSource.Event event) {
    	webView.loadUrl("javascript:appendEvent(("+event.event.toString()+"))");
    	webView.pageDown(true);
    }
    
    public void onResume() {
    	super.onResume();
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
    	refresh();
    }
    
    private void refresh() {
    	webView.setWebViewClient(new WebViewClient() {
    	    @Override  
    	    public void onPageFinished(WebView view, String url) {
    	    	ArrayList<EventsDataSource.Event> events = EventsDataSource.getInstance().getEventsForBuffer(buffer.bid);
    	    	for(int i = 0; i < events.size(); i++) {
    	    		insertEvent(events.get(i));
    	    	}
    	    	webView.setWebViewClient(null);
    	    }
    	});
    	webView.loadUrl("file:///android_asset/messageview.html");
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
				refresh();
				break;
			case NetworkConnection.EVENT_BUFFERMSG:
				EventsDataSource.Event e = (EventsDataSource.Event)msg.obj;
				if(e.bid == buffer.bid)
					insertEvent(e);
				break;
			default:
				break;
			}
		}
	};
}
