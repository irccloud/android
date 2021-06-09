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
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.model.BackgroundTask;

import org.json.JSONObject;

import java.util.List;

public class BackgroundTaskWorker extends Worker {
    @Dao
    public interface BackgroundTasksDao {
        @Query("SELECT * FROM BackgroundTask")
        List<BackgroundTask> getBackgroundTasks();

        @Query("SELECT * FROM BackgroundTask WHERE type = :type")
        List<BackgroundTask> getBackgroundTasks(int type);

        @Query("SELECT * FROM BackgroundTask WHERE type = :type AND data = :data")
        List<BackgroundTask> getBackgroundTasks(int type, String data);

        @Query("SELECT * FROM BackgroundTask WHERE tag = :tag LIMIT 1")
        BackgroundTask getBackgroundTask(String tag);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(BackgroundTask backgroundTask);

        @Update
        void update(BackgroundTask backgroundTask);

        @Delete
        void delete(BackgroundTask backgroundTask);
    }

    public BackgroundTaskWorker(@NonNull Context appContext, @NonNull WorkerParameters params) {
        super(appContext, params);
    }

    public static void registerGCM(String token) {
        List<BackgroundTask> tasks = IRCCloudDatabase.getInstance().BackgroundTasksDao().getBackgroundTasks(BackgroundTask.TYPE_FCM_REGISTER);
        for(BackgroundTask t : tasks) {
            IRCCloudLog.Log(Log.INFO, "IRCCloud", "Removing old FCM registration task: " + t.getTag());
            try {
                WorkManager.getInstance(IRCCloudApplication.getInstance().getApplicationContext()).cancelUniqueWork(t.getTag());
            } catch (Exception e) {
            }
            IRCCloudDatabase.getInstance().BackgroundTasksDao().delete(t);
        }

        if(NetworkConnection.getInstance().session != null && NetworkConnection.getInstance().session.length() > 0) {
            BackgroundTask task = new BackgroundTask();
            task.setType(BackgroundTask.TYPE_FCM_REGISTER);
            task.setData(token);
            task.setTag(Long.toString(System.currentTimeMillis()));
            task.setSession(NetworkConnection.getInstance().session);

            IRCCloudLog.Log(Log.INFO, "IRCCloud", "Scheduled FCM registration task: " + task.getTag());
            try {
                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BackgroundTaskWorker.class)
                        .setInputData(new Data.Builder().putString("tag", task.getTag()).build())
                        .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .build();
                WorkManager.getInstance(IRCCloudApplication.getInstance().getApplicationContext()).enqueueUniqueWork(task.getTag(), ExistingWorkPolicy.REPLACE, request);
            } catch (Exception e) {
                return;
            }
            IRCCloudDatabase.getInstance().BackgroundTasksDao().insert(task);
        }
    }

    private static void scheduleUnregister(String token, String session) {
        if(token != null && token.length() > 0) {
            List<BackgroundTask> tasks = IRCCloudDatabase.getInstance().BackgroundTasksDao().getBackgroundTasks(BackgroundTask.TYPE_FCM_REGISTER, token);

            for(BackgroundTask t : tasks) {
                try {
                    WorkManager.getInstance(IRCCloudApplication.getInstance().getApplicationContext()).cancelUniqueWork(t.getTag());
                } catch (Exception e) {
                }
                IRCCloudDatabase.getInstance().BackgroundTasksDao().delete(t);
            }

            BackgroundTask task = new BackgroundTask();
            task.setType(BackgroundTask.TYPE_FCM_UNREGISTER);
            task.setTag(Long.toString(System.currentTimeMillis()));
            task.setData(token);
            task.setSession(session);

            try {
                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BackgroundTaskWorker.class)
                        .setInputData(new Data.Builder().putString("tag", task.getTag()).build())
                        .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .build();
                WorkManager.getInstance(IRCCloudApplication.getInstance().getApplicationContext()).enqueueUniqueWork(task.getTag(), ExistingWorkPolicy.REPLACE, request);
            } catch (Exception e) {
                return;
            }
            IRCCloudDatabase.getInstance().BackgroundTasksDao().insert(task);
        }
    }

    public static void unregisterGCM() {
        final String session = NetworkConnection.getInstance().session;
        final String token = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).getString("gcm_token", null);

        if(token == null)
            return;

        List<BackgroundTask> tasks = IRCCloudDatabase.getInstance().BackgroundTasksDao().getBackgroundTasks(BackgroundTask.TYPE_FCM_REGISTER, token);

        for(BackgroundTask t : tasks) {
            try {
                WorkManager.getInstance(IRCCloudApplication.getInstance().getApplicationContext()).cancelUniqueWork(t.getTag());
            } catch (Exception e) {
            }
            IRCCloudDatabase.getInstance().BackgroundTasksDao().delete(t);
        }

        if(Looper.myLooper() == Looper.getMainLooper()) {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... voids) {
                    FirebaseMessaging.getInstance().deleteToken();
                    if(!onGcmUnregister(token, session).equals(Result.success()))
                        scheduleUnregister(token, session);
                    return null;
                }
            }.execute((Void) null);
        } else {
            FirebaseMessaging.getInstance().deleteToken();
            if(!onGcmUnregister(token, session).equals(Result.success()))
                scheduleUnregister(token, session);
        }
    }

    @Override
    public ListenableWorker.Result doWork() {
        String tag = getInputData().getString("tag");
        IRCCloudLog.Log(Log.INFO, "IRCCloud", "Executing background task with tag: " + tag);
        BackgroundTask task = IRCCloudDatabase.getInstance().BackgroundTasksDao().getBackgroundTask(tag);
        if(task != null) {
            switch(task.getType()) {
                case BackgroundTask.TYPE_FCM_REGISTER:
                    return onGcmRegister(task);
                case BackgroundTask.TYPE_FCM_UNREGISTER:
                    Result result = onGcmUnregister(task.getData(), task.getSession());
                    if(!result.equals(Result.retry()))
                        IRCCloudDatabase.getInstance().BackgroundTasksDao().delete(task);
                    return result;
            }
        } else {
            IRCCloudLog.Log(Log.ERROR, "IRCCloud", "Task not found");
        }

        return Result.failure();
    }

    private Result onGcmRegister(BackgroundTask task) {
        try {
            if(task.getSession() == null || task.getSession().length() == 0)
                return Result.failure();

            IRCCloudLog.Log(Log.INFO, "IRCCloud", "Registering for FCM");
            String token = task.getData();
            if(token != null && token.length() > 0) {
                JSONObject result = NetworkConnection.getInstance().registerGCM(token, task.getSession());
                if (result != null && result.has("success")) {
                    if(result.getBoolean("success")) {
                        IRCCloudLog.Log(Log.INFO, "IRCCloud", "Device successfully registered");
                        IRCCloudDatabase.getInstance().BackgroundTasksDao().delete(task);
                        return Result.success();
                    } else {
                        IRCCloudLog.Log(Log.ERROR, "IRCCloud", "Unable to register device: " + result.toString());
                        return Result.retry();
                    }
                } else {
                    IRCCloudLog.Log(Log.INFO, "IRCCloud", "Rescheduling FCM registration");
                    return Result.retry();
                }
            }
        } catch (Exception e) {
            NetworkConnection.printStackTraceToCrashlytics(e);
        }
        IRCCloudLog.Log(Log.ERROR, "IRCCloud", "FCM registration failed");
        IRCCloudDatabase.getInstance().BackgroundTasksDao().delete(task);

        return Result.failure();
    }

    private static Result onGcmUnregister(final String token, String session) {
        if(token != null) {
            try {
                IRCCloudLog.Log(Log.INFO, "IRCCloud", "Unregistering FCM");
                try {
                    FirebaseMessaging.getInstance().getToken().addOnSuccessListener(new OnSuccessListener<String>() {
                        @Override
                        public void onSuccess(@NonNull String s) {
                            if (token.equals(s)) {
                                IRCCloudLog.Log(Log.INFO, "IRCCloud", "Deleting old FCM token");
                                FirebaseMessaging.getInstance().deleteToken();
                            }
                        }
                    });
                } catch (Exception e) {
                    NetworkConnection.printStackTraceToCrashlytics(e);
                }
                JSONObject result = NetworkConnection.getInstance().unregisterGCM(token, session);
                if (result != null && result.has("success")) {
                    if (result.getBoolean("success") || result.getString("message").equals("auth")) {
                        IRCCloudLog.Log(Log.INFO, "IRCCloud", "Device successfully unregistered");
                        SharedPreferences.Editor e = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).edit();
                        e.remove(session);
                        e.apply();
                        return Result.success();
                    } else {
                        IRCCloudLog.Log(Log.ERROR, "IRCCloud", "Unable to unregister device: " + result.toString());
                        return Result.retry();
                    }
                } else {
                    IRCCloudLog.Log(Log.INFO, "IRCCloud", "Rescheduling FCM unregistration");
                    return Result.retry();
                }
            } catch (Exception e) {
                NetworkConnection.printStackTraceToCrashlytics(e);
            }
            IRCCloudLog.Log(Log.ERROR, "IRCCloud", "FCM unregistration failed");
        }

        return Result.failure();
    }
}
