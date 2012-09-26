package com.irccloud.androidnative;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

public class EditConnectionActivity extends SherlockActivity {
	ServersDataSource.Server server;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_connection);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.heading_bg_blue));

		getSupportActionBar().setCustomView(R.layout.actionbar_edit_connection);
		presets = (Spinner)findViewById(R.id.presets);
		hostname = (EditText)findViewById(R.id.hostname);
		port = (EditText)findViewById(R.id.port);
		ssl = (CheckBox)findViewById(R.id.ssl);
		nickname = (EditText)findViewById(R.id.nickname);
		realname = (EditText)findViewById(R.id.realname);
		channels = (EditText)findViewById(R.id.channels);
		nickserv_pass = (EditText)findViewById(R.id.nickservpassword);
		join_commands = (EditText)findViewById(R.id.commands);
		server_pass = (EditText)findViewById(R.id.serverpassword);

		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
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
						hostname.setText("");
			    		port.setText("");
			   			ssl.setChecked(false);
					}
					hostname.setEnabled(true);
					port.setEnabled(true);
					ssl.setEnabled(true);
				} else {
					hostname.setText(PRESET_SERVERS[position]);
					if(PRESET_NETWORKS[position].contains(" (SSL")) {
			    		port.setText("6697");
			   			ssl.setChecked(true);
					} else {
			    		port.setText("6667");
			   			ssl.setChecked(false);
					}
					hostname.setEnabled(false);
					port.setEnabled(false);
					ssl.setEnabled(false);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				if(server != null) {
					hostname.setText(server.hostname);
		    		port.setText(String.valueOf(server.port));
		   			ssl.setChecked(server.ssl == 1);
				} else {
					hostname.setText("");
		    		port.setText("");
		   			ssl.setChecked(false);
				}
			}
			
		});
		
    	getSupportActionBar().getCustomView().findViewById(R.id.action_cancel).setOnClickListener(new CancelClickListener());
    	getSupportActionBar().getCustomView().findViewById(R.id.action_done).setOnClickListener(new DoneClickListener());

		if(savedInstanceState != null && savedInstanceState.containsKey("cid"))
			server = ServersDataSource.getInstance().getServer(savedInstanceState.getInt("cid"));
    }

    class CancelClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			finish();
		}
    }
    
    class DoneClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			if(server == null) {
				String netname = hostname.getText().toString();
				if(presets.getSelectedItemPosition() > 0) {
					netname = PRESET_NETWORKS[presets.getSelectedItemPosition()];
				}
				NetworkConnection.getInstance().addServer(hostname.getText().toString(), Integer.parseInt(port.getText().toString()), 
						ssl.isChecked()?1:0, netname, nickname.getText().toString(), realname.getText().toString(), server_pass.getText().toString(),
								nickserv_pass.getText().toString(), join_commands.getText().toString(), channels.getText().toString());
			} else {
				String netname = hostname.getText().toString();
				if(presets.getSelectedItemPosition() > 0) {
					netname = PRESET_NETWORKS[presets.getSelectedItemPosition()];
				} else if(!server.name.contains(".")) {
					netname = server.name;
				}
				NetworkConnection.getInstance().editServer(server.cid, hostname.getText().toString(), Integer.parseInt(port.getText().toString()), 
						ssl.isChecked()?1:0, netname, nickname.getText().toString(), realname.getText().toString(), server_pass.getText().toString(),
								nickserv_pass.getText().toString(), join_commands.getText().toString());
				
			}
			finish();
		}
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	
    	if(server == null && getIntent() != null && getIntent().hasExtra("cid"))
			server = ServersDataSource.getInstance().getServer(getIntent().getIntExtra("cid",-1));
    	
    	if(server != null) {
    		findViewById(R.id.channels_wrapper).setVisibility(View.GONE);
    		
    		hostname.setText(server.hostname);
    		port.setText(String.valueOf(server.port));
   			ssl.setChecked(server.ssl == 1);
   			nickname.setText(server.nick);
   			realname.setText(server.realname);
   			nickserv_pass.setText(server.nickserv_pass);
   			if(server.join_commands != null && !server.join_commands.equals("null"))
   				join_commands.setText(server.join_commands);
   			server_pass.setText(server.server_pass);
    	}
    }
}
