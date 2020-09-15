/*
 * Copyright (c) 2016 IRCCloud, Ltd.
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

import android.annotation.TargetApi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

@TargetApi(8)
public class JSONFetcher extends HTTPFetcher {
    private static final String TAG = "JSONFetcher";
    private static final ObjectMapper mapper = new ObjectMapper();

    public JSONFetcher(URL uri) {
        super(uri);
        mURI = uri;
    }

    protected void onStreamConnected(InputStream is) throws Exception {
        if(isCancelled)
            return;

        if (is != null) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            is.close();
            if(os.size() > 0)
                onJSONParsed(mapper.readValue(os.toString("UTF-8"), JsonNode.class));
            else
                onFetchFailed();
        }
    }

    protected void onJSONParsed(JsonNode response) {

    }
}

