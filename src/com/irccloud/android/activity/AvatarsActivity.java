/*
 * Copyright (c) 2018 IRCCloud, Ltd.
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.damnhandy.uri.template.UriTemplate;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.collection.ImageList;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.data.model.Server;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AvatarsActivity extends BaseActivity implements NetworkConnection.IRCEventHandler {
    private AvatarsAdapter adapter = new AvatarsAdapter();
    private int orgId = -1;
    private UriTemplate template;
    private MainActivity.FileUploadTask fileUploadTask;

    private static final int REQUEST_EXTERNAL_MEDIA_IRCCLOUD = 1;

    private class AvatarsAdapterEntry {
        public String id;
        public URL url;
        public String label;
        public int orgId;
        public Bitmap image;
        public boolean image_failed;
    }

    private class AvatarsAdapter extends BaseAdapter {
        private class ViewHolder {
            ImageView avatar;
            TextView name;
            ProgressBar progress;
            ImageButton delete;
        }
        public List<AvatarsAdapterEntry> avatars = new ArrayList<>();

        public void setAvatars(List<AvatarsAdapterEntry> avatars) {
            this.avatars = avatars;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return avatars.size();
        }

        @Override
        public Object getItem(int i) {
            return avatars.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        private View.OnClickListener deleteClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AvatarsAdapterEntry avatarToDelete = (AvatarsAdapterEntry)getItem((Integer)view.getTag());
                AlertDialog.Builder builder = new AlertDialog.Builder(AvatarsActivity.this);
                builder.setTitle("Delete Avatar");
                builder.setMessage("Are you sure you want to delete this avatar?");
                builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        NetworkConnection.getInstance().set_avatar(avatarToDelete.orgId, null, null);
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                AlertDialog d = builder.create();
                d.setOwnerActivity(AvatarsActivity.this);
                d.show();
            }
        };

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            View row = view;
            ViewHolder holder;

            if (row == null) {
                LayoutInflater inflater = getLayoutInflater();
                row = inflater.inflate(R.layout.row_avatar, viewGroup, false);

                holder = new ViewHolder();
                holder.avatar = row.findViewById(R.id.avatar);
                holder.name = row.findViewById(R.id.name);
                holder.progress = row.findViewById(R.id.progress);
                holder.delete = row.findViewById(R.id.delete);

                row.setTag(holder);
            } else {
                holder = (ViewHolder) row.getTag();
            }

            AvatarsAdapterEntry a = avatars.get(i);
            holder.name.setText(a.label);
            if (!a.image_failed) {
                if (a.image != null) {
                    holder.progress.setVisibility(View.GONE);
                    holder.avatar.setVisibility(View.VISIBLE);
                    holder.avatar.setImageBitmap(a.image);
                } else {
                    holder.avatar.setVisibility(View.GONE);
                    holder.avatar.setImageBitmap(null);
                    holder.progress.setVisibility(View.VISIBLE);
                }
            } else {
                holder.avatar.setVisibility(View.GONE);
                holder.progress.setVisibility(View.GONE);
            }
            holder.delete.setOnClickListener(deleteClickListener);
            holder.delete.setColorFilter(ColorScheme.getInstance().colorControlNormal, PorterDuff.Mode.SRC_ATOP);
            holder.delete.setTag(i);

            return row;
        }
    }

    private class RefreshTask extends AsyncTaskEx<Void, Void, Void> {
        ArrayList<AvatarsAdapterEntry> avatars = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(findViewById(android.R.id.list).getVisibility() != View.VISIBLE)
                findViewById(R.id.progress).setVisibility(View.VISIBLE);
            if(NetworkConnection.avatar_uri_template != null)
                template = UriTemplate.fromTemplate(NetworkConnection.avatar_uri_template);
        }

        public void addAvatar(String id, String label, int orgId) {
            AvatarsAdapterEntry a = new AvatarsAdapterEntry();
            a.id = id;
            a.label = label;
            a.orgId = orgId;
            try {
                if (NetworkConnection.avatar_uri_template != null) {
                    a.url = new URL(template.set("id", a.id).set("modifiers", "w320").expand());
                    a.image = ImageList.getInstance().getImage(a.url, 320);
                    final AvatarsAdapterEntry a1 = a;
                    if (a.image == null)
                        ImageList.getInstance().fetchImage(a.url, new ImageList.OnImageFetchedListener() {

                            @Override
                            public void onImageFetched(Bitmap image) {
                                a1.image = image;
                                if (a1.image == null)
                                    a1.image_failed = true;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        });
                }
            } catch (OutOfMemoryError e) {
                a.image_failed = true;
            } catch (Exception e) {
                a.image_failed = true;
                NetworkConnection.printStackTraceToCrashlytics(e);
            }
            avatars.add(a);
        }

        @Override
        protected Void doInBackground(Void... params) {
            SparseArray<Server> serversArray = ServersList.getInstance().getServers();
            ArrayList<Server> servers = new ArrayList<>();

            for (int i = 0; i < serversArray.size(); i++) {
                Server s = serversArray.valueAt(i);
                if(s != null)
                    servers.add(s);
            }
            Collections.sort(servers);

            for(Server s : servers) {
                if(s.getAvatar() != null && s.getAvatar().length() > 0) {
                    addAvatar(s.getAvatar(), s.getNick() + " on " + ((s.getName() != null && s.getName().length() > 0) ? s.getName() : s.getHostname()), s.getOrgId());
                }
            }
            if(NetworkConnection.getInstance().getUserInfo() != null && NetworkConnection.getInstance().getUserInfo().avatar != null) {
                addAvatar(NetworkConnection.getInstance().getUserInfo().avatar, "Public avatar", -1);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            adapter.setAvatars(avatars);
            if(adapter.getCount() > 0) {
                findViewById(android.R.id.list).setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ColorScheme.getDialogWhenLargeTheme(ColorScheme.getUserTheme()));
        onMultiWindowModeChanged(isMultiWindow());

        orgId = getIntent().getIntExtra("orgId", -1);

        setContentView(R.layout.listview);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setVisibility(View.VISIBLE);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        if(getSupportActionBar() != null) {
            getSupportActionBar().setElevation(0);
        }

        adapter = new AvatarsAdapter();
        ListView listView = findViewById(android.R.id.list);

        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                AvatarsAdapterEntry e = (AvatarsAdapterEntry)adapterView.getItemAtPosition(i);
                if(e != null) {
                    NetworkConnection.getInstance().set_avatar(orgId, e.id, null);
                    finish();
                }
            }
        });

        NetworkConnection.getInstance().addHandler(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        new RefreshTask().execute((Void)null);
        if(!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("avatars-off", true) || !PreferenceManager.getDefaultSharedPreferences(this).getBoolean("avatar-images", false)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AvatarsActivity.this);
                    builder.setTitle("Enable Avatars");
                    builder.setMessage("Viewing avatars in messages requires both the User Icons and Avatars settings to be enabled.  Would you like to enable them now?");
                    builder.setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AvatarsActivity.this).edit();
                            editor.putBoolean("avatars-off", true);
                            editor.putBoolean("avatar-images", true);
                            editor.apply();
                            try {
                                dialog.dismiss();
                            } catch (IllegalArgumentException e) {
                            }
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                dialog.dismiss();
                            } catch (IllegalArgumentException e) {
                            }
                        }
                    });
                    if(!isFinishing()) {
                        AlertDialog dialog = builder.create();
                        dialog.setOwnerActivity(AvatarsActivity.this);
                        dialog.show();
                    }
                }
            });
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
    protected void onDestroy() {
        super.onDestroy();
        NetworkConnection.getInstance().removeHandler(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri uri = CropImage.getActivityResult(imageReturnedIntent).getUri();
            if (uri != null) {
                fileUploadTask = new MainActivity.FileUploadTask(uri, null);
                fileUploadTask.avatar = true;
                fileUploadTask.orgId = orgId;
                if(Build.VERSION.SDK_INT >= 16 && ActivityCompat.checkSelfPermission(AvatarsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(AvatarsActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_EXTERNAL_MEDIA_IRCCLOUD);
                } else {
                    fileUploadTask.execute((Void) null);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case REQUEST_EXTERNAL_MEDIA_IRCCLOUD:
                    if(fileUploadTask != null) {
                        fileUploadTask.execute((Void) null);
                    }
                    break;
            }
        } else {
            if(fileUploadTask != null) {
                fileUploadTask.cancel(true);
                fileUploadTask = null;
            }
            Toast.makeText(this, "Upload cancelled: permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_avatars, menu);
        setMenuColorFilter(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_upload:
                CropImage.activity()
                        .setInitialCropWindowPaddingRatio(0)
                        .setAspectRatio(1,1)
                        .start(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onIRCEvent(int what, Object obj) {
        super.onIRCEvent(what, obj);

        switch (what) {
            case NetworkConnection.EVENT_USERINFO:
            case NetworkConnection.EVENT_AVATARCHANGE:
            case NetworkConnection.EVENT_MAKESERVER:
            case NetworkConnection.EVENT_BACKLOG_END:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new RefreshTask().execute((Void)null);
                    }
                });
                break;
        }
    }
}