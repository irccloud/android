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
import android.text.TextPaint;

import com.irccloud.android.ColorScheme;
import com.irccloud.android.IRCCloudApplication;

public class Avatar {
    private static final int AVATAR_SIZE = 320;
    private Bitmap bitmap;
    private static Typeface font = null;

    public long lastAccessTime = 0;
    public int cid;
    public String nick;

    public Bitmap getBitmap(boolean isDarkTheme) {
        lastAccessTime = System.currentTimeMillis();
        if(bitmap == null && nick != null && nick.length() > 0) {
            if(font == null) {
                font = Typeface.createFromAsset(IRCCloudApplication.getInstance().getApplicationContext().getAssets(), "SourceSansPro-Regular.otf");
            }

            bitmap = Bitmap.createBitmap(AVATAR_SIZE, AVATAR_SIZE, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bitmap);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setStyle(Paint.Style.FILL);

            if(isDarkTheme) {
                p.setColor(Color.parseColor("#" + ColorScheme.colorForNick(nick, true)));
                c.drawCircle(AVATAR_SIZE / 2, AVATAR_SIZE / 2, AVATAR_SIZE / 2, p);
            } else {
                float[] hsv = new float[3];
                int color = Color.parseColor("#" + ColorScheme.colorForNick(nick, false));
                Color.colorToHSV(color, hsv);
                hsv[2] *= 0.8f;
                p.setColor(Color.HSVToColor(hsv));
                c.drawCircle(AVATAR_SIZE / 2, AVATAR_SIZE / 2, (AVATAR_SIZE / 2) - 4, p);
                p.setColor(color);
                c.drawCircle(AVATAR_SIZE / 2, (AVATAR_SIZE / 2) - 4, (AVATAR_SIZE / 2) - 4, p);
            }
            TextPaint tp = new TextPaint();
            tp.setTextAlign(Paint.Align.CENTER);
            tp.setTypeface(font);
            tp.setTextSize((AVATAR_SIZE / 3) * 2);
            tp.setFakeBoldText(true);
            if (isDarkTheme) {
                tp.setColor(ColorScheme.getInstance().contentBackgroundColor);
                c.drawText(nick.toUpperCase().substring(0, 1), AVATAR_SIZE /2, (AVATAR_SIZE /2) - ((tp.descent() + tp.ascent()) / 2), tp);
            } else {
                tp.setColor(0xFFFFFFFF);
                c.drawText(nick.toUpperCase().substring(0, 1), AVATAR_SIZE /2, (AVATAR_SIZE /2) - 4 - ((tp.descent() + tp.ascent()) / 2), tp);
            }
        }
        return bitmap;
    }

    public String toString() {
        return "{cid: " + cid + ", nick: " + nick + "}";
    }

    protected void finalize() throws Throwable {
        try {
            if(bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
                bitmap = null;
            }
        } finally {
            super.finalize();
        }
    }
}