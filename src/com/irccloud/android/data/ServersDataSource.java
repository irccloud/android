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

package com.irccloud.android.data;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irccloud.android.Notifications;

public class ServersDataSource {
	public class Server implements Comparable<Server> {
		public int cid;
        public String name;
        public String hostname;
        public int port;
        public String nick;
        public String status;
        public long lag;
        public int ssl;
        public String realname;
        public String server_pass;
        public String nickserv_pass;
        public String join_commands;
        public ObjectNode fail_info;
        public String away;
        public String usermask;
        public String mode;
        public ObjectNode isupport;
        public JsonNode raw_ignores;
		public ArrayList<String> ignores;
        public int order;
        public String CHANTYPES;
        public ObjectNode PREFIX;
        public String MODE_OWNER = "q";
        public String MODE_ADMIN = "a";
        public String MODE_OP = "o";
        public String MODE_HALFOP = "h";
        public String MODE_VOICED = "v";

        @Override
        public int compareTo(Server another) {
            if(order != another.order)
                return Integer.valueOf(order).compareTo(another.order);
            return Integer.valueOf(cid).compareTo(another.cid);
        }
    }

	private SparseArray<Server> servers;
	
	private static ServersDataSource instance = null;
	
	public synchronized static ServersDataSource getInstance() {
		if(instance == null)
			instance = new ServersDataSource();
		return instance;
	}

	public ServersDataSource() {
		servers = new SparseArray<Server>();
	}

	public void clear() {
		servers.clear();
	}
	
	public Server createServer(int cid, String name, String hostname, int port, String nick, String status, long lag, int ssl, String realname, String server_pass, String nickserv_pass, String join_commands, ObjectNode fail_info, String away, JsonNode ignores, int order) {
		Server s = getServer(cid);
		if(s == null) {
			s = new Server();
			servers.put(cid, s);
		}
		s.cid = cid;
		s.name = name;
		s.hostname = hostname;
		s.port = port;
		s.nick = nick;
		s.status = status;
		s.lag = lag;
		s.ssl = ssl;
		s.realname = realname;
		s.server_pass = server_pass;
		s.nickserv_pass = nickserv_pass;
		s.join_commands = join_commands;
		s.fail_info = fail_info;
		s.away = away;
		s.usermask = "";
		s.mode = "";
        s.order = order;
        s.isupport = new ObjectMapper().createObjectNode();
        if(s.name == null || s.name.length() == 0)
            s.name = s.hostname;
        if(ignores != null)
            updateIgnores(cid, ignores);
		return s;
	}
	
	public int count() {
		return servers.size();
	}
	
	public void updateLag(int cid, long lag) {
		Server s = getServer(cid);
		if(s != null) {
			s.lag = lag;
		}
	}

	public void updateNick(int cid, String nick) {
		Server s = getServer(cid);
		if(s != null) {
			s.nick = nick;
		}
	}

	public void updateStatus(int cid, String status, ObjectNode fail_info) {
		Server s = getServer(cid);
		if(s != null) {
			s.status = status;
			s.fail_info = fail_info;
		}
	}

	public void updateAway(int cid, String away) {
		Server s = getServer(cid);
		if(s != null) {
			s.away = away;
		}
	}

	public void updateUsermask(int cid, String usermask) {
		Server s = getServer(cid);
		if(s != null) {
			s.usermask = usermask;
		}
	}

	public void updateMode(int cid, String mode) {
		Server s = getServer(cid);
		if(s != null) {
			s.mode = mode;
		}
	}

    public void updateUserModes(int cid, String modes) {
        if(modes != null && modes.length() == 5 && modes.charAt(0) != 'q') {
            Server s = getServer(cid);
            if(s != null) {
                s.MODE_OWNER = modes.substring(0, 1);
            }
        }
    }

	public void updateIsupport(int cid, ObjectNode params) {
		Server s = getServer(cid);
		if(s != null) {
            s.isupport.putAll(params);
            if(s.isupport.has("PREFIX")) {
                s.PREFIX = (ObjectNode)s.isupport.get("PREFIX");
            } else {
                s.PREFIX = new ObjectMapper().createObjectNode();
                s.PREFIX.put(s.MODE_OWNER, "~");
                s.PREFIX.put(s.MODE_ADMIN, "&");
                s.PREFIX.put(s.MODE_OP, "@");
                s.PREFIX.put(s.MODE_HALFOP, "%");
                s.PREFIX.put(s.MODE_VOICED, "+");
            }
            if(s.isupport.has("CHANTYPES"))
                s.CHANTYPES = s.isupport.get("CHANTYPES").asText();
            else
                s.CHANTYPES = null;
		}
	}

	public void updateIgnores(int cid, JsonNode ignores) {
		Server s = getServer(cid);
		if(s != null) {
            s.raw_ignores = ignores;
            s.ignores = new ArrayList<String>();
            for(int i = 0; i < ignores.size(); i++) {
                String mask = ignores.get(i).asText().toLowerCase()
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
                s.ignores.add(mask);
            }
		}
	}

	public void deleteServer(int cid) {
        servers.remove(cid);
	}

	public void deleteAllDataForServer(int cid) {
		Server s = getServer(cid);
		if(s != null) {
			ArrayList<BuffersDataSource.Buffer> buffersToRemove = new ArrayList<BuffersDataSource.Buffer>();
			
			Iterator<BuffersDataSource.Buffer> i = BuffersDataSource.getInstance().getBuffersForServer(cid).iterator();
			while(i.hasNext()) {
				BuffersDataSource.Buffer b = i.next();
				buffersToRemove.add(b);
			}
			
			i=buffersToRemove.iterator();
			while(i.hasNext()) {
				BuffersDataSource.Buffer b = i.next();
				BuffersDataSource.getInstance().deleteAllDataForBuffer(b.bid);
				Notifications.getInstance().deleteNotificationsForBid(b.bid);
			}
			servers.remove(cid);
		}
	}

	public synchronized SparseArray<Server> getServers() {
		return servers;
	}

	public Server getServer(int cid) {
        return servers.get(cid);
	}

	public Server getServer(String hostname) {
        for(int i = 0; i < servers.size(); i++) {
            Server s = servers.valueAt(i);
            if(s.hostname.equalsIgnoreCase(hostname))
                return s;
        }
		return null;
	}

	public Server getServer(String hostname, int port) {
        for(int i = 0; i < servers.size(); i++) {
            Server s = servers.valueAt(i);
            if(s.hostname.equalsIgnoreCase(hostname) && s.port == port)
                return s;
        }
		return null;
	}

	public Server getServer(String hostname, boolean ssl) {
        for(int i = 0; i < servers.size(); i++) {
            Server s = servers.valueAt(i);
            if(s.hostname.equalsIgnoreCase(hostname) && ((!ssl && s.ssl == 0) || (ssl && s.ssl > 0)))
                return s;
        }
		return null;
	}
}
