package com.irccloud.android;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

public class PreferencesActivity extends SherlockPreferenceActivity {
	NetworkConnection conn;
	SaveSettingsTask saveSettingsTask = null;
	SavePreferencesTask savePreferencesTask = null;
	int save_prefs_reqid = -1;
	int save_settings_reqid = -1;
	
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		conn = NetworkConnection.getInstance();
		getSupportActionBar().setTitle("Settings");
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		addPreferencesFromResource(R.xml.preferences_account);
		addPreferencesFromResource(R.xml.preferences_display);
		addPreferencesFromResource(R.xml.preferences_device);
		addPreferencesFromResource(R.xml.preferences_notifications);
		addPreferencesFromResource(R.xml.preferences_about);
		findPreference("name").setOnPreferenceChangeListener(settingstoggle);
		if(conn.getUserInfo() != null)
			findPreference("name").setSummary(conn.getUserInfo().name);
		else
			findPreference("name").setSummary(((EditTextPreference)findPreference("name")).getText());
		findPreference("email").setOnPreferenceChangeListener(settingstoggle);
		if(conn.getUserInfo() != null)
			findPreference("email").setSummary(conn.getUserInfo().email);
		else
			findPreference("email").setSummary(((EditTextPreference)findPreference("email")).getText());
		findPreference("highlights").setOnPreferenceChangeListener(settingstoggle);
		if(conn.getUserInfo() != null)
			findPreference("highlights").setSummary(conn.getUserInfo().highlights);
		else
			findPreference("highlights").setSummary(((EditTextPreference)findPreference("highlights")).getText());
		findPreference("autoaway").setOnPreferenceChangeListener(settingstoggle);
		findPreference("time-24hr").setOnPreferenceChangeListener(prefstoggle);
		findPreference("time-seconds").setOnPreferenceChangeListener(prefstoggle);
		findPreference("mode-showsymbol").setOnPreferenceChangeListener(prefstoggle);
		findPreference("faq").setOnPreferenceClickListener(urlClick);
		findPreference("feedback").setOnPreferenceClickListener(urlClick);
		//findPreference("subscriptions").setOnPreferenceClickListener(urlClick);
		//findPreference("changes").setOnPreferenceClickListener(urlClick);
		findPreference("notify_type").setOnPreferenceChangeListener(notificationstoggle);
		switch(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("notify_type", "1"))) {
			case 0:
				findPreference("notify_type").setSummary("Disabled");
				break;
			case 1:
				findPreference("notify_type").setSummary("Enabled");
				break;
			case 2:
				findPreference("notify_type").setSummary("Only while active");
				break;
		}
		if(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("notify_type", "1")) > 0) {
			findPreference("notify_vibrate").setEnabled(true);
			findPreference("notify_ringtone").setEnabled(true);
			findPreference("notify_lights").setEnabled(true);
		} else {
			findPreference("notify_vibrate").setEnabled(false);
			findPreference("notify_ringtone").setEnabled(false);
			findPreference("notify_lights").setEnabled(false);
		}

		try {
			findPreference("version").setSummary(getPackageManager().getPackageInfo("com.irccloud.android", 0).versionName);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
    	}
        return super.onOptionsItemSelected(item);
    }
	
    @Override
    public void onResume() {
    	super.onResume();
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
    }

    @Override
    public void onPause() {
    	super.onPause();

    	if(conn != null) {
        	conn.removeHandler(mHandler);
    	}
    }
    
	@SuppressWarnings("deprecation")
    @SuppressLint("HandlerLeak")
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			IRCCloudJSONObject o;
			
			switch(msg.what) {
			case NetworkConnection.EVENT_SUCCESS:
				o = (IRCCloudJSONObject)msg.obj;
				if(o.has("_reqid")) {
					if(o.getInt("_reqid") == save_settings_reqid) {
						save_settings_reqid = -1;
					} else if(o.getInt("_reqid") == save_prefs_reqid) {
						save_prefs_reqid = -1;
					}
				}
				break;
			case NetworkConnection.EVENT_FAILURE_MSG:
				o = (IRCCloudJSONObject)msg.obj;
				if(o.has("_reqid")) {
					if(o.getInt("_reqid") == save_settings_reqid) {
						save_settings_reqid = -1;
						Log.e("IRCCloud", "Settings not updated: " + o.getString("message"));
					} else if(o.getInt("_reqid") == save_prefs_reqid) {
						save_prefs_reqid = -1;
						Log.e("IRCCloud", "Prefs not updated: " + o.getString("message"));
					}
					Toast.makeText(PreferencesActivity.this, "An error occured while saving settings.  Please try again.", Toast.LENGTH_SHORT).show();
				}
				break;
			case NetworkConnection.EVENT_USERINFO:
				NetworkConnection.UserInfo userInfo = conn.getUserInfo();
				if(userInfo != null) {
					JSONObject prefs = userInfo.prefs;
					((EditTextPreference)findPreference("name")).setText(userInfo.name);
					findPreference("name").setSummary(userInfo.name);
					((EditTextPreference)findPreference("email")).setText(userInfo.email);
					findPreference("email").setSummary(userInfo.email);
					((EditTextPreference)findPreference("highlights")).setText(userInfo.highlights);
					findPreference("highlights").setSummary(userInfo.highlights);
					((CheckBoxPreference)findPreference("autoaway")).setChecked(userInfo.auto_away);
					if(prefs != null) {
						try {
							((CheckBoxPreference)findPreference("time-24hr")).setChecked(prefs.has("time-24hr")?prefs.getBoolean("time-24hr"):false);
							((CheckBoxPreference)findPreference("time-seconds")).setChecked(prefs.has("time-seconds")?prefs.getBoolean("time-seconds"):false);
							((CheckBoxPreference)findPreference("mode-showsymbol")).setChecked(prefs.has("mode-showsymbol")?prefs.getBoolean("mode-showsymbol"):false);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
				break;
			}
		}
    };
    
	Preference.OnPreferenceChangeListener settingstoggle = new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			if(preference.getKey().equals("name")) {
				conn.getUserInfo().name = (String)newValue;
				findPreference("name").setSummary((String)newValue);
			} else if(preference.getKey().equals("email")) {
				conn.getUserInfo().email = (String)newValue;
				findPreference("email").setSummary((String)newValue);
			} else if(preference.getKey().equals("highlights")) {
				conn.getUserInfo().highlights = (String)newValue;
				findPreference("highlights").setSummary((String)newValue);
			} else if(preference.getKey().equals("autoaway")) {
				conn.getUserInfo().auto_away = (Boolean)newValue;
			}
			if(saveSettingsTask != null)
				saveSettingsTask.cancel(true);
			saveSettingsTask = new SaveSettingsTask();
			saveSettingsTask.execute((Void)null);
			return true;
		}
	};
	
	Preference.OnPreferenceChangeListener prefstoggle = new Preference.OnPreferenceChangeListener() {
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			JSONObject prefs = conn.getUserInfo().prefs;
			try {
				if(prefs == null) {
					prefs = new JSONObject();
					conn.getUserInfo().prefs = prefs;
				}
	
				if(preference.getKey().equals("time-24hr")) {
					prefs.put("time-24hr", (Boolean)newValue);
				}
				if(preference.getKey().equals("time-seconds")) {
					prefs.put("time-seconds", (Boolean)newValue);
				}
				if(preference.getKey().equals("mode-showsymbol")) {
					prefs.put("mode-showsymbol", (Boolean)newValue);
				}
			} catch (JSONException e) {
				prefs = null;
			}
			if(savePreferencesTask != null)
				savePreferencesTask.cancel(true);
			if(prefs != null) {
				savePreferencesTask = new SavePreferencesTask();
				savePreferencesTask.execute((Void)null);
			}
			return true;
		}
	};
	
	Preference.OnPreferenceChangeListener notificationstoggle = new Preference.OnPreferenceChangeListener() {
		@SuppressWarnings("deprecation")
		public boolean onPreferenceChange(Preference preference, Object newValue) {
			switch(Integer.parseInt((String)newValue)) {
			case 0:
				findPreference("notify_type").setSummary("Disabled");
				break;
			case 1:
				findPreference("notify_type").setSummary("Enabled");
				break;
			case 2:
				findPreference("notify_type").setSummary("Only while active");
				break;
			}
			if(Integer.parseInt((String)newValue) > 0) {
				findPreference("notify_vibrate").setEnabled(true);
				findPreference("notify_ringtone").setEnabled(true);
				findPreference("notify_lights").setEnabled(true);
			} else {
				findPreference("notify_vibrate").setEnabled(false);
				findPreference("notify_ringtone").setEnabled(false);
				findPreference("notify_lights").setEnabled(false);
				Notifications.getInstance().clear();
			}
			return true;
		}
	};
	
	private class SavePreferencesTask extends AsyncTaskEx<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			if(!isCancelled() && conn.getUserInfo() != null && conn.getUserInfo().prefs != null)
				save_prefs_reqid = conn.set_prefs(conn.getUserInfo().prefs.toString());
			else
				save_prefs_reqid = -1;
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			savePreferencesTask = null;
		}
		
	};
	
	private class SaveSettingsTask extends AsyncTaskEx<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			if(!isCancelled()) {
				NetworkConnection.UserInfo userInfo = conn.getUserInfo();
				if(userInfo != null)
					save_settings_reqid = conn.set_user_settings(userInfo.email, userInfo.name, userInfo.highlights, userInfo.auto_away);
			}
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			saveSettingsTask = null;
		}
		
	};
	
	Preference.OnPreferenceClickListener urlClick = new Preference.OnPreferenceClickListener() {

		public boolean onPreferenceClick(Preference preference) {
			Intent i = null;
			if (preference.getKey().equals("faq"))
				i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.irccloud.com/faq"));
			if (preference.getKey().equals("feedback")) {
				ServersDataSource.Server s = ServersDataSource.getInstance().getServer("irc.irccloud.com");
				if(s != null && s.ssl > 0)
					i = new Intent(Intent.ACTION_VIEW, Uri.parse("ircs://irc.irccloud.com/%23android"));
				else
					i = new Intent(Intent.ACTION_VIEW, Uri.parse("irc://irc.irccloud.com/%23android"));
			} if (preference.getKey().equals("subscriptions"))
				i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.irccloud.com/#?/upgrade"));
			if (preference.getKey().equals("changes"))
				i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.irccloud.com/android-changelog.txt"));

			if (i != null)
				startActivity(i);
			return false;
		}
	};
}
