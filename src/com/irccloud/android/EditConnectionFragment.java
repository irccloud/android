package com.irccloud.android;

import java.util.ArrayList;

import com.actionbarsherlock.app.SherlockDialogFragment;

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
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class EditConnectionFragment extends SherlockDialogFragment {
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
		
		public PresetServersAdapter(Activity context) {
			ctx = context;
			data = new ArrayList<PresetServer>();
			data.add(new PresetServer(null, null, 0));
			data.add(new PresetServer("IRCCloud", "irc.irccloud.com", 6667));
			data.add(new PresetServer("Freenode", "irc.freenode.net", 6697));
			data.add(new PresetServer("QuakeNet", "irc.quakenet.org", 6667));
			data.add(new PresetServer("IRCNet", "irc.atw-inter.net", 6667));
			data.add(new PresetServer("Undernet", "irc.undernet.org", 6667));
			data.add(new PresetServer("DALNet", "irc.dal.net", 6667));
			data.add(new PresetServer("OFTC", "irc.oftc.net", 6667));
			data.add(new PresetServer("GameSurge", "irc.gamesurge.net", 6667));
			data.add(new PresetServer("Efnet", "efnet.xs4all.nl", 6667));
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
	
	public String default_hostname = null;
	public int default_port = 6667;
	public String default_channels = null;
	
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

		adapter = new PresetServersAdapter(getActivity());
		presets.setAdapter(adapter);
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
				dialog.dismiss();
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

	public void save() {
		if(server == null) {
			String netname = hostname.getText().toString();
			if(presets.getSelectedItemPosition() > 0) {
				netname = ((PresetServersAdapter.PresetServer)adapter.getItem(presets.getSelectedItemPosition())).network;
			}
			NetworkConnection.getInstance().addServer(hostname.getText().toString(), (port.getText().length() > 0)?Integer.parseInt(port.getText().toString()):6667, 
					ssl.isChecked()?1:0, netname, nickname.getText().toString(), realname.getText().toString(), server_pass.getText().toString(),
							nickserv_pass.getText().toString(), join_commands.getText().toString(), channels.getText().toString());
		} else {
			NetworkConnection.getInstance().editServer(server.cid, hostname.getText().toString(), (port.getText().length() > 0)?Integer.parseInt(port.getText().toString()):6667, 
					ssl.isChecked()?1:0, server.name, nickname.getText().toString(), realname.getText().toString(), server_pass.getText().toString(),
							nickserv_pass.getText().toString(), join_commands.getText().toString());
			
		}
	}
	
    class DoneClickListener implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			save();
			dialog.dismiss();
		}
    }
}
