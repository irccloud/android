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
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.ExifInterface;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.text.emoji.EmojiCompat;
import android.support.text.emoji.EmojiSpan;
import android.support.text.emoji.TypefaceEmojiSpan;
import android.support.v13.view.inputmethod.InputContentInfoCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.KeyboardShortcutGroup;
import android.view.KeyboardShortcutInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.commonsware.cwac.richedit.Effect;
import com.commonsware.cwac.richedit.RichEditText;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ShareEvent;
import com.damnhandy.uri.template.UriTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.irccloud.android.ActionEditText;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.BackgroundTaskService;
import com.irccloud.android.BuildConfig;
import com.irccloud.android.CollapsedEventsList;
import com.irccloud.android.ColorFormatter;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.FontAwesome;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.IRCCloudLinkMovementMethod;
import com.irccloud.android.IRCEditText;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.data.collection.AvatarsList;
import com.irccloud.android.data.collection.ImageList;
import com.irccloud.android.data.collection.NotificationsList;
import com.irccloud.android.R;
import com.irccloud.android.data.collection.RecentConversationsList;
import com.irccloud.android.data.model.Avatar;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.model.Channel;
import com.irccloud.android.data.collection.ChannelsList;
import com.irccloud.android.data.model.Event;
import com.irccloud.android.data.collection.EventsList;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.data.model.User;
import com.irccloud.android.data.collection.UsersList;
import com.irccloud.android.fragment.AcceptListFragment;
import com.irccloud.android.fragment.BufferOptionsFragment;
import com.irccloud.android.fragment.BuffersListFragment;
import com.irccloud.android.fragment.ChannelListFragment;
import com.irccloud.android.fragment.ChannelModeListFragment;
import com.irccloud.android.fragment.ChannelOptionsFragment;
import com.irccloud.android.fragment.EditConnectionFragment;
import com.irccloud.android.fragment.IRCColorPickerFragment;
import com.irccloud.android.fragment.IgnoreListFragment;
import com.irccloud.android.fragment.LinksListFragment;
import com.irccloud.android.fragment.MessageViewFragment;
import com.irccloud.android.fragment.NamesListFragment;
import com.irccloud.android.fragment.NickservFragment;
import com.irccloud.android.fragment.SpamFragment;
import com.irccloud.android.fragment.TextListFragment;
import com.irccloud.android.fragment.ServerReorderFragment;
import com.irccloud.android.fragment.UsersListFragment;
import com.irccloud.android.fragment.WhoListFragment;
import com.irccloud.android.fragment.WhoWasFragment;
import com.irccloud.android.fragment.WhoisFragment;

import org.chromium.customtabsclient.shared.CustomTabsHelper;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.UUID;

import static com.irccloud.android.fragment.MessageViewFragment.ROW_FILE;
import static com.irccloud.android.fragment.MessageViewFragment.ROW_THUMBNAIL;

public class MainActivity extends BaseActivity implements UsersListFragment.OnUserSelectedListener, BuffersListFragment.OnBufferSelectedListener, MessageViewFragment.MessageViewListener, NetworkConnection.IRCEventHandler, IRCColorPickerFragment.OnColorPickedListener {
    private Buffer buffer;
    private Server server;
    private ActionEditText messageTxt;
    private ImageButton sendBtn;
    private ImageButton photoBtn;
    private User selected_user;
    private View userListView;
    private View buffersListView;
    private TextView title;
    private TextView subtitle;
    private TextView key;
    private DrawerLayout drawerLayout;
    private NetworkConnection conn;
    private boolean shouldFadeIn = false;
    private RefreshUpIndicatorTask refreshUpIndicatorTask = null;
    private ExcludeBIDTask excludeBIDTask = null;
    private ArrayList<Integer> backStack = new ArrayList<Integer>();
    private int launchBid = -1;
    private Uri launchURI = null;
    private AlertDialog channelsListDialog;
    private String bufferToOpen = null;
    private int cidToOpen = -1;
    private Uri imageCaptureURI = null;
    private ProgressBar progressBar;
    private TextView errorMsg = null;
    private static Timer countdownTimer = null;
    private TimerTask countdownTimerTask = null;
    private String error = null;
    private TextWatcher textWatcher = null;
    private Intent pastebinResult = null;
    private ColorScheme colorScheme = ColorScheme.getInstance();

    private ColorFilter highlightsFilter;
    private ColorFilter unreadFilter;
    private ColorFilter normalFilter;
    private ColorFilter upDrawableFilter;

    private static final int REQUEST_EXTERNAL_MEDIA_IMGUR = 1;
    private static final int REQUEST_EXTERNAL_MEDIA_IRCCLOUD = 2;
    private static final int REQUEST_EXTERNAL_MEDIA_TAKE_PHOTO = 3;
    private static final int REQUEST_EXTERNAL_MEDIA_RECORD_VIDEO = 4;
    private static final int REQUEST_EXTERNAL_MEDIA_CHOOSE_PHOTO = 5;
    private static final int REQUEST_EXTERNAL_MEDIA_CHOOSE_DOCUMENT = 6;

    private String theme;

    private class SuggestionsAdapter extends ArrayAdapter<String> {
        public SuggestionsAdapter() {
            super(MainActivity.this, R.layout.row_suggestion);
            setNotifyOnChange(false);
        }

        public int activePos = -1;
        public boolean atMention = false;

        @Override
        public void clear() {
            super.clear();
            activePos = -1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView v = (TextView) super.getView(position, convertView, parent);

            if (position == activePos) {
                v.setTextColor(colorScheme.selectedBufferTextColor);
                v.setBackgroundColor(colorScheme.selectedBufferBackgroundColor);
            } else {
                v.setTextColor(colorScheme.bufferTextColor);
                v.setBackgroundColor(colorScheme.bufferBackgroundColor);
            }

            if(Build.VERSION.SDK_INT >= 19 && EmojiCompat.get().getLoadState() == EmojiCompat.LOAD_STATE_SUCCEEDED)
                v.setText(EmojiCompat.get().process(getItem(position)));

            //This will prevent GridView from stealing focus from the EditText by bypassing the check on line 1397 of GridView.java in the Android Source
            v.setSelected(true);
            return v;
        }
    }

    private SuggestionsAdapter suggestionsAdapter;
    private View suggestionsContainer;
    private GridView suggestions;
    private static Timer suggestionsTimer = null;
    private TimerTask suggestionsTimerTask = null;
    private ArrayList<User> sortedUsers = null;
    private ArrayList<Channel> sortedChannels = null;
    private ImgurUploadTask imgurTask = null;
    private FileUploadTask fileUploadTask = null;
    private ActionBar actionBar = null;

    private Drawable upDrawable;

    private HashMap<Integer, Event> pendingEvents = new HashMap<>();
    private int lastDrawerWidth = 0;

    private IRCColorPickerFragment mColorPickerFragment;
    private ActionMode formattingActionMode = null;
    private ActionMode currentActionMode = null;

    @Override
    public void onActionModeStarted(ActionMode mode) {
        super.onActionModeStarted(mode);

        if(mode != formattingActionMode)
            currentActionMode = mode;
    }

    @Override
    public void onColorPicked(int color, boolean background) {
        if(background)
            messageTxt.applyBackgroundColor(color);
        else
            messageTxt.applyForegroundColor(color);

        if (Build.VERSION.SDK_INT >= 16) {
            mColorPickerFragment.getView().animate().alpha(0).withEndAction(new Runnable() {
                @Override
                public void run() {
                    mColorPickerFragment.getView().setVisibility(View.GONE);
                }
            });
        } else {
            mColorPickerFragment.getView().setVisibility(View.GONE);
        }
    }

    private void startFormatActionMode() {
        formattingActionMode = startActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                getMenuInflater().inflate(R.menu.formatting_action_mode, menu);
                menu.findItem(R.id.menu_bold).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.findItem(R.id.menu_italics).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.findItem(R.id.menu_underline).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.findItem(R.id.menu_color).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                if(getWindowManager().getDefaultDisplay().getWidth() >= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 400, getResources().getDisplayMetrics())) {
                    menu.findItem(R.id.menu_background).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    menu.findItem(R.id.menu_clear).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                }
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                int activeColor = colorScheme.isDarkTheme ? 0xFFFFFFFF : 0xFF000000;
                int inactiveColor = colorScheme.colorControlNormal;

                VectorDrawableCompat d = VectorDrawableCompat.create(getResources(), R.drawable.ic_format_bold, null);
                if(d != null) {
                    d.setTint(messageTxt.hasEffect(RichEditText.BOLD) ? activeColor : inactiveColor);
                    menu.findItem(R.id.menu_bold).setIcon(d);
                }

                d = VectorDrawableCompat.create(getResources(), R.drawable.ic_format_italics, null);
                if(d != null) {
                    d.setTint(messageTxt.hasEffect(RichEditText.ITALIC) ? activeColor : inactiveColor);
                    menu.findItem(R.id.menu_italics).setIcon(d);
                }

                d = VectorDrawableCompat.create(getResources(), R.drawable.ic_format_underline, null);
                if(d != null) {
                    d.setTint(messageTxt.hasEffect(RichEditText.UNDERLINE) ? activeColor : inactiveColor);
                    menu.findItem(R.id.menu_underline).setIcon(d);
                }

                d = VectorDrawableCompat.create(getResources(), R.drawable.ic_format_color, null);
                if(d != null) {
                    d.setTint(messageTxt.hasEffect(RichEditText.FOREGROUND) ? messageTxt.getEffectValue(RichEditText.FOREGROUND) : inactiveColor);
                    menu.findItem(R.id.menu_color).setIcon(d);
                }

                d = VectorDrawableCompat.create(getResources(), R.drawable.ic_format_background, null);
                if(d != null) {
                    d.setTint(messageTxt.hasEffect(RichEditText.BACKGROUND) ? messageTxt.getEffectValue(RichEditText.BACKGROUND) : inactiveColor);
                    menu.findItem(R.id.menu_background).setIcon(d);
                }

                d = VectorDrawableCompat.create(getResources(), R.drawable.ic_format_clear, null);
                if(d != null) {
                    d.setTint(inactiveColor);
                    menu.findItem(R.id.menu_clear).setIcon(d);
                }
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                Bundle b;

                switch (menuItem.getItemId()) {
                    case R.id.menu_bold:
                        messageTxt.toggleTypingEffect(RichEditText.BOLD);
                        return true;
                    case R.id.menu_italics:
                        messageTxt.toggleTypingEffect(RichEditText.ITALIC);
                        return true;
                    case R.id.menu_underline:
                        messageTxt.toggleTypingEffect(RichEditText.UNDERLINE);
                        return true;
                    case R.id.menu_clear:
                        messageTxt.clearTypingEffects();
                        messageTxt.onSelectionChanged(messageTxt.getSelectionStart(), messageTxt.getSelectionEnd());
                        if(mColorPickerFragment.getView().getVisibility() == View.VISIBLE) {
                            if (Build.VERSION.SDK_INT >= 16) {
                                mColorPickerFragment.getView().animate().alpha(0).withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        mColorPickerFragment.getView().setVisibility(View.GONE);
                                    }
                                });
                            } else {
                                mColorPickerFragment.getView().setVisibility(View.GONE);
                            }
                        }
                        return true;
                    case R.id.menu_color:
                        b = new Bundle();
                        b.putBoolean(IRCColorPickerFragment.ARG_BACKGROUND, false);
                        if (mColorPickerFragment.getView().getVisibility() == View.GONE) {
                            mColorPickerFragment.setArguments(b);
                            if (Build.VERSION.SDK_INT >= 16) {
                                mColorPickerFragment.getView().setAlpha(0);
                                mColorPickerFragment.getView().animate().alpha(1.0f);
                            }
                            mColorPickerFragment.getView().setVisibility(View.VISIBLE);
                        } else {
                            if(mColorPickerFragment.isBackground()) {
                                mColorPickerFragment.setArguments(b);
                            } else {
                                if (Build.VERSION.SDK_INT >= 16) {
                                    mColorPickerFragment.getView().animate().alpha(0).withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            mColorPickerFragment.getView().setVisibility(View.GONE);
                                        }
                                    });
                                } else {
                                    mColorPickerFragment.getView().setVisibility(View.GONE);
                                }
                            }
                        }
                        return true;
                    case R.id.menu_background:
                        b = new Bundle();
                        b.putBoolean(IRCColorPickerFragment.ARG_BACKGROUND, true);
                        if (mColorPickerFragment.getView().getVisibility() == View.GONE) {
                            mColorPickerFragment.setArguments(b);
                            if (Build.VERSION.SDK_INT >= 16) {
                                mColorPickerFragment.getView().setAlpha(0);
                                mColorPickerFragment.getView().animate().alpha(1.0f);
                            }
                            mColorPickerFragment.getView().setVisibility(View.VISIBLE);
                        } else {
                            if(!mColorPickerFragment.isBackground()) {
                                mColorPickerFragment.setArguments(b);
                            } else {
                                if (Build.VERSION.SDK_INT >= 16) {
                                    mColorPickerFragment.getView().animate().alpha(0).withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            mColorPickerFragment.getView().setVisibility(View.GONE);
                                        }
                                    });
                                } else {
                                    mColorPickerFragment.getView().setVisibility(View.GONE);
                                }
                            }
                        }
                        return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                formattingActionMode = null;
            }
        });
    }

    @SuppressLint("NewApi")
    @SuppressWarnings({"deprecation", "unchecked"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = ColorScheme.getUserTheme();
        setTheme(ColorScheme.getTheme(theme, false));
        suggestionsTimer = new Timer("suggestions-timer");
        countdownTimer = new Timer("messsage-countdown-timer");

        highlightsFilter = new PorterDuffColorFilter(Color.RED, PorterDuff. Mode.SRC_ATOP);
        unreadFilter = new PorterDuffColorFilter(colorScheme.unreadBlueColor, PorterDuff. Mode.SRC_ATOP);
        normalFilter = new PorterDuffColorFilter(colorScheme.navBarSubheadingColor, PorterDuff. Mode.SRC_ATOP);

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, filter);

        filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(timeZoneReceiver, filter);

        setContentView(R.layout.activity_message);

        suggestionsAdapter = new SuggestionsAdapter();
        progressBar = findViewById(R.id.progress);
        errorMsg = findViewById(R.id.errorMsg);
        buffersListView = findViewById(R.id.BuffersList);
        drawerLayout = findViewById(R.id.drawerLayout);

        mColorPickerFragment = (IRCColorPickerFragment)getSupportFragmentManager().findFragmentById(R.id.messageViewFragment).getChildFragmentManager().findFragmentById(R.id.colorPickerFragmet);
        mColorPickerFragment.getView().setVisibility(View.GONE);

        final ImageButton formatBtn = findViewById(R.id.formatBtn);
        formatBtn.setColorFilter(colorScheme.colorControlNormal, PorterDuff.Mode.SRC_ATOP);
        formatBtn.setFocusable(false);
        formatBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(formattingActionMode != null)
                    formattingActionMode.finish();
                else
                    startFormatActionMode();
            }
        });
        messageTxt = findViewById(R.id.messageTxt);
        ActionMode.Callback formatCallback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                getMenuInflater().inflate(R.menu.format, menu);
                VectorDrawableCompat d = VectorDrawableCompat.create(getResources(), R.drawable.ic_text_format, null);
                d.setTint(colorScheme.isDarkTheme ? 0xFFFFFFFF : 0xFF000000);
                menu.findItem(R.id.menu_format).setIcon(d);
                menu.findItem(R.id.menu_format).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                final int selectionStart = messageTxt.getSelectionStart();
                final int selectionEnd = messageTxt.getSelectionEnd();

                if(menuItem.getItemId() == R.id.menu_format) {
                    if(currentActionMode != null)
                        currentActionMode.finish();
                    startFormatActionMode();
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                        messageTxt.setCurrentActionMode(formattingActionMode);
                    messageTxt.setSelection(selectionStart, selectionEnd);
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

            }
        };
        messageTxt.setCustomSelectionActionModeCallback(formatCallback);
        messageTxt.setOnSelectionChangedListener(new RichEditText.OnSelectionChangedListener() {
            @Override
            public void onSelectionChanged(int start, int end, List<Effect<?>> list) {
                if(formattingActionMode != null)
                    formattingActionMode.invalidate();
                formatBtn.setColorFilter(messageTxt.hasEffect(RichEditText.FOREGROUND) ? messageTxt.getEffectValue(RichEditText.FOREGROUND) : colorScheme.colorControlNormal, PorterDuff.Mode.SRC_ATOP);
                formatBtn.setBackgroundColor(messageTxt.hasEffect(RichEditText.BACKGROUND) ? messageTxt.getEffectValue(RichEditText.BACKGROUND) : colorScheme.textareaBackgroundColor);
            }
        });
        messageTxt.setKeyboardShortcutsEnabled(false);
        messageTxt.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (sendBtn.isEnabled() && NetworkConnection.getInstance().getState() == NetworkConnection.STATE_CONNECTED && event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && messageTxt.getText() != null && messageTxt.getText().length() > 0) {
                    sendBtn.setEnabled(false);
                    new SendTask().execute((Void) null);
                } else if (keyCode == KeyEvent.KEYCODE_TAB) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN)
                        nextSuggestion();
                    return true;
                }
                return false;
            }
        });
        messageTxt.setOnIMEImageReceivedListener(new ActionEditText.OnIMEImageReceivedListener() {
            @Override
            public boolean onIMEImageReceived(InputContentInfoCompat info) {
                if(info.getLinkUri() != null && info.getLinkUri().getScheme().startsWith("http")) {
                    if(messageTxt.getText().length() > 0 && !messageTxt.getText().toString().endsWith(" "))
                        messageTxt.getText().append(" ");
                    messageTxt.getText().append(info.getLinkUri().toString());
                } else {
                    Uri uri = (info.getLinkUri() != null) ? makeTempCopy(info.getContentUri(), MainActivity.this, info.getLinkUri().getLastPathSegment()) : makeTempCopy(info.getContentUri(), MainActivity.this);
                    if (!NetworkConnection.getInstance().uploadsAvailable() || PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("image_service", "IRCCloud").equals("imgur")) {
                        new ImgurRefreshTask(uri).execute((Void) null);
                    } else {
                        fileUploadTask = new FileUploadTask(uri, MainActivity.this);
                        if (Build.VERSION.SDK_INT >= 16 && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    REQUEST_EXTERNAL_MEDIA_IRCCLOUD);
                        } else {
                            fileUploadTask.execute((Void) null);
                        }
                    }
                }
                return true;
            }
        });
        messageTxt.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (drawerLayout != null && v == messageTxt && hasFocus) {
                    drawerLayout.closeDrawers();
                    update_suggestions(false);
                } else if (!hasFocus) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            suggestionsContainer.setVisibility(View.INVISIBLE);
                        }
                    });
                }
            }
        });
        messageTxt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout != null) {
                    drawerLayout.closeDrawers();
                }
            }
        });
        messageTxt.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                if (sendBtn.isEnabled() && NetworkConnection.getInstance().getState() == NetworkConnection.STATE_CONNECTED && actionId == EditorInfo.IME_ACTION_SEND && messageTxt.getText() != null && messageTxt.getText().length() > 0) {
                    sendBtn.setEnabled(false);
                    new SendTask().execute((Void) null);
                }
                return true;
            }
        });
        textWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                Object[] spans = s.getSpans(0, s.length(), Object.class);
                for (Object o : spans) {
                    if (((s.getSpanFlags(o) & Spanned.SPAN_COMPOSING) != Spanned.SPAN_COMPOSING) && o instanceof CharacterStyle && o.getClass() != StyleSpan.class && o.getClass() != ForegroundColorSpan.class && o.getClass() != BackgroundColorSpan.class && o.getClass() != UnderlineSpan.class && o.getClass() != StrikethroughSpan.class && o.getClass() != EmojiSpan.class && o.getClass() != TypefaceEmojiSpan.class) {
                        s.removeSpan(o);
                    }
                }
                if (s.length() > 0 && NetworkConnection.getInstance().getState() == NetworkConnection.STATE_CONNECTED) {
                    sendBtn.setEnabled(true);
                } else {
                    sendBtn.setEnabled(false);
                }
                String text = s.toString();
                if (text.endsWith("\t")) { //Workaround for Swype
                    text = text.substring(0, text.length() - 1);
                    messageTxt.setTextWithEmoji(text);
                    nextSuggestion();
                } else if (suggestionsContainer != null && suggestionsContainer.getVisibility() == View.VISIBLE) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update_suggestions(false);
                        }
                    });
                } else {
                    if(suggestionsTimer != null) {
                        if (suggestionsTimerTask != null)
                            suggestionsTimerTask.cancel();
                        suggestionsTimerTask = new TimerTask() {
                            @Override
                            public void run() {
                                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                                update_suggestions(false);
                            }
                        };
                        suggestionsTimer.schedule(suggestionsTimerTask, 250);
                    }
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        messageTxt.addTextChangedListener(textWatcher);
        sendBtn = findViewById(R.id.sendBtn);
        sendBtn.setColorFilter(colorScheme.colorControlNormal, PorterDuff.Mode.SRC_ATOP);
        sendBtn.setFocusable(false);
        sendBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (NetworkConnection.getInstance().getState() == NetworkConnection.STATE_CONNECTED)
                    new SendTask().execute((Void) null);
            }
        });

        photoBtn = findViewById(R.id.photoBtn);
        if (photoBtn != null) {
            photoBtn.setColorFilter(colorScheme.colorControlNormal, PorterDuff.Mode.SRC_ATOP);
            photoBtn.setFocusable(false);
            photoBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    insertPhoto();
                }
            });
        }
        userListView = findViewById(R.id.usersListFragment);

        View v = getLayoutInflater().inflate(R.layout.actionbar_messageview, null);
        v.findViewById(R.id.actionTitleArea).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(buffer != null) {
                    Channel c = ChannelsList.getInstance().getChannelForBuffer(buffer.getBid());
                    if(c != null)
                        show_topic_popup(c);
                }
            }
        });
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setCustomView(v);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            upDrawable = VectorDrawableCompat.create(getResources(), R.drawable.ic_action_navigation_menu, null).mutate();
        else
            upDrawable = getResources().getDrawable(R.drawable.ic_action_navigation_menu, null).mutate();
        upDrawable.setColorFilter(normalFilter);
        upDrawableFilter = normalFilter;
        drawerLayout.setDrawerListener(mDrawerListener);

        messageTxt.setDrawerLayout(drawerLayout);

        title = v.findViewById(R.id.title);
        subtitle = v.findViewById(R.id.subtitle);
        key = v.findViewById(R.id.key);
        key.setTypeface(FontAwesome.getTypeface());

        if (savedInstanceState != null && savedInstanceState.containsKey("cid")) {
            server = ServersList.getInstance().getServer(savedInstanceState.getInt("cid"));
            buffer = BuffersList.getInstance().getBuffer(savedInstanceState.getInt("bid"));
            backStack = (ArrayList<Integer>) savedInstanceState.getSerializable("backStack");
        } else if(NetworkConnection.getInstance().ready && NetworkConnection.getInstance().getUserInfo() != null) {
            buffer = BuffersList.getInstance().getBuffer(NetworkConnection.getInstance().getUserInfo().last_selected_bid);
            if(buffer != null)
                server = buffer.getServer();
        }

        if (savedInstanceState != null && savedInstanceState.containsKey("imagecaptureuri"))
            imageCaptureURI = Uri.parse(savedInstanceState.getString("imagecaptureuri"));
        else
            imageCaptureURI = null;

        ConfigInstance config = (ConfigInstance) getLastCustomNonConfigurationInstance();
        if (config != null) {
            imgurTask = config.imgurUploadTask;
            fileUploadTask = config.fileUploadTask;
        }

        drawerLayout.setScrimColor(0);
        drawerLayout.closeDrawers();

        actionBar.setElevation(0);
        if(conn == null) {
            conn = NetworkConnection.getInstance();
            conn.addHandler(this);
        }

        adjustTabletLayout();

        drawerLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(drawerLayout.getWidth() != lastDrawerWidth) {
                    lastDrawerWidth = drawerLayout.getWidth();
                    adjustTabletLayout();
                    updateUsersListFragmentVisibility();
                    supportInvalidateOptionsMenu();
                }
            }
        });

        View.OnDragListener dragListener = new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                switch(dragEvent.getAction()) {
                    case DragEvent.ACTION_DRAG_STARTED:
                        if(dragEvent.getLocalState() == null) {
                            ClipDescription d = dragEvent.getClipDescription();
                            if(d == null)
                                return false;
                            for (int i = 0; i < d.getMimeTypeCount(); i++) {
                                if (d.getMimeType(i).startsWith("text/") || d.getMimeType(i).startsWith("image/") || d.getMimeType(i).startsWith("video/") || d.getMimeType(i).startsWith("application/")) {
                                    findViewById(R.id.drop_target).setVisibility(View.VISIBLE);
                                    return true;
                                }
                            }
                        }
                        break;
                    case DragEvent.ACTION_DROP:
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                            requestDragAndDropPermissions(dragEvent);
                        ClipData c = dragEvent.getClipData();
                        for(int i = 0; i < c.getItemCount(); i++) {
                            ClipData.Item item = c.getItemAt(i);
                            if(item.getUri() != null) {
                                String type = getContentResolver().getType(item.getUri());
                                if(type != null) {
                                    Uri uri = makeTempCopy(item.getUri(), MainActivity.this);
                                    if (!NetworkConnection.getInstance().uploadsAvailable() || PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("image_service", "IRCCloud").equals("imgur")) {
                                        new ImgurRefreshTask(uri).execute((Void) null);
                                    } else {
                                        fileUploadTask = new FileUploadTask(uri, MainActivity.this);
                                        if (Build.VERSION.SDK_INT >= 16 && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                            ActivityCompat.requestPermissions(MainActivity.this,
                                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                    REQUEST_EXTERNAL_MEDIA_IRCCLOUD);
                                        } else {
                                            fileUploadTask.execute((Void) null);
                                        }
                                    }
                                    return true;
                                }
                            } else if(item.getText().length() > 0) {
                                if(buffer != null)
                                    buffer.setDraft(item.getText());
                                messageTxt.setTextWithEmoji(item.getText());
                                return true;
                            }
                        }
                        return false;
                    case DragEvent.ACTION_DRAG_ENDED:
                        findViewById(R.id.drop_target).setVisibility(View.GONE);
                        return true;
                }
                return false;
            }
        };

        MessageViewFragment mvf = (MessageViewFragment)getSupportFragmentManager().findFragmentById(R.id.messageViewFragment);
        if(mvf != null && mvf.getView() != null)
           mvf.getView().setOnDragListener(dragListener);
        messageTxt.setOnDragListener(dragListener);

        ImageView img = findViewById(R.id.drop_target_icon);
        img.getDrawable().mutate().setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public void onMultiWindowModeChanged(boolean inMultiWindow) {
        super.onMultiWindowModeChanged(inMultiWindow);
        lastDrawerWidth = drawerLayout.getWidth();
        adjustTabletLayout();
        updateUsersListFragmentVisibility();
        supportInvalidateOptionsMenu();
    }

    private void adjustTabletLayout() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if(toolbar != null) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && getResources().getBoolean(R.bool.isTablet) && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("tabletMode", true) && !isMultiWindow()) {
                toolbar.setNavigationIcon(null);
                findViewById(R.id.BuffersListDocked).setVisibility(View.VISIBLE);
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
            } else {
                toolbar.setNavigationIcon(upDrawable);
                toolbar.setNavigationContentDescription("Show navigation drawer");
                findViewById(R.id.BuffersListDocked).setVisibility(View.GONE);
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.LEFT);
                if (actionBar != null)
                    actionBar.setHomeButtonEnabled(true);
                if (refreshUpIndicatorTask != null)
                    refreshUpIndicatorTask.cancel(true);
                refreshUpIndicatorTask = new RefreshUpIndicatorTask();
                refreshUpIndicatorTask.execute((Void) null);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(timeZoneReceiver);
        unregisterReceiver(screenReceiver);
        if(countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
        if(suggestionsTimer != null) {
            suggestionsTimer.cancel();
            suggestionsTimer = null;
        }
        if(messageTxt != null) {
            messageTxt.setDrawerLayout(null);
            if(textWatcher != null)
                messageTxt.removeTextChangedListener(textWatcher);
            messageTxt.setTextWithEmoji(null);
        }
        textWatcher = null;
        fileUploadTask = null;
        imgurTask = null;
        for (Event e : pendingEvents.values()) {
            try {
                if(e.expiration_timer != null)
                    e.expiration_timer.cancel();
            } catch (Exception ex) {
                //Task already cancelled
            }
            e.expiration_timer = null;
            e.failed = true;
            e.bg_color = colorScheme.errorBackgroundColor;
        }
        pendingEvents.clear();
        if (conn != null)
            conn.removeHandler(this);
        conn = null;
    }

    private void updateReconnecting() {
        if (conn == null)
            return;

        if (conn.getState() == NetworkConnection.STATE_CONNECTED) {
            actionBar.setTitle("Loading");
            actionBar.setSubtitle(null);
        } else if (conn.getState() == NetworkConnection.STATE_CONNECTING || conn.getReconnectTimestamp() > 0) {
            if(actionBar != null) {
                actionBar.setDisplayShowCustomEnabled(false);
                actionBar.setDisplayShowTitleEnabled(true);
            }
            progressBar.setProgress(0);
            progressBar.setIndeterminate(true);
            if (progressBar.getVisibility() != View.VISIBLE) {
                if (Build.VERSION.SDK_INT >= 16) {
                    progressBar.setAlpha(0);
                    progressBar.animate().alpha(1).setDuration(200);
                }
                progressBar.setVisibility(View.VISIBLE);
            }
            if (conn.getState() == NetworkConnection.STATE_DISCONNECTED && conn.getReconnectTimestamp() > 0) {
                int seconds = (int) ((conn.getReconnectTimestamp() - System.currentTimeMillis()) / 1000);
                if (seconds < 1) {
                    actionBar.setTitle("Connecting");
                    actionBar.setSubtitle(null);
                    errorMsg.setVisibility(View.GONE);
                } else if (seconds >= 10) {
                    actionBar.setTitle("Reconnecting in 0:" + seconds);
                    actionBar.setSubtitle(null);
                    if (error != null && error.length() > 0) {
                        errorMsg.setText(error);
                        errorMsg.setVisibility(View.VISIBLE);
                    } else {
                        errorMsg.setVisibility(View.GONE);
                        error = null;
                    }
                } else {
                    actionBar.setTitle("Reconnecting in 0:0" + seconds);
                    actionBar.setSubtitle(null);
                    errorMsg.setVisibility(View.GONE);
                    error = null;
                }
                try {
                    if(countdownTimer != null) {
                        if (countdownTimerTask != null)
                            countdownTimerTask.cancel();
                        countdownTimerTask = new TimerTask() {
                            public void run() {
                                if (conn != null) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            updateReconnecting();
                                        }
                                    });
                                }
                            }
                        };
                        countdownTimer.schedule(countdownTimerTask, 1000);
                    }
                } catch (Exception e) {
                }
            } else {
                actionBar.setTitle("Connecting");
                actionBar.setSubtitle(null);
                error = null;
                errorMsg.setVisibility(View.GONE);
            }
        } else {
            progressBar.setVisibility(View.GONE);
            progressBar.setIndeterminate(false);
            progressBar.setProgress(0);
            if(buffer == null) {
                actionBar.setTitle("Offline");
                actionBar.setSubtitle(null);
                if(actionBar != null) {
                    actionBar.setDisplayShowCustomEnabled(false);
                    actionBar.setDisplayShowTitleEnabled(true);
                }
            } else {
                if(actionBar != null) {
                    actionBar.setDisplayShowCustomEnabled(true);
                    actionBar.setDisplayShowTitleEnabled(false);
                }
                update_subtitle();
            }
        }
    }

    private void show_pastebin_prompt() {
        Intent i = new Intent(this, PastebinEditorActivity.class);
        i.putExtra("paste_contents", messageTxt.getText().toString());
        startActivityForResult(i, REQUEST_PASTEBIN);
    }

    private void show_topic_popup(final Channel c) {
        if (c != null) {
            Server s = ServersList.getInstance().getServer(c.cid);
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            View v = getLayoutInflater().inflate(R.layout.dialog_topic, null);
            String heading = "Topic for " + c.name;
            if(s != null) {
                heading += " (" + ((s.getName() != null && s.getName().length() > 0)? s.getName() : s.getHostname()) + ")";
            }
            ((TextView) v.findViewById(R.id.topic_heading)).setText(heading);
            if (c.topic_text.length() > 0) {
                String author = "";
                if (c.topic_author != null && c.topic_author.length() > 0) {
                    author = "— Set by " + c.topic_author;
                    if (c.topic_time > 0) {
                        author += " on " + DateFormat.getDateTimeInstance().format(new Date(c.topic_time * 1000));
                    }
                    v.findViewById(R.id.setBy).setVisibility(View.VISIBLE);
                    ((TextView) v.findViewById(R.id.setBy)).setText(author);
                }
                ((TextView) v.findViewById(R.id.topic)).setText(ColorFormatter.html_to_spanned(ColorFormatter.emojify(ColorFormatter.irc_to_html(TextUtils.htmlEncode(c.topic_text))), true, server));
            } else {
                ((TextView) v.findViewById(R.id.topic)).setText("No topic set.");
            }
            if (c.mode != null && c.mode.length() > 0) {
                v.findViewById(R.id.mode).setVisibility(View.VISIBLE);
                ((TextView) v.findViewById(R.id.mode)).setText("Mode: +" + c.mode);

                for (Channel.Mode m : c.modes) {
                    switch (m.mode) {
                        case "i":
                            v.findViewById(R.id.mode_i).setVisibility(View.VISIBLE);
                            break;
                        case "k":
                            v.findViewById(R.id.mode_k).setVisibility(View.VISIBLE);
                            ((TextView) v.findViewById(R.id.key)).setText(m.param);
                            break;
                        case "m":
                            v.findViewById(R.id.mode_m).setVisibility(View.VISIBLE);
                            break;
                        case "n":
                            v.findViewById(R.id.mode_n).setVisibility(View.VISIBLE);
                            break;
                        case "p":
                            v.findViewById(R.id.mode_p).setVisibility(View.VISIBLE);
                            break;
                        case "s":
                            v.findViewById(R.id.mode_s).setVisibility(View.VISIBLE);
                            break;
                        case "t":
                            v.findViewById(R.id.mode_t).setVisibility(View.VISIBLE);
                            break;
                    }
                }
            }
            builder.setView(v);
            builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            boolean canEditTopic = false;
            if (c.hasMode("t")) {
                if(s != null) {
                    User self_user = UsersList.getInstance().getUser(c.bid, s.getNick());
                    canEditTopic = (self_user != null && (self_user.mode.contains(s.MODE_OPER) || self_user.mode.contains(s.MODE_OWNER) || self_user.mode.contains(s.MODE_ADMIN) || self_user.mode.contains(server.MODE_OP) || self_user.mode.contains(server.MODE_HALFOP)));
                }
            } else {
                canEditTopic = c.bid > 0;
            }

            if (canEditTopic) {
                builder.setPositiveButton("Edit Topic", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        editTopic(c);
                    }
                });
            }
            final AlertDialog dialog = builder.create();
            dialog.setOwnerActivity(MainActivity.this);
            dialog.show();

            ((TextView) v.findViewById(R.id.topic)).setMovementMethod(new IRCCloudLinkMovementMethod() {
                @Override
                public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
                    if (super.onTouchEvent(widget, buffer, event) && event.getAction() == MotionEvent.ACTION_UP) {
                        dialog.dismiss();
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private void update_suggestions(boolean force) {
        if (buffer != null && suggestionsContainer != null && messageTxt != null && messageTxt.getText() != null) {
            String text;
            try {
                text = messageTxt.getText().toString();
            } catch (Exception e) {
                text = "";
            }
            if (text.lastIndexOf(' ') > 0 && text.lastIndexOf(' ') < text.length() - 1) {
                text = text.substring(text.lastIndexOf(' ') + 1);
            }
            if (text.endsWith(":"))
                text = text.substring(0, text.length() - 1);
            if (text.startsWith("@")) {
                suggestionsAdapter.atMention = true;
                text = text.substring(1);
            } else {
                suggestionsAdapter.atMention = false;
            }
            text = text.toLowerCase();
            final ArrayList<String> sugs = new ArrayList<String>();
            HashSet<String> sugs_set = new HashSet<String>();
            if (text.length() > 1 || force || (text.length() > 0 && suggestionsAdapter.activePos != -1)) {
                ArrayList<Channel> channels = sortedChannels;
                if (channels == null) {
                    channels = ChannelsList.getInstance().getChannels();
                    Collections.sort(channels, new Comparator<Channel>() {
                        @Override
                        public int compare(Channel lhs, Channel rhs) {
                            return lhs.name.compareTo(rhs.name);
                        }
                    });

                    sortedChannels = channels;
                }

                if (buffer != null && messageTxt.getText().length() > 0 && buffer.isChannel() && buffer.getName().toLowerCase().startsWith(text) && !sugs_set.contains(buffer.getName())) {
                    sugs_set.add(buffer.getName());
                    sugs.add(buffer.getName());
                }
                if(channels != null) {
                    for (Channel channel : channels) {
                        if (text.length() > 0 && text.charAt(0) == channel.name.charAt(0) && channel.name.toLowerCase().startsWith(text) && !sugs_set.contains(channel.name)) {
                            sugs_set.add(channel.name);
                            sugs.add(channel.name);
                        }
                    }
                }

                JSONObject disableAutoSuggest = null;
                if (NetworkConnection.getInstance().getUserInfo() != null && NetworkConnection.getInstance().getUserInfo().prefs != null) {
                    try {
                        if (NetworkConnection.getInstance().getUserInfo().prefs.has("channel-disableAutoSuggest"))
                            disableAutoSuggest = NetworkConnection.getInstance().getUserInfo().prefs.getJSONObject("channel-disableAutoSuggest");
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        NetworkConnection.printStackTraceToCrashlytics(e);
                    }
                }

                boolean disabled;
                try {
                    disabled = disableAutoSuggest != null && disableAutoSuggest.has(String.valueOf(buffer.getBid())) && disableAutoSuggest.getBoolean(String.valueOf(buffer.getBid()));
                } catch (JSONException e) {
                    disabled = false;
                }
                if(!PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("enableAutoSuggest", true))
                    disabled = true;

                ArrayList<User> users = sortedUsers;
                if (users == null && buffer != null && (force || !disabled)) {
                    users = UsersList.getInstance().getUsersForBuffer(buffer.getBid());
                    if (users != null) {
                        Collections.sort(users, new Comparator<User>() {
                            @Override
                            public int compare(User lhs, User rhs) {
                                if (lhs.last_mention > rhs.last_mention)
                                    return -1;
                                if (lhs.last_mention < rhs.last_mention)
                                    return 1;
                                return lhs.nick.compareToIgnoreCase(rhs.nick);
                            }
                        });
                    }
                    sortedUsers = users;
                }
                if (users != null) {
                    for (User user : users) {
                        String nick = user.nick_lowercase;
                        if (text.matches("^[a-zA-Z0-9]+.*"))
                            nick = nick.replaceFirst("^[^a-zA-Z0-9]+", "");

                        if (nick.startsWith(text) && !sugs_set.contains(user.nick)) {
                            sugs_set.add(user.nick);
                            sugs.add(user.nick);
                        }
                    }
                }
            }

            if (text.startsWith(":") && text.length() > 1) {
                String q = text.toLowerCase().substring(1);
                for (String emocode : ColorFormatter.emojiMap.keySet()) {
                    if (emocode.startsWith(q)) {
                        String emoji = ColorFormatter.emojiMap.get(emocode);
                        if (!sugs_set.contains(emoji)) {
                            sugs_set.add(emoji);
                            sugs.add(emoji);
                        }
                    }
                }
            }

            if(sugs.size() == 0 && suggestionsContainer.getVisibility() == View.INVISIBLE)
                return;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (sugs.size() > 0) {
                        if (suggestionsAdapter.activePos == -1) {
                            suggestionsAdapter.clear();
                            for (String s : sugs) {
                                suggestionsAdapter.add(s);
                            }
                            suggestionsAdapter.notifyDataSetChanged();
                            suggestions.smoothScrollToPosition(0);
                        }
                        if (suggestionsContainer.getVisibility() == View.INVISIBLE) {
                            if (Build.VERSION.SDK_INT < 16) {
                                AlphaAnimation anim = new AlphaAnimation(0, 1);
                                anim.setDuration(250);
                                anim.setFillAfter(true);
                                suggestionsContainer.startAnimation(anim);
                            } else {
                                suggestionsContainer.setAlpha(0);
                                suggestionsContainer.setTranslationY(1000);
                                suggestionsContainer.animate().alpha(1).translationY(0).setInterpolator(new DecelerateInterpolator());
                            }
                            suggestionsContainer.setVisibility(View.VISIBLE);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (suggestionsContainer.getHeight() < 48) {
                                        if(actionBar != null)
                                            actionBar.hide();
                                    }
                                }
                            });
                        }
                    } else {
                        if (suggestionsContainer.getVisibility() == View.VISIBLE) {
                            if (Build.VERSION.SDK_INT < 16) {
                                AlphaAnimation anim = new AlphaAnimation(1, 0);
                                anim.setDuration(250);
                                anim.setFillAfter(true);
                                anim.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {

                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        suggestionsContainer.setVisibility(View.INVISIBLE);
                                        suggestionsAdapter.clear();
                                        suggestionsAdapter.notifyDataSetChanged();
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) {

                                    }
                                });
                                suggestionsContainer.startAnimation(anim);
                            } else {
                                suggestionsContainer.animate().alpha(1).translationY(1000).setInterpolator(new AccelerateInterpolator()).withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        suggestionsContainer.setVisibility(View.INVISIBLE);
                                        suggestionsAdapter.clear();
                                        suggestionsAdapter.notifyDataSetChanged();
                                    }
                                });
                            }
                            sortedUsers = null;
                            sortedChannels = null;
                            if (actionBar != null && !actionBar.isShowing())
                                actionBar.show();
                        }
                    }
                }
            });
        }
    }

    private void nextSuggestion() {
        if (suggestionsAdapter.getCount() == 0)
            update_suggestions(true);

        if (suggestionsAdapter.getCount() > 0) {
            if (suggestionsAdapter.activePos < 0 || suggestionsAdapter.activePos >= suggestionsAdapter.getCount() - 1) {
                suggestionsAdapter.activePos = 0;
            } else {
                suggestionsAdapter.activePos++;
            }
            suggestionsAdapter.notifyDataSetChanged();
            suggestions.smoothScrollToPosition(suggestionsAdapter.activePos);

            String nick = suggestionsAdapter.getItem(suggestionsAdapter.activePos);
            String text = messageTxt.getText().toString();
            if(suggestionsAdapter.atMention)
                nick = "@" + nick;

            if (text.lastIndexOf(' ') > 0) {
                messageTxt.setTextWithEmoji(text.substring(0, text.lastIndexOf(' ') + 1) + nick);
            } else {
                if (nick.startsWith("#") || text.startsWith(":"))
                    messageTxt.setTextWithEmoji(nick);
                else
                    messageTxt.setTextWithEmoji(nick + ":");
            }
            messageTxt.setSelection(messageTxt.getText().length());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        try {
            super.onSaveInstanceState(state);
        } catch (IllegalStateException e) {
            //Support library bug
        }
        if (server != null)
            state.putInt("cid", server.getCid());
        if (buffer != null) {
            state.putInt("bid", buffer.getBid());
            if (messageTxt != null && messageTxt.getText() != null)
                buffer.setDraft(messageTxt.getText());
            else
                buffer.setDraft(null);
        }
        synchronized (backStack) {
            state.putSerializable("backStack", backStack);
        }
        if (imageCaptureURI != null)
            state.putString("imagecaptureuri", imageCaptureURI.toString());
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && (drawerLayout.isDrawerOpen(Gravity.LEFT) || drawerLayout.isDrawerOpen(Gravity.RIGHT))) {
            drawerLayout.closeDrawers();
            return;
        }
        synchronized (backStack) {
            while (backStack != null && backStack.size() > 0) {
                Integer bid = backStack.get(0);
                backStack.remove(0);
                if (buffer == null || bid != buffer.getBid()) {
                    Buffer b = BuffersList.getInstance().getBuffer(bid);
                    if (b != null) {
                        onBufferSelected(bid);
                        if (backStack.size() > 0)
                            backStack.remove(0);
                        return;
                    }
                }
            }
        }
        finish();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (event.isCtrlPressed()) {
            switch(keyCode) {
                case KeyEvent.KEYCODE_C:
                    Bundle b = new Bundle();
                    b.putBoolean(IRCColorPickerFragment.ARG_BACKGROUND, false);
                    mColorPickerFragment.setArguments(b);
                    if (mColorPickerFragment.getView().getVisibility() == View.GONE) {
                        if (Build.VERSION.SDK_INT >= 16) {
                            mColorPickerFragment.getView().setAlpha(0);
                            mColorPickerFragment.getView().animate().alpha(1.0f);
                        }
                        mColorPickerFragment.getView().setVisibility(View.VISIBLE);
                    }
                    return true;
                case KeyEvent.KEYCODE_B:
                    messageTxt.toggleTypingEffect(RichEditText.BOLD);
                    return true;
                case KeyEvent.KEYCODE_I:
                    messageTxt.toggleTypingEffect(RichEditText.ITALIC);
                    return true;
                case KeyEvent.KEYCODE_U:
                    messageTxt.toggleTypingEffect(RichEditText.UNDERLINE);
                    return true;
            }
        }
        BuffersListFragment blf = (BuffersListFragment) getSupportFragmentManager().findFragmentById(R.id.BuffersList);
        if(blf != null) {
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && event.isAltPressed()) {
                blf.prev(event.isShiftPressed());
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN && event.isAltPressed()) {
                blf.next(event.isShiftPressed());
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_R && event.isAltPressed()) {
                if(event.isShiftPressed()) {
                    ArrayList<Integer> cids = new ArrayList<Integer>();
                    ArrayList<Integer> bids = new ArrayList<Integer>();
                    ArrayList<Long> eids = new ArrayList<Long>();

                    for (Buffer b : BuffersList.getInstance().getBuffers()) {
                        if (b.getUnread() == 1 && EventsList.getInstance().lastEidForBuffer(b.getBid()) > 0) {
                            b.setUnread(0);
                            b.setHighlights(0);
                            b.setLast_seen_eid(EventsList.getInstance().lastEidForBuffer(b.getBid()));
                            cids.add(b.getCid());
                            bids.add(b.getBid());
                            eids.add(b.getLast_seen_eid());
                        }
                    }
                    if (conn != null && buffer != null)
                        conn.heartbeat(buffer.getBid(), cids.toArray(new Integer[cids.size()]), bids.toArray(new Integer[bids.size()]), eids.toArray(new Long[eids.size()]), null);
                } else {
                    if (conn != null)
                        conn.heartbeat(buffer.getCid(), buffer.getBid(), EventsList.getInstance().lastEidForBuffer(buffer.getBid()), null);
                }
            }
        }

        return super.onKeyUp(keyCode, event);
    }

    private class SendTask extends AsyncTaskEx<Void, Void, Void> {
        boolean forceText = false;
        Event e = null;
        boolean bold, underline, italic;
        int color_fg = -1, color_bg = -1;

        public SendTask() {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
            forceText = !prefs.getBoolean("pastebin-disableprompt", true);
        }

        @Override
        protected void onPreExecute() {
            if (conn != null && conn.getState() == NetworkConnection.STATE_CONNECTED && messageTxt.getText() != null && messageTxt.getText().length() > 0 && buffer != null && server != null) {
                if(!PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("reset-after-send", true)) {
                    bold = messageTxt.hasEffect(RichEditText.BOLD);
                    underline = messageTxt.hasEffect(RichEditText.UNDERLINE);
                    italic = messageTxt.hasEffect(RichEditText.ITALIC);
                    color_bg = messageTxt.getEffectValue(RichEditText.BACKGROUND);
                    color_fg = messageTxt.getEffectValue(RichEditText.FOREGROUND);
                }
                sendBtn.setEnabled(false);
                messageTxt.clearComposingText();

                String formatted = messageTxt.toIRC();
                String msg = formatted;
                if (msg.startsWith("//"))
                    msg = msg.substring(1);
                else if (msg.startsWith("/") && !msg.startsWith("/me "))
                    msg = null;

                if(formatted.equals("/paste") || formatted.startsWith("/paste ") || (!forceText && msg != null && (msg.length() > 1080 || msg.split("\n").length > 1))) {
                    if(formatted.equals("/paste"))
                        messageTxt.setTextWithEmoji("");
                    else if(formatted.startsWith("/paste "))
                        messageTxt.setTextWithEmoji(messageTxt.getText().toString().substring(7));
                    show_pastebin_prompt();
                    return;
                } else if(formatted.equals("/ignore")) {
                    messageTxt.setTextWithEmoji("");
                    Bundle args = new Bundle();
                    args.putInt("cid", buffer.getCid());
                    IgnoreListFragment ignoreList = new IgnoreListFragment();
                    ignoreList.setArguments(args);
                    ignoreList.show(getSupportFragmentManager(), "ignorelist");
                    return;
                } else if(formatted.equals("/clear")) {
                    messageTxt.setTextWithEmoji("");
                    EventsList.getInstance().deleteEventsForBuffer(buffer.getBid());
                    onBufferSelected(buffer.getBid());
                    return;
                }
                User u = UsersList.getInstance().getUser(buffer.getBid(), server.getNick());
                e = new Event();
                e.command = ColorFormatter.emojify(formatted);
                e.cid = buffer.getCid();
                e.bid = buffer.getBid();
                e.eid = (System.currentTimeMillis() + 5000) * 1000L;
                if (e.eid < EventsList.getInstance().lastEidForBuffer(buffer.getBid()))
                    e.eid = EventsList.getInstance().lastEidForBuffer(buffer.getBid()) + 1000;
                e.self = true;
                e.from = server.getNick();
                e.nick = server.getNick();
                e.from_realname = server.getServerRealname();
                e.chan = buffer.getName();
                if (u != null)
                    e.from_mode = u.mode;
                e.msg = msg;
                if (msg != null && msg.toLowerCase().startsWith("/me ")) {
                    e.type = "buffer_me_msg";
                    e.msg = msg.substring(4);
                } else {
                    e.type = "buffer_msg";
                }
                e.color = colorScheme.timestampColor;
                if (title.getText() != null && title.getText().equals(server.getNick()))
                    e.bg_color = colorScheme.contentBackgroundColor;
                else
                    e.bg_color = colorScheme.selfBackgroundColor;
                e.row_type = 0;
                e.html = null;
                e.group_msg = null;
                e.linkify = true;
                e.target_mode = null;
                e.highlight = false;
                e.reqid = -1;
                e.pending = true;
            }
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            if (BuildConfig.DEBUG && e != null && e.command != null) {
                if (e.command.equals("/starttrace") || e.command.equals("/stoptrace") || e.command.equals("/crash")) {
                    e.reqid = -2;
                    return null;
                }
            }
            if (e != null && e.command != null && e.command.equals("/ignore")) {
                e.reqid = -2;
                return null;
            }
            if (e != null && conn != null && conn.getState() == NetworkConnection.STATE_CONNECTED && messageTxt.getText() != null && messageTxt.getText().length() > 0) {
                e.reqid = conn.say(e.cid, e.chan, e.command, new NetworkConnection.IRCResultCallback() {
                    @Override
                    public void onIRCResult(IRCCloudJSONObject result) {
                        if(!result.getBoolean("success")) {
                            EventsList.getInstance().deleteEvent(e.eid, e.bid);
                            pendingEvents.remove(e.reqid);
                            e.failed = true;
                            e.bg_color = colorScheme.errorBackgroundColor;
                            if (e.expiration_timer != null)
                                e.expiration_timer.cancel();
                            conn.notifyHandlers(NetworkConnection.EVENT_BUFFERMSG, e);
                        }
                    }
                });
                if (e.msg != null)
                    pendingEvents.put(e.reqid, e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (e != null && e.msg != null) {
                e.msg = TextUtils.htmlEncode(e.msg);
                EventsList.getInstance().addEvent(e);
                conn.notifyHandlers(NetworkConnection.EVENT_BUFFERMSG, e, MainActivity.this);
                RecentConversationsList.getInstance().updateConversation(e.cid, e.bid, System.currentTimeMillis());
            }
            if (BuildConfig.DEBUG) {
                if (messageTxt.getText().toString().equals("/starttrace")) {
                    Debug.startMethodTracing("irccloud");
                    showAlert(e.cid, "Method tracing started");
                } else if (messageTxt.getText().toString().equals("/stoptrace")) {
                    Debug.stopMethodTracing();
                    showAlert(e.cid, "Method tracing finished");
                } else if (messageTxt.getText().toString().equals("/crash")) {
                    Crashlytics.getInstance().crash();
                }
            }
            if (e != null && e.reqid != -1) {
                messageTxt.setTextWithEmoji("");
                Buffer b = BuffersList.getInstance().getBuffer(e.bid);
                if(b != null)
                    b.setDraft(null);
                e.expiration_timer = new TimerTask() {
                    @Override
                    public void run() {
                        if (pendingEvents.containsKey(e.reqid)) {
                            pendingEvents.remove(e.reqid);
                            e.failed = true;
                            e.color = colorScheme.networkErrorColor;
                            e.bg_color = colorScheme.errorBackgroundColor;
                            e.formatted = null;
                            e.expiration_timer = null;
                            if (conn != null)
                                conn.notifyHandlers(NetworkConnection.EVENT_BUFFERMSG, e, MainActivity.this);
                        }
                    }
                };
                try {
                    if(countdownTimer != null)
                        countdownTimer.schedule(e.expiration_timer, 30000);
                } catch (IllegalStateException e) {
                    //Timer has already expired
                }
            } else {
                sendBtn.setEnabled(true);
            }
            if(formattingActionMode != null)
                formattingActionMode.finish();
            if(!PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getBoolean("reset-after-send", true)) {
                if(bold)
                    messageTxt.toggleTypingEffect(RichEditText.BOLD);
                if(underline)
                    messageTxt.toggleTypingEffect(RichEditText.UNDERLINE);
                if(italic)
                    messageTxt.toggleTypingEffect(RichEditText.ITALIC);
                if(color_bg != -1)
                    messageTxt.applyBackgroundColor(color_bg);
                if(color_fg != -1) {
                    messageTxt.applyForegroundColor(color_fg);
                }
            }
        }
    }

    private class RefreshUpIndicatorTask extends AsyncTaskEx<Void, Void, Void> {
        int unread = 0;
        int highlights = 0;

        @Override
        protected Void doInBackground(Void... arg0) {
            if (drawerLayout != null) {
                JSONObject channelDisabledMap = null;
                JSONObject bufferDisabledMap = null;
                if (NetworkConnection.getInstance().getUserInfo() != null && NetworkConnection.getInstance().getUserInfo().prefs != null) {
                    try {
                        if (NetworkConnection.getInstance().getUserInfo().prefs.has("channel-disableTrackUnread"))
                            channelDisabledMap = NetworkConnection.getInstance().getUserInfo().prefs.getJSONObject("channel-disableTrackUnread");
                        if (NetworkConnection.getInstance().getUserInfo().prefs.has("buffer-disableTrackUnread"))
                            bufferDisabledMap = NetworkConnection.getInstance().getUserInfo().prefs.getJSONObject("buffer-disableTrackUnread");
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        NetworkConnection.printStackTraceToCrashlytics(e);
                    }
                }

                ArrayList<Buffer> buffers = BuffersList.getInstance().getBuffers();
                for (int j = 0; j < buffers.size(); j++) {
                    Buffer b = buffers.get(j);
                    if (buffer == null || b.getBid() != buffer.getBid()) {
                        if (unread == 0) {
                            int u = 0;
                            try {
                                u = b.getUnread();
                                if (b.isChannel() && channelDisabledMap != null && channelDisabledMap.has(String.valueOf(b.getBid())) && channelDisabledMap.getBoolean(String.valueOf(b.getBid())))
                                    u = 0;
                                else if (bufferDisabledMap != null && bufferDisabledMap.has(String.valueOf(b.getBid())) && bufferDisabledMap.getBoolean(String.valueOf(b.getBid())))
                                    u = 0;
                            } catch (JSONException e) {
                                NetworkConnection.printStackTraceToCrashlytics(e);
                            }
                            unread += u;
                        }
                        if (highlights == 0) {
                            try {
                                if (!b.isConversation() || bufferDisabledMap == null || !bufferDisabledMap.has(String.valueOf(b.getBid())) || !bufferDisabledMap.getBoolean(String.valueOf(b.getBid()))) {
                                    highlights += b.getHighlights();
                                }
                            } catch (JSONException e) {
                            }
                        }
                        if (highlights > 0)
                            break;
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled() && upDrawable != null) {
                if (highlights > 0) {
                    upDrawable.setColorFilter(highlightsFilter);
                    upDrawableFilter = highlightsFilter;
                } else if (unread > 0) {
                    upDrawable.setColorFilter(unreadFilter);
                    upDrawableFilter = unreadFilter;
                } else {
                    upDrawable.setColorFilter(normalFilter);
                    upDrawableFilter = normalFilter;
                }
                refreshUpIndicatorTask = null;
            }
        }
    }

    private class ExcludeBIDTask extends AsyncTaskEx<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... params) {
            try {
                NotificationsList.getInstance().excludeBid(params[0]);
            } catch (Exception e) {
            }
            if(excludeBIDTask == this)
                excludeBIDTask = null;
            return null;
        }
    }

    private void setFromIntent(Intent intent) {
        launchBid = -1;
        launchURI = null;

        if (NetworkConnection.getInstance().ready)
            setIntent(new Intent(this, MainActivity.class));

        if(intent.hasExtra("eid"))
            NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int)(intent.getLongExtra("eid", -1) / 1000));

        if (intent.hasExtra("bid")) {
            int new_bid = intent.getIntExtra("bid", 0);
            if (NetworkConnection.getInstance().ready && NetworkConnection.getInstance().getState() == NetworkConnection.STATE_CONNECTED && BuffersList.getInstance().getBuffer(new_bid) == null) {
                Crashlytics.log(Log.WARN, "IRCCloud", "Invalid bid requested by launch intent: " + new_bid);
                NotificationsList.getInstance().deleteNotificationsForBid(new_bid);
                if (excludeBIDTask != null)
                    excludeBIDTask.cancel(true);
                excludeBIDTask = new ExcludeBIDTask();
                excludeBIDTask.execute(new_bid);
                return;
            } else if (BuffersList.getInstance().getBuffer(new_bid) != null) {
                Crashlytics.log(Log.DEBUG, "IRCCloud", "Found BID, switching buffers");
                synchronized (backStack) {
                    if (buffer != null && buffer.getBid() != new_bid)
                        backStack.add(0, buffer.getBid());
                }
                buffer = BuffersList.getInstance().getBuffer(new_bid);
                server = buffer.getServer();
            } else {
                Crashlytics.log(Log.DEBUG, "IRCCloud", "BID not found, will try after reconnecting");
                launchBid = new_bid;
            }
        }

        if (intent.getData() != null && intent.getData().getScheme() != null && intent.getData().getScheme().startsWith("irc")) {
            if (open_uri(intent.getData())) {
                return;
            } else {
                launchURI = intent.getData();
                buffer = null;
                server = null;
            }
        } else if (intent.getData() != null && intent.getData().getPath() != null && intent.getData().getPath().equals("/invite")) {
            try {
                String uri = "irc";
                if (intent.getData().getQueryParameter("ssl") != null && intent.getData().getQueryParameter("ssl").equals("1"))
                    uri += "s";
                uri += "://" + intent.getData().getQueryParameter("hostname") + ":" + intent.getData().getQueryParameter("port") + "/" + URLEncoder.encode(intent.getData().getQueryParameter("channel"), "UTF-8");
                if (open_uri(Uri.parse(uri))) {
                    return;
                } else {
                    launchURI = Uri.parse(uri);
                    buffer = null;
                    server = null;
                }
            } catch(Exception e) {
                buffer = null;
                server = null;
            }
        } else if (intent.hasExtra("cid")) {
            if (buffer == null) {
                buffer = BuffersList.getInstance().getBufferByName(intent.getIntExtra("cid", 0), intent.getStringExtra("name"));
                if (buffer != null) {
                    server = ServersList.getInstance().getServer(intent.getIntExtra("cid", 0));
                }
            }
        }

        if (buffer == null) {
            server = null;
        } else {
            if (intent.hasExtra(Intent.EXTRA_STREAM) && intent.getParcelableExtra(Intent.EXTRA_STREAM) != null) {
                Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                String type = getContentResolver().getType(uri);
                uri = makeTempCopy(uri, this);

                if (type != null && type.startsWith("image/") && (!NetworkConnection.getInstance().uploadsAvailable() || PreferenceManager.getDefaultSharedPreferences(this).getString("image_service", "IRCCloud").equals("imgur"))) {
                    new ImgurRefreshTask(uri).execute((Void) null);
                } else {
                    fileUploadTask = new FileUploadTask(uri, this);
                    if(Build.VERSION.SDK_INT >= 16 && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_EXTERNAL_MEDIA_IRCCLOUD);
                    } else {
                        fileUploadTask.execute((Void) null);
                    }
                }
            }

            if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                if (intent.hasExtra(Intent.EXTRA_SUBJECT))
                    buffer.setDraft(intent.getStringExtra(Intent.EXTRA_SUBJECT) + " (" + intent.getStringExtra(Intent.EXTRA_TEXT) + ")");
                else
                    buffer.setDraft(intent.getStringExtra(Intent.EXTRA_TEXT));
            }
        }

        if (buffer == null) {
            launchBid = intent.getIntExtra("bid", -1);
        } else {
            int bid = buffer.getBid();
            onBufferSelected(bid);
            if(server == null)
                launchBid = bid;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent != null) {
            Crashlytics.log(Log.DEBUG, "IRCCloud", "Got new launch intent");
            buffer = null;
            setFromIntent(intent);
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onResume() {
        Crashlytics.log(Log.DEBUG, "IRCCloud", "Resuming app");

        if(!theme.equals(ColorScheme.getUserTheme())) {
            super.onResume();
            Crashlytics.log(Log.DEBUG, "IRCCloud", "Theme changed, relaunching");
            drawerLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    recreate();
                }
            }, 250);
        }

        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("screenlock", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        if (drawerLayout != null && !NetworkConnection.getInstance().ready) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            if(actionBar != null)
                actionBar.setHomeButtonEnabled(false);
        } else {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            if(actionBar != null)
                actionBar.setHomeButtonEnabled(true);
        }

        if (conn.getState() != NetworkConnection.STATE_CONNECTED) {
            sendBtn.setEnabled(false);
            if(conn.config == null) {
                photoBtn.setEnabled(false);
            }
        } else {
            if (messageTxt.getText() != null && messageTxt.getText().length() > 0) {
                sendBtn.setEnabled(true);
            }
            photoBtn.setEnabled(true);
        }

        if (server == null || launchURI != null || (getIntent() != null && (getIntent().hasExtra("bid") || getIntent().getData() != null))) {
            if (getIntent() != null && (getIntent().hasExtra("bid") || getIntent().getData() != null)) {
                Crashlytics.log(Log.DEBUG, "IRCCloud", "Launch intent contains a BID or URL");
                setFromIntent(getIntent());
            } else if (conn.getUserInfo() != null && conn.ready) {
                if (launchURI == null || !open_uri(launchURI)) {
                    if (!open_bid(conn.getUserInfo().last_selected_bid)) {
                        open_bid(BuffersList.getInstance().firstBid());
                    }
                }
            }
        } else if (buffer != null) {
            int bid = buffer.getBid();
            onBufferSelected(bid);
        }

        if(buffer != null && actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        adjustTabletLayout();
        updateUsersListFragmentVisibility();
        update_subtitle();

        suggestions = ((MessageViewFragment) getSupportFragmentManager().findFragmentById(R.id.messageViewFragment)).suggestions;
        suggestions.setAdapter(suggestionsAdapter);
        suggestions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String nick = suggestionsAdapter.getItem(position);
                String text = messageTxt.getText().toString();
                if(suggestionsAdapter.atMention)
                    nick = "@" + nick;

                if (text.lastIndexOf(' ') > 0) {
                    messageTxt.setTextWithEmoji(text.substring(0, text.lastIndexOf(' ') + 1) + nick + " ");
                } else {
                    if (nick.startsWith("#") || text.startsWith(":"))
                        messageTxt.setTextWithEmoji(nick + " ");
                    else
                        messageTxt.setTextWithEmoji(nick + ": ");
                }
                messageTxt.setSelection(messageTxt.getText().length());
            }
        });
        suggestionsContainer = ((MessageViewFragment) getSupportFragmentManager().findFragmentById(R.id.messageViewFragment)).suggestionsContainer;
        update_suggestions(false);

        if (refreshUpIndicatorTask != null)
            refreshUpIndicatorTask.cancel(true);
        refreshUpIndicatorTask = new RefreshUpIndicatorTask();
        refreshUpIndicatorTask.execute((Void) null);

        supportInvalidateOptionsMenu();

        if (NetworkConnection.getInstance().ready && buffer != null) {
            try {
                if (excludeBIDTask != null)
                    excludeBIDTask.cancel(true);
                excludeBIDTask = new ExcludeBIDTask();
                excludeBIDTask.execute(buffer.getBid());
            } catch (Exception e) {
            }
        }
        sendBtn.setEnabled(messageTxt.getText().length() > 0);

        if (drawerLayout != null)
            drawerLayout.closeDrawers();

        updateReconnecting();

        if (imgurTask != null)
            imgurTask.setActivity(this);

        if (fileUploadTask != null) {
            fileUploadTask.setActivity(this);
            if(fileUploadTask.metadataDialog == null && !fileUploadTask.filenameSet && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                fileUploadTask.show_dialog();
        }
        messageTxt.setEnabled(buffer != null);

        if(pastebinResult != null) {
            String text = "";
            String url = pastebinResult.getStringExtra("url");
            String message = pastebinResult.getStringExtra("message");
            if(url != null && url.length() > 0) {
                if (message != null && message.length() > 0)
                    text += message + " ";
                text += url;
            } else {
                text = pastebinResult.getStringExtra("paste_contents");
            }
            messageTxt.setTextWithEmoji(text);
            SendTask t = new SendTask();
            t.forceText = true;
            t.execute((Void) null);
            pastebinResult = null;
        }

        if(NetworkConnection.getInstance().session != null && NetworkConnection.getInstance().session.length() > 0) {
            new GcmTask().execute((Void)null);
        }
    }

    CustomTabsServiceConnection mCustomTabsConnection = new CustomTabsServiceConnection() {
        @Override
        public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
            client.warmup(0);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                String packageName = CustomTabsHelper.getPackageNameToUse(this);
                if (packageName != null && packageName.length() > 0)
                    CustomTabsClient.bindCustomTabsService(this, packageName, mCustomTabsConnection);
            }
        } catch (Exception e) {
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (imgurTask != null)
            imgurTask.setActivity(null);
        if (fileUploadTask != null)
            fileUploadTask.setActivity(null);
        try {
            if (excludeBIDTask != null)
                excludeBIDTask.cancel(true);
        } catch (Exception e) {
        }
        ExcludeBIDTask t = new ExcludeBIDTask();
        t.execute(-1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            try {
                unbindService(mCustomTabsConnection);
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if(isFinishing()) {
            if (imgurTask != null)
                imgurTask.setActivity(null);
            if (fileUploadTask != null)
                fileUploadTask.setActivity(null);

            if (shouldMarkAsRead()) {
                Long eid = EventsList.getInstance().lastEidForBuffer(buffer.getBid());
                if (eid >= buffer.getLast_seen_eid() && conn != null && conn.getState() == NetworkConnection.STATE_CONNECTED) {
                    NetworkConnection.getInstance().heartbeat(buffer.getCid(), buffer.getBid(), eid, null);
                    buffer.setLast_seen_eid(eid);
                    buffer.setUnread(0);
                    buffer.setHighlights(0);
                }
            }

            try {
                if (excludeBIDTask != null)
                    excludeBIDTask.cancel(true);
            } catch (Exception e) {
            }
            excludeBIDTask = new ExcludeBIDTask();
            excludeBIDTask.execute(-1);
            if (channelsListDialog != null)
                channelsListDialog.dismiss();
            suggestionsAdapter.clear();
            progressBar.setVisibility(View.GONE);
            errorMsg.setVisibility(View.GONE);
            error = null;
            synchronized (backStack) {
                if (buffer != null)
                    backStack.add(0, buffer.getBid());
            }
            ImageList.getInstance().clearFailures();
            ImageList.getInstance().clear();
            ImageList.getInstance().purge();
        }
        excludeBIDTask = new ExcludeBIDTask();
        excludeBIDTask.execute(-1);
        if (channelsListDialog != null)
            channelsListDialog.dismiss();
        suggestionsAdapter.clear();
        progressBar.setVisibility(View.GONE);
        errorMsg.setVisibility(View.GONE);
        error = null;
        synchronized (backStack) {
            if (buffer != null)
                backStack.add(0, buffer.getBid());
        }
    }

    public static class ConfigInstance {
        public ImgurUploadTask imgurUploadTask;
        public FileUploadTask fileUploadTask;
    }

    public Object onRetainCustomNonConfigurationInstance() {
        ConfigInstance config = new ConfigInstance();
        config.imgurUploadTask = imgurTask;
        config.fileUploadTask = fileUploadTask;
        if(fileUploadTask != null) {
            if(fileUploadTask.metadataDialog != null)
                fileUploadTask.metadataDialog.dismiss();
            fileUploadTask.metadataDialog = null;
            fileUploadTask.fileSize = null;
        }
        return config;
    }

    private boolean open_uri(Uri uri) {
        if (uri != null && conn != null && conn.ready) {
            launchURI = null;
            Server s = null;
            try {
                if (uri.getHost().equals("cid")) {
                    s = ServersList.getInstance().getServer(Integer.parseInt(uri.getPathSegments().get(0)));
                }
            } catch (NumberFormatException e) {

            }
            if (s == null) {
                if (uri.getPort() > 0)
                    s = ServersList.getInstance().getServer(uri.getHost(), uri.getPort());
                else if (uri.getScheme() != null && uri.getScheme().equalsIgnoreCase("ircs"))
                    s = ServersList.getInstance().getServer(uri.getHost(), true);
                else
                    s = ServersList.getInstance().getServer(uri.getHost());
            }

            if (s != null) {
                if (uri.getPath() != null && uri.getPath().length() > 1) {
                    String key = null;
                    String channel = uri.getLastPathSegment();
                    if (channel.contains(",")) {
                        key = channel.substring(channel.indexOf(",") + 1);
                        channel = channel.substring(0, channel.indexOf(","));
                    }
                    Buffer b = BuffersList.getInstance().getBufferByName(s.getCid(), channel);
                    if (b != null) {
                        server = null;
                        return open_bid(b.getBid());
                    } else {
                        onBufferSelected(-1);
                        title.setText(channel);
                        actionBar.setTitle(channel);
                        actionBar.setSubtitle(null);
                        bufferToOpen = channel;
                        if(channel.substring(0,1).matches("[a-zA-Z0-9]"))
                            conn.say(s.getCid(), null, "/query " + channel, null);
                        else
                            conn.join(s.getCid(), channel, key, null);
                    }
                    return true;
                } else {
                    Buffer b = BuffersList.getInstance().getBufferByName(s.getCid(), "*");
                    if (b != null)
                        return open_bid(b.getBid());
                }
            } else {
                if (!getResources().getBoolean(R.bool.isTablet)) {
                    Intent i = new Intent(this, EditConnectionActivity.class);
                    i.putExtra("hostname", uri.getHost());
                    if (uri.getPort() > 0)
                        i.putExtra("port", uri.getPort());
                    else if (uri.getScheme().equalsIgnoreCase("ircs"))
                        i.putExtra("port", 6697);
                    if (uri.getPath() != null && uri.getPath().length() > 1)
                        i.putExtra("channels", uri.getPath().substring(1).replace(",", " "));
                    startActivity(i);
                } else {
                    EditConnectionFragment connFragment = new EditConnectionFragment();
                    connFragment.default_hostname = uri.getHost();
                    if (uri.getPort() > 0)
                        connFragment.default_port = uri.getPort();
                    else if (uri.getScheme().equalsIgnoreCase("ircs"))
                        connFragment.default_port = 6697;
                    if (uri.getPath() != null && uri.getPath().length() > 1)
                        connFragment.default_channels = uri.getPath().substring(1).replace(",", " ");
                    connFragment.show(getSupportFragmentManager(), "addnetwork");
                }
                return true;
            }
        }
        return false;
    }

    private boolean open_bid(int bid) {
        if (BuffersList.getInstance().getBuffer(bid) != null) {
            onBufferSelected(bid);
            if (bid == launchBid)
                launchBid = -1;
            return true;
        }
        Log.w("IRCCloud", "Requested BID not found");
        return false;
    }

    private void update_subtitle() {
        if (server == null || buffer == null) {
            title.setText(bufferToOpen);
            subtitle.setVisibility(View.GONE);
        } else {
            if (buffer.isConsole()) {
                if (server.getName().length() > 0) {
                    title.setText(server.getName());
                    if (progressBar.getVisibility() == View.GONE) {
                        actionBar.setTitle(server.getName());
                        actionBar.setSubtitle(null);
                    }
                } else {
                    title.setText(server.getHostname());
                    if (progressBar.getVisibility() == View.GONE) {
                        actionBar.setTitle(server.getHostname());
                        actionBar.setSubtitle(null);
                    }
                }
            } else {
                if(Build.VERSION.SDK_INT >= 19 && EmojiCompat.get().getLoadState() == EmojiCompat.LOAD_STATE_SUCCEEDED)
                    title.setText(EmojiCompat.get().process(buffer.getName()));
                else
                    title.setText(buffer.getName());
                if (progressBar.getVisibility() == View.GONE) {
                    actionBar.setTitle(buffer.getName());
                    actionBar.setSubtitle(null);
                }
            }

            if (buffer.getArchived() > 0 && !buffer.isConsole()) {
                subtitle.setVisibility(View.VISIBLE);
                subtitle.setText("(archived)");
                if (buffer.isConversation()) {
                    title.setContentDescription("Conversation with " + title.getText());
                } else if (buffer.isChannel()) {
                    title.setContentDescription("Channel " + buffer.normalizedName());
                }
            } else {
                if (buffer.isConversation()) {
                    title.setContentDescription("Conversation with " + title.getText());
                    if (buffer.getAway_msg() != null && buffer.getAway_msg().length() > 0) {
                        subtitle.setVisibility(View.VISIBLE);
                        if (buffer.getAway_msg() != null && buffer.getAway_msg().length() > 0) {
                            subtitle.setText(ColorFormatter.strip("Away: " + buffer.getAway_msg()));
                        } else {
                            subtitle.setText("Away");
                        }
                    } else {
                        User u = UsersList.getInstance().findUserOnConnection(buffer.getCid(), buffer.getName());
                        if (u != null && u.away > 0) {
                            subtitle.setVisibility(View.VISIBLE);
                            if (u.away_msg != null && u.away_msg.length() > 0) {
                                subtitle.setText(ColorFormatter.strip("Away: " + u.away_msg));
                            } else {
                                subtitle.setText("Away");
                            }
                        } else {
                            subtitle.setVisibility(View.GONE);
                        }
                    }
                    key.setVisibility(View.GONE);
                } else if (buffer.isChannel()) {
                    title.setContentDescription("Channel " + buffer.normalizedName() + ". Double-tap to view or edit the topic.");
                    Channel c = ChannelsList.getInstance().getChannelForBuffer(buffer.getBid());
                    if (c != null && c.topic_text.length() > 0) {
                        subtitle.setVisibility(View.VISIBLE);
                        subtitle.setText(ColorFormatter.strip(c.topic_text));
                        subtitle.setContentDescription(".");
                    } else {
                        subtitle.setVisibility(View.GONE);
                    }
                    if (c != null && c.key) {
                        key.setText(FontAwesome.LOCK);
                        key.setVisibility(View.VISIBLE);
                    } else {
                        key.setVisibility(View.GONE);
                    }
                } else if (buffer.isConsole()) {
                    subtitle.setVisibility(View.VISIBLE);
                    subtitle.setText(server.getHostname() + ":" + server.getPort());
                    title.setContentDescription("Network " + server.getName());
                    subtitle.setContentDescription(".");
                    if (server.getSsl() > 0)
                        key.setText(FontAwesome.SHIELD);
                    else
                        key.setText(FontAwesome.GLOBE);
                    key.setVisibility(View.VISIBLE);
                }

                if(progressBar.getVisibility() == View.GONE && NetworkConnection.getInstance().getState() != NetworkConnection.STATE_CONNECTED) {
                    subtitle.setVisibility(View.VISIBLE);
                    subtitle.setText("(Offline)");
                    actionBar.setSubtitle("(Offline)");
                }
            }
        }
        supportInvalidateOptionsMenu();
    }

    private void updateUsersListFragmentVisibility() {
        boolean hide = true;
        View usersListFragmentDocked = findViewById(R.id.usersListFragmentDocked);
        if (usersListFragmentDocked != null) {
            Channel c = null;
            if (buffer != null && buffer.isChannel()) {
                c = ChannelsList.getInstance().getChannelForBuffer(buffer.getBid());
                if (c != null)
                    hide = false;
            }
            try {
                if (conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null) {
                    JSONObject hiddenMap = conn.getUserInfo().prefs.getJSONObject("channel-hiddenMembers");
                    if (hiddenMap.has(String.valueOf(buffer.getBid())) && hiddenMap.getBoolean(String.valueOf(buffer.getBid())))
                        hide = true;
                }
            } catch (Exception e) {
            }

            if(conn != null && !conn.ready)
                hide = true;

            if (hide) {
                usersListFragmentDocked.setVisibility(View.GONE);
                if (drawerLayout != null) {
                    if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && getResources().getBoolean(R.bool.isTablet) && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("tabletMode", true) && !isMultiWindow() && c != null)
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
                    else
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
                }
            } else {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && getResources().getBoolean(R.bool.isTablet) && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("tabletMode", true) && !isMultiWindow()) {
                    usersListFragmentDocked.setVisibility(View.VISIBLE);
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
                } else {
                    usersListFragmentDocked.setVisibility(View.GONE);
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
                }
            }
        }
    }

    public void onIRCEvent(int what, Object obj) {
        super.onIRCEvent(what, obj);
        Integer event_bid = 0;
        final IRCCloudJSONObject event;
        final Object o = obj;
        Buffer b = null;
        Channel c = null;
        switch (what) {
            case NetworkConnection.EVENT_FONT_DOWNLOADED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        update_subtitle();
                        update_suggestions(false);
                    }
                });
                break;
            case NetworkConnection.EVENT_CACHE_START:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("IRCCloud", "Cache load started");
                        if(actionBar != null) {
                            actionBar.setTitle("Loading");
                            actionBar.setSubtitle(null);
                        }
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setIndeterminate(true);
                    }
                });
                break;
            case NetworkConnection.EVENT_CACHE_END:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("IRCCloud", "Cache load finished");
                        progressBar.setVisibility(View.GONE);
                        updateReconnecting();
                        if (drawerLayout != null && NetworkConnection.getInstance().ready) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                                    if(actionBar != null)
                                        actionBar.setHomeButtonEnabled(true);
                                    updateUsersListFragmentVisibility();
                                }
                            });
                        }
                    }
                });
                break;
            case NetworkConnection.EVENT_DEBUG:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        errorMsg.setVisibility(View.VISIBLE);
                        errorMsg.setText(o.toString());
                    }
                });
                break;
            case NetworkConnection.EVENT_PROGRESS:
                final float progress = (Float) obj;
                if (progressBar != null && progressBar.getProgress() < progress) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (progressBar != null) {
                                progressBar.setIndeterminate(false);
                                progressBar.setProgress((int) progress);
                                if (progressBar.getVisibility() != View.VISIBLE && progress < 1000) {
                                    progressBar.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_BACKLOG_START:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    if (progressBar != null)
                            progressBar.setProgress(0);
                    }
                });
                break;
            case NetworkConnection.EVENT_RENAMECONVERSATION:
                if (buffer != null && (Integer) obj == buffer.getBid()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update_subtitle();
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_CHANNELTOPICIS:
                event = (IRCCloudJSONObject) obj;
                if(buffer != null && buffer.getCid() == event.cid()) {
                    b = BuffersList.getInstance().getBufferByName(event.cid(), event.getString("chan"));
                    if (b != null) {
                        c = ChannelsList.getInstance().getChannelForBuffer(b.getBid());
                    }
                    if (c == null) {
                        c = new Channel();
                        c.cid = event.cid();
                        c.name = event.getString("chan");
                        c.topic_author = event.has("author") ? event.getString("author") : event.getString("server");
                        c.topic_time = event.getLong("time");
                        c.topic_text = event.getString("text");
                    }
                    final Channel channel = c;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update_subtitle();
                            show_topic_popup(channel);
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_LINKCHANNEL:
                event = (IRCCloudJSONObject) obj;
                if (event != null && cidToOpen == event.cid() && event.has("invalid_chan") && event.has("valid_chan") && event.getString("invalid_chan").equalsIgnoreCase(bufferToOpen)) {
                    bufferToOpen = event.getString("valid_chan");
                    obj = BuffersList.getInstance().getBuffer(event.bid());
                } else {
                    bufferToOpen = null;
                    return;
                }
            case NetworkConnection.EVENT_MAKEBUFFER:
                b = (Buffer) obj;
                if (cidToOpen == b.getCid() && (bufferToOpen == null || (b.getName().equalsIgnoreCase(bufferToOpen) && (buffer == null || !bufferToOpen.equalsIgnoreCase(buffer.getName()))))) {
                    server = null;
                    final int bid = b.getBid();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onBufferSelected(bid);
                        }
                    });
                    bufferToOpen = null;
                    cidToOpen = -1;
                }
                break;
            case NetworkConnection.EVENT_OPENBUFFER:
                event = (IRCCloudJSONObject) obj;
                try {
                    b = BuffersList.getInstance().getBufferByName(event.cid(), event.getString("name"));
                    if (b == null) {
                        cidToOpen = event.cid();
                        bufferToOpen = event.getString("name");
                    } else if (b != buffer) {
                        server = null;
                        bufferToOpen = null;
                        cidToOpen = -1;
                        final int bid = b.getBid();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onBufferSelected(bid);
                            }
                        });
                    }
                } catch (Exception e2) {
                    NetworkConnection.printStackTraceToCrashlytics(e2);
                }
                break;
            case NetworkConnection.EVENT_CONNECTIVITY:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateReconnecting();
                    }
                });
                if (conn != null) {
                    if (conn.getState() == NetworkConnection.STATE_CONNECTED) {
                        for (Event e : pendingEvents.values()) {
                            try {
                                e.expiration_timer.cancel();
                            } catch (Exception ex) {
                                //Task already cancellled
                            }
                            e.expiration_timer = null;
                            e.failed = true;
                            e.bg_color = colorScheme.errorBackgroundColor;
                        }
                        if (drawerLayout != null && NetworkConnection.getInstance().ready) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                                    if(actionBar != null)
                                        actionBar.setHomeButtonEnabled(true);
                                    updateUsersListFragmentVisibility();
                                }
                            });
                        }
                        if (server != null && messageTxt.getText() != null && messageTxt.getText().length() > 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    sendBtn.setEnabled(true);
                                }
                            });
                        }
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (drawerLayout != null && !NetworkConnection.getInstance().ready) {
                                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                                    if(actionBar != null)
                                        actionBar.setHomeButtonEnabled(false);
                                }
                                sendBtn.setEnabled(false);
                                photoBtn.setEnabled(false);
                            }
                        });
                        if (conn.getState() == NetworkConnection.STATE_DISCONNECTED && conn.ready && server == null) {
                            Crashlytics.log(Log.DEBUG, "IRCCloud", "Offline cache available and we're waiting for a buffer, switching now");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (conn == null || conn.getUserInfo() == null || !open_bid(conn.getUserInfo().last_selected_bid)) {
                                        open_bid(BuffersList.getInstance().firstBid());
                                    }
                                }
                            });
                        }
                    }
                }
                break;
            case NetworkConnection.EVENT_BANLIST:
                event = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (event != null && event.cid() == buffer.getCid()) {
                            Bundle args = new Bundle();
                            args.putInt("cid", event.cid());
                            Buffer b = BuffersList.getInstance().getBufferByName(event.cid(), event.getString("channel"));
                            if(b != null)
                                args.putInt("bid", b.getBid());
                            args.putString("mode", "b");
                            args.putString("placeholder", "No bans in effect.\n\nYou can ban someone by tapping their nickname in the user list, long-pressing a message, or by using /ban.");
                            args.putString("mask", "mask");
                            args.putString("list", "bans");
                            args.putString("title", "Ban list for " + event.getString("channel"));
                            args.putString("event", event.toString());
                            ChannelModeListFragment channelModeList = (ChannelModeListFragment) getSupportFragmentManager().findFragmentByTag("banlist");
                            if (channelModeList == null) {
                                channelModeList = new ChannelModeListFragment();
                                channelModeList.setArguments(args);
                                try {
                                    channelModeList.show(getSupportFragmentManager(), "banlist");
                                } catch (IllegalStateException e) {
                                    //App lost focus already
                                }
                            } else {
                                channelModeList.setArguments(args);
                            }
                        }
                    }
                });
                break;
            case NetworkConnection.EVENT_QUIETLIST:
                event = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (event != null && event.cid() == buffer.getCid()) {
                            Bundle args = new Bundle();
                            args.putInt("cid", event.cid());
                            Buffer b = BuffersList.getInstance().getBufferByName(event.cid(), event.getString("channel"));
                            if(b != null)
                                args.putInt("bid", b.getBid());
                            args.putString("mode", "q");
                            args.putString("placeholder", "Empty quiet list.");
                            args.putString("mask", "quiet_mask");
                            args.putString("list", "list");
                            args.putString("title", "Quiet list for " + event.getString("channel"));
                            args.putString("event", event.toString());
                            ChannelModeListFragment channelModeList = (ChannelModeListFragment) getSupportFragmentManager().findFragmentByTag("quietlist");
                            if (channelModeList == null) {
                                channelModeList = new ChannelModeListFragment();
                                channelModeList.setArguments(args);
                                try {
                                    channelModeList.show(getSupportFragmentManager(), "quietlist");
                                } catch (IllegalStateException e) {
                                    //App lost focus already
                                }
                            } else {
                                channelModeList.setArguments(args);
                            }
                        }
                    }
                });
                break;
            case NetworkConnection.EVENT_BANEXCEPTIONLIST:
                event = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (event != null && event.cid() == buffer.getCid()) {
                            Bundle args = new Bundle();
                            args.putInt("cid", event.cid());
                            Buffer b = BuffersList.getInstance().getBufferByName(event.cid(), event.getString("channel"));
                            if(b != null)
                                args.putInt("bid", b.getBid());
                            args.putString("mode", "e");
                            args.putString("placeholder", "Empty exception list.");
                            args.putString("mask", "mask");
                            args.putString("list", "exceptions");
                            args.putString("title", "Exception list for " + event.getString("channel"));
                            args.putString("event", event.toString());
                            ChannelModeListFragment channelModeList = (ChannelModeListFragment) getSupportFragmentManager().findFragmentByTag("exceptionlist");
                            if (channelModeList == null) {
                                channelModeList = new ChannelModeListFragment();
                                channelModeList.setArguments(args);
                                try {
                                    channelModeList.show(getSupportFragmentManager(), "exceptionlist");
                                } catch (IllegalStateException e) {
                                    //App lost focus already
                                }
                            } else {
                                channelModeList.setArguments(args);
                            }
                        }
                    }
                });
                break;
            case NetworkConnection.EVENT_INVITELIST:
                event = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (event != null && event.cid() == buffer.getCid()) {
                            Bundle args = new Bundle();
                            args.putInt("cid", event.cid());
                            Buffer b = BuffersList.getInstance().getBufferByName(event.cid(), event.getString("channel"));
                            if(b != null)
                                args.putInt("bid", b.getBid());
                            args.putString("mode", "I");
                            args.putString("placeholder", "Empty invite list");
                            args.putString("mask", "mask");
                            args.putString("list", "list");
                            args.putString("title", "Invite list for " + event.getString("channel"));
                            args.putString("event", event.toString());
                            ChannelModeListFragment channelModeList = (ChannelModeListFragment) getSupportFragmentManager().findFragmentByTag("invitelist");
                            if (channelModeList == null) {
                                channelModeList = new ChannelModeListFragment();
                                channelModeList.setArguments(args);
                                try {
                                    channelModeList.show(getSupportFragmentManager(), "invitelist");
                                } catch (IllegalStateException e) {
                                    //App lost focus already
                                }
                            } else {
                                channelModeList.setArguments(args);
                            }
                        }
                    }
                });
                break;
            case NetworkConnection.EVENT_ACCEPTLIST:
                event = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (event != null && event.cid() == buffer.getCid()) {
                            Bundle args = new Bundle();
                            args.putInt("cid", buffer.getCid());
                            args.putString("event", event.toString());
                            AcceptListFragment acceptList = (AcceptListFragment) getSupportFragmentManager().findFragmentByTag("acceptlist");
                            if (acceptList == null) {
                                acceptList = new AcceptListFragment();
                                acceptList.setArguments(args);
                                try {
                                    acceptList.show(getSupportFragmentManager(), "acceptlist");
                                } catch (IllegalStateException e) {
                                    //App lost focus already
                                }
                            } else {
                                acceptList.setArguments(args);
                            }
                        }
                    }
                });
                break;
            case NetworkConnection.EVENT_WHOLIST:
                event = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Bundle args = new Bundle();
                        args.putString("event", event.toString());
                        WhoListFragment whoList = (WhoListFragment) getSupportFragmentManager().findFragmentByTag("wholist");
                        if (whoList == null) {
                            whoList = new WhoListFragment();
                            whoList.setArguments(args);
                            try {
                                whoList.show(getSupportFragmentManager(), "wholist");
                            } catch (IllegalStateException e) {
                                //App lost focus already
                            }
                        } else {
                            whoList.setArguments(args);
                        }
                    }
                });
                break;
            case NetworkConnection.EVENT_NAMESLIST:
                event = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Bundle args = new Bundle();
                        args.putString("event", event.toString());
                        NamesListFragment namesList = (NamesListFragment) getSupportFragmentManager().findFragmentByTag("nameslist");
                        if (namesList == null) {
                            namesList = new NamesListFragment();
                            namesList.setArguments(args);
                            try {
                                namesList.show(getSupportFragmentManager(), "nameslist");
                            } catch (IllegalStateException e) {
                                //App lost focus already
                            }
                        } else {
                            namesList.setArguments(args);
                        }
                    }
                });
                break;
            case NetworkConnection.EVENT_SERVERMAPLIST:
                event = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder sb = new StringBuilder();
                        JsonNode servers = event.getJsonNode("servers");
                        if(servers != null) {
                            for (int i = 0; i < servers.size(); i++) {
                                sb.append(servers.get(i).asText()).append("\n");
                            }
                        }

                        Bundle args = new Bundle();
                        args.putString("title", "Server Map");
                        args.putString("text", (sb.length() > 0) ? sb.toString(): "No results found.");
                        TextListFragment serversList = (TextListFragment) getSupportFragmentManager().findFragmentByTag("serverslist");
                        if (serversList == null) {
                            serversList = new TextListFragment();
                            serversList.setArguments(args);
                            try {
                                serversList.show(getSupportFragmentManager(), "serverslist");
                            } catch (IllegalStateException e) {
                                //App lost focus already
                            }
                        } else {
                            serversList.getArguments().putAll(args);
                            serversList.refresh();
                        }
                    }
                });
                break;
            case NetworkConnection.EVENT_CHANNELQUERY:
                event = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String msg = "";
                        String type = event.getString("query_type");

                        if(type.equals("mode")) {
                            msg = event.getString("channel") + " mode is " + event.getString("diff");
                        } else if(type.equals("timestamp")) {
                            msg = event.getString("channel") + " created on " + DateFormat.getDateTimeInstance().format(new Date(event.getLong("timestamp") * 1000));
                        } else {
                            Log.w("IRCCloud", "Unhandled channel query type: " + type);
                        }

                        Bundle args = new Bundle();
                        args.putString("text", msg);
                        args.putString("title", server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")");
                        TextListFragment queryFragment = (TextListFragment) getSupportFragmentManager().findFragmentByTag("channelquery");
                        if (queryFragment == null) {
                            queryFragment = new TextListFragment();
                            queryFragment.setArguments(args);
                            try {
                                queryFragment.show(getSupportFragmentManager(), "channelquery");
                            } catch (IllegalStateException e) {
                                //App lost focus already
                            }
                        } else {
                            args = queryFragment.getArguments();
                            msg = args.getString("text") + "\n" + msg;
                            args.putString("text", msg);
                            queryFragment.refresh();
                        }
                        getSupportFragmentManager().executePendingTransactions();
                    }
                });
                break;
            case NetworkConnection.EVENT_WHOSPECIALRESPONSE:
                event = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder sb = new StringBuilder();
                        JsonNode users = event.getJsonNode("users");
                        for(int i = 0; i < users.size(); i++) {
                            JsonNode node = users.get(i);
                            for(int j = 0; j < node.size(); j++)
                                sb.append(node.get(j).asText()).append(" \t");
                            sb.deleteCharAt(sb.length() - 1);
                            sb.append("\n");
                        }

                        Bundle args = new Bundle();
                        args.putString("title", "WHO for " + event.getString("subject"));
                        args.putString("text", (sb.length() > 0) ? sb.toString(): "No results found.");
                        TextListFragment whoList = (TextListFragment) getSupportFragmentManager().findFragmentByTag("whospecial");
                        if (whoList == null) {
                            whoList = new TextListFragment();
                            whoList.setArguments(args);
                            try {
                                whoList.show(getSupportFragmentManager(), "whospecial");
                            } catch (IllegalStateException e) {
                                //App lost focus already
                            }
                        } else {
                            whoList.getArguments().putAll(args);
                            whoList.refresh();
                        }
                    }
                });
                break;
            case NetworkConnection.EVENT_LINKSRESPONSE:
                event = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Bundle args = new Bundle();
                        args.putString("event", event.toString());
                        LinksListFragment linksFragment = new LinksListFragment();
                        linksFragment.setArguments(args);
                        linksFragment.show(getSupportFragmentManager(), "linkslist");
                    }
                });
                break;
            case NetworkConnection.EVENT_MODULESLIST:
                event = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder sb = new StringBuilder();
                        JsonNode modules = event.getJsonNode("modules");
                        for(int i = 0; i < modules.size(); i++) {
                            sb.append(modules.get(i).asText()).append("\n");
                        }

                        Bundle args = new Bundle();
                        args.putString("title", "Modules list for " + server.getHostname());
                        args.putString("text", (sb.length() > 0) ? sb.toString(): "No results found.");
                        TextListFragment modulesList = (TextListFragment) getSupportFragmentManager().findFragmentByTag("moduleslist");
                        if (modulesList == null) {
                            modulesList = new TextListFragment();
                            modulesList.setArguments(args);
                            try {
                                modulesList.show(getSupportFragmentManager(), "moduleslist");
                            } catch (IllegalStateException e) {
                                //App lost focus already
                            }
                        } else {
                            modulesList.getArguments().putAll(args);
                            modulesList.refresh();
                        }
                    }
                });
                break;
            case NetworkConnection.EVENT_TRACERESPONSE:
                event = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder sb = new StringBuilder();
                        JsonNode trace = event.getJsonNode("trace");
                        for(int i = 0; i < trace.size(); i++) {
                            sb.append(trace.get(i).asText()).append("\n");
                        }

                        Bundle args = new Bundle();
                        args.putString("title", "Trace for " + event.getString("server"));
                        args.putString("text", (sb.length() > 0) ? sb.toString(): "No results found.");
                        TextListFragment traceFragment = (TextListFragment) getSupportFragmentManager().findFragmentByTag("trace");
                        if (traceFragment == null) {
                            traceFragment = new TextListFragment();
                            traceFragment.setArguments(args);
                            try {
                                traceFragment.show(getSupportFragmentManager(), "trace");
                            } catch (IllegalStateException e) {
                                //App lost focus already
                            }
                        } else {
                            traceFragment.getArguments().putAll(args);
                            traceFragment.refresh();
                        }
                    }
                });
                break;
            case NetworkConnection.EVENT_WHOIS:
                event = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Bundle args = new Bundle();
                        args.putString("event", event.toString());
                        WhoisFragment whois = (WhoisFragment) getSupportFragmentManager().findFragmentByTag("whois");
                        if (whois == null) {
                            whois = new WhoisFragment();
                            whois.setArguments(args);
                            try {
                                whois.show(getSupportFragmentManager(), "whois");
                            } catch (IllegalStateException e) {
                                //App lost focus already
                            }
                        } else {
                            whois.setArguments(args);
                        }
                    }
                });
                break;
            case NetworkConnection.EVENT_WHOWAS:
                event = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Bundle args = new Bundle();
                        args.putString("title", "WHOWAS response for " + event.getString("nick"));
                        args.putString("event", event.toString());
                        WhoWasFragment whoWas = (WhoWasFragment) getSupportFragmentManager().findFragmentByTag("whowas");
                        if (whoWas == null) {
                            whoWas = new WhoWasFragment();
                            whoWas.setArguments(args);
                            try {
                                whoWas.show(getSupportFragmentManager(), "whowas");
                            } catch (IllegalStateException e) {
                                //App lost focus already
                            }
                        } else {
                            whoWas.setArguments(args);
                        }
                    }
                });
                break;
            case NetworkConnection.EVENT_LISTRESPONSEFETCHING:
                event = (IRCCloudJSONObject) obj;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String dialogtitle = "List of channels on " + ServersList.getInstance().getServer(event.cid()).getHostname();
                            if (channelsListDialog == null) {
                                Context ctx = MainActivity.this;
                                final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                                builder.setView(getLayoutInflater().inflate(R.layout.dialog_channelslist, null));
                                builder.setTitle(dialogtitle);
                                builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                channelsListDialog = builder.create();
                                channelsListDialog.setOwnerActivity(MainActivity.this);
                            } else {
                                channelsListDialog.setTitle(dialogtitle);
                            }
                            if(!isFinishing())
                                channelsListDialog.show();
                            ChannelListFragment channels = (ChannelListFragment) getSupportFragmentManager().findFragmentById(R.id.channelListFragment);
                            Bundle args = new Bundle();
                            args.putInt("cid", event.cid());
                            channels.setArguments(args);
                        } catch (IllegalStateException e) {
                            //App lost focus already
                        }
                    }
                });
                break;
            case NetworkConnection.EVENT_USERINFO:
                if(conn != null && conn.ready && !colorScheme.theme.equals(ColorScheme.getUserTheme())) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            recreate();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateUsersListFragmentVisibility();
                            supportInvalidateOptionsMenu();
                            if (refreshUpIndicatorTask != null)
                                refreshUpIndicatorTask.cancel(true);
                            refreshUpIndicatorTask = new RefreshUpIndicatorTask();
                            refreshUpIndicatorTask.execute((Void) null);
                        }
                    });
                    if (launchBid == -1 && server == null && conn != null && conn.getUserInfo() != null)
                        launchBid = conn.getUserInfo().last_selected_bid;
                }
                break;
            case NetworkConnection.EVENT_STATUSCHANGED:
                try {
                    event = (IRCCloudJSONObject) obj;
                    if (event != null && server != null && event.cid() == server.getCid()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                supportInvalidateOptionsMenu();
                            }
                        });
                    }
                } catch (Exception e1) {
                    NetworkConnection.printStackTraceToCrashlytics(e1);
                }
                break;
            case NetworkConnection.EVENT_MAKESERVER:
                Server s = (Server) obj;
                if (server != null && s != null && s.getCid() == server.getCid()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            supportInvalidateOptionsMenu();
                            update_subtitle();
                        }
                    });
                } else {
                    cidToOpen = s.getCid();
                    bufferToOpen = "*";
                }
                break;
            case NetworkConnection.EVENT_BUFFERARCHIVED:
            case NetworkConnection.EVENT_BUFFERUNARCHIVED:
                event_bid = (Integer) obj;
                if (buffer != null && event_bid == buffer.getBid()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update_subtitle();
                        }
                    });
                }
                if (refreshUpIndicatorTask != null)
                    refreshUpIndicatorTask.cancel(true);
                refreshUpIndicatorTask = new RefreshUpIndicatorTask();
                refreshUpIndicatorTask.execute((Void) null);
                break;
            case NetworkConnection.EVENT_JOIN:
                event = (IRCCloudJSONObject) obj;
                if (event != null && buffer != null && event.bid() == buffer.getBid() && event.type().equals("you_joined_channel")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            supportInvalidateOptionsMenu();
                            updateUsersListFragmentVisibility();
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_PART:
            case NetworkConnection.EVENT_KICK:
                event = (IRCCloudJSONObject) obj;
                if (event != null && buffer != null && event.bid() == buffer.getBid() && event.type().toLowerCase().startsWith("you_")) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            supportInvalidateOptionsMenu();
                            updateUsersListFragmentVisibility();
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_CHANNELINIT:
                c = (Channel) obj;
                if (c != null && buffer != null && c.bid == buffer.getBid()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update_subtitle();
                            supportInvalidateOptionsMenu();
                            updateUsersListFragmentVisibility();
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_BACKLOG_END:
                if(!colorScheme.theme.equals(ColorScheme.getUserTheme())) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            recreate();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            errorMsg.setVisibility(View.GONE);
                            error = null;
                            if (progressBar.getVisibility() == View.VISIBLE) {
                                if (Build.VERSION.SDK_INT >= 16) {
                                    progressBar.animate().alpha(0).setDuration(200).withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            progressBar.setVisibility(View.GONE);
                                        }
                                    });
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                }
                            }
                            if(actionBar != null) {
                                actionBar.setDisplayShowTitleEnabled(false);
                                actionBar.setDisplayShowCustomEnabled(true);
                            }
                            if (drawerLayout != null) {
                                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                                if(actionBar != null)
                                    actionBar.setHomeButtonEnabled(true);
                                updateUsersListFragmentVisibility();
                            }
                            if (ServersList.getInstance().count() < 1) {
                                Crashlytics.log(Log.DEBUG, "IRCCloud", "No servers configured, launching add dialog");
                                addNetwork();
                            } else {
                                if (server == null || launchURI != null || launchBid != -1) {
                                    Crashlytics.log(Log.DEBUG, "IRCCloud", "Backlog loaded and we're waiting for a buffer, switching now");
                                    if (launchURI == null || !open_uri(launchURI)) {
                                        if (launchBid == -1 || !open_bid(launchBid)) {
                                            if (conn == null || conn.getUserInfo() == null || !open_bid(conn.getUserInfo().last_selected_bid)) {
                                                open_bid(BuffersList.getInstance().firstBid());
                                            }
                                        }
                                    }
                                }
                                update_subtitle();
                            }
                            if (refreshUpIndicatorTask != null)
                                refreshUpIndicatorTask.cancel(true);
                            refreshUpIndicatorTask = new RefreshUpIndicatorTask();
                            refreshUpIndicatorTask.execute((Void) null);
                            photoBtn.setEnabled(true);
                        }
                    });
                    //TODO: prune and pop the back stack if the current BID has disappeared
                }
                break;
            case NetworkConnection.EVENT_CONNECTIONDELETED:
            case NetworkConnection.EVENT_DELETEBUFFER:
                Integer id = (Integer) obj;
                if (what == NetworkConnection.EVENT_DELETEBUFFER) {
                    synchronized (backStack) {
                        for (int i = 0; i < backStack.size(); i++) {
                            if (backStack.get(i).equals(id)) {
                                backStack.remove(i);
                                i--;
                            }
                        }
                    }
                }
                if (buffer != null && id == ((what == NetworkConnection.EVENT_CONNECTIONDELETED) ? buffer.getCid() : buffer.getBid())) {
                    synchronized (backStack) {
                        while (backStack != null && backStack.size() > 0) {
                            final Integer bid = backStack.get(0);
                            backStack.remove(0);
                            b = BuffersList.getInstance().getBuffer(bid);
                            if (b != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        onBufferSelected(bid);
                                        if (backStack.size() > 0)
                                            backStack.remove(0);
                                    }
                                });
                                return;
                            }
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (BuffersList.getInstance().count() == 0) {
                                startActivity(new Intent(MainActivity.this, EditConnectionActivity.class));
                                finish();
                            } else {
                                if ((NetworkConnection.getInstance().getUserInfo() == null || !open_bid(NetworkConnection.getInstance().getUserInfo().last_selected_bid)) && !open_bid(BuffersList.getInstance().firstBid()))
                                    finish();
                            }
                        }
                    });
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (refreshUpIndicatorTask != null)
                            refreshUpIndicatorTask.cancel(true);
                        refreshUpIndicatorTask = new RefreshUpIndicatorTask();
                        refreshUpIndicatorTask.execute((Void) null);
                    }
                });
                break;
            case NetworkConnection.EVENT_CHANNELMODE:
            case NetworkConnection.EVENT_CHANNELTOPIC:
                event = (IRCCloudJSONObject) obj;
                if (event != null && buffer != null && event.bid() == buffer.getBid()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            update_subtitle();
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_SELFBACK:
            case NetworkConnection.EVENT_AWAY:
                try {
                    event = (IRCCloudJSONObject) obj;
                    if (event != null && buffer != null && event.cid() == buffer.getCid() && event.getString("nick").equalsIgnoreCase(buffer.getName())) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                update_subtitle();
                            }
                        });
                    }
                } catch (Exception e1) {
                    NetworkConnection.printStackTraceToCrashlytics(e1);
                }
                break;
            case NetworkConnection.EVENT_HEARTBEATECHO:
                boolean shouldRefresh = false;
                event = (IRCCloudJSONObject) obj;
                JsonNode seenEids = event.getJsonNode("seenEids");
                Iterator<Map.Entry<String, JsonNode>> iterator = seenEids.fields();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> entry = iterator.next();
                    JsonNode eids = entry.getValue();
                    Iterator<Map.Entry<String, JsonNode>> j = eids.fields();
                    while (j.hasNext()) {
                        Map.Entry<String, JsonNode> eidentry = j.next();
                        Integer bid = Integer.valueOf(eidentry.getKey());
                        if (buffer != null && bid != buffer.getBid()) {
                            shouldRefresh = true;
                        }
                    }
                }
                if (shouldRefresh) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (refreshUpIndicatorTask != null)
                                refreshUpIndicatorTask.cancel(true);
                            refreshUpIndicatorTask = new RefreshUpIndicatorTask();
                            refreshUpIndicatorTask.execute((Void) null);
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_AUTH_FAILED:
                Intent i = new Intent(MainActivity.this, LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                finish();
                break;
            case NetworkConnection.EVENT_TEMP_UNAVAILABLE:
                error = "Your account is temporarily unavailable";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateReconnecting();
                    }
                });
                break;
            case NetworkConnection.EVENT_BUFFERMSG:
                try {
                    Event e = (Event) obj;
                    if (e != null && buffer != null) {
                        if (e.bid != buffer.getBid() && upDrawable != null) {
                            Buffer buf = BuffersList.getInstance().getBuffer(e.bid);
                            if (e.isImportant(buf.getType())) {
                                if (!upDrawableFilter.equals(highlightsFilter) && (e.highlight || buf.isConversation())) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            upDrawable.setColorFilter(highlightsFilter);
                                            upDrawableFilter = highlightsFilter;
                                        }
                                    });
                                } else if (upDrawableFilter.equals(normalFilter)) {
                                    JSONObject channelDisabledMap = null;
                                    JSONObject bufferDisabledMap = null;
                                    JSONObject channelEnabledMap = null;
                                    JSONObject bufferEnabledMap = null;
                                    if (NetworkConnection.getInstance().getUserInfo() != null && NetworkConnection.getInstance().getUserInfo().prefs != null) {
                                        try {
                                            if (NetworkConnection.getInstance().getUserInfo().prefs.has("channel-disableTrackUnread"))
                                                channelDisabledMap = NetworkConnection.getInstance().getUserInfo().prefs.getJSONObject("channel-disableTrackUnread");
                                            if (NetworkConnection.getInstance().getUserInfo().prefs.has("buffer-disableTrackUnread"))
                                                bufferDisabledMap = NetworkConnection.getInstance().getUserInfo().prefs.getJSONObject("buffer-disableTrackUnread");
                                            if (NetworkConnection.getInstance().getUserInfo().prefs.has("channel-enableTrackUnread"))
                                                channelEnabledMap = NetworkConnection.getInstance().getUserInfo().prefs.getJSONObject("channel-enableTrackUnread");
                                            if (NetworkConnection.getInstance().getUserInfo().prefs.has("buffer-enableTrackUnread"))
                                                bufferEnabledMap = NetworkConnection.getInstance().getUserInfo().prefs.getJSONObject("buffer-enableTrackUnread");
                                        } catch (Exception e1) {
                                            NetworkConnection.printStackTraceToCrashlytics(e1);
                                        }
                                    }

                                    boolean enabled = !(conn.getUserInfo().prefs.has("disableTrackUnread") && conn.getUserInfo().prefs.get("disableTrackUnread") instanceof Boolean && conn.getUserInfo().prefs.getBoolean("disableTrackUnread"));
                                    if (buf.isChannel() && channelDisabledMap != null && channelDisabledMap.has(String.valueOf(buf.getBid())) && channelDisabledMap.getBoolean(String.valueOf(buf.getBid())))
                                        enabled = false;
                                    else if (bufferDisabledMap != null && bufferDisabledMap.has(String.valueOf(buf.getBid())) && bufferDisabledMap.getBoolean(String.valueOf(buf.getBid())))
                                        enabled = false;
                                    else if (buf.isChannel() && channelEnabledMap != null && channelEnabledMap.has(String.valueOf(buf.getBid())) && channelEnabledMap.getBoolean(String.valueOf(buf.getBid())))
                                        enabled = true;
                                    else if (bufferEnabledMap != null && bufferEnabledMap.has(String.valueOf(buf.getBid())) && bufferEnabledMap.getBoolean(String.valueOf(buf.getBid())))
                                        enabled = true;

                                    if(enabled)
                                        runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            upDrawable.setColorFilter(unreadFilter);
                                            upDrawableFilter = unreadFilter;
                                        }
                                    });
                                }
                            }
                        }
                        if (e.from.equalsIgnoreCase(buffer.getName())) {
                            pendingEvents.clear();
                        } else if (pendingEvents.containsKey(e.reqid)) {
                            pendingEvents.remove(e.reqid);
                        }
                    }
                } catch (Exception e1) {
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (NetworkConnection.getInstance().ready) {
            if(buffer != null && buffer.getType() != null) {
                if (buffer.isChannel()) {
                    getMenuInflater().inflate(R.menu.activity_message_channel_userlist, menu);
                    getMenuInflater().inflate(R.menu.activity_message_channel, menu);
                } else if (buffer.isConversation())
                    getMenuInflater().inflate(R.menu.activity_message_conversation, menu);
                else if (buffer.isConsole())
                    getMenuInflater().inflate(R.menu.activity_message_console, menu);

                getMenuInflater().inflate(R.menu.activity_message_archive, menu);
                getMenuInflater().inflate(R.menu.activity_message_shortcut, menu);
            }
            getMenuInflater().inflate(R.menu.activity_main, menu);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu != null && buffer != null && buffer.getType() != null && NetworkConnection.getInstance().ready) {
            if (buffer.getArchived() == 0) {
                if (menu.findItem(R.id.menu_archive) != null)
                    menu.findItem(R.id.menu_archive).setTitle(R.string.menu_archive);
            } else {
                if (menu.findItem(R.id.menu_archive) != null)
                    menu.findItem(R.id.menu_archive).setTitle(R.string.menu_unarchive);
            }
            if (buffer.isChannel()) {
                if (ChannelsList.getInstance().getChannelForBuffer(buffer.getBid()) == null) {
                    if (menu.findItem(R.id.menu_leave) != null) {
                        menu.findItem(R.id.menu_leave).setTitle(R.string.menu_rejoin);
                        menu.findItem(R.id.menu_leave).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                    }

                    if (menu.findItem(R.id.menu_archive) != null) {
                        menu.findItem(R.id.menu_archive).setVisible(true);
                        menu.findItem(R.id.menu_archive).setEnabled(true);
                    }
                    if (menu.findItem(R.id.menu_userlist) != null) {
                        menu.findItem(R.id.menu_userlist).setEnabled(false);
                        menu.findItem(R.id.menu_userlist).setVisible(false);
                    }
                    if (menu.findItem(R.id.menu_ban_list) != null) {
                        menu.findItem(R.id.menu_ban_list).setVisible(false);
                        menu.findItem(R.id.menu_ban_list).setEnabled(false);
                    }
                } else {
                    if (menu.findItem(R.id.menu_leave) != null) {
                        menu.findItem(R.id.menu_leave).setTitle(R.string.menu_leave);
                        menu.findItem(R.id.menu_leave).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                    }
                    if (menu.findItem(R.id.menu_archive) != null) {
                        menu.findItem(R.id.menu_archive).setVisible(false);
                        menu.findItem(R.id.menu_archive).setEnabled(false);
                    }
                    if (menu.findItem(R.id.menu_ban_list) != null) {
                        menu.findItem(R.id.menu_ban_list).setVisible(true);
                        menu.findItem(R.id.menu_ban_list).setEnabled(true);
                    }
                    if (menu.findItem(R.id.menu_userlist) != null && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && getResources().getBoolean(R.bool.isTablet) && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("tabletMode", true) && !isMultiWindow()) {
                        boolean hide = true;
                        try {
                            if (conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null) {
                                JSONObject hiddenMap = conn.getUserInfo().prefs.getJSONObject("channel-hiddenMembers");
                                if (hiddenMap.has(String.valueOf(buffer.getBid())) && hiddenMap.getBoolean(String.valueOf(buffer.getBid())))
                                    hide = false;
                            }
                        } catch (JSONException e) {
                        }
                        if (hide) {
                            menu.findItem(R.id.menu_userlist).setEnabled(false);
                            menu.findItem(R.id.menu_userlist).setVisible(false);
                        } else {
                            menu.findItem(R.id.menu_userlist).setEnabled(true);
                            menu.findItem(R.id.menu_userlist).setVisible(true);
                        }
                    }
                }
            } else if (buffer.isConsole()) {
                if (menu.findItem(R.id.menu_archive) != null) {
                    menu.findItem(R.id.menu_archive).setVisible(false);
                    menu.findItem(R.id.menu_archive).setEnabled(false);
                }
                if (server != null && server.getStatus() != null && (server.getStatus().equalsIgnoreCase("waiting_to_retry") || server.getStatus().equalsIgnoreCase("queued") || (server.getStatus().contains("connected") && !server.getStatus().startsWith("dis")))) {
                    if (menu.findItem(R.id.menu_disconnect) != null)
                        menu.findItem(R.id.menu_disconnect).setTitle(R.string.menu_disconnect);
                    if (menu.findItem(R.id.menu_delete) != null) {
                        menu.findItem(R.id.menu_delete).setVisible(false);
                        menu.findItem(R.id.menu_delete).setEnabled(false);
                    }
                } else {
                    if (menu.findItem(R.id.menu_disconnect) != null)
                        menu.findItem(R.id.menu_disconnect).setTitle(R.string.menu_reconnect);
                    if (menu.findItem(R.id.menu_delete) != null) {
                        menu.findItem(R.id.menu_delete).setVisible(true);
                        menu.findItem(R.id.menu_delete).setEnabled(true);
                    }
                }
            }
        }
        if (menu != null && menu.findItem(R.id.menu_clear_backlog) != null) {
            menu.findItem(R.id.menu_clear_backlog).setEnabled(buffer != null);
        }
        if (menu != null && menu.findItem(R.id.menu_userlist) != null && menu.findItem(R.id.menu_userlist).isVisible() && userListView != null)
            userListView.setVisibility(View.VISIBLE);

        return super.onPrepareOptionsMenu(menu);
    }

    @TargetApi(24)
    @Override
    public void onProvideKeyboardShortcuts(List<KeyboardShortcutGroup> data, Menu menu, int deviceId) {
        super.onProvideKeyboardShortcuts(data, menu, deviceId);

        KeyboardShortcutGroup group = new KeyboardShortcutGroup("IRCCloud");
        group.addItem(new KeyboardShortcutInfo("Switch to previous channel", KeyEvent.KEYCODE_DPAD_UP, KeyEvent.META_ALT_ON));
        group.addItem(new KeyboardShortcutInfo("Switch to next channel", KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.META_ALT_ON));
        group.addItem(new KeyboardShortcutInfo("Switch to previous unread channel", KeyEvent.KEYCODE_DPAD_UP, KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON));
        group.addItem(new KeyboardShortcutInfo("Switch to next unread channel", KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON));
        group.addItem(new KeyboardShortcutInfo("Complete nicknames and channels", KeyEvent.KEYCODE_TAB, 0));
        group.addItem(new KeyboardShortcutInfo("Mark channel as read", KeyEvent.KEYCODE_R, KeyEvent.META_ALT_ON));
        group.addItem(new KeyboardShortcutInfo("Mark all channels as read", KeyEvent.KEYCODE_R, KeyEvent.META_ALT_ON | KeyEvent.META_SHIFT_ON));
        group.addItem(new KeyboardShortcutInfo("Toggle bold", KeyEvent.KEYCODE_B, KeyEvent.META_CTRL_ON));
        group.addItem(new KeyboardShortcutInfo("Toggle italics", KeyEvent.KEYCODE_I, KeyEvent.META_CTRL_ON));
        group.addItem(new KeyboardShortcutInfo("Toggle underline", KeyEvent.KEYCODE_U, KeyEvent.META_CTRL_ON));
        group.addItem(new KeyboardShortcutInfo("Change text color", KeyEvent.KEYCODE_C, KeyEvent.META_CTRL_ON));

        data.add(group);
    }

    private class ToggleListener implements DrawerLayout.DrawerListener {

        @Override
        public void onDrawerSlide(View view, float slideOffset) {
        }

        @Override
        public void onDrawerOpened(View view) {
            if (((DrawerLayout.LayoutParams) view.getLayoutParams()).gravity == Gravity.LEFT) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
            }
            try {
                if (getCurrentFocus() != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                }
            } catch (Exception e) {
            }
        }

        @Override
        public void onDrawerClosed(View view) {
            if (((DrawerLayout.LayoutParams) view.getLayoutParams()).gravity == Gravity.LEFT) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                updateUsersListFragmentVisibility();
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.LEFT);
            }
            MessageViewFragment mvf = (MessageViewFragment) getSupportFragmentManager().findFragmentById(R.id.messageViewFragment);
            if (mvf != null)
                mvf.drawerClosed();
        }

        @Override
        public void onDrawerStateChanged(int i) {
            if (i != DrawerLayout.STATE_SETTLING) {
                if (buffersListView != null)
                    buffersListView.setVisibility(View.VISIBLE);
                if (userListView != null)
                    userListView.setVisibility(View.VISIBLE);
                drawerLayout.setScrimColor(0x99000000);
            }
        }
    }

    private ToggleListener mDrawerListener = new ToggleListener();

    private final int REQUEST_CAMERA = 1;
    private final int REQUEST_PHOTO = 2;
    private final int REQUEST_DOCUMENT = 3;
    private final int REQUEST_UPLOADS = 4;
    private final int REQUEST_PASTEBIN = 5;

    public static Uri makeTempCopy(Uri fileUri, Context context) {
        if(fileUri == null)
            return null;

        String original_filename;

        if (Build.VERSION.SDK_INT < 16) {
            original_filename = fileUri.getLastPathSegment();
        } else {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(fileUri, null, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    original_filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                } else {
                    original_filename = fileUri.getLastPathSegment();
                }
            } catch (Exception e) {
                e.printStackTrace();
                original_filename = String.valueOf(System.currentTimeMillis());
            }
            if(cursor != null)
                cursor.close();
        }

        if(original_filename == null || original_filename.length() == 0)
            original_filename = "file";

        return makeTempCopy(fileUri, context, original_filename);
    }

    public static Uri makeTempCopy(Uri fileUri, Context context, String original_filename) {
        if(fileUri == null)
            return null;

        if(fileUri.toString().contains(context.getCacheDir().getAbsolutePath()))
            return fileUri;

        String type = context.getContentResolver().getType(fileUri);
        if (type == null) {
            String lower = original_filename.toLowerCase();
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
                type = "image/jpeg";
            else if (lower.endsWith(".png"))
                type = "image/png";
            else if (lower.endsWith(".gif"))
                type = "image/gif";
            else if (lower.endsWith(".webp"))
                type = "image/webp";
            else if (lower.endsWith(".bmp"))
                type = "image/bmp";
            else if (lower.endsWith(".mp4"))
                type = "video/mp4";
            else if (lower.endsWith(".3gpp"))
                type = "video/3gpp";
            else
                type = "application/octet-stream";
        }

        if (!original_filename.contains("."))
            original_filename += "." + type.substring(type.indexOf("/") + 1);

        try {
            Uri out = Uri.fromFile(new File(context.getCacheDir(), original_filename));
            Log.d("IRCCloud", "Copying file to " + out);
            InputStream is = IRCCloudApplication.getInstance().getApplicationContext().getContentResolver().openInputStream(fileUri);
            OutputStream os = IRCCloudApplication.getInstance().getApplicationContext().getContentResolver().openOutputStream(out);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            is.close();
            os.close();
            return out;
        } catch (Exception e) {
            e.printStackTrace();
            return fileUri;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        if (buffer != null) {
            if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
                if (imageCaptureURI != null) {
                    if (!NetworkConnection.getInstance().uploadsAvailable() || PreferenceManager.getDefaultSharedPreferences(this).getString("image_service", "IRCCloud").equals("imgur")) {
                        new ImgurRefreshTask(imageCaptureURI).execute((Void) null);
                    } else {
                        fileUploadTask = new FileUploadTask(imageCaptureURI, this);
                        if(Build.VERSION.SDK_INT >= 16 && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    REQUEST_EXTERNAL_MEDIA_IRCCLOUD);
                        } else {
                            fileUploadTask.execute((Void) null);
                        }
                    }

                    if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("keep_photos", false) && imageCaptureURI.toString().startsWith("file://")) {
                        ContentValues image = new ContentValues();
                        image.put(MediaStore.Images.Media.DATA, imageCaptureURI.toString().substring(7));
                        getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, image);
                    }
                }
            } else if (requestCode == REQUEST_PHOTO && resultCode == RESULT_OK) {
                Uri selectedImage = imageReturnedIntent.getData();
                if (selectedImage != null) {
                    selectedImage = makeTempCopy(selectedImage, this);
                    if (!NetworkConnection.getInstance().uploadsAvailable() || PreferenceManager.getDefaultSharedPreferences(this).getString("image_service", "IRCCloud").equals("imgur")) {
                        new ImgurRefreshTask(selectedImage).execute((Void) null);
                    } else {
                        fileUploadTask = new FileUploadTask(selectedImage, this);
                        if(Build.VERSION.SDK_INT >= 16 && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    REQUEST_EXTERNAL_MEDIA_IRCCLOUD);
                        } else {
                            fileUploadTask.execute((Void) null);
                        }
                    }
                }
            } else if (requestCode == REQUEST_DOCUMENT && resultCode == RESULT_OK) {
                Uri selectedFile = imageReturnedIntent.getData();
                if (selectedFile != null) {
                    selectedFile = makeTempCopy(selectedFile, this);
                    fileUploadTask = new FileUploadTask(selectedFile, this);
                    if(Build.VERSION.SDK_INT >= 16 && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_EXTERNAL_MEDIA_IRCCLOUD);
                    } else {
                        fileUploadTask.execute((Void) null);
                    }
                }
            } else if (requestCode == REQUEST_UPLOADS && resultCode == RESULT_OK) {
                buffer.setDraft("");
                messageTxt.setTextWithEmoji("");
            } else if (requestCode == REQUEST_PASTEBIN) {
                if(resultCode == RESULT_OK) {
                    pastebinResult = imageReturnedIntent;
                } else if(resultCode == RESULT_CANCELED) {
                    buffer.setDraft(imageReturnedIntent.getStringExtra("paste_contents"));
                    messageTxt.setTextWithEmoji(buffer.getDraft());
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Intent i;

        if(grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case REQUEST_EXTERNAL_MEDIA_TAKE_PHOTO:
                    try {
                        File imageDir = new File(Environment.getExternalStorageDirectory(), "IRCCloud");
                        imageDir.mkdirs();
                        new File(imageDir, ".nomedia").createNewFile();
                        File tempFile = File.createTempFile("irccloudcapture", ".jpg", imageDir);
                        imageCaptureURI = Uri.fromFile(tempFile);
                        i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                            i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageCaptureURI);
                        else
                            i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(MainActivity.this,getPackageName() + ".fileprovider",tempFile));
                        startActivityForResult(i, REQUEST_CAMERA);
                    } catch (IOException e) {
                    }
                    break;
                case REQUEST_EXTERNAL_MEDIA_RECORD_VIDEO:
                    try {
                        File imageDir = new File(Environment.getExternalStorageDirectory(), "IRCCloud");
                        imageDir.mkdirs();
                        new File(imageDir, ".nomedia").createNewFile();
                        File tempFile = File.createTempFile("irccloudcapture", ".mp4", imageDir);
                        imageCaptureURI = Uri.fromFile(tempFile);
                        i = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                            i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageCaptureURI);
                        else
                            i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(MainActivity.this,getPackageName() + ".fileprovider",tempFile));
                        startActivityForResult(i, REQUEST_CAMERA);
                    } catch (IOException e) {
                    }
                    break;
                case REQUEST_EXTERNAL_MEDIA_CHOOSE_PHOTO:
                    i = new Intent(Intent.ACTION_GET_CONTENT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("image/*");
                    startActivityForResult(Intent.createChooser(i, "Select Picture"), REQUEST_PHOTO);
                    break;
                case REQUEST_EXTERNAL_MEDIA_CHOOSE_DOCUMENT:
                    i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    startActivityForResult(Intent.createChooser(i, "Select A Document"), REQUEST_DOCUMENT);
                    break;
                case REQUEST_EXTERNAL_MEDIA_IRCCLOUD:
                    if(fileUploadTask != null) {
                        fileUploadTask.setActivity(this);
                        fileUploadTask.execute((Void) null);
                        if(fileUploadTask.metadataDialog == null && !fileUploadTask.filenameSet)
                            fileUploadTask.show_dialog();
                    }
                    break;
                case REQUEST_EXTERNAL_MEDIA_IMGUR:
                    if(imgurTask != null) {
                        imgurTask.setActivity(this);
                        imgurTask.execute((Void)null);
                    }
                    break;
            }
        } else {
            if(fileUploadTask != null) {
                if(fileUploadTask.metadataDialog != null) {
                    try {
                        fileUploadTask.metadataDialog.cancel();
                    } catch (Exception e) {

                    }
                }
                fileUploadTask.cancel(true);
                fileUploadTask = null;
            }
            if(imgurTask != null) {
                imgurTask.cancel(true);
                imgurTask = null;
            }
            Toast.makeText(this, "Upload cancelled: permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private void insertPhoto() {
        if(buffer == null)
            return;

        AlertDialog.Builder builder;
        AlertDialog dialog;
        builder = new AlertDialog.Builder(this);
        String[] items = (Build.VERSION.SDK_INT < 19 || !NetworkConnection.getInstance().uploadsAvailable()) ? new String[]{"Take a Photo", "Choose Existing", "Post a Text Snippet", "Text Snippets"} : new String[]{"Take a Photo", "Record a Video", "Choose Existing Photo", "Choose Existing Document", "Post a Text Snippet", "Text Snippets"};
        if(NetworkConnection.getInstance().uploadsAvailable()) {
            items = Arrays.copyOf(items, items.length + 1);
            items[items.length - 1] = "File Uploads";
        }

        final String[] dialogItems = items;

        builder.setItems(dialogItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent i;
                if(buffer != null) {
                    switch(dialogItems[which]) {
                        case "Take a Photo":
                            if(Build.VERSION.SDK_INT >= 16 && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        REQUEST_EXTERNAL_MEDIA_TAKE_PHOTO);
                            } else {
                                try {
                                    File imageDir = new File(Environment.getExternalStorageDirectory(), "IRCCloud");
                                    imageDir.mkdirs();
                                    new File(imageDir, ".nomedia").createNewFile();
                                    File tempFile = File.createTempFile("irccloudcapture", ".jpg", imageDir);
                                    imageCaptureURI = Uri.fromFile(tempFile);
                                    i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                                        i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageCaptureURI);
                                    else
                                        i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(MainActivity.this,getPackageName() + ".fileprovider",tempFile));
                                    startActivityForResult(i, REQUEST_CAMERA);
                                } catch (IOException e) {
                                }
                            }
                            break;
                        case "Record a Video":
                            if(Build.VERSION.SDK_INT >= 16 && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        REQUEST_EXTERNAL_MEDIA_RECORD_VIDEO);
                            } else {
                                try {
                                    File imageDir = new File(Environment.getExternalStorageDirectory(), "IRCCloud");
                                    imageDir.mkdirs();
                                    new File(imageDir, ".nomedia").createNewFile();
                                    File tempFile = File.createTempFile("irccloudcapture", ".mp4", imageDir);
                                    imageCaptureURI = Uri.fromFile(tempFile);
                                    i = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                                        i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageCaptureURI);
                                    else
                                        i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(MainActivity.this,getPackageName() + ".fileprovider",tempFile));
                                    startActivityForResult(i, REQUEST_CAMERA);
                                } catch (IOException e) {
                                }
                            }
                            break;
                        case "Choose Existing":
                        case "Choose Existing Photo":
                            if(Build.VERSION.SDK_INT >= 16 && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        REQUEST_EXTERNAL_MEDIA_CHOOSE_PHOTO);
                            } else {
                                i = new Intent(Intent.ACTION_GET_CONTENT, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                i.addCategory(Intent.CATEGORY_OPENABLE);
                                i.setType("image/*");
                                startActivityForResult(Intent.createChooser(i, "Select Picture"), REQUEST_PHOTO);
                            }
                            break;
                        case "Choose Existing Document":
                            if(Build.VERSION.SDK_INT >= 16 && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        REQUEST_EXTERNAL_MEDIA_CHOOSE_DOCUMENT);
                            } else {
                                i = new Intent(Intent.ACTION_GET_CONTENT);
                                i.addCategory(Intent.CATEGORY_OPENABLE);
                                i.setType("*/*");
                                startActivityForResult(Intent.createChooser(i, "Select A Document"), REQUEST_DOCUMENT);
                            }
                            break;
                        case "Post a Text Snippet":
                            show_pastebin_prompt();
                            break;
                        case "Text Snippets":
                            i = new Intent(MainActivity.this, PastebinsActivity.class);
                            startActivity(i);
                            break;
                        case "File Uploads":
                            i = new Intent(MainActivity.this, UploadsActivity.class);
                            i.putExtra("cid", buffer.getCid());
                            i.putExtra("to", buffer.getName());
                            i.putExtra("msg", messageTxt.getText().toString());
                            startActivityForResult(i, REQUEST_UPLOADS);
                            break;
                    }
                }
                dialog.dismiss();
            }
        });
        dialog = builder.create();
        dialog.setOwnerActivity(MainActivity.this);
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        AlertDialog.Builder builder;
        AlertDialog dialog;

        switch (item.getItemId()) {
            case android.R.id.home:
                if (drawerLayout != null) {
                    if (drawerLayout.isDrawerOpen(Gravity.LEFT))
                        drawerLayout.closeDrawer(Gravity.LEFT);
                    else if(drawerLayout.getDrawerLockMode(Gravity.LEFT) == DrawerLayout.LOCK_MODE_UNLOCKED)
                        drawerLayout.openDrawer(Gravity.LEFT);
                    drawerLayout.closeDrawer(Gravity.RIGHT);
                }
                break;
            case R.id.menu_whois:
                User u = UsersList.getInstance().findUserOnConnection(buffer.getCid(), buffer.getName());
                if(u != null)
                    if(u.ircserver != null && u.ircserver.length() > 0)
                        NetworkConnection.getInstance().whois(buffer.getCid(), buffer.getName(), u.ircserver, null);
                    else
                        NetworkConnection.getInstance().whois(buffer.getCid(), buffer.getName(), (u.joined > 0)?buffer.getName():null, null);
                else
                    NetworkConnection.getInstance().whois(buffer.getCid(), buffer.getName(), null, null);
                break;
            case R.id.menu_identify:
                NickservFragment nsFragment = new NickservFragment();
                nsFragment.setCid(buffer.getCid());
                nsFragment.show(getSupportFragmentManager(), "nickserv");
                break;
            case R.id.menu_add_network:
                addNetwork();
                break;
            case R.id.menu_channel_options:
                ChannelOptionsFragment newFragment = new ChannelOptionsFragment(buffer.getCid(), buffer.getBid());
                newFragment.show(getSupportFragmentManager(), "channeloptions");
                break;
            case R.id.menu_buffer_options:
                BufferOptionsFragment bufferFragment = new BufferOptionsFragment(buffer.getCid(), buffer.getBid(), buffer.getType());
                bufferFragment.show(getSupportFragmentManager(), "bufferoptions");
                break;
            case R.id.menu_userlist:
                if (drawerLayout != null) {
                    if (drawerLayout.isDrawerOpen(Gravity.RIGHT)) {
                        drawerLayout.closeDrawers();
                    } else {
                        drawerLayout.closeDrawer(Gravity.LEFT);
                        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
                        drawerLayout.openDrawer(Gravity.RIGHT);
                    }
                    if (!getSharedPreferences("prefs", 0).getBoolean("userSwipeTip", false)) {
                        Toast.makeText(IRCCloudApplication.getInstance().getApplicationContext(), "Drag from the edge of the screen to quickly open and close the user list", Toast.LENGTH_LONG).show();
                        SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
                        editor.putBoolean("userSwipeTip", true);
                        editor.commit();
                    }
                }
                return true;
            case R.id.menu_ignore_list:
                Bundle args = new Bundle();
                args.putInt("cid", buffer.getCid());
                IgnoreListFragment ignoreList = new IgnoreListFragment();
                ignoreList.setArguments(args);
                ignoreList.show(getSupportFragmentManager(), "ignorelist");
                return true;
            case R.id.menu_ban_list:
                NetworkConnection.getInstance().mode(buffer.getCid(), buffer.getName(), "b", null);
                return true;
            case R.id.menu_leave:
                if (ChannelsList.getInstance().getChannelForBuffer(buffer.getBid()) == null)
                    NetworkConnection.getInstance().join(buffer.getCid(), buffer.getName(), null, null);
                else
                    NetworkConnection.getInstance().part(buffer.getCid(), buffer.getName(), null, null);
                return true;
            case R.id.menu_archive:
                if (buffer.getArchived() == 0)
                    NetworkConnection.getInstance().archiveBuffer(buffer.getCid(), buffer.getBid(), null);
                else
                    NetworkConnection.getInstance().unarchiveBuffer(buffer.getCid(), buffer.getBid(), null);
                return true;
            case R.id.menu_delete:
                builder = new AlertDialog.Builder(MainActivity.this);

                if (buffer.isConsole())
                    builder.setTitle("Delete Connection");
                else
                    builder.setTitle("Delete History");

                if (buffer.isConsole())
                    builder.setMessage("Are you sure you want to remove this connection?");
                else if (buffer.isChannel())
                    builder.setMessage("Are you sure you want to clear your history in " + buffer.getName() + "?");
                else
                    builder.setMessage("Are you sure you want to clear your history with " + buffer.getName() + "?");

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (buffer.isConsole()) {
                            NetworkConnection.getInstance().deleteServer(buffer.getCid(), null);
                        } else {
                            NetworkConnection.getInstance().deleteBuffer(buffer.getCid(), buffer.getBid(), null);
                        }
                        dialog.dismiss();
                    }
                });
                dialog = builder.create();
                dialog.setOwnerActivity(MainActivity.this);
                dialog.show();
                return true;
            case R.id.menu_editconnection:
                if (!getResources().getBoolean(R.bool.isTablet)) {
                    Intent i = new Intent(this, EditConnectionActivity.class);
                    i.putExtra("cid", buffer.getCid());
                    startActivity(i);
                } else {
                    EditConnectionFragment editFragment = new EditConnectionFragment();
                    editFragment.setCid(buffer.getCid());
                    editFragment.show(getSupportFragmentManager(), "editconnection");
                }
                return true;
            case R.id.menu_disconnect:
                if (server != null && server.getStatus() != null && (server.getStatus().equalsIgnoreCase("waiting_to_retry")) || (server.getStatus().contains("connected") && !server.getStatus().startsWith("dis"))) {
                    NetworkConnection.getInstance().disconnect(buffer.getCid(), null, null);
                } else {
                    NetworkConnection.getInstance().reconnect(buffer.getCid(), null);
                }
                return true;
            case R.id.menu_invite:
                View view = getDialogTextPrompt();
                TextView prompt = view.findViewById(R.id.prompt);
                final EditText input = view.findViewById(R.id.textInput);
                input.setText("");
                prompt.setText("Invite someone to join " + buffer.getName());
                builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")");
                builder.setView(view);
                builder.setPositiveButton("Invite", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        conn.invite(buffer.getCid(), buffer.getName(), input.getText().toString(), null);
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setNeutralButton("Share URL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String uri;
                        try {
                            if (BuildConfig.ENTERPRISE) {
                                uri = "irc";
                                if (server.getSsl() > 0)
                                    uri += "s";
                                uri += "://" + server.getHostname() + ":" + server.getPort();
                                if (buffer.isChannel()) {
                                    uri += "/" + URLEncoder.encode(buffer.getName(), "UTF-8");
                                    Channel c = ChannelsList.getInstance().getChannelForBuffer(buffer.getBid());
                                    if (c != null && c.hasMode("k"))
                                        uri += "," + c.paramForMode("k");
                                }
                            } else {
                                uri = "https://www.irccloud.com/invite?";
                                uri += "channel=" + URLEncoder.encode(buffer.getName(), "UTF-8");
                                uri += "&hostname=" + server.getHostname();
                                uri += "&port=" + server.getPort();
                                if (server.getSsl() > 0)
                                    uri += "&ssl=1";
                            }
                            Intent i = new Intent(Intent.ACTION_SEND);
                            i.setType("text/plain");
                            i.putExtra(Intent.EXTRA_TEXT, uri);
                            startActivity(Intent.createChooser(i, "Share URL"));
                        } catch (Exception e) {

                        }
                        dialog.dismiss();
                    }
                });
                dialog = builder.create();
                dialog.setOwnerActivity(MainActivity.this);
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                dialog.show();
                return true;
            case R.id.menu_clear_backlog:
                EventsList.getInstance().deleteEventsForBuffer(buffer.getBid());
                onBufferSelected(buffer.getBid());
                return true;
            case R.id.menu_create_shortcut:
                try {
                    String uri = "irc";
                    if (server.getSsl() > 0)
                        uri += "s";
                    uri += "://" + server.getHostname() + ":" + server.getPort();
                    uri += "/" + URLEncoder.encode(buffer.getName(), "UTF-8");
                    if (buffer.isChannel()) {
                        Channel c = ChannelsList.getInstance().getChannelForBuffer(buffer.getBid());
                        if (c != null && c.hasMode("k"))
                            uri += "," + c.paramForMode("k");
                    }
                    final Intent shortcutIntent = new Intent(this, MainActivity.class);
                    shortcutIntent.setAction(Intent.ACTION_VIEW);
                    shortcutIntent.setData(Uri.parse(uri));

                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        final Intent intent = new Intent();
                        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, buffer.getName());
                        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher));
                        intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                        sendBroadcast(intent);
                    } else {
                        ShortcutManager mShortcutManager = getSystemService(ShortcutManager.class);

                        if (mShortcutManager.isRequestPinShortcutSupported()) {
                            Icon icon = null;
                            if(buffer.isChannel()) {
                                icon = Icon.createWithAdaptiveBitmap(Avatar.generateBitmap("#", 0xFFFFFFFF, 0xFFAAAAAA, false, (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 108, getResources().getDisplayMetrics()), false));
                            } else if(buffer.isConversation()) {
                                icon = Icon.createWithAdaptiveBitmap(AvatarsList.getInstance().getAvatar(buffer.getCid(), buffer.getName()).getBitmap(false, (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 108, getResources().getDisplayMetrics()), false, false));
                            }
                            ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(this, String.valueOf(buffer.getBid()))
                                    .setIntent(shortcutIntent)
                                    .setShortLabel(buffer.getName())
                                    .setLongLabel(buffer.getName() + " (" + ((server.getName() != null && server.getName().length() > 0) ? server.getName() : server.getHostname()) + ")")
                                    .setIcon(icon)
                                    .build();

                            mShortcutManager.requestPinShortcut(pinShortcutInfo, null);
                        }
                    }
                    moveTaskToBack(true);
                } catch (Exception e) {
                    NetworkConnection.printStackTraceToCrashlytics(e);
                }
                return true;
            case R.id.menu_download_logs:
                Intent i = new Intent(this, LogExportsActivity.class);
                if(buffer != null) {
                    i.putExtra("cid", buffer.getCid());
                    i.putExtra("bid", buffer.getBid());
                }
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void editTopic(final Channel c) {
        if(c != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = getDialogTextPrompt();
            TextView prompt = view.findViewById(R.id.prompt);
            final IRCEditText input = view.findViewById(R.id.textInput);
            input.setMaxLines(getResources().getInteger(R.integer.max_input_lines));
            input.setHorizontallyScrolling(false);
            input.setText(ColorFormatter.html_to_spanned(ColorFormatter.emojify(ColorFormatter.irc_to_html(TextUtils.htmlEncode(c.topic_text))), false, null));
            prompt.setVisibility(View.GONE);
            if(server.isupport != null && server.isupport.has("TOPICLEN"))
                builder.setTitle("Topic for " + c.name + " (" + (server.isupport.get("TOPICLEN").asInt() - input.getText().length()) + " chars)");
            else
                builder.setTitle("Topic for " + c.name);
            builder.setView(view);
            builder.setPositiveButton("Set Topic", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    conn.topic(c.cid, c.name, input.toIRC(), null);
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            final AlertDialog dialog = builder.create();
            input.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    if(server.isupport != null && server.isupport.has("TOPICLEN"))
                        dialog.setTitle("Topic for " + c.name + " (" + (server.isupport.get("TOPICLEN").asInt() - input.getText().length()) + " chars)");
                    else
                        dialog.setTitle("Topic for " + c.name);
                }
            });
            dialog.setOwnerActivity(this);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            dialog.show();
        }
    }

    @Override
    public void onMessageDoubleClicked(Event event) {
        if (event == null)
            return;

        String from = event.from;
        if (from == null || from.length() == 0)
            from = event.nick;

        onUserDoubleClicked(from);
    }

    @Override
    public void onUserDoubleClicked(String from) {
        if (messageTxt == null || from == null || from.length() == 0)
            return;

        if (!getSharedPreferences("prefs", 0).getBoolean("mentionTip", false)) {
            SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
            editor.putBoolean("mentionTip", true);
            editor.commit();
        }

        if (drawerLayout != null)
            drawerLayout.closeDrawers();

        if (messageTxt.getText().length() == 0) {
            messageTxt.append(from + ": ");
        } else {
            int oldPosition = messageTxt.getSelectionStart();
            if(oldPosition < 0)
                oldPosition = 0;
            String text = messageTxt.getText().toString();
            int start = oldPosition - 1;
            if (start > 0 && text.charAt(start) == ' ')
                start--;
            while (start > 0 && text.charAt(start) != ' ')
                start--;
            int match = text.indexOf(from, start);
            int end = oldPosition + from.length();
            if (end > text.length() - 1)
                end = text.length() - 1;
            char nextChar = (match + from.length() < text.length()) ? text.charAt(match + from.length()) : 0;
            if (match >= 0 && match < end && (nextChar == 0 || nextChar == ' ' || nextChar == ':')) {
                String newtext = "";
                if (match > 1 && text.charAt(match - 1) == ' ')
                    newtext = text.substring(0, match - 1);
                else
                    newtext = text.substring(0, match);
                if (match + from.length() < text.length() && text.charAt(match + from.length()) == ':' &&
                        match + from.length() + 1 < text.length() && text.charAt(match + from.length() + 1) == ' ') {
                    if (match + from.length() + 2 < text.length())
                        newtext += text.substring(match + from.length() + 2, text.length());
                } else if (match + from.length() < text.length()) {
                    newtext += text.substring(match + from.length(), text.length());
                }
                if (newtext.endsWith(" "))
                    newtext = newtext.substring(0, newtext.length() - 1);
                if (newtext.equals(":"))
                    newtext = "";
                messageTxt.setTextWithEmoji(newtext);
                if (match < newtext.length())
                    messageTxt.setSelection(match);
                else
                    messageTxt.setSelection(newtext.length());
            } else {
                if (oldPosition == text.length() - 1) {
                    text += " " + from + " ";
                } else {
                    String newtext = oldPosition > 0?text.substring(0, oldPosition):"";
                    if (!newtext.endsWith(" "))
                        from = " " + from;
                    if (!text.substring(oldPosition, text.length()).startsWith(" "))
                        from += " ";
                    newtext += from;
                    newtext += text.substring(oldPosition, text.length());
                    if (newtext.endsWith(" "))
                        newtext = newtext.substring(0, newtext.length() - 1);
                    text = newtext + " ";
                }
                messageTxt.setTextWithEmoji(text);
                if (text.length() > 0) {
                    if (oldPosition + from.length() + 2 < text.length())
                        messageTxt.setSelection(oldPosition + from.length());
                    else
                        messageTxt.setSelection(text.length());
                }
            }
        }
        messageTxt.requestFocus();
        InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        keyboard.showSoftInput(messageTxt, 0);
    }

    @Override
    public boolean onBufferLongClicked(final Buffer b) {
        if (b == null)
            return false;

        ArrayList<String> itemList = new ArrayList<String>();
        final String[] items;
        Server s = b.getServer();

        if (buffer == null || b.getBid() != buffer.getBid())
            itemList.add("Open");

        if(conn != null && conn.getState() == NetworkConnection.STATE_CONNECTED) {
            if (ChannelsList.getInstance().getChannelForBuffer(b.getBid()) != null) {
                itemList.add("Leave");
                itemList.add("Display Options…");
                itemList.add("Invite to Channel…");
                itemList.add("Delete");
            } else {
                if (b.isChannel())
                    itemList.add("Join");
                else if (b.isConsole()) {
                    if (s.getStatus().equalsIgnoreCase("waiting_to_retry") || (s.getStatus().contains("connected") && !s.getStatus().startsWith("dis"))) {
                        itemList.add("Join a Channel…");
                        itemList.add("Disconnect");
                    } else {
                        itemList.add("Connect");
                        itemList.add("Delete");
                    }
                    itemList.add("Edit Connection…");
                }
                if (!b.isConsole()) {
                    if (b.getArchived() == 0)
                        itemList.add("Archive");
                    else
                        itemList.add("Unarchive");
                    itemList.add("Delete");
                }
                if (!b.isChannel()) {
                    itemList.add("Display Options…");
                }
            }
            itemList.add("Mark All As Read");

            ArrayList<Buffer> buffers = BuffersList.getInstance().getBuffersForServer(b.getCid());
            for(Buffer b1 : buffers) {
                if(b1.getArchived() == 0 && b1.getType().equals("conversation")) {
                    itemList.add("Delete Active Conversations…");
                    break;
                }
            }
            itemList.add("Add A Network");
            itemList.add("Reorder Connections");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (b.isConsole())
            builder.setTitle(s.getName());
        else
            builder.setTitle(b.getName());
        items = itemList.toArray(new String[itemList.size()]);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int item) {
                AlertDialog.Builder builder;
                AlertDialog dialog;

                if (conn == null || b == null)
                    return;

                if (items[item].equals("Open")) {
                    onBufferSelected(b.getBid());
                } else if (items[item].equals("Join")) {
                    conn.join(b.getCid(), b.getName(), null, null);
                } else if (items[item].equals("Leave")) {
                    conn.part(b.getCid(), b.getName(), null, null);
                } else if (items[item].equals("Archive")) {
                    conn.archiveBuffer(b.getCid(), b.getBid(), null);
                } else if (items[item].equals("Unarchive")) {
                    conn.unarchiveBuffer(b.getCid(), b.getBid(), null);
                } else if (items[item].equals("Connect")) {
                    conn.reconnect(b.getCid(), null);
                } else if (items[item].equals("Disconnect")) {
                    conn.disconnect(b.getCid(), null, null);
                } else if (items[item].equals("Display Options…")) {
                    if (b.isChannel()) {
                        ChannelOptionsFragment newFragment = new ChannelOptionsFragment(b.getCid(), b.getBid());
                        newFragment.show(getSupportFragmentManager(), "channeloptions");
                    } else {
                        BufferOptionsFragment newFragment = new BufferOptionsFragment(b.getCid(), b.getBid(), b.getType());
                        newFragment.show(getSupportFragmentManager(), "bufferoptions");
                    }
                } else if (items[item].equals("Edit Connection…")) {
                    if (!getResources().getBoolean(R.bool.isTablet)) {
                        Intent i = new Intent(MainActivity.this, EditConnectionActivity.class);
                        i.putExtra("cid", b.getCid());
                        startActivity(i);
                    } else {
                        EditConnectionFragment editFragment = new EditConnectionFragment();
                        editFragment.setCid(b.getCid());
                        editFragment.show(getSupportFragmentManager(), "editconnection");
                    }
                } else if (items[item].equals("Mark All As Read")) {
                    ArrayList<Integer> cids = new ArrayList<Integer>();
                    ArrayList<Integer> bids = new ArrayList<Integer>();
                    ArrayList<Long> eids = new ArrayList<Long>();

                    for (Buffer b : BuffersList.getInstance().getBuffers()) {
                        if (b.getUnread() == 1 && EventsList.getInstance().lastEidForBuffer(b.getBid()) > 0) {
                            b.setUnread(0);
                            b.setHighlights(0);
                            b.setLast_seen_eid(EventsList.getInstance().lastEidForBuffer(b.getBid()));
                            cids.add(b.getCid());
                            bids.add(b.getBid());
                            eids.add(b.getLast_seen_eid());
                        }
                    }
                    BuffersListFragment blf = (BuffersListFragment) getSupportFragmentManager().findFragmentById(R.id.BuffersList);
                    if (blf != null)
                        blf.refresh();
                    if (conn != null && buffer != null)
                        conn.heartbeat(buffer.getBid(), cids.toArray(new Integer[cids.size()]), bids.toArray(new Integer[bids.size()]), eids.toArray(new Long[eids.size()]), null);
                } else if (items[item].equals("Delete")) {
                    builder = new AlertDialog.Builder(MainActivity.this);

                    if (b.isConsole())
                        builder.setTitle("Delete Connection");
                    else
                        builder.setTitle("Delete History");

                    if (b.isConsole())
                        builder.setMessage("Are you sure you want to remove this connection?");
                    else if (b.isChannel())
                        builder.setMessage("Are you sure you want to clear your history in " + b.getName() + "?");
                    else
                        builder.setMessage("Are you sure you want to clear your history with " + b.getName() + "?");

                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (b.isConsole()) {
                                NetworkConnection.getInstance().deleteServer(b.getCid(), null);
                            } else {
                                NetworkConnection.getInstance().deleteBuffer(b.getCid(), b.getBid(), null);
                            }
                            dialog.dismiss();
                        }
                    });
                    dialog = builder.create();
                    dialog.setOwnerActivity(MainActivity.this);
                    dialog.show();
                } else if (items[item].equals("Add A Network")) {
                    addNetwork();
                } else if (items[item].equals("Reorder Connections")) {
                    reorder();
                } else if (items[item].equals("Invite to Channel…")) {
                    View view = getDialogTextPrompt();
                    TextView prompt = view.findViewById(R.id.prompt);
                    final EditText input = view.findViewById(R.id.textInput);
                    input.setText("");
                    prompt.setText("Invite someone to join " + b.getName());
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")");
                    builder.setView(view);
                    builder.setPositiveButton("Invite", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            NetworkConnection.getInstance().invite(b.getCid(), b.getName(), input.getText().toString(), null);
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.setNeutralButton("Share URL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String uri;
                            try {
                                Server s = b.getServer();
                                if (BuildConfig.ENTERPRISE) {
                                    uri = "irc";
                                    if (s.getSsl() > 0)
                                        uri += "s";
                                    uri += "://" + s.getHostname() + ":" + s.getPort();
                                    if (b.isChannel()) {
                                        uri += "/" + URLEncoder.encode(b.getName(), "UTF-8");
                                        Channel c = ChannelsList.getInstance().getChannelForBuffer(b.getBid());
                                        if (c != null && c.hasMode("k"))
                                            uri += "," + c.paramForMode("k");
                                    }
                                } else {
                                    uri = "https://www.irccloud.com/invite?";
                                    uri += "channel=" + URLEncoder.encode(b.getName(), "UTF-8");
                                    uri += "&hostname=" + s.getHostname();
                                    uri += "&port=" + s.getPort();
                                    if (s.getSsl() > 0)
                                        uri += "&ssl=1";
                                }
                                Intent i = new Intent(Intent.ACTION_SEND);
                                i.setType("text/plain");
                                i.putExtra(Intent.EXTRA_TEXT, uri);
                                i.putExtra(ShareCompat.EXTRA_CALLING_PACKAGE, getPackageName());
                                i.putExtra(ShareCompat.EXTRA_CALLING_ACTIVITY, getPackageManager().getLaunchIntentForPackage(getPackageName()).getComponent());
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_NEW_TASK);

                                Answers.getInstance().logShare(new ShareEvent().putContentType("Channel"));
                                startActivity(Intent.createChooser(i, "Share URL"));
                            } catch (Exception e) {

                            }
                            dialog.dismiss();
                        }
                    });
                    dialog = builder.create();
                    dialog.setOwnerActivity(MainActivity.this);
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    dialog.show();
                } else if (items[item].equals("Join a Channel…")) {
                    View view = getDialogTextPrompt();
                    TextView prompt = view.findViewById(R.id.prompt);
                    final EditText input = view.findViewById(R.id.textInput);
                    input.setText("");
                    prompt.setText("Which channel do you want to join?");
                    builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")");
                    builder.setView(view);
                    builder.setPositiveButton("Join", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            NetworkConnection.getInstance().say(b.getCid(), null, "/join " + input.getText().toString(), null);
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog = builder.create();
                    dialog.setOwnerActivity(MainActivity.this);
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    dialog.show();
                } else if (items[item].equals("Delete Active Conversations…")) {
                    SpamFragment spamFragment = new SpamFragment();
                    spamFragment.setCid(b.getCid());
                    spamFragment.show(getSupportFragmentManager(), "spam");
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setOwnerActivity(this);
        dialog.show();
        return true;
    }

    @Override
    public boolean onMessageLongClicked(Event event) {
        String from = event.from;
        if (from == null || from.length() == 0)
            from = event.nick;

        User user = UsersList.getInstance().getUser(buffer.getBid(), from);

        if(user == null)
            user = UsersList.getInstance().findUserOnConnection(buffer.getCid(), from);

        if (user == null && from != null && event.hostmask != null) {
            user = new User();
            user.nick = from;
            user.hostmask = event.hostmask;
            user.mode = "";
            user.joined = 0;
        }

        if (user == null && event.html == null)
            return false;

        if (event.hostmask != null && event.hostmask.length() > 0)
            user.hostmask = event.from_hostmask != null ? event.from_hostmask : event.hostmask;

        if (event.row_type == ROW_FILE || event.row_type == ROW_THUMBNAIL) {
            if(event.entities.has("id"))
                showUserPopup(user, ColorFormatter.html_to_spanned(UriTemplate.fromTemplate(ColorFormatter.file_uri_template).set("id", event.entities.get("id").asText()).expand(), true, ServersList.getInstance().getServer(event.cid)), event.entities);
            else
                showUserPopup(user, ColorFormatter.html_to_spanned(event.entities.get("url").asText(), true, ServersList.getInstance().getServer(event.cid)), event.entities);
        } else if (event.html != null) {
            String html = event.html;

            if ((event.type.equals("buffer_msg") || event.type.equals("notice")) && user != null) {
                CollapsedEventsList c = new CollapsedEventsList();
                html = "<b>&lt;" + ColorFormatter.irc_to_html(c.formatNick(event.from, event.from_mode, false)) + "&gt;</b> " + ColorFormatter.irc_to_html(event.msg);
            }
            String timestamp = event.timestamp;
            if(timestamp.length() == 0 && event.parent_eid > 0) {
                Event e = EventsList.getInstance().getEvent(event.parent_eid, event.bid);
                timestamp = e.timestamp;
            }
            showUserPopup(user, ColorFormatter.html_to_spanned(timestamp + " " + html, true, ServersList.getInstance().getServer(event.cid)), null);
        } else {
            showUserPopup(user, null, null);
        }
        return true;
    }

    @Override
    public void onFailedMessageClicked(Event event) {
        final Event e = event;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if(server != null)
            builder.setTitle(server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")");
        else
            builder.setTitle("IRCCloud");
        builder.setMessage("This message could not be sent");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    dialog.dismiss();
                } catch (IllegalArgumentException e) {
                }
            }
        });
        builder.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    pendingEvents.remove(e.reqid);
                    e.pending = true;
                    e.failed = false;
                    e.bg_color = colorScheme.selfBackgroundColor;
                    e.reqid = NetworkConnection.getInstance().say(e.cid, e.chan, e.command, new NetworkConnection.IRCResultCallback() {
                        @Override
                        public void onIRCResult(IRCCloudJSONObject result) {
                            if(!result.getBoolean("success")) {
                                EventsList.getInstance().deleteEvent(e.eid, e.bid);
                                pendingEvents.remove(e.reqid);
                                e.failed = true;
                                e.bg_color = colorScheme.errorBackgroundColor;
                                if (e.expiration_timer != null)
                                    e.expiration_timer.cancel();
                                conn.notifyHandlers(NetworkConnection.EVENT_BUFFERMSG, e);
                            }
                        }
                    });
                    if (e.reqid >= 0) {
                        pendingEvents.put(e.reqid, e);
                        e.expiration_timer = new TimerTask() {
                            @Override
                            public void run() {
                                if (pendingEvents.containsKey(e.reqid)) {
                                    pendingEvents.remove(e.reqid);
                                    e.failed = true;
                                    e.bg_color = colorScheme.errorBackgroundColor;
                                    e.expiration_timer = null;
                                    NetworkConnection.getInstance().notifyHandlers(NetworkConnection.EVENT_BUFFERMSG, e, MainActivity.this);
                                }
                            }
                        };
                        if(countdownTimer != null)
                            countdownTimer.schedule(e.expiration_timer, 30000);
                    }
                    dialog.dismiss();
                } catch (IllegalArgumentException e) {
                }
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setOwnerActivity(this);
        dialog.show();
    }

    @Override
    public void onUserSelected(int c, String chan, String nick) {
        UsersList u = UsersList.getInstance();
        showUserPopup(u.getUser(buffer.getBid(), nick), null, null);
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private void showUserPopup(User user, Spanned message, final JsonNode entities) {
        ArrayList<String> itemList = new ArrayList<String>();
        final String[] items;
        SpannableStringBuilder sb;
        if(message != null && message.length() > 0) {
            sb = new SpannableStringBuilder(message);
            for (int i = 0; i < sb.length(); i++) {
                if (sb.charAt(i) == '\u00a0') {
                    sb.replace(i, i + 1, " ");
                }
            }
        } else {
            sb = new SpannableStringBuilder();
        }
        final SpannableString text_to_copy = SpannableString.valueOf(sb);

        selected_user = user;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        if (message != null) {
            if (message.getSpans(0, message.length(), URLSpan.class).length > 0) {
                itemList.add("Copy URL to clipboard");
                itemList.add("Share URL…");
            }
            if(entities != null) {
                if(entities.has("own_file") && entities.get("own_file").asBoolean())
                    itemList.add("Delete File");
                itemList.add("Close Preview");
            }
            itemList.add("Copy Message");
        }

        if (selected_user != null) {
            if(conn != null && conn.getState() == NetworkConnection.STATE_CONNECTED) {
                itemList.add("Whois…");
                itemList.add("Send a message");
                itemList.add("Mention");
                itemList.add("Invite to a channel…");
                itemList.add("Ignore");
                if (buffer.isChannel()) {
                    User self_user = UsersList.getInstance().getUser(buffer.getBid(), server.getNick());
                    if (self_user != null && self_user.mode != null) {
                        if (self_user.mode.contains(server != null ? server.MODE_OPER : "Y") || self_user.mode.contains(server != null ? server.MODE_OWNER : "q") || self_user.mode.contains(server != null ? server.MODE_ADMIN : "a") || self_user.mode.contains(server != null ? server.MODE_OP : "o")) {
                            if (selected_user.mode.contains(server != null ? server.MODE_OP : "o"))
                                itemList.add("Deop");
                            else
                                itemList.add("Op");
                        }
                        if (self_user.mode.contains(server != null ? server.MODE_OPER : "Y") || self_user.mode.contains(server != null ? server.MODE_OWNER : "q") || self_user.mode.contains(server != null ? server.MODE_ADMIN : "a") || self_user.mode.contains(server != null ? server.MODE_OP : "o") || self_user.mode.contains(server != null ? server.MODE_HALFOP : "h")) {
                            if (selected_user.mode.contains(server != null ? server.MODE_VOICED : "v"))
                                itemList.add("Devoice");
                            else
                                itemList.add("Voice");
                            itemList.add("Kick…");
                            itemList.add("Ban…");
                        }
                    }
                }
            }
            itemList.add("Copy Hostmask");
        }

        items = itemList.toArray(new String[itemList.size()]);

        if (selected_user != null)
            if (selected_user.hostmask != null && selected_user.hostmask.length() > 0)
                builder.setTitle(selected_user.nick + "\n(" + ColorFormatter.strip(selected_user.hostmask) + ")");
            else
                builder.setTitle(selected_user.nick);
        else
            builder.setTitle("Message");

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, final int item) {
                if (conn == null || buffer == null)
                    return;

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                View view;
                final TextView prompt;
                final EditText input;
                AlertDialog dialog;

                if (items[item].equals("Copy Message")) {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        android.content.ClipData clip = android.content.ClipData.newPlainText("IRCCloud Message", text_to_copy);
                        clipboard.setPrimaryClip(clip);
                    } else {
                        Toast.makeText(IRCCloudApplication.getInstance().getApplicationContext(), "Unable to copy message. Please try again.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(IRCCloudApplication.getInstance().getApplicationContext(), "Message copied to clipboard", Toast.LENGTH_SHORT).show();
                } else if (items[item].equals("Copy Hostmask")) {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Hostmask", selected_user.nick + "!" + selected_user.hostmask);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(IRCCloudApplication.getInstance().getApplicationContext(), "Hostmask copied to clipboard", Toast.LENGTH_SHORT).show();
                } else if ((items[item].equals("Copy URL to clipboard") || items[item].equals("Share URL…") ) && text_to_copy != null) {
                    final ArrayList<String> urlListItems = new ArrayList<String>();

                    for (URLSpan o : text_to_copy.getSpans(0, text_to_copy.length(), URLSpan.class)) {
                        String url = o.getURL();
                        url = url.replace(getResources().getString(R.string.IMAGE_SCHEME) + "://", "http://");
                        url = url.replace(getResources().getString(R.string.IMAGE_SCHEME_SECURE) + "://", "https://");
                        url = url.replace(getResources().getString(R.string.VIDEO_SCHEME) + "://", "http://");
                        url = url.replace(getResources().getString(R.string.VIDEO_SCHEME_SECURE) + "://", "https://");
                        if (server != null) {
                            url = url.replace(getResources().getString(R.string.IRCCLOUD_SCHEME) + "://cid/" + server.getCid() + "/", ((server.getSsl() >0)?"ircs://":"irc://") + server.getHostname() + ":" + server.getPort() + "/");
                        }
                        urlListItems.add(url);
                    }
                    if (urlListItems.size() == 1) {
                        if(items[item].equals("Copy URL to clipboard")) {
                            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            android.content.ClipData clip = android.content.ClipData.newPlainText(urlListItems.get(0), urlListItems.get(0));
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(IRCCloudApplication.getInstance().getApplicationContext(), "URL copied to clipboard", Toast.LENGTH_SHORT).show();
                        } else {
                            Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse(urlListItems.get(0)));
                            intent.setType("text/plain");
                            intent.putExtra(Intent.EXTRA_TEXT, urlListItems.get(0));
                            intent.putExtra(ShareCompat.EXTRA_CALLING_PACKAGE, getPackageName());
                            intent.putExtra(ShareCompat.EXTRA_CALLING_ACTIVITY, getPackageManager().getLaunchIntentForPackage(getPackageName()).getComponent());
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(Intent.createChooser(intent, "Share URL"));
                            Answers.getInstance().logShare(new ShareEvent().putContentType("URL"));
                        }
                    } else {
                        builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setTitle("Choose a URL");

                        builder.setItems(urlListItems.toArray(new String[urlListItems.size()]), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(items[item].equals("Copy URL to clipboard")) {
                                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                    android.content.ClipData clip = android.content.ClipData.newPlainText(urlListItems.get(i), urlListItems.get(i));
                                    clipboard.setPrimaryClip(clip);
                                    Toast.makeText(IRCCloudApplication.getInstance().getApplicationContext(), "URL copied to clipboard", Toast.LENGTH_SHORT).show();
                                } else {
                                    Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse(urlListItems.get(i)));
                                    intent.setType("text/plain");
                                    intent.putExtra(Intent.EXTRA_TEXT, urlListItems.get(i));
                                    intent.putExtra(ShareCompat.EXTRA_CALLING_PACKAGE, getPackageName());
                                    intent.putExtra(ShareCompat.EXTRA_CALLING_ACTIVITY, getPackageManager().getLaunchIntentForPackage(getPackageName()).getComponent());
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(Intent.createChooser(intent, "Share URL"));
                                    Answers.getInstance().logShare(new ShareEvent().putContentType("URL"));
                                }
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        dialog = builder.create();
                        dialog.setOwnerActivity(MainActivity.this);
                        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        dialog.show();
                    }
                } else if (items[item].equals("Delete File")) {
                    conn.deleteFile(entities.get("id").asText(), new NetworkConnection.IRCResultCallback() {
                        @Override
                        public void onIRCResult(IRCCloudJSONObject result) {
                            if(result.getBoolean("success")) {
                                MessageViewFragment mvf = (MessageViewFragment) getSupportFragmentManager().findFragmentById(R.id.messageViewFragment);
                                if (mvf != null)
                                    mvf.uncacheFileId(entities.get("id").asText());
                            } else {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "Unable to delete file. Please try again shortly.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });
                } else if (items[item].equals("Close Preview")) {
                    MessageViewFragment mvf = (MessageViewFragment) getSupportFragmentManager().findFragmentById(R.id.messageViewFragment);
                    if (mvf != null) {
                        if(entities.has("id"))
                            mvf.hideFileId(entities.get("id").asText());
                        else
                            mvf.hideFileId(entities.get("url").asText());
                    }
                } else if (items[item].equals("Whois…")) {
                    if(selected_user.ircserver != null && selected_user.ircserver.length() > 0)
                        conn.whois(buffer.getCid(), selected_user.nick, selected_user.ircserver, null);
                    else
                        conn.whois(buffer.getCid(), selected_user.nick, (selected_user.joined > 0)?selected_user.nick:null, null);
                } else if (items[item].equals("Send a message")) {
                    drawerLayout.closeDrawers();
                    drawerLayout.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            NetworkConnection.getInstance().say(buffer.getCid(), null, "/query " + selected_user.nick, null);
                        }
                    }, 300);
                } else if (items[item].equals("Mention")) {
                    if (!getSharedPreferences("prefs", 0).getBoolean("mentionTip", false)) {
                        Toast.makeText(IRCCloudApplication.getInstance().getApplicationContext(), "Double-tap a message to quickly reply to the sender", Toast.LENGTH_LONG).show();
                        SharedPreferences.Editor editor = getSharedPreferences("prefs", 0).edit();
                        editor.putBoolean("mentionTip", true);
                        editor.commit();
                    }
                    onUserDoubleClicked(selected_user.nick);
                } else if (items[item].equals("Invite to a channel…")) {
                    view = getDialogTextPrompt();
                    prompt = view.findViewById(R.id.prompt);
                    input = view.findViewById(R.id.textInput);
                    input.setText("");
                    prompt.setText("Invite " + selected_user.nick + " to a channel");
                    builder.setTitle(server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")");
                    builder.setView(view);
                    builder.setPositiveButton("Invite", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            NetworkConnection.getInstance().invite(buffer.getCid(), input.getText().toString(), selected_user.nick, null);
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog = builder.create();
                    dialog.setOwnerActivity(MainActivity.this);
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    dialog.show();
                } else if (items[item].equals("Ignore")) {
                    view = getDialogTextPrompt();
                    prompt = view.findViewById(R.id.prompt);
                    input = view.findViewById(R.id.textInput);
                    if(selected_user.hostmask != null && selected_user.hostmask.length() > 0)
                        input.setText("*!" + selected_user.hostmask);
                    else
                        input.setText(selected_user.nick + "!*");
                    prompt.setText("Ignore messages for " + selected_user.nick + " at this hostmask");
                    builder.setTitle(server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")");
                    builder.setView(view);
                    builder.setPositiveButton("Ignore", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            NetworkConnection.getInstance().ignore(buffer.getCid(), input.getText().toString(), null);
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog = builder.create();
                    dialog.setOwnerActivity(MainActivity.this);
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    dialog.show();
                } else if (items[item].equals("Op")) {
                    conn.mode(buffer.getCid(), buffer.getName(), "+" + (server != null ? server.MODE_OP : "o") + " " + selected_user.nick, null);
                } else if (items[item].equals("Deop")) {
                    conn.mode(buffer.getCid(), buffer.getName(), "-" + (server != null ? server.MODE_OP : "o") + " " + selected_user.nick, null);
                } else if (items[item].equals("Voice")) {
                    conn.mode(buffer.getCid(), buffer.getName(), "+" + (server != null ? server.MODE_VOICED : "v") + " " + selected_user.nick, null);
                } else if (items[item].equals("Devoice")) {
                    conn.mode(buffer.getCid(), buffer.getName(), "-" + (server != null ? server.MODE_VOICED : "v") + " " + selected_user.nick, null);
                } else if (items[item].equals("Kick…")) {
                    view = getDialogTextPrompt();
                    prompt = view.findViewById(R.id.prompt);
                    input = view.findViewById(R.id.textInput);
                    input.setText("");
                    prompt.setText("Give a reason for kicking");
                    builder.setTitle(server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")");
                    builder.setView(view);
                    builder.setPositiveButton("Kick", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            NetworkConnection.getInstance().kick(buffer.getCid(), buffer.getName(), selected_user.nick, input.getText().toString(), null);
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog = builder.create();
                    dialog.setOwnerActivity(MainActivity.this);
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    dialog.show();
                } else if (items[item].equals("Ban…")) {
                    view = getDialogTextPrompt();
                    prompt = view.findViewById(R.id.prompt);
                    input = view.findViewById(R.id.textInput);
                    if(selected_user.hostmask != null && selected_user.hostmask.length() > 0)
                        input.setText("*!" + selected_user.hostmask);
                    else
                        input.setText(selected_user.nick + "!*");
                    prompt.setText("Add a banmask for " + selected_user.nick);
                    builder.setTitle(server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")");
                    builder.setView(view);
                    builder.setPositiveButton("Ban", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            NetworkConnection.getInstance().mode(buffer.getCid(), buffer.getName(), "+b " + input.getText().toString(), null);
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    dialog = builder.create();
                    dialog.setOwnerActivity(MainActivity.this);
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                    dialog.show();
                }
                dialogInterface.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setOwnerActivity(this);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                MessageViewFragment mvf = (MessageViewFragment) getSupportFragmentManager().findFragmentById(R.id.messageViewFragment);
                if (mvf != null)
                    mvf.longPressOverride = false;
            }
        });
        dialog.show();
    }

    private boolean shouldMarkAsRead() {
        try {
            if (buffer != null && conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null) {
                if(conn.getUserInfo().prefs.has(buffer.getType().equals("channel")?"channel-enableReadOnSelect":"buffer-enableReadOnSelect")) {
                    JSONObject markAsReadMap = conn.getUserInfo().prefs.getJSONObject(buffer.getType().equals("channel") ? "channel-enableReadOnSelect" : "buffer-enableReadOnSelect");
                    if (markAsReadMap.has(String.valueOf(buffer.getBid())) && markAsReadMap.getBoolean(String.valueOf(buffer.getBid()))) {
                        return true;
                    }
                }
                if(conn.getUserInfo().prefs.has(buffer.getType().equals("channel")?"channel-disableReadOnSelect":"buffer-disableReadOnSelect")) {
                    JSONObject markAsReadMap = conn.getUserInfo().prefs.getJSONObject(buffer.getType().equals("channel") ? "channel-disableReadOnSelect" : "buffer-disableReadOnSelect");
                    if (markAsReadMap.has(String.valueOf(buffer.getBid())) && markAsReadMap.getBoolean(String.valueOf(buffer.getBid()))) {
                        return false;
                    }
                }
                return (conn.getUserInfo().prefs.has("enableReadOnSelect") && conn.getUserInfo().prefs.get("enableReadOnSelect") instanceof Boolean && conn.getUserInfo().prefs.getBoolean("enableReadOnSelect"));
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            NetworkConnection.printStackTraceToCrashlytics(e);
        }
        return false;
    }

    @Override
    public void onBufferSelected(int bid) {
        boolean changed = (buffer == null || buffer.getBid() != bid);

        launchBid = -1;
        launchURI = null;
        cidToOpen = -1;
        bufferToOpen = null;
        setIntent(new Intent(this, MainActivity.class));

        if (suggestionsTimerTask != null)
            suggestionsTimerTask.cancel();
        sortedChannels = null;
        sortedUsers = null;

        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
        if (bid != -1 && conn != null && conn.getUserInfo() != null) {
            conn.getUserInfo().last_selected_bid = bid;
        }
        synchronized (backStack) {
            for (int i = 0; i < backStack.size(); i++) {
                if (buffer != null && backStack.get(i) == buffer.getBid())
                    backStack.remove(i);
            }
        }
        if (buffer != null && buffer.getBid() >= 0 && bid != buffer.getBid()) {
            synchronized (backStack) {
                backStack.add(0, buffer.getBid());
            }
            buffer.setDraft(messageTxt.getText());

            if(shouldMarkAsRead()) {
                Long eid = EventsList.getInstance().lastEidForBuffer(buffer.getBid());
                if (eid >= buffer.getLast_seen_eid() && conn != null && conn.getState() == NetworkConnection.STATE_CONNECTED) {
                    NetworkConnection.getInstance().heartbeat(buffer.getCid(), buffer.getBid(), eid, null);
                    buffer.setLast_seen_eid(eid);
                    buffer.setUnread(0);
                    buffer.setHighlights(0);
                }
            }
        }
        shouldFadeIn = buffer != null && changed && !(buffer != null && buffer.getBid() == bid);
        buffer = BuffersList.getInstance().getBuffer(bid);
        if (buffer != null) {
            Crashlytics.log(Log.DEBUG, "IRCCloud", "Buffer selected: cid" + buffer.getCid() + " bid" + bid + " shouldFadeIn: " + shouldFadeIn);
            server = buffer.getServer();

            try {
                TreeMap<Long, Event> events = EventsList.getInstance().getEventsForBuffer(buffer.getBid());
                if (events != null) {
                    events = (TreeMap<Long, Event>) events.clone();
                    for (Event e : events.values()) {
                        if (e != null && e.highlight && e.from != null) {
                            User u = UsersList.getInstance().getUser(buffer.getBid(), e.from);
                            if (u != null && u.last_mention < e.eid)
                                u.last_mention = e.eid;
                        }
                    }
                }
            } catch (Exception e) {
                Crashlytics.logException(e);
            }

            try {
                if (Build.VERSION.SDK_INT >= 16 && buffer != null && server != null) {
                    NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
                    if (nfc != null) {
                        String uri = "irc";
                        if (server.getSsl() > 0)
                            uri += "s";
                        uri += "://" + server.getHostname() + ":" + server.getPort();
                        uri += "/" + URLEncoder.encode(buffer.getName(), "UTF-8");
                        if (buffer.isChannel()) {
                            Channel c = ChannelsList.getInstance().getChannelForBuffer(buffer.getBid());
                            if (c != null && c.hasMode("k"))
                                uri += "," + c.paramForMode("k");
                        }
                        nfc.setNdefPushMessage(new NdefMessage(NdefRecord.createUri(uri)), this);
                    }
                }
            } catch (Exception e) {
            }
        } else {
            Crashlytics.log(Log.DEBUG, "IRCCloud", "Buffer selected but not found: bid" + bid + " shouldFadeIn: " + shouldFadeIn);
            server = null;
        }
        update_subtitle();
        final Bundle b = new Bundle();
        if (buffer != null)
            b.putInt("cid", buffer.getCid());
        b.putInt("bid", bid);
        b.putBoolean("fade", shouldFadeIn);
        BuffersListFragment blf = (BuffersListFragment) getSupportFragmentManager().findFragmentById(R.id.BuffersList);
        BuffersListFragment blf2 = (BuffersListFragment) getSupportFragmentManager().findFragmentById(R.id.BuffersListDocked);
        final MessageViewFragment mvf = (MessageViewFragment) getSupportFragmentManager().findFragmentById(R.id.messageViewFragment);
        UsersListFragment ulf = (UsersListFragment) getSupportFragmentManager().findFragmentById(R.id.usersListFragment);
        UsersListFragment ulf2 = (UsersListFragment) getSupportFragmentManager().findFragmentById(R.id.usersListFragmentDocked);
        if (mvf != null)
            mvf.ready = false;
        if (blf != null)
            blf.setSelectedBid(bid);
        if (blf2 != null)
            blf2.setSelectedBid(bid);
        if (ulf != null)
            ulf.setArguments(b);
        if (ulf2 != null)
            ulf2.setArguments(b);

        if (shouldFadeIn) {
            ImageList.getInstance().prune();
            Crashlytics.log(Log.DEBUG, "IRCCloud", "Fade Out");
            if (Build.VERSION.SDK_INT < 16) {
                AlphaAnimation anim = new AlphaAnimation(1, 0);
                anim.setDuration(150);
                anim.setFillAfter(true);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (mvf != null)
                            mvf.setArguments(b);
                        messageTxt.setTextWithEmoji("");
                        if (buffer != null && buffer.getDraft() != null)
                            messageTxt.append(buffer.getDraft());
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                try {
                    if(mvf != null) {
                        mvf.getListView().startAnimation(anim);
                        mvf.avatar.startAnimation(anim);
                    }

                    if(ulf != null)
                        ulf.getRecyclerView().startAnimation(anim);
                } catch (Exception e) {

                }
            } else {
                if(mvf != null) {
                    mvf.avatar.animate().alpha(0);
                    mvf.getListView().animate().alpha(0).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            mvf.setArguments(b);
                            messageTxt.setTextWithEmoji("");
                            if (buffer != null && buffer.getDraft() != null)
                                messageTxt.append(buffer.getDraft());
                        }
                    });
                }
                if(ulf != null)
                    ulf.getRecyclerView().animate().alpha(0);
            }
            if(mvf != null)
                mvf.showSpinner(true);
        } else {
            if (mvf != null)
                mvf.setArguments(b);
            messageTxt.setTextWithEmoji("");
            if (buffer != null && buffer.getDraft() != null)
                messageTxt.append(buffer.getDraft());
        }

        updateUsersListFragmentVisibility();
        supportInvalidateOptionsMenu();
        if (excludeBIDTask != null)
            excludeBIDTask.cancel(true);
        excludeBIDTask = new ExcludeBIDTask();
        excludeBIDTask.execute(bid);
        if (drawerLayout != null)
            new RefreshUpIndicatorTask().execute((Void) null);
        if (buffer != null && buffer.getCid() != -1) {
            if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && getResources().getBoolean(R.bool.isTablet) && PreferenceManager.getDefaultSharedPreferences(this).getBoolean("tabletMode", true) && !isMultiWindow()) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.LEFT);
                if(actionBar != null)
                    actionBar.setHomeButtonEnabled(true);
            }
        }
        update_suggestions(false);
        messageTxt.setEnabled(buffer != null);
    }

    @Override
    public void onMessageViewReady() {
        if (shouldFadeIn) {
            Crashlytics.log(Log.DEBUG, "IRCCloud", "Fade In");
            MessageViewFragment mvf = (MessageViewFragment) getSupportFragmentManager().findFragmentById(R.id.messageViewFragment);
            UsersListFragment ulf = (UsersListFragment) getSupportFragmentManager().findFragmentById(R.id.usersListFragment);

            if (Build.VERSION.SDK_INT < 16) {
                AlphaAnimation anim = new AlphaAnimation(0, 1);
                anim.setDuration(150);
                anim.setFillAfter(true);
                if (mvf != null && mvf.getListView() != null)
                    mvf.getListView().startAnimation(anim);
                if (mvf != null && mvf.avatar != null)
                    mvf.avatar.startAnimation(anim);
                if (ulf != null && ulf.getRecyclerView() != null)
                    ulf.getRecyclerView().startAnimation(anim);
            } else {
                if (mvf != null && mvf.getListView() != null)
                    mvf.getListView().animate().alpha(1);
                if (mvf != null && mvf.avatar != null)
                    mvf.avatar.animate().alpha(1);
                if (ulf != null && ulf.getRecyclerView() != null)
                    ulf.getRecyclerView().animate().alpha(1);
            }
            if (mvf != null && mvf.getListView() != null) {
                if (mvf.buffer != buffer && buffer != null && BuffersList.getInstance().getBuffer(buffer.getBid()) != null) {
                    Bundle b = new Bundle();
                    b.putInt("cid", buffer.getCid());
                    b.putInt("bid", buffer.getBid());
                    b.putBoolean("fade", false);
                    mvf.setArguments(b);
                }
                mvf.showSpinner(false);
            }
            shouldFadeIn = false;
        }
    }

    @Override
    public void addButtonPressed(int cid) {
        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
    }

    @Override
    public void addNetwork() {
        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
        if (getWindowManager().getDefaultDisplay().getWidth() < TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 800, getResources().getDisplayMetrics())) {
            Intent i = new Intent(this, EditConnectionActivity.class);
            startActivity(i);
        } else {
            EditConnectionFragment connFragment = new EditConnectionFragment();
            connFragment.show(getSupportFragmentManager(), "addnetwork");
        }
    }

    @Override
    public void reorder() {
        if (drawerLayout != null) {
            drawerLayout.closeDrawers();
        }
        if (!getResources().getBoolean(R.bool.isTablet)) {
            Intent i = new Intent(this, ServerReorderActivity.class);
            startActivity(i);
        } else {
            try {
                ServerReorderFragment fragment = new ServerReorderFragment();
                fragment.show(getSupportFragmentManager(), "reorder");
            } catch (IllegalStateException e) {
                Intent i = new Intent(this, ServerReorderActivity.class);
                startActivity(i);
            }
        }
    }

    private int getOrientation(Context context, Uri photoUri) {
        int orientation = -1;
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            orientation = cursor.getInt(0);
            cursor.close();
        }

        return orientation;
    }

    private Bitmap loadThumbnail(Context context, Uri photoUri) throws IOException {
        InputStream is = context.getContentResolver().openInputStream(photoUri);
        BitmapFactory.Options dbo = new BitmapFactory.Options();
        dbo.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, dbo);
        is.close();

        int rotatedWidth, rotatedHeight;
        int orientation = getOrientation(context, photoUri);

        if (orientation == 90 || orientation == 270) {
            rotatedWidth = dbo.outHeight;
            rotatedHeight = dbo.outWidth;
        } else {
            rotatedWidth = dbo.outWidth;
            rotatedHeight = dbo.outHeight;
        }

        Bitmap srcBitmap;
        is = context.getContentResolver().openInputStream(photoUri);
        if (rotatedWidth > 1024 || rotatedHeight > 1024) {
            float widthRatio = ((float) rotatedWidth) / ((float) 1024);
            float heightRatio = ((float) rotatedHeight) / ((float) 1024);
            float maxRatio = Math.max(widthRatio, heightRatio);

            // Create the bitmap from file
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = (int) maxRatio;
            srcBitmap = BitmapFactory.decodeStream(is, null, options);
        } else {
            srcBitmap = BitmapFactory.decodeStream(is);
        }
        is.close();

    /*
     * if the orientation is not 0 (or -1, which means we don't know), we
     * have to do a rotation.
     */
        if (orientation > 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);

            srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(),
                    srcBitmap.getHeight(), matrix, true);
        }

        return srcBitmap;
    }

    private Uri resize(Uri in) {
        Uri out = null;
        try {
            int MAX_IMAGE_SIZE = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("photo_size", "1024"));

            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(IRCCloudApplication.getInstance().getApplicationContext().getContentResolver().openInputStream(in), null, o);
            int scale = 1;

            if (o.outWidth < MAX_IMAGE_SIZE && o.outHeight < MAX_IMAGE_SIZE)
                return in;

            if (o.outWidth > o.outHeight) {
                if (o.outWidth > MAX_IMAGE_SIZE)
                    scale = o.outWidth / MAX_IMAGE_SIZE;
            } else {
                if (o.outHeight > MAX_IMAGE_SIZE)
                    scale = o.outHeight / MAX_IMAGE_SIZE;
            }

            o = new BitmapFactory.Options();
            o.inSampleSize = scale;
            Bitmap bmp = BitmapFactory.decodeStream(IRCCloudApplication.getInstance().getApplicationContext().getContentResolver().openInputStream(in), null, o);

            //ExifInterface can only work on local files, so make a temporary copy on the SD card
            out = Uri.fromFile(File.createTempFile("irccloudcapture-original", ".jpg", getCacheDir()));
            InputStream is = IRCCloudApplication.getInstance().getApplicationContext().getContentResolver().openInputStream(in);
            OutputStream os = IRCCloudApplication.getInstance().getApplicationContext().getContentResolver().openOutputStream(out);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            is.close();
            os.close();

            ExifInterface exif = new ExifInterface(out.getPath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            new File(out.getPath()).delete();

            out = Uri.fromFile(File.createTempFile("irccloudcapture-resized", ".jpg", getCacheDir()));
            if (orientation > 1) {
                Matrix matrix = new Matrix();
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.postRotate(90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.postRotate(180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.postRotate(270);
                        break;
                }
                try {
                    bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                } catch (OutOfMemoryError e) {
                    Log.e("IRCCloud", "Out of memory rotating the photo, it may look wrong on imgur");
                }
            }

            if (bmp == null || !bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, IRCCloudApplication.getInstance().getApplicationContext().getContentResolver().openOutputStream(out))) {
                out = null;
            }
            if (bmp != null)
                bmp.recycle();
        } catch (IOException e) {
            NetworkConnection.printStackTraceToCrashlytics(e);
        } catch (Exception e) {
            Crashlytics.logException(e);
        } catch (OutOfMemoryError e) {
            Log.e("IRCCloud", "Out of memory rotating the photo, it may look wrong on imgur");
        }
        if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("keep_photos", false) && in.toString().contains("irccloudcapture")) {
            try {
                new File(in.getPath()).delete();
            } catch (Exception e) {
            }
        }
        if (out != null) {
            if(in.toString().contains(getCacheDir().getAbsolutePath())) {
                Log.i("IRCCloud", "Removing temporary file: " + in);
                new File(in.getPath()).delete();
            }
            return out;
        } else {
            return in;
        }
    }

    public class ImgurRefreshTask extends AsyncTaskEx<Void, Void, JSONObject> {
        private final String REFRESH_URL = "https://api.imgur.com/oauth2/token";
        private Uri mImageUri;  // local Uri to upload

        public ImgurRefreshTask(Uri imageUri) {
            mImageUri = imageUri;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                if (getSharedPreferences("prefs", 0).contains("imgur_refresh_token")) {
                    JSONObject o = NetworkConnection.getInstance().fetchJSON(REFRESH_URL,
                            "client_id=" + BuildConfig.IMGUR_KEY
                                    + "&client_secret=" + BuildConfig.IMGUR_SECRET
                                    + "&grant_type=refresh_token"
                                    + "&refresh_token=" + getSharedPreferences("prefs", 0).getString("imgur_refresh_token", "")
                    );
                    return o;
                }
            } catch (IOException e) {
                NetworkConnection.printStackTraceToCrashlytics(e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject o) {
            try {
                if (getSharedPreferences("prefs", 0).contains("imgur_refresh_token")) {
                    if (o == null || (o.has("success") && !o.getBoolean("success"))) {
                        startActivity(new Intent(MainActivity.this, ImgurAuthActivity.class));
                    } else {
                        SharedPreferences.Editor prefs = getSharedPreferences("prefs", 0).edit();
                        Iterator<String> i = o.keys();
                        while (i.hasNext()) {
                            String k = i.next();
                            prefs.putString("imgur_" + k, o.getString(k));
                        }
                        prefs.commit();
                        if (mImageUri != null) {
                            imgurTask = new ImgurUploadTask(mImageUri);
                            if(Build.VERSION.SDK_INT >= 16 && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        REQUEST_EXTERNAL_MEDIA_IMGUR);
                            } else {
                                imgurTask.execute((Void) null);
                            }
                        }
                    }
                } else {
                    if (mImageUri != null) {
                        imgurTask = new ImgurUploadTask(mImageUri);
                        if(Build.VERSION.SDK_INT >= 16 && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    REQUEST_EXTERNAL_MEDIA_IMGUR);
                        } else {
                            imgurTask.execute((Void) null);
                        }
                    }
                }
            } catch (JSONException e) {
                NetworkConnection.printStackTraceToCrashlytics(e);
            }
        }
    }

    public class ImgurUploadTask extends AsyncTaskEx<Void, Float, String> {
        private final String UPLOAD_URL = (BuildConfig.MASHAPE_KEY.length() > 0) ? "https://imgur-apiv3.p.mashape.com/3/image" : "https://api.imgur.com/3/image";
        private Uri mImageUri;  // local Uri to upload
        private int total = 0;
        public Activity activity;
        private String error;
        private Buffer mBuffer;

        public ImgurUploadTask(Uri imageUri) {
            Crashlytics.log(Log.INFO, "IRCCloud", "Uploading image to " + UPLOAD_URL);
            mImageUri = imageUri;
            mBuffer = buffer;
            setActivity(MainActivity.this);
        }

        @Override
        protected String doInBackground(Void... params) {
            InputStream imageIn;
            try {
                while (activity == null)
                    Thread.sleep(100);
                String type = activity.getContentResolver().getType(mImageUri);
                if ((type != null && !type.equals("image/gif")) || Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(activity).getString("photo_size", "1024")) > 0) {
                    mImageUri = resize(mImageUri);
                }
                imageIn = activity.getContentResolver().openInputStream(mImageUri);
                total = imageIn.available();
            } catch (Exception e) {
                Crashlytics.log(Log.ERROR, "IRCCloud", "could not open InputStream: " + e);
                return null;
            }

            HttpURLConnection conn = null;
            InputStream responseIn = null;

            try {
                conn = (HttpURLConnection) new URL(UPLOAD_URL).openConnection();
                conn.setReadTimeout(60000);
                conn.setConnectTimeout(60000);
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(total);
                if (BuildConfig.MASHAPE_KEY.length() > 0)
                    conn.setRequestProperty("X-Mashape-Authorization", BuildConfig.MASHAPE_KEY);
                if (getSharedPreferences("prefs", 0).contains("imgur_access_token")) {
                    conn.setRequestProperty("Authorization", "Bearer " + getSharedPreferences("prefs", 0).getString("imgur_access_token", ""));
                } else {
                    conn.setRequestProperty("Authorization", "Client-ID " + BuildConfig.IMGUR_KEY);
                }

                OutputStream out = conn.getOutputStream();
                copy(imageIn, out);
                out.flush();
                out.close();
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    responseIn = conn.getInputStream();
                    return onInput(responseIn);
                } else {
                    Crashlytics.log(Log.INFO, "IRCCloud", "responseCode=" + conn.getResponseCode());
                    responseIn = conn.getErrorStream();
                    StringBuilder sb = new StringBuilder();
                    Scanner scanner = new Scanner(responseIn).useDelimiter("\\A");
                    while (scanner.hasNext()) {
                        sb.append(scanner.next());
                    }
                    JSONObject root = new JSONObject(sb.toString());
                    if (root.has("data") && root.getJSONObject("data").has("error"))
                        error = root.getJSONObject("data").getString("error");
                    else
                        error = null;
                    Crashlytics.log(Log.ERROR, "IRCCloud", "error response: " + sb.toString());
                    return null;
                }
            } catch (Exception ex) {
                Crashlytics.log(Log.ERROR, "IRCCloud", "Error during POST: " + ex);
                return null;
            } finally {
                try {
                    responseIn.close();
                } catch (Exception ignore) {
                }
                try {
                    conn.disconnect();
                } catch (Exception ignore) {
                }
                try {
                    imageIn.close();
                } catch (Exception ignore) {
                }
                if(mImageUri.toString().contains(getCacheDir().getAbsolutePath())) {
                    Log.i("IRCCloud", "Removing temporary file: " + mImageUri);
                    new File(mImageUri.getPath()).delete();
                }
            }
        }

        public void setActivity(Activity a) {
            activity = a;
            if (a != null) {
                if (total > 0) {
                    actionBar.setTitle("Uploading");
                    actionBar.setSubtitle(null);
                    actionBar.setDisplayShowCustomEnabled(false);
                    actionBar.setDisplayShowTitleEnabled(true);
                    progressBar.setProgress(0);
                    progressBar.setIndeterminate(true);
                    if (progressBar.getVisibility() != View.VISIBLE) {
                        if (Build.VERSION.SDK_INT >= 16) {
                            progressBar.setAlpha(0);
                            progressBar.animate().alpha(1).setDuration(200);
                        }
                        progressBar.setVisibility(View.VISIBLE);
                    }
                }
            }
        }

        @Override
        protected void onProgressUpdate(Float... values) {
            if (activity != null) {
                try {
                    if (progressBar.getVisibility() != View.VISIBLE) {
                        actionBar.setTitle("Uploading");
                        actionBar.setSubtitle(null);
                        actionBar.setDisplayShowCustomEnabled(false);
                        actionBar.setDisplayShowTitleEnabled(true);
                        if (Build.VERSION.SDK_INT >= 16) {
                            progressBar.setAlpha(0);
                            progressBar.animate().alpha(1).setDuration(200);
                        }
                        progressBar.setVisibility(View.VISIBLE);
                    }
                    if (values[0] < 1.0f) {
                        progressBar.setIndeterminate(false);
                        progressBar.setProgress((int) (values[0] * 1000));
                    } else {
                        progressBar.setIndeterminate(true);
                    }
                } catch (Exception e) {
                }
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if (mImageUri != null && mImageUri.toString().contains("irccloudcapture") && s != null && s.length() > 0) {
                if (!PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("keep_photos", false) || mImageUri.toString().contains("irccloudcapture-resized")) {
                    try {
                        new File(new URI(mImageUri.toString())).delete();
                    } catch (Exception e) {
                    }
                }
            }
            if (activity != null) {
                if (progressBar.getVisibility() == View.VISIBLE) {
                    if (Build.VERSION.SDK_INT >= 16) {
                        progressBar.animate().alpha(0).setDuration(200).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setVisibility(View.GONE);
                            }
                        });
                    } else {
                        progressBar.setVisibility(View.GONE);
                    }
                }
                if(actionBar != null) {
                    actionBar.setDisplayShowCustomEnabled(true);
                    actionBar.setDisplayShowTitleEnabled(false);
                }
            }
            setText(s);
        }

        private void setText(final String s) {
            //If the user rotated the screen, we might not be attached to an activity yet.  Keep trying until we reattach
            if (s == null) {
                try {
                    if (error != null) {
                        JSONObject root = new JSONObject(error);
                        if (root.has("status") && root.getInt("status") == 403) {
                            new ImgurRefreshTask(mImageUri).execute((Void) null);
                            return;
                        }
                    }
                } catch (JSONException e) {
                }
            }
            if (activity != null) {
                Crashlytics.log(Log.INFO, "IRCCloud", "Upload finished");
                if (s != null) {
                    if (mBuffer != null) {
                        if (mBuffer.getDraft() == null)
                            mBuffer.setDraft("");
                        if (mBuffer.getDraft().length() > 0 && !mBuffer.getDraft().toString().endsWith(" "))
                            mBuffer.setDraft(mBuffer.getDraft() + " ");
                        mBuffer.setDraft(mBuffer.getDraft() + s);
                    }
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ActionEditText messageTxt = activity.findViewById(R.id.messageTxt);
                            String txt = messageTxt.getText().toString();
                            if (txt.length() > 0 && !txt.endsWith(" "))
                                txt += " ";
                            txt += s.replace("http://", "https://");
                            messageTxt.setTextWithEmoji(txt);
                        }
                    });
                } else {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (activity != null) {
                                if (Looper.myLooper() == null)
                                    Looper.prepare();
                                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                                builder.setTitle("Upload Failed");
                                builder.setMessage("Unable to upload photo to imgur.  Please try again." + ((error != null) ? ("\n\n" + error) : ""));
                                builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                                AlertDialog dialog = builder.create();
                                dialog.setOwnerActivity(activity);
                                if(!activity.isFinishing())
                                    dialog.show();
                            }
                        }
                    });
                }
                imgurTask = null;
            } else if (mBuffer != null && s != null) {
                Crashlytics.log(Log.INFO, "IRCCloud", "Upload finished, updating draft");
                if (mBuffer.getDraft() == null)
                    mBuffer.setDraft("");
                if (mBuffer.getDraft().length() > 0 && !mBuffer.getDraft().toString().endsWith(" "))
                    mBuffer.setDraft(mBuffer.getDraft() + " ");
                mBuffer.setDraft(mBuffer.getDraft() + s);
            } else {
                if(suggestionsTimer != null)
                    suggestionsTimer.schedule(new TimerTask() {

                    @Override
                    public void run() {
                        setText(s);
                    }
                }, 500);
            }
        }

        private int copy(InputStream input, OutputStream output) throws IOException {
            byte[] buffer = new byte[8192];
            int count = 0;
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
                publishProgress((float) count / (float) total);
            }
            return count;
        }

        protected String onInput(InputStream in) throws Exception {
            StringBuilder sb = new StringBuilder();
            Scanner scanner = new Scanner(in).useDelimiter("\\A");
            while (scanner.hasNext()) {
                sb.append(scanner.next());
            }

            JSONObject root = new JSONObject(sb.toString());
            if (root.has("data") && root.getJSONObject("data").has("error"))
                error = root.getJSONObject("data").getString("error");
            else
                error = null;
            total = 0;
            return root.getJSONObject("data").getString("link");
        }
    }

    public static class FileUploadTask extends AsyncTaskEx<Void, Float, String> {
        private HttpURLConnection http = null;
        private Uri mFileUri;  // local Uri to upload
        private int total = 0;
        private TextView fileSize;
        public AlertDialog metadataDialog;
        public MainActivity activity;
        public Buffer mBuffer;
        private String error;

        public String filename;
        public String original_filename;
        public String type;
        public String file_id;
        public String message;
        public boolean uploadFinished = false;
        public boolean filenameSet = false;

        private NotificationCompat.Builder notification;

        private BroadcastReceiver cancelListener = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                cancel(true);
                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(mBuffer.getBid());
                if(activity != null) {
                    activity.fileUploadTask = null;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            hide_progress();
                        }
                    });
                }
            }
        };

        public FileUploadTask(Uri fileUri, final MainActivity activity) {
            mBuffer = activity.buffer;
            mFileUri = fileUri;
            type = activity.getContentResolver().getType(mFileUri);

            if (Build.VERSION.SDK_INT < 16) {
                original_filename = fileUri.getLastPathSegment();
            } else {
                Cursor cursor = null;
                try {
                    cursor = activity.getContentResolver().query(fileUri, null, null, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        original_filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    } else {
                        original_filename = fileUri.getLastPathSegment();
                    }
                } catch (Exception e) {
                    original_filename = String.valueOf(System.currentTimeMillis());
                }
                if(cursor != null)
                    cursor.close();
            }

            if(original_filename == null || original_filename.length() == 0)
                original_filename = "file";

            if (type == null) {
                String lower = original_filename.toLowerCase();
                if (lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
                    type = "image/jpeg";
                else if (lower.endsWith(".png"))
                    type = "image/png";
                else if (lower.endsWith(".gif"))
                    type = "image/gif";
                else if (lower.endsWith(".webp"))
                    type = "image/webp";
                else if (lower.endsWith(".bmp"))
                    type = "image/bmp";
                else if (lower.endsWith(".mp4"))
                    type = "video/mp4";
                else if (lower.endsWith(".3gpp"))
                    type = "video/3gpp";
                else
                    type = "application/octet-stream";
            }

            if (!original_filename.contains("."))
                original_filename += "." + type.substring(type.indexOf("/") + 1);

            setActivity(activity);

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(activity.getPackageName() + ".cancel_upload");
            IRCCloudApplication.getInstance().getApplicationContext().registerReceiver(cancelListener, intentFilter);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel c = new NotificationChannel("upload_progress", "Upload Progress", NotificationManagerCompat.IMPORTANCE_LOW);
                c.setSound(null, null);
                c.setShowBadge(false);
                ((NotificationManager)IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(c);
            }

            notification = new NotificationCompat.Builder(IRCCloudApplication.getInstance().getApplicationContext(), "upload_progress")
                    .setContentTitle("Uploading File")
                    .setContentText("Calculating size… • " + type)
                    .setProgress(0, 0, true)
                    .setLocalOnly(true)
                    .setOngoing(true)
                    .setColor(IRCCloudApplication.getInstance().getApplicationContext().getResources().getColor(R.color.ic_background))
                    .addAction(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP?R.drawable.ic_action_cancel_upload:R.drawable.ic_action_cancel, "Cancel", PendingIntent.getBroadcast(activity, 0, new Intent(activity.getPackageName() + ".cancel_upload"), PendingIntent.FLAG_UPDATE_CURRENT))
                    .setSmallIcon(android.R.drawable.stat_sys_upload);

            Intent i = new Intent();
            i.setComponent(new ComponentName(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), "com.irccloud.android.MainActivity"));
            i.putExtra("bid", mBuffer.getBid());
            i.setData(Uri.parse("bid://" + mBuffer.getBid()));
            notification.setContentIntent(PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT));

            if(ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
                show_dialog();

            Crashlytics.log(Log.INFO, "IRCCloud", "Uploading file to IRCCloud: " + original_filename + " " + type);
        }

        public void show_dialog() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    final View view = activity.getLayoutInflater().inflate(R.layout.dialog_upload, null);
                    final EditText fileinput = view.findViewById(R.id.filename);
                    final EditText messageinput = view.findViewById(R.id.message);
                    final ImageView thumbnail = view.findViewById(R.id.thumbnail);
                    messageinput.setText(activity.buffer.getDraft());
                    activity.buffer.setDraft("");
                    activity.messageTxt.setTextWithEmoji("");

                    view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (fileinput.hasFocus() || messageinput.hasFocus()) {
                                view.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        view.scrollTo(0, view.getBottom());
                                    }
                                });
                            }
                        }
                    });

                    if (type.startsWith("image/")) {
                        try {
                            thumbnail.setImageBitmap(activity.loadThumbnail(IRCCloudApplication.getInstance().getApplicationContext(), mFileUri));
                            thumbnail.setVisibility(View.VISIBLE);
                        } catch (OutOfMemoryError e) {
                            thumbnail.setVisibility(View.GONE);
                        } catch (Exception e) {
                            NetworkConnection.printStackTraceToCrashlytics(e);
                        }
                    } else {
                        thumbnail.setVisibility(View.GONE);
                    }

                    fileSize = view.findViewById(R.id.filesize);
                    String filesize;
                    if (total == 0) {
                        fileSize.setText("Calculating size… • " + type);
                    } else if (total < 1024) {
                        filesize = total + " B";
                        fileSize.setText(filesize + " • " + type);
                    } else {
                        int exp = (int) (Math.log(total) / Math.log(1024));
                        filesize = String.format("%.1f ", total / Math.pow(1024, exp)) + ("KMGTPE".charAt(exp - 1)) + "B";
                        fileSize.setText(filesize + " • " + type);
                    }

                    fileinput.setText(original_filename);
                    fileinput.setOnEditorActionListener(new OnEditorActionListener() {
                        public boolean onEditorAction(TextView exampleView, int actionId, KeyEvent event) {
                            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
                                try {
                                    filename = fileinput.getText().toString();
                                    filenameSet = true;
                                    finalize_upload();
                                } catch (Exception e) {
                                    // TODO Auto-generated catch block
                                    NetworkConnection.printStackTraceToCrashlytics(e);
                                }
                                ((AlertDialog) fileinput.getTag()).dismiss();
                            }
                            return true;
                        }
                    });
                    builder.setTitle("Upload A File To " + mBuffer.getName());
                    builder.setView(view);
                    builder.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                filename = fileinput.getText().toString();
                                message = messageinput.getText().toString();
                                filenameSet = true;
                                finalize_upload();
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                NetworkConnection.printStackTraceToCrashlytics(e);
                            }
                            dialog.dismiss();
                            metadataDialog = null;
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            metadataDialog = null;
                        }
                    });
                    builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            if (activity != null) {
                                if (activity.fileUploadTask != null)
                                    activity.fileUploadTask.cancel(true);
                                activity.fileUploadTask = null;
                                hide_progress();
                                if (activity.buffer != null)
                                    activity.buffer.setDraft(messageinput.getText());
                                if (activity.messageTxt != null)
                                    activity.messageTxt.setTextWithEmoji(messageinput.getText());
                            }
                            dialog.dismiss();
                            metadataDialog = null;
                        }
                    });
                    metadataDialog = builder.create();
                    fileinput.setTag(metadataDialog);
                    metadataDialog.setOwnerActivity(activity);
                    metadataDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                    if(!activity.isFinishing())
                        metadataDialog.show();
                }
            });
        }

        private void finalize_upload() {
            if (uploadFinished && filenameSet && !isCancelled()) {
                if (file_id != null && file_id.length() > 0) {
                    NetworkConnection.getInstance().finalize_upload(file_id, filename, original_filename, new NetworkConnection.IRCResultCallback() {
                        @Override
                        public void onIRCResult(final IRCCloudJSONObject result) {
                            if(result.getBoolean("success")) {
                                if (result.getJsonObject("file") != null && result.getJsonObject("file").has("url")) {
                                    if (message == null || message.length() == 0)
                                        message = "";
                                    else
                                        message += " ";
                                    message += result.getJsonObject("file").get("url").asText();
                                    NetworkConnection.getInstance().say(mBuffer.getCid(), mBuffer.getName(), message, null);
                                    NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(mBuffer.getBid());
                                    if(activity != null) {
                                        activity.fileUploadTask = null;
                                        activity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                hide_progress();
                                            }
                                        });
                                    }
                                }
                            } else {
                                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(mBuffer.getBid());
                                if(activity != null) {
                                    activity.fileUploadTask = null;
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            hide_progress();
                                            show_alert("Upload Failed", "Unable to upload file to IRCCloud: " + result.getString("message"));
                                        }
                                    });
                                }
                            }
                        }
                    });
                } else {
                    if (activity != null)
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                show_alert("Upload Failed", (error != null) ? error : "Unable to upload file to IRCCloud.");
                            }
                        });
                }
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            InputStream fileIn;
            try {
                while (activity == null)
                    Thread.sleep(100);
                if (type != null && type.startsWith("image/") && !type.equals("image/gif") && !type.equals("image/png") && Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(activity).getString("photo_size", "1024")) > 0) {
                    mFileUri = activity.resize(mFileUri);
                }
                fileIn = activity.getContentResolver().openInputStream(mFileUri);
                Cursor c = activity.getContentResolver().query(mFileUri, new String[]{OpenableColumns.SIZE}, null, null, null);
                if (c != null && c.moveToFirst()) {
                    total = c.getInt(0);
                } else {
                    total = fileIn.available();
                }
                if(c != null)
                    c.close();
            } catch (Exception e) {
                Crashlytics.log(Log.ERROR, "IRCCloud", "could not open InputStream: " + e);
                if(activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            show_alert("Upload Failed", "Unable to open input file stream");
                        }
                    });
                } else {
                    NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(mBuffer.getBid());
                    NotificationsList.getInstance().alert(mBuffer.getBid(), "Upload Failed", "Unable to upload file to IRCCloud.");
                }
                return null;
            }

            if (total > 15000000) {
                if(activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            show_alert("Upload Failed", "Sorry, you can’t upload files larger than 15 MB");
                        }
                    });
                } else {
                    NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(mBuffer.getBid());
                    NotificationsList.getInstance().alert(mBuffer.getBid(), "Upload Failed", "Sorry, you can’t upload files larger than 15 MB");
                }
                return null;
            }

            if(activity != null)
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String filesize;
                        if (total < 1024) {
                            filesize = total + " B";
                        } else {
                            int exp = (int) (Math.log(total) / Math.log(1024));
                            filesize = String.format("%.1f ", total / Math.pow(1024, exp)) + ("KMGTPE".charAt(exp - 1)) + "B";
                        }
                        if(fileSize != null)
                            fileSize.setText(filesize + " • " + type);
                        notification.setContentText(filesize + " • " + type);
                        NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify(mBuffer.getBid(), notification.build());
                    }
                });

            InputStream responseIn = null;

            try {
                String boundary = UUID.randomUUID().toString();
                http = (HttpURLConnection) new URL("https://" + NetworkConnection.IRCCLOUD_HOST + "/chat/upload").openConnection();
                http.setReadTimeout(60000);
                http.setConnectTimeout(60000);
                http.setDoOutput(true);
                http.setFixedLengthStreamingMode(total + (boundary.length() * 2) + original_filename.length() + type.length() + 88);
                http.setRequestProperty("User-Agent", NetworkConnection.getInstance().useragent);
                http.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                http.setRequestProperty("Cookie", "session=" + NetworkConnection.getInstance().session);
                http.setRequestProperty("x-irccloud-session", NetworkConnection.getInstance().session);

                OutputStream out = http.getOutputStream();
                out.write(("--" + boundary + "\r\n").getBytes());
                out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + original_filename + "\"\r\n").getBytes());
                out.write(("Content-Type: " + type + "\r\n\r\n").getBytes());
                copy(fileIn, out);

                if (!isCancelled()) {
                    out.write(("\r\n--" + boundary + "--\r\n").getBytes());
                    out.flush();
                    out.close();
                    if (http.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        responseIn = http.getInputStream();
                        return onInput(responseIn);
                    } else {
                        Crashlytics.log(Log.INFO, "IRCCloud", "responseCode=" + http.getResponseCode());
                        responseIn = http.getErrorStream();
                        StringBuilder sb = new StringBuilder();
                        Scanner scanner = new Scanner(responseIn).useDelimiter("\\A");
                        while (scanner.hasNext()) {
                            sb.append(scanner.next());
                        }
                        Crashlytics.log(Log.ERROR, "IRCCloud", "error response: " + sb.toString());
                        try {
                            JSONObject root = new JSONObject(sb.toString());
                            if (root.has("message")) {
                                error = root.getString("message");
                                switch(error) {
                                    case "upload_limit_reached":
                                        error = "Sorry, you can't upload more than 100 MB of files.  Delete some uploads and try again.";
                                        break;
                                    case "upload_already_exists":
                                        error = "You've already uploaded this file";
                                        break;
                                    case "banned_content":
                                        error = "Banned content";
                                        break;
                                }
                            }
                        } catch (JSONException e) {
                            error = sb.toString();
                        }
                    }
                } else {
                    Log.e("IRCCloud", "Upload cancelled");
                    NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(mBuffer.getBid());
                }
            } catch (IOException ex) {
                NetworkConnection.printStackTraceToCrashlytics(ex);
            } catch (Exception ex) {
                NetworkConnection.printStackTraceToCrashlytics(ex);
                Crashlytics.logException(ex);
                error = "An unexpected error occurred. Please try again later.";
            } finally {
                try {
                    if (responseIn != null)
                        responseIn.close();
                } catch (Exception ignore) {
                }
                try {
                    if (http != null)
                        http.disconnect();
                } catch (Exception ignore) {
                }
                try {
                    fileIn.close();
                } catch (Exception ignore) {
                }
                if(activity != null && mFileUri.toString().contains(IRCCloudApplication.getInstance().getApplicationContext().getCacheDir().getAbsolutePath())) {
                    Log.i("IRCCloud", "Removing temporary file: " + mFileUri);
                    new File(mFileUri.getPath()).delete();
                }
            }
            return null;
        }

        public void setActivity(MainActivity a) {
            activity = a;
            if (a != null) {
                if (total > 0 && !uploadFinished) {
                    activity.actionBar.setTitle("Uploading");
                    activity.actionBar.setSubtitle(null);
                    activity.actionBar.setDisplayShowCustomEnabled(false);
                    activity.actionBar.setDisplayShowTitleEnabled(true);
                    activity.progressBar.setProgress(0);
                    activity.progressBar.setIndeterminate(true);
                    if (activity.progressBar.getVisibility() != View.VISIBLE) {
                        if (Build.VERSION.SDK_INT >= 16) {
                            activity.progressBar.setAlpha(0);
                            activity.progressBar.animate().alpha(1).setDuration(200);
                        }
                        activity.progressBar.setVisibility(View.VISIBLE);
                    }
                }
            }
        }

        @Override
        protected void onProgressUpdate(Float... values) {
            if (values[0] < 1.0f)
                notification.setProgress(1000, (int) (values[0] * 1000), false);
            else
                notification.setProgress(0, 0, true);
            NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify(mBuffer.getBid(), notification.build());

            if (activity != null) {
                try {
                    if (activity.progressBar.getVisibility() != View.VISIBLE) {
                        activity.actionBar.setTitle("Uploading");
                        activity.actionBar.setSubtitle(null);
                        activity.actionBar.setDisplayShowCustomEnabled(false);
                        activity.actionBar.setDisplayShowTitleEnabled(true);
                        if (Build.VERSION.SDK_INT >= 16) {
                            activity.progressBar.setAlpha(0);
                            activity.progressBar.animate().alpha(1).setDuration(200);
                        }
                        activity.progressBar.setVisibility(View.VISIBLE);
                    }
                    if (values[0] < 1.0f) {
                        activity.progressBar.setIndeterminate(false);
                        activity.progressBar.setProgress((int) (values[0] * 1000));
                    } else {
                        activity.progressBar.setIndeterminate(true);
                    }
                } catch (Exception e) {
                }
            }
        }

        @Override
        protected void onPostExecute(String s) {
            if (mFileUri != null && mFileUri.toString().contains("irccloudcapture") && s != null && s.length() > 0) {
                if (!PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("keep_photos", false) || mFileUri.toString().contains("irccloudcapture-resized")) {
                    try {
                        new File(new URI(mFileUri.toString())).delete();
                    } catch (Exception e) {
                    }
                }
            }

            file_id = s;
            uploadFinished = true;
            finalize_upload();
            IRCCloudApplication.getInstance().getApplicationContext().unregisterReceiver(cancelListener);
            Log.e("IRCCloud", "FileUploadTask finished");
        }

        private int copy(InputStream input, OutputStream output) throws IOException {
            byte[] buffer = new byte[8192];
            int count = 0;
            int n = 0;
            while (-1 != (n = input.read(buffer)) && !isCancelled()) {
                output.write(buffer, 0, n);
                count += n;
                publishProgress((float) count / (float) total);
            }
            return count;
        }

        protected String onInput(InputStream in) throws Exception {
            StringBuilder sb = new StringBuilder();
            Scanner scanner = new Scanner(in).useDelimiter("\\A");
            while (scanner.hasNext()) {
                sb.append(scanner.next());
            }

            JSONObject root = new JSONObject(sb.toString());
            if (root.has("success") && root.getBoolean("success")) {
                return root.getString("id");
            } else {
                return null;
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if(activity != null)
                activity.fileUploadTask = null;
            hide_progress();
            if(metadataDialog != null)
                metadataDialog.cancel();
            NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(mBuffer.getBid());
        }

        private void hide_progress() {
            if (activity != null && activity.progressBar != null && activity.progressBar.getVisibility() == View.VISIBLE) {
                if (Build.VERSION.SDK_INT >= 16) {
                    activity.progressBar.animate().alpha(0).setDuration(200).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if(activity != null && activity.progressBar != null)
                                activity.progressBar.setVisibility(View.GONE);
                        }
                    });
                } else {
                    activity.progressBar.setVisibility(View.GONE);
                }
            }
            if(activity != null && activity.actionBar != null) {
                activity.actionBar.setDisplayShowCustomEnabled(true);
                activity.actionBar.setDisplayShowTitleEnabled(false);
            }
            NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(mBuffer.getBid());
        }

        private void show_alert(String title, String message) {
            NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(mBuffer.getBid());
            try {
                if (activity == null)
                    throw new IllegalStateException();

                if (Looper.myLooper() == null)
                    Looper.prepare();
                hide_progress();

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(title);
                builder.setMessage(message);
                builder.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            dialog.dismiss();
                        } catch (IllegalArgumentException e) {
                        }
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.setOwnerActivity(activity);
                if(!activity.isFinishing())
                    dialog.show();

                metadataDialog.dismiss();
            } catch (Exception e) {
                NotificationsList.getInstance().alert(mBuffer.getBid(), title, message);
            }
        }
    }

    public class ScreenReceiver extends BroadcastReceiver {
        public boolean wasScreenOn = true;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(messageTxt.getWindowToken(), 0);
                wasScreenOn = false;
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                wasScreenOn = true;
            }
        }
    }

    private ScreenReceiver screenReceiver = new ScreenReceiver();

    public class TimeZoneReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("IRCCloud", "Time zone changed, refreshing event timestamps");
            EventsList.getInstance().clearCaches();
            if(buffer != null)
                onBufferSelected(buffer.getBid());
        }
    }
    private TimeZoneReceiver timeZoneReceiver = new TimeZoneReceiver();

    private class GcmTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                Crashlytics.log(Log.INFO, "IRCCloud", "Registering for GCM");
                String GCM_ID = BuildConfig.GCM_ID;
                if(BuildConfig.ENTERPRISE && getSharedPreferences("prefs", 0).getString("host", BuildConfig.HOST).equals("api.irccloud.com"))
                    GCM_ID = BuildConfig.GCM_ID_IRCCLOUD;
                String token = InstanceID.getInstance(MainActivity.this).getToken(GCM_ID, GoogleCloudMessaging.INSTANCE_ID_SCOPE);
                SharedPreferences.Editor editor = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).edit();
                editor.putString("gcm_token", token);
                editor.commit();
                if(token != null && token.length() > 0) {
                    JSONObject result = NetworkConnection.getInstance().registerGCM(token, NetworkConnection.getInstance().session);
                    if (result != null && result.has("success")) {
                        return result.getBoolean("success");
                    }
                }
            } catch (Exception e) {
                NetworkConnection.printStackTraceToCrashlytics(e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result) {
                Crashlytics.log(Log.INFO, "IRCCloud", "Device successfully registered");
            } else {
                Crashlytics.log(Log.ERROR, "IRCCloud", "GCM registration failed, scheduling background task");
                BackgroundTaskService.registerGCM(MainActivity.this);
            }
        }
    }
}
