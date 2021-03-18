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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

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
        assertThat(nextIntent.getAction(), equalTo(Intent.ACTION_VIEW));
        assertThat(nextIntent.getData().toString(), equalTo("tel:0123456789"));
    }

    @Test
    public void testOpenIntentWebView() {
        SwrveIntentHelper.openIntentWebView(Uri.parse("www.google.com"), mainActivity, "some_referrer");
        ShadowActivity shadowMainActivity = Shadows.shadowOf(mainActivity);
        Intent nextIntent = shadowMainActivity.peekNextStartedActivityForResult().intent;
        assertThat(nextIntent.getAction(), equalTo(Intent.ACTION_VIEW));
        assertThat(nextIntent.getData().toString(), equalTo("www.google.com"));
        Bundle bundle = nextIntent.getBundleExtra(Browser.EXTRA_HEADERS);
        assertNotNull(bundle);
        assertThat(bundle.getString("referrer"), equalTo("some_referrer"));
    }

    @Test
    public void testOpenDeepLink() {
        SwrveIntentHelper.openDeepLink(mainActivity, "www.google.com");
        ShadowActivity shadowMainActivity = Shadows.shadowOf(mainActivity);
        Intent nextIntent = shadowMainActivity.peekNextStartedActivityForResult().intent;
        assertThat(nextIntent.getAction(), equalTo(Intent.ACTION_VIEW));
        assertThat(nextIntent.getData().toString(), equalTo("www.google.com"));
    }
}
