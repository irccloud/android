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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.TaskParams;
import com.google.android.gms.iid.InstanceID;
import com.irccloud.android.data.model.BackgroundTask;
import com.irccloud.android.data.model.BackgroundTask$Table;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Select;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class BackgroundTaskService extends GcmTaskService {
    private static final int GCM_INTERVAL = 30; //Wait up to 30 seconds before sending GCM registration
    private static final int SYNC_INTERVAL = 60 * 60; //Sync backlog hourly

    public static void registerGCM(Context context) {
        List<BackgroundTask> tasks = new Select().from(BackgroundTask.class).where(Condition.column(BackgroundTask$Table.TYPE).is(BackgroundTask.TYPE_GCM_REGISTER)).queryList();
        for(BackgroundTask t : tasks) {
            GcmNetworkManager.getInstance(context).cancelTask(t.tag, BackgroundTaskService.class);
            t.delete();
        }
        BackgroundTask task = new BackgroundTask();
        task.type = BackgroundTask.TYPE_GCM_REGISTER;
        task.tag = Long.toString(System.currentTimeMillis());
        task.session = NetworkConnection.getInstance().session;
        task.save();

        GcmNetworkManager.getInstance(context).schedule(new OneoffTask.Builder()
                .setTag(task.tag)
                .setExecutionWindow(1, GCM_INTERVAL)
                .setService(BackgroundTaskService.class)
                .build());
    }

    public static void unregisterGCM(final Context context) {
        String token = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).getString("gcm_token", null);
        if(token != null && token.length() > 0) {
            List<BackgroundTask> tasks = new Select().from(BackgroundTask.class).where(Condition.column(BackgroundTask$Table.TYPE).is(BackgroundTask.TYPE_GCM_REGISTER))
                    .and(Condition.column(BackgroundTask$Table.DATA).is(token))
                    .queryList();

            for(BackgroundTask t : tasks) {
                GcmNetworkManager.getInstance(context).cancelTask(t.tag, BackgroundTaskService.class);
                t.delete();
            }

            BackgroundTask task = new BackgroundTask();
            task.type = BackgroundTask.TYPE_GCM_UNREGISTER;
            task.tag = Long.toString(System.currentTimeMillis());
            task.data = token;
            task.session = NetworkConnection.getInstance().session;
            task.save();

            GcmNetworkManager.getInstance(context).schedule(new OneoffTask.Builder()
                    .setTag(task.tag)
                    .setExecutionWindow(1, GCM_INTERVAL)
                    .setService(BackgroundTaskService.class)
                    .build());
        }
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    InstanceID.getInstance(context).deleteInstanceID();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute((Void) null);
    }

    public static void scheduleBacklogSync(Context context) {
        /*android.util.Log.d("IRCCloud", "Scheduling background sync");
        List<BackgroundTask> tasks = new Select().from(BackgroundTask.class).where(Condition.column(BackgroundTask$Table.TYPE).is(BackgroundTask.TYPE_BACKLOG_SYNC)).queryList();
        for(BackgroundTask t : tasks) {
            GcmNetworkManager.getInstance(context).cancelTask(t.tag, BackgroundTaskService.class);
            t.delete();
        }
        BackgroundTask task = new BackgroundTask();
        task.type = BackgroundTask.TYPE_BACKLOG_SYNC;
        task.tag = Long.toString(System.currentTimeMillis());
        task.session = NetworkConnection.getInstance().session;
        task.save();

        GcmNetworkManager.getInstance(context).schedule(new PeriodicTask.Builder()
                .setTag(task.tag)
                .setPeriod(SYNC_INTERVAL)
                .setService(BackgroundTaskService.class)
                .build());

        android.util.Log.d("IRCCloud", "Scheduled job with task ID: " + task.tag);*/
    }

    public static void cancelBacklogSync(Context context) {
        /*android.util.Log.d("IRCCloud", "Cancelling background sync");
        List<BackgroundTask> tasks = new Select().from(BackgroundTask.class).where(Condition.column(BackgroundTask$Table.TYPE).is(BackgroundTask.TYPE_BACKLOG_SYNC)).queryList();
        for (BackgroundTask t : tasks) {
            android.util.Log.d("IRCCloud", "Cancelled job with task ID: " + t.tag);
            GcmNetworkManager.getInstance(context).cancelTask(t.tag, BackgroundTaskService.class);
            t.delete();
        }*/
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        BackgroundTask task = new Select().from(BackgroundTask.class).where(Condition.column(BackgroundTask$Table.TAG).is(taskParams.getTag())).querySingle();
        if(task != null) {
            switch(task.type) {
                case BackgroundTask.TYPE_GCM_REGISTER:
                    return onGcmRegister(task);
                case BackgroundTask.TYPE_GCM_UNREGISTER:
                    return onGcmUnregister(task);
                case BackgroundTask.TYPE_BACKLOG_SYNC:
                    sendBroadcast(new Intent(this, SyncReceiver.class));
                    return GcmNetworkManager.RESULT_SUCCESS;
            }
        }

        return GcmNetworkManager.RESULT_FAILURE;
    }

    private int onGcmRegister(BackgroundTask task) {
        try {
            android.util.Log.i("IRCCloud", "Registering for GCM");
            String token = task.data;
            if(token == null || token.length() == 0) {
                token = InstanceID.getInstance(this).getToken(BuildConfig.GCM_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                task.data = token;
                task.save();
                SharedPreferences.Editor editor = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).edit();
                editor.putString("gcm_token", token);
                editor.commit();
            }
            if(token != null && token.length() > 0) {
                JSONObject result = NetworkConnection.getInstance().registerGCM(token, task.session);
                if (result.has("success")) {
                    if(result.getBoolean("success")) {
                        android.util.Log.i("IRCCloud", "Device successfully registered");
                        task.delete();
                        return GcmNetworkManager.RESULT_SUCCESS;
                    } else {
                        android.util.Log.e("IRCCloud", "Unable to register device: " + result.toString());
                        return GcmNetworkManager.RESULT_RESCHEDULE;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        android.util.Log.e("IRCCloud", "GCM registration failed");
        task.delete();

        return GcmNetworkManager.RESULT_FAILURE;
    }

    private int onGcmUnregister(BackgroundTask task) {
        try {
            android.util.Log.i("IRCCloud", "Unregistering GCM");
            JSONObject result = NetworkConnection.getInstance().unregisterGCM(task.data, task.session);
            if (result.has("success")) {
                if(result.getBoolean("success")) {
                    android.util.Log.i("IRCCloud", "Device successfully unregistered");
                    task.delete();
                    SharedPreferences.Editor e = getSharedPreferences("prefs", 0).edit();
                    e.remove(task.data);
                    e.commit();
                    return GcmNetworkManager.RESULT_SUCCESS;
                } else {
                    android.util.Log.e("IRCCloud", "Unable to unregister device: " + result.toString());
                    return GcmNetworkManager.RESULT_RESCHEDULE;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        android.util.Log.e("IRCCloud", "GCM unregistration failed");
        task.delete();

        return GcmNetworkManager.RESULT_FAILURE;
    }
}
