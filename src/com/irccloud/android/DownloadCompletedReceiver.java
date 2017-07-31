/*
 * Copyright (c) 2016 IRCCloud, Ltd.
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

import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.irccloud.android.activity.LogExportsActivity;
import com.irccloud.android.data.collection.LogExportsList;
import com.irccloud.android.data.collection.NotificationsList;
import com.irccloud.android.data.model.LogExport;

public class DownloadCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        LogExport e = LogExportsList.getInstance().getDownload(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1));

        if(e != null) {
            DownloadManager d = (DownloadManager) IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor c = d.query(new DownloadManager.Query().setFilterById(e.download_id));

            if(c != null && c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));

                NotificationsList.getInstance().createChannel("export_complete", "Download Completed", NotificationManagerCompat.IMPORTANCE_DEFAULT, null);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
                String ringtone = prefs.getString("notify_ringtone", "android.resource://" + IRCCloudApplication.getInstance().getApplicationContext().getPackageName() + "/" + R.raw.digit);

                NotificationCompat.Builder notification = new NotificationCompat.Builder(IRCCloudApplication.getInstance().getApplicationContext(), "export_complete")
                        .setContentTitle(e.file_name)
                        .setContentText((status == DownloadManager.STATUS_SUCCESSFUL) ? "Download complete." : "Download failed.")
                        .setAutoCancel(true)
                        .setLocalOnly(true)
                        .setColor(IRCCloudApplication.getInstance().getApplicationContext().getResources().getColor(R.color.ic_background))
                        .setSmallIcon(android.R.drawable.stat_sys_download_done);

                if (ringtone.length() > 0)
                    notification.setSound(Uri.parse(ringtone));

                int defaults = 0;
                int led_color = Integer.parseInt(prefs.getString("notify_led_color", "1"));
                if (led_color == 1) {
                    defaults = android.app.Notification.DEFAULT_LIGHTS;
                } else if (led_color == 2) {
                    notification.setLights(0xFF0000FF, 500, 500);
                }

                if (prefs.getBoolean("notify_vibrate", true))
                    defaults |= android.app.Notification.DEFAULT_VIBRATE;
                else
                    notification.setVibrate(new long[]{0L});

                notification.setDefaults(defaults);

                Intent i = new Intent(context, LogExportsActivity.class);
                notification.setContentIntent(PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT));

                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify((int)e.download_id, notification.build());

            }
        }
    }
}
