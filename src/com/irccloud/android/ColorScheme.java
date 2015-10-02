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

package com.irccloud.android;

import android.content.Context;
import android.content.res.ColorStateList;
import android.preference.PreferenceManager;
import android.util.TypedValue;

public class ColorScheme {
    private static ColorScheme instance = new ColorScheme();

    public static ColorScheme getInstance() {
        return instance;
    }

    public static String getUserTheme() {
        return PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getString("theme", "dusk");
    }

    public static int getTheme(String theme, boolean actionbar) {
        switch(theme) {
            case "dusk":
                return actionbar?R.style.dusk:R.style.duskNoActionBar;
            case "tropic":
                return actionbar?R.style.tropic:R.style.tropicNoActionBar;
            case "emerald":
                return actionbar?R.style.emerald:R.style.emeraldNoActionBar;
            case "sand":
                return actionbar?R.style.sand:R.style.sandNoActionBar;
            case "rust":
                return actionbar?R.style.rust:R.style.rustNoActionBar;
            case "orchid":
                return actionbar?R.style.orchid:R.style.orchidNoActionBar;
            case "ash":
                return actionbar?R.style.ash:R.style.ashNoActionBar;
            default:
                return actionbar?R.style.dusk:R.style.duskNoActionBar;
        }
    }

    public static int getPrefsTheme(String theme) {
        switch(theme) {
            case "dusk":
                return R.style.duskPrefsTheme;
            case "tropic":
                return R.style.tropicPrefsTheme;
            case "emerald":
                return R.style.emeraldPrefsTheme;
            case "sand":
                return R.style.sandPrefsTheme;
            case "rust":
                return R.style.rustPrefsTheme;
            case "orchid":
                return R.style.orchidPrefsTheme;
            case "ash":
                return R.style.ashPrefsTheme;
            default:
                return R.style.duskPrefsTheme;
        }
    }

    public void setThemeFromContext(Context ctx, String theme_name) {
        theme = theme_name;
        contentBackgroundColor = colorForAttribute(ctx, R.attr.contentBackgroundColor);
        messageTextColor = colorForAttribute(ctx, R.attr.messageTextColor);
        opersGroupColor = colorForAttribute(ctx, R.attr.opersGroupColor);
        ownersGroupColor = colorForAttribute(ctx, R.attr.ownersGroupColor);
        adminsGroupColor = colorForAttribute(ctx, R.attr.adminsGroupColor);
        opsGroupColor = colorForAttribute(ctx, R.attr.opsGroupColor);
        halfopsGroupColor = colorForAttribute(ctx, R.attr.halfopsGroupColor);
        voicedGroupColor = colorForAttribute(ctx, R.attr.voicedGroupColor);
        membersGroupColor = colorForAttribute(ctx, R.attr.membersGroupColor);
        opersHeadingColor = colorForAttribute(ctx, R.attr.opersHeadingColor);
        ownersHeadingColor = colorForAttribute(ctx, R.attr.ownersHeadingColor);
        adminsHeadingColor = colorForAttribute(ctx, R.attr.adminsHeadingColor);
        opsHeadingColor = colorForAttribute(ctx, R.attr.opsHeadingColor);
        halfopsHeadingColor = colorForAttribute(ctx, R.attr.halfopsHeadingColor);
        voicedHeadingColor = colorForAttribute(ctx, R.attr.voicedHeadingColor);
        membersHeadingColor = colorForAttribute(ctx, R.attr.membersHeadingColor);
        memberListTextColor = colorForAttribute(ctx, R.attr.memberListTextColor);
        memberListAwayTextColor = colorForAttribute(ctx, R.attr.memberListAwayTextColor);
        timestampColor = colorForAttribute(ctx, R.attr.timestampColor);
        darkBlueColor = colorForAttribute(ctx, R.attr.darkBlueColor);
        networkErrorBackgroundColor = colorForAttribute(ctx, R.attr.networkErrorBackgroundColor);
        networkErrorColor = colorForAttribute(ctx, R.attr.networkErrorColor);
        errorBackgroundColor = colorForAttribute(ctx, R.attr.errorBackgroundColor);
        statusBackgroundColor = colorForAttribute(ctx, R.attr.statusBackgroundColor);
        selfBackgroundColor = colorForAttribute(ctx, R.attr.selfBackgroundColor);
        highlightBackgroundColor = colorForAttribute(ctx, R.attr.highlightBackgroundColor);
        highlightTimestampColor = colorForAttribute(ctx, R.attr.highlightTimestampColor);
        noticeBackgroundColor = colorForAttribute(ctx, R.attr.noticeBackgroundColor);
        timestampBackgroundColor = colorForAttribute(ctx, R.attr.timestampBackgroundColor);
        newMsgsBackgroundColor = colorForAttribute(ctx, R.attr.newMsgsBackgroundColor);
        collapsedRowTextColor = colorForAttribute(ctx, R.attr.collapsedRowTextColor);
        collapsedHeadingBackgroundColor = colorForAttribute(ctx, R.attr.collapsedHeadingBackgroundColor);
        navBarColor = colorForAttribute(ctx, R.attr.navBarColor);
        navBarHeadingColor = colorForAttribute(ctx, R.attr.navBarHeadingColor);
        navBarSubheadingColor = colorForAttribute(ctx, R.attr.navBarSubheadingColor);
        navBarBorderColor = colorForAttribute(ctx, R.attr.navBarBorderColor);
        textareaTextColor = colorForAttribute(ctx, R.attr.textareaTextColor);
        textareaBackgroundColor = colorForAttribute(ctx, R.attr.textareaBackgroundColor);
        linkColor = colorForAttribute(ctx, R.attr.linkColor);
        lightLinkColor = colorForAttribute(ctx, R.attr.lightLinkColor);
        serverBackgroundColor = colorForAttribute(ctx, R.attr.serverBackgroundColor);
        bufferBackgroundColor = colorForAttribute(ctx, R.attr.bufferBackgroundColor);
        unreadBorderColor = colorForAttribute(ctx, R.attr.unreadBorderColor);
        highlightBorderColor = colorForAttribute(ctx, R.attr.highlightBorderColor);
        networkErrorBorderColor = colorForAttribute(ctx, R.attr.networkErrorBorderColor);
        bufferTextColor = colorForAttribute(ctx, R.attr.bufferTextColor);
        inactiveBufferTextColor = colorForAttribute(ctx, R.attr.inactiveBufferTextColor);
        unreadBufferTextColor = colorForAttribute(ctx, R.attr.unreadBufferTextColor);
        selectedBufferTextColor = colorForAttribute(ctx, R.attr.selectedBufferTextColor);
        selectedBufferBackgroundColor = colorForAttribute(ctx, R.attr.selectedBufferBackgroundColor);
        bufferBorderColor = colorForAttribute(ctx, R.attr.bufferBorderColor);
        selectedBufferBorderColor = colorForAttribute(ctx, R.attr.selectedBufferBorderColor);
        backlogDividerColor = colorForAttribute(ctx, R.attr.backlogDividerColor);
        chatterBarTextColor = colorForAttribute(ctx, R.attr.chatterBarTextColor);
        chatterBarColor = colorForAttribute(ctx, R.attr.chatterBarColor);
        awayBarTextColor = colorForAttribute(ctx, R.attr.awayBarTextColor);
        awayBarColor = colorForAttribute(ctx, R.attr.awayBarColor);
        connectionBarTextColor = colorForAttribute(ctx, R.attr.connectionBarTextColor);
        connectionBarColor = colorForAttribute(ctx, R.attr.connectionBarColor);
        placeholderColor = colorForAttribute(ctx, R.attr.placeholderColor);
        unreadBlueColor = colorForAttribute(ctx, R.attr.unreadBlueColor);
        serverBorderColor = colorForAttribute(ctx, R.attr.serverBorderColor);
        failedServerBorderColor = colorForAttribute(ctx, R.attr.failedServerBorderColor);
        archivesHeadingTextColor = colorForAttribute(ctx, R.attr.archivesHeadingTextColor);
        archivedChannelTextColor = colorForAttribute(ctx, R.attr.archivedChannelTextColor);
        archivedBufferTextColor = colorForAttribute(ctx, R.attr.archivedBufferTextColor);
        selectedArchivesHeadingColor = colorForAttribute(ctx, R.attr.selectedArchivesHeadingColor);
        timestampTopBorderColor = colorForAttribute(ctx, R.attr.timestampTopBorderColor);
        timestampBottomBorderColor = colorForAttribute(ctx, R.attr.timestampBottomBorderColor);
        expandCollapseIndicatorColor = colorForAttribute(ctx, R.attr.expandCollapseIndicatorColor);
        bufferHighlightColor = colorForAttribute(ctx, R.attr.bufferHighlightColor);
        selectedBufferHighlightColor = colorForAttribute(ctx, R.attr.selectedBufferHighlightColor);
        archivedBufferHighlightColor = colorForAttribute(ctx, R.attr.archivedBufferHighlightColor);
        selectedArchivedBufferHighlightColor = colorForAttribute(ctx, R.attr.selectedArchivedBufferHighlightColor);
        selectedArchivedBufferBackgroundColor = colorForAttribute(ctx, R.attr.selectedArchivedBufferBackgroundColor);
        contentBorderColor = colorForAttribute(ctx, R.attr.contentBorderColor);
        bufferBorderDrawable = resourceForAttribute(ctx, R.attr.bufferBorderDrawable);
        serverBorderDrawable = resourceForAttribute(ctx, R.attr.serverBorderDrawable);
        selectedBorderDrawable = resourceForAttribute(ctx, R.attr.selectedBorderDrawable);
        bufferBackgroundDrawable = resourceForAttribute(ctx, R.attr.bufferBackgroundDrawable);
        serverBackgroundDrawable = resourceForAttribute(ctx, R.attr.serverBackgroundDrawable);
        selectedBackgroundDrawable = resourceForAttribute(ctx, R.attr.selectedBackgroundDrawable);
        lastSeenEIDBackgroundDrawable = resourceForAttribute(ctx, R.attr.lastSeenEIDBackgroundDrawable);
        socketclosedBackgroundDrawable = resourceForAttribute(ctx, R.attr.socketclosedBackgroundDrawable);
        timestampBackgroundDrawable = resourceForAttribute(ctx, R.attr.timestampBackgroundDrawable);
        actionBarDrawable = resourceForAttribute(ctx, R.attr.actionbarDrawable);
        colorControlNormal = colorForAttribute(ctx, R.attr.colorControlNormal);
        dialogBackgroundColor = colorForAttribute(ctx, R.attr.dialogBackgroundColor);
        isDarkTheme = colorForAttribute(ctx, R.attr.isDarkTheme) != 0;
    }

    private int colorForAttribute(Context ctx, int attribute) {
        TypedValue v = new TypedValue();
        ctx.getTheme().resolveAttribute(attribute, v, true);
        return v.data;
    }

    private int resourceForAttribute(Context ctx, int attribute) {
        TypedValue v = new TypedValue();
        ctx.getTheme().resolveAttribute(attribute, v, true);
        return v.resourceId;
    }

    public int contentBackgroundColor;
    public int messageTextColor;
    public int opersGroupColor;
    public int ownersGroupColor;
    public int adminsGroupColor;
    public int opsGroupColor;
    public int halfopsGroupColor;
    public int voicedGroupColor;
    public int membersGroupColor;
    public int opersHeadingColor;
    public int ownersHeadingColor;
    public int adminsHeadingColor;
    public int opsHeadingColor;
    public int halfopsHeadingColor;
    public int voicedHeadingColor;
    public int membersHeadingColor;
    public int memberListTextColor;
    public int memberListAwayTextColor;
    public int timestampColor;
    public int darkBlueColor;
    public int networkErrorBackgroundColor;
    public int networkErrorColor;
    public int errorBackgroundColor;
    public int statusBackgroundColor;
    public int selfBackgroundColor;
    public int highlightBackgroundColor;
    public int highlightTimestampColor;
    public int noticeBackgroundColor;
    public int timestampBackgroundColor;
    public int newMsgsBackgroundColor;
    public int collapsedRowTextColor;
    public int collapsedHeadingBackgroundColor;
    public int navBarColor;
    public int navBarHeadingColor;
    public int navBarSubheadingColor;
    public int navBarBorderColor;
    public int textareaTextColor;
    public int textareaBackgroundColor;
    public int linkColor;
    public int lightLinkColor;
    public int serverBackgroundColor;
    public int bufferBackgroundColor;
    public int unreadBorderColor;
    public int highlightBorderColor;
    public int networkErrorBorderColor;
    public int bufferTextColor;
    public int inactiveBufferTextColor;
    public int unreadBufferTextColor;
    public int selectedBufferTextColor;
    public int selectedBufferBackgroundColor;
    public int bufferBorderColor;
    public int selectedBufferBorderColor;
    public int backlogDividerColor;
    public int chatterBarTextColor;
    public int chatterBarColor;
    public int awayBarTextColor;
    public int awayBarColor;
    public int connectionBarTextColor;
    public int connectionBarColor;
    public int placeholderColor;
    public int unreadBlueColor;
    public int serverBorderColor;
    public int failedServerBorderColor;
    public int archivesHeadingTextColor;
    public int archivedChannelTextColor;
    public int archivedBufferTextColor;
    public int selectedArchivesHeadingColor;
    public int timestampTopBorderColor;
    public int timestampBottomBorderColor;
    public int expandCollapseIndicatorColor;
    public int bufferHighlightColor;
    public int selectedBufferHighlightColor;
    public int archivedBufferHighlightColor;
    public int selectedArchivedBufferHighlightColor;
    public int selectedArchivedBufferBackgroundColor;
    public int contentBorderColor;
    public int bufferBorderDrawable;
    public int serverBorderDrawable;
    public int selectedBorderDrawable;
    public int bufferBackgroundDrawable;
    public int serverBackgroundDrawable;
    public int selectedBackgroundDrawable;
    public int lastSeenEIDBackgroundDrawable;
    public int socketclosedBackgroundDrawable;
    public int timestampBackgroundDrawable;
    public int colorControlNormal;
    public int actionBarDrawable;
    public int dialogBackgroundColor;
    public String theme;
    public boolean isDarkTheme;
}
