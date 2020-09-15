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

package com.irccloud.android.data;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.irccloud.android.BackgroundTaskWorker;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.data.collection.AvatarsList;
import com.irccloud.android.data.collection.LogExportsList;
import com.irccloud.android.data.collection.NotificationsList;
import com.irccloud.android.data.collection.RecentConversationsList;
import com.irccloud.android.data.model.Avatar;
import com.irccloud.android.data.model.BackgroundTask;
import com.irccloud.android.data.model.LogExport;
import com.irccloud.android.data.model.Notification;
import com.irccloud.android.data.model.Notification_LastSeenEID;
import com.irccloud.android.data.model.Notification_ServerNick;
import com.irccloud.android.data.model.RecentConversation;

@Database(entities = {RecentConversation.class, BackgroundTask.class, LogExport.class, Notification.class, Notification_LastSeenEID.class, Notification_ServerNick.class, Avatar.class}, version = IRCCloudDatabase.VERSION, exportSchema = false)
public abstract class IRCCloudDatabase extends RoomDatabase {
    public static final String NAME = "irccloud";
    public static final int VERSION = 13;

    public abstract RecentConversationsList.RecentConversationsDao RecentConversationsDao();
    public abstract BackgroundTaskWorker.BackgroundTasksDao BackgroundTasksDao();
    public abstract LogExportsList.LogExportsDao LogExportsDao();
    public abstract NotificationsList.NotificationsDao NotificationsDao();
    public abstract AvatarsList.AvatarsDao AvatarsDao();

    private static IRCCloudDatabase sInstance;
    public static IRCCloudDatabase getInstance() {
        if (sInstance == null) {
            synchronized (IRCCloudDatabase.class) {
                if (sInstance == null) {
                    sInstance = Room.databaseBuilder(IRCCloudApplication.getInstance().getApplicationContext(), IRCCloudDatabase.class, NAME)
                            .fallbackToDestructiveMigrationFrom(12)
                            .fallbackToDestructiveMigration()
                            .fallbackToDestructiveMigrationOnDowngrade()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return sInstance;
    }
}
