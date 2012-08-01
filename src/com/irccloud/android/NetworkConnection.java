package com.irccloud.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
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
	String session = null;

	public static final int EVENT_CONNECTIVITY = 0;
	public static final int EVENT_USERINFO = 1;
	public static final int EVENT_MAKESERVER = 2;
	public static final int EVENT_MAKEBUFFER = 3;
	public static final int EVENT_DELETEBUFFER = 4;
	public static final int EVENT_BUFFERMSG = 5;
	
	public static final int EVENT_BACKLOG_START = 100;
	public static final int EVENT_BACKLOG_END = 101;
	
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
	
	public String login(String email, String password) throws IOException {
		String postdata = "email="+email+"&password="+password;
		String response = doPost(new URL("https://alpha.irccloud.com/chat/login"), postdata);
		try {
			Log.d(TAG, "Result: " + response);
			JSONObject o = new JSONObject(response);
			return o.getString("session");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void connect(String sk) {
		session = sk;
		
		List<BasicNameValuePair> extraHeaders = Arrays.asList(
		    new BasicNameValuePair("Cookie", "session="+session)
		);

		client = new WebSocketClient(URI.create("wss://alpha.irccloud.com"), new WebSocketClient.Listener() {
		    @Override
		    public void onConnect() {
		        Log.d(TAG, "Connected!");
		        state = STATE_CONNECTED;
		        notifyHandlers(EVENT_CONNECTIVITY, null);
		    }

		    @Override
		    public void onMessage(String message) {
		    	if(message.length() > 0) {
					try {
			    		parse_object(new JSONObject(message), false);
					} catch (JSONException e) {
						Log.e(TAG, "Unable to parse: " + message);
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    	}
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
	
	public void say(int cid, String to, String message) {
		client.send("{\"_reqid\":12345, \"_method\": \"say\", \"cid\":"+cid+", \"to\":\""+to+"\", \"msg\":\""+message+"\"}\n");
	}
	
	private void parse_object(JSONObject object, boolean backlog) throws JSONException {
		//Log.d(TAG, "New event: " + object);
		String type = object.getString("type");
		if(type != null && type.length() > 0) {
			if(type.equalsIgnoreCase("stat_user")) {
				userInfo = new UserInfo(object);
				notifyHandlers(EVENT_USERINFO, userInfo);
			} else if(type.equalsIgnoreCase("makeserver")) {
				ServersDataSource s = ServersDataSource.getInstance();
				s.deleteServer(object.getInt("cid"));
				ServersDataSource.Server server = s.createServer(object.getInt("cid"), object.getString("name"), object.getString("hostname"),
						object.getInt("port"), object.getString("nick"), (object.has("connected") && object.getBoolean("connected"))?1:0);
				if(!backlog)
					notifyHandlers(EVENT_MAKESERVER, server);
			} else if(type.equalsIgnoreCase("makebuffer")) {
				BuffersDataSource b = BuffersDataSource.getInstance();
				b.deleteBuffer(object.getInt("bid"));
				BuffersDataSource.Buffer buffer = b.createBuffer(object.getInt("bid"), object.getInt("cid"),
						(object.has("max_eid") && !object.getString("max_eid").equalsIgnoreCase("undefined"))?object.getLong("max_eid"):0,
								(object.has("last_seen_eid") && !object.getString("last_seen_eid").equalsIgnoreCase("undefined"))?object.getLong("last_seen_eid"):-1, object.getString("name"), object.getString("buffer_type"), (object.has("archived") && object.getBoolean("archived"))?1:0, (object.has("deferred") && object.getBoolean("deferred"))?1:0);
				if(!backlog)
					notifyHandlers(EVENT_MAKEBUFFER, buffer);
			} else if(type.equalsIgnoreCase("delete_buffer")) {
				BuffersDataSource b = BuffersDataSource.getInstance();
				b.deleteBuffer(object.getInt("bid"));
				if(!backlog)
					notifyHandlers(EVENT_DELETEBUFFER, object.getInt("bid"));
			} else if(type.equalsIgnoreCase("buffer_msg")) {
				EventsDataSource e = EventsDataSource.getInstance();
				e.deleteEvent(object.getLong("eid"), object.getInt("bid"));
				EventsDataSource.Event event = e.createEvent(object.getLong("eid"), object.getInt("bid"), object.getInt("cid"), object.getString("type"), (object.has("highlight") && object.getBoolean("highlight"))?1:0, object);
				if(!backlog)
					notifyHandlers(EVENT_BUFFERMSG, event);
			} else if(type.equalsIgnoreCase("oob_include")) {
				try {
					Looper.prepare();
					new OOBIncludeTask().execute(new URL("https://alpha.irccloud.com" + object.getString("url")));
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				//Log.e(TAG, "Unhandled type: " + object);
			}
		}
	}
	
	private String doGet(URL url) throws IOException {
		Log.d(TAG, "Requesting: " + url);
		
		HttpURLConnection conn = null;

        if (url.getProtocol().toLowerCase().equals("https")) {
            HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
            conn = https;
        } else {
        	conn = (HttpURLConnection) url.openConnection();
        }
		conn.setRequestMethod("GET");
		conn.setRequestProperty("Connection", "close");
		conn.setRequestProperty("Cookie", "session="+session);
		conn.setRequestProperty("Accept", "application/json");
		conn.setRequestProperty("Content-type", "application/json");
		conn.setRequestProperty("Accept-Encoding", "gzip");
		BufferedReader reader = null;
		String response = "";
		conn.connect();
		try {
			if(conn.getInputStream() != null) {
				if(conn.getContentEncoding().equalsIgnoreCase("gzip"))
					reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(conn.getInputStream())), 512);
				else
					reader = new BufferedReader(new InputStreamReader(conn.getInputStream()), 512);
			}
		} catch (IOException e) {
			if(conn.getErrorStream() != null) {
				if(conn.getContentEncoding().equalsIgnoreCase("gzip"))
					reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(conn.getErrorStream())), 512);
				else
					reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()), 512);
			}
		}

		if(reader != null) {
			response = toString(reader);
			reader.close();
		}
		return response;
	}

	private String doPost(URL url, String postdata) throws IOException {
		Log.d(TAG, "POSTing to: " + url);
		
		HttpURLConnection conn = null;

        if (url.getProtocol().toLowerCase().equals("https")) {
            HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
            conn = https;
        } else {
        	conn = (HttpURLConnection) url.openConnection();
        }
		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setRequestProperty("Connection", "close");
		conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
		OutputStream ostr = null;
		try {
			ostr = conn.getOutputStream();
			ostr.write(postdata.getBytes());
		} finally {
			if (ostr != null)
				ostr.close();
		}

		BufferedReader reader = null;
		String response = "";
		conn.connect();
		try {
			if(conn.getInputStream() != null) {
				reader = new BufferedReader(new InputStreamReader(conn.getInputStream()), 512);
			}
		} catch (IOException e) {
			if(conn.getErrorStream() != null) {
				reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()), 512);
			}
		}

		if(reader != null) {
			response = toString(reader);
			reader.close();
		}
		return response;
	}

	private static String toString(BufferedReader reader) throws IOException {
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			sb.append(line).append('\n');
		}
		return sb.toString();
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
	
	private class OOBIncludeTask extends AsyncTask<URL, Void, Boolean> {
		String json = null;
		
		@Override
		protected Boolean doInBackground(URL... url) {
			try {
				json = doGet(url[0]);
				Log.i("IRCCloud", "Beginning backlog...");
				notifyHandlers(EVENT_BACKLOG_START, null);
				DBHelper.getInstance().beginBatch();
				JSONArray a = new JSONArray(json);
				for(int i = 0; i < a.length(); i++)
					parse_object(a.getJSONObject(i), true);
				DBHelper.getInstance().endBatch();
				Log.i("IRCCloud", "Backlog complete!");
				notifyHandlers(EVENT_BACKLOG_END, null);
				return true;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return false;
		}
		
		@Override
		public void onPostExecute(Boolean result) {
			Log.i("IRCCloud", "Hello?");
			try {
				if(result) {
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
