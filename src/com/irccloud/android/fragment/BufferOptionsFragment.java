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
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;

import org.json.JSONException;
import org.json.JSONObject;@SuppressLint("ValidFragment")
public class BufferOptionsFragment extends DialogFragment {
    SwitchCompat unread;
    SwitchCompat joinpart;
    SwitchCompat collapse;
    SwitchCompat expandDisco;
    SwitchCompat readOnSelect;
    SwitchCompat inlineFiles;
    SwitchCompat inlineImages;
    SwitchCompat replyCollapse;
    int cid;
    int bid;
    String type;

    public BufferOptionsFragment() {
        cid = bid = -1;
        type = null;
    }

    public BufferOptionsFragment(int cid, int bid, String type) {
        this.cid = cid;
        this.bid = bid;
        this.type = type;
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

                    prefs = updatePref(prefs, unread.isChecked(), "buffer-disableTrackUnread");
                    prefs = updatePref(prefs, !unread.isChecked(), "buffer-enableTrackUnread");
                    prefs = updatePref(prefs, readOnSelect.isChecked(), "buffer-disableReadOnSelect");
                    prefs = updatePref(prefs, !readOnSelect.isChecked(), "buffer-enableReadOnSelect");
                    if (!type.equalsIgnoreCase("console")) {
                        prefs = updatePref(prefs, joinpart.isChecked(), "buffer-hideJoinPart");
                        prefs = updatePref(prefs, collapse.isChecked(), "buffer-expandJoinPart");
                        prefs = updatePref(prefs, !collapse.isChecked(), "buffer-collapseJoinPart");
                        prefs = updatePref(prefs, inlineFiles.isChecked(), "buffer-files-disableinline");
                        prefs = updatePref(prefs, !replyCollapse.isChecked(), "buffer-reply-collapse");
                    } else {
                        prefs = updatePref(prefs, expandDisco.isChecked(), "buffer-expandDisco");
                    }
                    prefs = updatePref(prefs, inlineImages.isChecked(), "buffer-inlineimages-disable");
                    prefs = updatePref(prefs, !inlineImages.isChecked(), "buffer-inlineimages");
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
                    if (prefs.has("buffer-hideJoinPart")) {
                        JSONObject hiddenMap = prefs.getJSONObject("buffer-hideJoinPart");
                        if (hiddenMap.has(String.valueOf(bid)) && hiddenMap.getBoolean(String.valueOf(bid)))
                            joinpart.setChecked(false);
                        else
                            joinpart.setChecked(true);
                    } else {
                        joinpart.setChecked(true);
                    }
                    boolean enabled = !(prefs.has("disableTrackUnread") && prefs.get("disableTrackUnread") instanceof Boolean && prefs.getBoolean("disableTrackUnread"));
                    if (prefs.has("buffer-disableTrackUnread")) {
                        JSONObject unreadMap = prefs.getJSONObject("buffer-disableTrackUnread");
                        if (unreadMap.has(String.valueOf(bid)) && unreadMap.getBoolean(String.valueOf(bid)))
                            enabled = false;
                    }
                    if (prefs.has("buffer-enableTrackUnread")) {
                        JSONObject unreadMap = prefs.getJSONObject("buffer-enableTrackUnread");
                        if (unreadMap.has(String.valueOf(bid)) && unreadMap.getBoolean(String.valueOf(bid)))
                            enabled = true;
                    }
                    unread.setChecked(enabled);
                    enabled = (prefs.has("expandJoinPart") && prefs.get("expandJoinPart") instanceof Boolean && prefs.getBoolean("expandJoinPart"));
                    if (prefs.has("buffer-expandJoinPart")) {
                        JSONObject expandMap = prefs.getJSONObject("buffer-expandJoinPart");
                        if (expandMap.has(String.valueOf(bid)) && expandMap.getBoolean(String.valueOf(bid)))
                            enabled = true;
                    }
                    if (prefs.has("buffer-collapseJoinPart")) {
                        JSONObject collapseMap = prefs.getJSONObject("buffer-collapseJoinPart");
                        if (collapseMap.has(String.valueOf(bid)) && collapseMap.getBoolean(String.valueOf(bid)))
                            enabled = false;
                    }
                    collapse.setChecked(enabled);
                    if (prefs.has("buffer-expandDisco")) {
                        JSONObject expandMap = prefs.getJSONObject("buffer-expandDisco");
                        if (expandMap.has(String.valueOf(bid)) && expandMap.getBoolean(String.valueOf(bid)))
                            expandDisco.setChecked(false);
                        else
                            expandDisco.setChecked(true);
                    } else {
                        expandDisco.setChecked(true);
                    }
                    if (prefs.has("buffer-files-disableinline")) {
                        JSONObject inlineMap = prefs.getJSONObject("buffer-files-disableinline");
                        if (inlineMap.has(String.valueOf(bid)) && inlineMap.getBoolean(String.valueOf(bid)))
                            inlineFiles.setChecked(false);
                        else
                            inlineFiles.setChecked(true);
                    } else {
                        inlineFiles.setChecked(true);
                    }
                    enabled = (prefs.has("enableReadOnSelect") && prefs.get("enableReadOnSelect") instanceof Boolean && prefs.getBoolean("enableReadOnSelect"));
                    if (prefs.has("buffer-enableReadOnSelect")) {
                        JSONObject readOnSelectMap = prefs.getJSONObject("buffer-enableReadOnSelect");
                        if (readOnSelectMap.has(String.valueOf(bid)) && readOnSelectMap.getBoolean(String.valueOf(bid)))
                            enabled = true;
                    }
                    if (prefs.has("buffer-disableReadOnSelect")) {
                        JSONObject readOnSelectMap = prefs.getJSONObject("buffer-disableReadOnSelect");
                        if (readOnSelectMap.has(String.valueOf(bid)) && readOnSelectMap.getBoolean(String.valueOf(bid)))
                            enabled = false;
                    }
                    readOnSelect.setChecked(enabled);
                    if(prefs.has("inlineimages") && prefs.get("inlineimages") instanceof Boolean && prefs.getBoolean("inlineimages")) {
                        JSONObject inlineImagesMap = null;
                        if (prefs.has("buffer-inlineimages-disable"))
                            inlineImagesMap = prefs.getJSONObject("buffer-inlineimages-disable");

                        inlineImages.setChecked(!(inlineImagesMap != null && inlineImagesMap.has(String.valueOf(bid)) && inlineImagesMap.getBoolean(String.valueOf(bid))));
                    } else {
                        JSONObject inlineImagesMap = null;
                        if (prefs.has("buffer-inlineimages"))
                            inlineImagesMap = prefs.getJSONObject("buffer-inlineimages");

                        inlineImages.setChecked((inlineImagesMap != null && inlineImagesMap.has(String.valueOf(bid)) && inlineImagesMap.getBoolean(String.valueOf(bid))));
                    }
                    if (prefs.has("buffer-reply-collapse")) {
                        JSONObject collapseMap = prefs.getJSONObject("buffer-reply-collapse");
                        if (collapseMap.has(String.valueOf(bid)) && collapseMap.getBoolean(String.valueOf(bid)))
                            replyCollapse.setChecked(false);
                        else
                            replyCollapse.setChecked(true);
                    } else {
                        replyCollapse.setChecked(true);
                    }
                } else {
                    joinpart.setChecked(true);
                    unread.setChecked(true);
                    collapse.setChecked(true);
                    expandDisco.setChecked(true);
                    readOnSelect.setChecked(false);
                    inlineFiles.setChecked(true);
                    inlineImages.setChecked(false);
                    replyCollapse.setChecked(false);
                }
            }
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
        Context ctx = getActivity();
        if(ctx == null)
            return null;
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.dialog_buffer_options, null);
        unread = v.findViewById(R.id.unread);
        joinpart = v.findViewById(R.id.joinpart);
        collapse = v.findViewById(R.id.collapse);
        replyCollapse = v.findViewById(R.id.replyCollapse);
        expandDisco = v.findViewById(R.id.expandDisco);
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

        if (savedInstanceState != null && bid == -1 && savedInstanceState.containsKey("bid")) {
            bid = savedInstanceState.getInt("bid");
            cid = savedInstanceState.getInt("cid");
            type = savedInstanceState.getString("type");
        }

        if (type != null && type.equalsIgnoreCase("console")) {
            joinpart.setVisibility(View.GONE);
            collapse.setVisibility(View.GONE);
            inlineFiles.setVisibility(View.GONE);
            inlineImages.setVisibility(View.GONE);
            replyCollapse.setVisibility(View.GONE);
        } else {
            expandDisco.setVisibility(View.GONE);
        }

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
