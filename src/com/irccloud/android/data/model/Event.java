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

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.Ignore;
import com.irccloud.android.R;
import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.collection.ServersList;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ConflictAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.annotation.Unique;
import com.raizlabs.android.dbflow.annotation.UniqueGroup;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.TimerTask;

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
    public String type;

    @Column
    public String msg;

    @Column
    public String hostmask;

    @Column(name = "event_from")
    public String from;

    @Column
    public String from_mode;

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
    public int color = R.color.row_message_label;

    @Column
    public int bg_color = R.color.message_bg;

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

    public String timestamp;
    public String html;
    public Spanned formatted;
    public TimerTask expiration_timer;

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
                "}";
    }

    public synchronized boolean isImportant(String buffer_type) {
        if (self)
            return false;
        if (type == null) {
            return false;
        }

        Ignore ignore = new Ignore();
        Server s = ServersList.getInstance().getServer(cid);
        if (s != null) {
            ignore.setIgnores(s.ignores);
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
        return (type.equals("buffer_msg") ||
                type.equals("buffer_me_msg") ||
                type.equals("notice") ||
                type.equals("channel_invite") ||
                type.equals("callerid") ||
                type.equals("wallops"));
    }
}
