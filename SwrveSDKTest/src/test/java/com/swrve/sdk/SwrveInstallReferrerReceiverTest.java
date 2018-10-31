package com.swrve.sdk;

import android.content.Intent;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SwrveInstallReferrerReceiverTest extends SwrveBaseTest {

    private Swrve swrveSpy;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Swrve swrveReal = (Swrve) SwrveSDK.createInstance(RuntimeEnvironment.application, 1, "apiKey");
        swrveSpy = Mockito.spy(swrveReal);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveReal, swrveSpy); // disable token registration
        SwrveTestUtils.setSDKInstance(swrveSpy);
        SwrveTestUtils.disableAssetsManager(swrveSpy);
        swrveSpy.init(mActivity);
    }

    @Test
    public void testSwrveInstallReferrerReceiver() throws Exception {
        SharedPreferences settings = mActivity.getSharedPreferences(swrveSpy.SDK_PREFS_NAME, 0);
        settings.edit().remove("swrve.referrer_id").apply();

        SwrveInstallReferrerReceiver receiver1 = new SwrveInstallReferrerReceiver();
        receiver1.onReceive(mActivity, new Intent());
        assertNull(settings.getString("swrve.referrer_id", null));

        SwrveInstallReferrerReceiver receiver2 = new SwrveInstallReferrerReceiver();
        Intent intent = new Intent();
        intent.putExtra("referrer", "http://www.somewebsite.com/blah1 234");
        receiver2.onReceive(mActivity, intent);
        assertEquals("http://www.somewebsite.com/blah1 234", settings.getString("swrve.referrer_id", null));
    }

    @Test
    public void testReferrerUserUpdateCalled() throws Exception {
        SharedPreferences settings = mActivity.getSharedPreferences(swrveSpy.SDK_PREFS_NAME, 0);

        settings.edit().remove("swrve.referrer_id").apply();
        swrveSpy.initialised = false;
        swrveSpy.onCreate(mActivity);
        Mockito.verify(swrveSpy, Mockito.never()).userUpdate(Mockito.anyMap());

        settings.edit().putString("swrve.referrer_id", "some referrer text").apply();
        swrveSpy.initialised = false;
        swrveSpy.onCreate(mActivity);
        ArgumentCaptor<Map> attributesCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(swrveSpy, Mockito.atLeastOnce()).userUpdate(attributesCaptor.capture());

        Mockito.reset(swrveSpy);
        SwrveTestUtils.disableBeforeSendDeviceInfo(swrveSpy, swrveSpy); // disable token registration
        swrveSpy.initialised = false;
        swrveSpy.onCreate(mActivity); // calling init again should NOT increment counter
        Mockito.verify(swrveSpy, Mockito.never()).userUpdate(Mockito.anyMap());
    }
}
