package com.irccloud.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

public class EditConnectionFragment extends DialogFragment {
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
	
	private static final String[] PRESET_NETWORKS = new String[] { "Choose a network…",
		"IRCCloud", "Freenode", "Freenode (SSL)", "QuakeNet", "IRCNet", "Undernet",
		"DALNet", "OFTC", "GameSurge", "Efnet", "Mozilla", "Mozilla (SSL)", "Rizon",
		"Espernet", "ReplayIRC", "synIRC", "synIRC (SSL)", "fossnet", "fossnet (SSL)",
		"P2P-NET", "P2P-NET (SSL)", "euIRCnet", "euIRCnet (SSL)", "SlashNET", "SlashNET (SSL)",
		"Atrum", "Atrum (SSL)", "Indymedia", "Indymedia (SSL)", "TWiT", "TWiT (SSL)"
    };
	
	private static final String[] PRESET_SERVERS = new String[] { "",
		"irc.irccloud.com",
		"irc.freenode.net",
		"irc.freenode.net",
		"irc.quakenet.org",
		"irc.atw-inter.net",
		"irc.undernet.org",
		"irc.dal.net",
		"irc.oftc.net",
		"irc.gamesurge.net",
		"efnet.xs4all.nl",
		"irc.mozilla.org",
		"irc.mozilla.org",
		"irc.rizon.net",
		"irc.esper.net",
		"irc.replayirc.com",
		"moonlight.se.eu.synirc.net", 
		"moonlight.se.eu.synirc.net",
		"irc.fossnet.info",
		"irc.fossnet.info",
		"irc.p2p-network.net", 
		"irc.p2p-network.net",
		"irc.euirc.net",
		"irc.euirc.net",
		"irc.slashnet.org", 
		"irc.slashnet.org",
		"irc.atrum.org",
		"irc.atrum.org",
		"irc.indymedia.org", 
		"irc.indymedia.org",
		"irc.twit.tv",
		"irc.twit.tv"
	};
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		Context ctx = getActivity();
		if(Build.VERSION.SDK_INT < 11)
			ctx = new ContextThemeWrapper(ctx, android.R.style.Theme_Dialog);

		LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View v = inflater.inflate(R.layout.dialog_edit_connection, null);
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

		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_spinner_item, PRESET_NETWORKS);
    	adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
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
					hostname.setText(PRESET_SERVERS[position]);
					if(PRESET_NETWORKS[position].contains(" (SSL")) {
			    		port.setText("6697");
			   			ssl.setChecked(true);
					} else {
			    		port.setText("6667");
			   			ssl.setChecked(false);
					}
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
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		if(server != null)
			savedInstanceState.putInt("cid", server.cid);
	}
	
    class DoneClickListener implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if(server == null) {
				String netname = hostname.getText().toString();
				if(presets.getSelectedItemPosition() > 0) {
					netname = PRESET_NETWORKS[presets.getSelectedItemPosition()];
				}
				NetworkConnection.getInstance().addServer(hostname.getText().toString(), Integer.parseInt(port.getText().toString()), 
						ssl.isChecked()?1:0, netname, nickname.getText().toString(), realname.getText().toString(), server_pass.getText().toString(),
								nickserv_pass.getText().toString(), join_commands.getText().toString(), channels.getText().toString());
			} else {
				NetworkConnection.getInstance().editServer(server.cid, hostname.getText().toString(), Integer.parseInt(port.getText().toString()), 
						ssl.isChecked()?1:0, server.name, nickname.getText().toString(), realname.getText().toString(), server_pass.getText().toString(),
								nickserv_pass.getText().toString(), join_commands.getText().toString());
				
			}
			dialog.dismiss();
		}
    }
}
