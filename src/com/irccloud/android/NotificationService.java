
package com.irccloud.android;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import com.irccloud.android.BackgroundTaskService;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.NetworkConnection;
import com.google.android.gms.common.GooglePlayServicesUtil;

import android.content.Context;
import android.util.Log;
import com.crashlytics.android.Crashlytics;
import me.pushy.sdk.Pushy;
import org.json.JSONObject;
import java.io.IOException;
import java.util.List;


public class NotificationService extends IntentService {
    private String baseUrl;

    public NotificationService() {
        super("IRCCloudNotificationService");
    }

    public void onCreate() {
        super.onCreate();
    }

    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (action.equals("com.irccloud.android.notificationService.registerUser")){
            onRegisterUser(intent);
        }
        else if (action.equals("com.irccloud.android.notificationService.unregisterUser")){
            onUnregisterUser(intent);
        }
        else {
        }
    }

    private void onRegisterUser(Intent intent) {
        if(BuildConfig.GCM_ID.length() > 0 && GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) != 0){
		BackgroundTaskService.registerGCM(this);
        } else {
		onPushyRegister(this);
        }
    }

    private void onUnregisterUser(Intent intent) {
        if(GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) != 0){
		BackgroundTaskService.unregisterGCM(this);
        } else {
		onPushyUnregister(this);
        }
    }
    private int onPushyRegister(Context context){

        try {
            Crashlytics.log(Log.INFO, "IRCCloud", "Registering for Pushy");
            String session = NetworkConnection.getInstance().session;
            String token = Pushy.register(IRCCloudApplication.getInstance().getApplicationContext());
            SharedPreferences.Editor editor = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).edit();
            editor.putString("pushy_token", token);
            editor.commit();
            if(token != null && token.length() > 0) {
                JSONObject result = NetworkConnection.getInstance().registerPushy(token, session);
                if (result.has("success")) {
                    if(result.getBoolean("success")) {
                        Crashlytics.log(Log.INFO, "IRCCloud", "Device successfully registered");
                        return 0;
                    } else {
                        Crashlytics.log(Log.ERROR, "IRCCloud", "Unable to register device: " + result.toString());
                        return 2;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Crashlytics.log(Log.ERROR, "IRCCloud", "GCM registration failed");

        return 2;

    }
    private int onPushyUnregister(Context context){
        try {
            Crashlytics.log(Log.INFO, "IRCCloud", "Unregistering Pushy");
            final String session = NetworkConnection.getInstance().session;
            final String token = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).getString("pushy_token", null);
            JSONObject result = NetworkConnection.getInstance().unregisterPushy(token, session);
            if (result.has("success")) {
                if(result.getBoolean("success")) {
                    Crashlytics.log(Log.INFO, "IRCCloud", "Device successfully unregistered");
                    SharedPreferences.Editor e = context.getSharedPreferences("prefs", 0).edit();
                    e.remove(session);
                    e.commit();
                    return 0;
                } else {
                    Crashlytics.log(Log.ERROR, "IRCCloud", "Unable to unregister device: " + result.toString());
                    return 2;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Crashlytics.log(Log.ERROR, "IRCCloud", "Pushy unregistration failed");

        return 2;
    }
}


