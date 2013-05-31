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
 * The input info describes a host application input.
 */
public class InputInfo {

    private final long mId;

    private final boolean mEnabled;

    private final KeyPadInfo mKeyPad;

    /**
     * Create input info.
     *
     * @param id The input id.
     * @param enabled True if input is enabled.
     * @param keyPad The key pad info.
     */
    public InputInfo(long id, boolean enabled, KeyPadInfo keyPad) {
        mId = id;
        mEnabled = enabled;
        mKeyPad = keyPad;
    }

    /**
     * Get the id.
     *
     * @see Registration.InputColumns.#_ID
     *
     * @return The id.
     */
    public long getId() {
        return mId;
    }

    /**
     * Is the input enabled.
     *
     * @see Registration.InputColumns.#ENABLED
     *
     * @return True if enabled.
     */
    public boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Get the key pad.
     *
     * @return The key pad.
     */
    public KeyPadInfo getKeyPad() {
        return mKeyPad;
    }

}
