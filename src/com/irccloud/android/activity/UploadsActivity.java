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

package com.irccloud.android.activity;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class UploadsActivity extends BaseActivity {
    private int page = 0;
    private int reqid = -1;
    private FilesAdapter adapter = new FilesAdapter();
    private boolean canLoadMore = true;
    private View footer;
    private File fileToDelete = null;

    private String to;
    private int cid = -1;
    private String msg;

    private final BlockingQueue<Runnable> mDecodeWorkQueue = new LinkedBlockingQueue<>();
    private static final int KEEP_ALIVE_TIME = 10;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    private final ThreadPoolExecutor mDownloadThreadPool = new ThreadPoolExecutor(
            4,       // Initial pool size
            8,       // Max pool size
            KEEP_ALIVE_TIME,
            KEEP_ALIVE_TIME_UNIT,
            mDecodeWorkQueue);

    private static class File implements Serializable {
        private static final long serialVersionUID = 0L;

        String id;
        String name;
        String url;
        String url_template;
        String mime_type;
        String extension;
        int size;
        Date date;
        String date_formatted;
        String metadata;
        Bitmap image;
        boolean image_failed;
    }

    private class FilesAdapter extends BaseAdapter {
        private class ViewHolder {
            TextView date;
            ImageView image;
            TextView extension;
            TextView name;
            TextView metadata;
            ProgressBar progress;
            ImageButton delete;
        }

        private ArrayList<File> files = new ArrayList<>();
        private DateFormat dateFormat = DateFormat.getDateTimeInstance();

        public void clear() {
            for(File f : files) {
                if(f.image != null && !f.image.isRecycled())
                    f.image.recycle();
                f.image = null;
            }
            files.clear();
            notifyDataSetInvalidated();
        }

        public void saveInstanceState(Bundle state) {
            state.putSerializable("adapter", files.toArray(new File[files.size()]));
        }

        public void addFile(String id, String name, String url, String url_template, String mime_type, String extension, int size, Date date) {
            File f = new File();
            f.id = id;
            f.name = name;
            f.url = url;
            f.url_template = url_template;
            f.mime_type = mime_type;
            f.extension = extension;
            f.size = size;
            f.date = date;
            if (f.extension != null && f.extension.length() > 1 && f.extension.startsWith("."))
                f.extension = f.extension.substring(1).toUpperCase();

            addFile(f);
        }

        public void addFile(final File f) {
            files.add(f);

            try {
                if(f.image == null && f.mime_type.startsWith("image/")) {
                    mDownloadThreadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                            try {
                                f.image = NetworkConnection.getInstance().fetchImage(new URL(f.url_template.replace(":modifiers", "w320")), false);
                                if (f.image == null)
                                    f.image_failed = true;
                            } catch (Exception e) {
                                f.image_failed = true;
                                e.printStackTrace();
                            }
                            if (f.image_failed && (f.extension == null || f.extension.length() == 0))
                                f.extension = "IMAGE";
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    notifyDataSetChanged();
                                }
                            });
                        }
                    });
                }
            } catch (Exception e) {
                //Thread pool has shut down
            }
        }

        @Override
        public int getCount() {
            return files.size();
        }

        @Override
        public Object getItem(int i) {
            return files.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        private View.OnClickListener deleteClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileToDelete = (File)getItem((Integer)view.getTag());
                AlertDialog.Builder builder = new AlertDialog.Builder(UploadsActivity.this);
                builder.setTitle("Delete File");
                if(fileToDelete.name != null && fileToDelete.name.length() > 0) {
                    builder.setMessage("Are you sure you want to delete '" + fileToDelete.name + "'?");
                } else {
                    builder.setMessage("Are you sure you want to delete this file?");
                }
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        reqid = NetworkConnection.getInstance().deleteFile(fileToDelete.id);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        fileToDelete = null;
                    }
                });
                AlertDialog d = builder.create();
                d.setOwnerActivity(UploadsActivity.this);
                d.show();
            }
        };

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View row = view;
            ViewHolder holder;

            if (row == null) {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.row_file, viewGroup, false);

                holder = new ViewHolder();
                holder.date = (TextView) row.findViewById(R.id.date);
                holder.image = (ImageView) row.findViewById(R.id.image);
                holder.extension = (TextView) row.findViewById(R.id.extension);
                holder.name = (TextView) row.findViewById(R.id.name);
                holder.metadata = (TextView) row.findViewById(R.id.metadata);
                holder.progress = (ProgressBar) row.findViewById(R.id.progress);
                holder.delete = (ImageButton) row.findViewById(R.id.delete);

                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }

            try {
                File f = files.get(i);
                if (f.date_formatted == null)
                    f.date_formatted = dateFormat.format(f.date);
                holder.date.setText(f.date_formatted);
                if (f.extension != null && f.extension.length() > 1)
                    holder.extension.setText(f.extension);
                else
                    holder.extension.setText("???");
                holder.name.setText(f.name);
                if (f.metadata == null) {
                    if (f.size < 1024) {
                        f.metadata = f.size + " B";
                    } else {
                        int exp = (int) (Math.log(f.size) / Math.log(1024));
                        f.metadata = String.format("%.1f ", f.size / Math.pow(1024, exp)) + ("KMGTPE".charAt(exp - 1)) + "B";
                    }
                    f.metadata += " • " + f.mime_type;
                }
                holder.metadata.setText(f.metadata);
                if (!f.image_failed && f.mime_type.startsWith("image/")) {
                    if (f.image != null) {
                        holder.progress.setVisibility(View.GONE);
                        holder.extension.setVisibility(View.GONE);
                        holder.image.setVisibility(View.VISIBLE);
                        holder.image.setImageBitmap(f.image);
                    } else {
                        holder.extension.setVisibility(View.GONE);
                        holder.image.setVisibility(View.GONE);
                        holder.progress.setVisibility(View.VISIBLE);
                    }
                } else {
                    holder.extension.setVisibility(View.VISIBLE);
                    holder.image.setVisibility(View.GONE);
                    holder.progress.setVisibility(View.GONE);
                }
                holder.delete.setOnClickListener(deleteClickListener);
                holder.delete.setTag(i);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return row;
        }
    }

    private class FetchFilesTask extends AsyncTaskEx<Void, Void, JSONObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            canLoadMore = false;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                return NetworkConnection.getInstance().files(++page);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            if (jsonObject != null) {
                try {
                    if (jsonObject.getBoolean("success")) {
                        JSONArray files = jsonObject.getJSONArray("files");
                        Log.e("IRCCloud", "Got " + files.length() + " files for page " + page);
                        for (int i = 0; i < files.length(); i++) {
                            JSONObject file = files.getJSONObject(i);
                            adapter.addFile(file.getString("id"), file.getString("name"), file.getString("url"), file.getString("url_template"), file.getString("mime_type"), file.getString("extension"), file.getInt("size"), new Date(file.getLong("date") * 1000L));
                        }
                        adapter.notifyDataSetChanged();
                        canLoadMore = files.length() > 0 && adapter.getCount() < jsonObject.getInt("total");
                        if(!canLoadMore)
                            footer.findViewById(R.id.progress).setVisibility(View.GONE);
                    } else {
                        page--;
                        Log.e("IRCCloud", "Failed: " + jsonObject.toString());
                        if(jsonObject.has("message") && jsonObject.getString("message").equals("server_error")) {
                            canLoadMore = true;
                            new FetchFilesTask().execute((Void)null);
                        } else {
                            canLoadMore = false;
                        }
                    }
                } catch (JSONException e) {
                    page--;
                    e.printStackTrace();
                }
            } else {
                canLoadMore = true;
                new FetchFilesTask().execute((Void) null);
            }
            checkEmpty();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            Bitmap cloud = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.app_name), cloud, 0xFFF2F7FC));
            cloud.recycle();
        }

        if(Build.VERSION.SDK_INT >= 14) {
            try {
                java.io.File httpCacheDir = new java.io.File(getCacheDir(), "http");
                long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
                HttpResponseCache.install(httpCacheDir, httpCacheSize);
            } catch (IOException e) {
                Log.i("IRCCloud", "HTTP response cache installation failed:" + e);
            }
        }
        setContentView(R.layout.ignorelist);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar));
        getSupportActionBar().setElevation(0);

        if(savedInstanceState != null && savedInstanceState.containsKey("cid")) {
            cid = savedInstanceState.getInt("cid");
            to = savedInstanceState.getString("to");
            msg = savedInstanceState.getString("msg");
            page = savedInstanceState.getInt("page");
            File[] files = (File[])savedInstanceState.getSerializable("adapter");
            for(File f : files) {
                adapter.addFile(f);
            }
            adapter.notifyDataSetChanged();
        }

        footer = getLayoutInflater().inflate(R.layout.messageview_header, null);
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(adapter);
        listView.addFooterView(footer);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (canLoadMore && firstVisibleItem + visibleItemCount > totalItemCount - 4) {
                    canLoadMore = false;
                    new FetchFilesTask().execute((Void) null);
                }
            }
        });
        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final File f = (File)adapter.getItem(i);

                AlertDialog.Builder builder = new AlertDialog.Builder(UploadsActivity.this);
                builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
                final View v = getLayoutInflater().inflate(R.layout.dialog_upload, null);
                final EditText messageinput = (EditText) v.findViewById(R.id.message);
                messageinput.setText(msg);
                final ImageView thumbnail = (ImageView) v.findViewById(R.id.thumbnail);

                v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (messageinput.hasFocus()) {
                            v.post(new Runnable() {
                                @Override
                                public void run() {
                                    v.scrollTo(0, v.getBottom());
                                }
                            });
                        }
                    }
                });

                if(f.mime_type.startsWith("image/")) {
                    try {
                        thumbnail.setImageBitmap(f.image);
                        thumbnail.setVisibility(View.VISIBLE);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    thumbnail.setVisibility(View.GONE);
                }

                ((TextView)v.findViewById(R.id.filesize)).setText(f.metadata);
                v.findViewById(R.id.filename).setVisibility(View.GONE);
                v.findViewById(R.id.filename_heading).setVisibility(View.GONE);

                builder.setTitle("Send A File To " + to);
                builder.setView(v);
                builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String message = messageinput.getText().toString();
                        if(message.length() > 0)
                            message += " ";
                        message += f.url;

                        dialog.dismiss();
                        if(getParent() == null) {
                            setResult(Activity.RESULT_OK);
                        } else {
                            getParent().setResult(Activity.RESULT_OK);
                        }
                        finish();

                        NetworkConnection.getInstance().say(cid, to, message);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                AlertDialog d = builder.create();
                d.setOwnerActivity(UploadsActivity.this);
                d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                d.show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        NetworkConnection.getInstance().addHandler(this);

        if(cid == -1) {
            cid = getIntent().getIntExtra("cid", -1);
            to = getIntent().getStringExtra("to");
            msg = getIntent().getStringExtra("msg");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        NetworkConnection.getInstance().removeHandler(this);
        mDownloadThreadPool.shutdownNow();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.clear();
        if(Build.VERSION.SDK_INT >= 14) {
            HttpResponseCache cache = HttpResponseCache.getInstalled();
            if (cache != null) {
                cache.flush();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("cid", cid);
        outState.putString("to", to);
        outState.putString("msg", msg);
        outState.putInt("page", page);
        adapter.saveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    public void onIRCEvent(int what, Object o) {
        IRCCloudJSONObject obj;
        switch (what) {
            case NetworkConnection.EVENT_SUCCESS:
                obj = (IRCCloudJSONObject) o;
                if (obj.getInt("_reqid") == reqid) {
                    Log.d("IRCCloud", "File deleted successfully");
                    reqid = -1;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.files.remove(fileToDelete);
                            adapter.notifyDataSetChanged();
                            checkEmpty();
                            fileToDelete = null;
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_FAILURE_MSG:
                obj = (IRCCloudJSONObject) o;
                if (obj.getInt("_reqid") == reqid) {
                    Crashlytics.log(Log.ERROR, "IRCCloud", "Delete failed: " + obj.toString());
                    reqid = -1;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(UploadsActivity.this);
                            builder.setTitle("Error");
                            builder.setMessage("Unable to delete file.  Please try again shortly.");
                            builder.setNeutralButton("Close", null);
                            builder.show();
                        }
                    });
                }
                break;
            default:
                break;
        }
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

    private void checkEmpty() {
        if(adapter.getCount() == 0 && !canLoadMore) {
            findViewById(android.R.id.list).setVisibility(View.GONE);
            TextView empty = (TextView)findViewById(android.R.id.empty);
            empty.setVisibility(View.VISIBLE);
            empty.setText("You haven't uploaded any files to IRCCloud yet.");
        } else {
            findViewById(android.R.id.list).setVisibility(View.VISIBLE);
            findViewById(android.R.id.empty).setVisibility(View.GONE);
        }
    }
}