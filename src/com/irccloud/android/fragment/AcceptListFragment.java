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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.ServersDataSource;
import com.squareup.leakcanary.RefWatcher;

public class AcceptListFragment extends DialogFragment {
    JsonNode acceptList;
    int cid;
    IRCCloudJSONObject event;
    AcceptListAdapter adapter;
    NetworkConnection conn;
    ListView listView;

    private class AcceptListAdapter extends BaseAdapter {
        private DialogFragment ctx;

        private class ViewHolder {
            int position;
            TextView user;
            Button removeBtn;
        }

        public AcceptListAdapter(DialogFragment context) {
            ctx = context;
        }

        @Override
        public int getCount() {
            return acceptList.size();
        }

        @Override
        public Object getItem(int pos) {
            try {
                return acceptList.get(pos);
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
                    conn.say(cid, null, "/accept -" + acceptList.get(position).asText());
                    conn.say(cid, null, "/accept *");
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewHolder holder;

            if (row != null && ((ViewHolder) row.getTag()).position != position)
                row = null;

            if (row == null) {
                LayoutInflater inflater = ctx.getLayoutInflater(null);
                row = inflater.inflate(R.layout.row_acceptlist, null);

                holder = new ViewHolder();
                holder.position = position;
                holder.user = (TextView) row.findViewById(R.id.nick);
                holder.removeBtn = (Button) row.findViewById(R.id.removeBtn);

                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }

            try {
                holder.user.setText(acceptList.get(position).asText());
                holder.removeBtn.setOnClickListener(removeClickListener);
                holder.removeBtn.setTag(position);
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

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.ignorelist, null);
        listView = (ListView) v.findViewById(android.R.id.list);
        TextView empty = (TextView) v.findViewById(android.R.id.empty);
        empty.setText("No accepted nicks.\n\nYou can accept someone by tapping their message request or by using /accept.");
        listView.setEmptyView(empty);
        if (savedInstanceState != null && savedInstanceState.containsKey("cid")) {
            cid = savedInstanceState.getInt("cid");
            event = new IRCCloudJSONObject(savedInstanceState.getString("event"));
            acceptList = event.getJsonNode("nicks");
            adapter = new AcceptListAdapter(this);
            listView.setAdapter(adapter);
        }
        ServersDataSource.Server s = ServersDataSource.getInstance().getServer(cid);
        String network = s.name;
        if (network == null || network.length() == 0)
            network = s.hostname;
        AlertDialog d = new AlertDialog.Builder(ctx)
                .setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                .setTitle("Accept list for " + network)
                .setView(v)
                .setPositiveButton("Add Nickname", new AddClickListener())
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();

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
            input.setHint("nickname");
            prompt.setText("Accept messages from this nickname");
            builder.setTitle(server.name + " (" + server.hostname + ":" + (server.port) + ")");
            builder.setView(view);
            builder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    conn.say(cid, null, "/accept " + input.getText().toString());
                    conn.say(cid, null, "/accept *");
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
        state.putString("event", event.toString());
    }

    @Override
    public void setArguments(Bundle args) {
        cid = args.getInt("cid", 0);
        event = new IRCCloudJSONObject(args.getString("event"));
        acceptList = event.getJsonNode("nicks");
        if (getActivity() != null && cid > 0 && listView != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (adapter == null) {
                        adapter = new AcceptListAdapter(AcceptListFragment.this);
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

        if (cid > 0) {
            adapter = new AcceptListAdapter(this);
            listView.setAdapter(adapter);
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = IRCCloudApplication.getRefWatcher(getActivity());
        refWatcher.watch(this);
    }
}
