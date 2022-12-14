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
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.codebutler.android_websockets.HybiParser;
import com.datatheorem.android.trustkit.TrustKit;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.HttpMetric;

import org.apache.http.conn.ssl.StrictHostnameVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import okhttp3.Headers;
import okhttp3.internal.http.StatusLine;

@TargetApi(8)
public class HTTPFetcher {
    private static final int MAX_THREADS = 6;
    private static final String TAG = "HTTPFetcher";

    protected URL mURI;
    protected Socket mSocket;
    protected Thread mThread;
    protected String mProxyHost;
    protected int mProxyPort;
    protected boolean isCancelled;
    private HttpMetric metric;

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

    public HTTPFetcher(URL uri) {
        mURI = uri;

        mProxyHost = System.getProperty("http.proxyHost", null);
        try {
            mProxyPort = Integer.parseInt(System.getProperty("http.proxyPort", "8080"));
        } catch (NumberFormatException e) {
            mProxyPort = -1;
        }

        if (mProxyHost != null && mProxyHost.length() > 0 && (mProxyHost.equalsIgnoreCase("localhost") || mProxyHost.equalsIgnoreCase("127.0.0.1")))
            mProxyHost = null;

        try {
            metric = FirebasePerformance.getInstance().newHttpMetric(uri, FirebasePerformance.HttpMethod.GET);
        } catch (Exception e) {
        }
    }

    public void cancel() {
        IRCCloudLog.Log(Log.INFO, TAG, "HTTP request cancelled");
        isCancelled = true;
    }

    private static final ArrayList<Thread> mSocketThreads = new ArrayList<>();
    private final ArrayList<Thread> mCurrentSocketThreads = new ArrayList<>();
    private int mAddressCount;
    private int mAttempts;

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
                IRCCloudLog.Log(Log.INFO, TAG, "Connecting to address: " + mAddress.getAddress() + " port: " + mAddress.getPort() + " (attempt " + mAttempts + ")");
                Socket socket = mSocketFactory.createSocket();
                if(Build.VERSION.SDK_INT < 24)
                    socket.getClass().getMethod("setHostname", String.class).invoke(socket, mURI.getHost());
                socket.connect(mAddress, 30000);
                if(mSocket == null) {
                    mSocket = socket;
                    IRCCloudLog.Log(Log.INFO, TAG, "Connected to " + mAddress.getAddress() + " (attempt " + mAttempts + ")");
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
                    mThread = Thread.currentThread();
                    http_thread();
                } else {
                    socket.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                if(mSocket == null) {
                    NetworkConnection.printStackTraceToCrashlytics(ex);
                }
            }
            mSocketThreads.remove(Thread.currentThread());
            mCurrentSocketThreads.remove(Thread.currentThread());
            if(mSocket == null && mCurrentSocketThreads.size() == 0 && mAttempts == mAddressCount) {
                IRCCloudLog.Log(Log.ERROR, TAG, "Failed to connect after " + mAttempts + " attempts");
                onFetchFailed();
            }
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

                    if(metric != null)
                        metric.start();
                    int port = (mURI.getPort() != -1) ? mURI.getPort() : (mURI.getProtocol().equals("https") ? 443 : 80);
                    SocketFactory factory = mURI.getProtocol().equals("https") ? getSSLSocketFactory() : SocketFactory.getDefault();
                    if (mProxyHost != null && mProxyHost.length() > 0 && mProxyPort > 0) {
                        IRCCloudLog.Log(Log.INFO, TAG, "Connecting to proxy: " + mProxyHost + " port: " + mProxyPort);
                        mSocket = SocketFactory.getDefault().createSocket(mProxyHost, mProxyPort);
                        mThread = new Thread(new Runnable() {
                            @SuppressLint("NewApi")
                            public void run() {
                                http_thread();
                            }
                        });
                        mThread.setName("http-stream-thread");
                        mThread.start();
                    } else {
                        InetAddress[] addresses = InetAddress.getAllByName(mURI.getHost());
                        mAddressCount = addresses.length;
                        for (InetAddress address : addresses) {
                            if(mSocket == null && !isCancelled) {
                                if(mSocketThreads.size() >= MAX_THREADS) {
                                    IRCCloudLog.Log(Log.INFO, TAG, "Waiting for other HTTP requests to complete before continuing");

                                    while (mSocketThreads.size() >= MAX_THREADS) {
                                        Thread.sleep(1000);
                                    }
                                }
                                Thread t = new Thread(new ConnectRunnable(factory, new InetSocketAddress(address, port)));
                                mSocketThreads.add(t);
                                mCurrentSocketThreads.add(t);
                                mAttempts++;
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

    private void http_thread() {
        try {
            mThread.setName("http-stream-thread");
            int port = (mURI.getPort() != -1) ? mURI.getPort() : (mURI.getProtocol().equals("https") ? 443 : 80);

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
                String statusLineString = readLine(stream);
                if (statusLineString == null) {
                    throw new Exception("Received no reply from server.");
                } else {
                    StatusLine statusLine = StatusLine.parse(statusLineString);
                    if (statusLine.code != HttpURLConnection.HTTP_OK) {
                        throw new Exception(statusLine.toString());
                    }
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

            IRCCloudLog.Log(Log.DEBUG, TAG, "Sending HTTP request");

            out.print("GET " + path + " HTTP/1.0\r\n");
            out.print("Host: " + mURI.getHost() + "\r\n");
            if(mURI.getHost().equals(NetworkConnection.IRCCLOUD_HOST) && NetworkConnection.getInstance().session != null && NetworkConnection.getInstance().session.length() > 0)
                out.print("Cookie: session=" + NetworkConnection.getInstance().session + "\r\n");
            out.print("Connection: close\r\n");
            out.print("Accept-Encoding: gzip\r\n");
            out.print("User-Agent: " + NetworkConnection.getInstance().useragent + "\r\n");
            out.print("\r\n");
            out.flush();

            HybiParser.HappyDataInputStream stream = new HybiParser.HappyDataInputStream(mSocket.getInputStream());

            // Read HTTP response status line.
            String statusLineString = readLine(stream);
            StatusLine statusLine;
            if (statusLineString == null) {
                throw new Exception("Received no reply from server.");
            } else {
                IRCCloudLog.Log(Log.DEBUG, TAG, "Got HTTP response: " + statusLineString);
                statusLine = StatusLine.parse(statusLineString);
                if(metric != null)
                    metric.setHttpResponseCode(statusLine.code);
                if (statusLine.code != HttpURLConnection.HTTP_OK && statusLine.code != HttpURLConnection.HTTP_MOVED_PERM) {
                    IRCCloudLog.Log(Log.ERROR, TAG, "Failure: " + mURI + ": " + statusLine.toString());
                    throw new Exception(statusLine.toString());
                }
            }

            // Read HTTP response headers.
            String line;

            boolean gzipped = false;
            while (!TextUtils.isEmpty(line = readLine(stream))) {
                int index = line.indexOf(":");
                Headers header = new Headers.Builder().add(line.substring(0, index).trim(), line.substring(index + 1)).build();
                if(header.name(0).equalsIgnoreCase("content-encoding") && header.value(0).equalsIgnoreCase("gzip"))
                    gzipped = true;
                if(metric != null && header.name(0).equalsIgnoreCase("content-type"))
                    metric.setResponseContentType(header.value(0));
                if(metric != null && header.name(0).equalsIgnoreCase("content-length"))
                    metric.setResponsePayloadSize(Long.valueOf(header.value(0)));
                if(statusLine.code == HttpURLConnection.HTTP_MOVED_PERM && header.name(0).equalsIgnoreCase("location")) {
                    IRCCloudLog.Log(Log.INFO, TAG, "Redirecting to: " + header.value(0));
                    mURI = new URL(header.value(0));
                    mSocket.close();
                    mSocket = null;
                    mThread = null;
                    connect();
                    return;
                }
            }

            if(gzipped)
                onStreamConnected(new GZIPInputStream(mSocket.getInputStream()));
            else
                onStreamConnected(mSocket.getInputStream());

            onFetchComplete();
            if(metric != null)
                metric.stop();
        } catch (Exception ex) {
            NetworkConnection.printStackTraceToCrashlytics(ex);
            onFetchFailed();
        }
    }

    protected void onFetchComplete() {

    }

    protected void onFetchFailed() {

    }

    protected void onStreamConnected(InputStream stream) throws Exception {

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

    private SSLSocketFactory getSSLSocketFactory() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLS");

        TrustManager[] trustManagers = null;
        trustManagers = new TrustManager[1];
        trustManagers[0] = TrustKit.getInstance().getTrustManager(mURI.getHost());

        context.init(null, trustManagers, null);
        return context.getSocketFactory();
    }
}

