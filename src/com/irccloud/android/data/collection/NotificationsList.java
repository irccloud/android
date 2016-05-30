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
import android.support.v4.os.BuildCompat;
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
import com.irccloud.android.activity.MainActivity;
import com.irccloud.android.activity.QuickReplyActivity;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.model.Notification;
import com.irccloud.android.data.model.Notification$Table;
import com.irccloud.android.data.model.Notification_LastSeenEID;
import com.irccloud.android.data.model.Notification_LastSeenEID$Table;
import com.irccloud.android.data.model.Notification_ServerNick;
import com.irccloud.android.data.model.Notification_ServerNick$Table;
import com.irccloud.android.data.model.Server;
import com.raizlabs.android.dbflow.runtime.TransactionManager;
import com.raizlabs.android.dbflow.sql.builder.Condition;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
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

    public interface NotificationAddedListener {
        void onNotificationAdded(Notification notification);
    }

    public NotificationAddedListener notificationAddedListener = null;

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
            NetworkConnection.printStackTraceToCrashlytics(e);
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

    public String getServerNick(int cid) {
        synchronized (dbLock) {
            Notification_ServerNick nick = new Select().from(Notification_ServerNick.class).where(Condition.column(Notification_ServerNick$Table.CID).is(cid)).querySingle();
            if (nick != null)
                return nick.nick;
            else
                return null;
        }
    }

    public synchronized void updateServerNick(int cid, String nick) {
        Notification_ServerNick n = new Notification_ServerNick();
        n.cid = cid;
        n.nick = nick;
        synchronized (dbLock) {
            n.save();
        }
    }

    public synchronized void dismiss(int bid, long eid) {
        synchronized (dbLock) {
            Log.d("IRCCloud", "Dismiss bid" + bid + " eid"+eid);
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

        if(notificationAddedListener != null)
            notificationAddedListener.onNotificationAdded(n);
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
        Log.d("IRCCloud", "Removing all notifications for bid" + bid);
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
                    .and(Condition.column(Notification$Table.MESSAGE_TYPE).isNot("callerid_success"))
                    .and(Condition.column(Notification$Table.MESSAGE_TYPE).isNot("channel_invite"))
                    .orderBy(Notification$Table.BID + ", " + Notification$Table.EID).queryList();
        }
    }

    public List<Notification> getOtherNotifications() {
        synchronized (dbLock) {
            return new Select().from(Notification.class).where(
                    Condition.CombinedCondition.begin(Condition.column(Notification$Table.BID).isNot(excludeBid))
                            .and(Condition.CombinedCondition.begin(Condition.column(Notification$Table.MESSAGE_TYPE).is("callerid"))
                                    .or(Condition.column(Notification$Table.MESSAGE_TYPE).is("callerid_success"))
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

    public void showNotificationsNow() {
        if(mNotificationTimerTask != null)
            mNotificationTimerTask.cancel();
        mNotificationTimerTask = null;
        showMessageNotifications(mTicker);
        showOtherNotifications();
        mTicker = null;
        IRCCloudApplication.getInstance().getApplicationContext().sendBroadcast(new Intent(DashClock.REFRESH_INTENT));
        updateTeslaUnreadCount();
    }

    public synchronized void showNotifications(String ticker) {
        if (ticker != null)
            mTicker = ColorFormatter.emojify(ticker);

        if (mNotificationTimerTask == null) {
            try {
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        showNotificationsNow();
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
                NetworkConnection.printStackTraceToCrashlytics(e);
            }
        }
    }

    private void showOtherNotifications() {
        String title = "";
        String text = "";
        String ticker;
        NotificationCompat.Action action = null;
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
                        title = n.network;
                        text = n.nick + " is trying to contact you";
                        ticker = n.nick + " is trying to contact you on " + n.network;

                        Intent i = new Intent(RemoteInputService.ACTION_REPLY);
                        i.setComponent(new ComponentName(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), RemoteInputService.class.getName()));
                        i.putExtra("cid", n.cid);
                        i.putExtra("eid", n.eid);
                        i.putExtra("chan", n.chan);
                        i.putExtra("buffer_type", n.buffer_type);
                        i.putExtra("network", n.network);
                        i.putExtra("to", n.nick);
                        i.putExtra("reply", "/accept " + n.nick);
                        action = new NotificationCompat.Action(R.drawable.ic_wearable_add, "Accept", PendingIntent.getService(IRCCloudApplication.getInstance().getApplicationContext(), (int)(n.eid / 1000), i, PendingIntent.FLAG_UPDATE_CURRENT));
                    } else if(n.message_type.equals("callerid_success")) {
                        title = n.network;
                        text = n.nick + " has been added to your accept list";
                        ticker = n.nick + " has been added to your accept list on " + n.network;
                        Intent i = new Intent(RemoteInputService.ACTION_REPLY);
                        i.setComponent(new ComponentName(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), RemoteInputService.class.getName()));
                        i.putExtra("cid", n.cid);
                        i.putExtra("eid", n.eid);
                        i.putExtra("chan", n.chan);
                        i.putExtra("buffer_type", n.buffer_type);
                        i.putExtra("network", n.network);
                        i.putExtra("to", n.nick);
                        action = new NotificationCompat.Action.Builder(R.drawable.ic_wearable_reply, "Message", PendingIntent.getService(IRCCloudApplication.getInstance().getApplicationContext(), (int)(n.eid / 1000), i, PendingIntent.FLAG_UPDATE_CURRENT))
                                .setAllowGeneratedReplies(true)
                                .addRemoteInput(new RemoteInput.Builder("extra_reply").setLabel("Message to " + n.nick).build()).build();
                    } else if(n.message_type.equals("channel_invite")) {
                        title = n.network;
                        text = n.nick + " invited you to join " + n.chan;
                        ticker = text;
                        try {
                            Intent i = new Intent();
                            i.setComponent(new ComponentName(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), "com.irccloud.android.MainActivity"));
                            i.setData(Uri.parse(IRCCloudApplication.getInstance().getResources().getString(R.string.IRCCLOUD_SCHEME) + "://cid/" + n.cid + "/" + URLEncoder.encode(n.chan, "UTF-8")));
                            i.putExtra("eid", n.eid);
                            action = new NotificationCompat.Action(R.drawable.ic_wearable_add, "Join", PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), (int)(n.eid / 1000), i, PendingIntent.FLAG_UPDATE_CURRENT));
                        } catch (Exception e) {
                            action = null;
                        }
                    } else {
                        title = n.nick;
                        text = n.message;
                        ticker = n.message;
                        action = null;
                    }
                    if(title != null && text != null)
                        NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify((int) (n.eid / 1000), buildNotification(ticker, n.bid, new long[]{n.eid}, title, text, 1, null, n.network, null, action));
                }
                n.delete();
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
    private android.app.Notification buildNotification(String ticker, int bid, long[] eids, String title, String text, int count, Intent replyIntent, String network, Notification messages[], NotificationCompat.Action otherAction) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(IRCCloudApplication.getInstance().getApplicationContext())
                .setContentTitle(title + ((network != null && !network.equals(title)) ? (" (" + network + ")") : ""))
                .setContentText(Html.fromHtml(text))
                .setAutoCancel(true)
                .setTicker(ticker)
                .setWhen(eids[0] / 1000)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setColor(IRCCloudApplication.getInstance().getApplicationContext().getResources().getColor(R.color.notification_icon_bg))
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(hasTouchWiz() ? NotificationCompat.PRIORITY_DEFAULT : NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(false);

        if (ticker != null && (System.currentTimeMillis() - prefs.getLong("lastNotificationTime", 0)) > 2000) {
            if (prefs.getBoolean("notify_vibrate", true))
                builder.setDefaults(android.app.Notification.DEFAULT_VIBRATE);
            String ringtone = prefs.getString("notify_ringtone", "content://settings/system/notification_sound");
            if (ringtone != null && ringtone.length() > 0)
                builder.setSound(Uri.parse(ringtone));
        }

        int led_color = Integer.parseInt(prefs.getString("notify_led_color", "1"));
        if (led_color == 1) {
            if (prefs.getBoolean("notify_vibrate", true) && ticker != null && (System.currentTimeMillis() - prefs.getLong("lastNotificationTime", 0)) > 2000)
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

        if(messages != null && messages.length > 0) {
            NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(getServerNick(messages[0].cid));
            style.setConversationTitle(title + ((network != null) ? (" (" + network + ")") : ""));
            for(Notification n : messages) {
                if(n != null && n.message != null && n.message.length() > 0) {
                    style.addMessage(Html.fromHtml(n.message).toString(), n.eid / 1000, n.nick);
                }
            }

            ArrayList<String> history = new ArrayList<>(messages.length);
            for(int j = messages.length - 1; j >= 0; j--) {
                Notification n = messages[j];
                if(n != null) {
                    if(n.nick == null)
                        history.add(Html.fromHtml(n.message).toString());
                    else
                        break;
                }
            }
            builder.setRemoteInputHistory(history.toArray(new String[history.size()]));
            builder.setStyle(style);
        }

        WearableExtender wearableExtender = new WearableExtender();
        if (replyIntent != null) {
            PendingIntent replyPendingIntent = PendingIntent.getService(IRCCloudApplication.getInstance().getApplicationContext(), bid + 1, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(new NotificationCompat.Action.Builder(0,
                    "Reply", replyPendingIntent)
                    .setAllowGeneratedReplies(true)
                    .addRemoteInput(new RemoteInput.Builder("extra_reply").setLabel("Reply to " + title).build()).build());

            wearableExtender.addAction(new NotificationCompat.Action.Builder(R.drawable.ic_wearable_reply,
                    "Reply", replyPendingIntent)
                    .setAllowGeneratedReplies(true)
                    .addRemoteInput(new RemoteInput.Builder("extra_reply").setLabel("Reply to " + title).build()).build());

            //if (count > 1 && wear_text != null)
            //    extender.addPage(new NotificationCompat.Builder(IRCCloudApplication.getInstance().getApplicationContext()).setContentText(wear_text).extend(new WearableExtender().setStartScrollBottom(true)).build());

            NotificationCompat.CarExtender.UnreadConversation.Builder unreadConvBuilder =
                    new NotificationCompat.CarExtender.UnreadConversation.Builder(title + ((network != null) ? (" (" + network + ")") : ""))
                            .setReadPendingIntent(dismissPendingIntent)
                            .setReplyAction(replyPendingIntent, new RemoteInput.Builder("extra_reply").setLabel("Reply to " + title).build());

            if (messages != null) {
                for (Notification n : messages) {
                    if (n != null && n.nick != null && n.message != null && n.message.length() > 0) {
                        if (n.buffer_type.equals("conversation")) {
                            if (n.message_type.equals("buffer_me_msg"))
                                unreadConvBuilder.addMessage("— " + n.nick + " " + Html.fromHtml(n.message).toString());
                            else
                                unreadConvBuilder.addMessage(Html.fromHtml(n.message).toString());
                        } else {
                            if (n.message_type.equals("buffer_me_msg"))
                                unreadConvBuilder.addMessage("— " + n.nick + " " + Html.fromHtml(n.message).toString());
                            else
                                unreadConvBuilder.addMessage(n.nick + " said: " + Html.fromHtml(n.message).toString());
                        }
                        unreadConvBuilder.addMessage(n.message);
                    }
                }
            } else {
                unreadConvBuilder.addMessage(text);
            }
            unreadConvBuilder.setLatestTimestamp(eids[count - 1] / 1000);

            builder.extend(new NotificationCompat.CarExtender().setUnreadConversation(unreadConvBuilder.build()));
        }

        if (replyIntent != null && prefs.getBoolean("notify_quickreply", true) && !BuildCompat.isAtLeastN()) {
            i = new Intent(IRCCloudApplication.getInstance().getApplicationContext(), QuickReplyActivity.class);
            i.setData(Uri.parse("irccloud-bid://" + bid));
            i.putExtras(replyIntent);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent quickReplyIntent = PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(0, "Quick Reply", quickReplyIntent);
        }

        if(otherAction != null) {
            builder.addAction(new NotificationCompat.Action(0, otherAction.getTitle(), otherAction.getActionIntent()));
            wearableExtender.addAction(otherAction);
        }

        builder.extend(wearableExtender);

        return builder.build();
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
            NetworkConnection.printStackTraceToCrashlytics(e);
        }
    }

    private void showMessageNotifications(String ticker) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
        String text = "";
        List<Notification> notifications = getMessageNotifications();

        int notify_type = Integer.parseInt(prefs.getString("notify_type", "1"));
        boolean notify = false;
        if (notify_type == 1 || (notify_type == 2 && NetworkConnection.getInstance().isVisible()))
            notify = true;

        if (notifications.size() > 0 && notify) {
            int lastbid = notifications.get(0).bid;
            int count = 0;
            long[] eids = new long[notifications.size()];
            Notification[] messages = new Notification[notifications.size()];
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
                        replyIntent.putExtra("chan", last.chan);
                        replyIntent.putExtra("buffer_type", last.buffer_type);
                        replyIntent.putExtra("to", last.chan);

                        String body = "";
                        if (last.buffer_type.equals("channel")) {
                            if (last.message_type.equals("buffer_me_msg"))
                                body = "<b>— " + ((last.nick != null)?last.nick:getServerNick(last.cid)) + "</b> " + last.message;
                            else
                                body = "<b>&lt;" + ((last.nick != null)?last.nick:getServerNick(last.cid)) + "&gt;</b> " + last.message;
                        } else {
                            if (last.message_type.equals("buffer_me_msg"))
                                body = "— " + ((last.nick != null)?last.nick:getServerNick(last.cid)) + " " + last.message;
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

                        try {
                            NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify(lastbid, buildNotification(ticker, lastbid, eids, title, body, count, replyIntent, last.network, messages, null));
                        } catch (Exception e) {
                            Crashlytics.logException(e);
                        }
                    }
                    lastbid = n.bid;
                    text = "";
                    count = 0;
                    eids = new long[notifications.size()];
                    show = false;
                    messages = new Notification[notifications.size()];
                }

                if (text.length() > 0)
                    text += "<br/>";
                if (n.buffer_type.equals("conversation") && n.message_type.equals("buffer_me_msg"))
                    text += "— " + n.message;
                else if (n.buffer_type.equals("conversation"))
                    text += n.message;
                else if (n.message_type.equals("buffer_me_msg"))
                    text += "<b>— " + ((n.nick != null)?n.nick:getServerNick(n.cid)) + "</b> " + n.message;
                else
                    text += "<b>" + ((n.nick != null)?n.nick:getServerNick(n.cid)) + "</b> " + n.message;

                if (!n.shown) {
                    n.shown = true;
                    show = true;
                    synchronized (dbLock) {
                        n.save();
                    }

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

                    if (prefs.getBoolean("notify_pebble", false) && ((n.chan != null && n.nick != null) || n.chan == null)) {
                        String pebbleTitle = n.network + ":\n";
                        String pebbleBody = "";
                        if (n.buffer_type.equals("channel") && n.chan != null && n.chan.length() > 0)
                            pebbleTitle = n.chan + ":\n";

                        if (n.message_type.equals("buffer_me_msg"))
                            pebbleBody = "— " + n.message;
                        else
                            pebbleBody = n.message;

                        if (n.chan != null && n.nick != null && n.nick.length() > 0)
                            notifyPebble(n.nick, pebbleTitle + Html.fromHtml(pebbleBody).toString());
                        else
                            notifyPebble(n.network, pebbleTitle + Html.fromHtml(pebbleBody).toString());
                    }
                }
                messages[count] = n;
                eids[count++] = n.eid;
                last = n;
            }

            if (show) {
                String title = last.chan;
                if (title == null || title.length() == 0)
                    title = last.network;

                Intent replyIntent = new Intent(RemoteInputService.ACTION_REPLY);
                replyIntent.putExtra("bid", last.bid);
                replyIntent.putExtra("cid", last.cid);
                replyIntent.putExtra("network", last.network);
                replyIntent.putExtra("eids", eids);
                replyIntent.putExtra("chan", last.chan);
                replyIntent.putExtra("buffer_type", last.buffer_type);
                replyIntent.putExtra("to", last.chan);

                String body = "";
                if (last.buffer_type.equals("channel")) {
                    if (last.message_type.equals("buffer_me_msg"))
                        body = "<b>— " + ((last.nick != null)?last.nick:getServerNick(last.cid)) + "</b> " + last.message;
                    else
                        body = "<b>&lt;" + ((last.nick != null)?last.nick:getServerNick(last.cid)) + "&gt;</b> " + last.message;
                } else {
                    if (last.message_type.equals("buffer_me_msg"))
                        body = "— " + ((last.nick != null)?last.nick:getServerNick(last.cid)) + " " + last.message;
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

                try {
                    NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify(last.bid, buildNotification(ticker, last.bid, eids, title, body, count, replyIntent, last.network, messages, null));
                } catch (Exception e) {
                    Crashlytics.logException(e);
                }
            }
        }
    }

    public NotificationCompat.Builder alert(int bid, String title, String body) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(IRCCloudApplication.getInstance().getApplicationContext())
                .setContentTitle(title)
                .setContentText(body)
                .setTicker(body)
                .setAutoCancel(true)
                .setColor(IRCCloudApplication.getInstance().getApplicationContext().getResources().getColor(R.color.notification_icon_bg))
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
            NetworkConnection.printStackTraceToCrashlytics(ex);
        }
    }
}