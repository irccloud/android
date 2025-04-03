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

package com.irccloud.android.data.model;

import android.text.Spanned;
import android.util.Patterns;

import com.damnhandy.uri.template.UriTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.BuildConfig;
import com.irccloud.android.Ignore;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.collection.ImageList;
import com.irccloud.android.data.collection.ServersList;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.TimerTask;
import java.util.regex.Matcher;

public class Event {
    public int cid;

    public int bid;

    public long eid;

    public long server_time;

    public String type;

    public String msg;

    public String hostmask;

    //@Column(name = "event_from")
    public String from;

    public String from_nick;

    public String from_mode;

    public String from_realname;

    public String from_hostmask;

    public String nick;

    public String old_nick;

    public String server;

    public String diff;

    public String chan;

    public boolean highlight;

    public boolean self;

    public boolean to_chan;

    public boolean to_buffer;

    public int color;

    public int bg_color;

    public JsonNode ops;

    public long group_eid;

    public int row_type = 0;

    public String group_msg;

    public boolean linkify = true;

    public boolean linkified;

    public String target_mode;

    public int reqid;

    public boolean pending;

    public boolean failed;

    public String command;

    public int day = -1;

    public Spanned contentDescription;

    public JsonNode entities;

    public String avatar;

    public String avatar_url;

    public String msgid;

    public String account;

    public String timestamp;
    public String html;
    public String html_prefix;
    public Spanned formatted;
    public Spanned formatted_nick;
    public Spanned formatted_realname;
    public TimerTask expiration_timer;
    public boolean header;
    public boolean quoted;
    public boolean code_block;
    public long parent_eid;
    public boolean is_reply;
    public int reply_count;
    public HashSet<String> reply_nicks;
    public int mention_offset;
    public boolean ready_for_display;
    public long lastEditEID;
    public boolean edited;
    public boolean deleted;
    public String redactedReason;

    public Event() {

    }

    public Event(Event e) {
        cid = e.cid;
        bid = e.bid;
        eid = e.eid;
        server_time = e.server_time;
        type = e.type;
        msg = e.msg;
        hostmask = e.hostmask;
        from = e.from;
        from_mode = e.from_mode;
        from_realname = e.from_realname;
        from_hostmask = e.from_hostmask;
        nick = e.nick;
        old_nick = e.old_nick;
        server = e.server;
        diff = e.diff;
        chan = e.chan;
        highlight = e.highlight;
        self = e.self;
        to_chan = e.to_chan;
        to_buffer = e.to_buffer;
        color = e.color;
        bg_color = e.bg_color;
        ops = e.ops;
        group_eid = e.group_eid;
        row_type = e.row_type;
        group_msg = e.group_msg;
        linkify = e.linkify;
        target_mode = e.target_mode;
        reqid = e.reqid;
        pending = e.pending;
        failed = e.failed;
        command = e.command;
        day = e.day;
        contentDescription = e.contentDescription;
        entities = e.entities;
        account = e.account;
    }

    public String toString() {
        return "{" +
                "cid: " + cid +
                ", bid: " + bid +
                ", eid: " + eid +
                ", type: " + type +
                ", timestamp: " + timestamp +
                ", from: " + from +
                ", hostmask: " + hostmask +
                ", msg: " + msg +
                ", html: " + html +
                ", formatted: " + formatted +
                ", group_eid: " + group_eid +
                ", group_msg: " + group_msg +
                ", pending: " + pending +
                ", self: " + self +
                ", msgid: " + msgid +
                ", account: " + account +
                ", header: " + header +
                ", avatar: " + avatar +
                ", avatar_url: " + avatar_url +
                ", getAvatarURL: " + getAvatarURL(72) +
                "}";
    }

    public long getTime() {
        if(server_time > 0)
            return server_time;
        else
            return (eid / 1000) + NetworkConnection.getInstance().clockOffset;
    }

    public synchronized boolean isImportant(String buffer_type) {
        if (self || deleted)
            return false;
        if (type == null) {
            return false;
        }

        Server s = ServersList.getInstance().getServer(cid);
        if (s != null) {
            Ignore ignore = s.ignores;
            String from = this.from_nick;
            if (from == null || from.length() == 0)
                from = this.nick;
            if (ignore.match(from + "!" + hostmask))
                return false;
        }

        if (type.equals("notice") || type.equalsIgnoreCase("channel_invite")) {
            // Notices sent from the server (with no nick sender) aren't important
            // e.g. *** Looking up your hostname...
            if (from == null || from.length() == 0)
                return false;

            // Notices and invites sent to a buffer shouldn't notify in the server buffer
            if (buffer_type.equalsIgnoreCase("console") && (to_chan || to_buffer))
                return false;
        }
        return isMessage();
    }

    public boolean isMessage() {
        return (type.equals("buffer_msg") ||
                type.equals("buffer_me_msg") ||
                type.equals("notice") ||
                type.equals("channel_invite") ||
                type.equals("callerid") ||
                type.equals("wallops"));
    }

    private String cachedAvatarURL;
    private int cachedAvatarSize;

    public String getGravatar(int size) {
        if(from_realname != null && from_realname.length() > 0) {
            Matcher m = Patterns.EMAIL_ADDRESS.matcher(from_realname);
            if(m.find()) {
                String email = from_realname.substring(m.start(), m.end()).toLowerCase();
                try {
                    cachedAvatarURL = "https://www.gravatar.com/avatar/" + ImageList.MD5(email).toLowerCase() + "?size=" + size + "&default=404";
                    if(ImageList.getInstance().isFailedURL(cachedAvatarURL))
                        cachedAvatarURL = null;
                    else
                        cachedAvatarSize = size;
                } catch (Exception e) {
                    cachedAvatarURL = null;
                }
            }
        }
        return cachedAvatarURL;
    }

    public String getAvatarURL(int size) {
        Server s = ServersList.getInstance().getServer(cid);
        Buffer b = BuffersList.getInstance().getBuffer(bid);
        return getAvatarURL(size, b != null && b.isChannel(), s != null && s.isSlack());
    }

    public String getAvatarURL(int size, boolean isChannel, boolean isSlack) {
        boolean isIRCCloudAvatar = false;
        if(!BuildConfig.ENTERPRISE && isMessage() && size != cachedAvatarSize) {
            cachedAvatarURL = null;
            if (avatar != null && avatar.length() > 0) {
                if (NetworkConnection.avatar_uri_template != null)
                    cachedAvatarURL = UriTemplate.fromTemplate(NetworkConnection.avatar_uri_template).set("id", avatar).set("modifiers", "s" + size).expand();
            } else if (avatar_url != null && avatar_url.length() > 0 && avatar_url.startsWith("https://")) {
                cachedAvatarURL = avatar_url;
                if (cachedAvatarURL.contains("{size}")) {
                    if(size <= 72)
                        cachedAvatarURL = UriTemplate.fromTemplate(cachedAvatarURL).set("size", "72").expand();
                    else if(size <= 192)
                        cachedAvatarURL = UriTemplate.fromTemplate(cachedAvatarURL).set("size", "192").expand();
                    else
                        cachedAvatarURL = UriTemplate.fromTemplate(cachedAvatarURL).set("size", "512").expand();
                }
            } else if (NetworkConnection.avatar_redirect_uri_template != null) {
                if (hostmask != null && hostmask.length() > 0 && hostmask.contains("@")) {
                    String ident = hostmask.substring(0, hostmask.indexOf("@"));
                    if(ident.startsWith("~"))
                        ident = ident.substring(1);
                    if (ident.startsWith("uid") || ident.startsWith("sid")) {
                        ident = ident.substring(3);
                        try {
                            if (Integer.valueOf(ident) > 0) {
                                cachedAvatarURL = UriTemplate.fromTemplate(NetworkConnection.avatar_redirect_uri_template).set("id", ident).set("modifiers", "s" + size).expand();
                                isIRCCloudAvatar = true;
                            }
                        } catch (NumberFormatException e) {
                            cachedAvatarURL = null;
                            isIRCCloudAvatar = false;
                        }
                    }
                }
            }

            if (cachedAvatarURL != null && avatar == null && !isIRCCloudAvatar && !isSlack) {
                JSONObject prefs = NetworkConnection.getInstance().getUserInfo().prefs;
                boolean pref_inlineImages = false;

                try {
                    if (prefs.has("inlineimages") && prefs.get("inlineimages") instanceof Boolean && prefs.getBoolean("inlineimages")) {
                        JSONObject inlineImagesMap = null;
                        if (isChannel) {
                            if (prefs.has("channel-inlineimages-disable"))
                                inlineImagesMap = prefs.getJSONObject("channel-inlineimages-disable");
                        } else {
                            if (prefs.has("buffer-inlineimages-disable"))
                                inlineImagesMap = prefs.getJSONObject("buffer-inlineimages-disable");
                        }

                        pref_inlineImages = !(inlineImagesMap != null && inlineImagesMap.has(String.valueOf(bid)) && inlineImagesMap.getBoolean(String.valueOf(bid)));
                    } else {
                        JSONObject inlineImagesMap = null;
                        if (isChannel) {
                            if (prefs.has("channel-inlineimages"))
                                inlineImagesMap = prefs.getJSONObject("channel-inlineimages");
                        } else {
                            if (prefs.has("buffer-inlineimages"))
                                inlineImagesMap = prefs.getJSONObject("buffer-inlineimages");
                        }

                        pref_inlineImages = (inlineImagesMap != null && inlineImagesMap.has(String.valueOf(bid)) && inlineImagesMap.getBoolean(String.valueOf(bid)));
                    }
                } catch (Exception e) {
                    NetworkConnection.printStackTraceToCrashlytics(e);
                }
                if (!pref_inlineImages)
                    cachedAvatarURL = null;
            }
        }
        if(cachedAvatarURL == null || ImageList.getInstance().isFailedURL(cachedAvatarURL))
            cachedAvatarURL = getGravatar(size);
        if(cachedAvatarURL != null)
            cachedAvatarSize = size;
        return cachedAvatarURL;
    }

    public String reply = null;
    public String reply() {
        if(reply != null)
            return reply;
        if(entities != null && entities.has("reply") && !entities.get("reply").isNull())
            return entities.get("reply").asText();
        if(entities != null && entities.has("known_client_tags") && !entities.get("known_client_tags").isNull() && entities.get("known_client_tags").has("reply"))
            return entities.get("known_client_tags").get("reply").asText();
        return null;
    }

    public boolean hasSameAccount(String account) {
        return this.account != null && !this.account.equals("*") && this.account.equals(account);
    }
}
