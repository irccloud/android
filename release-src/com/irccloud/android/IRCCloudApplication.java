/*
 * Copyright (c) 2013 IRCCloud, Ltd.
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

import android.content.Context;
import com.squareup.leakcanary.RefWatcher;

@SuppressWarnings("unused")
public class IRCCloudApplication extends IRCCloudApplicationBase {
    private static IRCCloudApplication instance = null;

    public static IRCCloudApplication getInstance() {
        if (instance != null) {
            return instance;
        } else {
            return new IRCCloudApplication();
        }
    }

    public static RefWatcher getRefWatcher(Context context) {
        return RefWatcher.DISABLED;
    }

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
    }
}