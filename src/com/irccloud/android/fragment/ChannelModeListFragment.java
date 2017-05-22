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
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.model.Event;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.data.model.User;
import com.irccloud.android.data.collection.UsersList;
import com.irccloud.android.databinding.RowChannelmodeBinding;

import org.solovyev.android.views.llm.LinearLayoutManager;

public class ChannelModeListFragment extends DialogFragment implements NetworkConnection.IRCEventHandler {
    JsonNode data;
    int cid;
    int bid = -1;
    IRCCloudJSONObject event;
    Adapter adapter;
    NetworkConnection conn;
    RecyclerView recyclerView;
    TextView empty;
    String mode;
    String placeholder;
    String mask;
    String list;
    String title;
    boolean canChangeMode = false;

    private class ViewHolder extends RecyclerView.ViewHolder {
        public RowChannelmodeBinding binding;

        public ViewHolder(View v) {
            super(v);
            binding = DataBindingUtil.bind(v);
        }
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {

        @Override
        public int getItemCount() {
            return data.size();
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
                    NetworkConnection.printStackTraceToCrashlytics(e);
                }
            }
        };

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = RowChannelmodeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot();
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            RowChannelmodeBinding row = holder.binding;
            JsonNode node = data.get(position);
            row.setMask(Html.fromHtml(node.get(mask).asText()));
            if(node.has("usermask") && node.get("usermask") != null && node.get("usermask").asText() != null)
                row.setSetBy("Set " + DateUtils.getRelativeTimeSpanString(node.get("time").asLong() * 1000L, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS)
                    + " by " + node.get("usermask").asText());
            else
                row.setSetBy("");
            if (canChangeMode) {
                row.removeBtn.setVisibility(View.VISIBLE);
                row.removeBtn.setOnClickListener(removeClickListener);
                row.removeBtn.setColorFilter(ColorScheme.getInstance().colorControlNormal, PorterDuff.Mode.SRC_ATOP);
                row.removeBtn.setTag(position);
            } else {
                row.removeBtn.setVisibility(View.GONE);
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
        recyclerView = (RecyclerView) v.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
        empty = (TextView) v.findViewById(android.R.id.empty);
        empty.setText(placeholder);
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

            ServersList s = ServersList.getInstance();
            Server server = s.getServer(cid);
            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.dialog_textprompt, null);
            TextView prompt = (TextView) view.findViewById(R.id.prompt);
            final EditText input = (EditText) view.findViewById(R.id.textInput);
            input.setHint("nickname!user@host.name");
            prompt.setText("Add this hostmask");
            builder.setTitle(server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")");
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
        cid = args.getInt("cid", -1);
        bid = args.getInt("bid", -1);
        event = new IRCCloudJSONObject(args.getString("event"));
        list = args.getString("list");
        mask = args.getString("mask");
        placeholder = args.getString("placeholder");
        title = args.getString("title");
        mode = args.getString("mode");
        data = event.getJsonNode(list);
        Server s = ServersList.getInstance().getServer(cid);
        if(s != null) {
            User self_user = UsersList.getInstance().getUser(bid, s.getNick());
            canChangeMode = (self_user != null && (self_user.mode.contains(s.MODE_OWNER) || self_user.mode.contains(s.MODE_ADMIN) || self_user.mode.contains(s.MODE_OP)));
        } else {
            canChangeMode = false;
        }
        if (getActivity() != null && cid > 0 && recyclerView != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (adapter == null) {
                        adapter = new Adapter();
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

        if (cid > 0) {
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
        Server s = ServersList.getInstance().getServer(cid);
        if (s != null) {
            User self_user = UsersList.getInstance().getUser(bid, s.getNick());
            canChangeMode = (self_user != null && (self_user.mode.contains(s.MODE_OWNER) || self_user.mode.contains(s.MODE_ADMIN) || self_user.mode.contains(s.MODE_OP) || self_user.mode.contains(s.MODE_HALFOP)));
        } else {
            canChangeMode = false;
        }
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
        if (conn != null)
            conn.removeHandler(this);
    }

    public void onIRCEvent(int what, Object obj) {
        switch (what) {
            case NetworkConnection.EVENT_USERCHANNELMODE:
                Server s = ServersList.getInstance().getServer(cid);
                User self_user = UsersList.getInstance().getUser(bid, s.getNick());
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
                Event e = (Event) obj;
                if (e.bid == bid && e.type.equals("channel_mode_list_change"))
                    conn.mode(cid, event.getString("channel"), mode);
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
