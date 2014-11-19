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

package com.irccloud.android;

import java.util.*;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;

import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompatExtras;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.support.v4.app.RemoteInput;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.RemoteViews;

public class Notifications {
	public class Notification {
		public int cid;
		public int bid;
		public long eid;
		public String nick;
		public String message;
		public String network;
		public String chan;
		public String buffer_type;
		public String message_type;
		public boolean shown = false;
		
		public String toString() {
			return "{cid: " + cid + ", bid: " + bid + ", eid: " + eid + ", nick: " + nick + ", message: " + message + ", network: " + network + " shown: " + shown + "}";
		}
	}
	
	public class comparator implements Comparator<Notification> {
		public int compare(Notification n1, Notification n2) {
			if(n1.cid != n2.cid)
				return Integer.valueOf(n1.cid).compareTo(n2.cid);
			else if(n1.bid != n2.bid)
				return Integer.valueOf(n1.bid).compareTo(n2.bid);
			else
				return Long.valueOf(n1.eid).compareTo(n2.eid);
		}
	}
	
	private ArrayList<Notification> mNotifications = null;
	private SparseArray<String> mNetworks = null;
	private SparseArray<Long> mLastSeenEIDs = null;
	private SparseArray<HashSet<Long>> mDismissedEIDs = null;
	
	private static Notifications instance = null;
	private int excludeBid = -1;
	private static final Timer mNotificationTimer = new Timer("notification-timer");
    private TimerTask mNotificationTimerTask = null;

	public static Notifications getInstance() {
		if(instance == null)
			instance = new Notifications();
		return instance;
	}
	
	public Notifications() {
        try {
    		load();
        } catch (Exception e) {
        }
	}

	private void load() {
		mNotifications = new ArrayList<Notification>();
		mNetworks = new SparseArray<String>();
		mLastSeenEIDs = new SparseArray<Long>();
		mDismissedEIDs = new SparseArray<HashSet<Long>>();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());

		if(prefs.contains("notifications_json")) {
			try {
				JSONArray array = new JSONArray(prefs.getString("networks_json", "{}"));
				for(int i = 0; i < array.length(); i++) {
					JSONObject o = array.getJSONObject(i);
					mNetworks.put(o.getInt("cid"), o.getString("network"));
				}
				
				array = new JSONArray(prefs.getString("lastseeneids_json", "{}"));
				for(int i = 0; i < array.length(); i++) {
					JSONObject o = array.getJSONObject(i);
					mLastSeenEIDs.put(o.getInt("bid"), o.getLong("eid"));
				}
				
				array = new JSONArray(prefs.getString("dismissedeids_json", "{}"));
				for(int i = 0; i < array.length(); i++) {
					JSONObject o = array.getJSONObject(i);
					int bid = o.getInt("bid");
					mDismissedEIDs.put(bid, new HashSet<Long>());
					
					JSONArray eids = o.getJSONArray("eids");
					for(int j = 0; j < eids.length(); j++) {
						mDismissedEIDs.get(bid).add(eids.getLong(j));
					}
				}
				
				synchronized(mNotifications) {
					array = new JSONArray(prefs.getString("notifications_json", "{}"));
					for(int i = 0; i < array.length(); i++) {
						JSONObject o = array.getJSONObject(i);
						Notification n = new Notification();
						n.bid = o.getInt("bid");
						n.cid = o.getInt("cid");
						n.eid = o.getLong("eid");
						n.nick = o.getString("nick");
						n.message = o.getString("message");
						n.chan = o.getString("chan");
						n.buffer_type = o.getString("buffer_type");
						n.message_type = o.getString("message_type");
						n.network = mNetworks.get(n.cid);
						if(o.has("shown"))
							n.shown = o.getBoolean("shown");
						mNotifications.add(n);
					}
					Collections.sort(mNotifications, new comparator());
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	private static final Timer mSaveTimer = new Timer("notifications-save-timer");
	private TimerTask mSaveTimerTask = null;

	@TargetApi(9)
	private void save() {
        if(mSaveTimerTask != null)
            mSaveTimerTask.cancel();
        mSaveTimerTask = new TimerTask() {

            @Override
            public void run() {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).edit();
                try {
                    JSONArray array = new JSONArray();
                    synchronized(mNotifications) {
                        for(Notification n : mNotifications) {
                            JSONObject o = new JSONObject();
                            o.put("cid", n.cid);
                            o.put("bid", n.bid);
                            o.put("eid", n.eid);
                            o.put("nick", n.nick);
                            o.put("message", n.message);
                            o.put("chan", n.chan);
                            o.put("buffer_type", n.buffer_type);
                            o.put("message_type", n.message_type);
                            o.put("shown", n.shown);
                            array.put(o);
                        }
                        editor.putString("notifications_json", array.toString());
                    }

                    array = new JSONArray();
                    for(int i = 0; i < mNetworks.size(); i++) {
                        int cid = mNetworks.keyAt(i);
                        String network = mNetworks.get(cid);
                        JSONObject o = new JSONObject();
                        o.put("cid", cid);
                        o.put("network", network);
                        array.put(o);
                    }
                    editor.putString("networks_json", array.toString());

                    array = new JSONArray();
                    for(int i = 0; i < mLastSeenEIDs.size(); i++) {
                        int bid = mLastSeenEIDs.keyAt(i);
                        long eid = mLastSeenEIDs.get(bid);
                        JSONObject o = new JSONObject();
                        o.put("bid", bid);
                        o.put("eid", eid);
                        array.put(o);
                    }
                    editor.putString("lastseeneids_json", array.toString());

                    array = new JSONArray();
                    for(int i = 0; i < mDismissedEIDs.size(); i++) {
                        JSONArray a = new JSONArray();
                        int bid = mDismissedEIDs.keyAt(i);
                        HashSet<Long> eids = mDismissedEIDs.get(bid);
                        for(long eid : eids) {
                            a.put(eid);
                        }
                        JSONObject o = new JSONObject();
                        o.put("bid", bid);
                        o.put("eids", a);
                        array.put(o);
                    }
                    editor.putString("dismissedeids_json", array.toString());

                    if(Build.VERSION.SDK_INT >= 9)
                        editor.apply();
                    else
                        editor.commit();
                } catch (ConcurrentModificationException e) {
                } catch (OutOfMemoryError e) {
                    editor.remove("notifications_json");
                    editor.remove("networks_json");
                    editor.remove("lastseeneids_json");
                    editor.remove("dismissedeids_json");
                    editor.commit();
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
		mSaveTimer.schedule(mSaveTimerTask, 60000);
	}
	
	public void clearDismissed() {
		mDismissedEIDs.clear();
		save();
	}
	
	public void clear() {
		try {
            NotificationManager nm = (NotificationManager)IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
			synchronized(mNotifications) {
				if(mNotifications.size() > 0) {
			        for(Notification n : mNotifications) {
                        NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int) (n.eid / 1000));
                        NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(n.bid);
			        }
				}
			}
            IRCCloudApplication.getInstance().getApplicationContext().sendBroadcast(new Intent(DashClock.REFRESH_INTENT));
            if(PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("notify_sony", false))
                NotificationUtil.deleteAllEvents(IRCCloudApplication.getInstance().getApplicationContext());
        } catch (Exception e) {
			e.printStackTrace();
		}
        if(mSaveTimerTask != null)
            mSaveTimerTask.cancel();
        mNotifications.clear();
        mLastSeenEIDs.clear();
        mDismissedEIDs.clear();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).edit();
        editor.remove("notifications_json");
        editor.remove("lastseeneids_json");
        editor.remove("dismissedeids_json");
        editor.commit();
	}

    public void clearNetworks() {
        mNetworks.clear();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).edit();
        editor.remove("networks_json");
        editor.commit();
    }

	public long getLastSeenEid(int bid) {
		if(mLastSeenEIDs.get(bid) != null)
			return mLastSeenEIDs.get(bid);
		else
			return -1;
	}
	
	public synchronized void updateLastSeenEid(int bid, long eid) {
		mLastSeenEIDs.put(bid, eid);
		save();
	}

	public synchronized boolean isDismissed(int bid, long eid) {
		if(mDismissedEIDs.get(bid) != null) {
			for(Long e : mDismissedEIDs.get(bid)) {
				if(e == eid)
					return true;
			}
		}
		return false;
	}
	
	public synchronized void dismiss(int bid, long eid) {
		if(mDismissedEIDs.get(bid) == null)
			mDismissedEIDs.put(bid, new HashSet<Long>());
		
		mDismissedEIDs.get(bid).add(eid);
		Notification n = getNotification(eid);
		synchronized(mNotifications) {
			if(n != null)
				mNotifications.remove(n);
		}
		save();
        if(IRCCloudApplication.getInstance() != null)
            IRCCloudApplication.getInstance().getApplicationContext().sendBroadcast(new Intent(DashClock.REFRESH_INTENT));
	}
	
	public synchronized void addNetwork(int cid, String network) {
		mNetworks.put(cid, network);
		save();
	}

	public synchronized void deleteNetwork(int cid) {
		mNetworks.remove(cid);
		save();
	}
	
	public synchronized void addNotification(int cid, int bid, long eid, String from, String message, String chan, String buffer_type, String message_type) {
		if(isDismissed(bid, eid)) {
			return;
		}
		long last_eid = getLastSeenEid(bid);
		if(eid <= last_eid) {
			return;
		}
		
		String network = getNetwork(cid);
		if(network == null)
			addNetwork(cid, "Unknown Network");
		Notification n = new Notification();
		n.bid = bid;
		n.cid = cid;
		n.eid = eid;
		n.nick = from;
		n.message = TextUtils.htmlEncode(message);
		n.chan = chan;
		n.buffer_type = buffer_type;
		n.message_type = message_type;
		n.network = network;
		
		synchronized(mNotifications) {
			mNotifications.add(n);
			Collections.sort(mNotifications, new comparator());
		}
		save();
	}

	public void deleteNotification(int cid, int bid, long eid) {
		synchronized(mNotifications) {
			for(Notification n : mNotifications) {
				if(n.cid == cid && n.bid == bid && n.eid == eid) {
					mNotifications.remove(n);
					save();
					return;
				}
			}
		}
	}
	
	public void deleteOldNotifications(int bid, long last_seen_eid) {
        boolean changed = false;
		if(mNotificationTimerTask != null) {
			mNotificationTimerTask.cancel();
		}

		ArrayList<Notification> notifications = getOtherNotifications();

		if(notifications.size() > 0) {
	        for(Notification n : notifications) {
	        	if(n.bid == bid && n.eid <= last_seen_eid) {
                    NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int) (n.eid / 1000));
                    changed = true;
	        	}
	        }
		}

		synchronized(mNotifications) {
			for(int i = 0; i < mNotifications.size(); i++) {
				Notification n = mNotifications.get(i);
				if(n.bid == bid && n.eid <= last_seen_eid) {
					mNotifications.remove(n);
					i--;
                    NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(bid);
                    changed = true;
				}
			}
		}
		if(mDismissedEIDs.get(bid) != null) {
			HashSet<Long> eids = mDismissedEIDs.get(bid);
			Long[] eidsArray = eids.toArray(new Long[eids.size()]);
			for(int i = 0; i < eidsArray.length; i++) {
				if(eidsArray[i] <= last_seen_eid) {
					eids.remove(eidsArray[i]);
				}
			}
		}
        if(changed) {
            IRCCloudApplication.getInstance().getApplicationContext().sendBroadcast(new Intent(DashClock.REFRESH_INTENT));
            try {
                if(PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("notify_sony", false))
                    NotificationUtil.deleteEvents(IRCCloudApplication.getInstance().getApplicationContext(),com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.FRIEND_KEY + " = ?", new String[] {String.valueOf(bid)});
            } catch (Exception e) {
            }
        }
	}
	
	public void deleteNotificationsForBid(int bid) {
		ArrayList<Notification> notifications = getOtherNotifications();
		
		if(notifications.size() > 0) {
	        for(Notification n : notifications) {
                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int)(n.eid/1000));
	        }
		}
        NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(bid);

		synchronized(mNotifications) {
			for(int i = 0; i < mNotifications.size(); i++) {
				Notification n = mNotifications.get(i);
				if(n.bid == bid) {
					mNotifications.remove(n);
					i--;
					continue;
				}
			}
		}
		mDismissedEIDs.remove(bid);
		mLastSeenEIDs.remove(bid);
        IRCCloudApplication.getInstance().getApplicationContext().sendBroadcast(new Intent(DashClock.REFRESH_INTENT));
        try {
            if (PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("notify_sony", false))
                NotificationUtil.deleteEvents(IRCCloudApplication.getInstance().getApplicationContext(), com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.FRIEND_KEY + " = ?", new String[]{String.valueOf(bid)});
        } catch (Exception e) {
            //User has probably uninstalled Sony Liveware
        }
	}
	
	private boolean isMessage(String type) {
		return !(type.equalsIgnoreCase("channel_invite") || type.equalsIgnoreCase("callerid"));
	}

    public int count() {
        return mNotifications.size();
    }

	public ArrayList<Notification> getMessageNotifications() {
		ArrayList<Notification> notifications = new ArrayList<Notification>();

		synchronized(mNotifications) {
			for(int i = 0; i < mNotifications.size(); i++) {
				Notification n = mNotifications.get(i);
				if(n.bid != excludeBid && isMessage(n.message_type)) {
					if(n.network == null)
						n.network = getNetwork(n.cid);
					notifications.add(n);
				}
			}
		}
		return notifications;
	}
	
	public ArrayList<Notification> getOtherNotifications() {
		ArrayList<Notification> notifications = new ArrayList<Notification>();

		synchronized(mNotifications) {
			for(int i = 0; i < mNotifications.size(); i++) {
				Notification n = mNotifications.get(i);
				if(n.bid != excludeBid && !isMessage(n.message_type)) {
					if(n.network == null)
						n.network = getNetwork(n.cid);
					notifications.add(n);
				}
			}
		}
		return notifications;
	}
	
	public String getNetwork(int cid) {
		return mNetworks.get(cid);
	}
	
	public Notification getNotification(long eid) {
		synchronized(mNotifications) {
			for(int i = 0; i < mNotifications.size(); i++) {
				Notification n = mNotifications.get(i);
				if(n.bid != excludeBid && n.eid == eid && isMessage(n.message_type)) {
					if(n.network == null)
						n.network = getNetwork(n.cid);
					return n;
				}
			}
		}
		return null;
	}
	
	public synchronized void excludeBid(int bid) {
		excludeBid = -1;
        ArrayList<Notification> notifications = getOtherNotifications();
		
		if(notifications.size() > 0) {
	        for(Notification n : notifications) {
                NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel((int)(n.eid/1000));
	        }
		}
        NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).cancel(bid);
		excludeBid = bid;
	}
	
	private String mTicker = null;
	
	public synchronized void showNotifications(String ticker) {
		if(ticker != null)
			mTicker = ticker;
		
		ArrayList<Notification> notifications = getMessageNotifications();
		for(Notification n : notifications) {
			if(isDismissed(n.bid, n.eid)) {
				deleteNotification(n.cid, n.bid, n.eid);
			}
		}

		if(mNotificationTimerTask != null)
			mNotificationTimerTask.cancel();

        try {
            mNotificationTimerTask = new TimerTask() {
                @Override
                public void run() {
                    showMessageNotifications(mTicker);
                    showOtherNotifications();
                    mTicker = null;
                    IRCCloudApplication.getInstance().getApplicationContext().sendBroadcast(new Intent(DashClock.REFRESH_INTENT));
                }
            };
            mNotificationTimer.schedule(mNotificationTimerTask, 5000);
        } catch (Exception e) {

        }
	}
	
	private void showOtherNotifications() {
		String title = "";
		String text = "";
		String ticker = null;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
        ArrayList<Notification> notifications = getOtherNotifications();
		
		int notify_type = Integer.parseInt(prefs.getString("notify_type", "1"));
		boolean notify = false;
		if(notify_type == 1 || (notify_type == 2 && NetworkConnection.getInstance().isVisible()))
			notify = true;
		
		if(notifications.size() > 0 && notify) {
	        for(Notification n : notifications) {
	        	if(!n.shown) {
					if(n.message_type.equals("callerid")) {
						title = "Callerid: " + n.nick + " (" + n.network + ")";
						text = n.nick + " " + n.message;
						ticker = n.nick + " " + n.message;
					} else {
						title = n.nick + " (" + n.network + ")";
						text = n.message;
						ticker = n.message;
					}
                    NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify((int) (n.eid / 1000), buildNotification(ticker, n.bid, new long[]{n.eid}, title, text, Html.fromHtml(text), 1));
			        n.shown = true;
	        	}
	        }
		}
	}
	
	@SuppressLint("NewApi")
	private android.app.Notification buildNotification(String ticker, int bid, long[] eids, String title, String text, Spanned big_text, int count) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());

		NotificationCompat.Builder builder = new NotificationCompat.Builder(IRCCloudApplication.getInstance().getApplicationContext())
		.setContentTitle(title)
		.setContentText(Html.fromHtml(text).toString())
        .setTicker(ticker)
        .setWhen(eids[0] / 1000)
        .setSmallIcon(R.drawable.ic_stat_notify)
        .setGroup(String.valueOf(bid))
        .setGroupSummary(true)
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setPriority(NotificationCompat.PRIORITY_HIGH);

        if(ticker != null && (System.currentTimeMillis() - prefs.getLong("lastNotificationTime", 0)) > 10000) {
			if(prefs.getBoolean("notify_vibrate", true))
				builder.setDefaults(android.app.Notification.DEFAULT_VIBRATE);
			String ringtone = prefs.getString("notify_ringtone", "content://settings/system/notification_sound");
			if(ringtone != null && ringtone.length() > 0)
				builder.setSound(Uri.parse(ringtone));
		}

        int led_color = Integer.parseInt(prefs.getString("notify_led_color", "1"));
		if(led_color == 1) {
            builder.setDefaults(android.app.Notification.DEFAULT_LIGHTS);
        } else if(led_color == 2) {
            builder.setLights(0xFF0000FF, 500, 500);
        }

		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong("lastNotificationTime", System.currentTimeMillis());
		editor.commit();
		
		if(count == 1)
			builder.setOnlyAlertOnce(true);
		
		Intent i = new Intent();
        i.setComponent(new ComponentName(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), "com.irccloud.android.MainActivity"));
		i.putExtra("bid", bid);
		i.setData(Uri.parse("bid://" + bid));
    	Intent dismiss = new Intent(IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.DISMISS_NOTIFICATION));
		dismiss.setData(Uri.parse("irccloud-dismiss://" + bid));
    	dismiss.putExtra("bid", bid);
		dismiss.putExtra("eids", eids);
        builder.setContentIntent(PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT));
		builder.setDeleteIntent(PendingIntent.getBroadcast(IRCCloudApplication.getInstance().getApplicationContext(), 0, dismiss, PendingIntent.FLAG_UPDATE_CURRENT));

        android.app.Notification notification = builder.build();
		RemoteViews contentView = new RemoteViews(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), R.layout.notification);
		contentView.setTextViewText(R.id.title, title);
		contentView.setTextViewText(R.id.text, text);
        contentView.setLong(R.id.time, "setTime", eids[0]/1000);
		notification.contentView = contentView;
		
		if(Build.VERSION.SDK_INT >= 16 && big_text != null) {
			RemoteViews bigContentView = new RemoteViews(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), R.layout.notification_expanded);
			bigContentView.setTextViewText(R.id.title, title);
			bigContentView.setTextViewText(R.id.text, big_text);
            bigContentView.setLong(R.id.time, "setTime", eids[0]/1000);
			if(count > 4) {
				bigContentView.setViewVisibility(R.id.divider, View.VISIBLE);
				bigContentView.setViewVisibility(R.id.more, View.VISIBLE);
				bigContentView.setTextViewText(R.id.more, "+" + (count - 4) + " more");
			} else {
				bigContentView.setViewVisibility(R.id.divider, View.GONE);
				bigContentView.setViewVisibility(R.id.more, View.GONE);
			}
			notification.bigContentView = bigContentView;
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
		String title = "";
		String text = "";
		ArrayList<Notification> notifications = getMessageNotifications();

		int notify_type = Integer.parseInt(prefs.getString("notify_type", "1"));
		boolean notify = false;
		if(notify_type == 1 || (notify_type == 2 && NetworkConnection.getInstance().isVisible()))
			notify = true;
		
		if(notifications.size() > 0 && notify) {
			int lastbid = notifications.get(0).bid;
			int count = 0;
			long[] eids = new long[notifications.size()];
			Notification last = null;
			count = 0;
    		title = notifications.get(0).chan + " (" + notifications.get(0).network + ")";
    		boolean show = false;
	        for(Notification n : notifications) {
	        	if(n.bid != lastbid) {
	        		if(show) {
						if(count == 1) {
							if(last.nick != null && last.nick.length() > 0) {
								title = last.nick;
								if(!last.buffer_type.equals("conversation") && !last.message_type.equals("wallops") && last.chan.length() > 0)
									title += " in " + last.chan;
								title += " (" + last.network + ")";
							} else {
								title = last.network;
							}
				        	if(last.message_type.equals("buffer_me_msg"))
				        		text = "— " + last.message;
				        	else
				        		text = last.message;

                            NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify(lastbid, buildNotification(ticker, lastbid, eids, title, Html.fromHtml(text).toString(), Html.fromHtml(text), count));
						} else {
                            NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify(lastbid, buildNotification(ticker, lastbid, eids, title, count + " unread highlight" + ((count == 1) ? "." : "s."), Html.fromHtml(text), count));
						}
	        		}
	        		lastbid = n.bid;
	        		if(n.chan != null && n.chan.length() > 0)
	        			title = n.chan + " (" + n.network + ")";
	        		else
	        			title = n.network;
	        		text = "";
					count = 0;
					eids = new long[notifications.size()];
					show = false;
	        	}
	        	if(count < 4) {
	        		if(text.length() > 0)
	        			text += "<br/>";
		        	if(n.buffer_type.equals("conversation") && n.message_type.equals("buffer_me_msg"))
		        		text += "— " + n.message;
		        	else if(n.buffer_type.equals("conversation"))
		        		text += n.message;
		        	else if(n.message_type.equals("buffer_me_msg"))
			    		text += "<b>— " + n.nick + "</b> " + n.message;
		        	else
			    		text += "<b>" + n.nick + "</b> " + n.message;
	        	}
	        	if(!n.shown) {
	        		n.shown = true;
	        		show = true;

                    if(prefs.getBoolean("notify_sony", false)) {
                        long time = System.currentTimeMillis();
                        long sourceId = NotificationUtil.getSourceId(IRCCloudApplication.getInstance().getApplicationContext(), SonyExtensionService.EXTENSION_SPECIFIC_ID);
                        if (sourceId == NotificationUtil.INVALID_ID) {
                            Log.e("IRCCloud", "SONY: Failed to insert data");
                            return;
                        }

                        ContentValues eventValues = new ContentValues();
                        eventValues.put(com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.EVENT_READ_STATUS, false);
                        eventValues.put(com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.DISPLAY_NAME, n.nick);

                        if(n.buffer_type.equals("channel") && n.chan != null && n.chan.length() > 0)
                            eventValues.put(com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.TITLE, n.chan);
                        else
                            eventValues.put(com.sonyericsson.extras.liveware.aef.notification.Notification.EventColumns.TITLE, n.network);

                        if(n.message_type.equals("buffer_me_msg"))
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

                    if(prefs.getBoolean("notify_pebble", false)) {
                        String pebbleTitle = n.network + ":\n";
                        String pebbleBody = "";
                        if(n.buffer_type.equals("channel") && n.chan != null && n.chan.length() > 0)
                            pebbleTitle = n.chan + ":\n";

                        if(n.message_type.equals("buffer_me_msg"))
                            pebbleBody = "— " + n.message;
                        else
                            pebbleBody = n.message;

                        if(n.nick != null && n.nick.length() > 0)
                            notifyPebble(n.nick, pebbleTitle + Html.fromHtml(pebbleBody).toString());
                        else
                            notifyPebble(n.network, pebbleTitle + Html.fromHtml(pebbleBody).toString());
                    }

                    if(Build.VERSION.SDK_INT >= 17) {
                        String wearTitle = n.nick;
                        String wearBody = Html.fromHtml(n.message).toString();
                        if (n.buffer_type.equals("channel") && n.chan != null && n.chan.length() > 0) {
                            wearTitle = n.chan;
                            wearBody = "<" + n.nick + "> " + n.message;
                        }
                        if (n.message_type.equals("buffer_me_msg"))
                            wearBody = "— " + wearBody;

                        NotificationCompat.Builder builder = new NotificationCompat.Builder(IRCCloudApplication.getInstance().getApplicationContext())
                                .setContentTitle(wearTitle)
                                .setContentText(wearBody)
                                .setWhen(n.eid / 1000)
                                .setGroup(String.valueOf(n.bid))
                                .setSmallIcon(R.drawable.ic_stat_notify);

                        Intent i = new Intent();
                        i.setComponent(new ComponentName(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), "com.irccloud.android.MainActivity"));
                        i.putExtra("bid", n.bid);
                        i.setData(Uri.parse("bid://" + n.bid));
                        builder.setContentIntent(PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), (int)n.eid, i, PendingIntent.FLAG_UPDATE_CURRENT));

                        Intent replyIntent = new Intent(RemoteInputService.ACTION_REPLY);
                        replyIntent.putExtra("cid", n.cid);
                        replyIntent.putExtra("bid", n.bid);
                        replyIntent.putExtra("eid", n.eid);
                        if (n.buffer_type.equals("channel")) {
                            replyIntent.putExtra("to", n.chan);
                            replyIntent.putExtra("nick", n.nick);
                        } else {
                            replyIntent.putExtra("to", n.nick);
                        }

                        builder.extend(new WearableExtender().addAction(new NotificationCompat.Action.Builder(R.drawable.ic_reply,
                                "Reply", PendingIntent.getService(IRCCloudApplication.getInstance().getApplicationContext(), (int)(n.eid / 1000), replyIntent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_CANCEL_CURRENT))
                                .addRemoteInput(new RemoteInput.Builder("extra_reply").setLabel("Reply").build())
                                .build()));
                        NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify((int) (n.eid / 1000), builder.build());
                    }
                }
	        	eids[count++] = n.eid;
	        	last = n;
	        }
	        if(show) {
				if(count == 1) {
					if(last.nick != null && last.nick.length() > 0) {
						title = last.nick;
						if(!last.buffer_type.equals("conversation") && !last.message_type.equals("wallops") && last.chan.length() > 0)
							title += " in " + last.chan;
						title += " (" + last.network + ")";
					} else {
						title = last.network;
					}
		        	if(last.message_type.equals("buffer_me_msg"))
		        		text = "— " + last.message;
		        	else
		        		text = last.message;
                    NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify(lastbid, buildNotification(ticker, lastbid, eids, title, Html.fromHtml(text).toString(), Html.fromHtml(text), count));
				} else {
                    NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify(lastbid, buildNotification(ticker, lastbid, eids, title, count + " unread highlight" + ((count == 1) ? "." : "s."), Html.fromHtml(text), count));
				}
	        }
		}
	}

    public void alert(int bid, String title, String body) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(IRCCloudApplication.getInstance().getApplicationContext())
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_stat_notify);

        Intent i = new Intent();
        i.setComponent(new ComponentName(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), "com.irccloud.android.MainActivity"));
        i.putExtra("bid", bid);
        i.setData(Uri.parse("bid://" + bid));
        builder.setContentIntent(PendingIntent.getActivity(IRCCloudApplication.getInstance().getApplicationContext(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT));

        NotificationManagerCompat.from(IRCCloudApplication.getInstance().getApplicationContext()).notify((int)(System.currentTimeMillis() / 1000), builder.build());
    }
}