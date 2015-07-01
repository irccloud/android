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

package com.irccloud.android.data.collection;

import android.database.sqlite.SQLiteException;
import android.util.SparseArray;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irccloud.android.Notifications;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.model.Server;
import com.raizlabs.android.dbflow.runtime.TransactionManager;
import com.raizlabs.android.dbflow.sql.language.Delete;
import com.raizlabs.android.dbflow.sql.language.Select;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ServersList {
    private SparseArray<Server> servers;

    private static ServersList instance = null;

    public synchronized static ServersList getInstance() {
        if (instance == null)
            instance = new ServersList();
        return instance;
    }

    public ServersList() {
        servers = new SparseArray<>(10);
    }

    public void clear() {
        servers.clear();
        new Delete().from(Server.class).queryClose();
    }

    public void load() {
        try {
            List<Server> c = new Select().all().from(Server.class).queryList();
            if (c != null && !c.isEmpty()) {
                for (Server s : c) {
                    servers.put(s.cid, s);
                    updateIgnores(s.cid, s.raw_ignores);
                }
            }
        } catch (SQLiteException e) {
            servers.clear();
        }
    }

    public void save() {
        ArrayList<Server> s = new ArrayList<>(servers.size());
        for (int i = 0; i < servers.size(); i++) {
            s.add(servers.valueAt(i));
        }
        TransactionManager.getInstance().saveOnSaveQueue(s);
    }

    public Server createServer(int cid, String name, String hostname, int port, String nick, String status, long lag, int ssl, String realname, String server_pass, String nickserv_pass, String join_commands, ObjectNode fail_info, String away, JsonNode ignores, int order) {
        Server s = getServer(cid);
        if (s == null) {
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
        if (s.name == null || s.name.length() == 0)
            s.name = s.hostname;
        if (ignores != null)
            updateIgnores(cid, ignores);
        return s;
    }

    public int count() {
        return servers.size();
    }

    public void updateLag(int cid, long lag) {
        Server s = getServer(cid);
        if (s != null) {
            s.lag = lag;
            TransactionManager.getInstance().saveOnSaveQueue(s);
        }
    }

    public void updateNick(int cid, String nick) {
        Server s = getServer(cid);
        if (s != null) {
            s.nick = nick;
            TransactionManager.getInstance().saveOnSaveQueue(s);
        }
    }

    public void updateStatus(int cid, String status, ObjectNode fail_info) {
        Server s = getServer(cid);
        if (s != null) {
            s.status = status;
            s.fail_info = fail_info;
            TransactionManager.getInstance().saveOnSaveQueue(s);
        }
    }

    public void updateAway(int cid, String away) {
        Server s = getServer(cid);
        if (s != null) {
            s.away = away;
            TransactionManager.getInstance().saveOnSaveQueue(s);
        }
    }

    public void updateUsermask(int cid, String usermask) {
        Server s = getServer(cid);
        if (s != null) {
            s.usermask = usermask;
            TransactionManager.getInstance().saveOnSaveQueue(s);
        }
    }

    public void updateMode(int cid, String mode) {
        Server s = getServer(cid);
        if (s != null) {
            s.mode = mode;
            TransactionManager.getInstance().saveOnSaveQueue(s);
        }
    }

    public void updateUserModes(int cid, String modes) {
        if (modes != null && modes.length() == 5 && modes.charAt(0) != 'q') {
            Server s = getServer(cid);
            if (s != null) {
                s.MODE_OWNER = modes.substring(0, 1);
                TransactionManager.getInstance().saveOnSaveQueue(s);
            }
        }
    }

    public void updateIsupport(int cid, ObjectNode params) {
        Server s = getServer(cid);
        if (s != null) {
            if (params != null && !params.isArray())
                s.isupport.putAll(params);
            else
                s.isupport = new ObjectMapper().createObjectNode();

            if (s.isupport.has("PREFIX")) {
                s.PREFIX = (ObjectNode) s.isupport.get("PREFIX");
            } else {
                s.PREFIX = new ObjectMapper().createObjectNode();
                s.PREFIX.put(s.MODE_OPER, "!");
                s.PREFIX.put(s.MODE_OWNER, "~");
                s.PREFIX.put(s.MODE_ADMIN, "&");
                s.PREFIX.put(s.MODE_OP, "@");
                s.PREFIX.put(s.MODE_HALFOP, "%");
                s.PREFIX.put(s.MODE_VOICED, "+");
            }
            if (s.isupport.has("CHANTYPES"))
                s.CHANTYPES = s.isupport.get("CHANTYPES").asText();
            else
                s.CHANTYPES = null;
            TransactionManager.getInstance().saveOnSaveQueue(s);
        }
    }

    public void updateIgnores(int cid, JsonNode ignores) {
        Server s = getServer(cid);
        if (s != null) {
            s.raw_ignores = ignores;
            s.ignores = new ArrayList<>();
            for (int i = 0; i < ignores.size(); i++) {
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
                if (!mask.contains("!"))
                    if (mask.contains("@"))
                        mask = ".*!" + mask;
                    else
                        mask += "!.*";
                if (!mask.contains("@"))
                    if (mask.contains("!"))
                        mask = mask.replace("!", "!.*@");
                    else
                        mask += "@.*";
                if (mask.equals(".*!.*@.*"))
                    continue;
                s.ignores.add(mask);
            }
            TransactionManager.getInstance().saveOnSaveQueue(s);
        }
    }

    public void deleteServer(int cid) {
        servers.remove(cid);
    }

    public void deleteAllDataForServer(int cid) {
        Server s = getServer(cid);
        if (s != null) {
            ArrayList<Buffer> buffersToRemove = new ArrayList<Buffer>();

            Iterator<Buffer> i = BuffersList.getInstance().getBuffersForServer(cid).iterator();
            while (i.hasNext()) {
                Buffer b = i.next();
                buffersToRemove.add(b);
            }

            i = buffersToRemove.iterator();
            while (i.hasNext()) {
                Buffer b = i.next();
                BuffersList.getInstance().deleteAllDataForBuffer(b.bid);
                Notifications.getInstance().deleteNotificationsForBid(b.bid);
            }
            servers.remove(cid);
            s.delete();
        }
    }

    public synchronized SparseArray<Server> getServers() {
        return servers;
    }

    public Server getServer(int cid) {
        return servers.get(cid);
    }

    public Server getServer(String hostname) {
        for (int i = 0; i < servers.size(); i++) {
            Server s = servers.valueAt(i);
            if (s.hostname.equalsIgnoreCase(hostname))
                return s;
        }
        return null;
    }

    public Server getServer(String hostname, int port) {
        for (int i = 0; i < servers.size(); i++) {
            Server s = servers.valueAt(i);
            if (s.hostname.equalsIgnoreCase(hostname) && s.port == port)
                return s;
        }
        return null;
    }

    public Server getServer(String hostname, boolean ssl) {
        for (int i = 0; i < servers.size(); i++) {
            Server s = servers.valueAt(i);
            if (s.hostname.equalsIgnoreCase(hostname) && ((!ssl && s.ssl == 0) || (ssl && s.ssl > 0)))
                return s;
        }
        return null;
    }
}
