package com.irccloud.android;

import android.app.Application;

public class IRCCloudApplication extends Application {
	private static IRCCloudApplication instance = null;
	private NetworkConnection conn = null;
	
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
		conn = NetworkConnection.getInstance();
	}
}
