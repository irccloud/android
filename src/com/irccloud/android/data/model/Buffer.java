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
import android.text.TextUtils;

import com.irccloud.android.AlphanumComparator;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.FontAwesome;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.collection.ChannelsList;
import com.irccloud.android.data.collection.ServersList;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Unique;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/*@Table(databaseName = IRCCloudDatabase.NAME,
        uniqueColumnGroups = {@UniqueGroup(groupNumber = 1, uniqueConflict = ConflictAction.REPLACE)})*/
public class Buffer extends BaseObservable /*extends ObservableBaseModel*/ {
    public static final String TYPE_CONSOLE = "console";
    public static final String TYPE_CHANNEL = "channel";
    public static final String TYPE_CONVERSATION = "conversation";
    public static final String TYPE_ARCHIVES_HEADER = "archives_header";
    public static final String TYPE_JOIN_CHANNEL = "join_channel";
    public static final String TYPE_SPAM = "spam";
    public static final String DEFAULT_CHANTYPES = "#&!+";

    private enum Type {
        CONSOLE, CHANNEL, CONVERSATION, ARCHIVES_HEADER, JOIN_CHANNEL, SPAM, OTHER
    }

    private Server server = null;
    private ColorScheme colorScheme = ColorScheme.getInstance();

    public Server getServer() {
        if(server == null)
            server = ServersList.getInstance().getServer(this.cid);

        return server;
    }

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
    private long created;

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
    private CharSequence draft;

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

    private Type type_int;

    private String displayName;

    private boolean serverIsSlack;
    private int isMPDM = -1;

    public String toString() {
        return "{cid:" + getCid() + ", bid:" + getBid() + ", name: " + getName() + ", type: " + getType() + ", archived: " + getArchived() + "}";
    }

    public String normalizedName() {
        if(isMPDM())
            return getDisplayName().toLowerCase();

        if (getChan_types() == null || getChan_types().length() < 2) {
            Server s = ServersList.getInstance().getServer(getCid());
            if (s != null && s.CHANTYPES != null && s.CHANTYPES.length() > 0)
                setChan_types(s.CHANTYPES);
            else
                setChan_types(DEFAULT_CHANTYPES);
        }
        return getName().toLowerCase().replaceAll("^[" + getChan_types() + "]+", "");
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
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    @Bindable
    public String getName() {
        return name;
    }

    @Bindable
    public CharSequence getEmojiCompatName() {
        if(Build.VERSION.SDK_INT >= 19 && EmojiCompat.get().getLoadState() == EmojiCompat.LOAD_STATE_SUCCEEDED)
            return EmojiCompat.get().process(getDisplayName());
        else
            return getDisplayName();
    }

    public void setName(String name) {
        this.name = name;
        this.displayName = null;
        this.isMPDM = -1;
        if(this.bid != -1) {
            BuffersList.getInstance().dirty = true;
        }
    }

    public boolean isMPDM() {
        if(isMPDM == -1) {
            isMPDM = (serverIsSlack && name.matches("#mpdm-(.*)-\\d")) ? 1 : 0;
        }
        return isMPDM == 1;
    }

    @Bindable
    public String getDisplayName() {
        if(isMPDM()) {
            if (displayName == null) {
                String selfNick = getServer().getNick();
                ArrayList<String> names = new ArrayList<String>(Arrays.asList(name.split("-")));
                names.remove(0);
                names.remove(names.size() - 1);
                for(int i = 0; i < names.size(); i++) {
                    String name = names.get(i);
                    if(name == null || name.length() == 0 || name.equalsIgnoreCase(selfNick)) {
                        names.remove(i);
                        i--;
                    }
                }
                Collator collator = Collator.getInstance();
                collator.setStrength(Collator.SECONDARY);
                Collections.sort(names, new AlphanumComparator(collator));
                displayName = TextUtils.join(", ", names);
            }
            return displayName;
        }
        return name;
    }

    public void setServerIsSlack(boolean serverIsSlack) {
        this.serverIsSlack = serverIsSlack;
        if(this.bid != -1) {
            BuffersList.getInstance().dirty = true;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
        switch(type) {
            case TYPE_CONSOLE:
                this.type_int = Type.CONSOLE;
                break;
            case TYPE_CHANNEL:
                this.type_int = Type.CHANNEL;
                break;
            case TYPE_CONVERSATION:
                this.type_int = Type.CONVERSATION;
                break;
            case TYPE_ARCHIVES_HEADER:
                this.type_int = Type.ARCHIVES_HEADER;
                break;
            case TYPE_JOIN_CHANNEL:
                this.type_int = Type.JOIN_CHANNEL;
                break;
            case TYPE_SPAM:
                this.type_int = Type.SPAM;
                break;
            default:
                this.type_int = Type.OTHER;
                break;
        }
    }

    public boolean isConsole() {
        return type_int == Type.CONSOLE;
    }

    @Bindable
    public boolean getIsConsole() {
        return isConsole();
    }

    public boolean isChannel() {
        return type_int == Type.CHANNEL;
    }

    @Bindable
    public boolean getIsChannel() {
        return isChannel();
    }

    public boolean isConversation() {
        return type_int == Type.CONVERSATION;
    }

    @Bindable
    public boolean getIsConversation() {
        return isConversation();
    }

    @Bindable
    public boolean getIsGroupHeading() {
        return isConsole() || type_int == Type.OTHER;
    }

    @Bindable
    public boolean getIsArchivesHeader() {
        return type_int == Type.ARCHIVES_HEADER;
    }

    public int getArchived() {
        return isConsole()?0:archived;
    }

    public void setArchived(int archived) {
        this.archived = archived;
        if(this.bid != -1) {
            BuffersList.getInstance().dirty = true;
        }
    }

    public boolean isSpam() {
        return type_int == Type.SPAM;
    }

    @Bindable
    public boolean getIsSpam() {
        return isSpam();
    }

    public int getDeferred() {
        return deferred;
    }

    public void setDeferred(int deferred) {
        this.deferred = deferred;
    }

    @Bindable
    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getAway_msg() {
        return away_msg;
    }

    public void setAway_msg(String away_msg) {
        this.away_msg = away_msg;
    }

    public CharSequence getDraft() {
        return draft;
    }

    public void setDraft(CharSequence draft) {
        this.draft = draft;
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
        JSONObject channelEnabledMap = null;
        JSONObject bufferEnabledMap = null;
        NetworkConnection conn = NetworkConnection.getInstance();

        if (conn.getUserInfo() != null && conn.getUserInfo().prefs != null) {
            try {
                if (conn.getUserInfo().prefs.has("channel-disableTrackUnread"))
                    channelDisabledMap = conn.getUserInfo().prefs.getJSONObject("channel-disableTrackUnread");
                if (conn.getUserInfo().prefs.has("buffer-disableTrackUnread"))
                    bufferDisabledMap = conn.getUserInfo().prefs.getJSONObject("buffer-disableTrackUnread");
                if (conn.getUserInfo().prefs.has("channel-enableTrackUnread"))
                    channelEnabledMap = conn.getUserInfo().prefs.getJSONObject("channel-enableTrackUnread");
                if (conn.getUserInfo().prefs.has("buffer-enableTrackUnread"))
                    bufferEnabledMap = conn.getUserInfo().prefs.getJSONObject("buffer-enableTrackUnread");
            } catch (JSONException e1) {
                NetworkConnection.printStackTraceToCrashlytics(e1);
            }
        }

        try {
            if (type.equals(TYPE_CHANNEL) && channelDisabledMap != null && channelDisabledMap.has(String.valueOf(bid)) && channelDisabledMap.getBoolean(String.valueOf(bid))) {
                return 0;
            } else if(bufferDisabledMap != null && bufferDisabledMap.has(String.valueOf(bid)) && bufferDisabledMap.getBoolean(String.valueOf(bid))) {
                return 0;
            } else if (type.equals(TYPE_CHANNEL) && channelEnabledMap != null && channelEnabledMap.has(String.valueOf(bid)) && channelEnabledMap.getBoolean(String.valueOf(bid))) {
                return unread;
            } else if(bufferEnabledMap != null && bufferEnabledMap.has(String.valueOf(bid)) && bufferEnabledMap.getBoolean(String.valueOf(bid))) {
                return unread;
            } else if(!type.equals(TYPE_CONVERSATION) && conn.getUserInfo() != null && conn.getUserInfo().prefs != null && conn.getUserInfo().prefs.has("disableTrackUnread") && conn.getUserInfo().prefs.get("disableTrackUnread") instanceof Boolean && conn.getUserInfo().prefs.getBoolean("disableTrackUnread")) {
                return 0;
            }
        } catch (JSONException e) {
            NetworkConnection.printStackTraceToCrashlytics(e);
        }
        return unread;
    }

    public void setUnread(int unread) {
        this.unread = unread;
    }

    @Bindable
    public int getHighlights() {
        return highlights;
    }

    @Bindable
    public String getHighlightsString() {
        return String.valueOf(highlights);
    }

    public void setHighlights(int highlights) {
        this.highlights = highlights;
    }

    public int getValid() {
        return valid;
    }

    public void setValid(int valid) {
        this.valid = valid;
    }

    /*@Override
    public void save() {
        if(bid != -1)
            super.save();
    }*/

    public boolean isJoined() {
        return ChannelsList.getInstance().getChannelForBuffer(bid) != null;
    }

    @Bindable public String getContentDescription() {
        if(isConversation())
            return "Conversation with " + getDisplayName();
        else
            return "Channel " + normalizedName();
    }

    @Bindable
    public int getTextColor() {
        if (type_int == Type.ARCHIVES_HEADER) {
            return colorScheme.archivesHeadingTextColor;
        } else if (type_int == Type.JOIN_CHANNEL) {
            return R.color.row_label_join;
        } else if (getArchived() == 1) {
            return (type_int == Type.CHANNEL) ? colorScheme.archivedChannelTextColor : colorScheme.archivedBufferTextColor;
        } else if (isConsole()) {
            if(getServer() != null && getServer().isFailed())
                return colorScheme.networkErrorColor;
            else if(getServer() == null || !getServer().isConnected())
                return colorScheme.inactiveBufferTextColor;
            else
                return colorScheme.bufferTextColor;
        } else if (isChannel() && !isJoined()) {
            return colorScheme.inactiveBufferTextColor;
        } else if (isSpam()) {
            return colorScheme.networkErrorColor;
        } else {
            return colorScheme.bufferTextColor;
        }
    }

    @Bindable
    public String getIcon() {
        if(isChannel()) {
            Channel c = ChannelsList.getInstance().getChannelForBuffer(bid);
            if(c != null && (c.hasMode("k") || (serverIsSlack && !isMPDM() && c.hasMode("s"))))
                return FontAwesome.LOCK;
        } else if(isSpam()) {
            return FontAwesome.EXCLAMATION_TRIANGLE;
        }
        return null;
    }

    @Bindable
    public int getBackgroundResource() {
        if(isChannel() || isConversation())
            return colorScheme.bufferBackgroundDrawable;
        else if(isSpam())
            return R.drawable.row_failed_bg;
        else if(isConsole())
            return (getServer() != null && getServer().isFailed()) ? R.drawable.row_failed_bg : colorScheme.serverBackgroundDrawable;
        else if(type_int == Type.JOIN_CHANNEL)
            return colorScheme.bufferBackgroundDrawable;
        else if(type_int == Type.ARCHIVES_HEADER)
            return (getArchived() == 0)?colorScheme.bufferBackgroundDrawable:R.drawable.archived_bg_selected;
        else
            return colorScheme.serverBackgroundDrawable;
    }

    @Bindable
    public int getSelectedBackgroundResource() {
        if(isConsole())
            return (getServer() != null && getServer().isFailed()) ? R.drawable.status_fail_bg : colorScheme.selectedBackgroundDrawable;
        else
            return colorScheme.selectedBackgroundDrawable;
    }

    @Bindable
    public int getSelectedBorderResource() {
        if(isConsole())
            return (getServer() != null && getServer().isFailed()) ? R.drawable.status_fail_bg : colorScheme.selectedBorderDrawable;
        else
            return colorScheme.selectedBorderDrawable;
    }

    @Bindable
    public int getBorderResource() {
        if(isConsole())
            return (getServer() != null && getServer().isFailed()) ? R.drawable.networkErrorBorder : colorScheme.serverBorderDrawable;
        else
            return colorScheme.bufferBorderDrawable;
    }

    @Bindable
    public int getSelectedTextColor() {
        if(type_int == Type.ARCHIVES_HEADER)
            return colorScheme.selectedArchivesHeadingColor;
        else
            return colorScheme.selectedBufferTextColor;
    }

    @Bindable
    public int getUnreadColor() { return R.drawable.row_unread_border; }

    @Bindable
    public boolean getShowSpinner() {
        if(type_int == Type.ARCHIVES_HEADER)
            return (getServer() != null && getServer().deferred_archives > 0);
        else
            return (isConsole() && getServer() != null)? getServer().isConnecting() : (this.timeout > 0);
    }

    @Bindable
    public int getSpamIconColor() {
        return 0xffff6e00;
    }
}
