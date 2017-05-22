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
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.databinding.RowHostmaskBinding;

import org.solovyev.android.views.llm.LinearLayoutManager;

public class IgnoreListFragment extends DialogFragment implements NetworkConnection.IRCEventHandler {
    JsonNode ignores;
    int cid;
    IgnoresAdapter adapter;
    NetworkConnection conn;
    RecyclerView recyclerView;
    TextView empty;

    private class ViewHolder extends RecyclerView.ViewHolder {
        public RowHostmaskBinding binding;

        public ViewHolder(View v) {
            super(v);
            binding = DataBindingUtil.bind(v);
        }
    }

    private class IgnoresAdapter extends RecyclerView.Adapter<ViewHolder> {
        @Override
        public int getItemCount() {
            return ignores.size();
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
                    conn.unignore(cid, ignores.get(position).asText());
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    NetworkConnection.printStackTraceToCrashlytics(e);
                }
            }
        };

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = RowHostmaskBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot();
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            RowHostmaskBinding row = holder.binding;
            row.setLabel(ignores.get(position).asText());
            row.removeBtn.setOnClickListener(removeClickListener);
            row.removeBtn.setColorFilter(ColorScheme.getInstance().colorControlNormal, PorterDuff.Mode.SRC_ATOP);
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
        empty.setText("You're not ignoring anyone at the moment.\n\nYou can ignore someone by tapping their nickname in the user list, long-pressing a message, or by using /ignore.");
        if (savedInstanceState != null && savedInstanceState.containsKey("cid")) {
            cid = savedInstanceState.getInt("cid");
            ignores = ServersList.getInstance().getServer(cid).raw_ignores;
            adapter = new IgnoresAdapter();
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
                .setTitle("Ignore list for " + ServersList.getInstance().getServer(cid).getName())
                .setView(v)
                .setPositiveButton("Add Ignore Mask", new AddClickListener())
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
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
            prompt.setText("Ignore messages from this hostmask");
            builder.setTitle(server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")");
            builder.setView(view);
            builder.setPositiveButton("Ignore", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    conn.ignore(cid, input.getText().toString());
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

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putInt("cid", cid);
    }

    @Override
    public void setArguments(Bundle args) {
        cid = args.getInt("cid", 0);
        if (getActivity() != null && cid > 0 && recyclerView != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ignores = ServersList.getInstance().getServer(cid).raw_ignores;
                    adapter = new IgnoresAdapter();
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
        if (ignores == null && cid > 0) {
            ignores = ServersList.getInstance().getServer(cid).raw_ignores;
            adapter = new IgnoresAdapter();
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

    public void onIRCEvent(int what, Object obj) {
        switch (what) {
            case NetworkConnection.EVENT_MAKESERVER:
                Server s = (Server) obj;
                if (s.getCid() == cid && getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ignores = ServersList.getInstance().getServer(cid).raw_ignores;
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
                }
                break;
            case NetworkConnection.EVENT_SETIGNORES:
                IRCCloudJSONObject o = (IRCCloudJSONObject) obj;
                if (o.cid() == cid && getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ignores = ServersList.getInstance().getServer(cid).raw_ignores;
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
                }
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
