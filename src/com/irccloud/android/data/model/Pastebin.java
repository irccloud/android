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

package com.irccloud.android.data.model;

import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.text.format.DateUtils;

import com.damnhandy.uri.template.UriTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.irccloud.android.BR;
import com.irccloud.android.ColorFormatter;
import com.irccloud.android.IRCCloudJSONObject;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.data.OnErrorListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

public class Pastebin extends BaseObservable implements Serializable {
    private static HashMap<String, String> fileTypes = new HashMap<>();
    private static HashMap<String, String> fileTypesMap = new HashMap<String, String>() {{
        put("ABAP",        "abap");
        put("ActionScript","as");
        put("ADA",         "ada|adb");
        put("Apache Conf", "^htaccess|^htgroups|^htpasswd|^conf|htaccess|htgroups|htpasswd");
        put("AsciiDoc",    "asciidoc");
        put("Assembly_x86","asm");
        put("AutoHotKey",  "ahk");
        put("BatchFile",   "bat|cmd");
        put("C9Search",    "c9search_results");
        put("C/C++",       "cpp|c|cc|cxx|h|hh|hpp");
        put("Cirru",       "cirru|cr");
        put("Clojure",     "clj|cljs");
        put("Cobol",       "cbl|cob");
        put("CoffeeScript","coffee|cf|cson|^cakefile");
        put("ColdFusion",  "cfm");
        put("C#",          "cs");
        put("CSS",         "css");
        put("Curly",       "curly");
        put("D",           "d|di");
        put("Dart",        "dart");
        put("Diff",        "diff|patch");
        put("Dockerfile",  "^dockerfile");
        put("Dot",         "dot");
        put("Erlang",      "erl|hrl");
        put("EJS",         "ejs");
        put("Forth",       "frt|fs|ldr");
        put("FreeMarker",  "ftl");
        put("Gherkin",     "feature");
        put("Gitignore",   "^.gitignore");
        put("Glsl",        "glsl|frag|vert");
        put("Go",          "go");
        put("Groovy",      "groovy");
        put("HAML",        "haml");
        put("Handlebars",  "hbs|handlebars|tpl|mustache");
        put("Haskell",     "hs");
        put("haXe",        "hx");
        put("HTML",        "html|htm|xhtml");
        put("HTML (Ruby)", "erb|rhtml|html.erb");
        put("INI",         "ini|conf|cfg|prefs");
        put("Jack",        "jack");
        put("Jade",        "jade");
        put("Java",        "java");
        put("JavaScript",  "js|jsm");
        put("JSON",        "json");
        put("JSONiq",      "jq");
        put("JSP",         "jsp");
        put("JSX",         "jsx");
        put("Julia",       "jl");
        put("LaTeX",       "tex|latex|ltx|bib");
        put("LESS",        "less");
        put("Liquid",      "liquid");
        put("Lisp",        "lisp");
        put("LiveScript",  "ls");
        put("LogiQL",      "logic|lql");
        put("LSL",         "lsl");
        put("Lua",         "lua");
        put("LuaPage",     "lp");
        put("Lucene",      "lucene");
        put("Makefile",    "makefile|gnumakefile|ocamlmakefile|make");
        put("MATLAB",      "matlab");
        put("Markdown",    "md|markdown");
        put("MEL",         "mel");
        put("MySQL",       "mysql");
        put("MUSHCode",    "mc|mush");
        put("Nix",         "nix");
        put("Objective-C", "m|mm");
        put("OCaml",       "ml|mli");
        put("Pascal",      "pas|p");
        put("Perl",        "pl|pm");
        put("pgSQL",       "pgsql");
        put("PHP",         "php|phtml");
        put("Powershell",  "ps1");
        put("Prolog",      "plg|prolog");
        put("Properties",  "properties");
        put("Protobuf",    "proto");
        put("Python",      "py");
        put("R",           "r");
        put("RDoc",        "rd");
        put("RHTML",       "rhtml");
        put("Ruby",        "rb|ru|gemspec|rake|^guardfile|^rakefile|^gemfile");
        put("Rust",        "rs");
        put("SASS",        "sass");
        put("SCAD",        "scad");
        put("Scala",       "scala");
        put("Smarty",      "smarty|tpl");
        put("Scheme",      "scm|rkt");
        put("SCSS",        "scss");
        put("SH",          "sh|bash|bashrc");
        put("SJS",         "sjs");
        put("Space",       "space");
        put("snippets",    "snippets");
        put("Soy Template","soy");
        put("SQL",         "sql");
        put("Stylus",      "styl|stylus");
        put("SVG",         "svg");
        put("Tcl",         "tcl");
        put("Tex",         "tex");
        put("Text",        "txt");
        put("Textile",     "textile");
        put("Toml",        "toml");
        put("Twig",        "twig");
        put("Typescript",  "ts|typescript|str");
        put("Vala",        "vala");
        put("VBScript",    "vbs");
        put("Velocity",    "vm");
        put("Verilog",     "v|vh|sv|svh");
        put("XML",         "xml|rdf|rss|wsdl|xslt|atom|mathml|mml|xul|xbl");
        put("XQuery",      "xq");
        put("YAML",        "yaml|yml");
    }};

    private static final long serialVersionUID = 0L;

    private String id;
    private String name;
    private String url;
    private int lines;
    private Date date;
    private String date_formatted;
    private String body;
    private String extension;
    private boolean own_paste;

    private OnErrorListener<Pastebin> onSaveListener = null;
    private OnErrorListener<Pastebin> onDeleteListener = null;

    public Pastebin() {

    }

    public Pastebin(JSONObject o) throws JSONException {
        setId(o.getString("id"));
        setName(o.getString("name"));
        setBody(o.getString("body"));
        setExtension(o.getString("extension"));
        setLines(o.getInt("lines"));
        setOwn_paste(o.getBoolean("own_paste"));
        setDate(new Date(o.getLong("date") * 1000L));
        setUrl(UriTemplate.fromTemplate(ColorFormatter.pastebin_uri_template).set("id", id).set("name", name).expand());
    }

    @Bindable
    public String getFileType() {
        String fileType = "Text";
        if(extension != null && extension.length() > 0) {
            if (fileTypes.containsKey(extension.toLowerCase())) {
                fileType = fileTypes.get(extension.toLowerCase());
            } else {
                String lower = extension.toLowerCase();
                for(String type : fileTypesMap.keySet()) {
                    if(lower.matches(fileTypesMap.get(type))) {
                        fileType = type;
                    }
                }
                fileTypes.put(lower, fileType);
            }
        }
        return fileType;
    }

    @Bindable
    public String getMetadata() {
        return date_formatted + " • " + lines + " line" + (lines == 1?"":"s") + " • " + getFileType();
    }

    @Bindable
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
        notifyPropertyChanged(BR.id);
    }

    @Bindable
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        notifyPropertyChanged(BR.name);
    }

    @Bindable
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        notifyPropertyChanged(BR.url);
    }

    @Bindable
    public int getLines() {
        return lines;
    }

    public void setLines(int lines) {
        this.lines = lines;
        notifyPropertyChanged(BR.lines);
        notifyPropertyChanged(BR.metadata);
    }

    @Bindable
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
        this.date_formatted = DateUtils.getRelativeTimeSpanString(date.getTime(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS, 0).toString();
        notifyPropertyChanged(BR.date);
        notifyPropertyChanged(BR.metadata);
    }

    @Bindable
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
        notifyPropertyChanged(BR.body);
    }

    @Bindable
    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
        notifyPropertyChanged(BR.extension);
        notifyPropertyChanged(BR.metadata);
    }

    @Bindable
    public boolean isOwn_paste() {
        return own_paste;
    }

    public void setOwn_paste(boolean own_paste) {
        this.own_paste = own_paste;
        notifyPropertyChanged(BR.own_paste);
    }

    public static Pastebin fetch(String pasteID) throws IOException {
        try {
            JSONObject o = NetworkConnection.getInstance().fetchJSON(UriTemplate.fromTemplate(ColorFormatter.pastebin_uri_template).set("id", pasteID).set("type", "json").expand());
            if(o != null) {
                return new Pastebin(o);
            }
        } catch (JSONException e) {
            NetworkConnection.printStackTraceToCrashlytics(e);
        }
        return null;
    }

    public void save(OnErrorListener<Pastebin> onErrorListener) {
        this.onSaveListener = onErrorListener;

        NetworkConnection.IRCResultCallback callback = new NetworkConnection.IRCResultCallback() {
            @Override
            public void onIRCResult(IRCCloudJSONObject result) {
                if(result.getBoolean("success")) {
                    ObjectNode o = result.getJsonObject("paste");
                    if(o == null && result.has("id"))
                        o = (ObjectNode)result.getObject();
                    if(o != null) {
                        setId(o.get("id").asText());
                        setName(o.get("name").asText());
                        setBody(o.get("body").asText());
                        setExtension(o.get("extension").asText());
                        setLines(o.get("lines").asInt());
                        setOwn_paste(o.get("own_paste").asBoolean());
                        setDate(new Date(o.get("date").asLong() * 1000L));
                        setUrl(o.get("url").asText());
                    }
                    if(onSaveListener != null)
                        onSaveListener.onSuccess(Pastebin.this);
                } else {
                    android.util.Log.e("IRCCloud", "Paste failed: " + result.toString());
                    if(onSaveListener != null)
                        onSaveListener.onFailure(Pastebin.this);
                }
            }
        };

        if(id != null && id.length() > 0) {
            NetworkConnection.getInstance().edit_paste(id, name, extension, body, callback);
        } else {
            NetworkConnection.getInstance().paste(name, extension, body, callback);
        }
    }

    public void delete(OnErrorListener<Pastebin> onErrorListener) {
        this.onSaveListener = onErrorListener;

        NetworkConnection.getInstance().delete_paste(id, new NetworkConnection.IRCResultCallback() {
            @Override
            public void onIRCResult(IRCCloudJSONObject result) {
                if(result.getBoolean("success")) {
                    if(onDeleteListener != null)
                        onDeleteListener.onSuccess(Pastebin.this);
                } else {
                    android.util.Log.e("IRCCloud", "Paste delete failed: " + result.toString());
                    if(onDeleteListener != null)
                        onDeleteListener.onFailure(Pastebin.this);
                }
            }
        });
    }
}
