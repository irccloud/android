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

import com.sonyericsson.extras.liveware.aef.notification.Notification;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.extension.util.Dbg;
import com.sonyericsson.extras.liveware.extension.util.ExtensionService;
import com.sonyericsson.extras.liveware.extension.util.sensor.AccessorySensor;

import android.content.ContentValues;
import android.content.Context;

/**
 * Provides information needed during extension registration
 */
public abstract class RegistrationInformation {

    public static int API_NOT_REQUIRED = 0;

    /**
     * Get the required notifications API version
     *
     * @see Registration.ExtensionColumns#NOTIFICATION_API_VERSION
     * @see #getSourceRegistrationConfigurations
     * @see ExtensionService#onViewEvent
     * @see ExtensionService#onRefreshRequest
     *
     * @return Required notification API version, or 0 if not supporting
     *         notification.
     */
    public abstract int getRequiredNotificationApiVersion();

    /**
     * Get the extension registration information.
     *
     * @see ExtensionService#onRegisterResult
     * @return The registration configuration.
     */
    public abstract ContentValues getExtensionRegistrationConfiguration();

    /**
     * Get all source registration configurations.
     *
     * The extensions specific id must be set if there are more than one source.
     *
     * Override this method if this is a notification extension
     *
     * @see Notification.SourceColumns#EXTENSION_SPECIFIC_ID
     * @see #getRequiredNotificationApiVersion()
     *
     * @param extensionSpecificId The extension specific id.
     * @return The source registration information.
     */
    public ContentValues[] getSourceRegistrationConfigurations() {
        throw new IllegalArgumentException(
                "getSourceRegistrationConfiguration() not implemented. Notification extensions must override this method");
    }

    /**
     * Checks if the widget size is supported. Override this to provide
     * extension specific implementation.
     *
     * @param width The widget width.
     * @param height The widget height.
     * @return True if the widget size is supported.
     */
    public boolean isWidgetSizeSupported(int width, int height) {
        throw new IllegalArgumentException(
                "isWidgetSizeSupported() not implemented. Widget extensions must override this method");
    }

    /**
     * Get the required widget API version
     *
     * @see #isWidgetSizeSupported
     * @see ExtensionService#createWidgetExtension
     * @see Registration.ApiRegistrationColumns#WIDGET_API_VERSION
     * @return Required API widget version, or 0 if not supporting widget.
     */
    abstract public int getRequiredWidgetApiVersion();

    /**
     * Checks if the display size is supported.
     *
     * @param width The display width.
     * @param height The display height.
     *
     * @see ExtensionService#createControlExtension
     *
     * @return True if the display size is supported.
     */
    public boolean isDisplaySizeSupported(final int width, final int height) {
        throw new IllegalArgumentException(
                "isDisplaySizeSupported() not implemented. Control extensions must override this method");
    }

    /**
     * Get the required control API version
     *
     * @see #isDisplaySizeSupported
     * @see ExtensionService#createControlExtension
     *
     * @see Registration.ApiRegistrationColumns#CONTROL_API_VERSION
     * @return Required API control version, or 0 if not supporting control.
     */
    abstract public int getRequiredControlApiVersion();

    /**
     * Check if the sensor is supported.
     *
     * @param sensor The sensor.
     * @return True if sensor is supported.
     */
    public boolean isSensorSupported(final AccessorySensor sensor) {
        throw new IllegalArgumentException(
                "isSensorSupported() not implemented. Sensor extensions must override this method");

    }

    /**
     * Get the required sensor API version
     *
     * @see #isDisplaySizeSupported
     *
     * @see Registration.ApiRegistrationColumns#SENSOR_API_VERSION
     * @return Required API sensor version, or 0 if not supporting sensor.
     */
    abstract public int getRequiredSensorApiVersion();

    /**
     * Return true if the sources shall be updated when the extension service is
     * created. This might be handy if the extension use dynamic sources and the
     * source can change when the extension service is not running.
     *
     * @return True if the source registration shall be update when the
     *         extension service is created.
     */
    public boolean isSourcesToBeUpdatedAtServiceCreation() {
        return false;
    }

    /**
     * Check if widget shall be supported for this host application by checking
     * that the host application has device with a supported widget size.
     *
     * This method can be override to provide extension specific
     * implementations.
     *
     * @param context The context.
     * @param hostApplication The host application.
     * @return True if widget shall be supported.
     */
    public boolean isSupportedWidgetAvailable(final Context context,
            final HostApplicationInfo hostApplication) {
        if (getRequiredWidgetApiVersion() == API_NOT_REQUIRED) {
            return false;
        }

        if (hostApplication.getWidgetApiVersion() == 0) {
            return false;
        }

        if (getRequiredWidgetApiVersion() > hostApplication.getWidgetApiVersion()) {
            if (Dbg.DEBUG) {
                Dbg.w("isSupportedWidgetAvailable: required widget API version not supported");
            }
            return false;
        }

        for (DeviceInfo device : hostApplication.getDevices()) {
            if (isWidgetSizeSupported(device.getWidgetWidth(), device.getWidgetHeight())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if sensor shall be supported for this host application by checking
     * if the host application has at least one supported sensor.
     *
     * This method can be override to provide extension specific
     * implementations.
     *
     * @param context The context.
     * @param hostApplication The host application.
     * @return True if sensor shall be supported.
     */
    public boolean isSupportedSensorAvailable(final Context context,
            final HostApplicationInfo hostApplication) {
        if (getRequiredSensorApiVersion() == API_NOT_REQUIRED) {
            return false;
        }

        if (hostApplication.getSensorApiVersion() == 0) {
            return false;
        }

        if (getRequiredSensorApiVersion() > hostApplication.getSensorApiVersion()) {
            if (Dbg.DEBUG) {
                Dbg.w("isSupportedSensorAvailable: required sensor API version not supported");
            }
            return false;
        }

        for (DeviceInfo device : hostApplication.getDevices()) {
            for (AccessorySensor sensor : device.getSensors()) {
                if (isSensorSupported(sensor)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if control shall be supported for this host application by checking
     * that the host application has a device with a supported display size.
     *
     * This method can be override to provide extension specific
     * implementations.
     *
     * @param context The context.
     * @param hostApplication The host application.
     * @return True if control shall be supported.
     */
    public boolean isSupportedControlAvailable(final Context context,
            final HostApplicationInfo hostApplication) {
        if (getRequiredControlApiVersion() == API_NOT_REQUIRED) {
            return false;
        }

        if (hostApplication.getControlApiVersion() == 0) {
            return false;
        }

        if (getRequiredControlApiVersion() > hostApplication.getControlApiVersion()) {
            if (Dbg.DEBUG) {
                Dbg.w("isSupportedControlAvailable: required control API version not supported");
            }
            return false;
        }

        for (DeviceInfo device : hostApplication.getDevices()) {
            for (DisplayInfo display : device.getDisplays()) {
                if (isDisplaySizeSupported(display.getWidth(), display.getHeight())) {
                    return true;
                }
            }
        }

        return false;
    }

}
