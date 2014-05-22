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

package com.irccloud.android.activity;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.ShareActionProviderHax;

import org.json.JSONObject;

import java.net.URL;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class ImageViewerActivity extends BaseActivity implements ShareActionProviderHax.OnShareActionProviderSubVisibilityChangedListener{

    private class OEmbedTask extends AsyncTaskEx<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                JSONObject o = NetworkConnection.getInstance().fetchJSON(params[0]);
                if(o.getString("type").equalsIgnoreCase("photo"))
                    return o.getString("url");
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String url) {
            if(url != null) {
                loadImage(url);
            } else {
                fail();
            }
        }
    }

    private class ClLyTask extends AsyncTaskEx<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                JSONObject o = NetworkConnection.getInstance().fetchJSON(params[0]);
                if(o.getString("item_type").equalsIgnoreCase("image"))
                    return o.getString("content_url");
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String url) {
            if(url != null) {
                loadImage(url);
            } else {
                fail();
            }
        }
    }

    private class WikiTask extends AsyncTaskEx<String, Void, String> {

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
            if(url != null) {
                loadImage(url);
            } else {
                fail();
            }
        }
    }

    WebView mImage;
    ProgressBar mProgress;
    Timer mHideTimer = new Timer("actionbar-hide-timer");
    TimerTask mHideTimerTask = null;

    public class JSInterface {
        @JavascriptInterface
        public void imageFailed() {
            fail();
        }

        @JavascriptInterface
        public void imageClicked() {
            ImageViewerActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(getSupportActionBar().isShowing()) {
                        if(mHideTimerTask != null)
                            mHideTimerTask.cancel();
                        getSupportActionBar().hide();
                    } else {
                        getSupportActionBar().show();
                        hide_actionbar();
                    }
                }
            });
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState == null)
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        setContentView(R.layout.activity_imageviewer);
        if(Integer.parseInt(Build.VERSION.SDK) >= 14 && Integer.parseInt(Build.VERSION.SDK) < 19)
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        getSupportActionBar().setTitle("Image Viewer");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_translucent));

        mImage = (WebView)findViewById(R.id.image);
        mImage.setBackgroundColor(0);
        mImage.addJavascriptInterface(new JSInterface(), "Android");
        mImage.getSettings().setBuiltInZoomControls(true);
        if(Integer.parseInt(Build.VERSION.SDK) >= 19)
            mImage.getSettings().setDisplayZoomControls(false);
        mImage.getSettings().setJavaScriptEnabled(true);
        mImage.getSettings().setLoadWithOverviewMode(true);
        mImage.getSettings().setUseWideViewPort(true);
        mImage.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mProgress.setVisibility(View.GONE);
                mImage.setVisibility(View.VISIBLE);
                hide_actionbar();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                fail();
            }
        }
        );
        mProgress = (ProgressBar)findViewById(R.id.progress);

        if(getIntent() != null && getIntent().getDataString() != null) {
            String url = getIntent().getDataString().replace(getResources().getString(R.string.IMAGE_SCHEME), "http");
            String lower = url.toLowerCase().replace("https://", "").replace("http://", "");
            if(lower.startsWith("www.dropbox.com/")) {
                if(lower.startsWith("www.dropbox.com/s/")) {
                    url = url.replace("://www.dropbox.com/s/", "://dl.dropboxusercontent.com/s/");
                } else {
                    url = url + "?dl=1";
                }
            } else if((lower.startsWith("d.pr/i/") || lower.startsWith("droplr.com/i/")) && !lower.endsWith("+")) {
                url += "+";
            } else if(lower.startsWith("imgur.com/")) {
                new OEmbedTask().execute("http://api.imgur.com/oembed.json?url=" + url);
                return;
            } else if(lower.startsWith("flickr.com/") || lower.startsWith("www.flickr.com/")) {
                new OEmbedTask().execute("https://www.flickr.com/services/oembed/?format=json&url=" + url);
                return;
            } else if(lower.startsWith("instagram.com/") || lower.startsWith("www.instagram.com/") || lower.startsWith("instagr.am/")) {
                new OEmbedTask().execute("http://api.instagram.com/oembed?url=" + url);
                return;
            } else if(lower.startsWith("cl.ly")) {
                new ClLyTask().execute(url);
                return;
            } else if(url.contains("/wiki/File:")) {
                new WikiTask().execute(url.replace("/wiki/", "/w/api.php?action=query&format=json&prop=imageinfo&iiprop=url&titles="));
            }
            loadImage(url);
        } else {
            finish();
        }
    }

    private void loadImage(String urlStr) {
        try {
            URL url = new URL(urlStr);
            mImage.loadDataWithBaseURL(null,"<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<body bgcolor='#000'>\n" +
                    "<img src='" + url.toString() + "' width='100%' style='top:0; bottom:0; margin: auto; position: absolute;'  onerror='Android.imageFailed()' onclick='Android.imageClicked()'/>\n" +
                    "</body>\n" +
                    "</html>", "text/html", "UTF-8",null);
        } catch (Exception e) {
            fail();
        }
    }

    private void fail() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.IMAGE_SCHEME), "http")));
        startActivity(intent);
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mProgress.getVisibility() == View.GONE)
            hide_actionbar();
    }

    private void hide_actionbar() {
        if(mHideTimerTask != null)
            mHideTimerTask.cancel();
        mHideTimerTask = new TimerTask() {
            @Override
            public void run() {
                ImageViewerActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getSupportActionBar().hide();
                    }
                });
            }
        };
        mHideTimer.schedule(mHideTimerTask, 3000);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_imageviewer, menu);

        if(getIntent() != null && getIntent().getDataString() != null) {
            Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.IMAGE_SCHEME), "http")));
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, getIntent().getDataString().replace(getResources().getString(R.string.IMAGE_SCHEME), "http"));

            MenuItem shareItem = menu.findItem(R.id.action_share);
            ShareActionProviderHax share = (ShareActionProviderHax)MenuItemCompat.getActionProvider(shareItem);
            share.onShareActionProviderSubVisibilityChangedListener = this;
            share.setShareIntent(intent);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
            return true;
        } else if(item.getItemId() == R.id.action_browser) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.IMAGE_SCHEME), "http")));
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onShareActionProviderSubVisibilityChanged(boolean visible) {
        if(visible) {
            if(mHideTimerTask != null)
                mHideTimerTask.cancel();
        } else {
            hide_actionbar();
        }
    }
}
