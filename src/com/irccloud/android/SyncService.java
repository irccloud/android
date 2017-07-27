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

public class SyncService extends IntentService implements NetworkConnection.IRCEventHandler {
    public SyncService() {
        super("SyncService");
    }

    private boolean running;

    @Override
    protected void onHandleIntent(Intent intent) {
        NetworkConnection conn = NetworkConnection.getInstance();
        if (intent != null && conn.session != null && conn.getState() == NetworkConnection.STATE_DISCONNECTED) {
            android.util.Log.d("IRCCloud", "Syncing IRCCloud backlog");
            running = true;
            conn.addHandler(this);
            conn.notifier = true;
            conn.load();
            conn.connect();

            while(running) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    break;
                }
            }

            android.util.Log.d("IRCCloud", "Backlog downloaded, finishing up");
            conn.removeHandler(this);
            if(conn.notifier) {
                conn.disconnect();
            } else {
                android.util.Log.d("IRCCloud", "Socket was upgraded, not closing");
            }
            SyncReceiver.completeWakefulIntent(intent);
        }
    }

    @Override
    public void onIRCEvent(int message, Object object) {
        switch(message) {
            case NetworkConnection.EVENT_CONNECTIVITY:
                int state = NetworkConnection.getInstance().getState();
                if(state == NetworkConnection.STATE_CONNECTING || state == NetworkConnection.STATE_CONNECTED)
                    break;
            case NetworkConnection.EVENT_BACKLOG_FAILED:
            case NetworkConnection.EVENT_BACKLOG_END:
                running = false;
                break;
            default:
                break;
        }
    }
}
