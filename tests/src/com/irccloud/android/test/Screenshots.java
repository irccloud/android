package com.irccloud.android.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.irccloud.android.activity.MainActivity;

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
        editor.commit();

        Screengrab.setDefaultScreenshotStrategy(new UiAutomatorScreenshotStrategy());

        activityRule.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activityRule.getActivity().recreate();
            }
        });
    }

    @Test
    public void testTakeScreenshotsPortrait() {
        UiDevice device = UiDevice.getInstance(getInstrumentation());
        try {
            device.setOrientationNatural();
        } catch (Exception e) {

        }
        SystemClock.sleep(5000);
        device.findObject(new UiSelector().className("com.irccloud.android.IRCEditText").enabled(false));
        Screengrab.screenshot("messages-portrait-" + theme);
        try {
            device.findObject(new UiSelector().description("Channel list")).click();
            SystemClock.sleep(5000);
            Screengrab.screenshot("menu-portrait-" + theme);
            device.pressBack();
        } catch (Exception e) {

        }
        try {
            device.findObject(new UiSelector().description("Channel members list")).click();
            SystemClock.sleep(5000);
            Screengrab.screenshot("members-portrait-" + theme);
            device.pressBack();
        } catch (Exception e) {

        }
    }

    @Test
    public void testTakeScreenshotsLandscape() {
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