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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.GingerbreadImageProxy;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.ShareActionProviderHax;

import org.json.JSONObject;

import java.net.URL;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

public class ImageViewerActivity extends BaseActivity implements ShareActionProviderHax.OnShareActionProviderSubVisibilityChangedListener {

    private class OEmbedTask extends AsyncTaskEx<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            try {
                JSONObject o = NetworkConnection.getInstance().fetchJSON(params[0]);
                if (o.getString("type").equalsIgnoreCase("photo"))
                    return o.getString("url");
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String url) {
            if (url != null) {
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
                if (o.getString("item_type").equalsIgnoreCase("image"))
                    return o.getString("content_url");
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String url) {
            if (url != null) {
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
            if (url != null) {
                loadImage(url);
            } else {
                fail();
            }
        }
    }

    WebView mImage;
    ProgressBar mSpinner;
    ProgressBar mProgress;
    Toolbar toolbar;
    private static Timer mHideTimer;
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
                    if (toolbar.getVisibility() == View.VISIBLE) {
                        if (mHideTimerTask != null)
                            mHideTimerTask.cancel();
                        if (Build.VERSION.SDK_INT > 16) {
                            toolbar.animate().alpha(0).translationY(-toolbar.getHeight()).withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    toolbar.setVisibility(View.GONE);
                                }
                            });
                        } else {
                            toolbar.setVisibility(View.GONE);
                        }
                    } else {
                        if (Build.VERSION.SDK_INT > 16) {
                            toolbar.setAlpha(0);
                            toolbar.animate().alpha(1).translationY(0);
                        }
                        toolbar.setVisibility(View.VISIBLE);
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
        mHideTimer = new Timer("actionbar-hide-timer");
        if (savedInstanceState == null)
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        setContentView(R.layout.activity_imageviewer);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        try {
            setSupportActionBar(toolbar);
        } catch (Throwable t) {
        }
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 19)
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        else if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 21) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
            int resid = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resid > 0)
                lp.topMargin = getResources().getDimensionPixelSize(resid);
            else
                lp.topMargin = getResources().getDimensionPixelSize(R.dimen.status_bar_height);
            toolbar.setLayoutParams(lp);
        }
        getSupportActionBar().setTitle("Image Viewer");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar_translucent));

        mImage = (WebView) findViewById(R.id.image);
        mImage.setBackgroundColor(0);
        mImage.addJavascriptInterface(new JSInterface(), "Android");
        mImage.getSettings().setBuiltInZoomControls(true);
        if (Integer.parseInt(Build.VERSION.SDK) >= 19)
            mImage.getSettings().setDisplayZoomControls(false);
        mImage.getSettings().setJavaScriptEnabled(true);
        mImage.getSettings().setLoadWithOverviewMode(true);
        mImage.getSettings().setUseWideViewPort(true);
        mImage.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                mProgress.setProgress(newProgress);
            }
        });
        mImage.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mSpinner.setVisibility(View.GONE);
                mProgress.setVisibility(View.GONE);
                mImage.setVisibility(View.VISIBLE);
                hide_actionbar();
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                fail();
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                mSpinner.setVisibility(View.GONE);
                mProgress.setVisibility(View.VISIBLE);
            }
        });
        mSpinner = (ProgressBar) findViewById(R.id.spinner);
        mProgress = (ProgressBar) findViewById(R.id.progress);

        if (getIntent() != null && getIntent().getDataString() != null) {
            String url = getIntent().getDataString().replace(getResources().getString(R.string.IMAGE_SCHEME), "http");
            String lower = url.toLowerCase().replace("https://", "").replace("http://", "");
            if (lower.startsWith("www.dropbox.com/")) {
                if (lower.startsWith("www.dropbox.com/s/")) {
                    url = url.replace("://www.dropbox.com/s/", "://dl.dropboxusercontent.com/s/");
                } else {
                    url = url + "?dl=1";
                }
            } else if ((lower.startsWith("d.pr/i/") || lower.startsWith("droplr.com/i/")) && !lower.endsWith("+")) {
                url += "+";
            } else if (lower.startsWith("imgur.com/") || lower.startsWith("www.imgur.com/")) {
                new OEmbedTask().execute("https://api.imgur.com/oembed.json?url=" + url);
                return;
            } else if (lower.startsWith("flickr.com/") || lower.startsWith("www.flickr.com/")) {
                new OEmbedTask().execute("https://www.flickr.com/services/oembed/?format=json&url=" + url);
                return;
            } else if (lower.startsWith("instagram.com/") || lower.startsWith("www.instagram.com/") || lower.startsWith("instagr.am/") || lower.startsWith("www.instagr.am/")) {
                new OEmbedTask().execute("http://api.instagram.com/oembed?url=" + url);
                return;
            } else if (lower.startsWith("cl.ly")) {
                new ClLyTask().execute(url);
                return;
            } else if (url.contains("/wiki/File:")) {
                new WikiTask().execute(url.replace("/wiki/", "/w/api.php?action=query&format=json&prop=imageinfo&iiprop=url&titles="));
            } else if (lower.startsWith("leetfiles.com/") || lower.startsWith("www.leetfiles.com/")) {
                url = url.replace("www.", "").replace("leetfiles.com/image/", "i.leetfiles.com/").replace("?id=", "");
            } else if (lower.startsWith("leetfil.es/") || lower.startsWith("www.leetfil.es/")) {
                url = url.replace("www.", "").replace("leetfil.es/image/", "i.leetfiles.com/").replace("?id=", "");
            }
            loadImage(url);
        } else {
            finish();
        }
    }

    private void loadImage(String urlStr) {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB && urlStr.startsWith("https://")) {
                GingerbreadImageProxy proxy = new GingerbreadImageProxy();
                proxy.init();
                proxy.start();
                urlStr = String.format("http://127.0.0.1:%d/%s", proxy.getPort(), urlStr);
            }
            URL url = new URL(urlStr);

            mImage.loadDataWithBaseURL(null, "<!DOCTYPE html>\n" +
                    "<html><head><style>html, body, table { height: 100%; width: 100%; background-color: #000;}</style></head>\n" +
                    "<body>\n" +
                    "<table><tr><td>" +
                    "<img src='" + url.toString() + "' width='100%' onerror='Android.imageFailed()' onclick='Android.imageClicked()' style='background-color: #fff;'/>\n" +
                    "</td></tr></table>" +
                    "</body>\n" +
                    "</html>", "text/html", "UTF-8", null);

            try {
                if (Build.VERSION.SDK_INT >= 16) {
                    NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
                    if (nfc != null) {
                        nfc.setNdefPushMessage(new NdefMessage(NdefRecord.createUri(urlStr)), this);
                    }
                }
            } catch (Exception e) {
            }
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
        if (mSpinner != null && mSpinner.getVisibility() == View.GONE)
            hide_actionbar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHideTimer.cancel();
    }

    private void hide_actionbar() {
        if (mHideTimerTask != null)
            mHideTimerTask.cancel();
        mHideTimerTask = new TimerTask() {
            @Override
            public void run() {
                ImageViewerActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (Build.VERSION.SDK_INT > 16) {
                            toolbar.animate().alpha(0).translationY(-toolbar.getHeight()).withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    toolbar.setVisibility(View.GONE);
                                }
                            });
                        } else {
                            toolbar.setVisibility(View.GONE);
                        }
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

        if (getIntent() != null && getIntent().getDataString() != null) {
            Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.IMAGE_SCHEME), "http")));
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, getIntent().getDataString().replace(getResources().getString(R.string.IMAGE_SCHEME), "http"));
            intent.putExtra(ShareCompat.EXTRA_CALLING_PACKAGE, getPackageName());
            intent.putExtra(ShareCompat.EXTRA_CALLING_ACTIVITY, getPackageManager().getLaunchIntentForPackage(getPackageName()).getComponent());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);

            MenuItem shareItem = menu.findItem(R.id.action_share);
            ShareActionProviderHax share = (ShareActionProviderHax) MenuItemCompat.getActionProvider(shareItem);
            share.onShareActionProviderSubVisibilityChangedListener = this;
            share.setShareIntent(intent);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mHideTimerTask != null)
            mHideTimerTask.cancel();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
            return true;
        } else if (item.getItemId() == R.id.action_browser) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.IMAGE_SCHEME), "http")));
            startActivity(intent);
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_copy) {
            if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setText(getIntent().getDataString().replace(getResources().getString(R.string.IMAGE_SCHEME), "http"));
            } else {
                @SuppressLint("ServiceCast") android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newRawUri("IRCCloud Image URL", Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.IMAGE_SCHEME), "http")));
                clipboard.setPrimaryClip(clip);
            }
            Toast.makeText(ImageViewerActivity.this, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onShareActionProviderSubVisibilityChanged(boolean visible) {
        if (visible) {
            if (mHideTimerTask != null)
                mHideTimerTask.cancel();
        } else {
            hide_actionbar();
        }
    }
}
