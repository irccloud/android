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

package com.irccloud.android;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

public class OffsetLinearLayout extends LinearLayout implements ViewTreeObserver.OnGlobalLayoutListener {
    private int offset_left;
    private int offset_top;

    public OffsetLinearLayout(Context context) {
        super(context);
        init();
    }

    public OffsetLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public OffsetLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public OffsetLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    public void offset(int left, int top) {
        offset_left = left;
        offset_top = top;
        layout();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        layout();
    }

    private void layout() {
        if (getLeft() != offset_left || getTop() != offset_top) {
            MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
            lp.leftMargin = offset_left;
            lp.topMargin = offset_top;
            setLayoutParams(lp);
            //requestLayout();
        }
    }

    @Override
    public void onGlobalLayout() {
        layout();
    }
}
