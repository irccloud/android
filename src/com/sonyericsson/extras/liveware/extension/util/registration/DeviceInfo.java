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

import com.sonyericsson.extras.liveware.aef.registration.Registration.Display;
import com.sonyericsson.extras.liveware.aef.registration.Registration.DisplayColumns;
import com.sonyericsson.extras.liveware.aef.registration.Registration.Input;
import com.sonyericsson.extras.liveware.aef.registration.Registration.InputColumns;
import com.sonyericsson.extras.liveware.aef.registration.Registration.KeyPad;
import com.sonyericsson.extras.liveware.aef.registration.Registration.KeyPadColumns;
import com.sonyericsson.extras.liveware.aef.registration.Registration.SensorColumns;
import com.sonyericsson.extras.liveware.aef.registration.Registration.SensorType;
import com.sonyericsson.extras.liveware.aef.registration.Registration.SensorTypeColumns;
import com.sonyericsson.extras.liveware.extension.util.Dbg;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensor;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensorType;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

import java.util.ArrayList;
import java.util.List;

/**
 * The device info class describes a host application device. This class only
 * contains a subset of the information available for the device.
 */
public class DeviceInfo {

    private final Context mContext;

    private final String mHostAppPackageName;

    private final long mId;

    private final int mWidgetWidth;

    private final int mWidgetHeight;

    private final boolean mVibrator;

    private List<DisplayInfo> mDisplays = null;

    private List<AccessorySensor> mSensors = null;

    private List<InputInfo> mInputs = null;

    /**
     * Create device info.
     *
     * @param context The context.
     * @param hostAppPackageName The host application package name.
     * @param id The device id.
     * @param widgetWidth The widget width.
     * @param widgetHeight The widget height.
     * @param vibrator True if device has a vibrator.
     */
    public DeviceInfo(final Context context, final String hostAppPackageName, final long id,
            final int widgetWidth, final int widgetHeight, final boolean vibrator) {
        mContext = context;
        mHostAppPackageName = hostAppPackageName;
        mId = id;
        mWidgetWidth = widgetWidth;
        mWidgetHeight = widgetHeight;
        mVibrator = vibrator;
    }

    /**
     * Get the id.
     *
     * @see Registration.DeviceColumns.#_ID
     *
     * @return The device id.
     */
    public long getId() {
        return mId;
    }

    /**
     * Get the widget width.
     *
     * @see Registration.DeviceColumns.#WIDGET_IMAGE_WIDTH
     *
     * @return The widget width.
     */
    public int getWidgetWidth() {
        return mWidgetWidth;
    }

    /**
     * Get the widget height.
     *
     * @see Registration.DeviceColumns.#WIDGET_IMAGE_HEIGHT
     *
     * @return The widget height.
     */
    public int getWidgetHeight() {
        return mWidgetHeight;
    }

    /**
     * Checks if the device has a vibrator.
     *
     * @see Registration.DeviceColumns.#VIBRATOR
     *
     * @return True if the device has a vibrator.
     */
    public boolean hasVibrator() {
        return mVibrator;
    }

    /**
     * Get the displays available.
     *
     * @return List with displays.
     */
    public List<DisplayInfo> getDisplays() {
        if (mDisplays != null) {
            // List of displays already available. Avoid re-reading from
            // database.
            return mDisplays;
        }

        mDisplays = new ArrayList<DisplayInfo>();

        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(Display.URI, null,
                    DisplayColumns.DEVICE_ID + " = " + mId, null, null);
            while (cursor != null && cursor.moveToNext()) {
                long displayId = cursor.getLong(cursor.getColumnIndexOrThrow(DisplayColumns._ID));
                int height = cursor.getInt(cursor
                        .getColumnIndexOrThrow(DisplayColumns.DISPLAY_HEIGHT));
                int width = cursor.getInt(cursor
                        .getColumnIndexOrThrow(DisplayColumns.DISPLAY_WIDTH));
                int colors = cursor.getInt(cursor.getColumnIndexOrThrow(DisplayColumns.COLORS));
                int refreshRate = cursor.getInt(cursor
                        .getColumnIndexOrThrow(DisplayColumns.REFRESH_RATE));
                int latency = cursor.getInt(cursor.getColumnIndexOrThrow(DisplayColumns.LATENCY));
                boolean tapTouch = (cursor.getInt(cursor
                        .getColumnIndexOrThrow(DisplayColumns.TAP_TOUCH)) == 1);
                boolean motionTouch = (cursor.getInt(cursor
                        .getColumnIndexOrThrow(DisplayColumns.MOTION_TOUCH)) == 1);
                DisplayInfo display = new DisplayInfo(displayId, width, height, colors,
                        refreshRate, latency, tapTouch, motionTouch);
                mDisplays.add(display);
            }
        } catch (SQLException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query displays", e);
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query displays", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query displays", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return mDisplays;
    }

    /**
     * Get the sensors available.
     *
     * @return List of sensors.
     */
    public List<AccessorySensor> getSensors() {
        if (mSensors != null) {
            // List of sensors already available. Avoid re-reading from
            // database.
            return mSensors;
        }

        mSensors = new ArrayList<AccessorySensor>();

        Cursor cursor = null;

        try {
            cursor = mContext.getContentResolver().query(
                    com.sonyericsson.extras.liveware.aef.registration.Registration.Sensor.URI,
                    null, SensorColumns.DEVICE_ID + " = ?", new String[] {
                        Long.toString(mId)
                    }, null);
            while (cursor != null && cursor.moveToNext()) {
                int sensorId = cursor.getInt(cursor.getColumnIndexOrThrow(SensorColumns.SENSOR_ID));
                boolean isInterruptSupported = cursor.getInt(cursor
                        .getColumnIndexOrThrow(SensorColumns.SUPPORTS_SENSOR_INTERRUPT)) == 1;
                String name = cursor.getString(cursor.getColumnIndexOrThrow(SensorColumns.NAME));
                int resolution = cursor.getInt(cursor
                        .getColumnIndexOrThrow(SensorColumns.RESOLUTION));
                int minimumDelay = cursor.getInt(cursor
                        .getColumnIndexOrThrow(SensorColumns.MINIMUM_DELAY));
                int maximumRange = cursor.getInt(cursor
                        .getColumnIndexOrThrow(SensorColumns.MAXIMUM_RANGE));
                int typeId = cursor.getInt(cursor
                        .getColumnIndexOrThrow(SensorColumns.SENSOR_TYPE_ID));

                AccessorySensorType type = getSensorType(typeId);

                AccessorySensor sensor = new AccessorySensor(mContext, mHostAppPackageName,
                        sensorId, type, isInterruptSupported, name, resolution, minimumDelay,
                        maximumRange);
                mSensors.add(sensor);
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

        return mSensors;
    }

    /**
     * Get the inputs available.
     *
     * @return List of inputs.
     */
    public List<InputInfo> getInputs() {
        if (mInputs != null) {
            // List of inputs already available. Avoid re-reading from
            // database.
            return mInputs;
        }

        mInputs = new ArrayList<InputInfo>();
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(Input.URI, null,
                    InputColumns.DEVICE_ID + " = ?", new String[] {
                        Long.toString(mId)
                    }, null);
            while (cursor != null && cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(InputColumns._ID));
                boolean enabled = cursor.getInt(cursor.getColumnIndexOrThrow(InputColumns.ENABLED)) == 1;
                long keyPadId = cursor.getLong(cursor
                        .getColumnIndexOrThrow(InputColumns.KEY_PAD_ID));
                KeyPadInfo keyPad = getKeyPad(keyPadId);
                InputInfo input = new InputInfo(id, enabled, keyPad);
                mInputs.add(input);
            }

        } catch (SQLException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query inputs", e);
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query inputs", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query inputs", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return mInputs;
    }

    /**
     * Get the sensor type.
     *
     * @param typeId The sensor type id.
     * @return The sensor type.
     */
    private AccessorySensorType getSensorType(int typeId) {
        AccessorySensorType type = null;
        Cursor cursor = null;

        try {
            cursor = mContext.getContentResolver().query(SensorType.URI, null,
                    SensorTypeColumns._ID + " = ?", new String[] {
                        Integer.toString(typeId)
                    }, null);
            if (cursor != null && cursor.moveToFirst()) {
                boolean isDelicate = cursor.getInt(cursor
                        .getColumnIndexOrThrow(SensorTypeColumns.DELICATE_SENSOR_DATA)) == 1;
                String name = cursor
                        .getString(cursor.getColumnIndexOrThrow(SensorTypeColumns.TYPE));
                type = new AccessorySensorType(name, isDelicate, typeId);
            }
        } catch (SQLException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query sensor type", e);
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query sensor type", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query sensor type", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return type;
    }

    /**
     * Get the key pad.
     *
     * @param id The key pad id.
     * @return The key pad.
     */
    private KeyPadInfo getKeyPad(long id) {
        KeyPadInfo keyPad = null;
        Cursor cursor = null;

        try {
            cursor = mContext.getContentResolver().query(KeyPad.URI, null,
                    KeyPadColumns._ID + " = ?", new String[] {
                        Long.toString(id)
                    }, null);
            if (cursor != null && cursor.moveToFirst()) {
                String type = cursor.getString(cursor.getColumnIndexOrThrow(KeyPadColumns.TYPE));
                keyPad = new KeyPadInfo(id, type);
            }

        } catch (SQLException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query key pad", e);
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query key pad", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query key pad", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return keyPad;
    }

}
