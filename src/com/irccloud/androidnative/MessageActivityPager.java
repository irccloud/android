package com.irccloud.androidnative;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.widget.HorizontalScrollView;
import android.widget.ListView;

public class MessageActivityPager extends HorizontalScrollView {
	private int startX = 0;
	private int buffersDisplayWidth = 0;
	private int usersDisplayWidth = 0;
	MessageActivity activity;
	
	public MessageActivityPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		buffersDisplayWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 250, getResources().getDisplayMetrics());
		usersDisplayWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 250, getResources().getDisplayMetrics());
		activity = (MessageActivity)context;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_MOVE && startX == 0) {
			startX = getScrollX();
		}
		if(event.getAction() == MotionEvent.ACTION_MOVE) { //Prevent dragging from one drawer to the other
			if((startX < buffersDisplayWidth && getScrollX() >= buffersDisplayWidth) ||
					(startX >= buffersDisplayWidth + usersDisplayWidth / 4 && getScrollX() <= buffersDisplayWidth))
				return true;
		} else if(event.getAction() == MotionEvent.ACTION_UP) { //Finger is lifted, snap into place!
			if(Math.abs(startX - getScrollX()) > buffersDisplayWidth / 4) { //If they've dragged a drawer more than 25% on screen, snap the drawer onto the screen
				if(startX < buffersDisplayWidth + buffersDisplayWidth / 4 && getScrollX() < startX) {
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
				else if(startX > buffersDisplayWidth + buffersDisplayWidth / 4)
					smoothScrollTo(buffersDisplayWidth + usersDisplayWidth, 0);
				else
					smoothScrollTo(buffersDisplayWidth, 0);
			}
			startX = 0;
			return true;
		}
		return super.onTouchEvent(event);
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
