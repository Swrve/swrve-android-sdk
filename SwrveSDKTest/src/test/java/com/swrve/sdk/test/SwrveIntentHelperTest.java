package com.swrve.sdk.test;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;

import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrveIntentHelper;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class SwrveIntentHelperTest extends SwrveBaseTest {

    private MainActivity mainActivity;

    @Before
    public void setUp() {
        mainActivity = Robolectric.buildActivity(MainActivity.class).create().start().resume().visible().get();
    }

    @Test
    public void testOpenDialer() {
        SwrveIntentHelper.openDialer(Uri.parse("tel:0123456789"), mainActivity);
        ShadowActivity shadowMainActivity = Shadows.shadowOf(mainActivity);
        Intent nextIntent = shadowMainActivity.peekNextStartedActivityForResult().intent;
        assertEquals(nextIntent.getAction(), (Intent.ACTION_VIEW));
        assertEquals(nextIntent.getData().toString(), ("tel:0123456789"));
    }

    @Test
    public void testOpenIntentWebView() {
        SwrveIntentHelper.openIntentWebView(Uri.parse("www.google.com"), mainActivity, "some_referrer");
        ShadowActivity shadowMainActivity = Shadows.shadowOf(mainActivity);
        Intent nextIntent = shadowMainActivity.peekNextStartedActivityForResult().intent;
        assertEquals(nextIntent.getAction(), (Intent.ACTION_VIEW));
        assertEquals(nextIntent.getData().toString(), ("www.google.com"));
        Bundle bundle = nextIntent.getBundleExtra(Browser.EXTRA_HEADERS);
        assertNotNull(bundle);
        assertEquals(bundle.getString("referrer"), ("some_referrer"));
    }

    @Test
    public void testOpenDeepLink() {
        SwrveIntentHelper.openDeepLink(mainActivity, "www.google.com");
        ShadowActivity shadowMainActivity = Shadows.shadowOf(mainActivity);
        Intent nextIntent = shadowMainActivity.peekNextStartedActivityForResult().intent;
        assertEquals(nextIntent.getAction(), (Intent.ACTION_VIEW));
        assertEquals(nextIntent.getData().toString(), ("www.google.com"));
    }
}
