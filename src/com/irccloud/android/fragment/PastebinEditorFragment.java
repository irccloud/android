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

package com.irccloud.android.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.github.fge.uritemplate.URITemplate;
import com.github.fge.uritemplate.vars.VariableMap;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;

public class PastebinEditorFragment extends DialogFragment implements NetworkConnection.IRCEventHandler{

    public interface PastebinEditorListener {
        void onPastebinFailed(String pastecontents);
        void onPastebinSaved();
        void onPastebinSendAsText(String text);
        void onPastebinCancelled(String pastecontents);
    }

    private class FetchPastebinTask extends AsyncTaskEx<Void, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                URITemplate uri_template = new URITemplate(NetworkConnection.getInstance().config.getString("pastebin_uri_template"));
                return NetworkConnection.getInstance().fetchJSON(uri_template.toString(VariableMap.newBuilder().addScalarValue("id", pasteID).addScalarValue("type", "json").freeze()));
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject o) {
            if(o != null) {
                try {
                    paste.setText(o.getString("body"));
                    filename.setText(o.getString("name"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private EditText paste;
    private EditText filename;
    private EditText message;
    private int pastereqid = -1;
    public String pastecontents;
    public String pasteID;
    public PastebinEditorListener listener;

    public PastebinEditorFragment() {
        // Required empty public constructor
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        NetworkConnection.getInstance().addHandler(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setInverseBackgroundForced(Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB);
        View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_pastebinprompt, null);
        paste = (EditText)v.findViewById(R.id.paste);
        filename = (EditText)v.findViewById(R.id.filename);
        message = (EditText)v.findViewById(R.id.message);

        paste.setText(pastecontents);
        builder.setView(v);
        builder.setTitle("Pastebin");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(listener != null)
                    listener.onPastebinCancelled(pastecontents);
                dialog.cancel();
            }
        });
        if(pasteID != null) {
            message.setVisibility(View.GONE);
            v.findViewById(R.id.message_prompt).setVisibility(View.GONE);
            builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    pastecontents = paste.getText().toString();
                    pastereqid = NetworkConnection.getInstance().edit_paste(pasteID, filename.getText().toString(), extension(), pastecontents);
                }
            });
            new FetchPastebinTask().execute((Void)null);
        } else {
            builder.setPositiveButton("Pastebin", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    pastecontents = paste.getText().toString();
                    pastereqid = NetworkConnection.getInstance().paste(filename.getText().toString(), extension(), pastecontents);
                }
            });
            builder.setNeutralButton("Send as Text", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (listener != null)
                        listener.onPastebinSendAsText(paste.getText().toString());
                    NetworkConnection.getInstance().removeHandler(PastebinEditorFragment.this);
                    dialog.dismiss();
                }
            });
        }
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                if (listener != null)
                    listener.onPastebinCancelled(pastecontents);
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.setOwnerActivity(getActivity());
        return dialog;
    }

    private String extension() {
        try {
            if (filename != null && filename.getText() != null && filename.getText().length() > 0) {
                String file = filename.getText().toString();
                if (file.contains(".")) {
                    String extension = file.substring(file.lastIndexOf(".") + 1);
                    if (extension.length() > 0)
                        return extension;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "txt";
    }

    @Override
    public void onIRCEvent(final int msg, Object obj) {
        final IRCCloudJSONObject event;
        switch (msg) {
            case NetworkConnection.EVENT_FAILURE_MSG:
                event = (IRCCloudJSONObject) obj;
                if (event != null && event.has("_reqid")) {
                    Log.e("IRCCloud", "Pastebin Error: " + obj.toString());
                    int reqid = event.getInt("_reqid");
                    if (reqid == pastereqid) {
                        if(listener != null)
                            listener.onPastebinFailed(pastecontents);
                        NetworkConnection.getInstance().removeHandler(this);
                    }
                }
                break;
            case NetworkConnection.EVENT_SUCCESS:
                event = (IRCCloudJSONObject) obj;
                if (event != null && event.has("_reqid")) {
                    int reqid = event.getInt("_reqid");
                    if (reqid == pastereqid) {
                        if(listener != null) {
                            if(pasteID != null) {
                                listener.onPastebinSaved();
                            } else {
                                if (message != null && message.getText() != null && message.getText().length() > 0)
                                    listener.onPastebinSendAsText(message.getText() + " " + event.getString("url"));
                                else
                                    listener.onPastebinSendAsText(event.getString("url"));
                            }
                        }
                        NetworkConnection.getInstance().removeHandler(this);
                    }
                }
                break;
            default:
                break;
        }
    }
}
