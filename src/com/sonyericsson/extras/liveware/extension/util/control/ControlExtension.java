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

package com.sonyericsson.extras.liveware.extension.util.control;

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.aef.registration.Registration.Device;
import com.sonyericsson.extras.liveware.aef.registration.Registration.DeviceColumns;
import com.sonyericsson.extras.liveware.aef.registration.Registration.HostApp;
import com.sonyericsson.extras.liveware.aef.registration.Registration.HostAppColumns;
import com.sonyericsson.extras.liveware.extension.util.Dbg;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;

import java.io.ByteArrayOutputStream;

/**
 * The control extension handles a control on an accessory.
 */
public abstract class ControlExtension {

    private static final int STATE_CREATED = 0;

    private static final int STATE_STARTED = 1;

    private static final int STATE_FOREGROUND = 2;

    private int mState = STATE_CREATED;

    protected final Context mContext;

    protected final String mHostAppPackageName;

    protected final BitmapFactory.Options mBitmapOptions;


    /**
     * Create control extension.
     *
     * @param context The context.
     * @param hostAppPackageName Package name of host application.
     */
    public ControlExtension(final Context context, final String hostAppPackageName) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        mContext = context;
        mHostAppPackageName = hostAppPackageName;

        // Set some default bitmap factory options that we frequently will use.
        mBitmapOptions = new BitmapFactory.Options();
        // We use default throughout the extension to avoid any automatic
        // scaling.
        // Keep in mind that we are not showing the images on the phone, but on
        // the accessory.
        mBitmapOptions.inDensity = DisplayMetrics.DENSITY_DEFAULT;
        mBitmapOptions.inTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
    }

    /**
     * Start control.
     */
    public final void start() {
        mState = STATE_STARTED;
        onStart();
    }

    /**
     * Resume control.
     */
    public final void resume() {
        mState = STATE_FOREGROUND;
        onResume();
    }

    /**
     * Pause control.
     */
    public final void pause() {
        mState = STATE_STARTED;
        onPause();
    }

    /**
     * Stop control.
     */
    public final void stop() {
        // If in foreground then pause it.
        if (mState == STATE_FOREGROUND) {
            pause();
        }

        mState = STATE_CREATED;
        onStop();
    }

    /**
     * Destroy control.
     */
    public final void destroy() {
        // If in foreground then pause it.
        if (mState == STATE_FOREGROUND) {
            pause();
        }
        // If started then stop it.
        if (mState == STATE_STARTED) {
            stop();
        }

        // No state for destroyed.
        onDestroy();
    }

    /**
     * Take action based on request code
     *
     * @see ControlReceiver#doActionOnAllControls(int)
     *
     * @param requestCode Code used to distinguish between different actions.
     * @param bundle Optional bundle with additional information.
     */
    public void onDoAction(int requestCode, Bundle bundle) {

    }


    /**
     * Called to notify a control extension that it is no longer used and is
     * being removed. The control extension should clean up any resources it
     * holds (threads, registered receivers, etc) at this point.
     */
    public void onDestroy() {

    }

    /**
     * Called when the control extension is started by the host application.
     */
    public void onStart() {

    }

    /**
     * Called when the control extension is stopped by the host application.
     */
    public void onStop() {

    }

    /**
     * Called when the control extension is paused by the host application.
     */
    public void onPause() {
    }

    /**
     * Called when the control extension is resumed by the host application.
     * The extension is expected to send a new image each time it is resumed.
     */
    public void onResume() {
    }

    /**
     * Called when host application reports an error.
     *
     * @param code The reported error code. {@link Control.Intents#EXTRA_ERROR_CODE}
     */
    public void onError(final int code) {

    }

    /**
     * Called when a key event has occurred.
     *
     * @param action The key action, one of
     *            <ul>
     *            <li> {@link Control.Intents#ACTION_LONGPRESS}</li>
     *            <li> {@link Control.Intents#ACTION_PRESS}</li>
     *            <li> {@link Control.Intents#ACTION_RELEASE}</li>
     *            </ul>
     * @param keyCode The key code.
     * @param timeStamp The time when the event occurred.
     */
    public void onKey(final int action, final int keyCode, final long timeStamp) {

    }

    /**
     * Called when a touch event has occurred.
     *
     * @param event The touch event.
     */
    public void onTouch(final ControlTouchEvent event) {

    }

    /**
     * Called when a swipe event has occurred
     *
     * @param direction The swipe direction, one of
     *            <ul>
     *            <li> {@link Control.Intents#DIRECTION_DOWN}</li>
     *            <li> {@link Control.Intents#DIRECTION_LEFT}</li>
     *            <li> {@link Control.Intents#DIRECTION_RIGHT}</li>
     *            <li> {@link Control.Intents#DIRECTION_UP}</li>
     *            </ul>
     */
    public void onSwipe(int direction) {

    }

    /**
     * Send request to start to host application.
     */
    protected void startRequest() {
        if (Dbg.DEBUG) {
            Dbg.d("Sending start request");
        }
        Intent intent = new Intent(Control.Intents.CONTROL_START_REQUEST_INTENT);
        sendToHostApp(intent);
    }

    /**
     * Send request to stop to host application.
     */
    protected void stopRequest() {
        if (Dbg.DEBUG) {
            Dbg.d("Sending stop request");
        }
        Intent intent = new Intent(Control.Intents.CONTROL_STOP_REQUEST_INTENT);
        sendToHostApp(intent);
    }

    /**
     * Show an image on the accessory.
     *
     * @param resourceId The image resource id.
     */
    protected void showImage(final int resourceId) {
        if (Dbg.DEBUG) {
            Dbg.d("showImage: " + resourceId);
        }

        Intent intent = new Intent();
        intent.setAction(Control.Intents.CONTROL_DISPLAY_DATA_INTENT);

        Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), resourceId,
                mBitmapOptions);
        ByteArrayOutputStream os = new ByteArrayOutputStream(256);
        bitmap.compress(CompressFormat.PNG, 100, os);
        byte[] buffer = os.toByteArray();
        intent.putExtra(Control.Intents.EXTRA_DATA, buffer);
        sendToHostApp(intent);
    }

    /**
     * Show bitmap on accessory.
     *
     * @param bitmap The bitmap to show.
     */
    protected void showBitmap(final Bitmap bitmap) {
        if (Dbg.DEBUG) {
            Dbg.d("showBitmap");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(256);
        bitmap.compress(CompressFormat.PNG, 100, outputStream);

        Intent intent = new Intent(Control.Intents.CONTROL_DISPLAY_DATA_INTENT);
        intent.putExtra(Control.Intents.EXTRA_DATA, outputStream.toByteArray());
        sendToHostApp(intent);
    }

    /**
     * Show bitmap on accessory. Used when only updating part of the screen.
     *
     * @param bitmap The bitmap to show.
     * @param x The x position.
     * @param y The y position.
     */
    protected void showBitmap(final Bitmap bitmap, final int x, final int y) {
        if (Dbg.DEBUG) {
            Dbg.v("showBitmap x: " + x + " y: " + y);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(256);
        bitmap.compress(CompressFormat.PNG, 100, outputStream);

        Intent intent = new Intent(Control.Intents.CONTROL_DISPLAY_DATA_INTENT);
        intent.putExtra(Control.Intents.EXTRA_X_OFFSET, x);
        intent.putExtra(Control.Intents.EXTRA_Y_OFFSET, y);
        intent.putExtra(Control.Intents.EXTRA_DATA, outputStream.toByteArray());
        sendToHostApp(intent);
    }

    /**
     * Set the accessory screens state.
     *
     * @see Control.Intents#SCREEN_STATE_AUTO
     * @see Control.Intents#SCREEN_STATE_DIM
     * @see Control.Intents#SCREEN_STATE_OFF
     * @see Control.Intents#SCREEN_STATE_ON
     *
     * @param state The screen state.
     */
    protected void setScreenState(final int state) {
        if (Dbg.DEBUG) {
            Dbg.d("setScreenState: " + state);
        }
        Intent intent = new Intent(Control.Intents.CONTROL_SET_SCREEN_STATE_INTENT);
        intent.putExtra(Control.Intents.EXTRA_SCREEN_STATE, state);
        sendToHostApp(intent);
    }

    /**
     * Start repeating vibrator
     *
     * @param onDuration On duration in milliseconds.
     * @param offDuration Off duration in milliseconds.
     * @param repeats The number of repeats of the on/off pattern. Use
     *            {@link Control.Intents#REPEAT_UNTIL_STOP_INTENT} to repeat
     *            until explicitly stopped.
     */
    protected void startVibrator(int onDuration, int offDuration, int repeats) {
        if (Dbg.DEBUG) {
            Dbg.v("startVibrator: onDuration: " + onDuration + ", offDuration: " + offDuration
                    + ", repeats: " + repeats);
        }
        Intent intent = new Intent(Control.Intents.CONTROL_VIBRATE_INTENT);
        intent.putExtra(Control.Intents.EXTRA_ON_DURATION, onDuration);
        intent.putExtra(Control.Intents.EXTRA_OFF_DURATION, offDuration);
        intent.putExtra(Control.Intents.EXTRA_REPEATS, repeats);
        sendToHostApp(intent);
    }

    /**
     * Stop vibrator.
     */
    protected void stopVibrator() {
        if (Dbg.DEBUG) {
            Dbg.v("Vibrator stop");
        }
        Intent intent = new Intent(Control.Intents.CONTROL_STOP_VIBRATE_INTENT);
        sendToHostApp(intent);
    }

    /**
     * Start a LED pattern.
     *
     * @param id Id of the LED to be controlled.
     * @param color Color you want the LED to blink with.
     * @param onDuration On duration in milliseconds.
     * @param offDuration Off duration in milliseconds.
     * @param repeats The number of repeats of the on/off pattern. Use
     *            {@link Control.Intents#REPEAT_UNTIL_STOP_INTENT} to repeat
     *            until explicitly stopped.
     *
     */
    protected void startLedPattern(int id, int color, int onDuration, int offDuration, int repeats) {
        if (Dbg.DEBUG) {
            Dbg.v("startLedPattern: id: " + id + ", color: " + color + "onDuration: " + onDuration
                + ", offDuration: " + offDuration + ", repeats: " + repeats);
        }
        Intent intent = new Intent(Control.Intents.CONTROL_LED_INTENT);
        intent.putExtra(Control.Intents.EXTRA_LED_ID, id);
        intent.putExtra(Control.Intents.EXTRA_LED_COLOR, color);
        intent.putExtra(Control.Intents.EXTRA_ON_DURATION, onDuration);
        intent.putExtra(Control.Intents.EXTRA_OFF_DURATION, offDuration);
        intent.putExtra(Control.Intents.EXTRA_REPEATS, repeats);
        sendToHostApp(intent);
    }

    /**
     * Turn led off
     *
     * @param id Id of the LED to be controlled.
     */
    protected void stopLedPattern(int id) {
        if (Dbg.DEBUG) {
            Dbg.v("stopLedPattern: id: " + id);
        }
        Intent intent = new Intent(Control.Intents.CONTROL_LED_INTENT);
        intent.putExtra(Control.Intents.EXTRA_LED_ID, id);
        sendToHostApp(intent);
    }

    /**
     * Clear accessory diplay.
     */
    protected void clearDisplay() {
        if (Dbg.DEBUG) {
            Dbg.v("Clear display");
        }
        Intent intent = new Intent(Control.Intents.CONTROL_CLEAR_DISPLAY_INTENT);
        sendToHostApp(intent);
    }

    /**
     * Send intent to host application. Adds host application package name and
     * our package name.
     *
     * @param intent The intent to send.
     */
    protected void sendToHostApp(final Intent intent) {
        intent.putExtra(Control.Intents.EXTRA_AEA_PACKAGE_NAME, mContext.getPackageName());
        intent.setPackage(mHostAppPackageName);
        mContext.sendBroadcast(intent, Registration.HOSTAPP_PERMISSION);
    }

    /**
     * Get the host application id for this control.
     *
     * @return The host application id.
     */
    protected long getHostAppId() {
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(HostApp.URI, new String[] {
                HostAppColumns._ID
            }, HostAppColumns.PACKAGE_NAME + " = ?", new String[] {
                mHostAppPackageName
            }, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(HostAppColumns._ID));
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
        return -1;
    }

    /**
     * Check if this host application has a vibrator.
     *
     * @return True if vibrator exists.
     */
    protected boolean hasVibrator() {
        long hostAppId = getHostAppId();

        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(
                    Device.URI,
                    new String[] {
                        DeviceColumns.VIBRATOR
                    },
                    DeviceColumns.HOST_APPLICATION_ID + " = " + hostAppId + " AND "
                            + DeviceColumns.VIBRATOR + " = 1", null, null);
            if (cursor != null) {
                return (cursor.getCount() > 0);
            }
        } catch (SQLException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query vibrator", exception);
            }
        } catch (SecurityException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query vibrator", exception);
            }
        } catch (IllegalArgumentException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query vibrator", exception);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return false;
    }
}
