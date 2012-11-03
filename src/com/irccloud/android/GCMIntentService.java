package com.irccloud.android;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {

	@Override
	protected void onError(Context context, String errorId) {
		Log.e("IRCCloud", "GCM Error: " + errorId);
	}

	@SuppressLint("NewApi")
	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.i("IRCCloud", "Recieved GCM message!");
		if(intent != null && intent.getExtras() != null) {
	    	Log.d("IRCCloud", "GCM K/V pairs: " + intent.getExtras().toString());
	    	int cid = Integer.valueOf(intent.getStringExtra("cid"));
	    	int bid = Integer.valueOf(intent.getStringExtra("bid"));
	    	long eid = Long.valueOf(intent.getStringExtra("eid"));
	    	if(Notifications.getInstance().getNotification(eid) != null) {
	    		Log.d("IRCCloud", "A notification for this event already exists in the db, ignoring");
	    		return;
	    	}
	    	String from = intent.getStringExtra("from_nick");
	    	String msg = intent.getStringExtra("msg");
	    	String chan = intent.getStringExtra("chan");
	    	if(chan == null)
	    		chan = "";
	    	String type = intent.getStringExtra("type");
	    	String buffer_type = intent.getStringExtra("buffer_type");
	    	String server_name = intent.getStringExtra("server_name");
	    	if(server_name == null || server_name.length() == 0)
	    		server_name = intent.getStringExtra("server_hostname");
	    	
	    	Notifications.Network network = Notifications.getInstance().getNetwork(cid);
	    	if(network == null)
	    		Notifications.getInstance().addNetwork(cid, server_name);
	    	
	    	Notifications.getInstance().deleteNotification(cid, bid, eid);
	    	Notifications.getInstance().addNotification(cid, bid, eid, from, msg, chan, buffer_type, type);
	    	
			if(buffer_type.equals("channel"))
				Notifications.getInstance().showNotifications(chan + ": <" + from + "> " + ColorFormatter.html_to_spanned(msg));
			else
				Notifications.getInstance().showNotifications(from + ": " + ColorFormatter.html_to_spanned(msg));
		}
	}

	@Override
	protected void onRegistered(Context context, String regId) {
		Log.i("IRCCloud", "GCM registered, id: " + regId);
		try {
			NetworkConnection.getInstance().registerGCM(regId);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
		Log.i("IRCCloud", "GCM unregistered, id: " + regId);
	}

}
