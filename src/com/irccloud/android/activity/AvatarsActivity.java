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
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
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

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.damnhandy.uri.template.UriTemplate;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.IRCCloudLog;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.collection.ImageList;
import com.irccloud.android.data.collection.ServersList;
import com.irccloud.android.data.model.Server;
import com.canhub.cropper.CropImage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AvatarsActivity extends BaseActivity implements NetworkConnection.IRCEventHandler {
    private AvatarsAdapter adapter = new AvatarsAdapter();
    private int orgId = -1;
    private int cid = -1;
    private UriTemplate template;
    private MainActivity.FileUploadTask fileUploadTask;
    private Uri imageCaptureURI = null;

    private static final int REQUEST_EXTERNAL_MEDIA_TAKE_PHOTO = 1;
    private static final int REQUEST_EXTERNAL_MEDIA_CHOOSE_PHOTO = 2;

    private final ActivityResultLauncher<CropImageContractOptions> cropImage =
            registerForActivityResult(new CropImageContract(), this::onCropImageResult);

    private class AvatarsAdapterEntry {
        public String id;
        public URL url;
        public String label;
        public int orgId;
        public int cid;
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
                        NetworkConnection.getInstance().set_avatar(avatarToDelete.cid, avatarToDelete.orgId, null, null);
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

        public void addAvatar(String id, String label, int orgId, int cid) {
            AvatarsAdapterEntry a = new AvatarsAdapterEntry();
            a.id = id;
            a.label = label;
            a.orgId = orgId;
            a.cid = cid;
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
                    addAvatar(s.getAvatar(), s.getNick() + " on " + ((s.getName() != null && s.getName().length() > 0) ? s.getName() : s.getHostname()), s.getOrgId(), s.getCid());
                }
            }
            if(NetworkConnection.getInstance().getUserInfo() != null && NetworkConnection.getInstance().getUserInfo().avatar != null) {
                addAvatar(NetworkConnection.getInstance().getUserInfo().avatar, "Public avatar", -1, -1);
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
        getWindow().setNavigationBarColor(ColorScheme.getInstance().dialogBackgroundColor);
        onMultiWindowModeChanged(isMultiWindow());

        orgId = getIntent().getIntExtra("orgId", -1);
        cid = getIntent().getIntExtra("cid", -1);

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
                    NetworkConnection.getInstance().set_avatar(cid, orgId, e.id, null);
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

    public void onCropImageResult(@NonNull CropImageView.CropResult result) {
        if (result.isSuccessful()) {
            fileUploadTask = new MainActivity.FileUploadTask(result.getUriContent(), null);
            fileUploadTask.avatar = true;
            fileUploadTask.orgId = orgId;
            fileUploadTask.cid = cid;
            fileUploadTask.execute((Void) null);
        }
    }

    ActivityResultLauncher<Uri> requestCamera = registerForActivityResult(new ActivityResultContracts.TakePicture(), new ActivityResultCallback<Boolean>() {
        @Override
        public void onActivityResult(Boolean success) {
            if(success) {
                if (imageCaptureURI != null) {
                    CropImageContractOptions options = new CropImageContractOptions(imageCaptureURI, new CropImageOptions())
                            .setInitialCropWindowPaddingRatio(0)
                            .setAspectRatio(1,1);
                    cropImage.launch(options);
                }
            }
        }
    });

    ActivityResultLauncher<PickVisualMediaRequest> requestPhoto = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), new ActivityResultCallback<Uri>() {
        @Override
        public void onActivityResult(Uri selectedImage) {
            if (selectedImage != null) {
                selectedImage = MainActivity.makeTempCopy(selectedImage, AvatarsActivity.this);
                CropImageContractOptions options = new CropImageContractOptions(selectedImage, new CropImageOptions())
                        .setInitialCropWindowPaddingRatio(0)
                        .setAspectRatio(1,1);
                cropImage.launch(options);
            }
        }
    });


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for(int j = 0; j < grantResults.length; j++) {
            if(grantResults[j] != PackageManager.PERMISSION_GRANTED) {
                IRCCloudLog.Log(Log.ERROR, "IRCCloud", "Permission denied: " + permissions[j]);
                if(fileUploadTask != null) {
                    if(fileUploadTask.metadataDialog != null) {
                        try {
                            fileUploadTask.metadataDialog.cancel();
                        } catch (Exception e) {

                        }
                    }
                    fileUploadTask.cancel(true);
                    fileUploadTask = null;
                }
                Toast.makeText(this, "Upload cancelled: permission denied", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        switch (requestCode) {
            case REQUEST_EXTERNAL_MEDIA_TAKE_PHOTO:
                try {
                    File imageDir = new File(Environment.getExternalStorageDirectory(), "IRCCloud");
                    imageDir.mkdirs();
                    new File(imageDir, ".nomedia").createNewFile();
                    File tempFile = File.createTempFile("irccloudcapture", ".jpg", imageDir);
                    imageCaptureURI = Uri.fromFile(tempFile);
                    requestCamera.launch(FileProvider.getUriForFile(AvatarsActivity.this,getPackageName() + ".fileprovider",tempFile));
                } catch (IOException e) {
                }
                break;
            case REQUEST_EXTERNAL_MEDIA_CHOOSE_PHOTO:
                requestPhoto.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
                break;
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
                upload();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean mediaPermissionsGranted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(AvatarsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(AvatarsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    private void requestMediaPermissions(int requestCode) {
        ActivityCompat.requestPermissions(AvatarsActivity.this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                requestCode);
    }

    private void upload() {
        AlertDialog.Builder builder;
        AlertDialog dialog;
        builder = new AlertDialog.Builder(this);
        final String[] dialogItems = new String[]{"Take a Photo", "Choose Existing Photo"};

        builder.setItems(dialogItems, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch(dialogItems[which]) {
                    case "Take a Photo":
                        if(!mediaPermissionsGranted()) {
                            requestMediaPermissions(REQUEST_EXTERNAL_MEDIA_TAKE_PHOTO);
                        } else {
                            try {
                                File imageDir = (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) ?
                                        new File(Environment.getExternalStorageDirectory(), "IRCCloud") :
                                        new File(getExternalFilesDir(null), "uploads")
                                        ;
                                imageDir.mkdirs();
                                new File(imageDir, ".nomedia").createNewFile();
                                File tempFile = File.createTempFile("irccloudcapture", ".jpg", imageDir);
                                imageCaptureURI = Uri.fromFile(tempFile);
                                requestCamera.launch(FileProvider.getUriForFile(AvatarsActivity.this,getPackageName() + ".fileprovider",tempFile));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    case "Choose Existing Photo":
                        if(!mediaPermissionsGranted()) {
                            requestMediaPermissions(REQUEST_EXTERNAL_MEDIA_CHOOSE_PHOTO);
                        } else {
                            requestPhoto.launch(new PickVisualMediaRequest.Builder()
                                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                                    .build());
                        }
                        break;
                }
                dialog.dismiss();
            }
        });
        dialog = builder.create();
        dialog.setOwnerActivity(AvatarsActivity.this);
        dialog.show();
    }

    @Override
    public void onIRCEvent(int what, Object obj) {
        IRCCloudJSONObject object;
        super.onIRCEvent(what, obj);

        switch (what) {
            case NetworkConnection.EVENT_AVATARCHANGE:
                object = (IRCCloudJSONObject)obj;
                if(!object.getBoolean("self"))
                    break;
            case NetworkConnection.EVENT_USERINFO:
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