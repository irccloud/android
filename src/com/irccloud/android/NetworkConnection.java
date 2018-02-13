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

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.codebutler.android_websockets.WebSocketClient;
import com.crashlytics.android.Crashlytics;
import com.damnhandy.uri.template.UriTemplate;
import com.datatheorem.android.trustkit.TrustKit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.android.gms.iid.InstanceID;
import com.irccloud.android.data.collection.ImageList;
import com.irccloud.android.data.collection.LogExportsList;
import com.irccloud.android.data.collection.NotificationsList;
import com.irccloud.android.data.collection.RecentConversationsList;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

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
    private static final RecentConversationsList mRecentConversations = RecentConversationsList.getInstance();

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public static final int STATE_DISCONNECTING = 3;
    private int state = STATE_DISCONNECTED;
    private long highest_eid = -1;

    public interface IRCEventHandler {
        void onIRCEvent(int message, Object object);
    }

    public interface IRCResultCallback {
        void onIRCResult(IRCCloudJSONObject result);
    }

    private WebSocketClient client = null;
    private UserInfo userInfo = null;
    private final ArrayList<IRCEventHandler> handlers = new ArrayList<IRCEventHandler>();
    private final HashMap<Integer, IRCResultCallback> resultCallbacks = new HashMap<>();
    public String session = null;
    private volatile int last_reqid = 0;
    private static final Timer idleTimer = new Timer("websocket-idle-timer");
    //private static final Timer saveTimer = new Timer("backlog-save-timer");
    private TimerTask idleTimerTask = null;
    private TimerTask saveTimerTask = null;
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
    public static String file_uri_template;
    public static String pastebin_uri_template;
    public static String avatar_uri_template;
    public static String avatar_redirect_uri_template;

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
    public static final int EVENT_CHANNELQUERY = 48;
    public static final int EVENT_WHOSPECIALRESPONSE = 49;
    public static final int EVENT_MODULESLIST = 50;
    public static final int EVENT_LINKSRESPONSE = 51;
    public static final int EVENT_WHOWAS = 52;
    public static final int EVENT_TRACERESPONSE = 53;
    public static final int EVENT_LOGEXPORTFINISHED = 54;
    public static final int EVENT_DISPLAYNAMECHANGE = 55;
    public static final int EVENT_AVATARCHANGE = 56;

    public static final int EVENT_BACKLOG_START = 100;
    public static final int EVENT_BACKLOG_END = 101;
    public static final int EVENT_BACKLOG_FAILED = 102;
    public static final int EVENT_AUTH_FAILED = 103;
    public static final int EVENT_TEMP_UNAVAILABLE = 104;
    public static final int EVENT_PROGRESS = 105;
    public static final int EVENT_ALERT = 106;
    public static final int EVENT_CACHE_START = 107;
    public static final int EVENT_CACHE_END = 108;
    public static final int EVENT_OOB_START = 109;
    public static final int EVENT_OOB_END = 110;
    public static final int EVENT_OOB_FAILED = 111;
    public static final int EVENT_FONT_DOWNLOADED = 112;

    public static final int EVENT_DEBUG = 999;

    public static String IRCCLOUD_HOST = BuildConfig.HOST;
    public static String IRCCLOUD_PATH = "/";

    public final Object parserLock = new Object();
    private WifiManager.WifiLock wifiLock = null;

    public long clockOffset = 0;

    private float numbuffers = 0;
    private float totalbuffers = 0;
    private int currentcount = 0;

    public boolean ready = false;
    public String globalMsg = null;

    private HashMap<Integer, OOBFetcher> oobTasks = new HashMap<Integer, OOBFetcher>();

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

    public SSLSocketFactory IRCCloudSocketFactory = new SSLSocketFactory() {
        final String CIPHERS[] = {
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA",
                "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
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
                printStackTraceToCrashlytics(e);
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
                printStackTraceToCrashlytics(e);
            }

            try {
                socket.setEnabledCipherSuites(CIPHERS);
            } catch (IllegalArgumentException e) {
                //Not supported on older Android versions
                printStackTraceToCrashlytics(e);
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
                printStackTraceToCrashlytics(e);
            }

            try {
                socket.setEnabledCipherSuites(CIPHERS);
            } catch (IllegalArgumentException e) {
                //Not supported on older Android versions
                printStackTraceToCrashlytics(e);
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
                printStackTraceToCrashlytics(e);
            }

            try {
                socket.setEnabledCipherSuites(CIPHERS);
            } catch (IllegalArgumentException e) {
                //Not supported on older Android versions
                printStackTraceToCrashlytics(e);
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
                printStackTraceToCrashlytics(e);
            }

            try {
                socket.setEnabledCipherSuites(CIPHERS);
            } catch (IllegalArgumentException e) {
                //Not supported on older Android versions
                printStackTraceToCrashlytics(e);
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
                printStackTraceToCrashlytics(e);
            }

            try {
                socket.setEnabledCipherSuites(CIPHERS);
            } catch (IllegalArgumentException e) {
                //Not supported on older Android versions
                printStackTraceToCrashlytics(e);
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
            try {
                ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo ni = cm.getActiveNetworkInfo();

                if (intent.hasExtra("networkInfo") && ((NetworkInfo) intent.getParcelableExtra("networkInfo")).isConnected()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        if (intent.getIntExtra("networkType", 0) == ConnectivityManager.TYPE_VPN) {
                            if (state == STATE_CONNECTED || state == STATE_CONNECTING) {
                                Crashlytics.log(Log.INFO, TAG, "A VPN has connected, reconnecting websocket");
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
                    } else {
                        boolean hasVPN = false;
                        try {
                            for (NetworkInterface intf : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                                if (!intf.isUp() || intf.getInterfaceAddresses().size() == 0)
                                    continue;

                                if (intf.getName().startsWith("tun") || intf.getName().startsWith("ppp")) {
                                    hasVPN = true;
                                    break;
                                }
                            }
                        } catch (Exception e) {
                        }

                        if (hasVPN && state == STATE_CONNECTED) {
                            Crashlytics.log(Log.INFO, TAG, "A network became available while a VPN is active, reconnecting");
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
                }

                Crashlytics.log(Log.INFO, TAG, "Connectivity changed, connected: " + ((ni != null) ? ni.isConnected() : "Unknown") + ", connected or connecting: " + ((ni != null) ? ni.isConnectedOrConnecting() : "Unknown"));

                if (ni != null && ni.isConnected() && (state == STATE_DISCONNECTED || state == STATE_DISCONNECTING) && session != null && handlers.size() > 0 && !notifier) {
                    Crashlytics.log(Log.INFO, TAG, "Network became available, reconnecting");
                    if (idleTimerTask != null)
                        idleTimerTask.cancel();
                    connect();
                } else if (ni == null || !ni.isConnected()) {
                    Crashlytics.log(Log.INFO, TAG, "Network lost, disconnecting");
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
            } catch (Exception e) {
                printStackTraceToCrashlytics(e);
                Crashlytics.logException(e);
            }
        }
    };

    BroadcastReceiver dataSaverListener = new BroadcastReceiver() {
        @TargetApi(24)
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && cm.isActiveNetworkMetered() && cm.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED) {
                Crashlytics.log(Log.INFO, TAG, "Data Saver was enabled");
                if(!isVisible() && state == STATE_CONNECTED) {
                    notifier = false;
                    disconnect();
                }
            } else {
                Crashlytics.log(Log.INFO, TAG, "Data Saver was disabled");
                if(isVisible() && state != STATE_CONNECTED)
                    connect();
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
            if(config.has("file_uri_template"))
                file_uri_template = config.getString("file_uri_template");
            else
                file_uri_template = null;

            if(config.has("pastebin_uri_template"))
                pastebin_uri_template = config.getString("pastebin_uri_template");
            else
                pastebin_uri_template = null;

            if(config.has("avatar_uri_template"))
                avatar_uri_template = config.getString("avatar_uri_template");
            else
                avatar_uri_template = null;

            if(config.has("avatar_redirect_uri_template"))
                avatar_redirect_uri_template = config.getString("avatar_redirect_uri_template");
            else
                avatar_redirect_uri_template = null;
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            config = new JSONObject();
        }

        String userinfojson = IRCCloudApplication.getInstance().getApplicationContext().getSharedPreferences("prefs", 0).getString("userinfo", null);
        if(userinfojson != null)
            userInfo = new UserInfo(new IRCCloudJSONObject(userinfojson));

        useragent = "IRCCloud" + version + " (" + android.os.Build.MODEL + "; " + Locale.getDefault().getCountry().toLowerCase() + "; "
                + "Android " + android.os.Build.VERSION.RELEASE;

        WindowManager wm = (WindowManager) IRCCloudApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
        useragent += "; " + wm.getDefaultDisplay().getWidth() + "x" + wm.getDefaultDisplay().getHeight();

        if (network_type != null)
            useragent += "; " + network_type;

        useragent += ")";

        Crashlytics.log(Log.INFO, "IRCCloud", useragent);

        WifiManager wfm = (WifiManager) IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wfm.createWifiLock(TAG);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
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
                            MessageDigest md = MessageDigest.getInstance("SHA-256");
                            byte[] sha256 = md.digest(chain[0].getEncoded());
                            // http://stackoverflow.com/questions/9655181/convert-from-byte-array-to-hex-string-in-java
                            final char[] hexArray = "0123456789ABCDEF".toCharArray();
                            char[] hexChars = new char[sha256.length * 2];
                            for (int j = 0; j < sha256.length; j++) {
                                int v = sha256[j] & 0xFF;
                                hexChars[j * 2] = hexArray[v >>> 4];
                                hexChars[j * 2 + 1] = hexArray[v & 0x0F];
                            }
                            String hexCharsStr = new String(hexChars);
                            boolean matched = false;
                            for (String fp : BuildConfig.SSL_FPS) {
                                if (fp.equals(hexCharsStr)) {
                                    matched = true;
                                    break;
                                }
                            }
                            if (!matched)
                                throw new CertificateException("Incorrect CN in cert chain");
                        } catch (NoSuchAlgorithmException e) {
                            printStackTraceToCrashlytics(e);
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
        IRCCloudApplication.getInstance().cancelNotifierTimer();
        try {
            if (wifiLock.isHeld())
                wifiLock.release();
        } catch (RuntimeException e) {

        }
        reconnect_timestamp = 0;
        synchronized (oobTasks) {
            for (Integer bid : oobTasks.keySet()) {
                try {
                    oobTasks.get(bid).cancel();
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
            printStackTraceToCrashlytics(e);
            return null;
        } catch (IOException e) {
            printStackTraceToCrashlytics(e);
            return null;
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            JSONObject o = new JSONObject();
            try {
                o.put("message", "json_error");
            } catch (JSONException e1) {
            }
            return o;
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
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
            printStackTraceToCrashlytics(e);
            return null;
        } catch (IOException e) {
            printStackTraceToCrashlytics(e);
            return null;
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            JSONObject o = new JSONObject();
            try {
                o.put("message", "json_error");
            } catch (JSONException e1) {
            }
            return o;
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
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
            printStackTraceToCrashlytics(e);
            return null;
        } catch (IOException e) {
            printStackTraceToCrashlytics(e);
            return null;
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            JSONObject o = new JSONObject();
            try {
                o.put("message", "json_error");
            } catch (JSONException e1) {
            }
            return o;
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
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
            printStackTraceToCrashlytics(e);
            return null;
        } catch (IOException e) {
            printStackTraceToCrashlytics(e);
            return null;
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            JSONObject o = new JSONObject();
            try {
                o.put("message", "json_error");
            } catch (JSONException e1) {
            }
            return o;
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
        }
        return null;
    }

    public JSONObject fetchJSON(String url) throws IOException {
        try {
            String response = fetch(new URL(url), null, null, null, null);
            return new JSONObject(response);
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
        }
        return null;
    }

    public JSONObject fetchJSON(String url, String postdata) throws IOException {
        try {
            String response = fetch(new URL(url), postdata, null, null, null);
            return new JSONObject(response);
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
        }
        return null;
    }

    public JSONObject fetchJSON(String url, HashMap<String, String>headers) throws IOException {
        try {
            String response = fetch(new URL(url), null, null, null, headers);
            return new JSONObject(response);
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
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

                if(config.has("file_uri_template"))
                    file_uri_template = config.getString("file_uri_template");
                else
                    file_uri_template = null;

                if(config.has("pastebin_uri_template"))
                    pastebin_uri_template = config.getString("pastebin_uri_template");
                else
                    pastebin_uri_template = null;

                if(config.has("avatar_uri_template"))
                    avatar_uri_template = config.getString("avatar_uri_template");
                else
                    avatar_uri_template = null;

                if(config.has("avatar_redirect_uri_template"))
                    avatar_redirect_uri_template = config.getString("avatar_redirect_uri_template");
                else
                    avatar_redirect_uri_template = null;

                if(BuildConfig.ENTERPRISE && !(config.get("enterprise") instanceof JSONObject)) {
                    globalMsg = "Some features, such as push notifications, may not work as expected.  Please download the standard IRCCloud app from the <a href=\"" + config.getString("android_app") + "\">Play Store</a>";
                    notifyHandlers(EVENT_GLOBALMSG, null);
                }
                set_pastebin_cookie();
            }
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
        }
        return config;
    }

    public void set_pastebin_cookie() {
        try {
            if (Build.VERSION.SDK_INT >= 16 && config != null) {
                CookieSyncManager sm = CookieSyncManager.createInstance(IRCCloudApplication.getInstance().getApplicationContext());
                CookieManager cm = CookieManager.getInstance();
                Uri u = Uri.parse(config.getString("pastebin_uri_template"));
                cm.setCookie(u.getScheme() + "://" + u.getHost() + "/", "session=" + session);
                if (Build.VERSION.SDK_INT >= 21)
                    cm.flush();
                sm.sync();
                cm.setAcceptCookie(true);
            }
        } catch (Exception e) {
        }
    }

    public JSONObject registerGCM(String regId, String sk) throws IOException {
        String postdata = "device_id=" + regId + "&session=" + sk;
        try {
            String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/gcm-register"), postdata, sk, null, null);
            if(response.length() > 0)
                return new JSONObject(response);
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
        }
        return null;
    }

    public JSONObject unregisterGCM(String regId, String sk) throws IOException {
        String postdata = "device_id=" + regId + "&session=" + sk;
        try {
            String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/gcm-unregister"), postdata, sk, null, null);
            if(response.length() > 0)
                return new JSONObject(response);
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
        }
        return null;
    }

    public JSONObject files(int page) throws IOException {
        try {
            String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/chat/files?page=" + page), null, session, null, null);
            if(response.length() > 0)
                return new JSONObject(response);
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
        }
        return null;
    }

    public JsonNode propertiesForFile(String fileID) throws IOException {
        try {
            String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/file/json/" + fileID), null, session, null, null);
            if(response.length() > 0)
                return new ObjectMapper().readValue(response, JsonNode.class);
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
        }
        return null;
    }

    public JSONObject pastebins(int page) throws IOException {
        try {
            String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/chat/pastebins?page=" + page), null, session, null, null);
            if(response.length() > 0)
                return new JSONObject(response);
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
        }
        return null;
    }

    public JSONObject logExports() throws IOException {
        try {
            String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/chat/log-exports"), null, session, null, null);
            if(response.length() > 0)
                return new JSONObject(response);
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
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
                    printStackTraceToCrashlytics(e);
                }
            }
        }, 50);
    }

    public JSONArray networkPresets() throws IOException {
        try {
            String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/static/networks.json"), null, null, null, null);
            if(response.length() > 0) {
                JSONObject o = new JSONObject(response);
                return o.getJSONArray("networks");
            }
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
        }
        return null;
    }

    @TargetApi(24)
    public void registerForConnectivity() {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            IRCCloudApplication.getInstance().getApplicationContext().registerReceiver(connectivityListener, intentFilter);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intentFilter = new IntentFilter();
                intentFilter.addAction(ConnectivityManager.ACTION_RESTRICT_BACKGROUND_CHANGED);
                IRCCloudApplication.getInstance().getApplicationContext().registerReceiver(dataSaverListener, intentFilter);
            }
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
        }
    }

    public void unregisterForConnectivity() {
        try {
            IRCCloudApplication.getInstance().getApplicationContext().unregisterReceiver(connectivityListener);
        } catch (IllegalArgumentException e) {
            //The broadcast receiver hasn't been registered yet
        }
        try {
            IRCCloudApplication.getInstance().getApplicationContext().unregisterReceiver(dataSaverListener);
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

    public void connect() {
        connect(false);
    }

    @TargetApi(24)
    public synchronized void connect(boolean ignoreNetworkState) {
        Crashlytics.log(Log.DEBUG, TAG, "connect()");
        Context ctx = IRCCloudApplication.getInstance().getApplicationContext();
        session = ctx.getSharedPreferences("prefs", 0).getString("session_key", "");
        String host = null;
        int port = -1;
        int limit = 100;

        if (session.length() == 0) {
            Crashlytics.log(Log.INFO, TAG, "Session key not set");
            state = STATE_DISCONNECTED;
            return;
        }

        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm != null) {
            NetworkInfo ni = cm.getActiveNetworkInfo();

            if (!ignoreNetworkState && ni != null && !ni.isConnectedOrConnecting()) {
                Crashlytics.log(Log.INFO, TAG, "No active network connection");
                cancel_idle_timer();
                state = STATE_DISCONNECTED;
                reconnect_timestamp = 0;
                notifyHandlers(EVENT_CONNECTIVITY, null);
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && cm.isActiveNetworkMetered() && cm.getRestrictBackgroundStatus() == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED) {
                limit = 50;
            }

            if (ni != null && ni.getType() == ConnectivityManager.TYPE_MOBILE && (ni.getSubtype() == TelephonyManager.NETWORK_TYPE_EDGE || ni.getSubtype() == TelephonyManager.NETWORK_TYPE_GPRS || ni.getSubtype() == TelephonyManager.NETWORK_TYPE_CDMA || ni.getSubtype() == TelephonyManager.NETWORK_TYPE_1xRTT)) {
                limit = 25;
            }
        }

        if (state == STATE_CONNECTING || state == STATE_CONNECTED) {
            Crashlytics.log(Log.INFO, TAG, "Ignoring duplicate connect request");
            return;
        }

        new FetchConfigTask().execute((Void)null);

        state = STATE_CONNECTING;

        if (saveTimerTask != null)
            saveTimerTask.cancel();
        saveTimerTask = null;

        if (oobTasks.size() > 0) {
            Crashlytics.log(Log.DEBUG, TAG, "Clearing OOB tasks before connecting");
        }
        synchronized (oobTasks) {

            for (Integer bid : oobTasks.keySet()) {
                try {
                    oobTasks.get(bid).cancel();
                } catch (Exception e) {
                    printStackTraceToCrashlytics(e);
                }
            }
            oobTasks.clear();
        }

        host = System.getProperty("http.proxyHost", null);
        try {
            port = Integer.parseInt(System.getProperty("http.proxyPort", "8080"));
        } catch (NumberFormatException e) {
            port = -1;
        }

        if (!wifiLock.isHeld())
            wifiLock.acquire();

        List<BasicNameValuePair> extraHeaders = Arrays.asList(
                new BasicNameValuePair("User-Agent", useragent)
        );

        String url = "wss://" + IRCCLOUD_HOST + IRCCLOUD_PATH;
        if (highest_eid > 0 && streamId != null && streamId.length() > 0) {
            url += "?exclude_archives=1&since_id=" + highest_eid + "&stream_id=" + streamId;
            if(notifier)
                url += "&notifier=1";
            url += "&limit=" + limit;
        } else if(notifier) {
            url += "?exclude_archives=1&notifier=1&limit="+limit;
        } else {
            url += "?exclude_archives=1&limit=" + limit;
        }

        if (host != null && host.length() > 0 && !host.equalsIgnoreCase("localhost") && !host.equalsIgnoreCase("127.0.0.1") && port > 0) {
            Crashlytics.log(Log.DEBUG, TAG, "Connecting: " + url + " via proxy: " + host);
        } else {
            Crashlytics.log(Log.DEBUG, TAG, "Connecting: " + url);
        }

        Crashlytics.log(Log.DEBUG, TAG, "Attempt: " + failCount);

        if(client != null) {
            client.setListener(null);
            client.disconnect();
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            TrustManager[] trustManagers = new TrustManager[1];
            trustManagers[0] = TrustKit.getInstance().getTrustManager(IRCCLOUD_HOST);
            WebSocketClient.setTrustManagers(trustManagers);
        }

        client = new WebSocketClient(URI.create(url), new WebSocketClient.Listener() {
            @Override
            public void onConnect() {
                if (client != null && client.getListener() == this) {
                    Crashlytics.log(Log.DEBUG, TAG, "WebSocket connected");
                    try {
                        JSONObject o = new JSONObject();
                        o.put("cookie", session);
                        send("auth", o, null);
                    } catch (JSONException e) {
                        printStackTraceToCrashlytics(e);
                    }

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
                    if (disconnectSockerTimerTask != null)
                        disconnectSockerTimerTask.cancel();
                    if (notifier) {
                        disconnectSockerTimerTask = new TimerTask() {
                            @Override
                            public void run() {
                                disconnectSockerTimerTask = null;
                                if (notifier) {
                                    Log.d("IRCCloud", "Notifier socket expired");
                                    disconnect();
                                }
                            }
                        };
                        idleTimer.schedule(disconnectSockerTimerTask, 600000);
                    }
                } else {
                    Crashlytics.log(Log.WARN, "IRCCloud", "Got websocket onConnect for inactive websocket");
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
                        printStackTraceToCrashlytics(e);
                    }
                }
            }

            @Override
            public void onMessage(byte[] data) {
                //Log.d(TAG, String.format("Got binary message! %s", toHexString(data));
            }

            @Override
            public void onDisconnect(int code, String reason) {
                if (client != null && client.getListener() == this) {
                    Crashlytics.log(Log.DEBUG, TAG, "WebSocket disconnected");
                    try {
                        if (wifiLock.isHeld())
                            wifiLock.release();
                    } catch (RuntimeException e) {

                    }

                    Crashlytics.log(Log.DEBUG, TAG, "Clearing OOB tasks");
                    synchronized (oobTasks) {
                        for (Integer bid : oobTasks.keySet()) {
                            try {
                                oobTasks.get(bid).cancel();
                            } catch (Exception e) {
                                printStackTraceToCrashlytics(e);
                            }
                        }
                        oobTasks.clear();
                    }

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
                } else {
                    Crashlytics.log(Log.WARN, "IRCCloud", "Got websocket onDisconnect for inactive websocket");
                }
            }

            @Override
            public void onError(Exception error) {
                if (client != null && client.getListener() == this) {
                    Crashlytics.log(Log.ERROR, TAG, "The WebSocket encountered an error: " + error.toString());
                    try {
                        if (wifiLock.isHeld())
                            wifiLock.release();
                    } catch (RuntimeException e) {

                    }
                    Crashlytics.log(Log.DEBUG, TAG, "Clearing OOB tasks");
                    synchronized (oobTasks) {
                        for (Integer bid : oobTasks.keySet()) {
                            try {
                                oobTasks.get(bid).cancel();
                            } catch (Exception e) {
                                printStackTraceToCrashlytics(e);
                            }
                        }
                        oobTasks.clear();
                    }
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
                } else {
                    Crashlytics.log(Log.WARN, "IRCCloud", "Got websocket onError for inactive websocket");
                }
            }
        }, extraHeaders);

        reconnect_timestamp = 0;
        idle_interval = 0;
        accrued = 0;
        resultCallbacks.clear();
        notifyHandlers(EVENT_CONNECTIVITY, null);
        if (client != null) {
            client.setDebugListener(new WebSocketClient.DebugListener() {
                @Override
                public void onDebugMsg(String msg) {
                    Crashlytics.log(Log.DEBUG, "IRCCloud", msg);
                }
            });
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
        try {
            if(BuildConfig.GCM_ID.length() > 0) {
                BackgroundTaskService.unregisterGCM(IRCCloudApplication.getInstance().getApplicationContext());
            }
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
        }
        ready = false;
        accrued = 0;
        highest_eid = -1;
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).edit();
        prefs.remove("name");
        prefs.remove("email");
        prefs.remove("highlights");
        prefs.remove("theme");
        prefs.remove("monospace");
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
        ImageList.getInstance().purge();
        RecentConversationsList.getInstance().clear();
        LogExportsList.getInstance().clear();
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

    private synchronized int send(String method, JSONObject params, IRCResultCallback callback) {
        if (client == null || (state != STATE_CONNECTED && !method.equals("auth")))
            return -1;
        try {
            params.put("_reqid", ++last_reqid);
            if(callback != null)
                resultCallbacks.put(last_reqid, callback);
            params.put("_method", method);
            //Log.d(TAG, "Reqid: " + last_reqid + " Method: " + method + " Params: " + params.toString());
            client.send(params.toString());
            return last_reqid;
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int heartbeat(int cid, int bid, long last_seen_eid, IRCResultCallback callback) {
        return heartbeat(bid, new Integer[]{cid}, new Integer[]{bid}, new Long[]{last_seen_eid}, callback);
    }

    public int heartbeat(int selectedBuffer, Integer cids[], Integer bids[], Long last_seen_eids[], IRCResultCallback callback) {
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
            return send("heartbeat", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int disconnect(int cid, String message, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            o.put("msg", message);
            return send("disconnect", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int reconnect(int cid, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            int reqid = send("reconnect", o, callback);
            if(reqid > 0) {
                Server s = mServers.getServer(cid);
                if(s != null) {
                    s.setStatus("queued");
                    notifyHandlers(EVENT_CONNECTIONLAG, new IRCCloudJSONObject(o));
                }
            }
            return reqid;
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int say(int cid, String to, String message, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            if (to != null)
                o.put("to", to);
            else
                o.put("to", "*");
            o.put("msg", message);
            return send("say", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public JSONObject postSay(int cid, String to, String message, String sk) throws IOException {
        if(to == null)
            to = "*";
        String postdata = "cid=" + cid + "&to=" + URLEncoder.encode(to, "UTF-8") + "&msg=" + URLEncoder.encode(message, "UTF-8") + "&session=" + sk;
        try {
            String response = fetch(new URL("https://" + IRCCLOUD_HOST + "/chat/say"), postdata, sk, null, null);
            return new JSONObject(response);
        } catch (Exception e) {
            printStackTraceToCrashlytics(e);
        }
        return null;
    }

    public int join(int cid, String channel, String key, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            o.put("channel", channel);
            o.put("key", key);
            return send("join", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int part(int cid, String channel, String message, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            o.put("channel", channel);
            o.put("msg", message);
            return send("part", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int kick(int cid, String channel, String nick, String message, IRCResultCallback callback) {
        return say(cid, channel, "/kick " + nick + " " + message, callback);
    }

    public int mode(int cid, String channel, String mode, IRCResultCallback callback) {
        return say(cid, channel, "/mode " + channel + " " + mode, callback);
    }

    public int invite(int cid, String channel, String nick, IRCResultCallback callback) {
        return say(cid, channel, "/invite " + nick + " " + channel, callback);
    }

    public int archiveBuffer(int cid, long bid, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            o.put("id", bid);
            return send("archive-buffer", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int unarchiveBuffer(int cid, long bid, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            o.put("id", bid);
            return send("unarchive-buffer", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int renameChannel(String name, int cid, long bid, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            o.put("id", bid);
            o.put("name", name);
            return send("rename-channel", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int renameConversation(String name, int cid, long bid, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            o.put("id", bid);
            o.put("name", name);
            return send("rename-conversation", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int deleteBuffer(int cid, long bid, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            o.put("id", bid);
            return send("delete-buffer", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int deleteServer(int cid, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            return send("delete-connection", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int deleteFile(String id, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("file", id);
            return send("delete-file", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int restoreFile(String id, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("file", id);
            return send("restore-file", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int addServer(String hostname, int port, int ssl, String netname, String nickname, String realname, String server_pass, String nickserv_pass, String joincommands, String channels, IRCResultCallback callback) {
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
            return send("add-server", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int editServer(int cid, String hostname, int port, int ssl, String netname, String nickname, String realname, String server_pass, String nickserv_pass, String joincommands, IRCResultCallback callback) {
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
            return send("edit-server", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int ignore(int cid, String mask, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            o.put("mask", mask);
            return send("ignore", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int unignore(int cid, String mask, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            o.put("mask", mask);
            return send("unignore", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int set_prefs(String prefs, IRCResultCallback callback) {
        try {
            Log.i("IRCCloud", "Setting prefs: " + prefs);
            JSONObject o = new JSONObject();
            o.put("prefs", prefs);
            return send("set-prefs", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int set_user_settings(String email, String realname, String hwords, boolean autoaway, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("email", email);
            o.put("realname", realname);
            o.put("hwords", hwords);
            o.put("autoaway", autoaway ? "1" : "0");
            return send("user-settings", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int change_password(String oldPassword, String newPassword, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("password", oldPassword);
            o.put("newpassword", newPassword);
            return send("change-password", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int delete_account(String password, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("password", password);
            return send("delete-account", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int ns_help_register(int cid, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            return send("ns-help-register", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int set_nspass(int cid, String nspass, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            o.put("nspass", nspass);
            return send("set-nspass", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int whois(int cid, String nick, String server, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            o.put("nick", nick);
            if (server != null)
                o.put("server", server);
            return send("whois", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int topic(int cid, String channel, String topic, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            o.put("channel", channel);
            o.put("topic", topic);
            return send("topic", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int back(int cid, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cid", cid);
            return send("back", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int reorder_connections(String cids, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("cids", cids);
            return send("reorder-connections", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int resend_verify_email(IRCResultCallback callback) {
        JSONObject o = new JSONObject();
        return send("resend-verify-email", o, callback);
    }

    public int finalize_upload(String id, String filename, String original_filename, boolean avatar, int orgId, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("filename", filename);
            o.put("original_filename", original_filename);
            if(avatar) {
                o.put("type", "avatar");
                if(orgId == -1) {
                    o.put("primary", "1");
                } else {
                    o.put("org", orgId);
                }
            }
            return send("upload-finalise", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int paste(String name, String extension, String contents, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            if(name != null && name.length() > 0)
                o.put("name", name);
            o.put("contents", contents);
            o.put("extension", extension);
            return send("paste", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int edit_paste(String id, String name, String extension, String contents, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            if(name != null && name.length() > 0)
                o.put("name", name);
            o.put("body", contents);
            o.put("extension", extension);
            return send("edit-pastebin", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int delete_paste(String id, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id);
            return send("delete-pastebin", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int export_log(int cid, int bid, String timezone, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            if(cid > 0)
                o.put("cid", cid);
            if(bid > 0)
                o.put("bid", bid);
            o.put("timezone", timezone);
            return send("export-log", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public int set_avatar(int orgId, String avatar_id, IRCResultCallback callback) {
        try {
            JSONObject o = new JSONObject();
            if(avatar_id != null)
                o.put("id", avatar_id);
            else
                o.put("clear", "1");
            if(orgId != -1)
                o.put("org", orgId);
            else
                o.put("primary", "1");
            return send("set-avatar", o, callback);
        } catch (JSONException e) {
            printStackTraceToCrashlytics(e);
            return -1;
        }
    }

    public void request_backlog(int cid, int bid, long beforeId) {
        try {
            synchronized (oobTasks) {
                if (oobTasks.containsKey(bid)) {
                    Crashlytics.log(Log.WARN, TAG, "Ignoring duplicate backlog request for BID: " + bid);
                    return;
                }
            }
            if (session == null || session.length() == 0) {
                Crashlytics.log(Log.WARN, TAG, "Not fetching backlog before session is set");
                return;
            }
            if (Looper.myLooper() == null)
                Looper.prepare();

            OOBFetcher task;
            if (beforeId > 0)
                task = new OOBFetcher(new URL("https://" + IRCCLOUD_HOST + "/chat/backlog?cid=" + cid + "&bid=" + bid + "&beforeid=" + beforeId), bid);
            else
                task = new OOBFetcher(new URL("https://" + IRCCLOUD_HOST + "/chat/backlog?cid=" + cid + "&bid=" + bid), bid);
            synchronized (oobTasks) {
                oobTasks.put(bid, task);
                if(oobTasks.size() == 1)
                    task.connect();
            }
        } catch (MalformedURLException e) {
            printStackTraceToCrashlytics(e);
        }
    }

    public void request_archives(int cid) {
        try {
            synchronized (oobTasks) {
                if (oobTasks.containsKey(cid)) {
                    Crashlytics.log(Log.WARN, TAG, "Ignoring duplicate archives request for CID: " + cid);
                    return;
                }
            }
            if (session == null || session.length() == 0) {
                Crashlytics.log(Log.WARN, TAG, "Not fetching archives before session is set");
                return;
            }

            OOBFetcher task = new OOBFetcher(new URL("https://" + IRCCLOUD_HOST + "/chat/archives?cid=" + cid), cid);
            synchronized (oobTasks) {
                oobTasks.put(cid, task);
                if(oobTasks.size() == 1)
                    task.connect();
            }
        } catch (MalformedURLException e) {
            printStackTraceToCrashlytics(e);
        }
    }

    public void upgrade() {
        if (disconnectSockerTimerTask != null)
            disconnectSockerTimerTask.cancel();
        notifier = false;
        send("upgrade_notifier", new JSONObject(), null);
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
        void parse(IRCCloudJSONObject object) throws JSONException;
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

    public HashMap<String, Parser> parserMap = new HashMap<String, Parser>() {{
        //Ignored events
        put("idle", null);
        put("end_of_backlog", null);
        put("user_account", null);
        put("twitch_hosttarget_start", null);
        put("twitch_hosttarget_stop", null);
        put("twitch_usernotice", null);

        put("header", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                if (!(object.has("resumed") && object.getBoolean("resumed"))) {
                    Log.d("IRCCloud", "Socket was not resumed");
                    NotificationsList.getInstance().clearLastSeenEIDs();
                    mEvents.clear();
                    if(streamId != null) {
                        Crashlytics.log(Log.WARN, "IRCCloud", "Unable to resume socket, requesting full OOB load");
                        highest_eid = 0;
                        streamId = null;
                        failCount = 0;
                        ready = false;
                        if (client != null)
                            client.disconnect();
                        return;
                    }
                }
                idle_interval = object.getLong("idle_interval") + 15000;
                clockOffset = object.getLong("time") - (System.currentTimeMillis() / 1000);
                currentcount = 0;
                currentBid = -1;
                firstEid = -1;
                streamId = object.getString("streamid");
                if (object.has("accrued"))
                    accrued = object.getInt("accrued");
                state = STATE_CONNECTED;
                notifyHandlers(EVENT_CONNECTIVITY, null);
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
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("name", userInfo.name);
                editor.putString("email", userInfo.email);
                editor.putString("highlights", userInfo.highlights);
                editor.putBoolean("autoaway", userInfo.auto_away);
                if (userInfo.prefs != null) {
                    editor.putBoolean("time-24hr", userInfo.prefs.has("time-24hr") && userInfo.prefs.get("time-24hr") instanceof Boolean && userInfo.prefs.getBoolean("time-24hr"));
                    editor.putBoolean("time-seconds", userInfo.prefs.has("time-seconds") && userInfo.prefs.get("time-seconds") instanceof Boolean && userInfo.prefs.getBoolean("time-seconds"));
                    editor.putBoolean("mode-showsymbol", userInfo.prefs.has("mode-showsymbol") && userInfo.prefs.get("mode-showsymbol") instanceof Boolean && userInfo.prefs.getBoolean("mode-showsymbol"));
                    editor.putBoolean("nick-colors", userInfo.prefs.has("nick-colors") && userInfo.prefs.get("nick-colors") instanceof Boolean && userInfo.prefs.getBoolean("nick-colors"));
                    editor.putBoolean("emoji-disableconvert", !(userInfo.prefs.has("emoji-disableconvert") && userInfo.prefs.get("emoji-disableconvert") instanceof Boolean && userInfo.prefs.getBoolean("emoji-disableconvert")));
                    editor.putBoolean("pastebin-disableprompt", !(userInfo.prefs.has("pastebin-disableprompt") && userInfo.prefs.get("pastebin-disableprompt") instanceof Boolean && userInfo.prefs.getBoolean("pastebin-disableprompt")));
                    editor.putBoolean("hideJoinPart", !(userInfo.prefs.has("hideJoinPart") && userInfo.prefs.get("hideJoinPart") instanceof Boolean && userInfo.prefs.getBoolean("hideJoinPart")));
                    editor.putBoolean("expandJoinPart", !(userInfo.prefs.has("expandJoinPart") && userInfo.prefs.get("expandJoinPart") instanceof Boolean && userInfo.prefs.getBoolean("expandJoinPart")));
                    editor.putBoolean("notifications_all", (userInfo.prefs.has("notifications-all") && userInfo.prefs.get("notifications-all") instanceof Boolean && userInfo.prefs.getBoolean("notifications-all")));
                    editor.putBoolean("disableTrackUnread", !(userInfo.prefs.has("disableTrackUnread") && userInfo.prefs.get("disableTrackUnread") instanceof Boolean && userInfo.prefs.getBoolean("disableTrackUnread")));
                    editor.putBoolean("enableReadOnSelect", (userInfo.prefs.has("enableReadOnSelect") && userInfo.prefs.get("enableReadOnSelect") instanceof Boolean && userInfo.prefs.getBoolean("enableReadOnSelect")));
                    editor.putBoolean("ascii-compact", (userInfo.prefs.has("ascii-compact") && userInfo.prefs.get("ascii-compact") instanceof Boolean && userInfo.prefs.getBoolean("ascii-compact")));
                    editor.putBoolean("emoji-nobig", !(userInfo.prefs.has("emoji-nobig") && userInfo.prefs.get("emoji-nobig") instanceof Boolean && userInfo.prefs.getBoolean("emoji-nobig")));
                    editor.putBoolean("files-disableinline", !(userInfo.prefs.has("files-disableinline") && userInfo.prefs.get("files-disableinline") instanceof Boolean && userInfo.prefs.getBoolean("files-disableinline")));
                    editor.putBoolean("chat-nocodespan", !(userInfo.prefs.has("chat-nocodespan") && userInfo.prefs.get("chat-nocodespan") instanceof Boolean && userInfo.prefs.getBoolean("chat-nocodespan")));
                    editor.putBoolean("chat-nocodeblock", !(userInfo.prefs.has("chat-nocodeblock") && userInfo.prefs.get("chat-nocodeblock") instanceof Boolean && userInfo.prefs.getBoolean("chat-nocodeblock")));
                    editor.putBoolean("chat-noquote", !(userInfo.prefs.has("chat-noquote") && userInfo.prefs.get("chat-noquote") instanceof Boolean && userInfo.prefs.getBoolean("chat-noquote")));
                    editor.putBoolean("inlineimages", (userInfo.prefs.has("inlineimages") && userInfo.prefs.get("inlineimages") instanceof Boolean && userInfo.prefs.getBoolean("inlineimages")));
                    if(userInfo.prefs.has("theme") && !prefs.contains("theme"))
                        editor.putString("theme", userInfo.prefs.getString("theme"));
                    if(userInfo.prefs.has("font") && !prefs.contains("monospace"))
                        editor.putBoolean("monospace", userInfo.prefs.getString("font").equals("mono"));
                    if(userInfo.prefs.has("time-left") && !prefs.contains("time-left")) {
                        editor.putBoolean("time-left", !(userInfo.prefs.has("time-left") && userInfo.prefs.get("time-left") instanceof Boolean && userInfo.prefs.getBoolean("time-left")));
                    }
                    if(userInfo.prefs.has("avatars-off") && !prefs.contains("avatars-off")) {
                        editor.putBoolean("avatars-off", !(userInfo.prefs.has("avatars-off") && userInfo.prefs.get("avatars-off") instanceof Boolean && userInfo.prefs.getBoolean("avatars-off")));
                    }
                    if(userInfo.prefs.has("chat-oneline") && !prefs.contains("chat-oneline")) {
                        editor.putBoolean("chat-oneline", !(userInfo.prefs.has("chat-oneline") && userInfo.prefs.get("chat-oneline") instanceof Boolean && userInfo.prefs.getBoolean("chat-oneline")));
                    }
                    if(userInfo.prefs.has("chat-norealname") && !prefs.contains("chat-norealname")) {
                        editor.putBoolean("chat-norealname", !(userInfo.prefs.has("chat-norealname") && userInfo.prefs.get("chat-norealname") instanceof Boolean && userInfo.prefs.getBoolean("chat-norealname")));
                    }
                    if(userInfo.prefs.has("labs") && userInfo.prefs.getJSONObject("labs").has("avatars") && !prefs.contains("avatar-images")) {
                        editor.putBoolean("avatar-images", userInfo.prefs.getJSONObject("labs").getBoolean("avatars"));
                    }
                } else {
                    editor.putBoolean("time-24hr", false);
                    editor.putBoolean("time-seconds", false);
                    editor.putBoolean("time-left", true);
                    editor.putBoolean("avatars-off", true);
                    editor.putBoolean("chat-oneline", true);
                    editor.putBoolean("chat-norealname", true);
                    editor.putBoolean("mode-showsymbol", false);
                    editor.putBoolean("nick-colors", false);
                    editor.putBoolean("emoji-disableconvert", true);
                    editor.putBoolean("pastebin-disableprompt", true);
                    editor.putBoolean("ascii-compact", false);
                    editor.putBoolean("emoji-nobig", true);
                    editor.putBoolean("files-disableinline", true);
                    editor.putBoolean("chat-nocodespan", true);
                    editor.putBoolean("chat-nocodeblock", true);
                    editor.putBoolean("chat-noquote", true);
                    editor.putBoolean("inlineimages", false);
                    editor.putBoolean("avatar-images", false);
                }
                editor.commit();
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
                    ready = false;
                    backlog = true;
                    OOBFetcher t = new OOBFetcher(new URL("https://" + IRCCLOUD_HOST + object.getString("url")), -1);
                    synchronized (oobTasks) {
                        oobTasks.put(-1, t);
                    }
                    t.connect();
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    printStackTraceToCrashlytics(e);
                }
            }
        });

        put("oob_timeout", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Crashlytics.log(Log.WARN, "IRCCloud", "OOB timed out");
                highest_eid = 0;
                streamId = null;
                ready = false;
                if (client != null)
                    client.disconnect();
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
                        ready = true;
                    }
                });

        put("backlog_cache_init", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                mEvents.deleteEventsForBuffer(object.bid());
                Buffer b = mBuffers.getBuffer(object.bid());
                if(b != null && b.getCreated() < object.eid())
                    b.setDeferred(1);
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
                NotificationsList.getInstance().pruneNotificationChannels();
                mRecentConversations.prune();
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
        final Parser alert = new BroadcastParser(EVENT_ALERT);
        String[] alerts = {"too_many_channels", "no_such_channel", "bad_channel_name",
                "no_such_nick", "invalid_nick_change", "chan_privs_needed",
                "accept_exists", "banned_from_channel", "oper_only",
                "no_nick_change", "no_messages_from_non_registered", "not_registered",
                "already_registered", "too_many_targets", "no_such_server",
                "unknown_command", "help_not_found", "accept_full",
                "accept_not", "nick_collision", "nick_too_fast", "need_registered_nick",
                "save_nick", "unknown_mode", "user_not_in_channel",
                "need_more_params", "users_dont_match", "users_disabled",
                "invalid_operator_password", "flood_warning", "privs_needed",
                "operator_fail", "not_on_channel", "ban_on_chan",
                "cannot_send_to_chan", "user_on_channel", "no_nick_given",
                "no_text_to_send", "no_origin", "only_servers_can_change_mode",
                "silence", "no_channel_topic", "invite_only_chan", "channel_full", "channel_key_set",
                "blocked_channel","unknown_error","channame_in_use","pong",
                "monitor_full","mlock_restricted","cannot_do_cmd","secure_only_chan",
                "cannot_change_chan_mode","knock_delivered","too_many_knocks",
                "chan_open","knock_on_chan","knock_disabled","cannotknock","ownmode",
                "nossl","redirect_error","invalid_flood","join_flood","metadata_limit",
                "metadata_targetinvalid","metadata_nomatchingkey","metadata_keyinvalid",
                "metadata_keynotset","metadata_keynopermission","metadata_toomanysubs"};
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
                        object.getInt("port"), object.getString("nick"), object.getString("status"), object.getBoolean("ssl") ? 1 : 0,
                        object.getString("realname"), object.getString("server_pass"), object.getString("nickserv_pass"), object.getString("join_commands"),
                        object.getJsonObject("fail_info"), away, object.getJsonNode("ignores"), (object.has("order") && !object.getString("order").equals("undefined")) ? object.getInt("order") : 0, object.getString("server_realname"),
                        object.getString("ircserver"), (object.has("orgid") && !object.getString("orgid").equals("undefined")) ? object.getInt("orgid") : 0, (object.has("avatars_supported") && !object.getString("avatars_supported").equals("undefined")) ? object.getInt("avatars_supported") : 0);

                NotificationsList.getInstance().updateServerNick(object.cid(), object.getString("nick"));
                NotificationsList.getInstance().addNotificationGroup(server.getCid(), server.getName() != null && server.getName().length() > 0 ? server.getName() : server.getHostname());

                if(object.has("deferred_archives"))
                    server.deferred_archives = object.getInt("deferred_archives");

                if(object.has("avatar"))
                    server.setAvatar(object.getString("avatar"));

                if(object.has("avatar_url"))
                    server.setAvatarURL(object.getString("avatar_url"));

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
                NotificationsList.getInstance().pruneNotificationChannels();
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
                    s.updateIsupport(object.getJsonObject("params"));
                    s.updateUserModes(object.getString("usermodes"));
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
                        (object.has("archived") && object.getBoolean("archived")) ? 1 : 0, (object.has("deferred") && object.getBoolean("deferred")) ? 1 : 0, (object.has("timeout") && object.getBoolean("timeout")) ? 1 : 0, object.getLong("created"));
                if(buffer.getTimeout() == 1 || buffer.getDeferred() == 1)
                    mEvents.deleteEventsForBuffer(buffer.getBid());
                NotificationsList.getInstance().updateLastSeenEid(buffer.getBid(), buffer.getLast_seen_eid());
                if(buffer.getTimeout() == 1 || buffer.getDeferred() == 1)
                    mEvents.deleteEventsForBuffer(buffer.getBid());
                if(buffer.getServer() != null && buffer.getArchived() == 1)
                    buffer.getServer().deferred_archives--;
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
                NotificationsList.getInstance().pruneNotificationChannels();
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
                Server s = mServers.getServer(object.cid());
                if(s != null && s.deferred_archives > 0)
                    s.deferred_archives--;
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
        put("rename_channel", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Buffer b = mBuffers.getBuffer(object.bid());
                if(b != null)
                    b.setName(object.getString("new_name"));
                Channel c = mChannels.getChannelForBuffer(object.bid());
                if(c != null)
                    c.name = object.getString("new_name");
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

                if(event.eid == -1) {
                    alert.parse(object);
                } else {
                    if (b != null && event.eid > b.getLast_seen_eid() && event.isImportant(b.getType())) {
                        if ((event.highlight || b.isConversation())) {
                            if (newEvent) {
                                b.setHighlights(b.getHighlights() + 1);
                                b.setUnread(1);
                            }
                            if (!backlog) {
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
                                    String message = ColorFormatter.strip(event.msg).toString();
                                    String server_name = b.getServer().getName();
                                    if (server_name == null || server_name.length() == 0)
                                        server_name = b.getServer().getHostname();
                                    String from = event.nick;
                                    if (from == null)
                                        from = (event.from != null) ? event.from : event.server;

                                    NotificationsList.getInstance().addNotification(event.cid, event.bid, event.eid, (event.nick != null) ? event.nick : event.from, message, b.getName(), b.getType(), event.type, server_name, event.getAvatarURL(512));
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

                if(event.self) {
                    mRecentConversations.updateConversation(event.cid, event.bid, event.eid / 1000);
                }

                if(b != null && b.isConversation() && b.getName().equalsIgnoreCase(event.from) && event.isMessage()) {
                    String avatar = event.getAvatarURL(512);
                    mRecentConversations.updateAvatar(event.cid, event.bid, avatar);
                    try {
                        if(avatar != null && PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("avatar-images", true)) {
                            URL url = new URL(event.getAvatarURL(512));
                            if (!ImageList.getInstance().cacheFile(url).exists()) {
                                ImageList.getInstance().fetchImage(url, null);
                            }
                        }
                    } catch (Exception e) {
                    }
                }

                if (!backlog) {
                    notifyHandlers(EVENT_BUFFERMSG, event);
                }
            }
        };

        String[] msgs = { "buffer_msg", "buffer_me_msg", "wait", "banned", "kill", "connecting_cancelled",
                "target_callerid", "notice", "server_motdstart", "server_welcome", "server_motd", "server_endofmotd",
                "server_nomotd", "server_luserclient", "server_luserop", "server_luserconns", "server_luserme", "server_n_local",
                "server_luserchannels", "server_n_global", "server_yourhost","server_created", "server_luserunknown",
                "services_down", "your_unique_id", "callerid", "target_notified", "myinfo", "hidden_host_set", "unhandled_line",
                "unparsed_line", "connecting_failed", "nickname_in_use", "channel_invite", "motd_response", "socket_closed",
                "channel_mode_list_change", "msg_services", "stats", "statslinkinfo", "statscommands", "statscline",
                "statsnline", "statsiline", "statskline", "statsqline", "statsyline", "statsbline", "statsgline", "statstline",
                "statseline", "statsvline", "statslline", "statsuptime", "statsoline", "statshline", "statssline", "statsuline",
                "statsdebug", "spamfilter", "endofstats", "inviting_to_channel", "error", "too_fast", "no_bots",
                "wallops", "logged_in_as", "sasl_fail", "sasl_too_long", "sasl_aborted", "sasl_already",
                "you_are_operator", "btn_metadata_set", "sasl_success", "version", "channel_name_change",
                "cap_ls", "cap_list", "cap_new", "cap_del", "cap_req","cap_ack","cap_nak","cap_raw","cap_invalid",
                "help_topics_start", "help_topics", "help_topics_end", "helphdr", "helpop", "helptlr", "helphlp", "helpfwd", "helpign",
                "newsflash", "invited", "server_snomask", "codepage", "logged_out", "nick_locked", "info_response", "generic_server_info",
                "unknown_umode", "bad_ping", "cap_raw", "rehashed_config", "knock", "bad_channel_mask", "kill_deny",
                "chan_own_priv_needed", "not_for_halfops", "chan_forbidden", "starircd_welcome", "zurna_motd",
                "ambiguous_error_message", "list_usage", "list_syntax", "who_syntax", "text", "admin_info",
                "watch_status", "sqline_nick", "user_chghost", "loaded_module", "unloaded_module", "invite_notify" };
        for (String event : msgs) {
            put(event, msg);
        }

        //Channel events
        put("link_channel", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                mEvents.addEvent(object);
                if (!backlog) {
                    notifyHandlers(EVENT_LINKCHANNEL, object);
                }
            }
        });
        put("channel_init", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                JsonNode topic = object.getJsonObject("topic");
                String set_by = "";
                if(topic != null && topic.has("nick") && !topic.get("nick").isNull())
                    set_by = topic.get("nick").asText();
                else if(topic != null && topic.has("server") && !topic.get("server").isNull())
                    set_by = topic.get("server").asText();
                Channel channel = mChannels.createChannel(object.cid(), object.bid(), object.getString("chan"),
                        (topic == null || topic.get("text").isNull()) ? "" : topic.get("text").asText(),
                        topic == null ? 0 : topic.get("time").asLong(),
                        set_by, object.getString("channel_type"),
                        object.getLong("timestamp"));
                mChannels.updateMode(object.bid(), object.getString("mode"), object.getJsonObject("ops"), true);
                mChannels.updateURL(object.bid(), object.getString("url"));
                mUsers.deleteUsersForBuffer(object.bid());
                JsonNode users = object.getJsonNode("members");
                Iterator<JsonNode> iterator = users.elements();
                while(iterator.hasNext()) {
                    JsonNode user = iterator.next();
                    User u = mUsers.createUser(object.cid(), object.bid(), user.get("nick").asText(), user.get("usermask").asText(), user.get("mode").asText(), user.get("ircserver").asText(), user.get("away").asBoolean() ? 1 : 0, user.hasNonNull("display_name")?user.get("display_name").asText():null,false);
                }
                mBuffers.dirty = true;
                if (!backlog)
                    notifyHandlers(EVENT_CHANNELINIT, channel);
            }
        });
        put("channel_topic", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                mEvents.addEvent(object);
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
                mEvents.addEvent(object);
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
                mEvents.addEvent(object);
                if (!backlog) {
                    User u = mUsers.createUser(object.cid(), object.bid(), object.getString("nick"), object.getString("hostmask"), "", object.getString("ircserver"), 0, object.getString("display_name"));
                    notifyHandlers(EVENT_JOIN, object);
                }
            }
        });
        put("you_joined_channel", get("joined_channel"));
        put("parted_channel", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                mEvents.addEvent(object);
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
                mEvents.addEvent(object);
                if (!backlog) {
                    mUsers.deleteUser(object.bid(), object.getString("nick"));
                    notifyHandlers(EVENT_QUIT, object);
                }
            }
        });
        put("quit_server", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                mEvents.addEvent(object);
                if (!backlog) {
                    notifyHandlers(EVENT_QUIT, object);
                }
            }
        });
        put("kicked_channel", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                mEvents.addEvent(object);
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
                mEvents.addEvent(object);
                if (!backlog) {
                    mUsers.updateNick(object.bid(), object.getString("oldnick"), object.getString("newnick"));
                    if (object.type().equals("you_nickchange")) {
                        if(mServers.getServer(object.cid()) != null)
                            mServers.getServer(object.cid()).setNick(object.getString("newnick"));
                        NotificationsList.getInstance().updateServerNick(object.cid(), object.getString("newnick"));
                    }
                    notifyHandlers(EVENT_NICKCHANGE, object);
                }
            }
        });
        put("you_nickchange", get("nickchange"));
        put("display_name_change", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                if (!backlog) {
                    mUsers.updateDisplayName(object.cid(), object.getString("nick"), object.getString("display_name"));
                    notifyHandlers(EVENT_DISPLAYNAMECHANGE, object);
                }
            }
        });
        put("user_channel_mode", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                mEvents.addEvent(object);
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
                    mUsers.updateAway(object.cid(), user.get("nick").asText(), user.get("away").asBoolean() ? 1 : 0);
                    mUsers.updateHostmask(object.bid(), user.get("nick").asText(), user.get("usermask").asText());
                }
                if (!backlog)
                    notifyHandlers(EVENT_MEMBERUPDATES, null);
            }
        });
        put("user_away", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Buffer b = mBuffers.getBuffer(object.bid());
                if(b != null && b.isConsole()) {
                    mUsers.updateAwayMsg(object.cid(), object.getString("nick"), 1, object.getString("msg"));
                    b = mBuffers.getBufferByName(object.cid(), object.getString("nick"));
                    if (b != null)
                        b.setAway_msg(object.getString("msg"));
                    if (!backlog) {
                        notifyHandlers(EVENT_AWAY, object);
                    }
                }
            }
        });
        put("away", get("user_away"));
        put("user_back", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                Buffer b = mBuffers.getBuffer(object.bid());
                if(b != null && b.isConsole()) {
                    mUsers.updateAwayMsg(object.cid(), object.getString("nick"), 0, "");
                    b = mBuffers.getBufferByName(object.cid(), object.getString("nick"));
                    if (b != null)
                        b.setAway_msg("");
                    if (!backlog) {
                        notifyHandlers(EVENT_AWAY, object);
                    }
                }
            }
        });
        put("self_away", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                if (!backlog) {
                    mUsers.updateAwayMsg(object.cid(), object.getString("nick"), 1, object.getString("away_msg"));
                    if(mServers.getServer(object.cid()) != null)
                        mServers.getServer(object.cid()).setAway(object.getString("away_msg"));
                    notifyHandlers(EVENT_AWAY, object);
                }
            }
        });
        put("self_back", new Parser() {
            @Override
            public void parse(IRCCloudJSONObject object) throws JSONException {
                mUsers.updateAwayMsg(object.cid(), object.getString("nick"), 0, "");
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
                if(s != null) {
                    s.setUsermask(object.getString("usermask"));
                    if(object.has("server_realname"))
                        s.setServerRealname(object.getString("server_realname"));
                }
                Event e = mEvents.addEvent(object);
                if (!backlog) {
                    notifyHandlers(EVENT_SELFDETAILS, e);
                    e = mEvents.getEvent(e.eid + 1, e.bid);
                    if(e != null)
                        notifyHandlers(EVENT_SELFDETAILS, e);
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
        put("channel_query", new BroadcastParser(EVENT_CHANNELQUERY));
        put("who_special_response", new BroadcastParser(EVENT_WHOSPECIALRESPONSE));
        put("modules_list", new BroadcastParser(EVENT_MODULESLIST));
        put("links_response", new BroadcastParser(EVENT_LINKSRESPONSE));
        put("whowas_response", new BroadcastParser(EVENT_WHOWAS));
        put("trace_response", new BroadcastParser(EVENT_TRACERESPONSE));
        put("export_finished", new BroadcastParser(EVENT_LOGEXPORTFINISHED));
        put("avatar_change", new BroadcastParser(EVENT_AVATARCHANGE));
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
                            mUsers.updateAway(b.getCid(), user.get("nick").asText(), user.get("away").asBoolean() ? 1 : 0);
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

    public synchronized void parse_object(IRCCloudJSONObject object) throws JSONException {
        cancel_idle_timer();
        //Log.d(TAG, "Backlog: " + backlog + " count: " + currentcount + " New event: " + object);
        if (!object.has("type")) {
            //Log.d(TAG, "Response: " + object);
            if (object.has("success") && !object.getBoolean("success") && object.has("message")) {
                Crashlytics.log(Log.ERROR, TAG, "Error: " + object);
                if(object.getString("message").equals("auth")) {
                    logout();
                    notifyHandlers(EVENT_AUTH_FAILED, object);
                } else if(object.getString("message").equals("set_shard")) {
                    disconnect();
                    ready = false;
                    SharedPreferences.Editor editor = IRCCloudApplication.getInstance().getSharedPreferences("prefs", 0).edit();
                    editor.putString("session_key", object.getString("cookie"));
                    if (object.has("websocket_host")) {
                        IRCCLOUD_HOST = object.getString("websocket_host");
                        IRCCLOUD_PATH = object.getString("websocket_path");
                    }
                    editor.putString("host", NetworkConnection.IRCCLOUD_HOST);
                    editor.putString("path", NetworkConnection.IRCCLOUD_PATH);
                    editor.commit();
                    connect();
                } else if(object.getString("message").equals("temp_unavailable")) {
                    notifyHandlers(EVENT_TEMP_UNAVAILABLE, object);
                } else if(object.getString("message").equals("invalid_nick")) {
                    notifyHandlers(EVENT_INVALIDNICK, object);
                }
            }
            if (object.has("_reqid") && resultCallbacks.containsKey(object.getInt("_reqid"))) {
                resultCallbacks.get(object.getInt("_reqid")).onIRCResult(object);
                resultCallbacks.remove(object.getInt("_reqid"));
            }
            return;
        }
        String type = object.type();
        if (type != null && type.length() > 0) {
            //notifyHandlers(EVENT_DEBUG, "Type: " + type + " BID: " + object.bid() + " EID: " + object.eid());
            //Crashlytics.log("New event: " + type);
            //Log.d(TAG, "New event: " + type);
            if ((backlog || accrued > 0) && object.bid() > -1 && object.bid() != currentBid && object.eid() > 0) {
                if(!backlog) {
                    if(firstEid == -1) {
                        firstEid = object.eid();
                        if (firstEid > highest_eid) {
                            Log.w("IRCCloud", "Backlog gap detected, purging cache and reconnecting");
                            highest_eid = 0;
                            streamId = null;
                            failCount = 0;
                            if (client != null)
                                client.disconnect();
                            return;
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

    public boolean isWifi() {
        ConnectivityManager cm = (ConnectivityManager) IRCCloudApplication.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI;
    }

    public String fetch(URL url, String postdata, String sk, String token, HashMap<String, String>headers) throws Exception {
        HttpURLConnection conn = null;

        Proxy proxy = null;
        int port = -1;

        String host = System.getProperty("http.proxyHost", null);
        try {
            port = Integer.parseInt(System.getProperty("http.proxyPort", "8080"));
        } catch (NumberFormatException e) {
            port = -1;
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
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 && url.getHost().equals(IRCCLOUD_HOST))
                https.setSSLSocketFactory(IRCCloudSocketFactory);
            else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                https.setSSLSocketFactory(TrustKit.getInstance().getSSLSocketFactory(url.getHost()));
            conn = https;
        } else {
            conn = (HttpURLConnection) ((proxy != null) ? url.openConnection(proxy) : url.openConnection(Proxy.NO_PROXY));
        }

        conn.setReadTimeout(30000);
        conn.setConnectTimeout(30000);
        conn.setUseCaches(false);
        conn.setRequestProperty("User-Agent", useragent);
        conn.setRequestProperty("Accept", "application/json");
        if(headers != null) {
            for (String key : headers.keySet()) {
                conn.setRequestProperty(key, headers.get(key));
            }
        }
        if (sk != null && url.getProtocol().equals("https") && url.getUserInfo() == null && (url.getHost().equals(IRCCLOUD_HOST) || (!BuildConfig.ENTERPRISE && url.getHost().endsWith(".irccloud.com"))))
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
                printStackTraceToCrashlytics(e);
            } finally {
                if (ostr != null)
                    ostr.close();
            }
        }
        InputStream is = null;
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
                is = conn.getInputStream();
            }
        } catch (IOException e) {
            if (conn.getErrorStream() != null) {
                is = conn.getErrorStream();
            } else {
                printStackTraceToCrashlytics(e);
            }
        }

        if (is != null) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            response = os.toString("UTF-8");
            is.close();
        }
        conn.disconnect();
        return response;
    }

    public synchronized void addHandler(IRCEventHandler handler) {
        synchronized (handlers) {
            if (!handlers.contains(handler))
                handlers.add(handler);
            if (saveTimerTask != null)
                saveTimerTask.cancel();
            saveTimerTask = null;
        }
    }

    @TargetApi(24)
    public synchronized void removeHandler(IRCEventHandler handler) {
        synchronized (handlers) {
            handlers.remove(handler);
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

    public synchronized void notifyHandlers(int message, final Object object, IRCEventHandler exclude) {
        int bid;

        switch(message) {
            case EVENT_OOB_START:
                backlog = true;
                numbuffers = 0;
                totalbuffers = 0;
                currentBid = -1;
                if (object != null && (int)object == -1) {
                    mBuffers.invalidate();
                    mChannels.invalidate();
                    return;
                }
                break;
            case EVENT_OOB_END:
                backlog = false;
                bid = ((OOBFetcher)object).getBid();
                ArrayList<Buffer> buffers = mBuffers.getBuffers();
                for (Buffer b : buffers) {
                    if (b.getTimeout() > 0) {
                        Crashlytics.log(Log.DEBUG, TAG, "Requesting backlog for timed-out buffer: bid" + b.getBid());
                        request_backlog(b.getCid(), b.getBid(), 0);
                    }

                    if(oobTasks.size() > 10)
                        break;
                }
                NotificationsList.getInstance().deleteOldNotifications();
                NotificationsList.getInstance().pruneNotificationChannels();
                if (bid != -1) {
                    Buffer b = mBuffers.getBuffer(bid);
                    if(b != null) {
                        b.setTimeout(0);
                        b.setDeferred(0);
                    }
                }
                oobTasks.remove(bid);
                if(oobTasks.size() > 0)
                    oobTasks.values().toArray(new OOBFetcher[oobTasks.values().size()])[0].connect();
                break;
            case EVENT_OOB_FAILED:
                backlog = false;
                bid = ((OOBFetcher)object).getBid();
                if (bid == -1) {
                    Crashlytics.log(Log.ERROR, TAG, "Failed to fetch the initial backlog, reconnecting!");
                    streamId = null;
                    highest_eid = 0;
                    if (client != null)
                        client.disconnect();
                    return;
                } else {
                    Buffer b = mBuffers.getBuffer(bid);
                    if (b != null && b.getTimeout() == 1) {
                        //TODO: move this
                        int retryDelay = 1000;
                        Crashlytics.log(Log.WARN, TAG, "Failed to fetch backlog for timed-out buffer, retrying in " + retryDelay + "ms");
                        idleTimer.schedule(new TimerTask() {
                            public void run() {
                                ((OOBFetcher)object).connect();
                            }
                        }, retryDelay);
                        retryDelay *= 2;
                    } else {
                        Crashlytics.log(Log.ERROR, TAG, "Failed to fetch backlog");
                        synchronized (oobTasks) {
                            oobTasks.remove(bid);
                            if(oobTasks.size() > 0)
                                oobTasks.values().toArray(new OOBFetcher[oobTasks.values().size()])[0].connect();
                        }
                    }
                }
                break;
            default:
                break;
        }

        synchronized (handlers) {
            if (handlers != null && (message == EVENT_PROGRESS || accrued == 0)) {
                for (int i = 0; i < handlers.size(); i++) {
                    IRCEventHandler handler = handlers.get(i);
                    if (handler != exclude) {
                        handler.onIRCEvent(message, object);
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
        public String avatar;

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
            if(object.has("avatar") && object.getString("avatar").length() > 0 && !object.getString("avatar").equals("null"))
                avatar = object.getString("avatar");
            else
                avatar = null;

            if (object.has("prefs") && object.getString("prefs").length() > 0 && !object.getString("prefs").equals("null")) {
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

    public static void printStackTraceToCrashlytics(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String stack = sw.toString();
        for(String s : stack.split("\n")) {
            Crashlytics.log(Log.WARN, TAG, s);
        }
    }

    private class FetchConfigTask extends AsyncTaskEx<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            fetchConfig();
            return null;
        }
    }
}
