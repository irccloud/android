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

import android.content.Context;
import android.support.v7.internal.view.ContextThemeWrapper;
import android.support.v7.widget.ShareActionProvider;

/**
 * Created by sam on 2/12/14.
 */

public class ShareActionProviderHax extends ShareActionProvider {
    public OnShareActionProviderSubVisibilityChangedListener onShareActionProviderSubVisibilityChangedListener;

    public ShareActionProviderHax(Context context) {
        super(new ContextThemeWrapper(context.getApplicationContext(), R.style.ImageViewerTheme));
    }

    //Expose the submenu visibility change to the parent activity
    public void subUiVisibilityChanged(boolean visible) {
        super.subUiVisibilityChanged(visible);
        if (onShareActionProviderSubVisibilityChangedListener != null)
            onShareActionProviderSubVisibilityChangedListener.onShareActionProviderSubVisibilityChanged(visible);
    }

    public interface OnShareActionProviderSubVisibilityChangedListener {
        public void onShareActionProviderSubVisibilityChanged(boolean visible);
    }
}
