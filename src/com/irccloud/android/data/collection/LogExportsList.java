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

package com.irccloud.android.data.collection;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.model.LogExport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LogExportsList {
    @Dao
    public interface LogExportsDao {
        @Query("SELECT * FROM LogExport ORDER BY start_date DESC")
        List<LogExport> getLogExports();

        @Query("SELECT * FROM LogExport WHERE id = :id LIMIT 1")
        LogExport getLogExport(int id);

        @Query("SELECT * FROM LogExport WHERE download_id = :download_id LIMIT 1")
        LogExport getDownload(long download_id);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(LogExport logExport);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insertAll(List<LogExport> logExports);

        @Update
        void update(LogExport logExport);

        @Delete
        void delete(LogExport logExport);

        @Query("DELETE FROM LogExport")
        void clear();
    }

    private static LogExportsList instance = null;
    private final Object dbLock = new Object();

    public static LogExportsList getInstance() {
        if (instance == null)
            instance = new LogExportsList();
        return instance;
    }

    public void clear() {
        IRCCloudDatabase.getInstance().LogExportsDao().clear();
    }

    public List<LogExport> getLogExports() {
        return IRCCloudDatabase.getInstance().LogExportsDao().getLogExports();
    }

    public LogExport get(int id) {
        return IRCCloudDatabase.getInstance().LogExportsDao().getLogExport(id);
    }

    public LogExport getDownload(long downloadid) {
        return IRCCloudDatabase.getInstance().LogExportsDao().getDownload(downloadid);
    }

    public void update(JSONObject logs) {
        HashMap<Integer, LogExport> dbCache = new HashMap<>();
        final ArrayList<LogExport> add = new ArrayList<>();

        synchronized (dbLock) {
            for(LogExport e : getLogExports()) {
                dbCache.put(e.getId(), e);
            }
        }

        try {
            add.addAll(createAll(dbCache, logs.getJSONArray("inprogress")));
            add.addAll(createAll(dbCache, logs.getJSONArray("available")));

            for(int i = 0; i < logs.getJSONArray("expired").length(); i++) {
                JSONObject log = logs.getJSONArray("expired").getJSONObject(i);
                if (dbCache.containsKey(log.getInt("id"))) {
                    LogExport e = dbCache.get(log.getInt("id"));
                    if (!e.file().exists())
                        IRCCloudDatabase.getInstance().LogExportsDao().delete(e);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(add.size() > 0)
            IRCCloudDatabase.getInstance().runInTransaction(new Runnable() {
                @Override
                public void run() {
                    IRCCloudDatabase.getInstance().LogExportsDao().insertAll(add);
                }
            });
    }

    public LogExport create(JSONObject log) throws JSONException {
        LogExport e = new LogExport();
        e.setId(log.getInt("id"));
        if(log.isNull("cid"))
            e.setCid(-1);
        else
            e.setCid(log.getInt("cid"));
        if(log.isNull("bid"))
            e.setBid(-1);
        else
            e.setBid(log.getInt("bid"));
        e.setFile_name(log.getString("file_name"));
        e.setRedirect_url(log.getString("redirect_url"));
        if(!log.isNull("startdate"))
            e.setStart_date(log.getLong("startdate"));
        if(!log.isNull("finishdate"))
            e.setFinish_date(log.getLong("finishdate"));
        if(!log.isNull("expirydate"))
            e.setExpiry_date(log.getLong("expirydate"));

        return e;
    }

    public LogExport create(JsonNode log) {
        LogExport e = new LogExport();
        e.setId(log.get("id").asInt());
        if(log.get("cid") != null && log.get("cid").isNull())
            e.setCid(-1);
        else
            e.setCid(log.get("cid").asInt());
        if(log.get("bid") != null && log.get("bid").isNull())
            e.setBid(-1);
        else
            e.setBid(log.get("bid").asInt());
        if(log.get("file_name") != null && !log.get("file_name").isNull())
            e.setFile_name(log.get("file_name").asText());
        if(log.get("redirect_url") != null && !log.get("redirect_url").isNull())
            e.setRedirect_url(log.get("redirect_url").asText());
        if(log.get("startdate") != null && !log.get("startdate").isNull())
            e.setStart_date(log.get("startdate").asLong());
        if(log.get("finishdate") != null && !log.get("finishdate").isNull())
            e.setFinish_date(log.get("finishdate").asLong());
        if(log.get("expirydate") != null && !log.get("expirydate").isNull())
            e.setExpiry_date(log.get("expirydate").asLong());

        return e;
    }

    private ArrayList<LogExport> createAll(HashMap<Integer, LogExport> dbCache, JSONArray logs) {
        ArrayList<LogExport> add = new ArrayList<>();

        for(int i = 0; i < logs.length(); i++) {
            try {
                JSONObject log = logs.getJSONObject(i);
                if (!dbCache.containsKey(log.getInt("id"))) {
                    add.add(create(log));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return add;
    }
}
