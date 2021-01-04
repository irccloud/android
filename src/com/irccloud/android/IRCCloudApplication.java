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

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.provider.FontRequest;
import androidx.core.provider.FontsContractCompat;
import androidx.emoji.text.EmojiCompat;
import androidx.emoji.text.FontRequestEmojiCompatConfig;

import com.datatheorem.android.trustkit.TrustKit;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.collection.EventsList;
import com.irccloud.android.data.collection.ImageList;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.data.model.Buffer;

import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("unused")
public class IRCCloudApplication extends Application {
    private static IRCCloudApplication instance = null;

    public static IRCCloudApplication getInstance() {
        if (instance != null) {
            return instance;
        } else {
            return new IRCCloudApplication();
        }
    }

    private NetworkConnection conn = null;
    private TimerTask notifierSockerTimerTask = null;
    private static final Timer notifierTimer = new Timer("notifier-timer");
    private Typeface csFont;
    private Handler mFontsHandler;

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        FirebaseApp.initializeApp(getApplicationContext());
        TrustKit.initializeWithNetworkSecurityConfiguration(getApplicationContext(), R.xml.network_security_config);
        try {
            FirebaseAnalytics.getInstance(this).setUserId(null);
            IRCCloudLog.CrashlyticsEnabled = true;
            IRCCloudLog.Log(Log.INFO, "IRCCloud", "Crashlytics Initialized");
        } catch (Exception e) {

        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if(prefs.getInt("dbVersion", 0) < IRCCloudDatabase.VERSION) {
            getApplicationContext().getDatabasePath(IRCCloudDatabase.NAME + ".db").delete();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("dbVersion", IRCCloudDatabase.VERSION);
            editor.apply();
        }

        EmojiCompat.init(new FontRequestEmojiCompatConfig(getApplicationContext(), new FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Noto Color Emoji Compat",
                R.array.com_google_android_gms_fonts_certs))
                .setReplaceAll(!prefs.getBoolean("preferSystemEmoji", true))
                .registerInitCallback(new EmojiCompat.InitCallback() {
                    @Override
                    public void onInitialized() {
                        Log.i("IRCCloud", "EmojiCompat initialized");
                        super.onInitialized();
                        EventsList.getInstance().clearCaches();
                        conn.notifyHandlers(NetworkConnection.EVENT_FONT_DOWNLOADED, null);
                    }
                    @Override
                    public void onFailed(@Nullable Throwable throwable) {
                        Log.e("IRCCloud", "EmojiCompat initialization failed: ", throwable);
                        IRCCloudLog.LogException(throwable);
                    }
                }));
        //EmojiCompat.init(new BundledEmojiCompatConfig(this).setReplaceAll(!prefs.getBoolean("preferSystemEmoji", true)));
        NetworkConnection.getInstance().registerForConnectivity();

        //Disable HTTP keep-alive for our app, as some versions of Android will return an empty response
        System.setProperty("http.keepAlive", "false");

        //Allocate all the shared objects at launch
        conn = NetworkConnection.getInstance();
        ColorFormatter.init();

        prefs = getSharedPreferences("prefs", 0);
        NetworkConnection.IRCCLOUD_HOST = prefs.getString("host", BuildConfig.HOST);
        NetworkConnection.IRCCLOUD_PATH = prefs.getString("path", "/");

        /*try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE))
                    WebView.setWebContentsDebuggingEnabled(true);
            }
        } catch (Exception e) {
        }*/

        FontRequest request = new FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                "Dekko",
                R.array.com_google_android_gms_fonts_certs);

        FontsContractCompat.requestFont(getApplicationContext(), request, new FontsContractCompat.FontRequestCallback() {
            @Override
            public void onTypefaceRetrieved(Typeface typeface) {
                csFont = typeface;
                EventsList.getInstance().clearCaches();
                NetworkConnection.getInstance().notifyHandlers(NetworkConnection.EVENT_FONT_DOWNLOADED, null);
            }
        }, getFontsHandler());


        IRCCloudLog.Log(Log.INFO, "IRCCloud", "App Initialized");
    }

    @Override
    public void onTerminate() {
        NetworkConnection.getInstance().unregisterForConnectivity();
        NetworkConnection.getInstance().save(1);
        super.onTerminate();
    }

    public void onPause(final Activity context) {
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (notifierSockerTimerTask != null)
            notifierSockerTimerTask.cancel();
        notifierSockerTimerTask = new TimerTask() {
            @Override
            public void run() {
                notifierSockerTimerTask = null;
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
                    if(am != null && am.getRunningAppProcesses() != null) {
                        for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
                            if (info.processName.equals(context.getPackageName()) && info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND)
                                return;
                        }
                    }
                }
                if(!conn.notifier && conn.getState() == NetworkConnection.STATE_CONNECTED) {
                    conn.disconnect();
                    if(ServersList.getInstance().count() < 1) {
                        IRCCloudLog.Log(Log.DEBUG, "IRCCloud", "No servers configured, not connecting notifier socket");
                        return;
                    }
                    if(!(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && cm.isActiveNetworkMetered() && cm.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED)) {
                        try {
                            Thread.sleep(1000);
                            conn.notifier = true;
                            conn.connect();
                        } catch (Exception e) {
                        }
                    }
                }
            }
        };

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && cm.isActiveNetworkMetered() && cm.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED) {
            android.util.Log.d("IRCCloud", "notifier timer scheduled for 5000 seconds");
            notifierTimer.schedule(notifierSockerTimerTask, 5000);
        } else {
            android.util.Log.d("IRCCloud", "notifier timer scheduled for 300000 seconds");
            notifierTimer.schedule(notifierSockerTimerTask, 300000);
        }
    }

    public void onResume(Activity context) {
        cancelNotifierTimer();
    }

    public void cancelNotifierTimer() {
        if(notifierSockerTimerTask != null) {
            android.util.Log.d("IRCCloud", "notifier timer cancelled");
            notifierSockerTimerTask.cancel();
            notifierSockerTimerTask = null;
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (!NetworkConnection.getInstance().isVisible()) {
            IRCCloudLog.Log(Log.DEBUG, "IRCCloud", "Received low memory warning in the background, cleaning backlog in all buffers");
            BuffersList buffersList = BuffersList.getInstance();
            EventsList eventsList = EventsList.getInstance();
            for (Buffer b : buffersList.getBuffers()) {
                if (!b.getScrolledUp())
                    eventsList.pruneEvents(b.getBid());
            }
        }
        ImageList.getInstance().clear();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if(level >= TRIM_MEMORY_MODERATE) {
            onLowMemory();
        }
    }

    public Typeface getCsFont() {
        return csFont;
    }

    private Handler getFontsHandler() {
        if (mFontsHandler == null) {
            HandlerThread handlerThread = new HandlerThread("fonts");
            handlerThread.start();
            mFontsHandler = new Handler(handlerThread.getLooper());
        }
        return mFontsHandler;
    }
}
