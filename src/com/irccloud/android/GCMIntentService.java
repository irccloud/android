package com.irccloud.android;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {

	@Override
	protected void onError(Context context, String errorId) {
		Log.e("IRCCloud", "GCM Error: " + errorId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.i("IRCCloud", "Recieved GCM message!");
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
