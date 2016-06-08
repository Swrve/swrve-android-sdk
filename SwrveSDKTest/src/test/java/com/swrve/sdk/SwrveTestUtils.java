package com.swrve.sdk;

import android.content.Context;
import android.content.res.AssetManager;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveMessageFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SwrveTestUtils {

    public static void removeSwrveSDKSingletonInstance() throws Exception{
        Field instance = SwrveSDKBase.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

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


    public static ISwrveCampaignManager getTestSwrveCampaignManager() {
        return new ISwrveCampaignManager() {
            @Override
            public Date getNow() {
                return new Date();
            }

            @Override
            public Date getInitialisedTime() {
                return new Date();
            }

            @Override
            public File getCacheDir() {
                return new File("");
            }

            @Override
            public Set<String> getAssetsOnDisk() {
                Set<String> set = new HashSet<>();
                set.add("asset1");
                return set;
            }

            @Override
            public SwrveConfigBase getConfig() {
                return new SwrveConfig();
            }

            @Override
            public String getAppStoreURLForApp(int appId) {
                return "";
            }

            @Override
            public void buttonWasPressedByUser(SwrveButton button) {
                // empty
            }

            @Override
            public void messageWasShownToUser(SwrveMessageFormat messageFormat) {
                // empty
            }
        };
    }
}
