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

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v7.widget.RecyclerView;
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
import com.squareup.leakcanary.RefWatcher;

import org.solovyev.android.views.llm.LinearLayoutManager;

import java.util.ArrayList;

public class ChannelListFragment extends Fragment implements NetworkConnection.IRCEventHandler {
    ArrayList<ChannelRow> channels;
    ChannelsAdapter adapter;
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
    public View onCreateView(LayoutInflater i, ViewGroup container, Bundle savedInstanceState) {
        conn = NetworkConnection.getInstance();
        conn.addHandler(this);

        Context ctx = getActivity();
        if (ctx == null)
            return null;

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.recyclerview, null);
        recyclerView = (RecyclerView) v.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
        empty = (TextView) v.findViewById(android.R.id.empty);
        empty.setText("Loading channel list…");
        return v;
    }

    @Override
    public void setArguments(Bundle args) {
        server = ServersList.getInstance().getServer(args.getInt("cid", -1));
        channels = null;
        if (recyclerView != null && getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    empty.setText("Loading channel list…");
                    adapter = new ChannelsAdapter();
                    recyclerView.setAdapter(adapter);
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
        if (conn == null) {
            conn = NetworkConnection.getInstance();
            conn.addHandler(this);
        }

        if (adapter == null) {
            adapter = new ChannelsAdapter();
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
        if (conn != null)
            conn.removeHandler(this);
        conn = null;
    }

    public void onIRCEvent(int what, Object obj) {
        final IRCCloudJSONObject o;
        switch (what) {
            case NetworkConnection.EVENT_LISTRESPONSE:
                o = (IRCCloudJSONObject) obj;
                if (getActivity() != null)
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.set(o.getJsonNode("channels"));
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

    @Override
    public void onIRCRequestSucceeded(int reqid, IRCCloudJSONObject object) {
    }

    @Override
    public void onIRCRequestFailed(int reqid, IRCCloudJSONObject object) {
    }
}
