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
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.TypedValue;

public class ColorScheme {
    private static ColorScheme instance = new ColorScheme();

    public static ColorScheme getInstance() {
        return instance;
    }

    public static String getUserTheme() {
        return PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getString("theme", "dawn");
    }

    public static int getIRCColor(int color, boolean background) {
        String s = ColorFormatter.COLOR_MAP[color];

        if(getInstance().isDarkTheme && !background) {
            if(ColorFormatter.DARK_FG_SUBSTITUTIONS.containsKey(s))
                s = ColorFormatter.DARK_FG_SUBSTITUTIONS.get(s);
        }

        return 0xff000000 + Integer.parseInt(s, 16);
    }

    public static int getTheme(String theme, boolean actionbar) {
        switch(theme) {
            case "dawn":
                return actionbar?R.style.dawn:R.style.dawnNoActionBar;
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
            case "midnight":
                return actionbar?R.style.midnight:R.style.midnightNoActionBar;
            default:
                return actionbar?R.style.dawn:R.style.dawnNoActionBar;
        }
    }

    public static int getDialogTheme(String theme) {
        switch(theme) {
            case "dawn":
                return R.style.dawnDialog;
            case "dusk":
                return R.style.duskDialog;
            case "tropic":
                return R.style.tropicDialog;
            case "emerald":
                return R.style.emeraldDialog;
            case "sand":
                return R.style.sandDialog;
            case "rust":
                return R.style.rustDialog;
            case "orchid":
                return R.style.orchidDialog;
            case "ash":
                return R.style.ashDialog;
            case "midnight":
                return R.style.midnightDialog;
            default:
                return R.style.dawnDialog;
        }
    }

    public static int getDialogWhenLargeTheme(String theme) {
        switch(theme) {
            case "dawn":
                return R.style.dawnDialogWhenLarge;
            case "dusk":
                return R.style.duskDialogWhenLarge;
            case "tropic":
                return R.style.tropicDialogWhenLarge;
            case "emerald":
                return R.style.emeraldDialogWhenLarge;
            case "sand":
                return R.style.sandDialogWhenLarge;
            case "rust":
                return R.style.rustDialogWhenLarge;
            case "orchid":
                return R.style.orchidDialogWhenLarge;
            case "ash":
                return R.style.ashDialogWhenLarge;
            case "midnight":
                return R.style.midnightDialogWhenLarge;
            default:
                return R.style.dawnDialogWhenLarge;
        }
    }

    private static String[] light_nick_colors = {
            "b22222",
            "d2691e",
            "ff9166",
            "fa8072",
            "ff8c00",
            "228b22",
            "808000",
            "b7b05d",
            "8ebd2e",
            "2ebd2e",
            "82b482",
            "37a467",
            "57c8a1",
            "1da199",
            "579193",
            "008b8b",
            "00bfff",
            "4682b4",
            "1e90ff",
            "4169e1",
            "6a5acd",
            "7b68ee",
            "9400d3",
            "8b008b",
            "ba55d3",
            "ff00ff",
            "ff1493"
    };

    private static String dark_nick_colors[] = {
            "deb887",
            "ffd700",
            "ff9166",
            "fa8072",
            "ff8c00",
            "00ff00",
            "ffff00",
            "bdb76b",
            "9acd32",
            "32cd32",
            "8fbc8f",
            "3cb371",
            "66cdaa",
            "20b2aa",
            "40e0d0",
            "00ffff",
            "00bfff",
            "87ceeb",
            "339cff",
            "6495ed",
            "b2a9e5",
            "ff69b4",
            "da70d6",
            "ee82ee",
            "d68fff",
            "ff00ff",
            "ffb6c1"
    };

    public static String colorForNick(String nick, boolean isDarkTheme) {
        String colors[] = isDarkTheme ? dark_nick_colors : light_nick_colors;
        // Normalise a bit
        // typically ` and _ are used on the end alone
        String normalizedNick = nick.toLowerCase().replaceAll("[`_]+$", "");
        //remove |<anything> from the end
        normalizedNick = normalizedNick.replaceAll("\\|.*$", "");

        Double hash = 0.0;

        for (int i = 0; i < normalizedNick.length(); i++) {
            hash = ((int) normalizedNick.charAt(i)) + (double) ((int) (hash.longValue()) << 6) + (double) ((int) (hash.longValue()) << 16) - hash;
        }

        return colors[(int) Math.abs(hash.longValue() % colors.length)];
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
        collapsedRowNickColor = colorForAttribute(ctx, R.attr.collapsedRowNickColor);
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
        windowBackgroundDrawable = resourceForAttribute(ctx, R.attr.windowBackgroundDrawable);
        row_opers_bg_drawable = resourceForAttribute(ctx, R.attr.row_opers_bg_drawable);
        row_owners_bg_drawable = resourceForAttribute(ctx, R.attr.row_owners_bg_drawable);
        row_admins_bg_drawable = resourceForAttribute(ctx, R.attr.row_admins_bg_drawable);
        row_ops_bg_drawable = resourceForAttribute(ctx, R.attr.row_ops_bg_drawable);
        row_halfops_bg_drawable = resourceForAttribute(ctx, R.attr.row_halfops_bg_drawable);
        row_voiced_bg_drawable = resourceForAttribute(ctx, R.attr.row_voiced_bg_drawable);
        row_members_bg_drawable = resourceForAttribute(ctx, R.attr.row_members_bg_drawable);
        codeSpanForegroundColor = colorForAttribute(ctx, R.attr.codeSpanForegroundColor);
        codeSpanBackgroundColor = colorForAttribute(ctx, R.attr.codeSpanBackgroundColor);
        if(Build.VERSION.SDK_INT >= 21)
            statusBarColor = colorForAttribute(ctx, android.R.attr.statusBarColor);
        isDarkTheme = !theme.equals("dawn");
        selfTextColor = isDarkTheme?"ffffff":"142b43";
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
    public int collapsedRowNickColor;
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
    public int windowBackgroundDrawable;
    public int statusBarColor;
    public int row_opers_bg_drawable;
    public int row_owners_bg_drawable;
    public int row_admins_bg_drawable;
    public int row_ops_bg_drawable;
    public int row_halfops_bg_drawable;
    public int row_voiced_bg_drawable;
    public int row_members_bg_drawable;
    public String theme;
    public boolean isDarkTheme;
    public String selfTextColor;
    public int codeSpanForegroundColor;
    public int codeSpanBackgroundColor;
}
