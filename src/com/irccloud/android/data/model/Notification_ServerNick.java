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
import androidx.room.PrimaryKey;

@Entity
public class Notification_ServerNick {
    @PrimaryKey
    private int cid;

    private String nick;

    private String avatar_url;

    private boolean isSlack;

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getAvatar_url() {
        return avatar_url;
    }

    public void setAvatar_url(String url) {
        this.avatar_url = url;
    }

    public boolean getIsSlack() {
        return isSlack;
    }

    public void setIsSlack(boolean isSlack) {
        this.isSlack = isSlack;
    }

    @Override
    public String toString() {
        return nick;
    }
}
