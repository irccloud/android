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

import java.util.ArrayList;
import java.util.HashMap;

public class Ignore {
    private ArrayList<String> ignores = new ArrayList<>();
    private HashMap<String, Boolean> cache = new HashMap<>();

    public synchronized void setIgnores(ArrayList<String> ignores) {
        cache.clear();
        if(ignores != null)
            this.ignores = new ArrayList<>(ignores);
        else
            this.ignores = new ArrayList<>();
    }

    public synchronized void clear() {
        cache.clear();
        ignores.clear();
    }

    public synchronized void addMask(String usermask) {
        ignores.add(usermask);
    }

    public synchronized boolean match(String usermask) {
        if(cache.containsKey(usermask))
            return cache.get(usermask);

        if (ignores != null && ignores.size() > 0) {
            for (String ignore : ignores) {
                if (usermask.replace("!~", "!").toLowerCase().matches(ignore)) {
                    cache.put(usermask, true);
                    return true;
                }
            }
        }

        cache.put(usermask, false);
        return false;
    }
}
