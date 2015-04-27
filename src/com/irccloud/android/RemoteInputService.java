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

import org.json.JSONObject;

public class RemoteInputService extends IntentService {
    public static final String ACTION_REPLY = IRCCloudApplication.getInstance().getApplicationContext().getString(R.string.ACTION_REPLY);

    public RemoteInputService() {
        super("RemoteInputService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        boolean success = false;
        String sk = getSharedPreferences("prefs", 0).getString("session_key", "");
        if (intent != null && sk.length() > 0) {
            final String action = intent.getAction();
            if (ACTION_REPLY.equals(action)) {
                Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                if (remoteInput != null || intent.hasExtra("reply")) {
                    Crashlytics.log(Log.INFO, "IRCCloud", "Got reply from RemoteInput");
                    String reply = remoteInput != null?remoteInput.getCharSequence("extra_reply").toString():intent.getStringExtra("reply");
                    if (reply.length() > 0 && !reply.startsWith("/")) {
                        try {
                            JSONObject o = NetworkConnection.getInstance().say(intent.getIntExtra("cid", -1), intent.getStringExtra("to"), reply, sk);
                            success = o.getBoolean("success");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(intent.getIntExtra("bid", 0));
                    if (intent.hasExtra("eids")) {
                        int bid = intent.getIntExtra("bid", -1);
                        long[] eids = intent.getLongArrayExtra("eids");
                        for (int j = 0; j < eids.length; j++) {
                            if (eids[j] > 0) {
                                Notifications.getInstance().dismiss(bid, eids[j]);
                            }
                        }
                    }
                    Notifications.getInstance().showNotifications(null);
                    if (!success)
                        Notifications.getInstance().alert(intent.getIntExtra("bid", -1), "Sending Failed", "Your message was not sent. Please try again shortly.");
                } else {
                    Crashlytics.log(Log.ERROR, "IRCCloud", "RemoteInputService received no remoteinput");
                }
            }
        }
    }
}
