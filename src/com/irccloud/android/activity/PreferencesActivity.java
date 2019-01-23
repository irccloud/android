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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.irccloud.android.AppCompatEditTextPreference;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.BuildConfig;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.data.collection.NotificationsList;
import com.irccloud.android.R;
import com.irccloud.android.data.collection.EventsList;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.collection.ServersList;

import org.chromium.customtabsclient.shared.CustomTabsHelper;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class PreferencesActivity extends BaseActivity implements NetworkConnection.IRCEventHandler {
    private NetworkConnection conn;
    private SaveSettingsTask saveSettingsTask = null;
    private SavePreferencesTask savePreferencesTask = null;
    private String newpassword;
    private GoogleApiClient mGoogleApiClient;
    private SettingsFragment mSettingsFragment;

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if(getActivity() != null)
                ((PreferencesActivity)getActivity()).addPreferences();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (conn != null) {
            conn.removeHandler(this);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        if (getIntent() != null && getIntent().hasExtra(":android:show_fragment")) {
            getIntent().removeExtra(":android:show_fragment");
            super.onCreate(icicle);
            finish();
            return;
        }

        super.onCreate(icicle);
        boolean themeChanged = false;
        String theme = ColorScheme.getUserTheme();
        if (ColorScheme.getInstance().theme == null || !ColorScheme.getInstance().theme.equals(theme)) {
            themeChanged = true;
        }
        setTheme(ColorScheme.getDialogWhenLargeTheme(theme));
        ColorScheme.getInstance().setThemeFromContext(this, theme);
        if (themeChanged)
            EventsList.getInstance().clearCaches();
        onMultiWindowModeChanged(isMultiWindow());

        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setElevation(0);
        }

        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mSettingsFragment = new SettingsFragment();
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment, mSettingsFragment)
                .commit();

        conn = NetworkConnection.getInstance();
        conn.addHandler(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.CREDENTIALS_API)
                .build();
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        if(getWindowManager().getDefaultDisplay().getWidth() > TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 800, getResources().getDisplayMetrics()) && !isMultiWindow()) {
            params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 800, getResources().getDisplayMetrics());
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 800, getResources().getDisplayMetrics());
        } else {
            params.width = -1;
            params.height = -1;
        }
        getWindow().setAttributes(params);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    private void addPreferences() {
        if(mSettingsFragment == null)
            return;

        if(BuildConfig.ENTERPRISE) {
            JSONObject config = NetworkConnection.getInstance().config;
            try {
                if(config != null && config.has("auth_mechanism") && config.getString("auth_mechanism").equals("internal"))
                    addPreferencesFromResource(R.xml.preferences_account);
                else
                    addPreferencesFromResource(R.xml.preferences_account_enterprise);
            } catch (JSONException e) {
                addPreferencesFromResource(R.xml.preferences_account_enterprise);
            }
        } else {
            addPreferencesFromResource(R.xml.preferences_account);
        }
        addPreferencesFromResource(R.xml.preferences_display);
        addPreferencesFromResource(R.xml.preferences_message);
        addPreferencesFromResource(R.xml.preferences_embeds);
        addPreferencesFromResource(R.xml.preferences_device);
        addPreferencesFromResource(R.xml.preferences_photos);
        addPreferencesFromResource(R.xml.preferences_notifications);
        if(BuildConfig.DEBUG) {
            //addPreferencesFromResource(R.xml.preferences_debug);
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
        if(findPreference("change_password") != null)
            findPreference("change_password").setOnPreferenceClickListener(changePasswordClick);
        if(findPreference("delete_account") != null)
            findPreference("delete_account").setOnPreferenceClickListener(deleteAccountPasswordClick);
        if(findPreference("public_avatar") != null) {
            findPreference("public_avatar").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent(PreferencesActivity.this, AvatarsActivity.class);
                    startActivity(i);
                    return false;
                }
            });
            findPreference("avatars_faq").setOnPreferenceClickListener(urlClick);
        }
        findPreference("time-24hr").setOnPreferenceChangeListener(prefstoggle);
        findPreference("time-seconds").setOnPreferenceChangeListener(prefstoggle);
        findPreference("mode-showsymbol").setOnPreferenceChangeListener(prefstoggle);
        findPreference("pastebin-disableprompt").setOnPreferenceChangeListener(prefstoggle);
        findPreference("hideJoinPart").setOnPreferenceChangeListener(prefstoggle);
        findPreference("expandJoinPart").setOnPreferenceChangeListener(prefstoggle);
        findPreference("notifications_all").setOnPreferenceChangeListener(prefstoggle);
        findPreference("disableTrackUnread").setOnPreferenceChangeListener(prefstoggle);
        findPreference("enableReadOnSelect").setOnPreferenceChangeListener(prefstoggle);
        findPreference("ascii-compact").setOnPreferenceChangeListener(prefstoggle);
        if (findPreference("emoji-disableconvert") != null) {
            findPreference("emoji-disableconvert").setOnPreferenceChangeListener(prefstoggle);
            findPreference("emoji-disableconvert").setSummary(":thumbsup: → \uD83D\uDC4D");
            findPreference("emoji-nobig").setOnPreferenceChangeListener(prefstoggle);
        }
        findPreference("files-disableinline").setOnPreferenceChangeListener(prefstoggle);
        findPreference("inlineimages").setOnPreferenceChangeListener(prefstoggle);
        findPreference("nick-colors").setOnPreferenceChangeListener(prefstoggle);
        findPreference("mention-colors").setOnPreferenceChangeListener(prefstoggle);
        findPreference("chat-nocodespan").setOnPreferenceChangeListener(prefstoggle);
        findPreference("chat-nocodeblock").setOnPreferenceChangeListener(prefstoggle);
        findPreference("chat-noquote").setOnPreferenceChangeListener(prefstoggle);
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
        if(findPreference("notify_ringtone") != null)
            findPreference("notify_ringtone").setOnPreferenceClickListener(ringtoneClick);
        findPreference("changes").setOnPreferenceClickListener(urlClick);
        findPreference("notify_type").setOnPreferenceChangeListener(notificationstoggle);
        if(findPreference("notify_channels") != null)
            findPreference("notify_channels").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    NotificationsList.getInstance().pruneNotificationChannels();
                    return true;
                }
            });
        if(findPreference("notify_led_color") != null)
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

        if(BuildConfig.ENTERPRISE) {
            if(findPreference("public_avatar") != null) {
                PreferenceCategory c = (PreferenceCategory) findPreference("account");
                c.removePreference(findPreference("public_avatar"));
                c.removePreference(findPreference("avatars_faq"));
            }
            PreferenceCategory c = (PreferenceCategory) findPreference("message");
            c.removePreference(findPreference("avatar-images"));
        }

        try {
            final String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName + " (" + getPackageManager().getPackageInfo(getPackageName(), 0).versionCode + ")";
            findPreference("version").setSummary(version);
            findPreference("version").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("IRCCloud Version", version);
                    clipboard.setPrimaryClip(clip);
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

        if(findPreference("preferSystemEmoji") != null) {
            findPreference("preferSystemEmoji").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    Toast.makeText(PreferencesActivity.this, "This change will take effect next time the app is launched", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

        if(findPreference("notification_channels") != null) {
            findPreference("notification_channels").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent();
                    intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                    intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
                    startActivity(intent);
                    return false;
                }
            });
        }
    }

    private Preference findPreference(String key) {
        return mSettingsFragment.findPreference(key);
    }

    private void addPreferencesFromResource(int id) {
        mSettingsFragment.addPreferencesFromResource(id);
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
            if(findPreference("email") != null) {
                findPreference("email").setOnPreferenceClickListener(changeEmailClick);
                if (conn.getUserInfo() != null)
                    findPreference("email").setSummary(conn.getUserInfo().email);
                else
                    findPreference("email").setSummary(null);
            }
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
            if(findPreference("notify_led_color") != null) {
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
            if(findPreference("notify_led_color") != null) {
                if (Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("notify_type", "1")) > 0) {
                    findPreference("notify_vibrate").setEnabled(true);
                    findPreference("notify_ringtone").setEnabled(true);
                    findPreference("notify_led_color").setEnabled(true);
                } else {
                    findPreference("notify_vibrate").setEnabled(false);
                    findPreference("notify_ringtone").setEnabled(false);
                    findPreference("notify_led_color").setEnabled(false);
                }
            }
            if (findPreference("imgur_account_username") != null)
                findPreference("imgur_account_username").setSummary(getSharedPreferences("prefs", 0).getString("imgur_account_username", null));
            if (findPreference("image_service") != null)
                findPreference("image_service").setSummary(PreferenceManager.getDefaultSharedPreferences(this).getString("image_service", "IRCCloud"));
            if(findPreference("theme") != null)
                findPreference("theme").setSummary(ColorScheme.getUserTheme());
            if(findPreference("chat-oneline") != null) {
                if(((SwitchPreference) findPreference("chat-oneline")).isChecked()) {
                    if(((SwitchPreference) findPreference("avatars-off")).isChecked()) {
                        findPreference("time-left").setEnabled(false);
                    } else {
                        findPreference("time-left").setEnabled(true);
                    }
                } else {
                    findPreference("time-left").setEnabled(true);
                }
            }
            /*if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && ((ActivityManager)getSystemService(Context.ACTIVITY_SERVICE)).isLowRamDevice()) {
                if(findPreference("files-disableinline") != null)
                    ((PreferenceCategory) findPreference("embeds")).removePreference(findPreference("files-disableinline"));
                if(findPreference("inlineimages") != null)
                    ((PreferenceCategory) findPreference("embeds")).removePreference(findPreference("inlineimages"));
                if(findPreference("files-usemobiledata") != null)
                    ((PreferenceCategory) findPreference("embeds")).removePreference(findPreference("files-usemobiledata"));
            }*/
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
                            if(findPreference("email") != null) {
                                findPreference("email").setSummary(userInfo.email);
                            }
                            ((AppCompatEditTextPreference) findPreference("highlights")).setText(userInfo.highlights);
                            findPreference("highlights").setSummary(userInfo.highlights);
                            ((SwitchPreference) findPreference("autoaway")).setChecked(userInfo.auto_away);
                            if (prefs != null) {
                                try {
                                    ((SwitchPreference) findPreference("time-24hr")).setChecked(prefs.has("time-24hr") && prefs.get("time-24hr").getClass().equals(Boolean.class) && prefs.getBoolean("time-24hr"));
                                    ((SwitchPreference) findPreference("time-seconds")).setChecked(prefs.has("time-seconds") && prefs.get("time-seconds").getClass().equals(Boolean.class) && prefs.getBoolean("time-seconds"));
                                    ((SwitchPreference) findPreference("mode-showsymbol")).setChecked(prefs.has("mode-showsymbol") && prefs.get("mode-showsymbol").getClass().equals(Boolean.class) && prefs.getBoolean("mode-showsymbol"));
                                    ((SwitchPreference) findPreference("pastebin-disableprompt")).setChecked(!(prefs.has("pastebin-disableprompt") && prefs.get("pastebin-disableprompt").getClass().equals(Boolean.class) && prefs.getBoolean("pastebin-disableprompt")));
                                    if (findPreference("emoji-disableconvert") != null) {
                                        ((SwitchPreference) findPreference("emoji-disableconvert")).setChecked(!(prefs.has("emoji-disableconvert") && prefs.get("emoji-disableconvert").getClass().equals(Boolean.class) && prefs.getBoolean("emoji-disableconvert")));
                                        ((SwitchPreference) findPreference("emoji-nobig")).setChecked(!(prefs.has("emoji-nobig") && prefs.get("emoji-nobig").getClass().equals(Boolean.class) && prefs.getBoolean("emoji-nobig")));
                                    }
                                    ((SwitchPreference) findPreference("hideJoinPart")).setChecked(!(prefs.has("hideJoinPart") && prefs.get("hideJoinPart").getClass().equals(Boolean.class) && prefs.getBoolean("hideJoinPart")));
                                    ((SwitchPreference) findPreference("expandJoinPart")).setChecked(!(prefs.has("expandJoinPart") && prefs.get("expandJoinPart").getClass().equals(Boolean.class) && prefs.getBoolean("expandJoinPart")));
                                    ((SwitchPreference) findPreference("files-disableinline")).setChecked(!(prefs.has("files-disableinline") && prefs.get("files-disableinline").getClass().equals(Boolean.class) && prefs.getBoolean("files-disableinline")));
                                    ((SwitchPreference) findPreference("inlineimages")).setChecked((prefs.has("inlineimages") && prefs.get("inlineimages").getClass().equals(Boolean.class) && prefs.getBoolean("inlineimages")));
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

    Preference.OnPreferenceChangeListener settingstoggle = new Preference.OnPreferenceChangeListener() {
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (conn == null || conn.getUserInfo() == null) {
                Toast.makeText(PreferencesActivity.this, "An error occurred while saving settings.  Please try again shortly", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (preference.getKey().equals("name")) {
                conn.getUserInfo().name = (String) newValue;
                findPreference("name").setSummary((String) newValue);
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

    Preference.OnPreferenceChangeListener messagelayouttoggle = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object o) {
            mSettingsFragment.getView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(findPreference("chat-oneline") != null) {
                        if(((SwitchPreference) findPreference("chat-oneline")).isChecked()) {
                            if(((SwitchPreference) findPreference("avatars-off")).isChecked()) {
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
        public boolean onPreferenceChange(final Preference preference, final Object newValue) {
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

                if (preference.getKey().equals("inlineimages") && (Boolean)newValue) {
                    final JSONObject p = prefs;

                    AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
                    builder.setTitle("Warning");
                    builder.setMessage("External URLs may load insecurely and could result in your IP address being revealed to external site operators");

                    builder.setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                p.put(preference.getKey(), newValue);

                                if (savePreferencesTask != null)
                                    savePreferencesTask.cancel(true);
                                savePreferencesTask = new SavePreferencesTask();
                                savePreferencesTask.execute((Void) null);
                                ((SwitchPreference) findPreference("inlineimages")).setChecked(true);
                            } catch (Exception e) {
                                Crashlytics.log(Log.ERROR, "IRCCloud", "Unable to set preference: " + preference.getKey());
                                Crashlytics.logException(e);
                                Toast.makeText(PreferencesActivity.this, "An error occurred while saving settings.  Please try again shortly", Toast.LENGTH_SHORT).show();
                            }
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog d = builder.create();
                    if(!isFinishing())
                        d.setOwnerActivity(PreferencesActivity.this);
                    d.show();
                    return false;
                }

                if (preference.getKey().equals("disableTrackUnread") || preference.getKey().equals("emoji-disableconvert") || preference.getKey().equals("pastebin-disableprompt") || preference.getKey().equals("hideJoinPart") || preference.getKey().equals("expandJoinPart") || preference.getKey().equals("time-left") || preference.getKey().equals("avatars-off") || preference.getKey().equals("chat-oneline") || preference.getKey().equals("chat-norealname") || preference.getKey().equals("emoji-nobig") || preference.getKey().equals("files-disableinline") || preference.getKey().equals("chat-nocodespan") || preference.getKey().equals("chat-nocodeblock") || preference.getKey().equals("chat-noquote"))
                    prefs.put(preference.getKey(), !(Boolean) newValue);
                else if(preference.getKey().equals("monospace"))
                    prefs.put("font", ((Boolean)newValue)?"mono":"sans");
                else if(preference.getKey().equals("notifications_all"))
                    prefs.put("notifications-all", newValue);
                else
                    prefs.put(preference.getKey(), newValue);

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

    private class SavePreferencesTask extends AsyncTaskEx<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            if (!isCancelled() && conn.getUserInfo() != null && conn.getUserInfo().prefs != null)
                conn.set_prefs(conn.getUserInfo().prefs.toString(), new NetworkConnection.IRCResultCallback() {
                    @Override
                    public void onIRCResult(IRCCloudJSONObject result) {
                        if(!result.getBoolean("success")) {
                            Crashlytics.log(Log.ERROR, "IRCCloud", "Settings not updated: " + result.getString("message"));
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(PreferencesActivity.this, "An error occured while saving settings, please try again.", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
            EventsList.getInstance().clearCaches();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            savePreferencesTask = null;
        }

    }

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
                    conn.set_user_settings(userInfo.name, userInfo.highlights, userInfo.auto_away, new NetworkConnection.IRCResultCallback() {
                        @Override
                        public void onIRCResult(IRCCloudJSONObject result) {
                            if(!result.getBoolean("success")) {
                                Crashlytics.log(Log.ERROR, "IRCCloud", "Prefs not updated: " + result.getString("message"));
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(PreferencesActivity.this, "An error occured while saving settings, please try again.", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    });
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            saveSettingsTask = null;
        }

    }

    Preference.OnPreferenceClickListener imgurClick = new Preference.OnPreferenceClickListener() {

        public boolean onPreferenceClick(Preference preference) {
            SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
            editor.remove("imgur_account_username");
            editor.remove("imgur_access_token");
            editor.remove("imgur_refresh_token");
            editor.remove("imgur_token_type");
            editor.remove("imgur_expires_in");
            editor.apply();
            startActivity(new Intent(PreferencesActivity.this, ImgurAuthActivity.class));
            return false;
        }
    };

    Preference.OnPreferenceClickListener licensesClick = new Preference.OnPreferenceClickListener() {

        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);

            View v = getLayoutInflater().inflate(R.layout.dialog_licenses, null);
            TextView tv = v.findViewById(R.id.licenses);
            tv.setText(R.string.licenses);
            builder.setView(v);
            builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog d = builder.create();
            if(!isFinishing())
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
            if (preference.getKey().equals("avatars_faq"))
                url = "https://www.irccloud.com/faq#faq-avatars";
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
                    if (intent.startAnimationBundle != null) {
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

            d = ((RadioButton) v.findViewById(R.id.midnight)).getCompoundDrawables()[2].mutate();
            d.setColorFilter(getResources().getColor(R.color.midnight_black), PorterDuff.Mode.SRC_ATOP);
            ((RadioButton) v.findViewById(R.id.midnight)).setCompoundDrawables(null, null, d, null);

            final RadioGroup group = v.findViewById(R.id.radioGroup);
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
                    editor.apply();

                    if(!ColorScheme.getInstance().theme.equals(ColorScheme.getUserTheme()))
                        recreate();
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.setView(v);

            if(!isFinishing())
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
            if (ActivityCompat.checkSelfPermission(PreferencesActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
                r.uri = "android.resource://" + getPackageName() + "/raw/digit";
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

                String notification_uri = PreferenceManager.getDefaultSharedPreferences(PreferencesActivity.this).getString("notify_ringtone", "android.resource://" + getPackageName() + "/raw/digit");
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
                        editor.apply();
                    }
                });
                builder.setNegativeButton("Cancel", null);

                if(!isFinishing())
                    builder.show();
                return false;
            }
        }
    };

    Preference.OnPreferenceClickListener changePasswordClick = new Preference.OnPreferenceClickListener() {

        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
            builder.setTitle("Change Password");

            View v = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
            final EditText oldPassword = v.findViewById(R.id.oldpassword);
            final EditText newPassword = v.findViewById(R.id.newpassword);

            builder.setPositiveButton("Change Password", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    newpassword = newPassword.getText().toString();
                    conn.change_password(null, oldPassword.getText().toString(), newpassword, new NetworkConnection.IRCResultCallback() {
                        @Override
                        public void onIRCResult(IRCCloudJSONObject result) {
                            if(result.getBoolean("success")) {
                                if (mGoogleApiClient.isConnected()) {
                                    Credential.Builder builder = new Credential.Builder(conn.getUserInfo().email).setPassword(newpassword);
                                    if (conn.getUserInfo().name != null && conn.getUserInfo().name.length() > 0)
                                        builder.setName(conn.getUserInfo().name);
                                    Auth.CredentialsApi.save(mGoogleApiClient, builder.build()).setResultCallback(new ResultCallback<Status>() {
                                        @Override
                                        public void onResult(com.google.android.gms.common.api.Status status) {
                                            if (status.isSuccess()) {
                                                Log.e("IRCCloud", "Credentials saved");
                                            } else if (status.hasResolution()) {
                                                Log.e("IRCCloud", "Credentials require resolution");
                                                try {
                                                    startIntentSenderForResult(status.getResolution().getIntentSender(), 1000, null, 0, 0, 0);
                                                } catch (IntentSender.SendIntentException e) {
                                                }
                                            } else {
                                                Log.e("IRCCloud", "Credentials request failed");
                                            }
                                            newpassword = null;
                                        }
                                    });
                                } else {
                                    newpassword = null;
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(PreferencesActivity.this, "Your password has been successfully updated and all your other sessions have been logged out", Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                String error;
                                newpassword = null;
                                Crashlytics.log(Log.ERROR, "IRCCloud", "Password not changed: " + result.getString("message"));
                                switch(result.getString("message")) {
                                    case "oldpassword":
                                        error = "Current password incorrect";
                                        break;
                                    case "rate_limited":
                                        error = "Rate limited, try again in a few minutes";
                                        break;
                                    case "newpassword":
                                    case "password_error":
                                        error = "Invalid password, please try again";
                                        break;
                                    default:
                                        error = result.getString("message");
                                        break;
                                }
                                final String msg = "Error changing password: " + error;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(PreferencesActivity.this, msg, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    });
                    Answers.getInstance().logCustom(new CustomEvent("change-password"));
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.setView(v);

            if(!isFinishing())
                builder.show();
            return false;
        }
    };

    Preference.OnPreferenceClickListener changeEmailClick = new Preference.OnPreferenceClickListener() {

        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
            builder.setTitle("Change Email Address");

            View v = getLayoutInflater().inflate(R.layout.dialog_change_email, null);
            final EditText email = v.findViewById(R.id.email);
            email.setText(conn.getUserInfo().email);
            final EditText password = v.findViewById(R.id.password);

            builder.setPositiveButton("Change Email", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    conn.change_password(email.getText().toString(), password.getText().toString(), null, new NetworkConnection.IRCResultCallback() {
                        @Override
                        public void onIRCResult(IRCCloudJSONObject result) {
                            if(result.getBoolean("success")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(PreferencesActivity.this, "Your email address has been successfully updated", Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                String error;
                                Crashlytics.log(Log.ERROR, "IRCCloud", "Email not changed: " + result.getString("message"));
                                switch(result.getString("message")) {
                                    case "oldpassword":
                                        error = "Current password incorrect";
                                        break;
                                    case "rate_limited":
                                        error = "Rate limited, try again in a few minutes";
                                        break;
                                    case "newpassword":
                                    case "password_error":
                                        error = "Invalid password, please try again";
                                        break;
                                    case "email_not_unique":
                                        error = "This email address is already in use";
                                        break;
                                    default:
                                        error = result.getString("message");
                                        break;
                                }
                                final String msg = "Error changing email: " + error;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(PreferencesActivity.this, msg, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    });
                    Answers.getInstance().logCustom(new CustomEvent("change-email"));
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.setView(v);

            if(!isFinishing())
                builder.show();
            return false;
        }
    };

    Preference.OnPreferenceClickListener deleteAccountPasswordClick = new Preference.OnPreferenceClickListener() {

        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder builder = new AlertDialog.Builder(PreferencesActivity.this);
            builder.setTitle("Delete Your Account");

            View v = getLayoutInflater().inflate(R.layout.dialog_textprompt, null);
            final TextView prompt = v.findViewById(R.id.prompt);
            prompt.setText("Re-enter your password to confirm");
            final EditText textInput = v.findViewById(R.id.textInput);
            textInput.setTransformationMethod(PasswordTransformationMethod.getInstance());

            builder.setPositiveButton("Delete Account", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    conn.delete_account(textInput.getText().toString(), new NetworkConnection.IRCResultCallback() {
                        @Override
                        public void onIRCResult(IRCCloudJSONObject result) {
                            if(result.getBoolean("success")) {
                                conn.logout();
                                if (mGoogleApiClient.isConnected() && conn.getUserInfo() != null) {
                                    Credential.Builder builder = new Credential.Builder(conn.getUserInfo().email);
                                    if (conn.getUserInfo().name != null && conn.getUserInfo().name.length() > 0)
                                        builder.setName(conn.getUserInfo().name);
                                    Auth.CredentialsApi.delete(mGoogleApiClient, builder.build()).setResultCallback(new ResultCallback<Status>() {
                                        @Override
                                        public void onResult(com.google.android.gms.common.api.Status status) {
                                            Auth.CredentialsApi.disableAutoSignIn(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                                                @Override
                                                public void onResult(@NonNull Status status) {
                                                    Intent i = new Intent(PreferencesActivity.this, LoginActivity.class);
                                                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                    startActivity(i);
                                                    finish();
                                                }
                                            });
                                        }
                                    });
                                } else {
                                    Intent i = new Intent(PreferencesActivity.this, LoginActivity.class);
                                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(i);
                                    finish();
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(PreferencesActivity.this, "Your account has been deleted", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                final String error;
                                Crashlytics.log(Log.ERROR, "IRCCloud", "Account not deleted: " + result.getString("message"));
                                switch(result.getString("message")) {
                                    case "bad_pass":
                                        error = "Incorrect password, please try again";
                                        break;
                                    case "rate_limited":
                                        error = "Rate limited, try again in a few minutes";
                                        break;
                                    case "last_admin_cant_leave":
                                        error = "You can’t delete your account as the last admin of a team.  Please transfer ownership before continuing.";
                                        break;
                                    default:
                                        error = result.getString("message");
                                        break;
                                }
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(PreferencesActivity.this, error, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });
                }
            });
            builder.setNegativeButton("Cancel", null);
            builder.setView(v);

            final AlertDialog dialog = builder.create();
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFFF0000);
                }
            });
            if(!isFinishing())
                dialog.show();
            return false;
        }
    };
}
