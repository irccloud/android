package com.irccloud.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationDismissBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context ctx, Intent i) {
		if(i.hasExtra("eids")) {
			int bid = i.getIntExtra("bid", -1);
			long[] eids = i.getLongArrayExtra("eids");
			for(int j = 0; j < eids.length; j++) {
				if(eids[j] > 0) {
					Notifications.getInstance().dismiss(bid, eids[j]);
				}
			}
		}
        IRCCloudApplication.getInstance().getApplicationContext().sendBroadcast(new Intent(DashClock.REFRESH_INTENT));
	}
}
