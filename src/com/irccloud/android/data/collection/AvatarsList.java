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

import com.irccloud.android.data.model.Avatar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class AvatarsList {
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
    }

    public synchronized Avatar getAvatar(int cid, String nick) {
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
            avatars.get(cid).put(nick, a);
        }

        return a;
    }

    public synchronized void pruneAvatars() {
        android.util.Log.i("IRCCloud", "Pruning avatar cache");
        for (HashMap<String, Avatar> cid : avatars.values()) {
            ArrayList<Avatar> avs = new ArrayList<>(cid.values());
            Collections.sort(avs, new Comparator<Avatar>() {
                @Override
                public int compare(Avatar a1, Avatar a2) {
                    return (int)(a2.lastAccessTime - a1.lastAccessTime);
                }
            });
            while(avs.size() > MAX_AVATARS) {
                Avatar a = avs.get(0);
                avs.remove(a);
                cid.remove(a);
            }
        }
    }
}
