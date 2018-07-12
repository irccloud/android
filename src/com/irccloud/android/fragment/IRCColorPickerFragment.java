/*
 * Copyright (c) 2018 IRCCloud, Ltd.
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

package com.irccloud.android.fragment;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.irccloud.android.ColorScheme;
import com.irccloud.android.R;

public class IRCColorPickerFragment extends Fragment {
    public static final String ARG_BACKGROUND = "background";
    private boolean mBackground;

    private OnColorPickedListener mListener;

    public IRCColorPickerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mBackground = getArguments().getBoolean(ARG_BACKGROUND);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mListener != null)
                    mListener.onColorPicked((int)view.getTag(), mBackground);
            }
        };

        View v = inflater.inflate(R.layout.irccolorpicker, container, false);

        v.findViewById(R.id.color0).setOnClickListener(clickListener);
        v.findViewById(R.id.color1).setOnClickListener(clickListener);
        v.findViewById(R.id.color2).setOnClickListener(clickListener);
        v.findViewById(R.id.color3).setOnClickListener(clickListener);
        v.findViewById(R.id.color4).setOnClickListener(clickListener);
        v.findViewById(R.id.color5).setOnClickListener(clickListener);
        v.findViewById(R.id.color6).setOnClickListener(clickListener);
        v.findViewById(R.id.color7).setOnClickListener(clickListener);
        v.findViewById(R.id.color8).setOnClickListener(clickListener);
        v.findViewById(R.id.color9).setOnClickListener(clickListener);
        v.findViewById(R.id.color10).setOnClickListener(clickListener);
        v.findViewById(R.id.color11).setOnClickListener(clickListener);
        v.findViewById(R.id.color12).setOnClickListener(clickListener);
        v.findViewById(R.id.color13).setOnClickListener(clickListener);
        v.findViewById(R.id.color14).setOnClickListener(clickListener);
        v.findViewById(R.id.color15).setOnClickListener(clickListener);

        return v;
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        mBackground = args.getBoolean(ARG_BACKGROUND);
        updateColors();
    }

    public boolean isBackground() {
        return mBackground;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateColors();
    }

    private void updateColors() {
        View v = getView();

        if(v != null) {
            Drawable d = VectorDrawableCompat.create(getActivity().getResources(), R.drawable.circle, null).mutate();
            d.setColorFilter(ColorScheme.getIRCColor(0, mBackground), PorterDuff.Mode.SRC_ATOP);
            (v.findViewById(R.id.color0)).setBackgroundDrawable(d);
            (v.findViewById(R.id.color0)).setTag(ColorScheme.getIRCColor(0, mBackground));

            d = VectorDrawableCompat.create(getActivity().getResources(), R.drawable.circle, null).mutate();
            d.setColorFilter(ColorScheme.getIRCColor(1, mBackground), PorterDuff.Mode.SRC_ATOP);
            (v.findViewById(R.id.color1)).setBackgroundDrawable(d.mutate());
            (v.findViewById(R.id.color1)).setTag(ColorScheme.getIRCColor(1, mBackground));

            d = VectorDrawableCompat.create(getActivity().getResources(), R.drawable.circle, null).mutate();
            d.setColorFilter(ColorScheme.getIRCColor(2, mBackground), PorterDuff.Mode.SRC_ATOP);
            (v.findViewById(R.id.color2)).setBackgroundDrawable(d.mutate());
            (v.findViewById(R.id.color2)).setTag(ColorScheme.getIRCColor(2, mBackground));

            d = VectorDrawableCompat.create(getActivity().getResources(), R.drawable.circle, null).mutate();
            d.setColorFilter(ColorScheme.getIRCColor(3, mBackground), PorterDuff.Mode.SRC_ATOP);
            (v.findViewById(R.id.color3)).setBackgroundDrawable(d.mutate());
            (v.findViewById(R.id.color3)).setTag(ColorScheme.getIRCColor(3, mBackground));

            d = VectorDrawableCompat.create(getActivity().getResources(), R.drawable.circle, null).mutate();
            d.setColorFilter(ColorScheme.getIRCColor(4, mBackground), PorterDuff.Mode.SRC_ATOP);
            (v.findViewById(R.id.color4)).setBackgroundDrawable(d.mutate());
            (v.findViewById(R.id.color4)).setTag(ColorScheme.getIRCColor(4, mBackground));

            d = VectorDrawableCompat.create(getActivity().getResources(), R.drawable.circle, null).mutate();
            d.setColorFilter(ColorScheme.getIRCColor(5, mBackground), PorterDuff.Mode.SRC_ATOP);
            (v.findViewById(R.id.color5)).setBackgroundDrawable(d.mutate());
            (v.findViewById(R.id.color5)).setTag(ColorScheme.getIRCColor(5, mBackground));

            d = VectorDrawableCompat.create(getActivity().getResources(), R.drawable.circle, null).mutate();
            d.setColorFilter(ColorScheme.getIRCColor(6, mBackground), PorterDuff.Mode.SRC_ATOP);
            (v.findViewById(R.id.color6)).setBackgroundDrawable(d.mutate());
            (v.findViewById(R.id.color6)).setTag(ColorScheme.getIRCColor(6, mBackground));

            d = VectorDrawableCompat.create(getActivity().getResources(), R.drawable.circle, null).mutate();
            d.setColorFilter(ColorScheme.getIRCColor(7, mBackground), PorterDuff.Mode.SRC_ATOP);
            (v.findViewById(R.id.color7)).setBackgroundDrawable(d.mutate());
            (v.findViewById(R.id.color7)).setTag(ColorScheme.getIRCColor(7, mBackground));

            d = VectorDrawableCompat.create(getActivity().getResources(), R.drawable.circle, null).mutate();
            d.setColorFilter(ColorScheme.getIRCColor(8, mBackground), PorterDuff.Mode.SRC_ATOP);
            (v.findViewById(R.id.color8)).setBackgroundDrawable(d.mutate());
            (v.findViewById(R.id.color8)).setTag(ColorScheme.getIRCColor(8, mBackground));

            d = VectorDrawableCompat.create(getActivity().getResources(), R.drawable.circle, null).mutate();
            d.setColorFilter(ColorScheme.getIRCColor(9, mBackground), PorterDuff.Mode.SRC_ATOP);
            (v.findViewById(R.id.color9)).setBackgroundDrawable(d.mutate());
            (v.findViewById(R.id.color9)).setTag(ColorScheme.getIRCColor(9, mBackground));

            d = VectorDrawableCompat.create(getActivity().getResources(), R.drawable.circle, null).mutate();
            d.setColorFilter(ColorScheme.getIRCColor(10, mBackground), PorterDuff.Mode.SRC_ATOP);
            (v.findViewById(R.id.color10)).setBackgroundDrawable(d.mutate());
            (v.findViewById(R.id.color10)).setTag(ColorScheme.getIRCColor(10, mBackground));

            d = VectorDrawableCompat.create(getActivity().getResources(), R.drawable.circle, null).mutate();
            d.setColorFilter(ColorScheme.getIRCColor(11, mBackground), PorterDuff.Mode.SRC_ATOP);
            (v.findViewById(R.id.color11)).setBackgroundDrawable(d.mutate());
            (v.findViewById(R.id.color11)).setTag(ColorScheme.getIRCColor(11, mBackground));

            d = VectorDrawableCompat.create(getActivity().getResources(), R.drawable.circle, null).mutate();
            d.setColorFilter(ColorScheme.getIRCColor(12, mBackground), PorterDuff.Mode.SRC_ATOP);
            (v.findViewById(R.id.color12)).setBackgroundDrawable(d.mutate());
            (v.findViewById(R.id.color12)).setTag(ColorScheme.getIRCColor(12, mBackground));

            d = VectorDrawableCompat.create(getActivity().getResources(), R.drawable.circle, null).mutate();
            d.setColorFilter(ColorScheme.getIRCColor(13, mBackground), PorterDuff.Mode.SRC_ATOP);
            (v.findViewById(R.id.color13)).setBackgroundDrawable(d.mutate());
            (v.findViewById(R.id.color13)).setTag(ColorScheme.getIRCColor(13, mBackground));

            d = VectorDrawableCompat.create(getActivity().getResources(), R.drawable.circle, null).mutate();
            d.setColorFilter(ColorScheme.getIRCColor(14, mBackground), PorterDuff.Mode.SRC_ATOP);
            (v.findViewById(R.id.color14)).setBackgroundDrawable(d.mutate());
            (v.findViewById(R.id.color14)).setTag(ColorScheme.getIRCColor(14, mBackground));

            d = VectorDrawableCompat.create(getActivity().getResources(), R.drawable.circle, null).mutate();
            d.setColorFilter(ColorScheme.getIRCColor(15, mBackground), PorterDuff.Mode.SRC_ATOP);
            (v.findViewById(R.id.color15)).setBackgroundDrawable(d.mutate());
            (v.findViewById(R.id.color15)).setTag(ColorScheme.getIRCColor(15, mBackground));
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnColorPickedListener) {
            mListener = (OnColorPickedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnColorPickedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnColorPickedListener {
        void onColorPicked(int color, boolean background);
    }
}
