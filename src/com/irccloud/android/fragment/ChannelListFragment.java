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
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.ListFragment;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.ColorFormatter;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.IRCCloudLinkMovementMethod;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.databinding.RowChannelBinding;

import org.solovyev.android.views.llm.LinearLayoutManager;

import java.util.ArrayList;

public class ChannelListFragment extends DialogFragment implements NetworkConnection.IRCEventHandler {
    ArrayList<ChannelRow> channels;
    ChannelsAdapter adapter = new ChannelsAdapter();
    NetworkConnection conn;
    RecyclerView recyclerView;
    TextView empty;
    Server server;

    public static class ChannelRow {
        public Spanned name;
        public Spanned topic;
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        public RowChannelBinding binding;

        public ViewHolder(View v) {
            super(v);
            binding = DataBindingUtil.bind(v);
        }
    }

    private class ChannelsAdapter extends RecyclerView.Adapter<ViewHolder> {

        public void set(JsonNode json) {
            channels = new ArrayList<>(json.size());

            for (int i = 0; i < json.size(); i++) {
                ChannelRow c = new ChannelRow();
                JsonNode o = json.get(i);
                String channel = o.get("name").asText() + " (" + o.get("num_members").asInt() + " member";
                if (o.get("num_members").asInt() != 1)
                    channel += "s";
                channel += ")";
                c.name = ColorFormatter.html_to_spanned(channel, true, server);

                String topic = o.get("topic").asText();
                if (topic.length() > 0) {
                    c.topic = ColorFormatter.html_to_spanned(topic, true, server);
                } else {
                    c.topic = null;
                }
                channels.add(c);
            }
        }

        @Override
        public int getItemCount() {
            if (channels == null)
                return 0;
            else
                return channels.size();
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = RowChannelBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot();
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            RowChannelBinding row = holder.binding;
            ChannelRow c = channels.get(position);

            row.setChannel(c);
            row.name.setMovementMethod(IRCCloudLinkMovementMethod.getInstance());
            row.topic.setMovementMethod(IRCCloudLinkMovementMethod.getInstance());
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
        recyclerView = v.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
        recyclerView.setAdapter(adapter);
        empty = v.findViewById(android.R.id.empty);
        empty.setText("Loading channel list…");
        Dialog d = new AlertDialog.Builder(ctx)
                .setTitle("List of channels on " + server.getHostname())
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
    public void onDestroy() {
        super.onDestroy();
        if (conn != null)
            conn.removeHandler(this);
    }

    @Override
    public void setArguments(Bundle args) {
        if(args != null) {
            server = ServersList.getInstance().getServer(args.getInt("cid", -1));
            channels = null;
            if (recyclerView != null && getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getDialog().setTitle("List of channels on " + server.getHostname());
                        if (adapter.getItemCount() > 0) {
                            empty.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        } else {
                            empty.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }

    public void onResume() {
        super.onResume();
        if(adapter.getItemCount() > 0) {
            empty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        } else {
            empty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }
        adapter.notifyDataSetChanged();
    }

    public void onIRCEvent(int what, Object obj) {
        final IRCCloudJSONObject o;
        switch (what) {
            case NetworkConnection.EVENT_LISTRESPONSE:
                o = (IRCCloudJSONObject) obj;
                server = ServersList.getInstance().getServer(o.cid());
                adapter.set(o.getJsonNode("channels"));
                if (getActivity() != null)
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getDialog().setTitle("List of channels on " + server.getHostname());
                            adapter.notifyDataSetChanged();
                            if(adapter.getItemCount() > 0) {
                                empty.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                            } else {
                                empty.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.GONE);
                            }
                        }
                    });
                break;
            case NetworkConnection.EVENT_LISTRESPONSETOOMANY:
                if (getActivity() != null)
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            empty.setText("Too many channels to list.  Try limiting the list to only respond with channels that have more than e.g. 50 members: /LIST >50");
                        }
                    });
                break;
            default:
                break;
        }
    }
}
