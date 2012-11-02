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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;

import com.codebutler.android_websockets.WebSocketClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public class NetworkConnection {
	private static final String TAG = "IRCCloud";
	private static NetworkConnection instance = null;

	public static final int WEBSOCKET_TAG = 0x50C37;
	public static final int BACKLOG_TAG = 0xB4C106;
	
	public static final int STATE_DISCONNECTED = 0;
	public static final int STATE_CONNECTING = 1;
	public static final int STATE_CONNECTED = 2;
	public static final int STATE_DISCONNECTING = 3;
	private int state = STATE_DISCONNECTED;

	private WebSocketClient client = null;
	private UserInfo userInfo = null;
	private ArrayList<Handler> handlers = null;
	private String session = null;
	private int last_reqid = 0;
	private Timer shutdownTimer = null;
	private Timer idleTimer = null;
	private long idle_interval = 30000;
	private long reconnect_timestamp = 0;
	private String useragent = null;
	
	public static final int EVENT_CONNECTIVITY = 0;
	public static final int EVENT_USERINFO = 1;
	public static final int EVENT_MAKESERVER = 2;
	public static final int EVENT_MAKEBUFFER = 3;
	public static final int EVENT_DELETEBUFFER = 4;
	public static final int EVENT_BUFFERMSG = 5;
	public static final int EVENT_HEARTBEATECHO = 6;
	public static final int EVENT_CHANNELINIT = 7;
	public static final int EVENT_CHANNELTOPIC = 8;
	public static final int EVENT_JOIN = 9;
	public static final int EVENT_PART = 10;
	public static final int EVENT_NICKCHANGE = 11;
	public static final int EVENT_QUIT = 12;
	public static final int EVENT_MEMBERUPDATES = 13;
	public static final int EVENT_USERCHANNELMODE = 14;
	public static final int EVENT_BUFFERARCHIVED = 15;
	public static final int EVENT_BUFFERUNARCHIVED = 16;
	public static final int EVENT_RENAMECONVERSATION = 17;
	public static final int EVENT_STATUSCHANGED = 18;
	public static final int EVENT_CONNECTIONDELETED = 19;
	public static final int EVENT_AWAY = 20;
	public static final int EVENT_SELFBACK = 21;
	public static final int EVENT_KICK = 22;
	public static final int EVENT_CHANNELMODE = 23;
	public static final int EVENT_CHANNELTIMESTAMP = 24;
	public static final int EVENT_SELFDETAILS = 25;
	public static final int EVENT_USERMODE = 26;
	public static final int EVENT_SETIGNORES = 27;
	public static final int EVENT_BADCHANNELKEY = 28;
	public static final int EVENT_OPENBUFFER = 29;
	public static final int EVENT_NOSUCHCHANNEL = 30;
	public static final int EVENT_NOSUCHNICK = 31;
	public static final int EVENT_TOOMANYCHANNELS = 32;
	public static final int EVENT_INVALIDNICK = 33;
	public static final int EVENT_BANLIST = 34;
	public static final int EVENT_CHANPRIVSNEEDED = 35;
	
	public static final int EVENT_BACKLOG_START = 100;
	public static final int EVENT_BACKLOG_END = 101;
	public static final int EVENT_FAILURE_MSG = 102;
	public static final int EVENT_SUCCESS = 103;
	
	private static final String IRCCLOUD_HOST = "alpha.irccloud.com";
	
	private Object parserLock = new Object();
	private WifiManager.WifiLock wifiLock = null;
	
	public static NetworkConnection getInstance() {
		if(instance == null) {
			instance = new NetworkConnection();
		}
		return instance;
	}

	@SuppressWarnings("deprecation")
	public NetworkConnection() {
		String version;
		String network_type = null;
		try {
			version = "/" + IRCCloudApplication.getInstance().getPackageManager().getPackageInfo("com.irccloud.android", 0).versionName;
		} catch (Exception e) {
			version = "";
		}

		ConnectivityManager cm = (ConnectivityManager)IRCCloudApplication.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if(ni != null)
			network_type = ni.getTypeName();
		
		useragent = "IRCCloud" + version + " (" + android.os.Build.MODEL + "; " + Locale.getDefault().getCountry().toLowerCase() + "; "
				+ "Android " + android.os.Build.VERSION.RELEASE;
		
		WindowManager wm = (WindowManager)IRCCloudApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
		useragent += "; " + wm.getDefaultDisplay().getWidth() + "x" + wm.getDefaultDisplay().getHeight();
		
		if(network_type != null)
			useragent += "; " + network_type;
		
		useragent += ")";
		
		WifiManager wfm = (WifiManager) IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		wifiLock = wfm.createWifiLock("IRCCloud");
	}
	
	public int getState() {
		return state;
	}
	
	public void disconnect() {
		if(client!=null) {
			state = STATE_DISCONNECTING;
			client.disconnect();
		} else {
			state = STATE_DISCONNECTED;
		}
		if(idleTimer != null) {
			idleTimer.cancel();
			idleTimer = null;
		}
		if(wifiLock.isHeld())
			wifiLock.release();
	}
	
	public JSONObject login(String email, String password) throws IOException {
		String postdata = "email="+email+"&password="+password;
		String response = doPost(new URL("https://" + IRCCLOUD_HOST + "/chat/login"), postdata);
		try {
			Log.d(TAG, "Result: " + response);
			JSONObject o = new JSONObject(response);
			return o;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public JSONObject registerGCM(String regId) throws IOException {
		String postdata = "device_id="+regId+"&session="+session;
		String response = doPost(new URL("https://" + IRCCLOUD_HOST + "/gcm-register"), postdata);
		try {
			Log.d(TAG, "Result: " + response);
			JSONObject o = new JSONObject(response);
			return o;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public void connect(String sk) {
		session = sk;
		if(!wifiLock.isHeld())
			wifiLock.acquire();
		
		List<BasicNameValuePair> extraHeaders = Arrays.asList(
		    new BasicNameValuePair("Cookie", "session="+session),
		    new BasicNameValuePair("User-Agent", useragent)
		);

		String url = "wss://" + IRCCLOUD_HOST;
		if(EventsDataSource.getInstance().highest_eid > 0)
			url += "?since_id=" + EventsDataSource.getInstance().highest_eid;

		Log.d("IRCCloud", "Opening websocket: " + url);
		
		client = new WebSocketClient(URI.create(url), new WebSocketClient.Listener() {
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
						synchronized(parserLock) {
							parse_object(new IRCCloudJSONObject(message), false);
						}
					} catch (Exception e) {
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
		        if(state == STATE_DISCONNECTING)
		        	cancel_idle_timer();
		        else
		        	schedule_idle_timer();
		        	
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
		client.setSocketTag(WEBSOCKET_TAG);
		client.connect();
	}
	
	public int heartbeat(long selected_buffer, int cid, long bid, long last_seen_eid) {
		try {
			JSONObject o = new JSONObject();
			o.put("_reqid", ++last_reqid);
			o.put("_method", "heartbeat");
			o.put("selectedBuffer", selected_buffer);
			JSONObject eids = new JSONObject();
			eids.put(String.valueOf(bid), last_seen_eid);
			JSONObject cids = new JSONObject();
			cids.put(String.valueOf(cid), eids);
			o.put("seenEids", cids.toString());
			client.send(o.toString());
			Log.d("IRCCloud", "Reqid: " + last_reqid + " Method: heartbeat");
			return last_reqid;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int disconnect(int cid, String message) {
		try {
			JSONObject o = new JSONObject();
			o.put("_reqid", ++last_reqid);
			o.put("_method", "disconnect");
			o.put("cid", cid);
			o.put("msg", message);
			client.send(o.toString());
			Log.d("IRCCloud", "Reqid: " + last_reqid + " Method: disconnect");
			return last_reqid;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int reconnect(int cid) {
		try {
			JSONObject o = new JSONObject();
			o.put("_reqid", ++last_reqid);
			o.put("_method", "reconnect");
			o.put("cid", cid);
			client.send(o.toString());
			Log.d("IRCCloud", "Reqid: " + last_reqid + " Method: reconnect");
			return last_reqid;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int say(int cid, String to, String message) {
		try {
			JSONObject o = new JSONObject();
			o.put("_reqid", ++last_reqid);
			o.put("_method", "say");
			o.put("cid", cid);
			if(to != null)
				o.put("to", to);
			o.put("msg", message);
			client.send(o.toString());
			Log.d("IRCCloud", "Reqid: " + last_reqid + " Method: say");
			return last_reqid;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int join(int cid, String channel, String key) {
		try {
			JSONObject o = new JSONObject();
			o.put("_reqid", ++last_reqid);
			o.put("_method", "join");
			o.put("cid", cid);
			o.put("channel", channel);
			o.put("key", key);
			Log.d("IRCCloud", "Reqid: " + last_reqid + " Method: join");
			client.send(o.toString());
			return last_reqid;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int part(int cid, String channel, String message) {
		try {
			JSONObject o = new JSONObject();
			o.put("_reqid", ++last_reqid);
			o.put("_method", "part");
			o.put("cid", cid);
			o.put("channel", channel);
			o.put("msg", message);
			client.send(o.toString());
			Log.d("IRCCloud", "Reqid: " + last_reqid + " Method: part");
			return last_reqid;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int kick(int cid, String channel, String nick, String message) {
		return say(cid, channel, "/kick " + nick + " " + message);
	}
	
	public int mode(int cid, String channel, String mode) {
		return say(cid, channel, "/mode " + channel + " " + mode);
	}
	
	public int invite(int cid, String channel, String nick) {
		return say(cid, channel, "/invite " + nick + " " + channel);
	}
	
	public int archiveBuffer(int cid, long bid) {
		try {
			JSONObject o = new JSONObject();
			o.put("_reqid", ++last_reqid);
			o.put("_method", "archive-buffer");
			o.put("cid", cid);
			o.put("id", bid);
			client.send(o.toString());
			Log.d("IRCCloud", "Reqid: " + last_reqid + " Method: archive-buffer");
			return last_reqid;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int unarchiveBuffer(int cid, long bid) {
		try {
			JSONObject o = new JSONObject();
			o.put("_reqid", ++last_reqid);
			o.put("_method", "unarchive-buffer");
			o.put("cid", cid);
			o.put("id", bid);
			client.send(o.toString());
			Log.d("IRCCloud", "Reqid: " + last_reqid + " Method: unarchive-buffer");
			return last_reqid;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int deleteBuffer(int cid, long bid) {
		try {
			JSONObject o = new JSONObject();
			o.put("_reqid", ++last_reqid);
			o.put("_method", "delete-buffer");
			o.put("cid", cid);
			o.put("id", bid);
			client.send(o.toString());
			Log.d("IRCCloud", "Reqid: " + last_reqid + " Method: delete-buffer");
			return last_reqid;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int deleteServer(int cid) {
		try {
			JSONObject o = new JSONObject();
			o.put("_reqid", ++last_reqid);
			o.put("_method", "delete-connection");
			o.put("cid", cid);
			client.send(o.toString());
			Log.d("IRCCloud", "Reqid: " + last_reqid + " Method: delete-connection");
			return last_reqid;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int addServer(String hostname, int port, int ssl, String netname, String nickname, String realname, String server_pass, String nickserv_pass, String joincommands, String channels) {
		try {
			JSONObject o = new JSONObject();
			o.put("_reqid", ++last_reqid);
			o.put("_method", "add-server");
			o.put("hostname", hostname);
			o.put("port", port);
			o.put("ssl", ssl);
			o.put("netname", netname);
			o.put("nickname", nickname);
			o.put("realname", realname);
			o.put("server_pass", server_pass);
			o.put("nspass", nickserv_pass);
			o.put("joincommands", joincommands);
			o.put("channels", channels);
			client.send(o.toString());
			Log.d("IRCCloud", "Reqid: " + last_reqid + " Method: add-server");
			return last_reqid;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int editServer(int cid, String hostname, int port, int ssl, String netname, String nickname, String realname, String server_pass, String nickserv_pass, String joincommands) {
		try {
			JSONObject o = new JSONObject();
			o.put("_reqid", ++last_reqid);
			o.put("_method", "edit-server");
			o.put("hostname", hostname);
			o.put("port", port);
			o.put("ssl", ssl);
			o.put("netname", netname);
			o.put("nickname", nickname);
			o.put("realname", realname);
			o.put("server_pass", server_pass);
			o.put("nspass", nickserv_pass);
			o.put("joincommands", joincommands);
			o.put("cid", cid);
			client.send(o.toString());
			Log.d("IRCCloud", "Reqid: " + last_reqid + " Method: edit-server");
			return last_reqid;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int ignore(int cid, String mask) {
		try {
			JSONObject o = new JSONObject();
			o.put("_reqid", ++last_reqid);
			o.put("_method", "ignore");
			o.put("cid", cid);
			o.put("mask", mask);
			client.send(o.toString());
			Log.d("IRCCloud", "Reqid: " + last_reqid + " Method: ignore");
			return last_reqid;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int unignore(int cid, String mask) {
		try {
			JSONObject o = new JSONObject();
			o.put("_reqid", ++last_reqid);
			o.put("_method", "unignore");
			o.put("cid", cid);
			o.put("mask", mask);
			client.send(o.toString());
			Log.d("IRCCloud", "Reqid: " + last_reqid + " Method: unignore");
			return last_reqid;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int set_prefs(String prefs) {
		try {
			JSONObject o = new JSONObject();
			o.put("_reqid", ++last_reqid);
			o.put("_method", "set-prefs");
			o.put("prefs", prefs);
			client.send(o.toString());
			Log.d("IRCCloud", "Reqid: " + last_reqid + " Method: set-prefs");
			return last_reqid;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int set_user_settings(String email, String realname, String hwords, boolean autoaway) {
		try {
			JSONObject o = new JSONObject();
			o.put("_reqid", ++last_reqid);
			o.put("_method", "user-settings");
			o.put("email", email);
			o.put("realname", realname);
			o.put("hwords", hwords);
			o.put("autoaway", autoaway?"1":"0");
			client.send(o.toString());
			Log.d("IRCCloud", "Reqid: " + last_reqid + " Method: user-settings");
			return last_reqid;
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public void request_backlog(int cid, long bid, long beforeId) {
		try {
			if(Looper.myLooper() == null)
				Looper.prepare();
			if(beforeId > 0)
				new OOBIncludeTask().execute(new URL("https://" + IRCCLOUD_HOST + "/chat/backlog?cid="+cid+"&bid="+bid+"&beforeid="+beforeId));
			else
				new OOBIncludeTask().execute(new URL("https://" + IRCCLOUD_HOST + "/chat/backlog?cid="+cid+"&bid="+bid));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void cancel_idle_timer() {
		if(idleTimer != null) {
			idleTimer.cancel();
			idleTimer = null;
		}
	}
	
	public void schedule_idle_timer() {
		if(idleTimer != null) {
			idleTimer.cancel();
			idleTimer = null;
		}
		if(idle_interval <= 0)
			return;
		
		idleTimer = new Timer();

		idleTimer.schedule( new TimerTask(){
             public void run() {
            	 if(handlers.size() > 0) {
	            	 Log.i("IRCCloud", "Websocket idle time exceeded, reconnecting...");
	            	 state = STATE_CONNECTING;
	            	 notifyHandlers(EVENT_CONNECTIVITY, null);
	            	 client.disconnect();
	            	 connect(session);
            	 }
                 idleTimer = null;
                 reconnect_timestamp = 0;
              }
           }, idle_interval + 10000);
		reconnect_timestamp = System.currentTimeMillis() + idle_interval + 10000;
	}
	
	public long getReconnectTimestamp() {
		return reconnect_timestamp;
	}
	
	private void parse_object(IRCCloudJSONObject object, boolean backlog) throws JSONException {
		cancel_idle_timer();
		//Log.d(TAG, "New event: " + object);
		if(!object.has("type")) { //TODO: This is probably a command response, parse it and send the result back up to the UI!
			Log.d(TAG, "Response: " + object);
			if(object.has("success") && !object.getBoolean("success") && object.has("message")) {
				notifyHandlers(EVENT_FAILURE_MSG, object);
			} else if(object.has("success")) {
				notifyHandlers(EVENT_SUCCESS, object);
			}
			return;
		}
		String type = object.type();
		if(type != null && type.length() > 0) {
			if(type.equalsIgnoreCase("header")) {
				idle_interval = object.getLong("idle_interval");
				Notifications.getInstance().clear();
			} else if(type.equalsIgnoreCase("idle") || type.equalsIgnoreCase("end_of_backlog") || type.equalsIgnoreCase("backlog_complete")) {
			} else if(type.equalsIgnoreCase("num_invites")) {
				if(userInfo != null)
					userInfo.num_invites = object.getInt("num_invites");
			} else if(type.equalsIgnoreCase("stat_user")) {
				userInfo = new UserInfo(object);
				SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).edit();
				prefs.putString("name", userInfo.name);
				prefs.putString("email", userInfo.email);
				prefs.putString("highlights", userInfo.highlights);
				prefs.putBoolean("autoaway", userInfo.auto_away);
				if(userInfo.prefs != null) {
					prefs.putBoolean("time-24hr", userInfo.prefs.has("time-24hr")?userInfo.prefs.getBoolean("time-24hr"):false);
					prefs.putBoolean("time-seconds", userInfo.prefs.has("time-seconds")?userInfo.prefs.getBoolean("time-seconds"):false);
					prefs.putBoolean("mode-showsymbol", userInfo.prefs.has("mode-showsymbol")?userInfo.prefs.getBoolean("mode-showsymbol"):false);
				} else {
					prefs.putBoolean("time-24hr", false);
					prefs.putBoolean("time-seconds", false);
					prefs.putBoolean("mode-showsymbol", false);
				}
				prefs.commit();
				notifyHandlers(EVENT_USERINFO, userInfo);
			} else if(type.equalsIgnoreCase("bad_channel_key")) {
				notifyHandlers(EVENT_BADCHANNELKEY, object);
			} else if(type.equalsIgnoreCase("too_many_channels")) {
				notifyHandlers(EVENT_TOOMANYCHANNELS, object);
			} else if(type.equalsIgnoreCase("open_buffer")) {
				notifyHandlers(EVENT_OPENBUFFER, object);
			} else if(type.equalsIgnoreCase("no_such_channel")) {
				notifyHandlers(EVENT_NOSUCHCHANNEL, object);
			} else if(type.equalsIgnoreCase("no_such_nick")) {
				notifyHandlers(EVENT_NOSUCHNICK, object);
			} else if(type.equalsIgnoreCase("invalid_nick")) {
				notifyHandlers(EVENT_INVALIDNICK, object);
			} else if(type.equalsIgnoreCase("ban_list")) {
				notifyHandlers(EVENT_BANLIST, object);
			} else if(type.equalsIgnoreCase("chan_privs_needed")) {
				notifyHandlers(EVENT_CHANPRIVSNEEDED, object);
			} else if(type.equalsIgnoreCase("makeserver") || type.equalsIgnoreCase("server_details_changed")) {
				ServersDataSource s = ServersDataSource.getInstance();
				s.deleteServer(object.getInt("cid"));
				ServersDataSource.Server server = s.createServer(object.getInt("cid"), object.getString("name"), object.getString("hostname"),
						object.getInt("port"), object.getString("nick"), object.getString("status"), object.getString("lag").equalsIgnoreCase("undefined")?0:object.getLong("lag"), object.getBoolean("ssl")?1:0,
								object.getString("realname"), object.getString("server_pass"), object.getString("nickserv_pass"), object.getString("join_commands"),
								object.getJsonObject("fail_info"), object.getString("away"), object.getJsonArray("ignores"));
				Notifications.getInstance().deleteNetwork(object.getInt("cid"));
				if(object.getString("name") != null && object.getString("name").length() > 0)
					Notifications.getInstance().addNetwork(object.getInt("cid"), object.getString("name"));
				else
					Notifications.getInstance().addNetwork(object.getInt("cid"), object.getString("hostname"));

				if(!backlog)
					notifyHandlers(EVENT_MAKESERVER, server);
			} else if(type.equalsIgnoreCase("connection_deleted")) {
				ServersDataSource s = ServersDataSource.getInstance();
				s.deleteAllDataForServer(object.getInt("cid"));
				if(!backlog)
					notifyHandlers(EVENT_CONNECTIONDELETED, object.getInt("cid"));
			} else if(type.equalsIgnoreCase("makebuffer")) {
				BuffersDataSource b = BuffersDataSource.getInstance();
				ChannelsDataSource c = ChannelsDataSource.getInstance();
				b.deleteBuffer(object.getInt("bid"));
				c.deleteChannel(object.getInt("bid"));
				BuffersDataSource.Buffer buffer = b.createBuffer(object.getInt("bid"), object.getInt("cid"),
						(object.has("min_eid") && !object.getString("min_eid").equalsIgnoreCase("undefined"))?object.getLong("min_eid"):0,
								(object.has("last_seen_eid") && !object.getString("last_seen_eid").equalsIgnoreCase("undefined"))?object.getLong("last_seen_eid"):-1, object.getString("name"), object.getString("buffer_type"), (object.has("archived") && object.getBoolean("archived"))?1:0, (object.has("deferred") && object.getBoolean("deferred"))?1:0);
				if(!backlog)
					notifyHandlers(EVENT_MAKEBUFFER, buffer);
				if(object.getString("buffer_type") == null)
					Log.w("IRCCloud", "NULL buffer type! JSON: " + object.toString());
			} else if(type.equalsIgnoreCase("delete_buffer")) {
				BuffersDataSource b = BuffersDataSource.getInstance();
				b.deleteAllDataForBuffer(object.getInt("bid"));
				if(!backlog)
					notifyHandlers(EVENT_DELETEBUFFER, object.getInt("bid"));
			} else if(type.equalsIgnoreCase("buffer_archived")) {
				BuffersDataSource b = BuffersDataSource.getInstance();
				b.updateArchived(object.getInt("bid"), 1);
				if(!backlog)
					notifyHandlers(EVENT_BUFFERARCHIVED, object.getInt("bid"));
			} else if(type.equalsIgnoreCase("buffer_unarchived")) {
				BuffersDataSource b = BuffersDataSource.getInstance();
				b.updateArchived(object.getInt("bid"), 0);
				if(!backlog)
					notifyHandlers(EVENT_BUFFERUNARCHIVED, object.getInt("bid"));
			} else if(type.equalsIgnoreCase("rename_conversation")) {
				BuffersDataSource b = BuffersDataSource.getInstance();
				b.updateName(object.getInt("bid"), object.getString("new_name"));
				if(!backlog)
					notifyHandlers(EVENT_RENAMECONVERSATION, object.getInt("bid"));
			} else if(type.equalsIgnoreCase("status_changed")) {
				ServersDataSource s = ServersDataSource.getInstance();
				s.updateStatus(object.getInt("cid"), object.getString("new_status"), object.getJsonObject("fail_info"));
				if(!backlog)
					notifyHandlers(EVENT_STATUSCHANGED, object);
			} else if(type.equalsIgnoreCase("buffer_msg") || type.equalsIgnoreCase("buffer_me_msg") || type.equalsIgnoreCase("server_motdstart") || type.equalsIgnoreCase("wait") || type.equalsIgnoreCase("banned") || type.equalsIgnoreCase("kill") || type.equalsIgnoreCase("connecting_cancelled")
					 || type.equalsIgnoreCase("notice") || type.equalsIgnoreCase("server_welcome") || type.equalsIgnoreCase("server_motd") || type.equalsIgnoreCase("server_endofmotd") || type.equalsIgnoreCase("services_down") || type.equalsIgnoreCase("your_unique_id")
					 || type.equalsIgnoreCase("server_luserclient") || type.equalsIgnoreCase("server_luserop") || type.equalsIgnoreCase("server_luserconns") || type.equalsIgnoreCase("myinfo") || type.equalsIgnoreCase("hidden_host_set") || type.equalsIgnoreCase("unhandled_line") || type.equalsIgnoreCase("unparsed_line")
					 || type.equalsIgnoreCase("server_luserme") || type.equalsIgnoreCase("server_n_local") || type.equalsIgnoreCase("server_luserchannels") || type.equalsIgnoreCase("connecting_failed") || type.equalsIgnoreCase("nickname_in_use") || type.equalsIgnoreCase("channel_invite")
					 || type.equalsIgnoreCase("server_n_global") || type.equalsIgnoreCase("motd_response") || type.equalsIgnoreCase("server_luserunknown") || type.equalsIgnoreCase("socket_closed") || type.equalsIgnoreCase("channel_mode_list_change")
					 || type.equalsIgnoreCase("server_yourhost") || type.equalsIgnoreCase("server_created") || type.equalsIgnoreCase("inviting_to_channel") || type.equalsIgnoreCase("error") || type.equalsIgnoreCase("too_fast") || type.equalsIgnoreCase("no_bots") || type.equalsIgnoreCase("wallops")) {
				EventsDataSource e = EventsDataSource.getInstance();
				EventsDataSource.Event event = e.addEvent(object);
				BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBuffer(object.getInt("bid"));
				
				if((event.highlight && e.isImportant(event, b.type) || b.type.equals("conversation"))) {
					if(event.eid > b.last_seen_eid) {
						Notifications.getInstance().deleteNotification(event.cid, event.bid, event.eid);
						Notifications.getInstance().addNotification(event.cid, event.bid, event.eid, (event.nick != null)?event.nick:event.from, ColorFormatter.html_to_spanned(event.msg).toString(), b.name, b.type, event.type);
					}
					if(!backlog) {
						if(b.type.equals("conversation"))
							Notifications.getInstance().showNotifications(b.name + ": " + ColorFormatter.html_to_spanned(event.msg).toString());
						else
							Notifications.getInstance().showNotifications(b.name + ": <" + event.from + "> " + ColorFormatter.html_to_spanned(event.msg).toString());
					}
				}
				
				if(!backlog)
					notifyHandlers(EVENT_BUFFERMSG, event);
			} else if(type.equalsIgnoreCase("channel_init")) {
				ChannelsDataSource c = ChannelsDataSource.getInstance();
				c.deleteChannel(object.getLong("bid"));
				ChannelsDataSource.Channel channel = c.createChannel(object.getInt("cid"), object.getLong("bid"), object.getString("chan"),
						object.getJsonObject("topic").get("text").isJsonNull()?"":object.getJsonObject("topic").get("text").getAsString(),
								object.getJsonObject("topic").get("time").getAsLong(), 
						object.getJsonObject("topic").get("nick").getAsString(), object.getString("channel_type"),
						object.getString("mode"), object.getLong("timestamp"));
				UsersDataSource u = UsersDataSource.getInstance();
				u.deleteUsersForChannel(object.getInt("cid"), object.getString("chan"));
				JsonArray users = object.getJsonArray("members");
				for(int i = 0; i < users.size(); i++) {
					JsonObject user = users.get(i).getAsJsonObject();
					u.createUser(object.getInt("cid"), object.getString("chan"), user.get("nick").getAsString(), user.get("usermask").getAsString(), user.get("mode").getAsString(), user.get("away").getAsBoolean()?1:0);
				}
				if(!backlog)
					notifyHandlers(EVENT_CHANNELINIT, channel);
			} else if(type.equalsIgnoreCase("channel_topic")) {
				ChannelsDataSource c = ChannelsDataSource.getInstance();
				c.updateTopic(object.getLong("bid"), object.getString("topic"), object.getLong("eid"), object.getString("author"));
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog)
					notifyHandlers(EVENT_CHANNELTOPIC, object);
			} else if(type.equalsIgnoreCase("channel_url")) {
				ChannelsDataSource c = ChannelsDataSource.getInstance();
				c.updateURL(object.getLong("bid"), object.getString("url"));
			} else if(type.equalsIgnoreCase("channel_mode") || type.equalsIgnoreCase("channel_mode_is")) {
				ChannelsDataSource c = ChannelsDataSource.getInstance();
				c.updateMode(object.getLong("bid"), object.getString("newmode"));
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog)
					notifyHandlers(EVENT_CHANNELMODE, object);
			} else if(type.equalsIgnoreCase("channel_timestamp")) {
				ChannelsDataSource c = ChannelsDataSource.getInstance();
				c.updateTimestamp(object.getLong("bid"), object.getLong("timestamp"));
				if(!backlog)
					notifyHandlers(EVENT_CHANNELTIMESTAMP, object);
			} else if(type.equalsIgnoreCase("joined_channel") || type.equalsIgnoreCase("you_joined_channel")) {
				UsersDataSource u = UsersDataSource.getInstance();
				u.deleteUser(object.getInt("cid"), object.getString("chan"), object.getString("nick"));
				u.createUser(object.getInt("cid"), object.getString("chan"), object.getString("nick"), object.getString("hostmask"), "", 0);
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog)
					notifyHandlers(EVENT_JOIN, object);
			} else if(type.equalsIgnoreCase("parted_channel") || type.equalsIgnoreCase("you_parted_channel")) {
				UsersDataSource u = UsersDataSource.getInstance();
				u.deleteUser(object.getInt("cid"), object.getString("chan"), object.getString("nick"));
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog && type.equalsIgnoreCase("you_parted_channel")) {
					ChannelsDataSource c = ChannelsDataSource.getInstance();
					c.deleteChannel(object.getInt("bid"));
				}
				if(!backlog)
					notifyHandlers(EVENT_PART, object);
			} else if(type.equalsIgnoreCase("quit")) {
				UsersDataSource u = UsersDataSource.getInstance();
				u.deleteUser(object.getInt("cid"), object.getString("chan"), object.getString("nick"));
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog)
					notifyHandlers(EVENT_QUIT, object);
			} else if(type.equalsIgnoreCase("quit_server")) {
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog)
					notifyHandlers(EVENT_QUIT, object);
			} else if(type.equalsIgnoreCase("kicked_channel") || type.equalsIgnoreCase("you_kicked_channel")) {
				UsersDataSource u = UsersDataSource.getInstance();
				u.deleteUser(object.getInt("cid"), object.getString("chan"), object.getString("nick"));
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog && type.equalsIgnoreCase("you_kicked_channel")) {
					ChannelsDataSource c = ChannelsDataSource.getInstance();
					c.deleteChannel(object.getInt("bid"));
				}
				if(!backlog)
					notifyHandlers(EVENT_KICK, object);
			} else if(type.equalsIgnoreCase("nickchange") || type.equalsIgnoreCase("you_nickchange")) {
				ChannelsDataSource c = ChannelsDataSource.getInstance();
				ChannelsDataSource.Channel chan = c.getChannelForBuffer(object.getLong("bid"));
				if(chan != null) {
					UsersDataSource u = UsersDataSource.getInstance();
					u.updateNick(object.getInt("cid"), chan.name, object.getString("oldnick"), object.getString("newnick"));
				}
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog)
					notifyHandlers(EVENT_NICKCHANGE, object);
			} else if(type.equalsIgnoreCase("user_channel_mode")) {
				ChannelsDataSource c = ChannelsDataSource.getInstance();
				ChannelsDataSource.Channel chan = c.getChannelForBuffer(object.getLong("bid"));
				if(chan != null) {
					UsersDataSource u = UsersDataSource.getInstance();
					u.updateMode(object.getInt("cid"), chan.name, object.getString("nick"), object.getString("newmode"));
				}
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog)
					notifyHandlers(EVENT_USERCHANNELMODE, object);
			} else if(type.equalsIgnoreCase("member_updates")) {
				JsonObject updates = object.getJsonObject("updates");
				Iterator<Entry<String, JsonElement>> i = updates.entrySet().iterator();
				while(i.hasNext()) {
					Entry<String, JsonElement> entry = i.next();
					JsonObject user = entry.getValue().getAsJsonObject();
					ChannelsDataSource c = ChannelsDataSource.getInstance();
					ChannelsDataSource.Channel chan = c.getChannelForBuffer(object.getLong("bid"));
					if(chan != null) {
						UsersDataSource u = UsersDataSource.getInstance();
						u.updateAway(object.getInt("cid"), chan.name, user.get("nick").getAsString(), user.get("away").getAsBoolean()?1:0);
						u.updateHostmask(object.getInt("cid"), chan.name, user.get("nick").getAsString(), user.get("usermask").getAsString());
					}
				}
				if(!backlog)
					notifyHandlers(EVENT_MEMBERUPDATES, null);
			} else if(type.equalsIgnoreCase("user_away") || type.equalsIgnoreCase("away")) {
				BuffersDataSource b = BuffersDataSource.getInstance();
				UsersDataSource u = UsersDataSource.getInstance();
				u.updateAwayMsg(object.getInt("cid"), object.getString("nick"), 1, object.getString("msg"));
				b.updateAway(object.getInt("bid"), object.getString("msg"));
				if(!backlog)
					notifyHandlers(EVENT_AWAY, object);
			} else if(type.equalsIgnoreCase("self_away")) {
				ServersDataSource s = ServersDataSource.getInstance();
				UsersDataSource u = UsersDataSource.getInstance();
				u.updateAwayMsg(object.getInt("cid"), object.getString("nick"), 1, object.getString("away_msg"));
				s.updateAway(object.getInt("cid"), object.getString("away_msg"));
				if(!backlog)
					notifyHandlers(EVENT_AWAY, object);
			} else if(type.equalsIgnoreCase("self_back")) {
				ServersDataSource s = ServersDataSource.getInstance();
				UsersDataSource u = UsersDataSource.getInstance();
				u.updateAwayMsg(object.getInt("cid"), object.getString("nick"), 0, "");
				s.updateAway(object.getInt("cid"), "");
				if(!backlog)
					notifyHandlers(EVENT_SELFBACK, object);
			} else if(type.equalsIgnoreCase("self_details")) {
				ServersDataSource s = ServersDataSource.getInstance();
				s.updateUsermask(object.getInt("cid"), object.getString("usermask"));
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog)
					notifyHandlers(EVENT_SELFDETAILS, object);
			} else if(type.equalsIgnoreCase("user_mode")) {
				ServersDataSource s = ServersDataSource.getInstance();
				s.updateMode(object.getInt("cid"), object.getString("newmode"));
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog)
					notifyHandlers(EVENT_USERMODE, object);
			} else if(type.equalsIgnoreCase("connection_lag")) {
				ServersDataSource s = ServersDataSource.getInstance();
				s.updateLag(object.getInt("cid"), object.getLong("lag"));
			} else if(type.equalsIgnoreCase("isupport_params")) {
				ServersDataSource s = ServersDataSource.getInstance();
				s.updateIsupport(object.getInt("cid"), object.getJsonObject("params"));
			} else if(type.equalsIgnoreCase("set_ignores") || type.equalsIgnoreCase("ignore_list")) {
				ServersDataSource s = ServersDataSource.getInstance();
				s.updateIgnores(object.getInt("cid"), object.getJsonArray("masks"));
				if(!backlog)
					notifyHandlers(EVENT_SETIGNORES, object);
			} else if(type.equalsIgnoreCase("heartbeat_echo")) {
				JsonObject seenEids = object.getJsonObject("seenEids");
				Iterator<Entry<String, JsonElement>> i = seenEids.entrySet().iterator();
				while(i.hasNext()) {
					Entry<String, JsonElement> entry = i.next();
					JsonObject eids = entry.getValue().getAsJsonObject();
					Iterator<Entry<String, JsonElement>> j = eids.entrySet().iterator();
					while(j.hasNext()) {
						Entry<String, JsonElement> eidentry = j.next();
						String bid = eidentry.getKey();
						long eid = eidentry.getValue().getAsLong();
						BuffersDataSource.getInstance().updateLastSeenEid(Integer.valueOf(bid), eid);
						Notifications.getInstance().deleteOldNotifications(Integer.valueOf(bid), eid);
					}
				}
				if(!backlog) {
					notifyHandlers(EVENT_HEARTBEATECHO, object);
					Notifications.getInstance().showNotifications(null);
				}
			} else if(type.equalsIgnoreCase("oob_include")) {
				try {
					if(Looper.myLooper() == null)
						Looper.prepare();
					String url = "https://" + IRCCLOUD_HOST + object.getString("url");
					new OOBIncludeTask().execute(new URL(url));
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				Log.w(TAG, "Unhandled type: " + object);
			}
		}
		if(idle_interval > 0)
			schedule_idle_timer();
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
		conn.setRequestProperty("User-Agent", useragent);
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
		if(shutdownTimer != null) {
			Log.d("IRCCloud", "Disconnect timer cancelled");
			shutdownTimer.cancel();
			shutdownTimer = null;
		}
	}

	public void removeHandler(Handler handler) {
		handlers.remove(handler);
		if(handlers.isEmpty()){
			if(shutdownTimer == null) {
				shutdownTimer = new Timer();
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
				long timeout = Long.valueOf(prefs.getString("timeout", "300000"));
				Log.d("IRCCloud", "Scheduling disconnect timer for " + timeout + "ms");
				shutdownTimer.schedule( new TimerTask(){
		             public void run() {
		            	 if(handlers.isEmpty()) {
			                 disconnect();
		            	 }
		                 shutdownTimer = null;
		              }
		           }, timeout);
			}
			if(idleTimer != null && state != STATE_CONNECTED) {
				idleTimer.cancel();
				idleTimer = null;
				state = STATE_DISCONNECTED;
			}
		}
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
		int last_selected_bid;
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
		int num_invites;
		JSONObject prefs;
		String highlights;
		
		public UserInfo(IRCCloudJSONObject object) throws JSONException {
			name = object.getString("name");
			email = object.getString("email");
			verified = object.getBoolean("verified");
			last_selected_bid = object.getInt("last_selected_bid");
			connections = object.getLong("num_connections");
			active_connections = object.getLong("num_active_connections");
			join_date = object.getLong("join_date");
			auto_away = object.getBoolean("autoaway");
			if(object.has("prefs") && !object.getString("prefs").equals("null"))
				prefs = new JSONObject(object.getString("prefs"));
			else
				prefs = null;
			
			limits_name = object.getString("limits_name");
			JsonObject limits = object.getJsonObject("limits");
			limit_networks = limits.get("networks").getAsLong();
			limit_passworded_servers = limits.get("passworded_servers").getAsBoolean();
			limit_zombiehours = limits.get("zombiehours").getAsLong();
			limit_download_logs = limits.get("download_logs").getAsBoolean();
			limit_maxhistorydays = limits.get("maxhistorydays").getAsLong();
			if(object.has("highlights")) {
				JsonArray h = object.getJsonArray("highlights");
				highlights = "";
				for(int i = 0; i < h.size(); i++) {
					if(highlights.length() > 0)
						highlights += ", ";
					highlights += h.get(i).getAsString();
				}
			}
		}
	}
	
	private class OOBIncludeTask extends AsyncTask<URL, Void, Boolean> {
		@SuppressLint("NewApi")
		@Override
		protected Boolean doInBackground(URL... url) {
			try {
				if(Build.VERSION.SDK_INT >= 14)
					TrafficStats.setThreadStatsTag(BACKLOG_TAG);
				Log.d(TAG, "Requesting: " + url[0]);
				
				HttpURLConnection conn = null;

		        if (url[0].getProtocol().toLowerCase().equals("https")) {
		            HttpsURLConnection https = (HttpsURLConnection) url[0].openConnection();
		            conn = https;
		        } else {
		        	conn = (HttpURLConnection) url[0].openConnection();
		        }
				conn.setRequestMethod("GET");
				conn.setRequestProperty("Connection", "close");
				conn.setRequestProperty("Cookie", "session="+session);
				conn.setRequestProperty("Accept", "application/json");
				conn.setRequestProperty("Content-type", "application/json");
				conn.setRequestProperty("Accept-Encoding", "gzip");
				conn.setRequestProperty("User-Agent", useragent);
				conn.connect();
				JsonReader reader = null;
				try {
					if(conn.getInputStream() != null) {
						if(conn.getContentEncoding().equalsIgnoreCase("gzip"))
							reader = new JsonReader(new InputStreamReader(new GZIPInputStream(conn.getInputStream())));
						else
							reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
					}
				} catch (IOException e) {
					if(conn.getErrorStream() != null) {
						if(conn.getContentEncoding().equalsIgnoreCase("gzip"))
							reader = new JsonReader(new InputStreamReader(new GZIPInputStream(conn.getErrorStream())));
						else
							reader = new JsonReader(new InputStreamReader(conn.getErrorStream()));
					}
				}

				if(reader != null && reader.peek() == JsonToken.BEGIN_ARRAY) {
					synchronized(parserLock) {
						Notifications.getInstance().beginBatch();
						long time = System.currentTimeMillis();
						Log.i("IRCCloud", "Beginning backlog...");
						notifyHandlers(EVENT_BACKLOG_START, null);
						JsonParser parser = new JsonParser();
						reader.beginArray();
						int count = 0;
						while(reader.hasNext()) {
							JsonElement e = parser.parse(reader);
							parse_object(new IRCCloudJSONObject(e.getAsJsonObject()), true);
							count++;
							if(Build.VERSION.SDK_INT >= 14)
								TrafficStats.incrementOperationCount(1);
						}
						reader.endArray();
						Log.i("IRCCloud", "Backlog complete: " + count + " events");
						Log.i("IRCCloud", "Backlog processing took: " + (System.currentTimeMillis() - time) + "ms");
						Notifications.getInstance().endBatch();
					}
				} else if(ServersDataSource.getInstance().count() < 1) {
					Log.e("IRCCloud", "Failed to fetch the initial backlog, reconnecting!");
					client.disconnect();
				}
				if(reader != null)
					reader.close();

				ArrayList<BuffersDataSource.Buffer> buffers = BuffersDataSource.getInstance().getBuffers();
				for(BuffersDataSource.Buffer b : buffers) {
					Notifications.getInstance().deleteOldNotifications(b.bid, b.last_seen_eid);
				}
				Notifications.getInstance().showNotifications(null);
				notifyHandlers(EVENT_BACKLOG_END, null);
				return true;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(Build.VERSION.SDK_INT >= 14)
				TrafficStats.clearThreadStatsTag();
			return false;
		}
	}
	
}
