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

public class User {
    //@Unique(unique = false, uniqueGroups = 1)
    public int cid;

    //@Unique(unique = false, uniqueGroups = 1)
    public int bid;

    //@Unique(unique = false, uniqueGroups = 1)
    public String nick;

    public String old_nick = null;

    public String nick_lowercase;

    public String hostmask;

    public String mode;

    public int away;

    public String away_msg;

    public String ircserver;

    public String display_name;

    public int joined;

    public long last_mention = -1;
    public long last_message = -1;

    public String getDisplayName() {
        if(display_name != null && display_name.length() > 0)
            return display_name;
        else
            return nick;
    }

    public String toString() {
        return "{cid: " + cid + ", bid: " + bid + ", nick: " + nick + ", hostmask: " + hostmask + "}";
    }
}
