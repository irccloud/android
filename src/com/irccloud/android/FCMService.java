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

import android.preference.PreferenceManager;
import android.util.Log;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.collection.EventsList;
import com.irccloud.android.data.collection.NotificationsList;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.model.Event;
import com.irccloud.android.data.model.Server;

import java.util.Iterator;
import java.util.Map;

public class FCMService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        BackgroundTaskWorker.registerGCM(s);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Map<String, String> data = message.getData();

        if(!data.containsKey("type"))
            super.onMessageReceived(message);

        if(NetworkConnection.getInstance().session == null || NetworkConnection.getInstance().session.length() == 0) {
            Log.e("IRCCloud", "Got a FCM while logged out, deleting token");
            try {
                FirebaseInstanceId.getInstance().deleteInstanceId();
            } catch (Exception e) {
                NetworkConnection.printStackTraceToCrashlytics(e);
            }
            return;
        }

        //Log.d("IRCCloud", "FCM K/V pairs: " + data);
        try {
            if(!NetworkConnection.getInstance().ready)
                NetworkConnection.getInstance().load();

            String type = data.get("type");
            if (type != null && type.equalsIgnoreCase("heartbeat_echo") && data.get("seenEids") != null) {
                NetworkConnection conn = NetworkConnection.getInstance();
                ObjectMapper mapper = new ObjectMapper();
                JsonParser parser = mapper.getFactory().createParser(data.get("seenEids"));
                JsonNode seenEids = mapper.readTree(parser);
                Iterator<Map.Entry<String, JsonNode>> iterator = seenEids.fields();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> entry = iterator.next();
                    JsonNode eids = entry.getValue();
                    Iterator<Map.Entry<String, JsonNode>> j = eids.fields();
                    while (j.hasNext()) {
                        Map.Entry<String, JsonNode> eidentry = j.next();
                        String bid = eidentry.getKey();
                        long eid = eidentry.getValue().asLong();
                        if (conn.ready && conn.getState() != NetworkConnection.STATE_CONNECTED) {
                            Buffer b = BuffersList.getInstance().getBuffer(Integer.valueOf(bid));
                            if (b != null) {
                                b.setLast_seen_eid(eid);
                                if (EventsList.getInstance().lastEidForBuffer(b.getBid()) <= eid) {
                                    b.setUnread(0);
                                    b.setHighlights(0);
                                    //TransactionManager.getInstance().saveOnSaveQueue(b);
                                }
                            }
                        }
                        NotificationsList.getInstance().updateLastSeenEid(Integer.valueOf(bid), eid);
                        NotificationsList.getInstance().deleteOldNotifications();
                    }
                }
                parser.close();
            } else {
                int cid = Integer.valueOf(data.get("cid"));
                int bid = Integer.valueOf(data.get("bid"));
                long eid = Long.valueOf(data.get("eid"));

                /*if(NetworkConnection.getInstance().getState() == NetworkConnection.STATE_DISCONNECTED) {
                    NetworkConnection.getInstance().notifier = true;
                    NetworkConnection.getInstance().connect();
                    //NetworkConnection.getInstance().request_backlog(cid, bid, 0);
                }*/

                String from = data.get("from_nick");
                String msg = data.get("msg");
                String chan = data.get("chan");
                if(from == null && data.containsKey("nick"))
                    from = data.get("nick");
                if(type != null && type.equals("channel_invite")) {
                    chan = data.get("channel");
                }
                if (msg != null)
                    msg = ColorFormatter.strip(msg).toString();
                if (chan == null)
                    chan = from;
                String buffer_type = data.get("buffer_type");
                String server_name = data.get("server_name");
                if (server_name == null || server_name.length() == 0)
                    server_name = data.get("server_hostname");

                Server s = ServersList.getInstance().getServer(cid);
                if(s != null)
                    s.setNick(data.get("server_nick"));

                String avatar_url = null;

                Event e = new Event();
                e.from = from;
                e.msg = msg;
                e.type = type;
                e.hostmask = data.get("hostmask");
                if (data.containsKey("avatar"))
                    e.avatar = data.get("avatar");
                if (data.containsKey("avatar_url"))
                    e.avatar_url = data.get("avatar_url");

                avatar_url = e.getAvatarURL(512, buffer_type != null && buffer_type.equals("channel"), NotificationsList.getInstance().getServerIsSlack(cid));

                NotificationsList.getInstance().addNotificationGroup(cid, server_name);
                NotificationsList.getInstance().updateServerNick(cid, data.get("server_nick"));
                NotificationsList.getInstance().addNotification(cid, bid, eid, (from == null)?data.get("server_hostname"):from, msg, chan, buffer_type, type, server_name, avatar_url);

                if (from == null || from.length() == 0)
                    NotificationsList.getInstance().showNotifications(server_name + ": " + msg);
                else if (buffer_type != null && buffer_type.equals("channel")) {
                    if (type != null && type.equals("buffer_me_msg"))
                        NotificationsList.getInstance().showNotifications(chan + ": — " + from + " " + msg);
                    else
                        NotificationsList.getInstance().showNotifications(chan + ": <" + from + "> " + msg);
                } else {
                    if (type != null && type.equals("buffer_me_msg"))
                        NotificationsList.getInstance().showNotifications("— " + from + " " + msg);
                    else
                        NotificationsList.getInstance().showNotifications(from + ": " + msg);
                }
            }
        } catch (Exception e) {
            NetworkConnection.printStackTraceToCrashlytics(e);
            Log.w("IRCCloud", "Unable to parse FCM message");
        }
    }
}
