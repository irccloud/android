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

package com.irccloud.android.activity;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.SeparatedListAdapter;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.collection.LogExportsList;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.model.LogExport;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.databinding.RowExportBinding;

import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class LogExportsActivity extends BaseActivity implements NetworkConnection.IRCEventHandler {
    private LogExportsAdapter inProgressAdapter = new LogExportsAdapter();
    private LogExportsAdapter downloadedAdapter = new LogExportsAdapter();
    private LogExportsAdapter availableAdapter = new LogExportsAdapter();

    private class LogExportsAdapter extends BaseAdapter {
        public List<LogExport> exports = new ArrayList<>();

        public void setExports(List<LogExport> exports) {
            this.exports = exports;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return exports.size();
        }

        @Override
        public Object getItem(int i) {
            return exports.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        private View.OnClickListener deleteClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final LogExport fileToDelete = (LogExport)getItem((Integer)view.getTag());
                AlertDialog.Builder builder = new AlertDialog.Builder(LogExportsActivity.this);
                builder.setTitle("Delete File");
                builder.setMessage("Are you sure you want to delete '" + fileToDelete.file_name + "'?");
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        fileToDelete.file().delete();
                        fileToDelete.download_id = 0;
                        fileToDelete.save();
                        new RefreshTask().execute((Void)null);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                AlertDialog d = builder.create();
                d.setOwnerActivity(LogExportsActivity.this);
                d.show();
            }
        };

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            RowExportBinding binding;

            if (view == null || view.getTag() == null) {
                binding = RowExportBinding.inflate(getLayoutInflater(), viewGroup, false);
            } else {
                binding = (RowExportBinding)view.getTag();
            }

            binding.setExport(exports.get(i));
            binding.delete.setOnClickListener(deleteClickListener);
            binding.delete.setColorFilter(ColorScheme.getInstance().colorControlNormal, PorterDuff.Mode.SRC_ATOP);
            binding.delete.setTag(i);
            binding.executePendingBindings();
            return binding.getRoot();
        }
    }

    private class FetchExportsTask extends AsyncTaskEx<Void, Void, JSONObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(findViewById(android.R.id.list).getVisibility() != View.VISIBLE)
                findViewById(R.id.progress).setVisibility(View.VISIBLE);
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                return NetworkConnection.getInstance().logExports();
            } catch (IOException e) {
                NetworkConnection.printStackTraceToCrashlytics(e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            if (jsonObject != null) {
                LogExportsList.getInstance().update(jsonObject);
                new RefreshTask().execute((Void)null);
            } else {
                findViewById(R.id.progress).setVisibility(View.GONE);
            }
        }
    }

    private class RefreshTask extends AsyncTaskEx<Void, Void, Void> {
        ArrayList<LogExport> inprogress = new ArrayList<>();
        ArrayList<LogExport> downloaded = new ArrayList<>();
        ArrayList<LogExport> available = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(findViewById(android.R.id.list).getVisibility() != View.VISIBLE)
                findViewById(R.id.progress).setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<LogExport> exports = LogExportsList.getInstance().getLogExports();

            Uri uriToDownload = null;

            if(getIntent() != null)
                uriToDownload = getIntent().getData();

            for(LogExport e : exports) {
                e.clearCache();
                if(e.finish_date == 0 && e.expiry_date == 0)
                    inprogress.add(e);
                else if(e.getExists())
                    downloaded.add(e);
                else
                    available.add(e);

                if(uriToDownload != null && uriToDownload.toString().equals(e.redirect_url)) {
                    uriToDownload = null;

                    if(e.download_id == 0)
                        e.download();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            inProgressAdapter.setExports(inprogress);
            downloadedAdapter.setExports(downloaded);
            availableAdapter.setExports(available);

            SeparatedListAdapter adapter = new SeparatedListAdapter(LogExportsActivity.this);

            if(inProgressAdapter.getCount() > 0)
                adapter.addSection("In Progress", inProgressAdapter);

            if(downloadedAdapter.getCount() > 0)
                adapter.addSection("Downloaded", downloadedAdapter);

            if(availableAdapter.getCount() > 0)
                adapter.addSection("Available", availableAdapter);

            if(adapter.getCount() > 0) {
                findViewById(android.R.id.list).setVisibility(View.VISIBLE);
            }
            findViewById(R.id.progress).setVisibility(View.GONE);
            ((ListView)findViewById(android.R.id.list)).setAdapter(adapter);
        }
    }

    BroadcastReceiver downloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            new RefreshTask().execute((Void)null);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ColorScheme.getDialogWhenLargeTheme(ColorScheme.getUserTheme()));
        onMultiWindowModeChanged(isMultiWindow());

        final int cid = getIntent().getIntExtra("cid", -1);
        final int bid = getIntent().getIntExtra("bid", -1);

        setContentView(R.layout.activity_export);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(android.support.v7.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        if(getSupportActionBar() != null) {
            getSupportActionBar().setElevation(0);
        }

        final Spinner exportType = findViewById(R.id.exportType);
        String types[] = {"This Channel", "This Network", "All Networks"};

        if(cid > 0) {
            Buffer b = BuffersList.getInstance().getBuffer(bid);
            if(b != null) {
                Server s = b.getServer();
                if (b.isChannel())
                    types[0] = "This Channel (" + b.getName() + ")";
                else if (b.isConversation())
                    types[0] = "This Conversation (" + b.getName() + ")";
                else
                    types[0] = "This Network Console (" + s.getHostname() + ")";

                types[1] = "This Network (" + ((s.getName() != null && s.getName().length() > 0) ? s.getName() : s.getHostname()) + ")";
            }
        } else {
            findViewById(R.id.textPrompt).setVisibility(View.GONE);
            findViewById(R.id.exportType).setVisibility(View.GONE);
            findViewById(R.id.export).setVisibility(View.GONE);
        }

        ArrayAdapter<String> typesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        exportType.setAdapter(typesAdapter);

        final Button exportButton = findViewById(R.id.export);
        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exportButton.setEnabled(false);
                exportButton.setAlpha(0.5f);
                NetworkConnection.IRCResultCallback callback = new NetworkConnection.IRCResultCallback() {
                    @Override
                    public void onIRCResult(final IRCCloudJSONObject result) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(result.getBoolean("success")) {
                                    LogExport e = LogExportsList.getInstance().create(result.getJsonNode("export"));
                                    e.save();
                                    new RefreshTask().execute((Void)null);
                                    Snackbar.make(findViewById(android.R.id.list), "Your log export is in progress.", Snackbar.LENGTH_SHORT).show();
                                } else if (result.getString("message").equals("rate_limited")) {
                                    Snackbar.make(findViewById(android.R.id.list), "You have requested too many exports, please try again later.", Snackbar.LENGTH_SHORT).show();
                                } else {
                                    Snackbar.make(findViewById(android.R.id.list), "Unable to export log: " + result.getString("message"), Snackbar.LENGTH_SHORT).show();
                                }
                                exportButton.setEnabled(true);
                                exportButton.setAlpha(1.0f);
                            }
                        });
                    }
                };

                int requestCid = -1, requestBid = -1;

                switch(exportType.getSelectedItemPosition()) {
                    case 0:
                        requestCid = cid;
                        requestBid = bid;
                        Answers.getInstance().logCustom(new CustomEvent("export_logs").putCustomAttribute("type", "buffer"));
                        break;
                    case 1:
                        requestCid = cid;
                        requestBid = -1;
                        Answers.getInstance().logCustom(new CustomEvent("export_logs").putCustomAttribute("type", "network"));
                        break;
                    case 2:
                        requestCid = -1;
                        requestBid = -1;
                        Answers.getInstance().logCustom(new CustomEvent("export_logs").putCustomAttribute("type", "all"));
                        break;
                }

                for(LogExport e : inProgressAdapter.exports) {
                    if(e.cid == requestCid && e.bid == requestBid) {
                        Snackbar.make(findViewById(android.R.id.list), "This log export is already in progress.", Snackbar.LENGTH_SHORT).show();
                        exportButton.setEnabled(true);
                        exportButton.setAlpha(1.0f);
                        return;
                    }
                }
                NetworkConnection.getInstance().export_log(requestCid, requestBid, TimeZone.getDefault().getID(), callback);

            }
        });

        ListView listView = findViewById(android.R.id.list);
        listView.setAdapter(new SeparatedListAdapter(this));

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                LogExport e = (LogExport)adapterView.getItemAtPosition(i);
                if(e.getExists()) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setData(FileProvider.getUriForFile(LogExportsActivity.this,getPackageName() + ".fileprovider", e.file()));
                    intent.setType("application/zip");
                    intent.putExtra(Intent.EXTRA_STREAM, intent.getData());
                    intent.putExtra(ShareCompat.EXTRA_CALLING_PACKAGE, getPackageName());
                    intent.putExtra(ShareCompat.EXTRA_CALLING_ACTIVITY, getPackageManager().getLaunchIntentForPackage(getPackageName()).getComponent());
                    if(Build.VERSION.SDK_INT >= 16)
                        intent.setClipData(ClipData.newRawUri(e.file_name, intent.getData()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Open Log Archive"));
                } else {
                    if(e.download_id == 0 && e.redirect_url != null)
                        e.download();
                }
            }
        });

        NetworkConnection.getInstance().addHandler(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        registerReceiver(downloadComplete, intentFilter);
    }

    private TimerTask updateTimerTask;
    @Override
    public void onResume() {
        super.onResume();
        new RefreshTask().execute((Void)null);
        new FetchExportsTask().execute((Void)null);

        updateTimerTask = new TimerTask() {
            @Override
            public void run() {
                if(inProgressAdapter != null) {
                    for(LogExport e : inProgressAdapter.exports) {
                        e.clearCache();
                    }
                }
                if(downloadedAdapter != null) {
                    for(LogExport e : downloadedAdapter.exports) {
                        e.clearCache();
                    }
                }
                if(availableAdapter != null) {
                    for(LogExport e : availableAdapter.exports) {
                        e.clearCache();
                    }
                }
            }
        };

        new Timer().scheduleAtFixedRate(updateTimerTask, 30000, 30000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateTimerTask.cancel();
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        if(getWindowManager().getDefaultDisplay().getWidth() > TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 800, getResources().getDisplayMetrics()) && !isMultiWindow()) {
            params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 800, getResources().getDisplayMetrics());
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 800, getResources().getDisplayMetrics());
        } else {
            params.width = -1;
            params.height = -1;
        }
        getWindow().setAttributes(params);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NetworkConnection.getInstance().removeHandler(this);
        unregisterReceiver(downloadComplete);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onIRCEvent(int what, Object obj) {
        super.onIRCEvent(what, obj);

        switch (what) {
            case NetworkConnection.EVENT_LOGEXPORTFINISHED:
            case NetworkConnection.EVENT_BACKLOG_END:
                new RefreshTask().execute((Void)null);
                break;
        }
    }
}