package com.swrve.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;

import com.swrve.sdk.config.SwrveConfig;

import junit.framework.Assert;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.SSLEngineResult;

import fi.iki.elonen.NanoHTTPD;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SwrveTestUtils {

    protected static final String LOG_TAG = "SwrveSDKTest";

    public static TestHTTPServer httpEventServer;
    public static TestHTTPServer httpContentServer;

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

    public static void removeSingletonInstance() throws Exception {
        setSingletonInstance(null);
    }

    public static void setSingletonInstance(ISwrveBase instance) throws Exception {
        Field singleton = SwrveSDKBase.class.getDeclaredField("instance");
        singleton.setAccessible(true);
        singleton.set(null, instance);
    }

    public static SwrveConfig configToLocalServer() {
        SwrveConfig config = new SwrveConfig();
        try {
            config.setEventsUrl(new URL("http://localhost:8085"));
            config.setContentUrl(new URL("http://localhost:8083"));
        } catch(Exception exp) {
        }
        return config;
    }

    public static void startTestServers(final Context context) throws IOException {
        if (httpEventServer != null) {
            httpEventServer.clearHandlers();
        } else {
            httpEventServer = new TestHTTPServer(8085);
            httpEventServer.start();
        }
        httpEventServer.setDefaultResponse(NanoHTTPD.Response.Status.OK, "{}");


        if (httpContentServer != null) {
            httpContentServer.clearHandlers();
        } else {
            httpContentServer = new TestHTTPServer(8083);
            httpContentServer.start();
        }
        httpContentServer.setDefaultResponse(NanoHTTPD.Response.Status.OK, "{}");
        httpContentServer.setHandler("/cdn/", new TestHTTPServer.IRequestHandler() {
            @Override
            public NanoHTTPD.Response serve(String uri, NanoHTTPD.Method method, Map<String, String> header, Map<String, String> params) {
                try {
                    AssetManager assetManager = context.getAssets();
                    InputStream inputStream = assetManager.open(uri.replace("/cdn/", ""));
                    return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, "image/png", inputStream);
                } catch (IOException e) {
                    SwrveLogger.e(LOG_TAG, "Error SwrveTestHelper.", e);
                }
                return null;
            }
        });
    }

    public static void stopTestServers() {
        if (httpEventServer != null) {
            httpEventServer.stop();
            httpEventServer = null;
        }
        if (httpContentServer != null) {
            httpContentServer.stop();
            httpContentServer = null;
        }
    }

    public static JSONObject getEventWithName(LinkedHashMap<Long, String> events, String eventName) throws JSONException {
        Iterator<String> it = events.values().iterator();
        while(it.hasNext()) {
            String eventJson = it.next();
            JSONObject event = new JSONObject(eventJson);
            if (event.has("name") && event.get("name").equals(eventName)) {
                return event;
            }
        }
        return null;
    }
}
