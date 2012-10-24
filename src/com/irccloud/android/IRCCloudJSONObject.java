package com.irccloud.android;

import org.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class IRCCloudJSONObject {
	JsonObject o;
	
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
		int cid = 0;
		if(o.has("cid"))
			cid = o.get("cid").getAsInt();
		return cid;
	}
	
	public int bid() {
		int bid = 0;
		if(o.has("bid"))
			bid = o.get("bid").getAsInt();
		return bid;
	}

	public long eid() {
		long eid = 0;
		if(o.has("eid"))
			eid = o.get("eid").getAsLong();
		return eid;
	}
	
	public String type() {
		String type = "undefined";
		if(o.has("type"))
			type = o.get("type").getAsString();
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
		return o.get(name).getAsInt();
	}
	
	public long getLong(String name) {
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
