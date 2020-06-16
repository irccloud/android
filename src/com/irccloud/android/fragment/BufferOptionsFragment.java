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

package com.irccloud.android.fragment;



import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.irccloud.android.BuildConfig;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.IRCCloudLog;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;

import org.json.JSONException;
import org.json.JSONObject;@SuppressLint("ValidFragment")
public class BufferOptionsFragment extends DialogFragment {
    private SwitchCompat members;
    private SwitchCompat unread;
    private SwitchCompat joinpart;
    private SwitchCompat collapse;
    private SwitchCompat notifyAll;
    private SwitchCompat autosuggest;
    private SwitchCompat readOnSelect;
    private SwitchCompat inlineFiles;
    private SwitchCompat inlineImages;
    private SwitchCompat replyCollapse;
    private SwitchCompat expandDisco;
    private SwitchCompat muted;
    private SwitchCompat formatColors;
    private int cid;
    private int bid;
    private String type;
    private String pref_type;

    public BufferOptionsFragment() {
        cid = bid = -1;
        type = null;
    }

    public BufferOptionsFragment(int cid, int bid, String type) {
        this.cid = cid;
        this.bid = bid;
        this.type = type;
        if(type.equals("channel"))
            pref_type = "channel";
        else
            pref_type = "buffer";
    }

    public JSONObject updatePref(JSONObject prefs, boolean checked, String key) throws JSONException {
        if (!checked) {
            JSONObject map;
            if (prefs.has(key))
                map = prefs.getJSONObject(key);
            else
                map = new JSONObject();
            map.put(String.valueOf(bid), true);
            prefs.put(key, map);
        } else {
            if (prefs.has(key)) {
                JSONObject map = prefs.getJSONObject(key);
                map.remove(String.valueOf(bid));
                prefs.put(key, map);
            }
        }
        return prefs;
    }

    class SaveClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            JSONObject prefs = null;
            try {
                if (NetworkConnection.getInstance().getUserInfo() != null) {
                    prefs = NetworkConnection.getInstance().getUserInfo().prefs;
                    if (prefs == null) {
                        prefs = new JSONObject();
                    }

                    prefs = updatePref(prefs, unread.isChecked(), pref_type + "-disableTrackUnread");
                    prefs = updatePref(prefs, !unread.isChecked(), pref_type + "-enableTrackUnread");
                    prefs = updatePref(prefs, readOnSelect.isChecked(), pref_type + "-disableReadOnSelect");
                    prefs = updatePref(prefs, !readOnSelect.isChecked(), pref_type + "-enableReadOnSelect");
                    prefs = updatePref(prefs, !muted.isChecked(), pref_type + "-notifications-mute");
                    prefs = updatePref(prefs, muted.isChecked(), pref_type + "-notifications-mute-disable");
                    prefs = updatePref(prefs, !formatColors.isChecked(), pref_type + "-chat-color");
                    prefs = updatePref(prefs, formatColors.isChecked(), pref_type + "-chat-nocolor");
                    if (type.equals("console")) {
                        prefs = updatePref(prefs, expandDisco.isChecked(), pref_type + "-expandDisco");
                    } else {
                        if(type.equals("channel")) {
                            prefs = updatePref(prefs, members.isChecked(), pref_type + "-hiddenMembers");
                            prefs = updatePref(prefs, notifyAll.isChecked(), pref_type + "-notifications-all-disable");
                            prefs = updatePref(prefs, !notifyAll.isChecked(), pref_type + "-notifications-all");
                            prefs = updatePref(prefs, autosuggest.isChecked(), pref_type + "-disableAutoSuggest");
                        }
                        prefs = updatePref(prefs, joinpart.isChecked(), pref_type + "-hideJoinPart");
                        prefs = updatePref(prefs, !joinpart.isChecked(), pref_type + "-showJoinPart");
                        prefs = updatePref(prefs, collapse.isChecked(), pref_type + "-expandJoinPart");
                        prefs = updatePref(prefs, !collapse.isChecked(), pref_type + "-collapseJoinPart");
                        prefs = updatePref(prefs, inlineFiles.isChecked(), pref_type + "-files-disableinline");
                        prefs = updatePref(prefs, !inlineFiles.isChecked(), pref_type + "-files-enableinline");
                        prefs = updatePref(prefs, !replyCollapse.isChecked(), pref_type + "-reply-collapse");
                        prefs = updatePref(prefs, inlineImages.isChecked(), pref_type + "-inlineimages-disable");
                        prefs = updatePref(prefs, !inlineImages.isChecked(), pref_type + "-inlineimages");
                    }
                    NetworkConnection.getInstance().set_prefs(prefs.toString(), null);
                } else {
                    Toast.makeText(getActivity(), "An error occurred while saving preferences.  Please try again shortly", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                IRCCloudLog.LogException(e);
                Toast.makeText(getActivity(), "An error occurred while saving preferences.  Please try again shortly", Toast.LENGTH_SHORT).show();
            }
            dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            notifyAll.setChecked(false);
            joinpart.setChecked(true);
            unread.setChecked(true);
            members.setChecked(true);
            collapse.setChecked(true);
            autosuggest.setChecked(true);
            readOnSelect.setChecked(false);
            inlineFiles.setChecked(true);
            inlineImages.setChecked(false);
            replyCollapse.setChecked(false);
            expandDisco.setChecked(true);
            muted.setChecked(false);
            formatColors.setChecked(true);

            if (NetworkConnection.getInstance().getUserInfo() != null) {
                JSONObject prefs = NetworkConnection.getInstance().getUserInfo().prefs;
                if (prefs != null) {
                    boolean enabled = (prefs.has("hideJoinPart") && prefs.get("hideJoinPart") instanceof Boolean && prefs.getBoolean("hideJoinPart"));
                    if (prefs.has(pref_type + "-showJoinPart")) {
                        JSONObject showMap = prefs.getJSONObject(pref_type + "-showJoinPart");
                        if (showMap.has(String.valueOf(bid)) && showMap.getBoolean(String.valueOf(bid)))
                            enabled = false;
                    }
                    if (prefs.has(pref_type + "-hideJoinPart")) {
                        JSONObject hideMap = prefs.getJSONObject(pref_type + "-hideJoinPart");
                        if (hideMap.has(String.valueOf(bid)) && hideMap.getBoolean(String.valueOf(bid)))
                            enabled = true;
                    }
                    joinpart.setChecked(!enabled);
                    enabled = !(prefs.has("disableTrackUnread") && prefs.get("disableTrackUnread") instanceof Boolean && prefs.getBoolean("disableTrackUnread"));
                    if (prefs.has(pref_type + "-disableTrackUnread")) {
                        JSONObject unreadMap = prefs.getJSONObject(pref_type + "-disableTrackUnread");
                        if (unreadMap.has(String.valueOf(bid)) && unreadMap.getBoolean(String.valueOf(bid)))
                            enabled = false;
                    }
                    if (prefs.has(pref_type + "-enableTrackUnread")) {
                        JSONObject unreadMap = prefs.getJSONObject(pref_type + "-enableTrackUnread");
                        if (unreadMap.has(String.valueOf(bid)) && unreadMap.getBoolean(String.valueOf(bid)))
                            enabled = true;
                    }
                    unread.setChecked(enabled);
                    if (prefs.has(pref_type + "-hiddenMembers")) {
                        JSONObject membersMap = prefs.getJSONObject(pref_type + "-hiddenMembers");
                        if (membersMap.has(String.valueOf(bid)) && membersMap.getBoolean(String.valueOf(bid)))
                            members.setChecked(false);
                        else
                            members.setChecked(true);
                    } else {
                        members.setChecked(true);
                    }
                    enabled = (prefs.has("expandJoinPart") && prefs.get("expandJoinPart") instanceof Boolean && prefs.getBoolean("expandJoinPart"));
                    if (prefs.has(pref_type + "-expandJoinPart")) {
                        JSONObject expandMap = prefs.getJSONObject(pref_type + "-expandJoinPart");
                        if (expandMap.has(String.valueOf(bid)) && expandMap.getBoolean(String.valueOf(bid)))
                            enabled = true;
                    }
                    if (prefs.has(pref_type + "-collapseJoinPart")) {
                        JSONObject collapseMap = prefs.getJSONObject(pref_type + "-collapseJoinPart");
                        if (collapseMap.has(String.valueOf(bid)) && collapseMap.getBoolean(String.valueOf(bid)))
                            enabled = false;
                    }
                    collapse.setChecked(!enabled);
                    enabled = (prefs.has("notifications-all") && prefs.get("notifications-all") instanceof Boolean && prefs.getBoolean("notifications-all"));
                    if (prefs.has(pref_type + "-notifications-all")) {
                        JSONObject notifyAllMap = prefs.getJSONObject(pref_type + "-notifications-all");
                        if (notifyAllMap.has(String.valueOf(bid)) && notifyAllMap.getBoolean(String.valueOf(bid)))
                            enabled = true;
                    }
                    if (prefs.has(pref_type + "-notifications-all-disable")) {
                        JSONObject notifyAllMap = prefs.getJSONObject(pref_type + "-notifications-all-disable");
                        if (notifyAllMap.has(String.valueOf(bid)) && notifyAllMap.getBoolean(String.valueOf(bid)))
                            enabled = false;
                    }
                    notifyAll.setChecked(enabled);
                    enabled = (prefs.has("notifications-mute") && prefs.get("notifications-mute") instanceof Boolean && prefs.getBoolean("notifications-mute"));
                    if (prefs.has(pref_type + "-notifications-mute")) {
                        JSONObject notifyMuteMap = prefs.getJSONObject(pref_type + "-notifications-mute");
                        if (notifyMuteMap.has(String.valueOf(bid)) && notifyMuteMap.getBoolean(String.valueOf(bid)))
                            enabled = true;
                    }
                    if (prefs.has(pref_type + "-notifications-mute-disable")) {
                        JSONObject notifyMuteMap = prefs.getJSONObject(pref_type + "-notifications-mute-disable");
                        if (notifyMuteMap.has(String.valueOf(bid)) && notifyMuteMap.getBoolean(String.valueOf(bid)))
                            enabled = false;
                    }
                    muted.setChecked(enabled);
                    if (prefs.has(pref_type + "-disableAutoSuggest")) {
                        JSONObject suggestMap = prefs.getJSONObject(pref_type + "-disableAutoSuggest");
                        if (suggestMap.has(String.valueOf(bid)) && suggestMap.getBoolean(String.valueOf(bid)))
                            autosuggest.setChecked(false);
                        else
                            autosuggest.setChecked(true);
                    } else {
                        autosuggest.setChecked(true);
                    }
                    enabled = prefs.has("enableReadOnSelect") && prefs.get("enableReadOnSelect") instanceof Boolean && prefs.getBoolean("enableReadOnSelect");
                    if (prefs.has(pref_type + "-enableReadOnSelect")) {
                        JSONObject readOnSelectMap = prefs.getJSONObject(pref_type + "-enableReadOnSelect");
                        if (readOnSelectMap.has(String.valueOf(bid)) && readOnSelectMap.getBoolean(String.valueOf(bid)))
                            enabled = true;
                    }
                    if (prefs.has(pref_type + "-disableReadOnSelect")) {
                        JSONObject readOnSelectMap = prefs.getJSONObject(pref_type + "-disableReadOnSelect");
                        if (readOnSelectMap.has(String.valueOf(bid)) && readOnSelectMap.getBoolean(String.valueOf(bid)))
                            enabled = false;
                    }
                    readOnSelect.setChecked(enabled);
                    enabled = (prefs.has("files-disableinline") && prefs.get("files-disableinline") instanceof Boolean && prefs.getBoolean("files-disableinline"));
                    if (prefs.has(pref_type + "-files-disableinline")) {
                        JSONObject noInlineMap = prefs.getJSONObject(pref_type + "-files-disableinline");
                        if (noInlineMap.has(String.valueOf(bid)) && noInlineMap.getBoolean(String.valueOf(bid)))
                            enabled = true;
                    }
                    if (prefs.has(pref_type + "-files-enableinline")) {
                        JSONObject inlineMap = prefs.getJSONObject(pref_type + "-files-enableinline");
                        if (inlineMap.has(String.valueOf(bid)) && inlineMap.getBoolean(String.valueOf(bid)))
                            enabled = false;
                    }
                    inlineFiles.setChecked(!enabled);
                    if(prefs.has("inlineimages") && prefs.get("inlineimages") instanceof Boolean && prefs.getBoolean("inlineimages")) {
                        JSONObject inlineImagesMap = null;
                        if (prefs.has(pref_type + "-inlineimages-disable"))
                            inlineImagesMap = prefs.getJSONObject(pref_type + "-inlineimages-disable");

                        inlineImages.setChecked(!(inlineImagesMap != null && inlineImagesMap.has(String.valueOf(bid)) && inlineImagesMap.getBoolean(String.valueOf(bid))));
                    } else {
                        JSONObject inlineImagesMap = null;
                        if (prefs.has(pref_type + "-inlineimages"))
                            inlineImagesMap = prefs.getJSONObject(pref_type + "-inlineimages");

                        inlineImages.setChecked((inlineImagesMap != null && inlineImagesMap.has(String.valueOf(bid)) && inlineImagesMap.getBoolean(String.valueOf(bid))));
                    }
                    if (prefs.has(pref_type + "-reply-collapse")) {
                        JSONObject collapseMap = prefs.getJSONObject(pref_type + "-reply-collapse");
                        if (collapseMap.has(String.valueOf(bid)) && collapseMap.getBoolean(String.valueOf(bid)))
                            replyCollapse.setChecked(true);
                        else
                            replyCollapse.setChecked(false);
                    } else {
                        replyCollapse.setChecked(false);
                    }
                    if (prefs.has(pref_type + "-expandDisco")) {
                        JSONObject expandMap = prefs.getJSONObject(pref_type + "-expandDisco");
                        if (expandMap.has(String.valueOf(bid)) && expandMap.getBoolean(String.valueOf(bid)))
                            expandDisco.setChecked(false);
                        else
                            expandDisco.setChecked(true);
                    }
                    enabled = !(prefs.has("chat-nocolor") && prefs.get("chat-nocolor") instanceof Boolean && prefs.getBoolean("chat-nocolor"));
                    if (prefs.has(pref_type + "-chat-color")) {
                        JSONObject chatColorMap = prefs.getJSONObject(pref_type + "-chat-color");
                        if (chatColorMap.has(String.valueOf(bid)) && chatColorMap.getBoolean(String.valueOf(bid)))
                            enabled = true;
                    }
                    if (prefs.has(pref_type + "-chat-nocolor")) {
                        JSONObject chatNoColorMap = prefs.getJSONObject(pref_type + "-chat-nocolor");
                        if (chatNoColorMap.has(String.valueOf(bid)) && chatNoColorMap.getBoolean(String.valueOf(bid)))
                            enabled = false;
                    }
                    formatColors.setChecked(enabled);
                }
            }
            if (!getActivity().getResources().getBoolean(R.bool.isTablet))
                members.setVisibility(View.GONE);
        } catch (JSONException e) {
            NetworkConnection.printStackTraceToCrashlytics(e);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context ctx = getActivity();
        if(ctx == null)
            return null;
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.dialog_buffer_options, null);
        expandDisco = v.findViewById(R.id.expandDisco);
        members = v.findViewById(R.id.members);
        unread = v.findViewById(R.id.unread);
        notifyAll = v.findViewById(R.id.notifyAll);
        muted = v.findViewById(R.id.muted);
        joinpart = v.findViewById(R.id.joinpart);
        collapse = v.findViewById(R.id.collapse);
        replyCollapse = v.findViewById(R.id.replyCollapse);
        autosuggest = v.findViewById(R.id.autosuggest);
        readOnSelect = v.findViewById(R.id.readOnSelect);
        inlineFiles = v.findViewById(R.id.inlineFiles);
        formatColors = v.findViewById(R.id.formatColors);
        inlineImages = v.findViewById(R.id.inlineImages);
        inlineImages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(inlineImages.isChecked()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Warning");
                    builder.setMessage("External URLs may load insecurely and could result in your IP address being revealed to external site operators");

                    builder.setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            inlineImages.setChecked(false);
                            dialog.dismiss();
                        }
                    });
                    AlertDialog d = builder.create();
                    if (!getActivity().isFinishing()) {
                        d.setOwnerActivity(getActivity());
                        d.show();
                    }
                }
            }
        });

        if (savedInstanceState != null && bid == -1 && savedInstanceState.containsKey("bid")) {
            bid = savedInstanceState.getInt("bid");
            cid = savedInstanceState.getInt("cid");
            type = savedInstanceState.getString("type");
            if(type.equals("channel"))
                pref_type = "channel";
            else
                pref_type = "buffer";
        }

        if (type != null && type.equals("console")) {
            notifyAll.setVisibility(View.GONE);
            members.setVisibility(View.GONE);
            autosuggest.setVisibility(View.GONE);
            joinpart.setVisibility(View.GONE);
            collapse.setVisibility(View.GONE);
            inlineFiles.setVisibility(View.GONE);
            inlineImages.setVisibility(View.GONE);
            replyCollapse.setVisibility(View.GONE);
        } else {
            expandDisco.setVisibility(View.GONE);
            if(type.equals("conversation")) {
                notifyAll.setVisibility(View.GONE);
                members.setVisibility(View.GONE);
                autosuggest.setVisibility(View.GONE);
            }
        }

        if(BuildConfig.ENTERPRISE)
            muted.setVisibility(View.GONE);

        return new AlertDialog.Builder(ctx)
                .setTitle("Display Options")
                .setView(v)
                .setPositiveButton("Save", new SaveClickListener())
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("bid", bid);
        outState.putInt("cid", cid);
        outState.putString("type", type);
    }
}
