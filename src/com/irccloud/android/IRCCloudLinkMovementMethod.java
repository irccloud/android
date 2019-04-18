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

package com.irccloud.android;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Browser;
import androidx.browser.customtabs.CustomTabsIntent;
import android.text.Layout;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.damnhandy.uri.template.UriTemplate;
import com.irccloud.android.data.collection.ImageList;

import org.chromium.customtabsclient.shared.CustomTabsHelper;
import org.json.JSONObject;

import java.util.HashMap;

public class IRCCloudLinkMovementMethod extends LinkMovementMethod {
    private static IRCCloudLinkMovementMethod instance = null;
    private static HashMap<String, String> file_ids = new HashMap<>();

    private static class FetchFileIDTask extends AsyncTaskEx<String, Void, JSONObject> {
        public Uri original_url;
        public Context context;

        @Override
        protected JSONObject doInBackground(String... params) {
            if(!isCancelled()) {
                try {
                    Thread.sleep(1000);
                    return NetworkConnection.getInstance().fetchJSON("https://" + NetworkConnection.IRCCLOUD_HOST + "/file/json/" + params[0]);
                } catch (Exception e) {
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if(!isCancelled()) {
                try {
                    if (result != null && result.has("mime_type")) {
                        UriTemplate template = UriTemplate.fromTemplate(NetworkConnection.file_uri_template);
                        template.set("id", result.getString("id"));
                        String mime_type = result.getString("mime_type");
                        String extension = result.has("extension") ? result.getString("extension") : null;
                        if(extension == null || extension.length() == 0)
                            extension = "." + mime_type.substring(mime_type.indexOf("/") + 1);

                        String name = result.has("name") ? result.getString("name") : "";
                        if(!name.toLowerCase().endsWith(extension.toLowerCase()))
                            name = result.getString("id") +  extension;

                        template.set("name", name);

                        if (PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("imageviewer", true)) {
                            if(ImageList.isImageURL(template.expand())) {
                                original_url = Uri.parse(IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.IMAGE_SCHEME) + template.expand().substring(4));
                            }
                        }

                        if (PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("videoviewer", true)) {
                            if(mime_type.equals("video/mp4") || mime_type.equals("video/webm") || mime_type.equals("video/3gpp") || name.toLowerCase().matches("(^.*/.*\\.3gpp?)|(^.*/.*\\.mp4$)|(^.*/.*\\.m4v$)|(^.*/.*\\.webm$)")) {
                                original_url = Uri.parse(IRCCloudApplication.getInstance().getApplicationContext().getResources().getString(R.string.VIDEO_SCHEME) + template.expand().substring(4));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                launchBrowser(original_url, context);
            }
        }
    }


    public synchronized static IRCCloudLinkMovementMethod getInstance() {
        if (instance == null)
            instance = new IRCCloudLinkMovementMethod();
        return instance;
    }

    @Override
    protected boolean handleMovementKey(TextView widget, Spannable buffer, int keyCode, int movementMetaState, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (KeyEvent.metaStateHasNoModifiers(movementMetaState)) {
                    if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                        Layout layout = widget.getLayout();
                        int padding = widget.getTotalPaddingTop() + widget.getTotalPaddingBottom();
                        int areaTop = widget.getScrollY();
                        int areaBot = areaTop + widget.getHeight() - padding;
                        int lineTop = layout.getLineForVertical(areaTop);
                        int lineBot = layout.getLineForVertical(areaBot);
                        int first = layout.getLineStart(lineTop);
                        int last = layout.getLineEnd(lineBot);
                        int a = Selection.getSelectionStart(buffer);
                        int b = Selection.getSelectionEnd(buffer);
                        int selStart = Math.min(a, b);
                        int selEnd = Math.max(a, b);
                        if (selStart < 0) {
                            if (buffer.getSpanStart(FROM_BELOW) >= 0) {
                                selStart = selEnd = buffer.length();
                            }
                        }
                        if (selStart > last)
                            selStart = selEnd = Integer.MAX_VALUE;
                        if (selEnd < first)
                            selStart = selEnd = -1;

                        if (selStart == selEnd) {
                            return false;
                        }
                        URLSpan[] links = buffer.getSpans(selStart, selEnd, URLSpan.class);
                        if (links.length != 1) {
                            return false;
                        }

                        launchURI(Uri.parse(links[0].getURL()), widget.getContext());

                        return true;
                    }
                }
                break;
        }
        return super.handleMovementKey(widget, buffer, keyCode, movementMetaState, event);
    }

    @Override
    public boolean onTouchEvent(TextView widget, Spannable buffer, MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();
            x += widget.getScrollX();
            y += widget.getScrollY();
            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);
            URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);
            if (link.length != 0) {
                if (action == MotionEvent.ACTION_UP) {
                    Uri uri = Uri.parse(link[0].getURL());
                    Context context = widget.getContext();
                    launchURI(uri, context);
                } else {
                    Selection.setSelection(buffer, buffer.getSpanStart(link[0]), buffer.getSpanEnd(link[0]));
                }
                return true;
            } else {
                Selection.removeSelection(buffer);
            }
        }
        return false;
    }

    public static void clearFileIDs() {
        file_ids.clear();
    }

    public static void addFileID(String url, String file_id) {
        file_ids.put(url, file_id);
    }

    public static void launchURI(Uri uri, Context context) {
        if(file_ids.containsKey(uri.toString())) {
            FetchFileIDTask task = new FetchFileIDTask();
            task.original_url = uri;
            task.context = context;
            task.execute(file_ids.get(uri.toString()));
            return;
        }

        launchBrowser(uri, context);
    }

    private static void launchBrowser(Uri uri, Context context) {
        if(!PreferenceManager.getDefaultSharedPreferences(IRCCloudApplication.getInstance().getApplicationContext()).getBoolean("browser", false) && uri.getScheme().startsWith("http") && CustomTabsHelper.getPackageNameToUse(context) != null) {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            builder.setToolbarColor(ColorScheme.getInstance().navBarColor);
            builder.addDefaultShareMenuItem();
            builder.addMenuItem("Copy URL", PendingIntent.getBroadcast(context, 0, new Intent(context, ChromeCopyLinkBroadcastReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT));
            CustomTabsIntent intent = builder.build();
            intent.intent.setData(uri);
            if (intent.startAnimationBundle != null) {
                context.startActivity(intent.intent, intent.startAnimationBundle);
            } else {
                context.startActivity(intent.intent);
            }
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if(!uri.toString().startsWith("irccloud-"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(context, "Unable to find an application to handle this URL scheme", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static Object FROM_BELOW = new NoCopySpan.Concrete();
}
