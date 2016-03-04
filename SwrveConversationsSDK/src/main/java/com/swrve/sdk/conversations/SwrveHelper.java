package com.swrve.sdk.conversations;

import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Used internally to provide MD5, token generation and other helper methods.
 */
public final class SwrveHelper {

    private static final String LOG_TAG = "SwrveSDK";

    public static boolean isNullOrEmpty(String val) {
        return (val == null || val.length() == 0);
    }

    public static String readStringFromInputStream(InputStream is) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));

        StringBuilder body = new StringBuilder();
        String line;

        while ((line = rd.readLine()) != null) {
            body.append(line);
        }

        return body.toString();
    }

    public static boolean hasFileAccess(String filePath) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            return true;
        }
        File file = new File(filePath);
        if (file.canRead()) {
            return true;
        } else {
            return false;
        }
    }
}
