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

import android.databinding.BaseObservable;

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.data.IRCCloudDatabase;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ConflictAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.annotation.Unique;
import com.raizlabs.android.dbflow.annotation.UniqueGroup;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.ArrayList;

/*@Table(databaseName = IRCCloudDatabase.NAME,
        uniqueColumnGroups = {@UniqueGroup(groupNumber = 1, uniqueConflict = ConflictAction.REPLACE)})*/
public class Channel extends BaseObservable /*extends ObservableBaseModel*/ {
    public static class Mode {
        public String mode;
        public String param;
    }

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
    public String topic_text;

    @Column
    public long topic_time;

    @Column
    public String topic_author;

    @Column
    public String type;

    @Column
    public String mode;

    @Column
    public long timestamp;

    @Column
    public String url;

    @Column
    public boolean key;

    public ArrayList<Mode> modes = new ArrayList<>();
    public int valid = 1;

    public synchronized void addMode(String mode, String param, boolean init) {
        if (!init)
            removeMode(mode);
        if (mode.equals("k"))
            key = true;
        Mode m = new Mode();
        m.mode = mode;
        m.param = param;
        modes.add(m);
    }

    public synchronized void removeMode(String mode) {
        if (mode.equals("k"))
            key = false;
        for (Mode m : modes) {
            if (m.mode.equals(mode)) {
                modes.remove(m);
                return;
            }
        }
    }

    public synchronized boolean hasMode(String mode) {
        for (Mode m : modes) {
            if (m.mode.equals(mode)) {
                return true;
            }
        }
        return false;
    }

    public synchronized String paramForMode(String mode) {
        for (Mode m : modes) {
            if (m.mode.equals(mode)) {
                return m.param;
            }
        }
        return null;
    }
}
