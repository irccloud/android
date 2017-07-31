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

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.model.LogExport;
import com.irccloud.android.data.model.LogExport_Table;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.database.transaction.FastStoreModelTransaction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LogExportsList {
    private static LogExportsList instance = null;
    private final Object dbLock = new Object();

    public static LogExportsList getInstance() {
        if (instance == null)
            instance = new LogExportsList();
        return instance;
    }

    public void clear() {
        synchronized (dbLock) {
            Delete.table(LogExport.class);
        }
    }

    public List<LogExport> getLogExports() {
        synchronized (dbLock) {
            return new Select().from(LogExport.class).where().orderBy(LogExport_Table.start_date, false).queryList();
        }
    }

    public LogExport get(int id) {
        synchronized (dbLock) {
            return new Select().from(LogExport.class).where(LogExport_Table.id.is(id)).querySingle();
        }
    }

    public LogExport getDownload(long downloadid) {
        synchronized (dbLock) {
            return new Select().from(LogExport.class).where(LogExport_Table.download_id.is(downloadid)).querySingle();
        }
    }

    public void update(JSONObject logs) {
        HashMap<Integer, LogExport> dbCache = new HashMap<>();
        ArrayList<LogExport> add = new ArrayList<>();

        synchronized (dbLock) {
            for(LogExport e : getLogExports()) {
                dbCache.put(e.id, e);
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
                        e.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(add.size() > 0)
            FastStoreModelTransaction
                    .insertBuilder(FlowManager.getModelAdapter(LogExport.class))
                    .addAll(add)
                    .build().execute(FlowManager.getWritableDatabase(IRCCloudDatabase.class));
    }

    public LogExport create(JSONObject log) throws JSONException {
        LogExport e = new LogExport();
        e.id = log.getInt("id");
        if(log.isNull("cid"))
            e.cid = -1;
        else
            e.cid = log.getInt("cid");
        if(log.isNull("bid"))
            e.bid = -1;
        else
            e.bid = log.getInt("bid");
        e.file_name = log.getString("file_name");
        e.redirect_url = log.getString("redirect_url");
        if(!log.isNull("startdate"))
            e.start_date = log.getLong("startdate");
        if(!log.isNull("finishdate"))
            e.finish_date = log.getLong("finishdate");
        if(!log.isNull("expirydate"))
            e.expiry_date = log.getLong("expirydate");

        return e;
    }

    public LogExport create(JsonNode log) {
        LogExport e = new LogExport();
        e.id = log.get("id").asInt();
        if(log.get("cid") != null && log.get("cid").isNull())
            e.cid = -1;
        else
            e.cid = log.get("cid").asInt();
        if(log.get("bid") != null && log.get("bid").isNull())
            e.bid = -1;
        else
            e.bid = log.get("bid").asInt();
        if(log.get("file_name") != null && !log.get("file_name").isNull())
            e.file_name = log.get("file_name").asText();
        if(log.get("redirect_url") != null && !log.get("redirect_url").isNull())
            e.redirect_url = log.get("redirect_url").asText();
        if(log.get("startdate") != null && !log.get("startdate").isNull())
            e.start_date = log.get("startdate").asLong();
        if(log.get("finishdate") != null && !log.get("finishdate").isNull())
            e.finish_date = log.get("finishdate").asLong();
        if(log.get("expirydate") != null && !log.get("expirydate").isNull())
            e.expiry_date = log.get("expirydate").asLong();

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
