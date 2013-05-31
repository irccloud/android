/*
 Copyright (c) 2011, Sony Ericsson Mobile Communications AB
 Copyright (c) 2012 Sony Mobile Communications AB.

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

import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.aef.sensor.Sensor;
import com.sonyericsson.extras.liveware.extension.util.Dbg;

import android.content.Context;
import android.content.Intent;
import android.net.LocalServerSocket;
import android.os.Handler;
import android.os.Message;

import java.io.DataInputStream;
import java.io.IOException;

/**
 * The accessory sensor class is used to interact with a sensor on an accessory.
 */
public class AccessorySensor {

    private final Context mContext;

    private final String mHostAppPackageName;

    private final int mSensorId;

    private final AccessorySensorType mType;

    private final boolean mIsInterruptModeSupported;

    private final String mName;

    private final int mResolution;

    private final int mMinimumDelay;

    private final int mMaximumRange;

    private final String mSocketName;

    private ServerThread mServerThread;

    private LocalServerSocket mLocalServerSocket;

    private int mSensorRate;

    private int mInterruptMode;

    private AccessorySensorEventListener mListener = null;

    /**
     * Create accessory sensor. This constructor is normally not called
     * directly. Instead it is created from AccessorySensorManager or
     * DeviceInfo.
     *
     * @param context The context.
     * @param hostAppPackageName The host application package name.
     * @param sensorId The sensor id.
     * @param type The sensor type.
     * @param isInterruptSupported True if interrupt mode is supported.
     * @param name The name.
     * @param resolution The resolution.
     * @param minimumDelay The minimum delay.
     * @param maximumRange The maximum range.
     */
    public AccessorySensor(final Context context, final String hostAppPackageName,
            final int sensorId, final AccessorySensorType type, final boolean isInterruptSupported,
            final String name, final int resolution, final int minimumDelay, final int maximumRange) {
        mContext = context;
        mHostAppPackageName = hostAppPackageName;
        mSensorId = sensorId;
        mType = type;
        mIsInterruptModeSupported = isInterruptSupported;
        mName = name;
        mResolution = resolution;
        mMinimumDelay = minimumDelay;
        mMaximumRange = maximumRange;
        mSocketName = hostAppPackageName + "" + type.getName();
    }

    /**
     * Register a sensor event listener. It is only possible to have one
     * listener per sensor.
     *
     * @param listener The event listener.
     * @param sensorRate The sensor rate.
     * @param interruptMode The interrupt mode.
     */
    public void registerListener(final AccessorySensorEventListener listener, final int sensorRate,
            final int interruptMode) throws AccessorySensorException {
        if (listener == null) {
            throw new IllegalArgumentException("listener == null");
        }

        mListener = listener;
        mSensorRate = sensorRate;
        mInterruptMode = interruptMode;

        openSocket();
    }

    /**
     * Register a sensor event listener that gets new data when the sensor has
     * new data. It is only possible to have one listener per sensor.
     *
     * @param listener The event listener.
     */
    public void registerInterruptListener(final AccessorySensorEventListener listener)
            throws AccessorySensorException {

        if (!mIsInterruptModeSupported) {
            throw new IllegalStateException("Interrupt mode not supported");
        }

        // Rate is ignored in interrupt mode.
        registerListener(listener, Sensor.SensorRates.SENSOR_DELAY_NORMAL,
                Sensor.SensorInterruptMode.SENSOR_INTERRUPT_ENABLED);
    }

    /**
     * Register a sensor event listener that gets new data at a fixed rate. It
     * is only possible to have one listener per sensor.
     *
     * @param listener The event listener.
     * @param sensorRate The rate.
     */
    public void registerFixedRateListener(final AccessorySensorEventListener listener,
            int sensorRate) throws AccessorySensorException {
        registerListener(listener, sensorRate, Sensor.SensorInterruptMode.SENSOR_INTERRUPT_DISABLED);
    }

    /**
     * Unregister sensor event listener.
     */
    public void unregisterListener() {
        mListener = null;

        closeSocket();
    }

    /**
     * Get the sensor id.
     *
     * @see Registration.SensorColumns.#SENSOR_ID
     * @return The sensor id.
     */
    public int getSensorId() {
        return mSensorId;
    }

    /**
     * Get the sensor type.
     *
     * @return The sensor type.
     */
    public AccessorySensorType getType() {
        return mType;
    }

    /**
     * Is interrupt mode supported.
     *
     * @see Registration.SensorColumns.#SUPPORTS_SENSOR_INTERRUPT
     * @return True if interrupt mode is supported.
     */
    public boolean isInterruptModeSupported() {
        return mIsInterruptModeSupported;
    }

    /**
     * Get the sensor name.
     *
     * @see Registration.SensorColumns.#NAME
     * @return The sensor name.
     */
    public String getName() {
        return mName;
    }

    /**
     * Get the sensor resolution.
     *
     * @see Registration.SensorColumns.#RESOLUTION
     * @return The sensor resolution.
     */
    public int getResolution() {
        return mResolution;
    }

    /**
     * Get the minimum delay.
     *
     * @see Registration.SensorColumns.#MINIMUM_DELAY
     * @return The minimum delay.
     */
    public int getMinimumDelay() {
        return mMinimumDelay;
    }

    /**
     * Get the maximum range.
     *
     * @see Registration.SensorColumns.#MAXIMUM_RANGE
     * @return The maximum range.
     */
    public int getMaximumRange() {
        return mMaximumRange;
    }

    /**
     * Create socket to be able to read sensor data
     */
    private void openSocket() throws AccessorySensorException {
        try {
            // Open socket
            mLocalServerSocket = new LocalServerSocket(mSocketName);

            // Stop server listening thread if running
            if (mServerThread != null) {
                mServerThread.interrupt();
                mServerThread = null;
            }

            // Start server listening thread
            mServerThread = new ServerThread(new Handler() {
                public void handleMessage(Message msg) {
                    AccessorySensorEvent accessorySensorEvent = (AccessorySensorEvent)msg.obj;
                    if (accessorySensorEvent != null && mListener != null) {
                        mListener.onSensorEvent(accessorySensorEvent);
                    }
                }
            });
            mServerThread.start();

            // Send intent to Aha
            sendSensorStartListeningIntent();
        } catch (IOException e) {
            if (Dbg.DEBUG) {
                Dbg.e(e.getMessage(), e);
            }
            throw new AccessorySensorException(e.getMessage());
        }
    }

    /**
     * Close socket to be able to read sensor data
     */
    private void closeSocket() {
        // Close socket
        if (mLocalServerSocket != null) {
            try {
                mLocalServerSocket.close();
                mLocalServerSocket = null;
            } catch (IOException e) {
                if (Dbg.DEBUG) {
                    Dbg.w(e.getMessage(), e);
                }
            }
        }

        // Stop thread
        if (mServerThread != null) {
            mServerThread.interrupt();
            mServerThread = null;
        }

        // Send intent to Aha
        sendSensorStopListeningIntent();
    }

    /**
     * Send start listening intent to host application
     *
     * @see Sensor.Intents#SENSOR_REGISTER_LISTENER_INTENT
     */
    private void sendSensorStartListeningIntent() {
        Intent i = new Intent(Sensor.Intents.SENSOR_REGISTER_LISTENER_INTENT);
        i.putExtra(Sensor.Intents.EXTRA_SENSOR_ID, mSensorId);
        i.putExtra(Sensor.Intents.EXTRA_SENSOR_LOCAL_SERVER_SOCKET_NAME, mSocketName);
        i.putExtra(Sensor.Intents.EXTRA_SENSOR_REQUESTED_RATE, mSensorRate);
        i.putExtra(Sensor.Intents.EXTRA_SENSOR_INTERRUPT_MODE, mInterruptMode);
        sendToHostApp(i);
    }

    /**
     * Send stop listening intent to host application
     *
     * @see Sensor.Intents#SENSOR_UNREGISTER_LISTENER_INTENT
     */
    private void sendSensorStopListeningIntent() {
        Intent i = new Intent(Sensor.Intents.SENSOR_UNREGISTER_LISTENER_INTENT);
        i.putExtra(Sensor.Intents.EXTRA_SENSOR_ID, mSensorId);
        sendToHostApp(i);
    }

    /**
     * Send intent to host application. Adds host application package name and
     * our package name.
     *
     * @param intent The intent to send.
     */
    private void sendToHostApp(final Intent intent) {
        intent.putExtra(Control.Intents.EXTRA_AEA_PACKAGE_NAME, mContext.getPackageName());
        intent.setPackage(mHostAppPackageName);
        mContext.sendBroadcast(intent, Registration.HOSTAPP_PERMISSION);
    }

    /**
     * Provides a thread which can read from the socket
     */
    private class ServerThread extends Thread {
        private final Handler mHandler;

        /**
         * Creates a thread which can read from the socket
         *
         * @param handler The handler to post messages on.
         */
        public ServerThread(Handler handler) {
            mHandler = handler;
        }

        @Override
        public void run() {
            try {
                DataInputStream inStream = new DataInputStream(mLocalServerSocket.accept()
                        .getInputStream());
                while (!isInterrupted()) {
                    AccessorySensorEvent event = decodeSensorData(inStream);
                    if (event != null) {
                        Message msg = new Message();
                        msg.obj = event;
                        mHandler.sendMessage(msg);
                    }
                }
            } catch (IOException e) {
                if (Dbg.DEBUG) {
                    Dbg.w(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Decodes data from the socket
     *
     * @param inStream The data stream
     * @return The sensor event.
     */
    private AccessorySensorEvent decodeSensorData(DataInputStream inStream) throws IOException {
        int totalLength = inStream.readInt();
        if (totalLength == 0) {
            return null;
        }
        int accuracy = inStream.readInt();
        long timestamp = inStream.readLong();
        int sensorValueCount = inStream.readInt();
        float[] sensorValues = new float[sensorValueCount];
        for (int i = 0; i < sensorValueCount; i++) {
            sensorValues[i] = inStream.readFloat();
        }
        return new AccessorySensorEvent(accuracy, timestamp, sensorValues);
    }
}
