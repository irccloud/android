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

package com.irccloud.android.activity;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatCallback;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.BuildConfig;
import com.irccloud.android.DashClock;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.Notifications;
import com.irccloud.android.R;
import com.irccloud.android.data.EventsDataSource;
import com.irccloud.android.data.ServersDataSource;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;

import org.json.JSONException;
import org.json.JSONObject;

public class PreferencesActivity extends PreferenceActivity implements AppCompatCallback, NetworkConnection.IRCEventHandler {
    NetworkConnection conn;
    SaveSettingsTask saveSettingsTask = null;
    SavePreferencesTask savePreferencesTask = null;
    int save_prefs_reqid = -1;
    int save_settings_reqid = -1;
    private AppCompatDelegate appCompatDelegate;

    private AppCompatDelegate getDelegate() {
        if(appCompatDelegate == null) {
            appCompatDelegate = AppCompatDelegate.create(this, this);
        }
        return appCompatDelegate;
    }

    @Override
    public void onStop() {
        super.onStop();
        getDelegate().onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getDelegate().onDestroy();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle icicle) {
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        super.onCreate(icicle);
        getDelegate().installViewFactory();
        getDelegate().onCreate(icicle);
        if (Build.VERSION.SDK_INT >= 21) {
            Bitmap cloud = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.app_name), cloud, 0xFFF2F7FC));
            cloud.recycle();
        }
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.actionbar_prefs);

        Toolbar toolbar = (Toolbar) findViewById(R.id.actionbar);
        toolbar.setTitle(getTitle());
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        toolbar.setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar));

        if (Build.VERSION.SDK_INT >= 21)
            toolbar.setElevation(0);

        conn = NetworkConnection.getInstance();
        addPreferencesFromResource(R.xml.preferences_account);
        addPreferencesFromResource(R.xml.preferences_display);
        addPreferencesFromResource(R.xml.preferences_device);
        addPreferencesFromResource(R.xml.preferences_photos);
        addPreferencesFromResource(R.xml.preferences_notifications);
        addPreferencesFromResource(R.xml.preferences_dashclock);
        findPreference("dashclock_showmsgs").setOnPreferenceChangeListener(dashclocktoggle);
        try {
            int pebbleVersion = getPackageManager().getPackageInfo("com.getpebble.android", 0).versionCode;
            if (pebbleVersion < 553)
                addPreferencesFromResource(R.xml.preferences_pebble);
        } catch (Exception e) {
        }
        boolean foundSony = false;
        try {
            getPackageManager().getPackageInfo("com.sonyericsson.extras.liveware", 0);
            addPreferencesFromResource(R.xml.preferences_sony);
            foundSony = true;
        } catch (Exception e) {
        }
        if (!foundSony) {
            try {
                getPackageManager().getPackageInfo("com.sonyericsson.extras.smartwatch", 0);
                addPreferencesFromResource(R.xml.preferences_sony);
                foundSony = true;
            } catch (Exception e) {
            }
        }
        if (!foundSony) {
            try {
                getPackageManager().getPackageInfo("com.sonyericsson.extras.liveview", 0);
                addPreferencesFromResource(R.xml.preferences_sony);
                foundSony = true;
            } catch (Exception e) {
            }
        }
        if (foundSony)
            findPreference("notify_sony").setOnPreferenceChangeListener(sonytoggle);
        if(BuildConfig.DEBUG)
            addPreferencesFromResource(R.xml.preferences_debug);
        addPreferencesFromResource(R.xml.preferences_about);
        findPreference("name").setOnPreferenceChangeListener(settingstoggle);

        findPreference("autoaway").setOnPreferenceChangeListener(settingstoggle);
        findPreference("time-24hr").setOnPreferenceChangeListener(prefstoggle);
        findPreference("time-seconds").setOnPreferenceChangeListener(prefstoggle);
        findPreference("mode-showsymbol").setOnPreferenceChangeListener(prefstoggle);
        if (findPreference("emoji-disableconvert") != null) {
            findPreference("emoji-disableconvert").setOnPreferenceChangeListener(prefstoggle);
            findPreference("emoji-disableconvert").setSummary(":thumbsup: → \uD83D\uDC4D");
        }
        findPreference("nick-colors").setOnPreferenceChangeListener(prefstoggle);
        findPreference("faq").setOnPreferenceClickListener(urlClick);
        findPreference("feedback").setOnPreferenceClickListener(urlClick);
        findPreference("licenses").setOnPreferenceClickListener(licensesClick);
        findPreference("imageviewer").setOnPreferenceChangeListener(imageviewertoggle);
        findPreference("imgur_account_username").setOnPreferenceClickListener(imgurClick);
        //findPreference("subscriptions").setOnPreferenceClickListener(urlClick);
        //findPreference("changes").setOnPreferenceClickListener(urlClick);
        findPreference("notify_type").setOnPreferenceChangeListener(notificationstoggle);
        findPreference("notify_led_color").setOnPreferenceChangeListener(ledtoggle);
        findPreference("photo_size").setOnPreferenceChangeListener(photosizetoggle);

        imgurPreference = findPreference("imgur_account_username");
        if (NetworkConnection.getInstance().uploadsAvailable()) {
            if (!PreferenceManager.getDefaultSharedPreferences(this).getString("image_service", "IRCCloud").equals("imgur")) {
                PreferenceCategory c = (PreferenceCategory) findPreference("photos");
                c.removePreference(imgurPreference);
            }
            findPreference("image_service").setOnPreferenceChangeListener(imageservicetoggle);
        } else {
            PreferenceCategory c = (PreferenceCategory) findPreference("photos");
            c.removePreference(findPreference("image_service"));
        }

        try {
            final String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            findPreference("version").setSummary(version);
            findPreference("version").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText(version);
                    } else {
                        @SuppressLint("ServiceCast") android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("IRCCloud Version", version);
                        clipboard.setPrimaryClip(clip);
                    }
                    Toast.makeText(PreferencesActivity.this, "Version number copied to clipboard", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
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

        String session = getSharedPreferences("prefs", 0).getString("session_key", "");
        if (session != null && session.length() > 0) {

            conn = NetworkConnection.getInstance();
            conn.addHandler(this);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);

            if (conn.getUserInfo() != null)
                findPreference("name").setSummary(conn.getUserInfo().name);
            else
                findPreference("name").setSummary(((EditTextPreference) findPreference("name")).getText());
            findPreference("email").setOnPreferenceChangeListener(settingstoggle);
            if (conn.getUserInfo() != null)
                findPreference("email").setSummary(conn.getUserInfo().email);
            else
                findPreference("email").setSummary(((EditTextPreference) findPreference("email")).getText());
            findPreference("highlights").setOnPreferenceChangeListener(settingstoggle);
            if (conn.getUserInfo() != null)
                findPreference("highlights").setSummary(conn.getUserInfo().highlights);
            else
                findPreference("highlights").setSummary(((EditTextPreference) findPreference("highlights")).getText());

            switch (Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("notify_type", "1"))) {
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
            switch (Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("notify_led_color", "1"))) {
                case 0:
                    findPreference("notify_led_color").setSummary("Disabled");
                    break;
                case 1:
                    findPreference("notify_led_color").setSummary("Default Color");
                    break;
                case 2:
                    findPreference("notify_led_color").setSummary("Blue");
                    break;
            }
            switch (Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("photo_size", "1024"))) {
                case 512:
                    findPreference("photo_size").setSummary("Small");
                    break;
                case 1024:
                    findPreference("photo_size").setSummary("Medium");
                    break;
                case 2048:
                    findPreference("photo_size").setSummary("Large");
                    break;
                case -1:
                    findPreference("photo_size").setSummary("Original");
                    break;
            }
            if (Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("notify_type", "1")) > 0) {
                findPreference("notify_vibrate").setEnabled(true);
                findPreference("notify_ringtone").setEnabled(true);
                findPreference("notify_led_color").setEnabled(true);
            } else {
                findPreference("notify_vibrate").setEnabled(false);
                findPreference("notify_ringtone").setEnabled(false);
                findPreference("notify_led_color").setEnabled(false);
            }
            if (findPreference("imgur_account_username") != null)
                findPreference("imgur_account_username").setSummary(getSharedPreferences("prefs", 0).getString("imgur_account_username", null));
            if (findPreference("image_service") != null)
                findPreference("image_service").setSummary(PreferenceManager.getDefaultSharedPreferences(this).getString("image_service", "IRCCloud"));
        } else {
            Toast.makeText(this, "You must login to the IRCCloud app first", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (conn != null) {
            conn.removeHandler(this);
        }

        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }

    @SuppressWarnings("deprecation")
    public void onIRCEvent(int what, Object obj) {
        IRCCloudJSONObject o;

        switch (what) {
            case NetworkConnection.EVENT_SUCCESS:
                o = (IRCCloudJSONObject) obj;
                if (o.has("_reqid")) {
                    if (o.getInt("_reqid") == save_settings_reqid) {
                        save_settings_reqid = -1;
                    } else if (o.getInt("_reqid") == save_prefs_reqid) {
                        save_prefs_reqid = -1;
                    }
                }
                break;
            case NetworkConnection.EVENT_FAILURE_MSG:
                o = (IRCCloudJSONObject) obj;
                if (o.has("_reqid")) {
                    if (o.getInt("_reqid") == save_settings_reqid) {
                        save_settings_reqid = -1;
                        Log.e("IRCCloud", "Settings not updated: " + o.getString("message"));
                    } else if (o.getInt("_reqid") == save_prefs_reqid) {
                        save_prefs_reqid = -1;
                        Log.e("IRCCloud", "Prefs not updated: " + o.getString("message"));
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(PreferencesActivity.this, "An error occurred while saving settings.  Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_USERINFO:
                final NetworkConnection.UserInfo userInfo = conn.getUserInfo();
                if (userInfo != null) {
                    final JSONObject prefs = userInfo.prefs;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((EditTextPreference) findPreference("name")).setText(userInfo.name);
                            findPreference("name").setSummary(userInfo.name);
                            ((EditTextPreference) findPreference("email")).setText(userInfo.email);
                            findPreference("email").setSummary(userInfo.email);
                            ((EditTextPreference) findPreference("highlights")).setText(userInfo.highlights);
                            findPreference("highlights").setSummary(userInfo.highlights);
                            ((CheckBoxPreference) findPreference("autoaway")).setChecked(userInfo.auto_away);
                            if (prefs != null) {
                                try {
                                    ((CheckBoxPreference) findPreference("time-24hr")).setChecked(prefs.has("time-24hr") ? prefs.getBoolean("time-24hr") : false);
                                    ((CheckBoxPreference) findPreference("time-seconds")).setChecked(prefs.has("time-seconds") ? prefs.getBoolean("time-seconds") : false);
                                    ((CheckBoxPreference) findPreference("mode-showsymbol")).setChecked(prefs.has("mode-showsymbol") ? prefs.getBoolean("mode-showsymbol") : false);
                                    if (findPreference("emoji-disableconvert") != null)
                                        ((CheckBoxPreference) findPreference("emoji-disableconvert")).setChecked(!(prefs.has("emoji-disableconvert") ? prefs.getBoolean("emoji-disableconvert") : false));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }
                break;
        }
    }

    Preference.OnPreferenceChangeListener settingstoggle = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (conn == null || conn.getUserInfo() == null) {
                Toast.makeText(PreferencesActivity.this, "An error occurred while saving settings.  Please try again shortly", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (preference.getKey().equals("name")) {
                conn.getUserInfo().name = (String) newValue;
                findPreference("name").setSummary((String) newValue);
            } else if (preference.getKey().equals("email")) {
                conn.getUserInfo().email = (String) newValue;
                findPreference("email").setSummary((String) newValue);
            } else if (preference.getKey().equals("highlights")) {
                conn.getUserInfo().highlights = (String) newValue;
                findPreference("highlights").setSummary((String) newValue);
            } else if (preference.getKey().equals("autoaway")) {
                conn.getUserInfo().auto_away = (Boolean) newValue;
            }
            if (saveSettingsTask != null)
                saveSettingsTask.cancel(true);
            saveSettingsTask = new SaveSettingsTask();
            saveSettingsTask.execute((Void) null);
            return true;
        }
    };

    Preference.OnPreferenceChangeListener imageviewertoggle = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            EventsDataSource.getInstance().clearCaches();
            return true;
        }
    };

    Preference.OnPreferenceChangeListener dashclocktoggle = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            sendBroadcast(new Intent(DashClock.REFRESH_INTENT));
            return true;
        }
    };

    Preference.OnPreferenceChangeListener sonytoggle = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (!(Boolean) newValue) {
                NotificationUtil.deleteAllEvents(PreferencesActivity.this);
            }
            return true;
        }
    };

    Preference.OnPreferenceChangeListener prefstoggle = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (conn == null || conn.getUserInfo() == null) {
                Toast.makeText(PreferencesActivity.this, "An error occurred while saving settings.  Please try again shortly", Toast.LENGTH_SHORT).show();
                return false;
            }
            JSONObject prefs = conn.getUserInfo().prefs;
            try {
                if (prefs == null) {
                    prefs = new JSONObject();
                    conn.getUserInfo().prefs = prefs;
                    Crashlytics.logException(new Exception("Users prefs was null, creating new object"));
                }

                if (preference.getKey().equals("emoji-disableconvert"))
                    prefs.put(preference.getKey(), !(Boolean) newValue);
                else
                    prefs.put(preference.getKey(), (Boolean) newValue);

                if (savePreferencesTask != null)
                    savePreferencesTask.cancel(true);
                savePreferencesTask = new SavePreferencesTask();
                savePreferencesTask.execute((Void) null);
            } catch (JSONException e) {
                Crashlytics.log(Log.ERROR, "IRCCloud", "Unable to set preference: " + preference.getKey());
                Crashlytics.logException(e);
                Toast.makeText(PreferencesActivity.this, "An error occurred while saving settings.  Please try again shortly", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }
    };

    Preference.OnPreferenceChangeListener notificationstoggle = new Preference.OnPreferenceChangeListener() {
        @SuppressWarnings("deprecation")
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            switch (Integer.parseInt((String) newValue)) {
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
            if (Integer.parseInt((String) newValue) > 0) {
                findPreference("notify_vibrate").setEnabled(true);
                findPreference("notify_ringtone").setEnabled(true);
                findPreference("notify_led_color").setEnabled(true);
            } else {
                findPreference("notify_vibrate").setEnabled(false);
                findPreference("notify_ringtone").setEnabled(false);
                findPreference("notify_led_color").setEnabled(false);
                Notifications.getInstance().clear();
            }
            return true;
        }
    };

    Preference.OnPreferenceChangeListener ledtoggle = new Preference.OnPreferenceChangeListener() {
        @SuppressWarnings("deprecation")
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            switch (Integer.parseInt((String) newValue)) {
                case 0:
                    findPreference("notify_led_color").setSummary("Disabled");
                    break;
                case 1:
                    findPreference("notify_led_color").setSummary("Default Color");
                    break;
                case 2:
                    findPreference("notify_led_color").setSummary("Blue");
                    break;
            }
            return true;
        }
    };

    Preference.OnPreferenceChangeListener photosizetoggle = new Preference.OnPreferenceChangeListener() {
        @SuppressWarnings("deprecation")
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            switch (Integer.parseInt((String) newValue)) {
                case 512:
                    findPreference("photo_size").setSummary("Small");
                    break;
                case 1024:
                    findPreference("photo_size").setSummary("Medium");
                    break;
                case 2048:
                    findPreference("photo_size").setSummary("Large");
                    break;
                case -1:
                    findPreference("photo_size").setSummary("Original");
                    break;
            }
            return true;
        }
    };

    private Preference imgurPreference;

    Preference.OnPreferenceChangeListener imageservicetoggle = new Preference.OnPreferenceChangeListener() {
        @SuppressWarnings("deprecation")
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (newValue.equals("imgur")) {
                PreferenceCategory c = (PreferenceCategory) findPreference("photos");
                c.addPreference(imgurPreference);
            } else {
                PreferenceCategory c = (PreferenceCategory) findPreference("photos");
                c.removePreference(imgurPreference);
            }
            findPreference("image_service").setSummary((String) newValue);
            return true;
        }
    };

    @Override
    public void onSupportActionModeStarted(ActionMode mode) {

    }

    @Override
    public void onSupportActionModeFinished(ActionMode mode) {

    }

    private class SavePreferencesTask extends AsyncTaskEx<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            if (!isCancelled() && conn.getUserInfo() != null && conn.getUserInfo().prefs != null)
                save_prefs_reqid = conn.set_prefs(conn.getUserInfo().prefs.toString());
            else
                save_prefs_reqid = -1;
            EventsDataSource.getInstance().clearCaches();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            savePreferencesTask = null;
        }

    }

    ;

    private class SaveSettingsTask extends AsyncTaskEx<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            if (!isCancelled()) {
                NetworkConnection.UserInfo userInfo = conn.getUserInfo();
                if (userInfo != null)
                    save_settings_reqid = conn.set_user_settings(userInfo.email, userInfo.name, userInfo.highlights, userInfo.auto_away);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            saveSettingsTask = null;
        }

    }

    ;

    Preference.OnPreferenceClickListener imgurClick = new Preference.OnPreferenceClickListener() {

        public boolean onPreferenceClick(Preference preference) {
            SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
            editor.remove("imgur_account_username");
            editor.remove("imgur_access_token");
            editor.remove("imgur_refresh_token");
            editor.remove("imgur_token_type");
            editor.remove("imgur_expires_in");
            editor.commit();
            startActivity(new Intent(PreferencesActivity.this, ImgurAuthActivity.class));
            return false;
        }
    };

    Preference.OnPreferenceClickListener licensesClick = new Preference.OnPreferenceClickListener() {

        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);

            View v = getLayoutInflater().inflate(R.layout.dialog_licenses, null);
            TextView tv = (TextView) v.findViewById(R.id.licenses);
            tv.setText("IRCCloud\n" +
                    "Copyright (C) 2013 IRCCloud, Ltd.\n" +
                    "\n" +
                    "Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                    "you may not use this file except in compliance with the License.\n" +
                    "You may obtain a copy of the License at\n" +
                    "\n" +
                    "http://www.apache.org/licenses/LICENSE-2.0\n" +
                    "\n" +
                    "Unless required by applicable law or agreed to in writing, software\n" +
                    "distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                    "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                    "See the License for the specific language governing permissions and\n" +
                    "limitations under the License.\n" +
                    "\n" +
                    "You should have received a copy of the GNU General Public License\n" +
                    "along with this program.  If not, see &lt;http://www.gnu.org/licenses/&gt;.\n" +
                    "\n\n" +
                    "HybiParser.java: draft-ietf-hybi-thewebsocketprotocol-13 parser\n" +
                    "\n" +
                    "Based on code from the faye project.\n" +
                    "https://github.com/faye/faye-websocket-node\n" +
                    "Copyright (c) 2009-2012 James Coglan\n" +
                    "\n" +
                    "Ported from Javascript to Java by Eric Butler <eric@codebutler.com>\n" +
                    "\n" +
                    "(The MIT License)\n" +
                    "\n" +
                    "Permission is hereby granted, free of charge, to any person obtaining\n" +
                    "a copy of this software and associated documentation files (the\n" +
                    "\"Software\"), to deal in the Software without restriction, including\n" +
                    "without limitation the rights to use, copy, modify, merge, publish,\n" +
                    "distribute, sublicense, and/or sell copies of the Software, and to\n" +
                    "permit persons to whom the Software is furnished to do so, subject to\n" +
                    "the following conditions:\n" +
                    "\n" +
                    "The above copyright notice and this permission notice shall be\n" +
                    "included in all copies or substantial portions of the Software.\n" +
                    "\n" +
                    "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND,\n" +
                    "EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF\n" +
                    "MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND\n" +
                    "NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE\n" +
                    "LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION\n" +
                    "OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION\n" +
                    "WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.\n" +
                    "\n\n" +
                    "Sony LiveWare library\n" +
                    "Copyright (c) 2011, Sony Ericsson Mobile Communications AB\n" +
                    "Copyright (C) 2012-2013 Sony Mobile Communications AB\n" +
                    "\n" +
                    "All rights reserved.\n" +
                    "\n" +
                    "Redistribution and use in source and binary forms, with or without\n" +
                    "modification, are permitted provided that the following conditions are met:\n" +
                    "\n" +
                    "* Redistributions of source code must retain the above copyright notice, this\n" +
                    "  list of conditions and the following disclaimer.\n" +
                    "\n" +
                    "* Redistributions in binary form must reproduce the above copyright notice,\n" +
                    "  this list of conditions and the following disclaimer in the documentation\n" +
                    "  and/or other materials provided with the distribution.\n" +
                    "\n" +
                    "* Neither the name of the Sony Ericsson Mobile Communications AB nor the names\n" +
                    "  of its contributors may be used to endorse or promote products derived from\n" +
                    "  this software without specific prior written permission.\n" +
                    "\n" +
                    "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND\n" +
                    "ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED\n" +
                    "WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE\n" +
                    "DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE\n" +
                    "FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL\n" +
                    "DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR\n" +
                    "SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER\n" +
                    "CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,\n" +
                    "OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE\n" +
                    "OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.\n" +
                    "\n\n" +
                    "AsyncTaskEx\n" +
                    "Copyright (c) 2008-2009 CommonsWare, LLC\n" +
                    "Portions (c) 2009 Google, Inc.\n" +
                    "Licensed under the Apache License, Version 2.0 (the \"License\"); you may\n" +
                    "not use this file except in compliance with the License. You may obtain\n" +
                    "a copy of the License at\n" +
                    "http://www.apache.org/licenses/LICENSE-2.0\n" +
                    "Unless required by applicable law or agreed to in writing, software\n" +
                    "distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                    "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                    "See the License for the specific language governing permissions and\n" +
                    "limitations under the License.\n" +
                    "A subclass of the Android ListView component that enables drag\n" +
                    "and drop re-ordering of list items.\n" +
                    "\n\n" +
                    "DragSortListView\n" +
                    "Copyright 2012 Carl Bauer\n" +
                    "\n" +
                    "Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                    "you may not use this file except in compliance with the License.\n" +
                    "You may obtain a copy of the License at\n" +
                    "\n" +
                    "http://www.apache.org/licenses/LICENSE-2.0\n" +
                    "\n" +
                    "Unless required by applicable law or agreed to in writing, software\n" +
                    "distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                    "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                    "See the License for the specific language governing permissions and\n" +
                    "limitations under the License.\n\n" +
                    "Jackson JSON Parser by FasterXML\n" +
                    "\n" +
                    "Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
                    "you may not use this file except in compliance with the License.\n" +
                    "You may obtain a copy of the License at\n" +
                    "\n" +
                    "http://www.apache.org/licenses/LICENSE-2.0\n" +
                    "\n" +
                    "Unless required by applicable law or agreed to in writing, software\n" +
                    "distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
                    "WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
                    "See the License for the specific language governing permissions and\n" +
                    "limitations under the License.\n\n" +
                    "SeekBarPreference by RoboBunny\n\n" +
                    "SwitchPreferenceCompat by Christian Gollner\n\n" +
                    "The MIT License (MIT)\n" +
                    "\n" +
                    "Copyright (c) 2014 Andrew Chen\n" +
                    "\n" +
                    "Permission is hereby granted, free of charge, to any person obtaining a copy\n" +
                    "of this software and associated documentation files (the \"Software\"), to deal\n" +
                    "in the Software without restriction, including without limitation the rights\n" +
                    "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell\n" +
                    "copies of the Software, and to permit persons to whom the Software is\n" +
                    "furnished to do so, subject to the following conditions:\n" +
                    "\n" +
                    "The above copyright notice and this permission notice shall be included in all\n" +
                    "copies or substantial portions of the Software.\n" +
                    "\n" +
                    "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR\n" +
                    "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,\n" +
                    "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE\n" +
                    "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER\n" +
                    "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,\n" +
                    "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE\n" +
                    "SOFTWARE.\n");
            builder.setView(v);
            builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
            builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog d = builder.create();
            d.setOwnerActivity(PreferencesActivity.this);
            d.show();
            return false;
        }
    };

    Preference.OnPreferenceClickListener urlClick = new Preference.OnPreferenceClickListener() {

        public boolean onPreferenceClick(Preference preference) {
            Intent i = null;
            if (preference.getKey().equals("faq"))
                i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.irccloud.com/faq"));
            if (preference.getKey().equals("feedback")) {
                ServersDataSource.Server s = ServersDataSource.getInstance().getServer("irc.irccloud.com");
                if (s != null && s.ssl > 0)
                    i = new Intent(Intent.ACTION_VIEW, Uri.parse("ircs://irc.irccloud.com/%23feedback"));
                else
                    i = new Intent(Intent.ACTION_VIEW, Uri.parse("irc://irc.irccloud.com/%23feedback"));
            }
            if (preference.getKey().equals("subscriptions"))
                i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.irccloud.com/#?/upgrade"));
            if (preference.getKey().equals("changes"))
                i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.irccloud.com/android-changelog.txt"));

            if (i != null)
                startActivity(i);
            return false;
        }
    };
}
