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

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.gcm.GcmListenerService;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.collection.EventsList;
import com.irccloud.android.data.collection.NotificationsList;
import com.irccloud.android.data.model.Buffer;

import java.util.Iterator;
import java.util.Map;

public class GCMService extends GcmListenerService {

    @Override
    public void onMessageReceived(String id, Bundle data) {
        if(!data.containsKey("type"))
            super.onMessageReceived(id, data);

        try {
            if(!NetworkConnection.getInstance().ready)
                NetworkConnection.getInstance().load();

            String type = data.getString("type");
            if (type != null && type.equalsIgnoreCase("heartbeat_echo") && data.getString("seenEids") != null) {
                NetworkConnection conn = NetworkConnection.getInstance();
                ObjectMapper mapper = new ObjectMapper();
                JsonParser parser = mapper.getFactory().createParser(data.getString("seenEids"));
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
                int cid = Integer.valueOf(data.getString("cid"));
                int bid = Integer.valueOf(data.getString("bid"));
                long eid = Long.valueOf(data.getString("eid"));
                if (NotificationsList.getInstance().getNotification(eid) != null) {
                    Log.e("IRCCloud", "GCM got EID that already exists");
                    return;
                }

                /*if(NetworkConnection.getInstance().getState() == NetworkConnection.STATE_DISCONNECTED) {
                    NetworkConnection.getInstance().notifier = true;
                    NetworkConnection.getInstance().connect();
                    //NetworkConnection.getInstance().request_backlog(cid, bid, 0);
                }*/

                String from = data.getString("from_nick");
                String msg = data.getString("msg");
                if(type.equals("channel_invite"))
                    msg = "Invitation to join " + data.getString("channel");
                if (msg != null)
                    msg = ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(TextUtils.htmlEncode(msg))).toString();
                String chan = data.getString("chan");
                if (chan == null)
                    chan = "";
                String buffer_type = data.getString("buffer_type");
                String server_name = data.getString("server_name");
                if (server_name == null || server_name.length() == 0)
                    server_name = data.getString("server_hostname");

                NotificationsList.getInstance().addNotification(cid, bid, eid, (from == null)?data.getString("server_hostname"):from, msg, chan, buffer_type, type, server_name);

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
            //Log.d("IRCCloud", "Notifications: " + NotificationsList.getInstance().getNotifications());
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("IRCCloud", "Unable to parse GCM message");
        }
    }
}
