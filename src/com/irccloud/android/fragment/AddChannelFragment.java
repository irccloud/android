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

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v4.app.DialogFragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.activity.EditConnectionActivity;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.collection.ServersList;

import java.util.ArrayList;

public class AddChannelFragment extends DialogFragment {
    SparseArray<Server> servers;
    Spinner spinner;
    TextView channels;
    int defaultCid = -1;

    class DoneClickListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            int pos = spinner.getSelectedItemPosition();
            if (pos >= 0 && pos < servers.size()) {
                int cid = servers.valueAt(spinner.getSelectedItemPosition()).getCid();
                String[] splitchannels = channels.getText().toString().split(",");
                for (int i = 0; i < splitchannels.length; i++) {
                    String[] channelandkey = splitchannels[i].split(" ");
                    if (channelandkey.length > 1)
                        NetworkConnection.getInstance().join(cid, channelandkey[0].trim(), channelandkey[1], null);
                    else
                        NetworkConnection.getInstance().join(cid, channelandkey[0].trim(), "", null);
                }
                dismiss();
            }
        }
    }

    public void setDefaultCid(int cid) {
        defaultCid = cid;
    }

    @Override
    public void onResume() {
        int pos = 0;
        super.onResume();
        servers = ServersList.getInstance().getServers();

        ArrayList<String> servernames = new ArrayList<String>();
        for (int i = 0; i < servers.size(); i++) {
            servernames.add(servers.valueAt(i).getName());
            if (servers.valueAt(i).getCid() == defaultCid)
                pos = i;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, android.R.id.text1, servernames.toArray(new String[servernames.size()]));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(pos);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context ctx = getActivity();
        if(ctx == null)
            return null;
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View v = inflater.inflate(R.layout.dialog_add_channel, null);
        spinner = v.findViewById(R.id.networkSpinner);
        channels = v.findViewById(R.id.channels);
        channels.setText("");
        channels.append("#");
        Button b = v.findViewById(R.id.addBtn);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!getActivity().getResources().getBoolean(R.bool.isTablet)) {
                    Intent i = new Intent(getActivity(), EditConnectionActivity.class);
                    startActivity(i);
                } else {
                    EditConnectionFragment newFragment = new EditConnectionFragment();
                    newFragment.show(getActivity().getSupportFragmentManager(), "editconnection");
                }
            }
        });

        return new AlertDialog.Builder(ctx)
                .setTitle("Join A Channel")
                .setView(v)
                .setPositiveButton("Join", new DoneClickListener())
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
    }
}
