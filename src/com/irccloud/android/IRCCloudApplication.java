package com.irccloud.android;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(formKey = "")
@SuppressWarnings("unused")
public class IRCCloudApplication extends Application {
	private static IRCCloudApplication instance = null;
	private NetworkConnection conn = null;
	private ServersDataSource s = null;
	private BuffersDataSource b = null;
	private ChannelsDataSource c = null;
	private UsersDataSource u = null;
	private EventsDataSource e = null;
	
	public static IRCCloudApplication getInstance() {
		if(instance != null) {
			return instance;
		} else {
			return new IRCCloudApplication();
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
		//Allocate all the shared objects at launch
		conn = NetworkConnection.getInstance();
		s = ServersDataSource.getInstance();
		b = BuffersDataSource.getInstance();
		c = ChannelsDataSource.getInstance();
		u = UsersDataSource.getInstance();
		e = EventsDataSource.getInstance();
		ACRA.init(this);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		if(prefs.contains("notify")) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("notify_type", prefs.getBoolean("notify", true)?"1":"0");
			editor.remove("notify");
			editor.commit();
		}
	}
}
