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
import android.support.v7.widget.ShareActionProvider;
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

public class PastebinViewerActivity extends BaseActivity implements ShareActionProviderHax.OnShareActionProviderSubVisibilityChangedListener {

    private class FetchPastebinTask extends AsyncTaskEx<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                String html = NetworkConnection.getInstance().fetch(new URL(url), null, null, null);
                return html.replace("</head>", "<style>" +
                        "html, body, .mainContainer, .pasteContainer { height: 100%; width: 100% }" +
                        "body, div.paste { background-color: #fafafa; }" +
                        "h1#title, div#pasteSidebar, div.paste h1 { display: none; }" +
                        ".mainContainerFull, .mainContent, .mainContentPaste, div.paste { margin: 0px; padding: 0px; border: none; width: 100%; height: 100%; }" +
                        "</style></head>").replace("</body>", "<script>" +
                        "window.PASTEVIEW.on('rendered', _.bind(function () {\n" +
                        "window.PASTEVIEW.ace.container.style.height = \"100%\";\n" +
                        "}, window.PASTEVIEW));\n</script></body>");
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String html) {
            if (html != null) {
                PastebinViewerActivity.this.html = html;
                mWebView.loadDataWithBaseURL(url, html, "text/html", "UTF-8", null);
            } else {
                fail();
            }
        }
    }

    WebView mWebView;
    ProgressBar mSpinner;
    ProgressBar mProgress;
    Toolbar toolbar;
    String html;
    String url;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null)
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        setContentView(R.layout.activity_imageviewer);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setVisibility(View.GONE);

        findViewById(R.id.frame).setBackgroundResource(R.color.background_material_light);

        getSupportActionBar().setTitle("Pastebin");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar));

        mWebView = (WebView) findViewById(R.id.image);
        mWebView.getSettings().setBuiltInZoomControls(true);
        if (Integer.parseInt(Build.VERSION.SDK) >= 19)
            mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setLoadWithOverviewMode(false);
        mWebView.getSettings().setUseWideViewPort(false);
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                mProgress.setProgress(newProgress);
            }

            public boolean onConsoleMessage(android.webkit.ConsoleMessage cm) {
                android.util.Log.d("IRCCloud", cm.message() + " -- From line "
                        + cm.lineNumber() + " of "
                        + cm.sourceId());
                return true;
            }
        });
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mSpinner.setVisibility(View.GONE);
                mProgress.setVisibility(View.GONE);
                mWebView.setVisibility(View.VISIBLE);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                fail();
            }

            @Override
            public void onLoadResource(WebView view, String url) {
            }
        });
        mSpinner = (ProgressBar) findViewById(R.id.spinner);
        mProgress = (ProgressBar) findViewById(R.id.progress);

        if(savedInstanceState != null && savedInstanceState.containsKey("url")) {
            url = savedInstanceState.getString("url");
            html = savedInstanceState.getString("html");
            mWebView.loadDataWithBaseURL(url, html, "text/html", "UTF-8", null);
        } else {
            if (getIntent() != null && getIntent().getDataString() != null) {
                url = getIntent().getDataString().replace(getResources().getString(R.string.PASTE_SCHEME), "https");
                new FetchPastebinTask().execute();
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("url", url);
        outState.putString("html", html);
    }

    private void fail() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mWebView != null) {
            mWebView.setWebViewClient(null);
            mWebView.setWebChromeClient(null);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_pastebin, menu);

        if (getIntent() != null && getIntent().getDataString() != null) {
            Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse(url));
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, url);
            intent.putExtra(ShareCompat.EXTRA_CALLING_PACKAGE, getPackageName());
            intent.putExtra(ShareCompat.EXTRA_CALLING_ACTIVITY, getPackageManager().getLaunchIntentForPackage(getPackageName()).getComponent());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_NEW_TASK);

            MenuItem shareItem = menu.findItem(R.id.action_share);
            ShareActionProvider share = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
            share.setShareIntent(intent);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
            return true;
        } else if(item.getItemId() == R.id.action_linenumbers) {
            item.setChecked(!item.isChecked());
            mWebView.loadUrl("javascript:$('a.lines').click()");
        } else if (item.getItemId() == R.id.action_browser) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_copy) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setText(url);
            } else {
                @SuppressLint("ServiceCast") android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newRawUri("IRCCloud Image URL", Uri.parse(url));
                clipboard.setPrimaryClip(clip);
            }
            Toast.makeText(PastebinViewerActivity.this, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onShareActionProviderSubVisibilityChanged(boolean visible) {
    }
}
