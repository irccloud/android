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

/**
 * A sensor event from an accessory.
 */
public class AccessorySensorEvent {
    private final int mAccuracy;

    private final long mTimestamp;

    private final float[] mVal;

    /**
     * Create a sensor event.
     *
     * @param accuracy The accuracy as defined in
     *            {@link com.sonyericsson.extras.liveware.aef.sensor.Sensor.SensorAccuracy}
     * @param timeStamp The time in nanoseconds when the event fired.
     * @param sensorValues Array of sensor values. The length of the array is
     *            dependent on the sensor.
     */
    public AccessorySensorEvent(int accuracy, long timestamp, float[] sensorValues) {
        mAccuracy = accuracy;
        mTimestamp = timestamp;
        mVal = sensorValues;
    }

    /**
     * Get data values from sensor The length of the array is dependent on the
     * sensor.
     */
    public float[] getSensorValues() {
        return mVal;
    }

    /**
     * @return The time in nanoseconds when the event fired.
     */
    public long getTimestamp() {
        return mTimestamp;
    }

    /**
     * @return The accuracy as defined in
     *         {@link com.sonyericsson.extras.liveware.aef.sensor.Sensor.SensorAccuracy}
     */
    public int getAccuracy() {
        return mAccuracy;
    }
}
