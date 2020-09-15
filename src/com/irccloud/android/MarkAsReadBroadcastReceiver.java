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
import android.os.StrictMode;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.irccloud.android.data.collection.NotificationsList;

public class MarkAsReadBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent i) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        if (i.hasExtra("eids")) {
            int cid = i.getIntExtra("cid", -1);
            int bid = i.getIntExtra("bid", -1);
            long[] eids = i.getLongArrayExtra("eids");
            long highestEid = 0;
            for (int j = 0; j < eids.length; j++) {
                if (eids[j] > 0) {
                    if (eids[j] > highestEid)
                        highestEid = eids[j];
                    IRCCloudLog.Log(Log.INFO, "IRCCloud", "Dismiss bid" + bid + " eid" + eids[j]);
                    NotificationsList.getInstance().dismiss(bid, eids[j]);
                    NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int) (eids[j] / 1000));
                }
            }
            IRCCloudLog.Log(Log.INFO, "IRCCloud", "Mark as read bid" + bid);
            NetworkConnection.getInstance().postHeartbeat(cid, bid, highestEid, IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).getString("session_key", ""));
            IRCCloudLog.Log(Log.INFO, "IRCCloud", "Cancel bid" + bid);
            NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(bid);
        }
    }
}
