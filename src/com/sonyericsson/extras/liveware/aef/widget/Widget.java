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

package com.sonyericsson.extras.liveware.aef.widget;

import com.sonyericsson.extras.liveware.aef.registration.Registration.ExtensionColumns;

/**
 * <h1>The Widget API is a part of the Smart Extension API's</h1>
 * <p>
 * Some of our advanced accessories will support the Widget API.
 * The Widget API enables the Extension to display a live image on the
 * main menu of the accessory, sort of a preview of what the Extension is about.
 * </p>
 * <p>Topics covered here:
 * <ol>
 * <li><a href="#Initial">Initial Widget image</a>
 * <li><a href="#WidgetSize">How do Extensions find out correct Widget image size</a>
 * <li><a href="#Refresh">Refreshing the Widget image</a>
 * </ol>
 * </p>
 * <a name="Initial"></a>
 * <h3>Initial Widget image</h3>
 * <p>
 * Widgets will usually be the building blocks for the main menu of the accessories
 * that support Widgets.
 * At certain occasions, the Host Application will request a Widget image from the
 * Extension, {@link Intents#WIDGET_START_REFRESH_IMAGE_INTENT}. E.g. when the user powers on the accessory,
 * or when a new Widget Extension is installed.
 * When the Extension receives this Intent it must send back a Widget image to the Host
 * Application, {@link Intents#WIDGET_IMAGE_UPDATE_INTENT}
 * The Extension can continue to update its image when it finds it appropriate as long as the intent
 * {@link Intents#WIDGET_STOP_REFRESH_IMAGE_INTENT} has not been received. The extension can resume updating
 * its image when {@link Intents#WIDGET_START_REFRESH_IMAGE_INTENT} has been received again.
 * </p>
 * <a name="WidgetSize"></a>
 * <h3>How do Extensions find out correct Widget image size</h3>
 * <p>
 * Before an Extension sends the Widget image to the Host Application it has to figure out
 * what size the image should be. This might vary between accessories as some have a larger
 * display. This information can be found using the Registration &amp; Capabilities API.
 * Every Host Application will write down its parameters into the Capabilities database.
 * </p>
 * <a name="Refresh"></a>
 * <h3>Refreshing the Widget image</h3>
 * <p>
 * In order to allow the user to interact with the Widget, the Extension will get touch events
 * that occur when your Widget is in focus on the accessory display, {@link Intents#WIDGET_ONTOUCH_INTENT}
 * This way the Extension receives user feedback and can adapt, refresh Widget image.
 * </p>
 * <p>
 * As an example one could mention a media player controller Widget. The initial Widget image show a couple
 * of buttons, play/pause, previous, next, etc. When a user presses somewhere on on the Widget the
 * {@link Intents#WIDGET_ONTOUCH_INTENT} will be sent to the Extension. Since the Extension provided the
 * initial image it knows the exact layout/position of the buttons and can that way determine what button
 * that was pressed and take action. In this case it could be to start playing a song. The Extension can
 * also choose to update the Widget image so that it reflects the latest state, instead of play button,
 * it might show the pause button and the title of the playing song.
 * </p>

 */

public class Widget {

    /**
     * @hide
     * This class is only intended as a utility class containing declared constants
     *  that will be used by Widget API Extension developers.
     */
    protected Widget() {
    }

    /**
     * Intents sent between Widget Extensions and Accessory Host Applications.
     */
    public interface Intents {

        /**
         * Intent sent by the Accessory Host Application whenever it wants the Widget to start update it's Widget image.
         * Usually this Intent will be sent out when the accessory just starts and is about to show the Widget menu.
         * The Widget image should be updated as soon as possible and after the initial update the Widget image should
         * be updated occasionally until WIDGET_STOP_REFRESH_IMAGE_INTENT is received
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
        String WIDGET_START_REFRESH_IMAGE_INTENT = "com.sonyericsson.extras.aef.widget.START_REFRESH_IMAGE_REQUEST";

        /**
         * Intent sent by the Accessory Host Application whenever it wants the Widget to stop/pause update it's Widget image.
         * The Widget should resume updating its image when WIDGET_START_REFRESH_IMAGE_INTENT is received.
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
        String WIDGET_STOP_REFRESH_IMAGE_INTENT = "com.sonyericsson.extras.aef.widget.STOP_REFRESH_IMAGE_REQUEST";

        /**
         * Intent used by the Widget Extension whenever it wants to update its widget image.
         * The Widget image should be updated occasionally.
         * If the Extension tries to update its Widget image to often, the Host Application will ignore the requests.
         * <p>
         * This intent should be sent with enforced security by supplying the host application permission
         * to sendBroadcast(Intent, String). {@link com.sonyericsson.extras.liveware.aef.registration.Registration#HOSTAPP_PERMISSION}
         * </p>
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_WIDGET_IMAGE_URI}</li>
         * <li>{@link #EXTRA_WIDGET_IMAGE_DATA}</li>
         * <li>{@link #EXTRA_AEA_PACKAGE_NAME}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String WIDGET_IMAGE_UPDATE_INTENT = "com.sonyericsson.extras.aef.widget.IMAGE_UPDATE";

        /**
         * This intent may be used by the Widget Extension as a response to a {@link #WIDGET_ONTOUCH_INTENT}.
         * The widget should send this intent when it does not want to perform any action based on the on touch intent.
         * When receiving this intent the host application is free to stop interaction with this widget and enter a new
         * level or state internally.
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
        String WIDGET_ENTER_NEXT_LEVEL_INTENT = "com.sonyericsson.extras.aef.widget.ENTER_NEW_LEVEL";

        /**
         * Intent sent by the Host Application to the Widget Extension whenever a user interacts with the Widget image.
         * Usually as a result of this Intent the Widget Extension will update its Widget image and take appropriate action
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * <li>{@link #EXTRA_EVENT_TYPE}</li>
         * <li>{@link #EXTRA_EVENT_X_POS}</li>
         * <li>{@link #EXTRA_EVENT_Y_POS}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * </p>
         * @since 1.0
         */
        String WIDGET_ONTOUCH_INTENT = "com.sonyericsson.extras.aef.widget.ONTOUCH";

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
         * The name of the Intent-extra used to identify the URI of the Widget image.
         * If the image is in raw data (e.g. an array of bytes) use {@link #EXTRA_WIDGET_IMAGE_DATA} instead.
         * The image is displayed in the Widget row on the Accessory display.
         * The image can be updated by the Extension at a later stage
         * <P>
         * TYPE: TEXT
         * </P>
         * @since 1.0
         */
        String EXTRA_WIDGET_IMAGE_URI = "widget_image_uri";

        /**
         * The name of the Intent-extra used to identify the Widget image.
         * This Intent-extra should be used if the image is in raw data (e.g. an array of bytes).
         * The image is displayed in the Widget row on the Accessory display.
         * The image can be updated by the Extension at a later stage
         * <P>
         * TYPE: BYTE ARRAY
         * </P>
         * @since 1.0
         */
        String EXTRA_WIDGET_IMAGE_DATA = "widget_image_data";

        /**
         * The name of the Intent-extra used to identify the touch event
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * ALLOWED VALUES:
         * <ul>
         * <li>0: 'short tap'</li>
         * <li>1: 'long tap'</li>
         * </ul>
         * </P>
         * @since 1.0
         */
        String EXTRA_EVENT_TYPE = "widget_event_type";

        /**
         * The name of the Intent-extra used to carry the X coordinate of the touch event
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        String EXTRA_EVENT_X_POS = "widget_event_x_pos";

        /**
         * The name of the Intent-extra used to carry the Y coordinate of the touch event
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        String EXTRA_EVENT_Y_POS = "widget_event_y_pos";

        /**
         * The name of the Intent-extra containing the key set by the extension
         * in {@link ExtensionColumns#EXTENSION_KEY}. This Intent-data is present in
         * all Intents sent by accessory host application, except where
         * {@link android.app.Activity#startActivity(android.content.Intent)}
         * is used. See section <a href="Registration.html#Security">Security</a>
         * for more information
         *
         * @since 1.0
         */
        String EXTRA_EXTENSION_KEY = "extension_key";

        /**
         * The event type is a short tap
         *
         * @since 1.0
         */
        int EVENT_TYPE_SHORT_TAP = 0;

        /**
         * The event type is a long tap
         *
         * @since 1.0
         */
        int EVENT_TYPE_LONG_TAP = 1;
    }
}
