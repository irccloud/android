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

package com.irccloud.android;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;

import com.codebutler.android_websockets.HybiParser;
import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.http.*;
import org.apache.http.client.HttpResponseException;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.message.BasicLineParser;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

@TargetApi(8)
public class OOBFetcher {
    private static final String TAG = "OOBFetcher";

    private URL mURI;
    private int mBid;
    private Socket mSocket;
    private Thread mThread;
    private static final HandlerThread mHandlerThread = new HandlerThread("oobfetcher-thread");
    private String mProxyHost;
    private int mProxyPort;
    private boolean isCancelled;
    private String mSession;

    private static final String ENABLED_CIPHERS[] = {
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

    private static final String ENABLED_PROTOCOLS[] = {
            "TLSv1.2", "TLSv1.1", "TLSv1"
    };

    private static TrustManager[] sTrustManagers;

    public static void setTrustManagers(TrustManager[] tm) {
        sTrustManagers = tm;
    }

    public OOBFetcher(URL uri, int bid, String session) {
        mURI = uri;
        mBid = bid;
        mSession = session;

        if(!mHandlerThread.isAlive())
            mHandlerThread.start();
    }

    public void cancel() {
        isCancelled = true;
    }

    public int getBid() {
        return mBid;
    }

    private ArrayList<Thread> mSocketThreads = new ArrayList<>();

    private class ConnectRunnable implements Runnable {
        private SocketFactory mSocketFactory;
        private InetSocketAddress mAddress;

        ConnectRunnable(SocketFactory factory, InetSocketAddress address) {
            mSocketFactory = factory;
            mAddress = address;
        }

        @Override
        public void run() {
            try {
                Crashlytics.log(Log.INFO, TAG, "Connecting to address: " + mAddress.getAddress() + " port: " + mAddress.getPort());
                Socket socket = mSocketFactory.createSocket();
                socket.connect(mAddress, 30000);
                if(mSocket == null) {
                    mSocket = socket;
                    Crashlytics.log(Log.INFO, TAG, "Connected to " + mAddress.getAddress());
                    if (mURI.getProtocol().equals("https")) {
                        SSLSocket s = (SSLSocket) mSocket;
                        try {
                            s.setEnabledProtocols(ENABLED_PROTOCOLS);
                        } catch (IllegalArgumentException e) {
                            //Not supported on older Android versions
                        }
                        try {
                            s.setEnabledCipherSuites(ENABLED_CIPHERS);
                        } catch (IllegalArgumentException e) {
                            //Not supported on older Android versions
                        }
                    }
                    start_socket_thread();
                } else {
                    socket.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                if(mSocket == null && mSocketThreads.size() == 1) {
                    NetworkConnection.printStackTraceToCrashlytics(ex);
                }
            }
            mSocketThreads.remove(Thread.currentThread());
        }
    }

    public void connect() {
        if (mThread != null && mThread.isAlive()) {
            return;
        }

        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(isCancelled)
                        return;

                    int port = (mURI.getPort() != -1) ? mURI.getPort() : (mURI.getProtocol().equals("https") ? 443 : 80);
                    SocketFactory factory = mURI.getProtocol().equals("https") ? getSSLSocketFactory() : SocketFactory.getDefault();
                    if (mProxyHost != null && mProxyHost.length() > 0) {
                        Crashlytics.log(Log.INFO, TAG, "Connecting to proxy: " + mProxyHost + " port: " + mProxyPort);
                        mSocket = SocketFactory.getDefault().createSocket(mProxyHost, mProxyPort);
                        start_socket_thread();
                    } else {
                        InetAddress[] addresses = InetAddress.getAllByName(mURI.getHost());
                        for (InetAddress address : addresses) {
                            if(mSocket == null) {
                                Thread t = new Thread(new ConnectRunnable(factory, new InetSocketAddress(address, port)));
                                mSocketThreads.add(t);
                                t.start();
                                Thread.sleep(300);
                            } else {
                                break;
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        mThread.start();
    }

    private void start_socket_thread() {
        mThread = new Thread(new Runnable() {
            @SuppressLint("NewApi")
            public void run() {
                try {
                    int port = (mURI.getPort() != -1) ? mURI.getPort() : (mURI.getProtocol().equals("wss") ? 443 : 80);

                    String path = TextUtils.isEmpty(mURI.getPath()) ? "/" : mURI.getPath();
                    if (!TextUtils.isEmpty(mURI.getQuery())) {
                        path += "?" + mURI.getQuery();
                    }

                    PrintWriter out = new PrintWriter(mSocket.getOutputStream());

                    if(mProxyHost != null && mProxyHost.length() > 0 && mProxyPort > 0) {
                        out.print("CONNECT " + mURI.getHost() + ":" + port + " HTTP/1.0\r\n");
                        out.print("\r\n");
                        out.flush();
                        HybiParser.HappyDataInputStream stream = new HybiParser.HappyDataInputStream(mSocket.getInputStream());

                        // Read HTTP response status line.
                        StatusLine statusLine = parseStatusLine(readLine(stream));
                        if (statusLine == null) {
                            throw new HttpException("Received no reply from server.");
                        } else if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                            throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
                        }

                        // Read HTTP response headers.
                        while (!TextUtils.isEmpty(readLine(stream)));
                        if(mURI.getProtocol().equals("https")) {
                            mSocket = getSSLSocketFactory().createSocket(mSocket, mURI.getHost(), port, false);
                            SSLSocket s = (SSLSocket)mSocket;
                            try {
                                s.setEnabledProtocols(ENABLED_PROTOCOLS);
                            } catch (IllegalArgumentException e) {
                                //Not supported on older Android versions
                            }
                            try {
                                s.setEnabledCipherSuites(ENABLED_CIPHERS);
                            } catch (IllegalArgumentException e) {
                                //Not supported on older Android versions
                            }
                            out = new PrintWriter(mSocket.getOutputStream());
                        }
                    }

                    if(mURI.getProtocol().equals("https")) {
                        SSLSocket s = (SSLSocket) mSocket;
                        StrictHostnameVerifier verifier = new StrictHostnameVerifier();
                        if (!verifier.verify(mURI.getHost(), s.getSession()))
                            throw new SSLException("Hostname mismatch");
                    }

                    out.print("GET " + path + " HTTP/1.1\r\n");
                    out.print("Host: " + mURI.getHost() + "\r\n");
                    out.print("Cookie: session=" + mSession + "\r\n");
                    out.print("Connection: close\r\n");
                    out.print("Content-Type: application/json\r\n");
                    out.print("Accept: application/json\r\n");
                    out.print("Accept-Encoding: gzip\r\n");
                    out.print("User-Agent: " + NetworkConnection.getInstance().useragent + "\r\n");
                    out.print("\r\n");
                    out.flush();

                    HybiParser.HappyDataInputStream stream = new HybiParser.HappyDataInputStream(mSocket.getInputStream());

                    // Read HTTP response status line.
                    StatusLine statusLine = parseStatusLine(readLine(stream));
                    if (statusLine == null) {
                        throw new HttpException("Received no reply from server.");
                    } else if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                        throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
                    }

                    // Read HTTP response headers.
                    String line;

                    boolean gzipped = false;
                    while (!TextUtils.isEmpty(line = readLine(stream))) {
                        Header header = parseHeader(line);
                        if(header.getName().equalsIgnoreCase("content-encoding") && header.getValue().equalsIgnoreCase("gzip"))
                            gzipped = true;
                    }

                    if(gzipped)
                        parse_oob(new GZIPInputStream(mSocket.getInputStream()));
                    else
                        parse_oob(mSocket.getInputStream());
                    if(!isCancelled)
                        NetworkConnection.getInstance().notifyHandlers(NetworkConnection.EVENT_OOB_END, OOBFetcher.this);
                } catch (Exception ex) {
                    NetworkConnection.printStackTraceToCrashlytics(ex);
                    if(!isCancelled)
                        NetworkConnection.getInstance().notifyHandlers(NetworkConnection.EVENT_OOB_FAILED, OOBFetcher.this);
                }
            }
        });
        mThread.setName("oob-parser-thread");
        mThread.start();
    }

    private void parse_oob(InputStream stream) throws Exception {
        if(isCancelled)
            return;

        long totalTime = System.currentTimeMillis();
        long totalParseTime = 0;
        long totalJSONTime = 0;
        long longestEventTime = 0;
        String longestEventType = "";

        NetworkConnection conn = NetworkConnection.getInstance();
        InputStreamReader reader = new InputStreamReader(stream);

        JsonParser parser = new ObjectMapper().getFactory().createParser(reader);

        if (parser.nextToken() == JsonToken.START_ARRAY && !isCancelled) {
            conn.cancel_idle_timer();
            //android.os.Debug.startMethodTracing("/sdcard/oob", 16*1024*1024);
            Crashlytics.log(Log.DEBUG, TAG, "Beginning backlog...");
            synchronized (conn.parserLock) {
                conn.notifyHandlers(NetworkConnection.EVENT_OOB_START, mBid);
                int count = 0;
                while (parser.nextToken() == JsonToken.START_OBJECT) {
                    if (isCancelled) {
                        Crashlytics.log(Log.DEBUG, TAG, "Backlog parsing cancelled");
                        return;
                    }
                    long time = System.currentTimeMillis();
                    JsonNode e = parser.readValueAsTree();
                    totalJSONTime += (System.currentTimeMillis() - time);
                    time = System.currentTimeMillis();
                    IRCCloudJSONObject o = new IRCCloudJSONObject(e);
                    try {
                        conn.parse_object(o);
                    } catch (Exception ex) {
                        Crashlytics.log(Log.ERROR, TAG, "Unable to parse message type: " + o.type());
                        NetworkConnection.printStackTraceToCrashlytics(ex);
                        Crashlytics.logException(ex);
                    }
                    long t = (System.currentTimeMillis() - time);
                    if (t > longestEventTime) {
                        longestEventTime = t;
                        longestEventType = o.type();
                    }
                    totalParseTime += t;
                    count++;
                }
                //android.os.Debug.stopMethodTracing();
                totalTime = (System.currentTimeMillis() - totalTime);
                Crashlytics.log(Log.DEBUG, TAG, "Backlog complete: " + count + " events");
                Crashlytics.log(Log.DEBUG, TAG, "JSON parsing took: " + totalJSONTime + "ms (" + (totalJSONTime / (float) count) + "ms / object)");
                Crashlytics.log(Log.DEBUG, TAG, "Backlog processing took: " + totalParseTime + "ms (" + (totalParseTime / (float) count) + "ms / object)");
                Crashlytics.log(Log.DEBUG, TAG, "Total OOB load time: " + totalTime + "ms (" + (totalTime / (float) count) + "ms / object)");
                Crashlytics.log(Log.DEBUG, TAG, "Longest event: " + longestEventType + " (" + longestEventTime + "ms)");
                totalTime -= totalJSONTime;
                totalTime -= totalParseTime;
                Crashlytics.log(Log.DEBUG, TAG, "Total non-processing time: " + totalTime + "ms (" + (totalTime / (float) count) + "ms / object)");
            }
            conn.schedule_idle_timer();
        } else {
            throw new Exception("Unexpected JSON response");
        }
        parser.close();
    }

    private StatusLine parseStatusLine(String line) {
        if (TextUtils.isEmpty(line)) {
            return null;
        }
        return BasicLineParser.parseStatusLine(line, new BasicLineParser());
    }

    private Header parseHeader(String line) {
        return BasicLineParser.parseHeader(line, new BasicLineParser());
    }

    // Can't use BufferedReader because it buffers past the HTTP data.
    private String readLine(HybiParser.HappyDataInputStream reader) throws IOException {
        int readChar = reader.read();
        if (readChar == -1) {
            return null;
        }
        StringBuilder string = new StringBuilder("");
        while (readChar != '\n') {
            if (readChar != '\r') {
                string.append((char) readChar);
            }

            readChar = reader.read();
            if (readChar == -1) {
                return null;
            }
        }
        return string.toString();
    }

    public void setProxy(String host, int port) {
        mProxyHost = host;
        mProxyPort = port;
    }

    private SSLSocketFactory getSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, sTrustManagers, null);
        return context.getSocketFactory();
    }
}

