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

import android.databinding.Bindable;
import android.databinding.PropertyChangeRegistry;

import com.irccloud.android.BR;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.collection.ChannelsList;
import com.irccloud.android.data.collection.ServersList;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ConflictAction;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.annotation.Unique;
import com.raizlabs.android.dbflow.annotation.UniqueGroup;
import com.raizlabs.android.dbflow.runtime.TransactionManager;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.json.JSONException;
import org.json.JSONObject;

@Table(databaseName = IRCCloudDatabase.NAME,
        uniqueColumnGroups = {@UniqueGroup(groupNumber = 1, uniqueConflict = ConflictAction.REPLACE)})
public class Buffer extends BaseModel implements android.databinding.Observable {
    public static final String TYPE_CONSOLE = "console";
    public static final String TYPE_CHANNEL = "channel";
    public static final String TYPE_CONVERSATION = "conversation";
    public static final String TYPE_ARCHIVES_HEADER = "archives_header";
    public static final String TYPE_JOIN_CHANNEL = "join_channel";
    public static final String TYPE_ADD_NETWORK = "add_network";
    public static final String TYPE_REORDER = "reorder";

    @Column
    @PrimaryKey
    @Unique(unique = false, uniqueGroups = 1)
    private int bid = -1;

    @Column
    @PrimaryKey
    @Unique(unique = false, uniqueGroups = 1)
    private int cid = -1;

    @Column
    private long min_eid;

    @Column
    private long last_seen_eid;

    @Column
    private String name;

    @Column
    private String type;

    @Column
    private int archived;

    @Column
    private int deferred;

    @Column
    private int timeout;

    @Column
    private String away_msg;

    @Column
    private String draft;

    @Column
    private String chan_types;

    @Column
    private boolean scrolledUp;

    @Column
    private int scrollPosition;

    @Column
    private int scrollPositionOffset;

    @Column
    private int unread;

    @Column
    private int highlights;

    private int valid = 1;

    public String toString() {
        return "{cid:" + getCid() + ", bid:" + getBid() + ", name: " + getName() + ", type: " + getType() + ", archived: " + getArchived() + "}";
    }

    public String normalizedName() {
        if (getChan_types() == null || getChan_types().length() < 2) {
            Server s = ServersList.getInstance().getServer(getCid());
            if (s != null && s.CHANTYPES != null && s.CHANTYPES.length() > 0)
                setChan_types(s.CHANTYPES);
            else
                setChan_types("#");
        }
        return getName().replaceAll("^[" + getChan_types() + "]+", "");
    }


    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public long getMin_eid() {
        return min_eid;
    }

    public void setMin_eid(long min_eid) {
        this.min_eid = min_eid;
    }

    public long getLast_seen_eid() {
        return last_seen_eid;
    }

    public void setLast_seen_eid(long last_seen_eid) {
        if(this.last_seen_eid < last_seen_eid)
            this.last_seen_eid = last_seen_eid;
        if(this.bid != -1)
            TransactionManager.getInstance().saveOnSaveQueue(this);
    }

    @Bindable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if(this.bid != -1) {
            BuffersList.getInstance().dirty = true;
            TransactionManager.getInstance().saveOnSaveQueue(this);
        }
        callbacks.notifyChange(this, BR.name);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getArchived() {
        return archived;
    }

    public void setArchived(int archived) {
        this.archived = archived;
        if(this.bid != -1) {
            BuffersList.getInstance().dirty = true;
            TransactionManager.getInstance().saveOnSaveQueue(this);
        }
    }

    public int getDeferred() {
        return deferred;
    }

    public void setDeferred(int deferred) {
        this.deferred = deferred;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
        if(this.bid != -1)
            TransactionManager.getInstance().saveOnSaveQueue(this);
    }

    public String getAway_msg() {
        return away_msg;
    }

    public void setAway_msg(String away_msg) {
        this.away_msg = away_msg;
        if(this.bid != -1)
            TransactionManager.getInstance().saveOnSaveQueue(this);
    }

    public String getDraft() {
        return draft;
    }

    public void setDraft(String draft) {
        this.draft = draft;
        if(this.bid != -1)
            TransactionManager.getInstance().saveOnSaveQueue(this);
    }

    public String getChan_types() {
        return chan_types;
    }

    public void setChan_types(String chan_types) {
        this.chan_types = chan_types;
    }

    public boolean getScrolledUp() {
        return scrolledUp;
    }

    public void setScrolledUp(boolean scrolledUp) {
        this.scrolledUp = scrolledUp;
    }

    public int getScrollPosition() {
        return scrollPosition;
    }

    public void setScrollPosition(int scrollPosition) {
        this.scrollPosition = scrollPosition;
    }

    public int getScrollPositionOffset() {
        return scrollPositionOffset;
    }

    public void setScrollPositionOffset(int scrollPositionOffset) {
        this.scrollPositionOffset = scrollPositionOffset;
    }

    @Bindable
    public int getUnread() {
        JSONObject channelDisabledMap = null;
        JSONObject bufferDisabledMap = null;
        NetworkConnection conn = NetworkConnection.getInstance();

        if (conn != null && conn.getUserInfo() != null && conn.getUserInfo().prefs != null) {
            try {
                if (conn.getUserInfo().prefs.has("channel-disableTrackUnread"))
                    channelDisabledMap = conn.getUserInfo().prefs.getJSONObject("channel-disableTrackUnread");
                if (conn.getUserInfo().prefs.has("buffer-disableTrackUnread"))
                    bufferDisabledMap = conn.getUserInfo().prefs.getJSONObject("buffer-disableTrackUnread");
            } catch (JSONException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }

        try {
            if (type.equals(TYPE_CHANNEL) && channelDisabledMap != null && channelDisabledMap.has(String.valueOf(bid)) && channelDisabledMap.getBoolean(String.valueOf(bid))) {
                return 0;
            } else if(bufferDisabledMap != null && bufferDisabledMap.has(String.valueOf(bid)) && bufferDisabledMap.getBoolean(String.valueOf(bid))) {
                return 0;
            }
            if (conn != null && conn.getState() == NetworkConnection.STATE_CONNECTED && conn.ready) {
                return unread;
            } else {
                return 0;
            }
        } catch (JSONException e) {
            return 0;
        }
    }

    public void setUnread(int unread) {
        this.unread = unread;
        if(this.bid != -1)
            TransactionManager.getInstance().saveOnSaveQueue(this);
        callbacks.notifyChange(this, BR.unread);
    }

    @Bindable
    public int getHighlights() {
        return highlights;
    }

    public void setHighlights(int highlights) {
        this.highlights = highlights;
        if(this.bid != -1)
            TransactionManager.getInstance().saveOnSaveQueue(this);
        callbacks.notifyChange(this, BR.highlights);
    }

    public int getValid() {
        return valid;
    }

    public void setValid(int valid) {
        this.valid = valid;
    }

    @Override
    public void save() {
        if(bid != -1)
            super.save();
    }

    public boolean isJoined() {
        return ChannelsList.getInstance().getChannelForBuffer(bid) != null;
    }

    @Bindable public String getContentDescription() {
        if(type.equals(TYPE_CHANNEL))
            return "Channel " + normalizedName();
        else
            return "Conversation with " + normalizedName();
    }

    PropertyChangeRegistry callbacks = new PropertyChangeRegistry();

    @Override
    public void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        callbacks.remove(callback);
    }

    @Bindable
    public int getTextColor() {
        if (type.equals(TYPE_ARCHIVES_HEADER)) {
            return R.color.row_label_archives_heading;
        } else if (type.equals(TYPE_JOIN_CHANNEL)) {
            return R.color.row_label_join;
        } else if (archived == 1) {
            return R.color.row_label_archived;
        } else if (type.equals(TYPE_CHANNEL) && !isJoined()) {
            return R.color.row_label_inactive;
        } else if (unread > 0) {
            return R.color.row_label_unread;
        } else {
            return R.color.row_label;
        }
    }

    @Bindable
    public int getIcon() {
        switch(type) {
            case TYPE_JOIN_CHANNEL:
                return R.drawable.add;
            case TYPE_REORDER:
                return R.drawable.move;
            case TYPE_ADD_NETWORK:
                return R.drawable.world_add;
            case TYPE_CHANNEL:
                Channel c = ChannelsList.getInstance().getChannelForBuffer(bid);
                if(c != null && c.hasMode("k"))
                    return R.drawable.lock;
                break;
            default:
                break;
        }
        return 0;
    }

    @Bindable
    public int getBackgroundResource() {
        if(type.equals(TYPE_CHANNEL) || type.equals(TYPE_CONVERSATION) || type.equals(TYPE_JOIN_CHANNEL))
            return (archived == 0)?R.drawable.row_buffer_bg:R.drawable.row_buffer_bg_archived;
        else if(type.equals(TYPE_ARCHIVES_HEADER))
            return (archived == 0)?R.drawable.row_buffer_bg:R.drawable.archived_bg_selected;
        else
            return R.drawable.row_buffergroup_bg;
    }

    @Bindable
    public int getSelectedBackgroundResource() {
        if(archived == 0)
            return R.drawable.selected_blue;
        else
            return R.drawable.archived_bg_selected;
    }
}
