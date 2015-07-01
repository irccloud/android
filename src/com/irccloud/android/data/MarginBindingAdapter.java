package com.irccloud.android.data;

import android.databinding.BindingAdapter;
import android.view.View;
import android.view.ViewGroup;

public class MarginBindingAdapter {
    @BindingAdapter("android:layout_marginTop")
    public static void setLayoutMarginTop(View view, float margin) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)view.getLayoutParams();
        if (lp != null) {
            lp.setMargins(lp.leftMargin, (int)margin, lp.rightMargin, lp.bottomMargin);
            view.setLayoutParams(lp);
        }
    }

    @BindingAdapter("android:layout_marginBottom")
    public static void setLayoutMarginBottom(View view, float margin) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)view.getLayoutParams();
        if (lp != null) {
            lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, (int)margin);
            view.setLayoutParams(lp);
        }
    }
}
