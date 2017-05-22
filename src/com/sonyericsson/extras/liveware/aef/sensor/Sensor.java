/*
Copyright (c) 2011, Sony Ericsson Mobile Communications AB
Copyright (C) 2012-2013 Sony Mobile Communications AB

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

package com.sonyericsson.extras.liveware.aef.sensor;

/**
 * <h1>Sensor API is a part of the Smart Extension APIs</h1>
 * <h3>Overview</h3>
 * The Sensor API is used to send accessory sensor data from a host application to
 * an accessory extension. The API can be used by any registered extension.
 * <p>
 * Sensor data is sent over a LocalSocket. In order to start communication the extension
 * should setup a LocalServerSocket {@link android.net.LocalServerSocket}
 * in listening mode and send the {@link Intents#SENSOR_REGISTER_LISTENER_INTENT} intent.
 * The capability API can be used to query the host application for supported sensors.
 * If multiple sensors are used, multiple LocalServerSocket objects must also be used since
 * one sensor is bound to exactly one LocalServerSocket.
 *</p>
 * <h3>Sensor data format</h3>
 * The sensor data is sent from the host application over a LocalSocket and can be accessed
 * through an InputStream.
 * The data has the following format:
 * <ol>
 * <li>Byte  0 -  3   Total length of the data package</li>
 * <li>Byte  4 -  7   Accuracy. For more information see {@link SensorAccuracy}</li>
 * <li>Byte  8 - 15   Timestamp. The time in nanosecond at which the event happened</li>
 * <li>Byte 16 - 19   Length of sensor values in bytes</li>
 * <li>Byte 20 - nn   Sensor values. This should be interpreted as an array of float values, each float value is 4 bytes long</li>
 * </ol>
 *The length and contents of the values array depends on which sensor type is being monitored.
 *
 */

public class Sensor {

    /**
     * @hide
     * This class is only intended as a utility class containing declared constants
     *  that will be used by Sensor API Extension developers.
     */
    protected Sensor() {
    }

    /**
     * Intents sent between Sensor Extensions and Accessory Host Applications
     */
    public interface Intents {

        /**
         * Intent used by the Sensor Extension whenever it wants to start listen
         * to sensor data.
         * The purpose of the intent is to tell the host application that a
         * local server socket is now waiting for a connection from the host application.
         * The name of the local server socket is sent in the intent.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_SENSOR_ID}</li>
         * <li>{@link #EXTRA_SENSOR_LOCAL_SERVER_SOCKET_NAME}</li>
         * <li>{@link #EXTRA_SENSOR_REQUESTED_RATE}</li>
         * <li>{@link #EXTRA_SENSOR_INTERRUPT_MODE}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String SENSOR_REGISTER_LISTENER_INTENT = "com.sonyericsson.extras.aef.sensor.REGISTER_LISTENER";

        /**
         * Intent used by the Sensor Extension whenever it wants to stop listen
         * to sensor data.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_SENSOR_ID}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String SENSOR_UNREGISTER_LISTENER_INTENT = "com.sonyericsson.extras.aef.sensor.UNREGISTER_LISTENER";

        /**
         * Intent sent by the Host Application when an error situation has occurred
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_SENSOR_ID}</li>
         * <li>{@link #EXTRA_ERROR_CODE}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String SENSOR_ERROR_MESSAGE_INTENT = "com.sonyericsson.extras.aef.sensor.ERROR_MESSSAGE";

        /**
         * The name of the Intent-extra used to identify the Extension.
         * The extension will send its package name
         * <P>
         * TYPE: TEXT
         * </P>
         * @since 1.0
         */
        String EXTRA_AEA_PACKAGE_NAME = "aea_package_name";

        /**
         * The name of the Intent-extra used to identify the Sensor.
         * The Extension will send the ID of the sensor. The ID must
         * be identical to the value of the SENSOR_ID column from the Sensor
         * table of a sensor that is attached to the current host application
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        String EXTRA_SENSOR_ID = "sensor_id";

        /**
         * The name of the Intent-extra used to identify the name of the Android
         * Local Server Socket that is now waiting for a connection from
         * the host application
         * <P>
         * TYPE: TEXT
         * </P>
         * @since 1.0
         */
        String EXTRA_SENSOR_LOCAL_SERVER_SOCKET_NAME = "local_server_socket_name";

        /**
         * The name of the Intent-extra used to set the
         * preferred delivery rate of the sensor data.
         * The value must one the predefined constants
         * {@link SensorRates}
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        String EXTRA_SENSOR_REQUESTED_RATE = "requested_rate";

        /**
         * The name of the Intent-extra used to set the
         * sensor interrupt mode.
         * The value must one the predefined constants
         * {@link SensorInterruptMode}
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        String EXTRA_SENSOR_INTERRUPT_MODE = "interrupt_mode";

        /**
         * The name of the Intent-extra used to set the
         * error code of an error message from the
         * host application.
         * The value must be one of the predefined constants
         * {@link SensorApiErrorCodes}
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        String EXTRA_ERROR_CODE = "error_code";

        /**
         * The name of the Intent-extra used to identify the Host Application.
         * The Host Application will send its package name.
         * <P>
         * TYPE: TEXT
         * </P>
         * @since 1.0
         */
        String EXTRA_AHA_PACKAGE_NAME = "aha_package_name";
    }

    /**
     * Interface used to define constants for
     * sensor rates.
     * The extension can chose from one of the
     * constants defined in this interface.
     */
    public interface SensorRates {

        /**
         * Get sensor data as fast as possible
         */
        int SENSOR_DELAY_FASTEST = 1;

        /**
         * Rate suitable for games
         */
        int SENSOR_DELAY_GAME = 2;

        /**
         * Rate suitable for screen orientation changes
         */
        int SENSOR_DELAY_NORMAL = 3;

        /**
         * Rate suitable for user interface
         */
        int SENSOR_DELAY_UI = 4;
    }

    /**
     * Interface used to define constants for
     * sensor accuracy.
     * The accuracy is sent together with the sensor data
     * over the local socket connection.
     */
    public interface SensorAccuracy {

        /**
         * The values returned by this sensor cannot be trusted,
         * calibration is needed or the environment will not allow readings
         */
        int SENSOR_STATUS_UNRELIABLE = 0;

        /**
         * This sensor is reporting data with low accuracy,
         * calibration with the environment is needed
         */
        int SENSOR_STATUS_ACCURACY_LOW = 1;

       /**
        * This sensor is reporting data with an average level of accuracy,
        * calibration with the environment may improve the readings
        */
       int SENSOR_STATUS_ACCURACY_MEDIUM = 2;

        /**
         * This sensor is reporting data with maximum accuracy
         */
        int SENSOR_STATUS_ACCURACY_HIGH = 3;
    }

    /**
     * Interface used to define constants for
     * sensor interrupt mode.
     * The interrupt mode is set when registering a listener
     */
    public interface SensorInterruptMode {

        /**
         * The interrupt mode is disabled,
         * e.g. the sensor is sending data continuously
         */
        int SENSOR_INTERRUPT_DISABLED = 0;

        /**
         * The interrupt mode is enabled,
         * e.g. no sensor is sent until new sensor data is available
         */
        int SENSOR_INTERRUPT_ENABLED = 1;
    }

    /**
     * Interface used to define constants for
     * sensor error codes sent from the host application
     */
    public interface SensorApiErrorCodes {

        /**
         * Error code indicating that the action
         * requested by the extension is not allowed
         * in the current state
         */
        int SENSOR_ERROR_CODE_NOT_ALLOWED = 0;
    }

    /**
     * Constant defining the sensor type Accelerometer.
     * Sensor data is sent as an array of 3 float values representing
     * the acceleration on the x-axis, y-axis and z-axis respectively.
     * All values are in SI units (m/s^2)
     * For more information about the accelerometer sensor type,
     * see {@link android.hardware.Sensor#TYPE_ACCELEROMETER}
     */
    public static final String SENSOR_TYPE_ACCELEROMETER = "Accelerometer";

    /**
     * Constant defining the sensor type Light.
     * Sensor data is sent as one float value representing
     * the light level in SI lux units.
     * For more information about the light sensor type,
     * see {@link android.hardware.Sensor#TYPE_LIGHT}
     */
    public static final String SENSOR_TYPE_LIGHT = "Light";
}
