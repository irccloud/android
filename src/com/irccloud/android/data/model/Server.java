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

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import android.os.Build;
import androidx.emoji.text.EmojiCompat;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irccloud.android.FontAwesome;
import com.irccloud.android.Ignore;
import com.irccloud.android.data.collection.BuffersList;

public class Server extends BaseObservable implements Comparable<Server> {
    private int cid = -1;

    private String name;

    private String hostname;

    private int port;

    private String nick;

    private String from;

    private String avatar;

    private String avatar_url;

    private String status;

    private int ssl;

    private String realname;

    private String server_realname;

    private ObjectNode fail_info;

    private String away;

    private String usermask;

    private String mode;

    private String ircserver;

    private int orgId;

    private int avatars_supported;

    private int slack;

    public ObjectNode isupport = new ObjectMapper().createObjectNode();

    public JsonNode raw_ignores;

    //@Column(name = "server_order")
    private int order;

    public String CHANTYPES;

    public ObjectNode PREFIX;

    public String MODE_OPER = "Y";

    public String MODE_OWNER = "q";

    public String MODE_ADMIN = "a";

    public String MODE_OP = "o";

    public String MODE_HALFOP = "h";

    public String MODE_VOICED = "v";

    private String server_pass;
    private String nickserv_pass;
    private String join_commands;
    public Ignore ignores = new Ignore();
    public int deferred_archives;
    private int isSlack = -1;

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

    @Bindable
    public CharSequence getEmojiCompatName() {
        if(Build.VERSION.SDK_INT >= 19 && EmojiCompat.get().getLoadState() == EmojiCompat.LOAD_STATE_SUCCEEDED)
            return EmojiCompat.get().process(name);
        else
            return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Bindable
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
        isSlack = -1;
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

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getAvatarURL() {
        return avatar_url;
    }

    public void setAvatarURL(String avatar_url) {
        this.avatar_url = avatar_url;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ObjectNode getFail_info() {
        return fail_info;
    }

    public void setFail_info(ObjectNode fail_info) {
        this.fail_info = fail_info;
    }

    public boolean isConnecting() {
        return this.status.equals("waiting_to_retry") || this.status.equals("queued") || this.status.equals("connecting") || this.status.equals("connected_joining");
    }

    @Bindable
    public boolean getIsConnecting() {
        return isConnecting();
    }

    public boolean isConnected() {
        return this.status.equals("connected_ready");
    }

    @Bindable
    public boolean getIsConnected() {
        return isConnected();
    }

    public boolean isFailed() {
        return (this.status.equals("waiting_to_retry") || this.status.equals("pool_unavailable") || (this.status.equals("disconnected") && this.getFail_info() != null && this.getFail_info().has("type")));
    }

    @Bindable
    public boolean getIsFailed() {
        return isFailed();
    }

    public int getSsl() {
        return ssl;
    }

    public void setSsl(int ssl) {
        this.ssl = ssl;
    }

    public String getRealname() {
        return realname;
    }

    public void setRealname(String realname) {
        this.realname = realname;
    }

    public String getServerRealname() {
        return server_realname;
    }

    public void setServerRealname(String server_realname) {
        this.server_realname = server_realname;
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

    public String getIRCServer() {
        return ircserver;
    }

    public void setIRCServer(String ircserver) {
        this.ircserver = ircserver;
        isSlack = -1;
    }

    public int getOrgId() {
        return orgId;
    }

    public void setOrgId(int orgId) {
        this.orgId = orgId;
    }

    public int getAvatars_supported() {
        return avatars_supported;
    }

    public void setAvatars_supported(int avatars_supported) {
        this.avatars_supported = avatars_supported;
    }

    public int getSlack() {
        return slack;
    }

    public void setSlack(int slack) {
        this.slack = slack;
    }

    public void updateUserModes(String modes) {
        if (modes != null && modes.length() > 0) {
            if(isupport != null && isupport.has("OWNER") && isupport.get("OWNER").asText().equals(modes.substring(0,1))) {
                MODE_OWNER = modes.substring(0, 1);
                if(MODE_OPER.equalsIgnoreCase(MODE_OWNER))
                    MODE_OPER = "";
            }
        }
    }

    public void updateIsupport(ObjectNode params) {
        if(isupport == null)
            isupport = new ObjectMapper().createObjectNode();

        if (params != null && !params.isArray())
            isupport.putAll(params);
        else
            isupport = new ObjectMapper().createObjectNode();

        if (isupport.has("PREFIX")) {
            PREFIX = (ObjectNode) isupport.get("PREFIX");
        } else {
            PREFIX = new ObjectMapper().createObjectNode();
            PREFIX.put(MODE_OPER, "!");
            PREFIX.put(MODE_OWNER, "~");
            PREFIX.put(MODE_ADMIN, "&");
            PREFIX.put(MODE_OP, "@");
            PREFIX.put(MODE_HALFOP, "%");
            PREFIX.put(MODE_VOICED, "+");
        }
        if (isupport.has("CHANTYPES"))
            CHANTYPES = isupport.get("CHANTYPES").asText();
        else
            CHANTYPES = Buffer.DEFAULT_CHANTYPES;

        for(Buffer b : BuffersList.getInstance().getBuffersForServer(cid)) {
            b.setChan_types(CHANTYPES);
        }
    }

    public void updateIgnores(JsonNode ignores) {
        this.raw_ignores = ignores;
        this.ignores.clear();
        if(ignores != null) {
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
                this.ignores.addMask(mask);
            }
        }
    }

    @Bindable
    public String getIcon() {
        if(isSlack())
            return FontAwesome.SLACK;
        else if(ssl > 0)
            return FontAwesome.SHIELD;
        else
            return FontAwesome.GLOBE;
    }

    public boolean isSlack() {
        if(isSlack == -1)
            isSlack = (slack == 1 || (hostname != null && hostname.endsWith(".slack.com")) || (ircserver != null && ircserver.endsWith(".slack.com"))) ? 1 : 0;
        return isSlack == 1;
    }

    public String getSlackBaseURL() {
        String host = hostname;
        if(host == null || !host.endsWith(".slack.com"))
            host = ircserver;
        if(host != null && host.endsWith(".slack.com"))
            return "https://" + host;
        return null;
    }

    /*@Override
    public void save() {
        if(cid != -1)
            super.save();
    }*/
}
