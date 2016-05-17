package com.swrve.sdk;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SwrveTestUtils {

    public static String getAssetAsText(Context context, String assetName) {
        AssetManager assetManager = context.getAssets();
        InputStream in;
        String result = null;
        try {
            in = assetManager.open(assetName);
            assertNotNull(in);
            java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
            result = s.hasNext() ? s.next() : "";
            assertFalse(result.length() == 0);
        } catch (IOException ex) {
            SwrveLogger.e("SwrveSDKTest", "Error getting asset as text:" + assetName, ex);
            fail("Error getting asset as text:" + assetName);
        }
        return result;
    }

}
