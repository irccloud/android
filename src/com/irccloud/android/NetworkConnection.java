/*
 * Copyright (c) 2013 IRCCloud, Ltd.
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

package com.irccloud.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;

import java.net.Proxy;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
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
import com.testflightapp.lib.TestFlight;

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
	private volatile int last_reqid = 0;
	private Timer shutdownTimer = null;
	private Timer idleTimer = null;
	public long idle_interval = 1000;
    private int failCount = 0;
	private long reconnect_timestamp = 0;
	private String useragent = null;
	
	public static final int BACKLOG_BUFFER_MAX = 100;
	
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
	public static final int EVENT_INVALIDNICK = 30;
	public static final int EVENT_BANLIST = 31;
	public static final int EVENT_WHOLIST = 32;
	public static final int EVENT_WHOIS = 33;
	public static final int EVENT_LINKCHANNEL = 34;
	public static final int EVENT_LISTRESPONSEFETCHING = 35;
	public static final int EVENT_LISTRESPONSE = 36;
	public static final int EVENT_LISTRESPONSETOOMANY = 37;
	public static final int EVENT_CONNECTIONLAG = 38;
	public static final int EVENT_GLOBALMSG = 39;
	public static final int EVENT_ACCEPTLIST = 40;
    public static final int EVENT_NAMESLIST = 41;
	
	public static final int EVENT_BACKLOG_START = 100;
	public static final int EVENT_BACKLOG_END = 101;
    public static final int EVENT_BACKLOG_FAILED = 102;
	public static final int EVENT_FAILURE_MSG = 103;
	public static final int EVENT_SUCCESS = 104;
	public static final int EVENT_PROGRESS = 105;
	public static final int EVENT_ALERT = 106;

    public static final int EVENT_DEBUG = 999;

	private static final String IRCCLOUD_HOST = "www.irccloud.com";
	
	private Object parserLock = new Object();
	private WifiManager.WifiLock wifiLock = null;
	
	public long clockOffset = 0;

	private float numbuffers = 0;
	private float totalbuffers = 0;
    private int currentcount = 0;

	public boolean ready = false;
	public String globalMsg = null;

	private HashMap<Integer, OOBIncludeTask> oobTasks = new HashMap<Integer, OOBIncludeTask>();
	
	public static NetworkConnection getInstance() {
		if(instance == null) {
			instance = new NetworkConnection();
		}
		return instance;
	}

	BroadcastReceiver connectivityListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo ni = cm.getActiveNetworkInfo();
			if(ni != null && ni.isConnected() && state == STATE_DISCONNECTED && session != null && handlers.size() > 0) {
                TestFlight.log("Network is online");
				if(idleTimer != null)
					idleTimer.cancel();
				idleTimer = null;
				connect(session);
			} else if(ni == null || !ni.isConnected()) {
                TestFlight.log("Network is offline");
                cancel_idle_timer();
                reconnect_timestamp = 0;
				if(client != null) {
					state = STATE_DISCONNECTING;
					client.disconnect();
				}
                state = STATE_DISCONNECTED;
                notifyHandlers(EVENT_CONNECTIVITY, null);
			}
		}
	};
	
	@SuppressWarnings("deprecation")
	public NetworkConnection() {
		String version;
		String network_type = null;
		try {
			version = "/" + IRCCloudApplication.getInstance().getPackageManager().getPackageInfo("com.irccloud.android", 0).versionName;
		} catch (Exception e) {
			version = "";
		}

		try {
			ConnectivityManager cm = (ConnectivityManager)IRCCloudApplication.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo ni = cm.getActiveNetworkInfo();
			if(ni != null)
				network_type = ni.getTypeName();
		} catch (Exception e) {
		}
		
		useragent = "IRCCloud" + version + " (" + android.os.Build.MODEL + "; " + Locale.getDefault().getCountry().toLowerCase() + "; "
				+ "Android " + android.os.Build.VERSION.RELEASE;
		
		WindowManager wm = (WindowManager)IRCCloudApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
		useragent += "; " + wm.getDefaultDisplay().getWidth() + "x" + wm.getDefaultDisplay().getHeight();
		
		if(network_type != null)
			useragent += "; " + network_type;
		
		useragent += ")";
		
		WifiManager wfm = (WifiManager) IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		wifiLock = wfm.createWifiLock(TAG);
		
		TrustManager tms[] = new TrustManager[1];
		tms[0] = new X509TrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				throw new CertificateException("Not implemented");
			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
				try {
					TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
					trustManagerFactory.init((KeyStore)null);

					for (TrustManager trustManager: trustManagerFactory.getTrustManagers()) {  
					    if (trustManager instanceof X509TrustManager) {  
					        X509TrustManager x509TrustManager = (X509TrustManager)trustManager;  
					        x509TrustManager.checkServerTrusted(chain, authType);
					    }  
					}
				} catch (KeyStoreException e) {
					throw new CertificateException(e);
				} catch (NoSuchAlgorithmException e) {
					throw new CertificateException(e);
				}

				if(!chain[0].getSubjectDN().getName().startsWith("CN=*.irccloud.com,")) {
					throw new CertificateException("Incorrect CN in cert chain");
				}
			}

			@Override
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}
		};
		WebSocketClient.setTrustManagers(tms);
	}
	
	public int getState() {
		return state;
	}
	
	public void disconnect() {
        TestFlight.log("Disconnecting WebSocket");
		if(client!=null) {
			state = STATE_DISCONNECTING;
			client.disconnect();
		} else {
			state = STATE_DISCONNECTED;
		}
		if(idleTimer != null) {
            try {
    			idleTimer.cancel();
            } catch (NullPointerException e) {
                //The timer expired already
            }
			idleTimer = null;
		}
		if(wifiLock.isHeld())
			wifiLock.release();
		reconnect_timestamp = 0;
		for(Integer bid : oobTasks.keySet()) {
			try {
				oobTasks.get(bid).cancel(true);
			} catch (Exception e) {
			}
		}
		oobTasks.clear();
		session = null;
		try {
			IRCCloudApplication.getInstance().getApplicationContext().unregisterReceiver(connectivityListener);
		} catch (IllegalArgumentException e) {
			//The broadcast receiver hasn't been registered yet
		}
	}
	
	public JSONObject login(String email, String password) {
		try {
            String postdata = "email="+URLEncoder.encode(email, "UTF-8")+"&password="+URLEncoder.encode(password, "UTF-8");
            String response = doFetch(new URL("https://" + IRCCLOUD_HOST + "/chat/login"), postdata, null);
			JSONObject o = new JSONObject(response);
			return o;
        } catch (UnknownHostException e) {
            return null;
		} catch (IOException e) {
            return null;
        } catch (Exception e) {
            try {
                JSONObject o = new JSONObject();
                o.put("exception", e.toString());
                return o;
            } catch (JSONException e1) {
            }
		}
		return null;
	}
	
	public JSONObject registerGCM(String regId, String sk) throws IOException {
		String postdata = "device_id="+regId+"&session="+sk;
		try {
            String response = doFetch(new URL("https://" + IRCCLOUD_HOST + "/gcm-register"), postdata, sk);
			JSONObject o = new JSONObject(response);
			return o;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public JSONObject unregisterGCM(String regId, String sk) throws IOException {
		String postdata = "device_id="+regId+"&session="+sk;
		try {
            String response = doFetch(new URL("https://" + IRCCLOUD_HOST + "/gcm-unregister"), postdata, sk);
			JSONObject o = new JSONObject(response);
			return o;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

    public JSONArray networkPresets() throws IOException {
        try {
            String response = doFetch(new URL("https://" + IRCCLOUD_HOST + "/static/networks.json"), null, null);
            JSONObject o = new JSONObject(response);
            return o.getJSONArray("networks");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public synchronized void connect(String sk) {
		session = sk;
        String host = null;
        int port = -1;

        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            IRCCloudApplication.getInstance().getApplicationContext().registerReceiver(connectivityListener, intentFilter);
        } catch (Exception e) {
        }

        ConnectivityManager cm = (ConnectivityManager)IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();

        if(ni == null || !ni.isConnected()) {
            TestFlight.log("Network is not connected");
            cancel_idle_timer();
            state = STATE_DISCONNECTED;
            reconnect_timestamp = 0;
            notifyHandlers(EVENT_CONNECTIVITY, null);
            return;
        }

        if(state == STATE_CONNECTING || state == STATE_CONNECTED) {
            Log.w(TAG, "Ignoring duplicate connect request");
            return;
        }
        state = STATE_CONNECTING;

        if(oobTasks.size() > 0) {
            Log.d("IRCCloud", "Clearing OOB tasks before connecting");
        }
        for(Integer bid : oobTasks.keySet()) {
            try {
                oobTasks.get(bid).cancel(true);
            } catch (Exception e) {
            }
        }
        oobTasks.clear();

        if(Build.VERSION.SDK_INT <11) {
            host = android.net.Proxy.getHost(IRCCloudApplication.getInstance().getApplicationContext());
            port = android.net.Proxy.getPort(IRCCloudApplication.getInstance().getApplicationContext());
        } else {
            host = System.getProperty("http.proxyHost", null);
            try {
                port = Integer.parseInt(System.getProperty("http.proxyPort", "8080"));
            } catch (NumberFormatException e) {
                port = -1;
            }
        }

		if(!wifiLock.isHeld())
			wifiLock.acquire();
		
		List<BasicNameValuePair> extraHeaders = Arrays.asList(
		    new BasicNameValuePair("Cookie", "session="+session),
		    new BasicNameValuePair("User-Agent", useragent)
		);

		String url = "wss://" + IRCCLOUD_HOST;
		if(EventsDataSource.getInstance().highest_eid > 0)
			url += "?since_id=" + EventsDataSource.getInstance().highest_eid;

        if(host != null && host.length() > 0 && port > 0) {
            TestFlight.log("Connecting: " + url + " via proxy: " + host);
            Log.d(TAG, "Connecting: " + url + " via proxy: " + host);
        } else {
            TestFlight.log("Connecting: " + url);
            Log.d(TAG, "Connecting: " + url);
        }

		client = new WebSocketClient(URI.create(url), new WebSocketClient.Listener() {
		    @Override
		    public void onConnect() {
                TestFlight.log("WebSocket connected");
                Log.d(TAG, "WebSocket connected");
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
                        TestFlight.log("Unable to parse: " + message);
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
                TestFlight.log("WebSocket disconnected");
                Log.d(TAG, "WebSocket disconnected");
                ConnectivityManager cm = (ConnectivityManager)IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = cm.getActiveNetworkInfo();
                if(state == STATE_DISCONNECTING || ni == null || !ni.isConnected())
		        	cancel_idle_timer();
		        else {
                    failCount++;
                    if(failCount < 4)
                        idle_interval = failCount * 1000;
                    else if(failCount < 10)
                        idle_interval = 10000;
                    else
                        idle_interval = 30000;
		        	schedule_idle_timer();
                    TestFlight.log("Reconnecting in " + idle_interval/1000 + " seconds");
                    Log.d(TAG, "Reconnecting in " + idle_interval/1000 + " seconds");
                }

		        state = STATE_DISCONNECTED;
		        notifyHandlers(EVENT_CONNECTIVITY, null);
		        
		        if(reason != null && reason.equals("SSL")) {
                    TestFlight.log("The socket was disconnected due to an SSL error");
		        	try {
			        	JSONObject o = new JSONObject();
						o.put("message", "Unable to establish a secure connection to the IRCCloud servers.");
				        notifyHandlers(EVENT_FAILURE_MSG, new IRCCloudJSONObject(o));
					} catch (JSONException e) {
					}
		        }
		    }

		    @Override
		    public void onError(Exception error) {
                TestFlight.log("The WebSocket encountered an error: " + error.toString());
                Log.d(TAG, "The WebSocket encountered an error: " + error.toString());
		        if(state == STATE_DISCONNECTING)
		        	cancel_idle_timer();
		        else {
                    failCount++;
                    if(failCount < 4)
                        idle_interval = failCount * 1000;
                    else if(failCount < 10)
                        idle_interval = 10000;
                    else
                        idle_interval = 30000;
		        	schedule_idle_timer();
                    TestFlight.log("Reconnecting in " + idle_interval/1000 + " seconds");
                }
		        
		        state = STATE_DISCONNECTED;
		        notifyHandlers(EVENT_CONNECTIVITY, null);
		    }
		}, extraHeaders);

        Log.d("IRCCloud", "Creating websocket");
		reconnect_timestamp = 0;
        idle_interval = 0;
		notifyHandlers(EVENT_CONNECTIVITY, null);
		client.setSocketTag(WEBSOCKET_TAG);
        if(host != null && host.length() > 0 && port > 0)
            client.setProxy(host, port);
        else
            client.setProxy(null, -1);
		client.connect();
	}

    public void logout() {
        disconnect();
        ready = false;
        SharedPreferences.Editor editor = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).edit();
        try {
            String regId = GCMIntentService.getRegistrationId(IRCCloudApplication.getInstance().getApplicationContext());
            if(regId.length() > 0) {
                //Store the old session key so GCM can unregister later
                editor.putString(regId, IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).getString("session_key", ""));
                GCMIntentService.scheduleUnregisterTimer(1000, regId);
            }
        } catch (Exception e) {
            //GCM might not be available on the device
        }
        editor.remove("session_key");
        editor.remove("gcm_registered");
        editor.remove("mentionTip");
        editor.remove("userSwipeTip");
        editor.remove("bufferSwipeTip");
        editor.remove("longPressTip");
        editor.remove("email");
        editor.remove("name");
        editor.remove("highlights");
        editor.remove("autoaway");
        editor.commit();
        ServersDataSource.getInstance().clear();
        BuffersDataSource.getInstance().clear();
        ChannelsDataSource.getInstance().clear();
        UsersDataSource.getInstance().clear();
        EventsDataSource.getInstance().clear();
        Notifications.getInstance().clear();
    }

	private synchronized int send(String method, JSONObject params) {
		if(client == null || state != STATE_CONNECTED)
			return -1;
		try {
			params.put("_reqid", ++last_reqid);
			params.put("_method", method);
			//Log.d(TAG, "Reqid: " + last_reqid + " Method: " + method);
			client.send(params.toString());
			return last_reqid;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int heartbeat(long selected_buffer, int cid, long bid, long last_seen_eid) {
		try {
			JSONObject o = new JSONObject();
			o.put("selectedBuffer", selected_buffer);
			JSONObject eids = new JSONObject();
			eids.put(String.valueOf(bid), last_seen_eid);
			JSONObject cids = new JSONObject();
			cids.put(String.valueOf(cid), eids);
			o.put("seenEids", cids.toString());
			return send("heartbeat", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int disconnect(int cid, String message) {
		try {
			JSONObject o = new JSONObject();
			o.put("cid", cid);
			o.put("msg", message);
			return send("disconnect", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int reconnect(int cid) {
		try {
			JSONObject o = new JSONObject();
			o.put("cid", cid);
			return send("reconnect", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int say(int cid, String to, String message) {
		try {
			JSONObject o = new JSONObject();
			o.put("cid", cid);
			if(to != null)
				o.put("to", to);
			o.put("msg", message);
			return send("say", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int join(int cid, String channel, String key) {
		try {
			JSONObject o = new JSONObject();
			o.put("cid", cid);
			o.put("channel", channel);
			o.put("key", key);
			return send("join", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int part(int cid, String channel, String message) {
		try {
			JSONObject o = new JSONObject();
			o.put("cid", cid);
			o.put("channel", channel);
			o.put("msg", message);
			return send("part", o);
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
			o.put("cid", cid);
			o.put("id", bid);
			return send("archive-buffer", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int unarchiveBuffer(int cid, long bid) {
		try {
			JSONObject o = new JSONObject();
			o.put("cid", cid);
			o.put("id", bid);
			return send("unarchive-buffer", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int deleteBuffer(int cid, long bid) {
		try {
			JSONObject o = new JSONObject();
			o.put("cid", cid);
			o.put("id", bid);
			return send("delete-buffer", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int deleteServer(int cid) {
		try {
			JSONObject o = new JSONObject();
			o.put("cid", cid);
			return send("delete-connection", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int addServer(String hostname, int port, int ssl, String netname, String nickname, String realname, String server_pass, String nickserv_pass, String joincommands, String channels) {
		try {
			JSONObject o = new JSONObject();
			o.put("hostname", hostname);
			o.put("port", port);
			o.put("ssl", String.valueOf(ssl));
			o.put("netname", netname);
			o.put("nickname", nickname);
			o.put("realname", realname);
			o.put("server_pass", server_pass);
			o.put("nspass", nickserv_pass);
			o.put("joincommands", joincommands);
			o.put("channels", channels);
			return send("add-server", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int editServer(int cid, String hostname, int port, int ssl, String netname, String nickname, String realname, String server_pass, String nickserv_pass, String joincommands) {
		try {
			JSONObject o = new JSONObject();
			o.put("hostname", hostname);
			o.put("port", port);
			o.put("ssl", String.valueOf(ssl));
			o.put("netname", netname);
			o.put("nickname", nickname);
			o.put("realname", realname);
			o.put("server_pass", server_pass);
			o.put("nspass", nickserv_pass);
			o.put("joincommands", joincommands);
			o.put("cid", cid);
			return send("edit-server", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int ignore(int cid, String mask) {
		try {
			JSONObject o = new JSONObject();
			o.put("cid", cid);
			o.put("mask", mask);
			return send("ignore", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int unignore(int cid, String mask) {
		try {
			JSONObject o = new JSONObject();
			o.put("cid", cid);
			o.put("mask", mask);
			return send("unignore", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int set_prefs(String prefs) {
		try {
			JSONObject o = new JSONObject();
			o.put("prefs", prefs);
			return send("set-prefs", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int set_user_settings(String email, String realname, String hwords, boolean autoaway) {
		try {
			JSONObject o = new JSONObject();
			o.put("email", email);
			o.put("realname", realname);
			o.put("hwords", hwords);
			o.put("autoaway", autoaway?"1":"0");
			return send("user-settings", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int ns_help_register(int cid) {
		try {
			JSONObject o = new JSONObject();
			o.put("cid", cid);
			return send("ns-help-register", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int set_nspass(int cid, String nspass) {
		try {
			JSONObject o = new JSONObject();
			o.put("cid", cid);
			o.put("nspass", nspass);
			return send("set-nspass", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int whois(int cid, String nick, String server) {
		try {
			JSONObject o = new JSONObject();
			o.put("cid", cid);
			o.put("nick", nick);
			if(server != null)
				o.put("server", server);
			return send("whois", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int topic(int cid, String channel, String topic) {
		try {
			JSONObject o = new JSONObject();
			o.put("cid", cid);
			o.put("channel", channel);
			o.put("topic", topic);
			return send("topic", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public int back(int cid) {
		try {
			JSONObject o = new JSONObject();
			o.put("cid", cid);
			return send("back", o);
		} catch (JSONException e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public void request_backlog(int cid, int bid, long beforeId) {
		try {
			if(oobTasks.containsKey(bid)) {
                TestFlight.log("Ignoring duplicate backlog request for BID: " + bid);
                Log.w("IRCCloud", "Ignoring duplicate backlog request for BID: " + bid);
				return;
			}
			if(Looper.myLooper() == null)
				Looper.prepare();
			
			OOBIncludeTask task = new OOBIncludeTask(bid);
			oobTasks.put(bid, task);
			
			if(beforeId > 0)
				task.execute(new URL("https://" + IRCCLOUD_HOST + "/chat/backlog?cid="+cid+"&bid="+bid+"&beforeid="+beforeId));
			else
				task.execute(new URL("https://" + IRCCLOUD_HOST + "/chat/backlog?cid="+cid+"&bid="+bid));
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
            try {
    			idleTimer.cancel();
            } catch (Exception e) {

            }
			idleTimer = null;
		}
		if(idle_interval <= 0)
			return;
		
		try {
			idleTimer = new Timer();
			idleTimer.schedule(new TimerTask() {
                public void run() {
                    if (handlers.size() > 0) {
                        TestFlight.log("Websocket idle time exceeded, reconnecting...");
                        Log.i(TAG, "Websocket idle time exceeded, reconnecting...");
                        state = STATE_DISCONNECTING;
                        notifyHandlers(EVENT_CONNECTIVITY, null);
                        client.disconnect();
                        connect(session);
                    }
                    idleTimer = null;
                    reconnect_timestamp = 0;
                }
            }, idle_interval);
			reconnect_timestamp = System.currentTimeMillis() + idle_interval;
		} catch (IllegalStateException e) {
			//It's possible for timer to get canceled by another thread before before it gets scheduled
			//so catch the exception
		}
	}
	
	public long getReconnectTimestamp() {
		return reconnect_timestamp;
	}
	
	private void parse_object(IRCCloudJSONObject object, boolean backlog) throws JSONException {
		cancel_idle_timer();
		//Log.d(TAG, "New event: " + object);
		if(!object.has("type")) {
			//Log.d(TAG, "Response: " + object);
			if(object.has("success") && !object.getBoolean("success") && object.has("message")) {
                TestFlight.log("Error: " + object);
				notifyHandlers(EVENT_FAILURE_MSG, object);
			} else if(object.has("success")) {
				notifyHandlers(EVENT_SUCCESS, object);
			}
			return;
		}
		String type = object.type();
		if(type != null && type.length() > 0) {
            //notifyHandlers(EVENT_DEBUG, "Type: " + type + " BID: " + object.bid() + " EID: " + object.eid());
			//Log.d(TAG, "New event: " + type);
			if(type.equalsIgnoreCase("header")) {
				idle_interval = object.getLong("idle_interval") + 10000;
				clockOffset = object.getLong("time") - (System.currentTimeMillis()/1000);
                failCount = 0;
				TestFlight.log("Clock offset: " + clockOffset + "s");
			} else if(type.equalsIgnoreCase("global_system_message")) {
				String msgType = object.getString("system_message_type");
				if(msgType == null || (!msgType.equalsIgnoreCase("eval") && !msgType.equalsIgnoreCase("refresh"))) {
					globalMsg = object.getString("msg");
					notifyHandlers(EVENT_GLOBALMSG, object);
				}
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
				if(!backlog)
					notifyHandlers(EVENT_BADCHANNELKEY, object);
			} else if(type.equalsIgnoreCase("too_many_channels") || type.equalsIgnoreCase("no_such_channel") ||
					type.equalsIgnoreCase("no_such_nick") || type.equalsIgnoreCase("invalid_nick_change") ||
					type.equalsIgnoreCase("chan_privs_needed") || type.equalsIgnoreCase("accept_exists") ||
					type.equalsIgnoreCase("banned_from_channel") || type.equalsIgnoreCase("oper_only") ||
					type.equalsIgnoreCase("no_nick_change") || type.equalsIgnoreCase("no_messages_from_non_registered") ||
					type.equalsIgnoreCase("not_registered") || type.equalsIgnoreCase("already_registered") ||
					type.equalsIgnoreCase("too_many_targets") || type.equalsIgnoreCase("no_such_server") ||
					type.equalsIgnoreCase("unknown_command") || type.equalsIgnoreCase("help_not_found") ||
					type.equalsIgnoreCase("accept_full") || type.equalsIgnoreCase("accept_not") ||
					type.equalsIgnoreCase("nick_collision") || type.equalsIgnoreCase("nick_too_fast") ||
					type.equalsIgnoreCase("save_nick") || type.equalsIgnoreCase("unknown_mode") ||
					type.equalsIgnoreCase("user_not_in_channel") || type.equalsIgnoreCase("need_more_params") ||
					type.equalsIgnoreCase("users_dont_match") || type.equalsIgnoreCase("users_disabled") ||
					type.equalsIgnoreCase("invalid_operator_password") || type.equalsIgnoreCase("flood_warning") ||
					type.equalsIgnoreCase("privs_needed") || type.equalsIgnoreCase("operator_fail") ||
					type.equalsIgnoreCase("not_on_channel") || type.equalsIgnoreCase("ban_on_chan") ||
					type.equalsIgnoreCase("cannot_send_to_chan") || type.equalsIgnoreCase("user_on_channel") ||
					type.equalsIgnoreCase("no_nick_given") || type.equalsIgnoreCase("no_text_to_send") ||
					type.equalsIgnoreCase("no_origin") || type.equalsIgnoreCase("only_servers_can_change_mode") ||
					type.equalsIgnoreCase("silence") || type.equalsIgnoreCase("no_channel_topic") ||
					type.equalsIgnoreCase("invite_only_chan") || type.equalsIgnoreCase("channel_full")) {
				if(!backlog)
					notifyHandlers(EVENT_ALERT, object);
			} else if(type.equalsIgnoreCase("open_buffer")) {
				if(!backlog)
					notifyHandlers(EVENT_OPENBUFFER, object);
			} else if(type.equalsIgnoreCase("invalid_nick")) {
				if(!backlog)
					notifyHandlers(EVENT_INVALIDNICK, object);
			} else if(type.equalsIgnoreCase("ban_list")) {
				if(!backlog)
					notifyHandlers(EVENT_BANLIST, object);
			} else if(type.equalsIgnoreCase("accept_list")) {
				if(!backlog)
					notifyHandlers(EVENT_ACCEPTLIST, object);
			} else if(type.equalsIgnoreCase("who_response")) {
				if(!backlog) {
                    UsersDataSource u = UsersDataSource.getInstance();
                    JsonArray users = object.getJsonArray("users");
                    for(int i = 0; i < users.size(); i++) {
                        JsonObject user = users.get(i).getAsJsonObject();
                        u.updateHostmask(object.cid(), object.bid(), user.get("nick").getAsString(), user.get("usermask").getAsString());
                        u.updateAway(object.cid(), object.bid(), user.get("nick").getAsString(), user.get("away").getAsBoolean()?1:0);
                    }
					notifyHandlers(EVENT_WHOLIST, object);
                }
            } else if(type.equalsIgnoreCase("names_reply")) {
                if(!backlog)
                    notifyHandlers(EVENT_NAMESLIST, object);
			} else if(type.equalsIgnoreCase("whois_response")) {
				if(!backlog)
					notifyHandlers(EVENT_WHOIS, object);
			} else if(type.equalsIgnoreCase("list_response_fetching")) {
				if(!backlog)
					notifyHandlers(EVENT_LISTRESPONSEFETCHING, object);
			} else if(type.equalsIgnoreCase("list_response_toomany")) {
				if(!backlog)
					notifyHandlers(EVENT_LISTRESPONSETOOMANY, object);
			} else if(type.equalsIgnoreCase("list_response")) {
				if(!backlog)
					notifyHandlers(EVENT_LISTRESPONSE, object);
			} else if(type.equalsIgnoreCase("makeserver") || type.equalsIgnoreCase("server_details_changed")) {
				ServersDataSource s = ServersDataSource.getInstance();
				ServersDataSource.Server server = s.createServer(object.cid(), object.getString("name"), object.getString("hostname"),
						object.getInt("port"), object.getString("nick"), object.getString("status"), object.getString("lag").equalsIgnoreCase("undefined")?0:object.getLong("lag"), object.getBoolean("ssl")?1:0,
								object.getString("realname"), object.getString("server_pass"), object.getString("nickserv_pass"), object.getString("join_commands"),
								object.getJsonObject("fail_info"), object.getString("away"), object.getJsonArray("ignores"));
				Notifications.getInstance().deleteNetwork(object.cid());
				if(object.getString("name") != null && object.getString("name").length() > 0)
					Notifications.getInstance().addNetwork(object.cid(), object.getString("name"));
				else
					Notifications.getInstance().addNetwork(object.cid(), object.getString("hostname"));

				if(!backlog)
					notifyHandlers(EVENT_MAKESERVER, server);
			} else if(type.equalsIgnoreCase("connection_deleted")) {
				ServersDataSource s = ServersDataSource.getInstance();
				s.deleteAllDataForServer(object.cid());
				Notifications.getInstance().deleteNetwork(object.cid());
				Notifications.getInstance().showNotifications(null);
				if(!backlog)
					notifyHandlers(EVENT_CONNECTIONDELETED, object.cid());
			} else if(type.equalsIgnoreCase("backlog_starts")) {
				numbuffers = object.getInt("numbuffers");
				totalbuffers = 0;
			} else if(type.equalsIgnoreCase("makebuffer")) {
                //Log.d("IRCCloud", "MakeBuffer (" + (int)(totalbuffers + 1) + "/" + (int)numbuffers + ")");
                //Debug.startMethodTracing("oob-" + object.bid(), 16*1024*1024);
				BuffersDataSource b = BuffersDataSource.getInstance();
				BuffersDataSource.Buffer buffer = b.createBuffer(object.bid(), object.cid(),
						(object.has("min_eid") && !object.getString("min_eid").equalsIgnoreCase("undefined"))?object.getLong("min_eid"):0,
								(object.has("last_seen_eid") && !object.getString("last_seen_eid").equalsIgnoreCase("undefined"))?object.getLong("last_seen_eid"):-1, object.getString("name"), object.getString("buffer_type"),
										(object.has("archived") && object.getBoolean("archived"))?1:0, (object.has("deferred") && object.getBoolean("deferred"))?1:0, (object.has("timeout") && object.getBoolean("timeout"))?1:0);
				Notifications.getInstance().deleteOldNotifications(buffer.bid, buffer.last_seen_eid);
				Notifications.getInstance().updateLastSeenEid(buffer.bid, buffer.last_seen_eid);
				if(!backlog)
					notifyHandlers(EVENT_MAKEBUFFER, buffer);
                totalbuffers++;
                currentcount = 0;
			} else if(type.equalsIgnoreCase("delete_buffer")) {
				BuffersDataSource b = BuffersDataSource.getInstance();
				b.deleteAllDataForBuffer(object.bid());
				Notifications.getInstance().deleteNotificationsForBid(object.bid());
				Notifications.getInstance().showNotifications(null);
				if(!backlog)
					notifyHandlers(EVENT_DELETEBUFFER, object.bid());
			} else if(type.equalsIgnoreCase("buffer_archived")) {
				BuffersDataSource b = BuffersDataSource.getInstance();
				b.updateArchived(object.bid(), 1);
				if(!backlog)
					notifyHandlers(EVENT_BUFFERARCHIVED, object.bid());
			} else if(type.equalsIgnoreCase("buffer_unarchived")) {
				BuffersDataSource b = BuffersDataSource.getInstance();
				b.updateArchived(object.bid(), 0);
				if(!backlog)
					notifyHandlers(EVENT_BUFFERUNARCHIVED, object.bid());
			} else if(type.equalsIgnoreCase("rename_conversation")) {
				BuffersDataSource b = BuffersDataSource.getInstance();
				b.updateName(object.bid(), object.getString("new_name"));
				if(!backlog)
					notifyHandlers(EVENT_RENAMECONVERSATION, object.bid());
			} else if(type.equalsIgnoreCase("status_changed")) {
				ServersDataSource s = ServersDataSource.getInstance();
				s.updateStatus(object.cid(), object.getString("new_status"), object.getJsonObject("fail_info"));
				if(!backlog)
					notifyHandlers(EVENT_STATUSCHANGED, object);
			} else if(type.equalsIgnoreCase("buffer_msg") || type.equalsIgnoreCase("buffer_me_msg") || type.equalsIgnoreCase("server_motdstart") || type.equalsIgnoreCase("wait") || type.equalsIgnoreCase("banned") || type.equalsIgnoreCase("kill") || type.equalsIgnoreCase("connecting_cancelled") || type.equalsIgnoreCase("target_callerid")
					 || type.equalsIgnoreCase("notice") || type.equalsIgnoreCase("server_welcome") || type.equalsIgnoreCase("server_motd") || type.equalsIgnoreCase("server_endofmotd") || type.equalsIgnoreCase("services_down") || type.equalsIgnoreCase("your_unique_id") || type.equalsIgnoreCase("callerid") || type.equalsIgnoreCase("target_notified")
					 || type.equalsIgnoreCase("server_luserclient") || type.equalsIgnoreCase("server_luserop") || type.equalsIgnoreCase("server_luserconns") || type.equalsIgnoreCase("myinfo") || type.equalsIgnoreCase("hidden_host_set") || type.equalsIgnoreCase("unhandled_line") || type.equalsIgnoreCase("unparsed_line")
					 || type.equalsIgnoreCase("server_luserme") || type.equalsIgnoreCase("server_n_local") || type.equalsIgnoreCase("server_luserchannels") || type.equalsIgnoreCase("connecting_failed") || type.equalsIgnoreCase("nickname_in_use") || type.equalsIgnoreCase("channel_invite") || type.startsWith("stats")
					 || type.equalsIgnoreCase("server_n_global") || type.equalsIgnoreCase("motd_response") || type.equalsIgnoreCase("server_luserunknown") || type.equalsIgnoreCase("socket_closed") || type.equalsIgnoreCase("channel_mode_list_change") || type.equalsIgnoreCase("msg_services") || type.equalsIgnoreCase("endofstats")
					 || type.equalsIgnoreCase("server_yourhost") || type.equalsIgnoreCase("server_created") || type.equalsIgnoreCase("inviting_to_channel") || type.equalsIgnoreCase("error") || type.equalsIgnoreCase("too_fast") || type.equalsIgnoreCase("no_bots") || type.equalsIgnoreCase("wallops")) {
				EventsDataSource e = EventsDataSource.getInstance();
				EventsDataSource.Event event = e.addEvent(object);
				BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBuffer(object.bid());
				
				if(b != null && event.eid > b.last_seen_eid && e.isImportant(event, b.type) && ((event.highlight || b.type.equals("conversation")))) {
					JSONObject bufferDisabledMap = null;
					boolean show = true;
					if(userInfo != null && userInfo.prefs != null && userInfo.prefs.has("buffer-disableTrackUnread")) {
						bufferDisabledMap = userInfo.prefs.getJSONObject("buffer-disableTrackUnread");
						if(bufferDisabledMap != null && bufferDisabledMap.has(String.valueOf(b.bid)) && bufferDisabledMap.getBoolean(String.valueOf(b.bid)))
							show = false;
					}
					if(show && Notifications.getInstance().getNotification(event.eid) == null) {
						String message = ColorFormatter.irc_to_html(event.msg);
						message = ColorFormatter.html_to_spanned(message).toString();
						Notifications.getInstance().addNotification(event.cid, event.bid, event.eid, (event.nick != null)?event.nick:event.from, message, b.name, b.type, event.type);
						if(!backlog) {
							if(b.type.equals("conversation"))
								Notifications.getInstance().showNotifications(b.name + ": " + message);
							else if(b.type.equals("console")) {
								if(event.from == null || event.from.length() == 0) {
									ServersDataSource.Server s = ServersDataSource.getInstance().getServer(event.cid);
									if(s.name != null && s.name.length() > 0)
										Notifications.getInstance().showNotifications(s.name + ": " + message);
									else
										Notifications.getInstance().showNotifications(s.hostname + ": " + message);
								} else {
									Notifications.getInstance().showNotifications(event.from + ": " + message);
								}
							} else
								Notifications.getInstance().showNotifications(b.name + ": <" + event.from + "> " + message);
						}
					}
				}
				
				if(!backlog)
					notifyHandlers(EVENT_BUFFERMSG, event);
			} else if(type.equalsIgnoreCase("link_channel")) {
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog)
					notifyHandlers(EVENT_LINKCHANNEL, object);
			} else if(type.equalsIgnoreCase("channel_init")) {
				ChannelsDataSource c = ChannelsDataSource.getInstance();
				ChannelsDataSource.Channel channel = c.createChannel(object.cid(), object.bid(), object.getString("chan"),
						object.getJsonObject("topic").get("text").isJsonNull()?"":object.getJsonObject("topic").get("text").getAsString(),
								object.getJsonObject("topic").get("time").getAsLong(), 
						object.getJsonObject("topic").get("nick").getAsString(), object.getString("channel_type"),
						object.getLong("timestamp"));
                c.updateMode(object.bid(), object.getString("mode"), object.getJsonObject("ops"), true);
				UsersDataSource u = UsersDataSource.getInstance();
				u.deleteUsersForBuffer(object.cid(), object.bid());
				JsonArray users = object.getJsonArray("members");
				for(int i = 0; i < users.size(); i++) {
					JsonObject user = users.get(i).getAsJsonObject();
					u.createUser(object.cid(), object.bid(), user.get("nick").getAsString(), user.get("usermask").getAsString(), user.get("mode").getAsString(), user.get("away").getAsBoolean()?1:0, false);
				}
				if(!backlog)
					notifyHandlers(EVENT_CHANNELINIT, channel);
			} else if(type.equalsIgnoreCase("channel_topic")) {
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog) {
					ChannelsDataSource c = ChannelsDataSource.getInstance();
					c.updateTopic(object.bid(), object.getString("topic"), object.getLong("eid"), object.getString("author"));
					notifyHandlers(EVENT_CHANNELTOPIC, object);
				}
			} else if(type.equalsIgnoreCase("channel_url")) {
				ChannelsDataSource c = ChannelsDataSource.getInstance();
				c.updateURL(object.bid(), object.getString("url"));
			} else if(type.equalsIgnoreCase("channel_mode") || type.equalsIgnoreCase("channel_mode_is")) {
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog) {
					ChannelsDataSource c = ChannelsDataSource.getInstance();
					c.updateMode(object.bid(), object.getString("newmode"), object.getJsonObject("ops"), false);
					notifyHandlers(EVENT_CHANNELMODE, object);
				}
			} else if(type.equalsIgnoreCase("channel_timestamp")) {
				if(!backlog) {
					ChannelsDataSource c = ChannelsDataSource.getInstance();
					c.updateTimestamp(object.bid(), object.getLong("timestamp"));
					notifyHandlers(EVENT_CHANNELTIMESTAMP, object);
				}
			} else if(type.equalsIgnoreCase("joined_channel") || type.equalsIgnoreCase("you_joined_channel")) {
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog) {
					UsersDataSource u = UsersDataSource.getInstance();
					u.createUser(object.cid(), object.bid(), object.getString("nick"), object.getString("hostmask"), "", 0);
					notifyHandlers(EVENT_JOIN, object);
				}
			} else if(type.equalsIgnoreCase("parted_channel") || type.equalsIgnoreCase("you_parted_channel")) {
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog) {
					UsersDataSource u = UsersDataSource.getInstance();
					u.deleteUser(object.cid(), object.bid(), object.getString("nick"));
					if(type.equalsIgnoreCase("you_parted_channel")) {
						ChannelsDataSource c = ChannelsDataSource.getInstance();
						c.deleteChannel(object.bid());
						u.deleteUsersForBuffer(object.cid(), object.bid());
					}
					notifyHandlers(EVENT_PART, object);
				}
			} else if(type.equalsIgnoreCase("quit")) {
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog) {
					UsersDataSource u = UsersDataSource.getInstance();
					u.deleteUser(object.cid(), object.bid(), object.getString("nick"));
					notifyHandlers(EVENT_QUIT, object);
				}
			} else if(type.equalsIgnoreCase("quit_server")) {
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog)
					notifyHandlers(EVENT_QUIT, object);
			} else if(type.equalsIgnoreCase("kicked_channel") || type.equalsIgnoreCase("you_kicked_channel")) {
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog) {
					UsersDataSource u = UsersDataSource.getInstance();
					u.deleteUser(object.cid(), object.bid(), object.getString("nick"));
					if(type.equalsIgnoreCase("you_kicked_channel")) {
						ChannelsDataSource c = ChannelsDataSource.getInstance();
						c.deleteChannel(object.bid());
						u.deleteUsersForBuffer(object.cid(), object.bid());
					}
					notifyHandlers(EVENT_KICK, object);
				}
			} else if(type.equalsIgnoreCase("nickchange") || type.equalsIgnoreCase("you_nickchange")) {
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog) {
					UsersDataSource u = UsersDataSource.getInstance();
					u.updateNick(object.cid(), object.bid(), object.getString("oldnick"), object.getString("newnick"));
					if(type.equalsIgnoreCase("you_nickchange")) {
						ServersDataSource.getInstance().updateNick(object.cid(), object.getString("newnick"));
					}
					notifyHandlers(EVENT_NICKCHANGE, object);
				}
			} else if(type.equalsIgnoreCase("user_channel_mode")) {
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog) {
					UsersDataSource u = UsersDataSource.getInstance();
					u.updateMode(object.cid(), object.bid(), object.getString("nick"), object.getString("newmode"));
					notifyHandlers(EVENT_USERCHANNELMODE, object);
				}
			} else if(type.equalsIgnoreCase("member_updates")) {
				JsonObject updates = object.getJsonObject("updates");
				Iterator<Entry<String, JsonElement>> i = updates.entrySet().iterator();
				while(i.hasNext()) {
					Entry<String, JsonElement> entry = i.next();
					JsonObject user = entry.getValue().getAsJsonObject();
					UsersDataSource u = UsersDataSource.getInstance();
					u.updateAway(object.cid(), object.bid(), user.get("nick").getAsString(), user.get("away").getAsBoolean()?1:0);
					u.updateHostmask(object.cid(), object.bid(), user.get("nick").getAsString(), user.get("usermask").getAsString());
				}
				if(!backlog)
					notifyHandlers(EVENT_MEMBERUPDATES, null);
			} else if(type.equalsIgnoreCase("user_away") || type.equalsIgnoreCase("away")) {
				BuffersDataSource b = BuffersDataSource.getInstance();
				UsersDataSource u = UsersDataSource.getInstance();
				u.updateAwayMsg(object.cid(), object.getString("nick"), 1, object.getString("msg"));
				b.updateAway(object.bid(), object.getString("msg"));
				if(!backlog)
					notifyHandlers(EVENT_AWAY, object);
			} else if(type.equalsIgnoreCase("self_away")) {
				ServersDataSource s = ServersDataSource.getInstance();
				UsersDataSource u = UsersDataSource.getInstance();
				u.updateAwayMsg(object.cid(), object.getString("nick"), 1, object.getString("away_msg"));
				s.updateAway(object.cid(), object.getString("away_msg"));
				if(!backlog)
					notifyHandlers(EVENT_AWAY, object);
			} else if(type.equalsIgnoreCase("self_back")) {
				ServersDataSource s = ServersDataSource.getInstance();
				UsersDataSource u = UsersDataSource.getInstance();
				u.updateAwayMsg(object.cid(), object.getString("nick"), 0, "");
				s.updateAway(object.cid(), "");
				if(!backlog)
					notifyHandlers(EVENT_SELFBACK, object);
			} else if(type.equalsIgnoreCase("self_details")) {
				ServersDataSource s = ServersDataSource.getInstance();
				s.updateUsermask(object.cid(), object.getString("usermask"));
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog)
					notifyHandlers(EVENT_SELFDETAILS, object);
			} else if(type.equalsIgnoreCase("user_mode")) {
				EventsDataSource e = EventsDataSource.getInstance();
				e.addEvent(object);
				if(!backlog) {
					ServersDataSource s = ServersDataSource.getInstance();
					s.updateMode(object.cid(), object.getString("newmode"));
					notifyHandlers(EVENT_USERMODE, object);
				}
			} else if(type.equalsIgnoreCase("connection_lag")) {
				ServersDataSource s = ServersDataSource.getInstance();
				s.updateLag(object.cid(), object.getLong("lag"));
				if(!backlog)
					notifyHandlers(EVENT_CONNECTIONLAG, object);
			} else if(type.equalsIgnoreCase("isupport_params")) {
				ServersDataSource s = ServersDataSource.getInstance();
				s.updateIsupport(object.cid(), object.getJsonObject("params"));
			} else if(type.equalsIgnoreCase("set_ignores") || type.equalsIgnoreCase("ignore_list")) {
				ServersDataSource s = ServersDataSource.getInstance();
				s.updateIgnores(object.cid(), object.getJsonArray("masks"));
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
						Notifications.getInstance().updateLastSeenEid(Integer.valueOf(bid), eid);
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
					new OOBIncludeTask(-1).execute(new URL(url));
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
                TestFlight.log("Unhandled type: " + object.type());
				//Log.w(TAG, "Unhandled type: " + object);
			}
		}
        if(numbuffers > 0 && currentcount < BACKLOG_BUFFER_MAX) {
            notifyHandlers(EVENT_PROGRESS, ((totalbuffers + ((float)currentcount / (float)BACKLOG_BUFFER_MAX))/ numbuffers) * 1000.0f);
        }
		if(!backlog && idle_interval > 0)
			schedule_idle_timer();
	}
	
	private String doFetch(URL url, String postdata, String sk) throws Exception {
		HttpURLConnection conn = null;

        Proxy proxy = null;
        String host = null;
        int port = -1;

        if(Build.VERSION.SDK_INT <11) {
            host = android.net.Proxy.getHost(IRCCloudApplication.getInstance().getApplicationContext());
            port = android.net.Proxy.getPort(IRCCloudApplication.getInstance().getApplicationContext());
        } else {
            host = System.getProperty("http.proxyHost", null);
            try {
                port = Integer.parseInt(System.getProperty("http.proxyPort", "8080"));
            } catch (NumberFormatException e) {
                port = -1;
            }
        }

        if(host != null && host.length() > 0 && port > 0) {
            InetSocketAddress proxyAddr = new InetSocketAddress(host, port);
            proxy = new Proxy(Proxy.Type.HTTP, proxyAddr);
        }

        if (url.getProtocol().toLowerCase().equals("https")) {
            HttpsURLConnection https = (HttpsURLConnection)((proxy != null)?url.openConnection(proxy):url.openConnection());
            conn = https;
        } else {
        	conn = (HttpURLConnection)((proxy != null)?url.openConnection(proxy):url.openConnection());
        }

		conn.setRequestProperty("Connection", "close");
		conn.setRequestProperty("User-Agent", useragent);
        if(sk != null)
            conn.setRequestProperty("Cookie", "session="+sk);
        if(postdata != null) {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            OutputStream ostr = null;
            try {
                ostr = conn.getOutputStream();
                ostr.write(postdata.getBytes());
            } finally {
                if (ostr != null)
                    ostr.close();
            }
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
		if(!handlers.contains(handler))
			handlers.add(handler);
		if(shutdownTimer != null) {
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

	public boolean isVisible() {
		if(handlers != null && handlers.size() > 0)
			return true;
		else
			return false;
	}
	
	public void notifyHandlers(int message, Object object) {
		notifyHandlers(message,object,null);
	}
	
	public synchronized void notifyHandlers(int message, Object object, Handler exclude) {
		if(handlers != null) {
			for(int i = 0; i < handlers.size(); i++) {
				Handler handler = handlers.get(i);
				if(handler != exclude) {
					Message msg = handler.obtainMessage(message, object);
					handler.sendMessage(msg);
				}
			}
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
		private int bid = -1;
		private URL mUrl;
		private long retryDelay = 1000;
		
		public OOBIncludeTask(int bid) {
			this.bid = bid;
		}
		
		@SuppressLint("NewApi")
		@Override
		protected Boolean doInBackground(URL... url) {
			try {
				long totalTime = System.currentTimeMillis();
				long totalParseTime = 0;
				long totalJSONTime = 0;
                long longestEventTime = 0;
                String longestEventType = "";
				if(Build.VERSION.SDK_INT >= 14)
					TrafficStats.setThreadStatsTag(BACKLOG_TAG);
				Log.d(TAG, "Requesting: " + url[0]);
                TestFlight.log("Requesting: " + url[0]);
				mUrl = url[0];
				HttpURLConnection conn = null;
                Proxy proxy = null;
                String host = null;
                int port = -1;

                if(Build.VERSION.SDK_INT <11) {
                    host = android.net.Proxy.getHost(IRCCloudApplication.getInstance().getApplicationContext());
                    port = android.net.Proxy.getPort(IRCCloudApplication.getInstance().getApplicationContext());
                } else {
                    host = System.getProperty("http.proxyHost", null);
                    port = Integer.parseInt(System.getProperty("http.proxyPort", "8080"));
                }

                if(host != null && host.length() > 0) {
                    TestFlight.log("Connecting via proxy: " + host);
                    InetSocketAddress proxyAddr = new InetSocketAddress(host, port);
                    proxy = new Proxy(Proxy.Type.HTTP, proxyAddr);
                }

                if (url[0].getProtocol().toLowerCase().equals("https")) {
		            HttpsURLConnection https = (proxy != null)?(HttpsURLConnection) url[0].openConnection(proxy):(HttpsURLConnection) url[0].openConnection();
		            conn = https;
		        } else {
		        	conn = (HttpURLConnection)((proxy != null)?url[0].openConnection(proxy):url[0].openConnection());
		        }
				conn.setRequestMethod("GET");
				conn.setRequestProperty("Connection", "close");
				conn.setRequestProperty("Cookie", "session="+session);
				conn.setRequestProperty("Accept", "application/json");
				conn.setRequestProperty("Content-type", "application/json");
				conn.setRequestProperty("Accept-Encoding", "gzip");
				conn.setRequestProperty("User-Agent", useragent);
				conn.connect();
                if(conn.getResponseCode() == 200) {
                    JsonReader reader = null;
                    try {
                        if(conn.getInputStream() != null) {
                            if(conn.getContentEncoding().equalsIgnoreCase("gzip"))
                                reader = new JsonReader(new InputStreamReader(new GZIPInputStream(conn.getInputStream())));
                            else if(conn.getInputStream() != null)
                                reader = new JsonReader(new InputStreamReader(conn.getInputStream()));
                        }
                    } catch (IOException e) {
                        if(conn.getErrorStream() != null) {
                            if(conn.getContentEncoding().equalsIgnoreCase("gzip"))
                                reader = new JsonReader(new InputStreamReader(new GZIPInputStream(conn.getErrorStream())));
                            else if(conn.getErrorStream() != null)
                                reader = new JsonReader(new InputStreamReader(conn.getErrorStream()));
                        }
                    }

                    if(reader != null && reader.peek() == JsonToken.BEGIN_ARRAY) {
                        synchronized(parserLock) {
                            cancel_idle_timer();
                            //if(ready)
                            //Debug.startMethodTracing("oob", 16*1024*1024);
                            TestFlight.log("Connection time: " + (System.currentTimeMillis() - totalTime) + "ms");
                            Log.d(TAG, "Connection time: " + (System.currentTimeMillis() - totalTime) + "ms");
                            TestFlight.log("Beginning backlog...");
                            Log.d(TAG, "Beginning backlog...");
                            notifyHandlers(EVENT_BACKLOG_START, null);
                            if(bid == -1) {
                                BuffersDataSource.getInstance().invalidate();
                                ChannelsDataSource.getInstance().invalidate();
                            }
                            numbuffers = 0;
                            totalbuffers = 0;
                            JsonParser parser = new JsonParser();
                            reader.beginArray();
                            int count = 0;
                            int currentBid = -1;
                            long firstEid = 0;
                            while(reader.hasNext()) {
                                if(isCancelled()) {
                                    Log.d("IRCCloud", "Backlog parsing cancelled");
                                    return false;
                                }
                                long time = System.currentTimeMillis();
                                JsonElement e = parser.parse(reader);
                                totalJSONTime += (System.currentTimeMillis() - time);
                                time = System.currentTimeMillis();
                                IRCCloudJSONObject o = new IRCCloudJSONObject(e.getAsJsonObject());
                                try {
                                    parse_object(o, true);
                                } catch (Exception ex) {
                                    TestFlight.log("Unable to parse message type: " + o.type() + ": " + ex.toString());
                                    Log.e(TAG, "Unable to parse message type: " + o.type());
                                    ex.printStackTrace();
                                }
                                if(totalbuffers > 1 && (!reader.hasNext() || (o.bid() > -1 && !o.type().equalsIgnoreCase("makebuffer") && !o.type().equalsIgnoreCase("channel_init")))) {
                                    if(o.bid() != currentBid) {
                                        //Debug.stopMethodTracing();
                                        if(currentBid != -1) {
                                            if(currentcount >= BACKLOG_BUFFER_MAX) {
                                                EventsDataSource.getInstance().pruneEvents(currentBid, firstEid);
                                            }
                                        }
                                        currentBid = o.bid();
                                        currentcount = 0;
                                        firstEid = o.eid();
                                    }
                                    currentcount++;
                                }
                                long t = (System.currentTimeMillis() - time);
                                if(t > longestEventTime) {
                                    longestEventTime = t;
                                    longestEventType = o.type();
                                }
                                totalParseTime += t;
                                count++;
                                if(Build.VERSION.SDK_INT >= 14)
                                    TrafficStats.incrementOperationCount(1);
                            }
                            reader.endArray();
                            //Debug.stopMethodTracing();
                            totalTime = (System.currentTimeMillis() - totalTime);
                            TestFlight.log("Backlog complete: " + count + " events");
                            TestFlight.log("JSON parsing took: " + totalJSONTime + "ms (" + (totalJSONTime/(float)count) + "ms / object)");
                            TestFlight.log("Backlog processing took: " + totalParseTime + "ms (" + (totalParseTime/(float)count) + "ms / object)");
                            TestFlight.log("Total OOB load time: " +  totalTime + "ms (" + (totalTime/(float)count) +"ms / object)");
                            TestFlight.log("Longest event: " + longestEventType + " (" + longestEventTime + "ms)");
                            Log.d(TAG, "Backlog complete: " + count + " events");
                            Log.d(TAG, "JSON parsing took: " + totalJSONTime + "ms (" + (totalJSONTime/(float)count) + "ms / object)");
                            Log.d(TAG, "Backlog processing took: " + totalParseTime + "ms (" + (totalParseTime/(float)count) + "ms / object)");
                            Log.d(TAG, "Total OOB load time: " +  totalTime + "ms (" + (totalTime/(float)count) +"ms / object)");
                            Log.d(TAG, "Longest event: " + longestEventType + " (" + longestEventTime + "ms)");
                            totalTime -= totalJSONTime;
                            totalTime -= totalParseTime;
                            TestFlight.log("Total non-processing time: " +  totalTime + "ms (" + (totalTime/(float)count) +"ms / object)");
                            Log.d(TAG, "Total non-processing time: " +  totalTime + "ms (" + (totalTime/(float)count) +"ms / object)");

                            if(bid == -1) {
                                BuffersDataSource.getInstance().purgeInvalidBIDs();
                                ChannelsDataSource.getInstance().purgeInvalidChannels();
                            }

                            ArrayList<BuffersDataSource.Buffer> buffers = BuffersDataSource.getInstance().getBuffers();
                            for(BuffersDataSource.Buffer b : buffers) {
                                Notifications.getInstance().deleteOldNotifications(b.bid, b.last_seen_eid);
                                if(b.timeout > 0 && bid == -1) {
                                    TestFlight.log("Requesting backlog for timed-out buffer: " + b.name);
                                    Log.d(TAG, "Requesting backlog for timed-out buffer: " + b.name);
                                    request_backlog(b.cid, b.bid, 0);
                                }
                            }
                            Notifications.getInstance().showNotifications(null);
                            schedule_idle_timer();
                        }
                        ready = true;
                    } else if(ServersDataSource.getInstance().count() < 1) {
                        TestFlight.log("Failed to fetch the initial backlog, reconnecting!");
                        Log.e(TAG, "Failed to fetch the initial backlog, reconnecting!");
                        client.disconnect();
                    }
                    if(reader != null)
                        reader.close();

                    if(bid != -1) {
                        BuffersDataSource.getInstance().updateTimeout(bid, 0);
                        oobTasks.remove(bid);
                    }

                    TestFlight.log("OOB fetch complete!");
                    Log.d(TAG, "OOB fetch complete!");
                    if(Build.VERSION.SDK_INT >= 14)
                        TrafficStats.clearThreadStatsTag();
                    numbuffers = 0;
                    notifyHandlers(EVENT_BACKLOG_END, null);
                    return true;
                } else {
                    Log.e(TAG, "Invalid response code: " + conn.getResponseCode());
                    throw new Exception("Invalid response code: " + conn.getResponseCode());
                }
			} catch (Exception e) {
				if(bid != -1) {
					if(!isCancelled()) {
                        BuffersDataSource.Buffer b = BuffersDataSource.getInstance().getBuffer(bid);
                        if(b != null && b.timeout == 1) {
                            TestFlight.log("Failed to fetch backlog for timed-out buffer, retrying in " + retryDelay + "ms");
                            Log.w(TAG, "Failed to fetch backlog for timed-out buffer, retrying in " + retryDelay + "ms");
                            new Timer().schedule(new TimerTask() {
                                 public void run() {
                                     doInBackground(mUrl);
                                 }
                            }, retryDelay);
                            retryDelay *= 2;
                        } else {
                            TestFlight.log("Failed to fetch backlog");
                            Log.w(TAG, "Failed to fetch backlog");
                            oobTasks.remove(bid);
                        }
					}
				} else if(ServersDataSource.getInstance().count() < 1) {
					e.printStackTrace();
                    TestFlight.log("Failed to fetch the initial backlog, reconnecting!");
                    Log.e(TAG, "Failed to fetch the initial backlog, reconnecting!");
					client.disconnect();
				} else {
                    Log.e(TAG, "An error occured while parsing backlog");
                    e.printStackTrace();
                }
			}
			if(Build.VERSION.SDK_INT >= 14)
				TrafficStats.clearThreadStatsTag();
            notifyHandlers(EVENT_BACKLOG_FAILED, null);
			return false;
		}
    }
	
}
