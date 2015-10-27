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

package com.irccloud.android;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.codebutler.android_websockets.WebSocketClient;
import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.android.gms.iid.InstanceID;
import com.irccloud.android.data.collection.NotificationsList;
import com.irccloud.android.data.model.Buffer;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.model.Channel;
import com.irccloud.android.data.collection.ChannelsList;
import com.irccloud.android.data.model.Event;
import com.irccloud.android.data.collection.EventsList;
import com.irccloud.android.data.model.Server;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.data.collection.UsersList;
import com.irccloud.android.data.model.User;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;

public class NetworkConnection {
    private static final String TAG = "IRCCloud";
    private static NetworkConnection instance = null;

    private static final ServersList mServers = ServersList.getInstance();
    private static final BuffersList mBuffers = BuffersList.getInstance();
    private static final ChannelsList mChannels = ChannelsList.getInstance();
    private static final UsersList mUsers = UsersList.getInstance();
    private static final EventsList mEvents = EventsList.getInstance();

    public static final int WEBSOCKET_TAG = 0x50C37;
    public static final int BACKLOG_TAG = 0xB4C106;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_DISCONNECTING = 3;
    private int state = STATE_DISCONNECTED;
    private long highest_eid = -1;

    public interface IRCEventHandler {
        void onIRCEvent(int message, Object object);
        void onIRCRequestSucceeded(int reqid, IRCCloudJSONObject object);
        void onIRCRequestFailed(int reqid, IRCCloudJSONObject object);
    }

    private WebSocketClient client = null;
    private UserInfo userInfo = null;
    private final ArrayList<IRCEventHandler> handlers = new ArrayList<IRCEventHandler>();
    public String session = null;
    private volatile int last_reqid = 0;
    private static final Timer idleTimer = new Timer("websocket-idle-timer");
    //private static final Timer saveTimer = new Timer("backlog-save-timer");
    private TimerTask idleTimerTask = null;
    private TimerTask saveTimerTask = null;
    private TimerTask notifierSockerTimerTask = null;
    private TimerTask disconnectSockerTimerTask = null;
    public long idle_interval = 1000;
    private volatile int failCount = 0;
    private long reconnect_timestamp = 0;
    public String useragent = null;
    private String streamId = null;
    private int accrued = 0;
    private boolean backlog = false;
    int currentBid = -1;
    long firstEid = -1;
    public JSONObject config = null;
    public boolean notifier;

    private ObjectMapper mapper = new ObjectMapper();

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
    public static final int EVENT_REORDERCONNECTIONS = 42;
    public static final int EVENT_CHANNELTOPICIS = 43;
    public static final int EVENT_SERVERMAPLIST = 44;
    public static final int EVENT_QUIETLIST = 45;
    public static final int EVENT_BANEXCEPTIONLIST = 46;
    public static final int EVENT_INVITELIST = 47;

    public static final int EVENT_BACKLOG_START = 100;
    public static final int EVENT_BACKLOG_END = 101;
    public static final int EVENT_BACKLOG_FAILED = 102;
    public static final int EVENT_FAILURE_MSG = 103;
    public static final int EVENT_SUCCESS = 104;
    public static final int EVENT_PROGRESS = 105;
    public static final int EVENT_ALERT = 106;
    public static final int EVENT_CACHE_START = 107;
    public static final int EVENT_CACHE_END = 108;

    public static final int EVENT_DEBUG = 999;

    public static String IRCCLOUD_HOST = BuildConfig.HOST;
    public static String IRCCLOUD_PATH = "/";

    private final Object parserLock = new Object();
    private WifiManager.WifiLock wifiLock = null;

    public long clockOffset = 0;

    private float numbuffers = 0;
    private float totalbuffers = 0;
    private int currentcount = 0;

    public boolean ready = false;
    public String globalMsg = null;

    private HashMap<Integer, OOBIncludeTask> oobTasks = new HashMap<Integer, OOBIncludeTask>();

    private PrivateKey SSLAuthKey;
    private String SSLAuthAlias;
    private X509Certificate[] SSLAuthCertificateChain;

    public void setSSLAuth(String alias, PrivateKey key, X509Certificate[] certificateChain) {
        SSLAuthAlias = alias;
        SSLAuthKey = key;
        SSLAuthCertificateChain = certificateChain;
    }

    TrustManager tms[];
    X509ExtendedKeyManager kms[];

    private SSLSocketFactory IRCCloudSocketFactory = new SSLSocketFactory() {
        final String CIPHERS[] = {
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
                "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA",
                "TLS_RSA_WITH_AES_128_CBC_SHA",
                "TLS_RSA_WITH_AES_256_CBC_SHA",
                "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
                "SSL_RSA_WITH_RC4_128_SHA",
                "SSL_RSA_WITH_RC4_128_MD5",
        };
        final String PROTOCOLS[] = {
                "TLSv1.2", "TLSv1.1", "TLSv1"
        };
        SSLSocketFactory internalSocketFactory;

        private void init() {
            try {
                SSLContext c = SSLContext.getInstance("TLS");
                c.init(kms, tms, null);
                internalSocketFactory = c.getSocketFactory();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return CIPHERS;
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return CIPHERS;
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            if (internalSocketFactory == null)
                init();
            SSLSocket socket = (SSLSocket) internalSocketFactory.createSocket(s, host, port, autoClose);
            try {
                socket.setEnabledProtocols(PROTOCOLS);
            } catch (IllegalArgumentException e) {
                //Not supported on older Android versions
            }

            try {
                socket.setEnabledCipherSuites(CIPHERS);
            } catch (IllegalArgumentException e) {
                //Not supported on older Android versions
            }
            return socket;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            if (internalSocketFactory == null)
                init();
            SSLSocket socket = (SSLSocket) internalSocketFactory.createSocket(host, port);
            try {
                socket.setEnabledProtocols(PROTOCOLS);
            } catch (IllegalArgumentException e) {
                //Not supported on older Android versions
            }

            try {
                socket.setEnabledCipherSuites(CIPHERS);
            } catch (IllegalArgumentException e) {
                //Not supported on older Android versions
            }
            return socket;
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            if (internalSocketFactory == null)
                init();
            SSLSocket socket = (SSLSocket) internalSocketFactory.createSocket(host, port, localHost, localPort);
            try {
                socket.setEnabledProtocols(PROTOCOLS);
            } catch (IllegalArgumentException e) {
                //Not supported on older Android versions
            }

            try {
                socket.setEnabledCipherSuites(CIPHERS);
            } catch (IllegalArgumentException e) {
                //Not supported on older Android versions
            }
            return socket;
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            if (internalSocketFactory == null)
                init();
            SSLSocket socket = (SSLSocket) internalSocketFactory.createSocket(host, port);
            try {
                socket.setEnabledProtocols(PROTOCOLS);
            } catch (IllegalArgumentException e) {
                //Not supported on older Android versions
            }

            try {
                socket.setEnabledCipherSuites(CIPHERS);
            } catch (IllegalArgumentException e) {
                //Not supported on older Android versions
            }
            return socket;
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            if (internalSocketFactory == null)
                init();
            SSLSocket socket = (SSLSocket) internalSocketFactory.createSocket(address, port, localAddress, localPort);
            try {
                socket.setEnabledProtocols(PROTOCOLS);
            } catch (IllegalArgumentException e) {
                //Not supported on older Android versions
            }

            try {
                socket.setEnabledCipherSuites(CIPHERS);
            } catch (IllegalArgumentException e) {
                //Not supported on older Android versions
            }
            return socket;
        }
    };

    public synchronized static NetworkConnection getInstance() {
        if (instance == null) {
            instance = new NetworkConnection();
        }
        return instance;
    }

    BroadcastReceiver connectivityListener = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();

            if (ni != null && ni.isConnected() && (state == STATE_DISCONNECTED || state == STATE_DISCONNECTING) && session != null && handlers.size() > 0) {
                if (idleTimerTask != null)
                    idleTimerTask.cancel();
                connect();
            } else if (ni == null || !ni.isConnected()) {
                cancel_idle_timer();
                reconnect_timestamp = 0;
                try {
                    state = STATE_DISCONNECTING;
                    client.disconnect();
                    state = STATE_DISCONNECTED;
                    notifyHandlers(EVENT_CONNECTIVITY, null);
                } catch (Exception e) {
                }
            }
        }
    };

    @SuppressWarnings("deprecation")
    public NetworkConnection() {
        session = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).getString("session_key", "");
        String version;
        String network_type = null;
        try {
            version = "/" + IRCCloudApplication.getInstance().getPackageManager().getPackageInfo(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            version = "";
        }

        try {
            ConnectivityManager cm = (ConnectivityManager) IRCCloudApplication.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null)
                network_type = ni.getTypeName();
        } catch (Exception e) {
        }

        try {
            config = new JSONObject(PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getString("config", "{}"));
        } catch (JSONException e) {
            e.printStackTrace();
            config = new JSONObject();
        }

        useragent = "IRCCloud" + version + " (" + android.os.Build.MODEL + "; " + Locale.getDefault().getCountry().toLowerCase() + "; "
                + "Android " + android.os.Build.VERSION.RELEASE;

        WindowManager wm = (WindowManager) IRCCloudApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
        useragent += "; " + wm.getDefaultDisplay().getWidth() + "x" + wm.getDefaultDisplay().getHeight();

        if (network_type != null)
            useragent += "; " + network_type;

        useragent += ")";

        WifiManager wfm = (WifiManager) IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wfm.createWifiLock(TAG);

        kms = new X509ExtendedKeyManager[1];
        kms[0] = new X509ExtendedKeyManager() {
            @Override
            public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
                return SSLAuthAlias;
            }

            @Override
            public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
                throw new UnsupportedOperationException();
            }

            @Override
            public X509Certificate[] getCertificateChain(String alias) {
                return SSLAuthCertificateChain;
            }

            @Override
            public String[] getClientAliases(String keyType, Principal[] issuers) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String[] getServerAliases(String keyType, Principal[] issuers) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PrivateKey getPrivateKey(String alias) {
                return SSLAuthKey;
            }
        };

        tms = new TrustManager[1];
        tms[0] = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                throw new CertificateException("Not implemented");
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                try {
                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
                    trustManagerFactory.init((KeyStore) null);

                    for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                        if (trustManager instanceof X509TrustManager) {
                            X509TrustManager x509TrustManager = (X509TrustManager) trustManager;
                            x509TrustManager.checkServerTrusted(chain, authType);
                        }
                    }
                } catch (KeyStoreException e) {
                    throw new CertificateException(e);
                } catch (NoSuchAlgorithmException e) {
                    throw new CertificateException(e);
                }

                if (BuildConfig.SSL_FPS != null && BuildConfig.SSL_FPS.length > 0) {
                    try {
                        MessageDigest md = MessageDigest.getInstance("SHA-1");
                        byte[] sha1 = md.digest(chain[0].getEncoded());
                        // http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
                        final char[] hexArray = "0123456789ABCDEF".toCharArray();
                        char[] hexChars = new char[sha1.length * 2];
                        for (int j = 0; j < sha1.length; j++) {
                            int v = sha1[j] & 0xFF;
                            hexChars[j * 2] = hexArray[v >>> 4];
                            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
                        }
                        String hexCharsStr = new String(hexChars);
                        boolean matched = false;
                        for(String fp : BuildConfig.SSL_FPS) {
                            if(fp.equals(hexCharsStr)) {
                                matched = true;
                                break;
                            }
                        }
                        if (!matched)
                            throw new CertificateException("Incorrect CN in cert chain");
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
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
        if (client != null) {
            state = STATE_DISCONNECTING;
            client.disconnect();
        } else {
            state = STATE_DISCONNECTED;
        }
        if (idleTimerTask != null)
            idleTimerTask.cancel();
        if (wifiLock.isHeld())
            wifiLock.release();
        reconnect_timestamp = 0;
        synchronized (oobTasks) {
            for (Integer bid : oobTasks.keySet()) {
                try {
                    oobTasks.get(bid).cancel(true);
                } catch (Exception e) {
                }
            }
            oobTasks.clear();
        }
        for (Buffer b : mBuffers.getBuffers()) {
            if (!b.getScrolledUp())
                mEvents.pruneEvents(b.getBid());
        }
    }

    public JSONObject login(String email, String password) {
        try {
            String tokenResponse = fetch(new URL("https://" + IRCCLOUD_HOST + "/chat/auth-formtoken"), "", null, null, null);
            JSONObject token = new JSONObject(tokenResponse);
            if (token.has("token")) {
                String postdata = "email=" + URLEncoder.encode(email, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8") + "&token=" + token.getString("token");
                String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/chat/login"), postdata, null, token.getString("token"), null);
                if (response.length() < 1) {
                    JSONObject o = new JSONObject();
                    o.put("message", "empty_response");
                    return o;
                } else if (response.charAt(0) != '{') {
                    JSONObject o = new JSONObject();
                    o.put("message", "invalid_response");
                    return o;
                }
                return new JSONObject(response);
            } else {
                return null;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            JSONObject o = new JSONObject();
            try {
                o.put("message", "json_error");
            } catch (JSONException e1) {
            }
            return o;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject signup(String realname, String email, String password, String impression) {
        try {
            String tokenResponse = fetch(new URL("https://" + IRCCLOUD_HOST + "/chat/auth-formtoken"), "", null, null, null);
            JSONObject token = new JSONObject(tokenResponse);
            if (token.has("token")) {
                String postdata = "realname=" + URLEncoder.encode(realname, "UTF-8") + "&email=" + URLEncoder.encode(email, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8") + "&token=" + token.getString("token") + "&android_impression=" + URLEncoder.encode(impression, "UTF-8");
                String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/chat/signup"), postdata, null, token.getString("token"), null);
                if (response.length() < 1) {
                    JSONObject o = new JSONObject();
                    o.put("message", "empty_response");
                    return o;
                } else if (response.charAt(0) != '{') {
                    JSONObject o = new JSONObject();
                    o.put("message", "invalid_response");
                    return o;
                }
                return new JSONObject(response);
            } else {
                return null;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            JSONObject o = new JSONObject();
            try {
                o.put("message", "json_error");
            } catch (JSONException e1) {
            }
            return o;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject request_password(String email) {
        try {
            String tokenResponse = fetch(new URL("https://" + IRCCLOUD_HOST + "/chat/auth-formtoken"), "", null, null, null);
            JSONObject token = new JSONObject(tokenResponse);
            if (token.has("token")) {
                String postdata = "email=" + URLEncoder.encode(email, "UTF-8") + "&token=" + token.getString("token") + "&mobile=1";
                String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/chat/request-access-link"), postdata, null, token.getString("token"), null);
                if (response.length() < 1) {
                    JSONObject o = new JSONObject();
                    o.put("message", "empty_response");
                    return o;
                } else if (response.charAt(0) != '{') {
                    JSONObject o = new JSONObject();
                    o.put("message", "invalid_response");
                    return o;
                }
                return new JSONObject(response);
            } else {
                return null;
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            JSONObject o = new JSONObject();
            try {
                o.put("message", "json_error");
            } catch (JSONException e1) {
            }
            return o;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject impression(String adid, String referrer, String sk) {
        try {
            String postdata = "adid=" + URLEncoder.encode(adid, "UTF-8") + "&referrer=" + URLEncoder.encode(referrer, "UTF-8") + "&session=" + sk;
            String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/chat/android-impressions"), postdata, sk, null, null);
            if (response.length() < 1) {
                JSONObject o = new JSONObject();
                o.put("message", "empty_response");
                return o;
            } else if (response.charAt(0) != '{') {
                JSONObject o = new JSONObject();
                o.put("message", "invalid_response");
                return o;
            }
            return new JSONObject(response);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (JSONException e) {
            e.printStackTrace();
            JSONObject o = new JSONObject();
            try {
                o.put("message", "json_error");
            } catch (JSONException e1) {
            }
            return o;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject fetchJSON(String url) throws IOException {
        try {
            String response = fetch(new URL(url), null, null, null, null);
            return new JSONObject(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject fetchJSON(String url, String postdata) throws IOException {
        try {
            String response = fetch(new URL(url), postdata, null, null, null);
            return new JSONObject(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject fetchJSON(String url, HashMap<String, String>headers) throws IOException {
        try {
            String response = fetch(new URL(url), null, null, null, headers);
            return new JSONObject(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject fetchConfig() {
        try {
            JSONObject o = fetchJSON("https://" + IRCCLOUD_HOST + "/config");
            if(o != null) {
                config = o;
                SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).edit();
                prefs.putString("config", config.toString());
                prefs.commit();
                ColorFormatter.file_uri_template = config.getString("file_uri_template");
                ColorFormatter.pastebin_uri_template = config.getString("pastebin_uri_template");
                set_pastebin_cookie();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return config;
    }

    public void set_pastebin_cookie() {
        try {
            if (config != null) {
                CookieManager.getInstance().setAcceptCookie(true);
                CookieSyncManager sm = CookieSyncManager.createInstance(IRCCloudApplication.getInstance().getApplicationContext());
                CookieManager cm = CookieManager.getInstance();
                cm.removeSessionCookie();
                Uri u = Uri.parse(config.getString("pastebin_uri_template"));
                cm.setCookie(u.getScheme() + "://" + u.getHost() + "/", "session=" + session);
                if (Build.VERSION.SDK_INT >= 21)
                    cm.flush();
                sm.sync();
            }
        } catch (Exception e) {
        }
    }

    public JSONObject registerGCM(String regId, String sk) throws IOException {
        String postdata = "device_id=" + regId + "&session=" + sk;
        try {
            String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/gcm-register"), postdata, sk, null, null);
            return new JSONObject(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject unregisterGCM(String regId, String sk) throws IOException {
        String postdata = "device_id=" + regId + "&session=" + sk;
        try {
            String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/gcm-unregister"), postdata, sk, null, null);
            return new JSONObject(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject files(int page) throws IOException {
        try {
            String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/chat/files?page=" + page), null, session, null, null);
            return new JSONObject(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public JSONObject pastebins(int page) throws IOException {
        try {
            String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/chat/pastebins?page=" + page), null, session, null, null);
            return new JSONObject(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void logout(final String sk) {
        idleTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    Log.i("IRCCloud", "Invalidating session");
                    fetch(new URL("https://" + IRCCLOUD_HOST + "/chat/logout"), "session=" + sk, sk, null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 50);
    }

    public JSONArray networkPresets() throws IOException {
        try {
            String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/static/networks.json"), null, null, null, null);
            JSONObject o = new JSONObject(response);
            return o.getJSONArray("networks");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void registerForConnectivity() {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            IRCCloudApplication.getInstance().getApplicationContext().registerReceiver(connectivityListener, intentFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unregisterForConnectivity() {
        try {
            IRCCloudApplication.getInstance().getApplicationContext().unregisterReceiver(connectivityListener);
        } catch (IllegalArgumentException e) {
            //The broadcast receiver hasn't been registered yet
        }
    }

    public synchronized void load() {
        /*notifyHandlers(EVENT_CACHE_START, null);
        try {
            String versionName = IRCCloudApplication.getInstance().getPackageManager().getPackageInfo(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), 0).versionName;
            SharedPreferences prefs = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0);
            if(!versionName.equals(prefs.getString("cacheVersion", ""))) {
                Log.w("IRCCloud", "App version changed, clearing cache");
                clearOfflineCache();
            }
        } catch (PackageManager.NameNotFoundException e) {
        }

        if(PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("enable_cache", true)) {
            mServers.load();
            mBuffers.load();
            mChannels.load();
            if (mServers.count() > 0) {
                String u = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).getString("userinfo", null);
                if (u != null && u.length() > 0)
                    userInfo = new UserInfo(new IRCCloudJSONObject(u));
                highest_eid = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).getLong("highest_eid", -1);
                streamId = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).getString("streamId", null);
                ready = true;
            }
        }
        notifyHandlers(EVENT_CACHE_END, null);*/
    }

    public synchronized void save(int delay) {
        /*if (saveTimerTask != null)
            saveTimerTask.cancel();

        saveTimerTask = new TimerTask() {
            @Override
            public void run() {
                synchronized (saveTimer) {
                    saveTimerTask = null;
                    if (PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("enable_cache", true)) {
                        final long start = System.currentTimeMillis();
                        Log.i("IRCCloud", "Saving backlog");
                        final SharedPreferences.Editor editor = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).edit();
                        editor.remove("streamId");
                        editor.remove("highest_eid");
                        editor.commit();

                        mUsers.save();
                        mEvents.save();
                        mChannels.save();
                        mBuffers.save();
                        mServers.save();
                        TransactionManager.getInstance().getSaveQueue().setTransactionListener(new TransactionListener<List<Model>>() {
                            @Override
                            public void onResultReceived(List<Model> models) {
                                Log.i("IRCCloud", "Saved " + models.size() + " objects in " + (System.currentTimeMillis() - start) + "ms");
                                if (handlers.size() == 0) {
                                    editor.putString("streamId", streamId);
                                    editor.putLong("highest_eid", highest_eid);
                                    try {
                                        editor.putString("cacheVersion", IRCCloudApplication.getInstance().getPackageManager().getPackageInfo(IRCCloudApplication.getInstance().getApplicationContext().getPackageName(), 0).versionName);
                                    } catch (PackageManager.NameNotFoundException e) {
                                        editor.remove("cacheVersion");
                                    }
                                    editor.commit();
                                }
                                TransactionManager.getInstance().getSaveQueue().setTransactionListener(null);
                            }

                            @Override
                            public boolean onReady(BaseTransaction<List<Model>> baseTransaction) {
                                return true;
                            }

                            @Override
                            public boolean hasResult(BaseTransaction<List<Model>> baseTransaction, List<Model> models) {
                                return true;
                            }
                        });
                        TransactionManager.getInstance().getSaveQueue().purgeQueue();
                    }
                }
            }
        };
        saveTimer.schedule(saveTimerTask, delay);*/
    }

    public synchronized void connect() {
        Context ctx = IRCCloudApplication.getInstance().getApplicationContext();
        session = ctx.getSharedPreferences("prefs", 0).getString("session_key", "");
        String host = null;
        int port = -1;

        if (session == null || session.length() == 0)
            return;

        if (ctx != null) {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();

            if (ni == null || !ni.isConnected()) {
                cancel_idle_timer();
                state = STATE_DISCONNECTED;
                reconnect_timestamp = 0;
                notifyHandlers(EVENT_CONNECTIVITY, null);
                return;
            }
        }

        if (state == STATE_CONNECTING || state == STATE_CONNECTED) {
            Log.w(TAG, "Ignoring duplicate connect request");
            return;
        }
        state = STATE_CONNECTING;

        if (saveTimerTask != null)
            saveTimerTask.cancel();
        saveTimerTask = null;

        if (oobTasks.size() > 0) {
            Log.d("IRCCloud", "Clearing OOB tasks before connecting");
        }
        synchronized (oobTasks) {

            for (Integer bid : oobTasks.keySet()) {
                try {
                    oobTasks.get(bid).cancel(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            oobTasks.clear();
        }
        if (Build.VERSION.SDK_INT < 11) {
            if (ctx != null) {
                host = android.net.Proxy.getHost(ctx);
                port = android.net.Proxy.getPort(ctx);
            }
        } else {
            host = System.getProperty("http.proxyHost", null);
            try {
                port = Integer.parseInt(System.getProperty("http.proxyPort", "8080"));
            } catch (NumberFormatException e) {
                port = -1;
            }
        }

        if (!wifiLock.isHeld())
            wifiLock.acquire();

        List<BasicNameValuePair> extraHeaders = Arrays.asList(
                new BasicNameValuePair("Cookie", "session=" + session),
                new BasicNameValuePair("User-Agent", useragent)
        );

        String url = "wss://" + IRCCLOUD_HOST + IRCCLOUD_PATH;
        if (highest_eid > 0 && streamId != null && streamId.length() > 0) {
            url += "?since_id=" + highest_eid + "&stream_id=" + streamId;
            if(notifier)
                url += "&notifier=1";
        } else if(notifier) {
            url += "?notifier=1";
        }

        if (host != null && host.length() > 0 && !host.equalsIgnoreCase("localhost") && !host.equalsIgnoreCase("127.0.0.1") && port > 0) {
            Crashlytics.log(Log.DEBUG, TAG, "Connecting: " + url + " via proxy: " + host);
        } else {
            Crashlytics.log(Log.DEBUG, TAG, "Connecting: " + url);
        }

        Crashlytics.log(Log.DEBUG, TAG, "Attempt: " + failCount);

        client = new WebSocketClient(URI.create(url), new WebSocketClient.Listener() {
            @Override
            public void onConnect() {
                Crashlytics.log(Log.DEBUG, TAG, "WebSocket connected");
                state = STATE_CONNECTED;
                Crashlytics.log(Log.DEBUG, TAG, "Emptying cache");
                if (saveTimerTask != null)
                    saveTimerTask.cancel();
                saveTimerTask = null;
                final SharedPreferences.Editor editor = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).edit();
                editor.remove("streamId");
                editor.remove("highest_eid");
                editor.commit();
                //Delete.tables(Server.class, Buffer.class, Channel.class);
                notifyHandlers(EVENT_CONNECTIVITY, null);
                fetchConfig();
                if (disconnectSockerTimerTask != null)
                    disconnectSockerTimerTask.cancel();
                if(notifier) {
                    disconnectSockerTimerTask = new TimerTask() {
                        @Override
                        public void run() {
                            disconnectSockerTimerTask = null;
                            if(notifier) {
                                Log.d("IRCCloud", "Notifier socket expired");
                                disconnect();
                            }
                        }
                    };
                    idleTimer.schedule(disconnectSockerTimerTask, 600000);
                }
            }

            @Override
            public void onMessage(String message) {
                if (client != null && client.getListener() == this && message.length() > 0) {
                    try {
                        synchronized (parserLock) {
                            parse_object(new IRCCloudJSONObject(mapper.readValue(message, JsonNode.class)));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to parse: " + message);
                        Crashlytics.logException(e);
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
                Crashlytics.log(Log.DEBUG, TAG, "WebSocket disconnected");
                ConnectivityManager cm = (ConnectivityManager) IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = cm.getActiveNetworkInfo();
                if (state == STATE_DISCONNECTING || ni == null || !ni.isConnected())
                    cancel_idle_timer();
                else {
                    failCount++;
                    if (failCount < 4)
                        idle_interval = failCount * 1000;
                    else if (failCount < 10)
                        idle_interval = 10000;
                    else
                        idle_interval = 30000;
                    schedule_idle_timer();
                    Crashlytics.log(Log.DEBUG, TAG, "Reconnecting in " + idle_interval / 1000 + " seconds");
                }

                state = STATE_DISCONNECTED;
                notifyHandlers(EVENT_CONNECTIVITY, null);

                if (reason != null && reason.equals("SSL")) {
                    Crashlytics.log(Log.ERROR, TAG, "The socket was disconnected due to an SSL error");
                    try {
                        JSONObject o = new JSONObject();
                        o.put("message", "Unable to establish a secure connection to the IRCCloud servers.");
                        notifyHandlers(EVENT_FAILURE_MSG, new IRCCloudJSONObject(o));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                client = null;
            }

            @Override
            public void onError(Exception error) {
                Crashlytics.log(Log.ERROR, TAG, "The WebSocket encountered an error: " + error.toString());
                if (state == STATE_DISCONNECTING)
                    cancel_idle_timer();
                else {
                    failCount++;
                    if (failCount < 4)
                        idle_interval = failCount * 1000;
                    else if (failCount < 10)
                        idle_interval = 10000;
                    else
                        idle_interval = 30000;
                    schedule_idle_timer();
                    Crashlytics.log(Log.DEBUG, TAG, "Reconnecting in " + idle_interval / 1000 + " seconds");
                }

                state = STATE_DISCONNECTED;
                notifyHandlers(EVENT_CONNECTIVITY, null);
                client = null;
            }
        }, extraHeaders);

        Log.d("IRCCloud", "Creating websocket");
        reconnect_timestamp = 0;
        idle_interval = 0;
        accrued = 0;
        notifyHandlers(EVENT_CONNECTIVITY, null);
        if (client != null) {
            client.setSocketTag(WEBSOCKET_TAG);
            if (host != null && host.length() > 0 && !host.equalsIgnoreCase("localhost") && !host.equalsIgnoreCase("127.0.0.1") && port > 0)
                client.setProxy(host, port);
            else
                client.setProxy(null, -1);
            client.connect();
        }
    }

    public void logout() {
        streamId = null;
        disconnect();
        BackgroundTaskService.cancelBacklogSync(IRCCloudApplication.getInstance().getApplicationContext());
        if(BuildConfig.GCM_ID.length() > 0) {
            BackgroundTaskService.unregisterGCM(IRCCloudApplication.getInstance().getApplicationContext());
        }
        ready = false;
        accrued = 0;
        highest_eid = -1;
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).edit();
        prefs.clear();
        prefs.commit();
        SharedPreferences.Editor editor = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).edit();
        editor.clear();
        editor.commit();
        mServers.clear();
        mBuffers.clear();
        mChannels.clear();
        mUsers.clear();
        mEvents.clear();
        NotificationsList.getInstance().clear();
        userInfo = null;
        session = null;
        save(100);
    }

    public void clearOfflineCache() {
        SharedPreferences.Editor editor = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).edit();
        editor.remove("userinfo");
        editor.remove("highest_eid");
        editor.remove("streamId");
        editor.commit();
        highest_eid = -1;
        streamId = null;
        disconnect();
        mServers.clear();
        mBuffers.clear();
        mChannels.clear();
        mUsers.clear();
        mEvents.clear();
        connect();
    }

    private synchronized int send(String method, JSONObject params) {
        if (client == null || state != STATE_CONNECTED)
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

    public int heartbeat(int cid, int bid, long last_seen_eid) {
        return heartbeat(bid, new Integer[]{cid}, new Integer[]{bid}, new Long[]{last_seen_eid});
    }

    public int heartbeat(int selectedBuffer, Integer cids[], Integer bids[], Long last_seen_eids[]) {
        try {
            JSONObject heartbeat = new JSONObject();
            for (int i = 0; i < cids.length; i++) {
                JSONObject o;
                if (heartbeat.has(String.valueOf(cids[i]))) {
                    o = heartbeat.getJSONObject(String.valueOf(cids[i]));
                } else {
                    o = new JSONObject();
                    heartbeat.put(String.valueOf(cids[i]), o);
                }
                o.put(String.valueOf(bids[i]), last_seen_eids[i]);
            }

            JSONObject o = new JSONObject();
            o.put("selectedBuffer", selectedBuffer);
            o.put("seenEids", heartbeat.toString());
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
            int reqid = send("reconnect", o);
            if(reqid > 0) {
                Server s = mServers.getServer(cid);
                if(s != null) {
                    s.setStatus("queued");
                    notifyHandlers(EVENT_CONNECTIONLAG, new IRCCloudJSONObject(o));
                }
            }
            return reqid;
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int say(int cid, String to, String message) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            if (to != null)
                o.put("to", to);
            o.put("msg", message);
            return send("say", o);
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public JSONObject say(int cid, String to, String message, String sk) throws IOException {
        String postdata = "cid=" + cid + "&to=" + URLEncoder.encode(to, "UTF-8") + "&msg=" + URLEncoder.encode(message, "UTF-8") + "&session=" + sk;
        try {
            String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/chat/say"), postdata, sk, null, null);
            return new JSONObject(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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

    public int deleteFile(String id) {
        try {
            JSONObject o = new JSONObject();
            o.put("file", id);
            return send("delete-file", o);
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int restoreFile(String id) {
        try {
            JSONObject o = new JSONObject();
            o.put("file", id);
            return send("restore-file", o);
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
            if (netname != null)
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
            if (netname != null)
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
            Log.i("IRCCloud", "Setting prefs: " + prefs);
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
            o.put("autoaway", autoaway ? "1" : "0");
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
            if (server != null)
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

    public int reorder_connections(String cids) {
        try {
            JSONObject o = new JSONObject();
            o.put("cids", cids);
            return send("reorder-connections", o);
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int resend_verify_email() {
        JSONObject o = new JSONObject();
        return send("resend-verify-email", o);
    }

    public int finalize_upload(String id, String filename, String original_filename) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("filename", filename);
            o.put("original_filename", original_filename);
            return send("upload-finalise", o);
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int paste(String name, String extension, String contents) {
        try {
            JSONObject o = new JSONObject();
            if(name != null && name.length() > 0)
                o.put("name", name);
            o.put("contents", contents);
            o.put("extension", extension);
            return send("paste", o);
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int edit_paste(String id, String name, String extension, String contents) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            if(name != null && name.length() > 0)
                o.put("name", name);
            o.put("body", contents);
            o.put("extension", extension);
            return send("edit-pastebin", o);
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int delete_paste(String id) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            return send("delete-pastebin", o);
        } catch (JSONException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void request_backlog(int cid, int bid, long beforeId) {
        try {
            if (oobTasks.containsKey(bid)) {
                Crashlytics.log(Log.WARN, TAG, "Ignoring duplicate backlog request for BID: " + bid);
                return;
            }
            if (session == null || session.length() == 0) {
                Crashlytics.log(Log.WARN, TAG, "Not fetching backlog before session is set");
                return;
            }
            if (Looper.myLooper() == null)
                Looper.prepare();

            OOBIncludeTask task = new OOBIncludeTask(bid);
            synchronized (oobTasks) {
                oobTasks.put(bid, task);
            }
            if (beforeId > 0)
                task.execute(new URL("https://" + IRCCLOUD_HOST + "/chat/backlog?cid=" + cid + "&bid=" + bid + "&beforeid=" + beforeId));
            else
                task.execute(new URL("https://" + IRCCLOUD_HOST + "/chat/backlog?cid=" + cid + "&bid=" + bid));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void upgrade() {
        if (disconnectSockerTimerTask != null)
            disconnectSockerTimerTask.cancel();
        notifier = false;
        send("upgrade_notifier", new JSONObject());
    }

    public void cancel_idle_timer() {
        if (idleTimerTask != null)
            idleTimerTask.cancel();
        reconnect_timestamp = 0;
    }

    public void schedule_idle_timer() {
        if (idleTimerTask != null)
            idleTimerTask.cancel();
        if (idle_interval <= 0)
            return;

        try {
            idleTimerTask = new TimerTask() {
                public void run() {
                    if (handlers.size() > 0) {
                        Crashlytics.log(Log.INFO, TAG, "Websocket idle time exceeded, reconnecting...");
                        state = STATE_DISCONNECTING;
                        notifyHandlers(EVENT_CONNECTIVITY, null);
                        if (client != null)
                            client.disconnect();
                        connect();
                    }
                    reconnect_timestamp = 0;
                }
            };
            idleTimer.schedule(idleTimerTask, idle_interval);
            reconnect_timestamp = System.currentTimeMillis() + idle_interval;
        } catch (IllegalStateException e) {
            //It's possible for timer to get canceled by another thread before before it gets scheduled
            //so catch the exception
        }
    }

    public long getReconnectTimestamp() {
        return reconnect_timestamp;
    }

    public interface Parser {
        public void parse(IRCCloudJSONObject object) throws JSONException;
    }

    private class BroadcastParser implements Parser {
        int type;

        BroadcastParser(int t) {
            type = t;
        }

        public void parse(IRCCloudJSONObject object) {
            if (!backlog)
                notifyHandlers(type, object);
        }
    }

    HashMap<String, Parser> parserMap = new HashMap<String, Parser>() {{
        //Ignored events
        put("idle", null);
        put("end_of_backlog", null);
        put("user_account", null);

        put("header", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                idle_interval = object.getLong("idle_interval") + 15000;
                clockOffset = object.getLong("time") - (System.currentTimeMillis() / 1000);
                currentcount = 0;
                currentBid = -1;
                firstEid = -1;
                streamId = object.getString("streamid");
                if (object.has("accrued"))
                    accrued = object.getInt("accrued");
                if (!(object.has("resumed") && object.getBoolean("resumed"))) {
                    Log.d("IRCCloud", "Socket was not resumed");
                    NotificationsList.getInstance().clearLastSeenEIDs();
                    mEvents.clear();
                    mUsers.clear();
                }
            }
        });

        put("global_system_message", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                String msgType = object.getString("system_message_type");
                if (msgType == null || (!msgType.equalsIgnoreCase("eval") && !msgType.equalsIgnoreCase("refresh"))) {
                    globalMsg = object.getString("msg");
                    notifyHandlers(EVENT_GLOBALMSG, object);
                }
            }
        });

        put("num_invites", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                if (userInfo != null)
                    userInfo.num_invites = object.getInt("num_invites");
            }
        });

        put("stat_user", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                userInfo = new UserInfo(object);
                Crashlytics.setUserIdentifier("uid" + userInfo.id);
                SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).edit();
                prefs.putString("name", userInfo.name);
                prefs.putString("email", userInfo.email);
                prefs.putString("highlights", userInfo.highlights);
                prefs.putBoolean("autoaway", userInfo.auto_away);
                if (userInfo.prefs != null) {
                    prefs.putBoolean("time-24hr", userInfo.prefs.has("time-24hr") && userInfo.prefs.get("time-24hr") instanceof Boolean && userInfo.prefs.getBoolean("time-24hr"));
                    prefs.putBoolean("time-seconds", userInfo.prefs.has("time-seconds") && userInfo.prefs.get("time-seconds") instanceof Boolean && userInfo.prefs.getBoolean("time-seconds"));
                    prefs.putBoolean("mode-showsymbol", userInfo.prefs.has("mode-showsymbol") && userInfo.prefs.get("mode-showsymbol") instanceof Boolean && userInfo.prefs.getBoolean("mode-showsymbol"));
                    prefs.putBoolean("nick-colors", userInfo.prefs.has("nick-colors") && userInfo.prefs.get("nick-colors") instanceof Boolean && userInfo.prefs.getBoolean("nick-colors"));
                    prefs.putBoolean("emoji-disableconvert", !(userInfo.prefs.has("emoji-disableconvert") && userInfo.prefs.get("emoji-disableconvert") instanceof Boolean && userInfo.prefs.getBoolean("emoji-disableconvert")));
                    prefs.putBoolean("pastebin-disableprompt", !(userInfo.prefs.has("pastebin-disableprompt") && userInfo.prefs.get("pastebin-disableprompt") instanceof Boolean && userInfo.prefs.getBoolean("pastebin-disableprompt")));
                    prefs.putBoolean("hideJoinPart", !(userInfo.prefs.has("hideJoinPart") && userInfo.prefs.get("hideJoinPart") instanceof Boolean && userInfo.prefs.getBoolean("hideJoinPart")));
                    prefs.putBoolean("expandJoinPart", !(userInfo.prefs.has("expandJoinPart") && userInfo.prefs.get("expandJoinPart") instanceof Boolean && userInfo.prefs.getBoolean("expandJoinPart")));
                    if(userInfo.prefs.has("theme"))
                        prefs.putString("theme", userInfo.prefs.getString("theme"));
                    if(userInfo.prefs.has("font"))
                        prefs.putBoolean("monospace", userInfo.prefs.getString("font").equals("mono"));
                } else {
                    prefs.putBoolean("time-24hr", false);
                    prefs.putBoolean("time-seconds", false);
                    prefs.putBoolean("mode-showsymbol", false);
                    prefs.putBoolean("nick-colors", false);
                    prefs.putBoolean("emoji-disableconvert", true);
                    prefs.putBoolean("pastebin-disableprompt", true);
                }
                prefs.commit();
                mEvents.clearCaches();
                notifyHandlers(EVENT_USERINFO, userInfo);
            }
        });

        put("set_ignores", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                if(mServers.getServer(object.cid()) != null)
                    mServers.getServer(object.cid()).updateIgnores(object.getJsonNode("masks"));
                if (!backlog)
                    notifyHandlers(EVENT_SETIGNORES, object);
            }
        });
        put("ignore_list", get("set_ignores"));

        put("heartbeat_echo", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Iterator<Entry<String, JsonNode>> iterator = object.getJsonNode("seenEids").fields();
                while (iterator.hasNext()) {
                    Map.Entry<String, JsonNode> entry = iterator.next();
                    JsonNode eids = entry.getValue();
                    Iterator<Map.Entry<String, JsonNode>> j = eids.fields();
                    while (j.hasNext()) {
                        Map.Entry<String, JsonNode> eidentry = j.next();
                        int bid = Integer.valueOf(eidentry.getKey());
                        long eid = eidentry.getValue().asLong();
                        NotificationsList.getInstance().updateLastSeenEid(bid, eid);
                        Buffer b = mBuffers.getBuffer(bid);
                        if (b != null) {
                            b.setLast_seen_eid(eid);
                            if (mEvents.lastEidForBuffer(bid) <= eid) {
                                b.setUnread(0);
                                b.setHighlights(0);
                            }
                        }
                    }
                }
                NotificationsList.getInstance().deleteOldNotifications();
                if (!backlog) {
                    notifyHandlers(EVENT_HEARTBEATECHO, object);
                }
            }
        });

        //Backlog
        put("oob_include", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                try {
                    if (Looper.myLooper() == null)
                        Looper.prepare();
                    String url = "https://" + IRCCLOUD_HOST + object.getString("url");
                    OOBIncludeTask t = new OOBIncludeTask(-1);
                    synchronized (oobTasks) {
                        oobTasks.put(-1, t);
                    }
                    t.execute(new URL(url));
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        put("backlog_starts", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                numbuffers = object.getInt("numbuffers");
                totalbuffers = 0;
                currentBid = -1;
                notifyHandlers(EVENT_BACKLOG_START, null);
                backlog = true;
            }
        });

        put("oob_skipped", new Parser() {
                    @Override
                    public void parse(IRCCloudJSONObject object) throws JSONException {
                        Log.d("IRCCloud", "OOB was skipped");
                    }
                });

        put("backlog_complete", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                accrued = 0;
                backlog = false;
                Log.d("IRCCloud", "Cleaning up invalid BIDs");
                mBuffers.purgeInvalidBIDs();
                mChannels.purgeInvalidChannels();
                NotificationsList.getInstance().deleteOldNotifications();
                if (userInfo != null && userInfo.connections > 0 && (mServers.count() == 0 || mBuffers.count() == 0)) {
                    Log.e("IRCCloud", "Failed to load buffers list, reconnecting");
                    notifyHandlers(EVENT_BACKLOG_FAILED, null);
                    streamId = null;
                    if (client != null)
                        client.disconnect();
                } else {
                    failCount = 0;
                    ready = true;
                    notifyHandlers(EVENT_BACKLOG_END, null);
                    if(notifier)
                        save(1000);
                }
            }
        });

        //Misc. popup alerts
        put("bad_channel_key", new BroadcastParser(EVENT_BADCHANNELKEY));
        put("invalid_nick", new BroadcastParser(EVENT_INVALIDNICK));
        Parser alert = new BroadcastParser(EVENT_ALERT);
        String[] alerts = {"too_many_channels",
                "no_such_channel",
                "no_such_nick",
                "invalid_nick_change",
                "chan_privs_needed",
                "accept_exists",
                "banned_from_channel",
                "oper_only",
                "no_nick_change",
                "no_messages_from_non_registered",
                "not_registered",
                "already_registered",
                "too_many_targets",
                "no_such_server",
                "unknown_command",
                "help_not_found",
                "accept_full",
                "accept_not",
                "nick_collision",
                "nick_too_fast",
                "save_nick",
                "unknown_mode",
                "user_not_in_channel",
                "need_more_params",
                "users_dont_match",
                "users_disabled",
                "invalid_operator_password",
                "flood_warning",
                "privs_needed",
                "operator_fail",
                "not_on_channel",
                "ban_on_chan",
                "cannot_send_to_chan",
                "user_on_channel",
                "no_nick_given",
                "no_text_to_send",
                "no_origin",
                "only_servers_can_change_mode",
                "silence",
                "no_channel_topic",
                "invite_only_chan",
                "channel_full"};
        for (String event : alerts) {
            put(event, alert);
        }

        //Server events
        put("makeserver", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                String away = object.getString("away");
                if (getUserInfo() != null && getUserInfo().auto_away && away.equals("Auto-away"))
                    away = "";

                Server server = mServers.createServer(object.cid(), object.getString("name"), object.getString("hostname"),
                        object.getInt("port"), object.getString("nick"), object.getString("status"), object.getString("lag").equalsIgnoreCase("undefined") ? 0 : object.getLong("lag"), object.getBoolean("ssl") ? 1 : 0,
                        object.getString("realname"), object.getString("server_pass"), object.getString("nickserv_pass"), object.getString("join_commands"),
                        object.getJsonObject("fail_info"), away, object.getJsonNode("ignores"), (object.has("order") && !object.getString("order").equals("undefined")) ? object.getInt("order") : 0);

                if (!backlog) {
                    notifyHandlers(EVENT_MAKESERVER, server);
                }
            }
        });
        put("server_details_changed", get("makeserver"));
        put("connection_deleted", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                mServers.deleteAllDataForServer(object.cid());
                if (!backlog)
                    notifyHandlers(EVENT_CONNECTIONDELETED, object.cid());
            }
        });
        put("status_changed", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Server s = mServers.getServer(object.cid());
                if(s != null) {
                    s.setStatus(object.getString("new_status"));
                    s.setFail_info(object.getJsonObject("fail_info"));
                }
                if (!backlog) {
                    if (object.getString("new_status").equals("disconnected")) {
                        ArrayList<Buffer> buffers = mBuffers.getBuffersForServer(object.cid());
                        for (Buffer b : buffers) {
                            mChannels.deleteChannel(b.getBid());
                        }
                    }
                    notifyHandlers(EVENT_STATUSCHANGED, object);
                }
            }
        });
        put("isupport_params", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Server s = mServers.getServer(object.cid());
                if(s != null) {
                    s.updateUserModes(object.getString("usermodes"));
                    s.updateIsupport(object.getJsonObject("params"));
                }
            }
        });
        put("reorder_connections", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                JsonNode order = object.getJsonNode("order");
                for (int i = 0; i < order.size(); i++) {
                    mServers.getServer(order.get(i).asInt()).setOrder(i + 1);
                }
                notifyHandlers(EVENT_REORDERCONNECTIONS, object);
            }
        });

        //Buffer events
        put("open_buffer", new BroadcastParser(EVENT_OPENBUFFER));
        put("makebuffer", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Buffer buffer = mBuffers.createBuffer(object.bid(), object.cid(),
                        (object.has("min_eid") && !object.getString("min_eid").equalsIgnoreCase("undefined")) ? object.getLong("min_eid") : 0,
                        (object.has("last_seen_eid") && !object.getString("last_seen_eid").equalsIgnoreCase("undefined")) ? object.getLong("last_seen_eid") : -1, object.getString("name"), object.getString("buffer_type"),
                        (object.has("archived") && object.getBoolean("archived")) ? 1 : 0, (object.has("deferred") && object.getBoolean("deferred")) ? 1 : 0, (object.has("timeout") && object.getBoolean("timeout")) ? 1 : 0);
                NotificationsList.getInstance().updateLastSeenEid(buffer.getBid(), buffer.getLast_seen_eid());
                if (!backlog) {
                    notifyHandlers(EVENT_MAKEBUFFER, buffer);
                }
                totalbuffers++;
            }
        });
        put("delete_buffer", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                mBuffers.deleteAllDataForBuffer(object.bid());
                NotificationsList.getInstance().deleteNotificationsForBid(object.bid());
                if (!backlog)
                    notifyHandlers(EVENT_DELETEBUFFER, object.bid());
            }
        });
        put("buffer_archived", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Buffer b = mBuffers.getBuffer(object.bid());
                if(b != null)
                    b.setArchived(1);
                if (!backlog) {
                    notifyHandlers(EVENT_BUFFERARCHIVED, object.bid());
                }
            }
        });
        put("buffer_unarchived", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Buffer b = mBuffers.getBuffer(object.bid());
                if(b != null)
                    b.setArchived(0);
                if (!backlog) {
                    notifyHandlers(EVENT_BUFFERUNARCHIVED, object.bid());
                }
            }
        });
        put("rename_conversation", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Buffer b = mBuffers.getBuffer(object.bid());
                if(b != null)
                    b.setName(object.getString("new_name"));
                if (!backlog) {
                    notifyHandlers(EVENT_RENAMECONVERSATION, object.bid());
                }
            }
        });
        Parser msg = new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                boolean newEvent = (mEvents.getEvent(object.eid(), object.bid()) == null);
                Event event = mEvents.addEvent(object);
                Buffer b = mBuffers.getBuffer(object.bid());

                if (b != null && event.eid > b.getLast_seen_eid() && event.isImportant(b.getType())) {
                    if ((event.highlight || b.isConversation())) {
                        if (newEvent) {
                            b.setHighlights(b.getHighlights() + 1);
                            b.setUnread(1);
                        }
                        if(!backlog) {
                            JSONObject bufferDisabledMap = null;
                            boolean show = true;
                            if (userInfo != null && userInfo.prefs != null && userInfo.prefs.has("buffer-disableTrackUnread")) {
                                bufferDisabledMap = userInfo.prefs.getJSONObject("buffer-disableTrackUnread");
                                if (bufferDisabledMap != null && bufferDisabledMap.has(String.valueOf(b.getBid())) && bufferDisabledMap.getBoolean(String.valueOf(b.getBid())))
                                    show = false;
                            }
                            if (InstanceID.getInstance(IRCCloudApplication.getInstance().getApplicationContext()).getId().length() > 0)
                                show = false;
                            if (show && NotificationsList.getInstance().getNotification(event.eid) == null) {
                                String message = ColorFormatter.irc_to_html(event.msg);
                                message = ColorFormatter.html_to_spanned(message).toString();
                                String server_name = b.getServer().getName();
                                if(server_name == null || server_name.length() == 0)
                                    server_name = b.getServer().getHostname();
                                String from = event.nick;
                                if(from == null)
                                    from = (event.from != null) ? event.from : event.server;

                                NotificationsList.getInstance().addNotification(event.cid, event.bid, event.eid, (event.nick != null) ? event.nick : event.from, message, b.getName(), b.getType(), event.type, server_name);
                                switch (b.getType()) {
                                    case "conversation":
                                        if (event.type.equals("buffer_me_msg"))
                                            NotificationsList.getInstance().showNotifications("— " + b.getName() + " " + message);
                                        else
                                            NotificationsList.getInstance().showNotifications(b.getName() + ": " + message);
                                        break;
                                    case "console":
                                        if (event.from == null || event.from.length() == 0) {
                                            Server s = mServers.getServer(event.cid);
                                            if (s.getName() != null && s.getName().length() > 0)
                                                NotificationsList.getInstance().showNotifications(s.getName() + ": " + message);
                                            else
                                                NotificationsList.getInstance().showNotifications(s.getHostname() + ": " + message);
                                        } else {
                                            NotificationsList.getInstance().showNotifications(event.from + ": " + message);
                                        }
                                        break;
                                    default:
                                        if (event.type.equals("buffer_me_msg"))
                                            NotificationsList.getInstance().showNotifications(b.getName() + ": — " + event.nick + " " + message);
                                        else
                                            NotificationsList.getInstance().showNotifications(b.getName() + ": <" + event.from + "> " + message);
                                        break;
                                }
                            }
                        }
                    } else {
                        b.setUnread(1);
                    }
                } else if (b == null && !oobTasks.containsKey(-1)) {
                    Log.e("IRCCloud", "Got a message for a buffer that doesn't exist, reconnecting!");
                    notifyHandlers(EVENT_BACKLOG_FAILED, null);
                    streamId = null;
                    if (client != null)
                        client.disconnect();
                }

                if (handlers.size() == 0 && b != null && !b.getScrolledUp() && mEvents.getSizeOfBuffer(b.getBid()) > 200)
                    mEvents.pruneEvents(b.getBid());

                if (event.reqid >= 0) {
                    Event pending = mEvents.findPendingEventForReqid(event.bid, event.reqid);
                    if(pending != null) {
                        try {
                            if (pending.expiration_timer != null)
                                pending.expiration_timer.cancel();
                        } catch (Exception e1) {
                            //Timer already cancelled
                        }
                        if(pending.eid != event.eid)
                            mEvents.deleteEvent(pending.eid, pending.bid);
                    }
                } else if(event.self && b != null && b.isConversation()) {
                    mEvents.clearPendingEvents(event.bid);
                }

                if (!backlog) {
                    notifyHandlers(EVENT_BUFFERMSG, event);
                }
            }
        };

        String[] msgs = {
                "buffer_msg",
                "buffer_me_msg",
                "wait",
                "banned",
                "kill",
                "connecting_cancelled",
                "target_callerid",
                "notice",
                "server_motdstart",
                "server_welcome",
                "server_motd",
                "server_endofmotd",
                "server_nomotd",
                "server_luserclient",
                "server_luserop",
                "server_luserconns",
                "server_luserme",
                "server_n_local",
                "server_luserchannels",
                "server_n_global",
                "server_yourhost",
                "server_created",
                "server_luserunknown",
                "server_snomask",
                "services_down",
                "your_unique_id",
                "callerid",
                "target_notified",
                "myinfo",
                "hidden_host_set",
                "unhandled_line",
                "unparsed_line",
                "connecting_failed",
                "nickname_in_use",
                "channel_invite",
                "motd_response",
                "socket_closed",
                "channel_mode_list_change",
                "msg_services",
                "stats", "statslinkinfo", "statscommands", "statscline", "statsnline", "statsiline", "statskline", "statsqline", "statsyline", "statsbline", "statsgline", "statstline", "statseline", "statsvline", "statslline", "statsuptime", "statsoline", "statshline", "statssline", "statsuline", "statsdebug", "endofstats",
                "inviting_to_channel",
                "error",
                "too_fast",
                "no_bots",
                "wallops",
                "logged_in_as",
                "sasl_fail",
                "sasl_too_long",
                "sasl_aborted",
                "sasl_already",
                "you_are_operator",
                "btn_metadata_set",
                "sasl_success",
                "cap_ls",
                "cap_req",
                "cap_ack",
                "cap_raw",
                "help_topics_start", "help_topics", "help_topics_end", "helphdr", "helpop", "helptlr", "helphlp", "helpfwd", "helpign",
                "version",
                "newsflash",
                "invited",
                "codepage",
                "logged_out",
                "nick_locked",
                "info_response",
                "generic_server_info",
                "unknown_umode",
                "bad_ping",
                "rehashed_config",
                "knock",
                "bad_channel_mask",
                "kill_deny",
                "chan_own_priv_needed",
                "not_for_halfops",
                "chan_forbidden",
                "starircd_welcome",
                "zurna_motd",
                "ambiguous_error_message",
                "list_usage",
                "list_syntax",
                "who_syntax",
                "text",
                "admin_info",
                "watch_status",
                "sqline_nick"
        };
        for (String event : msgs) {
            put(event, msg);
        }

        //Channel events
        put("link_channel", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Event event = mEvents.addEvent(object);
                if (!backlog) {
                    notifyHandlers(EVENT_LINKCHANNEL, object);
                }
            }
        });
        put("channel_init", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Channel channel = mChannels.createChannel(object.cid(), object.bid(), object.getString("chan"),
                        object.getJsonObject("topic").get("text").isNull() ? "" : object.getJsonObject("topic").get("text").asText(),
                        object.getJsonObject("topic").get("time").asLong(),
                        object.getJsonObject("topic").has("nick") ? object.getJsonObject("topic").get("nick").asText() : object.getJsonObject("topic").get("server").asText(), object.getString("channel_type"),
                        object.getLong("timestamp"));
                mChannels.updateMode(object.bid(), object.getString("mode"), object.getJsonObject("ops"), true);
                mUsers.deleteUsersForBuffer(object.bid());
                JsonNode users = object.getJsonNode("members");
                for (int i = 0; i < users.size(); i++) {
                    JsonNode user = users.get(i);
                    User u = mUsers.createUser(object.cid(), object.bid(), user.get("nick").asText(), user.get("usermask").asText(), user.get("mode").asText(), user.get("away").asBoolean() ? 1 : 0, false);
                }
                mBuffers.dirty = true;
                if (!backlog)
                    notifyHandlers(EVENT_CHANNELINIT, channel);
            }
        });
        put("channel_topic", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Event event = mEvents.addEvent(object);
                if (!backlog) {
                    mChannels.updateTopic(object.bid(), object.getString("topic"), object.getLong("eid") / 1000000, object.has("author") ? object.getString("author") : object.getString("server"));
                    notifyHandlers(EVENT_CHANNELTOPIC, object);
                }
            }
        });
        put("channel_url", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                mChannels.updateURL(object.bid(), object.getString("url"));
            }
        });
        put("channel_mode", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Event event = mEvents.addEvent(object);
                if (!backlog) {
                    mChannels.updateMode(object.bid(), object.getString("newmode"), object.getJsonObject("ops"), false);
                    notifyHandlers(EVENT_CHANNELMODE, object);
                }
            }
        });
        put("channel_mode_is", get("channel_mode"));
        put("channel_timestamp", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                if (!backlog) {
                    mChannels.updateTimestamp(object.bid(), object.getLong("timestamp"));
                    notifyHandlers(EVENT_CHANNELTIMESTAMP, object);
                }
            }
        });
        put("joined_channel", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Event event = mEvents.addEvent(object);
                if (!backlog) {
                    User u = mUsers.createUser(object.cid(), object.bid(), object.getString("nick"), object.getString("hostmask"), "", 0);
                    notifyHandlers(EVENT_JOIN, object);
                }
            }
        });
        put("you_joined_channel", get("joined_channel"));
        put("parted_channel", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Event event = mEvents.addEvent(object);
                if (!backlog) {
                    mUsers.deleteUser(object.bid(), object.getString("nick"));
                    if (object.type().equals("you_parted_channel")) {
                        mChannels.deleteChannel(object.bid());
                        mUsers.deleteUsersForBuffer(object.bid());
                        mBuffers.dirty = true;
                    }
                    notifyHandlers(EVENT_PART, object);
                }
            }
        });
        put("you_parted_channel", get("parted_channel"));
        put("quit", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Event event = mEvents.addEvent(object);
                if (!backlog) {
                    mUsers.deleteUser(object.bid(), object.getString("nick"));
                    notifyHandlers(EVENT_QUIT, object);
                }
            }
        });
        put("quit_server", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Event event = mEvents.addEvent(object);
                if (!backlog) {
                    notifyHandlers(EVENT_QUIT, object);
                }
            }
        });
        put("kicked_channel", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Event event = mEvents.addEvent(object);
                if (!backlog) {
                    mUsers.deleteUser(object.bid(), object.getString("nick"));
                    if (object.type().equals("you_kicked_channel")) {
                        mChannels.deleteChannel(object.bid());
                        mUsers.deleteUsersForBuffer(object.bid());
                        mBuffers.dirty = true;
                    }
                    notifyHandlers(EVENT_KICK, object);
                }
            }
        });
        put("you_kicked_channel", get("kicked_channel"));

        //Member list events
        put("nickchange", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Event event = mEvents.addEvent(object);
                if (!backlog) {
                    mUsers.updateNick(object.bid(), object.getString("oldnick"), object.getString("newnick"));
                    if (object.type().equals("you_nickchange")) {
                        mServers.getServer(object.cid).setNick(object.getString("newnick"));
                    }
                    notifyHandlers(EVENT_NICKCHANGE, object);
                }
            }
        });
        put("you_nickchange", get("nickchange"));
        put("user_channel_mode", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Event event = mEvents.addEvent(object);
                if (!backlog) {
                    mUsers.updateMode(object.bid(), object.getString("nick"), object.getString("newmode"));
                    notifyHandlers(EVENT_USERCHANNELMODE, object);
                }
            }
        });
        put("member_updates", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                JsonNode updates = object.getJsonObject("updates");
                Iterator<Map.Entry<String, JsonNode>> i = updates.fields();
                while (i.hasNext()) {
                    Map.Entry<String, JsonNode> e = i.next();
                    JsonNode user = e.getValue();
                    mUsers.updateAway(object.bid(), user.get("nick").asText(), user.get("away").asBoolean() ? 1 : 0);
                    mUsers.updateHostmask(object.bid(), user.get("nick").asText(), user.get("usermask").asText());
                }
                if (!backlog)
                    notifyHandlers(EVENT_MEMBERUPDATES, null);
            }
        });
        put("user_away", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Buffer b = mBuffers.getBufferByName(object.cid(), object.getString("nick"));
                mUsers.updateAwayMsg(object.bid(), object.getString("nick"), 1, object.getString("msg"));
                if(b != null)
                    b.setAway_msg(object.getString("msg"));
                if (!backlog) {
                    notifyHandlers(EVENT_AWAY, object);
                }
            }
        });
        put("away", get("user_away"));
        put("user_back", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Buffer b = mBuffers.getBufferByName(object.cid(), object.getString("nick"));
                mUsers.updateAwayMsg(object.bid(), object.getString("nick"), 0, "");
                if(b != null)
                    b.setAway_msg("");
                if (!backlog) {
                    notifyHandlers(EVENT_AWAY, object);
                }
            }
        });
        put("self_away", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                if (!backlog) {
                    mUsers.updateAwayMsg(object.bid(), object.getString("nick"), 1, object.getString("away_msg"));
                    if(mServers.getServer(object.cid()) != null)
                        mServers.getServer(object.cid()).setAway(object.getString("away_msg"));
                    notifyHandlers(EVENT_AWAY, object);
                }
            }
        });
        put("self_back", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                mUsers.updateAwayMsg(object.bid(), object.getString("nick"), 0, "");
                if(mServers.getServer(object.cid()) != null)
                    mServers.getServer(object.cid()).setAway("");
                if (!backlog)
                    notifyHandlers(EVENT_SELFBACK, object);
            }
        });
        put("self_details", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Server s = mServers.getServer(object.cid());
                if(s != null)
                    s.setUsermask(object.getString("usermask"));
                Event event = mEvents.addEvent(object);
                if (!backlog) {
                    notifyHandlers(EVENT_SELFDETAILS, object);
                }
            }
        });
        put("user_mode", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                mEvents.addEvent(object);
                if (!backlog) {
                    Server s = mServers.getServer(object.cid());
                    if(s != null)
                        s.setMode(object.getString("newmode"));
                    notifyHandlers(EVENT_USERMODE, object);
                }
            }
        });

        //Various lists
        put("ban_list", new BroadcastParser(EVENT_BANLIST));
        put("accept_list", new BroadcastParser(EVENT_ACCEPTLIST));
        put("names_reply", new BroadcastParser(EVENT_NAMESLIST));
        put("whois_response", new BroadcastParser(EVENT_WHOIS));
        put("list_response_fetching", new BroadcastParser(EVENT_LISTRESPONSEFETCHING));
        put("list_response_toomany", new BroadcastParser(EVENT_LISTRESPONSETOOMANY));
        put("list_response", new BroadcastParser(EVENT_LISTRESPONSE));
        put("map_list", new BroadcastParser(EVENT_SERVERMAPLIST));
        put("quiet_list", new BroadcastParser(EVENT_QUIETLIST));
        put("ban_exception_list", new BroadcastParser(EVENT_BANEXCEPTIONLIST));
        put("invite_list", new BroadcastParser(EVENT_INVITELIST));
        put("who_response", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                if (!backlog) {
                    Buffer b = mBuffers.getBufferByName(object.cid(), object.getString("subject"));
                    if (b != null) {
                        JsonNode users = object.getJsonNode("users");
                        for (int i = 0; i < users.size(); i++) {
                            JsonNode user = users.get(i);
                            mUsers.updateHostmask(b.getBid(), user.get("nick").asText(), user.get("usermask").asText());
                            mUsers.updateAway(b.getBid(), user.get("nick").asText(), user.get("away").asBoolean() ? 1 : 0);
                        }
                    }
                    notifyHandlers(EVENT_WHOLIST, object);
                }
            }
        });
        put("time", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Event e = mEvents.addEvent(object);
                if (!backlog) {
                    notifyHandlers(EVENT_ALERT, object);
                    notifyHandlers(EVENT_BUFFERMSG, e);
                }
            }
        });
        put("channel_topic_is", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Buffer b = mBuffers.getBufferByName(object.cid(), object.getString("chan"));
                if (b != null) {
                    Channel c = mChannels.getChannelForBuffer(b.getBid());
                    if (c != null) {
                        c.topic_author = object.has("author") ? object.getString("author") : object.getString("server");
                        c.topic_time = object.getLong("time");
                        c.topic_text = object.getString("text");
                    }
                }
                if (!backlog)
                    notifyHandlers(EVENT_CHANNELTOPICIS, object);
            }
        });
    }};

    private synchronized void parse_object(IRCCloudJSONObject object) throws JSONException {
        cancel_idle_timer();
        //Log.d(TAG, "Backlog: " + backlog + " count: " + currentcount + " New event: " + object);
        if (!object.has("type")) {
            //Log.d(TAG, "Response: " + object);
            if (object.has("success") && !object.getBoolean("success") && object.has("message")) {
                Crashlytics.log(Log.ERROR, TAG, "Error: " + object);
                notifyHandlers(EVENT_FAILURE_MSG, object);
            } else if (object.has("success")) {
                notifyHandlers(EVENT_SUCCESS, object);
            }
            return;
        }
        String type = object.type();
        if (type != null && type.length() > 0) {
            //notifyHandlers(EVENT_DEBUG, "Type: " + type + " BID: " + object.bid() + " EID: " + object.eid());
            //Crashlytics.log("New event: " + type);
            //Log.d(TAG, "New event: " + type);
            if ((backlog || accrued > 0) && object.bid() > -1 && object.bid() != currentBid && object.eid > 0) {
                if(!backlog) {
                    if(firstEid == -1 && object.eid > highest_eid) {
                        firstEid = object.eid();
                        if (firstEid > highest_eid) {
                            Log.w("IRCCloud", "Backlog gap detected, purging cache");
                            mEvents.clear();
                        }
                    }
                }
                currentBid = object.bid();
                currentcount = 0;
            }
            Parser p = parserMap.get(type);
            if (p != null) {
                p.parse(object);
            } else if (!parserMap.containsKey(type)) {
                Crashlytics.log(Log.WARN, TAG, "Unhandled type: " + object.type());
                //Log.w(TAG, "Unhandled type: " + object);
            }

            if (backlog || type.equals("backlog_complete") || accrued > 0) {
                if ((object.bid() > -1 || type.equals("backlog_complete")) && !type.equals("makebuffer") && !type.equals("channel_init")) {
                    currentcount++;
                    if (object.bid() != currentBid) {
                        currentBid = object.bid();
                        currentcount = 0;
                    }
                }
            }
            if (backlog || type.equals("backlog_complete")) {
                if (numbuffers > 0 && currentcount < 100) {
                    notifyHandlers(EVENT_PROGRESS, ((totalbuffers + ((float) currentcount / (float) 100)) / numbuffers) * 1000.0f);
                }
            } else if (accrued > 0) {
                notifyHandlers(EVENT_PROGRESS, ((float) currentcount++ / (float) accrued) * 1000.0f);
            }
            if((!backlog || numbuffers > 0) && object.eid() > highest_eid) {
                highest_eid = object.eid();
                /*if(!backlog) {
                    SharedPreferences.Editor editor = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).edit();
                    editor.putLong("highest_eid", highest_eid);
                    editor.apply();
                }*/
            }
        }

        if (!backlog && idle_interval > 0 && accrued < 1)
            schedule_idle_timer();
    }

    public String fetch(URL url, String postdata, String sk, String token, HashMap<String, String>headers) throws Exception {
        HttpURLConnection conn = null;

        Proxy proxy = null;
        String host = null;
        int port = -1;

        if (Build.VERSION.SDK_INT < 11) {
            Context ctx = IRCCloudApplication.getInstance().getApplicationContext();
            if (ctx != null) {
                host = android.net.Proxy.getHost(ctx);
                port = android.net.Proxy.getPort(ctx);
            }
        } else {
            host = System.getProperty("http.proxyHost", null);
            try {
                port = Integer.parseInt(System.getProperty("http.proxyPort", "8080"));
            } catch (NumberFormatException e) {
                port = -1;
            }
        }

        if (host != null && host.length() > 0 && !host.equalsIgnoreCase("localhost") && !host.equalsIgnoreCase("127.0.0.1") && port > 0) {
            InetSocketAddress proxyAddr = new InetSocketAddress(host, port);
            proxy = new Proxy(Proxy.Type.HTTP, proxyAddr);
        }

        if (host != null && host.length() > 0 && !host.equalsIgnoreCase("localhost") && !host.equalsIgnoreCase("127.0.0.1") && port > 0) {
            Crashlytics.log(Log.DEBUG, TAG, "Requesting: " + url + " via proxy: " + host);
        } else {
            Crashlytics.log(Log.DEBUG, TAG, "Requesting: " + url);
        }

        if (url.getProtocol().toLowerCase().equals("https")) {
            HttpsURLConnection https = (HttpsURLConnection) ((proxy != null) ? url.openConnection(proxy) : url.openConnection(Proxy.NO_PROXY));
            if (url.getHost().equals(IRCCLOUD_HOST))
                https.setSSLSocketFactory(IRCCloudSocketFactory);
            conn = https;
        } else {
            conn = (HttpURLConnection) ((proxy != null) ? url.openConnection(proxy) : url.openConnection(Proxy.NO_PROXY));
        }

        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setUseCaches(false);
        conn.setRequestProperty("User-Agent", useragent);
        conn.setRequestProperty("Accept", "application/json");
        if(headers != null) {
            for (String key : headers.keySet()) {
                conn.setRequestProperty(key, headers.get(key));
            }
        }
        if (sk != null)
            conn.setRequestProperty("Cookie", "session=" + sk);
        if (token != null)
            conn.setRequestProperty("x-auth-formtoken", token);
        if (postdata != null) {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
            OutputStream ostr = null;
            try {
                ostr = conn.getOutputStream();
                ostr.write(postdata.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (ostr != null)
                    ostr.close();
            }
        }
        BufferedReader reader = null;
        String response = "";

        try {
            ConnectivityManager cm = (ConnectivityManager) IRCCloudApplication.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI) {
                Crashlytics.log(Log.DEBUG, TAG, "Loading via WiFi");
            } else {
                Crashlytics.log(Log.DEBUG, TAG, "Loading via mobile");
            }
        } catch (Exception e) {
        }

        try {
            if (conn.getInputStream() != null) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()), 512);
            }
        } catch (IOException e) {
            if (conn.getErrorStream() != null) {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream()), 512);
            }
        }

        if (reader != null) {
            response = toString(reader);
            reader.close();
        }
        conn.disconnect();
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

    public Bitmap fetchImage(URL url, boolean cacheOnly) throws Exception {
        HttpURLConnection conn = null;

        Proxy proxy = null;
        String host = null;
        int port = -1;

        if (Build.VERSION.SDK_INT < 11) {
            Context ctx = IRCCloudApplication.getInstance().getApplicationContext();
            if (ctx != null) {
                host = android.net.Proxy.getHost(ctx);
                port = android.net.Proxy.getPort(ctx);
            }
        } else {
            host = System.getProperty("http.proxyHost", null);
            try {
                port = Integer.parseInt(System.getProperty("http.proxyPort", "8080"));
            } catch (NumberFormatException e) {
                port = -1;
            }
        }

        if (host != null && host.length() > 0 && !host.equalsIgnoreCase("localhost") && !host.equalsIgnoreCase("127.0.0.1") && port > 0) {
            InetSocketAddress proxyAddr = new InetSocketAddress(host, port);
            proxy = new Proxy(Proxy.Type.HTTP, proxyAddr);
        }

        if (host != null && host.length() > 0 && !host.equalsIgnoreCase("localhost") && !host.equalsIgnoreCase("127.0.0.1") && port > 0) {
            Crashlytics.log(Log.DEBUG, TAG, "Requesting: " + url + " via proxy: " + host);
        } else {
            Crashlytics.log(Log.DEBUG, TAG, "Requesting: " + url);
        }

        if (url.getProtocol().toLowerCase().equals("https")) {
            HttpsURLConnection https = (HttpsURLConnection) ((proxy != null) ? url.openConnection(proxy) : url.openConnection(Proxy.NO_PROXY));
            if (url.getHost().equals(IRCCLOUD_HOST))
                https.setSSLSocketFactory(IRCCloudSocketFactory);
            conn = https;
        } else {
            conn = (HttpURLConnection) ((proxy != null) ? url.openConnection(proxy) : url.openConnection(Proxy.NO_PROXY));
        }

        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setUseCaches(true);
        conn.setRequestProperty("User-Agent", useragent);
        if(cacheOnly)
            conn.addRequestProperty("Cache-Control", "only-if-cached");
        Bitmap bitmap = null;

        try {
            ConnectivityManager cm = (ConnectivityManager) IRCCloudApplication.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            if (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI) {
                Crashlytics.log(Log.DEBUG, TAG, "Loading via WiFi");
            } else {
                Crashlytics.log(Log.DEBUG, TAG, "Loading via mobile");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (conn.getInputStream() != null) {
                bitmap = BitmapFactory.decodeStream(conn.getInputStream());
            }
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
        }

        conn.disconnect();
        return bitmap;
    }

    public synchronized void addHandler(IRCEventHandler handler) {
        synchronized (handlers) {
            if (!handlers.contains(handler))
                handlers.add(handler);
            if (saveTimerTask != null)
                saveTimerTask.cancel();
            if (notifierSockerTimerTask != null)
                notifierSockerTimerTask.cancel();
            saveTimerTask = null;
            notifierSockerTimerTask = null;
        }
    }

    public synchronized void removeHandler(IRCEventHandler handler) {
        synchronized (handlers) {
            handlers.remove(handler);
            if (notifierSockerTimerTask != null)
                notifierSockerTimerTask.cancel();
            notifierSockerTimerTask = new TimerTask() {
                @Override
                public void run() {
                    notifierSockerTimerTask = null;
                    if(!notifier && state == STATE_CONNECTED) {
                        disconnect();
                        try {
                            Thread.sleep(1000);
                            notifier = true;
                            connect();
                        } catch (Exception e) {
                        }
                    }
                }
            };

            idleTimer.schedule(notifierSockerTimerTask, 300000);
        }
    }

    public boolean uploadsAvailable() {
        return userInfo != null && !userInfo.uploads_disabled;
    }

    public boolean isVisible() {
        return (handlers != null && handlers.size() > 0);
    }

    public void notifyHandlers(int message, Object object) {
        notifyHandlers(message, object, null);
    }

    public synchronized void notifyHandlers(int message, Object object, IRCEventHandler exclude) {
        synchronized (handlers) {
            if (handlers != null && (message == EVENT_PROGRESS || accrued == 0)) {
                for (int i = 0; i < handlers.size(); i++) {
                    IRCEventHandler handler = handlers.get(i);
                    if (handler != exclude) {
                        handler.onIRCEvent(message, object);
                        if(message == EVENT_FAILURE_MSG) {
                            handler.onIRCRequestFailed(((IRCCloudJSONObject)object).getInt("_reqid"), (IRCCloudJSONObject)object);
                        }
                        if(message == EVENT_SUCCESS) {
                            handler.onIRCRequestSucceeded(((IRCCloudJSONObject)object).getInt("_reqid"), (IRCCloudJSONObject)object);
                        }
                    }
                }
            }
        }
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public class UserInfo {
        public int id;
        public String name;
        public String email;
        public boolean verified;
        public int last_selected_bid;
        public long connections;
        public long active_connections;
        public long join_date;
        public boolean auto_away;
        public String limits_name;
        public ObjectNode limits;
        public int num_invites;
        public JSONObject prefs;
        public String highlights;
        public boolean uploads_disabled;

        public UserInfo(IRCCloudJSONObject object) {
            SharedPreferences.Editor editor = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).edit();
            editor.putString("userinfo", object.toString());
            editor.apply();

            id = object.getInt("id");
            Crashlytics.log(Log.INFO, "IRCCloud", "Setting UserInfo for uid" + id);

            name = object.getString("name");
            email = object.getString("email");
            verified = object.getBoolean("verified");
            last_selected_bid = object.getInt("last_selected_bid");
            connections = object.getLong("num_connections");
            active_connections = object.getLong("num_active_connections");
            join_date = object.getLong("join_date");
            auto_away = object.getBoolean("autoaway");
            uploads_disabled = object.has("uploads_disabled") && object.getBoolean("uploads_disabled");

            if (object.has("prefs") && !object.getString("prefs").equals("null")) {
                try {
                    Crashlytics.log(Log.INFO, "IRCCloud", "Prefs: " + object.getString("prefs"));
                    prefs = new JSONObject(object.getString("prefs"));
                } catch (JSONException e) {
                    Crashlytics.log(Log.ERROR, "IRCCloud", "Unable to parse prefs: " + object.getString("prefs"));
                    Crashlytics.logException(e);
                    prefs = null;
                }
            } else {
                Crashlytics.log(Log.INFO, "IRCCloud", "User prefs not set");
                prefs = null;
            }

            limits_name = object.getString("limits_name");
            limits = object.getJsonObject("limits");

            if (object.has("highlights")) {
                JsonNode h = object.getJsonNode("highlights");
                highlights = "";
                for (int i = 0; i < h.size(); i++) {
                    if (highlights.length() > 0)
                        highlights += ", ";
                    highlights += h.get(i).asText();
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
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
                long totalTime = System.currentTimeMillis();
                long totalParseTime = 0;
                long totalJSONTime = 0;
                long longestEventTime = 0;
                String longestEventType = "";
                if (Build.VERSION.SDK_INT >= 14)
                    TrafficStats.setThreadStatsTag(BACKLOG_TAG);
                Crashlytics.log(Log.DEBUG, TAG, "Requesting: " + url[0]);
                mUrl = url[0];
                HttpURLConnection conn = null;
                Proxy proxy = null;
                String host = null;
                int port = -1;

                if (Build.VERSION.SDK_INT < 11) {
                    host = android.net.Proxy.getHost(IRCCloudApplication.getInstance().getApplicationContext());
                    port = android.net.Proxy.getPort(IRCCloudApplication.getInstance().getApplicationContext());
                } else {
                    host = System.getProperty("http.proxyHost", null);
                    port = Integer.parseInt(System.getProperty("http.proxyPort", "8080"));
                }

                if (host != null && host.length() > 0 && !host.equalsIgnoreCase("localhost") && !host.equalsIgnoreCase("127.0.0.1")) {
                    Crashlytics.log(Log.DEBUG, TAG, "Connecting via proxy: " + host);
                    InetSocketAddress proxyAddr = new InetSocketAddress(host, port);
                    proxy = new Proxy(Proxy.Type.HTTP, proxyAddr);
                }

                if (url[0].getProtocol().toLowerCase().equals("https")) {
                    HttpsURLConnection https = (proxy != null) ? (HttpsURLConnection) url[0].openConnection(proxy) : (HttpsURLConnection) url[0].openConnection(Proxy.NO_PROXY);
                    https.setSSLSocketFactory(IRCCloudSocketFactory);
                    conn = https;
                } else {
                    conn = (HttpURLConnection) ((proxy != null) ? url[0].openConnection(proxy) : url[0].openConnection(Proxy.NO_PROXY));
                }
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Connection", "close");
                conn.setRequestProperty("Cookie", "session=" + session);
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-type", "application/json");
                conn.setRequestProperty("Accept-Encoding", "gzip");
                conn.setRequestProperty("User-Agent", useragent);

                try {
                    ConnectivityManager cm = (ConnectivityManager) IRCCloudApplication.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo ni = cm.getActiveNetworkInfo();
                    if (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI) {
                        Crashlytics.log(Log.DEBUG, TAG, "Loading via WiFi");
                        conn.setConnectTimeout(2500);
                        conn.setReadTimeout(10000);
                    } else {
                        Crashlytics.log(Log.DEBUG, TAG, "Loading via mobile");
                        conn.setConnectTimeout(5000);
                        conn.setReadTimeout(30000);
                    }
                } catch (Exception e) {
                }

                conn.connect();
                if (conn.getResponseCode() == 200) {
                    InputStreamReader reader = null;
                    try {
                        if (conn.getInputStream() != null) {
                            if (conn.getContentEncoding() != null && conn.getContentEncoding().equalsIgnoreCase("gzip"))
                                reader = new InputStreamReader(new GZIPInputStream(conn.getInputStream()));
                            else if (conn.getInputStream() != null)
                                reader = new InputStreamReader(conn.getInputStream());
                        }
                    } catch (IOException e) {
                        if (conn.getErrorStream() != null) {
                            if (conn.getContentEncoding() != null && conn.getContentEncoding().equalsIgnoreCase("gzip"))
                                reader = new InputStreamReader(new GZIPInputStream(conn.getErrorStream()));
                            else if (conn.getErrorStream() != null)
                                reader = new InputStreamReader(conn.getErrorStream());
                        }
                    }

                    JsonParser parser = mapper.getFactory().createParser(reader);

                    if (reader != null && parser.nextToken() == JsonToken.START_ARRAY) {
                        synchronized (parserLock) {
                            cancel_idle_timer();
                            //if(ready)
                            //Debug.startMethodTracing("oob", 16*1024*1024);
                            Crashlytics.log(Log.DEBUG, TAG, "Connection time: " + (System.currentTimeMillis() - totalTime) + "ms");
                            Crashlytics.log(Log.DEBUG, TAG, "Beginning backlog...");
                            if (bid > 0)
                                notifyHandlers(EVENT_BACKLOG_START, null);
                            numbuffers = 0;
                            totalbuffers = 0;
                            currentBid = -1;
                            backlog = true;
                            if (bid == -1) {
                                mBuffers.invalidate();
                                mChannels.invalidate();
                            }
                            int count = 0;
                            while (parser.nextToken() == JsonToken.START_OBJECT) {
                                if (isCancelled()) {
                                    Crashlytics.log(Log.DEBUG, TAG, "Backlog parsing cancelled");
                                    return false;
                                }
                                long time = System.currentTimeMillis();
                                JsonNode e = parser.readValueAsTree();
                                totalJSONTime += (System.currentTimeMillis() - time);
                                time = System.currentTimeMillis();
                                IRCCloudJSONObject o = new IRCCloudJSONObject(e);
                                try {
                                    parse_object(o);
                                } catch (Exception ex) {
                                    Crashlytics.log(Log.ERROR, TAG, "Unable to parse message type: " + o.type());
                                    ex.printStackTrace();
                                    Crashlytics.logException(ex);
                                }
                                long t = (System.currentTimeMillis() - time);
                                if (t > longestEventTime) {
                                    longestEventTime = t;
                                    longestEventType = o.type();
                                }
                                totalParseTime += t;
                                count++;
                                if (Build.VERSION.SDK_INT >= 14)
                                    TrafficStats.incrementOperationCount(1);
                            }
                            backlog = false;
                            //Debug.stopMethodTracing();
                            totalTime = (System.currentTimeMillis() - totalTime);
                            Crashlytics.log(Log.DEBUG, TAG, "Backlog complete: " + count + " events");
                            Crashlytics.log(Log.DEBUG, TAG, "JSON parsing took: " + totalJSONTime + "ms (" + (totalJSONTime / (float) count) + "ms / object)");
                            Crashlytics.log(Log.DEBUG, TAG, "Backlog processing took: " + totalParseTime + "ms (" + (totalParseTime / (float) count) + "ms / object)");
                            Crashlytics.log(Log.DEBUG, TAG, "Total OOB load time: " + totalTime + "ms (" + (totalTime / (float) count) + "ms / object)");
                            Crashlytics.log(Log.DEBUG, TAG, "Longest event: " + longestEventType + " (" + longestEventTime + "ms)");
                            totalTime -= totalJSONTime;
                            totalTime -= totalParseTime;
                            Crashlytics.log(Log.DEBUG, TAG, "Total non-processing time: " + totalTime + "ms (" + (totalTime / (float) count) + "ms / object)");

                            ArrayList<Buffer> buffers = mBuffers.getBuffers();
                            for (Buffer b : buffers) {
                                if (b.getTimeout() > 0 && bid == -1) {
                                    Crashlytics.log(Log.DEBUG, TAG, "Requesting backlog for timed-out buffer: " + b.getName());
                                    request_backlog(b.getCid(), b.getBid(), 0);
                                }
                            }
                            NotificationsList.getInstance().deleteOldNotifications();
                            schedule_idle_timer();
                            if (bid > 0) {
                                notifyHandlers(EVENT_BACKLOG_END, bid);
                            }
                        }
                    } else {
                        throw new Exception("Unexpected JSON response");
                    }
                    parser.close();

                    if (bid != -1) {
                        Buffer b = mBuffers.getBuffer(bid);
                        if(b != null)
                            b.setTimeout(0);
                    }
                    synchronized (oobTasks) {
                        oobTasks.remove(bid);
                    }
                    Crashlytics.log(Log.DEBUG, TAG, "OOB fetch complete!");
                    if (Build.VERSION.SDK_INT >= 14)
                        TrafficStats.clearThreadStatsTag();
                    numbuffers = 0;
                    return true;
                } else {
                    Log.e(TAG, "Invalid response code: " + conn.getResponseCode());
                    throw new Exception("Invalid response code: " + conn.getResponseCode());
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (bid != -1) {
                    if (!isCancelled()) {
                        Buffer b = mBuffers.getBuffer(bid);
                        if (b != null && b.getTimeout() == 1) {
                            Crashlytics.log(Log.WARN, TAG, "Failed to fetch backlog for timed-out buffer, retrying in " + retryDelay + "ms");
                            idleTimer.schedule(new TimerTask() {
                                public void run() {
                                    doInBackground(mUrl);
                                }
                            }, retryDelay);
                            retryDelay *= 2;
                        } else {
                            Crashlytics.log(Log.ERROR, TAG, "Failed to fetch backlog");
                            synchronized (oobTasks) {
                                oobTasks.remove(bid);
                            }
                        }
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= 14)
                TrafficStats.clearThreadStatsTag();
            notifyHandlers(EVENT_BACKLOG_FAILED, null);
            backlog = false;
            if (bid == -1) {
                Crashlytics.log(Log.ERROR, TAG, "Failed to fetch the initial backlog, reconnecting!");
                streamId = null;
                if (client != null)
                    client.disconnect();
            }
            return false;
        }
    }
}
