package com.irccloud.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

public class PushyBroadcastReceiver extends WakefulBroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        WakefulBroadcastReceiver.startWakefulService(context, intent.setComponent(new ComponentName(context.getPackageName(), PushyIntentService.class.getName())));
        if (isOrderedBroadcast()) {
            setResultCode(-1);
        }
    }
}
