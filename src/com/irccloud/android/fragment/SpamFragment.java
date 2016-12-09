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
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.model.Server;

import java.util.ArrayList;

public class SpamFragment extends DialogFragment {
    Server server;
    ArrayList<Buffer> buffers;
    ArrayList<Buffer> buffersToRemove;
    ListView listView;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context ctx = getActivity();
        if (ctx == null)
            return null;

        if(server == null && savedInstanceState != null && savedInstanceState.containsKey("cid"))
            server = ServersList.getInstance().getServer(savedInstanceState.getInt("cid"));

        if(server == null)
            throw new IllegalArgumentException("invalid CID");

        if(buffers == null && savedInstanceState != null && savedInstanceState.containsKey("buffers")) {
            buffers = (ArrayList<Buffer>) savedInstanceState.getSerializable("buffers");
        } else {
            buffers = new ArrayList<>();
            for(Buffer b : BuffersList.getInstance().getBuffersForServer(server.getCid())) {
                if(b.getArchived() == 0 && b.getType().equals("conversation"))
                    buffers.add(b);
            }
        }

        ArrayList<String> itemsList = new ArrayList<>();
        for(Buffer b : buffers) {
            itemsList.add(b.getName());
        }

        if(buffersToRemove == null && savedInstanceState != null && savedInstanceState.containsKey("buffersToRemove")) {
            buffersToRemove = (ArrayList<Buffer>) savedInstanceState.getSerializable("buffersToRemove");
        } else {
            buffersToRemove = new ArrayList<>(buffers);
        }

        CharSequence[] items = itemsList.toArray(new CharSequence[itemsList.size()]);
        LayoutInflater inflater = LayoutInflater.from(ctx);
        View v = inflater.inflate(R.layout.dialog_spam, null);
        listView = (ListView)v.findViewById(android.R.id.list);
        listView.setAdapter(new ArrayAdapter<>(ctx, R.layout.row_spam, items));

        for(int i = 0; i < buffers.size(); i++) {
            listView.setItemChecked(i, buffersToRemove.contains(buffers.get(i)));
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                SparseBooleanArray checked = listView.getCheckedItemPositions();
                buffersToRemove.clear();
                for(int i = 0; i < buffers.size(); i++) {
                    if(checked.get(i, false))
                        buffersToRemove.add(buffers.get(i));
                }
            }
        });

        return new AlertDialog.Builder(ctx)
                .setView(v)
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        for(Buffer b : buffersToRemove) {
                            NetworkConnection.getInstance().deleteBuffer(b.getCid(), b.getBid());
                        }

                        new AlertDialog.Builder(ctx)
                                .setTitle(server.getName() + " (" + server.getHostname() + ":" + (server.getPort()) + ")")
                                .setMessage(buffersToRemove.size() + " conversations were deleted")
                                .setNegativeButton("Close", null)
                                .show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
    }

    public void setCid(int cid) {
        server = ServersList.getInstance().getServer(cid);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(server != null)
            outState.putInt("cid", server.getCid());
        if(buffers != null)
            outState.putSerializable("buffers", buffers);
        if(buffersToRemove != null)
            outState.putSerializable("buffersToRemove", buffersToRemove);
    }
}