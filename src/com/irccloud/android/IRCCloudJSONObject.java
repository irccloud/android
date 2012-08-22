package com.irccloud.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class IRCCloudJSONObject {
	JSONObject o;
	
	public IRCCloudJSONObject(String message) throws JSONException {
		o = new JSONObject(message);
	}
	
	public IRCCloudJSONObject(JSONObject object) {
		o = object;
	}
	
	public int cid() {
		int cid = 0;
		try {
			cid = o.getInt("cid");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return cid;
	}
	
	public int bid() {
		int bid = 0;
		try {
			bid = o.getInt("bid");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return bid;
	}

	public long eid() {
		long eid = 0;
		try {
			eid = o.getLong("eid");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return eid;
	}
	
	public String type() {
		String type = "undefined";
		try {
			type = o.getString("type");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return type;
	}

	public boolean highlight() {
		boolean highlight = false;
		try {
			if(o.has("highlight"))
				highlight = o.getBoolean("highlight");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return highlight;
	}
	
	public boolean has(String name) throws JSONException {
		return o.has(name);
	}
	
	public boolean getBoolean(String name) throws JSONException {
		return o.getBoolean(name);
	}
	
	public int getInt(String name) throws JSONException {
		return o.getInt(name);
	}
	
	public long getLong(String name) throws JSONException {
		return o.getLong(name);
	}
	
	public String getString(String name) throws JSONException {
		return o.getString(name);
	}
	
	public JSONObject getJSONObject(String name) throws JSONException {
		return o.getJSONObject(name);
	}
	
	public JSONArray getJSONArray(String name) throws JSONException {
		return o.getJSONArray(name);
	}
	
	public JSONObject getObject() {
		return o;
	}
	
	public String toString() {
		return o.toString();
	}
}
