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

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.irccloud.android.data.collection.NotificationsList;
import com.irccloud.android.data.collection.RecentConversationsList;

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
        if (intent != null && sk != null && sk.length() > 0) {
            if(intent.hasExtra("eid"))
                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int)(intent.getLongExtra("eid", -1) / 1000));
            final String action = intent.getAction();
            if (ACTION_REPLY.equals(action)) {
                Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                if (remoteInput != null || intent.hasExtra("reply")) {
                    Crashlytics.log(Log.INFO, "IRCCloud", "Got reply from RemoteInput");
                    String reply = remoteInput != null?remoteInput.getCharSequence("extra_reply").toString():intent.getStringExtra("reply");
                    if (reply.length() > 0 && !reply.contains("\n/") && (!reply.startsWith("/") || reply.toLowerCase().startsWith("/me ") || reply.toLowerCase().startsWith("/slap ") || reply.toLowerCase().startsWith("/accept "))) {
                        try {
                            JSONObject o = NetworkConnection.getInstance().postSay(intent.getIntExtra("cid", -1), intent.getStringExtra("to"), reply, sk);
                            success = o.getBoolean("success");
                        } catch (Exception e) {
                            NetworkConnection.printStackTraceToCrashlytics(e);
                        }
                    }

                    if (success) {
                        if(reply.startsWith("/accept ")) {
                            NotificationsList.getInstance().addNotification(intent.getIntExtra("cid", -1), 0, intent.getLongExtra("eid", System.currentTimeMillis() * 1000), intent.getStringExtra("to"), "was added to your accept list", intent.getStringExtra("chan"), intent.getStringExtra("buffer_type"), "callerid_success", intent.getStringExtra("network"), null);
                        } else {
                            String type = "buffer_msg";
                            if(reply.startsWith("/me ")) {
                                type = "buffer_me_msg";
                                reply = reply.substring(3);
                            } else if(reply.startsWith("/slap")) {
                                type = "buffer_me_msg";
                                reply = "slapped";
                            }
                            if(intent.getIntExtra("bid", -1) != -1)
                                NotificationsList.getInstance().addNotification(intent.getIntExtra("cid", -1), intent.getIntExtra("bid", -1), System.currentTimeMillis() * 1000, null, reply, intent.getStringExtra("chan"), intent.getStringExtra("buffer_type"), type, intent.getStringExtra("network"), null);
                        }
                        NotificationsList.getInstance().showNotificationsNow();
                        RecentConversationsList.getInstance().updateConversation(intent.getIntExtra("cid", -1), intent.getIntExtra("bid", -1), System.currentTimeMillis());
                    } else {
                        NotificationsList.getInstance().alert(intent.getIntExtra("bid", -1), "Sending Failed", reply.startsWith("/") ? "Please launch the IRCCloud app to send this command" : "Your message was not sent. Please try again shortly.");
                    }
                } else {
                    Crashlytics.log(Log.ERROR, "IRCCloud", "RemoteInputService received no remoteinput");
                }
            }
        }
    }
}
