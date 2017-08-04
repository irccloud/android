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
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.ShareEvent;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.ChromeCopyLinkBroadcastReceiver;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.ShareActionProviderHax;

import org.chromium.customtabsclient.shared.CustomTabsHelper;

import java.net.URL;

public class PastebinViewerActivity extends BaseActivity implements ShareActionProviderHax.OnShareActionProviderSubVisibilityChangedListener {
    private class FetchPastebinTask extends AsyncTaskEx<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            try {
                Thread.sleep(1000);
                return NetworkConnection.getInstance().fetch(new URL(url), null, NetworkConnection.getInstance().session, null, null);
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
        setTheme(ColorScheme.getDialogWhenLargeTheme(ColorScheme.getUserTheme()));
        onMultiWindowModeChanged(isMultiWindow());
        if(savedInstanceState == null && (getWindowManager().getDefaultDisplay().getWidth() < TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 800, getResources().getDisplayMetrics()) || isMultiWindow()))
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        setContentView(R.layout.activity_pastebin);
        mSpinner = findViewById(R.id.spinner);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(android.support.v7.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mWebView = findViewById(R.id.image);
        mWebView.setBackgroundColor(ColorScheme.getInstance().contentBackgroundColor);
        mWebView.getSettings().setBuiltInZoomControls(true);
        if (Integer.parseInt(Build.VERSION.SDK) >= 19)
            mWebView.getSettings().setDisplayZoomControls(!getPackageManager().hasSystemFeature("android.hardware.touchscreen"));
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
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Crashlytics.log(consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR ? Log.ERROR : Log.WARN, "IRCCloud", "Javascript error - line: " + consoleMessage.lineNumber() + " message: " + consoleMessage.message());
                return super.onConsoleMessage(consoleMessage);
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
                    url += "&mobile=android&version=" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName + "&theme=" + ColorScheme.getUserTheme();
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
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode) {
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        if(getWindowManager().getDefaultDisplay().getWidth() > TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 800, getResources().getDisplayMetrics()) && !isMultiWindow()) {
            params.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 800, getResources().getDisplayMetrics());
            params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 800, getResources().getDisplayMetrics());
        } else {
            params.width = -1;
            params.height = -1;
        }
        getWindow().setAttributes(params);
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

    CustomTabsServiceConnection mCustomTabsConnection = new CustomTabsServiceConnection() {
        @Override
        public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
            if(client != null) {
                client.warmup(0);
                CustomTabsSession session = client.newSession(null);
                if (session != null)
                    session.mayLaunchUrl(Uri.parse(url.contains("?") ? url.substring(0, url.indexOf("?")) : url), null, null);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                String packageName = CustomTabsHelper.getPackageNameToUse(this);
                if (packageName != null && packageName.length() > 0)
                    CustomTabsClient.bindCustomTabsService(this, packageName, mCustomTabsConnection);
            }
        } catch (Exception e) {
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            try {
                unbindService(mCustomTabsConnection);
            } catch (Exception e) {
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(getWindowManager().getDefaultDisplay().getWidth() < TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 800, getResources().getDisplayMetrics()) || isMultiWindow())
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_pastebin, menu);
        setMenuColorFilter(menu);
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
            if(getWindowManager().getDefaultDisplay().getWidth() < TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 800, getResources().getDisplayMetrics()) || isMultiWindow())
                overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
            return true;
        } else if(item.getItemId() == R.id.delete) {
            if(Uri.parse(url).getQueryParameter("own_paste").equals("1")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PastebinViewerActivity.this);
                builder.setTitle("Delete Snippet");
                builder.setMessage("Are you sure you want to delete this snippet?");
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        NetworkConnection.getInstance().delete_paste(Uri.parse(url).getQueryParameter("id"), null);
                        finish();
                        Toast.makeText(PastebinViewerActivity.this, "Snippet deleted", Toast.LENGTH_SHORT).show();
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
                if(!isFinishing())
                    d.show();
            }
        } else if(item.getItemId() == R.id.action_linenumbers) {
            item.setChecked(!item.isChecked());
            mWebView.loadUrl("javascript:window.PASTEVIEW.doToggleLines()");
        } else if (item.getItemId() == R.id.action_browser) {
            if(!PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("browser", false) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setToolbarColor(ColorScheme.getInstance().navBarColor);
                builder.addDefaultShareMenuItem();
                builder.addMenuItem("Copy URL", PendingIntent.getBroadcast(this, 0, new Intent(this, ChromeCopyLinkBroadcastReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT));

                CustomTabsIntent intent = builder.build();
                intent.intent.setData(Uri.parse(url.contains("?")?url.substring(0, url.indexOf("?")):url));
                if(Build.VERSION.SDK_INT >= 22)
                    intent.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse(Intent.URI_ANDROID_APP_SCHEME + "//" + getPackageName()));
                if (Build.VERSION.SDK_INT >= 16 && intent.startAnimationBundle != null) {
                    startActivity(intent.intent, intent.startAnimationBundle);
                } else {
                    startActivity(intent.intent);
                }
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.contains("?")?url.substring(0, url.indexOf("?")):url));
                startActivity(intent);
            }
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
                    clip = android.content.ClipData.newRawUri("IRCCloud Snippet URL", Uri.parse(url.substring(0, url.indexOf("?"))));
                else
                    clip = android.content.ClipData.newRawUri("IRCCloud Snippet URL", Uri.parse(url));
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

                startActivity(Intent.createChooser(intent, "Share Snippet"));
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
                getSupportActionBar().setTitle("Snippet");
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
