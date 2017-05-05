/*
 * Copyright (c) 2015 IRCCloud, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irccloud.android.data.model;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextPaint;

import com.irccloud.android.ColorScheme;
import com.irccloud.android.IRCCloudApplication;

import java.util.HashMap;

public class Avatar {
    private HashMap<Integer, Bitmap> bitmaps_dark = new HashMap<>();
    private HashMap<Integer, Bitmap> bitmaps_light = new HashMap<>();
    private HashMap<Integer, Bitmap> bitmaps_self_light = new HashMap<>();
    private HashMap<Integer, Bitmap> bitmaps_self_dark = new HashMap<>();
    private HashMap<Integer, Bitmap> bitmaps_square = new HashMap<>();
    private static Typeface font = null;

    public long lastAccessTime = 0;
    public int cid;
    public String nick;

    public static Bitmap generateBitmap(String text, int textColor, int bgColor, boolean isDarkTheme, int size, boolean round) {
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        if(bitmap != null) {
            Canvas c = new Canvas(bitmap);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStyle(Paint.Style.FILL);

            if (isDarkTheme || !round) {
                p.setColor(bgColor);
                if (round)
                    c.drawCircle(size / 2, size / 2, size / 2, p);
                else
                    c.drawColor(bgColor);
            } else {
                float[] hsv = new float[3];
                Color.colorToHSV(bgColor, hsv);
                hsv[2] *= 0.8f;
                p.setColor(Color.HSVToColor(hsv));
                c.drawCircle(size / 2, size / 2, (size / 2) - 2, p);
                p.setColor(bgColor);
                c.drawCircle(size / 2, (size / 2) - 2, (size / 2) - 2, p);
            }
            TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            tp.setTextAlign(Paint.Align.CENTER);
            tp.setTypeface(font);
            tp.setTextSize((int) (size * 0.65));
            tp.setColor(textColor);
            if (isDarkTheme || !round) {
                c.drawText(text, size / 2, (size / 2) - ((tp.descent() + tp.ascent()) / 2), tp);
            } else {
                c.drawText(text, size / 2, (size / 2) - 4 - ((tp.descent() + tp.ascent()) / 2), tp);
            }

            return bitmap;
        } else {
            return null;
        }
    }

    public static Bitmap generateBitmap(String text, int textColor, int bgColor, boolean isDarkTheme, int size) {
        return generateBitmap(text, textColor, bgColor, isDarkTheme, size, true);
    }

    public Bitmap getBitmap(boolean isDarkTheme, int size) {
        return getBitmap(isDarkTheme, size, false);
    }

    public Bitmap getBitmap(boolean isDarkTheme, int size, boolean self) {
        return getBitmap(isDarkTheme, size, self, true);
    }

    public Bitmap getBitmap(boolean isDarkTheme, int size, boolean self, boolean round) {
        lastAccessTime = System.currentTimeMillis();
        HashMap<Integer, Bitmap> bitmaps = round?(self?(isDarkTheme?bitmaps_self_dark:bitmaps_self_light):(isDarkTheme?bitmaps_dark:bitmaps_light)):bitmaps_square;

        if(!bitmaps.containsKey(size) && nick != null && nick.length() > 0) {
            String normalizedNick = nick.toUpperCase().replaceAll("[_\\W]+", "");
            if(normalizedNick.length() == 0)
                normalizedNick = nick.toUpperCase();

            if(font == null) {
                font = Typeface.createFromAsset(IRCCloudApplication.getInstance().getApplicationContext().getAssets(), "SourceSansPro-Semibold.otf");
            }

            try {
                if (isDarkTheme) {
                    bitmaps.put(size, generateBitmap(normalizedNick.substring(0, 1), ColorScheme.getInstance().contentBackgroundColor, Color.parseColor("#" + (self ? ColorScheme.getInstance().selfTextColor : ColorScheme.colorForNick(nick, true))), true, size, round));
                } else {
                    bitmaps.put(size, generateBitmap(normalizedNick.substring(0, 1), 0xFFFFFFFF, Color.parseColor("#" + (self ? ColorScheme.getInstance().selfTextColor : ColorScheme.colorForNick(nick, false))), false, size, round));
                }
            } catch (OutOfMemoryError e) {
                return null;
            }
        }
        return bitmaps.get(size);
    }

    public String toString() {
        return "{cid: " + cid + ", nick: " + nick + "}";
    }

    protected void finalize() throws Throwable {
        try {
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                for (Bitmap bitmap : bitmaps_dark.values()) {
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                }
            }
            bitmaps_dark.clear();
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                for (Bitmap bitmap : bitmaps_light.values()) {
                    if (bitmap != null && !bitmap.isRecycled()) {
                        bitmap.recycle();
                    }
                }
            }
            bitmaps_light.clear();
        } finally {
            super.finalize();
        }
    }
}