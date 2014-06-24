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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.Application;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.irccloud.android.data.BuffersDataSource;
import com.irccloud.android.data.ChannelsDataSource;
import com.irccloud.android.data.EventsDataSource;
import com.irccloud.android.data.ServersDataSource;
import com.irccloud.android.data.UsersDataSource;

@SuppressWarnings("unused")
public class IRCCloudApplication extends Application {
	private static final int RINGTONE_VERSION = 1;
	
	private static IRCCloudApplication instance = null;
	private NetworkConnection conn = null;
	private ServersDataSource s = null;
	private BuffersDataSource b = null;
	private ChannelsDataSource c = null;
	private UsersDataSource u = null;
	private EventsDataSource e = null;
	
	public static IRCCloudApplication getInstance() {
		if(instance != null) {
			return instance;
		} else {
			return new IRCCloudApplication();
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
        instance = this;
		//Allocate all the shared objects at launch
		conn = NetworkConnection.getInstance();
		s = ServersDataSource.getInstance();
		b = BuffersDataSource.getInstance();
		c = ChannelsDataSource.getInstance();
		u = UsersDataSource.getInstance();
		e = EventsDataSource.getInstance();
        ColorFormatter.init();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(prefs.getBoolean("acra.enable", true)) {
            try {
                if(getResources().getString(R.string.CRASHLYTICS_KEY).length() > 0)
                    Crashlytics.start(this);
            } catch (Exception e) {
            }
        }

        if(prefs.contains("notify")) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("notify_type", prefs.getBoolean("notify", true)?"1":"0");
			editor.remove("notify");
			editor.commit();
		}

		if(prefs.contains("notify_sound")) {
			SharedPreferences.Editor editor = prefs.edit();
			if(!prefs.getBoolean("notify_sound", true))
				editor.putString("notify_ringtone", "");
			editor.remove("notify_sound");
			editor.commit();
		}

        if(prefs.contains("notify_lights")) {
            SharedPreferences.Editor editor = prefs.edit();
            if(!prefs.getBoolean("notify_lights", true))
                editor.putString("notify_led_color", "0");
            editor.remove("notify_lights");
            editor.commit();
        }

		if(prefs.getInt("ringtone_version", 0) < RINGTONE_VERSION) {
			File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS);
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
		        MediaScannerConnection.scanFile(this,
		                new String[] { file.toString() }, null,
		                new MediaScannerConnection.OnScanCompletedListener() {
					@Override
		            public void onScanCompleted(String path, Uri uri) {
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
						SharedPreferences.Editor editor = prefs.edit();
						if(!prefs.contains("notify_ringtone")) {
							editor.putString("notify_ringtone", uri.toString());
						}
						editor.putInt("ringtone_version", RINGTONE_VERSION);
						editor.commit();
		            }
		        });
			} catch (IOException e) {
				if(!prefs.contains("notify_ringtone")) {
					SharedPreferences.Editor editor = prefs.edit();
					editor.putString("notify_ringtone", "content://settings/system/notification_sound");
					editor.commit();
				}
		    }
		}

        prefs = getSharedPreferences("prefs", 0);
        if(prefs.getString("host", "www.irccloud.com").equals("www.irccloud.com") && !prefs.contains("path") && prefs.contains("session_key")) {
            Crashlytics.log(Log.INFO, "IRCCloud", "Migrating path from session key");
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("path", "/websocket/" + prefs.getString("session_key", "").charAt(0));
            editor.commit();
        }
        if(prefs.contains("host") && prefs.getString("host", "").equals("www.irccloud.com")) {
            Crashlytics.log(Log.INFO, "IRCCloud", "Migrating host");
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("host", "api.irccloud.com");
            editor.commit();
        }

        NetworkConnection.IRCCLOUD_HOST = prefs.getString("host", BuildConfig.HOST);
        NetworkConnection.IRCCLOUD_PATH = prefs.getString("path", "/");
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if(!NetworkConnection.getInstance().isVisible()) {
            Crashlytics.log(Log.DEBUG, "IRCCloud", "Received low memory warning in the background, cleaning backlog in all buffers");
            for (BuffersDataSource.Buffer b : BuffersDataSource.getInstance().getBuffers()) {
                if (!b.scrolledUp && EventsDataSource.getInstance().getHighlightStateForBuffer(b.bid, b.last_seen_eid, b.type) == 0)
                    EventsDataSource.getInstance().pruneEvents(b.bid);
            }
        }
    }
}
