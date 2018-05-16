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
import com.irccloud.android.R;
import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.collection.ImageList;
import com.irccloud.android.data.collection.ServersList;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ConflictAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.annotation.Unique;
import com.raizlabs.android.dbflow.annotation.UniqueGroup;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.TimerTask;
import java.util.regex.Matcher;

/*@Table(databaseName = IRCCloudDatabase.NAME,
        uniqueColumnGroups = {@UniqueGroup(groupNumber = 1, uniqueConflict = ConflictAction.REPLACE)})*/
public class Event /*extends ObservableBaseModel*/ {
    @Column
    @PrimaryKey
    @Unique(unique = false, uniqueGroups = 1)
    public int cid;

    @Column
    @PrimaryKey
    @Unique(unique = false, uniqueGroups = 1)
    public int bid;

    @Column
    @PrimaryKey
    @Unique(unique = false, uniqueGroups = 1)
    public long eid;

    @Column
    public long server_time;

    @Column
    public String type;

    @Column
    public String msg;

    @Column
    public String hostmask;

    @Column(name = "event_from")
    public String from;

    @Column
    public String from_nick;

    @Column
    public String from_mode;

    @Column
    public String from_realname;

    @Column
    public String from_hostmask;

    @Column
    public String nick;

    @Column
    public String old_nick;

    @Column
    public String server;

    @Column
    public String diff;

    @Column
    public String chan;

    @Column
    public boolean highlight;

    @Column
    public boolean self;

    @Column
    public boolean to_chan;

    @Column
    public boolean to_buffer;

    @Column
    public int color;

    @Column
    public int bg_color;

    @Column
    public JsonNode ops;

    @Column
    public long group_eid;

    @Column
    public int row_type = 0;

    @Column
    public String group_msg;

    @Column
    public boolean linkify = true;

    @Column
    public String target_mode;

    @Column
    public int reqid;

    @Column
    public boolean pending;

    @Column
    public boolean failed;

    @Column
    public String command;

    @Column
    public int day = -1;

    @Column
    public String contentDescription;

    @Column
    public JsonNode entities;

    @Column
    public String avatar;

    @Column
    public String avatar_url;

    @Column
    public String msgid;

    public String timestamp;
    public String html;
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
    }

    public String toString() {
        return "{" +
                "cid: " + cid +
                " bid: " + bid +
                " eid: " + eid +
                " type: " + type +
                " timestamp: " + timestamp +
                " from: " + from +
                " msg: " + msg +
                " html: " + html +
                " group_eid: " + group_eid +
                " group_msg: " + group_msg +
                " pending: " + pending +
                " self: " + self +
                " header: " + header +
                " avatar: " + avatar +
                " avatar_url: " + avatar_url +
                " getAvatarURL: " + getAvatarURL(72) +
                "}";
    }

    public long getTime() {
        if(server_time > 0)
            return server_time;
        else
            return (eid / 1000) + NetworkConnection.getInstance().clockOffset;
    }

    public synchronized boolean isImportant(String buffer_type) {
        if (self)
            return false;
        if (type == null) {
            return false;
        }

        Server s = ServersList.getInstance().getServer(cid);
        if (s != null) {
            Ignore ignore = s.ignores;
            String from = this.from;
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
        boolean isIRCCloudAvatar = false;
        if(!BuildConfig.ENTERPRISE && isMessage() && size != cachedAvatarSize) {
            cachedAvatarURL = null;
            if (avatar != null && avatar.length() > 0) {
                if (NetworkConnection.avatar_uri_template != null)
                    cachedAvatarURL = UriTemplate.fromTemplate(NetworkConnection.avatar_uri_template).set("id", avatar).set("modifiers", "s" + size).expand();
            } else if (avatar_url != null && avatar_url.length() > 0 && avatar_url.startsWith("https://")) {
                cachedAvatarURL = avatar_url;
                if (cachedAvatarURL.contains("{size}")) {
                    cachedAvatarURL = UriTemplate.fromTemplate(cachedAvatarURL).set("size", size == 512 ? "512" : "72").expand();
                }
            } else if (NetworkConnection.avatar_redirect_uri_template != null) {
                if (hostmask != null && hostmask.length() > 0 && hostmask.contains("@")) {
                    String ident = hostmask.substring(0, hostmask.indexOf("@"));
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
            if (cachedAvatarURL != null && avatar == null && !isIRCCloudAvatar && !ServersList.getInstance().getServer(cid).isSlack()) {
                JSONObject prefs = NetworkConnection.getInstance().getUserInfo().prefs;
                Buffer buffer = BuffersList.getInstance().getBuffer(bid);
                boolean pref_inlineImages = false;

                try {
                    if (prefs.has("inlineimages") && prefs.get("inlineimages") instanceof Boolean && prefs.getBoolean("inlineimages")) {
                        JSONObject inlineImagesMap = null;
                        if (buffer.isChannel()) {
                            if (prefs.has("channel-inlineimages-disable"))
                                inlineImagesMap = prefs.getJSONObject("channel-inlineimages-disable");
                        } else {
                            if (prefs.has("buffer-inlineimages-disable"))
                                inlineImagesMap = prefs.getJSONObject("buffer-inlineimages-disable");
                        }

                        pref_inlineImages = !(inlineImagesMap != null && inlineImagesMap.has(String.valueOf(buffer.getBid())) && inlineImagesMap.getBoolean(String.valueOf(buffer.getBid())));
                    } else {
                        JSONObject inlineImagesMap = null;
                        if (buffer.isChannel()) {
                            if (prefs.has("channel-inlineimages"))
                                inlineImagesMap = prefs.getJSONObject("channel-inlineimages");
                        } else {
                            if (prefs.has("buffer-inlineimages"))
                                inlineImagesMap = prefs.getJSONObject("buffer-inlineimages");
                        }

                        pref_inlineImages = (inlineImagesMap != null && inlineImagesMap.has(String.valueOf(buffer.getBid())) && inlineImagesMap.getBoolean(String.valueOf(buffer.getBid())));
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
}
