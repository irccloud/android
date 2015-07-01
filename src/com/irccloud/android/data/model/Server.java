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

package com.irccloud.android.data.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irccloud.android.data.IRCCloudDatabase;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.ArrayList;

@Table(databaseName = IRCCloudDatabase.NAME)
public class Server extends BaseModel implements Comparable<Server> {
    @Column
    @PrimaryKey
    public int cid;

    @Column
    public String name;

    @Column
    public String hostname;

    @Column
    public int port;

    @Column
    public String nick;

    @Column
    public String status;

    @Column
    public long lag;

    @Column
    public int ssl;

    @Column
    public String realname;

    @Column
    public ObjectNode fail_info;

    @Column
    public String away;

    @Column
    public String usermask;

    @Column
    public String mode;

    @Column
    public ObjectNode isupport;

    @Column
    public JsonNode raw_ignores;

    @Column(name = "server_order")
    public int order;

    @Column
    public String CHANTYPES;

    @Column
    public ObjectNode PREFIX;

    @Column
    public String MODE_OPER = "Y";

    @Column
    public String MODE_OWNER = "q";

    @Column
    public String MODE_ADMIN = "a";

    @Column
    public String MODE_OP = "o";

    @Column
    public String MODE_HALFOP = "h";

    @Column
    public String MODE_VOICED = "v";

    public String server_pass;
    public String nickserv_pass;
    public String join_commands;
    public ArrayList<String> ignores;

    @Override
    public int compareTo(Server another) {
        if (order != another.order)
            return Integer.valueOf(order).compareTo(another.order);
        return Integer.valueOf(cid).compareTo(another.cid);
    }
}
