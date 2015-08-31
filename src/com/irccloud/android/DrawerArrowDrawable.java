/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.irccloud.android;


import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.util.TypedValue;

/**
 * A drawable that can draw a "Drawer hamburger" menu or an Arrow and animate between them.
 */
public class DrawerArrowDrawable extends Drawable {

    private final Paint mPaint = new Paint();

    // The angle in degress that the arrow head is inclined at.
    private static final float ARROW_HEAD_ANGLE = (float) Math.toRadians(45);
    private final float mBarThickness;
    // The length of top and bottom bars when they merge into an arrow
    private final float mTopBottomArrowSize;
    // The length of middle bar
    private final float mBarSize;
    // The length of the middle bar when arrow is shaped
    private final float mMiddleArrowSize;
    // The space between bars when they are parallel
    private final float mBarGap;
    // Whether bars should spin or not during progress
    private final boolean mSpin;
    // Use Path instead of canvas operations so that if color has transparency, overlapping sections
    // wont look different
    private final Path mPath = new Path();
    // The reported intrinsic size of the drawable.
    private final int mSize;
    // Whether we should mirror animation when animation is reversed.
    private boolean mVerticalMirror = false;
    // The interpolated version of the original progress
    private float mProgress;

    private final Activity mContext;

    /**
     * @param context used to get the configuration for the drawable from
     */
    public DrawerArrowDrawable(Activity context) {
        Resources r = context.getResources();
        mContext = context;
        mPaint.setAntiAlias(true);
        mPaint.setColor(r.getColor(android.R.color.secondary_text_light));
        mSize = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, r.getDisplayMetrics());
        mBarSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, r.getDisplayMetrics());
        mTopBottomArrowSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 11.31f, r.getDisplayMetrics());
        mBarThickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, r.getDisplayMetrics());
        mBarGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, r.getDisplayMetrics());
        mSpin = false;
        mMiddleArrowSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, r.getDisplayMetrics());

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.SQUARE);
        mPaint.setStrokeWidth(mBarThickness);
    }

    boolean isLayoutRtl() {
        return ViewCompat.getLayoutDirection(mContext.getWindow().getDecorView())
                == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    /**
     * If set, canvas is flipped when progress reached to end and going back to start.
     */
    protected void setVerticalMirror(boolean verticalMirror) {
        mVerticalMirror = verticalMirror;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        final boolean isRtl = isLayoutRtl();
        // Interpolated widths of arrow bars
        final float arrowSize = lerp(mBarSize, mTopBottomArrowSize, mProgress);
        final float middleBarSize = lerp(mBarSize, mMiddleArrowSize, mProgress);
        // Interpolated size of middle bar
        final float middleBarCut = lerp(0, mBarThickness / 2, mProgress);
        // The rotation of the top and bottom bars (that make the arrow head)
        final float rotation = lerp(0, ARROW_HEAD_ANGLE, mProgress);

        // The whole canvas rotates as the transition happens
        final float canvasRotate = lerp(isRtl ? 0 : -180, isRtl ? 180 : 0, mProgress);
        final float topBottomBarOffset = lerp(mBarGap + mBarThickness, 0, mProgress);
        mPath.rewind();

        final float arrowEdge = -middleBarSize / 2;
        // draw middle bar
        mPath.moveTo(arrowEdge + middleBarCut, 0);
        mPath.rLineTo(middleBarSize - middleBarCut, 0);

        final float arrowWidth = Math.round(arrowSize * Math.cos(rotation));
        final float arrowHeight = Math.round(arrowSize * Math.sin(rotation));

        // top bar
        mPath.moveTo(arrowEdge, topBottomBarOffset);
        mPath.rLineTo(arrowWidth, arrowHeight);

        // bottom bar
        mPath.moveTo(arrowEdge, -topBottomBarOffset);
        mPath.rLineTo(arrowWidth, -arrowHeight);
        mPath.moveTo(0, 0);
        mPath.close();

        canvas.save();
        // Rotate the whole canvas if spinning, if not, rotate it 180 to get
        // the arrow pointing the other way for RTL.
        if (mSpin) {
            canvas.rotate(canvasRotate * ((mVerticalMirror ^ isRtl) ? -1 : 1),
                    bounds.centerX(), bounds.centerY());
        } else if (isRtl) {
            canvas.rotate(180, bounds.centerX(), bounds.centerY());
        }
        canvas.translate(bounds.centerX(), bounds.centerY());
        canvas.drawPath(mPath, mPaint);

        canvas.restore();
    }

    @Override
    public void setAlpha(int i) {
        mPaint.setAlpha(i);
    }

    // override
    public boolean isAutoMirrored() {
        // Draws rotated 180 degrees in RTL mode.
        return true;
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    public ColorFilter getColorFilter() {
        return mPaint.getColorFilter();
    }

    public void setColor(int color) {
        mPaint.setColor(color);
        invalidateSelf();
    }

    public int getColor() {
        return mPaint.getColor();
    }

    @Override
    public int getIntrinsicHeight() {
        return mSize;
    }

    @Override
    public int getIntrinsicWidth() {
        return mSize;
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public float getProgress() {
        return mProgress;
    }

    public void setProgress(float progress) {
        if (progress == 1f) {
            setVerticalMirror(true);
        } else if (progress == 0f) {
            setVerticalMirror(false);
        }
        mProgress = progress;
        invalidateSelf();
    }

    /**
     * Linear interpolate between a and b with parameter t.
     */
    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}