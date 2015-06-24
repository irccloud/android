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

package com.irccloud.android.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v4.app.DialogFragment;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.EventsDataSource;
import com.irccloud.android.data.ServersDataSource;
import com.irccloud.android.data.UsersDataSource;
import com.squareup.leakcanary.RefWatcher;

public class ChannelModeListFragment extends DialogFragment implements NetworkConnection.IRCEventHandler {
    JsonNode data;
    int cid;
    int bid;
    IRCCloudJSONObject event;
    Adapter adapter;
    NetworkConnection conn;
    ListView listView;
    String mode;
    String placeholder;
    String mask;
    String list;
    String title;
    boolean canChangeMode = false;

    private class Adapter extends BaseAdapter {
        private DialogFragment ctx;

        private class ViewHolder {
            int position;
            TextView mask;
            TextView setBy;
            ImageButton removeBtn;
        }

        public Adapter(DialogFragment context) {
            ctx = context;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int pos) {
            try {
                return data.get(pos);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        OnClickListener removeClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer position = (Integer) v.getTag();
                try {
                    conn.mode(cid, event.getString("channel"), "-" + mode + " " + data.get(position).get(mask).asText());
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (ctx == null)
                return null;

            View row = convertView;
            ViewHolder holder;

            if (row != null && ((ViewHolder) row.getTag()).position != position)
                row = null;

            if (row == null) {
                LayoutInflater inflater = ctx.getLayoutInflater(null);
                row = inflater.inflate(R.layout.row_banlist, null);

                holder = new ViewHolder();
                holder.position = position;
                holder.mask = (TextView) row.findViewById(R.id.mask);
                holder.setBy = (TextView) row.findViewById(R.id.setBy);
                holder.removeBtn = (ImageButton) row.findViewById(R.id.removeBtn);

                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }

            try {
                holder.mask.setText(Html.fromHtml(data.get(position).get(mask).asText()));
                holder.setBy.setText("Set " + DateUtils.getRelativeTimeSpanString(data.get(position).get("time").asLong() * 1000L, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)
                        + " by " + data.get(position).get("usermask").asText());
                if (canChangeMode) {
                    holder.removeBtn.setVisibility(View.VISIBLE);
                    holder.removeBtn.setOnClickListener(removeClickListener);
                    holder.removeBtn.setTag(position);
                } else {
                    holder.removeBtn.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return row;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context ctx = getActivity();
        if (ctx == null)
            return null;

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.ignorelist, null);
        listView = (ListView) v.findViewById(android.R.id.list);
        TextView empty = (TextView) v.findViewById(android.R.id.empty);
        empty.setText(placeholder);
        listView.setEmptyView(empty);
        if (savedInstanceState != null && savedInstanceState.containsKey("cid")) {
            cid = savedInstanceState.getInt("cid");
            bid = savedInstanceState.getInt("bid");
            event = new IRCCloudJSONObject(savedInstanceState.getString("event"));
            list = savedInstanceState.getString("list");
            mask = savedInstanceState.getString("mask");
            placeholder = savedInstanceState.getString("placeholder");
            title = savedInstanceState.getString("title");
            mode = savedInstanceState.getString("mode");
            data = event.getJsonNode(list);
            adapter = new Adapter(this);
            listView.setAdapter(adapter);
        }
        AlertDialog.Builder b = new AlertDialog.Builder(ctx)
                .setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                .setTitle(title)
                .setView(v)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        try {
            if (canChangeMode) {
                b.setPositiveButton("Add", new AddClickListener());
            }
        } catch (Exception e) {

        }

        AlertDialog d = b.create();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return d;
    }

    class AddClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface d, int which) {
            Context ctx = getActivity();

            ServersDataSource s = ServersDataSource.getInstance();
            ServersDataSource.Server server = s.getServer(cid);
            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
            LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.dialog_textprompt, null);
            TextView prompt = (TextView) view.findViewById(R.id.prompt);
            final EditText input = (EditText) view.findViewById(R.id.textInput);
            input.setHint("nickname!user@host.name");
            prompt.setText("Add this hostmask");
            builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
            builder.setView(view);
            builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    conn.mode(cid, event.getString("channel"), "+" + mode + " " + input.getText().toString());
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.setOwnerActivity(getActivity());
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            dialog.show();
        }
    }


    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putInt("cid", cid);
        state.putInt("bid", bid);
        state.putString("event", event.toString());
        state.putString("list", list);
        state.putString("mask", mask);
        state.putString("placeholder", placeholder);
        state.putString("title", title);
        state.putString("mode", mode);
    }

    @Override
    public void setArguments(Bundle args) {
        cid = args.getInt("cid", 0);
        bid = args.getInt("bid", 0);
        event = new IRCCloudJSONObject(args.getString("event"));
        list = args.getString("list");
        mask = args.getString("mask");
        placeholder = args.getString("placeholder");
        title = args.getString("title");
        mode = args.getString("mode");
        data = event.getJsonNode(list);
        ServersDataSource.Server s = ServersDataSource.getInstance().getServer(cid);
        UsersDataSource.User self_user = UsersDataSource.getInstance().getUser(bid, s.nick);
        canChangeMode = (self_user != null && (self_user.mode.contains(s.MODE_OWNER) || self_user.mode.contains(s.MODE_ADMIN) || self_user.mode.contains(s.MODE_OP)));
        if (getActivity() != null && cid > 0 && listView != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (adapter == null) {
                        adapter = new Adapter(ChannelModeListFragment.this);
                        listView.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                }
            });
        }
    }

    public void onResume() {
        super.onResume();
        conn = NetworkConnection.getInstance();
        conn.addHandler(this);

        if (cid > 0) {
            adapter = new Adapter(this);
            listView.setAdapter(adapter);
        }
        ServersDataSource.Server s = ServersDataSource.getInstance().getServer(cid);
        if (s != null) {
            UsersDataSource.User self_user = UsersDataSource.getInstance().getUser(bid, s.nick);
            canChangeMode = (self_user != null && (self_user.mode.contains(s.MODE_OWNER) || self_user.mode.contains(s.MODE_ADMIN) || self_user.mode.contains(s.MODE_OP) || self_user.mode.contains(s.MODE_HALFOP)));
        } else {
            canChangeMode = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (conn != null)
            conn.removeHandler(this);
    }

    @Override public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = IRCCloudApplication.getRefWatcher(getActivity());
        if(refWatcher != null) {
            refWatcher.watch(this);
            if(data != null)
                refWatcher.watch(data);
            if(event != null)
                refWatcher.watch(event);
        }
    }

    public void onIRCEvent(int what, Object obj) {
        switch (what) {
            case NetworkConnection.EVENT_USERCHANNELMODE:
                ServersDataSource.Server s = ServersDataSource.getInstance().getServer(cid);
                UsersDataSource.User self_user = UsersDataSource.getInstance().getUser(bid, s.nick);
                canChangeMode = (self_user != null && (self_user.mode.contains(s.MODE_OWNER) || self_user.mode.contains(s.MODE_ADMIN) || self_user.mode.contains(s.MODE_OP)));
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (adapter != null)
                            adapter.notifyDataSetChanged();
                    }
                });
                break;
            case NetworkConnection.EVENT_BUFFERMSG:
                EventsDataSource.Event e = (EventsDataSource.Event) obj;
                if (e.bid == bid && e.type.equals("channel_mode_list_change"))
                    conn.mode(cid, event.getString("channel"), mode);
                break;
            default:
                break;
        }
    }
}
