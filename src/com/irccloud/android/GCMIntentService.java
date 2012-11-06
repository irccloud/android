package com.irccloud.android;

import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

public class GCMIntentService extends GCMBaseIntentService {

	public static final String GCM_ID = "";

	public static int versionCode() {
		int code = 0;
		try {
			code = IRCCloudApplication.getInstance().getPackageManager().getPackageInfo("com.irccloud.android", 0).versionCode;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return code;
	}
	
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
	    	try {
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
				boolean success = false;
				try {
					JSONObject result = NetworkConnection.getInstance().registerGCM(GCMRegistrar.getRegistrationId(IRCCloudApplication.getInstance().getApplicationContext()));
					if(result.has("success"))
						success = result.getBoolean("success");
					if(!success && result.has("message") && result.getString("message").equals("already_registered"))
						success = true;
				} catch (Exception e) {
					e.printStackTrace();
				}
				if(success) {
					SharedPreferences.Editor editor = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).edit();
					editor.putBoolean("gcm_registered", true);
					editor.commit();
				} else {
					Log.e("IRCCloud", "Failed to register device ID, will retry in " + ((retrydelay*2)/1000) + " seconds");
					scheduleRegisterTimer(retrydelay * 2);
				}
			}
			
		}, delay);
	}
	
	@Override
	protected void onRegistered(Context context, String regId) {
		Log.i("IRCCloud", "GCM registered, id: " + regId);
		scheduleRegisterTimer(30000);
		SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
		editor.putInt("GCM_VERSION", versionCode());
		editor.commit();
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
		Log.i("IRCCloud", "GCM unregistered, id: " + regId);
		SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
		editor.remove("gcm_registered");
		editor.commit();
		if(getSharedPreferences("prefs", 0).contains("session_key") && getSharedPreferences("prefs", 0).getInt("GCM_VERSION", 0) != versionCode())
			GCMRegistrar.register(this, GCM_ID);
	}

}
