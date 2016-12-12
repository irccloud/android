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

package com.irccloud.android.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.databind.JsonNode;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.CollapsedEventsList;
import com.irccloud.android.ColorFormatter;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.FontAwesome;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.IRCCloudLinkMovementMethod;
import com.irccloud.android.Ignore;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.OffsetLinearLayout;
import com.irccloud.android.R;
import com.irccloud.android.activity.BaseActivity;
import com.irccloud.android.data.collection.AvatarsList;
import com.irccloud.android.data.model.Avatar;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.model.Event;
import com.irccloud.android.data.collection.EventsList;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.fragment.BuffersListFragment.OnBufferSelectedListener;
import com.squareup.leakcanary.RefWatcher;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

public class MessageViewFragment extends ListFragment implements NetworkConnection.IRCEventHandler {
    private NetworkConnection conn;
    private TextView statusView;
    private View headerViewContainer;
    private View headerView;
    private View footerViewContainer;
    private TextView backlogFailed;
    private Button loadBacklogButton;
    private TextView unreadTopLabel;
    private TextView unreadBottomLabel;
    private View unreadTopView;
    private View unreadBottomView;
    private TextView highlightsTopLabel;
    private TextView highlightsBottomLabel;
    public ImageView avatar;
    private AvatarsList mAvatarsList = AvatarsList.getInstance();
    private OffsetLinearLayout avatarContainer;
    public Buffer buffer;
    private Server server;
    private long earliest_eid;
    private long backlog_eid = 0;
    private boolean requestingBacklog = false;
    private float avgInsertTime = 0;
    private int newMsgs = 0;
    private long newMsgTime = 0;
    private int newHighlights = 0;
    private MessageViewListener mListener;
    private View awayView = null;
    private TextView awayTxt = null;
    private int timestamp_width = -1;
    private float textSize = 14.0f;
    private View globalMsgView = null;
    private TextView globalMsg = null;
    private ProgressBar spinner = null;
    private final Handler mHandler = new Handler();
    private ColorScheme colorScheme = ColorScheme.getInstance();
    private Typeface hackRegular;

    public static final int ROW_MESSAGE = 0;
    public static final int ROW_TIMESTAMP = 1;
    public static final int ROW_BACKLOGMARKER = 2;
    public static final int ROW_SOCKETCLOSED = 3;
    public static final int ROW_LASTSEENEID = 4;
    private static final String TYPE_TIMESTAMP = "__timestamp__";
    private static final String TYPE_BACKLOGMARKER = "__backlog__";
    private static final String TYPE_LASTSEENEID = "__lastseeneid__";

    private MessageAdapter adapter;

    private long currentCollapsedEid = -1;
    private long lastCollapsedEid = -1;
    private CollapsedEventsList collapsedEvents = new CollapsedEventsList();
    private int lastCollapsedDay = -1;
    private HashSet<Long> expandedSectionEids = new HashSet<Long>();
    private RefreshTask refreshTask = null;
    private HeartbeatTask heartbeatTask = null;
    private Ignore ignore = new Ignore();
    private static Timer tapTimer = null;
    private TimerTask tapTimerTask = null;
    public boolean longPressOverride = false;
    private LinkMovementMethodNoLongPress linkMovementMethodNoLongPress = new LinkMovementMethodNoLongPress();
    public boolean ready = false;
    private final Object adapterLock = new Object();

    public View suggestionsContainer = null;
    public GridView suggestions = null;

    private boolean pref_24hr = false;
    private boolean pref_seconds = false;
    private boolean pref_trackUnread = true;
    private boolean pref_timeLeft = false;
    private boolean pref_nickColors = false;
    private boolean pref_hideJoinPart = false;
    private boolean pref_expandJoinPart = false;
    private boolean pref_avatarsOff = false;
    private boolean pref_chatOneLine = false;
    private boolean pref_norealname = false;
    private boolean pref_compact = false;

    private class LinkMovementMethodNoLongPress extends IRCCloudLinkMovementMethod {
        @Override
        public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
            int action = event.getAction();

            if (!longPressOverride && (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN)) {
                return super.onTouchEvent(widget, buffer, event);
            }
            return false;
        }
    }

    private class MessageAdapter extends BaseAdapter {
        final ArrayList<Event> data;
        private ListFragment ctx;
        private long max_eid = 0;
        private long min_eid = 0;
        private int lastDay = -1;
        private int lastSeenEidMarkerPosition = -1;
        private int currentGroupPosition = -1;
        private TreeSet<Integer> unseenHighlightPositions;

        private class ViewHolder {
            int type;
            LinearLayout lastSeenEidWrapper;
            LinearLayout messageContainer;
            LinearLayout socketClosedBar;
            TextView timestamp;
            TextView timestamp_left;
            TextView timestamp_right;
            TextView message;
            TextView expandable;
            TextView nickname;
            TextView realname;
            ImageView failed;
            ImageView avatar;
        }

        public MessageAdapter(ListFragment context, int capacity) {
            ctx = context;
            data = new ArrayList<>(capacity + 10);
            unseenHighlightPositions = new TreeSet<>(Collections.reverseOrder());
        }

        public void clear() {
            max_eid = 0;
            min_eid = 0;
            lastDay = -1;
            lastSeenEidMarkerPosition = -1;
            currentGroupPosition = -1;
            synchronized (data) {
                data.clear();
            }
            unseenHighlightPositions.clear();
            avatarContainer.setVisibility(View.GONE);
            avatar.setTag(null);
        }

        public void clearPending() {
            synchronized (data) {
                for (int i = 0; i < data.size(); i++) {
                    if (data.get(i).reqid != -1 && data.get(i).color == colorScheme.timestampColor) {
                        data.remove(i);
                        i--;
                    }
                }
            }
        }

        public void removeItem(long eid) {
            synchronized (data) {
                for (int i = 0; i < data.size(); i++) {
                    if (data.get(i).eid == eid) {
                        data.remove(i);
                        i--;
                    }
                }
            }
        }

        public int getBacklogMarkerPosition() {
            try {
                synchronized (data) {
                    for (int i = 0; i < data.size(); i++) {
                        Event e = data.get(i);
                        if (e != null && e.row_type == ROW_BACKLOGMARKER) {
                            return i;
                        }
                    }
                }
            } catch (Exception e) {
            }
            return -1;
        }

        public int insertLastSeenEIDMarker() {
            if (buffer == null)
                return -1;

            synchronized (data) {
                if ((min_eid > 0 && buffer.getLast_seen_eid() > 0 && min_eid >= buffer.getLast_seen_eid()) || (data.size() == 0 && buffer.getLast_seen_eid() > 0)) {
                    lastSeenEidMarkerPosition = 0;
                } else {
                    for (int i = data.size() - 1; i >= 0; i--) {
                        if (data.get(i).eid <= buffer.getLast_seen_eid() && data.get(i).row_type != ROW_LASTSEENEID) {
                            lastSeenEidMarkerPosition = i;
                            break;
                        }
                    }
                    if (lastSeenEidMarkerPosition > 0 && lastSeenEidMarkerPosition != data.size() - 1 && !data.get(lastSeenEidMarkerPosition).self && !data.get(lastSeenEidMarkerPosition).pending) {
                        if (data.get(lastSeenEidMarkerPosition - 1).row_type == ROW_TIMESTAMP)
                            lastSeenEidMarkerPosition--;
                        if (lastSeenEidMarkerPosition > 0) {
                            Event e = new Event();
                            e.bid = buffer.getBid();
                            e.cid = buffer.getCid();
                            e.eid = buffer.getLast_seen_eid() + 1;
                            e.type = TYPE_LASTSEENEID;
                            e.row_type = ROW_LASTSEENEID;
                            if(EventsList.getInstance().getEvent(buffer.getLast_seen_eid(), buffer.getBid()) != null)
                                e.from = EventsList.getInstance().getEvent(buffer.getLast_seen_eid(), buffer.getBid()).from;
                            e.bg_color = colorScheme.socketclosedBackgroundDrawable;
                            addItem(e.eid, e);
                            EventsList.getInstance().addEvent(e);
                            for (int i = 0; i < data.size(); i++) {
                                if (data.get(i).row_type == ROW_LASTSEENEID && data.get(i) != e) {
                                    EventsList.getInstance().deleteEvent(data.get(i).eid, buffer.getBid());
                                    data.remove(i);
                                }
                            }
                        }
                    } else {
                        lastSeenEidMarkerPosition = -1;
                    }
                }
                if (lastSeenEidMarkerPosition > 0 && lastSeenEidMarkerPosition <= currentGroupPosition)
                    currentGroupPosition++;

                if (lastSeenEidMarkerPosition == -1) {
                    for (int i = data.size() - 1; i >= 0; i--) {
                        if (data.get(i).row_type == ROW_LASTSEENEID) {
                            lastSeenEidMarkerPosition = i;
                            break;
                        }
                    }
                }
            }
            return lastSeenEidMarkerPosition;
        }

        public void clearLastSeenEIDMarker() {
            if(buffer != null) {
                synchronized (data) {
                    for (int i = 0; i < data.size(); i++) {
                        if (data.get(i).row_type == ROW_LASTSEENEID) {
                            EventsList.getInstance().deleteEvent(data.get(i).eid, buffer.getBid());
                            data.remove(i);
                        }
                    }
                }
                if (lastSeenEidMarkerPosition > 0)
                    lastSeenEidMarkerPosition = -1;
            }
        }

        public int getLastSeenEIDPosition() {
            return lastSeenEidMarkerPosition;
        }

        public int getUnreadHighlightsAbovePosition(int pos) {
            int count = 0;

            Iterator<Integer> i = unseenHighlightPositions.iterator();
            while (i.hasNext()) {
                Integer p = i.next();
                if (p < pos)
                    break;
                count++;
            }

            return unseenHighlightPositions.size() - count;
        }

        public synchronized void addItem(long eid, Event e) {
            synchronized (data) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(e.getTime());
                int insert_pos = -1;
                SimpleDateFormat formatter = null;
                if (e.timestamp == null || e.timestamp.length() == 0) {
                    formatter = new SimpleDateFormat("h:mm a");
                    if (pref_24hr) {
                        if (pref_seconds)
                            formatter = new SimpleDateFormat("HH:mm:ss");
                        else
                            formatter = new SimpleDateFormat("HH:mm");
                    } else if (pref_seconds) {
                        formatter = new SimpleDateFormat("h:mm:ss a");
                    }
                    e.timestamp = formatter.format(calendar.getTime());
                }
                e.group_eid = currentCollapsedEid;
                if (e.group_msg != null && e.html == null)
                    e.html = e.group_msg;

			/*if(e.html != null) {
                e.html = ColorFormatter.irc_to_html(e.html);
                e.formatted = ColorFormatter.html_to_spanned(e.html, e.linkify, server);
			}*/

                if (e.day < 1) {
                    e.day = calendar.get(Calendar.DAY_OF_YEAR);
                }

                if (currentGroupPosition > 0 && eid == currentCollapsedEid && e.eid != eid) { //Shortcut for replacing the current group
                    calendar.setTimeInMillis(e.getTime());
                    lastDay = e.day;
                    data.remove(currentGroupPosition);
                    data.add(currentGroupPosition, e);
                    insert_pos = currentGroupPosition;
                } else if (eid > max_eid || data.size() == 0 || eid > data.get(data.size() - 1).eid) { //Message at the bottom
                    if (data.size() > 0) {
                        lastDay = data.get(data.size() - 1).day;
                    } else {
                        lastDay = 0;
                    }
                    max_eid = eid;
                    data.add(e);
                    insert_pos = data.size() - 1;
                } else if (min_eid > eid) { //Message goes on top
                    if (data.size() > 1) {
                        lastDay = data.get(1).day;
                        if (calendar.get(Calendar.DAY_OF_YEAR) != lastDay) { //Insert above the dateline
                            data.add(0, e);
                            insert_pos = 0;
                        } else { //Insert below the dateline
                            data.add(1, e);
                            insert_pos = 1;
                        }
                    } else {
                        data.add(0, e);
                        insert_pos = 0;
                    }
                } else {
                    int i = 0;
                    for (Event e1 : data) {
                        if (e1.row_type != ROW_TIMESTAMP && e1.eid > eid && e.eid == eid && e1.group_eid != eid) { //Insert the message
                            if (i > 0 && data.get(i - 1).row_type != ROW_TIMESTAMP) {
                                lastDay = data.get(i - 1).day;
                                data.add(i, e);
                                insert_pos = i;
                                break;
                            } else { //There was a date line above our insertion point
                                lastDay = e1.day;
                                if (calendar.get(Calendar.DAY_OF_YEAR) != lastDay) { //Insert above the dateline
                                    if (i > 1) {
                                        lastDay = data.get(i - 2).day;
                                    } else {
                                        //We're above the first dateline, so we'll need to put a new one on top!
                                        lastDay = 0;
                                    }
                                    data.add(i - 1, e);
                                    insert_pos = i - 1;
                                } else { //Insert below the dateline
                                    data.add(i, e);
                                    insert_pos = i;
                                }
                                break;
                            }
                        } else if (e1.row_type != ROW_TIMESTAMP && (e1.eid == eid || e1.group_eid == eid)) { //Replace the message
                            lastDay = calendar.get(Calendar.DAY_OF_YEAR);
                            data.remove(i);
                            data.add(i, e);
                            insert_pos = i;
                            break;
                        }
                        i++;
                    }
                }

                if (insert_pos == -1) {
                    Log.e("IRCCloud", "Couldn't insert EID: " + eid + " MSG: " + e.html);
                    data.add(e);
                    insert_pos = data.size() - 1;
                }

                if (eid > buffer.getLast_seen_eid() && e.highlight)
                    unseenHighlightPositions.add(insert_pos);

                if (eid < min_eid || min_eid == 0)
                    min_eid = eid;

                if (eid == currentCollapsedEid && e.eid == eid) {
                    currentGroupPosition = insert_pos;
                } else if (currentCollapsedEid == -1) {
                    currentGroupPosition = -1;
                }

                if (calendar.get(Calendar.DAY_OF_YEAR) != lastDay) {
                    if (formatter == null)
                        formatter = new SimpleDateFormat("EEEE, MMMM dd, yyyy");
                    else
                        formatter.applyPattern("EEEE, MMMM dd, yyyy");
                    Event d = new Event();
                    d.type = TYPE_TIMESTAMP;
                    d.row_type = ROW_TIMESTAMP;
                    d.eid = eid - 1;
                    d.timestamp = formatter.format(calendar.getTime());
                    d.bg_color = colorScheme.timestampBackgroundDrawable;
                    d.day = lastDay = calendar.get(Calendar.DAY_OF_YEAR);
                    data.add(insert_pos++, d);
                    if (currentGroupPosition > -1)
                        currentGroupPosition++;
                }

                if (insert_pos > 0) {
                    Event prev = data.get(insert_pos - ((e.row_type == ROW_LASTSEENEID)?2:1));
                    e.header = (e.isMessage() && e.from != null && e.from.length() > 0 && e.group_eid < 1) && (prev.from == null || !prev.from.equals(e.from) || !prev.type.equals(e.type));
                }

                if (insert_pos < (data.size() - 1)) {
                    Event next = data.get(insert_pos + 1);
                    if (!e.isMessage() && e.row_type != ROW_LASTSEENEID) {
                        next.header = (next.isMessage() && next.from != null && next.from.length() > 0 && next.group_eid < 1);
                    }
                    if (next.from != null && next.from.equals(e.from) && next.type.equals(e.type)) {
                        next.header = false;
                    }
                }
            }
        }

        @Override
        public int getCount() {
            synchronized (data) {
                if (ctx != null)
                    return data.size();
                else
                    return 0;
            }
        }

        @Override
        public Object getItem(int position) {
            synchronized (data) {
                if (position < data.size())
                    return data.get(position);
                else
                    return null;
            }
        }

        @Override
        public long getItemId(int position) {
            synchronized (data) {
                if (position < data.size())
                    return data.get(position).eid;
                else
                    return -1;
            }
        }

        public void format() {
            for (int i = 0; i < data.size(); i++) {
                Event e = data.get(i);
                if (e != null) {
                    synchronized (e) {
                        if (e.html != null) {
                            try {
                                e.html = ColorFormatter.emojify(ColorFormatter.irc_to_html(e.html));
                                e.formatted = ColorFormatter.html_to_spanned(e.html, e.linkify, server, e.entities);
                                if (e.msg != null && e.msg.length() > 0)
                                    e.contentDescription = ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(e.msg), e.linkify, server).toString();
                                if (e.from != null && e.from.length() > 0) {
                                    e.formatted_nick = Html.fromHtml("<b>" + ColorFormatter.irc_to_html(collapsedEvents.formatNick(e.from, e.from_mode, !e.self && pref_nickColors, ColorScheme.getInstance().selfTextColor)) + "</b>");
                                }
                                if (e.formatted_realname == null && e.from_realname != null && e.from_realname.length() > 0) {
                                    e.formatted_realname = ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(ColorFormatter.emojify(e.from_realname)), true, null);
                                }
                            } catch (Exception ex) {
                            }
                        }
                    }
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            synchronized (data) {
                return data.get(position).row_type;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position >= data.size() || ctx == null)
                return null;

            Event e;
            synchronized (data) {
                e = data.get(position);
            }
            synchronized (e) {
                View row = convertView;
                ViewHolder holder;

                if (row != null && ((ViewHolder) row.getTag()).type != e.row_type)
                    row = null;

                if (row == null) {
                    LayoutInflater inflater = ctx.getLayoutInflater(null);
                    if (e.row_type == ROW_BACKLOGMARKER)
                        row = inflater.inflate(R.layout.row_backlogmarker, parent, false);
                    else if (e.row_type == ROW_TIMESTAMP)
                        row = inflater.inflate(R.layout.row_timestamp, parent, false);
                    else if (e.row_type == ROW_SOCKETCLOSED)
                        row = inflater.inflate(R.layout.row_socketclosed, parent, false);
                    else if (e.row_type == ROW_LASTSEENEID)
                        row = inflater.inflate(R.layout.row_lastseeneid, parent, false);
                    else
                        row = inflater.inflate(R.layout.row_message, parent, false);

                    holder = new ViewHolder();
                    holder.timestamp = (TextView) row.findViewById(R.id.timestamp);
                    holder.timestamp_left = (TextView) row.findViewById(R.id.timestamp_left);
                    holder.timestamp_right = (TextView) row.findViewById(R.id.timestamp_right);
                    holder.message = (TextView) row.findViewById(R.id.message);
                    holder.expandable = (TextView) row.findViewById(R.id.expandable);
                    if(holder.expandable != null)
                        holder.expandable.setTypeface(FontAwesome.getTypeface());
                    holder.nickname = (TextView) row.findViewById(R.id.nickname);
                    holder.realname = (TextView) row.findViewById(R.id.realname);
                    holder.failed = (ImageView) row.findViewById(R.id.failed);
                    holder.avatar = (ImageView) row.findViewById(R.id.avatar);
                    holder.lastSeenEidWrapper = (LinearLayout) row.findViewById(R.id.lastSeenEidWrapper);
                    holder.messageContainer = (LinearLayout) row.findViewById(R.id.messageContainer);
                    holder.socketClosedBar = (LinearLayout) row.findViewById(R.id.socketClosedBar);
                    holder.type = e.row_type;

                    row.setTag(holder);
                } else {
                    holder = (ViewHolder) row.getTag();
                }

                row.setOnClickListener(new OnItemClickListener(position));
                row.setOnLongClickListener(new OnItemLongClickListener(position));

                if (e.html != null && e.formatted == null) {
                    e.html = ColorFormatter.emojify(ColorFormatter.irc_to_html(e.html));
                    e.formatted = ColorFormatter.html_to_spanned(e.html, e.linkify, server, e.entities);
                    if (e.msg != null && e.msg.length() > 0)
                        e.contentDescription = ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(e.msg), e.linkify, server).toString();
                }

                if (e.formatted_nick == null && e.from != null && e.from.length() > 0) {
                    e.formatted_nick = Html.fromHtml("<b>" + ColorFormatter.irc_to_html(collapsedEvents.formatNick(e.from, e.from_mode, !e.self && pref_nickColors, ColorScheme.getInstance().selfTextColor)) + "</b>");
                }

                if (e.formatted_realname == null && e.from_realname != null && e.from_realname.length() > 0) {
                    e.formatted_realname = ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(ColorFormatter.emojify(e.from_realname)), true, null);
                }

                if (e.row_type == ROW_MESSAGE) {
                    if (e.bg_color == colorScheme.contentBackgroundColor)
                        row.setBackgroundDrawable(null);
                    else
                        row.setBackgroundColor(e.bg_color);
                    if (e.contentDescription != null && e.from != null && e.from.length() > 0 && e.msg != null && e.msg.length() > 0) {
                        row.setContentDescription("Message from " + e.from + " at " + e.timestamp + ": " + e.contentDescription);
                    }
                }

                boolean mono = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("monospace", false);

                if(holder.timestamp_left != null) {
                    if(pref_timeLeft && (pref_chatOneLine || pref_avatarsOff)) {
                        holder.timestamp_left.setVisibility(View.VISIBLE);
                        holder.timestamp_right.setVisibility(View.GONE);
                        holder.timestamp = holder.timestamp_left;
                    } else {
                        holder.timestamp_left.setVisibility(View.GONE);
                        holder.timestamp_right.setVisibility(View.VISIBLE);
                        holder.timestamp = holder.timestamp_right;
                    }
                }

                if (holder.timestamp != null) {
                    holder.timestamp.setTypeface(mono ? hackRegular : Typeface.DEFAULT);
                    if (e.row_type == ROW_TIMESTAMP) {
                        holder.timestamp.setTextSize(textSize);
                    } else {
                        holder.timestamp.setTextSize(textSize - 2);

                        if (timestamp_width == -1) {
                            String s = " 88:88";
                            if (pref_seconds)
                                s += ":88";
                            if (!pref_24hr)
                                s += " MM";
                            timestamp_width = (int) holder.timestamp.getPaint().measureText(s);
                        }
                        holder.timestamp.setMinWidth(timestamp_width);
                    }
                    if (e.highlight && !e.self)
                        holder.timestamp.setTextColor(colorScheme.highlightTimestampColor);
                    else if (e.row_type != ROW_TIMESTAMP)
                        holder.timestamp.setTextColor(colorScheme.timestampColor);
                    holder.timestamp.setText(e.timestamp);
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)holder.timestamp.getLayoutParams();
                    lp.topMargin = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pref_compact?0:2, getResources().getDisplayMetrics());

                }
                if (e.row_type == ROW_SOCKETCLOSED) {
                    if (e.msg != null && e.msg.length() > 0) {
                        holder.timestamp.setVisibility(View.VISIBLE);
                        holder.message.setVisibility(View.VISIBLE);
                    } else {
                        holder.timestamp.setVisibility(View.GONE);
                        holder.message.setVisibility(View.GONE);
                    }
                }

                if (holder.message != null && e.html != null) {
                    holder.message.setMovementMethod(linkMovementMethodNoLongPress);
                    holder.message.setOnClickListener(new OnItemClickListener(position));
                    holder.message.setOnLongClickListener(new OnItemLongClickListener(position));
                    if (mono || (e.msg != null && e.msg.startsWith("<pre>"))) {
                        holder.message.setTypeface(hackRegular);
                    } else {
                        holder.message.setTypeface(Typeface.DEFAULT);
                    }
                    try {
                        holder.message.setTextColor(e.color);
                    } catch (Exception e1) {

                    }
                    if (e.color == colorScheme.timestampColor || e.pending)
                        holder.message.setLinkTextColor(colorScheme.lightLinkColor);
                    else
                        holder.message.setLinkTextColor(colorScheme.linkColor);

                    if(pref_compact)
                        holder.message.setLineSpacing(0,1);
                    else
                        holder.message.setLineSpacing(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics()),1);

                    Spanned formatted = e.formatted;
                    if(formatted != null && !pref_avatarsOff && ((e.from != null && e.from.length() > 0) || e.type.equals("buffer_me_msg")) && e.group_eid < 0 && (pref_chatOneLine || e.type.equals("buffer_me_msg"))) {
                        Bitmap b = mAvatarsList.getAvatar(e.cid, e.type.equals("buffer_me_msg")?e.nick:e.from).getBitmap(ColorScheme.getInstance().isDarkTheme, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, textSize+4, getResources().getDisplayMetrics()), e.self);
                        if(b != null) {
                            SpannableStringBuilder s = new SpannableStringBuilder(formatted);
                            s.insert(0, "  ");
                            s.setSpan(new ImageSpan(getActivity(), b) {
                                @Override
                                public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
                                    Drawable b = getDrawable();
                                    canvas.save();

                                    canvas.translate(x, top + ((paint.getFontMetricsInt().descent - paint.getFontMetricsInt().ascent) / 2) - ((b.getBounds().bottom - b.getBounds().top) / 2) - (((int)textSize - b.getIntrinsicHeight() / 2)));
                                    b.draw(canvas);
                                    canvas.restore();
                                }
                            }, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            formatted = s;
                        }
                    }
                    holder.message.setText(formatted);
                    if (e.from != null && e.from.length() > 0 && e.msg != null && e.msg.length() > 0) {
                        holder.message.setContentDescription(e.from + ": " + e.contentDescription);
                    }
                    holder.message.setTextSize(textSize);
                }

                if (holder.expandable != null) {
                    if (e.group_eid > 0 && (e.group_eid != e.eid || expandedSectionEids.contains(e.group_eid))) {
                        if (expandedSectionEids.contains(e.group_eid)) {
                            if (e.group_eid == e.eid + 1) {
                                holder.expandable.setText(FontAwesome.MINUS_SQUARE_O);
                                holder.expandable.setContentDescription("expanded");
                                row.setBackgroundColor(colorScheme.collapsedHeadingBackgroundColor);
                            } else {
                                holder.expandable.setText(FontAwesome.ANGLE_RIGHT);
                                holder.expandable.setContentDescription("collapse");
                                row.setBackgroundColor(colorScheme.contentBackgroundColor);
                            }
                        } else {
                            holder.expandable.setText(FontAwesome.PLUS_SQUARE_O);
                            holder.expandable.setContentDescription("expand");
                        }
                        holder.expandable.setVisibility(View.VISIBLE);
                    } else {
                        holder.expandable.setVisibility(View.GONE);
                    }
                    holder.expandable.setTextColor(colorScheme.expandCollapseIndicatorColor);
                }

                if (holder.failed != null)
                    holder.failed.setVisibility(e.failed ? View.VISIBLE : View.GONE);

                if (holder.messageContainer != null) {
                    if(pref_compact)
                        holder.messageContainer.setPadding(0,0,0,0);
                    else
                        holder.messageContainer.setPadding(0,0,0,(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics()));
                }

                if (holder.avatar != null) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)holder.avatar.getLayoutParams();
                    if(pref_avatarsOff || pref_chatOneLine || (e.row_type == ROW_SOCKETCLOSED && (e.msg == null || e.msg.length() == 0))) {
                        holder.avatar.setImageBitmap(null);
                        lp.topMargin = lp.width = lp.height = 0;
                    } else {
                        lp.topMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
                        lp.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 32, getResources().getDisplayMetrics());
                        Bitmap b = null;
                        if (e.group_eid < 1 && e.from != null && e.from.length() > 0 && (pref_chatOneLine || e.header)) {
                            Avatar a = mAvatarsList.getAvatar(e.cid, e.from);
                            b = a.getBitmap(ColorScheme.getInstance().isDarkTheme, lp.width, e.self);
                            holder.avatar.setVisibility(View.VISIBLE);
                            holder.avatar.setTag(a);
                        }
                        holder.avatar.setImageBitmap(b);
                        if(b != null)
                            lp.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 16 * ((!e.header || b == null || e.group_eid > 0)?1:2), getResources().getDisplayMetrics());
                        else
                            lp.height = 0;
                    }
                    holder.avatar.setLayoutParams(lp);
                }

                if(!pref_chatOneLine && e.header && e.formatted_nick != null && e.formatted_nick.length() > 0 && e.group_eid < 1) {
                    if (holder.nickname != null) {
                        holder.nickname.setVisibility(View.VISIBLE);
                        if (mono)
                            holder.nickname.setTypeface(hackRegular);
                        else
                            holder.nickname.setTypeface(Typeface.DEFAULT);
                        holder.nickname.setTextSize(textSize);
                        holder.nickname.setText(e.formatted_nick);

                        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.nickname.getLayoutParams();
                        lp.leftMargin = (pref_timeLeft&&pref_avatarsOff)?timestamp_width:0;
                        holder.nickname.setLayoutParams(lp);
                    }

                    if (holder.realname != null) {
                        holder.realname.setMovementMethod(linkMovementMethodNoLongPress);
                        holder.realname.setVisibility((pref_chatOneLine || pref_norealname) ? View.GONE : View.VISIBLE);
                        if (mono)
                            holder.realname.setTypeface(hackRegular);
                        else
                            holder.realname.setTypeface(Typeface.DEFAULT);
                        holder.realname.setTextSize(textSize);
                        holder.realname.setTextColor(colorScheme.timestampColor);
                        holder.realname.setText(e.formatted_realname);
                    }
                } else {
                    if (holder.nickname != null)
                        holder.nickname.setVisibility(View.GONE);
                    if (holder.realname != null)
                        holder.realname.setVisibility(View.GONE);
                    if (holder.avatar != null)
                        holder.avatar.setVisibility((pref_avatarsOff || pref_chatOneLine) ? View.GONE : View.VISIBLE);
                    if(holder.message != null) {
                        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.message.getLayoutParams();
                        lp.leftMargin = 0;
                        holder.message.setLayoutParams(lp);
                    }
                }

                if(holder.lastSeenEidWrapper != null) {
                    ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) holder.lastSeenEidWrapper.getLayoutParams();
                    if(!pref_avatarsOff && !pref_chatOneLine)
                        lp.leftMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 48, getResources().getDisplayMetrics());
                    else
                        lp.leftMargin = 0;
                    holder.lastSeenEidWrapper.setLayoutParams(lp);
                    holder.lastSeenEidWrapper.setMinimumHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, pref_compact?(textSize + 2):(textSize + 4), getResources().getDisplayMetrics()));
                }

                if(holder.socketClosedBar != null) {
                    holder.socketClosedBar.setMinimumHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize + 2, getResources().getDisplayMetrics()));
                }

                if(e.row_type == ROW_BACKLOGMARKER)
                    row.setMinimumHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, pref_compact?4:(textSize + 2), getResources().getDisplayMetrics()));

                return row;
            }
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        hackRegular = Typeface.createFromAsset(getActivity().getAssets(), "Hack-Regular.otf");

        final View v = inflater.inflate(R.layout.messageview, container, false);
        statusView = (TextView) v.findViewById(R.id.statusView);
        statusView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (buffer != null && conn != null && server != null && server.getStatus() != null && server.getStatus().equalsIgnoreCase("disconnected")) {
                    NetworkConnection.getInstance().reconnect(buffer.getCid());
                }
            }

        });

        awayView = v.findViewById(R.id.away);
        awayView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(buffer != null)
                    NetworkConnection.getInstance().back(buffer.getCid());
            }

        });
        awayTxt = (TextView) v.findViewById(R.id.awayTxt);
        unreadBottomView = v.findViewById(R.id.unreadBottom);
        unreadBottomView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if(buffer != null)
                    buffer.setScrolledUp(false);
                getListView().setSelection(adapter.getCount() - 1);
                hideView(unreadBottomView);
            }

        });
        unreadBottomLabel = (TextView) v.findViewById(R.id.unread);
        highlightsBottomLabel = (TextView) v.findViewById(R.id.highlightsBottom);

        unreadTopView = v.findViewById(R.id.unreadTop);
        unreadTopView.setVisibility(View.GONE);
        unreadTopView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (adapter.getLastSeenEIDPosition() > 0) {
                    if (heartbeatTask != null)
                        heartbeatTask.cancel(true);
                    heartbeatTask = new HeartbeatTask();
                    heartbeatTask.execute((Void) null);
                    hideView(unreadTopView);
                }
                getListView().setSelection(adapter.getLastSeenEIDPosition());
            }

        });
        unreadTopLabel = (TextView) v.findViewById(R.id.unreadTopText);
        highlightsTopLabel = (TextView) v.findViewById(R.id.highlightsTop);
        Button b = (Button) v.findViewById(R.id.markAsRead);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideView(unreadTopView);
                if (heartbeatTask != null)
                    heartbeatTask.cancel(true);
                heartbeatTask = new HeartbeatTask();
                heartbeatTask.execute((Void) null);
            }
        });
        globalMsgView = v.findViewById(R.id.globalMessageView);
        globalMsg = (TextView) v.findViewById(R.id.globalMessageTxt);
        b = (Button) v.findViewById(R.id.dismissGlobalMessage);
        b.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (conn != null)
                    conn.globalMsg = null;
                update_global_msg();
            }
        });
        spinner = (ProgressBar) v.findViewById(R.id.spinner);
        suggestionsContainer = v.findViewById(R.id.suggestionsContainer);
        suggestions = (GridView) v.findViewById(R.id.suggestions);
        headerViewContainer = getLayoutInflater(null).inflate(R.layout.messageview_header, null);
        headerView = headerViewContainer.findViewById(R.id.progress);
        backlogFailed = (TextView) headerViewContainer.findViewById(R.id.backlogFailed);
        loadBacklogButton = (Button) headerViewContainer.findViewById(R.id.loadBacklogButton);
        loadBacklogButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (conn != null && buffer != null) {
                    backlogFailed.setVisibility(View.GONE);
                    loadBacklogButton.setVisibility(View.GONE);
                    headerView.setVisibility(View.VISIBLE);
                    NetworkConnection.getInstance().request_backlog(buffer.getCid(), buffer.getBid(), earliest_eid);
                }
            }
        });
        ((ListView) v.findViewById(android.R.id.list)).addHeaderView(headerViewContainer);
        footerViewContainer = new View(getActivity());
        ((ListView) v.findViewById(android.R.id.list)).addFooterView(footerViewContainer, null, false);
        avatarContainer = (OffsetLinearLayout) v.findViewById(R.id.avatarContainer);
        avatar = (ImageView) v.findViewById(R.id.avatar);
        return v;
    }

    public void showSpinner(boolean show) {
        if (show) {
            if (Build.VERSION.SDK_INT < 16) {
                AlphaAnimation anim = new AlphaAnimation(0, 1);
                anim.setDuration(150);
                anim.setFillAfter(true);
                spinner.setAnimation(anim);
            } else {
                spinner.setAlpha(0);
                spinner.animate().alpha(1);
            }
            spinner.setVisibility(View.VISIBLE);
        } else {
            if (Build.VERSION.SDK_INT < 16) {
                AlphaAnimation anim = new AlphaAnimation(1, 0);
                anim.setDuration(150);
                anim.setFillAfter(true);
                anim.setAnimationListener(new AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        spinner.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                spinner.setAnimation(anim);
            } else {
                spinner.animate().alpha(0).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        spinner.setVisibility(View.GONE);
                    }
                });
            }
        }
    }

    private void hideView(final View v) {
        if (v.getVisibility() != View.GONE) {
            if (Build.VERSION.SDK_INT >= 16) {
                v.animate().alpha(0).setDuration(100).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        v.setVisibility(View.GONE);
                    }
                });
            } else {
                v.setVisibility(View.GONE);
            }
        }
    }

    private void showView(final View v) {
        if (v.getVisibility() != View.VISIBLE) {
            if (Build.VERSION.SDK_INT >= 16) {
                v.setAlpha(0);
                v.animate().alpha(1).setDuration(100);
            }
            v.setVisibility(View.VISIBLE);
        }
    }

    public void drawerClosed() {
        try {
            ListView v = getListView();
            mOnScrollListener.onScroll(v, v.getFirstVisiblePosition(), v.getLastVisiblePosition() - v.getFirstVisiblePosition(), adapter.getCount());
        } catch (Exception e) {
        }
    }

    private void hide_avatar() {
        if (avatarContainer.getVisibility() != View.GONE)
            avatarContainer.setVisibility(View.GONE);
        if (avatar.getTag() != null) {
            ((ImageView) avatar.getTag()).setVisibility(View.VISIBLE);
            avatar.setTag(null);
        }
    }

    private OnScrollListener mOnScrollListener = new OnScrollListener() {
        @Override
        public void onScroll(final AbsListView view, final int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (!ready || buffer == null || adapter == null || visibleItemCount < 0) {
                hide_avatar();
                return;
            }

            if (conn.ready && !requestingBacklog && headerView != null && buffer.getMin_eid() > 0) {
                if (firstVisibleItem == 0 && headerView.getVisibility() == View.VISIBLE && conn.getState() == NetworkConnection.STATE_CONNECTED) {
                    requestingBacklog = true;
                    conn.request_backlog(buffer.getCid(), buffer.getBid(), earliest_eid);
                    hide_avatar();
                    return;
                } else if(firstVisibleItem > 0 && loadBacklogButton.getVisibility() == View.VISIBLE) {
                    loadBacklogButton.setVisibility(View.GONE);
                    headerView.setVisibility(View.VISIBLE);
                }
            }

            if (unreadBottomView != null && adapter.data.size() > 0) {
                if (firstVisibleItem + visibleItemCount >= totalItemCount) {
                    if(unreadBottomView.getVisibility() != View.GONE)
                        unreadBottomView.setVisibility(View.GONE);
                    if (unreadTopView.getVisibility() == View.GONE && conn.getState() == NetworkConnection.STATE_CONNECTED) {
                        if (heartbeatTask != null)
                            heartbeatTask.cancel(true);
                        heartbeatTask = new HeartbeatTask();
                        heartbeatTask.execute((Void) null);
                    }
                    newMsgs = 0;
                    newMsgTime = 0;
                    newHighlights = 0;
                }
            }
            if (firstVisibleItem + visibleItemCount < totalItemCount - 1) {
                View v = view.getChildAt(0);
                buffer.setScrolledUp(true);
                buffer.setScrollPosition(firstVisibleItem);
                buffer.setScrollPositionOffset((v == null) ? 0 : v.getTop());
            } else {
                buffer.setScrolledUp(false);
                buffer.setScrollPosition(-1);
            }

            if (adapter != null && adapter.data.size() > 0) {
                synchronized (adapter.data) {
                    if (unreadTopView != null && unreadTopView.getVisibility() == View.VISIBLE) {
                        update_top_unread(firstVisibleItem);
                        int markerPos = -1;
                        if (adapter != null)
                            markerPos = adapter.getLastSeenEIDPosition();
                        if (markerPos > 1 && getListView().getFirstVisiblePosition() <= markerPos) {
                            unreadTopView.setVisibility(View.GONE);
                            if (conn.getState() == NetworkConnection.STATE_CONNECTED) {
                                if (heartbeatTask != null)
                                    heartbeatTask.cancel(true);
                                heartbeatTask = new HeartbeatTask();
                                heartbeatTask.execute((Void) null);
                            }
                        }
                    }

                    if (!pref_chatOneLine && !pref_avatarsOff && firstVisibleItem > ((ListView) view).getHeaderViewsCount()) {
                        int offset = (unreadTopView.getVisibility() == View.VISIBLE) ? unreadTopView.getHeight() : 0;
                        View v;
                        int i = 0;
                        do {
                            v = view.getChildAt(i++);
                        } while (v != null && v.getTop() + v.getHeight() <= offset - 1);
                        if(v != null) {
                            int first = firstVisibleItem - ((ListView) view).getHeaderViewsCount() + i - 1;
                            MessageAdapter.ViewHolder vh = (MessageAdapter.ViewHolder) v.getTag();
                            MessageAdapter.ViewHolder top_vh = vh;
                            Event e = first >= 0 ? (Event) adapter.getItem(first) : null;
                            if (first > 0 && vh != null && v.getTop() <= offset && e != null && ((vh.avatar != null && e.group_eid < 1 && e.isMessage() && e.from != null && e.from.length() > 0) || e.row_type == ROW_LASTSEENEID)) {
                                for (i = first; i < adapter.getCount() && i < first + 4; i++) {
                                    e = (Event) adapter.getItem(i);
                                    if(e.row_type == ROW_LASTSEENEID)
                                        e = (Event) adapter.getItem(i-1);
                                    int next = i + 1;
                                    Event e1 = (Event) adapter.getItem(next);
                                    if(e1 != null && e1.row_type == ROW_LASTSEENEID) {
                                        next++;
                                        if(next < adapter.getCount())
                                            e1 = (Event) adapter.getItem(next);
                                        else
                                            break;
                                    }
                                    if (e != null && e1 != null && e.from != null && e.from.equals(e1.from) && e1.group_eid < 1 && !e1.header) {
                                        View v1 = view.getChildAt(next - (firstVisibleItem - ((ListView) view).getHeaderViewsCount()));
                                        if (v1 != null) {
                                            MessageAdapter.ViewHolder vh1 = (MessageAdapter.ViewHolder) v1.getTag();
                                            if (vh1 != null && vh1.avatar != null) {
                                                v = v1;
                                                vh = vh1;
                                            }
                                        }
                                    } else {
                                        break;
                                    }
                                }
                                if (avatar.getTag() != null && avatar.getTag() != top_vh.avatar) {
                                    ((ImageView) avatar.getTag()).setVisibility(View.VISIBLE);
                                }

                                if(e != null) {
                                    Bitmap bitmap = mAvatarsList.getAvatar(e.cid, e.from).getBitmap(ColorScheme.getInstance().isDarkTheme, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 32, getResources().getDisplayMetrics()), e.self);
                                    if(vh.avatar != null && bitmap != null) {
                                        int topMargin, leftMargin;
                                        int height = bitmap.getHeight() + (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics());
                                        if (v.getHeight() + v.getTop() < (height + offset)) {
                                            topMargin = v.getTop() + v.getHeight() - height;
                                        } else {
                                            topMargin = offset;
                                        }
                                        leftMargin = vh.avatar.getLeft();
                                        avatarContainer.offset(leftMargin, topMargin);
                                        if (top_vh.avatar == null || (avatar.getTag() != top_vh.avatar || top_vh.avatar.getVisibility() != View.INVISIBLE)) {
                                            avatar.setTag(top_vh.avatar);
                                            avatar.setImageBitmap(bitmap);
                                            avatarContainer.setVisibility(View.VISIBLE);
                                            if(top_vh.avatar != null)
                                                top_vh.avatar.setVisibility(View.INVISIBLE);
                                        }
                                    } else {
                                        hide_avatar();
                                    }
                                }
                            } else {
                                hide_avatar();
                            }
                        } else {
                            hide_avatar();
                        }
                    } else {
                        hide_avatar();
                    }
                }
            } else {
                hide_avatar();
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(tapTimer == null)
            tapTimer = new Timer("message-tap-timer");
        conn = NetworkConnection.getInstance();
        conn.addHandler(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (MessageViewListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement MessageViewListener");
        }
        if(tapTimer == null)
            tapTimer = new Timer("message-tap-timer");
    }

    @Override
    public void setArguments(Bundle args) {
        ready = false;
        if (heartbeatTask != null)
            heartbeatTask.cancel(true);
        heartbeatTask = null;
        if (tapTimerTask != null)
            tapTimerTask.cancel();
        tapTimerTask = null;
        if(tapTimer == null)
            tapTimer = new Timer("message-tap-timer");
        if (buffer != null && buffer.getBid() != args.getInt("bid", -1) && adapter != null)
            adapter.clearLastSeenEIDMarker();
        buffer = BuffersList.getInstance().getBuffer(args.getInt("bid", -1));
        if (buffer != null) {
            server = buffer.getServer();
            Crashlytics.log(Log.DEBUG, "IRCCloud", "MessageViewFragment: switched to bid: " + buffer.getBid());
        } else {
            Crashlytics.log(Log.WARN, "IRCCloud", "MessageViewFragment: couldn't find buffer to switch to");
        }
        requestingBacklog = false;
        avgInsertTime = 0;
        newMsgs = 0;
        newMsgTime = 0;
        newHighlights = 0;
        earliest_eid = 0;
        backlog_eid = 0;
        currentCollapsedEid = -1;
        lastCollapsedDay = -1;
        if (server != null) {
            ignore.setIgnores(server.ignores);
            if (server.getAway() != null && server.getAway().length() > 0) {
                awayTxt.setText(ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(TextUtils.htmlEncode("Away (" + server.getAway() + ")"))).toString());
                awayView.setVisibility(View.VISIBLE);
            } else {
                awayView.setVisibility(View.GONE);
            }
            collapsedEvents.setServer(server);
            update_status(server.getStatus(), server.getFail_info());
        }
        if (avatar != null) {
            avatarContainer.setVisibility(View.GONE);
            avatar.setTag(null);
        }
        if (unreadTopView != null)
            unreadTopView.setVisibility(View.GONE);
        backlogFailed.setVisibility(View.GONE);
        loadBacklogButton.setVisibility(View.GONE);
        try {
            if (getListView().getHeaderViewsCount() == 0) {
                getListView().addHeaderView(headerViewContainer);
            }
        } catch (IllegalStateException e) {
        }
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) headerView.getLayoutParams();
        lp.topMargin = 0;
        headerView.setLayoutParams(lp);
        lp = (ViewGroup.MarginLayoutParams) backlogFailed.getLayoutParams();
        lp.topMargin = 0;
        backlogFailed.setLayoutParams(lp);
        if (buffer != null && EventsList.getInstance().getEventsForBuffer(buffer.getBid()) != null) {
            requestingBacklog = true;
            if (refreshTask != null)
                refreshTask.cancel(true);
            refreshTask = new RefreshTask();
            if (args.getBoolean("fade")) {
                Crashlytics.log(Log.DEBUG, "IRCCloud", "MessageViewFragment: Loading message contents in the background");
                refreshTask.execute((Void) null);
            } else {
                Crashlytics.log(Log.DEBUG, "IRCCloud", "MessageViewFragment: Loading message contents");
                refreshTask.onPreExecute();
                refreshTask.onPostExecute(refreshTask.doInBackground());
            }
        } else {
            if (buffer == null || buffer.getMin_eid() == 0 || earliest_eid == buffer.getMin_eid() || conn.getState() != NetworkConnection.STATE_CONNECTED || !conn.ready) {
                headerView.setVisibility(View.GONE);
                loadBacklogButton.setVisibility(View.GONE);
            } else {
                headerView.setVisibility(View.GONE);
                loadBacklogButton.setVisibility(View.VISIBLE);
            }
            if (adapter != null) {
                adapter.clear();
                adapter.notifyDataSetInvalidated();
            } else {
                adapter = new MessageAdapter(MessageViewFragment.this, 0);
                setListAdapter(adapter);
            }
            if(mListener != null)
                mListener.onMessageViewReady();
            ready = true;
        }
    }

    private void runOnUiThread(Runnable r) {
        if (getActivity() != null)
            getActivity().runOnUiThread(r);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Crashlytics.log(Log.DEBUG, "IRCCloud", "Received low memory warning in the foreground, cleaning backlog in other buffers");
        for (Buffer b : BuffersList.getInstance().getBuffers()) {
            if (b != buffer)
                EventsList.getInstance().pruneEvents(b.getBid());
        }
    }

    private synchronized void insertEvent(final MessageAdapter adapter, Event event, boolean backlog, boolean nextIsGrouped) {
        synchronized (adapterLock) {
            try {
                long start = System.currentTimeMillis();
                if (event.eid <= buffer.getMin_eid()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            headerView.setVisibility(View.GONE);
                            backlogFailed.setVisibility(View.GONE);
                            loadBacklogButton.setVisibility(View.GONE);
                        }
                    });
                }
                if (earliest_eid == 0 || event.eid < earliest_eid)
                    earliest_eid = event.eid;

                String type = event.type;
                long eid = event.eid;

                if (type.startsWith("you_"))
                    type = type.substring(4);

                if (type.equals("joined_channel") || type.equals("parted_channel") || type.equals("nickchange") || type.equals("quit") || type.equals("user_channel_mode") || type.equals("socket_closed") || type.equals("connecting_cancelled") || type.equals("connecting_failed")) {
                    collapsedEvents.showChan = !buffer.isChannel();
                    if (pref_hideJoinPart && !type.equals("socket_closed") && !type.equals("connecting_cancelled") && !type.equals("connecting_failed")) {
                        adapter.removeItem(event.eid);
                        if (!backlog)
                            adapter.notifyDataSetChanged();
                        return;
                    }

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(event.getTime());

                    if (pref_expandJoinPart)
                        expandedSectionEids.clear();

                    if (event.type.equals("socket_closed") || event.type.equals("connecting_failed") || event.type.equals("connecting_cancelled")) {
                        Event last = EventsList.getInstance().getEvent(lastCollapsedEid, buffer.getBid());
                        if (last != null && !last.type.equals("socket_closed") && !last.type.equals("connecting_failed") && !last.type.equals("connecting_cancelled"))
                            currentCollapsedEid = -1;
                    } else {
                        Event last = EventsList.getInstance().getEvent(lastCollapsedEid, buffer.getBid());
                        if (last != null && (last.type.equals("socket_closed") || last.type.equals("connecting_failed") || last.type.equals("connecting_cancelled")))
                            currentCollapsedEid = -1;
                    }

                    if (currentCollapsedEid == -1 || calendar.get(Calendar.DAY_OF_YEAR) != lastCollapsedDay || pref_expandJoinPart || event.type.equals("you_parted_channel")) {
                        collapsedEvents.clear();
                        currentCollapsedEid = eid;
                        lastCollapsedDay = calendar.get(Calendar.DAY_OF_YEAR);
                    }

                    if (!collapsedEvents.showChan)
                        event.chan = buffer.getName();

                    if (!collapsedEvents.addEvent(event))
                        collapsedEvents.clear();

                    if ((currentCollapsedEid == event.eid || pref_expandJoinPart) && type.equals("user_channel_mode")) {
                        event.color = colorScheme.messageTextColor;
                        event.bg_color = colorScheme.collapsedHeadingBackgroundColor;
                    } else {
                        event.color = colorScheme.collapsedRowTextColor;
                        event.bg_color = colorScheme.contentBackgroundColor;
                    }

                    String msg;
                    if (expandedSectionEids.contains(currentCollapsedEid)) {
                        CollapsedEventsList c = new CollapsedEventsList();
                        c.showChan = collapsedEvents.showChan;
                        c.setServer(server);
                        c.addEvent(event);
                        msg = c.getCollapsedMessage();
                        if (!nextIsGrouped) {
                            String group_msg = collapsedEvents.getCollapsedMessage();
                            if (group_msg == null && type.equals("nickchange")) {
                                group_msg = event.old_nick + " → <b>" + event.nick + "</b>";
                            }
                            if (group_msg == null && type.equals("user_channel_mode")) {
                                if (event.from != null && event.from.length() > 0)
                                    msg = collapsedEvents.formatNick(event.nick, event.target_mode, false) + " was set to <b>" + event.diff + "</b> by <b>" + collapsedEvents.formatNick(event.from, event.from_mode, false) + "</b>";
                                else
                                    msg = collapsedEvents.formatNick(event.nick, event.target_mode, false) + " was set to <b>" + event.diff + "</b> by the server <b>" + event.server + "</b>";
                                currentCollapsedEid = eid;
                            }
                            Event heading = new Event();
                            heading.type = "__expanded_group_heading__";
                            heading.cid = event.cid;
                            heading.bid = event.bid;
                            heading.eid = currentCollapsedEid - 1;
                            heading.group_msg = group_msg;
                            heading.color = colorScheme.timestampColor;
                            heading.bg_color = colorScheme.contentBackgroundColor;
                            heading.linkify = false;
                            adapter.addItem(currentCollapsedEid - 1, heading);
                            if (event.type.equals("socket_closed") || event.type.equals("connecting_failed") || event.type.equals("connecting_cancelled")) {
                                Event last = EventsList.getInstance().getEvent(lastCollapsedEid, buffer.getBid());
                                if (last != null)
                                    last.row_type = ROW_MESSAGE;
                                event.row_type = ROW_SOCKETCLOSED;
                            }
                        }
                        event.timestamp = null;
                    } else {
                        msg = (nextIsGrouped && currentCollapsedEid != event.eid) ? "" : collapsedEvents.getCollapsedMessage();
                    }

                    if (msg == null && type.equals("nickchange")) {
                        msg = event.old_nick + " → <b>" + event.nick + "</b>";
                    }
                    if (msg == null && type.equals("user_channel_mode")) {
                        if (event.from != null && event.from.length() > 0)
                            msg = collapsedEvents.formatNick(event.nick, event.target_mode, false) + " was set to <b>" + event.diff + "</b> by <b>" + collapsedEvents.formatNick(event.from, event.from_mode, false) + "</b>";
                        else
                            msg = collapsedEvents.formatNick(event.nick, event.target_mode, false) + " was set to <b>" + event.diff + "</b> by the server <b>" + event.server + "</b>";
                        currentCollapsedEid = eid;
                    }
                    if (!expandedSectionEids.contains(currentCollapsedEid)) {
                        if (eid != currentCollapsedEid) {
                            event.color = colorScheme.timestampColor;
                            event.bg_color = colorScheme.contentBackgroundColor;
                        }
                        eid = currentCollapsedEid;
                    }
                    event.group_msg = msg;
                    event.html = null;
                    event.formatted = null;
                    event.linkify = false;
                    lastCollapsedEid = event.eid;
                    if ((buffer.isConsole() && !event.type.equals("socket_closed") && !event.type.equals("connecting_failed") && !event.type.equals("connecting_cancelled")) || event.type.equals("you_parted_channel")) {
                        currentCollapsedEid = -1;
                        lastCollapsedEid = -1;
                        collapsedEvents.clear();
                    }
                } else {
                    currentCollapsedEid = -1;
                    lastCollapsedEid = -1;
                    collapsedEvents.clear();
                    if (event.html == null) {
                        if ((pref_chatOneLine || !event.isMessage()) && event.from != null && event.from.length() > 0)
                            event.html = "<b>" + collapsedEvents.formatNick(event.from, event.from_mode, !event.self && pref_nickColors, ColorScheme.getInstance().selfTextColor) + "</b> " + event.msg;
                        else if (pref_chatOneLine && event.type.equals("buffer_msg") && event.server != null && event.server.length() > 0)
                            event.html = "<b>" + event.server + "</b> " + event.msg;
                        else
                            event.html = event.msg;
                    }

                    if (event.formatted_nick == null && event.from != null && event.from.length() > 0) {
                        event.formatted_nick = Html.fromHtml("<b>" + ColorFormatter.irc_to_html(collapsedEvents.formatNick(event.from, event.from_mode, !event.self && pref_nickColors, ColorScheme.getInstance().selfTextColor)) + "</b>");
                    }

                    if (event.formatted_realname == null && event.from_realname != null && event.from_realname.length() > 0) {
                        event.formatted_realname = ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(ColorFormatter.emojify(event.from_realname)), true, null);
                    }
                }

                String from = event.from;
                if (from == null || from.length() == 0)
                    from = event.nick;

                if (from != null && event.hostmask != null && event.isMessage() && buffer.getType() != null && !buffer.isConversation()) {
                    String usermask = from + "!" + event.hostmask;
                    if (ignore.match(usermask)) {
                        if (unreadTopView != null && unreadTopView.getVisibility() == View.GONE && unreadBottomView != null && unreadBottomView.getVisibility() == View.GONE) {
                            if (heartbeatTask != null)
                                heartbeatTask.cancel(true);
                            heartbeatTask = new HeartbeatTask();
                            heartbeatTask.execute((Void) null);
                        }
                        return;
                    }
                }

                switch (type) {
                    case "channel_mode":
                        if (event.nick != null && event.nick.length() > 0)
                            event.html = event.msg + " by <b>" + collapsedEvents.formatNick(event.nick, event.from_mode, false) + "</b>";
                        else if (event.server != null && event.server.length() > 0)
                            event.html = event.msg + " by the server <b>" + event.server + "</b>";
                        break;
                    case "buffer_me_msg":
                        event.html = "— <i><b>" + collapsedEvents.formatNick(event.nick, event.from_mode, !event.self && pref_nickColors, ColorScheme.getInstance().selfTextColor) + "</b> " + event.msg + "</i>";
                        break;
                    case "buffer_msg":
                    case "notice":
                        if (pref_chatOneLine && event.from != null && event.from.length() > 0)
                            event.html = "<b>" + collapsedEvents.formatNick(event.from, event.from_mode, !event.self && pref_nickColors, ColorScheme.getInstance().selfTextColor) + "</b> ";
                        else
                            event.html = "";
                        if(event.target_mode != null && server != null && server.PREFIX != null) {
                            if (server.PREFIX.has(server.MODE_OPER) && server.PREFIX.get(server.MODE_OPER) != null && event.target_mode.equals(server.PREFIX.get(server.MODE_OPER).asText()))
                                event.html += collapsedEvents.formatNick("Opers", server.MODE_OPER, false) + " ";
                            else if (server.PREFIX.has(server.MODE_OWNER) && server.PREFIX.get(server.MODE_OWNER) != null && event.target_mode.equals(server.PREFIX.get(server.MODE_OWNER).asText()))
                                event.html += collapsedEvents.formatNick("Owners", server.MODE_OWNER, false) + " ";
                            else if (server.PREFIX.has(server.MODE_ADMIN) && server.PREFIX.get(server.MODE_ADMIN) != null && event.target_mode.equals(server.PREFIX.get(server.MODE_ADMIN).asText()))
                                event.html += collapsedEvents.formatNick("Admins", server.MODE_ADMIN, false) + " ";
                            else if (server.PREFIX.has(server.MODE_OP) && server.PREFIX.get(server.MODE_OP) != null && event.target_mode.equals(server.PREFIX.get(server.MODE_OP).asText()))
                                event.html += collapsedEvents.formatNick("Ops", server.MODE_OP, false) + " ";
                            else if (server.PREFIX.has(server.MODE_HALFOP) && server.PREFIX.get(server.MODE_HALFOP) != null && event.target_mode.equals(server.PREFIX.get(server.MODE_HALFOP).asText()))
                                event.html += collapsedEvents.formatNick("Half Ops", server.MODE_HALFOP, false) + " ";
                            else if (server.PREFIX.has(server.MODE_VOICED) && server.PREFIX.get(server.MODE_VOICED) != null && event.target_mode.equals(server.PREFIX.get(server.MODE_VOICED).asText()))
                                event.html += collapsedEvents.formatNick("Voiced", server.MODE_VOICED, false) + " ";
                        }
                        if (buffer.isConsole() && event.to_chan && event.chan != null && event.chan.length() > 0) {
                            event.html += "<b>" + event.chan + "</b>: " + event.msg;
                        } else if (buffer.isConsole() && event.self && event.nick != null && event.nick.length() > 0) {
                            event.html += "<b>" + event.nick + "</b>: " + event.msg;
                        } else {
                            event.html += event.msg;
                        }
                        break;
                    case "kicked_channel":
                        String collapsedNickColor = Integer.toHexString(ColorScheme.getInstance().collapsedRowNickColor).substring(2);
                        event.html = "\u0004" + collapsedNickColor + "← \u000f";
                        if (event.type.startsWith("you_"))
                            event.html += "You";
                        else
                            event.html += "<b>" + collapsedEvents.formatNick(event.old_nick, null, false, collapsedNickColor) + "</b>";
                        if (event.hostmask != null && event.hostmask.length() > 0)
                            event.html += " (" + event.hostmask + ")";
                        if (event.type.startsWith("you_"))
                            event.html += " were";
                        else
                            event.html += " was";
                        if (event.from_hostmask != null && event.from_hostmask.length() > 0)
                            event.html += " kicked by " + collapsedEvents.formatNick(event.nick, event.from_mode, false, collapsedNickColor);
                        else
                            event.html += " kicked by the server \u0004" + collapsedNickColor + event.nick + "\u000f";
                        if (event.msg != null && event.msg.length() > 0 && !event.msg.equals(event.nick))
                            event.html += ": " + event.msg;
                        break;
                    case "callerid":
                        event.html = "<b>" + collapsedEvents.formatNick(event.from, event.from_mode, false) + "</b> (" + event.hostmask + ") " + event.msg + " Tap to accept.";
                        break;
                    case "channel_mode_list_change":
                        if (event.from.length() == 0) {
                            if (event.nick != null && event.nick.length() > 0)
                                event.html = "<b>" + collapsedEvents.formatNick(event.nick, event.from_mode, false) + "</b> " + event.msg;
                            else if (event.server != null && event.server.length() > 0)
                                event.html = "The server <b>" + event.server + "</b> " + event.msg;
                        }
                        break;
                }

                adapter.addItem(eid, event);
                if (!backlog)
                    adapter.notifyDataSetChanged();

                long time = (System.currentTimeMillis() - start);
                if (avgInsertTime == 0)
                    avgInsertTime = time;
                avgInsertTime += time;
                avgInsertTime /= 2.0;
                //Log.i("IRCCloud", "Average insert time: " + avgInsertTime);
                if (!backlog && buffer.getScrolledUp() && !event.self && event.isImportant(type)) {
                    if (newMsgTime == 0)
                        newMsgTime = System.currentTimeMillis();
                    newMsgs++;
                    if (event.highlight)
                        newHighlights++;
                    update_unread();
                    adapter.insertLastSeenEIDMarker();
                    adapter.notifyDataSetChanged();
                }
                if (!backlog && !buffer.getScrolledUp()) {
                    getListView().setSelection(adapter.getCount() - 1);
                    if (tapTimer != null) {
                        tapTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            getListView().setSelection(adapter.getCount() - 1);
                                        } catch (Exception e) {
                                            //List view isn't ready yet
                                        }
                                    }
                                });
                            }
                        }, 200);
                    }
                }

                if (!backlog && event.highlight && !getActivity().getSharedPreferences("prefs", 0).getBoolean("mentionTip", false)) {
                    Toast.makeText(getActivity(), "Double-tap a message to quickly reply to the sender", Toast.LENGTH_LONG).show();
                    SharedPreferences.Editor editor = getActivity().getSharedPreferences("prefs", 0).edit();
                    editor.putBoolean("mentionTip", true);
                    editor.commit();
                }
                if (!backlog) {
                    int markerPos = adapter.getLastSeenEIDPosition();
                    if (markerPos > 0 && getListView().getFirstVisiblePosition() > markerPos) {
                        unreadTopLabel.setText((getListView().getFirstVisiblePosition() - markerPos) + " unread messages");
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                NetworkConnection.printStackTraceToCrashlytics(e);
            }
        }
    }

    private class OnItemLongClickListener implements View.OnLongClickListener {
        private int pos;

        OnItemLongClickListener(int position) { pos = position; }

        @Override
        public boolean onLongClick(View view) {
            try {
                longPressOverride = mListener.onMessageLongClicked((Event) adapter.getItem(pos));
                return longPressOverride;
            } catch (Exception e) {
            }
            return false;
        }
    }

    private class OnItemClickListener implements OnClickListener {
        private int pos;

        OnItemClickListener(int position) {
            pos = position;
        }

        @Override
        public void onClick(View arg0) {
            longPressOverride = false;

            if (pos < 0 || pos >= adapter.data.size())
                return;

            if(tapTimer == null)
                tapTimer = new Timer("message-tap-timer");

            if (adapter != null) {
                if (tapTimerTask != null) {
                    tapTimerTask.cancel();
                    tapTimerTask = null;
                    mListener.onMessageDoubleClicked(adapter.data.get(pos));
                } else {
                    tapTimerTask = new TimerTask() {
                        int position = pos;

                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (adapter != null && adapter.data != null && position < adapter.data.size()) {
                                        Event e = adapter.data.get(position);
                                        if (e != null) {
                                            if (e.type.equals("channel_invite")) {
                                                conn.join(buffer.getCid(), e.old_nick, null);
                                            } else if (e.type.equals("callerid")) {
                                                conn.say(buffer.getCid(), null, "/accept " + e.from);
                                                Buffer b = BuffersList.getInstance().getBufferByName(buffer.getCid(), e.from);
                                                if (b != null) {
                                                    mListener.onBufferSelected(b.getBid());
                                                } else {
                                                    conn.say(buffer.getCid(), null, "/query " + e.from);
                                                }
                                            } else if (e.failed) {
                                                if(mListener != null)
                                                    mListener.onFailedMessageClicked(e);
                                            } else {
                                                long group = e.group_eid;
                                                if (expandedSectionEids.contains(group))
                                                    expandedSectionEids.remove(group);
                                                else if (e.eid != group)
                                                    expandedSectionEids.add(group);
                                                if (e.eid != e.group_eid) {
                                                    adapter.clearLastSeenEIDMarker();
                                                    if (refreshTask != null)
                                                        refreshTask.cancel(true);
                                                    refreshTask = new RefreshTask();
                                                    refreshTask.execute((Void) null);
                                                }
                                            }
                                        }
                                    }
                                }
                            });
                            tapTimerTask = null;
                        }
                    };
                    tapTimer.schedule(tapTimerTask, 300);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void onResume() {
        super.onResume();
        getListView().setOnScrollListener(mOnScrollListener);
        update_global_msg();
        if (buffer != null && adapter != null) {
            if(buffer.getUnread() == 0 && !buffer.getScrolledUp())
                adapter.clearLastSeenEIDMarker();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getListView().setAdapter(null);
    }

    @Override
    public void onStop() {
        if(headerViewContainer != null)
            headerViewContainer.setVisibility(View.GONE);
        if(footerViewContainer != null)
            footerViewContainer.setVisibility(View.GONE);
        super.onStop();
    }

    @Override
    public void onStart() {
        if(headerViewContainer != null)
            headerViewContainer.setVisibility(View.VISIBLE);
        if(footerViewContainer != null)
            footerViewContainer.setVisibility(View.VISIBLE);
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RefWatcher refWatcher = IRCCloudApplication.getRefWatcher(getActivity());
        if (refWatcher != null)
            refWatcher.watch(this);
        if (tapTimer != null) {
            tapTimer.cancel();
            tapTimer = null;
        }
        if(conn != null)
            conn.removeHandler(this);
        mListener = null;
        heartbeatTask = null;
    }

    private class HeartbeatTask extends AsyncTaskEx<Void, Void, Void> {
        Buffer b;

        public HeartbeatTask() {
            b = buffer;
            /*if(buffer != null)
                Log.d("IRCCloud", "Heartbeat task created. Ready: " + ready + " BID: " + buffer.getBid());
            Thread.dumpStack();*/
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
            }

            if (isCancelled() || !conn.ready || conn.getState() != NetworkConnection.STATE_CONNECTED || b == null || !ready || requestingBacklog || adapter.getCount() == 0)
                return null;

            if (getActivity() != null) {
                try {
                    DrawerLayout drawerLayout = (DrawerLayout) getActivity().findViewById(R.id.drawerLayout);

                    if (drawerLayout != null && (drawerLayout.isDrawerOpen(Gravity.LEFT) || drawerLayout.isDrawerOpen(Gravity.RIGHT)))
                        return null;
                } catch (Exception e) {
                }
            }

            if (unreadTopView.getVisibility() == View.VISIBLE || unreadBottomView.getVisibility() == View.VISIBLE)
                return null;

            try {
                Long eid = EventsList.getInstance().lastEidForBuffer(b.getBid());

                if (eid >= b.getLast_seen_eid() && conn != null && conn.getState() == NetworkConnection.STATE_CONNECTED) {
                    if (getActivity() != null && getActivity().getIntent() != null)
                        getActivity().getIntent().putExtra("last_seen_eid", eid);
                    NetworkConnection.getInstance().heartbeat(b.getCid(), b.getBid(), eid);
                    b.setLast_seen_eid(eid);
                    b.setUnread(0);
                    b.setHighlights(0);
                    //Log.e("IRCCloud", "Heartbeat: " + buffer.name + ": " + events.get(events.lastKey()).msg);
                }
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled())
                heartbeatTask = null;
        }
    }

    private class FormatTask extends AsyncTaskEx<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            adapter.format();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }

    private class RefreshTask extends AsyncTaskEx<Void, Void, Void> {
        private MessageAdapter adapter;

        TreeMap<Long, Event> events;
        Buffer buffer;
        int oldPosition = -1;
        int topOffset = -1;

        @Override
        protected void onPreExecute() {
            //Debug.startMethodTracing("refresh");
            try {
                oldPosition = getListView().getFirstVisiblePosition();
                View v = getListView().getChildAt(0);
                topOffset = (v == null) ? 0 : v.getTop();
                buffer = MessageViewFragment.this.buffer;
            } catch (IllegalStateException e) {
                //The list view isn't on screen anymore
                cancel(true);
                refreshTask = null;
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        protected Void doInBackground(Void... params) {
            TreeMap<Long, Event> evs = null;
            long time = System.currentTimeMillis();
            if (buffer != null)
                evs = EventsList.getInstance().getEventsForBuffer(buffer.getBid());
            Log.i("IRCCloud", "Loaded data in " + (System.currentTimeMillis() - time) + "ms");
            if (!isCancelled() && evs != null && evs.size() > 0) {
                try {
                    events = (TreeMap<Long, Event>) evs.clone();
                } catch (Exception e) {
                    NetworkConnection.printStackTraceToCrashlytics(e);
                    return null;
                }
                if (isCancelled())
                    return null;

                if (events != null) {
                    try {
                        if (events.size() > 0 && MessageViewFragment.this.adapter != null && MessageViewFragment.this.adapter.data.size() > 0 && earliest_eid > events.firstKey()) {
                            if (oldPosition > 0 && oldPosition == MessageViewFragment.this.adapter.data.size())
                                oldPosition--;
                            Event e = MessageViewFragment.this.adapter.data.get(oldPosition);
                            if (e != null)
                                backlog_eid = e.group_eid - 1;
                            else
                                backlog_eid = -1;
                            if (backlog_eid < 0) {
                                backlog_eid = MessageViewFragment.this.adapter.getItemId(oldPosition) - 1;
                            }
                            Event backlogMarker = new Event();
                            backlogMarker.eid = backlog_eid;
                            backlogMarker.type = TYPE_BACKLOGMARKER;
                            backlogMarker.row_type = ROW_BACKLOGMARKER;
                            backlogMarker.html = "__backlog__";
                            backlogMarker.bg_color = colorScheme.contentBackgroundColor;
                            events.put(backlog_eid, backlogMarker);
                        }
                        adapter = new MessageAdapter(MessageViewFragment.this, events.size());
                        refresh(adapter, events);
                    } catch (IllegalStateException e) {
                        //The list view doesn't exist yet
                        NetworkConnection.printStackTraceToCrashlytics(e);
                        Log.e("IRCCloud", "Tried to refresh the message list, but it didn't exist.");
                    } catch (Exception e) {
                        NetworkConnection.printStackTraceToCrashlytics(e);
                        return null;
                    }
                }
            } else if (buffer != null && buffer.getMin_eid() > 0 && conn.ready && conn.getState() == NetworkConnection.STATE_CONNECTED && !isCancelled()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        headerView.setVisibility(View.GONE);
                        backlogFailed.setVisibility(View.GONE);
                        loadBacklogButton.setVisibility(View.VISIBLE);
                        adapter = new MessageAdapter(MessageViewFragment.this, 0);
                        setListAdapter(adapter);
                        MessageViewFragment.this.adapter = adapter;
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        headerView.setVisibility(View.GONE);
                        backlogFailed.setVisibility(View.GONE);
                        loadBacklogButton.setVisibility(View.GONE);
                        adapter = new MessageAdapter(MessageViewFragment.this, 0);
                        setListAdapter(adapter);
                        MessageViewFragment.this.adapter = adapter;
                    }
                });
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            if (!isCancelled() && adapter != null) {
                try {
                    avatarContainer.setVisibility(View.GONE);
                    avatar.setTag(null);
                    if (adapter.getLastSeenEIDPosition() == 0)
                        headerViewContainer.setPadding(0,(int)getSafeResources().getDimension(R.dimen.top_bar_height),0,0);
                    else
                        headerViewContainer.setPadding(0,0,0,0);
                    setListAdapter(adapter);
                    MessageViewFragment.this.adapter = adapter;
                    if (events != null && events.size() > 0) {
                        int markerPos = adapter.getBacklogMarkerPosition();
                        if (markerPos != -1 && requestingBacklog)
                            getListView().setSelectionFromTop(oldPosition + markerPos + 1, headerViewContainer.getHeight());
                        else if (!buffer.getScrolledUp())
                            getListView().setSelection(adapter.getCount() - 1);
                        else {
                            getListView().setSelectionFromTop(buffer.getScrollPosition(), buffer.getScrollPositionOffset());

                            if (adapter.getLastSeenEIDPosition() > buffer.getScrollPosition()) {
                                newMsgs = 0;
                                newHighlights = 0;

                                for (int i = adapter.data.size() - 1; i >= 0; i--) {
                                    Event e = adapter.data.get(i);
                                    if (e.eid <= buffer.getLast_seen_eid())
                                        break;

                                    if (e.isImportant(buffer.getType())) {
                                        if (e.highlight)
                                            newHighlights++;
                                        else
                                            newMsgs++;
                                    }
                                }
                            }

                            update_unread();
                        }
                    }
                    new FormatTask().execute((Void) null);
                } catch (IllegalStateException e) {
                    //The list view isn't on screen anymore
                    NetworkConnection.printStackTraceToCrashlytics(e);
                }
                refreshTask = null;
                requestingBacklog = false;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            update_top_unread(getListView().getFirstVisiblePosition());
                        } catch (IllegalStateException e) {
                            //List view not ready yet
                        }
                        if (server != null)
                            update_status(server.getStatus(), server.getFail_info());
                        if (mListener != null && !ready)
                            mListener.onMessageViewReady();
                        ready = true;
                        try {
                            ListView v = getListView();
                            mOnScrollListener.onScroll(v, v.getFirstVisiblePosition(), v.getLastVisiblePosition() - v.getFirstVisiblePosition(), adapter.getCount());
                        } catch (Exception e) {
                        }
                    }
                }, 250);
                //Debug.stopMethodTracing();
            }
        }
    }

    private synchronized void refresh(MessageAdapter adapter, TreeMap<Long, Event> events) {
        pref_24hr = false;
        pref_seconds = false;
        pref_trackUnread = true;
        pref_timeLeft = false;
        pref_nickColors = false;
        pref_hideJoinPart = false;
        pref_expandJoinPart = false;
        pref_avatarsOff = false;
        pref_chatOneLine = false;
        pref_norealname = false;
        pref_compact = false;
        if (NetworkConnection.getInstance().getUserInfo() != null && NetworkConnection.getInstance().getUserInfo().prefs != null) {
            try {
                JSONObject prefs = NetworkConnection.getInstance().getUserInfo().prefs;
                pref_compact = (prefs.has("ascii-compact") && prefs.get("ascii-compact") instanceof Boolean && prefs.getBoolean("ascii-compact"));
                pref_24hr = (prefs.has("time-24hr") && prefs.get("time-24hr") instanceof Boolean && prefs.getBoolean("time-24hr"));
                pref_seconds = (prefs.has("time-seconds") && prefs.get("time-seconds") instanceof Boolean && prefs.getBoolean("time-seconds"));
                pref_timeLeft = !PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("time-left", true);
                pref_nickColors = (prefs.has("nick-colors") && prefs.get("nick-colors") instanceof Boolean && prefs.getBoolean("nick-colors"));
                pref_avatarsOff = !PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("avatars-off", true);
                pref_chatOneLine = !PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("chat-oneline", true);
                pref_norealname = !PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("chat-norealname", true);
                if(prefs.has("channel-disableTrackUnread")) {
                    JSONObject disabledMap = prefs.getJSONObject("channel-disableTrackUnread");
                    if (disabledMap.has(String.valueOf(buffer.getBid())) && disabledMap.getBoolean(String.valueOf(buffer.getBid()))) {
                        pref_trackUnread = false;
                    }
                }
                JSONObject hiddenMap = null;
                if (buffer.isChannel()) {
                    if (prefs.has("channel-hideJoinPart"))
                        hiddenMap = prefs.getJSONObject("channel-hideJoinPart");
                } else {
                    if (prefs.has("buffer-hideJoinPart"))
                        hiddenMap = prefs.getJSONObject("buffer-hideJoinPart");
                }

                pref_hideJoinPart = (prefs.has("hideJoinPart") && prefs.get("hideJoinPart") instanceof Boolean && prefs.getBoolean("hideJoinPart")) || (hiddenMap != null && hiddenMap.has(String.valueOf(buffer.getBid())) && hiddenMap.getBoolean(String.valueOf(buffer.getBid())));

                JSONObject expandMap = null;
                if (buffer.isChannel()) {
                    if (prefs.has("channel-expandJoinPart"))
                        expandMap = prefs.getJSONObject("channel-expandJoinPart");
                } else if (buffer.isConsole()) {
                    if (prefs.has("buffer-expandDisco"))
                        expandMap = prefs.getJSONObject("buffer-expandDisco");
                } else {
                    if (prefs.has("buffer-expandJoinPart"))
                        expandMap = prefs.getJSONObject("buffer-expandJoinPart");
                }

                pref_expandJoinPart = ((prefs.has("expandJoinPart") && prefs.get("expandJoinPart") instanceof Boolean && prefs.getBoolean("expandJoinPart")) || (expandMap != null && expandMap.has(String.valueOf(buffer.getBid())) && expandMap.getBoolean(String.valueOf(buffer.getBid()))));
            } catch (JSONException e1) {
                NetworkConnection.printStackTraceToCrashlytics(e1);
            }
        }
        synchronized (adapterLock) {
            if (getActivity() != null)
                textSize = PreferenceManager.getDefaultSharedPreferences(getActivity()).getInt("textSize", getActivity().getResources().getInteger(R.integer.default_text_size));
            timestamp_width = -1;
            if (conn.getReconnectTimestamp() == 0)
                conn.cancel_idle_timer(); //This may take a while...
            collapsedEvents.clear();
            currentCollapsedEid = -1;
            lastCollapsedDay = -1;

            if (events == null || (events.size() == 0 && buffer.getMin_eid() > 0)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        headerView.setVisibility(View.GONE);
                        backlogFailed.setVisibility(View.GONE);
                        loadBacklogButton.setVisibility(View.GONE);
                    }
                });
            } else if (events.size() > 0) {
                if (server != null) {
                    ignore.setIgnores(server.ignores);
                } else {
                    ignore.setIgnores(null);
                }
                collapsedEvents.setServer(server);
                earliest_eid = events.firstKey();
                if (events.size() > 0) {
                    avgInsertTime = 0;
                    //Debug.startMethodTracing("refresh");
                    long start = System.currentTimeMillis();
                    Iterator<Event> i = events.values().iterator();
                    Event next = i.next();
                    Calendar calendar = Calendar.getInstance();
                    while (next != null) {
                        Event e = next;
                        next = i.hasNext() ? i.next() : null;
                        String type = (next == null) ? "" : next.type;

                        if (next != null && currentCollapsedEid != -1 && !expandedSectionEids.contains(currentCollapsedEid) && (type.equalsIgnoreCase("joined_channel") || type.equalsIgnoreCase("parted_channel") || type.equalsIgnoreCase("nickchange") || type.equalsIgnoreCase("quit") || type.equalsIgnoreCase("user_channel_mode"))) {
                            calendar.setTimeInMillis(next.getTime());
                            insertEvent(adapter, e, true, calendar.get(Calendar.DAY_OF_YEAR) == lastCollapsedDay);
                        } else {
                            insertEvent(adapter, e, true, false);
                        }
                    }
                    adapter.insertLastSeenEIDMarker();
                    Log.i("IRCCloud", "Backlog rendering took: " + (System.currentTimeMillis() - start) + "ms");
                    //Debug.stopMethodTracing();
                    avgInsertTime = 0;
                    //adapter.notifyDataSetChanged();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            headerView.setVisibility(View.GONE);
                            if(buffer.getMin_eid() > 0 && earliest_eid > buffer.getMin_eid() && conn.ready && conn.getState() == NetworkConnection.STATE_CONNECTED)
                                loadBacklogButton.setVisibility(View.VISIBLE);
                            else
                                loadBacklogButton.setVisibility(View.GONE);
                        }
                    });
                }
            }
            if (conn.getReconnectTimestamp() == 0 && conn.getState() == NetworkConnection.STATE_CONNECTED)
                conn.schedule_idle_timer();
        }
    }

    private void update_top_unread(int first) {
        if (adapter != null && buffer != null) {
            try {
                int markerPos = adapter.getLastSeenEIDPosition();
                if (markerPos >= 0 && ((first > (markerPos + 1) && buffer.getUnread() > 0) || adapter.getCount() == 0)) {
                    if (pref_trackUnread) {
                        int highlights = adapter.getUnreadHighlightsAbovePosition(first);
                        int count = (first - markerPos - 1) - highlights;
                        StringBuilder txt = new StringBuilder();
                        if (highlights > 0) {
                            if (highlights == 1)
                                txt.append("mention");
                            else if (highlights > 0)
                                txt.append("mentions");
                            highlightsTopLabel.setText(String.valueOf(highlights));
                            highlightsTopLabel.setVisibility(View.VISIBLE);

                            if (count > 0)
                                txt.append(" and ");
                        } else {
                            highlightsTopLabel.setVisibility(View.GONE);
                        }
                        if (markerPos == 0) {
                            long seconds = (long) Math.ceil((earliest_eid - buffer.getLast_seen_eid()) / 1000000.0);
                            if (seconds <= 0) {
                                if (count < 0) {
                                    hideView(unreadTopView);
                                    return;
                                } else {
                                    if (count == 1)
                                        txt.append(count).append(" unread message");
                                    else if (count > 0)
                                        txt.append(count).append(" unread messages");
                                }
                            } else {
                                int minutes = (int) Math.ceil(seconds / 60.0);
                                int hours = (int) Math.ceil(seconds / 60.0 / 60.0);
                                int days = (int) Math.ceil(seconds / 60.0 / 60.0 / 24.0);
                                if (hours >= 24) {
                                    if (days == 1)
                                        txt.append(days).append(" day of unread messages");
                                    else
                                        txt.append(days).append(" days of unread messages");
                                } else if (hours > 0) {
                                    if (hours == 1)
                                        txt.append(hours).append(" hour of unread messages");
                                    else
                                        txt.append(hours).append(" hours of unread messages");
                                } else if (minutes > 0) {
                                    if (minutes == 1)
                                        txt.append(minutes).append(" minute of unread messages");
                                    else
                                        txt.append(minutes).append(" minutes of unread messages");
                                } else {
                                    if (seconds == 1)
                                        txt.append(seconds).append(" second of unread messages");
                                    else
                                        txt.append(seconds).append(" seconds of unread messages");
                                }
                            }
                        } else {
                            if (count == 1)
                                txt.append(count).append(" unread message");
                            else if (count > 0)
                                txt.append(count).append(" unread messages");
                        }
                        unreadTopLabel.setText(txt);
                        showView(unreadTopView);
                    } else {
                        hideView(unreadTopView);
                    }
                } else {
                    if (markerPos > 0) {
                        hideView(unreadTopView);
                        if (adapter.data.size() > 0 && ready) {
                            if (heartbeatTask != null)
                                heartbeatTask.cancel(true);
                            heartbeatTask = new HeartbeatTask();
                            heartbeatTask.execute((Void) null);
                        }
                    }
                }
            } catch (IllegalStateException e) {
                //The list view wasn't on screen yet
                NetworkConnection.printStackTraceToCrashlytics(e);
            }
        }
    }

    private class UnreadRefreshRunnable implements Runnable {
        @Override
        public void run() {
            update_unread();
        }
    }

    UnreadRefreshRunnable unreadRefreshRunnable = null;

    private void update_unread() {
        if (unreadRefreshRunnable != null) {
            mHandler.removeCallbacks(unreadRefreshRunnable);
            unreadRefreshRunnable = null;
        }

        if (newMsgs > 0) {
            /*int minutes = (int)((System.currentTimeMillis() - newMsgTime)/60000);

			if(minutes < 1)
				unreadBottomLabel.setText("Less than a minute of chatter (");
			else if(minutes == 1)
				unreadBottomLabel.setText("1 minute of chatter (");
			else
				unreadBottomLabel.setText(minutes + " minutes of chatter (");
			if(newMsgs == 1)
				unreadBottomLabel.setText(unreadBottomLabel.getText() + "1 message)");
			else
				unreadBottomLabel.setText(unreadBottomLabel.getText() + (newMsgs + " messages)"));*/

            String txt = "";
            int msgCnt = newMsgs - newHighlights;
            if (newHighlights > 0) {
                if (newHighlights == 1)
                    txt = "mention";
                else
                    txt = "mentions";
                if (msgCnt > 0)
                    txt += " and ";
                highlightsBottomLabel.setText(String.valueOf(newHighlights));
                highlightsBottomLabel.setVisibility(View.VISIBLE);
            } else {
                highlightsBottomLabel.setVisibility(View.GONE);
            }
            if (msgCnt == 1)
                txt += msgCnt + " unread message";
            else if (msgCnt > 0)
                txt += msgCnt + " unread messages";
            unreadBottomLabel.setText(txt);
            showView(unreadBottomView);
            unreadRefreshRunnable = new UnreadRefreshRunnable();
            mHandler.postDelayed(unreadRefreshRunnable, 10000);
        }
    }

    private class StatusRefreshRunnable implements Runnable {
        String status;
        JsonNode fail_info;

        public StatusRefreshRunnable(String status, JsonNode fail_info) {
            this.status = status;
            this.fail_info = fail_info;
        }

        @Override
        public void run() {
            update_status(status, fail_info);
        }
    }

    StatusRefreshRunnable statusRefreshRunnable = null;

    public static String ordinal(int i) {
        String[] sufixes = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + sufixes[i % 10];

        }
    }

    private String reason_txt(String reason) {
        String r = reason;
        switch (reason.toLowerCase()) {
            case "pool_lost":
                r = "Connection pool failed";
            case "no_pool":
                r = "No available connection pools";
                break;
            case "enetdown":
                r = "Network down";
                break;
            case "etimedout":
            case "timeout":
                r = "Timed out";
                break;
            case "ehostunreach":
                r = "Host unreachable";
                break;
            case "econnrefused":
                r = "Connection refused";
                break;
            case "nxdomain":
            case "einval":
                r = "Invalid hostname";
                break;
            case "server_ping_timeout":
                r = "PING timeout";
                break;
            case "ssl_certificate_error":
                r = "SSL certificate error";
                break;
            case "ssl_error":
                r = "SSL error";
                break;
            case "crash":
                r = "Connection crashed";
                break;
            case "networks":
                r = "You've exceeded the connection limit for free accounts.";
                break;
            case "passworded_servers":
                r = "You can't connect to passworded servers with free accounts.";
                break;
            case "unverified":
                r = "You can’t connect to external servers until you confirm your email address.";
                break;
        }
        return r;
    }

    private void update_status(String status, JsonNode fail_info) {
        if (statusRefreshRunnable != null) {
            mHandler.removeCallbacks(statusRefreshRunnable);
            statusRefreshRunnable = null;
        }

        statusView.setTextColor(colorScheme.connectionBarTextColor);
        statusView.setBackgroundColor(colorScheme.connectionBarColor);

        switch (status) {
            case "connected_ready":
                statusView.setVisibility(View.GONE);
                statusView.setText("");
                break;
            case "quitting":
                statusView.setVisibility(View.VISIBLE);
                statusView.setText("Disconnecting");
                break;
            case "disconnected":
                statusView.setVisibility(View.VISIBLE);
                if (fail_info.has("type") && fail_info.get("type").asText().length() > 0) {
                    String text = "Disconnected: ";
                    if (fail_info.get("type").asText().equals("connecting_restricted")) {
                        text = reason_txt(fail_info.get("reason").asText());
                        if (text.equals(fail_info.get("reason").asText()))
                            text = "You can’t connect to this server with a free account.";
                    } else if (fail_info.get("type").asText().equals("connection_blocked")) {
                        text = "Disconnected - Connections to this server have been blocked";
                    } else {
                        if (fail_info.has("type") && fail_info.get("type").asText().equals("killed"))
                            text = "Disconnected - Killed: ";
                        else if (fail_info.has("type") && fail_info.get("type").asText().equals("connecting_failed"))
                            text = "Disconnected: Failed to connect - ";
                        if (fail_info.has("reason"))
                            text += reason_txt(fail_info.get("reason").asText());
                    }
                    statusView.setText(text);
                    statusView.setTextColor(colorScheme.networkErrorColor);
                    statusView.setBackgroundColor(colorScheme.networkErrorBackgroundColor);
                } else {
                    statusView.setText("Disconnected. Tap to reconnect.");
                }
                break;
            case "queued":
                statusView.setVisibility(View.VISIBLE);
                statusView.setText("Connection queued");
                break;
            case "connecting":
                statusView.setVisibility(View.VISIBLE);
                statusView.setText("Connecting");
                break;
            case "connected":
                statusView.setVisibility(View.VISIBLE);
                statusView.setText("Connected");
                break;
            case "connected_joining":
                statusView.setVisibility(View.VISIBLE);
                statusView.setText("Connected: Joining Channels");
                break;
            case "pool_unavailable":
                statusView.setVisibility(View.VISIBLE);
                statusView.setText("Connection temporarily unavailable");
                statusView.setTextColor(colorScheme.networkErrorColor);
                statusView.setBackgroundColor(colorScheme.networkErrorBackgroundColor);
                break;
            case "waiting_to_retry":
                try {
                    statusView.setVisibility(View.VISIBLE);
                    long seconds = (fail_info.get("timestamp").asLong() + fail_info.get("retry_timeout").asLong() - conn.clockOffset) - System.currentTimeMillis() / 1000;
                    if (seconds > 0) {
                        String text = "Disconnected";
                        if (fail_info.has("reason") && fail_info.get("reason").asText().length() > 0) {
                            String reason = fail_info.get("reason").asText();
                            reason = reason_txt(reason);
                            text += ": " + reason + ". ";
                        } else
                            text += "; ";
                        text += "Reconnecting in ";
                        int minutes = (int) (seconds / 60.0);
                        int hours = (int) (seconds / 60.0 / 60.0);
                        int days = (int) (seconds / 60.0 / 60.0 / 24.0);
                        if (days > 0) {
                            if (days == 1)
                                text += days + " day.";
                            else
                                text += days + " days.";
                        } else if (hours > 0) {
                            if (hours == 1)
                                text += hours + " hour.";
                            else
                                text += hours + " hours.";
                        } else if (minutes > 0) {
                            if (minutes == 1)
                                text += minutes + " minute.";
                            else
                                text += minutes + " minutes.";
                        } else {
                            if (seconds == 1)
                                text += seconds + " second.";
                            else
                                text += seconds + " seconds.";
                        }
                        int attempts = fail_info.get("attempts").asInt();
                        if (attempts > 1)
                            text += " (" + ordinal(attempts) + " attempt)";
                        statusView.setText(text);
                        statusView.setTextColor(colorScheme.networkErrorColor);
                        statusView.setBackgroundColor(colorScheme.networkErrorBackgroundColor);
                        statusRefreshRunnable = new StatusRefreshRunnable(status, fail_info);
                    } else {
                        statusView.setVisibility(View.VISIBLE);
                        statusView.setText("Ready to connect, waiting our turn…");
                    }
                    mHandler.postDelayed(statusRefreshRunnable, 500);
                } catch (Exception e) {
                    NetworkConnection.printStackTraceToCrashlytics(e);
                }
                break;
            case "ip_retry":
                statusView.setVisibility(View.VISIBLE);
                statusView.setText("Trying another IP address");
                break;
        }

    }

    private void update_global_msg() {
        if (globalMsgView != null) {
            if (conn != null && conn.globalMsg != null) {
                globalMsg.setText(Html.fromHtml(conn.globalMsg));
                globalMsgView.setVisibility(View.VISIBLE);
            } else {
                globalMsgView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if(getActivity() == null || !((BaseActivity)getActivity()).isMultiWindow()) {
            if (statusRefreshRunnable != null) {
                mHandler.removeCallbacks(statusRefreshRunnable);
                statusRefreshRunnable = null;
            }
            try {
                getListView().setOnScrollListener(null);
            } catch (Exception e) {
            }
            ready = false;
        }
    }

    public void onIRCEvent(int what, final Object obj) {
        IRCCloudJSONObject e;

        switch (what) {
            case NetworkConnection.EVENT_BACKLOG_FAILED:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        headerView.setVisibility(View.GONE);
                        backlogFailed.setVisibility(View.VISIBLE);
                        loadBacklogButton.setVisibility(View.VISIBLE);
                    }
                });
                break;
            case NetworkConnection.EVENT_BACKLOG_END:
            case NetworkConnection.EVENT_CONNECTIVITY:
            case NetworkConnection.EVENT_USERINFO:
                if(buffer != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (refreshTask != null)
                                refreshTask.cancel(true);
                            refreshTask = new RefreshTask();
                            refreshTask.execute((Void) null);
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_CONNECTIONLAG:
                try {
                    IRCCloudJSONObject object = (IRCCloudJSONObject) obj;
                    if (server != null && buffer != null && object.cid() == buffer.getCid()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                update_status(server.getStatus(), server.getFail_info());
                            }
                        });
                    }
                } catch (Exception ex) {
                    NetworkConnection.printStackTraceToCrashlytics(ex);
                }
                break;
            case NetworkConnection.EVENT_STATUSCHANGED:
                try {
                    final IRCCloudJSONObject object = (IRCCloudJSONObject) obj;
                    if (buffer != null && object.cid() == buffer.getCid()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                update_status(object.getString("new_status"), object.getJsonObject("fail_info"));
                            }
                        });
                    }
                } catch (Exception ex) {
                    NetworkConnection.printStackTraceToCrashlytics(ex);
                }
                break;
            case NetworkConnection.EVENT_SETIGNORES:
                e = (IRCCloudJSONObject) obj;
                if (buffer != null && e.cid() == buffer.getCid()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (refreshTask != null)
                                refreshTask.cancel(true);
                            refreshTask = new RefreshTask();
                            refreshTask.execute((Void) null);
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_HEARTBEATECHO:
                try {
                    if (buffer != null && adapter != null && adapter.data.size() > 0) {
                        if (buffer.getLast_seen_eid() == adapter.data.get(adapter.data.size() - 1).eid || !pref_trackUnread) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hideView(unreadTopView);
                                }
                            });
                        }
                    }
                } catch (Exception ex) {
                }
                break;
            case NetworkConnection.EVENT_CHANNELTOPIC:
            case NetworkConnection.EVENT_JOIN:
            case NetworkConnection.EVENT_PART:
            case NetworkConnection.EVENT_NICKCHANGE:
            case NetworkConnection.EVENT_QUIT:
            case NetworkConnection.EVENT_KICK:
            case NetworkConnection.EVENT_CHANNELMODE:
            case NetworkConnection.EVENT_USERMODE:
            case NetworkConnection.EVENT_USERCHANNELMODE:
                e = (IRCCloudJSONObject) obj;
                if (buffer != null && e.bid() == buffer.getBid()) {
                    final Event event = EventsList.getInstance().getEvent(e.eid(), e.bid());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (adapter != null)
                                insertEvent(adapter, event, false, false);
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_SELFDETAILS:
            case NetworkConnection.EVENT_BUFFERMSG:
                final Event event = (Event) obj;
                if (buffer != null && event.bid == buffer.getBid()) {
                    if (event.from != null && event.from.equalsIgnoreCase(buffer.getName()) && event.reqid == -1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (adapter != null)
                                    adapter.clearPending();
                            }
                        });
                    } else if (event.reqid != -1) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (adapter != null && adapter.data != null) {
                                    for (int i = 0; i < adapter.data.size(); i++) {
                                        Event e = adapter.data.get(i);
                                        if (e.reqid == event.reqid && e.pending) {
                                            if (i > 0) {
                                                Event p = adapter.data.get(i - 1);
                                                if (p.row_type == ROW_TIMESTAMP) {
                                                    adapter.data.remove(p);
                                                    i--;
                                                }
                                            }
                                            adapter.data.remove(e);
                                            i--;
                                        }
                                    }
                                }
                            }
                        });
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            insertEvent(adapter, event, false, false);
                        }
                    });
                    if (event.pending && event.self && adapter != null && getListView() != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                getListView().setSelection(adapter.getCount() - 1);
                            }
                        });
                    }
                }
                Buffer b = BuffersList.getInstance().getBuffer(event.bid);
                if (b != null && !b.getScrolledUp() && EventsList.getInstance().getSizeOfBuffer(b.getBid()) > 200) {
                    EventsList.getInstance().pruneEvents(b.getBid());
                    if (buffer != null && b.getBid() == buffer.getBid()) {
                        if (b.getLast_seen_eid() < event.eid && unreadTopView.getVisibility() == View.GONE)
                            b.setLast_seen_eid(event.eid);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (refreshTask != null)
                                    refreshTask.cancel(true);
                                refreshTask = new RefreshTask();
                                refreshTask.execute((Void) null);
                            }
                        });
                    }
                }
                break;
            case NetworkConnection.EVENT_AWAY:
            case NetworkConnection.EVENT_SELFBACK:
                if (server != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (server.getAway() != null && server.getAway().length() > 0) {
                                awayTxt.setText(ColorFormatter.html_to_spanned(ColorFormatter.irc_to_html(TextUtils.htmlEncode("Away (" + server.getAway() + ")"))).toString());
                                awayView.setVisibility(View.VISIBLE);
                            } else {
                                awayView.setVisibility(View.GONE);
                            }
                        }
                    });
                }
                break;
            case NetworkConnection.EVENT_GLOBALMSG:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        update_global_msg();
                    }
                });
                break;
            default:
                break;
        }
    }

    @Override
    public void onIRCRequestSucceeded(int reqid, IRCCloudJSONObject object) {
    }

    @Override
    public void onIRCRequestFailed(int reqid, IRCCloudJSONObject object) {
    }

    public static Resources getSafeResources() {
        return IRCCloudApplication.getInstance().getApplicationContext().getResources();
    }

    public interface MessageViewListener extends OnBufferSelectedListener {
        public void onMessageViewReady();

        public boolean onMessageLongClicked(Event event);

        public void onMessageDoubleClicked(Event event);

        public void onFailedMessageClicked(Event event);
    }
}
