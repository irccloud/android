package com.irccloud.android;

import android.annotation.TargetApi;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.squareup.leakcanary.AbstractAnalysisResultService;
import com.squareup.leakcanary.AnalysisResult;
import com.squareup.leakcanary.HeapDump;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import static android.os.Build.VERSION_CODES.HONEYCOMB;
import static com.squareup.leakcanary.LeakCanary.leakInfo;
import static com.squareup.leakcanary.internal.LeakCanaryInternals.findNextAvailableHprofFile;
import static com.squareup.leakcanary.internal.LeakCanaryInternals.leakResultFile;
/**
 * Created by sam on 5/18/15.
 */
public class CrashlyticsLeakService extends AbstractAnalysisResultService {

    @TargetApi(HONEYCOMB) @Override
    protected final void onHeapAnalyzed(HeapDump heapDump, AnalysisResult result) {
        String leakInfo = leakInfo(this, heapDump, result, true);
        if (leakInfo.length() < 4000) {
            Log.d("LeakCanary", leakInfo);
        } else {
            String[] lines = leakInfo.split("\n");
            for (String line : lines) {
                Log.d("LeakCanary", line);
            }
        }

        if (result.failure == null && (!result.leakFound || result.excludedLeak)) {
            afterDefaultHandling(heapDump, result, leakInfo);
            return;
        }

        int maxStoredLeaks = getResources().getInteger(R.integer.__leak_canary_max_stored_leaks);
        File renamedFile = findNextAvailableHprofFile(maxStoredLeaks);

        if (renamedFile == null) {
            // No file available.
            Log.e("LeakCanary",
                    "Leak result dropped because we already store " + maxStoredLeaks + " leak traces.");
            afterDefaultHandling(heapDump, result, leakInfo);
            return;
        }

        heapDump = heapDump.renameFile(renamedFile);

        File resultFile = leakResultFile(renamedFile);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(resultFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(heapDump);
            oos.writeObject(result);
        } catch (IOException e) {
            Log.e("LeakCanary", "Could not save leak analysis result to disk", e);
            afterDefaultHandling(heapDump, result, leakInfo);
            return;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }

        afterDefaultHandling(heapDump, result, leakInfo);
    }

    protected void afterDefaultHandling(HeapDump heapDump, AnalysisResult result, String leakInfo) {
        if (!result.leakFound || result.excludedLeak) {
            return;
        }
        Crashlytics.log("*** Memory Leak ***");
        for(String s : leakInfo.split("\n")) {
            Crashlytics.log(s);
        }
        Crashlytics.logException(new Exception("Memory Leak Detected"));
    }
}