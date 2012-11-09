package com.irccloud.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationDismissBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent i) {
		Log.d("IRCCloud", "Dismiss notification!");
		if(i.hasExtra("eids")) {
			int[] bids = i.getIntArrayExtra("bids");
			long[] eids = i.getLongArrayExtra("eids");
			for(int j = 0; j < eids.length; j++) {
				Log.d("IRCCloud", "Dismiss: " + eids[j]);
				Notifications.getInstance().dismiss(bids[j], eids[j]);
			}
		}
	}

}
