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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.databinding.RowJumpresultBinding;

import org.solovyev.android.views.llm.LinearLayoutManager;

import java.util.ArrayList;

import me.xdrop.fuzzywuzzy.FuzzySearch;

public class JumpToChannelFragment extends DialogFragment implements NetworkConnection.IRCEventHandler {
    EditText query;
    JumpToChannelFragment.ResultsAdapter adapter;
    RecyclerView recyclerView;
    View loadingArchives;
    int currentBid = -1;
    JumpToChannelListener mListener;

    @Override
    public void onIRCEvent(int message, Object object) {
        if(message == NetworkConnection.EVENT_OOB_END)
            query.post(new Runnable() {
                @Override
                public void run() {
                    adapter.filter(query.getText().toString());
                }
            });
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        public RowJumpresultBinding binding;

        public ViewHolder(View v) {
            super(v);
            binding = DataBindingUtil.bind(v);
        }
    }

    private class ResultsAdapter extends RecyclerView.Adapter<JumpToChannelFragment.ViewHolder> {
        ArrayList<Buffer> results = new ArrayList<>();

        public void filter(String query) {
            SparseArray<Server> servers = ServersList.getInstance().getServers();
            ArrayList<Buffer> active = new ArrayList<>();
            ArrayList<Buffer> inactive = new ArrayList<>();
            ArrayList<Buffer> archived = new ArrayList<>();
            Buffer current = null;
            boolean loading = false;

            results.clear();

            for (int i = 0; i < servers.size(); i++) {
                Server s = servers.valueAt(i);
                if(s.deferred_archives > 0) {
                    loading = true;
                    NetworkConnection.getInstance().request_archives(s.getCid());
                }
                for(Buffer b : BuffersList.getInstance().getBuffersForServer(s.getCid())) {
                    if(!b.isConsole()) {
                        if (FuzzySearch.weightedRatio(query.toLowerCase(), b.normalizedName()) > 50) {
                            if(b.getBid() == currentBid)
                                current = b;
                            else if(b.getArchived() == 1)
                                archived.add(b);
                            else if(b.isChannel() && !b.isJoined())
                                inactive.add(b);
                            else if(!s.getStatus().equals("connected_ready"))
                                inactive.add(b);
                            else
                                active.add(b);
                        }
                    }
                }
            }

            results.addAll(active);
            if(current != null)
                results.add(current);
            results.addAll(inactive);
            results.addAll(archived);

            loadingArchives.setVisibility(loading ? View.VISIBLE : View.GONE);

            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return results.size();
        }

        @Override
        public long getItemId(int pos) {
            return results.get(pos).getBid();
        }

        @Override
        public JumpToChannelFragment.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = RowJumpresultBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot();
            return new JumpToChannelFragment.ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(JumpToChannelFragment.ViewHolder holder, final int position) {
            final Buffer b = results.get(position);
            Server s = b.getServer();
            RowJumpresultBinding row = holder.binding;
            row.setName(b.getDisplayName());
            row.setNetwork((s.getName() != null && s.getName().length() > 0) ? s.getName() : s.getHostname());
            row.getRoot().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.onBufferSelected(b.getBid());
                    getDialog().dismiss();
                }
            });
            row.executePendingBindings();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context ctx = getActivity();
        if (ctx == null)
            return null;

        if(savedInstanceState != null && savedInstanceState.containsKey("currentBid"))
            currentBid = savedInstanceState.getInt("currentBid");

        if(savedInstanceState != null && savedInstanceState.containsKey("query")) {
            query.setText(savedInstanceState.getString("query"));
        }

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.dialog_jumptochannel, null);
        loadingArchives = v.findViewById(R.id.loadingArchives);
        query = v.findViewById(R.id.query);
        query.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                adapter.filter(editable.toString());
            }
        });

        query.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if(adapter.results.size() > 0) {
                    mListener.onBufferSelected(adapter.results.get(0).getBid());
                    getDialog().dismiss();
                }
                return false;
            }
        });

        recyclerView = v.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
        recyclerView.setVisibility(View.VISIBLE);
        adapter = new JumpToChannelFragment.ResultsAdapter();
        adapter.filter(query.getText().toString());
        recyclerView.setAdapter(adapter);

        Dialog d = new AlertDialog.Builder(ctx)
                .setTitle("Jump To Channel")
                .setView(v)
                .setNegativeButton("Close", null)
                .create();

        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                query.requestFocus();
            }
        });

        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return d;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(query != null && query.getText().length() > 0)
            outState.putString("query", query.getText().toString());

        outState.putInt("currentBid", currentBid);
    }

    @Override
    public void onResume() {
        super.onResume();
        NetworkConnection.getInstance().addHandler(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        NetworkConnection.getInstance().removeHandler(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (JumpToChannelFragment.JumpToChannelListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement JumpToChannelListener");
        }
    }

    public interface JumpToChannelListener {
        void onBufferSelected(int bid);
    }

}