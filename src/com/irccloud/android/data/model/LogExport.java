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

package com.irccloud.android.data.model;

import android.app.DownloadManager;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.databinding.Bindable;
import android.net.Uri;
import android.text.format.DateUtils;

import com.irccloud.android.BR;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.collection.ServersList;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;

import java.io.File;

@Table(database = IRCCloudDatabase.class)
public class LogExport extends ObservableBaseModel {
    @Column
    @PrimaryKey
    public int id;

    @Column
    public int cid;

    @Column
    public int bid;

    @Column
    public String file_name;

    @Column
    public String redirect_url;

    @Column
    public long start_date;

    @Column
    public long finish_date;

    @Column
    public long expiry_date;

    @Column
    public long download_id;

    @Column
    public String name;

    @Bindable
    public String getName() {
        if(name == null) {
            Buffer b = BuffersList.getInstance().getBuffer(bid);
            Server s = ServersList.getInstance().getServer(cid);

            String serverName = (s != null) ? (s.getName() != null ? s.getName() : s.getHostname()) : "Unknown Network (" + cid + ")";
            String bufferName = (b != null) ? b.getName() : "Unknown Log (" + bid + ")";

            if(bid > 0) {
                if(b != null)
                    name = serverName + ": " + bufferName;
                else
                    return serverName + ": " + bufferName;
            } else if(cid > 0) {
                if(s != null)
                    name = serverName;
                else
                    return serverName;
            } else {
                name = "All Networks";
            }
            if(name != null)
                save();
        }
        return name;
    }

    private String startTime;
    @Bindable
    public String getStartTime() {
        if(startTime == null)
            startTime = (finish_date == 0 ? "Started " : "Exported ") + DateUtils.getRelativeTimeSpanString(start_date * 1000L, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString().toLowerCase();
        return startTime;
    }

    private String expiryTime;
    @Bindable
    public String getExpiryTime() {
        if(expiryTime == null)
            expiryTime = "Expires " + DateUtils.getRelativeTimeSpanString(expiry_date * 1000L, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString().toLowerCase();
        return expiryTime;
    }

    private boolean downloadComplete;

    @Bindable
    public int getDownloadProgress() {
        if(download_id > 0 && !downloadComplete) {
            DownloadManager d = (DownloadManager) IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);

            final Cursor downloadCursor = d.query(new DownloadManager.Query().setFilterById(download_id));
            if(downloadCursor != null)
                downloadCursor.registerContentObserver(new ContentObserver(null) {
                    @Override
                    public void onChange(boolean selfChange) {
                        super.onChange(selfChange);
                        notifyPropertyChanged(BR.downloadProgress);
                        notifyPropertyChanged(BR.isDownloading);
                        downloadCursor.unregisterContentObserver(this);
                    }
                });
            if(downloadCursor != null && downloadCursor.moveToFirst()) {
                int status = downloadCursor.getInt(downloadCursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status != DownloadManager.STATUS_FAILED && status != DownloadManager.STATUS_SUCCESSFUL) {
                    int downloaded = downloadCursor.getInt(downloadCursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int total = downloadCursor.getInt(downloadCursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    return (int)(downloaded * 100.0f / total);
                } else  {
                    downloadComplete = true;
                    if (status == DownloadManager.STATUS_FAILED) {
                        download_id = 0;
                        save();
                    }
                }
            } else {
                download_id = 0;
                save();
            }
        }
        return -1;
    }

    @Bindable
    public boolean getIsDownloading() {
        return getDownloadProgress() >= 0;
    }

    @Bindable
    public boolean getIsPreparing() {
        return finish_date == 0;
    }

    private String filesize;
    @Bindable
    public String getFileSize() {
        if(filesize == null) {
            if (getExists()) {
                long total = file().length();
                if (total < 1024) {
                    filesize = total + " B";
                } else {
                    int exp = (int) (Math.log(total) / Math.log(1024));
                    filesize = String.format("%.1f ", total / Math.pow(1024, exp)) + ("KMGTPE".charAt(exp - 1)) + "B";
                }
            }
        }
        return filesize;
    }

    public void download() {
        if(redirect_url != null) {
            path().mkdirs();
            DownloadManager d = (DownloadManager) IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(redirect_url));
            r.addRequestHeader("Cookie", "session=" + NetworkConnection.getInstance().session);
            r.setDestinationUri(Uri.fromFile(file()));
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            r.setVisibleInDownloadsUi(true);
            r.setMimeType("application/zip");
            download_id = d.enqueue(r);
            notifyPropertyChanged(BR.downloadProgress);
            notifyPropertyChanged(BR.isDownloading);
            save();
        }
    }

    public File path() {
        return new File(IRCCloudApplication.getInstance().getApplicationContext().getExternalFilesDir(null), "export");
    }

    private File file;
    public File file() {
        if(file == null && file_name != null)
            file = new File(path(), file_name);
        return file;
    }

    @Bindable
    public boolean getExists() {
        return file() != null && file().exists();
    }

    public String toString() {
        return "{id: " + id + ", cid: " + cid + ", bid: " + bid + ", file_name: " + file_name + ", name: " + getName() + "}";
    }

    public void clearCache() {
        expiryTime = null;
        getExpiryTime();
        notifyPropertyChanged(BR.expiryTime);
        startTime = null;
        getStartTime();
        notifyPropertyChanged(BR.startTime);
        filesize = null;
        getFileSize();
        notifyPropertyChanged(BR.fileSize);
    }
}