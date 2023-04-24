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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;

import com.google.android.material.tabs.TabLayout;
import com.irccloud.android.AsyncTaskEx;
import com.irccloud.android.ColorScheme;
import com.irccloud.android.FontAwesome;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.R;
import com.irccloud.android.data.OnErrorListener;
import com.irccloud.android.data.model.Pastebin;
import com.irccloud.android.fragment.EditConnectionFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class PastebinEditorActivity extends BaseActivity {
    private static HashMap<String, String> pastebinTypeExtensions = new HashMap<String, String>() {
        {
            put("ABAP", "abap");
            put("ABC", "abc");
            put("ActionScript", "as");
            put("ADA", "ada");
            put("Apache Conf", "htaccess");
            put("AsciiDoc", "asciidoc");
            put("Assembly x86", "asm");
            put("AutoHotKey", "ahk");
            put("BatchFile", "bat");
            put("Bro", "bro");
            put("C and C++", "cpp");
            put("C9Search", "c9search_results");
            put("Cirru", "cirru");
            put("Clojure", "clj");
            put("Cobol", "CBL");
            put("CoffeeScript", "coffee");
            put("ColdFusion", "cfm");
            put("C#", "cs");
            put("Csound Document", "csd");
            put("Csound", "orc");
            put("Csound Score", "sco");
            put("CSS", "css");
            put("Curly", "curly");
            put("D", "d");
            put("Dart", "dart");
            put("Diff", "diff");
            put("Dockerfile", "Dockerfile");
            put("Dot", "dot");
            put("Drools", "drl");
            put("Dummy", "dummy");
            put("DummySyntax", "dummy");
            put("Eiffel", "e");
            put("EJS", "ejs");
            put("Elixir", "ex");
            put("Elm", "elm");
            put("Erlang", "erl");
            put("Forth", "frt");
            put("Fortran", "f");
            put("FreeMarker", "ftl");
            put("Gcode", "gcode");
            put("Gherkin", "feature");
            put("Gitignore", "gitignore");
            put("Glsl", "glsl");
            put("Gobstones", "gbs");
            put("Go", "go");
            put("GraphQLSchema", "gql");
            put("Groovy", "groovy");
            put("HAML", "haml");
            put("Handlebars", "hbs");
            put("Haskell", "hs");
            put("Haskell Cabal", "cabal");
            put("haXe", "hx");
            put("Hjson", "hjson");
            put("HTML", "html");
            put("HTML (Elixir)", "eex");
            put("HTML (Ruby)", "erb");
            put("INI", "ini");
            put("Io", "io");
            put("Jack", "jack");
            put("Jade", "jade");
            put("Java", "java");
            put("JavaScript", "js");
            put("JSON", "json");
            put("JSONiq", "jq");
            put("JSP", "jsp");
            put("JSSM", "jssm");
            put("JSX", "jsx");
            put("Julia", "jl");
            put("Kotlin", "kt");
            put("LaTeX", "tex");
            put("LESS", "less");
            put("Liquid", "liquid");
            put("Lisp", "lisp");
            put("LiveScript", "ls");
            put("LogiQL", "logic");
            put("LSL", "lsl");
            put("Lua", "lua");
            put("LuaPage", "lp");
            put("Lucene", "lucene");
            put("Makefile", "Makefile");
            put("Markdown", "md");
            put("Mask", "mask");
            put("MATLAB", "matlab");
            put("Maze", "mz");
            put("MEL", "mel");
            put("MUSHCode", "mc");
            put("MySQL", "mysql");
            put("Nix", "nix");
            put("NSIS", "nsi");
            put("Objective-C", "m");
            put("OCaml", "ml");
            put("Pascal", "pas");
            put("Perl", "pl");
            put("pgSQL", "pgsql");
            put("PHP", "php");
            put("Pig", "pig");
            put("Powershell", "ps1");
            put("Praat", "praat");
            put("Prolog", "plg");
            put("Properties", "properties");
            put("Protobuf", "proto");
            put("Python", "py");
            put("R", "r");
            put("Razor", "cshtml");
            put("RDoc", "Rd");
            put("Red", "red");
            put("RHTML", "Rhtml");
            put("RST", "rst");
            put("Ruby", "rb");
            put("Rust", "rs");
            put("SASS", "sass");
            put("SCAD", "scad");
            put("Scala", "scala");
            put("Scheme", "scm");
            put("SCSS", "scss");
            put("SH", "sh");
            put("SJS", "sjs");
            put("Smarty", "smarty");
            put("snippets", "snippets");
            put("Soy Template", "soy");
            put("Space", "space");
            put("SQL", "sql");
            put("SQLServer", "sqlserver");
            put("Stylus", "styl");
            put("SVG", "svg");
            put("Swift", "swift");
            put("Tcl", "tcl");
            put("Tex", "tex");
            put("Plain Text", "txt");
            put("Textile", "textile");
            put("Toml", "toml");
            put("TSX", "tsx");
            put("Twig", "twig");
            put("Typescript", "ts");
            put("Vala", "vala");
            put("VBScript", "vbs");
            put("Velocity", "vm");
            put("Verilog", "v");
            put("VHDL", "vhd");
            put("Wollok", "wlk");
            put("XML", "xml");
            put("XQuery", "xq");
            put("YAML", "yaml");
            put("Django", "html");
        }
    };

    private class PastebinTypesAdapter extends BaseAdapter {
        private Activity ctx;

        private class PastebinType {
            String type;
            String extension;

            public PastebinType(String type, String extension) {
                this.type = type;
                this.extension = extension;
            }
        }

        private ArrayList<PastebinType> data;

        public PastebinTypesAdapter(Activity context) {
            ctx = context;
            data = new ArrayList<PastebinType>();

            for(String key : pastebinTypeExtensions.keySet()) {
                data.add(new PastebinType(key, pastebinTypeExtensions.get(key)));
            }

            Collections.sort(data, new Comparator<PastebinType>() {
                @Override
                public int compare(PastebinType pastebinType, PastebinType t1) {
                    return pastebinType.type.compareTo(t1.type);
                }
            });
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int pos) {
            return data.get(pos);
        }

        @Override
        public long getItemId(int pos) {
            return pos;
        }

        public int getPosition(String extension) {
            for(int i = 0; i < data.size(); i++) {
                if (data.get(i).extension.equalsIgnoreCase(extension))
                    return i;
            }
            return -1;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            TextView label;

            if (row == null) {
                LayoutInflater inflater = ctx.getLayoutInflater();
                row = inflater.inflate(android.R.layout.simple_spinner_item, null);

                label = row.findViewById(android.R.id.text1);
                row.setTag(label);
            } else {
                label = (TextView) row.getTag();
            }

            label.setText(data.get(position).type);

            return row;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            TextView label;

            if (row == null) {
                LayoutInflater inflater = ctx.getLayoutInflater();
                row = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, null);
                label = row.findViewById(android.R.id.text1);
                row.setTag(label);
            } else {
                label = (TextView) row.getTag();
            }

            label.setText(data.get(position).type);

            return row;
        }
    }

    PastebinTypesAdapter adapter;


    private OnErrorListener<Pastebin> pastebinOnErrorListener = new OnErrorListener<Pastebin>() {
        @Override
        public void onSuccess(Pastebin object) {
            result(RESULT_OK);
            finish();
        }

        @Override
        public void onFailure(Pastebin object) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(PastebinEditorActivity.this, "Unable to save text snippet, please try again shortly.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private class FetchPastebinTask extends AsyncTaskEx<String, Void, Pastebin> {

        @Override
        protected Pastebin doInBackground(String... params) {
            try {
                return Pastebin.fetch(params[0]);
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(Pastebin p) {
            if (p != null) {
                String extension = p.getExtension();
                pastebin = p;
                paste.setText(p.getBody());
                filename.setText(p.getName());
                pastebin.setExtension(extension);
                pastebinType.setSelection(adapter.getPosition(pastebin.getExtension()));
            }
        }
    }

    private EditText paste;
    private EditText filename;
    private Spinner pastebinType;
    private EditText message;
    private TextView messages_count;
    private Pastebin pastebin = new Pastebin();
    private int current_tab = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(ColorScheme.getDialogWhenLargeTheme(ColorScheme.getUserTheme()));
        onMultiWindowModeChanged(isMultiWindow());

        setContentView(R.layout.activity_pastebineditor);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        if(getSupportActionBar() != null) {
            if(!getWindow().isFloating()) {
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }

        paste = findViewById(R.id.paste);
        filename = findViewById(R.id.filename);
        filename.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                pastebin.setExtension(extension());
                pastebinType.setSelection(adapter.getPosition(extension()));
            }
        });
        pastebinType = findViewById(R.id.pastebinType);
        adapter = new PastebinTypesAdapter(this);
        pastebinType.setAdapter(adapter);
        pastebinType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pastebin.setExtension(((PastebinTypesAdapter.PastebinType)adapter.getItem(position)).extension);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        message = findViewById(R.id.message);
        messages_count = findViewById(R.id.messages_count);
        final TabLayout tabHost = findViewById(android.R.id.tabhost);

        if (savedInstanceState != null && savedInstanceState.containsKey("message"))
            message.setText(savedInstanceState.getString("message"));

        if (savedInstanceState != null && savedInstanceState.containsKey("pastebin"))
            pastebin = (Pastebin)savedInstanceState.getSerializable("pastebin");
        else if (getIntent() != null && getIntent().hasExtra("paste_id"))
            pastebin.setId(getIntent().getStringExtra("paste_id"));

        if (getIntent() != null && getIntent().hasExtra("paste_contents"))
            pastebin.setBody(getIntent().getStringExtra("paste_contents"));

        paste.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(tabHost.getSelectedTabPosition() == 1) {
                    int count = 0;
                    String lines[] = editable.toString().split("\n");
                    for (String line : lines) {
                        count += Math.ceil(line.length() / 1080.0f);
                    }
                    messages_count.setText("Text will be sent as " + count + " message" + (count == 1 ? "" : "s"));
                }
                invalidateOptionsMenu();
            }
        });
        paste.setText(pastebin.getBody());

        if (getIntent() != null && getIntent().hasExtra("filename"))
            filename.setText(getIntent().getStringExtra("filename"));

        ViewCompat.setElevation(toolbar, ViewCompat.getElevation(tabHost));

        if (pastebin.getId() != null) {
            tabHost.setVisibility(View.GONE);
            message.setVisibility(View.GONE);
            findViewById(R.id.message_heading).setVisibility(View.GONE);
        } else {
            tabHost.setTabGravity(TabLayout.GRAVITY_FILL);
            tabHost.setTabMode(TabLayout.MODE_FIXED);
            tabHost.addTab(tabHost.newTab().setText("Snippet"));
            tabHost.addTab(tabHost.newTab().setText("Messages"));
            tabHost.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    current_tab = tab.getPosition();
                    if (current_tab == 0) {
                        filename.setVisibility(View.VISIBLE);
                        message.setVisibility(View.VISIBLE);
                        messages_count.setText("Text snippets are visible to anyone with the URL but are not publicly listed or indexed.");
                        messages_count.setVisibility(View.VISIBLE);
                        findViewById(R.id.filename_heading).setVisibility(View.VISIBLE);
                        findViewById(R.id.message_heading).setVisibility(View.VISIBLE);
                    } else {
                        filename.setVisibility(View.GONE);
                        message.setVisibility(View.GONE);
                        int count = 0;
                        String lines[] = paste.getText().toString().split("\n");
                        for(String line : lines) {
                            count += Math.ceil(line.length() / 1080.0f);
                        }
                        messages_count.setText("Text will be sent as " + count + " message" + (count==1?"":"s"));
                        messages_count.setVisibility(View.VISIBLE);
                        findViewById(R.id.filename_heading).setVisibility(View.GONE);
                        findViewById(R.id.message_heading).setVisibility(View.GONE);
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });
            if (savedInstanceState != null && savedInstanceState.containsKey("tab"))
                tabHost.getTabAt(savedInstanceState.getInt("tab")).select();
            pastebinType.setSelection(adapter.getPosition(pastebin.getExtension()));
        }

        NetworkConnection.getInstance().addHandler(this);
        if (pastebin.getId() != null && (pastebin.getBody() == null || pastebin.getBody().length() == 0)) {
            new FetchPastebinTask().execute(pastebin.getId());
        }

        if(pastebin.getId() != null) {
            setTitle(R.string.title_activity_pastebin_editor_edit);
        } else {
            setTitle(R.string.title_activity_pastebin_editor);
        }

        supportInvalidateOptionsMenu();

        result(RESULT_CANCELED);
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
        NetworkConnection.getInstance().removeHandler(this);
        super.onDestroy();
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
            NetworkConnection.printStackTraceToCrashlytics(e);
        }
        return "txt";
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_pastebineditor, menu);
        if(pastebin.getId() != null) {
            menu.findItem(R.id.action_save).setVisible(true);
            menu.findItem(R.id.action_send).setVisible(false);
        } else {
            menu.findItem(R.id.action_save).setVisible(false);
            menu.findItem(R.id.action_send).setVisible(true);
        }
        setMenuColorFilter(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_save).setEnabled(paste.length() > 0);
        menu.findItem(R.id.action_send).setEnabled(paste.length() > 0);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_save:
                pastebin.setBody(paste.getText().toString());
                pastebin.setName(filename.getText().toString());
                pastebin.save(pastebinOnErrorListener);
                break;
            case R.id.action_send:
                pastebin.setBody(paste.getText().toString());
                if(current_tab == 0) {
                    pastebin.setName(filename.getText().toString());
                    pastebin.save(pastebinOnErrorListener);
                } else {
                    result(RESULT_OK);
                    finish();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void result(int resultCode) {
        Intent data = new Intent();
        data.putExtra("paste_contents", pastebin.getBody());
        data.putExtra("paste_id", pastebin.getId());
        data.putExtra("message", message.getText().toString());
        data.putExtra("url", pastebin.getUrl());
        setResult(resultCode, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        pastebin.setBody(paste.getText().toString());
        pastebin.setName(filename.getText().toString());
        outState.putSerializable("pastebin", pastebin);
        outState.putString("message", message.getText().toString());
        outState.putInt("tab", current_tab);
    }
}