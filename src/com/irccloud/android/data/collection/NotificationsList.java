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
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.WearableExtender;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.IconCompat;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import com.crashlytics.android.Crashlytics;
import com.irccloud.android.ColorFormatter;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.MarkAsReadBroadcastReceiver;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.NotificationDismissBroadcastReceiver;
import com.irccloud.android.R;
import com.irccloud.android.RemoteInputService;
import com.irccloud.android.activity.QuickReplyActivity;
import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.model.Notification;
import com.irccloud.android.data.model.Notification_LastSeenEID;
import com.irccloud.android.data.model.Notification_ServerNick;
import com.irccloud.android.data.model.Server;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NotificationsList {
    @Dao
    public interface NotificationsDao {
        @Query("SELECT COUNT(*) FROM Notification")
        int count();

        @Query("SELECT COUNT(*) FROM Notification WHERE nick != null")
        int unread_count();

        @Query("SELECT * FROM Notification ORDER BY bid,eid ASC")
        List<Notification> getNotifications();

        @Query("SELECT * FROM Notification WHERE bid != :excludeBid AND message_type != 'callerid' AND message_type != 'callerid_success' AND message_type != 'channel_invite' ORDER BY bid,eid ASC")
        List<Notification> getMessageNotifications(int excludeBid);

        @Query("SELECT * FROM Notification WHERE bid != :excludeBid AND (message_type = 'callerid' OR message_type = 'callerid_success' OR message_type = 'channel_invite') ORDER BY bid,eid ASC")
        List<Notification> getOtherNotifications(int excludeBid);

        @Query("SELECT * FROM Notification WHERE eid = :eid LIMIT 1")
        Notification getNotification(long eid);

        @Query("SELECT * FROM Notification_LastSeenEID WHERE bid = :bid LIMIT 1")
        Notification_LastSeenEID getLastSeenEID(int bid);

        @Query("SELECT * FROM Notification_ServerNick WHERE cid = :cid LIMIT 1")
        Notification_ServerNick getServerNick(int cid);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(Notification notification);

        @Update
        void update(Notification notification);

        @Update
        void update(List<Notification> notification);

        @Delete
        void delete(Notification notification);

        @Delete
        void delete(List<Notification> notifications);

        @Query("DELETE FROM Notification WHERE bid = :bid")
        void delete(int bid);

        @Query("DELETE FROM Notification WHERE bid = :bid AND eid = :eid")
        void delete(int bid, long eid);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(Notification_LastSeenEID lastSeenEID);

        @Update
        void update(Notification_LastSeenEID lastSeenEID);

        @Delete
        void delete(Notification_LastSeenEID lastSeenEID);

        @Query("DELETE FROM Notification_LastSeenEID WHERE bid = :bid")
        void deleteLastSeenEID(int bid);

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        void insert(Notification_ServerNick serverNick);

        @Update
        void update(Notification_ServerNick serverNick);

        @Delete
        void delete(Notification_ServerNick serverNick);

        @Query("DELETE FROM Notification")
        void clearNotifications();

        @Query("DELETE FROM Notification_LastSeenEID")
        void clearLastSeenEIDs();

        @Query("DELETE FROM Notification_ServerNick")
        void clearServerNicks();
    }

    private static NotificationsList instance = null;
    private int excludeBid = -1;
    private static final Timer mNotificationTimer = new Timer("notification-timer");
    private TimerTask mNotificationTimerTask = null;

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
        return IRCCloudDatabase.getInstance().NotificationsDao().getNotifications();
    }

    public void clear() {
        try {
            for (Notification n : getNotifications()) {
                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int) (n.getEid() / 1000));
                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(n.getBid());
            }
        } catch (Exception e) {
            NetworkConnection.printStackTraceToCrashlytics(e);
        }
        updateTeslaUnreadCount();
        IRCCloudDatabase.getInstance().NotificationsDao().clearNotifications();
    }

    public void clearLastSeenEIDs() {
        IRCCloudDatabase.getInstance().NotificationsDao().clearLastSeenEIDs();
    }

    public long getLastSeenEid(int bid) {
        Buffer b = BuffersList.getInstance().getBuffer(bid);
        if(b != null)
            return b.getLast_seen_eid();
        Notification_LastSeenEID eid = IRCCloudDatabase.getInstance().NotificationsDao().getLastSeenEID(bid);
        if (eid != null)
            return eid.getEid();
        else
            return -1;
    }

    public void updateLastSeenEid(int bid, long eid) {
        Notification_LastSeenEID n = new Notification_LastSeenEID();
        n.setBid(bid);
        n.setEid(eid);
        IRCCloudDatabase.getInstance().NotificationsDao().insert(n);
    }

    public String getServerNick(int cid) {
        Server s = ServersList.getInstance().getServer(cid);
        if(s != null && s.getNick() != null && s.getNick().length() > 0)
            return s.getNick();
        Notification_ServerNick nick = IRCCloudDatabase.getInstance().NotificationsDao().getServerNick(cid);
        if (nick != null)
            return nick.getNick();
        else
            return null;
    }

    public void updateServerNick(int cid, String nick) {
        updateServerNick(cid, nick, getServerAvatarURL(cid));
    }

    public void updateServerNick(int cid, String nick, String avatar_url) {
        Notification_ServerNick n = new Notification_ServerNick();
        n.setCid(cid);
        n.setNick(nick);
        n.setAvatar_url(avatar_url);
        IRCCloudDatabase.getInstance().NotificationsDao().insert(n);
    }

    public String getServerAvatarURL(int cid) {
        Notification_ServerNick nick = IRCCloudDatabase.getInstance().NotificationsDao().getServerNick(cid);
        if (nick != null)
            return nick.getAvatar_url();
        else
            return null;
    }

    public void dismiss(int bid, long eid) {
        Log.d("IRCCloud", "Dismiss bid" + bid + " eid"+eid);
        IRCCloudDatabase.getInstance().NotificationsDao().delete(bid, eid);
        updateTeslaUnreadCount();
    }

    public void addNotification(int cid, int bid, long eid, String from, String message, String chan, String buffer_type, String message_type, String network, String avatar_url) {
        long last_eid = getLastSeenEid(bid);
        if (eid <= last_eid) {
            Crashlytics.log("Refusing to add notification for seen eid: " + eid);
            return;
        }

        Notification n = new Notification();
        n.setBid(bid);
        n.setCid(cid);
        n.setEid(eid);
        n.setNick(from);
        n.setMessage(TextUtils.htmlEncode(ColorFormatter.strip(message).toString()));
        n.setChan(chan);
        n.setBuffer_type(buffer_type);
        n.setMessage_type(message_type);
        n.setNetwork(network);
        n.setAvatar_url(avatar_url);

        IRCCloudDatabase.getInstance().NotificationsDao().insert(n);

        if(notificationAddedListener != null)
            notificationAddedListener.onNotificationAdded(n);
    }

    public void deleteOldNotifications() {
        boolean changed = false, pending = false;
        if (mNotificationTimerTask != null) {
            mNotificationTimerTask.cancel();
            pending = true;
        }

        List<Notification> notifications = getNotifications();
        final ArrayList<Notification> oldNotifications = new ArrayList<>();

        for (Notification n : notifications) {
            long last_seen_eid = getLastSeenEid(n.getBid());
            Buffer b = BuffersList.getInstance().getBuffer(n.getBid());
            if(b != null)
                last_seen_eid = b.getLast_seen_eid();
            if (last_seen_eid == -1 || n.getEid() <= last_seen_eid) {
                oldNotifications.add(n);
                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(n.getBid());
                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int) (n.getEid() / 1000));
                changed = true;
            }
        }

        if (changed) {
            IRCCloudDatabase.getInstance().runInTransaction(new Runnable() {
                @Override
                public void run() {
                    IRCCloudDatabase.getInstance().NotificationsDao().delete(oldNotifications);
                }
            });

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
                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int) (n.getEid() / 1000));
            }
        }
        NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(bid);

        IRCCloudDatabase.getInstance().NotificationsDao().delete(bid);
        IRCCloudDatabase.getInstance().NotificationsDao().deleteLastSeenEID(bid);
        updateTeslaUnreadCount();
    }

    public int count() {
        return IRCCloudDatabase.getInstance().NotificationsDao().count();
    }

    public List<Notification> getMessageNotifications() {
        return IRCCloudDatabase.getInstance().NotificationsDao().getMessageNotifications(excludeBid);
    }

    public List<Notification> getOtherNotifications() {
        return IRCCloudDatabase.getInstance().NotificationsDao().getOtherNotifications(excludeBid);
    }

    public Notification getNotification(long eid) {
        return IRCCloudDatabase.getInstance().NotificationsDao().getNotification(eid);
    }

    public void excludeBid(int bid) {
        excludeBid = -1;
        List<Notification> notifications = getOtherNotifications();

        if (notifications.size() > 0) {
            for (Notification n : notifications) {
                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int) (n.getEid() / 1000));
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
        final List<Notification> notifications = getOtherNotifications();

        int notify_type = Integer.parseInt(prefs.getString("notify_type", "1"));
        boolean notify = false;
        if (notify_type == 1 || (notify_type == 2 && NetworkConnection.getInstance().isVisible()))
            notify = true;

        if (notifications.size() > 0 && notify) {
            for (Notification n : notifications) {
                if (!n.isShown()) {
                    Crashlytics.log(Log.DEBUG, "IRCCloud", "Posting notification for type " + n.getMessage_type());
                    if (n.getMessage_type().equals("callerid")) {
                        title = n.getNetwork();
                        text = n.getNick() + " is trying to contact you";
                        ticker = n.getNick() + " is trying to contact you on " + n.getNetwork();

                        Intent i = new Intent(RemoteInputService.ACTION_REPLY);
                        i.setComponent(new ComponentName(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), RemoteInputService.class.getName()));
                        i.putExtra("cid", n.getCid());
                        i.putExtra("eid", n.getEid());
                        i.putExtra("chan", n.getChan());
                        i.putExtra("buffer_type", n.getBuffer_type());
                        i.putExtra("network", n.getNetwork());
                        i.putExtra("to", n.getNick());
                        i.putExtra("reply", "/accept " + n.getNick());
                        action = new NotificationCompat.Action(R.drawable.ic_wearable_add, "Accept", PendingIntent.getService(IRCCloudApplication.getInstance().getApplicationContext(), (int)(n.getEid() / 1000), i, PendingIntent.FLAG_UPDATE_CURRENT));
                    } else if(n.getMessage_type().equals("callerid_success")) {
                        title = n.getNetwork();
                        text = n.getNick() + " has been added to your accept list";
                        ticker = n.getNick() + " has been added to your accept list on " + n.getNetwork();
                        Intent i = new Intent(RemoteInputService.ACTION_REPLY);
                        i.setComponent(new ComponentName(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), RemoteInputService.class.getName()));
                        i.putExtra("cid", n.getCid());
                        i.putExtra("eid", n.getEid());
                        i.putExtra("chan", n.getChan());
                        i.putExtra("buffer_type", n.getBuffer_type());
                        i.putExtra("network", n.getNetwork());
                        i.putExtra("to", n.getNick());
                        action = new NotificationCompat.Action.Builder(R.drawable.ic_wearable_reply, "Message", PendingIntent.getService(IRCCloudApplication.getInstance().getApplicationContext(), (int)(n.getEid() / 1000), i, PendingIntent.FLAG_UPDATE_CURRENT))
                                .setAllowGeneratedReplies(true)
                                .addRemoteInput(new RemoteInput.Builder("extra_reply").setLabel("Message to " + n.getNick()).build()).build();
                    } else if(n.getMessage_type().equals("channel_invite")) {
                        title = n.getNetwork();
                        text = n.getNick() + " invited you to join " + n.getChan();
                        ticker = text;
                        try {
                            Intent i = new Intent();
                            i.setComponent(new ComponentName(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), "com.irccloud.android.MainActivity"));
                            i.setData(Uri.parse(IRCCloudApplication.getInstance().getResources().getString(R.string.IRCCLOUD_SCHEME) + "://cid/" + n.getCid() + "/" + URLEncoder.encode(n.getChan(), "UTF-8")));
                            i.putExtra("eid", n.getEid());
                            action = new NotificationCompat.Action(R.drawable.ic_wearable_add, "Join", PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), (int)(n.getEid() / 1000), i, PendingIntent.FLAG_UPDATE_CURRENT));
                        } catch (Exception e) {
                            action = null;
                        }
                    } else {
                        title = n.getNick();
                        text = n.getMessage();
                        ticker = n.getMessage();
                        action = null;
                    }
                    if(title != null && text != null)
                        NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify((int) (n.getEid() / 1000), buildNotification(ticker, n.getCid(), n.getBid(), new long[]{n.getEid()}, title, text, 1, null, n.getNetwork(), null, action, AvatarsList.getInstance().getAvatar(n.getCid(), n.getNick(), null).getBitmap(false, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, IRCCloudApplication.getInstance().getApplicationContext().getResources().getDisplayMetrics()), false, Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP), AvatarsList.getInstance().getAvatar(n.getCid(), n.getNick(), null).getBitmap(false, 400, false, false), null));
                }
            }

            IRCCloudDatabase.getInstance().runInTransaction(new Runnable() {
                @Override
                public void run() {
                    IRCCloudDatabase.getInstance().NotificationsDao().delete(notifications);
                }
            });
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

    public void createChannel(String id, String title, int importance, String group) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ((NotificationManager)IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE));
            String ringtone = "android.resource://" + IRCCloudApplication.getInstance().getApplicationContext().getPackageName() + "/raw/digit";
            NotificationChannel defaults = nm.getNotificationChannel("highlight");
            NotificationChannel c = new NotificationChannel(id, title, importance);
            if(defaults != null && defaults.getSound() != null) {
                c.setSound(defaults.getSound(), defaults.getAudioAttributes());
            } else {
                if (ringtone.length() > 0)
                    c.setSound(Uri.parse(ringtone), new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                            .build());
            }
            if(defaults != null) {
                c.enableLights(defaults.shouldShowLights());
                c.setLightColor(defaults.getLightColor());
                c.enableVibration(defaults.shouldVibrate());
                c.setVibrationPattern(defaults.getVibrationPattern());
            } else {
                c.enableLights(true);
                c.enableVibration(true);
            }
            if(group != null)
                c.setGroup(group);
            nm.createNotificationChannel(c);
        }
    }

    @SuppressLint("NewApi")
    private android.app.Notification buildNotification(String ticker, int cid, int bid, long[] eids, String title, String text, int count, Intent replyIntent, String network, ArrayList<Notification> messages, NotificationCompat.Action otherAction, Bitmap largeIcon, Bitmap wearBackground, HashMap<String, Bitmap> avatars) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
        String ringtone = prefs.getString("notify_ringtone", "android.resource://" + IRCCloudApplication.getInstance().getApplicationContext().getPackageName() + "/raw/digit");
        String uid = prefs.getString("uid", "");
        int defaults = 0;
        String channelId = prefs.getBoolean("notify_channels", false) ? (uid + String.valueOf(bid)) : "highlight";
        if(prefs.getBoolean("notify_channels", false))
            createChannel(uid + String.valueOf(bid), title, NotificationManagerCompat.IMPORTANCE_HIGH, String.valueOf(cid));
        NotificationCompat.Builder builder = new NotificationCompat.Builder(IRCCloudApplication.getInstance().getApplicationContext(), channelId)
                .setContentTitle(title + ((network != null && !network.equals(title)) ? (" (" + network + ")") : ""))
                .setContentText(Html.fromHtml(text))
                .setAutoCancel(true)
                .setTicker(ticker)
                .setWhen(eids[0] / 1000)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setColor(IRCCloudApplication.getInstance().getApplicationContext().getResources().getColor(R.color.ic_background))
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setOnlyAlertOnce(false);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            builder.setLargeIcon(largeIcon);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && ticker != null && (System.currentTimeMillis() - prefs.getLong("lastNotificationTime", 0)) > 2000) {
            if (ringtone.length() > 0)
                builder.setSound(Uri.parse(ringtone));
        }
        if (!hasTouchWiz() && Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);

        int led_color = Integer.parseInt(prefs.getString("notify_led_color", "1"));
        if (led_color == 1) {
            defaults = android.app.Notification.DEFAULT_LIGHTS;
        } else if (led_color == 2) {
            builder.setLights(0xFF0000FF, 500, 500);
        }

        if (prefs.getBoolean("notify_vibrate", true) && ticker != null && (System.currentTimeMillis() - prefs.getLong("lastNotificationTime", 0)) > 2000)
            defaults |= android.app.Notification.DEFAULT_VIBRATE;

        builder.setDefaults(defaults);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong("lastNotificationTime", System.currentTimeMillis());
        editor.apply();

        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setComponent(new ComponentName(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), "com.irccloud.android.MainActivity"));
        i.putExtra("bid", bid);
        i.setData(Uri.parse("bid://" + bid));
        Intent dismiss = new Intent(IRCCloudApplication.getInstance().getApplicationContext(), NotificationDismissBroadcastReceiver.class);
        dismiss.setData(Uri.parse("irccloud-dismiss://" + bid));
        dismiss.putExtra("bid", bid);
        dismiss.putExtra("eids", eids);

        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(IRCCloudApplication.getInstance().getApplicationContext(), 0, dismiss, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT));
        builder.setDeleteIntent(dismissPendingIntent);

        WearableExtender wearableExtender = new WearableExtender();
        wearableExtender.setBackground(wearBackground);
        if(messages != null && messages.size() > 0) {
            StringBuilder weartext = new StringBuilder();
            HashMap<String, Person> people = new HashMap<>();
            String servernick = getServerNick(messages.get(0).getCid());
            if(avatars != null && avatars.containsKey(servernick)) {
                people.put(servernick, new Person.Builder().setName(servernick).setIcon(IconCompat.createWithBitmap(avatars.get(servernick))).build());
            } else {
                people.put(servernick, new Person.Builder().setName(servernick).build());
            }

            NotificationCompat.MessagingStyle style = new NotificationCompat.MessagingStyle(people.get(servernick));
            style.setConversationTitle(title + ((network != null) ? (" (" + network + ")") : ""));
            style.setGroupConversation(true);

            for(Notification n : messages) {
                if(n != null && n.getMessage() != null && n.getMessage().length() > 0) {
                    if (weartext.length() > 0)
                        weartext.append("<br/>");
                    String nick = (n.getNick() != null) ? n.getNick() : servernick;
                    Person p = people.get(nick);
                    if(p == null) {
                        if(avatars != null && avatars.containsKey(nick)) {
                            p = new Person.Builder()
                                    .setName(nick)
                                    .setIcon(IconCompat.createWithBitmap(avatars.get(nick)))
                                    .build();
                        } else {
                            p = new Person.Builder()
                                    .setName(nick)
                                    .build();
                        }
                        people.put(nick, p);
                    }
                    if (n.getMessage_type().equals("buffer_me_msg")) {
                        style.addMessage(new NotificationCompat.MessagingStyle.Message(Html.fromHtml("— " + n.getMessage()).toString(), n.getEid() / 1000, p));
                        weartext.append("<b>— ").append((n.getNick() == null) ? servernick : n.getNick()).append("</b> ").append(n.getMessage());
                    } else {
                        style.addMessage(new NotificationCompat.MessagingStyle.Message(Html.fromHtml(n.getMessage()).toString(), n.getEid() / 1000, p));
                        weartext.append("<b>&lt;").append((n.getNick() == null) ? servernick : n.getNick()).append("&gt;</b> ").append(n.getMessage());
                    }
                }
            }

            builder.setStyle(style);

            if(messages.size() > 1) {
                wearableExtender.addPage(new NotificationCompat.Builder(IRCCloudApplication.getInstance().getApplicationContext()).setContentText(Html.fromHtml(weartext.toString())).extend(new WearableExtender().setStartScrollBottom(true)).build());
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                weartext.setLength(0);
                int j = 0;
                for(Notification n : messages) {
                    if(messages.size() - ++j < 3) {
                        if (n != null && n.getMessage() != null && n.getMessage().length() > 0) {
                            if (weartext.length() > 0)
                                weartext.append("<br/>");
                            if (n.getMessage_type().equals("buffer_me_msg")) {
                                weartext.append("<b>— ").append((n.getNick() == null) ? servernick : n.getNick()).append("</b> ").append(n.getMessage());
                            } else {
                                weartext.append("<b>&lt;").append((n.getNick() == null) ? servernick : n.getNick()).append("&gt;</b> ").append(n.getMessage());
                            }
                        }
                    }
                }

                RemoteViews bigContentView = new RemoteViews(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), R.layout.notification_expanded);
                bigContentView.setTextViewText(R.id.title, title + (!title.equals(network) ? (" (" + network + ")") : ""));
                bigContentView.setTextViewText(R.id.text, Html.fromHtml(weartext.toString()));
                bigContentView.setImageViewBitmap(R.id.image, largeIcon);
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
                builder.setCustomBigContentView(bigContentView);
            }
        }

        if (replyIntent != null) {
            PendingIntent replyPendingIntent = PendingIntent.getService(IRCCloudApplication.getInstance().getApplicationContext(), bid + 1, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.addAction(new NotificationCompat.Action.Builder(0,
                        "Reply", replyPendingIntent)
                        .setAllowGeneratedReplies(true)
                        .addRemoteInput(new RemoteInput.Builder("extra_reply").setLabel("Reply to " + title).build()).build());
            }

            NotificationCompat.Action.Builder actionBuilder = new NotificationCompat.Action.Builder(R.drawable.ic_wearable_reply,
                    "Reply", replyPendingIntent)
                    .setAllowGeneratedReplies(true)
                    .addRemoteInput(new RemoteInput.Builder("extra_reply").setLabel("Reply to " + title).build());

            NotificationCompat.Action.WearableExtender actionExtender =
                    new NotificationCompat.Action.WearableExtender()
                            .setHintLaunchesActivity(true)
                            .setHintDisplayActionInline(true);

            wearableExtender.addAction(actionBuilder.extend(actionExtender).build());

            NotificationCompat.CarExtender.UnreadConversation.Builder unreadConvBuilder =
                    new NotificationCompat.CarExtender.UnreadConversation.Builder(title + ((network != null) ? (" (" + network + ")") : ""))
                            .setReadPendingIntent(dismissPendingIntent)
                            .setReplyAction(replyPendingIntent, new RemoteInput.Builder("extra_reply").setLabel("Reply to " + title).build());

            if (messages != null) {
                for (Notification n : messages) {
                    if (n != null && n.getNick() != null && n.getMessage() != null && n.getMessage().length() > 0) {
                        if (n.getBuffer_type().equals("conversation")) {
                            if (n.getMessage_type().equals("buffer_me_msg"))
                                unreadConvBuilder.addMessage("— " + n.getNick() + " " + Html.fromHtml(n.getMessage()).toString());
                            else
                                unreadConvBuilder.addMessage(Html.fromHtml(n.getMessage()).toString());
                        } else {
                            if (n.getMessage_type().equals("buffer_me_msg"))
                                unreadConvBuilder.addMessage("— " + n.getNick() + " " + Html.fromHtml(n.getMessage()).toString());
                            else
                                unreadConvBuilder.addMessage(n.getNick() + " said: " + Html.fromHtml(n.getMessage()).toString());
                        }
                    }
                }
            } else {
                unreadConvBuilder.addMessage(text);
            }
            unreadConvBuilder.setLatestTimestamp(eids[count - 1] / 1000);

            builder.extend(new NotificationCompat.CarExtender().setUnreadConversation(unreadConvBuilder.build()));
        }

        if (replyIntent != null && prefs.getBoolean("notify_quickreply", true) && Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            i = new Intent(IRCCloudApplication.getInstance().getApplicationContext(), QuickReplyActivity.class);
            i.setData(Uri.parse("irccloud-bid://" + bid));
            i.putExtras(replyIntent);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent quickReplyIntent = PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.ic_action_reply, "Quick Reply", quickReplyIntent);
        }

        /*if (replyIntent != null) {
            Intent markAsRead = new Intent(IRCCloudApplication.getInstance().getApplicationContext(), MarkAsReadBroadcastReceiver.class);
            markAsRead.setData(Uri.parse("irccloud-markasread://" + bid));
            markAsRead.putExtra("cid", cid);
            markAsRead.putExtra("bid", bid);
            markAsRead.putExtra("eids", eids);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(IRCCloudApplication.getInstance().getApplicationContext(), 0, markAsRead, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(0, "Mark As Read", pendingIntent);
        }*/

        if(otherAction != null) {
            int drawable = 0;
            if(otherAction.getIcon() == R.drawable.ic_wearable_add)
                drawable = R.drawable.ic_action_add;
            else if(otherAction.getIcon() == R.drawable.ic_wearable_reply)
                drawable = R.drawable.ic_action_reply;
            builder.addAction(new NotificationCompat.Action(drawable, otherAction.getTitle(), otherAction.getActionIntent()));
            wearableExtender.addAction(otherAction);
        }

        builder.extend(wearableExtender);

        return builder.build();
    }

    private synchronized void showMessageNotifications(String ticker) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
        String text = "";
        final List<Notification> notifications = getMessageNotifications();

        int notify_type = Integer.parseInt(prefs.getString("notify_type", "1"));
        boolean notify = false;
        if (notify_type == 1 || (notify_type == 2 && NetworkConnection.getInstance().isVisible()))
            notify = true;

        if (notifications.size() > 0 && notify) {
            int lastbid = notifications.get(0).getBid();
            int count = 0;
            long[] eids = new long[notifications.size()];
            ArrayList<Notification> messages = new ArrayList<>(notifications.size());
            HashMap<String, Bitmap> avatars = new HashMap<>();
            Notification last = notifications.get(0);
            boolean show = false;
            boolean downloading = false;
            Bitmap avatar = null;
            for (Notification n : notifications) {
                if (n.getBid() != lastbid) {
                    if (show) {
                        String title = last.getChan();
                        if (title == null || title.length() == 0)
                            title = last.getNick();
                        if (title == null || title.length() == 0)
                            title = last.getNetwork();

                        Intent replyIntent = new Intent(RemoteInputService.ACTION_REPLY);
                        replyIntent.putExtra("bid", last.getBid());
                        replyIntent.putExtra("cid", last.getCid());
                        replyIntent.putExtra("eids", eids);
                        replyIntent.putExtra("network", last.getNetwork());
                        replyIntent.putExtra("chan", last.getChan());
                        replyIntent.putExtra("buffer_type", last.getBuffer_type());
                        replyIntent.putExtra("to", last.getChan());

                        String body;
                        if (last.getBuffer_type().equals("channel")) {
                            if (last.getMessage_type().equals("buffer_me_msg"))
                                body = "<b>— " + ((last.getNick() != null)? last.getNick() :getServerNick(last.getCid())) + "</b> " + last.getMessage();
                            else
                                body = "<b>&lt;" + ((last.getNick() != null)? last.getNick() :getServerNick(last.getCid())) + "&gt;</b> " + last.getMessage();
                        } else {
                            if (last.getMessage_type().equals("buffer_me_msg"))
                                body = "— " + ((last.getNick() != null)? last.getNick() :getServerNick(last.getCid())) + " " + last.getMessage();
                            else
                                body = last.getMessage();
                        }

                        ArrayList<String> lines = new ArrayList<>(Arrays.asList(text.split("<br/>")));
                        while(lines.size() > 3)
                            lines.remove(0);

                        try {
                            Crashlytics.log(Log.DEBUG, "IRCCloud", "Posting notification for type " + last.getMessage_type());
                            Bitmap large_avatar = null;

                            if(last.getAvatar_url() != null && last.getAvatar_url().length() > 0) {
                                try {
                                    URL url = new URL(last.getAvatar_url());
                                    large_avatar = ImageList.getInstance().getImage(url);
                                    if(large_avatar == null) {
                                        downloading = true;
                                        last.setShown(false);
                                        ImageList.getInstance().fetchImage(url, new ImageList.OnImageFetchedListener() {
                                            @Override
                                            public void onImageFetched(Bitmap image) {
                                                showMessageNotifications(null);
                                            }
                                        });
                                    }
                                } catch (Exception e1) {
                                }
                            }

                            if(!downloading) {
                                if (large_avatar == null) {
                                    large_avatar = AvatarsList.getInstance().getAvatar(last.getCid(), ((last.getNick() != null)? last.getNick() :getServerNick(last.getCid())), null).getBitmap(false, 512, false, false);
                                }
                                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify(lastbid, buildNotification(ticker, last.getCid(), lastbid, eids, title, body, count, replyIntent, last.getNetwork(), messages, null, avatars.get(last.getNick()), large_avatar, avatars));
                            }
                        } catch (Exception e) {
                            Crashlytics.logException(e);
                        }
                    }
                    lastbid = n.getBid();
                    text = "";
                    count = 0;
                    eids = new long[notifications.size()];
                    show = false;
                    messages.clear();
                    avatars.clear();
                }

                if (text.length() > 0)
                    text += "<br/>";
                if (n.getBuffer_type().equals("conversation") && n.getMessage_type().equals("buffer_me_msg"))
                    text += "— " + n.getMessage();
                else if (n.getBuffer_type().equals("conversation"))
                    text += n.getMessage();
                else if (n.getMessage_type().equals("buffer_me_msg"))
                    text += "<b>— " + ((n.getNick() != null)? n.getNick() :getServerNick(n.getCid())) + "</b> " + n.getMessage();
                else
                    text += "<b>" + ((n.getNick() != null)? n.getNick() :getServerNick(n.getCid())) + "</b> " + n.getMessage();

                if (!n.isShown()) {
                    n.setShown(true);
                    show = true;
                }

                avatar = null;
                if(n.getAvatar_url() != null && n.getAvatar_url().length() > 0) {
                    try {
                        URL url = new URL(n.getAvatar_url());
                        avatar = ImageList.getInstance().getImage(url);
                        if(avatar == null) {
                            downloading = true;
                            show = false;
                            n.setShown(false);
                            ImageList.getInstance().fetchImage(url, new ImageList.OnImageFetchedListener() {
                                @Override
                                public void onImageFetched(Bitmap image) {
                                    showMessageNotifications(null);
                                }
                            });
                        }
                    } catch (Exception e1) {
                    }
                }

                if(!downloading && avatar == null) {
                    avatar = AvatarsList.getInstance().getAvatar(n.getCid(), ((n.getNick() != null)? n.getNick() :getServerNick(n.getCid())), null).getBitmap(false, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, IRCCloudApplication.getInstance().getApplicationContext().getResources().getDisplayMetrics()), false, Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
                }

                if(avatar != null)
                    avatars.put(((n.getNick() != null)? n.getNick() :getServerNick(n.getCid())), avatar);

                messages.add(n);
                eids[count++] = n.getEid();
                if(n.getNick() != null)
                    last = n;
            }

            if (show) {
                String title = last.getChan();
                if (title == null || title.length() == 0)
                    title = last.getNetwork();

                Intent replyIntent = new Intent(RemoteInputService.ACTION_REPLY);
                replyIntent.putExtra("bid", last.getBid());
                replyIntent.putExtra("cid", last.getCid());
                replyIntent.putExtra("network", last.getNetwork());
                replyIntent.putExtra("eids", eids);
                replyIntent.putExtra("chan", last.getChan());
                replyIntent.putExtra("buffer_type", last.getBuffer_type());
                replyIntent.putExtra("to", last.getChan());

                String body = "";
                if (last.getBuffer_type().equals("channel")) {
                    if (last.getMessage_type().equals("buffer_me_msg"))
                        body = "<b>— " + ((last.getNick() != null)? last.getNick() :getServerNick(last.getCid())) + "</b> " + last.getMessage();
                    else
                        body = "<b>&lt;" + ((last.getNick() != null)? last.getNick() :getServerNick(last.getCid())) + "&gt;</b> " + last.getMessage();
                } else {
                    if (last.getMessage_type().equals("buffer_me_msg"))
                        body = "— " + ((last.getNick() != null)? last.getNick() :getServerNick(last.getCid())) + " " + last.getMessage();
                    else
                        body = last.getMessage();
                }

                ArrayList<String> lines = new ArrayList<>(Arrays.asList(text.split("<br/>")));
                while(lines.size() > 3)
                    lines.remove(0);

                try {
                    Crashlytics.log(Log.DEBUG, "IRCCloud", "Posting notification for type " + last.getMessage_type());
                    Bitmap large_avatar = null;

                    if(last.getAvatar_url() != null && last.getAvatar_url().length() > 0) {
                        try {
                            URL url = new URL(last.getAvatar_url());
                            large_avatar = ImageList.getInstance().getImage(url);
                            if(large_avatar == null) {
                                downloading = true;
                                last.setShown(false);
                                ImageList.getInstance().fetchImage(url, new ImageList.OnImageFetchedListener() {
                                    @Override
                                    public void onImageFetched(Bitmap image) {
                                        showMessageNotifications(null);
                                    }
                                });
                            }
                        } catch (Exception e1) {
                        }
                    }

                    if(!downloading) {
                        if (large_avatar == null) {
                            large_avatar = AvatarsList.getInstance().getAvatar(last.getCid(), ((last.getNick() != null)? last.getNick() :getServerNick(last.getCid())), null).getBitmap(false, 512, false, false);
                        }
                        NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify(lastbid, buildNotification(ticker, last.getCid(), lastbid, eids, title, body, count, replyIntent, last.getNetwork(), messages, null, avatars.get(last.getNick()), large_avatar, avatars));
                    }
                } catch (Exception e) {
                    Crashlytics.logException(e);
                }
            }

            IRCCloudDatabase.getInstance().runInTransaction(new Runnable() {
                @Override
                public void run() {
                    IRCCloudDatabase.getInstance().NotificationsDao().update(notifications);
                }
            });
        }
    }

    public NotificationCompat.Builder alert(int bid, String title, String body) {
        Crashlytics.log(Log.DEBUG, "IRCCloud", "Posting alert notification");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(IRCCloudApplication.getInstance().getApplicationContext(), "alert")
                .setContentTitle(title)
                .setContentText(body)
                .setTicker(body)
                .setAutoCancel(true)
                .setColor(IRCCloudApplication.getInstance().getApplicationContext().getResources().getColor(R.color.ic_background))
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
            cv.put("count", IRCCloudDatabase.getInstance().NotificationsDao().unread_count());
            IRCCloudApplication.getInstance().getApplicationContext().getContentResolver().insert(Uri.parse("content://com.teslacoilsw.notifier/unread_count"), cv);
        } catch (IllegalArgumentException ex) {
        } catch (Exception ex) {
            NetworkConnection.printStackTraceToCrashlytics(ex);
        }
    }

    public void addNotificationGroup(int id, String name) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ((NotificationManager)IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannelGroup(new NotificationChannelGroup(String.valueOf(id), name));
        }
    }

    public void pruneNotificationChannels() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = ((NotificationManager) IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE));

            createChannel("alert", "Alerts", NotificationManagerCompat.IMPORTANCE_DEFAULT, null);
            createChannel("highlight", "Highlights and PMs", NotificationManagerCompat.IMPORTANCE_HIGH, null);

            for (NotificationChannelGroup c : nm.getNotificationChannelGroups()) {
                try {
                    if (ServersList.getInstance().getServer(Integer.valueOf(c.getId())) == null)
                        nm.deleteNotificationChannelGroup(c.getId());
                } catch(NumberFormatException e) {
                }
            }

            String uid = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getString("uid", "uid");
            for (NotificationChannel c : nm.getNotificationChannels()) {
                try {
                    if (c.getId().startsWith("uid") && BuffersList.getInstance().getBuffer(Integer.valueOf(c.getId().substring(uid.length()))) == null)
                        nm.deleteNotificationChannel(c.getId());
                } catch(NumberFormatException e) {
                }
                try {
                    if (Integer.valueOf(c.getId()) > 0)
                        nm.deleteNotificationChannelGroup(c.getId());
                } catch(NumberFormatException e) {
                }

                if(!PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("notify_channels", false) && c.getId().startsWith("uid")) {
                    nm.deleteNotificationChannel(c.getId());
                    if(c.getGroup() != null)
                        nm.deleteNotificationChannelGroup(c.getGroup());
                }
            }
        }
    }
}