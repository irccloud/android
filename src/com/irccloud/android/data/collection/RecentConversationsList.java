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

import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.model.RecentConversation;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

public class RecentConversationsList {
    @Dao
    public interface RecentConversationsDao {
        @Query("SELECT * FROM RecentConversation ORDER BY timestamp DESC")
        List<RecentConversation> getConversations();

        @Query("SELECT * FROM RecentConversation WHERE cid = :cid AND bid = :bid LIMIT 1")
        RecentConversation getConversation(int cid, int bid);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(RecentConversation recentConversation);

        @Update
        void update(RecentConversation recentConversation);

        @Delete
        void delete(RecentConversation recentConversation);

        @Query("DELETE FROM RecentConversation")
        void clear();
    }

    private static RecentConversationsList instance = null;

    public static RecentConversationsList getInstance() {
        if (instance == null)
            instance = new RecentConversationsList();
        return instance;
    }

    public void clear() {
        IRCCloudDatabase.getInstance().RecentConversationsDao().clear();
    }

    public List<RecentConversation> getConversations() {
        return IRCCloudDatabase.getInstance().RecentConversationsDao().getConversations();
    }

    public RecentConversation getConversation(int cid, int bid) {
        return IRCCloudDatabase.getInstance().RecentConversationsDao().getConversation(cid, bid);
    }

    public void updateConversation(int cid, int bid, long timestamp) {
        RecentConversation c = getConversation(cid, bid);
        if(c != null) {
            if(c.getTimestamp() < timestamp)
                c.setTimestamp(timestamp);
            else
                return;
        } else {
            Buffer b = BuffersList.getInstance().getBuffer(bid);
            if(b == null)
                return;
            c = new RecentConversation();
            c.setCid(cid);
            c.setBid(bid);
            c.setName(b.getName());
            c.setType(b.getType());
            c.setTimestamp(timestamp);
        }
        IRCCloudDatabase.getInstance().RecentConversationsDao().insert(c);
    }

    public void updateAvatar(int cid, int bid, String avatar_url) {
        RecentConversation c = getConversation(cid, bid);
        if(c != null) {
            c.setAvatar_url(avatar_url);
            IRCCloudDatabase.getInstance().RecentConversationsDao().update(c);
        }
    }

    public void prune() {
        List<RecentConversation> conversations = getConversations();
        for(RecentConversation c : conversations) {
            if(BuffersList.getInstance().getBuffer(c.getBid()) == null) {
                IRCCloudDatabase.getInstance().RecentConversationsDao().delete(c);
            }
        }

        conversations = getConversations();
        while(conversations.size() > 5) {
            RecentConversation last = conversations.get(conversations.size() - 1);
            conversations.remove(last);
            IRCCloudDatabase.getInstance().RecentConversationsDao().delete(last);
        }
    }
}
