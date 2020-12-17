/*
 * Copyright (c) 2020 IRCCloud, Ltd.
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

import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

public class IRCCloudLog {
    private static final LinkedList<String> lines = new LinkedList<>();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS", Locale.US);
    public static boolean CrashlyticsEnabled = false;
    public static String defaultTag = "IRCCloud";
    public static int defaultLevel = Log.INFO;

    public static void clear() {
        synchronized (lines) {
            lines.clear();
        }
    }

    public static String lines() {
        StringBuilder b = new StringBuilder();

        synchronized (lines) {
            for (String s : lines) {
                b.append(s).append("\n");
            }
        }

        return b.toString();
    }

    public static void LogException(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        Log(Log.ERROR, defaultTag, sw.toString());

        if(CrashlyticsEnabled)
            FirebaseCrashlytics.getInstance().recordException(t);
    }

    public static void Log(String message) {
        Log(defaultLevel, defaultTag, message);
    }

    public static void Log(int priority, String tag, String message) {
        if(BuildConfig.DEBUG && !CrashlyticsEnabled)
            android.util.Log.println(priority, tag, message);

        StringBuilder b = new StringBuilder();
        b.append(dateFormat.format(new Date())).append(" ");
        switch(priority) {
            case Log.ASSERT:
                b.append("A/");
                break;
            case Log.DEBUG:
                b.append("D/");
                break;
            case Log.ERROR:
                b.append("E/");
                break;
            case Log.INFO:
                b.append("I/");
                break;
            case Log.VERBOSE:
                b.append("V/");
                break;
            case Log.WARN:
                b.append("W/");
                break;
        }
        b.append(tag).append(": ");
        b.append(message);
        synchronized (lines) {
            lines.addLast(b.toString());
            while(lines.size() > 200)
                lines.removeFirst();
        }

        if(CrashlyticsEnabled)
            FirebaseCrashlytics.getInstance().log(b.toString());
    }
}
