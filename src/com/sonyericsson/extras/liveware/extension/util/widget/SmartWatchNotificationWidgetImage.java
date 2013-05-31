/*
Copyright (c) 2011, Sony Ericsson Mobile Communications AB

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

 * Neither the name of the Sony Ericsson Mobile Communications AB nor the names
  of its contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.sonyericsson.extras.liveware.extension.util.widget;

import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.irccloud.android.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * The class decorates a widget image with notification specific UI components.
 */
public class SmartWatchNotificationWidgetImage extends SmartWatchWidgetImage {

    private NotificationWidgetEvent mEvent;

    /**
     * Create notification widget image.
     *
     * @param context The context.
     * @param event The event.
     */
    public SmartWatchNotificationWidgetImage(final Context context,
            final NotificationWidgetEvent event) {
        super(context);
        setInnerLayoutResourceId(R.layout.smart_watch_notification_widget);
        setBadgeCount(event.getCount());
        mEvent = event;
    }

    @Override
    protected void applyInnerLayout(LinearLayout innerLayout) {

        Bitmap backgroundBitmap = mEvent.getImage();
        if (null != backgroundBitmap) {
            ((ImageView)innerLayout
                    .findViewById(R.id.smart_watch_notification_widget_background))
                    .setImageBitmap(backgroundBitmap);

            ((ImageView)innerLayout
                    .findViewById(R.id.smart_watch_notification_widget_text_background))
                    .setVisibility(View.VISIBLE);
        }

        // set title
        ((TextView)innerLayout.findViewById(R.id.smart_watch_notification_widget_text_title))
                .setText(mEvent.getTitle());

        // set time stamp
        String time = ExtensionUtils.getFormattedTime(mEvent.getTime());
        ((TextView)innerLayout.findViewById(R.id.smart_watch_notification_widget_text_time))
                .setText(time);

        // set name
        ((TextView)innerLayout.findViewById(R.id.smart_watch_notification_widget_text_name))
                .setText(mEvent.getName());
    }

}
