package com.irccloud.android;

import android.content.*;
import android.net.Uri;
import android.preference.PreferenceManager;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

public class DashClock extends DashClockExtension {
    public final static String REFRESH_INTENT = "com.irccloud.android.dashclock.REFRESH";
    RefreshReceiver receiver;

    class RefreshReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            onUpdateData(0);
        }
    }

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);
        if (receiver != null)
            try {
                unregisterReceiver(receiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        IntentFilter intentFilter = new IntentFilter(REFRESH_INTENT);
        receiver = new RefreshReceiver();
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null)
            try {
                unregisterReceiver(receiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    @Override
    protected void onUpdateData(int reason) {
        int count = Notifications.getInstance().count();
        if(count > 0) {
            publishUpdate(new ExtensionData()
                    .visible(true)
                    .icon(R.drawable.ic_stat_notify)
                    .status(String.valueOf(count))
                    .expandedTitle(String.valueOf(count) + " unread highlight" + ((count > 1)?"s":""))
                    .clickIntent(new Intent(IRCCloudApplication.getInstance().getApplicationContext(), MainActivity.class)));
        } else {
            publishUpdate(null);
        }
    }
}