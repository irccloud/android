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

package com.sonyericsson.extras.liveware.extension.util.sensor;

import com.sonyericsson.extras.liveware.aef.registration.Registration.Device;
import com.sonyericsson.extras.liveware.aef.registration.Registration.DeviceColumns;
import com.sonyericsson.extras.liveware.aef.registration.Registration.HostApp;
import com.sonyericsson.extras.liveware.aef.registration.Registration.HostAppColumns;
import com.sonyericsson.extras.liveware.aef.registration.Registration.SensorColumns;
import com.sonyericsson.extras.liveware.aef.registration.Registration.SensorType;
import com.sonyericsson.extras.liveware.aef.registration.Registration.SensorTypeColumns;
import com.sonyericsson.extras.liveware.extension.util.Dbg;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.text.TextUtils;

import java.util.ArrayList;

/**
 * Manages sensors on an accessory.
 */
public class AccessorySensorManager {

    public static final int INVALID_ID = -1;

    private final Context mContext;

    private final String mHostAppPackageName;

    /**
     * Create sensor manager for a host application.
     *
     * @param context The context.
     * @param hostAppPackageName The host application package name.
     */
    public AccessorySensorManager(final Context context, final String hostAppPackageName) {
        mContext = context;
        mHostAppPackageName = hostAppPackageName;
    }

    /**
     * Get sensor.
     *
     * @param sensorType The string identifying the sensor type.
     * @return The sensor.
     */
    public AccessorySensor getSensor(final String sensorType) {
        return getSensorForType(sensorType, null);
    }

    /**
     * Get sensor with a certain delicate class.
     *
     * @param sensorType The string identifying the sensor type.
     * @param delicate True if delicate, false otherwise.
     *
     * @return The sensor.
     */
    public AccessorySensor getSensor(final String sensorType, final boolean delicate) {
        return getSensorForType(sensorType, delicate);
    }

    /**
     * Get the sensor for a specific sensorType.
     *
     * @param sensorType The string identifying the sensor type.
     * @param delicate True if delicate only, false if not delicate, null if
     *            don't care.
     *
     * @return The sensor.
     */
    private AccessorySensor getSensorForType(final String sensorType, final Boolean delicate) {
        AccessorySensorType type = getSensorType(sensorType, delicate);
        if (type == null) {
            return null;
        }

        long hostAppId = getHostAppId();
        if (hostAppId == INVALID_ID) {
            return null;
        }

        long deviceId = getDeviceId(hostAppId);
        if (deviceId == INVALID_ID) {
            return null;
        }

        Cursor cursor = null;
        AccessorySensor sensor = null;

        try {
            cursor = mContext.getContentResolver().query(
                    com.sonyericsson.extras.liveware.aef.registration.Registration.Sensor.URI,
                    null,
                    SensorColumns.SENSOR_TYPE_ID + "= ? AND " + SensorColumns.DEVICE_ID + " = ?",
                    new String[] {
                            Integer.toString(type.getId()), Long.toString(deviceId)
                    }, null);
            if (cursor != null && cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(SensorColumns.SENSOR_ID));
                boolean isInterruptSupported = cursor.getInt(cursor
                        .getColumnIndexOrThrow(SensorColumns.SUPPORTS_SENSOR_INTERRUPT)) == 1;
                String name = cursor.getString(cursor.getColumnIndexOrThrow(SensorColumns.NAME));
                int resolution = cursor.getInt(cursor
                        .getColumnIndexOrThrow(SensorColumns.RESOLUTION));
                int minimumDelay = cursor.getInt(cursor
                        .getColumnIndexOrThrow(SensorColumns.MINIMUM_DELAY));
                int maximumRange = cursor.getInt(cursor
                        .getColumnIndexOrThrow(SensorColumns.MAXIMUM_RANGE));

                sensor = new AccessorySensor(mContext, mHostAppPackageName, id, type,
                        isInterruptSupported, name, resolution, minimumDelay, maximumRange);

            }
        } catch (SQLException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query sensor", e);
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query sensor", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query sensor", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return sensor;
    }

    /**
     * Get sensor type.
     *
     * @param sensorType The string identifying the sensor type.
     * @param delicate True if delicate only, false if not delicate, null if
     *            don't care.
     * @return The sensor type.
     */
    private AccessorySensorType getSensorType(final String sensorType, final Boolean delicate) {
        Cursor cursor = null;
        AccessorySensorType type = null;

        StringBuilder builder = new StringBuilder();
        ArrayList<String> arguments = new ArrayList<String>();

        if (sensorType != null && !TextUtils.isEmpty(sensorType)) {
            builder.append(SensorTypeColumns.TYPE + " = ?");
            arguments.add(sensorType);
        }

        if (delicate != null) {
            if (builder.length() > 0) {
                builder.append(" AND ");
            }

            builder.append(SensorTypeColumns.DELICATE_SENSOR_DATA + " = ?");
            arguments.add(delicate ? "1" : "0");
        }

        String where = builder.toString();
        try {
            cursor = mContext.getContentResolver().query(SensorType.URI, new String[] {
                    SensorTypeColumns._ID, SensorTypeColumns.DELICATE_SENSOR_DATA
            }, where, arguments.toArray(new String[arguments.size()]), null);
            if (cursor != null && cursor.moveToFirst()) {
                boolean isDelicate = cursor.getInt(cursor
                        .getColumnIndexOrThrow(SensorTypeColumns.DELICATE_SENSOR_DATA)) == 1;
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(SensorTypeColumns._ID));
                type = new AccessorySensorType(sensorType, isDelicate, id);
            }
            if (type == null) {
                if (Dbg.DEBUG) {
                    Dbg.w("Failed to query SensorType");
                }
                return null;
            }
        } catch (SQLException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query sensor types", e);
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query sensor types", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query sensor types", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return type;
    }

    /**
     * Get host application id.
     *
     * @return The host application id.
     */
    private long getHostAppId() {
        Cursor cursor = null;
        long hostAppId = INVALID_ID;
        try {
            cursor = mContext.getContentResolver().query(HostApp.URI, null,
                    HostAppColumns.PACKAGE_NAME + " = ?", new String[] {
                        mHostAppPackageName
                    }, null);
            if (cursor != null && cursor.moveToFirst()) {
                hostAppId = cursor.getLong(cursor.getColumnIndexOrThrow(HostAppColumns._ID));
            }
        } catch (SQLException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query host apps", e);
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query host apps", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query host apps", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return hostAppId;
    }

    /**
     * Get device id. This assumes only one device per host application.
     *
     * @param hostAppId The host application id.
     *
     * @return The device id.
     */
    private long getDeviceId(final long hostAppId) {
        Cursor cursor = null;
        long deviceId = INVALID_ID;
        try {
            cursor = mContext.getContentResolver().query(Device.URI, null,
                    DeviceColumns.HOST_APPLICATION_ID + " = " + hostAppId, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                deviceId = cursor.getLong(cursor.getColumnIndexOrThrow(DeviceColumns._ID));
            }
        } catch (SQLException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to get device id", exception);
            }
        } catch (SecurityException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to get device id", exception);
            }
        } catch (IllegalArgumentException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to get device id", exception);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return deviceId;
    }
}
