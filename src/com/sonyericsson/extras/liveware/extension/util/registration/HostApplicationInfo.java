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

package com.sonyericsson.extras.liveware.extension.util.registration;

import com.sonyericsson.extras.liveware.aef.registration.Registration.Device;
import com.sonyericsson.extras.liveware.aef.registration.Registration.DeviceColumns;
import com.sonyericsson.extras.liveware.extension.util.Dbg;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

import java.util.ArrayList;
import java.util.List;

/**
 * The host application class contains information about a host application.
 */
public class HostApplicationInfo {

    private final long mId;

    private final String mPackageName;

    private final int mWidgetApiVersion;

    private final int mControlApiVersion;

    private final int mSensorApiVersion;

    private final int mNotificationApiVersion;

    private final int mWidgetRefreshRate;

    private final Context mContext;

    private List<DeviceInfo> mDevices = null;

    /**
     * Create host application info.
     *
     * @param context The context.
     * @param packageName The package name.
     * @param id The host application id.
     * @param widgetApiVersion The widget API version.
     * @param controlApiVersion The control API version.
     * @param sensorApiVersion The sensor API version.
     * @param notificationApiVersion The notification API version.
     * @param widgetRefreshRate The widget refresh rate.
     */
    public HostApplicationInfo(final Context context, final String packageName, final long id,
            final int widgetApiVersion, final int controlApiVersion, final int sensorApiVersion,
            final int notificationApiVersion, final int widgetRefreshRate) {
        mContext = context;
        mPackageName = packageName;
        mId = id;
        mWidgetApiVersion = widgetApiVersion;
        mControlApiVersion = controlApiVersion;
        mSensorApiVersion = sensorApiVersion;
        mNotificationApiVersion = notificationApiVersion;
        mWidgetRefreshRate = widgetRefreshRate;
    }

    /**
     * Get the devices for this host application.
     *
     * @return List of the devices.
     */
    public List<DeviceInfo> getDevices() {
        if (mDevices != null) {
            // List of devices already available. Avoid re-reading from database.
            return mDevices;
        }

        mDevices = new ArrayList<DeviceInfo>();

        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(Device.URI, null,
                    DeviceColumns.HOST_APPLICATION_ID + " = " + mId, null, null);
            while (cursor != null && cursor.moveToNext()) {
                long deviceId = cursor.getLong(cursor.getColumnIndexOrThrow(DeviceColumns._ID));
                int widgetWidth = cursor.getInt(cursor
                        .getColumnIndexOrThrow(DeviceColumns.WIDGET_IMAGE_WIDTH));
                int widgetHeight = cursor.getInt(cursor
                        .getColumnIndexOrThrow(DeviceColumns.WIDGET_IMAGE_HEIGHT));
                boolean vibrator = (cursor.getInt(cursor
                        .getColumnIndexOrThrow(DeviceColumns.VIBRATOR)) == 1);

                DeviceInfo device = new DeviceInfo(mContext, mPackageName, deviceId, widgetWidth,
                        widgetHeight, vibrator);
                mDevices.add(device);
            }
        } catch (SQLException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query device", e);
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query device", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query device", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return mDevices;
    }

    /**
     * Get the id.
     *
     * @see egistration.HostAppColumns.#_ID
     *
     * @return The id.
     */
    public long getId() {
        return mId;
    }

    /**
     * Get the widget API version.
     *
     * @see Registration.HostAppColumns.#WIDGET_API_VERSION
     *
     * @return The widget API version.
     */
    public int getWidgetApiVersion() {
        return mWidgetApiVersion;
    }

    /**
     * Get the control API version.
     *
     * @see Registration.HostAppColumns.#CONTROL_API_VERSION
     *
     * @return The control API version.
     */
    public int getControlApiVersion() {
        return mControlApiVersion;
    }

    /**
     * Get the sensor API version.
     *
     * @see Registration.HostAppColumns.#SENSOR_API_VERSION
     *
     * @return The sensor API version.
     */
    public int getSensorApiVersion() {
        return mSensorApiVersion;
    }

    /**
     * Get the notification API version.
     *
     * @see Registration.HostAppColumns.#NOTIFICATION_API_VERSION
     *
     * @return The notification API version.
     */
    public int getNotificationApiVersion() {
        return mNotificationApiVersion;
    }

    /**
     * Get the widget refresh rate.
     *
     * @see Registration.HostAppColumns.#WIDGET_REFRESH_RATE
     *
     * @return The widget refresh rate.
     */
    public int getWidgetRefreshRate() {
        return mWidgetRefreshRate;
    }
}
