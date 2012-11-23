package com.irccloud.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.google.gson.JsonArray;

public class WhoisFragment extends SherlockDialogFragment {
	IRCCloudJSONObject event;
	TextView userip, name, mask, server, time, timeTitle, channels, opChannels, opTitle, secure;
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		Context ctx = getActivity();
		if(Build.VERSION.SDK_INT < 11)
			ctx = new ContextThemeWrapper(ctx, android.R.style.Theme_Dialog);

		LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View v = inflater.inflate(R.layout.dialog_whois, null);
    	userip = (TextView)v.findViewById(R.id.userip);
    	name = (TextView)v.findViewById(R.id.name);
    	mask = (TextView)v.findViewById(R.id.mask);
    	server = (TextView)v.findViewById(R.id.server);
    	timeTitle = (TextView)v.findViewById(R.id.timeTitle);
    	time = (TextView)v.findViewById(R.id.time);
    	opTitle = (TextView)v.findViewById(R.id.opTitle);
    	opChannels = (TextView)v.findViewById(R.id.opChannels);
    	opChannels.setMovementMethod(LinkMovementMethod.getInstance());
    	opChannels.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getDialog().dismiss();
			}
    	});
    	channels = (TextView)v.findViewById(R.id.channels);
    	channels.setMovementMethod(LinkMovementMethod.getInstance());
    	channels.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getDialog().dismiss();
			}
    	});
    	secure = (TextView)v.findViewById(R.id.secure);
        if(savedInstanceState != null && savedInstanceState.containsKey("event")) {
        	event = new IRCCloudJSONObject(savedInstanceState.getString("event"));
        }
    	Dialog d = new AlertDialog.Builder(ctx)
        .setTitle("WHOIS response for " + event.getString("user_nick"))
        .setView(v)
        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
        })
        .create();
    	return d;
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
    	state.putString("event", event.toString());
    }
	
    @Override
    public void setArguments(Bundle args) {
    	event = new IRCCloudJSONObject(args.getString("event"));
    }
    
    private String format_duration(long duration) {
		if(duration > (86400*30)) {
			return ((int)(duration / (86400*30))) + " months";
		} else if(duration > 86400) {
			return ((int)(duration / 86400)) + " days";
		} else if(duration > 3600) {
			return ((int)(duration / 3600)) + " hours";
		} else if(duration > 60) {
			return ((int)(duration / 60)) + " minutes";
		} else {
			return duration + " seconds";
		}
    }
    
    public void onResume() {
    	super.onResume();
    	
    	if(event != null) {
    		if(event.has("userip")) {
    			userip.setVisibility(View.VISIBLE);
    			userip.setText(event.getString("user_nick") + " " + event.getString("userip"));
    		} else {
    			userip.setVisibility(View.GONE);
    		}
    		String nametxt = event.getString("user_realname");
    		if(event.has("user_logged_in_as") && event.getString("user_logged_in_as").length() > 0) {
    			nametxt += " (authed as " + event.getString("user_logged_in_as") + ")";
    		}
    		name.setText(nametxt);
    		mask.setText(event.getString("user_username") + "@" + event.getString("user_host"));
    		String s = event.getString("server_addr");
    		if(event.has("server_extra") && event.getString("server_extra").length() > 0)
    			s += " (" + event.getString("server_extra") + ")";
    		server.setText(s);
    		if(event.has("signon_time")) {
	    		String timetxt = format_duration((System.currentTimeMillis() / 1000L) - event.getLong("signon_time"));
	    		if(event.has("idle_secs") && event.getLong("idle_secs") > 0)
	    			timetxt += " (idle for " + format_duration(event.getLong("idle_secs")) + ")";
	    		time.setText(timetxt);
    			timeTitle.setVisibility(View.VISIBLE);
    			time.setVisibility(View.VISIBLE);
    		} else {
    			timeTitle.setVisibility(View.GONE);
    			time.setVisibility(View.GONE);
    		}
    		if(event.has("channels_op")) {
    			opTitle.setVisibility(View.VISIBLE);
    			opChannels.setVisibility(View.VISIBLE);
	    		String channelstxt = "";
	    		JsonArray c = event.getJsonArray("channels_op");
	    		for(int i = 0; i < c.size(); i++) {
	    			String chan = c.get(i).getAsString();
	    			if(i > 0)
	    				channelstxt += ", ";
	    			channelstxt += chan;
	    		}
	    		opChannels.setText(ColorFormatter.html_to_spanned(channelstxt, true, ServersDataSource.getInstance().getServer(event.cid())));
    		} else {
    			opTitle.setVisibility(View.GONE);
    			opChannels.setVisibility(View.GONE);
    		}
    		String channelstxt = "";
    		JsonArray c = event.getJsonArray("channels_member");
    		for(int i = 0; i < c.size(); i++) {
    			String chan = c.get(i).getAsString();
    			if(i > 0)
    				channelstxt += ", ";
    			channelstxt += chan;
    		}
    		channels.setText(ColorFormatter.html_to_spanned(channelstxt, true, ServersDataSource.getInstance().getServer(event.cid())));
    		if(event.has("secure")) {
    			secure.setVisibility(View.VISIBLE);
    			secure.setText(event.getString("user_nick") + " " + event.getString("secure"));
    		} else {
    			secure.setVisibility(View.GONE);
    		}
    	}
    }
}
