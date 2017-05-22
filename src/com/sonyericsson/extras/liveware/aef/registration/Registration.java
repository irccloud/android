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

package com.sonyericsson.extras.liveware.aef.registration;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * <h1>The Registration and Capability API is a part of the Smart Extension API's</h1>
 * <p>
 * This API is used by accessory extensions and accessory host applications. Typically host applications insert
 * and maintain information about the accessories capabilities. Extensions use the capability information
 * in order to interact with the accessories in a correct way. Before an extension can interact with an accessory it must
 * provide (register) some information needed by the host applications.
 * The API defines and implements an Android ContentProvider that applications access via the Android ContentResolver API.
 * The ContentProvider implementation is backed by a database implementation.
 * </p>
 * <p>Topics covered here:
 * <ol>
 * <li><a href="#Registration">Extension registration</a>
 * <li><a href="#Capabilities">Using the capabilities API</a>
 * <li><a href="#Security">Security.</a>
 * </ol>
 * </p>
 * <a name="Registration"></a>
 * <h3>Extension registration</h3>
 * <p>
 * Before an extension can use an accessory, the extension must use the registration API content provider
 * to insert a record in the extension table. The URI is defined in the Extension interface {@link Extension#URI} and
 * the table scheme is defined in the ExtensionColumns interface {@link ExtensionColumns}.
 * After inserting a record in the extensions table, the extension is ready to use the Notification API.
 * No further registration is needed in order to use the Notification API and start writing sources and events.
 * More advanced extensions that also want to use any of the Widget API, Control API or Sensor API must also register
 * information in the registration table. This should be done for each host application that the extension wants to interact with.
 * In order to find out what host applications are available and what capabilities they support, the extension should use the
 * capability API <a href="#Capabilities">Using the capability API</a>.
 * The URI of the registration table is defined in the ApiRegistration interface {@link ApiRegistration#URI} and
 * the table schema is defined in the ApiRegistrationColumns interface {@link ApiRegistrationColumns}. The extension should provide
 * the host application package name and indicate what APIs it will use.
 * </p>
 * <a name="Capabilities"></a>
 * <h3>Using the capabilities API</h3>
 * <p>
 * This API is an Android content provider that provides information about the capabilities of the accessories. The information is
 * provided by the host applications and are used by the extensions to obtain necessary information in order to interact with the
 * accessories through the Control, Sensor and Widget APIs. The content provider contains the tables shown in the picture below.
 * <img src="../../../../../../../public_documentation/images/capabilities_database.png" alt="Operating context" border="1" />
 * For each accessory there is a corresponding record in the host_application table. A Particular host application is identified
 * by its package name. For each host application there is one or more device records in the device table.
 * A particular device can support zero or more displays, sensors, leds and inputs, defined in the display, sensor, led and input
 * tables respectively There is a sensor_type table describing each type of sensor and keypad table describing the capabilities if
 * the keypads of each input type. The capabilities tables are accessible through the content provider.
 * </p>
 * <p> Capability URI's and description
 * <ol>
 * <li> Host application URI {@link HostApp#URI} and columns {@link HostAppColumns}
 * <li> Host application URI {@link Device#URI} and columns {@link DeviceColumns}
 * <li> Host application URI {@link Display#URI} and columns {@link DisplayColumns}
 * <li> Host application URI {@link Sensor#URI} and columns {@link SensorColumns}
 * <li> Host application URI {@link Input#URI} and columns {@link InputColumns}
 * <li> Host application URI {@link Led#URI} and columns {@link LedColumns}
 * <li> Host application URI {@link SensorType#URI} and columns {@link SensorTypeColumns}
 * <li> Host application URI {@link KeyPad#URI} and columns {@link KeyPadColumns}
 * </ol>
 * It is also possible to use a view that returns all capabilities in a single query.
 * The URI is {@link Capabilities#URI}.
 * </p>
 *  * <a name="Security"></a>
 * <h3>Security</h3>
 * Each extension that which to interact with the Registration &amp; Capabilities API should
 * specify a specific plug-in permission in their manifest file {@link Registration#EXTENSION_PERMISSION}.
 * The API implements a security mechanism that ensure that each extension only can access their own
 * registration and notification data. Sharing information between extensions can be obtained for extensions
 * that use the <i>sharedUserId</i> mechanism, however this approach is not recommended.
 * Extensions do not have permission to write data in the capability tables, only host applications
 * have write access.
 * <p>
 * Android Intents are sent when interaction with the extension is needed.
 * See the documentation of the Control API, Widget API, Sensor API and
 * Notification API for more information about intents.
 * To enable the extension to verify the sender of the
 * Intents is a trusted application with access to the APIs and not a malicious
 * application that sends the same Intents on pretext of being a trusted application,
 * the {@link ExtensionColumns#EXTENSION_KEY} field allows plug-in developers to store
 * something that can be used as identification when the Intents are received.
 * Except where {@link android.app.Activity#startActivity(android.content.Intent)} is used,
 * this key is attached to every Intent the different accessory host applications send to
 * the extension as these applications are granted access to the accessory information.
 * The receiving extension should check the value of the key to see if it matches
 * what it has. If not, the plug-in should ignore the Intent. This key is
 * generated by the extension itself. It should be as unique as possible to
 * minimize the risk of several extensions having the key. An extension is free
 * to change the key stored in the database that it owns. As an added precaution,
 * Intents are sent as directed Intents where possible.
 * </p>
 */

public class Registration {

    /**
     * @hide
     * This class is only intended as a utility class containing declared constants
     *  that will be used by plug-in developers.
     */
    protected Registration(){
    }

    /**
     * All extensions should add in their AndroidManifest.xml a <uses-permission>
     * tag to use this permission. The purpose is to indicate to the end user
     * the application containing the extension interacts with the registration API.
     *
     * @since 1.0
     */
    public static final String EXTENSION_PERMISSION = "com.sonyericsson.extras.liveware.aef.EXTENSION_PERMISSION";

    /**
     * Permission used by host applications;
     * Extensions shall use this permission to enforce security when sending intents to
     * a host application using sendBroadcast(Intent, String)
     *
     * @since 1.0
     */
    public static final String HOSTAPP_PERMISSION = "com.sonyericsson.extras.liveware.aef.HOSTAPP_PERMISSION";

    /**
     * Authority for the Registration provider
     *
     * @since 1.0
     */
    public static final String AUTHORITY = "com.sonyericsson.extras.liveware.aef.registration";

    /**
     * Base URI for the Registration provider
     *
     * @since 1.0
     */
    protected static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * Broadcast Intents sent to extensions by the host applications
     */
    public interface Intents {

        /**
         * Intent sent to extensions to request
         * extension registrations.
         * Extensions that are already registered do not need to register again
         * @since 1.0
         */
        String EXTENSION_REGISTER_REQUEST_INTENT = "com.sonyericsson.extras.liveware.aef.registration.EXTENSION_REGISTER_REQUEST";

        /**
         * Intent sent from the host applications to extensions to
         * indicate that an accessory has been connected or disconnected
         * The host application must register the status in the {@link DeviceColumns#ACCESSORY_CONNECTED}
         *  column of the device table before sending this intent
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_CONNECTION_STATUS}</li>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String ACCESSORY_CONNECTION_INTENT = "com.sonyericsson.extras.liveware.aef.registration.ACCESSORY_CONNECTION";

        /**
         * The name of the Intent-extra used to identify the Host Application.
         * The Host Application will send its package name
         * <P>
         * TYPE: TEXT
         * </P>
         * @since 1.0
         */
        String EXTRA_AHA_PACKAGE_NAME = "aha_package_name";

        /**
         * The name of the Intent-extra used to identify the
         * accessory connection status
         * <P>
         * The value must one the predefined constants
         * {@link AccessoryConnectionStatus}.
         * <P>
         * TYPE: INT
         * </P>
         * @since 1.0
         */
        String EXTRA_CONNECTION_STATUS = "connnection_status";

        /**
         * This Intent-extra is used when the settings
         * of an extension is to be displayed by means
         * of starting the activity defined in the
         * {@link ExtensionColumns#CONFIGURATION_ACTIVITY} of the extension.
         * The Intent-extra is only valid for extensions that support the
         * notification API.
         *
         * This extra indicates if the accessory supports showing notifications history.
         * <P>
         * The value can be true (default) or false.
         * If false, the accessory does not support showing the history of notifications
         * and a notification extension might want to hide e.g. "Clear history" from its settings
         * <P>
         * TYPE: BOOLEAN
         * </P>
         * @since 1.0
         */
        String EXTRA_ACCESSORY_SUPPORTS_HISTORY = "supports_history";

        /**
         * This Intent-extra is used when the settings
         * of an extension is to be displayed by means
         * of starting the activity defined in the
         * {@link ExtensionColumns#CONFIGURATION_ACTIVITY} of the extension.
         * The Intent-extra is only valid for extensions that support the
         * notification API
         *
         * This extra indicates if the accessory supports triggering actions
         * linked to notification events. For more information about actions see.
         * {@link com.sonyericsson.extras.liveware.aef.notification.Notification.SourceColumns#ACTION_1}
         * <P>
         * The value can be true (default) or false.
         * If false, the accessory does not support triggering actions from notification events
         * and a notification extension might want to hide action related settings
         * <P>
         * TYPE: BOOLEAN
         * </P>
         * @since 1.0
         */
        String EXTRA_ACCESSORY_SUPPORTS_ACTIONS = "supports_actions";
    }

    /**
     * Interface used to define constants for
     * accessory connection status
     */
    public interface AccessoryConnectionStatus {
        /**
         * The accessory is disconnected from the
         * host application
         */
        int STATUS_DISCONNECTED = 0;

        /**
         * The accessory is connected to the
         * host application
         */
        int STATUS_CONNECTED = 1;
    }

    /**
     * Definitions used for interacting with the Extension-table
     *
     */
    public interface Extension {
        /**
         * Data row MIME type
         */
        String MIME_TYPE = "aef-extensions";

        /**
         * Path segment
         */
        String EXTENSIONS_PATH = "extensions";

        /**
         * Content URI
         */
        Uri URI = Uri.withAppendedPath(BASE_URI, EXTENSIONS_PATH);
    }

    /**
     * Column-definitions for the Extension table
     */
    public interface ExtensionColumns extends BaseColumns {

        /**
         * Displayable name of the extension that may be presented, e.g. in
         * settings
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         * </P>
         *
         * @since 1.0
         */
        String NAME = "name";

        /**
         * Class name of the Android Activity that contains the settings of the extension
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         * </P>
         *
         * @since 1.0
         */
        String CONFIGURATION_ACTIVITY = "configurationActivity";

        /**
         * Short text to describe the current configuration state of the extension
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         * </P>
         *
         * @since 1.0
         */
        String CONFIGURATION_TEXT = "configurationText";

        /**
         * URI of the Android launcher icon representing the extension.
         * This icon is used by the host application when listing extensions
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         * </P>
         *
         * @since 1.0
         */
        String HOST_APP_ICON_URI = "iconLargeUri";

        /**
         * URI of the icon representing the extension.
         * This icon is used on the accessory UI.
         * The size is 36x36 pixels
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         * </P>
         *
         * @since 1.0
         */
        String EXTENSION_ICON_URI = "extensionIconUri";

        /**
         * URI of the monochrome icon representing the extension.
         * This icon is used on the accessory UI.
         * The size is 18x18 pixels
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         * </P>
         *
         * @since 1.0
         */
        String EXTENSION_ICON_URI_BLACK_WHITE = "extensionIconUriBlackWhite";

        /**
         * Used for security reasons for the extension's benefit. If set, this key
         * will be sent as an extra-data in Intents sent to the extension from
         * the host application. This enables the extension to verify that the
         * sender has valid access to the registration content provider.
         * See section <a href="Registration.html#Security">Security</a>
         * for more information
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         * </P>
         *
         * @since 1.0
         */
        String EXTENSION_KEY = "extension_key";

        /**
         * API version. If the extension uses the notification API, this field
         * should tell what version of the notification API that is used.
         * Value 0 means that the API is not used
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         * </P>
         *
         * @since 1.0
         */
        String NOTIFICATION_API_VERSION = "notificationApiVersion";

        /**
         * The package name of an extension.
         * If an extension supports shared user id, the package name
         * must be specified
         * <P>
         * TYPE: TEXT
         * </P>
         * * <P>
         * PRESENCE: OPTIONAL (REQUIRED if shared user id is used by extension)
         * </P>
         *
         * @since 1.0
         */
        String PACKAGE_NAME = "packageName";
    }

    /**
     * Definitions used for interacting with the ApiRegistration-table
     *
     */
    public interface ApiRegistration {
        /**
         * Data row MIME type
         */
        String MIME_TYPE = "aef-registration";

        /**
         * Path segment
         */
        String EXTENSIONS_PATH = "registrations";

        /**
         * Content URI
         */
        Uri URI = Uri.withAppendedPath(BASE_URI, EXTENSIONS_PATH);
    }

    /**
     * Column-definitions for the ApiRegistration-table
     */
    public interface ApiRegistrationColumns extends BaseColumns {

        /**
         * The ID of the extension corresponding to
         * this registration
         *
         * <P>
         * TYPE: INTEGER (long)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String EXTENSION_ID = "extensionId";

        /**
         * Package name name of the Accessory Host Application that
         * this registration is registered to interact with
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         * </P>
         *
         * @since 1.0
         */
        String HOST_APPLICATION_PACKAGE = "hostAppPackageName";

        /**
         * API version. If the the widget API is used, this field
         * should tell what version of the widget API that is used.
         * Value 0 means that the API is not used
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         * </P>
         *
         * @since 1.0
         */
        String WIDGET_API_VERSION = "widgetApiVersion";

        /**
         * API version. If the the control API is used, this field
         * should tell what version of the control API that is used.
         * Value 0 means that the API is not used
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         * </P>
         *
         * @since 1.0
         */
        String CONTROL_API_VERSION = "controlApiVersion";

        /**
         * API version. If the the sensor API is used, this field
         * should tell what version of the sensor API that is used.
         * Value 0 means that the API is not used
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         * </P>
         *
         * @since 1.0
         */
        String SENSOR_API_VERSION = "sensorApiVersion";
    }

    /**
     * Definitions used for interacting with the Capabilities-view
     *
     */
    public interface Capabilities {

        /**
         * Data row MIME type for capabilities
         */
        String CAPABILITIES_MIME_TYPE = "aef-capabilities";

        /**
         * Path segment capabilities as a separate view
         */
        String CAPABILITIES_PATH = "capabilities";

        /**
         * Content URI
         */
        Uri URI = Uri.withAppendedPath(BASE_URI, CAPABILITIES_PATH);
    }

    /**
     * Definitions used for interacting with the Host application-table
     *
     */
    public interface HostApp {
        /**
         * Data row MIME type
         */
        String MIME_TYPE = "aef-host_application";

        /**
         * Path segment
         */
        String HOST_APP_PATH = "host_application";

        /**
         * Content URI
         */
        Uri URI = Uri.withAppendedPath(BASE_URI, HOST_APP_PATH);
    }

    /**
     * Column-definitions for the Host application table
     */
    public interface HostAppColumns extends BaseColumns {

        /**
         * The package name of a host application
         *
         * @since 1.0
         */
        String PACKAGE_NAME = "packageName";

        /**
         * The version of a host application
         *
         * @since 1.0
         */
        String VERSION = "version";

        /**
         * API version. If the host application supports the Widget API, this field
         * should tell what version of the Widget API that is supported.
         * Value 0 means that the API is not supported
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         * </P>
         *
         * @since 1.0
         */
        String WIDGET_API_VERSION = "widgetApiVersion";

        /**
         * The maximum supported widget refresh rate
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         * </P>
         *
         * @since 1.0
         */
        String WIDGET_REFRESH_RATE = "widgetRefreshrate";

        /**
         * API version. If the host application supports the Control API, this field
         * should tell what version of the Control API that is supported.
         * Value 0 means that the API is not supported
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         * </P>
         *
         * @since 1.0
         */
        String CONTROL_API_VERSION = "controlApiVersion";

        /**
         * API version. If the host application supports the Sensor API, this field
         * should tell what version of the Sensor API that is supported.
         * Value 0 means that the API is not supported
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         * </P>
         *
         * @since 1.0
         */
        String SENSOR_API_VERSION = "sensorApiVersion";

        /**
         * API version. If the host application supports the Notification API, this field
         * should tell what version of the Notification API that is supported.
         * Value 0 means that the API is not supported
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         * </P>
         *
         * @since 1.0
         */
        String NOTIFICATION_API_VERSION = "notificationApiVersion";

    }

    /**
     * Definitions used for interacting with the Device-table
     *
     */
    public interface Device {
        /**
         * Data row MIME type
         */
        String MIME_TYPE = "aef-device";

        /**
         * Path segment
         */
        String DEVICES_PATH = "device";

        /**
         * Content URI
         */
        Uri URI = Uri.withAppendedPath(BASE_URI, DEVICES_PATH);
    }

    /**
     * Column-definitions for the Device table
     */
    public interface DeviceColumns extends BaseColumns {

        /**
         * The ID of the host application corresponding to
         * this device
         *
         * <P>
         * TYPE: INTEGER (long)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String HOST_APPLICATION_ID = "hostAppId";

        /**
         * The device model
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         *  </P>
         *
         * @since 1.0
         */
        String MODEL = "model";

        /**
         * The type of the device
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         *  </P>
         *
         * @since 1.0
         */
        String TYPE = "type";

        /**
         * The sub-type of the device
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         *  </P>
         *
         * @since 1.0
         */
        String SUB_TYPE = "subType";

        /**
         * The marketing name of the device
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         *  </P>
         *
         * @since 1.0
         */
        String MARKETING_NAME = "marketingName";

        /**
         * The vendor of the device
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         *  </P>
         *
         * @since 1.0
         */
        String VENDOR = "vendor";

        /**
         * The UID of the device
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         *  </P>
         *
         * @since 1.0
         */
        String UID = "uid";

        /**
         * The firmware version of the device
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         *  </P>
         *
         * @since 1.0
         */
        String FIRMWARE_VERSION = "firmwareVersion";

        /**
         * The height of the widget image
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         *  </P>
         *
         * @since 1.0
         */
        String WIDGET_IMAGE_HEIGHT = "widgetImageHeight";

        /**
         * The width of the widget image
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         *  </P>
         *
         * @since 1.0
         */
        String WIDGET_IMAGE_WIDTH = "widgetImageWidtht";

        /**
         * Indicates if the device has a vibrator
         *
         * <P>
         * TYPE: BOOLEAN
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String VIBRATOR = "vibrator";

        /**
         * Indicates if the device is connected to the
         * host application
         *
         * <P>
         * TYPE: BOOLEAN
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String ACCESSORY_CONNECTED = "accessory_connected";
    }

    /**
     * Definitions used for interacting with the Display-table
     *
     */
    public interface Display {
        /**
         * Data row MIME type
         */
        String MIME_TYPE = "aef-display";

        /**
         * Path segment
         */
        String DISPLAYS_PATH = "display";

        /**
         * Content URI
         */
        Uri URI = Uri.withAppendedPath(BASE_URI, DISPLAYS_PATH);
    }


    /**
     * Column-definitions for the Display table
     */
    public interface DisplayColumns extends BaseColumns {

        /**
         * The ID of the device corresponding to
         * this display
         *
         * <P>
         * TYPE: INTEGER (long)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String DEVICE_ID = "deviceId";

        /**
         * The width of the display
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String DISPLAY_WIDTH = "width";

        /**
         * The width of the display
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String DISPLAY_HEIGHT = "height";

        /**
         * The number of colors supported by the display
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String COLORS = "colors";

        /**
         * The refresh rate supported by the display
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String REFRESH_RATE = "refreshRate";

        /**
         * The latency of the display
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String LATENCY = "latency";

        /**
         * Indicates if tap touch is supported by the display
         *
         * <P>
         * TYPE: BOOLEAN
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String TAP_TOUCH = "tapTouch";

        /**
         * Indicates if motion touch is supported by the display
         *
         * <P>
         * TYPE: BOOLEAN
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String MOTION_TOUCH = "motionTouch";
    }

    /**
     * Definitions used for interacting with the Sensor-table
     *
     */
    public interface Sensor {
        /**
         * Data row MIME type
         */
        String MIME_TYPE = "aef-sensor";

        /**
         * Path segment
         */
        String SENSORS_PATH = "sensor";

        /**
         * Content URI
         */
        Uri URI = Uri.withAppendedPath(BASE_URI, SENSORS_PATH);
    }


    /**
     * Column-definitions for the Sensor table
     */
    public interface SensorColumns extends BaseColumns {

        /**
         * The ID of the device corresponding to
         * this sensor
         *
         * <P>
         * TYPE: INTEGER (long)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String DEVICE_ID = "deviceId";

        /**
         * The ID of the SensorType corresponding to
         * this sensor
         *
         * <P>
         * TYPE: INTEGER (long)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String SENSOR_TYPE_ID = "sensorTypeId";

        /**
         * The sensor resolution
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         *  </P>
         *
         * @since 1.0
         */
        String RESOLUTION = "resolution";

        /**
         * The minimum delay of the sensor
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         *  </P>
         *
         * @since 1.0
         */
        String MINIMUM_DELAY = "minimumDelay";

        /**
         * The maximum range of the sensor
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         *  </P>
         *
         * @since 1.0
         */
        String MAXIMUM_RANGE = "maximumRange";

        /**
         * The name of the sensor
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         *  </P>
         *
         * @since 1.0
         */
        String NAME = "name";

        /**
         * The ID of the sensor as defined by the Host Application
         * this ID is used by the SensorAPI and is not necessarily the same
         * value as the ID column
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String SENSOR_ID = "sensorId";

        /**
         * Indicates if the sensor supports interrupt mode
         * In interrupt mode, the sensor only sends data when new values
         * are available
         *
         * <P>
         * TYPE: INTEGER (int) (0= Not supported, 1= Supported)
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         *  </P>
         *
         * @since 1.0
         */
        String SUPPORTS_SENSOR_INTERRUPT = "sensorInterrupt";
    }

    /**
     * Definitions used for interacting with the Led-table
     *
     */
    public interface Led {
        /**
         * Data row MIME type
         */
        String MIME_TYPE = "aef-led";

        /**
         * Path segment
         */
        String LEDS_PATH = "led";

        /**
         * Content URI
         */
        Uri URI = Uri.withAppendedPath(BASE_URI, LEDS_PATH);
    }

    /**
     * Column-definitions for the Led-table
     */
    public interface LedColumns extends BaseColumns {

        /**
         * The ID of the device corresponding to
         * this LED
         *
         * <P>
         * TYPE: INTEGER (long)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String DEVICE_ID = "deviceId";

        /**
         * The number of colors supported by the LED
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String COLORS = "colors";
    }

    /**
     * Definitions used for interacting with the Input-table
     *
     */
    public interface Input {
        /**
         * Data row MIME type
         */
        String MIME_TYPE = "aef-input";

        /**
         * Path segment
         */
        String INPUTS_PATH = "input";

        /**
         * Content URI
         */
        Uri URI = Uri.withAppendedPath(BASE_URI, INPUTS_PATH);
    }

    /**
     * Column-definitions for the Sensor table
     */
    public interface InputColumns extends BaseColumns {

        /**
         * The ID of the device corresponding to
         * an Input
         *
         * <P>
         * TYPE: INTEGER (long)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String DEVICE_ID = "deviceId";

        /**
         * The ID of the keypad
         *
         * <P>
         * TYPE: INTEGER (long)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String KEY_PAD_ID = "keyPadId";

        /**
         * The enable status of the Input
         *
         * <P>
         * TYPE: BOOLEAN
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String ENABLED = "enabled";
    }

    /**
     * Definitions used for interacting with the Sensor-type-table
     *
     */
    public interface SensorType {
        /**
         * Data row MIME type
         */
        String MIME_TYPE = "aef-sensor_type";

        /**
         * Path segment
         */
        String SENSOR_TYPES_PATH = "sensor_type";

        /**
         * Content URI
         */
        Uri URI = Uri.withAppendedPath(BASE_URI, SENSOR_TYPES_PATH);
    }

    /**
     * Column-definitions for the SensorType table
     */
    public interface SensorTypeColumns extends BaseColumns {

        /**
         * The Type.
         * <p>
         * The following sensor types are supported:
         * </p>
         * <ul>
         * <li>{@link com.sonyericsson.extras.liveware.aef.sensor.Sensor#SENSOR_TYPE_ACCELEROMETER}</li>
         * <li>{@link com.sonyericsson.extras.liveware.aef.sensor.Sensor#SENSOR_TYPE_LIGHT}</li>
         * </ul>
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String TYPE = "type";

        /**
         * This column value indicates whether the sensor
         * sends information of delicate nature
         *
         * <P>
         * TYPE: BOOLEAN
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         *  </P>
         *
         * @since 1.0
         */
        String DELICATE_SENSOR_DATA = "delicate_data";
    }

        /**
         * Definitions used for interacting with the Keypad-table
         *
         */
        public interface KeyPad {
            /**
             * Data row MIME type
             */
            String MIME_TYPE = "aef-keypad";

            /**
             * Path segment
             */
            String KEYPADS_PATH = "keypad";

            /**
             * Content URI
             */
            Uri URI = Uri.withAppendedPath(BASE_URI, KEYPADS_PATH);
        }

        /**
         * Column-definitions for the Keypad table
         */
        public interface KeyPadColumns extends BaseColumns {

            /**
             * The Type
             *
             * <P>
             * TYPE: TEXT
             * </P>
             * <P>
             * PRESENCE: REQUIRED
             *  </P>
             *
             * @since 1.0
             */
            String TYPE = "type";
        }
}