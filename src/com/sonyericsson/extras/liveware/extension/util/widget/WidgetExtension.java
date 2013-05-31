/*
Copyright (c) 2011, Sony Ericsson Mobile Communications AB

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the Sony Ericsson Mobile Communications AB nor the names
  of its contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.sonyericsson.extras.liveware.extension.util.widget;

import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.aef.widget.Widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;

import java.io.ByteArrayOutputStream;

/**
 * The widget extension handles a widget on an accessory.
 */
public abstract class WidgetExtension {

    private boolean mStarted = false;

    protected final Context mContext;

    protected final String mHostAppPackageName;

    public static final String SCHEDULED_REFRESH_INTENT = "com.sonyericsson.extras.liveware.extension.util.widget.scheduled.refresh";

    /**
     * Create widget extension.
     *
     * @param context The context.
     * @param hostAppPackageName Package name of host application.
     */
    public WidgetExtension(Context context, String hostAppPackageName) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        if (hostAppPackageName == null) {
            throw new IllegalArgumentException("hostAppPackageName == null");
        }
        mContext = context;
        mHostAppPackageName = hostAppPackageName;
    }

    /**
     * Start widget refresh.
     */
    public final void startRefresh() {
        mStarted = true;
        onStartRefresh();
    }

    /**
     * Stop widget refresh.
     */
    public final void stopRefresh() {
        mStarted = false;
        onStopRefresh();
    }

    /**
     * Destroy widget.
     */
    public final void destroy() {
        // If started then stop it first.
        if (mStarted) {
            stopRefresh();
        }

        onDestroy();
    }

    /**
     * Start refreshing the widget. The widget is now visible.
     */
    public abstract void onStartRefresh();

    /**
     * Stop refreshing the widget. The widget is no longer visible.
     */
    public abstract void onStopRefresh();

    /**
     * Override this method to take action on scheduled refresh.
     *
     * Example of how to schedule a refresh every 10th second in
     * {@link #onStartRefresh()} and cancel it in {@link #onStopRefresh()}
     *
     * <pre>
     * public void startRefresh() {
     *     // Update now and every 10th second
     *     scheduleRepeatingRefresh(System.currentTimeMillis(), 10 * 1000,
     *             SampleWidgetService.EXTENSION_KEY);
     * }
     *
     * public void stopRefresh() {
     *     cancelScheduledRefresh(SampleWidgetService.EXTENSION_KEY);
     * }
     *
     * public void onScheduledRefresh() {
     *     // Update widget...
     * }
     * </pre>
     *
     *
     * @see #scheduleRefresh(long, String)
     * @see #scheduleRepeatingRefresh(long, long, String)
     * @see #cancelScheduledRefresh(String)
     */
    public void onScheduledRefresh() {

    }

    /**
     * Utility that creates the pending intent used to schedule a refresh or to
     * cancel refreshing
     *
     * @see #onScheduledRefresh()
     *
     * @return The pending intent
     */
    private PendingIntent createPendingRefreshIntent(String extensionKey) {
        Intent intent = new Intent(SCHEDULED_REFRESH_INTENT);
        intent.putExtra(Widget.Intents.EXTRA_EXTENSION_KEY, extensionKey);
        intent.putExtra(Widget.Intents.EXTRA_AHA_PACKAGE_NAME, mHostAppPackageName);
        intent.setPackage(mContext.getPackageName());
        PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        return pi;
    }

    /**
     * Schedule a repeating refresh.
     *
     * @see #onScheduledRefresh()
     * @see #scheduleRefresh(long, String)
     * @see #cancelScheduledRefresh(String)
     *
     * @param triggerAtTime Time the scheduled refresh should trigger first time
     *            in {@link System#currentTimeMillis()} time.
     * @param interval Interval between subsequent repeats of the scheduled
     *            refresh.
     * @param extensionKey The extension key
     */
    protected void scheduleRepeatingRefresh(long triggerAtTime, long interval, String extensionKey) {
        AlarmManager am = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);

        am.setRepeating(AlarmManager.RTC_WAKEUP, triggerAtTime, interval,
                createPendingRefreshIntent(extensionKey));
    }

    /**
     * Schedule a refresh.
     *
     * @param triggerAtTime Time the scheduled refresh should trigger in
     *            {@link System#currentTimeMillis()} time.
     * @param extensionKey The extension key
     */
    protected void scheduleRefresh(long triggerAtTime, String extensionKey) {
        AlarmManager am = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);

        am.set(AlarmManager.RTC_WAKEUP, triggerAtTime,
                createPendingRefreshIntent(extensionKey));
    }

    /**
     * Cancel any pending scheduled refresh associated with the extension key.
     *
     * @param extensionKey The extension key
     */
    protected void cancelScheduledRefresh(String extensionKey) {
        AlarmManager mgr = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(createPendingRefreshIntent(extensionKey));
    }

    /**
     * Take action based on request code
     *
     * @see WidgetReceiver#doActionOnAllWidgets(int)
     *
     * @param requestCode Code used to distinguish between different actions.
     * @param bundle Optional bundle with additional information.
     */
    public void onDoAction(int requestCode, Bundle bundle) {

    }

    /**
     * Called to notify a widget extension that it is no longer used and is
     * being removed. The widget extension should clean up any resources it
     * holds (threads, registered receivers, etc) at this point.
     */
    public void onDestroy() {

    }

    /**
     * The widget has been touched. Override to handle touch events.
     *
     * @param type The type of touch event.
     * @param x The x position of the touch event.
     * @param y The y position of the touch event.
     */
    public void onTouch(final int type, final int x, final int y) {

    }

    /**
     * Sends an image to the host application.
     *
     * @param resourceId The image resource id.
     */
    protected void sendImageToHostApp(final int resourceId) {
        Intent intent = new Intent();
        intent.setAction(Widget.Intents.WIDGET_IMAGE_UPDATE_INTENT);
        BitmapDrawable bmd = (BitmapDrawable)mContext.getResources().getDrawable(resourceId);
        ByteArrayOutputStream os = new ByteArrayOutputStream(256);
        Bitmap bm = bmd.getBitmap();
        bm.compress(CompressFormat.PNG, 100, os);
        byte[] buffer = os.toByteArray();
        intent.putExtra(Widget.Intents.EXTRA_WIDGET_IMAGE_DATA, buffer);

        sendToHostApp(intent);
    }

    /**
     * Send intent to host application. Adds host application package name and
     * our package name.
     *
     * @param intent The intent to send.
     */
    protected void sendToHostApp(final Intent intent) {
        intent.putExtra(Widget.Intents.EXTRA_AEA_PACKAGE_NAME, mContext.getPackageName());
        intent.setPackage(mHostAppPackageName);
        mContext.sendBroadcast(intent, Registration.HOSTAPP_PERMISSION);
    }

    /**
     * Show bitmap on accessory.
     *
     * @param bitmap The bitmap to show.
     */
    protected void showBitmap(final Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(256);
        bitmap.compress(CompressFormat.PNG, 100, outputStream);

        Intent intent = new Intent(Widget.Intents.WIDGET_IMAGE_UPDATE_INTENT);
        intent.putExtra(Widget.Intents.EXTRA_WIDGET_IMAGE_DATA, outputStream.toByteArray());

        sendToHostApp(intent);
    }

}
