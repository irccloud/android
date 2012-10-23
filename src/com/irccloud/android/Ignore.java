package com.irccloud.android;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;

public class Ignore {
	private JsonArray ignores = null;
	
	public void setIgnores(JsonArray ignores) {
		this.ignores = ignores;
	}

	public void addMask(String usermask) {
		if(ignores == null)
			ignores = new JsonArray();

		ignores.add(new JsonPrimitive(usermask));
	}
	
	public boolean match(String usermask) {
		if(ignores != null && ignores.size() > 0) {
			for(int i = 0; i < ignores.size(); i++) {
				String mask = ignores.get(i).getAsString().toLowerCase().replace("*", ".*").replace("!~", "!");
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
				if(usermask.replace("!~","!").toLowerCase().matches(mask))
					return true;
			}
		}
		return false;
	}
}
