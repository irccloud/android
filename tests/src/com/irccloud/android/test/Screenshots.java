package com.irccloud.android.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.irccloud.android.activity.MainActivity;
import com.irccloud.android.data.collection.ImageList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiSelector;
import tools.fastlane.screengrab.Screengrab;
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 18)
public class Screenshots {
    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

    private String theme;

    @Before
    public void setup() {
        theme = "dawn";
        Bundle extras = InstrumentationRegistry.getArguments();
        if(extras != null) {
            if(extras.containsKey("theme"))
                theme = extras.getString("theme");
        }

        Context context = getInstrumentation().getTargetContext();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString("theme", theme);
        editor.putBoolean("monospace", theme.equals("ash"));
        editor.commit();

        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

        SystemClock.sleep(5000);

        ImageList.getInstance().purge();
        ImageList.getInstance().clearFailures();

        activityRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activityRule.getActivity().recreate();
            }
        });
        SystemClock.sleep(5000);
    }

    @Test
    public void testTakeScreenshotsPortrait() {
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        try {
            device.setOrientationNatural();
        } catch (Exception e) {

        }
        SystemClock.sleep(5000);
        device.findObject(new UiSelector().className("com.irccloud.android.IRCEditText").focused(false));
        Screengrab.screenshot("messages-portrait-" + theme);
        if(theme.equals("dawn")) {
            try {
                device.findObject(new UiSelector().description("Channel members list")).click();
                SystemClock.sleep(5000);
                Screengrab.screenshot("members-portrait-" + theme);
                device.pressBack();
            } catch (Exception e) {

            }
        }
    }

    @Test
    public void testTakeScreenshotsLandscape() {
        if(theme.equals("dawn")) {
            UiDevice device = UiDevice.getInstance(getInstrumentation());
            try {
                device.setOrientationLeft();
            } catch (Exception e) {

            }
            SystemClock.sleep(5000);
            device.findObject(new UiSelector().className("com.irccloud.android.IRCEditText").enabled(false));
            Screengrab.screenshot("messages-landscape-" + theme);
        }
    }
}