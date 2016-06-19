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

package com.irccloud.android.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.FontAwesome;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.collection.ServersList;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.Collections;

public class ServerReorderFragment extends DialogFragment implements NetworkConnection.IRCEventHandler {
    private ColorScheme colorScheme = ColorScheme.getInstance();
    private NetworkConnection conn;
    private ServerListAdapter adapter;
    private RefreshTask refreshTask = null;
    private DragSortListView listView = null;
    private DragSortListView.DropListener dropListener = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            Server s = adapter.data.get(from);
            adapter.data.remove(from);
            if (to >= adapter.data.size())
                adapter.data.add(s);
            else
                adapter.data.add(to, s);
            adapter.notifyDataSetChanged();

            String cids = "";
            for (int i = 0; i < adapter.data.size(); i++) {
                s = adapter.data.get(i);
                s.setOrder(i + 1);
                if (cids.length() > 0)
                    cids += ",";
                cids += s.getCid();
            }
            NetworkConnection.getInstance().reorder_connections(cids);
        }
    };

    private class ServerListAdapter extends BaseAdapter {
        ArrayList<Server> data;
        private DialogFragment ctx;
        int width = 0;

        private class ViewHolder {
            View background;
            TextView label;
            TextView icon;
            TextView drag_handle;
        }

        public ServerListAdapter(DialogFragment context) {
            ctx = context;
            data = new ArrayList<Server>();
            WindowManager wm = (WindowManager) IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            width = wm.getDefaultDisplay().getWidth();
        }

        public void setItems(ArrayList<Server> items) {
            data = items;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Server s = data.get(position);
            View row = convertView;
            ViewHolder holder;

            if (row == null) {
                LayoutInflater inflater = ctx.getLayoutInflater(null);
                row = inflater.inflate(R.layout.row_reorder, null);

                holder = new ViewHolder();
                holder.background = row.findViewById(R.id.serverBackground);
                holder.background.setFocusable(false);
                holder.background.setEnabled(false);
                holder.label = (TextView) row.findViewById(R.id.label);
                holder.icon = (TextView) row.findViewById(R.id.icon);
                holder.icon.setTypeface(FontAwesome.getTypeface());
                holder.drag_handle = (TextView) row.findViewById(R.id.drag_handle);
                holder.drag_handle.setTypeface(FontAwesome.getTypeface());
                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }

            if (s.getName() != null && s.getName().length() > 0)
                holder.label.setText(s.getName());
            else
                holder.label.setText(s.getHostname());

            if (s.getSsl() > 0)
                holder.icon.setText(FontAwesome.SHIELD);
            else
                holder.icon.setText(FontAwesome.GLOBE);

            if (s.getStatus() != null && s.getStatus().equals("connected_ready")) {
                holder.icon.setTextColor(colorScheme.bufferTextColor);
                holder.label.setTextColor(colorScheme.bufferTextColor);
            } else {
                holder.icon.setTextColor(colorScheme.inactiveBufferTextColor);
                holder.label.setTextColor(colorScheme.inactiveBufferTextColor);
            }

            holder.drag_handle.setText(FontAwesome.ARROWS);

            if (getShowsDialog())
                row.setBackgroundColor(colorScheme.dialogBackgroundColor);
            else
                row.setBackgroundColor(colorScheme.contentBackgroundColor);

            holder.label.setMinimumWidth(width);
            return row;
        }
    }

    private void refresh(ArrayList<Server> servers) {
        if (servers == null) {
            if (adapter != null) {
                adapter.data.clear();
                adapter.notifyDataSetInvalidated();
            }
            return;
        }

        if (adapter == null) {
            adapter = new ServerListAdapter(ServerReorderFragment.this);
        }

        adapter.setItems(servers);

        if (listView.getAdapter() == null)
            listView.setAdapter(adapter);
        else
            adapter.notifyDataSetChanged();
    }

    private class RefreshTask extends AsyncTaskEx<Void, Void, Void> {
        ArrayList<Server> servers = new ArrayList<Server>();

        @Override
        protected Void doInBackground(Void... params) {
            SparseArray<Server> s = ServersList.getInstance().getServers();
            for (int i = 0; i < s.size(); i++) {
                servers.add(s.valueAt(i));
            }
            Collections.sort(servers);
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled()) {
                refresh(servers);
                refreshTask = null;
            }
        }
    }

    private void init(View v) {
        listView = (DragSortListView) v.findViewById(android.R.id.list);
        listView.setDropListener(dropListener);
        TextView tv = (TextView) v.findViewById(R.id.hint);
        tv.setTypeface(FontAwesome.getTypeface());
        tv.setText(FontAwesome.ARROWS);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getShowsDialog()) {
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            final View v = inflater.inflate(R.layout.reorderservers, null);
            init(v);
            listView.setCacheColorHint(0xFFD9E7FF);
            return v;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context ctx = getActivity();
        if(ctx == null)
            return null;

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.reorderservers, null);
        init(v);
        listView.setCacheColorHint(0xfff3f3f3);

        Dialog d = new AlertDialog.Builder(ctx)
                .setTitle("Connections")
                .setView(v)
                .setNegativeButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        NetworkConnection.getInstance().removeHandler(ServerReorderFragment.this);
                    }
                })
                .create();
        return d;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        conn = NetworkConnection.getInstance();
        conn.addHandler(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(conn != null)
            conn.removeHandler(this);
    }

    public void onResume() {
        super.onResume();
        ArrayList<Server> servers = new ArrayList<Server>();
        SparseArray<Server> s = ServersList.getInstance().getServers();
        for (int i = 0; i < s.size(); i++) {
            servers.add(s.valueAt(i));
        }
        Collections.sort(servers);
        refresh(servers);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    public void onIRCEvent(int what, Object obj) {
        switch (what) {
            case NetworkConnection.EVENT_MAKESERVER:
            case NetworkConnection.EVENT_REORDERCONNECTIONS:
            case NetworkConnection.EVENT_CONNECTIONDELETED:
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (refreshTask != null)
                            refreshTask.cancel(true);
                        refreshTask = new RefreshTask();
                        refreshTask.execute((Void) null);
                    }
                });
                break;
            default:
                break;
        }
    }

    @Override
    public void onIRCRequestSucceeded(int reqid, IRCCloudJSONObject object) {
    }

    @Override
    public void onIRCRequestFailed(int reqid, IRCCloudJSONObject object) {
    }
}
