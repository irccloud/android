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
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.collection.ChannelsList;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.data.model.User;
import com.irccloud.android.data.collection.UsersList;
import com.irccloud.android.databinding.RowUserBinding;
import com.squareup.leakcanary.RefWatcher;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class UsersListFragment extends Fragment implements NetworkConnection.IRCEventHandler {
    private static final int TYPE_HEADING = 0;
    private static final int TYPE_USER = 1;

    private NetworkConnection conn;
    private UserListAdapter adapter;
    private OnUserSelectedListener mListener;
    private int cid = -1;
    private int bid = -1;
    private String channel;
    private static Timer tapTimer = null;
    private TimerTask tapTimerTask = null;
    private RecyclerView recyclerView = null;

    private ColorScheme colorScheme = ColorScheme.getInstance();

    public RecyclerView getRecyclerView() {
        return recyclerView;
    }

    public static class UserListEntry {
        public int type;
        public String text;
        public String count;
        public int bg_color;
        public int heading_color;
        public boolean away;
        public boolean last;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public RowUserBinding binding;

        public ViewHolder(View v) {
            super(v);
            binding = DataBindingUtil.bind(v);
        }
    }

    private class UserListAdapter extends RecyclerView.Adapter<ViewHolder> {
        ArrayList<UserListEntry> data;

        public UserListAdapter() {
            data = new ArrayList<>(50);
        }

        public void setItems(ArrayList<UserListEntry> items) {
            data = items;
        }

        public UserListEntry buildItem(int type, String text, String count, int bg_color, int heading_color, boolean away, boolean last) {
            UserListEntry e = new UserListEntry();
            e.type = type;
            e.text = text;
            e.count = count;
            e.bg_color = bg_color;
            e.heading_color = heading_color;
            e.away = away;
            e.last = last;
            return e;
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public Object getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = RowUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot();
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            UserListEntry e = data.get(position);
            RowUserBinding row = holder.binding;

            row.setUser(e);
            row.getRoot().setOnLongClickListener(new OnItemLongClickListener(position));
            row.getRoot().setOnClickListener(new OnItemClickListener(position));

            try {
                row.setMono(PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("monospace", false));
            } catch (Exception e1) {
                row.setMono(false);
            }

            if (e.type == TYPE_HEADING) {
                row.getRoot().setFocusable(false);
                row.getRoot().setEnabled(false);
            } else {
                row.getRoot().setFocusable(true);
                row.getRoot().setEnabled(true);
            }
            row.executePendingBindings();
        }
    }

    private void addUsersFromList(ArrayList<UserListEntry> entries, ArrayList<User> users, String heading, String symbol, int bg_color, int heading_color) {
        if (users.size() > 0 && symbol != null) {
            entries.add(adapter.buildItem(TYPE_HEADING, heading, users.size() > 0 ? symbol + String.valueOf(users.size()) : null, bg_color, heading_color, false, false));
            for (int i = 0; i < users.size(); i++) {
                User user = users.get(i);
                entries.add(adapter.buildItem(TYPE_USER, user.nick, null, bg_color, heading_color, user.away > 0, bg_color != colorScheme.row_members_bg_drawable && i == users.size() - 1));
            }
        }
    }

    private void refresh(ArrayList<User> users) {
        if (users == null) {
            if (adapter != null) {
                adapter.data.clear();
                adapter.notifyDataSetChanged();
            }
            return;
        }

        ArrayList<UserListEntry> entries = new ArrayList<>();
        ArrayList<User> opers = new ArrayList<>();
        ArrayList<User> owners = new ArrayList<>();
        ArrayList<User> admins = new ArrayList<>();
        ArrayList<User> ops = new ArrayList<>();
        ArrayList<User> halfops = new ArrayList<>();
        ArrayList<User> voiced = new ArrayList<>();
        ArrayList<User> members = new ArrayList<>();
        boolean showSymbol = false;
        try {
            if (conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null)
                showSymbol = conn.getUserInfo().prefs.getBoolean("mode-showsymbol");
        } catch (JSONException e) {
        }

        ObjectNode PREFIX = null;
        Server s = ServersList.getInstance().getServer(cid);
        if (s != null)
            PREFIX = s.PREFIX;

        if (PREFIX == null) {
            PREFIX = new ObjectMapper().createObjectNode();
            PREFIX.put(s != null ? s.MODE_OPER : "Y", "!");
            PREFIX.put(s != null ? s.MODE_OWNER : "q", "~");
            PREFIX.put(s != null ? s.MODE_ADMIN : "a", "&");
            PREFIX.put(s != null ? s.MODE_OP : "o", "@");
            PREFIX.put(s != null ? s.MODE_HALFOP : "h", "%");
            PREFIX.put(s != null ? s.MODE_VOICED : "v", "+");
        }

        if (adapter == null) {
            adapter = new UserListAdapter();
        }

        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            String mode = user.mode.toLowerCase();
            if (mode.contains(s != null ? s.MODE_OPER.toLowerCase() : "y") && (PREFIX.has(s != null ? s.MODE_OPER : "Y") || PREFIX.has(s != null ? s.MODE_OPER.toLowerCase() : "y"))) {
                opers.add(user);
            } else if (mode.contains(s != null ? s.MODE_OWNER.toLowerCase() : "q") && PREFIX.has(s != null ? s.MODE_OWNER : "q")) {
                owners.add(user);
            } else if (mode.contains(s != null ? s.MODE_ADMIN.toLowerCase() : "a") && PREFIX.has(s != null ? s.MODE_ADMIN : "a")) {
                admins.add(user);
            } else if (mode.contains(s != null ? s.MODE_OP.toLowerCase() : "o") && PREFIX.has(s != null ? s.MODE_OP : "o")) {
                ops.add(user);
            } else if (mode.contains(s != null ? s.MODE_HALFOP.toLowerCase() : "h") && PREFIX.has(s != null ? s.MODE_HALFOP : "h")) {
                halfops.add(user);
            } else if (mode.contains(s != null ? s.MODE_VOICED.toLowerCase() : "v") && PREFIX.has(s != null ? s.MODE_VOICED : "v")) {
                voiced.add(user);
            } else {
                members.add(user);
            }
        }

        if (opers.size() > 0) {
            if (showSymbol) {
                if (PREFIX.has(s != null ? s.MODE_OPER : "Y"))
                    addUsersFromList(entries, opers, "OPER", PREFIX.get(s != null ? s.MODE_OPER : "Y").asText() + " ", colorScheme.row_opers_bg_drawable, colorScheme.opersHeadingColor);
                else if (PREFIX.has(s != null ? s.MODE_OPER.toLowerCase() : "y"))
                    addUsersFromList(entries, opers, "OPER", PREFIX.get(s != null ? s.MODE_OPER.toLowerCase() : "y").asText() + " ", colorScheme.row_opers_bg_drawable, colorScheme.opersHeadingColor);
                else
                    addUsersFromList(entries, opers, "OPER", "", colorScheme.row_opers_bg_drawable, colorScheme.opersHeadingColor);
            } else {
                addUsersFromList(entries, opers, "OPER", "", colorScheme.row_opers_bg_drawable, colorScheme.opersHeadingColor);
            }
        }

        if (owners.size() > 0) {
            if (showSymbol) {
                if (PREFIX.has(s != null ? s.MODE_OWNER : "q"))
                    addUsersFromList(entries, owners, "OWNER", PREFIX.get(s != null ? s.MODE_OWNER : "q").asText() + " ", colorScheme.row_owners_bg_drawable, colorScheme.ownersHeadingColor);
                else
                    addUsersFromList(entries, owners, "OWNER", "", colorScheme.row_owners_bg_drawable, colorScheme.ownersHeadingColor);
            } else {
                addUsersFromList(entries, owners, "OWNER", "", colorScheme.row_owners_bg_drawable, colorScheme.ownersHeadingColor);
            }
        }

        if (admins.size() > 0) {
            if (showSymbol) {
                if (PREFIX.has(s != null ? s.MODE_ADMIN : "a"))
                    addUsersFromList(entries, admins, "ADMINS", PREFIX.get(s != null ? s.MODE_ADMIN : "a").asText() + " ", colorScheme.row_admins_bg_drawable, colorScheme.adminsHeadingColor);
                else
                    addUsersFromList(entries, admins, "ADMINS", "", colorScheme.row_admins_bg_drawable, colorScheme.adminsHeadingColor);
            } else {
                addUsersFromList(entries, admins, "ADMINS", "", colorScheme.row_admins_bg_drawable, colorScheme.adminsHeadingColor);
            }
        }

        if (ops.size() > 0) {
            if (showSymbol) {
                if (PREFIX.has(s != null ? s.MODE_OP : "o"))
                    addUsersFromList(entries, ops, "OPS", PREFIX.get(s != null ? s.MODE_OP : "o").asText() + " ", colorScheme.row_ops_bg_drawable, colorScheme.opsHeadingColor);
                else
                    addUsersFromList(entries, ops, "OPS", "", colorScheme.row_ops_bg_drawable, colorScheme.opsHeadingColor);
            } else {
                addUsersFromList(entries, ops, "OPS", "", colorScheme.row_ops_bg_drawable, colorScheme.opsHeadingColor);
            }
        }

        if (halfops.size() > 0) {
            if (showSymbol) {
                if (PREFIX.has(s != null ? s.MODE_HALFOP : "h"))
                    addUsersFromList(entries, halfops, "HALF OPS", PREFIX.get(s != null ? s.MODE_HALFOP : "h").asText() + " ", colorScheme.row_halfops_bg_drawable, colorScheme.halfopsHeadingColor);
                else
                    addUsersFromList(entries, halfops, "HALF OPS", "", colorScheme.row_halfops_bg_drawable, colorScheme.halfopsHeadingColor);
            } else {
                addUsersFromList(entries, halfops, "HALF OPS", "", colorScheme.row_halfops_bg_drawable, colorScheme.halfopsHeadingColor);
            }
        }

        if (voiced.size() > 0) {
            if (showSymbol) {
                if (PREFIX.has(s != null ? s.MODE_VOICED : "v"))
                    addUsersFromList(entries, voiced, "VOICED", PREFIX.get(s != null ? s.MODE_VOICED : "v").asText() + " ", colorScheme.row_voiced_bg_drawable, colorScheme.voicedHeadingColor);
                else
                    addUsersFromList(entries, voiced, "VOICED", "", colorScheme.row_voiced_bg_drawable, colorScheme.voicedHeadingColor);
            } else {
                addUsersFromList(entries, voiced, "VOICED", "", colorScheme.row_voiced_bg_drawable, colorScheme.voicedHeadingColor);
            }
        }

        if (members.size() > 0) {
            addUsersFromList(entries, members, "MEMBERS", "", colorScheme.row_members_bg_drawable, colorScheme.membersHeadingColor);
        }

        adapter.setItems(entries);

        if (recyclerView != null && entries.size() > 0 && recyclerView.getAdapter() != adapter)
            recyclerView.setAdapter(adapter);
        else
            adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (tapTimer == null)
            tapTimer = new Timer("users-tap-timer");

        if (savedInstanceState != null && savedInstanceState.containsKey("cid")) {
            cid = savedInstanceState.getInt("cid");
            bid = savedInstanceState.getInt("bid");
            channel = savedInstanceState.getString("channel");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.userslist, container);
        recyclerView = (RecyclerView)v.findViewById(android.R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
        return v;
    }

    public void onResume() {
        super.onResume();
        conn = NetworkConnection.getInstance();
        conn.addHandler(this);
        ArrayList<User> users = UsersList.getInstance().getUsersForBuffer(bid);
        refresh(users);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = IRCCloudApplication.getRefWatcher(getActivity());
        if (refWatcher != null)
            refWatcher.watch(this);
        if (tapTimer != null) {
            tapTimer.cancel();
            tapTimer = null;
        }
    }

    @Override
    public void setArguments(Bundle args) {
        if(args.getInt("bid", 0) != bid && recyclerView != null)
            recyclerView.scrollToPosition(0);
        cid = args.getInt("cid", 0);
        bid = args.getInt("bid", 0);
        channel = args.getString("name");
        if (tapTimer == null)
            tapTimer = new Timer("users-tap-timer");

        tapTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (getActivity() != null)
                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            ArrayList<User> users = null;
                            if (ChannelsList.getInstance().getChannelForBuffer(bid) != null)
                                users = UsersList.getInstance().getUsersForBuffer(bid);
                            refresh(users);
                            try {
                                if (recyclerView != null)
                                    recyclerView.scrollToPosition(0);
                            } catch (Exception e) { //Sometimes the list view isn't available yet
                            }
                        }

                    });
            }
        }, 100);
    }

    public void onPause() {
        super.onPause();
        if (conn != null)
            conn.removeHandler(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnUserSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnUserSelectedListener");
        }
        if (cid == -1) {
            cid = activity.getIntent().getIntExtra("cid", 0);
            channel = activity.getIntent().getStringExtra("name");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("cid", cid);
        state.putInt("bid", bid);
        state.putString("channel", channel);
    }

    private class OnItemLongClickListener implements OnLongClickListener {
        private int pos;

        OnItemLongClickListener(int position) {
            pos = position;
        }

        @Override
        public boolean onLongClick(View v) {
            if (pos < adapter.getItemCount()) {
                UserListEntry e = (UserListEntry) adapter.getItem(pos);
                if (e.type == TYPE_USER) {
                    mListener.onUserSelected(cid, channel, e.text);
                    return true;
                }
            }
            return false;
        }

    }

    private class OnItemClickListener implements OnClickListener {
        private int pos;

        OnItemClickListener(int position) {
            pos = position;
        }

        @Override
        public void onClick(View arg0) {
            if (pos < 0)
                return;

            if (adapter != null && tapTimer != null) {
                if (tapTimerTask != null) {
                    tapTimerTask.cancel();
                    tapTimerTask = null;
                    try {
                        UserListEntry e = (UserListEntry) adapter.getItem(pos);
                        if (e.type == TYPE_USER)
                            mListener.onUserDoubleClicked(e.text);
                    } catch (Exception e) {

                    }
                } else {
                    tapTimerTask = new TimerTask() {
                        int position = pos;

                        @Override
                        public void run() {
                            if (getActivity() != null)
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            UserListEntry e = (UserListEntry) adapter.getItem(position);
                                            if (e.type == TYPE_USER) {
                                                mListener.onUserSelected(cid, channel, e.text);

                                                if (!getActivity().getSharedPreferences("prefs", 0).getBoolean("longPressTip", false)) {
                                                    Toast.makeText(getActivity(), "Long-press a message to quickly interact with the sender", Toast.LENGTH_LONG).show();
                                                    SharedPreferences.Editor editor = getActivity().getSharedPreferences("prefs", 0).edit();
                                                    editor.putBoolean("longPressTip", true);
                                                    editor.commit();
                                                }
                                            }
                                        } catch (Exception e) {

                                        }
                                    }
                                });
                            tapTimerTask = null;
                        }
                    };
                    tapTimer.schedule(tapTimerTask, 300);
                }
            }
        }
    }

    public void onIRCEvent(int what, Object obj) {
        switch (what) {
            case NetworkConnection.EVENT_CONNECTIVITY:
                if (adapter != null && getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (adapter != null)
                                adapter.notifyDataSetChanged();
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_JOIN:
            case NetworkConnection.EVENT_PART:
            case NetworkConnection.EVENT_QUIT:
            case NetworkConnection.EVENT_USERCHANNELMODE:
            case NetworkConnection.EVENT_KICK:
            case NetworkConnection.EVENT_NICKCHANGE:
                if (((IRCCloudJSONObject) obj).bid() != bid)
                    break;
            case NetworkConnection.EVENT_CHANNELINIT:
            case NetworkConnection.EVENT_USERINFO:
            case NetworkConnection.EVENT_MEMBERUPDATES:
            case NetworkConnection.EVENT_BACKLOG_END:
                if (getActivity() != null) {
                    final ArrayList<User> users;
                    if (ChannelsList.getInstance().getChannelForBuffer(bid) != null) {
                        users = UsersList.getInstance().getUsersForBuffer(bid);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refresh(users);
                            }
                        });
                    }
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

    public interface OnUserSelectedListener {
        void onUserSelected(int cid, String channel, String name);
        void onUserDoubleClicked(String name);
    }
}
