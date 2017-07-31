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
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.text.emoji.EmojiCompat;
import android.support.text.emoji.FontRequestEmojiCompatConfig;
import android.support.text.emoji.bundled.BundledEmojiCompatConfig;
import android.support.v4.app.ActivityCompat;
import android.support.v4.provider.FontRequest;
import android.support.v4.provider.FontsContractCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.datatheorem.android.trustkit.TrustKit;
import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.collection.NotificationsList;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.collection.EventsList;
import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import io.fabric.sdk.android.Fabric;@SuppressWarnings("unused")
public class IRCCloudApplicationBase extends Application {
    private NetworkConnection conn = null;
    private TimerTask notifierSockerTimerTask = null;
    private static final Timer notifierTimer = new Timer("notifier-timer");
    private Typeface csFont;
    private Handler mFontsHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            TrustKit.initializeWithNetworkSecurityConfiguration(getApplicationContext(), R.xml.network_security_config);
        Fabric.with(this, new Crashlytics());
        Crashlytics.log(Log.INFO, "IRCCloud", "Crashlytics Initialized");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if(prefs.getInt("dbVersion", 0) < IRCCloudDatabase.VERSION) {
            getApplicationContext().getDatabasePath(IRCCloudDatabase.NAME + ".db").delete();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("dbVersion", IRCCloudDatabase.VERSION);
            editor.commit();
        }
        FlowManager.init(new FlowConfig.Builder(this).build());

        if(Build.VERSION.SDK_INT >= 19)
            EmojiCompat.init(new BundledEmojiCompatConfig(this).setReplaceAll(!prefs.getBoolean("preferSystemEmoji", true)));
            /*EmojiCompat.init(new FontRequestEmojiCompatConfig(getApplicationContext(), new FontRequest(
                    "com.google.android.gms.fonts",
                    "com.google.android.gms",
                    "Noto Color Emoji Compat",
                    R.array.com_google_android_gms_fonts_certs))
                    .setReplaceAll(!prefs.getBoolean("preferSystemEmoji", true))
                    .registerInitCallback(new EmojiCompat.InitCallback() {
                        @Override
                        public void onInitialized() {
                            super.onInitialized();
                            EventsList.getInstance().clearCaches();
                            conn.notifyHandlers(NetworkConnection.EVENT_FONT_DOWNLOADED, null);
                        }
                    }));*/
        NetworkConnection.getInstance().registerForConnectivity();

        //Disable HTTP keep-alive for our app, as some versions of Android will return an empty response
        System.setProperty("http.keepAlive", "false");

        //Allocate all the shared objects at launch
        conn = NetworkConnection.getInstance();
        ColorFormatter.init();

        if (prefs.contains("notify")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("notify_type", prefs.getBoolean("notify", true) ? "1" : "0");
            editor.remove("notify");
            editor.commit();
        }

        if (prefs.contains("notify_sound")) {
            SharedPreferences.Editor editor = prefs.edit();
            if (!prefs.getBoolean("notify_sound", true))
                editor.putString("notify_ringtone", "");
            editor.remove("notify_sound");
            editor.commit();
        }

        if (prefs.contains("notify_lights")) {
            SharedPreferences.Editor editor = prefs.edit();
            if (!prefs.getBoolean("notify_lights", true))
                editor.putString("notify_led_color", "0");
            editor.remove("notify_lights");
            editor.commit();
        }

        if(!prefs.getBoolean("ringtone_migrated", false)) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                String notification_uri = prefs.getString("notify_ringtone", "");
                if (notification_uri.startsWith("content://media/external/audio/media/")) {
                    Cursor c = getContentResolver().query(
                            Uri.parse(notification_uri),
                            new String[]{MediaStore.Audio.Media.TITLE},
                            null,
                            null,
                            null);

                    if (c != null && c.moveToFirst()) {
                        if (c.getString(0).equals("IRCCloud")) {
                            Log.d("IRCCloud", "Migrating notification ringtone setting: " + notification_uri);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.remove("notify_ringtone");
                            editor.commit();
                        }
                    }
                    if (c != null && !c.isClosed()) {
                        c.close();
                    }
                }

                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS);
                File file = new File(path, "IRCCloud.mp3");
                if (file.exists()) {
                    file.delete();
                }

                try {
                    getContentResolver().delete(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            MediaStore.Audio.Media.TITLE + " = 'IRCCloud'",
                            null);
                } catch (Exception e) {
                    // Ringtone not in media DB
                }
            }

            try {
                String notification_uri = prefs.getString("notify_ringtone", "");
                if (notification_uri.startsWith("content://media/")) {
                    Cursor c = getContentResolver().query(
                            Uri.parse(notification_uri),
                            new String[]{MediaStore.Audio.Media.TITLE},
                            null,
                            null,
                            null);

                    if (c != null && c.moveToFirst()) {
                        if (c.getString(0).equals("IRCCloud")) {
                            Log.d("IRCCloud", "Migrating notification ringtone setting: " + notification_uri);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.remove("notify_ringtone");
                            editor.commit();
                        }
                    }
                    if (c != null && !c.isClosed()) {
                        c.close();
                    }
                }
            } catch (Exception e) {
                //We might not have permission to query the media DB
            }

            try {
                getContentResolver().delete(
                        MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                        MediaStore.Audio.Media.TITLE + " = 'IRCCloud'",
                        null);
            } catch (Exception e) {
                // Ringtone not in media DB
                e.printStackTrace();
            }

            File file = new File(getFilesDir(), "IRCCloud.mp3");
            if (file.exists()) {
                file.delete();
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("ringtone_migrated", true);
            editor.commit();
        }

        if (prefs.contains("notify_pebble")) {
            try {
                int pebbleVersion = getPackageManager().getPackageInfo("com.getpebble.android", 0).versionCode;
                if (pebbleVersion >= 553 && Build.VERSION.SDK_INT >= 18) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.remove("notify_pebble");
                    editor.commit();
                }
            } catch (Exception e) {
            }
        }

        if (prefs.contains("acra.enable")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("acra.enable");
            editor.commit();
        }

        if(prefs.contains("notifications_json")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("notifications_json");
            editor.remove("networks_json");
            editor.remove("lastseeneids_json");
            editor.remove("dismissedeids_json");
            editor.commit();
        }

        prefs = getSharedPreferences("prefs", 0);
        if (prefs.getString("host", "www.irccloud.com").equals("www.irccloud.com") && !prefs.contains("path") && prefs.contains("session_key")) {
            Crashlytics.log(Log.INFO, "IRCCloud", "Migrating path from session key");
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("path", "/websocket/" + prefs.getString("session_key", "").charAt(0));
            editor.commit();
        }
        if (prefs.contains("host") && prefs.getString("host", "").equals("www.irccloud.com")) {
            Crashlytics.log(Log.INFO, "IRCCloud", "Migrating host");
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("host", "api.irccloud.com");
            editor.commit();
        }
        if (prefs.contains("gcm_app_version")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("gcm_app_version");
            editor.remove("gcm_app_build");
            editor.remove("gcm_registered");
            editor.remove("gcm_reg_id");
            editor.commit();
        }

        NetworkConnection.IRCCLOUD_HOST = prefs.getString("host", BuildConfig.HOST);
        NetworkConnection.IRCCLOUD_PATH = prefs.getString("path", "/");

        /*try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE))
                    WebView.setWebContentsDebuggingEnabled(true);
            }
        } catch (Exception e) {
        }*/

        /*FontRequest request = new FontRequest(
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
        }, getFontsHandler());*/


        Crashlytics.log(Log.INFO, "IRCCloud", "App Initialized");
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
                        Crashlytics.log(Log.DEBUG, "IRCCloud", "No servers configured, not connecting notifier socket");
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
            notifierTimer.schedule(notifierSockerTimerTask, 5000);
        } else {
            notifierTimer.schedule(notifierSockerTimerTask, 300000);
        }
    }

    public void onResume(Activity context) {
        cancelNotifierTimer();
    }

    public void cancelNotifierTimer() {
        if(notifierSockerTimerTask != null) {
            notifierSockerTimerTask.cancel();
            notifierSockerTimerTask = null;
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (!NetworkConnection.getInstance().isVisible()) {
            Crashlytics.log(Log.DEBUG, "IRCCloud", "Received low memory warning in the background, cleaning backlog in all buffers");
            BuffersList buffersList = BuffersList.getInstance();
            EventsList eventsList = EventsList.getInstance();
            for (Buffer b : buffersList.getBuffers()) {
                if (!b.getScrolledUp())
                    eventsList.pruneEvents(b.getBid());
            }
        }
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
