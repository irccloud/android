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
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.FontAwesome;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.collection.ServersList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;public class EditConnectionFragment extends DialogFragment implements NetworkConnection.IRCEventHandler {
    private class PresetServersAdapter extends BaseAdapter {
        private Activity ctx;

        private class ViewHolder {
            TextView network;
            TextView hostname;
            TextView lock;
        }

        private class PresetServer {
            String network;
            String host;
            int port;

            public PresetServer(String network, String host, int port) {
                this.network = network;
                this.host = host;
                this.port = port;
            }
        }

        private ArrayList<PresetServer> data;

        public PresetServersAdapter(Activity context, JSONArray networks) {
            ctx = context;
            data = new ArrayList<PresetServer>();
            data.add(new PresetServer(null, null, 0));
            for (int i = 0; i < networks.length(); i++) {
                try {
                    JSONObject o = networks.getJSONObject(i);
                    String name = o.getString("name");
                    JSONObject server = o.getJSONArray("servers").getJSONObject(0);
                    data.add(new PresetServer(name, server.getString("hostname"), server.getInt("port")));
                } catch (JSONException e) {
                }
            }
        }

        public PresetServersAdapter(Activity context) {
            ctx = context;
            data = new ArrayList<PresetServer>();
            data.add(new PresetServer(null, null, 0));
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int pos) {
            return data.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            TextView label;

            if (row == null) {
                LayoutInflater inflater = ctx.getLayoutInflater();
                row = inflater.inflate(android.R.layout.simple_spinner_item, null);

                label = (TextView) row.findViewById(android.R.id.text1);
                row.setTag(label);
            } else {
                label = (TextView) row.getTag();
            }

            if (position == 0) {
                label.setText("Choose a network…");
            } else {
                label.setText(data.get(position).network);
            }

            return row;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            ViewHolder holder;

            if (row == null) {
                LayoutInflater inflater = ctx.getLayoutInflater();
                row = inflater.inflate(R.layout.row_server, parent, false);

                holder = new ViewHolder();
                holder.network = (TextView) row.findViewById(R.id.network);
                holder.hostname = (TextView) row.findViewById(R.id.hostname);
                holder.lock = (TextView) row.findViewById(R.id.lock);
                holder.lock.setTypeface(FontAwesome.getTypeface());

                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }

            if (position == 0) {
                holder.network.setText("Choose a network…");
                holder.hostname.setVisibility(View.GONE);
                holder.lock.setVisibility(View.GONE);
            } else {
                PresetServer s = data.get(position);
                holder.network.setText(s.network);
                holder.hostname.setText(s.host);
                holder.hostname.setVisibility(View.VISIBLE);
                holder.lock.setVisibility(View.VISIBLE);
                if (s.port == 6697) {
                    holder.lock.setText(FontAwesome.SHIELD);
                } else {
                    holder.lock.setText(FontAwesome.GLOBE);
                }
            }
            return row;
        }
    }

    PresetServersAdapter adapter;

    Server server;

    LinearLayout channelsWrapper;
    Spinner presets;
    EditText hostname;
    EditText port;
    SwitchCompat ssl;
    EditText nickname;
    EditText realname;
    EditText channels;
    EditText nickserv_pass;
    EditText join_commands;
    EditText server_pass;
    EditText network;

    public String default_hostname = null;
    public int default_port = 6667;
    public String default_channels = null;

    private int reqid = -1;

    private void init(View v) {
        channelsWrapper = (LinearLayout) v.findViewById(R.id.channels_wrapper);
        presets = (Spinner) v.findViewById(R.id.presets);
        hostname = (EditText) v.findViewById(R.id.hostname);
        port = (EditText) v.findViewById(R.id.port);
        ssl = (SwitchCompat) v.findViewById(R.id.ssl);
        nickname = (EditText) v.findViewById(R.id.nickname);
        realname = (EditText) v.findViewById(R.id.realname);
        channels = (EditText) v.findViewById(R.id.channels);
        nickserv_pass = (EditText) v.findViewById(R.id.nickservpassword);
        join_commands = (EditText) v.findViewById(R.id.commands);
        server_pass = (EditText) v.findViewById(R.id.serverpassword);
        network = (EditText) v.findViewById(R.id.network);

        presets.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    if (server != null) {
                        hostname.setText(server.getHostname());
                        port.setText(String.valueOf(server.getPort()));
                        ssl.setChecked(server.getSsl() == 1);
                    } else {
                        if (default_hostname != null)
                            hostname.setText(default_hostname);
                        else
                            hostname.setText("");
                        port.setText(String.valueOf(default_port));
                        ssl.setChecked(default_port == 6697);
                    }
                } else {
                    PresetServersAdapter.PresetServer s = (PresetServersAdapter.PresetServer) adapter.getItem(position);
                    hostname.setText(s.host);
                    port.setText(String.valueOf(s.port));
                    ssl.setChecked(s.port == 6697);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (server != null) {
                    hostname.setText(server.getHostname());
                    port.setText(String.valueOf(server.getPort()));
                    ssl.setChecked(server.getSsl() == 1);
                } else {
                    if (default_hostname != null)
                        hostname.setText(default_hostname);
                    else
                        hostname.setText("");
                    port.setText(String.valueOf(default_port));
                    ssl.setChecked(default_port == 6697);
                }
            }

        });

        if (NetworkConnection.getInstance().getUserInfo() != null && !NetworkConnection.getInstance().getUserInfo().verified) {
            Button b = (Button) v.findViewById(R.id.resend);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NetworkConnection.getInstance().resend_verify_email();
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Confirmation Sent");
                    builder.setMessage("You should shortly receive an email with a link to confirm your address.");
                    builder.setNeutralButton("Close", null);
                    builder.show();
                }
            });
            v.findViewById(R.id.unverified).setVisibility(View.VISIBLE);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (getShowsDialog()) {
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            final View v = inflater.inflate(R.layout.dialog_edit_connection, container, false);
            init(v);
            return v;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context ctx = getActivity();
        if(ctx == null)
            return null;

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.dialog_edit_connection, null);
        init(v);

        if (savedInstanceState != null && savedInstanceState.containsKey("cid"))
            server = ServersList.getInstance().getServer(savedInstanceState.getInt("cid"));

        final AlertDialog d = new AlertDialog.Builder(ctx)
                .setTitle((server == null) ? "Add A Network" : "Edit Connection")
                .setView(v)
                .setPositiveButton((server == null) ? "Add" : "Save", null)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        NetworkConnection.getInstance().removeHandler(EditConnectionFragment.this);
                    }
                })
                .create();
        d.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        NetworkConnection.getInstance().removeHandler(EditConnectionFragment.this);
                        NetworkConnection.getInstance().addHandler(EditConnectionFragment.this);
                        reqid = save();
                    }
                });
            }
        });
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return d;
    }

    public void setCid(int cid) {
        server = ServersList.getInstance().getServer(cid);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (server != null) {
            presets.setSelection(0);
            presets.setVisibility(View.GONE);
            network.setVisibility(View.VISIBLE);
            network.setText(server.getName());
            channelsWrapper.setVisibility(View.GONE);
            hostname.setText(server.getHostname());
            port.setText(String.valueOf(server.getPort()));
            ssl.setChecked(server.getSsl() > 0);
            nickname.setText(server.getNick());
            realname.setText(server.getRealname());
            join_commands.setText(server.getJoin_commands());
            nickserv_pass.setText(server.getNickserv_pass());
            server_pass.setText(server.getServer_pass());
        } else {
            if (adapter == null)
                new LoadNetworkPresets().execute((Void) null);

            if (default_hostname != null)
                hostname.setText(default_hostname);
            port.setText(String.valueOf(default_port));
            ssl.setChecked(default_port == 6697);
            if (default_channels != null)
                channels.setText(default_channels);
            if (NetworkConnection.getInstance().getUserInfo() != null) {
                String name = NetworkConnection.getInstance().getUserInfo().name;
                String nick;
                if (name.contains(" ")) {
                    nick = name.substring(0, name.indexOf(" "));
                } else {
                    nick = name;
                }
                realname.setText(name);
                nickname.setText(nick.toLowerCase());
            }
        }
    }

    @Override public void onDestroy() {
        super.onDestroy();
        if(presets != null)
            presets.setOnItemSelectedListener(null);
        NetworkConnection.getInstance().removeHandler(EditConnectionFragment.this);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (server != null)
            savedInstanceState.putInt("cid", server.getCid());
    }

    public int save() {
        int portValue = 6667;
        try {
            portValue = Integer.parseInt(port.getText().toString());
        } catch (NumberFormatException e) {
        }
        if (server == null) {
            String netname = null;
            if (presets.getSelectedItemPosition() > 0) {
                netname = ((PresetServersAdapter.PresetServer) adapter.getItem(presets.getSelectedItemPosition())).network;
            }
            return NetworkConnection.getInstance().addServer(hostname.getText().toString(), portValue,
                    ssl.isChecked() ? 1 : 0, netname, nickname.getText().toString(), realname.getText().toString(), server_pass.getText().toString(),
                    nickserv_pass.getText().toString(), join_commands.getText().toString(), channels.getText().toString());
        } else {
            String netname = network.getText().toString();
            if (hostname.getText().toString().equalsIgnoreCase(netname))
                netname = null;
            return NetworkConnection.getInstance().editServer(server.getCid(), hostname.getText().toString(), portValue,
                    ssl.isChecked() ? 1 : 0, netname, nickname.getText().toString(), realname.getText().toString(), server_pass.getText().toString(),
                    nickserv_pass.getText().toString(), join_commands.getText().toString());

        }
    }

    public void onIRCEvent(int what, Object o) {
        IRCCloudJSONObject obj;
        switch (what) {
            case NetworkConnection.EVENT_USERINFO:
                try {
                    dismiss();
                } catch (Exception e) {
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onIRCRequestSucceeded(int reqid, IRCCloudJSONObject object) {
        if(reqid == this.reqid) {
            NetworkConnection.getInstance().removeHandler(EditConnectionFragment.this);
            try {
                dismiss();
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void onIRCRequestFailed(int reqid, IRCCloudJSONObject object) {
        if(reqid == this.reqid) {
            final String message = object.getString("message");
            if (getActivity() != null)
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (message) {
                            case "passworded_servers":
                                Toast.makeText(IRCCloudApplication.getInstance().getApplicationContext(), "You can’t connect to passworded servers with free accounts.", Toast.LENGTH_LONG).show();
                                break;
                            case "networks":
                                Toast.makeText(IRCCloudApplication.getInstance().getApplicationContext(), "You've exceeded the connection limit for free accounts.", Toast.LENGTH_LONG).show();
                                break;
                            case "unverified":
                                Toast.makeText(IRCCloudApplication.getInstance().getApplicationContext(), "You can’t connect to external servers until you confirm your email address.", Toast.LENGTH_LONG).show();
                                break;
                            case "sts_policy":
                                Toast.makeText(IRCCloudApplication.getInstance().getApplicationContext(), "You can’t disable secure connections to this network because it’s using a strict transport security policy.", Toast.LENGTH_LONG).show();
                                break;
                            default:
                                Toast.makeText(IRCCloudApplication.getInstance().getApplicationContext(), "Unable to add connection: invalid " + message, Toast.LENGTH_LONG).show();
                                break;
                        }
                    }
                });
        }
    }

    private class LoadNetworkPresets extends AsyncTaskEx<Void, Void, JSONArray> {

        @Override
        protected void onPreExecute() {
            presets.setEnabled(false);
            presets.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.loading_spinner_item, R.id.text, new String[]{"Loading networks"}));
        }

        @Override
        protected JSONArray doInBackground(Void... params) {
            try {
                return NetworkConnection.getInstance().networkPresets();
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONArray networks) {
            if (networks != null && networks.length() > 0) {
                adapter = new PresetServersAdapter(getActivity(), networks);
            } else {
                adapter = new PresetServersAdapter(getActivity());
            }
            presets.setAdapter(adapter);
            presets.setEnabled(true);
        }
    }
}
