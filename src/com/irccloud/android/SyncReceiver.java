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
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.irccloud.android.data.collection.ServersList;

public class SyncReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(ServersList.getInstance().count() > 0) {
            Crashlytics.log(Log.INFO, "IRCCloud", "Launching backlog sync service");
            Intent service = new Intent(context, SyncService.class);
            startWakefulService(context, service);
        } else {
            Crashlytics.log(Log.INFO, "IRCCloud", "App not ready, cancelling sync");
            BackgroundTaskService.cancelBacklogSync(context);
        }
    }
}
