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
import android.content.Context;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.BR;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.model.Event;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.databinding.RowBufferBinding;
import com.squareup.leakcanary.RefWatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class BuffersListFragment extends ListFragment implements NetworkConnection.IRCEventHandler {
    NetworkConnection conn;
    BufferListAdapter adapter;
    OnBufferSelectedListener mListener;
    View view;
    ListView listView = null;
    LinearLayout topUnreadIndicator = null;
    LinearLayout topUnreadIndicatorColor = null;
    LinearLayout topUnreadIndicatorBorder = null;
    LinearLayout bottomUnreadIndicator = null;
    LinearLayout bottomUnreadIndicatorColor = null;
    LinearLayout bottomUnreadIndicatorBorder = null;
    RefreshTask refreshTask = null;
    private boolean ready = false;
    public boolean readOnly = false;

    int firstUnreadPosition = -1;
    int lastUnreadPosition = -1;
    int firstHighlightPosition = -1;
    int lastHighlightPosition = -1;
    int firstFailurePosition = -1;
    int lastFailurePosition = -1;

    SparseBooleanArray mExpandArchives = new SparseBooleanArray();

    public static class SelectedBID extends BaseObservable {
        private int selected_bid = -1;

        @Bindable
        public int getSelectedBID() {
            return this.selected_bid;
        }

        public void setSelectedBID(int bid) {
            this.selected_bid = bid;
            notifyPropertyChanged(BR.selectedBID);
        }
    }

    private SelectedBID selected_bid = new SelectedBID();

    private class BufferListAdapter extends BaseAdapter {
        ArrayList<Buffer> data;

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
        }

        public void setItems(ArrayList<Buffer> items) {
            data = items;
        }

        public void updateBuffer(Buffer b) {
            int pos = positionForBid(b.getBid());
            if (pos >= 0 && data != null && pos < data.size()) {
                Buffer e = data.get(pos);

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

                /*if (e.type == TYPE_SERVER) {
                    if (e.fail_info != null && e.fail_info.has("type")) {
                        if (firstFailurePosition == -1 || firstFailurePosition > pos)
                            firstFailurePosition = pos;
                        if (lastFailurePosition == -1 || lastFailurePosition < pos)
                            lastFailurePosition = pos;
                    } else {
                        if (firstFailurePosition == pos) {
                            firstFailurePosition = -1;
                            for (int i = 0; i < data.size(); i++) {
                                BufferListEntry j = data.get(i);
                                if (j.type == TYPE_SERVER && j.fail_info != null && j.fail_info.has("type")) {
                                    firstFailurePosition = i;
                                    break;
                                }
                            }
                        }
                        if (lastFailurePosition == pos) {
                            lastFailurePosition = -1;
                            for (int i = pos; i >= 0; i--) {
                                BufferListEntry j = data.get(i);
                                if (j.type == TYPE_SERVER && j.fail_info != null && j.fail_info.has("type")) {
                                    lastFailurePosition = i;
                                    break;
                                }
                            }
                        }
                    }
                }*/

                if (BuffersListFragment.this.getActivity() != null) {
                    BuffersListFragment.this.getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (listView != null)
                                updateUnreadIndicators(listView.getFirstVisiblePosition(), listView.getLastVisiblePosition());
                        }
                    });
                }
            } else {
                if (BuffersListFragment.this.getActivity() != null) {
                    BuffersListFragment.this.getActivity().runOnUiThread(new Runnable() {
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
                    if (e.getUnread() > 0 || e.getHighlights() > 0 /*|| (e.isConsole() && e.fail_info != null && e.fail_info.has("type"))*/)
                        return i;
                }
            }
            return 0;
        }

        int unreadPositionBelow(int pos) {
            if (pos >= 0) {
                for (int i = pos; i < data.size(); i++) {
                    Buffer e = data.get(i);
                    if (e.getUnread() > 0 || e.getHighlights() > 0 /*|| (e.type == TYPE_SERVER && e.fail_info != null && e.fail_info.has("type"))*/)
                        return i;
                }
            }
            return data.size() - 1;
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
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

        private OnClickListener addClickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Buffer e = (Buffer) v.getTag();
                AddChannelFragment newFragment = new AddChannelFragment();
                newFragment.setDefaultCid(e.getCid());
                newFragment.show(getActivity().getSupportFragmentManager(), "dialog");
                mListener.addButtonPressed(e.getCid());
            }
        };

        @SuppressWarnings("deprecation")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Buffer b = data.get(position);
            Server s = ServersList.getInstance().getServer(b.getCid());
            RowBufferBinding row;

            if(convertView == null) {
                row = RowBufferBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                convertView = row.getRoot();
            } else {
                row = DataBindingUtil.getBinding(convertView);
            }

            row.setBuffer(b);
            row.setServer(s);
            row.setSelected(selected_bid);
            row.setReadOnly(readOnly);
            row.executePendingBindings();

            row.addBtn.setTag(b);
            row.addBtn.setOnClickListener(addClickListener);

            return convertView;
        }
    }

    private class RefreshTask extends AsyncTaskEx<Void, Void, Void> {
        ArrayList<Buffer> entries = new ArrayList<>();

        @Override
        protected synchronized Void doInBackground(Void... params) {
            if (!ready || isCancelled()) {
                Crashlytics.log(Log.WARN, "IRCCloud", "BuffersListFragment not ready or cancelled");
                return null;
            }

            SparseArray<Server> serversArray = ServersList.getInstance().getServers();
            ArrayList<Server> servers = new ArrayList<>();

            for (int i = 0; i < serversArray.size(); i++) {
                servers.add(serversArray.valueAt(i));
            }
            Collections.sort(servers);
            if (adapter == null) {
                Crashlytics.log(Log.DEBUG, "IRCCloud", "Created new BufferListAdapter");
                adapter = new BufferListAdapter();
            }

            firstUnreadPosition = -1;
            lastUnreadPosition = -1;
            firstHighlightPosition = -1;
            lastHighlightPosition = -1;
            firstFailurePosition = -1;
            lastFailurePosition = -1;
            int position = 0;

            for (Server s : servers) {
                if (isCancelled())
                    return null;

                int archiveCount = 0;
                ArrayList<Buffer> buffers = BuffersList.getInstance().getBuffersForServer(s.getCid());
                for (Buffer b : buffers) {
                    if (isCancelled())
                        return null;

                    if (b.getArchived() == 0) {
                        entries.add(b);
                        if (b.getUnread() > 0 && firstUnreadPosition == -1)
                            firstUnreadPosition = position;
                        if (b.getUnread() > 0 && (lastUnreadPosition == -1 || lastUnreadPosition < position))
                            lastUnreadPosition = position;
                        if (b.getHighlights() > 0 && firstHighlightPosition == -1)
                            firstHighlightPosition = position;
                        if (b.getHighlights() > 0 && (lastHighlightPosition == -1 || lastHighlightPosition < position))
                            lastHighlightPosition = position;
                        position++;
                    } else {
                        archiveCount++;
                    }
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
                if (buffers.size() == 1 && !readOnly) {
                    Buffer join = new Buffer();
                    join.setCid(s.getCid());
                    join.setName("Join a Channel");
                    join.setType(Buffer.TYPE_JOIN_CHANNEL);
                    entries.add(join);
                }
            }

            if (!readOnly) {
                Buffer b = new Buffer();
                b.setName("Add a network");
                b.setType(Buffer.TYPE_ADD_NETWORK);
                entries.add(b);
                b = new Buffer();
                b.setName("Reorder");
                b.setType(Buffer.TYPE_REORDER);
                entries.add(b);
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

            if (getListAdapter() == null && entries.size() > 0) {
                setListAdapter(adapter);
            } else
                adapter.notifyDataSetInvalidated();

            if (listView != null)
                updateUnreadIndicators(listView.getFirstVisiblePosition(), listView.getLastVisiblePosition());
            else {//The activity view isn't ready yet, try again
                Crashlytics.log(Log.WARN, "IRCCloud", "BuffersListFragment: OnPostExecute: The activity isn't ready yet, will retry");
                refreshTask = new RefreshTask();
                refreshTask.execute((Void) null);
            }
        }
    }

    public void setSelectedBid(int bid) {
        int last_bid = selected_bid.getSelectedBID();
        selected_bid.setSelectedBID(bid);
        if (adapter != null) {
            Buffer b = BuffersList.getInstance().getBuffer(last_bid);
            if (b != null)
                adapter.updateBuffer(b);
            b = BuffersList.getInstance().getBuffer(bid);
            if (b != null)
                adapter.updateBuffer(b);
        } else {
            Crashlytics.log(Log.WARN, "IRCCloud", "BufferListFragment: Request to set BID but I don't have an adapter yet, refreshing");
            RefreshTask t = new RefreshTask();
            t.doInBackground((Void) null);
            t.onPostExecute(null);
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
                    topUnreadIndicatorColor.setBackgroundResource(R.drawable.selected_blue);
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
                    bottomUnreadIndicatorColor.setBackgroundResource(R.drawable.selected_blue);
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

    @SuppressWarnings("unchecked")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        conn = NetworkConnection.getInstance();
        view = inflater.inflate(R.layout.bufferslist, null);
        topUnreadIndicator = (LinearLayout) view.findViewById(R.id.topUnreadIndicator);
        topUnreadIndicator.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                int scrollTo = adapter.unreadPositionAbove(getListView().getFirstVisiblePosition());
                if (scrollTo > 0)
                    getListView().setSelection(scrollTo - 1);
                else
                    getListView().setSelection(0);

                updateUnreadIndicators(getListView().getFirstVisiblePosition(), getListView().getLastVisiblePosition());
            }

        });
        topUnreadIndicatorColor = (LinearLayout) view.findViewById(R.id.topUnreadIndicatorColor);
        topUnreadIndicatorBorder = (LinearLayout) view.findViewById(R.id.topUnreadIndicatorBorder);
        bottomUnreadIndicator = (LinearLayout) view.findViewById(R.id.bottomUnreadIndicator);
        bottomUnreadIndicator.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                int offset = getListView().getLastVisiblePosition() - getListView().getFirstVisiblePosition();
                int scrollTo = adapter.unreadPositionBelow(getListView().getLastVisiblePosition()) - offset + 2;
                if (scrollTo < adapter.getCount())
                    getListView().setSelection(scrollTo);
                else
                    getListView().setSelection(adapter.getCount() - 1);

                updateUnreadIndicators(getListView().getFirstVisiblePosition(), getListView().getLastVisiblePosition());
            }

        });
        bottomUnreadIndicatorColor = (LinearLayout) view.findViewById(R.id.bottomUnreadIndicatorColor);
        bottomUnreadIndicatorBorder = (LinearLayout) view.findViewById(R.id.bottomUnreadIndicatorBorder);
        listView = (ListView) view.findViewById(android.R.id.list);
        listView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                updateUnreadIndicators(firstVisibleItem, firstVisibleItem + visibleItemCount - 1);
            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }
        });
        listView.setOnItemLongClickListener(new OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                return mListener.onBufferLongClicked(BuffersList.getInstance().getBuffer(adapter.data.get(pos).getBid()));
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
            refreshTask.onPostExecute(null);
            if (savedInstanceState != null && savedInstanceState.containsKey("scrollPosition"))
                listView.setSelection(savedInstanceState.getInt("scrollPosition"));
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
                if (mExpandArchives.get(s.getCid(), false))
                    expandedArchives.add(s.getCid());
            }
            state.putIntegerArrayList("expandedArchives", expandedArchives);
            if (listView != null)
                state.putInt("scrollPosition", listView.getFirstVisiblePosition());
        }
    }

    public void onResume() {
        super.onResume();
        conn.addHandler(this);
        ready = conn.ready;
        refresh();
    }

    @Override public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = IRCCloudApplication.getRefWatcher(getActivity());
        if(refWatcher != null)
            refWatcher.watch(this);
    }

    @Override
    public void onPause() {
        super.onPause();
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

    public void onListItemClick(ListView l, View v, int position, long id) {
        Buffer e = (Buffer) adapter.getItem(position);
        if(e != null) {
            switch (e.getType()) {
                case Buffer.TYPE_ADD_NETWORK:
                    mListener.addNetwork();
                    return;
                case Buffer.TYPE_REORDER:
                    mListener.reorder();
                    return;
                case Buffer.TYPE_ARCHIVES_HEADER:
                    mExpandArchives.put(e.getCid(), !mExpandArchives.get(e.getCid(), false));
                    refresh();
                    return;
                case Buffer.TYPE_JOIN_CHANNEL:
                    AddChannelFragment newFragment = new AddChannelFragment();
                    newFragment.setDefaultCid(e.getCid());
                    newFragment.show(getActivity().getSupportFragmentManager(), "dialog");
                    mListener.addButtonPressed(e.getCid());
                    return;
            }
            mListener.onBufferSelected(e.getBid());
        }
    }

    public void onIRCEvent(int what, Object obj) {
        Buffer b;
        IRCCloudJSONObject object = null;
        try {
            object = (IRCCloudJSONObject) obj;
        } catch (ClassCastException e) {
        }
        Event event = null;
        try {
            event = (Event) obj;
        } catch (ClassCastException e) {
        }
        switch (what) {
            case NetworkConnection.EVENT_CHANNELMODE:
                b = BuffersList.getInstance().getBuffer(object.bid());
                if (b != null && adapter != null)
                    adapter.updateBuffer(b);
                break;
            case NetworkConnection.EVENT_STATUSCHANGED:
                if (adapter != null) {
                    ArrayList<Buffer> buffers = BuffersList.getInstance().getBuffersForServer(object.cid());
                    for (Buffer buffer : buffers) {
                        adapter.updateBuffer(buffer);
                    }
                }
                break;
            case NetworkConnection.EVENT_BUFFERMSG:
                if (adapter != null) {
                    if (event.bid != selected_bid.getSelectedBID()) {
                        b = BuffersList.getInstance().getBuffer(event.bid);
                        if (b != null && event.isImportant(b.getType()))
                            adapter.updateBuffer(b);
                    }
                }
                break;
            case NetworkConnection.EVENT_HEARTBEATECHO:
                if (adapter != null) {
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
                if (object.type().startsWith("you_") && getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refresh();
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_USERINFO:
            case NetworkConnection.EVENT_CHANNELTOPIC:
            case NetworkConnection.EVENT_NICKCHANGE:
            case NetworkConnection.EVENT_MEMBERUPDATES:
            case NetworkConnection.EVENT_USERCHANNELMODE:
            case NetworkConnection.EVENT_AWAY:
            case NetworkConnection.EVENT_SELFBACK:
            case NetworkConnection.EVENT_CHANNELTIMESTAMP:
            case NetworkConnection.EVENT_SELFDETAILS:
            case NetworkConnection.EVENT_USERMODE:
            case NetworkConnection.EVENT_SETIGNORES:
            case NetworkConnection.EVENT_BADCHANNELKEY:
            case NetworkConnection.EVENT_OPENBUFFER:
            case NetworkConnection.EVENT_INVALIDNICK:
            case NetworkConnection.EVENT_BANLIST:
            case NetworkConnection.EVENT_WHOLIST:
            case NetworkConnection.EVENT_WHOIS:
            case NetworkConnection.EVENT_LINKCHANNEL:
            case NetworkConnection.EVENT_LISTRESPONSEFETCHING:
            case NetworkConnection.EVENT_LISTRESPONSE:
            case NetworkConnection.EVENT_LISTRESPONSETOOMANY:
            case NetworkConnection.EVENT_CONNECTIONLAG:
            case NetworkConnection.EVENT_GLOBALMSG:
            case NetworkConnection.EVENT_ACCEPTLIST:
            case NetworkConnection.EVENT_NAMESLIST:
            case NetworkConnection.EVENT_CHANNELTOPICIS:
            case NetworkConnection.EVENT_BACKLOG_FAILED:
            case NetworkConnection.EVENT_FAILURE_MSG:
            case NetworkConnection.EVENT_SUCCESS:
            case NetworkConnection.EVENT_PROGRESS:
            case NetworkConnection.EVENT_ALERT:
            case NetworkConnection.EVENT_DEBUG:
            case NetworkConnection.EVENT_CONNECTIVITY:
                break;
            case NetworkConnection.EVENT_BACKLOG_START:
                if (refreshTask != null)
                    refreshTask.cancel(true);
                break;
            case NetworkConnection.EVENT_BACKLOG_END:
                ready = true;
                if (obj != null && adapter != null) {
                    Integer bid = (Integer) obj;
                    b = BuffersList.getInstance().getBuffer(bid);
                    if (b != null) {
                        adapter.updateBuffer(b);
                        break;
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (refreshTask != null)
                                    refreshTask.cancel(true);
                                Crashlytics.log(Log.DEBUG, "IRCCloud", "Refreshing buffers list");
                                RefreshTask t = new RefreshTask();
                                t.doInBackground((Void) null);
                                t.onPostExecute(null);
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

    public Resources getSafeResources() {
        return IRCCloudApplication.getInstance().getApplicationContext().getResources();
    }

    public interface OnBufferSelectedListener {
        public void onBufferSelected(int bid);

        public boolean onBufferLongClicked(Buffer b);

        public void addButtonPressed(int cid);

        public void addNetwork();

        public void reorder();
    }
}
