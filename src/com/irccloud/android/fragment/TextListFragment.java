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

package com.irccloud.android.fragment;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.text.style.TabStopSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.irccloud.android.R;

public class TextListFragment extends DialogFragment {
    TextView textView;
    String title = null;
    String text = null;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context ctx = getActivity();
        if(ctx == null)
            return null;

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.dialog_textlist, null);
        textView = v.findViewById(R.id.textView);
        textView.setHorizontallyScrolling(true);
        textView.setMovementMethod(new ScrollingMovementMethod());

        if (savedInstanceState != null && savedInstanceState.containsKey("text")) {
            text = savedInstanceState.getString("text");
        }

        if(text != null) {
            setText(text);
        }

        Dialog d = new AlertDialog.Builder(ctx)
                .setView(v)
                .setTitle(title)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        return d;
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putString("text", text);
    }

    public void refresh() {
        Bundle args = getArguments();

        if(args.containsKey("title")) {
            title = args.getString("title");
            if(getDialog() != null)
                getDialog().setTitle(title);
        }

        if(args.containsKey("text")) {
            text = args.getString("text");
            if(textView != null)
                setText(text);
        }
    }

    private void setText(String text) {
        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        for (int i = 0; i < 100; i++)
            sb.setSpan(new TabStopSpan.Standard(i * 300), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(sb, TextView.BufferType.SPANNABLE);
    }

    @Override
    public void setArguments(Bundle args) {
        super.setArguments(args);
        refresh();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
