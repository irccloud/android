package com.irccloud.android;

import java.io.IOException;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.util.Log;

import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService {

	@Override
	protected void onError(Context context, String errorId) {
		Log.e("IRCCloud", "GCM Error: " + errorId);
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		Log.i("IRCCloud", "Recieved GCM message!");
        Notification noti = new Notification.InboxStyle(
      	      new Notification.Builder(this)
      	         .setContentTitle("7 unread highlights")
      	         .setSmallIcon(R.drawable.ic_launcher))
      	      .addLine(Html.fromHtml("<b>IRCCloud</b>"))
      	      .addLine(Html.fromHtml("• #alpha: &lt;<b>@RJ</b>&gt; sam: hello from IRCCloud this line is long…"))
      	      .addLine(Html.fromHtml("• #mobiledev: &lt;<b>@james</b>&gt; sam: hi"))
      	      .addLine("+2 more")
      	      .addLine(Html.fromHtml("<b>Last.fm</b>"))
      	      .addLine(Html.fromHtml("• #last.social: &lt;<b>jonty</b>&gt; pub"))
      	      .setSummaryText("+2 more")
      	      .build();
      
      NotificationManager mNotificationManager =
          (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      mNotificationManager.notify(123, noti);
	}

	@Override
	protected void onRegistered(Context context, String regId) {
		Log.i("IRCCloud", "GCM registered, id: " + regId);
		try {
			NetworkConnection.getInstance().registerGCM(regId);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onUnregistered(Context context, String regId) {
		Log.i("IRCCloud", "GCM unregistered, id: " + regId);
	}

}
