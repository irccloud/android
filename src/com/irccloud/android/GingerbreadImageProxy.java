// Copyright 2010 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.irccloud.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.SSLCertificateSocketFactory;
import android.os.Build;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseFactory;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionOperator;
import org.apache.http.conn.OperatedClientConnection;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.DefaultClientConnection;
import org.apache.http.impl.conn.DefaultClientConnectionOperator;
import org.apache.http.impl.conn.DefaultResponseParser;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.io.HttpMessageParser;
import org.apache.http.io.SessionInputBuffer;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.CharArrayBuffer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

public class GingerbreadImageProxy implements Runnable {
    private static final String LOG_TAG = GingerbreadImageProxy.class.getName();

    private int port = 0;

    public int getPort() {
        return port;
    }

    public boolean isRunning() {
        return isRunning;
    }

    private boolean isRunning = false;
    private ServerSocket socket;
    private Thread thread;

    public void init() {
        try {
            socket = new ServerSocket(port, 0, InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
            socket.setSoTimeout(5000);
            port = socket.getLocalPort();
        } catch (UnknownHostException e) {
            Log.e(LOG_TAG, "Error initializing server", e);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error initializing server", e);
        }
    }

    public void start() {
        if (socket == null) {
            throw new IllegalStateException("Cannot start proxy; it has not been initialized.");
        }

        isRunning = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        isRunning = false;

        if (thread == null) {
            throw new IllegalStateException("Cannot stop proxy; it has not been started.");
        }

        thread.interrupt();
        try {
            thread.join(5000);
        } catch (InterruptedException e) {
        }
        thread = null;
    }

    public void run() {
        while (isRunning) {
            try {
                Socket client = socket.accept();
                if (client == null) {
                    continue;
                }
                HttpRequest request = readRequest(client);
                if (isRunning)
                    processRequest(request, client);
                client.close();
            } catch (SocketTimeoutException e) {
                // Do nothing
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error connecting to client", e);
            }
        }
    }

    private HttpRequest readRequest(Socket client) {
        HttpRequest request = null;
        InputStream is;
        String firstLine;
        String range = null;
        String ua = null;
        try {
            is = client.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is),
                    8192);
            firstLine = reader.readLine();

            String line = null;

            do {
                line = reader.readLine();
                if (line != null && line.toLowerCase().startsWith("range: ")) {
                    range = line.substring(7);
                }
                if (line != null && line.toLowerCase().startsWith("user-agent: ")) {
                    ua = line.substring(12);
                }
            } while (line != null && reader.ready());
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error parsing request", e);
            return request;
        }

        if (firstLine == null) {
            Log.i(LOG_TAG, "Proxy client closed connection without a request.");
            return request;
        }

        StringTokenizer st = new StringTokenizer(firstLine);
        String method = st.nextToken();
        String uri = st.nextToken();
        String realUri = uri.substring(1);
        request = new BasicHttpRequest(method, realUri, new ProtocolVersion("HTTP", 1, 1));
        if (range != null)
            request.addHeader("Range", range);
        if (ua != null)
            request.addHeader("User-Agent", ua);
        return request;
    }

    private void processRequest(HttpRequest request, Socket client)
            throws IllegalStateException, IOException {
        if (request == null) {
            return;
        }
        URL url = new URL(request.getRequestLine().getUri());

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

        if (url.getProtocol().toLowerCase().equals("https")) {
            conn = (HttpsURLConnection) ((proxy != null) ? url.openConnection(proxy) : url.openConnection(Proxy.NO_PROXY));
        } else {
            conn = (HttpURLConnection) ((proxy != null) ? url.openConnection(proxy) : url.openConnection(Proxy.NO_PROXY));
        }

        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setUseCaches(true);

        if(!isRunning)
            return;

        try {
            client.getOutputStream().write(("HTTP/1.0 " + conn.getResponseCode() + " " + conn.getResponseMessage() + "\n\n").getBytes());

            if (conn.getResponseCode() == 200 && conn.getInputStream() != null) {
                byte[] buff = new byte[8192];
                int readBytes;
                while (isRunning && (readBytes = conn.getInputStream().read(buff, 0, buff.length)) != -1) {
                    client.getOutputStream().write(buff, 0, readBytes);
                }
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            client.close();
        }

        conn.disconnect();
        stop();
    }

    private class IcyLineParser extends BasicLineParser {
        private static final String ICY_PROTOCOL_NAME = "ICY";

        private IcyLineParser() {
            super();
        }

        @Override
        public boolean hasProtocolVersion(CharArrayBuffer buffer,
                                          ParserCursor cursor) {
            boolean superFound = super.hasProtocolVersion(buffer, cursor);
            if (superFound) {
                return true;
            }
            int index = cursor.getPos();

            final int protolength = ICY_PROTOCOL_NAME.length();

            if (buffer.length() < protolength)
                return false; // not long enough for "HTTP/1.1"

            if (index < 0) {
                // end of line, no tolerance for trailing whitespace
                // this works only for single-digit major and minor version
                index = buffer.length() - protolength;
            } else if (index == 0) {
                // beginning of line, tolerate leading whitespace
                while ((index < buffer.length()) &&
                        HTTP.isWhitespace(buffer.charAt(index))) {
                    index++;
                }
            } // else within line, don't tolerate whitespace

            return index + protolength <= buffer.length() &&
                    buffer.substring(index, index + protolength).equals(ICY_PROTOCOL_NAME);

        }


        @Override
        public ProtocolVersion parseProtocolVersion(CharArrayBuffer buffer,
                                                    ParserCursor cursor) throws ParseException {

            if (buffer == null) {
                throw new IllegalArgumentException("Char array buffer may not be null");
            }
            if (cursor == null) {
                throw new IllegalArgumentException("Parser cursor may not be null");
            }

            final int protolength = ICY_PROTOCOL_NAME.length();

            int indexFrom = cursor.getPos();
            int indexTo = cursor.getUpperBound();

            skipWhitespace(buffer, cursor);

            int i = cursor.getPos();

            // long enough for "HTTP/1.1"?
            if (i + protolength + 4 > indexTo) {
                throw new ParseException
                        ("Not a valid protocol version: " +
                                buffer.substring(indexFrom, indexTo));
            }

            // check the protocol name and slash
            if (!buffer.substring(i, i + protolength).equals(ICY_PROTOCOL_NAME)) {
                return super.parseProtocolVersion(buffer, cursor);
            }

            cursor.updatePos(i + protolength);

            return createProtocolVersion(1, 0);
        }

        @Override
        public StatusLine parseStatusLine(CharArrayBuffer buffer,
                                          ParserCursor cursor) throws ParseException {
            return super.parseStatusLine(buffer, cursor);
        }
    }

    class MyClientConnection extends DefaultClientConnection {
        @Override
        protected HttpMessageParser createResponseParser(
                final SessionInputBuffer buffer,
                final HttpResponseFactory responseFactory, final HttpParams params) {
            return new DefaultResponseParser(buffer, new IcyLineParser(),
                    responseFactory, params);
        }
    }

    class MyClientConnectionOperator extends DefaultClientConnectionOperator {
        public MyClientConnectionOperator(final SchemeRegistry sr) {
            super(sr);
        }

        @Override
        public OperatedClientConnection createConnection() {
            return new MyClientConnection();
        }
    }

    class MyClientConnManager extends SingleClientConnManager {
        private MyClientConnManager(HttpParams params, SchemeRegistry schreg) {
            super(params, schreg);
        }

        @Override
        protected ClientConnectionOperator createConnectionOperator(
                final SchemeRegistry sr) {
            return new MyClientConnectionOperator(sr);
        }
    }

    //From: http://blog.dev001.net/post/67082904181/android-using-sni-and-tlsv1-2-with-apache
    public static class TlsSniSocketFactory implements LayeredSocketFactory {
        private static final String TAG = "SNISocketFactory";

        final static HostnameVerifier hostnameVerifier = new StrictHostnameVerifier();


        // Plain TCP/IP (layer below TLS)

        @Override
        public Socket connectSocket(Socket s, String host, int port, InetAddress localAddress, int localPort, HttpParams params) throws IOException {
            return null;
        }

        @Override
        public Socket createSocket() throws IOException {
            return null;
        }

        @Override
        public boolean isSecure(Socket s) throws IllegalArgumentException {
            if (s instanceof SSLSocket)
                return ((SSLSocket) s).isConnected();
            return false;
        }


        // TLS layer

        @Override
        public Socket createSocket(Socket plainSocket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
            if (autoClose) {
                // we don't need the plainSocket
                plainSocket.close();
            }

            // create and connect SSL socket, but don't do hostname/certificate verification yet
            SSLCertificateSocketFactory sslSocketFactory = (SSLCertificateSocketFactory) SSLCertificateSocketFactory.getDefault(0);
            SSLSocket ssl = (SSLSocket) sslSocketFactory.createSocket(InetAddress.getByName(host), port);

            // enable TLSv1.1/1.2 if available
            // (see https://github.com/rfc2822/davdroid/issues/229)
            ssl.setEnabledProtocols(ssl.getSupportedProtocols());

            // set up SNI before the handshake
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                sslSocketFactory.setHostname(ssl, host);
            } else {
                try {
                    java.lang.reflect.Method setHostnameMethod = ssl.getClass().getMethod("setHostname", String.class);
                    setHostnameMethod.invoke(ssl, host);
                } catch (Exception e) {
                    Log.w(TAG, "SNI not useable", e);
                }
            }

            // verify hostname and certificate
            SSLSession session = ssl.getSession();
            if (!hostnameVerifier.verify(host, session))
                throw new SSLPeerUnverifiedException("Cannot verify hostname: " + host);

            return ssl;
        }
    }
}