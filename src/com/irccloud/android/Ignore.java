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

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;

public class Ignore {
	private ArrayList<String> ignores = new ArrayList<String>();
	
	public void setIgnores(JsonArray ignores) {
        for(int i = 0; i < ignores.size(); i++) {
            String mask = ignores.get(i).getAsString().toLowerCase()
                    .replace("\\", "\\\\")
                    .replace("(", "\\(")
                    .replace(")", "\\)")
                    .replace("[", "\\[")
                    .replace("]", "\\]")
                    .replace("{", "\\{")
                    .replace("}", "\\}")
                    .replace("-", "\\-")
                    .replace("^", "\\^")
                    .replace("$", "\\$")
                    .replace("|", "\\|")
                    .replace("+", "\\+")
                    .replace("?", "\\?")
                    .replace(".", "\\.")
                    .replace(",", "\\,")
                    .replace("#", "\\#")
                    .replace("*", ".*")
                    .replace("!~", "!");
            if(!mask.contains("!"))
                if(mask.contains("@"))
                    mask = ".*!" + mask;
                else
                    mask += "!.*";
            if(!mask.contains("@"))
                if(mask.contains("!"))
                    mask = mask.replace("!", "!.*@");
                else
                    mask += "@.*";
            if(mask.equals(".*!.*@.*"))
                continue;
            this.ignores.add(mask);
        }
    }

	public void addMask(String usermask) {
		ignores.add(usermask);
	}
	
	public boolean match(String usermask) {
		if(ignores != null && ignores.size() > 0) {
            for(String ignore : ignores) {
				if(usermask.replace("!~","!").toLowerCase().matches(ignore))
					return true;
			}
		}
		return false;
	}
}
