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

public class ChannelOptionsFragment extends DialogFragment {
	CheckBox members;
	CheckBox unread;
	CheckBox joinpart;
    CheckBox collapse;
    CheckBox notifyAll;
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
	
	public JSONObject updatePref(JSONObject prefs, CheckBox control, String key) throws JSONException {
        boolean checked = control.isChecked();
        if(control == notifyAll)
            checked = !checked;
		if(!checked) {
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
				
				prefs = updatePref(prefs, members, "channel-hiddenMembers");
				prefs = updatePref(prefs, unread, "channel-disableTrackUnread");
				prefs = updatePref(prefs, joinpart, "channel-hideJoinPart");
                prefs = updatePref(prefs, collapse, "channel-expandJoinPart");
                prefs = updatePref(prefs, notifyAll, "channel-notifications-all");

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
		    		if(prefs.has("channel-hideJoinPart")) {
				    	JSONObject hiddenMap = prefs.getJSONObject("channel-hideJoinPart");
						if(hiddenMap.has(String.valueOf(bid)) && hiddenMap.getBoolean(String.valueOf(bid)))
							joinpart.setChecked(false);
						else
							joinpart.setChecked(true);
		    		} else {
						joinpart.setChecked(true);
		    		}
		    		if(prefs.has("channel-disableTrackUnread")) {
				    	JSONObject unreadMap = prefs.getJSONObject("channel-disableTrackUnread");
						if(unreadMap.has(String.valueOf(bid)) && unreadMap.getBoolean(String.valueOf(bid)))
							unread.setChecked(false);
						else
							unread.setChecked(true);
		    		} else {
		    			unread.setChecked(true);
		    		}
		    		if(prefs.has("channel-hiddenMembers")) {
				    	JSONObject membersMap = prefs.getJSONObject("channel-hiddenMembers");
						if(membersMap.has(String.valueOf(bid)) && membersMap.getBoolean(String.valueOf(bid)))
							members.setChecked(false);
						else
							members.setChecked(true);
		    		} else {
		    			members.setChecked(true);
		    		}
                    if(prefs.has("channel-expandJoinPart")) {
                        JSONObject expandMap = prefs.getJSONObject("channel-expandJoinPart");
                        if(expandMap.has(String.valueOf(bid)) && expandMap.getBoolean(String.valueOf(bid)))
                            collapse.setChecked(false);
                        else
                            collapse.setChecked(true);
                    } else {
                        collapse.setChecked(true);
                    }
                    if(prefs.has("channel-notifications-all")) {
                        JSONObject notifyAllMap = prefs.getJSONObject("channel-notifications-all");
                        if(notifyAllMap.has(String.valueOf(bid)) && notifyAllMap.getBoolean(String.valueOf(bid)))
                            notifyAll.setChecked(true);
                        else
                            notifyAll.setChecked(false);
                    } else {
                        notifyAll.setChecked(false);
                    }
		    	} else {
                    notifyAll.setChecked(false);
					joinpart.setChecked(true);
	    			unread.setChecked(true);
	    			members.setChecked(true);
                    collapse.setChecked(true);
		    	}
	    	} else {
                notifyAll.setChecked(false);
				joinpart.setChecked(true);
    			unread.setChecked(true);
    			members.setChecked(true);
                collapse.setChecked(true);
	    	}
            if(getActivity().getWindowManager().getDefaultDisplay().getWidth() < 800)
                members.setVisibility(View.GONE);
		} catch (JSONException e) {
			e.printStackTrace();
		}
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	if(savedInstanceState != null && savedInstanceState.containsKey("cid") && cid == -1) {
    		cid = savedInstanceState.getInt("cid");
    		bid = savedInstanceState.getInt("bid");
    	}
		Context ctx = getActivity();
		if(Build.VERSION.SDK_INT < 11)
			ctx = new ContextThemeWrapper(ctx, android.R.style.Theme_Dialog);
		LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View v = inflater.inflate(R.layout.dialog_channel_options,null);
    	members = (CheckBox)v.findViewById(R.id.members);
    	unread = (CheckBox)v.findViewById(R.id.unread);
        notifyAll = (CheckBox)v.findViewById(R.id.notifyAll);
    	joinpart = (CheckBox)v.findViewById(R.id.joinpart);
        collapse = (CheckBox)v.findViewById(R.id.collapse);
    	
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
