package com.irccloud.android;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;
import android.widget.ListView;

public class MessageActivityPager extends HorizontalScrollView {
	private int startX = 0;
	private int buffersDisplayWidth = 0;
	private int usersDisplayWidth = 0;
	MessageActivity activity = null;
	private boolean overrideScrollPos = false;
	
	public MessageActivityPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		buffersDisplayWidth = (int)getResources().getDimension(R.dimen.drawer_width);
		usersDisplayWidth = (int)getResources().getDimension(R.dimen.userlist_width);
		if(!isInEditMode())
			activity = (MessageActivity)context;
	}

	@Override
	public void scrollTo(int x, int y) {
		if(x < buffersDisplayWidth)
			super.scrollTo(0, 0);
		else if(x > buffersDisplayWidth + usersDisplayWidth / 4)
			super.scrollTo(buffersDisplayWidth + usersDisplayWidth, 0);
		else
			super.scrollTo(buffersDisplayWidth, 0);
	}
	
	@Override
	public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
		overrideScrollPos = true;
		return super.requestChildRectangleOnScreen(child, rectangle, immediate);
	}
	
	@Override
	public void computeScroll() {
		super.computeScroll();
		if(overrideScrollPos) {
			super.scrollTo(buffersDisplayWidth, 0);
			overrideScrollPos = false;
		}
	}
	
	@Override
	protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if(changed)
			scrollTo(buffersDisplayWidth, 0);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(isEnabled()) {
			if(event.getAction() == MotionEvent.ACTION_MOVE && startX == 0) {
				startX = getScrollX();
			}
			if(event.getAction() == MotionEvent.ACTION_MOVE) { //Prevent dragging from one drawer to the other
				if((startX < buffersDisplayWidth && getScrollX() >= buffersDisplayWidth) ||
						(startX >= buffersDisplayWidth + usersDisplayWidth / 4 && getScrollX() <= buffersDisplayWidth))
					return true;
			} else if(event.getAction() == MotionEvent.ACTION_UP) { //Finger is lifted, snap into place!
				if(Math.abs(startX - getScrollX()) > buffersDisplayWidth / 4) { //If they've dragged a drawer more than 25% on screen, snap the drawer onto the screen
					if(startX < buffersDisplayWidth + usersDisplayWidth / 4 && getScrollX() < startX) {
						smoothScrollTo(0, 0);
						activity.showUpButton(false);
					} else if(startX >= buffersDisplayWidth && getScrollX() > startX) {
						smoothScrollTo(buffersDisplayWidth + usersDisplayWidth, 0);
						activity.showUpButton(true);
					} else {
						smoothScrollTo(buffersDisplayWidth, 0);
						activity.showUpButton(true);
					}
				} else { //Snap back
					if(startX < buffersDisplayWidth)
						smoothScrollTo(0, 0);
					else if(startX > buffersDisplayWidth + usersDisplayWidth / 4)
						smoothScrollTo(buffersDisplayWidth + usersDisplayWidth, 0);
					else
						smoothScrollTo(buffersDisplayWidth, 0);
				}
				startX = 0;
				return true;
			}
			return super.onTouchEvent(event);
		} else {
			return false;
		}
	}
}
