package com.irccloud.androidnative;

import org.json.JSONArray;
import org.json.JSONException;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;

public class IgnoreListFragment extends SherlockListFragment {
	JSONArray ignores;
	int cid;
	IgnoresAdapter adapter;
	NetworkConnection conn;
	
	private class IgnoresAdapter extends BaseAdapter {
		private SherlockListFragment ctx;
		
		private class ViewHolder {
			int position;
			TextView label;
			Button removeBtn;
		}
	
		public IgnoresAdapter(SherlockListFragment context) {
			ctx = context;
		}
		
		@Override
		public int getCount() {
			return ignores.length();
		}

		@Override
		public Object getItem(int pos) {
			try {
				return ignores.get(pos);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public long getItemId(int pos) {
			return pos;
		}

		OnClickListener removeClickListener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				Integer position = (Integer)v.getTag();
				try {
					conn.unignore(cid, ignores.getString(position));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			ViewHolder holder;

			if(row != null && ((ViewHolder)row.getTag()).position != position)
				row = null;
			
			if (row == null) {
				LayoutInflater inflater = ctx.getLayoutInflater(null);
				row = inflater.inflate(R.layout.row_hostmask, null);

				holder = new ViewHolder();
				holder.position = position;
				holder.label = (TextView) row.findViewById(R.id.label);
				holder.removeBtn = (Button) row.findViewById(R.id.removeBtn);

				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}
			
			try {
				holder.label.setText(ignores.getString(position));
				holder.removeBtn.setOnClickListener(removeClickListener);
				holder.removeBtn.setTag(position);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return row;
		}
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState.containsKey("cid")) {
        	cid = savedInstanceState.getInt("cid");
        	ignores = ServersDataSource.getInstance().getServer(cid).ignores;
        	adapter = new IgnoresAdapter(this);
        	setListAdapter(adapter);
        }
    }
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
	        Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.userslist, null);
		return view;
	}
	
    @Override
    public void onSaveInstanceState(Bundle state) {
    	state.putInt("cid", cid);
    }
	
    public void onResume() {
    	super.onResume();
    	conn = NetworkConnection.getInstance();
    	conn.addHandler(mHandler);
    	
    	if(ignores == null && getActivity().getIntent() != null && getActivity().getIntent().hasExtra("cid")) {
    		cid = getActivity().getIntent().getIntExtra("cid", -1);
        	ignores = ServersDataSource.getInstance().getServer(cid).ignores;
        	adapter = new IgnoresAdapter(this);
        	setListAdapter(adapter);
    	}
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	if(conn != null)
    		conn.removeHandler(mHandler);
    }
    
	private final Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case NetworkConnection.EVENT_MAKESERVER:
				ServersDataSource.Server s = (ServersDataSource.Server)msg.obj;
				if(s.cid == cid) {
		        	ignores = ServersDataSource.getInstance().getServer(cid).ignores;
		        	adapter.notifyDataSetChanged();
				}
				break;
			case NetworkConnection.EVENT_SETIGNORES:
				IRCCloudJSONObject o = (IRCCloudJSONObject)msg.obj;
				if(o.cid() == cid) {
		        	ignores = ServersDataSource.getInstance().getServer(cid).ignores;
		        	adapter.notifyDataSetChanged();
				}
				break;
			default:
				break;
			}
		}
	};
}
