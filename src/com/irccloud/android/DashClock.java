/*
 * Copyright (c) 2015 IRCCloud, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irccloud.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.preference.PreferenceManager;
import android.text.Html;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;
import com.irccloud.android.activity.MainActivity;
import com.irccloud.android.data.collection.NotificationsList;
import com.irccloud.android.data.model.Notification;

import java.util.ArrayList;
import java.util.List;

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
                NetworkConnection.printStackTraceToCrashlytics(e);
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
                NetworkConnection.printStackTraceToCrashlytics(e);
            }
    }

    @Override
    protected void onUpdateData(int reason) {
        long count = NotificationsList.getInstance().count();
        if (count > 0) {
            String msg = "";
            List<Notification> msgs = NotificationsList.getInstance().getMessageNotifications();
            count = 0;
            for (Notification n : msgs) {
                if(n.nick != null) {
                    if (n.message_type.equals("buffer_me_msg"))
                        msg += "— " + n.nick + " " + Html.fromHtml(n.message) + "\n";
                    else
                        msg += "<" + n.nick + "> " + Html.fromHtml(n.message) + "\n";
                    count++;
                }
            }
            if(count > 0) {
                if (PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("dashclock_showmsgs", false)) {
                    publishUpdate(new ExtensionData()
                            .visible(true)
                            .icon(R.drawable.ic_stat_notify)
                            .status(String.valueOf(count))
                            .expandedTitle(String.valueOf(count) + " unread highlight" + ((count > 1) ? "s" : ""))
                            .expandedBody(msg)
                            .clickIntent(new Intent(IRCCloudApplication.getInstance().getApplicationContext(), MainActivity.class)));
                } else {
                    publishUpdate(new ExtensionData()
                            .visible(true)
                            .icon(R.drawable.ic_stat_notify)
                            .status(String.valueOf(count))
                            .expandedTitle(String.valueOf(count) + " unread highlight" + ((count > 1) ? "s" : ""))
                            .clickIntent(new Intent(IRCCloudApplication.getInstance().getApplicationContext(), MainActivity.class)));
                }
            } else {
                publishUpdate(null);
            }
        } else {
            publishUpdate(null);
        }
    }
}