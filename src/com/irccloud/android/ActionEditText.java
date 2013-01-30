package com.irccloud.android;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.EditText;
import android.widget.HorizontalScrollView;

// An EditText that lets you use actions ("Done", "Go", etc.) on multi-line edits.
// From: http://stackoverflow.com/a/12570003/1406639
public class ActionEditText extends EditText
{
	private HorizontalScrollView mScrollView = null;
	
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
        return conn;
    }
    
    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        if(mScrollView != null && event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
        	mScrollView.scrollTo((int)getResources().getDimension(R.dimen.drawer_width), 0);
        }
        return super.onKeyPreIme(keyCode, event);
    }
    
    public void setScrollView(HorizontalScrollView view) {
    	mScrollView = view;
    }
}