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

package com.sonyericsson.extras.liveware.extension.util.registration;


/**
 * The display info describes a host application display.
 */
public class DisplayInfo {

    private final long mId;

    private final int mWidth;

    private final int mHeight;

    private final int mColors;

    private final int mRefreshRate;

    private final int mLatency;

    private final boolean mTapTouch;

    private final boolean mMotionTouch;

    /**
     * Create display info.
     *
     * @param id The id.
     * @param width The width.
     * @param height The height.
     * @param colors The colors.
     * @param refreshRate The refresh rate.
     * @param latency The latency.
     * @param tapTouch True if tap touch is supported.
     * @param motionTouch True if motion touch is supported.
     */
    public DisplayInfo(final long id, final int width, final int height, final int colors,
            final int refreshRate, final int latency, final boolean tapTouch,
            final boolean motionTouch) {
        mId = id;
        mWidth = width;
        mHeight = height;
        mColors = colors;
        mRefreshRate = refreshRate;
        mLatency = latency;
        mTapTouch = tapTouch;
        mMotionTouch = motionTouch;
    }

    /**
     * Get the id.
     *
     * @see Registration.DisplayColumns.#_ID
     *
     * @return The id.
     */
    public long getId() {
        return mId;
    }

    /**
     * Get the width.
     *
     * @see Registration.DisplayColumns.#DISPLAY_WIDTH
     *
     * @return The width.
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Get the height.
     *
     * @see Registration.DisplayColumns.#DISPLAY_HEIGHT
     *
     * @return The height.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Get the number of colors supported by the display.
     *
     * @see Registration.DisplayColumns.#COLORS
     *
     * @return The number of colors.
     */
    public int getColors() {
        return mColors;
    }

    /**
     * Get the refresh rate supported by the display.
     *
     * @see Registration.DisplayColumns.#REFRESH_RATE
     *
     * @return The refresh rate.
     */
    public int getRefreshRate() {
        return mRefreshRate;
    }

    /**
     * Get the display latency.
     *
     * @see Registration.DisplayColumns.#LATENCY
     *
     * @return The latency.
     */
    public int getLatency() {
        return mLatency;
    }

    /**
     * Is tap touch supported.
     *
     * @see Registration.DisplayColumns.#TAP_TOUCH
     *
     * @return True if tap touch is supported.
     */
    public boolean isTapTouch() {
        return mTapTouch;
    }

    /**
     * Is motion touch supported.
     *
     * @see Registration.DisplayColumns.#MOTION_TOUCH
     *
     * @return True if motion touch is supported.
     */
    public boolean isMotionTouch() {
        return mMotionTouch;
    }

    /**
     * Check if the display size is equal to the provided width and height.
     *
     * @param width The width to check.
     * @param height The height to check.
     * @return True if the display is equal to the provided with and height.
     */
    public boolean sizeEquals(int width, int height) {
        return (mWidth == width && mHeight == height);
    }

}
