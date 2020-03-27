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
import android.content.res.Resources;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.BuildConfig;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.collection.EventsList;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.model.Event;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.databinding.RowBufferBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class BuffersListFragment extends Fragment implements NetworkConnection.IRCEventHandler {
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private NetworkConnection conn;
    private BufferListAdapter adapter;
    private OnBufferSelectedListener mListener;
    private LinearLayout topUnreadIndicator = null;
    private LinearLayout topUnreadIndicatorColor = null;
    private LinearLayout topUnreadIndicatorBorder = null;
    private LinearLayout bottomUnreadIndicator = null;
    private LinearLayout bottomUnreadIndicatorColor = null;
    private LinearLayout bottomUnreadIndicatorBorder = null;
    private ProgressBar progressBar = null;
    private RefreshTask refreshTask = null;
    private boolean ready = false;
    private boolean requestingArchives = false;
    public boolean readOnly = false;

    private int firstUnreadPosition = -1;
    private int lastUnreadPosition = -1;
    private int firstHighlightPosition = -1;
    private int lastHighlightPosition = -1;
    private int firstFailurePosition = -1;
    private int lastFailurePosition = -1;

    private SparseBooleanArray mExpandArchives = new SparseBooleanArray();
    private SparseBooleanArray mExpandCids = new SparseBooleanArray();

    private int selected_bid = -1;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public RowBufferBinding binding;

        public ViewHolder(View v) {
            super(v);
            binding = DataBindingUtil.bind(v);
        }
    }

    private class BufferListAdapter extends RecyclerView.Adapter<ViewHolder> {
        ArrayList<Buffer> data;
        HashMap<String,Integer> nameCounts;

        public int positionForBid(int bid) {
            for (int i = 0; i < data.size(); i++) {
                Buffer e = data.get(i);
                if (e.getBid() == bid)
                    return i;
            }
            return -1;
        }

        public BufferListAdapter() {
            data = new ArrayList<>(BuffersList.getInstance().count() + ServersList.getInstance().count() + 10);
            nameCounts = new HashMap<>();
        }

        public void setItems(ArrayList<Buffer> items) {
            data = items;
            nameCounts = new HashMap<>();

            JSONObject prefs = NetworkConnection.getInstance().getUserInfo().prefs;
            HashSet<Integer> pinned = new HashSet<>();
            try {
                if (prefs.has("pinnedBuffers")) {
                    JSONArray pinnedBuffers = prefs.getJSONArray("pinnedBuffers");
                    if (pinnedBuffers.length() > 0) {
                        for (int i = 0; i < pinnedBuffers.length(); i++) {
                            pinned.add(pinnedBuffers.getInt(i));
                        }

                        for (Buffer b : data) {
                            if(pinned.contains(b.getBid())) {
                                int count = 1;
                                if (nameCounts.containsKey(b.getName()))
                                    count += nameCounts.get(b.getName());
                                nameCounts.put(b.getName(), count);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public synchronized void updateCollapsed(Server s) {
            ArrayList<Buffer> buffers = BuffersList.getInstance().getBuffersForServer(s.getCid());
            int collapsed_unread = 0;
            int collapsed_highlights = 0;
            for (Buffer b : buffers) {
                if (b.getArchived() == 0 || b.isConsole()) {
                    collapsed_unread += b.getUnread();
                    collapsed_highlights += b.getHighlights();
                }
            }

            s.collapsed.setUnread(collapsed_unread);
            s.collapsed.setHighlights(collapsed_highlights);

            int pos = 0;
            for(int i = 0; i < data.size(); i++) {
                Buffer b = data.get(i);
                if(b.isCollapsed() && b.getCid() == s.getCid()) {
                    pos = i;
                    if(getActivity() != null) {
                        final int p = pos;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                notifyItemChanged(p);
                            }
                        });
                    }
                    break;
                }
            }

            if(pos > 0) {
                if(collapsed_unread > 0) {
                    if (firstUnreadPosition == -1 || firstUnreadPosition > pos)
                        firstUnreadPosition = pos;
                    if (lastUnreadPosition == -1 || lastUnreadPosition < pos)
                        lastUnreadPosition = pos;
                } else {
                    if (firstUnreadPosition == pos) {
                        firstUnreadPosition = -1;
                        for (int i = 0; i < data.size(); i++) {
                            if (data.get(i).getUnread() > 0) {
                                firstUnreadPosition = i;
                                break;
                            }
                        }
                    }
                    if (lastUnreadPosition == pos) {
                        lastUnreadPosition = -1;
                        for (int i = pos; i >= 0; i--) {
                            if (data.get(i).getUnread() > 0) {
                                lastUnreadPosition = i;
                                break;
                            }
                        }
                    }
                }

                if (collapsed_highlights > 0) {
                    if (firstHighlightPosition == -1 || firstHighlightPosition > pos)
                        firstHighlightPosition = pos;
                    if (lastHighlightPosition == -1 || lastHighlightPosition < pos)
                        lastHighlightPosition = pos;
                } else {
                    if (firstHighlightPosition == pos) {
                        firstHighlightPosition = -1;
                        for (int i = 0; i < data.size(); i++) {
                            if (data.get(i).getHighlights() > 0) {
                                firstHighlightPosition = i;
                                break;
                            }
                        }
                    }
                    if (lastHighlightPosition == pos) {
                        lastHighlightPosition = -1;
                        for (int i = pos; i >= 0; i--) {
                            if (data.get(i).getHighlights() > 0) {
                                lastHighlightPosition = i;
                                break;
                            }
                        }
                    }
                }
            }
        }

        public synchronized void updateBuffer(Buffer b) {
            final int pos = positionForBid(b.getBid());
            if (pos >= 0 && data != null && pos < data.size()) {
                Buffer e = data.get(pos);
                try {
                    JSONObject prefs = NetworkConnection.getInstance().getUserInfo().prefs;
                    String pref_type = b.isChannel() ? "channel" : "buffer";
                    boolean muted = (prefs.has("notifications-mute") && prefs.get("notifications-mute") instanceof Boolean && prefs.getBoolean("notifications-mute"));
                    if (prefs.has(pref_type + "-notifications-mute")) {
                        JSONObject notifyMuteMap = prefs.getJSONObject(pref_type + "-notifications-mute");
                        if (notifyMuteMap.has(String.valueOf(b.getBid())) && notifyMuteMap.getBoolean(String.valueOf(b.getBid())))
                            muted = true;
                    }
                    if (prefs.has(pref_type + "-notifications-mute-disable")) {
                        JSONObject notifyMuteMap = prefs.getJSONObject(pref_type + "-notifications-mute-disable");
                        if (notifyMuteMap.has(String.valueOf(b.getBid())) && notifyMuteMap.getBoolean(String.valueOf(b.getBid())))
                            muted = false;
                    }
                    b.setMuted(muted);
                } catch (Exception e1) {

                }
                if(getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            notifyItemChanged(pos);
                        }
                    });
                }

                if (e.getUnread() > 0) {
                    if (firstUnreadPosition == -1 || firstUnreadPosition > pos)
                        firstUnreadPosition = pos;
                    if (lastUnreadPosition == -1 || lastUnreadPosition < pos)
                        lastUnreadPosition = pos;
                } else {
                    if (firstUnreadPosition == pos) {
                        firstUnreadPosition = -1;
                        for (int i = 0; i < data.size(); i++) {
                            if (data.get(i).getUnread() > 0) {
                                firstUnreadPosition = i;
                                break;
                            }
                        }
                    }
                    if (lastUnreadPosition == pos) {
                        lastUnreadPosition = -1;
                        for (int i = pos; i >= 0; i--) {
                            if (data.get(i).getUnread() > 0) {
                                lastUnreadPosition = i;
                                break;
                            }
                        }
                    }
                }

                if (e.getHighlights() > 0) {
                    if (firstHighlightPosition == -1 || firstHighlightPosition > pos)
                        firstHighlightPosition = pos;
                    if (lastHighlightPosition == -1 || lastHighlightPosition < pos)
                        lastHighlightPosition = pos;
                } else {
                    if (firstHighlightPosition == pos) {
                        firstHighlightPosition = -1;
                        for (int i = 0; i < data.size(); i++) {
                            if (data.get(i).getHighlights() > 0) {
                                firstHighlightPosition = i;
                                break;
                            }
                        }
                    }
                    if (lastHighlightPosition == pos) {
                        lastHighlightPosition = -1;
                        for (int i = pos; i >= 0; i--) {
                            if (data.get(i).getHighlights() > 0) {
                                lastHighlightPosition = i;
                                break;
                            }
                        }
                    }
                }

                if (e.isConsole()) {
                    Server s = ServersList.getInstance().getServer(e.getCid());
                    if (s != null) {
                        ObjectNode fail_info = s.getFail_info();

                        if (fail_info != null && fail_info.has("type")) {
                            if (firstFailurePosition == -1 || firstFailurePosition > pos)
                                firstFailurePosition = pos;
                            if (lastFailurePosition == -1 || lastFailurePosition < pos)
                                lastFailurePosition = pos;
                        } else {
                            if (firstFailurePosition == pos) {
                                firstFailurePosition = -1;
                                for (int i = 0; i < data.size(); i++) {
                                    Buffer j = data.get(i);
                                    s = j.getServer();
                                    if (j.isConsole() && s != null) {
                                        fail_info = s.getFail_info();
                                        if (fail_info != null && fail_info.has("type")) {
                                            firstFailurePosition = i;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (lastFailurePosition == pos) {
                                lastFailurePosition = -1;
                                for (int i = pos; i >= 0; i--) {
                                    Buffer j = data.get(i);
                                    s = j.getServer();
                                    if (j.isConsole() && s != null) {
                                        fail_info = s.getFail_info();
                                        if (fail_info != null && fail_info.has("type")) {
                                            lastFailurePosition = i;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (layoutManager != null)
                                updateUnreadIndicators(layoutManager.findFirstVisibleItemPosition(), layoutManager.findLastVisibleItemPosition());
                        }
                    });
                }
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refresh();
                        }
                    });
                }
            }
        }

        int unreadPositionAbove(int pos) {
            if (pos > 0) {
                for (int i = pos - 1; i >= 0; i--) {
                    Buffer e = data.get(i);
                    if (e.getUnread() > 0 || e.getHighlights() > 0 || (e.isConsole() && e.getServer().isFailed()))
                        return i;
                }
            }
            return 0;
        }

        int unreadPositionBelow(int pos) {
            if (pos >= 0) {
                for (int i = pos; i < data.size(); i++) {
                    Buffer e = data.get(i);
                    if (e.getUnread() > 0 || e.getHighlights() > 0 || (e.isConsole() && e.getServer().isFailed()))
                        return i;
                }
            }
            return data.size() - 1;
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public Object getItem(int position) {
            if (position < data.size())
                return data.get(position);
            else
                return null;
        }

        @Override
        public long getItemId(int position) {
            if (position < data.size()) {
                Buffer e = data.get(position);
                return e.getBid();
            } else {
                return -1;
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = RowBufferBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot();
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            final Buffer b = data.get(position);
            int count = 0;
            if(nameCounts.containsKey(b.getName()))
                count = nameCounts.get(b.getName());
            b.showServerSuffix(count > 1);
            RowBufferBinding row = holder.binding;

            row.setBuffer(b);
            row.setServer(b.getServer());
            row.setSelected(selected_bid);
            row.setReadOnly(readOnly);
            if(b.getType().equals(Buffer.TYPE_ARCHIVES_HEADER))
                row.setShowSpinner(requestingArchives && b.getArchived() > 0 && b.getServer() != null && b.getServer().deferred_archives > 0);
            else
                row.setShowSpinner(!readOnly && b.getShowSpinner());

            row.getRoot().setContentDescription(b.getContentDescription());

            row.getRoot().setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (b.getType()) {
                        case Buffer.TYPE_ARCHIVES_HEADER:
                            mExpandArchives.put(b.getCid(), !mExpandArchives.get(b.getCid(), false));
                            if(mExpandArchives.get(b.getCid(), false) && b.getServer() != null && b.getServer().deferred_archives > 0) {
                                requestingArchives = true;
                                NetworkConnection.getInstance().request_archives(b.getCid());
                            }
                            refresh();
                            return;
                        case Buffer.TYPE_JOIN_CHANNEL:
                            AddChannelFragment newFragment = new AddChannelFragment();
                            newFragment.setDefaultCid(b.getCid());
                            newFragment.show(getActivity().getSupportFragmentManager(), "dialog");
                            mListener.addButtonPressed(b.getCid());
                            return;
                        case Buffer.TYPE_SPAM:
                            SpamFragment spamFragment = new SpamFragment();
                            spamFragment.setCid(b.getCid());
                            spamFragment.show(getActivity().getSupportFragmentManager(), "spam");
                            return;
                        case Buffer.TYPE_COLLAPSED:
                            if(b.getArchived() > 0) {
                                mExpandCids.put(b.getCid(), true);
                                NetworkConnection.getInstance().unarchiveBuffer(b.getCid(), b.getBid(), null);
                            } else {
                                mExpandCids.delete(b.getCid());
                                NetworkConnection.getInstance().archiveBuffer(b.getCid(), b.getBid(), null);
                            }
                            return;
                        case Buffer.TYPE_PINNED:
                            return;
                    }
                    mListener.onBufferSelected(b.getBid());
                }
            });

            row.getRoot().setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    return (b.isChannel() || b.isConversation() || b.isConsole()) && mListener.onBufferLongClicked(b);
                }
            });

            row.executePendingBindings();
        }
    }

    private class RefreshTask extends AsyncTaskEx<Void, Void, Void> {
        private ArrayList<Buffer> entries = new ArrayList<>();
        private boolean shouldScroll = false;

        private void add(int position, Buffer b, Server s) {
            entries.add(position,b);
            if (b.getUnread() > 0 && firstUnreadPosition == -1)
                firstUnreadPosition = position;
            if (b.getUnread() > 0 && (lastUnreadPosition == -1 || lastUnreadPosition < position))
                lastUnreadPosition = position;
            if (b.getHighlights() > 0 && firstHighlightPosition == -1)
                firstHighlightPosition = position;
            if (b.getHighlights() > 0 && (lastHighlightPosition == -1 || lastHighlightPosition < position))
                lastHighlightPosition = position;
            if(s != null) {
                if (b.isConsole() && s.isFailed() && firstFailurePosition == -1)
                    firstFailurePosition = position;
                if (b.isConsole() && s.isFailed() && (lastFailurePosition == -1 || lastFailurePosition < position))
                    lastFailurePosition = position;
            }
        }

        @Override
        protected synchronized Void doInBackground(Void... params) {
            if (!NetworkConnection.getInstance().ready || !ready || isCancelled() || NetworkConnection.getInstance().getUserInfo() == null) {
                Crashlytics.log(Log.WARN, "IRCCloud", "BuffersListFragment not ready or cancelled " + ready + " " + isCancelled());
                return null;
            }

            JSONObject prefs = NetworkConnection.getInstance().getUserInfo().prefs;
            SparseArray<Server> serversArray = ServersList.getInstance().getServers();
            ArrayList<Server> servers = new ArrayList<>();

            for (int i = 0; i < serversArray.size(); i++) {
                Server s = serversArray.valueAt(i);
                if(s != null)
                    servers.add(s);
            }
            Collections.sort(servers);
            if (adapter == null) {
                Crashlytics.log(Log.DEBUG, "IRCCloud", "Created new BufferListAdapter");
                adapter = new BufferListAdapter();
            }

            if(adapter.getItemCount() == 0)
                shouldScroll = true;

            firstUnreadPosition = -1;
            lastUnreadPosition = -1;
            firstHighlightPosition = -1;
            lastHighlightPosition = -1;
            firstFailurePosition = -1;
            lastFailurePosition = -1;
            int position = 0;

            HashSet<Integer> pinned = new HashSet<>();
            try {
                if (prefs.has("pinnedBuffers")) {
                    JSONArray pinnedBuffers = prefs.getJSONArray("pinnedBuffers");
                    if(pinnedBuffers.length() > 0) {
                        for (int i = 0; i < pinnedBuffers.length(); i++) {
                            Buffer b = BuffersList.getInstance().getBuffer(pinnedBuffers.getInt(i));
                            if (b != null && b.getArchived() == 0) {
                                if(pinned.size() == 0) {
                                    Buffer heading = new Buffer();
                                    heading.setName("Pinned");
                                    heading.setType(Buffer.TYPE_PINNED);
                                    add(position,heading,null);
                                    position++;
                                }
                                pinned.add(b.getBid());
                                add(position,b,b.getServer());
                                position++;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            for (Server s : servers) {
                if (isCancelled())
                    return null;

                int spamCount = 0;
                int archiveCount = s.deferred_archives;
                Buffer collapsed = null;
                int collapsed_unread = 0;
                int collapsed_highlights = 0;
                int collapsed_row = 0;
                ArrayList<Buffer> buffers = BuffersList.getInstance().getBuffersForServer(s.getCid());
                for (Buffer b : buffers) {
                    if (isCancelled())
                        return null;

                    if(pinned.contains(b.getBid()))
                        continue;

                    try {
                        String pref_type = b.isChannel() ? "channel" : "buffer";
                        boolean muted = (prefs.has("notifications-mute") && prefs.get("notifications-mute") instanceof Boolean && prefs.getBoolean("notifications-mute"));
                        if (prefs.has(pref_type + "-notifications-mute")) {
                            JSONObject notifyMuteMap = prefs.getJSONObject(pref_type + "-notifications-mute");
                            if (notifyMuteMap.has(String.valueOf(b.getBid())) && notifyMuteMap.getBoolean(String.valueOf(b.getBid())))
                                muted = true;
                        }
                        if (prefs.has(pref_type + "-notifications-mute-disable")) {
                            JSONObject notifyMuteMap = prefs.getJSONObject(pref_type + "-notifications-mute-disable");
                            if (notifyMuteMap.has(String.valueOf(b.getBid())) && notifyMuteMap.getBoolean(String.valueOf(b.getBid())))
                                muted = false;
                        }
                        b.setMuted(muted);
                    } catch (Exception e) {

                    }

                    if (b.getArchived() == 0 || b.isConsole()) {
                        if(collapsed != null && !mExpandCids.get(b.getCid(), false) && b.getBid() != selected_bid) {
                            collapsed_unread += b.getUnread();
                            collapsed_highlights += b.getHighlights();
                        } else {
                            add(position,b,s);
                            if (!readOnly && b.isConversation() && b.getUnread() > 0 && EventsList.getInstance().getSizeOfBuffer(b.getBid()) == 1)
                                spamCount++;
                            position++;
                        }

                        if(!BuildConfig.ENTERPRISE) {
                            if (b.isConsole() && (b.getArchived() == 1 || mExpandCids.get(s.getCid(), false))) {
                                collapsed = new Buffer();
                                collapsed.setCid(s.getCid());
                                collapsed.setBid(b.getBid());
                                collapsed.setName(mExpandCids.get(s.getCid(), false) ? "Collapse" : "Expand");
                                collapsed.setArchived(b.getArchived());
                                collapsed.setType(Buffer.TYPE_COLLAPSED);
                                s.collapsed = collapsed;
                                collapsed_row = position;
                            }
                        }
                    } else if(!b.isConsole()) {
                        archiveCount++;
                    }
                }
                if(collapsed != null) {
                    collapsed.setUnread(collapsed_unread);
                    collapsed.setHighlights(collapsed_highlights);
                    add(collapsed_row, collapsed, s);
                    position++;
                }
                if (collapsed == null || mExpandCids.get(s.getCid(), false)) {
                    if (spamCount > 3) {
                        Buffer spam = new Buffer();
                        spam.setCid(s.getCid());
                        spam.setName("Spam detected");
                        spam.setType(Buffer.TYPE_SPAM);
                        for (int i = 0; i < entries.size(); i++) {
                            Buffer b = entries.get(i);
                            if (b.getCid() == spam.getCid() && b.isConversation()) {
                                entries.add(i, spam);
                                break;
                            }
                        }
                        position++;
                    }
                    if (archiveCount > 0) {
                        Buffer header = new Buffer();
                        header.setCid(s.getCid());
                        header.setName("Archives");
                        header.setType(Buffer.TYPE_ARCHIVES_HEADER);
                        header.setArchived(mExpandArchives.get(s.getCid(), false) ? 1 : 0);
                        entries.add(header);
                        position++;
                        if (mExpandArchives.get(s.getCid(), false)) {
                            for (Buffer b : buffers) {
                                if (b.getArchived() == 1) {
                                    entries.add(b);
                                    position++;
                                }
                            }
                        }
                    }
                    if (buffers.size() == 1 && s.getStatus().equals("connected_ready") && !readOnly && archiveCount == 0) {
                        Buffer join = new Buffer();
                        join.setCid(s.getCid());
                        join.setName("Join a Channel");
                        join.setType(Buffer.TYPE_JOIN_CHANNEL);
                        entries.add(join);
                    }
                }
            }

            Crashlytics.log(Log.DEBUG, "IRCCloud", "Buffers list adapter contains " + entries.size() + " entries");
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (isCancelled()) {
                Crashlytics.log(Log.WARN, "IRCCloud", "BuffersListFragment: OnPostExecute: This refresh task was cancelled");
                return;
            }

            refreshTask = null;

            if (adapter == null)
                return;

            adapter.setItems(entries);

            if (recyclerView.getAdapter() == null && entries.size() > 0) {
                recyclerView.setAdapter(adapter);
            } else
                adapter.notifyDataSetChanged();

            if (layoutManager != null)
                updateUnreadIndicators(layoutManager.findFirstVisibleItemPosition(), layoutManager.findLastVisibleItemPosition());
            else {//The activity view isn't ready yet, try again
                Crashlytics.log(Log.WARN, "IRCCloud", "BuffersListFragment: OnPostExecute: The activity isn't ready yet, will retry");
                refreshTask = new RefreshTask();
                refreshTask.execute((Void) null);
            }
            progressBar.setVisibility(View.GONE);
            if(shouldScroll)
                setSelectedBid(selected_bid);
        }
    }

    public void setSelectedBid(final int bid) {
        int last_bid = selected_bid;
        selected_bid = bid;
        if (adapter != null) {
            Buffer b = BuffersList.getInstance().getBuffer(last_bid);
            if (b != null)
                adapter.updateBuffer(b);
            b = BuffersList.getInstance().getBuffer(bid);
            if (b != null)
                adapter.updateBuffer(b);

            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    int pos = adapter.positionForBid(bid);
                    if(pos < layoutManager.findFirstCompletelyVisibleItemPosition() || pos > layoutManager.findLastCompletelyVisibleItemPosition()) {
                        int center = (layoutManager.findLastCompletelyVisibleItemPosition() - layoutManager.findFirstCompletelyVisibleItemPosition())/2;
                        if(pos+center < adapter.getItemCount())
                            pos += center;
                        else
                            pos = adapter.getItemCount() - 1;
                        recyclerView.scrollToPosition(pos);
                    }
                }
            });
        } else {
            Crashlytics.log(Log.WARN, "IRCCloud", "BufferListFragment: Request to set BID but I don't have an adapter yet, refreshing");
            refreshTask = new RefreshTask();
            refreshTask.doInBackground((Void) null);
            if(!refreshTask.isCancelled())
                refreshTask.onPostExecute(null);
            Crashlytics.log(Log.DEBUG, "IRCCloud", "Done");
        }
    }

    private void updateUnreadIndicators(int first, int last) {
        if (readOnly) {
            if (topUnreadIndicator != null)
                topUnreadIndicator.setVisibility(View.GONE);
            if (bottomUnreadIndicator != null)
                bottomUnreadIndicator.setVisibility(View.GONE);
        } else {
            if (topUnreadIndicator != null) {
                if (firstFailurePosition != -1 && first > firstFailurePosition) {
                    topUnreadIndicator.setVisibility(View.VISIBLE);
                    topUnreadIndicatorColor.setBackgroundResource(R.drawable.network_fail_bg);
                    topUnreadIndicatorBorder.setBackgroundResource(R.drawable.networkErrorBorder);
                } else {
                    topUnreadIndicator.setVisibility(View.GONE);
                }
                if (firstUnreadPosition != -1 && first > firstUnreadPosition) {
                    topUnreadIndicator.setVisibility(View.VISIBLE);
                    topUnreadIndicatorColor.setBackgroundResource(R.drawable.row_unread_border);
                    topUnreadIndicatorBorder.setBackgroundResource(R.drawable.unreadBorder);
                }
                if ((lastHighlightPosition != -1 && first > lastHighlightPosition) ||
                        (firstHighlightPosition != -1 && first > firstHighlightPosition)) {
                    topUnreadIndicator.setVisibility(View.VISIBLE);
                    topUnreadIndicatorColor.setBackgroundResource(R.drawable.highlight_red);
                    topUnreadIndicatorBorder.setBackgroundResource(R.drawable.highlightBorder);
                }
            }
            if (bottomUnreadIndicator != null) {
                if (lastFailurePosition != -1 && last < lastFailurePosition) {
                    bottomUnreadIndicator.setVisibility(View.VISIBLE);
                    bottomUnreadIndicatorColor.setBackgroundResource(R.drawable.network_fail_bg);
                    bottomUnreadIndicatorBorder.setBackgroundResource(R.drawable.networkErrorBorder);
                } else {
                    bottomUnreadIndicator.setVisibility(View.GONE);
                }
                if (lastUnreadPosition != -1 && last < lastUnreadPosition) {
                    bottomUnreadIndicator.setVisibility(View.VISIBLE);
                    bottomUnreadIndicatorColor.setBackgroundResource(R.drawable.row_unread_border);
                    bottomUnreadIndicatorBorder.setBackgroundResource(R.drawable.unreadBorder);
                }
                if ((firstHighlightPosition != -1 && last < firstHighlightPosition) ||
                        (lastHighlightPosition != -1 && last < lastHighlightPosition)) {
                    bottomUnreadIndicator.setVisibility(View.VISIBLE);
                    bottomUnreadIndicatorColor.setBackgroundResource(R.drawable.highlight_red);
                    bottomUnreadIndicatorBorder.setBackgroundResource(R.drawable.highlightBorder);
                }
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        conn = NetworkConnection.getInstance();
        conn.addHandler(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bufferslist, null);
        progressBar = view.findViewById(R.id.bufferprogress);
        recyclerView = view.findViewById(android.R.id.list);
        layoutManager = new LinearLayoutManager(view.getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);
        topUnreadIndicator = view.findViewById(R.id.topUnreadIndicator);
        topUnreadIndicator.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(adapter != null) {
                    try {
                        int scrollTo = adapter.unreadPositionAbove(layoutManager.findFirstVisibleItemPosition()) - 1;
                        if (scrollTo > 0)
                            recyclerView.smoothScrollToPosition(scrollTo);
                        else
                            recyclerView.smoothScrollToPosition(0);
                    } catch (Exception e) {
                    }
                    updateUnreadIndicators(layoutManager.findFirstVisibleItemPosition(), layoutManager.findLastVisibleItemPosition());
                }
            }

        });
        topUnreadIndicatorColor = view.findViewById(R.id.topUnreadIndicatorColor);
        topUnreadIndicatorBorder = view.findViewById(R.id.topUnreadIndicatorBorder);
        bottomUnreadIndicator = view.findViewById(R.id.bottomUnreadIndicator);
        bottomUnreadIndicator.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(adapter != null) {
                    try {
                        int scrollTo = adapter.unreadPositionBelow(layoutManager.findLastVisibleItemPosition()) + 1;
                        if (scrollTo < adapter.getItemCount())
                            recyclerView.smoothScrollToPosition(scrollTo);
                        else
                            recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
                    } catch (Exception e) {
                    }
                    updateUnreadIndicators(layoutManager.findFirstVisibleItemPosition(), layoutManager.findLastVisibleItemPosition());
                }
            }

        });
        bottomUnreadIndicatorColor = view.findViewById(R.id.bottomUnreadIndicatorColor);
        bottomUnreadIndicatorBorder = view.findViewById(R.id.bottomUnreadIndicatorBorder);
        recyclerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                updateUnreadIndicators(layoutManager.findFirstVisibleItemPosition(), layoutManager.findLastVisibleItemPosition());
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            }
        });

        ready = NetworkConnection.getInstance().ready;

        if (ready) {
            if (savedInstanceState != null && savedInstanceState.containsKey("expandedArchives")) {
                ArrayList<Integer> expandedArchives = savedInstanceState.getIntegerArrayList("expandedArchives");
                Iterator<Integer> i = expandedArchives.iterator();
                while (i.hasNext()) {
                    Integer cid = i.next();
                    mExpandArchives.put(cid, true);
                }
            }
            refreshTask = new RefreshTask();
            refreshTask.doInBackground((Void) null);
            if(!refreshTask.isCancelled())
                refreshTask.onPostExecute(null);
            if (savedInstanceState != null && savedInstanceState.containsKey("scrollPosition"))
                recyclerView.scrollToPosition(savedInstanceState.getInt("scrollPosition"));
        }
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        if (adapter != null && adapter.data != null && adapter.data.size() > 0) {
            ArrayList<Integer> expandedArchives = new ArrayList<>();
            SparseArray<Server> servers = ServersList.getInstance().getServers();
            for (int i = 0; i < servers.size(); i++) {
                Server s = servers.valueAt(i);
                if (s != null && mExpandArchives.get(s.getCid(), false))
                    expandedArchives.add(s.getCid());
            }
            state.putIntegerArrayList("expandedArchives", expandedArchives);
            if (recyclerView != null)
                state.putInt("scrollPosition", layoutManager.findFirstVisibleItemPosition());
        }
    }

    public void onResume() {
        super.onResume();
        ready = conn.ready;
        refresh();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if (conn != null)
            conn.removeHandler(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnBufferSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnBufferSelectedListener");
        }
    }

    public void onIRCEvent(int what, Object obj) {
        Buffer b;
        IRCCloudJSONObject object;
        Event event;
        switch (what) {
            case NetworkConnection.EVENT_BUFFERARCHIVED:
                b = BuffersList.getInstance().getBuffer((int)obj);
                if(b != null && b.isConsole())
                    mExpandCids.delete(b.getCid());
                if(getActivity() != null)
                    getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refresh();
                    }
                });
                break;
            case NetworkConnection.EVENT_CHANNELMODE:
                object = (IRCCloudJSONObject) obj;
                b = BuffersList.getInstance().getBuffer(object.bid());
                if (b != null && adapter != null)
                    adapter.updateBuffer(b);
                break;
            case NetworkConnection.EVENT_BUFFERMSG:
                if (adapter != null) {
                    event = (Event) obj;
                    if (event.bid != selected_bid) {
                        Server s = ServersList.getInstance().getServer(event.cid);
                        if(s != null && s.collapsed != null && !mExpandCids.get(event.cid, false)) {
                            adapter.updateCollapsed(s);
                        } else {
                            b = BuffersList.getInstance().getBuffer(event.bid);
                            if (b != null && event.isImportant(b.getType()))
                                adapter.updateBuffer(b);
                        }
                    }
                }
                break;
            case NetworkConnection.EVENT_HEARTBEATECHO:
                if (adapter != null) {
                    object = (IRCCloudJSONObject) obj;
                    JsonNode seenEids = object.getJsonNode("seenEids");
                    Iterator<Map.Entry<String, JsonNode>> iterator = seenEids.fields();
                    int count = 0;
                    while (iterator.hasNext()) {
                        Map.Entry<String, JsonNode> entry = iterator.next();
                        JsonNode eids = entry.getValue();
                        Iterator<Map.Entry<String, JsonNode>> j = eids.fields();
                        while (j.hasNext()) {
                            Map.Entry<String, JsonNode> eidentry = j.next();
                            Integer bid = Integer.valueOf(eidentry.getKey());
                            b = BuffersList.getInstance().getBuffer(bid);
                            if (b != null)
                                adapter.updateBuffer(b);
                            count++;
                        }
                    }
                    if (count > 1) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refresh();
                            }
                        });
                    }
                }
                break;
            case NetworkConnection.EVENT_JOIN:
            case NetworkConnection.EVENT_PART:
            case NetworkConnection.EVENT_QUIT:
            case NetworkConnection.EVENT_KICK:
                object = (IRCCloudJSONObject) obj;
                if (object.type().startsWith("you_") && getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refresh();
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_CHANNELTOPIC:
            case NetworkConnection.EVENT_NICKCHANGE:
            case NetworkConnection.EVENT_MEMBERUPDATES:
            case NetworkConnection.EVENT_USERCHANNELMODE:
            case NetworkConnection.EVENT_AWAY:
            case NetworkConnection.EVENT_SELFBACK:
            case NetworkConnection.EVENT_CHANNELTIMESTAMP:
            case NetworkConnection.EVENT_USERMODE:
            case NetworkConnection.EVENT_SETIGNORES:
            case NetworkConnection.EVENT_BADCHANNELKEY:
            case NetworkConnection.EVENT_OPENBUFFER:
            case NetworkConnection.EVENT_BANLIST:
            case NetworkConnection.EVENT_WHOLIST:
            case NetworkConnection.EVENT_WHOIS:
            case NetworkConnection.EVENT_LINKCHANNEL:
            case NetworkConnection.EVENT_LISTRESPONSEFETCHING:
            case NetworkConnection.EVENT_LISTRESPONSE:
            case NetworkConnection.EVENT_LISTRESPONSETOOMANY:
            case NetworkConnection.EVENT_GLOBALMSG:
            case NetworkConnection.EVENT_ACCEPTLIST:
            case NetworkConnection.EVENT_NAMESLIST:
            case NetworkConnection.EVENT_CHANNELTOPICIS:
            case NetworkConnection.EVENT_BACKLOG_FAILED:
            case NetworkConnection.EVENT_PROGRESS:
            case NetworkConnection.EVENT_ALERT:
            case NetworkConnection.EVENT_DEBUG:
            case NetworkConnection.EVENT_CONNECTIVITY:
            case NetworkConnection.EVENT_CACHE_START:
            case NetworkConnection.EVENT_OOB_START:
            case NetworkConnection.EVENT_OOB_FAILED:
            case NetworkConnection.EVENT_DISPLAYNAMECHANGE:
            case NetworkConnection.EVENT_AVATARCHANGE:
            case NetworkConnection.EVENT_MESSAGECHANGE:
                break;
            case NetworkConnection.EVENT_BACKLOG_START:
                if (refreshTask != null)
                    refreshTask.cancel(true);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility((adapter == null || adapter.getItemCount() == 0) ? View.VISIBLE : View.GONE);
                    }
                });
                break;
            case NetworkConnection.EVENT_BACKLOG_END:
            case NetworkConnection.EVENT_CACHE_END:
                if(!requestingArchives && obj == null)
                    mExpandArchives.clear();
                requestingArchives = false;
                ready = true;
                Integer bid = (obj == null)?-1:((Integer) obj);
                b = BuffersList.getInstance().getBuffer(bid);
                if (obj != null && adapter != null && b != null) {
                    adapter.updateBuffer(b);
                    break;
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (refreshTask != null)
                                    refreshTask.cancel(true);
                                Crashlytics.log(Log.DEBUG, "IRCCloud", "Refreshing buffers list");
                                refreshTask = new RefreshTask();
                                refreshTask.doInBackground((Void) null);
                                if(!refreshTask.isCancelled())
                                    refreshTask.onPostExecute(null);
                                Crashlytics.log(Log.DEBUG, "IRCCloud", "Done");
                            }
                        });
                    }
                }
                break;
            default:
                Crashlytics.log(Log.WARN, "IRCCloud", "Slow event: " + what);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refresh();
                        }
                    });
                }
                break;
        }
    }

    public void refresh() {
        if (refreshTask != null)
            refreshTask.cancel(true);
        refreshTask = new RefreshTask();
        refreshTask.execute((Void) null);
    }

    public void prev(boolean unread) {
        int pos = adapter.positionForBid(selected_bid);
        if(unread)
            pos = adapter.unreadPositionAbove(pos);
        else
            pos--;

        Buffer b;
        do {
            if(pos >= 0)
                b = (Buffer) adapter.getItem(pos);
            else
                b = null;
            pos--;
        } while(b != null && (b.getType().equals(Buffer.TYPE_ARCHIVES_HEADER) || b.getType().equals(Buffer.TYPE_JOIN_CHANNEL)));
        if(b != null)
            mListener.onBufferSelected(b.getBid());
    }

    public void next(boolean unread) {
        int pos = adapter.positionForBid(selected_bid);
        if(unread)
            pos = adapter.unreadPositionBelow(pos);
        else
            pos++;

        Buffer b;
        do {
            if(pos < adapter.getItemCount())
                b = (Buffer) adapter.getItem(pos);
            else
                b = null;
            pos++;
        } while(b != null && (b.getType().equals(Buffer.TYPE_ARCHIVES_HEADER) || b.getType().equals(Buffer.TYPE_JOIN_CHANNEL)));
        if(b != null)
            mListener.onBufferSelected(b.getBid());
    }

    public Resources getSafeResources() {
        return IRCCloudApplication.getInstance().getApplicationContext().getResources();
    }

    public interface OnBufferSelectedListener {
        void onBufferSelected(int bid);

        boolean onBufferLongClicked(Buffer b);

        void addButtonPressed(int cid);

        void addNetwork();

        void reorder();
    }
}
