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

package com.irccloud.android.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.databinding.RowWhowasBinding;

import org.solovyev.android.views.llm.LinearLayoutManager;

public class WhoWasFragment extends DialogFragment {
    JsonNode data;
    IRCCloudJSONObject event;
    Adapter adapter;
    NetworkConnection conn;
    RecyclerView recyclerView;
    TextView empty;
    String title;

    private class ViewHolder extends RecyclerView.ViewHolder {
        public RowWhowasBinding binding;

        public ViewHolder(View v) {
            super(v);
            binding = DataBindingUtil.bind(v);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        @Override
        public int getItemCount() {
            if(data.size() == 1 && data.get(0).has("no_such_nick"))
                return 0;
            else
                return data.size();
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = RowWhowasBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot();
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            RowWhowasBinding row = holder.binding;
            JsonNode node = data.get(position);
            row.setNick(node.get("nick").asText());
            if(node.has("user") && node.has("host")) {
                row.setUsermask(node.get("user").asText() + "@" + node.get("host").asText());
            } else {
                row.usermaskHeading.setVisibility(View.GONE);
                row.usermask.setVisibility(View.GONE);
            }
            row.setRealname(node.get("realname").asText());
            if(node.get("last_seen") != null)
                row.setLastconnected(node.get("last_seen").asText());
            else
                row.setLastconnected("");
            if(node.has("ircserver") && node.get("ircserver") != null) {
                row.setConnectedvia(node.get("ircserver").asText());
            } else {
                row.setConnectedvia("");
            }
            if(node.has("connecting_from") && node.get("connecting_from") != null) {
                row.setInfo(node.get("connecting_from").asText());
            } else {
                row.infoHeading.setVisibility(View.GONE);
                row.info.setVisibility(View.GONE);
            }
            if(node.has("actual_host") && node.get("actual_host") != null) {
                row.setActualhost(node.get("actual_host").asText());
            } else {
                row.actualhostHeading.setVisibility(View.GONE);
                row.actualhost.setVisibility(View.GONE);
            }
            row.executePendingBindings();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context ctx = getActivity();
        if (ctx == null)
            return null;

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.recyclerview, null);
        recyclerView = v.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
        empty = v.findViewById(android.R.id.empty);
        empty.setText("There was no such nickname");
        if (savedInstanceState != null && savedInstanceState.containsKey("event")) {
            event = new IRCCloudJSONObject(savedInstanceState.getString("event"));
            title = savedInstanceState.getString("title");
            data = event.getJsonNode("lines");
            adapter = new Adapter();
            recyclerView.setAdapter(adapter);
            if(adapter.getItemCount() > 0) {
                empty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                empty.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        }
        AlertDialog.Builder b = new AlertDialog.Builder(ctx)
                .setTitle(title)
                .setView(v)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog d = b.create();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return d;
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putString("event", event.toString());
        state.putString("title", title);
    }

    @Override
    public void setArguments(Bundle args) {
        if(args != null) {
            event = new IRCCloudJSONObject(args.getString("event"));
            title = args.getString("title");
            data = event.getJsonNode("lines");

            if (getActivity() != null && recyclerView != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (adapter == null) {
                            adapter = new Adapter();
                            recyclerView.setAdapter(adapter);
                        } else {
                            adapter.notifyDataSetChanged();
                        }
                        if (adapter.getItemCount() > 0) {
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
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter = new Adapter();
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
