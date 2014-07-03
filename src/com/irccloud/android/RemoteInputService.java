/*
 * Copyright (c) 2014 IRCCloud, Ltd.
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

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

public class RemoteInputService extends IntentService {
    public static final String ACTION_REPLY = IRCCloudApplication.getInstance().getApplicationContext().getString(R.string.ACTION_REPLY);
    public RemoteInputService() {
        super("RemoteInputService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null && getSharedPreferences("prefs", 0).getString("session_key", "").length() > 0) {
            final String action = intent.getAction();
            if (ACTION_REPLY.equals(action)) {
                Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                if(remoteInput != null) {
                    Crashlytics.log(Log.INFO, "IRCCloud", "Got reply from RemoteInput");
                    String reply = remoteInput.getCharSequence("extra_reply").toString();
                    if(reply.length() > 0 && !reply.startsWith("/")) {
                        if (NetworkConnection.getInstance().getState() == NetworkConnection.STATE_CONNECTED) {
                            NetworkConnection.getInstance().incoming_reply_bid = intent.getIntExtra("bid", -1);
                            NetworkConnection.getInstance().incoming_reply_reqid = NetworkConnection.getInstance().say(intent.getIntExtra("cid", -1), intent.getStringExtra("to"), (intent.hasExtra("nick") ? intent.getStringExtra("nick") + ": " : "") + reply);
                        } else {
                            NetworkConnection.getInstance().incoming_reply_cid = intent.getIntExtra("cid", -1);
                            NetworkConnection.getInstance().incoming_reply_bid = intent.getIntExtra("bid", -1);
                            NetworkConnection.getInstance().incoming_reply_to = intent.getStringExtra("to");
                            NetworkConnection.getInstance().incoming_reply_msg = (intent.hasExtra("nick") ? intent.getStringExtra("nick") + ": " : "") + reply;
                            NetworkConnection.getInstance().connect(getSharedPreferences("prefs", 0).getString("session_key", ""));
                        }
                    }
                    NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int) (intent.getLongExtra("eid", 0) / 1000));
                    NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(intent.getIntExtra("bid", 0));
                    Notifications.getInstance().dismiss(intent.getIntExtra("bid", 0), intent.getLongExtra("eid", 0));
                    Notifications.getInstance().showNotifications(null);
                } else {
                    Crashlytics.log(Log.ERROR, "IRCCloud", "RemoteInputService received no remoteinput");
                }
            }
        }
    }
}
