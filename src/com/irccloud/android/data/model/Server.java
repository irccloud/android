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

import android.content.Context;
import android.content.res.Resources;
import android.databinding.Bindable;
import android.databinding.Observable;
import android.databinding.PropertyChangeRegistry;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;

import com.irccloud.android.BR;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.R;
import com.irccloud.android.data.IRCCloudDatabase;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.util.ArrayList;

@Table(databaseName = IRCCloudDatabase.NAME)
public class Server extends BaseModel implements Comparable<Server>, android.databinding.Observable {
    @Column
    @PrimaryKey
    private int cid;

    @Column
    private String name;

    @Column
    private String hostname;

    @Column
    private int port;

    @Column
    private String nick;

    @Column
    private String status;

    @Column
    private long lag;

    @Column
    private int ssl;

    @Column
    private String realname;

    @Column
    public ObjectNode fail_info;

    @Column
    private String away;

    @Column
    private String usermask;

    @Column
    private String mode;

    @Column
    public ObjectNode isupport;

    @Column
    public JsonNode raw_ignores;

    @Column(name = "server_order")
    private int order;

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

    private String server_pass;
    private String nickserv_pass;
    private String join_commands;
    public ArrayList<String> ignores;

    @Override
    public int compareTo(Server another) {
        if (getOrder() != another.getOrder())
            return Integer.valueOf(getOrder()).compareTo(another.getOrder());
        return Integer.valueOf(getCid()).compareTo(another.getCid());
    }

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    @Bindable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        callbacks.notifyChange(this, BR.name);
    }

    @Bindable
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
        callbacks.notifyChange(this, BR.hostname);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getLag() {
        return lag;
    }

    public void setLag(long lag) {
        this.lag = lag;
    }

    public int getSsl() {
        return ssl;
    }

    public void setSsl(int ssl) {
        this.ssl = ssl;
        callbacks.notifyChange(this, BR.icon);
    }

    public String getRealname() {
        return realname;
    }

    public void setRealname(String realname) {
        this.realname = realname;
    }

    public String getAway() {
        return away;
    }

    public void setAway(String away) {
        this.away = away;
    }

    public String getUsermask() {
        return usermask;
    }

    public void setUsermask(String usermask) {
        this.usermask = usermask;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getServer_pass() {
        return server_pass;
    }

    public void setServer_pass(String server_pass) {
        this.server_pass = server_pass;
    }

    public String getNickserv_pass() {
        return nickserv_pass;
    }

    public void setNickserv_pass(String nickserv_pass) {
        this.nickserv_pass = nickserv_pass;
    }

    public String getJoin_commands() {
        return join_commands;
    }

    public void setJoin_commands(String join_commands) {
        this.join_commands = join_commands;
    }

    PropertyChangeRegistry callbacks = new PropertyChangeRegistry();

    @Override
    public void addOnPropertyChangedCallback(Observable.OnPropertyChangedCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeOnPropertyChangedCallback(Observable.OnPropertyChangedCallback callback) {
        callbacks.remove(callback);
    }

    @Bindable
    public int getIcon() {
        if(ssl > 0)
            return R.drawable.world_shield;
        else
            return R.drawable.world;
    }
}
