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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.irccloud.android.ColorScheme;
import com.irccloud.android.FontAwesome;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.model.Buffer;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class PinReorderFragment extends DialogFragment implements NetworkConnection.IRCEventHandler {
    private ColorScheme colorScheme = ColorScheme.getInstance();
    private NetworkConnection conn;
    private PinListAdapter adapter;
    private DragSortListView listView = null;
    private DragSortListView.DropListener dropListener = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            Integer i = adapter.data.get(from);
            adapter.data.remove(i);
            if (to >= adapter.data.size())
                adapter.data.add(i);
            else
                adapter.data.add(to, i);
            adapter.notifyDataSetChanged();

            try {
                if (NetworkConnection.getInstance().getUserInfo() != null) {
                    JSONObject prefs = NetworkConnection.getInstance().getUserInfo().prefs;
                    if (prefs == null) {
                        prefs = new JSONObject();
                    }
                    prefs.put("pinnedBuffers", new JSONArray(adapter.data));
                    conn.set_prefs(prefs.toString(), null);
                } else {
                    throw new IllegalStateException();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "An error occurred while saving preferences.  Please try again shortly", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private class PinListAdapter extends BaseAdapter {
        private ArrayList<Integer> data;
        private HashMap<String,Integer> nameCounts;
        private DialogFragment ctx;
        int width = 0;

        private class ViewHolder {
            View background;
            TextView label;
            TextView icon;
            TextView drag_handle;
        }

        public PinListAdapter(DialogFragment context) {
            ctx = context;
            data = new ArrayList<>();
            WindowManager wm = (WindowManager) IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            width = wm.getDefaultDisplay().getWidth();
        }

        public void setItems(ArrayList<Integer> items) {
            data = items;
            nameCounts = new HashMap<>();

            for (Integer bid : data) {
                Buffer b = BuffersList.getInstance().getBuffer(bid);
                if(b != null) {
                    int count = 1;
                    if (nameCounts.containsKey(b.getName()))
                        count += nameCounts.get(b.getName());
                    nameCounts.put(b.getName(), count);
                }
            }
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
            Buffer b = BuffersList.getInstance().getBuffer(data.get(position));
            View row = convertView;
            ViewHolder holder;

            if (row == null) {
                LayoutInflater inflater = ctx.getLayoutInflater(null);
                row = inflater.inflate(R.layout.row_reorder, null);

                holder = new ViewHolder();
                holder.background = row.findViewById(R.id.serverBackground);
                holder.background.setFocusable(false);
                holder.background.setEnabled(false);
                holder.label = row.findViewById(R.id.label);
                holder.icon = row.findViewById(R.id.icon);
                holder.icon.setTypeface(FontAwesome.getTypeface());
                holder.drag_handle = row.findViewById(R.id.drag_handle);
                holder.drag_handle.setTypeface(FontAwesome.getTypeface());
                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }

            b.showServerSuffix(nameCounts.containsKey(b.getName()) && nameCounts.get(b.getName()) > 1);
            String name = b.getDisplayName() + b.getServerSuffix();
            holder.label.setText(name);
            holder.label.setTextColor(b.getTextColor());
            holder.icon.setText(b.getIcon());
            holder.icon.setTextColor(b.getTextColor());

            holder.drag_handle.setText(FontAwesome.ARROWS);

            if (getShowsDialog())
                row.setBackgroundColor(colorScheme.dialogBackgroundColor);
            else
                row.setBackgroundColor(colorScheme.contentBackgroundColor);

            holder.label.setMinimumWidth(width);
            return row;
        }
    }

    private void refresh() {
        JSONObject prefs = NetworkConnection.getInstance().getUserInfo().prefs;
        if (prefs == null) {
            prefs = new JSONObject();
        }
        JSONArray pinnedBuffers = null;
        try {
            if (prefs.has("pinnedBuffers")) {
                pinnedBuffers = prefs.getJSONArray("pinnedBuffers");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (pinnedBuffers == null)
            pinnedBuffers = new JSONArray();
        ArrayList<Integer> pins = new ArrayList<>();

        try {
            for (int i = 0; i < pinnedBuffers.length(); i++) {
                if(BuffersList.getInstance().getBuffer(pinnedBuffers.getInt(i)) != null)
                    pins.add(pinnedBuffers.getInt(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (pins.size() == 0) {
            if (adapter != null) {
                adapter.data.clear();
                adapter.notifyDataSetInvalidated();
            }
            return;
        }

        if (adapter == null) {
            adapter = new PinListAdapter(PinReorderFragment.this);
        }

        adapter.setItems(pins);

        if (listView.getAdapter() == null)
            listView.setAdapter(adapter);
        else
            adapter.notifyDataSetChanged();
    }

    private void init(View v) {
        listView = v.findViewById(android.R.id.list);
        DragSortController controller = new DragSortController(listView);
        controller.setDragHandleId(R.id.drag_handle);
        controller.setSortEnabled(true);
        controller.setDragInitMode(DragSortController.ON_DRAG);

        listView.setOnTouchListener(controller);
        listView.setFloatViewManager(controller);
        listView.setDropListener(dropListener);
        TextView tv = v.findViewById(R.id.hint);
        tv.setTypeface(FontAwesome.getTypeface());
        tv.setText(FontAwesome.ARROWS);
        tv = v.findViewById(R.id.hintText);
        tv.setText(" to reorder the pinned channels");
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
                .setTitle("Pinned Channels")
                .setView(v)
                .setNegativeButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        NetworkConnection.getInstance().removeHandler(PinReorderFragment.this);
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
        refresh();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    public void onIRCEvent(int what, Object obj) {
        switch (what) {
            case NetworkConnection.EVENT_USERINFO:
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
                break;
            default:
                break;
        }
    }
}
