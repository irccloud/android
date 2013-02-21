package com.irccloud.android;

import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GCMIntentService extends GCMBaseIntentService {

	public static final String GCM_ID = "";

	@Override
	protected void onError(Context context, String errorId) {
		Log.e("IRCCloud", "GCM Error: " + errorId);
	}

	public GCMIntentService() {
        super(GCM_ID);
	}
	
	protected String[] getSenderIds(Context context) {
		String[] ids = { GCM_ID };
		return ids;
	}
	
	@SuppressLint("NewApi")
	@Override
	protected void onMessage(Context context, Intent intent) {
		if(intent != null && intent.getExtras() != null) {
	    	//Log.d("IRCCloud", "GCM K/V pairs: " + intent.getExtras().toString());
	    	try {
		    	String type = intent.getStringExtra("type");
		    	if(type.equalsIgnoreCase("heartbeat_echo")) {
					NetworkConnection conn = NetworkConnection.getInstance();
		    		JsonParser parser = new JsonParser();
		    		JsonObject seenEids = parser.parse(intent.getStringExtra("seenEids")).getAsJsonObject();
					Iterator<Entry<String, JsonElement>> i = seenEids.entrySet().iterator();
					while(i.hasNext()) {
						Entry<String, JsonElement> entry = i.next();
						JsonObject eids = entry.getValue().getAsJsonObject();
						Iterator<Entry<String, JsonElement>> j = eids.entrySet().iterator();
						while(j.hasNext()) {
							Entry<String, JsonElement> eidentry = j.next();
							String bid = eidentry.getKey();
							long eid = eidentry.getValue().getAsLong();
							if(conn.ready && conn.getState() != NetworkConnection.STATE_CONNECTED)
								BuffersDataSource.getInstance().updateLastSeenEid(Integer.valueOf(bid), eid);
							Notifications.getInstance().deleteOldNotifications(Integer.valueOf(bid), eid);
							Notifications.getInstance().updateLastSeenEid(Integer.valueOf(bid), eid);
						}
					}
					Notifications.getInstance().showNotifications(null);
		    	} else {
			    	int cid = Integer.valueOf(intent.getStringExtra("cid"));
			    	int bid = Integer.valueOf(intent.getStringExtra("bid"));
			    	long eid = Long.valueOf(intent.getStringExtra("eid"));
			    	if(Notifications.getInstance().getNotification(eid) != null) {
			    		return;
			    	}
			    	String from = intent.getStringExtra("from_nick");
			    	String msg = intent.getStringExtra("msg");
			    	if(msg != null)
			    		msg = ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(TextUtils.htmlEncode(msg))).toString();
			    	String chan = intent.getStringExtra("chan");
			    	if(chan == null)
			    		chan = "";
			    	String buffer_type = intent.getStringExtra("buffer_type");
			    	String server_name = intent.getStringExtra("server_name");
			    	if(server_name == null || server_name.length() == 0)
			    		server_name = intent.getStringExtra("server_hostname");
			    	
			    	String network = Notifications.getInstance().getNetwork(cid);
			    	if(network == null)
			    		Notifications.getInstance().addNetwork(cid, server_name);
			    	
			    	Notifications.getInstance().addNotification(cid, bid, eid, from, msg, chan, buffer_type, type);

			    	if(from == null || from.length() == 0)
						Notifications.getInstance().showNotifications(server_name + ": " + msg);
			    	else if(buffer_type.equals("channel"))
						Notifications.getInstance().showNotifications(chan + ": <" + from + "> " + msg);
					else
						Notifications.getInstance().showNotifications(from + ": " + msg);
		    	}
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    		Log.w("IRCCloud", "Unable to parse GCM message");
	    	}
		}
	}

	public static void scheduleRegisterTimer(int delay) {
		final int retrydelay = delay;
		
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				if(!GCMRegistrar.isRegistered(IRCCloudApplication.getInstance().getApplicationContext()) || !IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).contains("session_key")) {
					return;
				}
				if(IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).contains("gcm_registered")) {
					return;
				}
				boolean success = false;
				try {
					JSONObject result = NetworkConnection.getInstance().registerGCM(GCMRegistrar.getRegistrationId(IRCCloudApplication.getInstance().getApplicationContext()), IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).getString("session_key", ""));
					if(result.has("success"))
						success = result.getBoolean("success");
				} catch (Exception e) {
					e.printStackTrace();
				}
				if(success) {
					SharedPreferences.Editor editor = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).edit();
					editor.putBoolean("gcm_registered", true);
					editor.commit();
				} else {
					Log.w("IRCCloud", "Failed to register device ID, will retry in " + ((retrydelay*2)/1000) + " seconds");
					scheduleRegisterTimer(retrydelay * 2);
				}
			}
			
		}, delay);
	}
	
	@Override
	protected void onRegistered(Context context, String regId) {
		scheduleRegisterTimer(15000);
	}

	public static void scheduleUnregisterTimer(int delay, final String regId) {
		final int retrydelay = delay;
		
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				boolean success = false;
				try {
					String session = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).getString(regId, "");
					if(session.length() == 0)
						session = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).getString("session_key", "");
					JSONObject result = NetworkConnection.getInstance().unregisterGCM(regId, session);
					if(result.has("success"))
						success = result.getBoolean("success");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(success) {
					SharedPreferences.Editor editor = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).edit();
					editor.remove("gcm_registered");
					editor.remove(regId);
					editor.commit();
				} else {
					Log.w("IRCCloud", "Failed to unregister device ID, will retry in " + ((retrydelay*2)/1000) + " seconds");
					scheduleUnregisterTimer(retrydelay * 2, regId);
				}
			}
		}, delay);
	}
	
	@Override
	protected void onUnregistered(Context context, String regId) {
		scheduleUnregisterTimer(1000, regId);
	}

}
