/*
 * Copyright (c) 2013 IRCCloud, Ltd.
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

import java.io.IOException;
import java.util.ArrayList;

import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.ServersDataSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EditConnectionFragment extends DialogFragment implements NetworkConnection.IRCEventHandler {
	private class PresetServersAdapter extends BaseAdapter {
		private Activity ctx;
		
		private class ViewHolder {
			TextView network;
			TextView hostname;
			ImageView lock;
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
            for(int i = 0; i < networks.length(); i++) {
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
			data.add(new PresetServer("IRCCloud", "irc.irccloud.com", 6667));
			data.add(new PresetServer("Freenode", "irc.freenode.net", 6697));
			data.add(new PresetServer("QuakeNet", "blacklotus.ca.us.quakenet.org", 6667));
			data.add(new PresetServer("IRCNet", "ircnet.blacklotus.net", 6667));
			data.add(new PresetServer("Undernet", "losangeles.ca.us.undernet.org", 6667));
			data.add(new PresetServer("DALNet", "dalnet.blacklotus.net", 6667));
			data.add(new PresetServer("OFTC", "irc.oftc.net", 6667));
			data.add(new PresetServer("GameSurge", "irc.gamesurge.net", 6667));
			data.add(new PresetServer("Efnet", "efnet.port80.se", 6667));
			data.add(new PresetServer("Mozilla", "irc.mozilla.org", 6697));
			data.add(new PresetServer("Rizon", "irc6.rizon.net", 6697));
			data.add(new PresetServer("Espernet", "irc.esper.net", 6667));
			data.add(new PresetServer("ReplayIRC", "irc.replayirc.com", 6667));
			data.add(new PresetServer("synIRC", "naamio.fi.eu.synirc.net", 6697));
			data.add(new PresetServer("fossnet", "irc.fossnet.info", 6697));
			data.add(new PresetServer("P2P-NET", "irc.p2p-network.net", 6697));
			data.add(new PresetServer("euIRCnet", "irc.euirc.net", 6697));
			data.add(new PresetServer("SlashNET", "irc.slashnet.org", 6697));
			data.add(new PresetServer("Atrum", "irc.atrum.org", 6697));
			data.add(new PresetServer("Indymedia", "irc.indymedia.org", 6697));
			data.add(new PresetServer("TWiT", "irc.twit.tv", 6697));
            data.add(new PresetServer("Snoonet", "irc.snoonet.org", 6697));
            data.add(new PresetServer("BrasIRC", "irc.brasirc.org", 6667));
            data.add(new PresetServer("darkscience", "irc.darkscience.net", 6697));
            data.add(new PresetServer("Techman's World", "irc.techmansworld.com", 6697));
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
		
				label = (TextView)row.findViewById(android.R.id.text1);
				row.setTag(label);
			} else {
				label = (TextView)row.getTag();
			}
			
			if(position == 0) {
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
				row = inflater.inflate(R.layout.row_server, null);
		
				holder = new ViewHolder();
				holder.network = (TextView) row.findViewById(R.id.network);
				holder.hostname = (TextView) row.findViewById(R.id.hostname);
				holder.lock = (ImageView) row.findViewById(R.id.lock);
		
				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}

			if(position == 0) {
				holder.network.setText("Choose a network…");
				holder.hostname.setVisibility(View.GONE);
				holder.lock.setVisibility(View.GONE);
			} else {
				PresetServer s = data.get(position);
				holder.network.setText(s.network);
				holder.hostname.setText(s.host);
				holder.hostname.setVisibility(View.VISIBLE);
				if(s.port == 6697) {
					holder.lock.setVisibility(View.VISIBLE);
				} else {
					holder.lock.setVisibility(View.GONE);
				}
			}
			return row;
		}
	}

	PresetServersAdapter adapter;
	
	ServersDataSource.Server server;

	LinearLayout channelsWrapper;
	Spinner presets;
	EditText hostname;
	EditText port;
	CheckBox ssl;
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
    	channelsWrapper = (LinearLayout)v.findViewById(R.id.channels_wrapper);
		presets = (Spinner)v.findViewById(R.id.presets);
		hostname = (EditText)v.findViewById(R.id.hostname);
		port = (EditText)v.findViewById(R.id.port);
		ssl = (CheckBox)v.findViewById(R.id.ssl);
		nickname = (EditText)v.findViewById(R.id.nickname);
		realname = (EditText)v.findViewById(R.id.realname);
		channels = (EditText)v.findViewById(R.id.channels);
		nickserv_pass = (EditText)v.findViewById(R.id.nickservpassword);
		join_commands = (EditText)v.findViewById(R.id.commands);
		server_pass = (EditText)v.findViewById(R.id.serverpassword);
        network = (EditText)v.findViewById(R.id.network);

		presets.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if(position == 0) {
					if(server != null) {
						hostname.setText(server.hostname);
			    		port.setText(String.valueOf(server.port));
			   			ssl.setChecked(server.ssl == 1);
					} else {
						if(default_hostname != null)
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
				if(server != null) {
					hostname.setText(server.hostname);
		    		port.setText(String.valueOf(server.port));
		   			ssl.setChecked(server.ssl == 1);
				} else {
					if(default_hostname != null)
						hostname.setText(default_hostname);
					else
						hostname.setText("");
					port.setText(String.valueOf(default_port));
		   			ssl.setChecked(default_port == 6697);
				}
			}
			
		});
	}
	
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	if(getShowsDialog()) {
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
		if(Build.VERSION.SDK_INT < 11)
			ctx = new ContextThemeWrapper(ctx, android.R.style.Theme_Dialog);

		LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View v = inflater.inflate(R.layout.dialog_edit_connection, null);
    	init(v);

		if(savedInstanceState != null && savedInstanceState.containsKey("cid"))
			server = ServersDataSource.getInstance().getServer(savedInstanceState.getInt("cid"));
    	
    	Dialog d = new AlertDialog.Builder(ctx)
        .setTitle("Add A Network")
        .setView(v)
        .setPositiveButton((server == null)?"Add":"Save", new DoneClickListener())
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
                NetworkConnection.getInstance().removeHandler(EditConnectionFragment.this);
			}
        })
        .create();
	    d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    	return d;
    }

	public void setCid(int cid) {
		server = ServersDataSource.getInstance().getServer(cid);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		if(server != null) {
			presets.setSelection(0);
			presets.setVisibility(View.GONE);
            network.setVisibility(View.VISIBLE);
            network.setText(server.name);
			channelsWrapper.setVisibility(View.GONE);
			hostname.setText(server.hostname);
			port.setText(String.valueOf(server.port));
			ssl.setChecked(server.ssl > 0);
			nickname.setText(server.nick);
			realname.setText(server.realname);
			join_commands.setText(server.join_commands);
			nickserv_pass.setText(server.nickserv_pass);
			server_pass.setText(server.server_pass);
		} else {
            if(adapter == null)
                new LoadNetworkPresets().execute((Void)null);

            if(default_hostname != null)
				hostname.setText(default_hostname);
			port.setText(String.valueOf(default_port));
			ssl.setChecked(default_port == 6697);
			if(default_channels != null)
				channels.setText(default_channels);
			if(NetworkConnection.getInstance().getUserInfo() != null) {
				String name = NetworkConnection.getInstance().getUserInfo().name;
				String nick;
				if(name.contains(" ")) {
					nick = name.substring(0, name.indexOf(" "));
				} else {
					nick = name;
				}
				realname.setText(name);
				nickname.setText(nick.toLowerCase());
			}
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		if(server != null)
			savedInstanceState.putInt("cid", server.cid);
	}

	public int save() {
		int portValue = 6667;
		try {
			portValue = Integer.parseInt(port.getText().toString());
		} catch (NumberFormatException e) {
		}
		if(server == null) {
			String netname = hostname.getText().toString();
			if(presets.getSelectedItemPosition() > 0) {
				netname = ((PresetServersAdapter.PresetServer)adapter.getItem(presets.getSelectedItemPosition())).network;
			}
			return NetworkConnection.getInstance().addServer(hostname.getText().toString(), portValue,
					ssl.isChecked()?1:0, netname, nickname.getText().toString(), realname.getText().toString(), server_pass.getText().toString(),
							nickserv_pass.getText().toString(), join_commands.getText().toString(), channels.getText().toString());
		} else {
			return NetworkConnection.getInstance().editServer(server.cid, hostname.getText().toString(), portValue,
					ssl.isChecked()?1:0, network.getText().toString(), nickname.getText().toString(), realname.getText().toString(), server_pass.getText().toString(),
							nickserv_pass.getText().toString(), join_commands.getText().toString());
			
		}
	}
	
    class DoneClickListener implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
            NetworkConnection.getInstance().removeHandler(EditConnectionFragment.this);
            NetworkConnection.getInstance().addHandler(EditConnectionFragment.this);
            reqid = save();
		}
    }

    public void onIRCEvent(int what, Object o) {
        IRCCloudJSONObject obj;
        switch (what) {
            case NetworkConnection.EVENT_SUCCESS:
                obj = (IRCCloudJSONObject)o;
                if(obj.getInt("_reqid") == reqid) {
                    NetworkConnection.getInstance().removeHandler(EditConnectionFragment.this);
                }
                break;
            case NetworkConnection.EVENT_FAILURE_MSG:
                obj = (IRCCloudJSONObject)o;
                if(obj.getInt("_reqid") == reqid) {
                    final String message = obj.getString("message");
                    if(getActivity() != null)
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(message.equals("passworded_servers"))
                                    Toast.makeText(IRCCloudApplication.getInstance().getApplicationContext(), "You can’t connect to passworded servers with free accounts.", Toast.LENGTH_SHORT).show();
                                else if(message.equals("networks"))
                                    Toast.makeText(IRCCloudApplication.getInstance().getApplicationContext(), "You've exceeded the connection limit for free accounts.", Toast.LENGTH_SHORT).show();
                                else if(message.equals("unverified"))
                                    Toast.makeText(IRCCloudApplication.getInstance().getApplicationContext(), "You can’t connect to external servers until you confirm your email address.", Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(IRCCloudApplication.getInstance().getApplicationContext(), "Unable to add connection: invalid " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    NetworkConnection.getInstance().removeHandler(EditConnectionFragment.this);
                }
                break;
            default:
                break;
        }
    }

    private class LoadNetworkPresets extends AsyncTaskEx<Void, Void, JSONArray> {

        @Override
        protected void onPreExecute() {
            presets.setEnabled(false);
            presets.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.loading_spinner_item, R.id.text, new String[] {"Loading networks"}));
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
            if(networks != null && networks.length() > 0) {
                adapter = new PresetServersAdapter(getActivity(), networks);
            } else {
                adapter = new PresetServersAdapter(getActivity());
            }
            presets.setAdapter(adapter);
            presets.setEnabled(true);
        }
    }
}
