package com.irccloud.android;

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

public class ChannelOptionsFragment extends DialogFragment {
	CheckBox members;
	CheckBox unread;
	CheckBox joinpart;
	int cid;
	int bid;
	
	public ChannelOptionsFragment(int cid, int bid) {
		this.cid = cid;
		this.bid = bid;
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
				
				prefs = updatePref(prefs, members, "channel-hiddenMembers");
				prefs = updatePref(prefs, unread, "channel-disableTrackUnread");
				prefs = updatePref(prefs, joinpart, "channel-hideJoinPart");
				
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
    	View v = inflater.inflate(R.layout.dialog_channel_options,null);
    	members = (CheckBox)v.findViewById(R.id.members);
    	unread = (CheckBox)v.findViewById(R.id.unread);
    	joinpart = (CheckBox)v.findViewById(R.id.joinpart);
    	
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
