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

import android.graphics.Bitmap;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.TypedValue;

import androidx.core.graphics.drawable.IconCompat;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.irccloud.android.ColorScheme;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.model.Avatar;
import com.irccloud.android.data.model.Buffer;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class AvatarsList {
    @Dao
    public interface AvatarsDao {
        @Query("SELECT * FROM Avatar WHERE cid = :cid AND nick = :nick LIMIT 1")
        Avatar getAvatar(int cid, String nick);

        @Query("SELECT avatar_url FROM Avatar WHERE cid = :cid AND nick = :nick LIMIT 1")
        String getAvatarURL(int cid, String nick);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(Avatar avatar);

        @Update
        void update(Avatar avatar);

        @Delete
        void delete(Avatar avatar);

        @Query("DELETE FROM Avatar")
        void clear();
    }

    private static final int MAX_AVATARS = 50;
    private HashMap<Integer, HashMap<String, Avatar>> avatars;

    private static AvatarsList instance = null;

    public synchronized static AvatarsList getInstance() {
        if (instance == null)
            instance = new AvatarsList();
        return instance;
    }

    public AvatarsList() {
        avatars = new HashMap<>();
    }

    public synchronized void clear() {
        avatars.clear();
        IRCCloudDatabase.getInstance().AvatarsDao().clear();
    }

    public static void setAvatarURL(int cid, String nick, long eid, String avatar_url) {
        if(nick != null) {
            Avatar a = IRCCloudDatabase.getInstance().AvatarsDao().getAvatar(cid, nick);

            if(avatar_url != null) {
                if (a == null) {
                    a = new Avatar();
                    a.cid = cid;
                    a.nick = nick;
                    a.eid = eid;
                    a.avatar_url = avatar_url;
                    IRCCloudDatabase.getInstance().AvatarsDao().insert(a);
                } else if (a.eid < eid && !a.avatar_url.equals(avatar_url)) {
                    a.avatar_url = avatar_url;
                    a.eid = eid;
                    IRCCloudDatabase.getInstance().AvatarsDao().update(a);
                }
            } else {
                if(a != null)
                    IRCCloudDatabase.getInstance().AvatarsDao().delete(a);
            }
        }
    }

    public static String getAvatarURL(int cid, String nick) {
        return IRCCloudDatabase.getInstance().AvatarsDao().getAvatarURL(cid, nick);
    }

    public synchronized Avatar getAvatar(int cid, String nick, String display_name) {
        Avatar a = null;

        if(avatars.containsKey(cid) && avatars.get(cid) != null) {
            if(avatars.get(cid).containsKey(nick))
                a = avatars.get(cid).get(nick);
        } else {
            avatars.put(cid, new HashMap<String, Avatar>());
        }

        if(avatars.get(cid).size() > MAX_AVATARS)
            pruneAvatars();

        if(a == null) {
            a = new Avatar();
            a.cid = cid;
            a.nick = nick;
            a.display_name = (display_name != null && display_name.length() > 0) ? display_name : nick;
            avatars.get(cid).put(nick, a);
        }

        return a;
    }

    public synchronized void pruneAvatars() {
        android.util.Log.i("IRCCloud", "Pruning avatar cache");
        for (HashMap<String, Avatar> cid : avatars.values()) {
            ArrayList<Avatar> avs = new ArrayList<>(cid.values());
            try {
                Collections.sort(avs, new Comparator<Avatar>() {
                    @Override
                    public int compare(Avatar a1, Avatar a2) {
                        return Long.compare(a2.lastAccessTime, a1.lastAccessTime);
                    }
                });
            } catch (Exception e) {
                avatars.clear();
                return;
            }
            while(avs.size() > MAX_AVATARS) {
                Avatar a = avs.get(0);
                avs.remove(a);
                cid.remove(a);
            }
        }
    }

    public static int SHORTCUT_ICON_SIZE() {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 108, IRCCloudApplication.getInstance().getApplicationContext().getResources().getDisplayMetrics());
    }

    public static IconCompat getIconForBuffer(Buffer b, ImageList.OnImageFetchedListener onImageFetchedListener) {
        IconCompat avatar = null;

        if(b.getType().equals("channel")) {
            String name = b.normalizedName();
            if(name.length() > 0) {
                name = "#" + name.substring(0,1);
            } else if(b.getName().length() > 1){
                name = b.getName().toLowerCase().substring(0,2);
            } else {
                name = "#";
            }
            avatar = IconCompat.createWithAdaptiveBitmap(Avatar.generateBitmap(name.toUpperCase(), 0xFFFFFFFF, Color.parseColor("#" + ColorScheme.colorForNick(b.getName(), false)), false, AvatarsList.SHORTCUT_ICON_SIZE(), false));
        } else if(b.isConversation()) {
            String avatar_url = getAvatarURL(b.getCid(), b.getName());
            try {
                if (avatar_url != null && avatar_url.length() > 0 && PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("avatar-images", false)) {
                    Bitmap bitmap = ImageList.getInstance().getImage(new URL(avatar_url));
                    if (bitmap != null)
                        avatar = IconCompat.createWithAdaptiveBitmap(bitmap);
                    else if(onImageFetchedListener != null)
                        ImageList.getInstance().fetchImage(new URL(avatar_url), onImageFetchedListener);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if(avatar == null) {
                avatar = IconCompat.createWithAdaptiveBitmap(AvatarsList.getInstance().getAvatar(b.getCid(), b.getName(), null).getBitmap(false, AvatarsList.SHORTCUT_ICON_SIZE(), false, false));
            }
        }
        return avatar;
    }

}
