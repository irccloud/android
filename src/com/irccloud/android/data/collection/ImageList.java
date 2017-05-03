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
import com.irccloud.android.HTTPFetcher;
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
import java.util.ArrayList;
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
    private HashMap<String, ImageFetcher> imageFetchers;

    public synchronized static ImageList getInstance() {
        if (instance == null)
            instance = new ImageList();
        return instance;
    }

    public ImageList() {
        images = new HashMap<>();
        imageFetchers = new HashMap<>();
    }

    public void clear() {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            for (Bitmap b : images.values()) {
                if (!b.isRecycled())
                    b.recycle();
            }
        }
        images.clear();
        synchronized (imageFetchers) {
            for(ImageFetcher f : imageFetchers.values()) {
                f.cancel();
            }
            imageFetchers.clear();
        }
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
            if(ColorFormatter.file_uri_template != null)
                return getImage(new URL(UriTemplate.fromTemplate(ColorFormatter.file_uri_template).set("id", fileID).expand()));
        } catch (MalformedURLException e) {
        }
        return null;
    }

    public Bitmap getImage(String fileID, int width) throws OutOfMemoryError {
        try {
            if(ColorFormatter.file_uri_template != null)
                return getImage(new URL(UriTemplate.fromTemplate(ColorFormatter.file_uri_template).set("id", fileID).set("modifiers", "w" + width).expand()));
        } catch (MalformedURLException e) {
        }
        return null;
    }

    public void fetchImage(final URL url, final OnImageFetchedListener listener) {
        if(!imageFetchers.containsKey(url.toString())) {
            ImageFetcher f = new ImageFetcher(url, listener);
            imageFetchers.put(url.toString(), f);
            f.connect();
        }
    }

    public void fetchImage(String fileID, final OnImageFetchedListener listener) {
        try {
            fetchImage(new URL(UriTemplate.fromTemplate(ColorFormatter.file_uri_template).set("id", fileID).expand()), listener);
        } catch (Exception e) {
            if (listener != null)
                listener.onImageFetched(null);
        }
    }

    public void fetchImage(String fileID, int width, final OnImageFetchedListener listener) {
        try {
            fetchImage(new URL(UriTemplate.fromTemplate(ColorFormatter.file_uri_template).set("id", fileID).set("modifiers", "w" + width).expand()), listener);
        } catch (Exception e) {
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

    public class ImageFetcher extends HTTPFetcher {
        OnImageFetchedListener listener;

        ImageFetcher(URL url, OnImageFetchedListener listener) {
            super(url);
            this.listener = listener;
        }

        @Override
        protected void onStreamConnected(InputStream is) throws Exception {
            if(isCancelled)
                return;

            OutputStream os = new FileOutputStream(cacheFile(mURI));
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1 && !isCancelled) {
                os.write(buffer, 0, len);
            }
            is.close();
            os.close();

            if(isCancelled)
                return;

            Bitmap bitmap;
            try {
                bitmap = BitmapFactory.decodeStream(new FileInputStream(cacheFile(mURI)));
            } catch (OutOfMemoryError e) {
                bitmap = null;
            }

            if (bitmap != null && !isCancelled)
                images.put(MD5(mURI.toString()), bitmap);
            if (listener != null && !isCancelled)
                listener.onImageFetched(bitmap);
        }

        @Override
        protected void onFetchFailed() {
            if (listener != null)
                listener.onImageFetched(null);
            synchronized (imageFetchers) {
                imageFetchers.remove(this);
            }
        }

        @Override
        protected void onFetchComplete() {
            synchronized (imageFetchers) {
                imageFetchers.remove(this);
            }
        }
    }
}
