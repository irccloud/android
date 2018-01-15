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

package com.irccloud.android.data.collection;

import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.model.RecentConversation;
import com.irccloud.android.data.model.RecentConversation_Table;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Select;

import java.util.List;

public class RecentConversationsList {
    private static RecentConversationsList instance = null;
    private final Object dbLock = new Object();

    public static RecentConversationsList getInstance() {
        if (instance == null)
            instance = new RecentConversationsList();
        return instance;
    }

    public void clear() {
        synchronized (dbLock) {
            Delete.table(RecentConversation.class);
        }
    }

    public List<RecentConversation> getConversations() {
        synchronized (dbLock) {
            return new Select().from(RecentConversation.class).where().orderBy(RecentConversation_Table.timestamp, false).queryList();
        }
    }

    public RecentConversation getConversation(int cid, int bid) {
        synchronized (dbLock) {
            return new Select().from(RecentConversation.class).where(RecentConversation_Table.cid.is(cid)).and(RecentConversation_Table.bid.is(bid)).querySingle();
        }
    }

    public void updateConversation(int cid, int bid, long timestamp) {
        RecentConversation c = getConversation(cid, bid);
        if(c != null) {
            if(c.timestamp < timestamp)
                c.timestamp = timestamp;
            else
                return;
        } else {
            Buffer b = BuffersList.getInstance().getBuffer(bid);
            if(b == null)
                return;
            c = new RecentConversation();
            c.cid = cid;
            c.bid = bid;
            c.name = b.getName();
            c.type = b.getType();
            c.timestamp = timestamp;
        }
        synchronized (dbLock) {
            c.save();
        }
    }

    public void updateAvatar(int cid, int bid, String avatar_url) {
        RecentConversation c = getConversation(cid, bid);
        if(c != null) {
            c.avatar_url = avatar_url;
            synchronized (dbLock) {
                c.save();
            }
        }
    }

    public void prune() {
        List<RecentConversation> conversations = getConversations();
        for(RecentConversation c : conversations) {
            if(BuffersList.getInstance().getBuffer(c.bid) == null) {
                synchronized (dbLock) {
                    c.delete();
                }
            }
        }

        conversations = getConversations();
        while(conversations.size() > 5) {
            RecentConversation last = conversations.get(conversations.size() - 1);
            conversations.remove(last);
            synchronized (dbLock) {
                last.delete();
            }
        }
    }
}
