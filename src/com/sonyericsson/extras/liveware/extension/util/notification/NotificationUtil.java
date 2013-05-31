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

package com.sonyericsson.extras.liveware.extension.util.notification;

import com.sonyericsson.extras.liveware.aef.notification.Notification;
import com.sonyericsson.extras.liveware.extension.util.Dbg;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.util.ArrayList;

public class NotificationUtil {

    /**
     * Invalid id
     */
    public static final int INVALID_ID = -1;

    /**
     * Event Id to use in projection, selection and sortOrder when quering
     * {@link Notification.SourceEvent#URI}
     *
     * @see #queryEvents(Context, String[], String, String[], String)
     */
    public static final String EVENT_ID = Notification.Event.TABLE_NAME + "" + BaseColumns._ID;

    /** This class can not be instantiated */
    private NotificationUtil() {
    }

    /**
     * Get source id associated with extension specific id of the source.
     *
     * @param context Context with permissions to access Notification db
     * @param extensionSpecificId Extension specific identifier of the source.
     * @return Source id, INVALID_ID if not found
     */
    public static long getSourceId(Context context, String extensionSpecificId) {
        long sourceId = INVALID_ID;
        Cursor cursor = null;

        String whereClause = null;
        if (extensionSpecificId != null) {
            whereClause = Notification.SourceColumns.EXTENSION_SPECIFIC_ID + " = '"
                    + extensionSpecificId + "'";
        }

        try {
            cursor = querySources(context, new String[] {
                    Notification.SourceColumns._ID,
                    Notification.SourceColumns.EXTENSION_SPECIFIC_ID
            }, whereClause, null, null);
            if (cursor == null) {
                return INVALID_ID;
            }

            if (cursor.moveToFirst()) {
                sourceId = cursor.getLong(cursor.getColumnIndex(Notification.SourceColumns._ID));
            }
        } catch (SQLException exception) {
            return INVALID_ID;
        } catch (SecurityException exception) {
            return INVALID_ID;
        } catch (IllegalArgumentException exception) {
            return INVALID_ID;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return sourceId;
    }

    /**
     * Get extension specific id.
     *
     * @param context Context with permissions to access Notification db
     * @param sourceId The source id.
     * @return Extension specific id, null if not found
     */
    public static String getExtensionSpecificId(Context context, long sourceId) {
        String extensionSpecificId = null;
        Cursor cursor = null;

        try {
            cursor = querySources(context, new String[] {
                    Notification.SourceColumns._ID,
                    Notification.SourceColumns.EXTENSION_SPECIFIC_ID
            }, Notification.SourceColumns._ID + " = " + sourceId, null, null);
            if (cursor == null) {
                return null;
            }

            if (cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(Notification.SourceColumns.EXTENSION_SPECIFIC_ID);
                extensionSpecificId = cursor.getString(index);
            }
        } catch (SQLException exception) {
            return null;
        } catch (SecurityException exception) {
            return null;
        } catch (IllegalArgumentException exception) {
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return extensionSpecificId;
    }

    /**
     * Delete all events associated with a extension specific id
     *
     * @param context The context.
     * @param extensionSpecificId The extension specific id
     * @return The number of events that was deleted or {@link #INVALID_ID} on
     *         failure
     */
    public static int deleteAllEvents(Context context, String extensionSpecificId) {
        int result = 0;

        long sourceId = getSourceId(context, extensionSpecificId);
        String where = Notification.EventColumns.SOURCE_ID + " = " + sourceId;
        try {
            result = deleteEvents(context, where, null);
        } catch (SQLException exception) {
            result = INVALID_ID;
        } catch (SecurityException exception) {
            result = INVALID_ID;
        } catch (IllegalArgumentException exception) {
            result = INVALID_ID;
        }

        return result;
    }

    /**
     * Delete all events associated with this extension
     *
     * @param context The context.
     * @return The number of events that was deleted or {@link #INVALID_ID} on
     *         failure
     */
    public static int deleteAllEvents(Context context) {
        int result = 0;
        try {
            result = deleteEvents(context, null, null);
        } catch (SQLException exception) {
            result = INVALID_ID;
        } catch (SecurityException exception) {
            result = INVALID_ID;
        } catch (IllegalArgumentException exception) {
            result = INVALID_ID;
        }

        return result;
    }

    /**
     * Mark all events as read
     *
     * @param context
     * @return Number of updated rows in event table, INVALID_ID on failure
     */
    public static int markAllEventsAsRead(Context context) {
        int nbrUpdated = 0;
        try {
            ContentValues cv = new ContentValues();
            cv.put(Notification.EventColumns.EVENT_READ_STATUS, true);
            nbrUpdated = updateEvents(context, cv, null, null);
        } catch (SQLException exception) {
            nbrUpdated = INVALID_ID;
        } catch (SecurityException exception) {
            nbrUpdated = INVALID_ID;
        } catch (IllegalArgumentException exception) {
            nbrUpdated = INVALID_ID;
        }
        return nbrUpdated;
    }

    /**
     * Add new event to Event table
     *
     * @param context Context with permissions to access Notification db
     * @param eventValues A reference to the notification
     *            com.sonyericsson.extras
     *            .liveware.aef.notification.Notification.
     * @return Uri to the created event
     */
    public static Uri addEvent(final Context context, final ContentValues eventValues) {
        try {
            return context.getContentResolver().insert(Notification.Event.URI, eventValues);
        } catch (SQLException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to add event", e);
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to add event", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to add event", e);
            }
        }
        return null;
    }

    /**
     * Get source ids associated with extension.
     *
     * @param context Context with permissions to access Notification db
     * @return Source ids, empty array if not found
     */
    public static ArrayList<Integer> getSourceIds(final Context context, boolean enabled) {
        ArrayList<Integer> sourceIds = new ArrayList<Integer>();
        Cursor cursor = null;
        String where = Notification.SourceColumns.ENABLED + "=" + (enabled ? "1" : "0");
        try {
            cursor = querySources(context, new String[] {
                Notification.SourceColumns._ID
            }, where, null, null);
            while (cursor != null && cursor.moveToNext()) {
                sourceIds.add(cursor.getInt(cursor.getColumnIndex(Notification.SourceColumns._ID)));
            }
        } catch (SQLException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query source", e);
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query source", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query source", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return sourceIds;
    }

    /**
     * Get Extension Specific Ids associated with an extension.
     *
     * @param context Context with permissions to access Notification db
     * @return Extension Specific Ids ids for enabled sources, empty array if
     *         not found
     */
    public static ArrayList<String> getExtensionSpecificIds(final Context context) {
        ArrayList<String> extensionSpecificIds = new ArrayList<String>();
        Cursor cursor = null;
        try {
            cursor = querySources(context, new String[] {
                Notification.SourceColumns.EXTENSION_SPECIFIC_ID
            }, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    extensionSpecificIds.add(cursor.getString(cursor
                            .getColumnIndex(Notification.SourceColumns.EXTENSION_SPECIFIC_ID)));
                } while (cursor.moveToNext());
            }
        } catch (SQLException exception) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query source", exception);
            }
        } catch (SecurityException exception) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query source", exception);
            }
        } catch (IllegalArgumentException exception) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query source", exception);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return extensionSpecificIds;
    }

    /**
     * Get all source ids associated with extension
     *
     * @param context Context with permissions to access Notification db
     * @return All source ids, empty array if not found
     */
    public static ArrayList<Long> getSourceIds(final Context context) {
        ArrayList<Long> sourceIds = new ArrayList<Long>();
        Cursor cursor = null;
        try {
            cursor = querySources(context, new String[] {
                Notification.SourceColumns._ID
            }, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    sourceIds.add(cursor.getLong(cursor
                            .getColumnIndex(Notification.SourceColumns._ID)));
                } while (cursor.moveToNext());
            }
        } catch (SQLException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query sources", e);
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query sources", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query sources", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return sourceIds;
    }

    /**
     * Get friend key associated with event id
     *
     * @param context Context with permissions to access Notification db
     * @param eventId Id of event
     * @return Event title or null if not found.
     */
    public static String getFriendKey(final Context context, long eventId) {
        Cursor cursor = null;
        String freindKey = null;
        try {
            cursor = queryEvents(context, new String[] {
                Notification.EventColumns.FRIEND_KEY
            }, EVENT_ID + " = " + eventId, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int titleIndex = cursor.getColumnIndex(Notification.EventColumns.FRIEND_KEY);
                freindKey = cursor.getString(titleIndex);
            }
        } catch (SQLException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query events", e);
            }
        } catch (SecurityException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query events", e);
            }
        } catch (IllegalArgumentException e) {
            if (Dbg.DEBUG) {
                Dbg.w("Failed to query events", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return freindKey;
    }

    /**
     * Query {@link Notification.SourceEvent#URI}, limit query to affect events
     * in this extension only. Returned cursor is a join between
     * {@link Notification.Source#URI} and {@link Notification.Event#URI}.
     * Queries executed on {@link Notification.Event#URI} can also be executed
     * by this method if all references to {@link Notification.EventColumns#_ID}
     * are replaced with event._id
     * <p>
     *
     * This method is mainly aimed for extensions that shares user id and
     * process in which case they can access and modify data that belongs to
     * other extensions.
     * <p>
     *
     * Note that no runtime exceptions, such as {@link SecurityException} and
     * {@link SQLException}, will be handled by this method. Exceptions shall
     * instead be handled in the calling method if needed.
     * <p>
     *
     * This method is also convenient when querying events that belongs to a
     * specific {@link Notification.SourceColumns#EXTENSION_SPECIFIC_ID} since
     * no extra lookup of the source id is needed.
     *
     *
     * @param context The context
     * @param projection A list of which columns to return. Passing null will
     *            return all columns, which is inefficient. The projection can
     *            contain all columns in {@link Notification.EventColumns} and
     *            all columns in {@link Notification.SourceColumns} except for
     *            {@link Notification.SourceColumns#_ID}
     * @param selection A filter declaring which rows to return, formatted as an
     *            SQL WHERE clause (excluding the WHERE itself). Passing null
     *            will return all rows for the given URI.
     * @param selectionArgs Arguments to where String
     * @param sortOrder How to order the rows, formatted as an SQL ORDER BY
     *            clause (excluding the ORDER BY itself). Passing null will use
     *            the default sort order, which may be unordered.
     * @return A Cursor object, which is positioned before the first entry, or
     *         null
     */
    public static Cursor queryEvents(Context context, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        String extensionWhere = getSourcesWhere(context);
        if (!TextUtils.isEmpty(selection)) {
            extensionWhere += " AND (" + selection + ")";
        }
        return context.getContentResolver().query(Notification.SourceEvent.URI, projection,
                extensionWhere, selectionArgs, sortOrder);
    }

    /**
     * Query {@link Notification.SourceEvent#URI}, limit query to affect events
     * in this extension only and sources that are enabled. Returned cursor is a
     * join between {@link Notification.Source#URI} and
     * {@link Notification.Event#URI}. Queries executed on
     * {@link Notification.Event#URI} can also be executed by this method if all
     * references to {@link Notification.EventColumns#_ID} are replaced with
     * event._id
     * <p>
     *
     * This method is mainly aimed for extensions that shares user id and
     * process in which case they can access and modify data that belongs to
     * other extensions.
     * <p>
     *
     * Note that no runtime exceptions, such as {@link SecurityException} and
     * {@link SQLException}, will be handled by this method. Exceptions shall
     * instead be handled in the calling method if needed.
     * <p>
     *
     * This method is also convenient when querying events that belongs to a
     * specific {@link Notification.SourceColumns#EXTENSION_SPECIFIC_ID} since
     * no extra lookup of the source id is needed.
     *
     *
     * @param context The context
     * @param projection A list of which columns to return. Passing null will
     *            return all columns, which is inefficient. The projection can
     *            contain all columns in {@link Notification.EventColumns} and
     *            all columns in {@link Notification.SourceColumns} except for
     *            {@link Notification.SourceColumns#_ID}
     * @param selection A filter declaring which rows to return, formatted as an
     *            SQL WHERE clause (excluding the WHERE itself). Passing null
     *            will return all rows for the given URI.
     * @param selectionArgs Arguments to where String
     * @param sortOrder How to order the rows, formatted as an SQL ORDER BY
     *            clause (excluding the ORDER BY itself). Passing null will use
     *            the default sort order, which may be unordered.
     * @return A Cursor object, which is positioned before the first entry, or
     *         null
     */
    public static Cursor queryEventsFromEnabledSources(Context context, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        String where = Notification.SourceEventColumns.ENABLED + " = 1";
        if (!TextUtils.isEmpty(selection)) {
            where += " AND (" + selection + ")";
        }
        return queryEvents(context, projection, where, selectionArgs, sortOrder);
    }

    /**
     * Update events, limit update to affect events in this extension only.
     * <p>
     *
     * This method is mainly aimed for extensions that shares user id and
     * process in which case they can access and modify data that belongs to
     * other extensions.
     * <p>
     *
     * Note that no runtime exceptions, such as {@link SecurityException} and
     * {@link SQLException}, will be handled by this method. Exceptions shall
     * instead be handled in the calling method if needed.
     *
     * @param context The context
     * @param values The new field values. The key is the column name for the
     *            field. A null value will remove an existing field value.
     * @param where A filter to apply to rows before updating, formatted as an
     *            SQL WHERE clause (excluding the WHERE itself).
     * @param selectionArgs Arguments to where String
     * @return The number of rows updated
     */
    public static int updateEvents(Context context, ContentValues values, String where,
            String[] selectionArgs) {
        String extensionWhere = getEventsWhere(context);
        if (!TextUtils.isEmpty(where)) {
            extensionWhere += " AND (" + where + ")";
        }
        return context.getContentResolver().update(Notification.Event.URI, values, extensionWhere,
                selectionArgs);
    }

    /**
     * Delete events, limit delete to affect events in this extension only.
     * <p>
     *
     * This method is mainly aimed for extensions that shares user id and
     * process in which case they can access and modify data that belongs to
     * other extensions.
     * <p>
     *
     * Note that no runtime exceptions, such as {@link SecurityException} and
     * {@link SQLException}, will be handled by this method. Exceptions shall
     * instead be handled in the calling method if needed.
     *
     * @param context The context
     * @param where A filter to apply to rows before deleting, formatted as an
     *            SQL WHERE clause (excluding the WHERE itself).
     * @param selectionArgs Arguments to where String
     * @return The number of rows deleted
     */
    public static int deleteEvents(Context context, String where, String[] selectionArgs) {
        String extensionWhere = getEventsWhere(context);
        if (!TextUtils.isEmpty(where)) {
            extensionWhere += " AND (" + where + ")";
        }
        return context.getContentResolver().delete(Notification.Event.URI, extensionWhere,
                selectionArgs);

    }

    /**
     * Get where string that limits a queries to {@link Notification.Event#URI}
     * to affect events that belongs to this extension only
     *
     * @param context The context
     * @return The where string:
     *         <p>
     *         Template: sourceId IN ( sourceId1, sourceId2, ... )
     */
    public static String getEventsWhere(Context context) {
        ArrayList<Long> sourceIds = getSourceIds(context);
        if (sourceIds.size() == 0) {
            return "0";
        }
        // Build where clause
        StringBuilder whereBuilder = new StringBuilder();
        whereBuilder.append(Notification.EventColumns.SOURCE_ID + " IN ( ");
        for (int i = 0; i < sourceIds.size() - 1; i++) {
            whereBuilder.append(sourceIds.get(i) + ", ");
        }
        whereBuilder.append(sourceIds.get(sourceIds.size() - 1));
        whereBuilder.append(" )");
        return whereBuilder.toString();
    }

    /**
     * Query sources, limit scope to sources in this extension
     * <p>
     *
     * This method is mainly aimed for extensions that shares user id and
     * process in which case they can access and modify data that belongs to
     * other extensions.
     * <p>
     *
     * Note that no runtime exceptions, such as {@link SecurityException} and
     * {@link SQLException}, will be handled by this method. Exceptions shall
     * instead be handled in the calling method if needed.
     *
     * @param context The context
     * @param projection A list of which columns to return. Passing null will
     *            return all columns, which is inefficient.
     * @param selection A filter declaring which rows to return, formatted as an
     *            SQL WHERE clause (excluding the WHERE itself). Passing null
     *            will return all rows for the given URI.
     * @param selectionArgs Arguments to where String
     * @param sortOrder How to order the rows, formatted as an SQL ORDER BY
     *            clause (excluding the ORDER BY itself). Passing null will use
     *            the default sort order, which may be unordered.
     * @return A Cursor object, which is positioned before the first entry, or
     *         null
     */
    public static Cursor querySources(Context context, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        String extensionWhere = getSourcesWhere(context);
        if (!TextUtils.isEmpty(selection)) {
            extensionWhere += " AND (" + selection + ")";
        }
        return context.getContentResolver().query(Notification.Source.URI, projection,
                extensionWhere, selectionArgs, sortOrder);
    }

    /**
     * Update sources, limit scope to sources in this extension
     * <p>
     *
     * This method is mainly aimed for extensions that shares user id and
     * process in which case they can access and modify data that belongs to
     * other extensions.
     * <p>
     *
     * Note that no runtime exceptions, such as {@link SecurityException} and
     * {@link SQLException}, will be handled by this method. Exceptions shall
     * instead be handled in the calling method if needed.
     *
     * @param context The context
     * @param values The new field values. The key is the column name for the
     *            field. A null value will remove an existing field value.
     * @param where A filter to apply to rows before updating, formatted as an
     *            SQL WHERE clause (excluding the WHERE itself).
     * @param selectionArgs Arguments to where String
     * @return The number of rows updated
     */
    public static int updateSources(Context context, ContentValues values, String where,
            String[] selectionArgs) {
        String extensionWhere = getSourcesWhere(context);
        if (!TextUtils.isEmpty(where)) {
            extensionWhere += " AND (" + where + ")";
        }
        return context.getContentResolver().update(Notification.Source.URI, values, extensionWhere,
                selectionArgs);
    }

    /**
     * Delete sources, limit scope to sources in this extension
     * <p>
     *
     * This method is mainly aimed for extensions that shares user id and
     * process in which case they can access and modify data that belongs to
     * other extensions.
     * <p>
     *
     * Note that no runtime exceptions, such as {@link SecurityException} and
     * {@link SQLException}, will be handled by this method. Exceptions shall
     * instead be handled in the calling method if needed.
     *
     * @param context The context
     * @param where A filter to apply to rows before deleting, formatted as an
     *            SQL WHERE clause (excluding the WHERE itself).
     * @param selectionArgs Arguments to where String
     * @return The number of rows deleted
     */
    public static int deleteSources(Context context, String where, String[] selectionArgs) {
        String extensionWhere = getSourcesWhere(context);
        if (!TextUtils.isEmpty(where)) {
            extensionWhere += " AND (" + where + ")";
        }
        return context.getContentResolver().delete(Notification.Source.URI, extensionWhere,
                selectionArgs);

    }

    /**
     * Get where string that limits a queries to {@link Notification.Source#URI}
     * and {@link Notification.SourceEvents#URI} to affect sources and source
     * events that belongs to this extension only
     *
     * @param context The context
     * @return The where string
     */
    public static String getSourcesWhere(Context context) {
        return Notification.SourceColumns.PACKAGE_NAME + " = '" + context.getPackageName() + "'";
    }

}
