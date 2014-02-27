/*
 * Copyright (c) 2014 IRCCloud, Ltd.
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.SparseArray;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.ServersDataSource;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.Collections;

public class ServerReorderFragment extends DialogFragment implements NetworkConnection.IRCEventHandler {
	private NetworkConnection conn;
	private ServerListAdapter adapter;
	private RefreshTask refreshTask = null;
    private DragSortListView listView = null;
    private DragSortListView.DropListener dropListener = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            ServersDataSource.Server s = adapter.data.get(from);
            adapter.data.remove(from);
            if(to >= adapter.data.size())
                adapter.data.add(s);
            else
                adapter.data.add(to, s);
            adapter.notifyDataSetChanged();

            String cids = "";
            for(int i = 0; i < adapter.data.size(); i++) {
                s = adapter.data.get(i);
                s.order = i + 1;
                if(cids.length() > 0)
                    cids += ",";
                cids += s.cid;
            }
            NetworkConnection.getInstance().reorder_connections(cids);
        }
    };

    private class ServerListAdapter extends BaseAdapter {
		ArrayList<ServersDataSource.Server> data;
		private DialogFragment ctx;
        int width = 0;
		
		private class ViewHolder {
			TextView label;
            ImageView icon;
		}
	
		public ServerListAdapter(DialogFragment context) {
			ctx = context;
			data = new ArrayList<ServersDataSource.Server>();
            WindowManager wm = (WindowManager) IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            width = wm.getDefaultDisplay().getWidth();
		}
		
		public void setItems(ArrayList<ServersDataSource.Server> items) {
			data = items;
		}
		
		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public Object getItem(int position) {
			return data.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
            ServersDataSource.Server s = data.get(position);
			View row = convertView;
			ViewHolder holder;

			if (row == null) {
                LayoutInflater inflater = ctx.getLayoutInflater(null);
                row = inflater.inflate(R.layout.row_reorder, null);

				holder = new ViewHolder();
				holder.label = (TextView) row.findViewById(R.id.label);
                holder.icon = (ImageView) row.findViewById(R.id.icon);

				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}

            if(s.name != null && s.name.length() > 0)
    			holder.label.setText(s.name);
            else
                holder.label.setText(s.hostname);

            if(s.ssl > 0)
                holder.icon.setImageResource(R.drawable.world_shield);
            else
                holder.icon.setImageResource(R.drawable.world);

            if(getShowsDialog())
                row.setBackgroundColor(0xfff3f3f3);
            else
                row.setBackgroundResource(R.drawable.bg);

            holder.label.setMinimumWidth(width);
			return row;
		}
	}

	private void refresh(ArrayList<ServersDataSource.Server> servers) {
		if(servers == null) {
			if(adapter != null) {
				adapter.data.clear();
				adapter.notifyDataSetInvalidated();
			}
			return;
		}
		
		if(adapter == null) {
			adapter = new ServerListAdapter(ServerReorderFragment.this);
		}

		adapter.setItems(servers);
		
		if(listView.getAdapter() == null)
			listView.setAdapter(adapter);
		else
			adapter.notifyDataSetChanged();
	}
	
	private class RefreshTask extends AsyncTaskEx<Void, Void, Void> {
		ArrayList<ServersDataSource.Server> servers = new ArrayList<ServersDataSource.Server>();
		
		@Override
		protected Void doInBackground(Void... params) {
            SparseArray<ServersDataSource.Server> s = ServersDataSource.getInstance().getServers();
            for(int i = 0; i < s.size(); i++) {
                servers.add(s.valueAt(i));
            }
            Collections.sort(servers);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			if(!isCancelled()) {
				refresh(servers);
				refreshTask = null;
			}
		}
	}

    private void init(View v) {
        listView = (DragSortListView)v.findViewById(android.R.id.list);
        listView.setDropListener(dropListener);
        TextView tv = (TextView)v.findViewById(R.id.hint);
        Spannable s = new SpannableString(tv.getText());
        s.setSpan(new ImageSpan(getActivity(), R.drawable.move), 5, 6, 0);
        tv.setText(s);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(getShowsDialog()) {
            return super.onCreateView(inflater, container, savedInstanceState);
        } else {
            final View v = inflater.inflate(R.layout.reorderservers, null);
            init(v);
            listView.setCacheColorHint(0xFFD9E7FF);
            return v;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context ctx = getActivity();
        if(Build.VERSION.SDK_INT < 11)
            ctx = new ContextThemeWrapper(ctx, android.R.style.Theme_Dialog);

        LayoutInflater inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.reorderservers, null);
        init(v);
        listView.setCacheColorHint(0xfff3f3f3);

        Dialog d = new AlertDialog.Builder(ctx)
                .setTitle("Connections")
                .setView(v)
                .setNegativeButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        NetworkConnection.getInstance().removeHandler(ServerReorderFragment.this);
                    }
                })
                .create();
        return d;
    }

	public void onResume() {
    	super.onResume();
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(this);
        ArrayList<ServersDataSource.Server> servers = new ArrayList<ServersDataSource.Server>();
        SparseArray<ServersDataSource.Server> s = ServersDataSource.getInstance().getServers();
        for(int i = 0; i < s.size(); i++) {
            servers.add(s.valueAt(i));
        }
        Collections.sort(servers);
        refresh(servers);
    }
    
    public void onPause() {
    	super.onPause();
    	if(conn != null)
    		conn.removeHandler(this);
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    public void onIRCEvent(int what, Object obj) {
        switch (what) {
        case NetworkConnection.EVENT_MAKESERVER:
        case NetworkConnection.EVENT_REORDERCONNECTIONS:
        case NetworkConnection.EVENT_CONNECTIONDELETED:
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(refreshTask != null)
                        refreshTask.cancel(true);
                    refreshTask = new RefreshTask();
                    refreshTask.execute((Void)null);
                }
            });
            break;
        default:
            break;
        }
    }
}
