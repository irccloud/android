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

package com.irccloud.android.activity;

import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.OnErrorListener;
import com.irccloud.android.data.model.Pastebin;
import com.irccloud.android.databinding.RowPastebinBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

public class PastebinsActivity extends BaseActivity {
    private int page = 0;
    private PastebinsAdapter adapter = new PastebinsAdapter();
    private boolean canLoadMore = true;
    private View footer;

    private class PastebinsAdapter extends BaseAdapter {
        private ArrayList<Pastebin> pastebins = new ArrayList<>();

        public void clear() {
            pastebins.clear();
            notifyDataSetInvalidated();
        }

        public void saveInstanceState(Bundle state) {
            state.putSerializable("adapter", pastebins.toArray(new Pastebin[pastebins.size()]));
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
                final Pastebin pasteToDelete = (Pastebin)getItem((Integer)view.getTag());
                AlertDialog.Builder builder = new AlertDialog.Builder(PastebinsActivity.this);
                builder.setTitle("Delete Pastebin");
                if(pasteToDelete.getName() != null && pasteToDelete.getName().length() > 0) {
                    builder.setMessage("Are you sure you want to delete '" + pasteToDelete.getName() + "'?");
                } else {
                    builder.setMessage("Are you sure you want to delete this pastebin?");
                }
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        pasteToDelete.delete(new OnErrorListener<Pastebin>() {
                            @Override
                            public void onSuccess(final Pastebin object) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.pastebins.remove(object);
                                        adapter.notifyDataSetChanged();
                                        checkEmpty();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(Pastebin object) {
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
                        });
                        pastebins.remove(pasteToDelete);
                        notifyDataSetChanged();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                AlertDialog d = builder.create();
                d.setOwnerActivity(PastebinsActivity.this);
                d.show();
            }
        };

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            RowPastebinBinding binding;

            if (view == null) {
                binding = RowPastebinBinding.inflate(getLayoutInflater(), viewGroup, false);
            } else {
                binding = (RowPastebinBinding)view.getTag();
            }

            binding.setPastebin(pastebins.get(i));
            binding.delete.setOnClickListener(deleteClickListener);
            binding.delete.setColorFilter(ColorScheme.getInstance().colorControlNormal, PorterDuff.Mode.SRC_ATOP);
            binding.delete.setTag(i);
            binding.executePendingBindings();
            return binding.getRoot();
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
                NetworkConnection.printStackTraceToCrashlytics(e);
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
                            adapter.addPastebin(new Pastebin(pastebins.getJSONObject(i)));
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
                    NetworkConnection.printStackTraceToCrashlytics(e);
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
        setTheme(ColorScheme.getDialogWhenLargeTheme(ColorScheme.getUserTheme()));
        setContentView(R.layout.listview);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(android.support.v7.appcompat.R.drawable.abc_ic_ab_back_material);
            getSupportActionBar().setElevation(0);
        }

        if(savedInstanceState != null && savedInstanceState.containsKey("adapter")) {
            try {
                page = savedInstanceState.getInt("page");
                Pastebin[] pastebins = (Pastebin[]) savedInstanceState.getSerializable("adapter");
                for (Pastebin p : pastebins) {
                    adapter.addPastebin(p);
                }
                adapter.notifyDataSetChanged();
            } catch (Exception e) {
                page = 0;
                adapter.clear();
            }
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
                if(i < adapter.getCount()) {
                    final Pastebin p = (Pastebin) adapter.getItem(i);

                    Intent intent = new Intent(PastebinsActivity.this, PastebinViewerActivity.class);
                    intent.setData(Uri.parse(p.getUrl() + "?id=" + p.getId() + "&own_paste=" + (p.isOwn_paste() ? "1" : "0")));
                    startActivity(intent);
                }
            }
        });

        Toast.makeText(this, "Tap a pastebin to view full text with syntax highlighting", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        NetworkConnection.getInstance().addHandler(this);
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