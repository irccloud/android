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

package com.sonyericsson.extras.liveware.extension.util.control;

import com.sonyericsson.extras.liveware.aef.control.Control;

/**
 * The control touch event class holds information about
 * a touch event.
 */
public class ControlTouchEvent {
    private final int mAction;
    private final long mTimeStamp;
    private final int mX;
    private final int mY;

    /**
     * Create touch event.
     *
     * @see Control.Intents#ACTION_PRESS
     * @see Control.Intents#ACTION_LONGPRESS
     * @see Control.Intents#ACTION_RELEASE
     *
     * @param action    Touch action.
     * @param timeStamp The time when the event occurred.
     * @param x         The x position.
     * @param y         The y position.
     */
    public ControlTouchEvent(final int action, final long timeStamp, final int x, final int y) {
        mAction = action;
        mTimeStamp = timeStamp;
        mX = x;
        mY = y;
    }

    /**
     * Get the touch event action.
     *
     * @return The action.
     */
    public int getAction() {
        return mAction;
    }

    /**
     * Get the touch event time stamp.
     *
     * @return The time stamp.
     */
    public long getTimeStamp() {
        return mTimeStamp;
    }

    /**
     * Get the touch event x position.
     *
     * @return The x position.
     */
    public int getX() {
        return mX;
    }

    /**
     * Get the touch event y position.
     *
     * @return The y position.
     */
    public int getY() {
        return mY;
    }
}
