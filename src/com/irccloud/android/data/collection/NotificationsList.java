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

package com.irccloud.android.data.collection;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.SQLException;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.crashlytics.android.Crashlytics;
import com.irccloud.android.ColorFormatter;
import com.irccloud.android.DashClock;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.RemoteInputService;
import com.irccloud.android.SonyExtensionService;
import com.irccloud.android.activity.QuickReplyActivity;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.model.Notification;
import com.irccloud.android.data.model.Notification$Table;
import com.irccloud.android.data.model.Notification_LastSeenEID;
import com.irccloud.android.data.model.Notification_LastSeenEID$Table;
import com.raizlabs.android.dbflow.runtime.TransactionManager;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NotificationsList {
    private static NotificationsList instance = null;
    private int excludeBid = -1;
    private static final Timer mNotificationTimer = new Timer("notification-timer");
    private TimerTask mNotificationTimerTask = null;
    private final Object dbLock = new Object();

    public static NotificationsList getInstance() {
        if (instance == null)
            instance = new NotificationsList();
        return instance;
    }

    public List<Notification> getNotifications() {
        synchronized (dbLock) {
            return new Select().all().from(Notification.class).where().orderBy(Notification$Table.BID + ", " + Notification$Table.EID).queryList();
        }
    }

    public void clear() {
        try {
            for (Notification n : getNotifications()) {
                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int) (n.eid / 1000));
                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(n.bid);
            }
            IRCCloudApplication.getInstance().getApplicationContext().sendBroadcast(new Intent(DashClock.REFRESH_INTENT));
            try {
                if (PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("notify_sony", false))
                    NotificationUtil.deleteAllEvents(IRCCloudApplication.getInstance().getApplicationContext());
            } catch (Exception e) {
                //Sony LiveWare was probably removed
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateTeslaUnreadCount();
        synchronized (dbLock) {
            Delete.table(Notification.class);
        }
    }

    public void clearLastSeenEIDs() {
        synchronized (dbLock) {
            Delete.table(Notification_LastSeenEID.class);
        }
    }

    public long getLastSeenEid(int bid) {
        Buffer b = BuffersList.getInstance().getBuffer(bid);
        if(b != null)
            return b.getLast_seen_eid();
        synchronized (dbLock) {
            Notification_LastSeenEID eid = new Select().from(Notification_LastSeenEID.class).where(Condition.column(Notification_LastSeenEID$Table.BID).is(bid)).querySingle();
            if (eid != null)
                return eid.eid;
            else
                return -1;
        }
    }

    public synchronized void updateLastSeenEid(int bid, long eid) {
        Notification_LastSeenEID n = new Notification_LastSeenEID();
        n.bid = bid;
        n.eid = eid;
        synchronized (dbLock) {
            n.save();
        }
    }

    public synchronized void dismiss(int bid, long eid) {
        synchronized (dbLock) {
            Notification n = getNotification(eid);
            if (n != null)
                n.delete();
        }
        if (IRCCloudApplication.getInstance() != null)
            IRCCloudApplication.getInstance().getApplicationContext().sendBroadcast(new Intent(DashClock.REFRESH_INTENT));
        updateTeslaUnreadCount();
    }

    public synchronized void addNotification(int cid, int bid, long eid, String from, String message, String chan, String buffer_type, String message_type, String network) {
        long last_eid = getLastSeenEid(bid);
        if (eid <= last_eid) {
            Crashlytics.log("Refusing to add notification for seen eid: " + eid);
            return;
        }

        Notification n = new Notification();
        n.bid = bid;
        n.cid = cid;
        n.eid = eid;
        n.nick = from;
        n.message = TextUtils.htmlEncode(ColorFormatter.emojify(message));
        n.chan = chan;
        n.buffer_type = buffer_type;
        n.message_type = message_type;
        n.network = network;

        synchronized (dbLock) {
            n.save();
        }
    }

    public void deleteOldNotifications() {
        boolean changed = false, pending = false;
        if (mNotificationTimerTask != null) {
            mNotificationTimerTask.cancel();
            pending = true;
        }

        List<Notification> notifications = getOtherNotifications();

        if (notifications.size() > 0) {
            for (Notification n : notifications) {
                long last_seen_eid = getLastSeenEid(n.bid);
                Buffer b = BuffersList.getInstance().getBuffer(n.bid);
                if(b != null)
                    last_seen_eid = b.getLast_seen_eid();
                if (n.eid <= last_seen_eid) {
                    NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int) (n.eid / 1000));
                    changed = true;
                    try {
                        if (PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("notify_sony", false))
                            NotificationUtil.deleteEvents(IRCCloudApplication.getInstance().getApplicationContext(), com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.FRIEND_KEY + " = ?", new String[]{String.valueOf(n.bid)});
                    } catch (Exception e) {
                    }
                }
            }
        }

        notifications = getNotifications();

        for (Notification n : notifications) {
            long last_seen_eid = getLastSeenEid(n.bid);
            Buffer b = BuffersList.getInstance().getBuffer(n.bid);
            if(b != null)
                last_seen_eid = b.getLast_seen_eid();
            if (n.eid <= last_seen_eid) {
                n.delete();
                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(n.bid);
                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int) (n.eid / 1000));
                changed = true;
                try {
                    if (PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("notify_sony", false))
                        NotificationUtil.deleteEvents(IRCCloudApplication.getInstance().getApplicationContext(), com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.FRIEND_KEY + " = ?", new String[]{String.valueOf(n.bid)});
                } catch (Exception e) {
                }
            }
        }
        if (changed) {
            IRCCloudApplication.getInstance().getApplicationContext().sendBroadcast(new Intent(DashClock.REFRESH_INTENT));
            updateTeslaUnreadCount();
        }

        if(pending)
            showNotifications(mTicker);
    }

    public void deleteNotificationsForBid(int bid) {
        List<Notification> notifications = getOtherNotifications();

        if (notifications.size() > 0) {
            for (Notification n : notifications) {
                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int) (n.eid / 1000));
            }
        }
        NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(bid);

        synchronized (dbLock) {
            notifications = getNotifications();
            for (Notification n : notifications) {
                if (n.bid == bid) {
                    n.delete();
                }
            }
            new Delete().from(Notification_LastSeenEID.class).where(Condition.column(Notification_LastSeenEID$Table.BID).is(bid)).queryClose();
        }
        IRCCloudApplication.getInstance().getApplicationContext().sendBroadcast(new Intent(DashClock.REFRESH_INTENT));
        try {
            if (PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("notify_sony", false))
                NotificationUtil.deleteEvents(IRCCloudApplication.getInstance().getApplicationContext(), com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.FRIEND_KEY + " = ?", new String[]{String.valueOf(bid)});
        } catch (Exception e) {
            //User has probably uninstalled Sony Liveware
        }
        updateTeslaUnreadCount();
    }

    public long count() {
        synchronized (dbLock) {
            return new Select().count().from(Notification.class).count();
        }
    }

    public List<Notification> getMessageNotifications() {
        synchronized (dbLock) {
            return new Select().from(Notification.class).where(Condition.column(Notification$Table.BID).isNot(excludeBid))
                    .and(Condition.column(Notification$Table.MESSAGE_TYPE).isNot("callerid"))
                    .and(Condition.column(Notification$Table.MESSAGE_TYPE).isNot("channel_invite"))
                    .orderBy(Notification$Table.BID + ", " + Notification$Table.EID).queryList();
        }
    }

    public List<Notification> getOtherNotifications() {
        synchronized (dbLock) {
            return new Select().from(Notification.class).where(
                    Condition.CombinedCondition.begin(Condition.column(Notification$Table.BID).isNot(excludeBid))
                            .and(Condition.CombinedCondition.begin(Condition.column(Notification$Table.MESSAGE_TYPE).is("callerid"))
                                    .or(Condition.column(Notification$Table.MESSAGE_TYPE).is("channel_invite"))))
                    .orderBy(Notification$Table.BID + ", " + Notification$Table.EID).queryList();
        }
    }

    public Notification getNotification(long eid) {
        synchronized (dbLock) {
            return new Select().from(Notification.class).where(Condition.column(Notification$Table.EID).is(eid)).querySingle();
        }
    }

    public synchronized void excludeBid(int bid) {
        excludeBid = -1;
        List<Notification> notifications = getOtherNotifications();

        if (notifications.size() > 0) {
            for (Notification n : notifications) {
                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int) (n.eid / 1000));
            }
        }
        NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(bid);
        excludeBid = bid;
    }

    private String mTicker = null;

    public synchronized void showNotifications(String ticker) {
        if (ticker != null)
            mTicker = ColorFormatter.emojify(ticker);

        if (mNotificationTimerTask == null) {
            try {
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        mNotificationTimerTask = null;
                        showMessageNotifications(mTicker);
                        showOtherNotifications();
                        mTicker = null;
                        IRCCloudApplication.getInstance().getApplicationContext().sendBroadcast(new Intent(DashClock.REFRESH_INTENT));
                        updateTeslaUnreadCount();
                    }

                    @Override
                    public boolean cancel() {
                        mNotificationTimerTask = null;
                        return super.cancel();
                    }
                };
                mNotificationTimer.schedule(task, 5000);
                mNotificationTimerTask = task;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showOtherNotifications() {
        String title = "";
        String text = "";
        String ticker = null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
        List<Notification> notifications = getOtherNotifications();

        int notify_type = Integer.parseInt(prefs.getString("notify_type", "1"));
        boolean notify = false;
        if (notify_type == 1 || (notify_type == 2 && NetworkConnection.getInstance().isVisible()))
            notify = true;

        if (notifications.size() > 0 && notify) {
            for (Notification n : notifications) {
                if (!n.shown) {
                    if (n.message_type.equals("callerid")) {
                        title = "Callerid: " + n.nick + " (" + n.network + ")";
                        text = n.nick + " " + n.message;
                        ticker = n.nick + " " + n.message;
                    } else {
                        title = n.nick + " (" + n.network + ")";
                        text = n.message;
                        ticker = n.message;
                    }
                    NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify((int) (n.eid / 1000), buildNotification(ticker, n.bid, new long[]{n.eid}, title, text, Html.fromHtml(text), 1, null, null, title, null));
                    n.shown = true;
                }
            }
        }
    }

    private boolean hasTouchWiz() {
        try {
            IRCCloudApplication.getInstance().getApplicationContext().getPackageManager().getPackageInfo("com.sec.android.app.launcher", 0);
            return true;
        } catch (Exception e) {
        }
        return false;
    }

    @SuppressLint("NewApi")
    private android.app.Notification buildNotification(String ticker, int bid, long[] eids, String title, String text, Spanned big_text, int count, Intent replyIntent, Spanned wear_text, String network, String auto_messages[]) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(IRCCloudApplication.getInstance().getApplicationContext())
                .setContentTitle(title + ((network != null) ? (" (" + network + ")") : ""))
                .setContentText(Html.fromHtml(text))
                .setAutoCancel(true)
                .setTicker(ticker)
                .setWhen(eids[0] / 1000)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setColor(IRCCloudApplication.getInstance().getApplicationContext().getResources().getColor(R.color.dark_blue))
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(hasTouchWiz() ? NotificationCompat.PRIORITY_DEFAULT : NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(false);

        if (ticker != null && (System.currentTimeMillis() - prefs.getLong("lastNotificationTime", 0)) > 10000) {
            if (prefs.getBoolean("notify_vibrate", true))
                builder.setDefaults(android.app.Notification.DEFAULT_VIBRATE);
            String ringtone = prefs.getString("notify_ringtone", "content://settings/system/notification_sound");
            if (ringtone != null && ringtone.length() > 0)
                builder.setSound(Uri.parse(ringtone));
        }

        int led_color = Integer.parseInt(prefs.getString("notify_led_color", "1"));
        if (led_color == 1) {
            if (prefs.getBoolean("notify_vibrate", true))
                builder.setDefaults(android.app.Notification.DEFAULT_LIGHTS | android.app.Notification.DEFAULT_VIBRATE);
            else
                builder.setDefaults(android.app.Notification.DEFAULT_LIGHTS);
        } else if (led_color == 2) {
            builder.setLights(0xFF0000FF, 500, 500);
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("lastNotificationTime", System.currentTimeMillis());
        editor.commit();

        Intent i = new Intent();
        i.setComponent(new ComponentName(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), "com.irccloud.android.MainActivity"));
        i.putExtra("bid", bid);
        i.setData(Uri.parse("bid://" + bid));
        Intent dismiss = new Intent(IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.DISMISS_NOTIFICATION));
        dismiss.setData(Uri.parse("irccloud-dismiss://" + bid));
        dismiss.putExtra("bid", bid);
        dismiss.putExtra("eids", eids);

        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(IRCCloudApplication.getInstance().getApplicationContext(), 0, dismiss, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT));
        builder.setDeleteIntent(dismissPendingIntent);

        if (replyIntent != null) {
            WearableExtender extender = new WearableExtender();
            PendingIntent replyPendingIntent = PendingIntent.getService(IRCCloudApplication.getInstance().getApplicationContext(), bid + 1, replyIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT);
            extender.addAction(new NotificationCompat.Action.Builder(R.drawable.ic_reply,
                    "Reply", replyPendingIntent)
                    .addRemoteInput(new RemoteInput.Builder("extra_reply").setLabel("Reply to " + title).build()).build());

            if (count > 1 && wear_text != null)
                extender.addPage(new NotificationCompat.Builder(IRCCloudApplication.getInstance().getApplicationContext()).setContentText(wear_text).extend(new WearableExtender().setStartScrollBottom(true)).build());

            NotificationCompat.CarExtender.UnreadConversation.Builder unreadConvBuilder =
                    new NotificationCompat.CarExtender.UnreadConversation.Builder(title + ((network != null) ? (" (" + network + ")") : ""))
                            .setReadPendingIntent(dismissPendingIntent)
                            .setReplyAction(replyPendingIntent, new RemoteInput.Builder("extra_reply").setLabel("Reply to " + title).build());

            if (auto_messages != null) {
                for (String m : auto_messages) {
                    if (m != null && m.length() > 0) {
                        unreadConvBuilder.addMessage(m);
                    }
                }
            } else {
                unreadConvBuilder.addMessage(text);
            }
            unreadConvBuilder.setLatestTimestamp(eids[count - 1] / 1000);

            builder.extend(extender).extend(new NotificationCompat.CarExtender().setUnreadConversation(unreadConvBuilder.build()));
        }

        if(replyIntent != null && prefs.getBoolean("notify_quickreply", true)) {
            i = new Intent(IRCCloudApplication.getInstance().getApplicationContext(), QuickReplyActivity.class);
            i.setData(Uri.parse("irccloud-bid://" + bid));
            i.putExtras(replyIntent);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent quickReplyIntent = PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.ic_action_reply, "Quick Reply", quickReplyIntent);
        }

        android.app.Notification notification = builder.build();

        RemoteViews contentView = new RemoteViews(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), R.layout.notification);
        contentView.setTextViewText(R.id.title, title + " (" + network + ")");
        contentView.setTextViewText(R.id.text, (count == 1) ? Html.fromHtml(text) : (count + " unread highlights."));
        contentView.setLong(R.id.time, "setTime", eids[0] / 1000);
        notification.contentView = contentView;

        if (Build.VERSION.SDK_INT >= 16 && big_text != null) {
            RemoteViews bigContentView = new RemoteViews(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), R.layout.notification_expanded);
            bigContentView.setTextViewText(R.id.title, title + (!title.equals(network) ? (" (" + network + ")") : ""));
            bigContentView.setTextViewText(R.id.text, big_text);
            bigContentView.setLong(R.id.time, "setTime", eids[0] / 1000);
            if (count > 3) {
                bigContentView.setViewVisibility(R.id.more, View.VISIBLE);
                bigContentView.setTextViewText(R.id.more, "+" + (count - 3) + " more");
            } else {
                bigContentView.setViewVisibility(R.id.more, View.GONE);
            }
            if(replyIntent != null && prefs.getBoolean("notify_quickreply", true)) {
                bigContentView.setViewVisibility(R.id.actions, View.VISIBLE);
                bigContentView.setViewVisibility(R.id.action_divider, View.VISIBLE);
                i = new Intent(IRCCloudApplication.getInstance().getApplicationContext(), QuickReplyActivity.class);
                i.setData(Uri.parse("irccloud-bid://" + bid));
                i.putExtras(replyIntent);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent quickReplyIntent = PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
                bigContentView.setOnClickPendingIntent(R.id.action_reply, quickReplyIntent);
            }
            notification.bigContentView = bigContentView;
        }

        if (Build.VERSION.SDK_INT >= 21) {
            RemoteViews headsUpContentView = new RemoteViews(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), R.layout.notification_expanded);
            headsUpContentView.setTextViewText(R.id.title, title + " (" + network + ")");
            headsUpContentView.setTextViewText(R.id.text, Html.fromHtml(text));
            headsUpContentView.setLong(R.id.time, "setTime", eids[0] / 1000);
            if(replyIntent != null && prefs.getBoolean("notify_quickreply", true)) {
                headsUpContentView.setViewVisibility(R.id.actions, View.VISIBLE);
                headsUpContentView.setViewVisibility(R.id.action_divider, View.VISIBLE);
                i = new Intent(IRCCloudApplication.getInstance().getApplicationContext(), QuickReplyActivity.class);
                i.setData(Uri.parse("irccloud-bid://" + bid));
                i.putExtras(replyIntent);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent quickReplyIntent = PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
                headsUpContentView.setOnClickPendingIntent(R.id.action_reply, quickReplyIntent);
            }
            notification.headsUpContentView = headsUpContentView;
        }

        return notification;
    }

    private void notifyPebble(String title, String body) {
        JSONObject jsonData = new JSONObject();
        try {
            final Intent i = new Intent("com.getpebble.action.SEND_NOTIFICATION");
            jsonData.put("title", title);
            jsonData.put("body", body);
            final String notificationData = new JSONArray().put(jsonData).toString();

            i.putExtra("messageType", "PEBBLE_ALERT");
            i.putExtra("sender", "IRCCloud");
            i.putExtra("notificationData", notificationData);
            IRCCloudApplication.getInstance().getApplicationContext().sendBroadcast(i);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showMessageNotifications(String ticker) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
        String text = "";
        String weartext = "";
        List<Notification> notifications = getMessageNotifications();

        int notify_type = Integer.parseInt(prefs.getString("notify_type", "1"));
        boolean notify = false;
        if (notify_type == 1 || (notify_type == 2 && NetworkConnection.getInstance().isVisible()))
            notify = true;

        if (notifications.size() > 0 && notify) {
            int lastbid = notifications.get(0).bid;
            int count = 0;
            long[] eids = new long[notifications.size()];
            String[] auto_messages = new String[notifications.size()];
            Notification last = null;
            count = 0;
            boolean show = false;
            for (Notification n : notifications) {
                if (n.bid != lastbid) {
                    if (show) {
                        String title = last.chan;
                        if (title == null || title.length() == 0)
                            title = last.nick;
                        if (title == null || title.length() == 0)
                            title = last.network;

                        Intent replyIntent = new Intent(RemoteInputService.ACTION_REPLY);
                        replyIntent.putExtra("bid", last.bid);
                        replyIntent.putExtra("cid", last.cid);
                        replyIntent.putExtra("eids", eids);
                        replyIntent.putExtra("network", last.network);
                        if (last.buffer_type.equals("channel"))
                            replyIntent.putExtra("to", last.chan);
                        else
                            replyIntent.putExtra("to", last.nick);

                        String body = "";
                        if (last.buffer_type.equals("channel")) {
                            if (last.message_type.equals("buffer_me_msg"))
                                body = "<b>— " + last.nick + "</b> " + last.message;
                            else
                                body = "<b>&lt;" + last.nick + "&gt;</b> " + last.message;
                        } else {
                            if (last.message_type.equals("buffer_me_msg"))
                                body = "— " + last.nick + " " + last.message;
                            else
                                body = last.message;
                        }

                        ArrayList<String> lines = new ArrayList<>(Arrays.asList(text.split("<br/>")));
                        while(lines.size() > 3)
                            lines.remove(0);

                        StringBuilder big_text = new StringBuilder();
                        for(String l : lines) {
                            if(big_text.length() > 0)
                                big_text.append("<br/>");
                            big_text.append(l);
                        }

                        NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify(lastbid, buildNotification(ticker, lastbid, eids, title, body, Html.fromHtml(big_text.toString()), count, replyIntent, Html.fromHtml(weartext), last.network, auto_messages));
                    }
                    lastbid = n.bid;
                    text = "";
                    weartext = "";
                    count = 0;
                    eids = new long[notifications.size()];
                    show = false;
                    auto_messages = new String[notifications.size()];
                }

                if (text.length() > 0)
                    text += "<br/>";
                if (n.buffer_type.equals("conversation") && n.message_type.equals("buffer_me_msg"))
                    text += "— " + n.message;
                else if (n.buffer_type.equals("conversation"))
                    text += n.message;
                else if (n.message_type.equals("buffer_me_msg"))
                    text += "<b>— " + n.nick + "</b> " + n.message;
                else
                    text += "<b>" + n.nick + "</b> " + n.message;

                if (weartext.length() > 0)
                    weartext += "<br/><br/>";
                if (n.message_type.equals("buffer_me_msg"))
                    weartext += "<b>— " + n.nick + "</b> " + n.message;
                else
                    weartext += "<b>&lt;" + n.nick + "&gt;</b> " + n.message;

                if (n.buffer_type.equals("conversation")) {
                    if (n.message_type.equals("buffer_me_msg"))
                        auto_messages[count] = "— " + n.nick + " " + Html.fromHtml(n.message).toString();
                    else
                        auto_messages[count] = Html.fromHtml(n.message).toString();
                } else {
                    if (n.message_type.equals("buffer_me_msg"))
                        auto_messages[count] = "— " + n.nick + " " + Html.fromHtml(n.message).toString();
                    else
                        auto_messages[count] = n.nick + " said: " + Html.fromHtml(n.message).toString();
                }

                if (!n.shown) {
                    n.shown = true;
                    show = true;

                    if (prefs.getBoolean("notify_sony", false)) {
                        long time = System.currentTimeMillis();
                        long sourceId = NotificationUtil.getSourceId(IRCCloudApplication.getInstance().getApplicationContext(), SonyExtensionService.EXTENSION_SPECIFIC_ID);
                        if (sourceId == NotificationUtil.INVALID_ID) {
                            Crashlytics.log(Log.ERROR, "IRCCloud", "Sony LiveWare Manager not configured, disabling Sony notifications");
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("notify_sony", false);
                            editor.commit();
                        } else {
                            ContentValues eventValues = new ContentValues();
                            eventValues.put(com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.EVENT_READ_STATUS, false);
                            eventValues.put(com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.DISPLAY_NAME, n.nick);

                            if (n.buffer_type.equals("channel") && n.chan != null && n.chan.length() > 0)
                                eventValues.put(com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.TITLE, n.chan);
                            else
                                eventValues.put(com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.TITLE, n.network);

                            if (n.message_type.equals("buffer_me_msg"))
                                eventValues.put(com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.MESSAGE, "— " + Html.fromHtml(n.message).toString());
                            else
                                eventValues.put(com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.MESSAGE, Html.fromHtml(n.message).toString());

                            eventValues.put(com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.PERSONAL, 1);
                            eventValues.put(com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.PUBLISHED_TIME, time);
                            eventValues.put(com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.SOURCE_ID, sourceId);
                            eventValues.put(com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.FRIEND_KEY, String.valueOf(n.bid));

                            try {
                                IRCCloudApplication.getInstance().getApplicationContext().getContentResolver().insert(com.sonyericsson.extras.liveware.aef.notification.Notification.Event.URI, eventValues);
                            } catch (IllegalArgumentException e) {
                                Log.e("IRCCloud", "Failed to insert event", e);
                            } catch (SecurityException e) {
                                Log.e("IRCCloud", "Failed to insert event, is Live Ware Manager installed?", e);
                            } catch (SQLException e) {
                                Log.e("IRCCloud", "Failed to insert event", e);
                            }
                        }
                    }

                    if (prefs.getBoolean("notify_pebble", false)) {
                        String pebbleTitle = n.network + ":\n";
                        String pebbleBody = "";
                        if (n.buffer_type.equals("channel") && n.chan != null && n.chan.length() > 0)
                            pebbleTitle = n.chan + ":\n";

                        if (n.message_type.equals("buffer_me_msg"))
                            pebbleBody = "— " + n.message;
                        else
                            pebbleBody = n.message;

                        if (n.nick != null && n.nick.length() > 0)
                            notifyPebble(n.nick, pebbleTitle + Html.fromHtml(pebbleBody).toString());
                        else
                            notifyPebble(n.network, pebbleTitle + Html.fromHtml(pebbleBody).toString());
                    }
                }
                eids[count++] = n.eid;
                last = n;
            }

            if (show) {
                String title = last.chan;
                if (title == null || title.length() == 0)
                    title = last.nick;
                if (title == null || title.length() == 0)
                    title = last.network;

                Intent replyIntent = new Intent(RemoteInputService.ACTION_REPLY);
                replyIntent.putExtra("bid", last.bid);
                replyIntent.putExtra("cid", last.cid);
                replyIntent.putExtra("network", last.network);
                replyIntent.putExtra("eids", eids);
                if (last.buffer_type.equals("channel"))
                    replyIntent.putExtra("to", last.chan);
                else
                    replyIntent.putExtra("to", last.nick);

                String body = "";
                if (last.buffer_type.equals("channel")) {
                    if (last.message_type.equals("buffer_me_msg"))
                        body = "<b>— " + last.nick + "</b> " + last.message;
                    else
                        body = "<b>&lt;" + last.nick + "&gt;</b> " + last.message;
                } else {
                    if (last.message_type.equals("buffer_me_msg"))
                        body = "— " + last.nick + " " + last.message;
                    else
                        body = last.message;
                }

                ArrayList<String> lines = new ArrayList<>(Arrays.asList(text.split("<br/>")));
                while(lines.size() > 3)
                    lines.remove(0);

                StringBuilder big_text = new StringBuilder();
                for(String l : lines) {
                    if(big_text.length() > 0)
                        big_text.append("<br/>");
                    big_text.append(l);
                }

                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify(lastbid, buildNotification(ticker, lastbid, eids, title, body, Html.fromHtml(big_text.toString()), count, replyIntent, Html.fromHtml(weartext), last.network, auto_messages));
            }
        }
    }

    public NotificationCompat.Builder alert(int bid, String title, String body) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(IRCCloudApplication.getInstance().getApplicationContext())
                .setContentTitle(title)
                .setContentText(body)
                .setTicker(body)
                .setAutoCancel(true)
                .setColor(IRCCloudApplication.getInstance().getApplicationContext().getResources().getColor(R.color.dark_blue))
                .setSmallIcon(R.drawable.ic_stat_notify);

        Intent i = new Intent();
        i.setComponent(new ComponentName(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), "com.irccloud.android.MainActivity"));
        i.putExtra("bid", bid);
        i.setData(Uri.parse("bid://" + bid));
        builder.setContentIntent(PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT));

        NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify(bid, builder.build());

        return builder;
    }

    public void updateTeslaUnreadCount() {
        try {
            IRCCloudApplication.getInstance().getApplicationContext().getPackageManager().getPackageInfo("com.teslacoilsw.notifier", PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return;
        }

        try {
            ContentValues cv = new ContentValues();
            cv.put("tag", IRCCloudApplication.getInstance().getApplicationContext().getPackageManager().getLaunchIntentForPackage(IRCCloudApplication.getInstance().getApplicationContext().getPackageName()).getComponent().flattenToString());
            cv.put("count", (int)count());
            IRCCloudApplication.getInstance().getApplicationContext().getContentResolver().insert(Uri.parse("content://com.teslacoilsw.notifier/unread_count"), cv);
        } catch (IllegalArgumentException ex) {
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}