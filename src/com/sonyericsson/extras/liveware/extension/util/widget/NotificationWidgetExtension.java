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

import com.irccloud.android.R;

import com.sonyericsson.extras.liveware.aef.notification.Notification;
import com.sonyericsson.extras.liveware.aef.widget.Widget;
import com.sonyericsson.extras.liveware.extension.util.Dbg;
import com.sonyericsson.extras.liveware.extension.util.SmartWatchConst;
import com.sonyericsson.extras.liveware.extension.util.notification.NotificationUtil;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.format.DateUtils;

import java.util.GregorianCalendar;

/**
 * The widget extension handles a widget on an accessory.
 */
public class NotificationWidgetExtension extends WidgetExtension {

    public static final int REQUIRED_API_VERSION = 1;

    protected static final String[] EVENT_PROJECTION = {
            Notification.EventColumns.CONTACTS_REFERENCE, Notification.EventColumns.DISPLAY_NAME,
            Notification.EventColumns.FRIEND_KEY, Notification.EventColumns.TITLE,
            Notification.EventColumns.MESSAGE, Notification.EventColumns.PROFILE_IMAGE_URI,
            Notification.EventColumns.PUBLISHED_TIME, Notification.EventColumns.SOURCE_ID
    };

    protected final int mNoEventsTextResourceId;

    protected final int mDefaultSourceIconResourceId;

    protected final Handler mHandler;

    protected final String mExtensionKey;

    private EventContentObserver mEventContentObserver = null;

    protected NotificationWidgetEvent mLastEvent = null;

    /**
     * Create notification extension widget.
     *
     * @param context The context.
     * @param handler The handler.
     * @param hostAppPackageName The host app package name for this widget.
     * @param extensionKey The extension key.
     * @param noEventsTextResourceid The resource id of the string to show in
     *            the no events widget.
     * @param defaultSourceIconResourceId The resource if of the image to show
     *            when there are no events
     */
    public NotificationWidgetExtension(Context context, Handler handler, String hostAppPackageName,
            String extensionKey, int noEventsTextResourceid, int defaultSourceIconResourceId) {
        super(context, hostAppPackageName);
        if (extensionKey == null) {
            throw new IllegalArgumentException("extensionKey == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }

        mHandler = handler;

        mNoEventsTextResourceId = noEventsTextResourceid;
        mDefaultSourceIconResourceId = defaultSourceIconResourceId;

        mExtensionKey = extensionKey;

    }

    /**
     * Get supported widget width.
     *
     * @param context The context.
     * @return the width.
     */
    public static int getSupportedWidgetWidth(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.smart_watch_widget_width_outer);
    }

    /**
     * Get supported widget height.
     *
     * @param context The context.
     * @return the height.
     */
    public static int getSupportedWidgetHeight(Context context) {
        return context.getResources()
                .getDimensionPixelSize(R.dimen.smart_watch_widget_height_outer);
    }

    @Override
    public void onScheduledRefresh() {
        // Time stored in event is absolute time so we must force update of the
        // display when it is time for a scheduled refresh.
        updateWidget(false);
    }

    /**
     * Start refreshing the widget. The widget is now visible.
     */
    @Override
    public void onStartRefresh() {
        // Start observing the event table to get notified when new events
        // arrive.
        mEventContentObserver = new EventContentObserver(mHandler);
        mContext.getContentResolver().registerContentObserver(Notification.Event.URI, true,
                mEventContentObserver);

        // Widget just started. Update image.
        updateWidget(false);
    }

    /**
     * Stop refreshing the widget. The widget is no longer visible.
     */
    @Override
    public void onStopRefresh() {
        cancelScheduledRefresh(mExtensionKey);

        // Stop observing the event table.
        if (mEventContentObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mEventContentObserver);
            mEventContentObserver = null;
        }
    }

    /**
     * The widget has been touched.
     *
     * @param type The type of touch event.
     * @param x The x position of the touch event.
     * @param y The y position of the touch event.
     */
    @Override
    public void onTouch(final int type, final int x, final int y) {
        if (!SmartWatchConst.ACTIVE_WIDGET_TOUCH_AREA.contains(x, y)) {
            if (Dbg.DEBUG) {
                Dbg.d("Touch outside active area x: " + x + " y: " + y);
            }
            return;
        }

        // Both short and long tap enters next level.
        // Enter next level.
        Intent intent = new Intent(Widget.Intents.WIDGET_ENTER_NEXT_LEVEL_INTENT);
        sendToHostApp(intent);
    }

    /**
     * Get bitmap.
     *
     * @param event The event or null if no events.
     *
     * @return the bitmap to send to the host application.
     */
    public Bitmap getBitmap(final NotificationWidgetEvent event) {
        SmartWatchWidgetImage widgetImage;
        if (null == event) {
            widgetImage = new SmartWatchWidgetImage(mContext);
            widgetImage.setText(getNoEventsText());
            widgetImage.setIconByResourceId(mDefaultSourceIconResourceId);
        } else {
            widgetImage = new SmartWatchNotificationWidgetImage(mContext, event);
            widgetImage.setIconByUri(getSourceIconUri(event.getSourceId()));
            cancelScheduledRefresh(mExtensionKey);
            if (Math.abs(event.getTime() - System.currentTimeMillis()) < (DateUtils.HOUR_IN_MILLIS + DateUtils.MINUTE_IN_MILLIS)) {
                // refresh when next minute starts
                GregorianCalendar gregorianCalendar = new GregorianCalendar();
                gregorianCalendar.add(GregorianCalendar.SECOND, 1);
                gregorianCalendar.add(GregorianCalendar.MINUTE, 1);
                gregorianCalendar.set(GregorianCalendar.SECOND, 0);
                scheduleRefresh(gregorianCalendar.getTimeInMillis(), mExtensionKey);
            }
        }
        return widgetImage.getBitmap();
    }

    /**
     * Update widget.
     *
     * @param checkEvent True if we shall check if the event is new before we
     *            update. False to disable check.
     */
    protected void updateWidget(boolean checkEvent) {
        if (Dbg.DEBUG) {
            Dbg.d("updateWidget");
        }

        NotificationWidgetEvent event = getEvent();

        if (checkEvent) {
            // Check if the info is the same as the one already shown.
            if (mLastEvent != null && mLastEvent.equals(event) || mLastEvent == null
                    && event == null) {
                if (Dbg.DEBUG) {
                    Dbg.d("No change in widget data. No update.");
                }
                return;
            }
        }
        mLastEvent = event;

        showBitmap(getBitmap(event));
    }

    /**
     * Get the widget event to show.
     *
     * @return The widget event to show.
     */
    protected NotificationWidgetEvent getEvent() {
        Cursor cursor = null;
        NotificationWidgetEvent event = null;

        try {
            cursor = getEventCursor();
            if (cursor == null) {
                return null;
            }

            event = new NotificationWidgetEvent(mContext);

            // Get the contact Uri
            event.setContactReference(cursor.getString(cursor
                    .getColumnIndexOrThrow(Notification.EventColumns.CONTACTS_REFERENCE)));

            // Display name
            event.setName(cursor.getString(cursor
                    .getColumnIndexOrThrow(Notification.EventColumns.DISPLAY_NAME)));

            // Contact picture
            event.setProfileImageUri(cursor.getString(cursor
                    .getColumnIndexOrThrow(Notification.EventColumns.PROFILE_IMAGE_URI)));

            // Title
            event.setTitle(cursor.getString(cursor
                    .getColumnIndexOrThrow(Notification.EventColumns.TITLE)));

            // Message
            event.setMessage(cursor.getString(cursor
                    .getColumnIndexOrThrow(Notification.EventColumns.MESSAGE)));

            // Time
            event.setTime(cursor.getLong(cursor
                    .getColumnIndexOrThrow(Notification.EventColumns.PUBLISHED_TIME)));

            // Count
            event.setCount(getCount());

            // Source id
            event.setSourceId(cursor.getLong(cursor
                    .getColumnIndexOrThrow(Notification.EventColumns.SOURCE_ID)));

            // Friend key
            event.setFriendKey(cursor.getString(cursor
                    .getColumnIndexOrThrow(Notification.EventColumns.FRIEND_KEY)));

        } catch (SQLException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query events", e);
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query events", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query events", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return event;
    }

    /**
     * Get the event cursor for most relevant event, defined as: the event with
     * the lowest PUBLISHED_TIME with PUBLISHED_TIME > current time. If no such
     * event choose the event with highest PUBLISHED_TIME.
     *
     * @return The event cursor for the event
     */
    protected Cursor getEventCursor() {
        Cursor cursor = null;
        long now = System.currentTimeMillis();

        cursor = NotificationUtil.queryEventsFromEnabledSources(mContext, EVENT_PROJECTION,
                Notification.EventColumns.PUBLISHED_TIME + ">" + now, null,
                Notification.EventColumns.PUBLISHED_TIME + " asc limit 1");

        if (cursor == null || !cursor.moveToFirst()) {
            if (cursor != null) {
                cursor.close();
            }
            cursor = NotificationUtil.queryEventsFromEnabledSources(mContext, EVENT_PROJECTION,
                    null, null, Notification.EventColumns.PUBLISHED_TIME + " desc limit 1");
            if (cursor == null) {
                return null;
            }
            if (!cursor.moveToFirst()) {
                cursor.close();
                return null;
            }

        }

        return cursor;
    }

    /**
     * Get the number of new events.
     *
     * @return The number of new events.
     */
    protected int getCount() {
        int count = 0;
        Cursor cursor = null;
        try {
            cursor = NotificationUtil.queryEventsFromEnabledSources(mContext, null,
                    Notification.EventColumns.EVENT_READ_STATUS + "= 0", null, null);
            if (cursor != null) {
                count = cursor.getCount();
            }
        } catch (SQLException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query events", e);
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query events", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query events", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return count;
    }

    /**
     * Get the source icon uri for a source id.
     *
     * @param sourceId The source id.
     *
     * @return The source icon uri.
     */
    protected String getSourceIconUri(long sourceId) {
        String iconString = null;
        Cursor cursor = null;
        try {
            cursor = NotificationUtil.querySources(mContext, null, Notification.SourceColumns._ID
                    + "=" + sourceId, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                iconString = cursor.getString(cursor
                        .getColumnIndexOrThrow(Notification.SourceColumns.ICON_URI_1));
            }
        } catch (SQLException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query source", e);
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query source", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query source", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return iconString;
    }

    /**
     * Get the text to show in the no events widget.
     *
     * @return The text to show in the no events widget.
     */
    protected String getNoEventsText() {
        return mContext.getString(mNoEventsTextResourceId);
    }

    /**
     * The event content observer observes the event table in the notification
     * database.
     */
    private class EventContentObserver extends ContentObserver {
        public EventContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            // Update widget if the event has been changed.
            updateWidget(true);
        }
    }
}
