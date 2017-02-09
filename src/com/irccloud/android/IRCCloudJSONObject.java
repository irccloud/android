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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.jr.stree.JacksonJrsTreeCodec;
import com.fasterxml.jackson.jr.stree.JrsArray;
import com.fasterxml.jackson.jr.stree.JrsBoolean;
import com.fasterxml.jackson.jr.stree.JrsNumber;
import com.fasterxml.jackson.jr.stree.JrsObject;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.stree.JrsValue;

import org.json.JSONObject;

import java.io.IOException;

public class IRCCloudJSONObject {
    private JrsObject o;
    private int cid = -1;
    private int bid = -1;
    private long eid = -1;
    private String type = null;

    public IRCCloudJSONObject() {
        o = new JrsObject();
    }

    public IRCCloudJSONObject(JrsObject object) {
        o = object;
    }

    public IRCCloudJSONObject(String message) {
        try {
            o = (JrsObject)JSON.std.with(new JacksonJrsTreeCodec()).treeFrom(message);
        } catch (IOException e) {
            NetworkConnection.printStackTraceToCrashlytics(e);
        }
    }

    public IRCCloudJSONObject(JSONObject object) {
        try {
            o = (JrsObject)JSON.std.treeFrom(object);
        } catch (IOException e) {
            NetworkConnection.printStackTraceToCrashlytics(e);
        }
    }

    public int cid() {
        if (cid == -1 && has("cid"))
            cid = getInt("cid");
        return cid;
    }

    public int bid() {
        if (bid == -1 && has("bid"))
            bid = getInt("bid");
        return bid;
    }

    public long eid() {
        if (eid == -1 && has("eid"))
            eid = getLong("eid");
        return eid;
    }

    public String type() {
        if (type == null) {
            if (has("type"))
                type = getString("type");
            else
                type = "undefined";
        }
        return type;
    }

    public boolean has(String name) {
        return o.get(name) != null;
    }

    public boolean getBoolean(String name) {
        return o.get(name) != null && ((JrsBoolean)o.get(name)).booleanValue();
    }

    public int getInt(String name) {
        try {
            return ((JrsNumber)o.get(name)).asBigInteger().intValue();
        } catch (IOException e) {

        }
        return -1;
    }

    public long getLong(String name) {
        try {
            return ((JrsNumber)o.get(name)).asBigDecimal().longValue();
        } catch (IOException e) {

        }
        return -1;
    }

    public String getString(String name) {
        if(o.get(name) != null)
            return o.get(name).asText();
        else
            return null;
    }

    public JrsObject getJrsObject(String name) {
        return (JrsObject)o.get(name);
    }

    public JrsArray getArray(String name) {
        return (JrsArray)o.get(name);
    }

    private static final ObjectMapper mapper = new ObjectMapper();
    public ObjectNode getJsonObject(String name) {
        return mapper.convertValue(o.get(name), ObjectNode.class);
    }

    public String toString() {
        try {
            return JSON.std.with(new JacksonJrsTreeCodec()).asString(o);
        } catch (IOException e) {
            NetworkConnection.printStackTraceToCrashlytics(e);
        }
        return null;
    }
}
