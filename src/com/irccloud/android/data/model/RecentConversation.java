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
import com.raizlabs.android.dbflow.annotation.ForeignKey;
import com.raizlabs.android.dbflow.annotation.ForeignKeyReference;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.annotation.Unique;
import com.raizlabs.android.dbflow.annotation.UniqueGroup;
import com.raizlabs.android.dbflow.structure.BaseModel;

@Table(database = IRCCloudDatabase.class,
        uniqueColumnGroups = {@UniqueGroup(groupNumber = 1, uniqueConflict = ConflictAction.REPLACE)})
public class RecentConversation extends BaseModel {
    @Column
    @PrimaryKey
    @Unique(unique = false, uniqueGroups = 1)
    public int cid;

    @Column
    @PrimaryKey
    @Unique(unique = false, uniqueGroups = 1)
    public int bid;

    @Column
    public String name;

    @Column
    public String type;

    @Column
    public long timestamp;

    @Column
    public String avatar_url;

    public String toString() {
        return "{cid: " + cid + ", bid: " + bid + ", name: " + name + ", type: " + type + "}";
    }
}