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

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsServiceConnection;
import android.support.customtabs.CustomTabsSession;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;
import com.crashlytics.android.answers.ShareEvent;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.BuildConfig;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.ShareActionProviderHax;

import org.chromium.customtabsclient.shared.CustomTabsHelper;
import org.json.JSONObject;

public class VideoPlayerActivity extends BaseActivity implements ShareActionProviderHax.OnShareActionProviderSubVisibilityChangedListener {
    private View controls;
    private VideoView video;
    private ProgressBar mProgress;
    private TextView time_current, time;
    private SeekBar seekBar;
    private ImageButton rew, pause, play, ffwd;
    private Toolbar toolbar;
    private Handler handler = new Handler();
    private ShareActionProviderHax share;

    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            if(video != null && video.isPlaying()) {
                if (Build.VERSION.SDK_INT > 16) {
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                } else {
                    toolbar.setVisibility(View.GONE);
                    controls.setVisibility(View.GONE);
                }
            }
        }
    };
    private Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            handler.removeCallbacks(this);
            if(video.isPlaying()) {
                play.setVisibility(View.GONE);
                pause.setVisibility(View.VISIBLE);
            } else {
                play.setVisibility(View.VISIBLE);
                pause.setVisibility(View.GONE);
            }

            if(video.getDuration() > 0) {
                time_current.setText(DateUtils.formatElapsedTime(video.getCurrentPosition() / 1000));
                time.setText(DateUtils.formatElapsedTime(video.getDuration() / 1000));
                seekBar.setIndeterminate(false);
                seekBar.setProgress(video.getCurrentPosition());
                seekBar.setMax(video.getDuration());
                seekBar.setSecondaryProgress(video.getBufferPercentage());
            } else {
                seekBar.setIndeterminate(true);
                time.setText("--:--");
                time_current.setText("--:--");
            }
            handler.postDelayed(this, 250);
        }
    };

    private Runnable rewindRunnable = new Runnable() {
        @Override
        public void run() {
            int position = video.getCurrentPosition() - 500;
            if(position < 0)
                position = 0;
            video.seekTo(position);
            handler.postDelayed(this, 250);
            handler.post(mUpdateRunnable);
        }
    };

    private Runnable ffwdRunnable = new Runnable() {
        @Override
        public void run() {
            int position = video.getCurrentPosition() + 1000;
            if(position > video.getDuration())
                position = video.getDuration();
            video.seekTo(position);
            handler.postDelayed(this, 250);
            handler.post(mUpdateRunnable);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.ImageViewerTheme);
        if (savedInstanceState == null)
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        setContentView(R.layout.activity_video_player);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        try {
            setSupportActionBar(toolbar);
        } catch (Throwable t) {
        }
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 19)
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if(Build.VERSION.SDK_INT > 16) {
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                        toolbar.setAlpha(0);
                        toolbar.animate().alpha(1);
                        controls.setAlpha(0);
                        controls.animate().alpha(1);
                        hide_actionbar();
                    } else {
                        toolbar.setAlpha(1);
                        toolbar.animate().alpha(0);
                        controls.setAlpha(1);
                        controls.animate().alpha(0);
                    }
                }
            });
        }

        controls = findViewById(R.id.controls);
        mProgress = (ProgressBar) findViewById(R.id.progress);

        rew = (ImageButton)findViewById(R.id.rew);
        rew.setEnabled(false);
        rew.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    handler.removeCallbacks(mHideRunnable);
                    handler.post(rewindRunnable);
                }
                if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    handler.removeCallbacks(rewindRunnable);
                    hide_actionbar();
                }
                return false;
            }
        });
        play = (ImageButton)findViewById(R.id.play);
        play.setEnabled(false);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                video.start();
                handler.post(mUpdateRunnable);
                hide_actionbar();
            }
        });
        pause = (ImageButton)findViewById(R.id.pause);
        pause.setEnabled(false);
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                video.pause();
                handler.post(mUpdateRunnable);
                handler.removeCallbacks(mHideRunnable);
            }
        });
        ffwd = (ImageButton)findViewById(R.id.ffwd);
        ffwd.setEnabled(false);
        ffwd.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    handler.removeCallbacks(mHideRunnable);
                    handler.post(ffwdRunnable);
                }
                if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    handler.removeCallbacks(ffwdRunnable);
                    hide_actionbar();
                }
                return false;
            }
        });
        time_current = (TextView)findViewById(R.id.time_current);
        time = (TextView)findViewById(R.id.time);
        seekBar = (SeekBar)findViewById(R.id.mediacontroller_progress);
        seekBar.setEnabled(false);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    video.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if(video.isPlaying())
                    video.pause();
                handler.removeCallbacks(mHideRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                hide_actionbar();
            }
        });

        video = (VideoView)findViewById(R.id.video);
        video.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (Build.VERSION.SDK_INT > 16) {
                    if((getWindow().getDecorView().getSystemUiVisibility() & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                    } else {
                        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                        hide_actionbar();
                    }
                } else {
                    if (toolbar.getVisibility() == View.VISIBLE) {
                        toolbar.setVisibility(View.GONE);
                        controls.setVisibility(View.GONE);
                    } else {
                        toolbar.setVisibility(View.VISIBLE);
                        controls.setVisibility(View.VISIBLE);
                        hide_actionbar();
                    }
                }
                return false;
            }
        });
        video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
                mProgress.setVisibility(View.GONE);
                rew.setEnabled(true);
                pause.setEnabled(true);
                play.setEnabled(true);
                ffwd.setEnabled(true);
                seekBar.setEnabled(true);
                handler.post(mUpdateRunnable);
                hide_actionbar();
            }
        });
        video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                video.pause();
                video.seekTo(video.getDuration());
                handler.removeCallbacks(mHideRunnable);
                if (Build.VERSION.SDK_INT > 16) {
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
                } else {
                    toolbar.setVisibility(View.VISIBLE);
                    controls.setVisibility(View.VISIBLE);
                }
            }
        });
        video.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                AlertDialog d = new AlertDialog.Builder(VideoPlayerActivity.this)
                        .setTitle("Playback Failed")
                        .setMessage("An error occured while trying to play this video")
                        .setPositiveButton("Download", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                if(Build.VERSION.SDK_INT >= 16 && ActivityCompat.checkSelfPermission(VideoPlayerActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(VideoPlayerActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
                                } else {
                                    DownloadManager d = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                    if (d != null) {
                                        DownloadManager.Request r = new DownloadManager.Request(Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http")));
                                        if (Build.VERSION.SDK_INT >= 11) {
                                            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                            r.allowScanningByMediaScanner();
                                        }
                                        d.enqueue(r);
                                    }
                                }
                            }
                        })
                        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .create();
                d.show();
                return true;
            }
        });

        if (getIntent() != null && getIntent().getDataString() != null) {
            Uri url = Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http"));
            if(url.getHost().endsWith("facebook.com")) {
                new FacebookTask().execute(url);
            } else {
                video.setVideoURI(url);
            }
            Answers.getInstance().logContentView(new ContentViewEvent().putContentType("Video"));
        } else {
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(mUpdateRunnable);
    }

    @Override
    public void onStop() {
        super.onStop();
        handler.removeCallbacks(mUpdateRunnable);
    }

    CustomTabsSession mCustomTabsSession = null;
    CustomTabsServiceConnection mCustomTabsConnection = new CustomTabsServiceConnection() {
        @Override
        public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
            client.warmup(0);
            mCustomTabsSession = client.newSession(null);
            mCustomTabsSession.mayLaunchUrl(Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http")), null, null);
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
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(mHideRunnable);
        try {
            video.stopPlayback();
        } catch (Exception e) {
        }
        if(share != null) {
            share.setOnShareTargetSelectedListener(null);
            share.onShareActionProviderSubVisibilityChangedListener = null;
            share.setSubUiVisibilityListener(null);
            share.setVisibilityListener(null);
        }
    }

    private void hide_actionbar() {
        handler.removeCallbacks(mHideRunnable);
        handler.postDelayed(mHideRunnable, 3000);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_videoplayer, menu);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && getIntent() != null && getIntent().getDataString() != null) {
            Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http")));
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http"));
            intent.putExtra(ShareCompat.EXTRA_CALLING_PACKAGE, getPackageName());
            intent.putExtra(ShareCompat.EXTRA_CALLING_ACTIVITY, getPackageManager().getLaunchIntentForPackage(getPackageName()).getComponent());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_NEW_TASK);

            MenuItem shareItem = menu.findItem(R.id.action_share);
            share = (ShareActionProviderHax) MenuItemCompat.getActionProvider(shareItem);
            share.onShareActionProviderSubVisibilityChangedListener = this;
            share.setOnShareTargetSelectedListener(new ShareActionProvider.OnShareTargetSelectedListener() {
                @Override
                public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                    String name = intent.getComponent().getPackageName();
                    try {
                        name = String.valueOf(getPackageManager().getActivityInfo(intent.getComponent(), 0).loadLabel(getPackageManager()));
                    } catch (PackageManager.NameNotFoundException e) {
                        NetworkConnection.printStackTraceToCrashlytics(e);
                    }
                    Answers.getInstance().logShare(new ShareEvent().putContentType("Video").putMethod(name));
                    return false;
                }
            });
            share.setShareIntent(intent);
        } else {
            MenuItem shareItem = menu.findItem(R.id.action_share);
            shareItem.getIcon().mutate().setColorFilter(0xFFCCCCCC, PorterDuff.Mode.SRC_ATOP);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        handler.removeCallbacks(mHideRunnable);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
            return true;
        } else if (item.getItemId() == R.id.action_download) {
            if(Build.VERSION.SDK_INT >= 16 && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},0);
            } else {
                DownloadManager d = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                if (d != null) {
                    String uri = getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http");
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB && uri.startsWith("https://"))
                        uri = "http://" + uri.substring(8);
                    DownloadManager.Request r = new DownloadManager.Request(Uri.parse(uri));
                    r.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, getIntent().getData().getLastPathSegment());
                    if (Build.VERSION.SDK_INT >= 11) {
                        r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                        r.allowScanningByMediaScanner();
                    }
                    d.enqueue(r);
                    Answers.getInstance().logShare(new ShareEvent().putContentType("Video").putMethod("Download"));
                }
            }
            return true;
        } else if (item.getItemId() == R.id.action_copy) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setText(getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http"));
            } else {
                @SuppressLint("ServiceCast") android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newRawUri("IRCCloud Video URL", Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http")));
                clipboard.setPrimaryClip(clip);
            }
            Toast.makeText(VideoPlayerActivity.this, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
            Answers.getInstance().logShare(new ShareEvent().putContentType("Video").putMethod("Copy to Clipboard"));
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && item.getItemId() == R.id.action_share) {
            Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http")));
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http"));
            intent.putExtra(ShareCompat.EXTRA_CALLING_PACKAGE, getPackageName());
            intent.putExtra(ShareCompat.EXTRA_CALLING_ACTIVITY, getPackageManager().getLaunchIntentForPackage(getPackageName()).getComponent());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent, "Share Video"));
            Answers.getInstance().logShare(new ShareEvent().putContentType("Video"));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onShareActionProviderSubVisibilityChanged(boolean visible) {
        if (visible) {
            handler.removeCallbacks(mHideRunnable);
        } else {
            hide_actionbar();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            DownloadManager d = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (d != null) {
                DownloadManager.Request r = new DownloadManager.Request(Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http")));
                r.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, getIntent().getData().getLastPathSegment());
                if (Build.VERSION.SDK_INT >= 11) {
                    r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    r.allowScanningByMediaScanner();
                }
                d.enqueue(r);
                Answers.getInstance().logShare(new ShareEvent().putContentType("Video").putMethod("Download"));
            }
        } else {
            Toast.makeText(this, "Unable to download: permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    private class FacebookTask extends AsyncTaskEx<Uri, Void, String> {

        @Override
        protected String doInBackground(Uri... params) {
            try {
                String videoID = null;
                Uri url = params[0];
                if(url.getPath().equals("/video.php")) {
                    videoID = url.getQueryParameter("v");
                    if(videoID == null)
                        videoID = url.getQueryParameter("id");
                } else {
                    videoID = url.getPathSegments().get(2);
                }

                if(videoID != null) {
                    JSONObject o = NetworkConnection.getInstance().fetchJSON("https://graph.facebook.com/v2.2/" + videoID + "?fields=source&access_token=" + BuildConfig.FB_ACCESS_TOKEN);
                    if (o.has("source"))
                        return o.getString("source");
                }
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String url) {
            if (url != null) {
                if(mCustomTabsSession != null)
                    mCustomTabsSession.mayLaunchUrl(Uri.parse(url), null, null);
                video.setVideoURI(Uri.parse(url));
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http")));
                startActivity(intent);
                finish();
            }
        }
    }
}
