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
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

@TargetApi(8)
public class OOBFetcher extends HTTPFetcher {
    private static final String TAG = "OOBFetcher";

    private int mBid;

    public OOBFetcher(URL uri, int bid) {
        super(uri);
        mURI = uri;
        mBid = bid;
    }

    public int getBid() {
        return mBid;
    }

    protected void onFetchComplete() {
        if(!isCancelled)
            NetworkConnection.getInstance().notifyHandlers(NetworkConnection.EVENT_OOB_END, OOBFetcher.this);
    }

    protected void onFetchFailed() {
        if(!isCancelled)
            NetworkConnection.getInstance().notifyHandlers(NetworkConnection.EVENT_OOB_FAILED, OOBFetcher.this);
    }

    protected void onStreamConnected(InputStream stream) throws Exception {
        if(isCancelled)
            return;

        long totalTime = System.currentTimeMillis();
        long totalParseTime = 0;
        long totalJSONTime = 0;
        long longestEventTime = 0;
        String longestEventType = "";

        NetworkConnection conn = NetworkConnection.getInstance();
        InputStreamReader reader = new InputStreamReader(stream);

        JsonParser parser = new ObjectMapper().getFactory().createParser(reader);

        if (parser.nextToken() == JsonToken.START_ARRAY && !isCancelled) {
            conn.cancel_idle_timer();
            //android.os.Debug.startMethodTracing("/sdcard/oob", 16*1024*1024);
            IRCCloudLog.Log(Log.DEBUG, TAG, "Beginning backlog...");
            Trace trace = null;
            try {
                trace = FirebasePerformance.getInstance().newTrace("parseOOB");
                trace.start();
            } catch (IllegalStateException e) {

            }
            synchronized (conn.parserLock) {
                conn.notifyHandlers(NetworkConnection.EVENT_OOB_START, mBid);
                int count = 0;
                while (parser.nextToken() == JsonToken.START_OBJECT) {
                    if (isCancelled) {
                        IRCCloudLog.Log(Log.DEBUG, TAG, "Backlog parsing cancelled");
                        return;
                    }
                    long time = System.currentTimeMillis();
                    JsonNode e = parser.readValueAsTree();
                    totalJSONTime += (System.currentTimeMillis() - time);
                    time = System.currentTimeMillis();
                    IRCCloudJSONObject o = new IRCCloudJSONObject(e);
                    try {
                        conn.parse_object(o);
                    } catch (Exception ex) {
                        IRCCloudLog.Log(Log.ERROR, TAG, "Unable to parse message type: " + o.type());
                        NetworkConnection.printStackTraceToCrashlytics(ex);
                        IRCCloudLog.LogException(ex);
                    }
                    long t = (System.currentTimeMillis() - time);
                    if (t > longestEventTime) {
                        longestEventTime = t;
                        longestEventType = o.type();
                    }
                    totalParseTime += t;
                    count++;
                    if(trace != null)
                        trace.incrementMetric("object", 1);
                }
                //android.os.Debug.stopMethodTracing();
                totalTime = (System.currentTimeMillis() - totalTime);
                if(trace != null)
                    trace.stop();
                IRCCloudLog.Log(Log.DEBUG, TAG, "Backlog complete: " + count + " events");
                IRCCloudLog.Log(Log.DEBUG, TAG, "JSON parsing took: " + totalJSONTime + "ms (" + (totalJSONTime / (float) count) + "ms / object)");
                IRCCloudLog.Log(Log.DEBUG, TAG, "Backlog processing took: " + totalParseTime + "ms (" + (totalParseTime / (float) count) + "ms / object)");
                IRCCloudLog.Log(Log.DEBUG, TAG, "Total OOB load time: " + totalTime + "ms (" + (totalTime / (float) count) + "ms / object)");
                IRCCloudLog.Log(Log.DEBUG, TAG, "Longest event: " + longestEventType + " (" + longestEventTime + "ms)");
                totalTime -= totalJSONTime;
                totalTime -= totalParseTime;
                IRCCloudLog.Log(Log.DEBUG, TAG, "Total non-processing time: " + totalTime + "ms (" + (totalTime / (float) count) + "ms / object)");
            }
            conn.schedule_idle_timer();
        } else {
            throw new Exception("Unexpected JSON response");
        }
        parser.close();
    }
}

