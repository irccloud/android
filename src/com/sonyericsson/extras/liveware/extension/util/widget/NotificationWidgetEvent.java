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

import com.sonyericsson.extras.liveware.extension.util.Dbg;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;

/**
 * The notification widget event class represents an event in the notification
 * database.
 */
public class NotificationWidgetEvent {
    protected String mName = null;

    protected String mTitle = null;

    protected String mMessage = null;

    protected long mTime = 0L;

    protected int mCount = 0;

    protected long mSourceId = 0;

    protected String mContactReference = null;

    protected String mProfileImageUri = null;

    protected String mFriendKey = null;

    protected final Context mContext;

    /**
     * Create notification widget event.
     *
     * @param context The context.
     */
    public NotificationWidgetEvent(final Context context) {
        mContext = context;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (!(object instanceof NotificationWidgetEvent)) {
            return false;
        }

        NotificationWidgetEvent event = (NotificationWidgetEvent)object;
        if (!TextUtils.equals(mName, event.mName)) {
            return false;
        }
        if (!TextUtils.equals(mTitle, event.mTitle)) {
            return false;
        }
        if (!TextUtils.equals(mMessage, event.mMessage)) {
            return false;
        }
        if (mTime != event.mTime) {
            return false;
        }
        if (mCount != event.mCount) {
            return false;
        }
        if (mSourceId != event.mSourceId) {
            return false;
        }
        if (!TextUtils.equals(mContactReference, event.mContactReference)) {
            return false;
        }
        if (!TextUtils.equals(mProfileImageUri, event.mProfileImageUri)) {
            return false;
        }
        if (!TextUtils.equals(mFriendKey, event.mFriendKey)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        // Hashing not supported.
        assert false : "hashCode not designed";
        return -1;
    }


    /**
     * Get the time.
     *
     * @return The time.
     */
    public long getTime() {
        return mTime;
    }

    /**
     * Set the time.
     *
     * @param time The time.
     */
    public void setTime(long time) {
        mTime = time;
    }

    /**
     * Get the count.
     *
     * @return The count.
     */
    public int getCount() {
        return mCount;
    }

    /**
     * Set the count.
     *
     * @param count The count.
     */
    public void setCount(int count) {
        mCount = count;
    }

    /**
     * Get the source id.
     *
     * @return The source id.
     */
    public long getSourceId() {
        return mSourceId;
    }

    /**
     * Set the source id.
     *
     * @param sourceId The source id.
     */
    public void setSourceId(long sourceId) {
        mSourceId = sourceId;
    }

    /**
     * Set the name.
     *
     * @param name The name.
     */
    public void setName(String name) {
        mName = name;
    }

    /**
     * Set the title.
     *
     * @param title The title.
     */
    public void setTitle(String title) {
        mTitle = title;
    }

    /**
     * Set the message.
     *
     * @param message The message.
     */
    public void setMessage(String message) {
        mMessage = message;
    }

    /**
     * Set the contact reference.
     *
     * @param contactReference The contact reference.
     */
    public void setContactReference(String contactReference) {
        mContactReference = contactReference;
    }

    /**
     * Set the profile image URI.
     *
     * @param profileImageUri The profile image URI.
     */
    public void setProfileImageUri(String profileImageUri) {
        mProfileImageUri = profileImageUri;
    }

    /**
     * Get the widget image.
     *
     * @return The image.
     */
    public Bitmap getImage() {
        // If profile image explicitly set then use it.
        // Otherwise get the contact photo.
        if (mProfileImageUri != null) {
            return ExtensionUtils.getBitmapFromUri(mContext, mProfileImageUri);
        } else {
            if (mContactReference != null) {
                Uri uri = Uri.parse(mContactReference);
                return ExtensionUtils.getContactPhoto(mContext, uri);
            } else {
                if (Dbg.DEBUG) {
                    Dbg.e("No image available");
                }
                return null;
            }
        }
    }

    /**
     * Get the name.
     *
     * @return The name.
     */
    public String getName() {
        // If display name explicitly set then use it.
        // Otherwise get the display name from the contact.
        if (mName != null) {
            return mName;
        } else {
            if (mContactReference != null) {
                Uri uri = Uri.parse(mContactReference);
                return ExtensionUtils.getContactName(mContext, uri);
            } else {
                if (Dbg.DEBUG) {
                    Dbg.e("No name");
                }
                return null;
            }
        }
    }

    /**
     * Get the title.
     *
     * @return The title.
     */
    public String getTitle() {
        // If title exist then use it otherwise use message.
        if (mTitle != null) {
            return mTitle;
        } else {
            return mMessage;
        }
    }

    /**
     * Get the friend key.
     *
     * @return The friend key.
     */
    public String getFriendKey() {
        return mFriendKey;
    }

    /**
     * Set the friend key.
     *
     * @param friendKey The friend key.
     */
    public void setFriendKey(String friendKey) {
        mFriendKey = friendKey;
    }



}
