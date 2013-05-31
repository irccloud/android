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

package com.sonyericsson.extras.liveware.extension.util.notification;

import android.database.ContentObserver;
import android.os.Handler;

/**
 * The delayed content observer is used to handle a content observer change
 * after a delay. If more content observer changes are triggered during the
 * delay the original change is deleted and a new delay is set.
 */
public abstract class DelayedContentObserver extends ContentObserver {

    public static final int EVENT_READ_DELAY = 1000;

    public static final int CONTACTS_UPDATE_DELAY = 3000;

    private final Handler mHandler;

    private final int mDelay;

    private final Runnable mDelayedRunnable = new Runnable() {
        public void run() {
            onChangeDelayed();
        }
    };

    /**
     * Create delayed content observer.
     *
     * @param handler The handler.
     * @param delay The delay in ms.
     */
    public DelayedContentObserver(final Handler handler, final int delay) {
        super(handler);
        if (handler == null) {
            throw new IllegalArgumentException("handler is null");
        }

        mHandler = handler;
        mDelay = delay;
    }

    /**
     * onChangeDelayed is called when the delay has passed and no new changes
     * has been triggered during the delay.
     */
    public abstract void onChangeDelayed();

    @Override
    public void onChange(boolean selfChange) {
        mHandler.removeCallbacks(mDelayedRunnable);
        mHandler.postDelayed(mDelayedRunnable, mDelay);
    }

}
