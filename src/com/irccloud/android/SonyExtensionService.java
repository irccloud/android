
/*
Copyright (c) 2011, Sony Ericsson Mobile Communications AB

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the Sony Ericsson Mobile Communications AB nor the names
  of its contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.irccloud.android;

import android.net.Uri;
import com.sonyericsson.extras.liveware.aef.notification.Notification;
import com.sonyericsson.extras.liveware.extension.util.ExtensionService;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.Random;

/**
 * The sample extension service handles extension registration and inserts
 * data into the notification database.
 */
public class SonyExtensionService extends ExtensionService {

    /**
     * Extensions specific id for the source
     */
    public static final String EXTENSION_SPECIFIC_ID = "com.irccloud.android";

    /**
     * Extension key
     */
    public static final String EXTENSION_KEY = "com.irccloud.android.key";

    /**
     * Log tag
     */
    public static final String LOG_TAG = "IRCCloud";

    public SonyExtensionService() {
        super(EXTENSION_KEY);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Service#onStartCommand()
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * {@inheritDoc}
     *
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
    }

    @Override
    protected void onViewEvent(Intent intent) {
        String action = intent.getStringExtra(Notification.Intents.EXTRA_ACTION);
        int eventId = intent.getIntExtra(Notification.Intents.EXTRA_EVENT_ID, -1);
        if (Notification.SourceColumns.ACTION_1.equals(action)) {
            doAction1(eventId);
        }
    }

    @Override
    protected void onRefreshRequest() {
        // Do nothing here, only relevant for polling extensions, this
        // extension is always up to date
    }

    /**
     * Launch IRCCloud and open the correct buffer
     *
     * @param eventId The event id
     */
    public void doAction1(int eventId) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(Notification.Event.URI, null,
                    Notification.EventColumns._ID + " = " + eventId, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(Notification.EventColumns.DISPLAY_NAME);
                int messageIndex = cursor.getColumnIndex(Notification.EventColumns.MESSAGE);
                String bid = cursor.getString(cursor.getColumnIndex(Notification.EventColumns.FRIEND_KEY));
                Intent i = new Intent(IRCCloudApplication.getInstance().getApplicationContext(), MessageActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.putExtra("bid", Integer.parseInt(bid));
                i.setData(Uri.parse("bid://" + bid));
                startActivity(i);
            }
        } catch (SQLException e) {
            Log.e(LOG_TAG, "Failed to query event", e);
        } catch (SecurityException e) {
            Log.e(LOG_TAG, "Failed to query event", e);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Failed to query event", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Called when extension and sources has been successfully registered.
     * Override this method to take action after a successful registration.
     */
    @Override
    public void onRegisterResult(boolean result) {
        super.onRegisterResult(result);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
        if(prefs.getBoolean("notify_sony",true)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("notify_sony", true);
            editor.apply();
        }
    }

    @Override
    protected RegistrationInformation getRegistrationInformation() {
        return new SonyRegistrationInformation(this);
    }

    /* (non-Javadoc)
     * @see com.sonyericsson.extras.liveware.aef.util.ExtensionService#keepRunningWhenConnected()
     */
    @Override
    protected boolean keepRunningWhenConnected() {
        return false;
    }
}
