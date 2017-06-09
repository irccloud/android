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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

/*import com.squareup.leakcanary.AndroidExcludedRefs;
import com.squareup.leakcanary.ExcludedRefs;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;*/

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

    /*public static RefWatcher getRefWatcher(Context context) {
        return getInstance().refWatcher;
    }

    private RefWatcher refWatcher;*/

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        /*if(BuildConfig.DEBUG && prefs.getBoolean("detect_leaks", true) && Build.VERSION.SDK_INT < 23) {
            ExcludedRefs excludedRefs = AndroidExcludedRefs.createAndroidDefaults()
                    .thread("WebViewCoreThread")
                    .thread("CookieSyncManager")
                    .instanceField("android.webkit.WebViewCore", "mContext")
                    .instanceField("android.sec.clipboard.ClipboardUIManager", "mContext")
                    .instanceField("android.widget.Editor$Blink", "this$0")
                    .instanceField("android.view.textservice.SpellCheckerSession$1", "this$0")
                    .instanceField("android.view.Choreographer$FrameDisplayEventReceiver", "mMessageQueue")
                    .instanceField("android.speech.tts.TextToSpeech", "mContext")
                    .instanceField("android.widget.TextView$ChangeWatcher", "this$0")
                    .instanceField("com.samsung.android.smartclip.SpenGestureManager", "mContext")
                    .instanceField("org.chromium.content.browser.input.PopupTouchHandleDrawable", "mContext")
                    .instanceField("android.media.AudioManager", "mContext")
                    .instanceField("com.android.org.chromium.android_webview.AwResource", "sResources")
                    .instanceField("android.media.AudioManager$1", "this$0")
                    .build();
            refWatcher = LeakCanary.install(this, CrashlyticsLeakService.class, excludedRefs);
        } else {
            refWatcher = RefWatcher.DISABLED;
        }*/
    }
}
