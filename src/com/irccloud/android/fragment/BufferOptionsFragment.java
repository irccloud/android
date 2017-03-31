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
                        prefs = updatePref(prefs, inlineFiles.isChecked(), "buffer-files-disableinline");
                    } else {
                        prefs = updatePref(prefs, expandDisco.isChecked(), "buffer-expandDisco");
                    }
                    NetworkConnection.getInstance().set_prefs(prefs.toString());
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
                    if (prefs.has("buffer-expandJoinPart")) {
                        JSONObject expandMap = prefs.getJSONObject("buffer-expandJoinPart");
                        if (expandMap.has(String.valueOf(bid)) && expandMap.getBoolean(String.valueOf(bid)))
                            collapse.setChecked(false);
                        else
                            collapse.setChecked(true);
                    } else {
                        collapse.setChecked(true);
                    }
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
                } else {
                    joinpart.setChecked(true);
                    unread.setChecked(true);
                    collapse.setChecked(true);
                    expandDisco.setChecked(true);
                    readOnSelect.setChecked(false);
                    inlineFiles.setChecked(true);
                }
            }
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
        unread = (SwitchCompat) v.findViewById(R.id.unread);
        joinpart = (SwitchCompat) v.findViewById(R.id.joinpart);
        collapse = (SwitchCompat) v.findViewById(R.id.collapse);
        expandDisco = (SwitchCompat) v.findViewById(R.id.expandDisco);
        readOnSelect = (SwitchCompat) v.findViewById(R.id.readOnSelect);
        inlineFiles = (SwitchCompat) v.findViewById(R.id.inlineFiles);

        if (savedInstanceState != null && bid == -1 && savedInstanceState.containsKey("bid")) {
            bid = savedInstanceState.getInt("bid");
            cid = savedInstanceState.getInt("cid");
            type = savedInstanceState.getString("type");
        }

        if (type != null && type.equalsIgnoreCase("console")) {
            joinpart.setVisibility(View.GONE);
            collapse.setVisibility(View.GONE);
            inlineFiles.setVisibility(View.GONE);
        } else {
            expandDisco.setVisibility(View.GONE);
        }

        return new AlertDialog.Builder(ctx)
                .setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)
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
