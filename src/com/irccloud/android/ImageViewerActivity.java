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

package com.irccloud.android;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class ImageViewerActivity extends BaseActivity {

    WebView mImage;
    ProgressBar mProgress;

    public class JSInterface {
        @JavascriptInterface
        public void imageFailed() {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getIntent().getDataString().replace("irccloud-image", "http")));
            startActivity(intent);
            finish();
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imageviewer);
        if(Integer.parseInt(Build.VERSION.SDK) >= 11)
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        getSupportActionBar().setTitle("Image Viewer");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mImage = (WebView)findViewById(R.id.image);
        mImage.addJavascriptInterface(new JSInterface(), "Android");
        mImage.getSettings().setBuiltInZoomControls(true);
        mImage.getSettings().setJavaScriptEnabled(true);
        mImage.getSettings().setLoadWithOverviewMode(true);
        mImage.getSettings().setUseWideViewPort(true);
        mImage.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mProgress.setVisibility(View.GONE);
                mImage.setVisibility(View.VISIBLE);
            }
        });
        mProgress = (ProgressBar)findViewById(R.id.progress);

        if(getIntent() != null && getIntent().getDataString() != null) {
            mImage.loadData("<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<body bgcolor='#000'>\n" +
                    "<img src='" + getIntent().getDataString().replace("irccloud-image", "http") + "' width='100%' style='top:0; bottom:0; margin: auto; position: absolute;'  onerror='Android.imageFailed()'/>\n" +
                    "</body>\n" +
                    "</html>", "text/html", "UTF-8");
        } else {
            finish();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_imageviewer, menu);

        if(getIntent() != null && getIntent().getDataString() != null) {
            Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse(getIntent().getDataString().replace("irccloud-image", "http")));
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, getIntent().getDataString().replace("irccloud-image", "http"));

            MenuItem shareItem = menu.findItem(R.id.action_share);
            ShareActionProvider share = (ShareActionProvider)MenuItemCompat.getActionProvider(shareItem);
            share.setShareIntent(intent);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if(item.getItemId() == R.id.action_browser) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getIntent().getDataString().replace("irccloud-image", "http")));
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
