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
 * The accessory sensor type represents a sensor type.
 */
public class AccessorySensorType {

    private final String mName;

    private final boolean mIsDelicate;

    private final int mId;

    /**
     * Create accessory sensor type.
     *
     * @param name The name.
     * @param isDelicate True if sensor data is delicate.
     * @param id The sensor type id.
     */
    public AccessorySensorType(final String name, final boolean isDelicate, final int id) {
        mName = name;
        mIsDelicate = isDelicate;
        mId = id;
    }

    /**
     * Get the sensor type name.
     *
     * @see Registration.SensorTypeColumns.#TYPE
     * @return
     */
    public String getName() {
        return mName;
    }

    /**
     * Is the sensor type delicate.
     *
     * @see Registration.SensorTypeColumns.#DELICATE_SENSOR_DATA
     * @return True if delicate.
     */
    public boolean isDelicate() {
        return mIsDelicate;
    }

    /**
     * Get the type id.
     *
     * @return The type id.
     */
    public int getId() {
        return mId;
    }

}
