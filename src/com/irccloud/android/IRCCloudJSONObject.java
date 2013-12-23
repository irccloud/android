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

import org.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class IRCCloudJSONObject {
	JsonObject o;
	int cid = -1;
	int bid = -1;
	long eid = -1;
	String type = null;
	
	public IRCCloudJSONObject() {
		o = new JsonObject();
	}
	
	public IRCCloudJSONObject(String message) {
		JsonParser parser = new JsonParser();
		o = parser.parse(message).getAsJsonObject();
	}
	
	public IRCCloudJSONObject(JSONObject object) {
		JsonParser parser = new JsonParser();
		o = parser.parse(object.toString()).getAsJsonObject();
	}
	
	public IRCCloudJSONObject(JsonObject object) {
		o = object;
	}
	
	public int cid() {
		if(cid == -1 && o.has("cid"))
			cid = o.get("cid").getAsInt();
		return cid;
	}
	
	public int bid() {
		if(bid == -1 && o.has("bid"))
			bid = o.get("bid").getAsInt();
		return bid;
	}

	public long eid() {
		if(eid == -1 && o.has("eid"))
			eid = o.get("eid").getAsLong();
		return eid;
	}
	
	public String type() {
		if(type == null) {
			if(o.has("type"))
				type = o.get("type").getAsString();
			else
				type = "undefined";
		}
		return type;
	}

	public boolean highlight() {
		boolean highlight = false;
		if(o.has("highlight"))
			highlight = o.get("highlight").getAsBoolean();
		return highlight;
	}
	
	public boolean has(String name) {
		return o.has(name) && !o.get(name).isJsonNull();
	}
	
	public boolean getBoolean(String name) {
		if(!o.has(name) || o.get(name).isJsonNull())
			return false;
		return o.get(name).getAsBoolean();
	}
	
	public int getInt(String name) {
        if(!o.has(name) || o.get(name).isJsonNull())
            return -1;
		return o.get(name).getAsInt();
	}
	
	public long getLong(String name) {
        if(!o.has(name) || o.get(name).isJsonNull())
            return -1;
		return o.get(name).getAsLong();
	}
	
	public String getString(String name) {
		if(!o.has(name) || o.get(name).isJsonNull())
			return null;
		return o.get(name).getAsString();
	}
	
	public JsonObject getJsonObject(String name) {
		if(!o.has(name) || o.get(name).isJsonNull())
			return null;
		return o.getAsJsonObject(name);
	}
	
	public JsonArray getJsonArray(String name) {
		return o.getAsJsonArray(name);
	}
	
	public JsonObject getObject() {
		return o;
	}
	
	public String toString() {
		return o.toString();
	}
}
