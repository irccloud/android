/*
 * Copyright (c) 2013 IRCCloud, Ltd.
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

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;

public class BufferOptionsFragment extends DialogFragment {
	CheckBox unread;
	CheckBox joinpart;
    CheckBox collapse;
    CheckBox expandDisco;
	int cid;
	int bid;
	String type;
	
	public BufferOptionsFragment(int cid, int bid, String type) {
		this.cid = cid;
		this.bid = bid;
		this.type = type;
	}
	
	public JSONObject updatePref(JSONObject prefs, CheckBox control, String key) throws JSONException {
		if(!control.isChecked()) {
			JSONObject map;
			if(prefs.has(key))
				map = prefs.getJSONObject(key);
			else
				map = new JSONObject();
			map.put(String.valueOf(bid), true);
			prefs.put(key, map);
		} else {
			if(prefs.has(key)) {
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
				if(NetworkConnection.getInstance().getUserInfo() != null)
					prefs = NetworkConnection.getInstance().getUserInfo().prefs;
				if(prefs == null)
					prefs = new JSONObject();
				
				prefs = updatePref(prefs, unread, "buffer-disableTrackUnread");
		    	if(!type.equalsIgnoreCase("console")) {
		    		prefs = updatePref(prefs, joinpart, "buffer-hideJoinPart");
                    prefs = updatePref(prefs, collapse, "buffer-expandJoinPart");
                } else {
                    prefs = updatePref(prefs, expandDisco, "buffer-expandDisco");
                }
				NetworkConnection.getInstance().set_prefs(prefs.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
			dismiss();
		}
    }

    @Override
    public void onResume() {
    	super.onResume();
    	try {
	    	if(NetworkConnection.getInstance().getUserInfo() != null) {
		    	JSONObject prefs = NetworkConnection.getInstance().getUserInfo().prefs;
		    	if(prefs != null) {
		    		if(prefs.has("buffer-hideJoinPart")) {
				    	JSONObject hiddenMap = prefs.getJSONObject("buffer-hideJoinPart");
						if(hiddenMap.has(String.valueOf(bid)) && hiddenMap.getBoolean(String.valueOf(bid)))
							joinpart.setChecked(false);
						else
							joinpart.setChecked(true);
		    		} else {
						joinpart.setChecked(true);
		    		}
		    		if(prefs.has("buffer-disableTrackUnread")) {
				    	JSONObject unreadMap = prefs.getJSONObject("buffer-disableTrackUnread");
						if(unreadMap.has(String.valueOf(bid)) && unreadMap.getBoolean(String.valueOf(bid)))
							unread.setChecked(false);
						else
							unread.setChecked(true);
		    		} else {
						unread.setChecked(true);
		    		}
                    if(prefs.has("buffer-expandJoinPart")) {
                        JSONObject expandMap = prefs.getJSONObject("buffer-expandJoinPart");
                        if(expandMap.has(String.valueOf(bid)) && expandMap.getBoolean(String.valueOf(bid)))
                            collapse.setChecked(false);
                        else
                            collapse.setChecked(true);
                    } else {
                        collapse.setChecked(true);
                    }
                    if(prefs.has("buffer-expandDisco")) {
                        JSONObject expandMap = prefs.getJSONObject("buffer-expandDisco");
                        if(expandMap.has(String.valueOf(bid)) && expandMap.getBoolean(String.valueOf(bid)))
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
		if(Build.VERSION.SDK_INT < 11)
			ctx = new ContextThemeWrapper(ctx, android.R.style.Theme_Dialog);
		LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View v = inflater.inflate(R.layout.dialog_buffer_options,null);
    	unread = (CheckBox)v.findViewById(R.id.unread);
    	joinpart = (CheckBox)v.findViewById(R.id.joinpart);
        collapse = (CheckBox)v.findViewById(R.id.collapse);
        expandDisco = (CheckBox)v.findViewById(R.id.expandDisco);
    	if(type.equalsIgnoreCase("console")) {
    		joinpart.setVisibility(View.GONE);
            collapse.setVisibility(View.GONE);
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
}
