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



import android.*;
import android.app.Application;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.webkit.WebView;

import com.crashlytics.android.Crashlytics;
import com.irccloud.android.data.collection.NotificationsList;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.collection.EventsList;
import com.raizlabs.android.dbflow.config.FlowManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.fabric.sdk.android.Fabric;@SuppressWarnings("unused")
public class IRCCloudApplicationBase extends Application {
    private static final int RINGTONE_VERSION = 4;

    private NetworkConnection conn = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        FlowManager.init(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

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

        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
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
                    c.close();
                }
            }

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS);
            File file = new File(path, "IRCCloud.mp3");
            if(file.exists()) {
                file.delete();
            }

            getContentResolver().delete(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    MediaStore.Audio.Media.TITLE + " = 'IRCCloud'",
                    null);
        }

        if (prefs.getInt("ringtone_version", 0) < RINGTONE_VERSION) {
            SharedPreferences.Editor editor = prefs.edit();
            File path = getFilesDir();
            File file = new File(path, "IRCCloud.mp3");
            try {
                path.mkdirs();
                InputStream is = getResources().openRawResource(R.raw.digit);
                OutputStream os = new FileOutputStream(file);
                byte[] data = new byte[is.available()];
                is.read(data);
                os.write(data);
                is.close();
                os.close();
                file.setReadable(true, false);

                Cursor c = getContentResolver().query(
                        MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                        new String[] { MediaStore.Audio.Media._ID },
                        MediaStore.Audio.Media.IS_NOTIFICATION + " == 1 AND " + MediaStore.Audio.Media.TITLE + " == \"IRCCloud\"",
                        null,
                        null);

                if(c != null && c.moveToFirst()) {
                    do {
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Audio.Media.IS_NOTIFICATION, "1");
                        values.put(MediaStore.Audio.Media.IS_MUSIC, "0");
                        getContentResolver().update(MediaStore.Audio.Media.INTERNAL_CONTENT_URI.buildUpon().appendPath(c.getString(0)).build(), values, null, null);
                    } while(c.moveToNext());
                    c.close();
                } else {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Audio.Media.DATA, file.getAbsolutePath());
                    values.put(MediaStore.Audio.Media.TITLE, "IRCCloud");
                    values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3");
                    values.put(MediaStore.Audio.Media.IS_NOTIFICATION, "1");
                    values.put(MediaStore.Audio.Media.IS_MUSIC, "0");
                    Uri uri = getContentResolver().insert(MediaStore.Audio.Media.getContentUriForPath(file.getAbsolutePath()), values);

                    if (uri != null && prefs.getString("notify_ringtone", "").length() == 0) {
                        editor.putString("notify_ringtone", uri.toString());
                    }
                }
                editor.putInt("ringtone_version", RINGTONE_VERSION);
                editor.commit();

            } catch (IOException e) {
                if (!prefs.contains("notify_ringtone")) {
                    editor.putString("notify_ringtone", "content://settings/system/notification_sound");
                    editor.commit();
                }
            }
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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE))
                    WebView.setWebContentsDebuggingEnabled(true);
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onTerminate() {
        NetworkConnection.getInstance().save(1);
        super.onTerminate();
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
}
