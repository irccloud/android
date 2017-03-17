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

package com.irccloud.android.data.collection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.damnhandy.uri.template.UriTemplate;
import com.irccloud.android.ColorFormatter;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.NetworkConnection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import static com.irccloud.android.NetworkConnection.printStackTraceToCrashlytics;

public class ImageList {
    private static java.security.MessageDigest md;
    private static ImageList instance = null;
    private HashMap<String, Bitmap> images;
    private final BlockingQueue<Runnable> mWorkQueue = new LinkedBlockingQueue<>();
    private static final int KEEP_ALIVE_TIME = 10;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    private final ThreadPoolExecutor mDownloadThreadPool = new ThreadPoolExecutor(
            4,       // Initial pool size
            8,       // Max pool size
            KEEP_ALIVE_TIME,
            KEEP_ALIVE_TIME_UNIT,
            mWorkQueue);

    public synchronized static ImageList getInstance() {
        if (instance == null)
            instance = new ImageList();
        return instance;
    }

    public ImageList() {
        images = new HashMap<>();
    }

    public void clear() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            for (Bitmap b : images.values()) {
                if (!b.isRecycled())
                    b.recycle();
            }
        }
        images.clear();
        mDownloadThreadPool.purge();
    }

    public void purge() {
        Context context = IRCCloudApplication.getInstance().getApplicationContext();
        if(context != null) {
            File[] files = new File(context.getCacheDir(), "ImageCache").listFiles();
            if(files != null && files.length > 0) {
                for (File file : files) {
                    try {
                        file.delete();
                    } catch (Exception e) {
                        printStackTraceToCrashlytics(e);
                    }
                }
            }
        }
        clear();
    }

    public void prune() {
        long olde = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
        Context context = IRCCloudApplication.getInstance().getApplicationContext();
        if(context != null) {
            File[] files = new File(context.getCacheDir(), "ImageCache").listFiles();
            if(files != null && files.length > 0) {
                for (File file : files) {
                    if (file.lastModified() < olde) {
                        try {
                            file.delete();
                        } catch (Exception e) {
                            printStackTraceToCrashlytics(e);
                        }
                    }
                }
            }
        }
    }

    public Bitmap getImage(URL url) throws OutOfMemoryError {
        Bitmap bitmap = images.get(MD5(url.toString()));
        if(bitmap != null)
            return bitmap;

        if(cacheFile(url).exists()) {
            bitmap = BitmapFactory.decodeFile(cacheFile(url).getAbsolutePath());
            if (bitmap != null)
                images.put(MD5(url.toString()), bitmap);
        }
        return bitmap;
    }

    public Bitmap getImage(String fileID) throws OutOfMemoryError {
        try {
            return getImage(new URL(UriTemplate.fromTemplate(ColorFormatter.file_uri_template).set("id", fileID).expand()));
        } catch (MalformedURLException e) {
        }
        return null;
    }

    public Bitmap getImage(String fileID, int width) throws OutOfMemoryError {
        try {
            return getImage(new URL(UriTemplate.fromTemplate(ColorFormatter.file_uri_template).set("id", fileID).set("modifiers", "w" + width).expand()));
        } catch (MalformedURLException e) {
        }
        return null;
    }

    public void fetchImage(final URL url, final OnImageFetchedListener listener) {
        mDownloadThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn;

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
                    Crashlytics.log(Log.DEBUG, "IRCCloud", "Requesting: " + url + " via proxy: " + host);
                } else {
                    Crashlytics.log(Log.DEBUG, "IRCCloud", "Requesting: " + url);
                }

                try {
                    if (url.getProtocol().toLowerCase().equals("https")) {
                        conn = (HttpsURLConnection) ((proxy != null) ? url.openConnection(proxy) : url.openConnection(Proxy.NO_PROXY));
                    } else {
                        conn = (HttpURLConnection) ((proxy != null) ? url.openConnection(proxy) : url.openConnection(Proxy.NO_PROXY));
                    }
                } catch (IOException e) {
                    printStackTraceToCrashlytics(e);
                    return;
                }

                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.setUseCaches(true);
                conn.setRequestProperty("User-Agent", NetworkConnection.getInstance().useragent);

                try {
                    ConnectivityManager cm = (ConnectivityManager) IRCCloudApplication.getInstance().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo ni = cm.getActiveNetworkInfo();
                    if (ni != null && ni.getType() == ConnectivityManager.TYPE_WIFI) {
                        Crashlytics.log(Log.DEBUG, "IRCCloud", "Loading via WiFi");
                    } else {
                        Crashlytics.log(Log.DEBUG, "IRCCloud", "Loading via mobile");
                    }
                } catch (Exception e) {
                    printStackTraceToCrashlytics(e);
                }

                try {
                    if (conn.getInputStream() != null) {
                        InputStream is = conn.getInputStream();
                        OutputStream os = new FileOutputStream(cacheFile(url));
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = is.read(buffer)) != -1) {
                            os.write(buffer, 0, len);
                        }
                        is.close();
                        os.close();

                        Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(cacheFile(url)));
                        if (bitmap != null)
                            images.put(MD5(url.toString()), bitmap);
                        if (listener != null)
                            listener.onImageFetched(bitmap);
                    }
                } catch (FileNotFoundException e) {
                    if (listener != null)
                        listener.onImageFetched(null);
                } catch (IOException e) {
                    printStackTraceToCrashlytics(e);
                    if (listener != null)
                        listener.onImageFetched(null);
                }

                conn.disconnect();
            }
        });
    }

    public void fetchImage(String fileID, final OnImageFetchedListener listener) {
        try {
            fetchImage(new URL(UriTemplate.fromTemplate(ColorFormatter.file_uri_template).set("id", fileID).expand()), listener);
        } catch (MalformedURLException e) {
            if (listener != null)
                listener.onImageFetched(null);
        }
    }

    public void fetchImage(String fileID, int width, final OnImageFetchedListener listener) {
        try {
            fetchImage(new URL(UriTemplate.fromTemplate(ColorFormatter.file_uri_template).set("id", fileID).set("modifiers", "w" + width).expand()), listener);
        } catch (MalformedURLException e) {
            if (listener != null)
                listener.onImageFetched(null);
        }
    }

    private File cacheFile(URL url) {
        new File(IRCCloudApplication.getInstance().getApplicationContext().getCacheDir(), "ImageCache").mkdirs();
        return new File(new File(IRCCloudApplication.getInstance().getApplicationContext().getCacheDir(), "ImageCache"), MD5(url.toString()));
    }

    private String MD5(String md5) {
        try {
            if(md == null)
                md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes(Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            for (byte anArray : array) {
                sb.append(Integer.toHexString((anArray & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

    public static abstract class OnImageFetchedListener {
        public abstract void onImageFetched(Bitmap image);
    }
}
