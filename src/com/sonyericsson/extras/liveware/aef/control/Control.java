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

package com.sonyericsson.extras.liveware.aef.control;


/**
 * <h1>Control API is a part of the Smart Extension API's</h1>
 * <p>
 * Some of our Smart accessories will support the Control API.
 * The Control API enables the Extension to take total control of the accessory.
 * It takes control over the display, LEDs, vibrator, input events.
 * Because of this, only one Extension can run in this mode at a time.
 * </p>
 * <p>Topics covered here:
 * <ol>
 * <li><a href="#Registration">Registration</a>
 * <li><a href="#Lifecycle">Extension lifecycle</a>
 * <li><a href="#DisplayControl">Controlling the display</a>
 * <li><a href="#LEDControl">Controlling the LEDs</a>
 * <li><a href="#VibratorControl">Controlling the vibrator</a>
 * <li><a href="#KeyEvents">Key events</a>
 * <li><a href="#TouchEvents">Touch events</a>
 * <li><a href="#DataSending">Displaying content on the accessory display</a>
 * </ol>
 * </p>
 * <a name="Registration"></a>
 * <h3>Registration</h3>
 * <p>
 * Before an Control Extension can use an accessory, it must use the registration API
 * content provider to insert a record in the extension table. It must also register
 * information in the registration table. This must be done for each Host Application
 * that the Extension wants to interact with.
 * </p>
 * <p>
 * In order to find out what Host Applications are available and what capabilities they
 * support, the Extension should use the Capability API.
 * </p>
 * <a name="Lifecycle"></a>
 * <h3>Extension lifecycle</h3>
 * <p>
 * After a successful registration the Extension can start communicating with the Host
 * Application. Since a Extension implementing this API takes complete control over the
 * accessory only one Extension can run at a time.
 * </p>
 * <p>
 * An Extension cannot just start executing whenever it wants, it needs to make sure that
 * no other Extension is running, therefore the Extension can only request to be started,
 * {@link Intents#CONTROL_START_REQUEST_INTENT}. When the Host Application is ready to give
 * control to the Extension it will send a {@link Intents#CONTROL_START_INTENT}, see figure
 * below.
 * </p>
 * <p>
 * <img src="../../../../../../../images/control_api_extension_lifecycle.png"
 * alt="Extension lifecycle" border="1" />
 * </p>
 * <p>
 * When the Extension requests to start controlling the accessory the Host Application can
 * either accept the request and give control to the Extension, or if something is not right
 * the Host Application can send a {@link Intents#CONTROL_ERROR_INTENT}. See
 * {@link Intents#EXTRA_ERROR_CODE} for different error codes that the Host Application can send.
 * </p>
 * <p>
 * The {@link Intents#CONTROL_RESUME_INTENT} is sent when the Extension is visible on the accessory.
 * From this point on the Extension controls everything, the Host Application just forwards the
 * information between the accessory and the Extension.
 * </p>
 * <p>
 * An Extension can also be paused, either if a high priority Extension needs to run for a
 * while or if the Host Application is in charge of the display state and the display is
 * turned off. In this case the Host Application sends a {@link Intents#CONTROL_PAUSE_INTENT}
 * to the Extension. This means that there is no point for the Extension to update the display
 * since it is either turned off or someone else has control over it. If the Extension would
 * break this rule and try to update the display anyway, the Host Application will ignore these
 * calls.
 * </p>
 * <p>
 * When the Extension is in a paused state it no longer has control over the display/LEDs/
 * vibrator/key events. As an example one could say that a telephony Extension has high priority.
 * E.g. when a random Extension is running and the user receives a phone call. We want to pause
 * the running Extension and let the telephony Extension display the caller id on the accessory
 * display. When the phone call ends the telephony Extension is done and the other Extension can
 * resume its running, it will then receive a {@link Intents#CONTROL_RESUME_INTENT}.
 * </p>
 * <p>
 * When the {@link Intents#CONTROL_RESUME_INTENT} is sent from a Host Application the Extension is
 * once again in charge of everything.
 * </p>
 * <p>
 * When the user chooses to exit the Extension the Host Application will send a
 * {@link Intents#CONTROL_PAUSE_INTENT} followed by a {@link Intents#CONTROL_STOP_INTENT}.
 * From this point on the Host Application regains control.
 * </p>
 * <p>
 * If the Extension would like to stop itself when running, like the telephony Extension, it can
 * send a {@link Intents#CONTROL_STOP_REQUEST_INTENT} to the Host Application. The Host Application
 * will then make sure to stop it and send a {@link Intents#CONTROL_STOP_INTENT}.
 * If the extension was not already paused the it will be paused before it is stopped and a
 * {@link Intents#CONTROL_PAUSE_INTENT} is sent before the {@link Intents#CONTROL_STOP_INTENT}.
 * In case another Extension has been paused it will be resumed.
 * </p>
 *
 * <a name="DisplayControl"></a>
 * <h3>Controlling the display</h3>
 * <p>
 * Extensions implementing this API have the possibility to control the state of the accessory
 * display.The display can be controlled via {@link Intents#CONTROL_SET_SCREEN_STATE_INTENT}.
 * </p>
 * <p>
 * It is important that you program your Extension so that it consumes as little power as possible,
 * both on the phone side and on the accessory. The accessory has a much smaller battery then the
 * phone so use this functionality with caution. When possible, let the Host Application take control
 * of the display state. That way you don't have to bother about the power consumption on the accessory.
 * You can do this by setting the display state to "Auto".
 * </p>
 * <p>
 * By default when your Extension starts the display state will be set to "Auto", which means that the
 * Host Application controls the on/off/dim behavior. If the Extension wants to control the display state
 * it must explicitly change the state.
 * </p>
 * <p>
 * If the Extension controls the display state and you get a {@link Intents#CONTROL_STOP_INTENT}, meaning
 * your Extension is no longer running, the Host Application will automatically take over the display
 * control.
 * </p>
 * <p>
 * Note that when in "Auto" mode, the Extension will receive a {@link Intents#CONTROL_PAUSE_INTENT} when
 * display is off and a {@link Intents#CONTROL_RESUME_INTENT} when the display goes back on.
 * </p>
 * <a name="LEDControl"></a>
 * <h3>Controlling the LEDs</h3>
 * <p>
 * The accessory might have one or more LEDs that are used to notify the user about events. The
 * Extension can find information about the LEDs for a certain accessory via the Registration &amp;
 * Capability API.
 * </p>
 * <p>
 * If the accessory has LEDs, the Extension can control them via the Control API. The LEDs can be
 * controlled via the {@link Intents#CONTROL_LED_INTENT}.
 * Note that the Host Application might overtake the control of the LED at any time if it wants to
 * show some important notifications to the user, e.g. when the accessory battery level is low.
 * The Extension is unaware of this so it might still try to control the LEDs but the Host
 * Application will ignore the calls.
 * </p>
 * <a name="VibratorControl"></a>
 * <h3>Controlling the vibrator</h3>
 * <p>
 * Our accessories might or might not have a vibrator. The Extension can find this out by checking
 * the capabilities of the Host Application via the Registration &amp; Capability API. If the accessory
 * has a vibrator it is controllable via the Control API, {@link Intents#CONTROL_VIBRATE_INTENT}.
 * </p>
 * <a name="KeyEvents"></a>
 * <h3>Key events</h3>
 * <p>
 * The accessory might have several hardware keys. Your extension will receive the key events when
 * one of the keys is pressed. The {@link Intents#CONTROL_KEY_EVENT_INTENT} is sent to the Extension when
 * a user presses a key on the accessory.
 * </p>
 * <p>
 * The Intent carries a few parameters, such as the time stamp of the event, the type of event
 * (press, release and repeat) and also the key code. The accessory might have one or more keypads
 * defined. Extensions can look this up in the Registration &amp; Capabilities API. Each key will have a
 * unique key code for identification. Key codes can be found in the product SDK.
 * </p>
 * <a name="TouchEvents"></a>
 * <h3>Touch events</h3>
 * <p>
 * Certain accessories might have a touch display. Extensions can find this information using the
 * Registration &amp; Capabilities API. The {@link Intents#CONTROL_TOUCH_EVENT_INTENT} is sent to the
 * Extension when a user taps the accessory display.
 * </p>
 * <a name="DataSending"></a>
 * <h3>Displaying content on the accessory display</h3>
 * <p>
 * Since the Extension is controlling the accessory it also controls what is visible on the display.
 * The content visible to the user comes from the Extension. Basically the Extension sends images to
 * be displayed on the accessory display. To find out the dimensions of the display and the color depth
 * it supports the Extension can use the Registration &amp; Capabilities API. The
 * {@link Intents#CONTROL_DISPLAY_DATA_INTENT} is sent from the Extension when it wants to update the accessory
 * display. Extensions can also clear the accessory display at any point if they wants to by sending
 * the {@link Intents#CONTROL_CLEAR_DISPLAY_INTENT}.
 * </p>
 * <p>
 * The Extension can send images as raw data (byte array) or it can just send the URI of the image to
 * be displayed. Note that we are using Bluetooth as bearer which means that we can't send that many
 * frames per second (FPS). Refresh rate of the display can be found in the Registration &amp; Capabilities API.
 * </p>
 */

public class Control {

    /**
     * @hide
     * This class is only intended as a utility class containing declared constants
     * that will be used by Control API Extension developers.
     */
    protected Control() {
    }

    /**
     * Intents sent between Control Extensions and Accessory Host Applications.
     */
    public interface Intents {

        /**
         * Intent sent by the Extension when it wants to take control of the accessory display.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_START_REQUEST_INTENT = "com.sonyericsson.extras.aef.control.START_REQUEST";

        /**
         * Intent sent by the Extension when it wants to stop controlling the accessory display.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_STOP_REQUEST_INTENT = "com.sonyericsson.extras.aef.control.STOP_REQUEST";

        /**
         * Intent sent by the Host Application when it grants control of the accessory display to the Extension.
         * This Intent might be sent when the Host Application wants to start the Extension or as a
         * result of the Extensions sending a {@link #CONTROL_START_REQUEST_INTENT} Intent.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_START_INTENT = "com.sonyericsson.extras.aef.control.START";

        /**
         * Intent sent by the Host Application when it takes back control of the accessory display from the Extension.
         * This Intent might be sent when the Host Application wants to stop the Extension or as a
         * result of the Extensions sending a {@link #CONTROL_STOP_REQUEST_INTENT} Intent.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_STOP_INTENT = "com.sonyericsson.extras.aef.control.STOP";

        /**
         * Intent sent by the Host Application when the Extension is no longer visible on the display.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_PAUSE_INTENT = "com.sonyericsson.extras.aef.control.PAUSE";

        /**
         * Intent sent by the Host Application when the Extension is visible on the display.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_RESUME_INTENT = "com.sonyericsson.extras.aef.control.RESUME";

        /**
         * Intent sent by the Host Application when a error occurs
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_ERROR_CODE}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_ERROR_INTENT = "com.sonyericsson.extras.aef.control.ERROR";

        /**
         * Intent sent by the Extension when it wants to set the state of the accessory display.
         * If the Extension does not set the state explicitly it will by default be controlled by
         * the Host Application (Display Auto) for optimal power consumption.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_SCREEN_STATE}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_SET_SCREEN_STATE_INTENT = "com.sonyericsson.extras.aef.control.SET_SCREEN_STATE";

        /**
         * Intent sent by the Extension when it wants to control one of the LEDs available on the accessory.
         * Every Host Application will expose information about its LEDs in the
         * Registration &amp; Capabilities API.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_LED_ID}</li>
         * <li>{@link #EXTRA_LED_COLOR}</li>
         * <li>{@link #EXTRA_ON_DURATION}</li>
         * <li>{@link #EXTRA_OFF_DURATION}</li>
         * <li>{@link #EXTRA_REPEATS}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_LED_INTENT = "com.sonyericsson.extras.aef.control.LED";

        /**
         * Intent sent by the Extension when it wants to stop an ongoing LED sequence on the accessory.
         * If the LED specified in the Intent-extra if off this Intent will be ignored by the Host Application.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_LED_ID}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_STOP_LED_INTENT = "com.sonyericsson.extras.aef.control.STOP_LED";

        /**
         * Intent sent by the Extension when it wants to control the vibrator available on the accessory.
         * Every Host Application will expose information about the vibrator if it has one in the
         * Registration &amp; Capabilities API.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_ON_DURATION}</li>
         * <li>{@link #EXTRA_OFF_DURATION}</li>
         * <li>{@link #EXTRA_REPEATS}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_VIBRATE_INTENT = "com.sonyericsson.extras.aef.control.VIBRATE";

        /**
         * Intent sent by the Extension when it wants to stop an ongoing vibration on the accessory.
         * If no vibration is ongoing this Intent will be ignored by the Host Application.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_STOP_VIBRATE_INTENT = "com.sonyericsson.extras.aef.control.STOP_VIBRATE";

        /**
         * Intent sent by the Extension whenever it wants to update the accessory display.
         * The display size is accessory dependent and can be found using the Registration &amp; Capabilities API.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_DATA_URI}</li>
         * <li>{@link #EXTRA_DATA}</li>
         * <li>{@link #EXTRA_X_OFFSET}</li>
         * <li>{@link #EXTRA_Y_OFFSET}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_DISPLAY_DATA_INTENT = "com.sonyericsson.extras.aef.control.DISPLAY_DATA";

        /**
         * Intent sent by the Extension whenever it wants to clear the accessory display.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_CLEAR_DISPLAY_INTENT = "com.sonyericsson.extras.aef.control.CLEAR_DISPLAY";

        /**
         * Intent sent by the Host Application to the controlling Extension whenever an hardware
         * key is pressed/released.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_KEY_ACTION}</li>
         * <li>{@link #EXTRA_TIMESTAMP}</li>
         * <li>{@link #EXTRA_KEY_CODE}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_KEY_EVENT_INTENT = "com.sonyericsson.extras.aef.control.KEY_EVENT";

        /**
         * Intent sent by the Host Application to the controlling Extension whenever an touch
         * event is detected.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_TOUCH_ACTION}</li>
         * <li>{@link #EXTRA_TIMESTAMP}</li>
         * <li>{@link #EXTRA_X_POS}</li>
         * <li>{@link #EXTRA_Y_POS}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_TOUCH_EVENT_INTENT = "com.sonyericsson.extras.aef.control.TOUCH_EVENT";

        /**
         * Intent sent by the Host Application to the controlling Extension whenever an swipe
         * event is detected.
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_SWIPE_DIRECTION}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String CONTROL_SWIPE_EVENT_INTENT = "com.sonyericsson.extras.aef.control.SWIPE_EVENT";

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
         * The name of the Intent-extra used to identify the Extension.
         * The Extension will send its package name
         * <P>
         * TYPE: TEXT
         * </P>
         * @since 1.0
         */
        String EXTRA_AEA_PACKAGE_NAME = "aea_package_name";

        /**
         * The name of the Intent-extra carrying the state of the display
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * ALLOWED VALUES:
         * <ul>
         * <li>{@link #SCREEN_STATE_OFF}</li>
         * <li>{@link #SCREEN_STATE_DIM}</li>
         * <li>{@link #SCREEN_STATE_ON}</li>
         * <li>{@link #SCREEN_STATE_AUTO}</li>
         * </ul>
         * </P>
         * @since 1.0
         */
        String EXTRA_SCREEN_STATE = "screen_state";

        /**
         * The name of the Intent-extra carrying the ID of the LED to be controlled
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        String EXTRA_LED_ID = "led_id";

        /**
         * The name of the Intent-extra carrying the color you want the LED to blink with
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        String EXTRA_LED_COLOR = "led_color";

        /**
         * The name of the Intent-extra carrying the "on" duration in milliseconds
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        String EXTRA_ON_DURATION = "on_duration";

        /**
         * The name of the Intent-extra carrying the "off" duration in milliseconds
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        String EXTRA_OFF_DURATION = "off_duration";

        /**
         * The name of the Intent-extra carrying the number of repeats of the on/off pattern.
         * Note, the value {@link #REPEAT_UNTIL_STOP_INTENT} means that the on/off pattern is repeated until
         * the {@link #CONTROL_STOP_VIBRATE_INTENT} or {@link #CONTROL_STOP_LED_INTENT} intent is received
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        String EXTRA_REPEATS = "repeats";

        /**
         * The name of the Intent-extra used to identify the URI of the image to be displayed on the
         * accessory display. If the image is in raw data (e.g. an array of bytes) use
         * {@link #EXTRA_DATA} instead
         * <P>
         * TYPE: TEXT
         * </P>
         * @since 1.0
         */
        String EXTRA_DATA_URI = "data_uri";

        /**
         * The name of the Intent-extra used to identify the data to be displayed on the accessory
         * display. This Intent-extra should be used if the image is in raw data (e.g. an array of bytes)
         * <P>
         * TYPE: BYTE ARRAY
         * </P>
         * @since 1.0
         */
        String EXTRA_DATA = "data";

        /**
         * The name of the Intent-extra used to identify the pixel offset from the left side of the accessory
         * display
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        String EXTRA_X_OFFSET = "x_offset";

        /**
         * The name of the Intent-extra used to identify the pixel offset from the top of the accessory
         * display
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        String EXTRA_Y_OFFSET = "y_offset";

        /**
         * The name of the Intent-extra used to identify the type of key event
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * ALLOWED VALUES:
         * <ul>
         * <li>{@link #KEY_ACTION_PRESS}</li>
         * <li>{@link #KEY_ACTION_RELEASE}</li>
         * <li>{@link #KEY_ACTION_REPEAT}</li>
         * </ul>
         * </P>
         * @since 1.0
         */
        String EXTRA_KEY_ACTION = "event_type";

        /**
         * The name of the Intent-extra used to carry the time stamp of the key or touch event
         * <P>
         * TYPE: INTEGER (long)
         * </P>
         * @since 1.0
         */
        String EXTRA_TIMESTAMP = "timestamp";

        /**
         * The name of the Intent-extra used to identify the keycode.
         * Information about what type of keypad a accessory has can be found using the
         * Registration &amp; Capabilities API
         * <P>
         * ALLOWED VALUES:
         * Any key code defined in the {@link KeyCodes} interface.
         * </P>
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        String EXTRA_KEY_CODE = "key_code";

        /**
         * The name of the Intent-extra used to indicate the touch action
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * ALLOWED VALUES:
         * <ul>
         * <li>{@link #TOUCH_ACTION_PRESS}</li>
         * <li>{@link #TOUCH_ACTION_LONGPRESS}</li>
         * <li>{@link #TOUCH_ACTION_RELEASE}</li>
         * </ul>
         * </P>
         * @since 1.0
         */
        String EXTRA_TOUCH_ACTION = "action";

        /**
         * The name of the Intent-extra used to indicate the direction
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * ALLOWED VALUES:
         * <ul>
         * <li>{@link #SWIPE_DIRECTION_UP}</li>
         * <li>{@link #SWIPE_DIRECTION_DOWN}</li>
         * <li>{@link #SWIPE_DIRECTION_LEFT}</li>
         * <li>{@link #SWIPE_DIRECTION_RIGHT}</li>
         * </ul>
         * </P>
         * @since 1.0
         */
        String EXTRA_SWIPE_DIRECTION = "direction";

        /**
         * The name of the Intent-extra used to carry the X coordinate of the touch event
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        String EXTRA_X_POS = "x_pos";

        /**
         * The name of the Intent-extra used to carry the Y coordinate of the touch event
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        String EXTRA_Y_POS = "y_pos";

        /**
         * The name of the Intent-extra used to carry the error code
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * ALLOWED VALUES:
         * <ul>
         * <li>0: 'Registration information missing'</li>
         * <li>1: 'Accessory not connected'</li>
         * <li>2: 'Host Application busy'</li>
         * </ul>
         * </P>
         * @since 1.0
         */
        String EXTRA_ERROR_CODE = "error_code";

        /**
         * The name of the Intent-extra containing the key set by the extension.
         * This Intent-data is present in all Intents sent by accessory host application,
         * except where {@link android.app.Activity#startActivity(android.content.Intent)}
         * is used. See section <a href="Registration.html#Security">Security</a>
         * for more information
         *
         * @since 1.0
         */
        String EXTRA_EXTENSION_KEY = "extension_key";


        /**
         * The touch action is a press event
         *
         * @since 1.0
         */
        int TOUCH_ACTION_PRESS = 0;

        /**
         * The touch action is a long press event
         *
         * @since 1.0
         */
        int TOUCH_ACTION_LONGPRESS = 1;

        /**
         * The touch action is a release event
         *
         * @since 1.0
         */
        int TOUCH_ACTION_RELEASE = 2;

        /**
         * The direction of the swipe event is up
         *
         * @since 1.0
         */
        int SWIPE_DIRECTION_UP = 0;

        /**
         * The direction of the swipe event is down
         *
         * @since 1.0
         */
        int SWIPE_DIRECTION_DOWN = 1;

        /**
         * The direction of the swipe event is left
         *
         * @since 1.0
         */
        int SWIPE_DIRECTION_LEFT = 2;

        /**
         * The direction of the swipe event is right
         *
         * @since 1.0
         */
        int SWIPE_DIRECTION_RIGHT = 3;

        /**
         * The screen off state
         *
         * @since 1.0
         */
        int SCREEN_STATE_OFF = 0;

        /**
         * The screen dim state
         *
         * @since 1.0
         */
        int SCREEN_STATE_DIM = 1;

        /**
         * The screen on state
         *
         * @since 1.0
         */
        int SCREEN_STATE_ON = 2;

        /**
         * The screen state is automatically handled by the host application
         *
         * @since 1.0
         */
        int SCREEN_STATE_AUTO = 3;

        /**
         * The key event is a key press event
         *
         * @since 1.0
         */
        int KEY_ACTION_PRESS = 0;

        /**
         * The key event is a key release event
         *
         * @since 1.0
         */
        int KEY_ACTION_RELEASE = 1;

        /**
         * The key event is a key repeat event
         *
         * @since 1.0
         */
        int KEY_ACTION_REPEAT = 2;

        /**
         * The control action is turned on
         *
         * @since 1.0
         */
        int CONTROL_ACTION_ON = 0;

        /**
         * The control action is turned off
         *
         * @since 1.0
         */
        int CONTROL_ACTION_OFF = 1;

        /**
         * Vibration or LED is repeated until explicitly stopped
         *
         * @since 1.0
         */
        int REPEAT_UNTIL_STOP_INTENT = -1;
    }

    /**
     * Interface used to define constants for
     * keycodes
     */
    public interface KeyCodes {

        /**
         * Keycode representing a play button
         */
        int KEYCODE_PLAY = 1;

        /**
         * Keycode representing a next button
         */
        int KEYCODE_NEXT = 2;

        /**
         * Keycode representing a previous button
         */
        int KEYCODE_PREVIOUS = 3;

        /**
         * Keycode representing an action button
         */
        int KEYCODE_ACTION = 4;

        /**
         * Keycode representing a volume down button
         */
        int KEYCODE_VOLUME_DOWN = 5;

        /**
         * Keycode representing a volume up button
         */
        int KEYCODE_VOLUME_UP = 6;

        /**
         * Keycode representing a back button
         */
        int KEYCODE_BACK = 7;
    }
}