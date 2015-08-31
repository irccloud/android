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
import com.squareup.leakcanary.RefWatcher;

import org.json.JSONException;
import org.json.JSONObject;@SuppressLint("ValidFragment")
public class BufferOptionsFragment extends DialogFragment {
    SwitchCompat unread;
    SwitchCompat joinpart;
    SwitchCompat collapse;
    SwitchCompat expandDisco;
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

    public JSONObject updatePref(JSONObject prefs, SwitchCompat control, String key) throws JSONException {
        if (!control.isChecked()) {
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

                    prefs = updatePref(prefs, unread, "buffer-disableTrackUnread");
                    if (!type.equalsIgnoreCase("console")) {
                        prefs = updatePref(prefs, joinpart, "buffer-hideJoinPart");
                        prefs = updatePref(prefs, collapse, "buffer-expandJoinPart");
                    } else {
                        prefs = updatePref(prefs, expandDisco, "buffer-expandDisco");
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
                    if (prefs.has("buffer-disableTrackUnread")) {
                        JSONObject unreadMap = prefs.getJSONObject("buffer-disableTrackUnread");
                        if (unreadMap.has(String.valueOf(bid)) && unreadMap.getBoolean(String.valueOf(bid)))
                            unread.setChecked(false);
                        else
                            unread.setChecked(true);
                    } else {
                        unread.setChecked(true);
                    }
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
                } else {
                    joinpart.setChecked(true);
                    unread.setChecked(true);
                    collapse.setChecked(true);
                    expandDisco.setChecked(true);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
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

        if (savedInstanceState != null && bid == -1 && savedInstanceState.containsKey("bid")) {
            bid = savedInstanceState.getInt("bid");
            cid = savedInstanceState.getInt("cid");
            type = savedInstanceState.getString("type");
        }

        if (type != null && type.equalsIgnoreCase("console")) {
            joinpart.setVisibility(View.GONE);
            collapse.setVisibility(View.GONE);
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
