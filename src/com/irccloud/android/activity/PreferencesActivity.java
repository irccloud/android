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

package com.irccloud.android.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatCallback;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.cgollner.unclouded.preferences.SwitchPreferenceCompat;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.irccloud.android.AppCompatEditTextPreference;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.BuildConfig;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.DashClock;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.data.collection.NotificationsList;
import com.irccloud.android.R;
import com.irccloud.android.data.collection.EventsList;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.collection.ServersList;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;

import org.chromium.customtabsclient.shared.CustomTabsHelper;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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
        if (conn != null) {
            conn.removeHandler(this);
        }
        getDelegate().onDestroy();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle icicle) {
        if(getIntent() != null && getIntent().hasExtra(":android:show_fragment")) {
            getIntent().removeExtra(":android:show_fragment");
            super.onCreate(icicle);
            finish();
            return;
        }

        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        super.onCreate(icicle);
        boolean themeChanged = false;
        String theme = ColorScheme.getUserTheme();
        if(ColorScheme.getInstance().theme == null || !ColorScheme.getInstance().theme.equals(theme)) {
            themeChanged = true;
        }
        setTheme(ColorScheme.getTheme(theme, false));
        ColorScheme.getInstance().setThemeFromContext(this, theme);
        if(themeChanged)
            EventsList.getInstance().clearCaches();

        getDelegate().installViewFactory();
        getDelegate().onCreate(icicle);
        if (Build.VERSION.SDK_INT >= 21) {
            Bitmap cloud = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.app_name), cloud, ColorScheme.getInstance().navBarColor));
            getWindow().setStatusBarColor(ColorScheme.getInstance().statusBarColor);
            getWindow().setNavigationBarColor(getResources().getColor(android.R.color.black));
        }
        getWindow().setBackgroundDrawableResource(ColorScheme.getInstance().windowBackgroundDrawable);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.actionbar_prefs);

        if(Build.VERSION.SDK_INT >= 23) {
            if(theme.equals("dawn"))
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            else
                getWindow().getDecorView().setSystemUiVisibility(getWindow().getDecorView().getSystemUiVisibility() &~ View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.actionbar);
        toolbar.setBackgroundResource(ColorScheme.getInstance().actionBarDrawable);
        toolbar.setTitle(getTitle());
        toolbar.setNavigationIcon(android.support.v7.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (Build.VERSION.SDK_INT >= 21)
            toolbar.setElevation(0);

        conn = NetworkConnection.getInstance();
        conn.addHandler(this);
        addPreferencesFromResource(R.xml.preferences_account);
        addPreferencesFromResource(R.xml.preferences_display);
        addPreferencesFromResource(R.xml.preferences_message);
        addPreferencesFromResource(R.xml.preferences_device);
        addPreferencesFromResource(R.xml.preferences_photos);
        addPreferencesFromResource(R.xml.preferences_notifications);
        addPreferencesFromResource(R.xml.preferences_dashclock);
        findPreference("dashclock_showmsgs").setOnPreferenceChangeListener(dashclocktoggle);
        try {
            int pebbleVersion = getPackageManager().getPackageInfo("com.getpebble.android", 0).versionCode;
            if (pebbleVersion < 553 || Build.VERSION.SDK_INT < 18)
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
        if(BuildConfig.DEBUG) {
            addPreferencesFromResource(R.xml.preferences_debug);
            /*findPreference("enable_cache").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    if((Boolean)o) {
                        Toast.makeText(PreferencesActivity.this, "Current backlog will be cached shortly", Toast.LENGTH_SHORT).show();
                        NetworkConnection.getInstance().save(10000);
                    } else {
                        NetworkConnection.getInstance().clearOfflineCache();
                        Toast.makeText(PreferencesActivity.this, "Offline cache was cleared", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });
            findPreference("clear_cache").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    NetworkConnection.getInstance().clearOfflineCache();
                    Toast.makeText(PreferencesActivity.this, "Offline cache was reset", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });*/
        }

        addPreferencesFromResource(R.xml.preferences_about);
        if(BuildConfig.DEBUG) {
            ((PreferenceCategory)findPreference("about")).removePreference(findPreference("beta_invite"));
        } else {
            findPreference("beta_invite").setOnPreferenceClickListener(urlClick);
        }

        findPreference("name").setOnPreferenceChangeListener(settingstoggle);
        findPreference("autoaway").setOnPreferenceChangeListener(settingstoggle);
        findPreference("time-24hr").setOnPreferenceChangeListener(prefstoggle);
        findPreference("time-seconds").setOnPreferenceChangeListener(prefstoggle);
        findPreference("mode-showsymbol").setOnPreferenceChangeListener(prefstoggle);
        findPreference("pastebin-disableprompt").setOnPreferenceChangeListener(prefstoggle);
        findPreference("hideJoinPart").setOnPreferenceChangeListener(prefstoggle);
        findPreference("expandJoinPart").setOnPreferenceChangeListener(prefstoggle);
        findPreference("notifications_all").setOnPreferenceChangeListener(prefstoggle);
        findPreference("disableTrackUnread").setOnPreferenceChangeListener(prefstoggle);
        findPreference("enableReadOnSelect").setOnPreferenceChangeListener(prefstoggle);
        if (findPreference("emoji-disableconvert") != null) {
            findPreference("emoji-disableconvert").setOnPreferenceChangeListener(prefstoggle);
            findPreference("emoji-disableconvert").setSummary(":thumbsup: → \uD83D\uDC4D");
        }
        findPreference("nick-colors").setOnPreferenceChangeListener(prefstoggle);
        findPreference("time-left").setOnPreferenceChangeListener(messagelayouttoggle);
        findPreference("avatars-off").setOnPreferenceChangeListener(messagelayouttoggle);
        findPreference("chat-oneline").setOnPreferenceChangeListener(messagelayouttoggle);
        findPreference("chat-norealname").setOnPreferenceChangeListener(messagelayouttoggle);
        findPreference("faq").setOnPreferenceClickListener(urlClick);
        findPreference("feedback").setOnPreferenceClickListener(urlClick);
        findPreference("licenses").setOnPreferenceClickListener(licensesClick);
        findPreference("imageviewer").setOnPreferenceChangeListener(imageviewertoggle);
        findPreference("imgur_account_username").setOnPreferenceClickListener(imgurClick);
        findPreference("theme").setOnPreferenceClickListener(themesClick);
        findPreference("notify_ringtone").setOnPreferenceClickListener(ringtoneClick);
        findPreference("changes").setOnPreferenceClickListener(urlClick);
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
            final String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName + " (" + getPackageManager().getPackageInfo(getPackageName(), 0).versionCode + ")";
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
            NetworkConnection.printStackTraceToCrashlytics(e);
        }

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 || CustomTabsHelper.getPackageNameToUse(this) == null) {
            PreferenceCategory c = (PreferenceCategory) findPreference("device");
            c.removePreference(findPreference("browser"));
        }

        getListView().setBackgroundColor(ColorScheme.getInstance().contentBackgroundColor);
        getListView().setCacheColorHint(ColorScheme.getInstance().contentBackgroundColor);
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
    protected void onPause() {
        super.onPause();
        IRCCloudApplication.getInstance().onPause(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        IRCCloudApplication.getInstance().onResume(this);

        String session = getSharedPreferences("prefs", 0).getString("session_key", "");
        if (session != null && session.length() > 0) {
            if (conn.getUserInfo() != null)
                findPreference("name").setSummary(conn.getUserInfo().name);
            else
                findPreference("name").setSummary(((AppCompatEditTextPreference) findPreference("name")).getText());
            findPreference("email").setOnPreferenceChangeListener(settingstoggle);
            if (conn.getUserInfo() != null)
                findPreference("email").setSummary(conn.getUserInfo().email);
            else
                findPreference("email").setSummary(((AppCompatEditTextPreference) findPreference("email")).getText());
            findPreference("highlights").setOnPreferenceChangeListener(settingstoggle);
            if (conn.getUserInfo() != null)
                findPreference("highlights").setSummary(conn.getUserInfo().highlights);
            else
                findPreference("highlights").setSummary(((AppCompatEditTextPreference) findPreference("highlights")).getText());

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
            if(findPreference("theme") != null)
                findPreference("theme").setSummary(ColorScheme.getUserTheme());
            if(findPreference("chat-oneline") != null) {
                if(((SwitchPreferenceCompat) findPreference("chat-oneline")).isChecked()) {
                    if(((SwitchPreferenceCompat) findPreference("avatars-off")).isChecked()) {
                        findPreference("time-left").setEnabled(false);
                    } else {
                        findPreference("time-left").setEnabled(true);
                    }
                } else {
                    findPreference("time-left").setEnabled(true);
                }
            }
        } else {
            Toast.makeText(this, "You must login to the IRCCloud app first", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @SuppressWarnings("deprecation")
    public void onIRCEvent(int what, Object obj) {
        IRCCloudJSONObject o;

        switch (what) {
            case NetworkConnection.EVENT_USERINFO:
                final NetworkConnection.UserInfo userInfo = conn.getUserInfo();
                if (userInfo != null) {
                    final JSONObject prefs = userInfo.prefs;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((AppCompatEditTextPreference) findPreference("name")).setText(userInfo.name);
                            findPreference("name").setSummary(userInfo.name);
                            ((AppCompatEditTextPreference) findPreference("email")).setText(userInfo.email);
                            findPreference("email").setSummary(userInfo.email);
                            ((AppCompatEditTextPreference) findPreference("highlights")).setText(userInfo.highlights);
                            findPreference("highlights").setSummary(userInfo.highlights);
                            ((CheckBoxPreference) findPreference("autoaway")).setChecked(userInfo.auto_away);
                            if (prefs != null) {
                                try {
                                    ((SwitchPreferenceCompat) findPreference("time-24hr")).setChecked(prefs.has("time-24hr") && prefs.get("time-24hr").getClass().equals(Boolean.class) && prefs.getBoolean("time-24hr"));
                                    ((SwitchPreferenceCompat) findPreference("time-seconds")).setChecked(prefs.has("time-seconds") && prefs.get("time-seconds").getClass().equals(Boolean.class) && prefs.getBoolean("time-seconds"));
                                    ((SwitchPreferenceCompat) findPreference("mode-showsymbol")).setChecked(prefs.has("mode-showsymbol") && prefs.get("mode-showsymbol").getClass().equals(Boolean.class) && prefs.getBoolean("mode-showsymbol"));
                                    ((SwitchPreferenceCompat) findPreference("pastebin-disableprompt")).setChecked(!(prefs.has("pastebin-disableprompt") && prefs.get("pastebin-disableprompt").getClass().equals(Boolean.class) && prefs.getBoolean("pastebin-disableprompt")));
                                    if (findPreference("emoji-disableconvert") != null)
                                        ((SwitchPreferenceCompat) findPreference("emoji-disableconvert")).setChecked(!(prefs.has("emoji-disableconvert") && prefs.get("emoji-disableconvert").getClass().equals(Boolean.class) && prefs.getBoolean("emoji-disableconvert")));
                                    ((SwitchPreferenceCompat) findPreference("hideJoinPart")).setChecked(!(prefs.has("hideJoinPart") && prefs.get("hideJoinPart").getClass().equals(Boolean.class) && prefs.getBoolean("hideJoinPart")));
                                    ((SwitchPreferenceCompat) findPreference("expandJoinPart")).setChecked(!(prefs.has("expandJoinPart") && prefs.get("expandJoinPart").getClass().equals(Boolean.class) && prefs.getBoolean("expandJoinPart")));
                                } catch (JSONException e) {
                                    NetworkConnection.printStackTraceToCrashlytics(e);
                                }
                            }
                        }
                    });
                }
                break;
        }
    }

    @Override
    public void onIRCRequestSucceeded(int reqid, IRCCloudJSONObject object) {
        if (reqid == save_settings_reqid) {
            save_settings_reqid = -1;
        } else if (reqid == save_prefs_reqid) {
            save_prefs_reqid = -1;
        }
    }

    @Override
    public void onIRCRequestFailed(int reqid, IRCCloudJSONObject object) {
        if (reqid == save_settings_reqid) {
            save_settings_reqid = -1;
            Log.e("IRCCloud", "Settings not updated: " + object.getString("message"));
        } else if (reqid == save_prefs_reqid) {
            save_prefs_reqid = -1;
            Log.e("IRCCloud", "Prefs not updated: " + object.getString("message"));
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PreferencesActivity.this, "An error occurred while saving settings.  Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
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
            EventsList.getInstance().clearCaches();
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

    Preference.OnPreferenceChangeListener messagelayouttoggle = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            getListView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(findPreference("chat-oneline") != null) {
                        if(((SwitchPreferenceCompat) findPreference("chat-oneline")).isChecked()) {
                            if(((SwitchPreferenceCompat) findPreference("avatars-off")).isChecked()) {
                                findPreference("time-left").setEnabled(false);
                            } else {
                                findPreference("time-left").setEnabled(true);
                            }
                        } else {
                            findPreference("time-left").setEnabled(true);
                        }
                    }
                }
            }, 100);
            EventsList.getInstance().clearCaches();
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
                }

                if (preference.getKey().equals("disableTrackUnread") || preference.getKey().equals("emoji-disableconvert") || preference.getKey().equals("pastebin-disableprompt") || preference.getKey().equals("hideJoinPart") || preference.getKey().equals("expandJoinPart") || preference.getKey().equals("time-left") || preference.getKey().equals("avatars-off") || preference.getKey().equals("chat-oneline") || preference.getKey().equals("chat-norealname"))
                    prefs.put(preference.getKey(), !(Boolean) newValue);
                else if(preference.getKey().equals("monospace"))
                    prefs.put("font", ((Boolean)newValue)?"mono":"sans");
                else if(preference.getKey().equals("notifications_all"))
                    prefs.put("notifications-all", (Boolean) newValue);
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
                NotificationsList.getInstance().clear();
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

    @Nullable
    @Override
    public ActionMode onWindowStartingSupportActionMode(ActionMode.Callback callback) {
        return null;
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
            EventsList.getInstance().clearCaches();
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
            StringBuilder sb = new StringBuilder(getResources().getString(R.string.licenses));
            sb.append(GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(PreferencesActivity.this));
            tv.setText(sb.toString());
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
            String url = null;
            if (preference.getKey().equals("faq"))
                url = "https://www.irccloud.com/faq";
            if (preference.getKey().equals("feedback")) {
                Server s = ServersList.getInstance().getServer("irc.irccloud.com");
                if (s != null && s.getSsl() > 0)
                    url = "ircs://irc.irccloud.com/%23feedback";
                else
                    url = "irc://irc.irccloud.com/%23feedback";
            }
            if (preference.getKey().equals("changes"))
                url = "https://github.com/irccloud/android/releases";
            if (preference.getKey().equals("beta_invite")) {
                Answers.getInstance().logCustom(new CustomEvent("beta_invite"));
                url = "https://play.google.com/apps/testing/" + getPackageName();
            }
            if(url != null) {
                Answers.getInstance().logCustom(new CustomEvent("prefs_url").putCustomAttribute("url", url));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                    builder.setToolbarColor(ColorScheme.getInstance().navBarColor);
                    builder.addDefaultShareMenuItem();
                    CustomTabsIntent intent = builder.build();
                    intent.intent.setData(Uri.parse(url));
                    if (Build.VERSION.SDK_INT >= 22)
                        intent.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse(Intent.URI_ANDROID_APP_SCHEME + "//" + getPackageName()));
                    if (Build.VERSION.SDK_INT >= 16 && intent.startAnimationBundle != null) {
                        startActivity(intent.intent, intent.startAnimationBundle);
                    } else {
                        startActivity(intent.intent);
                    }
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                }
            }
            finish();
            return false;
        }
    };

    Preference.OnPreferenceClickListener themesClick = new Preference.OnPreferenceClickListener() {

        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
            builder.setTitle("Choose a Theme");

            View v = getLayoutInflater().inflate(R.layout.dialog_theme, null);

            Drawable d = ((RadioButton)v.findViewById(R.id.dawn)).getCompoundDrawables()[2].mutate();
            d.setColorFilter(getResources().getColor(R.color.dawn_theme_preview), PorterDuff.Mode.SRC_ATOP);
            ((RadioButton)v.findViewById(R.id.dawn)).setCompoundDrawables(null, null, d, null);

            d = ((RadioButton)v.findViewById(R.id.dusk)).getCompoundDrawables()[2].mutate();
            d.setColorFilter(getResources().getColor(R.color.dusk_background0), PorterDuff.Mode.SRC_ATOP);
            ((RadioButton)v.findViewById(R.id.dusk)).setCompoundDrawables(null, null, d, null);

            d = ((RadioButton)v.findViewById(R.id.tropic)).getCompoundDrawables()[2].mutate();
            d.setColorFilter(getResources().getColor(R.color.tropic_background0), PorterDuff.Mode.SRC_ATOP);
            ((RadioButton)v.findViewById(R.id.tropic)).setCompoundDrawables(null, null, d, null);

            d = ((RadioButton)v.findViewById(R.id.emerald)).getCompoundDrawables()[2].mutate();
            d.setColorFilter(getResources().getColor(R.color.emerald_background0), PorterDuff.Mode.SRC_ATOP);
            ((RadioButton)v.findViewById(R.id.emerald)).setCompoundDrawables(null, null, d, null);

            d = ((RadioButton)v.findViewById(R.id.sand)).getCompoundDrawables()[2].mutate();
            d.setColorFilter(getResources().getColor(R.color.sand_background0), PorterDuff.Mode.SRC_ATOP);
            ((RadioButton)v.findViewById(R.id.sand)).setCompoundDrawables(null, null, d, null);

            d = ((RadioButton)v.findViewById(R.id.rust)).getCompoundDrawables()[2].mutate();
            d.setColorFilter(getResources().getColor(R.color.rust_background0), PorterDuff.Mode.SRC_ATOP);
            ((RadioButton)v.findViewById(R.id.rust)).setCompoundDrawables(null, null, d, null);

            d = ((RadioButton)v.findViewById(R.id.orchid)).getCompoundDrawables()[2].mutate();
            d.setColorFilter(getResources().getColor(R.color.orchid_background0), PorterDuff.Mode.SRC_ATOP);
            ((RadioButton)v.findViewById(R.id.orchid)).setCompoundDrawables(null, null, d, null);

            d = ((RadioButton)v.findViewById(R.id.ash)).getCompoundDrawables()[2].mutate();
            d.setColorFilter(getResources().getColor(R.color.ash_background0), PorterDuff.Mode.SRC_ATOP);
            ((RadioButton)v.findViewById(R.id.ash)).setCompoundDrawables(null, null, d, null);

            if(BuildConfig.DEBUG) {
                d = ((RadioButton) v.findViewById(R.id.midnight)).getCompoundDrawables()[2].mutate();
                d.setColorFilter(getResources().getColor(R.color.midnight_background3), PorterDuff.Mode.SRC_ATOP);
                ((RadioButton) v.findViewById(R.id.midnight)).setCompoundDrawables(null, null, d, null);
            } else {
                v.findViewById(R.id.midnight).setVisibility(View.GONE);
            }
            final RadioGroup group = (RadioGroup) v.findViewById(R.id.radioGroup);
            switch(ColorScheme.getUserTheme()) {
                case "dawn":
                    group.check(R.id.dawn);
                    break;
                case "dusk":
                    group.check(R.id.dusk);
                    break;
                case "tropic":
                    group.check(R.id.tropic);
                    break;
                case "emerald":
                    group.check(R.id.emerald);
                    break;
                case "sand":
                    group.check(R.id.sand);
                    break;
                case "rust":
                    group.check(R.id.rust);
                    break;
                case "orchid":
                    group.check(R.id.orchid);
                    break;
                case "ash":
                    group.check(R.id.ash);
                    break;
                case "midnight":
                    group.check(R.id.midnight);
                    break;
            }

            builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String theme = ColorScheme.getUserTheme();
                    switch(group.getCheckedRadioButtonId()) {
                        case R.id.dawn:
                            theme = "dawn";
                            break;
                        case R.id.dusk:
                            theme = "dusk";
                            break;
                        case R.id.tropic:
                            theme = "tropic";
                            break;
                        case R.id.emerald:
                            theme = "emerald";
                            break;
                        case R.id.sand:
                            theme = "sand";
                            break;
                        case R.id.rust:
                            theme = "rust";
                            break;
                        case R.id.orchid:
                            theme = "orchid";
                            break;
                        case R.id.ash:
                            theme = "ash";
                            break;
                        case R.id.midnight:
                            theme = "midnight";
                            break;
                    }

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PreferencesActivity.this);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("theme", theme);
                    editor.commit();

                    if(Build.VERSION.SDK_INT >= 11 && !ColorScheme.getInstance().theme.equals(ColorScheme.getUserTheme()))
                        recreate();
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.setView(v);

            builder.show();
            return false;
        }
    };

    private static class Ringtone {
        public String uri;
        public String title;
    }

    private final int REQUEST_EXTERNAL_MEDIA_IRCCLOUD = 1;

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_EXTERNAL_MEDIA_IRCCLOUD) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ringtoneClick.onPreferenceClick(null);
            else
                Toast.makeText(this, "Permission denied while trying to load ringtones list. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    Preference.OnPreferenceClickListener ringtoneClick = new Preference.OnPreferenceClickListener() {
        MediaPlayer mp = new MediaPlayer();

        public boolean onPreferenceClick(Preference preference) {
            if (Build.VERSION.SDK_INT >= 16 && ActivityCompat.checkSelfPermission(PreferencesActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(PreferencesActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_MEDIA_IRCCLOUD);
                return false;
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
                builder.setTitle("Notification Ringtones");

                RingtoneManager rm = new RingtoneManager(PreferencesActivity.this);
                rm.setType(RingtoneManager.TYPE_NOTIFICATION);
                Cursor c = rm.getCursor();
                final ArrayList<Ringtone> ringtones = new ArrayList<>(c.getCount() + 2);
                while (c.moveToNext()) {
                    Ringtone r = new Ringtone();
                    r.uri = c.getString(RingtoneManager.URI_COLUMN_INDEX) + "/" + c.getString(RingtoneManager.ID_COLUMN_INDEX);
                    r.title = c.getString(RingtoneManager.TITLE_COLUMN_INDEX);
                    ringtones.add(r);
                }

                Ringtone r = new Ringtone();
                r.title = "IRCCloud";
                r.uri = "android.resource://" + getPackageName() + "/" + R.raw.digit;
                ringtones.add(r);

                Collections.sort(ringtones, new Comparator<Ringtone>() {
                    @Override
                    public int compare(Ringtone r1, Ringtone r2) {
                        return r1.title.compareTo(r2.title);
                    }
                });

                r = new Ringtone();
                r.title = "Default ringtone";
                r.uri = "content://settings/system/notification_sound";
                ringtones.add(0, r);

                r = new Ringtone();
                r.title = "Silent";
                r.uri = "";
                ringtones.add(1, r);

                String notification_uri = PreferenceManager.getDefaultSharedPreferences(PreferencesActivity.this).getString("notify_ringtone", "android.resource://" + getPackageName() + "/" + R.raw.digit);
                int i = 0;
                int selection = 0;
                String[] items = new String[ringtones.size()];
                for (Ringtone j : ringtones) {
                    if (j.uri.equals(notification_uri))
                        selection = i;
                    items[i++] = j.title;
                }

                builder.setSingleChoiceItems(items, selection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (mp.isPlaying())
                            mp.stop();
                        mp.reset();
                        if (ringtones.get(i).uri.length() > 0) {
                            try {
                                mp.setDataSource(PreferencesActivity.this, Uri.parse(ringtones.get(i).uri));
                                mp.prepare();
                                mp.start();
                            } catch (Exception e) {
                            }
                        }
                    }
                });

                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(PreferencesActivity.this);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("notify_ringtone", ringtones.get(((AlertDialog) dialog).getListView().getCheckedItemPosition()).uri);
                        editor.commit();
                    }
                });
                builder.setNegativeButton("Cancel", null);

                builder.show();
                return false;
            }
        }
    };

}
