package com.irccloud.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class LaunchBidBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if(isOrderedBroadcast()) {
			Intent i = new Intent(context, MainActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			i.putExtra("bid", intent.getIntExtra("bid", -1));
			context.startActivity(i);
			abortBroadcast();
		} else { //Re-send as an ordered broadcast so the MessageActivity can cancel if it's running
			Intent i = new Intent("com.irccloud.android.LAUNCH_BID");
			i.putExtra("bid", intent.getIntExtra("bid", -1));
			context.sendOrderedBroadcast(i, null);
		}
	}

}
