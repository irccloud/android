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

package com.sonyericsson.extras.liveware.extension.util;

import android.util.Log;

public final class Dbg {

    public static final boolean DEBUG = true;

    private static String LOG_TAG = "ExtensionUtils";

    private Dbg() {
    }

    private static boolean logEnabled() {
        if (DEBUG) {
            return Log.isLoggable(LOG_TAG, Log.DEBUG);
        } else {
            return false;
        }
    }

    public static void v(String s) {
        if (logEnabled()) {
            android.util.Log.v(LOG_TAG, s);
        }
    }

    public static void e(String s) {
        if (logEnabled()) {
            android.util.Log.e(LOG_TAG, s);
        }
    }

    public static void e(String s, Throwable t) {
        if (logEnabled()) {
            android.util.Log.e(LOG_TAG, s, t);
        }
    }

    public static void w(String s) {
        if (logEnabled()) {
            android.util.Log.w(LOG_TAG, s);
        }
    }

    public static void w(String s, Throwable t) {
        if (logEnabled()) {
            android.util.Log.w(LOG_TAG, s, t);
        }
    }

    public static void d(String s) {
        if (logEnabled()) {
            android.util.Log.d(LOG_TAG, s);
        }
    }

    public static void setLogTag(final String tag) {
        LOG_TAG = tag;
    }
}
