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
	    	Log.i("IRCCloud", "GCM K/V pairs: " + intent.getExtras().toString());
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
