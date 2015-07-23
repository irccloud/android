/*
 * Copyright (c) 2015 IRCCloud, Ltd.
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
import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.ShareEvent;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.ShareActionProviderHax;

import java.net.URL;

public class PastebinViewerActivity extends BaseActivity implements ShareActionProviderHax.OnShareActionProviderSubVisibilityChangedListener {
    private class FetchPastebinTask extends AsyncTaskEx<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            try {
                return NetworkConnection.getInstance().fetch(new URL(url), null, null, null, null);
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String html) {
            if (html != null) {
                PastebinViewerActivity.this.html = html;
                mWebView.loadDataWithBaseURL(url, html, "text/html", "UTF-8", null);

                try {
                    if (Build.VERSION.SDK_INT >= 16) {
                        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(PastebinViewerActivity.this);
                        if (nfc != null) {
                            nfc.setNdefPushMessage(new NdefMessage(NdefRecord.createUri(url)), PastebinViewerActivity.this);
                        }
                    }
                } catch (Exception e) {
                }
            } else {
                fail();
            }
        }
    }

    WebView mWebView;
    ProgressBar mSpinner;
    Toolbar toolbar;
    String html;
    String url;

    public class JSInterface {
        @JavascriptInterface
        public void ready() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSpinner.setVisibility(View.GONE);
                    supportInvalidateOptionsMenu();
                }
            });
        }

        @JavascriptInterface
        public void setTitle(final String title, final String subtitle) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(title != null && title.length() > 0) {
                        if (getSupportActionBar() != null)
                            getSupportActionBar().setTitle(title);
                    }
                    if(subtitle != null && subtitle.length() > 0) {
                        if (getSupportActionBar() != null)
                            getSupportActionBar().setSubtitle(subtitle);
                    }
                }
            });
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            Bitmap cloud = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
            setTaskDescription(new ActivityManager.TaskDescription(getResources().getString(R.string.app_name), cloud, 0xFFF2F7FC));
            cloud.recycle();
        }
        if (savedInstanceState == null)
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        setContentView(R.layout.activity_pastebin);
        mSpinner = (ProgressBar) findViewById(R.id.spinner);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.actionbar));

        mWebView = (WebView) findViewById(R.id.image);
        mWebView.getSettings().setBuiltInZoomControls(true);
        if (Integer.parseInt(Build.VERSION.SDK) >= 19)
            mWebView.getSettings().setDisplayZoomControls(false);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new JSInterface(), "Android");
        mWebView.getSettings().setLoadWithOverviewMode(false);
        mWebView.getSettings().setUseWideViewPort(false);
        mWebView.getSettings().setAppCacheEnabled(false);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                fail();
            }

            @Override
            public void onLoadResource(WebView view, String url) {
            }
        });

        if(savedInstanceState != null && savedInstanceState.containsKey("url")) {
            url = savedInstanceState.getString("url");
            html = savedInstanceState.getString("html");
            mWebView.loadDataWithBaseURL(url, html, "text/html", "UTF-8", null);
        } else {
            if (getIntent() != null && getIntent().getDataString() != null) {
                url = getIntent().getDataString().replace(getResources().getString(R.string.PASTE_SCHEME), "https");
                if(!url.contains("?"))
                    url += "?";
                try {
                    url += "&mobile=android&version=" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                } catch (PackageManager.NameNotFoundException e) {
                }
                new FetchPastebinTask().execute();
                Answers.getInstance().logContentView(new ContentViewEvent().putContentType("Pastebin"));
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
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(url == null || Uri.parse(url).getQueryParameter("own_paste") == null || !Uri.parse(url).getQueryParameter("own_paste").equals("1")) {
            menu.findItem(R.id.action_edit).setVisible(false);
            menu.findItem(R.id.delete).setVisible(false);
        }
        if(mSpinner == null || mSpinner.getVisibility() != View.GONE)
            menu.findItem(R.id.action_linenumbers).setEnabled(false);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
            return true;
        } else if(item.getItemId() == R.id.delete) {
            if(Uri.parse(url).getQueryParameter("own_paste").equals("1")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PastebinViewerActivity.this);
                builder.setTitle("Delete Pastebin");
                builder.setMessage("Are you sure you want to delete this pastebin?");
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        NetworkConnection.getInstance().delete_paste(Uri.parse(url).getQueryParameter("id"));
                        finish();
                        Toast.makeText(PastebinViewerActivity.this, "Pastebin deleted", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                AlertDialog d = builder.create();
                d.setOwnerActivity(PastebinViewerActivity.this);
                d.show();
            }
        } else if(item.getItemId() == R.id.action_linenumbers) {
            item.setChecked(!item.isChecked());
            mWebView.loadUrl("javascript:window.PASTEVIEW.doToggleLines()");
        } else if (item.getItemId() == R.id.action_browser) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.contains("?")?url.substring(0, url.indexOf("?")):url));
            startActivity(intent);
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_copy) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                if(url.contains("?"))
                    clipboard.setText(url.substring(0, url.indexOf("?")));
                else
                    clipboard.setText(url);
            } else {
                @SuppressLint("ServiceCast") android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                android.content.ClipData clip;
                if(url.contains("?"))
                    clip = android.content.ClipData.newRawUri("IRCCloud Pastebin URL", Uri.parse(url.substring(0, url.indexOf("?"))));
                else
                    clip = android.content.ClipData.newRawUri("IRCCloud Pastebin URL", Uri.parse(url));
                clipboard.setPrimaryClip(clip);
            }
            Toast.makeText(PastebinViewerActivity.this, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
        } else if(item.getItemId() == R.id.action_share) {
            if (getIntent() != null && getIntent().getDataString() != null) {
                Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse(url));
                intent.setType("text/plain");
                if(url.contains("?"))
                    intent.putExtra(Intent.EXTRA_TEXT, url.substring(0, url.indexOf("?")));
                else
                    intent.putExtra(Intent.EXTRA_TEXT, url);
                intent.putExtra(ShareCompat.EXTRA_CALLING_PACKAGE, getPackageName());
                intent.putExtra(ShareCompat.EXTRA_CALLING_ACTIVITY, getPackageManager().getLaunchIntentForPackage(getPackageName()).getComponent());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(Intent.createChooser(intent, "Share Pastebin"));
                Answers.getInstance().logShare(new ShareEvent().putContentType("Pastebin"));
            }
        } else if(item.getItemId() == R.id.action_edit) {
            mSpinner.setVisibility(View.VISIBLE);
            Intent i = new Intent(this, PastebinEditorActivity.class);
            i.putExtra("paste_id", Uri.parse(url).getQueryParameter("id"));
            startActivityForResult(i, 1);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onShareActionProviderSubVisibilityChanged(boolean visible) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 1) {
            if(resultCode == RESULT_OK) {
                mWebView.clearCache(true);
                mWebView.reload();
                getSupportActionBar().setTitle("Pastebin");
                getSupportActionBar().setSubtitle(null);
                mSpinner.setVisibility(View.VISIBLE);
                supportInvalidateOptionsMenu();
                new FetchPastebinTask().execute();
            } else {
                mSpinner.setVisibility(View.GONE);
            }
        }
    }
}
