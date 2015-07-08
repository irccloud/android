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
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ShareCompat;
import android.support.v4.view.MenuItemCompat;
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

import com.irccloud.android.R;
import com.irccloud.android.ShareActionProviderHax;

public class VideoPlayerActivity extends BaseActivity implements ShareActionProviderHax.OnShareActionProviderSubVisibilityChangedListener {
    private View controls;
    private VideoView video;
    private ProgressBar mProgress;
    private TextView time_current, time;
    private SeekBar seekBar;
    private ImageButton rew, pause, play, ffwd;
    private Toolbar toolbar;
    private Handler handler = new Handler();

    private Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            if(video != null && video.isPlaying()) {
                if (Build.VERSION.SDK_INT > 16) {
                    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
                } else {
                    toolbar.setVisibility(View.GONE);
                }
            }
        }
    };
    private Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
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
            } else {
                seekBar.setIndeterminate(true);
                time.setText("--:--");
                time_current.setText("--:--");
            }
            handler.removeCallbacks(this);
            handler.postDelayed(this, 250);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                    } else {
                        toolbar.setVisibility(View.VISIBLE);
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
                                DownloadManager d = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
                                if(d != null) {
                                    DownloadManager.Request r = new DownloadManager.Request(Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http")));
                                    if(Build.VERSION.SDK_INT >= 11) {
                                        r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                                        r.allowScanningByMediaScanner();
                                    }
                                    d.enqueue(r);
                                }
                            }
                        })
                        .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .setNeutralButton("Open in Browser", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.IMAGE_SCHEME), "http")));
                                startActivity(intent);
                                finish();
                            }
                        })
                        .create();
                d.show();
                return false;
            }
        });

        if (getIntent() != null && getIntent().getDataString() != null) {
            String url = getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http");
            video.setVideoURI(Uri.parse(url));
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
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(mUpdateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(mHideRunnable);
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

        if (getIntent() != null && getIntent().getDataString() != null) {
            Intent intent = new Intent(Intent.ACTION_SEND, Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http")));
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http"));
            intent.putExtra(ShareCompat.EXTRA_CALLING_PACKAGE, getPackageName());
            intent.putExtra(ShareCompat.EXTRA_CALLING_ACTIVITY, getPackageManager().getLaunchIntentForPackage(getPackageName()).getComponent());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET | Intent.FLAG_ACTIVITY_NEW_TASK);

            MenuItem shareItem = menu.findItem(R.id.action_share);
            ShareActionProviderHax share = (ShareActionProviderHax) MenuItemCompat.getActionProvider(shareItem);
            share.onShareActionProviderSubVisibilityChangedListener = this;
            share.setShareIntent(intent);
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
            DownloadManager d = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
            if(d != null) {
                DownloadManager.Request r = new DownloadManager.Request(Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.VIDEO_SCHEME), "http")));
                if(Build.VERSION.SDK_INT >= 11) {
                    r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    r.allowScanningByMediaScanner();
                }
                d.enqueue(r);
            }
            return true;
        } else if (item.getItemId() == R.id.action_copy) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                clipboard.setText(getIntent().getDataString().replace(getResources().getString(R.string.IMAGE_SCHEME), "http"));
            } else {
                @SuppressLint("ServiceCast") android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newRawUri("IRCCloud Image URL", Uri.parse(getIntent().getDataString().replace(getResources().getString(R.string.IMAGE_SCHEME), "http")));
                clipboard.setPrimaryClip(clip);
            }
            Toast.makeText(VideoPlayerActivity.this, "Link copied to clipboard", Toast.LENGTH_SHORT).show();
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
}
