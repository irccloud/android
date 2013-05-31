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
import com.irccloud.android.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.DisplayMetrics;

/**
 * This class contains Smart Wireless Headset pro specific utility functions and
 * constants
 */
public class SmartWirelessHeadsetProUtil {

    public static final int DISPLAY_WIDTH = 128;

    public static final int DISPLAY_HEIGHT = 36;

    public static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.ARGB_8888;

    public static final int CONFIRM_PADDING = 1;

    public static final int CONFIRM_TEXT_X = 1;

    public static final int CONFIRM_TEXT_Y = 22;

    /**
     * Create text paint for Smart Wireless Headset pro for current locale.
     *
     * @param context The context.
     * @return The text paint.
     */
    public static TextPaint createTextPaint(final Context context) {
        TextPaint textPaint = new TextPaint();

        textPaint.setTextSize(context.getResources().getDimensionPixelSize(
                R.dimen.headset_pro_text_size));
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setColor(Color.WHITE);
        return textPaint;
    }

    /**
     * Generate bitmap with ok and cancel icons and a text.
     *
     * @param context The context.
     * @param text The text.
     * @param okInFocus true if ok icon should be focused else false.
     * @param hideCancel true if cancel icon should be hidden.
     *
     * @return The text paint.
     */
    public static Bitmap getConfirmBitmap(final Context context, final String text,
            final boolean okInFocus, final boolean hideCancel) {
        Bitmap bitmap = Bitmap.createBitmap(DISPLAY_WIDTH, DISPLAY_HEIGHT, BITMAP_CONFIG);
        // Set the density to default to avoid scaling.
        bitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

        Canvas canvas = new Canvas(bitmap);
        // Black background
        canvas.drawColor(Color.BLACK);

        // draw text
        TextPaint textPaint = SmartWirelessHeadsetProUtil.createTextPaint(context);
        canvas.drawText(text, 0, text.length(), CONFIRM_TEXT_X, CONFIRM_TEXT_Y, textPaint);

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        // We use default throughout the extension to avoid any automatic
        // scaling.
        // Keep in mind that we are not showing the images on the phone, but on
        // the accessory.
        bitmapOptions.inDensity = DisplayMetrics.DENSITY_DEFAULT;
        bitmapOptions.inTargetDensity = DisplayMetrics.DENSITY_DEFAULT;

        Bitmap focusBitmap = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.headset_pro_focus_xs_icn, bitmapOptions);
        Bitmap okBitmap = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.headset_pro_ok_icn, bitmapOptions);
        Bitmap cancelBitmap = BitmapFactory.decodeResource(context.getResources(),
                R.drawable.headset_pro_cancel_icn, bitmapOptions);

        // draw focus mark
        if (okInFocus) {
            canvas.drawBitmap(focusBitmap, DISPLAY_WIDTH - focusBitmap.getWidth(), 0, null);
        } else if (!hideCancel) {
            canvas.drawBitmap(focusBitmap, DISPLAY_WIDTH - focusBitmap.getWidth(),
                    DISPLAY_HEIGHT / 2, null);
        }

        // draw ok
        canvas.drawBitmap(okBitmap, DISPLAY_WIDTH - okBitmap.getWidth() - CONFIRM_PADDING,
                CONFIRM_PADDING, null);

        // draw cancel
        if (!hideCancel) {
            canvas.drawBitmap(cancelBitmap, DISPLAY_WIDTH - cancelBitmap.getWidth()
                    - CONFIRM_PADDING, DISPLAY_HEIGHT - cancelBitmap.getHeight() - CONFIRM_PADDING,
                    null);
        }
        return bitmap;
    }

}
