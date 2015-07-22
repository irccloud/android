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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.databinding.RowAcceptlistBinding;
import com.squareup.leakcanary.RefWatcher;

import org.solovyev.android.views.llm.LinearLayoutManager;

public class AcceptListFragment extends DialogFragment {
    JsonNode acceptList;
    int cid;
    IRCCloudJSONObject event;
    AcceptListAdapter adapter;
    NetworkConnection conn;
    RecyclerView recyclerView;
    TextView empty;

    private class ViewHolder extends RecyclerView.ViewHolder {
        public RowAcceptlistBinding binding;

        public ViewHolder(View v) {
            super(v);
            binding = DataBindingUtil.bind(v);
        }
    }

    private class AcceptListAdapter extends RecyclerView.Adapter<ViewHolder> {
        @Override
        public int getItemCount() {
            return acceptList.size();
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
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = RowAcceptlistBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot();
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            RowAcceptlistBinding row = holder.binding;
            row.setNick(acceptList.get(position).asText());
            row.removeBtn.setOnClickListener(removeClickListener);
            row.removeBtn.setTag(position);
            row.executePendingBindings();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context ctx = getActivity();
        if(ctx == null)
            return null;

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.recyclerview, null);
        recyclerView = (RecyclerView) v.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
        empty = (TextView) v.findViewById(android.R.id.empty);
        empty.setText("No accepted nicks.\n\nYou can accept someone by tapping their message request or by using /accept.");
        if (savedInstanceState != null && savedInstanceState.containsKey("cid")) {
            cid = savedInstanceState.getInt("cid");
            event = new IRCCloudJSONObject(savedInstanceState.getString("event"));
            acceptList = event.getJsonNode("nicks");
            adapter = new AcceptListAdapter();
            recyclerView.setAdapter(adapter);
            if(adapter.getItemCount() > 0) {
                empty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                empty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        }
        Server s = ServersList.getInstance().getServer(cid);
        String network = s.getName();
        if (network == null || network.length() == 0)
            network = s.getHostname();
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

            ServersList s = ServersList.getInstance();
            Server server = s.getServer(cid);
            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
            LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.dialog_textprompt, null);
            TextView prompt = (TextView) view.findViewById(R.id.prompt);
            final EditText input = (EditText) view.findViewById(R.id.textInput);
            input.setHint("nickname");
            prompt.setText("Accept messages from this nickname");
            builder.setTitle(server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")");
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
        if (getActivity() != null && cid > 0 && recyclerView != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (adapter == null) {
                        adapter = new AcceptListAdapter();
                        recyclerView.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                    if(adapter.getItemCount() > 0) {
                        empty.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    } else {
                        empty.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    public void onResume() {
        super.onResume();
        conn = NetworkConnection.getInstance();

        if (cid > 0) {
            adapter = new AcceptListAdapter();
            recyclerView.setAdapter(adapter);
            if(adapter.getItemCount() > 0) {
                empty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                empty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = IRCCloudApplication.getRefWatcher(getActivity());
        if (refWatcher != null)
            refWatcher.watch(this);
    }
}
