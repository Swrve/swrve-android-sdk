package com.swrve.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;

import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.test.BuildConfig;
import com.swrve.sdk.test.MainActivity;
import com.swrve.sdk.utils.TestHttpServer;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowLog;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import fi.iki.elonen.NanoHTTPD;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk=21)
@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public abstract class SwrveBaseTest {

    protected static final String LOG_TAG = "SwrveBaseTest";

    protected Activity mActivity;
    protected ShadowActivity mShadowActivity;

    protected TestHttpServer httpEventServer;
    protected TestHttpServer httpContentServer;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        mActivity = Robolectric.buildActivity(MainActivity.class).create().visible().get();
        mShadowActivity = Shadows.shadowOf(mActivity);
    }

    @After
    public void tearDown() throws Exception {
        if (httpEventServer != null) {
            httpEventServer.stop();
        }
        if (httpContentServer != null) {
            httpContentServer.stop();
        }
    }

    protected void initLocalHttpServer(final Context ctx, SwrveConfig config) {
        httpEventServer = new TestHttpServer(8085);
        httpEventServer.setDefaultResponse(NanoHTTPD.Response.Status.OK, "{}");
        try {
            httpEventServer.start();
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Error starting event server", e);
        }

        httpContentServer = new TestHttpServer(8083);
        httpContentServer.setDefaultResponse(NanoHTTPD.Response.Status.OK, "{}");
        try {
            httpContentServer.start();
            httpContentServer.setHandler("/cdn/", new TestHttpServer.ITestHttpServerHandler() {
                @Override
                public NanoHTTPD.Response serve(String uri, NanoHTTPD.Method method, Map<String, String> header, Map<String, String> params) {
                    try {
                        AssetManager assetManager = ctx.getAssets();
                        InputStream in = assetManager.open(uri.replace("/cdn/", ""));
                        return new NanoHTTPD.Response(NanoHTTPD.Response.Status.OK, "image/png", in);
                    } catch (IOException e) {
                        SwrveLogger.e(LOG_TAG, "Error SwrveTestHelper.", e);
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Error starting http server", e);
        }

        try {
            config.setContentUrl(new URL("http://localhost:8083"));
            config.setEventsUrl(new URL("http://localhost:8085"));
        } catch (MalformedURLException e) {
            SwrveLogger.e(LOG_TAG, "Error SwrveTestHelper.", e);
        }
    }
}
