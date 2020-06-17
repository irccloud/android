/*
 * Copyright (c) 2020 IRCCloud, Ltd.
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

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.chooser.ChooserTarget;
import android.util.TypedValue;

import com.irccloud.android.ColorScheme;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.activity.MainActivity;
import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.model.Avatar;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.model.RecentConversation;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import androidx.core.app.Person;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
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
    private HashMap<Integer, Person[]> people = new HashMap<>();
    public void setPeople(int bid, Person[] p) {
        people.put(bid, p);
    }

    public static RecentConversationsList getInstance() {
        if (instance == null)
            instance = new RecentConversationsList();
        return instance;
    }

    public void clear() {
        IRCCloudDatabase.getInstance().RecentConversationsDao().clear();
        ShortcutManagerCompat.removeAllDynamicShortcuts(IRCCloudApplication.getInstance().getApplicationContext());
    }

    public List<RecentConversation> getConversations() {
        return IRCCloudDatabase.getInstance().RecentConversationsDao().getConversations();
    }

    public RecentConversation getConversation(int cid, int bid) {
        return IRCCloudDatabase.getInstance().RecentConversationsDao().getConversation(cid, bid);
    }

    public void deleteConversation(int cid, int bid) {
        RecentConversation c = getConversation(cid,bid);
        if(c != null)
            IRCCloudDatabase.getInstance().RecentConversationsDao().delete(c);
    }

    public void updateConversation(int cid, int bid, String name, String type, long timestamp) {
        RecentConversation c = getConversation(cid, bid);
        if(c != null) {
            if(c.getTimestamp() < timestamp)
                c.setTimestamp(timestamp);
            else
                return;
        } else if(name != null && type != null) {
            c = new RecentConversation();
            c.setCid(cid);
            c.setBid(bid);
            c.setName(name);
            c.setType(type);
            c.setTimestamp(timestamp);
        }
        if(c != null)
            IRCCloudDatabase.getInstance().RecentConversationsDao().insert(c);
    }

    public void prune() {
        List<RecentConversation> conversations = getConversations();
        IRCCloudDatabase.getInstance().beginTransaction();
        for(RecentConversation c : conversations) {
            if(BuffersList.getInstance().getBuffer(c.getBid()) == null || BuffersList.getInstance().getBuffer(c.getBid()).getArchived() == 1) {
                IRCCloudDatabase.getInstance().RecentConversationsDao().delete(c);
            }
        }
        IRCCloudDatabase.getInstance().endTransaction();

        conversations = getConversations();
        IRCCloudDatabase.getInstance().beginTransaction();
        int MAX_SHORTCUTS = ShortcutManagerCompat.getMaxShortcutCountPerActivity(IRCCloudApplication.getInstance().getApplicationContext());
        while(conversations.size() > MAX_SHORTCUTS) {
            RecentConversation last = conversations.get(conversations.size() - 1);
            conversations.remove(last);
            IRCCloudDatabase.getInstance().RecentConversationsDao().delete(last);
        }
        IRCCloudDatabase.getInstance().endTransaction();
    }

    public void publishShortcuts() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int MAX_SHORTCUTS = ShortcutManagerCompat.getMaxShortcutCountPerActivity(IRCCloudApplication.getInstance().getApplicationContext());

            HashSet<String> categories = new HashSet<>();
            categories.add("com.irccloud.android.SHARE_TARGET");

            ArrayList<ShortcutInfoCompat> shortcuts = new ArrayList<>();
            List<RecentConversation> conversations = getConversations();
            for(RecentConversation c : conversations) {
                IconCompat avatar = AvatarsList.getIconForBuffer(c.getBuffer(), new ImageList.OnImageFetchedListener() {
                    @Override
                    public void onImageFetched(Bitmap image) {
                        RecentConversationsList.getInstance().publishShortcuts();
                    }
                });

                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setComponent(new ComponentName(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), "com.irccloud.android.MainActivity"));
                i.putExtra("bid", c.getBid());

                ShortcutInfoCompat.Builder builder = new ShortcutInfoCompat.Builder(IRCCloudApplication.getInstance().getApplicationContext(), String.valueOf(c.getBid()))
                        .setShortLabel(c.getName())
                        .setIcon(avatar)
                        .setIntent(i)
                        .setLongLived(true)
                        .setCategories(categories);

                if(people.containsKey(c.getBid()))
                    builder.setPersons(people.get(c.getBid()));
                else if(c.getBuffer().isConversation())
                    builder.setPerson(new Person.Builder().setName(c.getBuffer().getDisplayName()).build());

                shortcuts.add(builder.build());

                if(shortcuts.size() >= MAX_SHORTCUTS)
                    break;
            }

            ShortcutManagerCompat.removeAllDynamicShortcuts(IRCCloudApplication.getInstance().getApplicationContext());
            ShortcutManagerCompat.addDynamicShortcuts(IRCCloudApplication.getInstance().getApplicationContext(), shortcuts);
        }
    }
}
