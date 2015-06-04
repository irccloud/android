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
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.format.DateUtils;
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
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.github.fge.uritemplate.URITemplate;
import com.github.fge.uritemplate.URITemplateException;
import com.github.fge.uritemplate.vars.VariableMap;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class PastebinsActivity extends BaseActivity {
    private int page = 0;
    private int reqid = -1;
    private PastebinsAdapter adapter = new PastebinsAdapter();
    private boolean canLoadMore = true;
    private View footer;
    private Pastebin pasteToDelete;
    private URITemplate uri_template = null;

    private static class Pastebin implements Serializable {
        private static final long serialVersionUID = 0L;

        String id;
        String name;
        String url;
        int lines;
        Date date;
        String date_formatted;
        String body;
        boolean own_paste;
    }

    private class PastebinsAdapter extends BaseAdapter {
        private class ViewHolder {
            TextView date;
            TextView body;
            ImageButton delete;
        }

        private ArrayList<Pastebin> pastebins = new ArrayList<>();

        public void clear() {
            pastebins.clear();
            notifyDataSetInvalidated();
        }

        public void saveInstanceState(Bundle state) {
            state.putSerializable("adapter", pastebins.toArray(new Pastebin[pastebins.size()]));
        }

        public void addPastebin(String id, String name, int lines, Date date, String body, boolean own_paste) {
            Pastebin p = new Pastebin();
            p.id = id;
            p.name = name;
            p.lines = lines;
            p.date = date;
            p.body = body;
            p.own_paste = own_paste;
            try {
                p.url = uri_template.toString(VariableMap.newBuilder().addScalarValue("id", p.id).addScalarValue("name", p.name).freeze());
            } catch (URITemplateException e) {
                e.printStackTrace();
            }

            addPastebin(p);
        }

        public void addPastebin(Pastebin p) {
            pastebins.add(p);
        }

        @Override
        public int getCount() {
            return pastebins.size();
        }

        @Override
        public Object getItem(int i) {
            return pastebins.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        private View.OnClickListener deleteClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pasteToDelete = (Pastebin)getItem((Integer)view.getTag());
                AlertDialog.Builder builder = new AlertDialog.Builder(PastebinsActivity.this);
                builder.setTitle("Delete Pastebin");
                if(pasteToDelete.name != null && pasteToDelete.name.length() > 0) {
                    builder.setMessage("Are you sure you want to delete '" + pasteToDelete.name + "'?");
                } else {
                    builder.setMessage("Are you sure you want to delete this pastebin?");
                }
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        reqid = NetworkConnection.getInstance().delete_paste(pasteToDelete.id);
                        pastebins.remove(pasteToDelete);
                        notifyDataSetChanged();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        pasteToDelete = null;
                    }
                });
                AlertDialog d = builder.create();
                d.setOwnerActivity(PastebinsActivity.this);
                d.show();
            }
        };

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View row = view;
            ViewHolder holder;

            if (row == null) {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.row_pastebin, viewGroup, false);

                holder = new ViewHolder();
                holder.date = (TextView) row.findViewById(R.id.date);
                holder.body = (TextView) row.findViewById(R.id.body);
                holder.delete = (ImageButton) row.findViewById(R.id.delete);

                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }

            try {
                Pastebin p = pastebins.get(i);
                if (p.date_formatted == null) {
                    p.date_formatted = "";
                    if(p.name != null && p.name.length() > 0)
                        p.date_formatted += p.name + " • ";
                    p.date_formatted += p.lines + " line";
                    if(p.lines != 1)
                        p.date_formatted += "s";
                    p.date_formatted += " • " + DateUtils.getRelativeTimeSpanString(p.date.getTime(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS, 0).toString();
                }
                holder.date.setText(p.date_formatted);
                holder.body.setText(p.body);
                holder.delete.setOnClickListener(deleteClickListener);
                holder.delete.setTag(i);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return row;
        }
    }

    private class FetchPastebinsTask extends AsyncTaskEx<Void, Void, JSONObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            canLoadMore = false;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                return NetworkConnection.getInstance().pastebins(++page);
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
                        JSONArray pastebins = jsonObject.getJSONArray("pastebins");
                        Log.e("IRCCloud", "Got " + pastebins.length() + " pastebins for page " + page);
                        for (int i = 0; i < pastebins.length(); i++) {
                            JSONObject pastebin = pastebins.getJSONObject(i);
                            adapter.addPastebin(pastebin.getString("id"), pastebin.getString("name"), pastebin.getInt("lines"), new Date(pastebin.getLong("date") * 1000L), pastebin.getString("body"), pastebin.getBoolean("own_paste"));
                        }
                        adapter.notifyDataSetChanged();
                        canLoadMore = pastebins.length() > 0 && adapter.getCount() < jsonObject.getInt("total");
                        if(!canLoadMore)
                            footer.findViewById(R.id.progress).setVisibility(View.GONE);
                    } else {
                        page--;
                        Log.e("IRCCloud", "Failed: " + jsonObject.toString());
                        if(jsonObject.has("message") && jsonObject.getString("message").equals("server_error")) {
                            canLoadMore = true;
                            new FetchPastebinsTask().execute((Void) null);
                        } else {
                            canLoadMore = false;
                        }
                    }
                } catch (JSONException e) {
                    page--;
                    e.printStackTrace();
                }
            } else {
                page--;
                canLoadMore = true;
                new FetchPastebinsTask().execute((Void) null);
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

        setContentView(R.layout.ignorelist);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar));
            getSupportActionBar().setElevation(0);
        }

        if(savedInstanceState != null && savedInstanceState.containsKey("adapter")) {
            page = savedInstanceState.getInt("page");
            Pastebin[] pastebins = (Pastebin[])savedInstanceState.getSerializable("adapter");
            for(Pastebin p : pastebins) {
                adapter.addPastebin(p);
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
                    new FetchPastebinsTask().execute((Void) null);
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
                final Pastebin p = (Pastebin) adapter.getItem(i);

                Intent intent = new Intent(PastebinsActivity.this, PastebinViewerActivity.class);
                intent.setData(Uri.parse(p.url + "?id=" + p.id + "&own_paste=" + (p.own_paste ? "1" : "0")));
                startActivity(intent);
            }
        });

        Toast.makeText(this, "Tap a pastebin to view full text with syntax highlighting", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        NetworkConnection.getInstance().addHandler(this);
        try {
            uri_template = new URITemplate(NetworkConnection.getInstance().config.getString("pastebin_uri_template"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        NetworkConnection.getInstance().removeHandler(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(adapter != null)
            adapter.clear();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

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
                    Log.d("IRCCloud", "Pastebin deleted successfully");
                    reqid = -1;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.pastebins.remove(pasteToDelete);
                            adapter.notifyDataSetChanged();
                            checkEmpty();
                            pasteToDelete = null;
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_FAILURE_MSG:
                obj = (IRCCloudJSONObject) o;
                if (reqid != -1 && obj.getInt("_reqid") == reqid) {
                    Crashlytics.log(Log.ERROR, "IRCCloud", "Delete failed: " + obj.toString());
                    reqid = -1;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(PastebinsActivity.this);
                            builder.setTitle("Error");
                            builder.setMessage("Unable to delete this pastebin.  Please try again shortly.");
                            builder.setPositiveButton("Close", null);
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
            empty.setText("You haven't created any pastebins yet.");
        } else {
            findViewById(android.R.id.list).setVisibility(View.VISIBLE);
            findViewById(android.R.id.empty).setVisibility(View.GONE);
        }
    }
}