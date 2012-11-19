package com.irccloud.android;

import android.content.Context;
import android.util.AttributeSet;
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
			x = 0;
		else if(x > buffersDisplayWidth + usersDisplayWidth / 4)
			x = buffersDisplayWidth + usersDisplayWidth;
		else
			x = buffersDisplayWidth;
		super.scrollTo(x, y);
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
						//enableDisableViewGroup((ViewGroup)findViewById(R.id.messageContainer), false);
					} else if(startX >= buffersDisplayWidth && getScrollX() > startX) {
						smoothScrollTo(buffersDisplayWidth + usersDisplayWidth, 0);
						activity.showUpButton(true);
						//enableDisableViewGroup((ViewGroup)findViewById(R.id.messageContainer), false);
					} else {
						smoothScrollTo(buffersDisplayWidth, 0);
						activity.showUpButton(true);
						//enableDisableViewGroup((ViewGroup)findViewById(R.id.messageContainer), true);
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
	
	//originally: http://stackoverflow.com/questions/5418510/disable-the-touch-events-for-all-the-views
	//modified for the needs here
	public void enableDisableViewGroup(ViewGroup viewGroup, boolean enabled) {
		int childCount = viewGroup.getChildCount();
		for (int i = 0; i < childCount; i++) {
			View view = viewGroup.getChildAt(i);
			if(view.isFocusable())
				view.setEnabled(enabled);
			if (view instanceof ViewGroup) {
				enableDisableViewGroup((ViewGroup) view, enabled);
			} else if (view instanceof ListView) {
				if(view.isFocusable())
					view.setEnabled(enabled);
				ListView listView = (ListView) view;
				int listChildCount = listView.getChildCount();
				for (int j = 0; j < listChildCount; j++) {
					if(view.isFocusable())
						listView.getChildAt(j).setEnabled(false);
				}
			}
		}
	}
}
