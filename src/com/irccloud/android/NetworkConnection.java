package com.irccloud.android;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.codebutler.android_websockets.WebSocketClient;

public class NetworkConnection {
	private static final String TAG = "IRCCloud";
	private static NetworkConnection instance = null;

	public static final int STATE_DISCONNECTED = 0;
	public static final int STATE_CONNECTING = 1;
	public static final int STATE_CONNECTED = 2;
	private int state = STATE_DISCONNECTED;

	WebSocketClient client = null;
	UserInfo userInfo = null;
	ArrayList<Handler> handlers = null;
	
	public static final int EVENT_CONNECTIVITY = 0;
	public static final int EVENT_USERINFO = 1;
	
	public static NetworkConnection getInstance() {
		if(instance == null) {
			instance = new NetworkConnection();
		}
		return instance;
	}
	
	public int getState() {
		return state;
	}
	
	public void disconnect() {
		if(client!=null)
			client.disconnect();
	}
	
	public void connect(String sk) {
		List<BasicNameValuePair> extraHeaders = Arrays.asList(
		    new BasicNameValuePair("Cookie", "session="+sk)
		);

		client = new WebSocketClient(URI.create("wss://irccloud.com"), new WebSocketClient.Listener() {
		    @Override
		    public void onConnect() {
		        Log.d(TAG, "Connected!");
		        state = STATE_CONNECTED;
		        notifyHandlers(EVENT_CONNECTIVITY, null);
		    }

		    @Override
		    public void onMessage(String message) {
		    	if(message.length() > 0)
		    		parse_message(message);
		    }

		    @Override
		    public void onMessage(byte[] data) {
		        //Log.d(TAG, String.format("Got binary message! %s", toHexString(data));
		    }

		    @Override
		    public void onDisconnect(int code, String reason) {
		        Log.d(TAG, String.format("Disconnected! Code: %d Reason: %s", code, reason));
		        state = STATE_DISCONNECTED;
		        notifyHandlers(EVENT_CONNECTIVITY, null);
		    }

		    @Override
		    public void onError(Exception error) {
		        Log.e(TAG, "Error!", error);
		    }
		}, extraHeaders);
		
		state = STATE_CONNECTING;
		notifyHandlers(EVENT_CONNECTIVITY, null);
		client.connect();
	}
	
	private void parse_message(String message) {
		try {
			JSONObject object = (JSONObject)new JSONTokener(message).nextValue();
			String type = object.getString("type");
			if(type != null && type.length() > 0) {
				Log.d(TAG, "New message! Type: " + type);
				if(type.equalsIgnoreCase("stat_user")) {
					userInfo = new UserInfo(object);
					notifyHandlers(EVENT_USERINFO, userInfo);
				}
			}
		} catch (JSONException e) {
			Log.e(TAG, "Unable to parse: " + message);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addHandler(Handler handler) {
		if(handlers == null)
			handlers = new ArrayList<Handler>();
		handlers.add(handler);
	}

	public void removeHandler(Handler handler) {
		handlers.remove(handler);
	}

	private void notifyHandlers(int message, Object object) {
		for(int i = 0; i < handlers.size(); i++) {
			Handler handler = handlers.get(i);
			Message msg = handler.obtainMessage(message, object);
			handler.sendMessage(msg);
		}
	}
	
	public UserInfo getUserInfo() {
		return userInfo;
	}
	
	public class UserInfo {
		String name;
		String email;
		boolean verified;
		long last_selected_bid;
		long connections;
		long active_connections;
		long join_date;
		boolean auto_away;
		String limits_name;
		long limit_networks;
		boolean limit_passworded_servers;
		long limit_zombiehours;
		boolean limit_download_logs;
		long limit_maxhistorydays;
		
		public UserInfo(JSONObject object) throws JSONException {
			name = object.getString("name");
			email = object.getString("email");
			verified = object.getBoolean("verified");
			last_selected_bid = object.getLong("last_selected_bid");
			connections = object.getLong("num_connections");
			active_connections = object.getLong("num_active_connections");
			join_date = object.getLong("join_date");
			auto_away = object.getBoolean("autoaway");
			
			limits_name = object.getString("limits_name");
			JSONObject limits = object.getJSONObject("limits");
			limit_networks = limits.getLong("networks");
			limit_passworded_servers = limits.getBoolean("passworded_servers");
			limit_zombiehours = limits.getLong("zombiehours");
			limit_download_logs = limits.getBoolean("download_logs");
			limit_maxhistorydays = limits.getLong("maxhistorydays");
		}
	}
}
