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
            servers.clear();
            List<Server> c = new Select().all().from(Server.class).queryList();
            if (c != null && !c.isEmpty()) {
                for (Server s : c) {
                    servers.put(s.getCid(), s);
                    s.updateIgnores(s.raw_ignores);
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
        s.setCid(cid);
        s.setName(name);
        s.setHostname(hostname);
        s.setPort(port);
        s.setNick(nick);
        s.setStatus(status);
        s.setSsl(ssl);
        s.setRealname(realname);
        s.setServer_pass(server_pass);
        s.setNickserv_pass(nickserv_pass);
        s.setJoin_commands(join_commands);
        s.setFail_info(fail_info);
        s.setAway(away);
        s.setUsermask("");
        s.setMode("");
        s.setOrder(order);
        s.isupport = new ObjectMapper().createObjectNode();
        if (s.getName() == null || s.getName().length() == 0)
            s.setName(s.getHostname());
        if (ignores != null)
            s.updateIgnores(ignores);
        return s;
    }

    public int count() {
        return servers.size();
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
                BuffersList.getInstance().deleteAllDataForBuffer(b.getBid());
                NotificationsList.getInstance().deleteNotificationsForBid(b.getBid());
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
            if (s.getHostname().equalsIgnoreCase(hostname))
                return s;
        }
        return null;
    }

    public Server getServer(String hostname, int port) {
        for (int i = 0; i < servers.size(); i++) {
            Server s = servers.valueAt(i);
            if (s.getHostname().equalsIgnoreCase(hostname) && s.getPort() == port)
                return s;
        }
        return null;
    }

    public Server getServer(String hostname, boolean ssl) {
        for (int i = 0; i < servers.size(); i++) {
            Server s = servers.valueAt(i);
            if (s.getHostname().equalsIgnoreCase(hostname) && ((!ssl && s.getSsl() == 0) || (ssl && s.getSsl() > 0)))
                return s;
        }
        return null;
    }
}
