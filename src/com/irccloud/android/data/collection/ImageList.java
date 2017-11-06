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
import android.graphics.Matrix;
import android.os.Build;
import android.support.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.damnhandy.uri.template.UriTemplate;
import com.datatheorem.android.trustkit.TrustKit;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.BuildConfig;
import com.irccloud.android.ColorFormatter;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.data.model.ImageURLInfo;

import org.json.JSONObject;

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
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import pl.droidsonroids.gif.GifDrawable;

import static com.irccloud.android.NetworkConnection.printStackTraceToCrashlytics;

public class ImageList {
    private static java.security.MessageDigest md;
    private static ImageList instance = null;
    private HashMap<String, Bitmap> images;
    private HashMap<String, GifDrawable> GIFs;
    private ArrayList<String> failedURLs;
    private HashMap<String, ImageURLInfo> urlInfo;
    private HashMap<String, ArrayList<OnImageFetchedListener>> downloadListeners;
    private final BlockingQueue<Runnable> mWorkQueue = new LinkedBlockingQueue<>();
    private static final int KEEP_ALIVE_TIME = 10;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    private final ThreadPoolExecutor mDownloadThreadPool = new ThreadPoolExecutor(
            2,       // Initial pool size
            4,       // Max pool size
            KEEP_ALIVE_TIME,
            KEEP_ALIVE_TIME_UNIT,
            mWorkQueue);

    public static boolean isImageURL(String url) {
        try {
            Uri uri = Uri.parse(url);
            if (uri != null && uri.getLastPathSegment() != null && uri.getLastPathSegment().contains(".")) {
                String extension = uri.getLastPathSegment().substring(uri.getLastPathSegment().indexOf(".") + 1).toLowerCase();
                if (extension.equals("jpg") || extension.equals("jpeg") || extension.equals("png") || extension.equals("gif") || extension.equals("bmp") || extension.equals("webp"))
                    return true;
            }
        } catch (Exception e) {
        }
        return url != null && url.matches(
            "(^https?://(www\\.)?flickr\\.com/photos/.*$)|" +
                "(^https?://(www\\.)?instagram\\.com/p/.*$)|(^https?://(www\\.)?instagr\\.am/p/.*$)|" +
                "(^https?://(www\\.)?imgur\\.com/.*$)|(^https?://m\\.imgur\\.com/.*$)|" +
                "(^https?://d\\.pr/i/.*)|(^https?://droplr\\.com/i/.*)|" +
                "(^https?://cl\\.ly/.*)|" +
                "(^https?://(www\\.)?leetfiles\\.com/image/.*)|" +
                "(^https?://(www\\.)?leetfil\\.es/image/.*)|" +
                "(^https?://i.imgur.com/.*\\.gifv$)|" +
                "(^https?://(www\\.)?giphy\\.com/gifs/.*)|" +
                "(^https?://gph\\.is/.*)|" +
                "(^https?://.*\\.twimg\\.com/media/.*\\.(png|jpe?g|gif|bmp):[a-z]+$)|" +
                "(^https?://(www\\.)?xkcd\\.com/[0-9]+/?)|" +
                "(^https?://.*\\.steampowered\\.com/ugc/.*)"
        ) && !url.matches("(^https?://cl\\.ly/robots\\.txt$)|(^https?://cl\\.ly/image/?$)") && !(url.contains("imgur.com") && url.contains(","));
    }

    public synchronized static ImageList getInstance() {
        if (instance == null)
            instance = new ImageList();
        return instance;
    }

    public ImageList() {
        images = new HashMap<>();
        GIFs = new HashMap<>();
        failedURLs = new ArrayList<>();
        urlInfo = new HashMap<>();
        downloadListeners = new HashMap<>();
    }

    public void clear() {
        images.clear();
        GIFs.clear();
        mDownloadThreadPool.purge();
    }

    public void clearFailures() {
        failedURLs.clear();
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

    public boolean isFailedURL(String fileID, int width) {
        try {
            if(ColorFormatter.file_uri_template != null)
                return isFailedURL(new URL(UriTemplate.fromTemplate(ColorFormatter.file_uri_template).set("id", fileID).expand()));
        } catch (Exception e) {
        }
        return false;
    }

    public boolean isFailedURL(String url) {
        try {
            return isFailedURL(new URL(url));
        } catch (Exception e) {
        }
        return false;
    }

    public boolean isFailedURL(URL url) {
        return failedURLs.contains(MD5(url.toString()));
    }

    public GifDrawable getGIF(URL url) throws OutOfMemoryError, FileNotFoundException {
        if(failedURLs.contains(MD5(url.toString())))
            throw new FileNotFoundException();

        GifDrawable gif = GIFs.get(MD5(url.toString()));
        if(gif != null)
            return gif;

        if(cacheFile(url).exists()) {
            try {
                gif = new GifDrawable(cacheFile(url).getAbsolutePath());
                GIFs.put(MD5(url.toString()), gif);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return gif;
    }

    public GifDrawable getGIF(String fileID) throws OutOfMemoryError, FileNotFoundException {
        try {
            if(ColorFormatter.file_uri_template != null)
                return getGIF(new URL(UriTemplate.fromTemplate(ColorFormatter.file_uri_template).set("id", fileID).expand()));
        } catch (Exception e) {
        }
        return null;
    }

    public GifDrawable getGIF(String fileID, int width) throws OutOfMemoryError, FileNotFoundException {
        try {
            if(ColorFormatter.file_uri_template != null)
                return getGIF(new URL(UriTemplate.fromTemplate(ColorFormatter.file_uri_template).set("id", fileID).set("modifiers", "w" + width).expand()));
        } catch (Exception e) {
        }
        return null;
    }


    public Bitmap getImage(URL url) throws OutOfMemoryError, FileNotFoundException, IOException {
        return getImage(url, 0);
    }

    public Bitmap getImage(URL url, int width) throws OutOfMemoryError, FileNotFoundException, IOException {
        if(failedURLs.contains(MD5(url.toString())))
            throw new FileNotFoundException();

        Bitmap bitmap = images.get(MD5(url.toString()));
        if(bitmap != null)
            return bitmap;

        if(cacheFile(url).exists()) {
            if(width > 0)
                bitmap = loadScaledBitmap(cacheFile(url), width);
            else
                bitmap = BitmapFactory.decodeFile(cacheFile(url).getAbsolutePath());

            ExifInterface exif = new ExifInterface(cacheFile(url).getPath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            if(orientation > 1) {
                Matrix matrix = new Matrix();
                switch (orientation) {
                    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                        matrix.setScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.setRotate(180);
                        break;
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                        matrix.setRotate(180);
                        matrix.postScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_TRANSPOSE:
                        matrix.setRotate(90);
                        matrix.postScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.setRotate(90);
                        break;
                    case ExifInterface.ORIENTATION_TRANSVERSE:
                        matrix.setRotate(-90);
                        matrix.postScale(-1, 1);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.setRotate(-90);
                        break;
                }
                try {
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                } catch (OutOfMemoryError e) {
                    Log.e("IRCCloud", "Out of memory rotating the photo");
                }
            }

            if (bitmap != null)
                images.put(MD5(url.toString()), bitmap);
        }
        return bitmap;
    }

    public Bitmap getImage(String fileID) throws OutOfMemoryError, FileNotFoundException {
        try {
            if(ColorFormatter.file_uri_template != null)
                return getImage(new URL(UriTemplate.fromTemplate(ColorFormatter.file_uri_template).set("id", fileID).expand()));
        } catch (Exception e) {
        }
        return null;
    }

    public Bitmap getImage(String fileID, int width) throws OutOfMemoryError, FileNotFoundException {
        try {
            if(ColorFormatter.file_uri_template != null)
                return getImage(new URL(UriTemplate.fromTemplate(ColorFormatter.file_uri_template).set("id", fileID).set("modifiers", "w" + width).expand()));
        } catch (Exception e) {
        }
        return null;
    }

    private void notifyListeners(URL url, Bitmap result) {
        synchronized (downloadListeners) {
            if (downloadListeners.containsKey(url.toString())) {
                for (OnImageFetchedListener listener : downloadListeners.get(url.toString()))
                    listener.onImageFetched(result);
                downloadListeners.remove(url.toString());
            }
        }
    }

    public static Bitmap loadScaledBitmap(File path, int width) throws FileNotFoundException, IOException, OutOfMemoryError {
        FileInputStream is = new FileInputStream(path);
        BitmapFactory.Options dbo = new BitmapFactory.Options();
        dbo.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, dbo);
        is.close();

        Bitmap b;
        is = new FileInputStream(path);
        if (dbo.outWidth > width) {
            float ratio = ((float) dbo.outWidth) / ((float) width);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = (int) ratio;
            b = BitmapFactory.decodeStream(is, null, options);
        } else {
            b = BitmapFactory.decodeStream(is);
        }
        is.close();

        return b;
    }

    public void fetchImage(URL url, OnImageFetchedListener listener) throws FileNotFoundException {
        fetchImage(url, 0, listener);
    }

    public void fetchImage(final URL url, final int width, final OnImageFetchedListener listener) throws FileNotFoundException {
        if(failedURLs.contains(MD5(url.toString())))
            throw new FileNotFoundException();

        synchronized (downloadListeners) {
            if (downloadListeners.containsKey(url.toString())) {
                if(listener != null)
                    downloadListeners.get(url.toString()).add(listener);
                return;
            } else {
                ArrayList<OnImageFetchedListener> list = new ArrayList<>();
                if(listener != null)
                    list.add(listener);
                downloadListeners.put(url.toString(), list);
            }
        }

        mDownloadThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (cacheFile(url).exists() && cacheFile(url).length() > 0) {
                        try {
                            Bitmap bitmap = getImage(url, width);
                            if (bitmap != null)
                                images.put(MD5(url.toString()), bitmap);
                            else
                                failedURLs.add(MD5(url.toString()));
                            notifyListeners(url, bitmap);
                        } catch (OutOfMemoryError e) {
                            notifyListeners(url, null);
                        }
                    } else {
                        HttpURLConnection conn;

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
                            Crashlytics.log(Log.DEBUG, "IRCCloud", "Requesting: " + url + " via proxy: " + host);
                        } else {
                            Crashlytics.log(Log.DEBUG, "IRCCloud", "Requesting: " + url);
                        }

                        try {
                            if (url.getProtocol().toLowerCase().equals("https")) {
                                HttpsURLConnection https = (HttpsURLConnection) ((proxy != null) ? url.openConnection(proxy) : url.openConnection(Proxy.NO_PROXY));
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1)
                                    https.setSSLSocketFactory(NetworkConnection.getInstance().IRCCloudSocketFactory);
                                else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                                    https.setSSLSocketFactory(TrustKit.getInstance().getSSLSocketFactory(url.getHost()));
                                conn = https;
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

                                Bitmap bitmap = getImage(url, width);
                                if(bitmap == null) {
                                    android.util.Log.e("IRCCloud", "Failed to load bitmap after download");
                                    failedURLs.add(MD5(url.toString()));
                                }
                                notifyListeners(url, bitmap);
                            } else {
                                android.util.Log.e("IRCCloud", "No input stream");
                                failedURLs.add(MD5(url.toString()));
                                notifyListeners(url, null);
                            }
                        } catch (OutOfMemoryError e) {
                            e.printStackTrace();
                            failedURLs.add(MD5(url.toString()));
                            notifyListeners(url, null);
                        } catch (FileNotFoundException e) {
                            failedURLs.add(MD5(url.toString()));
                            e.printStackTrace();
                            notifyListeners(url, null);
                        } catch (IOException e) {
                            e.printStackTrace();
                            failedURLs.add(MD5(url.toString()));
                            printStackTraceToCrashlytics(e);
                            notifyListeners(url, null);
                        }

                        conn.disconnect();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void fetchImage(String fileID, final OnImageFetchedListener listener) {
        try {
            if(ColorFormatter.file_uri_template != null)
                fetchImage(new URL(UriTemplate.fromTemplate(ColorFormatter.file_uri_template).set("id", fileID).expand()), listener);
        } catch (Exception e) {
            if (listener != null)
                listener.onImageFetched(null);
        }
    }

    public void fetchImage(String fileID, int width, final OnImageFetchedListener listener) {
        try {
            if(ColorFormatter.file_uri_template != null)
                fetchImage(new URL(UriTemplate.fromTemplate(ColorFormatter.file_uri_template).set("id", fileID).set("modifiers", "w" + width).expand()), listener);
        } catch (Exception e) {
            if (listener != null)
                listener.onImageFetched(null);
        }
    }

    public File cacheFile(URL url) {
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

    private void putImageInfo(ImageURLInfo info) {
        urlInfo.put(info.original_url, info);
    }

    public void fetchImageInfo(String URL, OnImageInfoListener listener) {
        if(urlInfo.containsKey(URL)) {
            listener.onImageInfo(urlInfo.get(URL));
        } else {
            String url = URL;
            String lower = url.toLowerCase().replace("https://", "").replace("http://", "");
            if (lower.startsWith("www.dropbox.com/")) {
                if (lower.startsWith("www.dropbox.com/s/")) {
                    url = url.replace("://www.dropbox.com/s/", "://dl.dropboxusercontent.com/s/");
                } else {
                    url = url + "?dl=1";
                }
            } else if ((lower.startsWith("d.pr/i/") || lower.startsWith("droplr.com/i/")) && !lower.endsWith("+")) {
                url += "+";
            } else if (lower.startsWith("imgur.com/") || lower.startsWith("www.imgur.com/") || lower.startsWith("m.imgur.com/")) {
                String id = url.replace("https://", "").replace("http://", "");
                id = id.substring(id.indexOf("/") + 1);

                if (!id.contains("/") && id.length() > 0) {
                    new ImgurTask("image", URL, listener).execute(id);
                } else if (id.startsWith("gallery/") && id.length() > 8) {
                    new ImgurTask("gallery", URL, listener).execute(id.substring(8));
                } else if (id.startsWith("a/") && id.length() > 2) {
                    new ImgurTask("album", URL, listener).execute(id.substring(2));
                } else {
                    listener.onImageInfo(null);
                }
                return;
            } else if (lower.startsWith("i.imgur.com") && (lower.endsWith(".gifv") || lower.endsWith(".gif"))) {
                String id = url.replace("https://", "").replace("http://", "");
                id = id.substring(id.indexOf("/") + 1);
                id = id.substring(0, id.lastIndexOf("."));
                new ImgurTask("image", URL, listener).execute(id);
                return;
            } else if (lower.startsWith("giphy.com/") || lower.startsWith("www.giphy.com/") || lower.startsWith("gph.is/")) {
                if (lower.contains("/gifs/") && lower.lastIndexOf("/") > lower.indexOf("/gifs/") + 6)
                    url = url.substring(0, lower.lastIndexOf("/"));
                new OEmbedTask(URL, listener).execute("https://giphy.com/services/oembed/?url=" + url);
                return;
            } else if (lower.startsWith("flickr.com/") || lower.startsWith("www.flickr.com/")) {
                new OEmbedTask(URL, listener).execute("https://www.flickr.com/services/oembed/?format=json&url=" + url);
                return;
            } else if (lower.startsWith("instagram.com/") || lower.startsWith("www.instagram.com/") || lower.startsWith("instagr.am/") || lower.startsWith("www.instagr.am/")) {
                new OEmbedTask(URL, listener).execute("https://api.instagram.com/oembed?url=" + url);
                return;
            } else if (lower.startsWith("cl.ly")) {
                new ClLyTask(URL, listener).execute(url);
                return;
            } else if (url.matches(".*/wiki/.*/File:.*")) {
                new WikiTask(URL, listener).execute(url.replaceAll("/wiki/.*/File:", "/w/api.php?action=query&format=json&prop=imageinfo&iiprop=url&titles=File:"));
            } else if (lower.startsWith("leetfiles.com/") || lower.startsWith("www.leetfiles.com/")) {
                url = url.replace("www.", "").replace("leetfiles.com/image/", "i.leetfiles.com/").replace("?id=", "");
            } else if (lower.startsWith("leetfil.es/") || lower.startsWith("www.leetfil.es/")) {
                url = url.replace("www.", "").replace("leetfil.es/image/", "i.leetfiles.com/").replace("?id=", "");
            } else if (lower.startsWith("xkcd.com/") || lower.startsWith("www.xkcd.com/")) {
                new XKCDTask(URL, listener).execute(URL);
                return;
            }

            ImageURLInfo info = new ImageURLInfo();
            info.thumbnail = url;
            info.original_url = URL;
            putImageInfo(info);
            listener.onImageInfo(info);
        }
    }

    public static abstract class OnImageFetchedListener {
        public abstract void onImageFetched(Bitmap image);
    }

    public static abstract class OnImageInfoListener {
        public abstract void onImageInfo(ImageURLInfo info);
    }

    private class OEmbedTask extends AsyncTaskEx<String, Void, String> {
        private String provider = null;
        private String giphy_fallback = null;
        private String title = null;
        private String original_url = null;
        private OnImageInfoListener listener = null;

        public OEmbedTask(String original_url, OnImageInfoListener listener) {
            this.listener = listener;
            this.original_url = original_url;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                JSONObject o = NetworkConnection.getInstance().fetchJSON(params[0]);
                if (o.has("title"))
                    title = o.getString("title");

                if (o.has("provider_name"))
                    provider = o.getString("provider_name");

                if (provider != null && provider.equals("Giphy") && o.has("image") && o.getString("image").endsWith(".gif"))
                    giphy_fallback = o.getString("image");

                if (provider != null && provider.equals("Instagram"))
                    return o.getString("thumbnail_url");

                if ((provider != null && provider.equals("Giphy")) || o.getString("type").equalsIgnoreCase("photo"))
                    return o.getString("url");
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String url) {
            if (url != null) {
                if (provider != null && provider.equals("Giphy")) {
                    new GiphyTask(original_url, listener, giphy_fallback).execute(url.substring(url.indexOf("/gifs/") + 6));
                } else {
                    ImageURLInfo info = new ImageURLInfo();
                    info.thumbnail = url;
                    info.description = title;
                    info.original_url = original_url;
                    putImageInfo(info);
                    listener.onImageInfo(info);
                }
            } else {
                listener.onImageInfo(null);
            }
        }
    }

    public class ImgurTask extends AsyncTaskEx<String, Void, JSONObject> {
        private String type = "gallery";
        private final String REST_URL = (BuildConfig.MASHAPE_KEY.length() > 0) ? "https://imgur-apiv3.p.mashape.com/3/" : "https://api.imgur.com/3/";
        private String original_url = null;
        private String title = null;
        private OnImageInfoListener listener = null;

        public ImgurTask(String type, String original_url, OnImageInfoListener listener) {
            this.type = type;
            this.listener = listener;
            this.original_url = original_url;
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                HashMap<String, String> headers = new HashMap<>();
                if (BuildConfig.MASHAPE_KEY.length() > 0)
                    headers.put("X-Mashape-Authorization", BuildConfig.MASHAPE_KEY);
                headers.put("Authorization", "Client-ID " + BuildConfig.IMGUR_KEY);
                JSONObject o = NetworkConnection.getInstance().fetchJSON(REST_URL + type + "/" + params[0], headers);
                if (o.getBoolean("success")) {
                    JSONObject data = o.getJSONObject("data");
                    if(data.has("title") && !data.isNull("title") && data.getString("title").length() > 0)
                        title = data.getString("title");
                    if((data.has("images_count") && data.getInt("images_count") == 1) || !data.has("is_album") || !data.getBoolean("is_album")) {
                        if(data.has("is_album") && data.getBoolean("is_album"))
                            data = data.getJSONArray("images").getJSONObject(0);
                        if(data.has("title") && !data.isNull("title") && data.getString("title").length() > 0)
                            title = data.getString("title");
                        return data;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject data) {
            if (data != null) {
                try {
                    ImageURLInfo info = new ImageURLInfo();
                    info.original_url = original_url;
                    info.title = title;
                    if(data.getBoolean("animated") && data.has("mp4")) {
                        info.thumbnail = data.getString("mp4").replace(".mp4", ".gif");
                        info.mp4 = data.getString("mp4");
                    } else {
                        info.thumbnail = data.getString("link");
                    }
                    if(data.has("description") && !data.isNull("description") && data.getString("description").length() > 0)
                        info.description = data.getString("description");
                    putImageInfo(info);
                    listener.onImageInfo(info);
                } catch (Exception e) {
                    listener.onImageInfo(null);
                }
            } else {
                listener.onImageInfo(null);
            }
        }
    }

    public class GiphyTask extends AsyncTaskEx<String, Void, JSONObject> {
        private String original_url = null;
        private OnImageInfoListener listener = null;
        private String fallback = null;

        public GiphyTask(String original_url, OnImageInfoListener listener, String fallback) {
            this.listener = listener;
            this.original_url = original_url;
            this.fallback = fallback;
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                HashMap<String, String> headers = new HashMap<>();
                JSONObject o = NetworkConnection.getInstance().fetchJSON("https://api.giphy.com/v1/gifs/" + params[0] + "?api_key=dc6zaTOxFJmzC", headers);
                if (o.has("data") && o.getJSONObject("data").has("images")) {
                    return o.getJSONObject("data").getJSONObject("images").getJSONObject("original");
                }
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject data) {
            if (data != null) {
                try {
                    ImageURLInfo info = new ImageURLInfo();
                    info.original_url = original_url;
                    info.thumbnail = fallback;
                    if (data.has("mp4") && data.getString("mp4").length() > 0)
                        info.mp4 = data.getString("mp4");
                    if (data.getString("url").endsWith(".gif"))
                        info.thumbnail = data.getString("url");
                    putImageInfo(info);
                    listener.onImageInfo(info);
                } catch (Exception e) {
                    listener.onImageInfo(null);
                }
            } else {
                listener.onImageInfo(null);
            }
        }
    }

    private class ClLyTask extends AsyncTaskEx<String, Void, String> {
        private String original_url = null;
        private OnImageInfoListener listener = null;

        public ClLyTask(String original_url, OnImageInfoListener listener) {
            this.listener = listener;
            this.original_url = original_url;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                JSONObject o = NetworkConnection.getInstance().fetchJSON(params[0]);
                if (o.getString("item_type").equalsIgnoreCase("image"))
                    return o.getString("content_url");
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String url) {
            if (url != null) {
                ImageURLInfo info = new ImageURLInfo();
                info.original_url = original_url;
                info.thumbnail = url;
                putImageInfo(info);
                listener.onImageInfo(info);
            } else {
                listener.onImageInfo(null);
            }
        }
    }

    private class WikiTask extends AsyncTaskEx<String, Void, String> {
        private String original_url = null;
        private OnImageInfoListener listener = null;

        public WikiTask(String original_url, OnImageInfoListener listener) {
            this.listener = listener;
            this.original_url = original_url;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                JSONObject o = NetworkConnection.getInstance().fetchJSON(params[0]);
                JSONObject pages = o.getJSONObject("query").getJSONObject("pages");
                Iterator<String> i = pages.keys();
                String pageid = i.next();
                return pages.getJSONObject(pageid).getJSONArray("imageinfo").getJSONObject(0).getString("url");
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String url) {
            if (url != null) {
                ImageURLInfo info = new ImageURLInfo();
                info.original_url = original_url;
                info.thumbnail = url;
                putImageInfo(info);
                listener.onImageInfo(info);
            } else {
                listener.onImageInfo(null);
            }
        }
    }

    public class XKCDTask extends AsyncTaskEx<String, Void, JSONObject> {
        private String original_url = null;
        private OnImageInfoListener listener = null;

        public XKCDTask(String original_url, OnImageInfoListener listener) {
            this.listener = listener;
            this.original_url = original_url;
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                return NetworkConnection.getInstance().fetchJSON( params[0] + "/info.0.json");
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject data) {
            if (data != null) {
                try {
                    ImageURLInfo info = new ImageURLInfo();
                    info.original_url = original_url;
                    info.thumbnail = data.getString("img");
                    info.title = data.getString("safe_title");
                    info.description = data.getString("alt");
                    putImageInfo(info);
                    listener.onImageInfo(info);
                } catch (Exception e) {
                    listener.onImageInfo(null);
                }
            } else {
                listener.onImageInfo(null);
            }
        }
    }
}