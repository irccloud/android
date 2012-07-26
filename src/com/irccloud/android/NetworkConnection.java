package com.irccloud.android;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;
import com.codebutler.android_websockets.WebSocketClient;

public class NetworkConnection {
	private static final String TAG = "IRCCloud";
	private static NetworkConnection instance = null;

	public static final int NETWORK_STATE_DISCONNECTED = 0;
	public static final int NETWORK_STATE_CONNECTING = 1;
	public static final int NETWORK_STATE_CONNECTED = 2;
	private int state = NETWORK_STATE_DISCONNECTED;

	WebSocketClient client = null;
	
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
		        state = NETWORK_STATE_CONNECTED;
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
		        state = NETWORK_STATE_DISCONNECTED;
		    }

		    @Override
		    public void onError(Exception error) {
		        Log.e(TAG, "Error!", error);
		    }
		}, extraHeaders);
		
		state = NETWORK_STATE_CONNECTING;
		client.connect();
	}
	
	private void parse_message(String message) {
		try {
			JSONObject object = (JSONObject) new JSONTokener(message).nextValue();
			Log.d(TAG, "New message! Type: " + object.getString("type"));
		} catch (JSONException e) {
			Log.e(TAG, "Unable to parse: " + message);
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
