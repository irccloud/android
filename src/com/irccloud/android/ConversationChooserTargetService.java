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

package com.irccloud.android;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.chooser.ChooserTarget;
import android.service.chooser.ChooserTargetService;
import android.util.TypedValue;

import com.irccloud.android.activity.MainActivity;
import com.irccloud.android.data.collection.AvatarsList;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.collection.ImageList;
import com.irccloud.android.data.collection.RecentConversationsList;
import com.irccloud.android.data.model.Avatar;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.model.RecentConversation;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@TargetApi(Build.VERSION_CODES.M)
public class ConversationChooserTargetService extends ChooserTargetService {
    private static final Icon channelIcon = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ?
            Icon.createWithBitmap(Avatar.generateBitmap("#", 0xFFFFFFFF, 0xFFAAAAAA, false, (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, IRCCloudApplication.getInstance().getApplicationContext().getResources().getDisplayMetrics()), false)) :
            Icon.createWithAdaptiveBitmap(Avatar.generateBitmap("#", 0xFFFFFFFF, 0xFFAAAAAA, false, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 108, IRCCloudApplication.getInstance().getApplicationContext().getResources().getDisplayMetrics()), false));

    @Override
    public List<ChooserTarget> onGetChooserTargets(ComponentName componentName, IntentFilter intentFilter) {
        ComponentName cn = new ComponentName(getPackageName(), MainActivity.class.getCanonicalName());
        ArrayList<ChooserTarget> targets = new ArrayList<>();
        List<RecentConversation> conversations = RecentConversationsList.getInstance().getConversations();

        for(RecentConversation c : conversations) {
            Buffer b = BuffersList.getInstance().getBuffer(c.bid);
            if(b == null) {
                BuffersList.getInstance().createBuffer(c.bid, c.cid, 0, 0, c.name, c.type, 0, 1, 0, 0);
                b = BuffersList.getInstance().getBuffer(c.bid);
            }
            Icon avatar = null;
            if(b.isConversation()) {
                try {
                    if (c.avatar_url != null && c.avatar_url.length() > 0 && PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("avatar-images", true)) {
                        Bitmap bitmap = ImageList.getInstance().getImage(new URL(c.avatar_url));
                        if (bitmap != null)
                            avatar = Icon.createWithBitmap(bitmap);
                    }
                } catch (Exception e) {
                }

                if(avatar == null) {
                    avatar = (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ?
                            Icon.createWithBitmap(AvatarsList.getInstance().getAvatar(c.cid, c.name).getBitmap(false, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, IRCCloudApplication.getInstance().getApplicationContext().getResources().getDisplayMetrics()))) :
                            Icon.createWithAdaptiveBitmap(AvatarsList.getInstance().getAvatar(c.cid, c.name).getBitmap(false, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 108, IRCCloudApplication.getInstance().getApplicationContext().getResources().getDisplayMetrics()), false, false))
                    );
                }
            }
            Bundle extras = new Bundle();
            extras.putInt("bid", c.bid);
            targets.add(new ChooserTarget(c.name, c.type.equals("channel")?channelIcon:avatar, 0.5f, cn, extras));
        }

        return targets;
    }
}
