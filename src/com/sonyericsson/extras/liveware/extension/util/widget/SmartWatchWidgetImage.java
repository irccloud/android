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

package com.sonyericsson.extras.liveware.extension.util.widget;

import com.irccloud.android.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;

/**
 * The SmartWatchWidgetImage class is used to generate widget bitmap which follows
 * official SmartWatch layout of where e.g. frame and icon shall be located.
 * The layout inside the frame can be customized by applying setting inner
 * layout resource id.
 */
public class SmartWatchWidgetImage {

    private final Bitmap mBitmap;

    private Bitmap mIconBitmap;

    private final Canvas mCanvas;

    private String mText;

    private int mBadgeCount;

    private int mInnerLayoutResid;

    protected final Context mContext;

    protected final BitmapFactory.Options mBitmapOptions;

    protected final int mOuterWidth;

    protected final int mOuterHeight;

    protected final int mInnerWidth;

    protected final int mInnerHeight;

    /**
     * Initiate the SmartWatch widget image.
     *
     * @param context The context.
     */
    public SmartWatchWidgetImage(final Context context) {
        mContext = context;

        mText = null;
        mIconBitmap = null;
        mInnerLayoutResid = 0;
        mBadgeCount = 0;

        mOuterWidth = mContext.getResources().getDimensionPixelSize(
                R.dimen.smart_watch_widget_width_outer);
        mOuterHeight = mContext.getResources().getDimensionPixelSize(
                R.dimen.smart_watch_widget_height_outer);

        mInnerWidth = mContext.getResources().getDimensionPixelSize(
                R.dimen.smart_watch_widget_width_inner);
        mInnerHeight = mContext.getResources().getDimensionPixelSize(
                R.dimen.smart_watch_widget_height_inner);

        mBitmap = Bitmap.createBitmap(mOuterWidth, mOuterHeight, Bitmap.Config.ARGB_8888);

        // Set the density to default to avoid scaling.
        mBitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
        mCanvas = new Canvas(mBitmap);

        // Options to avoid scaling.
        mBitmapOptions = new BitmapFactory.Options();
        mBitmapOptions.inDensity = DisplayMetrics.DENSITY_DEFAULT;
        mBitmapOptions.inTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
        mBitmapOptions.inScaled = false;
    }

    /**
     * Set custom text. Typically used when only a text shall be displayed, and
     * no specific layout is needed.
     *
     * @param text The text.
     *
     * @return this.
     */
    public SmartWatchWidgetImage setText(String text) {
        mText = text;
        return this;
    }

    /**
     * Set widget icon by id.
     *
     * @param iconId The icon id.
     *
     * @return this.
     */
    public SmartWatchWidgetImage setIconByResourceId(int iconId) {
        mIconBitmap = BitmapFactory.decodeResource(mContext.getResources(), iconId, mBitmapOptions);
        return this;
    }

    /**
     * Set widget icon by uri.
     *
     * @param iconUri The icon uri.
     *
     * @return this.
     */
    public SmartWatchWidgetImage setIconByUri(String iconUri) {
        if (iconUri == null) {
            return this;
        }

        Uri uri = Uri.parse(iconUri);
        if (uri != null) {
            try {
                mIconBitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri);
                // We use default density for all bitmaps to avoid scaling.
                mIconBitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);
            } catch (IOException e) {

            }
        }
        return this;
    }

    /**
     * Set custom layout inside the widget frame. Not needed setText is used.
     *
     * @param layoutId The layout id.
     *
     * @return this.
     */
    public SmartWatchWidgetImage setInnerLayoutResourceId(int layoutId) {
        mInnerLayoutResid = layoutId;
        return this;
    }

    /**
     * Set number to be shown in upper left badge. Badge is not visible if
     * number < 1.
     *
     * @param number The number.
     *
     * @return this.
     */
    public SmartWatchWidgetImage setBadgeCount(int number) {
        mBadgeCount = number;
        return this;
    }

    /**
     * Apply Set custom text. Typically used when only a text shall be
     * displayed, and no specific layout is needed.
     *
     * @param text The text.
     *
     * @return this.
     */
    private void draw() {
        LinearLayout root = new LinearLayout(mContext);
        root.setLayoutParams(new LayoutParams(mOuterWidth, mOuterHeight));

        LinearLayout linearLayout = (LinearLayout)LinearLayout.inflate(mContext,
                R.layout.smart_watch_widget, root);

        if (mBadgeCount > 0) {
            TextView badgeText = (TextView)linearLayout
                    .findViewById(R.id.smart_watch_widget_event_counter_text);
            badgeText.setText(Integer.toString(mBadgeCount));
            badgeText.setVisibility(View.VISIBLE);

            ImageView badgeBackground = (ImageView)linearLayout
                    .findViewById(R.id.smart_watch_widget_event_counter_badge);
            badgeBackground.setVisibility(View.VISIBLE);
        }

        ImageView icon = (ImageView)linearLayout.findViewById(R.id.smart_watch_widget_icon);
        icon.setImageBitmap(mIconBitmap);

        if (null != mText) {
            TextView textView = (TextView)linearLayout
                    .findViewById(R.id.smart_watch_widget_custom_text_view);
            textView.setText(mText);
        }

        ImageView customImage = (ImageView)linearLayout
                .findViewById(R.id.smart_watch_widget_custom_image);
        customImage.setImageBitmap(getInnerBitmap());

        linearLayout.measure(mOuterWidth, mOuterHeight);
        linearLayout
                .layout(0, 0, linearLayout.getMeasuredWidth(), linearLayout.getMeasuredHeight());

        linearLayout.draw(mCanvas);
    }

    /**
     * Get bitmap inside the frame.
     *
     * @return a bitmap or null if no inner layout is applied.
     */
    private Bitmap getInnerBitmap() {
        if (mInnerLayoutResid != 0) {
            Bitmap innerBitmap = Bitmap.createBitmap(mInnerWidth, mInnerHeight,
                    Bitmap.Config.ARGB_8888);

            // Set the density to default to avoid scaling.
            innerBitmap.setDensity(DisplayMetrics.DENSITY_DEFAULT);

            LinearLayout root = new LinearLayout(mContext);
            root.setLayoutParams(new LayoutParams(mInnerWidth, mInnerHeight));

            LinearLayout innerLayout = (LinearLayout)LinearLayout.inflate(mContext,
                    mInnerLayoutResid, root);

            applyInnerLayout(innerLayout);

            innerLayout.measure(mInnerWidth, mInnerHeight);
            innerLayout.layout(0, 0, innerLayout.getMeasuredWidth(),
                    innerLayout.getMeasuredHeight());

            Canvas innerCanvas = new Canvas(innerBitmap);
            innerLayout.draw(innerCanvas);

            return innerBitmap;
        } else {
            return null;
        }
    }

    /**
     * Get bitmap inside the frame.
     *
     * Example:
     * ((TextView)innerLayout.findViewById(R.id.my_custom_widget_city)).
     * setText("Paris");
     */
    protected void applyInnerLayout(LinearLayout innerLayout) {
        throw new IllegalArgumentException(
                "applyInnerLayout() not implemented. Child class must override this method since innerLayoutResid != 0 ");
    }

    /**
     * Get the bitmap.
     *
     * @return The bitmap.
     */
    public Bitmap getBitmap() {
        draw();
        return mBitmap;
    }

}
