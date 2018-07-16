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

import androidx.room.Entity;
import androidx.room.Index;

@Entity(primaryKeys = {"cid", "bid", "eid"}, indices = {@Index(value = {"cid", "bid", "eid"}, unique = true)})
public class Notification {
    private int cid;

    private int bid;

    private long eid;

    private String nick;

    private String message;

    private String network;

    private String chan;

    private String buffer_type;

    private String message_type;

    private String avatar_url;

    private boolean shown = false;

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }

    public long getEid() {
        return eid;
    }

    public void setEid(long eid) {
        this.eid = eid;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getChan() {
        return chan;
    }

    public void setChan(String chan) {
        this.chan = chan;
    }

    public String getBuffer_type() {
        return buffer_type;
    }

    public void setBuffer_type(String buffer_type) {
        this.buffer_type = buffer_type;
    }

    public String getMessage_type() {
        return message_type;
    }

    public void setMessage_type(String message_type) {
        this.message_type = message_type;
    }

    public String getAvatar_url() {
        return avatar_url;
    }

    public void setAvatar_url(String avatar_url) {
        this.avatar_url = avatar_url;
    }

    public boolean isShown() {
        return shown;
    }

    public void setShown(boolean shown) {
        this.shown = shown;
    }

    public String toString() {
        return "{cid: " + getCid() + ", bid: " + getBid() + ", eid: " + getEid() + ", nick: " + getNick() + ", message: " + getMessage() + ", network: " + getNetwork() + ", chan: " + getChan() + ", message_type: " + getMessage_type() + " shown: " + isShown() + "}";
    }
}