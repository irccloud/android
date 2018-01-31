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

import com.irccloud.android.data.IRCCloudDatabase;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ConflictAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.annotation.Unique;
import com.raizlabs.android.dbflow.annotation.UniqueGroup;
import com.raizlabs.android.dbflow.structure.BaseModel;

/*@Table(databaseName = IRCCloudDatabase.NAME,
        uniqueColumnGroups = {@UniqueGroup(groupNumber = 1, uniqueConflict = ConflictAction.REPLACE)})*/
public class User /*extends BaseModel*/ {
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
    public String nick;

    @Column
    public String old_nick = null;

    @Column
    public String nick_lowercase;

    @Column
    public String hostmask;

    @Column
    public String mode;

    @Column
    public int away;

    @Column
    public String away_msg;

    @Column
    public String ircserver;

    @Column
    public String display_name;

    @Column
    public int joined;

    @Column
    public long last_mention = -1;

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
