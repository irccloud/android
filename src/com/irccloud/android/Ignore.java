/*
 * Copyright (c) 2013 IRCCloud, Ltd.
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

import java.util.ArrayList;

public class Ignore {
    private ArrayList<String> ignores = new ArrayList<String>();

    public synchronized void setIgnores(ArrayList<String> ignores) {
        this.ignores = new ArrayList<String>(ignores);
    }

    public synchronized void addMask(String usermask) {
        ignores.add(usermask);
    }

    public synchronized boolean match(String usermask) {
        if (ignores != null && ignores.size() > 0) {
            for (String ignore : ignores) {
                if (usermask.replace("!~", "!").toLowerCase().matches(ignore))
                    return true;
            }
        }
        return false;
    }
}
