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

package com.sonyericsson.extras.liveware.aef.notification;

import com.sonyericsson.extras.liveware.aef.registration.Registration.ExtensionColumns;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * <h1>Overview</h1>
 * <p>Notification is a part of the Smart Extension API's
 * The Notification engine enables the gathering of event-type data from different
 * sources to one place so that accessory host applications will be able to
 * access this data, instead of getting the data from each individual source.
 * Examples of event-data are activity streams on a social network, new incoming
 * SMS and MMS message notifications, a missed call notification etc.
 * </p>
 * <p>
 * Application developers who wish to have their event-data presented by
 * accessories (granted with the permission to access the Notification
 * engine's data) should input their application's data according to the
 * schema defined by the Notification API.
 * </p>
 * <p>
 * The following diagram shows the position of the Notification API in its
 * operating context.
 * </p>
 * <p>
 * <img src="../../../../../../../images/notification_operating_environment.png"
 * alt="Operating context" border="1" />
 * </p>
 * The Notification API defines and implements an Android ContentProvider that
 * extensions access via the Android ContentResolver API. The ContentProvider
 * implementation is backed by a database implementation. In order for an extension
 * to interact with Notification API, the extension must have used the Registration API
 * and inserted information in the extension table. See Registration &amp; Capabilities API
 * for more information on how to insert a record in the extension table.
 * When needed, Android Intents are sent to the extensions to perform a task.
 * </p>
 * <p>
 * Extensions contribute their data using the set format dictated by the Notification API.
 * As they are standalone Android applications in their own right,
 * extensions may be uninstalled any time, unless they are part of the system
 * image, and may be installed any time during the operation of the device. They
 * may be &#39disabled&#39 as well by the end user via the user interface or by the
 * extension developer; when &#39disabled&#39, data from that extension may not be
 * displayed by the accessory host applications.
 * </p>
 * <p>
 * The accessory host applications provide the functionality to control and
 * present the data that is collected by the notification engine. Depending on the
 * purpose of the application, the notification engine may not be the only data
 * source the application interacts with. Accessory host applications have
 * read-access to the data provided by all the extensions, including the right to
 * update some of the data fields. Due to this reason, access needs to be controlled
 * and restricted so that unwanted information leaks are prevented;
 * this is done through the use of a permission.
 * </p>
 * <p>
 * The purpose of the engine is to provide a central store for event-data from
 * different sources that is of interest to present to the end user. The reasons
 * for choosing such a design are accessory host application performance and data
 * security. Cross-database queries are slow and even slower when there are
 * potentially many databases involved and this will severely impact the
 * performance of accessories and their perceived user experience.
 * It is difficult and practically impossible to allow the &#39correct&#39
 * applications to access the extension-data when there are many databases to
 * interact with. However tempting it may be, the purpose of the engine is NOT to
 * be a central store for all kinds of data, e.g. files, media etc., such that it
 * will be a &#34store room&#34 for all kinds of extension-data.
 * </p>
 * <p>Topics covered here:
 * <ol>
 * <li><a href="#Concepts">Concept Explanations</a>
 * <li><a href="#InterAppCommunication">Inter-application communication</a>
 * <li><a href="#Security">Security</a>
 * <li><a href="#ExtensionLifecycle">Extension Lifecycle</a>
 * <li><a href="#addSource">Adding a Source</a>
 * <li><a href="#configActivity">For the End User to Configure the Extension</a>
 * <li><a href="#getevents">Getting event-data</a>
 * <li><a href="#viewEvent">Showing the Detail View of an Event</a>
 * <li><a href="#ContactLinking">Contact Linking</a>
 * <li><a href="#Images">Handling Images</a>
 * <li><a href="#DataIntegrity">Data Integrity</a>
 * <li><a href="#Performance">Performance</a>
 * </ol>
 * </p>
 * <a name="Concepts"></a>
 * <h3>Concept Explanations</h3>
 * <p>
 * There are three fundamental concepts in the Notification engine's database
 * extension developers are required to understand.
 * </p>
 * <p>
 * The concept of <i>Extension</i> is on Android APK level. The extension table
 * of the registration database contains meta-information about each extension.
 * The purpose of the extension is to provide the necessary data to the notification
 * engine set by the database schema. The source of the extension's event-data may
 * be self-generated, other Android ContentProvider, a Web server or a combination of
 * these. The extension is a standalone application which may have its own GUI that
 * also has the capability to provide data to be shown by a host application
 * using the Notification engine, or it may not have its own GUI and it is
 * completely dependent on the host applications that use the Notification engine to
 * present its data.
 * </p>
 * <p><a name="SourceConcept"></a>
 * <i>Source</i> is a logical abstraction introduced to enable extension developers
 * who want to distinguish the presentation of data connected to different
 * backends but retain the ability to package these in a standalone APK. A use
 * case example is an email aggregator extension that allows the user to connect to
 * different email accounts through the installation of only one Android package
 * file; each email account can be set as a Source or the extension defines only
 * one Source. In the latter scenario, emails from all accounts may be shown
 * in one view instead of separate views. {@link Source} stores attribute
 * information about a Source. The accessory host application may use this
 * information to filter event-data by <i>Source</i> or provide
 * configuration options on the user interface to filter event-data by
 * <i>Source</i>.Extension developers who wish to have the accessory host
 * application display events from different <i>Sources</i>
 * clearly should add {@link Source} information. There is a limit set on
 * the number of <i>Sources</i> that is linked to a <i>Extension</i>. If the limit
 * is reached, an exception will be thrown. A <i>Source</i> always has to be
 * linked to an <i>Extension</i> .
 * </p>
 * <p>
 * An <i>Event</i> is a representation of a notification that may be noteworthy
 * to present to the end user. Examples of events are incoming SMS
 * message notifications, a missed call notification, updates from friends on a
 * social network etc. {@link Event} is used to store events provided
 * by the extensions. The accessory host application typically uses the information
 * in this table to present the data. An <i>Event</i> is always connected
 * to a <i>Source</i> but a </i>Source</i> may not always have to have an
 * <i>Event</i>. There is a limitation on the number of events from a <i>Source</i>
 * stored in {@link Event}; when the limit is reached, events will be automatically
 * removed.
 * </p>
 * <a name="InterAppCommunication"></a>
 * <h3>Inter-application communication</h3>
 * <p>
 * Extensions only use the Android ContentResolver API to communicate with the
 * Notification engine's ContentProvider. For the possibility to react to user
 * input, extensions should implement at least an Android BroadcastReceiver to
 * catch Intents sent from the accessory host
 * applications. Also see the Control API, Widget API and Sensor API documentation.
 * </p>
 * <p>
 * The list and descriptions of each BroadcastIntent extensions could listen to
 * are found in {@link Intents} together with the Intent-extra data
 * that are sent in each Intent.
 * </p>
 * <a name="Security"></a>
 * <h3>Security</h3>
 * <p>
 * In order to use the Notification API, an extension must first add information
 * in the extension table. This require a specific permission. See the documentation
 * of the Registration API for more information
 *</p>
 *<p>
 * A extension only has access to its own data: it is able to insert, query, update
 * and remove its data that is stored on the Notification engine. When an
 * application registered as a extension is uninstalled from the Android system,
 * the associated data that is stored in the engine is automatically removed
 * by the Notification engine's implementation.
 * </p>
 * <p>
 * If a extension developer wishes to allow another application to access its data
 * on the engine through the use of <i>sharedUserId</i>, it is possible to do so
 * though not recommended. When these two or more applications are registered as
 * extensions, the Notification engine's security mechanism will only treat these
 * extensions as one extension and the extension developer is responsible for any leakage
 * or misuse of its information stored in the Notification engine's content
 * provider.
 * </p>
 * <p>
 * Extension developers are free to sign their applications with their own
 * certificate.
 * </p>
 *  <a name="ExtensionLifecycle"></a>
 * <h3>Extension Lifecycle</h3>
 * <p>
 * Before an application can take full advantage of the Notification engine,
 * it must tell the engine that it exists and for the engine to have a record
 * of this application's attributes. This process is known as registration and
 * after a successful registration, the application is referred as an extension in
 * the Notification engine's context. In practice, the process of registering
 * a extension involves the application inserting some data about itself using the
 * Registration API.
 * </p>
 * <p>
 * From the Notification engine's perspective, the life cycle of a extension starts
 * from the time a successful registration takes place to the time the Android
 * system uninstalls the application or the extension deregisters itself from the
 * Notification engine. During this time, the extension is free to access the Event
 * Stream engine and receive Intents from it.
 * </p>
 *<a name="addSource"></a>
 * <h3>Adding a Source</h3>
 * <p>
 * As explained in an earlier section, <i>Source</i> is a logical abstraction
 * introduced to easily enable the presentation of event-data originating from
 * different backend. It is up to the extension developer to decide how to segment
 * the event-data contributed by that extension, but all event-data must be connected
 * to a Source, or else the insert operation for event-data will fail.
 * </p>
 * <p>
 * Setting the Source information in the ContentProvider should take place after
 * the extension is successfully registered and before inserting event-data.
 * <pre class="prettyprint">
 *        ContentValues values = new ContentValues();
 *        Builder iconUriBuilder = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
 *                                        .authority(getPackageName())
 *                                        .appendPath(Integer.toString(R.drawable.icon));
 *
 *        values.put("name", "RSS news feed");
 *        values.put("enabled", "1");
 *        values.put("icon_uri", iconUriBuilder.toString());
 *
 *        uri = cr.insert(Uri.parse("content://com.sonyericsson.extras.liveware.aef.notification/source"),
 *                values);
 * </pre>
 * </p>
 * <a name="getevents"></a>
 * <h3>Getting event-data</h3>
 * Your extension may automatically retrieve event-data periodically from the source where the
 * event-data is generated or rely on an Intent sent by the host application to trigger
 * the retrieval.
 * </p>
 * <p>
 * The name of the Intent is {@link Intents#REFRESH_REQUEST_INTENT}.
 * Define your BroadcastReceiver to receive this Intent if you wish to rely on this to
 * trigger the event-data retrieval. Do not rely on the interval when this Intent is sent
 * as it may be arbitrary and completely dependent on the implementation of the Intent sender.
 * However it is always sent when a host application is started and an extension can use this to start
 * collecting data and continue to be be active.
 * </p>
 * <p>
 * When your extension has event-data to insert to the Notification engine's
 * ContentProvider, it may use
 * {@link android.content.ContentResolver#insert(Uri, android.content.ContentValues)}
 * or {@link android.content.ContentResolver#bulkInsert(Uri, android.content.ContentValues[])}.
 * The latter method is recommended for performance reasons to use when there are many
 * rows of event-data to insert.
 * <pre class="prettyprint">
 *        ContentResolver cr = getContentResolver();
 *        ContentValues[] valueArray = new ContentValues[count];
 *        &lt fill valueArray with data &gt
 *        cr.bulkInsert(Uri.parse("content://com.sonyericsson.extras.liveware.aef.notification.event,
 *             valueArray);
 * </pre>
 * </p>
 * <p>
 * When retrieving event-data from the data source, it is highly recommended your
 * extension updates and retrieves other relevant data from the same data source,
 * e.g. the user's latest status update, at around the same time. This is to
 * minimize network signaling traffic and latency. It is also costly for battery
 * consumption if there is too frequent network signaling activity.
 * </p>
 * <p>
 * If you need to synchronize periodically with a server in a network, consider
 * using the {@link android.app.AlarmManager} to achieve optimal power consumption.
 * A tutorial explaining how you can implement this is posted at the
 * <a href="http://blogs.sonyericsson.com/developerworld/2010/08/23/android-tutorial-reducing-power-consumption-of-connected-apps/">
 * Sony Ericsson Developer blog</a>.
 * </p>
 * <a name="viewEvent"></a>
 * <h3>Showing the Detail View of an Event</h3>
 * <p>
 * The event-data supplied by your plug-in in {@link Event} may be a snapshot of
 * the information and the user has limited possibilities to interact with the
 * information presented by the accessory host application. The user may wish to
 * see all details related to that event and react to it, e.g. mark it as a
 * favorite, reply, watch the video etc. If your extension offers the user the
 * opportunity to interact with the event in your application or on a website,
 * listen for the {@link Intents#VIEW_EVENT_INTENT} Intent. This Intent
 * is sent by the accessory host application when the user performs an action
 * signaling the intention to view the event details. The Intent contains Intent-
 * data about the specific event-data that will enable your extension to launch the
 * detail view of that event.
 * </p>
 * <p>
 * <a name="Images"></a>
 * <h3>Handling Images</h3>
 * <p>
 * The location of images is represented as a string. Images may be stored
 * locally on the device or on the SD card.
 * </p>
 * <p>
 * The following URI schemes are supported:
 * </p>
 * <ul>
 * <li>android.resource://
 * </li>
 * <li>
 * content://
 * </li>
  * </ul>
 * <p>
 * * <a name="DataIntegrity"></a>
 * <h3>Data Integrity</h3>
 * <p>
 * In order to have a consistent database, the Notification Engine will enforce
 * database data integrity upon any data inserted or updated by extensions. This is
 * especially true for foreign keys. As an example, <i>sourceId</i> for an
 * <i>Event</i> is a foreign key to the column <i>_id</i> in the <i>Source</i>
 * table, thus the <i>source_id</i> must have a valid reference to a row in the
 * <i>Source</i> table which in turn is associated with a plug-in. If values for
 * the stated mandatory columns are not provided, SQLExceptions with constraint
 * failures will be thrown.
 * </p>
 * <a name="Performance"></a>
 * <h3>Performance</h3>
 * <p>
 * For best performance, it is recommended the extension developer use
 * {@link android.content.ContentResolver#bulkInsert(Uri, android.content.ContentValues[])}
 * or {@link android.content.ContentResolver#applyBatch(String, java.util.ArrayList)}
 * when doing inserts or updates to the Event Stream&#39s ContentProvider.
 * </p>
 */
public class Notification {

    /**
     * @hide
     * This class is only intended as a utility class containing declared constants
     *  that will be used by notification extension developers.
     */
    protected Notification(){
    }

    /**
     * Authority for the Notification provider.
     *
     * @since 1.0
     */
    public static final String AUTHORITY = "com.sonyericsson.extras.liveware.aef.notification";

    /**
     * Base URI for the Notification provider.
     *
     * @since 1.0
     */
    protected static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

    /**
     * Broadcast Intents sent to extensions by the host application.
     */
    public interface Intents {

        /**
         * Intent sent by the host application to the relevant extension
         * to display all details related to the event
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         * <li>{@link #EXTRA_EVENT_ID}</li>
         * <li>{@link #EXTRA_SOURCE_ID}</li>
         * <li>{@link #EXTRA_ACTION}</li>
         * <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * <li>{@link #EXTRA_AHA_PACKAGE_NAME}</li>
         * </ul>
         * @since 1.0
         */
        static final String VIEW_EVENT_INTENT = "com.sonyericsson.extras.liveware.aef.notification.VIEW_EVENT_DETAIL";

        /**
         * Intent sent by the host application when an update of available data is needed
         * <p>
         * Intent-extra data:
         * </p>
         * <ul>
         <li>{@link #EXTRA_EXTENSION_KEY}</li>
         * </ul>
         * @since 1.0
         */
        static final String REFRESH_REQUEST_INTENT = "com.sonyericsson.extras.liveware.aef.notification.REFRESH_REQUEST";

        /**
         * The name of the Intent-extra used to identify the event
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        static final String EXTRA_EVENT_ID = "event_id";

        /**
         * The name of the Intent-extra used to identify which Source an
         * Event is associated with
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * @since 1.0
         */
        static final String EXTRA_SOURCE_ID = "source_id";

        /**
         * The action requested by the user.
         * This is a string indicating a user action
         * corresponding to one of the three actions that are
         * defined in the source table
         * action_1 {@link SourceColumns#ACTION_1}
         * action_2 {@link SourceColumns#ACTION_2}
         * action_3 {@link SourceColumns#ACTION_3}
         * <P>
         * ALLOWED VALUES:
         * <ul>
         * <li>{@link #EXTENSION_ACTION_1}</li>
         * <li>{@link #EXTENSION_ACTION_2}</li>
         * <li>{@link #EXTENSION_ACTION_3}</li>
         * </P>
         *
         * @since 1.0
         */
        static final String EXTRA_ACTION = "action";

        /**
         * The name of the Intent-extra containing the key set by the extension
         * in {@link ExtensionColumns#EXTENSION_KEY}. This Intent-data is present in
         * all Intents sent by accessory host application, except where
         * {@link android.app.Activity#startActivity(android.content.Intent)}
         * is used. See section <a href="Registration.html#Security">Security</a>
         * for more information
         * <P>
         * TYPE: TEXT
         * </P>
         * @since 1.0
         */
        static final String EXTRA_EXTENSION_KEY = "extension_key";

        /**
         * The name of the Intent-extra used to identify the Host Application.
         * The Host Application will send its package name
         * <P>
         * TYPE: TEXT
         * </P>
         * @since 1.0
         */
        static final String EXTRA_AHA_PACKAGE_NAME = "aha_package_name";

        /**
         * Constant defining an action requested by the host application
         * using the {@link Intents#VIEW_EVENT_INTENT} intent.
         * The action corresponds the action that is
         * defined in the source table action_1 {@link SourceColumns#ACTION_1}
         * <P>
         * TYPE: TEXT
         * </P>
         * @since 1.0
         */
        static final String EXTENSION_ACTION_1 = "action_1";

        /**
         * Constant defining an action requested by the host application
         * using the {@link Intents#VIEW_EVENT_INTENT} intent.
         * The action corresponds the action that is
         * defined in the source table action_2 {@link SourceColumns#ACTION_2}
         * <P>
         * TYPE: TEXT
         * </P>
         * @since 1.0
         */
        static final String EXTENSION_ACTION_2 = "action_2";

        /**
         * Constant defining an action requested by the host application
         * using the {@link Intents#VIEW_EVENT_INTENT} intent.
         * The action corresponds the action that is
         * defined in the source table action_3 {@link SourceColumns#ACTION_3}
         * <P>
         * TYPE: TEXT
         * </P>
         * @since 1.0
         */
        static final String EXTENSION_ACTION_3 = "action_3";
    }

    /**
     * Definitions used for interacting with the Extension-table.
     *
     */
    public interface Source {

        /**
         * The source table name
         */
        static final String TABLE_NAME = "source";

        /**
         * Data row MIME type
         */
        static final String MIME_TYPE = "aef-source";

        /**
         * Path segment
         */
        static final String SOURCES_PATH = "source";

        /**
         * Content URI
         */
        static final Uri URI = Uri.withAppendedPath(BASE_URI, SOURCES_PATH);
    }

    /**
     * Column-definitions for the Source table.
     */
    public interface SourceColumns extends BaseColumns {

        /**
         * Displayable name of the source
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
        static final String NAME = "name";

        /**
         * Each Source can use up to 2 icons with different
         * sizes and one monochrome
         * This is the URI of the largest icon (30x30 pixels)
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
        static final String ICON_URI_1 = "iconUri1";

        /**
         * Each Source can use up to 2 icons with different
         * sizes and one monochrome
         * This is the URI of the second largest icon (18x18 pixels)
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
        static final String ICON_URI_2 = "iconUri2";

        /**
         * Each Source can use up to 2 icons with different
         * sizes and one monochrome
         * This is the URI of the monochrome icon (18x18 pixels)
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
        static final String ICON_URI_BLACK_WHITE = "iconUriBlackWhite";

        /**
         * Indicates if the source is enabled
         *
         * <P>
         * TYPE: BOOLEAN
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         * </P>
         *
         * @since 1.0
         */
        static final String ENABLED = "enabled";

        /**
         * Action supported by the extension.
         * The action is defined by the extension and supported
         * for this source.
         * Actions are sent to the extension from host applications
         * using the {@link Intents#VIEW_EVENT_INTENT} intent
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
        static final String ACTION_1 = "action_1";

        /**
         * Action supported by the extension.
         * The action is defined by the extension and supported
         * for this source.
         * Actions are sent to the extension from host applications
         * using the {@link Intents#VIEW_EVENT_INTENT} intent
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
        static final String ACTION_2 = "action_2";

        /**
         * Action supported by the extension.
         * The action is defined by the extension and supported
         * for this source..
         * Actions are sent to the extension from host applications
         * using the {@link Intents#VIEW_EVENT_INTENT} intent
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
        static final String ACTION_3 = "action_3";

        /**
         * The time (in milliseconds since January 1, 1970 00:00:00 UTC UNIX
         * EPOCH) when an event linked to this source was created.
         * Shall be stored as GMT+0 time
         *
         * <P>
         * TYPE: INTEGER (long)
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         * </P>
         *
         * @since 1.0
         */
        static final String UPDATE_TIME = "updateTime";

        /**
         * Text to speech specific text.
         * The text in this column is used in combination
         * with the events of the source to create speech events.
         * The text in this column is read out before the events
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
        static final String TEXT_TO_SPEECH = "textToSpeech";

        /**
         * Extension specific identifier of the source
         * It is up to the extension to define this identifier
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
        static final String EXTENSION_SPECIFIC_ID = "extension_specific_id";

        /**
         * The package name of a plug-in.
         * If an extension supports shared user id, the package name
         * must be specified
         * <P>
         * TYPE: TEXT
         * </P>
         * * <P>
         * PRESENCE: OPTIONAL (REQUIRED if shared user id is used by extension)
         * </P>
         * @since 1.0
         */
        static final String PACKAGE_NAME = "packageName";
    }

    /**
     * Definitions used for interacting with the event table.
     *
     */
    public interface Event {

        /**
         * The event table name
         */
        static final String TABLE_NAME = "event";

        /**
         * Data row MIME type
         */
        static final String MIME_TYPE = "aef-event";

        /**
         * Path segment
         */
        static final String EVENTS_PATH = "event";

        /**
         * Path segment
         */
        static final String EVENT_READ_STATUS_PATH = "read_status";

        /**
         * Content URI
         */
        static final Uri URI = Uri.withAppendedPath(BASE_URI, EVENTS_PATH);

        /**
         * Content URI used to observe changes in EVENT_READ_STATUS
         */
        static final Uri READ_STATUS_URI = Uri.withAppendedPath(BASE_URI, EVENT_READ_STATUS_PATH);
    }

    /**
     * Column-definitions for the event table.
     */
    public interface EventColumns extends BaseColumns {

        /**
         * The ID of the host source corresponding to
         * this event
         *
         * <P>
         * TYPE: INTEGER (long)
         * </P>
         * <P>
         * PRESENCE: MANDATORY
         *  </P>
         *
         * @since 1.0
         */
        static final String SOURCE_ID = "sourceId";

        /**
         * Short text describing the title for event linked with this data row.
         * This can be the phone number, username, email address etc
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
        static final String TITLE = "title";

        /**
         * Content URI to an image linked with the event at this data row
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         *
         * PRESENCE: OPTIONAL
         * </P>
         *
         * @since 1.0
         */
        static final String IMAGE_URI = "imageUri";

        /**
         * The time (in milliseconds since January 1, 1970 00:00:00 UTC UNIX
         * EPOCH) when the content linked with this data row was published on
         * the source. Shall be stored as GMT+0 time
         *
         * <P>
         * TYPE: INTEGER (long)
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         * </P>
         *
         * @since 1.0
         */
        static final String PUBLISHED_TIME = "publishedTime";

        /**
         * Whether the event linked with this data row is specifically directed
         * to the user ("me") or concerns the user ("me"), e.g. received SMS,
         * Facebook private message to the logged-in user, Facebook private
         * message from the logged-in user, @reply Tweets from the logged-in
         * user, user ("me") is tagged in a photo etc
         *
         * <P>
         * TYPE: INTEGER (int)
         * </P>
         * <P>
         * ALLOWED VALUES:
         * <ul>
         * <li>0: 'not personal'</li>
         * <li>1: 'personal'</li>
         * </ul>
         * </P>
         * <P>
         * PRESENCE: REQUIRED
         * </P>
         *
         * @since 1.0
         */
        static final String PERSONAL = "personal";

        /**
         * Message associated with this event
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
        static final String MESSAGE = "message";

        /**
         * Geo data associated with this event
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
        static final String GEO_DATA = "geoData";

        /**
         * Indicates if the event has been read by the user
         *
         * <P>
         * TYPE: BOOLEAN
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         * </P>
         *
         * @since 1.0
         */
        static final String EVENT_READ_STATUS = "readStatus";

        /**
         * The time (in milliseconds since January 1, 1970 00:00:00 UTC UNIX
         * EPOCH) when this row was created.
         * The time stamp is set automatically
         *
         * <P>
         * TYPE: INTEGER (long)
         * </P>
         * <P>
         * PRESENCE: The time stamp is set automatically
         * </P>
         *
         * @since 1.0
         */
        static final String TIME_STAMP = "timeStamp";

        /**
         * Displayable name of the user linked with this data row, e.g. full
         * name
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         * </P>
         *
         * @since 1
         */
        static final String DISPLAY_NAME = "display_name";

        /**
         * URI to the profile image of the user linked with this data row
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         * </P>
         *
         * @since 1
         */
        static final String PROFILE_IMAGE_URI = "profile_image_uri";

        /**
         * A reference to the contacts content provider.
         * The reference is a URI to a {@link android.provider.ContactsContract.RawContacts}
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         * </P>
         *
         * @since 1
         */
        static final String CONTACTS_REFERENCE = "contacts_reference";

        /**
         * Generic data column for use by the plug-in to store information that
         * may be used to identify the friend that is at this data row, in its
         * domain. See section <a href="EventStream.html#ContactLinking">
         * Contact Linking</a> for more information
         *
         * <P>
         * TYPE: TEXT
         * </P>
         * <P>
         * PRESENCE: OPTIONAL
         * </P>
         *
         * @since 1
         */
        static final String FRIEND_KEY = "friend_key";
    }

    /**
     * Definitions used for interacting with the source event join query.
     *
     */
    public interface SourceEvent {
        /**
         * Data row MIME type
         */
        static final String MIME_TYPE = "aef-source-event";

        /**
         * Path segment
         */
        static final String SOURCES_EVENTS_PATH = "source_event";

        /**
         * Content URI
         */
        static final Uri URI = Uri.withAppendedPath(BASE_URI, SOURCES_EVENTS_PATH);
    }


    /**
     * Column-definitions for the source event join query.
     */
    public interface SourceEventColumns extends SourceColumns, EventColumns {
        /**
         * The ID of the event in the source event join query.
         *
         * <P>
         * TYPE: INTEGER (long)
         * </P>
         */
        static final String EVENT_ID = "eventId";
    }
 }
