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
import com.irccloud.android.data.collection.ServersList;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ConflictAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.annotation.Unique;
import com.raizlabs.android.dbflow.annotation.UniqueGroup;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(databaseName = IRCCloudDatabase.NAME,
        uniqueColumnGroups = {@UniqueGroup(groupNumber = 1, uniqueConflict = ConflictAction.REPLACE)})
public class Buffer extends BaseModel {
    @Column
    @PrimaryKey
    @Unique(unique = false, uniqueGroups = 1)
    public int bid;

    @Column
    @PrimaryKey
    @Unique(unique = false, uniqueGroups = 1)
    public int cid;

    @Column
    public long min_eid;

    @Column
    public long last_seen_eid;

    @Column
    public String name;

    @Column
    public String type;

    @Column
    public int archived;

    @Column
    public int deferred;

    @Column
    public int timeout;

    @Column
    public String away_msg;

    @Column
    public String draft;

    @Column
    public String chan_types;

    @Column
    public boolean scrolledUp;

    @Column
    public int scrollPosition;

    @Column
    public int scrollPositionOffset;

    @Column
    public int unread;

    @Column
    public int highlights;

    public int valid = 1;

    public String toString() {
        return "{cid:" + cid + ", bid:" + bid + ", name: " + name + ", type: " + type + ", archived: " + archived + "}";
    }

    public String normalizedName() {
        if (chan_types == null || chan_types.length() < 2) {
            Server s = ServersList.getInstance().getServer(cid);
            if (s != null && s.CHANTYPES != null && s.CHANTYPES.length() > 0)
                chan_types = s.CHANTYPES;
            else
                chan_types = "#";
        }
        return name.replaceAll("^[" + chan_types + "]+", "");
    }
}
