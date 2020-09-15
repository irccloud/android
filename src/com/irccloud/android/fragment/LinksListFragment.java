/*
 * Copyright (c) 2016 IRCCloud, Ltd.
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.irccloud.android.R;
import com.irccloud.android.databinding.RowHostmaskBinding;

import org.solovyev.android.views.llm.LinearLayoutManager;

import java.io.IOException;

public class LinksListFragment extends DialogFragment {
    JsonNode event;
    LinksAdapter adapter;
    RecyclerView recyclerView;
    ObjectMapper mapper = new ObjectMapper();

    private class ViewHolder extends RecyclerView.ViewHolder {
        public RowHostmaskBinding binding;

        public ViewHolder(View v) {
            super(v);
            binding = DataBindingUtil.bind(v);
        }
    }

    private class LinksAdapter extends RecyclerView.Adapter<ViewHolder> {
        @Override
        public int getItemCount() {
            return event.get("links").size();
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = RowHostmaskBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false).getRoot();
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            JsonNode link = event.get("links").get(position);
            RowHostmaskBinding row = holder.binding;
            row.setLabel(link.get("server").asText() + "\n" + link.get("info").asText() + "\nHops: " + link.get("hopcount").asText());
            row.removeBtn.setVisibility(View.GONE);
            row.executePendingBindings();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context ctx = getActivity();
        if(ctx == null)
            return null;

        if (savedInstanceState != null && savedInstanceState.containsKey("event")) {
            try {
                event = mapper.readValue(savedInstanceState.getString("event"), JsonNode.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.recyclerview, null);
        recyclerView = v.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
        recyclerView.setVisibility(View.VISIBLE);
        adapter = new LinksAdapter();
        recyclerView.setAdapter(adapter);
        v.findViewById(android.R.id.empty).setVisibility(View.GONE);
        Dialog d = new AlertDialog.Builder(ctx)
                .setTitle("Servers linked to " + event.get("server").asText())
                .setView(v)
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return d;
    }

    @Override
    public void onSaveInstanceState(Bundle state) {
        state.putString("event", event.toString());
    }

    @Override
    public void setArguments(Bundle args) {
        if(args != null) {
            try {
                event = mapper.readValue(args.getString("event"), JsonNode.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (getActivity() != null && event != null && recyclerView != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter = new LinksAdapter();
                        recyclerView.setAdapter(adapter);
                    }
                });
            }
        }
    }
}
