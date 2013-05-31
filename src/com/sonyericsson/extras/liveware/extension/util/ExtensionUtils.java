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

package com.sonyericsson.extras.liveware.extension.util;

import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.aef.registration.Registration.Device;
import com.sonyericsson.extras.liveware.aef.registration.Registration.DeviceColumns;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.text.TextPaint;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.DisplayMetrics;

import java.io.IOException;
import java.io.InputStream;

/**
 * The extension utils class contains utility functions used by several
 * extensions.
 */
public class ExtensionUtils {

    /**
     * Invalid id
     */
    public static final int INVALID_ID = -1;

    /**
     * Draw text on canvas. Shade if text too long to fit.
     *
     * @param canvas The canvas to draw in.
     * @param text The text to draw.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @param textPaint The paint to draw with.
     * @param availableWidth The available width for the text
     */
    public static void drawText(Canvas canvas, String text, float x, float y, TextPaint textPaint,
            int availableWidth) {
        text = text.replaceAll("\\r?\\n", " ");
        final TextPaint localTextPaint = new TextPaint(textPaint);
        final float pixelsToShade = 1.5F * localTextPaint.getTextSize();
        int characters = text.length();

        if (localTextPaint.measureText(text) > availableWidth) {
            Paint.Align align = localTextPaint.getTextAlign();
            float shaderStopX;
            characters = localTextPaint.breakText(text, true, availableWidth, null);
            if (align == Paint.Align.LEFT) {
                shaderStopX = x + availableWidth;
            } else if (align == Paint.Align.CENTER) {
                float[] measuredWidth = new float[1];
                characters = localTextPaint.breakText(text, true, availableWidth, measuredWidth);
                shaderStopX = x + (measuredWidth[0] / 2);
            } else { // align == Paint.Align.RIGHT
                shaderStopX = x;
            }
            // Hex 0x60000000 = first two bytes is alpha, gives semitransparent
            localTextPaint.setShader(new LinearGradient(shaderStopX - pixelsToShade, 0,
                    shaderStopX, 0, localTextPaint.getColor(),
                    localTextPaint.getColor() + 0x60000000, Shader.TileMode.CLAMP));
        }
        canvas.drawText(text, 0, characters, x, y, localTextPaint);
    }

    /**
     * Get URI string from resourceId.
     *
     * @param context The context.
     * @param resourceId The resource id.
     * @return The URI string.
     */
    public static String getUriString(final Context context, final int resourceId) {
        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        return new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.getPackageName()).appendPath(Integer.toString(resourceId))
                .toString();
    }

    /**
     * Check in the database if there are any accessories connected.
     *
     * @param context The context
     * @return True if at least one accessories is connected.
     */
    public static boolean areAnyAccessoriesConnected(Context context) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Device.URI, null,
                    DeviceColumns.ACCESSORY_CONNECTED + " = 1", null, null);
            if (cursor != null) {
                return (cursor.getCount() > 0);
            }
        } catch (SQLException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query connected accessories", exception);
            }
        } catch (SecurityException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query connected accessories", exception);
            }
        } catch (IllegalArgumentException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query connected accessories", exception);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return false;
    }

    /**
     * Get the contact name from a URI.
     *
     * @param context The context.
     * @param contactUri The contact URI.
     *
     * @return The contact name.
     */
    public static String getContactName(final Context context, Uri contactUri) {
        String name = null;
        if (contactUri != null) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(contactUri, new String[] {
                    ContactsContract.Contacts.DISPLAY_NAME
                }, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    name = cursor.getString(cursor
                            .getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }

            }
        }

        return name;
    }

    /**
     * Get the contact photo from a contact URI.
     *
     * @param context The context.
     * @param contactUri The contact URI.
     *
     * @return The contact photo.
     */
    public static Bitmap getContactPhoto(final Context context, Uri contactUri) {
        Bitmap bitmap = null;
        if (contactUri != null) {
            InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(
                    context.getContentResolver(), contactUri);
            if (inputStream != null) {
                bitmap = BitmapFactory.decodeStream(inputStream);
                try {
                    inputStream.close();
                } catch (IOException e) {

                }
            }
        }

        return bitmap;
    }

    /**
     * Get bitmap from a URI.
     *
     * @param context The context.
     * @param uriString The URI as a string.
     *
     * @return The bitmap.
     */
    public static Bitmap getBitmapFromUri(final Context context, String uriString) {
        Bitmap bitmap = null;
        if (uriString == null) {
            return null;
        }

        Uri uri = Uri.parse(uriString);
        if (uri != null) {
            try {
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                if (bitmap != null) {
                    // We use default density for all bitmaps to avoid scaling.
                    bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
                }
            } catch (IOException e) {

            }
        }
        return bitmap;
    }

    /**
     * Get id of a registered extension
     *
     * @return Id, {@link #INVALID_ID} if extension is not registered
     */
    public static long getExtensionId(Context context) {
        Cursor cursor = null;
        long id = INVALID_ID;
        String selection = Registration.ExtensionColumns.PACKAGE_NAME + " = ?";
        String[] selectionArgs = new String[] {
            context.getPackageName()
        };
        try {
            cursor = context.getContentResolver().query(Registration.Extension.URI, null,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(Registration.ExtensionColumns._ID);
                id = cursor.getLong(idIndex);
            }
        } catch (SQLException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query extension", exception);
            }
        } catch (SecurityException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query extension", exception);
            }
        } catch (IllegalArgumentException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query extension", exception);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return id;
    }

    /**
     * Get id of a registered extension
     *
     * @return Id, {@link #INVALID_ID} if extension is not registered
     */
    public static long getRegistrationId(Context context, String hostAppPackageName, long extensionId) {
        Cursor cursor = null;
        long id = INVALID_ID;
        String selection = Registration.ApiRegistrationColumns.HOST_APPLICATION_PACKAGE
                + " = ? AND " + Registration.ApiRegistrationColumns.EXTENSION_ID + " = ?";
        String[] selectionArgs = new String[] {
                hostAppPackageName, Long.toString(extensionId)
        };
        try {
            cursor = context.getContentResolver().query(Registration.ApiRegistration.URI, null,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idIndex = cursor.getColumnIndex(Registration.ApiRegistrationColumns._ID);
                id = cursor.getLong(idIndex);
            }
        } catch (SQLException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query extension", exception);
            }
        } catch (SecurityException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query extension", exception);
            }
        } catch (IllegalArgumentException exception) {
            if (Dbg.DEBUG) {
                Dbg.e("Failed to query extension", exception);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return id;
    }

    /**
     * Get the value of the intent extra parameter
     * {@link Registration.Intents#EXTRA_ACCESSORY_SUPPORTS_HISTORY} from the
     * intent that started the configuration activity.
     *
     * @param intent The intent that started the configuration activity, see
     *            {@link Registration.ExtensionColumns#CONFIGURATION_ACTIVITY}
     * @return Value of
     *         {@link Registration.Intents#EXTRA_ACCESSORY_SUPPORTS_HISTORY},
     *         true if not contained in the intent extras.
     */
    public static boolean supportsHistory(Intent intent) {
        boolean supportsHistory = true;
        if (intent == null) {
            if (Dbg.DEBUG) {
                Dbg.e("ExtensionUtils.supportsHistory: intent == null");
            }
            return supportsHistory;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            if (Dbg.DEBUG) {
                Dbg.e("ExtensionUtils.supportsHistory: extras == null");
            }
            return supportsHistory;
        }
        if (extras.containsKey(Registration.Intents.EXTRA_ACCESSORY_SUPPORTS_HISTORY)) {
            supportsHistory = extras
                    .getBoolean(Registration.Intents.EXTRA_ACCESSORY_SUPPORTS_HISTORY);
        } else {
            if (Dbg.DEBUG) {
                Dbg.e("ExtensionUtils.supportsHistory: EXTRA_ACCESSORY_SUPPORTS_HISTORY not present");
            }
        }
        return supportsHistory;
    }

    /**
     * Get the value of the intent extra parameter
     * {@link Registration.Intents#EXTRA_ACCESSORY_SUPPORTS_ACTIONS} from the
     * intent that started the configuration activity.
     *
     * @param intent The intent that started the configuration activity, see
     *            {@link Registration.ExtensionColumns#CONFIGURATION_ACTIVITY}
     * @return Value of
     *         {@link Registration.Intents#EXTRA_ACCESSORY_SUPPORTS_ACTIONS},
     *         true if not contained in the intent extras.
     */
    public static boolean supportsActions(Intent intent) {
        boolean supportsActions = true;
        if (intent == null) {
            if (Dbg.DEBUG) {
                Dbg.e("ExtensionUtils.supportsActions: intent == null");
            }
            return supportsActions;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            if (Dbg.DEBUG) {
                Dbg.e("ExtensionUtils.supportsActions: extras == null");
            }
            return supportsActions;
        }
        if (extras.containsKey(Registration.Intents.EXTRA_ACCESSORY_SUPPORTS_ACTIONS)) {
            supportsActions = extras
                    .getBoolean(Registration.Intents.EXTRA_ACCESSORY_SUPPORTS_ACTIONS);
        } else {
            if (Dbg.DEBUG) {
                Dbg.e("ExtensionUtils.supportsActions: EXTRA_ACCESSORY_SUPPORTS_ACTIONS not present");
            }
        }
        return supportsActions;
    }

    /**
     * Get formatted time.
     *
     * @param publishedTime The published time in millis.
     *
     * @return The formatted time.
     */
    static public String getFormattedTime(long publishedTime) {
        // This is copied from RecentCallsListActivity.java

        long now = System.currentTimeMillis();

        // Set the date/time field by mixing relative and absolute times.
        int flags = DateUtils.FORMAT_ABBREV_ALL;

        if (!DateUtils.isToday(publishedTime)) {
            // DateUtils.getRelativeTimeSpanString doesn't consider the nature
            // days comparing with DateUtils.getRelativeDayString. Override the
            // real date to implement the requirement.

            Time time = new Time();
            time.set(now);
            long gmtOff = time.gmtoff;
            int days = Time.getJulianDay(publishedTime, gmtOff) - Time.getJulianDay(now, gmtOff);

            // Set the delta from now to get the correct display
            publishedTime = now + days * DateUtils.DAY_IN_MILLIS;
        } else if (publishedTime > now && (publishedTime - now) < DateUtils.HOUR_IN_MILLIS) {
            // Avoid e.g. "1 minute left" when publish time is "07:00" and
            // current time is "06:58"
            publishedTime += DateUtils.MINUTE_IN_MILLIS;
        }

        return (DateUtils.getRelativeTimeSpanString(publishedTime, now, DateUtils.MINUTE_IN_MILLIS,
                flags)).toString();
    }

}
