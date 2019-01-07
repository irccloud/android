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

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.model.BackgroundTask;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

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
            Crashlytics.log(Log.INFO, "IRCCloud", "Removing old FCM registration task: " + t.getTag());
            try {
                WorkManager.getInstance().cancelUniqueWork(t.getTag());
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

            Crashlytics.log(Log.INFO, "IRCCloud", "Scheduled FCM registration task: " + task.getTag());
            try {
                OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(BackgroundTaskWorker.class)
                        .setInputData(new Data.Builder().putString("tag", task.getTag()).build())
                        .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                        .build();
                WorkManager.getInstance().enqueueUniqueWork(task.getTag(), ExistingWorkPolicy.REPLACE, request);
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
                    WorkManager.getInstance().cancelUniqueWork(t.getTag());
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
                WorkManager.getInstance().enqueueUniqueWork(task.getTag(), ExistingWorkPolicy.REPLACE, request);
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
                WorkManager.getInstance().cancelUniqueWork(t.getTag());
            } catch (Exception e) {
            }
            IRCCloudDatabase.getInstance().BackgroundTasksDao().delete(t);
        }

        if(Looper.myLooper() == Looper.getMainLooper()) {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... voids) {
                    try {
                        FirebaseInstanceId.getInstance().deleteInstanceId();
                    } catch (IOException e) {
                        NetworkConnection.printStackTraceToCrashlytics(e);
                    }
                    if(!onGcmUnregister(token, session).equals(Result.success()))
                        scheduleUnregister(token, session);
                    return null;
                }
            }.execute((Void) null);
        } else {
            try {
                FirebaseInstanceId.getInstance().deleteInstanceId();
            } catch (IOException e) {
                NetworkConnection.printStackTraceToCrashlytics(e);
            }
            if(!onGcmUnregister(token, session).equals(Result.success()))
                scheduleUnregister(token, session);
        }
    }

    @Override
    public ListenableWorker.Result doWork() {
        String tag = getInputData().getString("tag");
        Crashlytics.log(Log.INFO, "IRCCloud", "Executing background task with tag: " + tag);
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
            Crashlytics.log(Log.ERROR, "IRCCloud", "Task not found");
        }

        return Result.failure();
    }

    private Result onGcmRegister(BackgroundTask task) {
        try {
            if(task.getSession() == null || task.getSession().length() == 0)
                return Result.failure();

            Crashlytics.log(Log.INFO, "IRCCloud", "Registering for FCM");
            String token = task.getData();
            if(token != null && token.length() > 0) {
                JSONObject result = NetworkConnection.getInstance().registerGCM(token, task.getSession());
                if (result != null && result.has("success")) {
                    if(result.getBoolean("success")) {
                        Crashlytics.log(Log.INFO, "IRCCloud", "Device successfully registered");
                        IRCCloudDatabase.getInstance().BackgroundTasksDao().delete(task);
                        return Result.success();
                    } else {
                        Crashlytics.log(Log.ERROR, "IRCCloud", "Unable to register device: " + result.toString());
                        return Result.retry();
                    }
                } else {
                    Crashlytics.log(Log.INFO, "IRCCloud", "Rescheduling FCM registration");
                    return Result.retry();
                }
            }
        } catch (Exception e) {
            NetworkConnection.printStackTraceToCrashlytics(e);
        }
        Crashlytics.log(Log.ERROR, "IRCCloud", "FCM registration failed");
        IRCCloudDatabase.getInstance().BackgroundTasksDao().delete(task);

        return Result.failure();
    }

    private static Result onGcmUnregister(final String token, String session) {
        if(token != null) {
            try {
                Crashlytics.log(Log.INFO, "IRCCloud", "Unregistering FCM");
                try {
                    FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                        @Override
                        public void onSuccess(InstanceIdResult instanceIdResult) {
                            if (token.equals(instanceIdResult.getToken())) {
                                Crashlytics.log(Log.INFO, "IRCCloud", "Deleting old FCM token");
                                try {
                                    FirebaseInstanceId.getInstance().deleteInstanceId();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    NetworkConnection.printStackTraceToCrashlytics(e);
                }
                JSONObject result = NetworkConnection.getInstance().unregisterGCM(token, session);
                if (result != null && result.has("success")) {
                    if (result.getBoolean("success") || result.getString("message").equals("auth")) {
                        Crashlytics.log(Log.INFO, "IRCCloud", "Device successfully unregistered");
                        SharedPreferences.Editor e = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).edit();
                        e.remove(session);
                        e.commit();
                        return Result.success();
                    } else {
                        Crashlytics.log(Log.ERROR, "IRCCloud", "Unable to unregister device: " + result.toString());
                        return Result.retry();
                    }
                } else {
                    Crashlytics.log(Log.INFO, "IRCCloud", "Rescheduling FCM unregistration");
                    return Result.retry();
                }
            } catch (Exception e) {
                NetworkConnection.printStackTraceToCrashlytics(e);
            }
            Crashlytics.log(Log.ERROR, "IRCCloud", "FCM unregistration failed");
        }

        return Result.failure();
    }
}
