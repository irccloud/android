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

package com.irccloud.android.data.model;

import androidx.databinding.BaseObservable;

import com.irccloud.android.data.collection.BuffersList;

import java.util.ArrayList;

public class Channel extends BaseObservable {
    public static class Mode {
        public String mode;
        public String param;
    }

    public int cid;

    public int bid;

    public String name;

    public String topic_text;

    public long topic_time;

    public String topic_author;

    public String type;

    public String mode;

    public long timestamp;

    public String url;

    public boolean key;

    public ArrayList<Mode> modes = new ArrayList<>();
    public int valid = 1;

    public synchronized void addMode(String mode, String param, boolean init) {
        if (!init)
            removeMode(mode);
        if (mode.equals("k"))
            key = true;
        Mode m = new Mode();
        m.mode = mode;
        m.param = param;
        modes.add(m);
    }

    public synchronized void removeMode(String mode) {
        if (mode.equals("k"))
            key = false;
        for (Mode m : modes) {
            if (m.mode.equals(mode)) {
                modes.remove(m);
                return;
            }
        }
    }

    public synchronized boolean hasMode(String mode) {
        for (Mode m : modes) {
            if (m.mode.equals(mode)) {
                return true;
            }
        }
        return false;
    }

    public synchronized String paramForMode(String mode) {
        for (Mode m : modes) {
            if (m.mode.equals(mode)) {
                return m.param;
            }
        }
        return null;
    }

    public Buffer getBuffer() {
        return BuffersList.getInstance().getBuffer(bid);
    }
}
