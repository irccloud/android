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
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;

import org.json.JSONException;
import org.json.JSONObject;

@SuppressLint("ValidFragment")
public class ChannelOptionsFragment extends DialogFragment {
    SwitchCompat members;
    SwitchCompat unread;
    SwitchCompat joinpart;
    SwitchCompat collapse;
    SwitchCompat notifyAll;
    SwitchCompat autosuggest;
    SwitchCompat readOnSelect;
    SwitchCompat inlineFiles;
    SwitchCompat inlineImages;
    int cid;
    int bid;

    public ChannelOptionsFragment() {
        cid = -1;
        bid = -1;
    }

    public ChannelOptionsFragment(int cid, int bid) {
        this.cid = cid;
        this.bid = bid;
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

                    prefs = updatePref(prefs, members.isChecked(), "channel-hiddenMembers");
                    prefs = updatePref(prefs, unread.isChecked(), "channel-disableTrackUnread");
                    prefs = updatePref(prefs, !unread.isChecked(), "channel-enableTrackUnread");
                    prefs = updatePref(prefs, joinpart.isChecked(), "channel-hideJoinPart");
                    prefs = updatePref(prefs, collapse.isChecked(), "channel-expandJoinPart");
                    prefs = updatePref(prefs, notifyAll.isChecked(), "channel-notifications-all-disable");
                    prefs = updatePref(prefs, !notifyAll.isChecked(), "channel-notifications-all");
                    prefs = updatePref(prefs, autosuggest.isChecked(), "channel-disableAutoSuggest");
                    prefs = updatePref(prefs, readOnSelect.isChecked(), "channel-disableReadOnSelect");
                    prefs = updatePref(prefs, !readOnSelect.isChecked(), "channel-enableReadOnSelect");
                    prefs = updatePref(prefs, inlineFiles.isChecked(), "channel-files-disableinline");
                    prefs = updatePref(prefs, inlineImages.isChecked(), "channel-inlineimages-disable");
                    prefs = updatePref(prefs, !inlineImages.isChecked(), "channel-inlineimages");

                    NetworkConnection.getInstance().set_prefs(prefs.toString(), null);
                } else {
                    Toast.makeText(getActivity(), "An error occurred while saving preferences.  Please try again shortly", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Crashlytics.logException(e);
                Toast.makeText(getActivity(), "An error occurred while saving preferences.  Please try again shortly", Toast.LENGTH_SHORT).show();
            }
            dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            if (NetworkConnection.getInstance().getUserInfo() != null) {
                JSONObject prefs = NetworkConnection.getInstance().getUserInfo().prefs;
                if (prefs != null) {
                    if (prefs.has("channel-hideJoinPart")) {
                        JSONObject hiddenMap = prefs.getJSONObject("channel-hideJoinPart");
                        if (hiddenMap.has(String.valueOf(bid)) && hiddenMap.getBoolean(String.valueOf(bid)))
                            joinpart.setChecked(false);
                        else
                            joinpart.setChecked(true);
                    } else {
                        joinpart.setChecked(true);
                    }
                    boolean enabled = !(prefs.has("disableTrackUnread") && prefs.get("disableTrackUnread") instanceof Boolean && prefs.getBoolean("disableTrackUnread"));
                    if (prefs.has("channel-disableTrackUnread")) {
                        JSONObject unreadMap = prefs.getJSONObject("channel-disableTrackUnread");
                        if (unreadMap.has(String.valueOf(bid)) && unreadMap.getBoolean(String.valueOf(bid)))
                            enabled = false;
                    }
                    if (prefs.has("channel-enableTrackUnread")) {
                        JSONObject unreadMap = prefs.getJSONObject("channel-enableTrackUnread");
                        if (unreadMap.has(String.valueOf(bid)) && unreadMap.getBoolean(String.valueOf(bid)))
                            enabled = true;
                    }
                    unread.setChecked(enabled);
                    if (prefs.has("channel-hiddenMembers")) {
                        JSONObject membersMap = prefs.getJSONObject("channel-hiddenMembers");
                        if (membersMap.has(String.valueOf(bid)) && membersMap.getBoolean(String.valueOf(bid)))
                            members.setChecked(false);
                        else
                            members.setChecked(true);
                    } else {
                        members.setChecked(true);
                    }
                    if (prefs.has("channel-expandJoinPart")) {
                        JSONObject expandMap = prefs.getJSONObject("channel-expandJoinPart");
                        if (expandMap.has(String.valueOf(bid)) && expandMap.getBoolean(String.valueOf(bid)))
                            collapse.setChecked(false);
                        else
                            collapse.setChecked(true);
                    } else {
                        collapse.setChecked(true);
                    }
                    enabled = (prefs.has("notifications-all") && prefs.get("notifications-all") instanceof Boolean && prefs.getBoolean("notifications-all"));
                    if (prefs.has("channel-notifications-all")) {
                        JSONObject notifyAllMap = prefs.getJSONObject("channel-notifications-all");
                        if (notifyAllMap.has(String.valueOf(bid)) && notifyAllMap.getBoolean(String.valueOf(bid)))
                            enabled = true;
                    }
                    if (prefs.has("channel-notifications-all-disable")) {
                        JSONObject notifyAllMap = prefs.getJSONObject("channel-notifications-all-disable");
                        if (notifyAllMap.has(String.valueOf(bid)) && notifyAllMap.getBoolean(String.valueOf(bid)))
                            enabled = false;
                    }
                    notifyAll.setChecked(enabled);
                    if (prefs.has("channel-disableAutoSuggest")) {
                        JSONObject suggestMap = prefs.getJSONObject("channel-disableAutoSuggest");
                        if (suggestMap.has(String.valueOf(bid)) && suggestMap.getBoolean(String.valueOf(bid)))
                            autosuggest.setChecked(false);
                        else
                            autosuggest.setChecked(true);
                    } else {
                        autosuggest.setChecked(true);
                    }
                    if (prefs.has("channel-files-disableinline")) {
                        JSONObject inlineMap = prefs.getJSONObject("channel-files-disableinline");
                        if (inlineMap.has(String.valueOf(bid)) && inlineMap.getBoolean(String.valueOf(bid)))
                            inlineFiles.setChecked(false);
                        else
                            inlineFiles.setChecked(true);
                    } else {
                        inlineFiles.setChecked(true);
                    }
                    enabled = !(prefs.has("disableTrackUnread") && prefs.get("disableTrackUnread") instanceof Boolean && prefs.getBoolean("disableTrackUnread"));
                    if (prefs.has("channel-enableReadOnSelect")) {
                        JSONObject readOnSelectMap = prefs.getJSONObject("channel-enableReadOnSelect");
                        if (readOnSelectMap.has(String.valueOf(bid)) && readOnSelectMap.getBoolean(String.valueOf(bid)))
                            enabled = true;
                    }
                    if (prefs.has("channel-disableReadOnSelect")) {
                        JSONObject readOnSelectMap = prefs.getJSONObject("channel-disableReadOnSelect");
                        if (readOnSelectMap.has(String.valueOf(bid)) && readOnSelectMap.getBoolean(String.valueOf(bid)))
                            enabled = false;
                    }
                    readOnSelect.setChecked(enabled);
                    if(prefs.has("inlineimages") && prefs.get("inlineimages") instanceof Boolean && prefs.getBoolean("inlineimages")) {
                        JSONObject inlineImagesMap = null;
                        if (prefs.has("channel-inlineimages-disable"))
                            inlineImagesMap = prefs.getJSONObject("channel-inlineimages-disable");

                        inlineImages.setChecked(!(inlineImagesMap != null && inlineImagesMap.has(String.valueOf(bid)) && inlineImagesMap.getBoolean(String.valueOf(bid))));
                    } else {
                        JSONObject inlineImagesMap = null;
                        if (prefs.has("channel-inlineimages"))
                            inlineImagesMap = prefs.getJSONObject("channel-inlineimages");

                        inlineImages.setChecked((inlineImagesMap != null && inlineImagesMap.has(String.valueOf(bid)) && inlineImagesMap.getBoolean(String.valueOf(bid))));
                    }

                } else {
                    notifyAll.setChecked(false);
                    joinpart.setChecked(true);
                    unread.setChecked(true);
                    members.setChecked(true);
                    collapse.setChecked(true);
                    autosuggest.setChecked(true);
                    readOnSelect.setChecked(false);
                    inlineFiles.setChecked(true);
                    inlineImages.setChecked(false);
                }
            } else {
                notifyAll.setChecked(false);
                joinpart.setChecked(true);
                unread.setChecked(true);
                members.setChecked(true);
                collapse.setChecked(true);
                autosuggest.setChecked(true);
                readOnSelect.setChecked(false);
                inlineImages.setChecked(false);
            }
            if (!getActivity().getResources().getBoolean(R.bool.isTablet))
                members.setVisibility(View.GONE);
        } catch (JSONException e) {
            NetworkConnection.printStackTraceToCrashlytics(e);
        }
        /*if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && getActivity() != null && ((ActivityManager)(getActivity().getSystemService(Context.ACTIVITY_SERVICE))).isLowRamDevice()) {
            inlineFiles.setVisibility(View.GONE);
            inlineImages.setVisibility(View.GONE);
        }*/
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey("cid") && cid == -1) {
            cid = savedInstanceState.getInt("cid");
            bid = savedInstanceState.getInt("bid");
        }
        Context ctx = getActivity();
        if(ctx == null)
            return null;
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.dialog_channel_options, null);
        members = v.findViewById(R.id.members);
        unread = v.findViewById(R.id.unread);
        notifyAll = v.findViewById(R.id.notifyAll);
        joinpart = v.findViewById(R.id.joinpart);
        collapse = v.findViewById(R.id.collapse);
        autosuggest = v.findViewById(R.id.autosuggest);
        readOnSelect = v.findViewById(R.id.readOnSelect);
        inlineFiles = v.findViewById(R.id.inlineFiles);
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
    public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putInt("cid", cid);
        state.putInt("bid", bid);
    }
}
