/*
 * Copyright (c) 2013 IRCCloud, Ltd.
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

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;

// An EditText that lets you use actions ("Done", "Go", etc.) on multi-line edits.
// From: http://stackoverflow.com/a/12570003/1406639
public class ActionEditText extends EditText
{
	private DrawerLayout mDrawerLayout = null;

    public ActionEditText(Context context)
    {
        super(context);
    }

    public ActionEditText(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public ActionEditText(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs)
    {
        InputConnection conn = super.onCreateInputConnection(outAttrs);
        outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NO_ENTER_ACTION;
        outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NAVIGATE_PREVIOUS;
        outAttrs.imeOptions &= ~EditorInfo.IME_FLAG_NAVIGATE_NEXT;
        if(PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("kb_send", true)) {
            outAttrs.inputType = EditorInfo.TYPE_CLASS_TEXT|EditorInfo.TYPE_TEXT_FLAG_AUTO_CORRECT;
        }
        if(PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("kb_caps", true)) {
            outAttrs.inputType |= EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES;
        } else {
            outAttrs.inputType &= ~EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES;
        }
        return conn;
    }
    
    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if(mDrawerLayout != null && event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
        	mDrawerLayout.closeDrawers();
        }
        return super.onKeyPreIme(keyCode, event);
    }
    
    public void setDrawerLayout(DrawerLayout view) {
    	mDrawerLayout = view;
    }
}