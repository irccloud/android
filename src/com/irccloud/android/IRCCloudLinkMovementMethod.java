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

package com.irccloud.android;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Browser;
import android.support.customtabs.CustomTabsIntent;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.android.sdk.multiwindow.SMultiWindowActivity;

import org.chromium.customtabsclient.shared.CustomTabsHelper;

public class IRCCloudLinkMovementMethod extends LinkMovementMethod {
    private static IRCCloudLinkMovementMethod instance = null;

    public synchronized static IRCCloudLinkMovementMethod getInstance() {
        if (instance == null)
            instance = new IRCCloudLinkMovementMethod();
        return instance;
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();
            x += widget.getScrollX();
            y += widget.getScrollY();
            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);
            URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);
            if (link.length != 0) {
                if (action == MotionEvent.ACTION_UP) {
                    Uri uri = Uri.parse(link[0].getURL());
                    Context context = widget.getContext();
                    if(!PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("browser", false) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 && uri.getScheme().startsWith("http") && CustomTabsHelper.getPackageNameToUse(context) != null) {
                        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                        builder.setToolbarColor(ColorScheme.getInstance().navBarColor);
                        builder.addDefaultShareMenuItem();
                        builder.addMenuItem("Copy URL", PendingIntent.getBroadcast(context, 0, new Intent(context, ChromeCopyLinkBroadcastReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT));
                        CustomTabsIntent intent = builder.build();
                        intent.intent.setData(uri);
                        if(Build.VERSION.SDK_INT >= 22)
                            intent.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse(Intent.URI_ANDROID_APP_SCHEME + "//" + context.getPackageName()));
                        if (Build.VERSION.SDK_INT >= 16 && intent.startAnimationBundle != null) {
                            context.startActivity(intent.intent, intent.startAnimationBundle);
                        } else {
                            context.startActivity(intent.intent);
                        }
                    } else {
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
                        if(Build.VERSION.SDK_INT >= 22)
                            intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse(Intent.URI_ANDROID_APP_SCHEME + "//" + context.getPackageName()));
                        try {
                            context.startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(context, "Unable to find an application to handle this URL scheme", Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Selection.setSelection(buffer, buffer.getSpanStart(link[0]), buffer.getSpanEnd(link[0]));
                }
                return true;
            } else {
                Selection.removeSelection(buffer);
            }
        }
        return false;
    }
}
