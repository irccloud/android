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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.ColorFormatter;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.databinding.RowWhoBinding;
import com.squareup.leakcanary.RefWatcher;

import org.solovyev.android.views.llm.LinearLayoutManager;

public class WhoListFragment extends DialogFragment {
    JsonNode users;
    UsersAdapter adapter;
    NetworkConnection conn;
    RecyclerView recyclerView;
    TextView empty;
    IRCCloudJSONObject event;

    private class ViewHolder extends RecyclerView.ViewHolder {
        public RowWhoBinding binding;

        public ViewHolder(View v) {
            super(v);
            binding = DataBindingUtil.bind(v);
        }
    }

    private class UsersAdapter extends RecyclerView.Adapter<ViewHolder> {

        @Override
        public int getItemCount() {
            return users.size();
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = RowWhoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot();
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            RowWhoBinding row = holder.binding;
            row.setNick(users.get(position).get("nick").asText());
            row.setName(ColorFormatter.html_to_spanned("&nbsp;(" + ColorFormatter.emojify(ColorFormatter.irc_to_html(TextUtils.htmlEncode(users.get(position).get("realname").asText()))) + ")"));
            row.setServer("Connected via " + users.get(position).get("ircserver").asText());
            row.setMask(users.get(position).get("usermask").asText());
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
        empty.setText("No results found.");
        if (savedInstanceState != null && savedInstanceState.containsKey("event")) {
            event = new IRCCloudJSONObject(savedInstanceState.getString("event"));
            users = event.getJsonNode("users");
            adapter = new UsersAdapter();
            recyclerView.setAdapter(adapter);
            if(adapter.getItemCount() > 0) {
                empty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                empty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        }
        Dialog d = new AlertDialog.Builder(ctx)
                .setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
                .setTitle("WHO response for " + event.getString("subject"))
                .setView(v)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        return d;
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putString("event", event.toString());
    }

    @Override
    public void setArguments(Bundle args) {
        event = new IRCCloudJSONObject(args.getString("event"));
        users = event.getJsonNode("users");
        if (getActivity() != null && recyclerView != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (adapter == null) {
                        adapter = new UsersAdapter();
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

        if (users != null) {
            adapter = new UsersAdapter();
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
    public void onPause() {
        super.onPause();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = IRCCloudApplication.getRefWatcher(getActivity());
        if(refWatcher != null)
            refWatcher.watch(this);
    }
}
