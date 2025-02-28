package com.swrve.sdk.test

import android.content.Intent
import android.net.Uri
import android.provider.Browser
import com.swrve.sdk.SwrveBaseTest
import com.swrve.sdk.SwrveIntentHelper
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.Shadows

class SwrveIntentHelperTest : SwrveBaseTest() {
    private var mainActivity: MainActivity? = null

    @Before
    override fun setUp() {
        mainActivity =
            Robolectric.buildActivity(MainActivity::class.java).create().start().resume().visible().get()
    }

    @Test
    fun testOpenDialer() {
        SwrveIntentHelper.openDialer(Uri.parse("tel:0123456789"), mainActivity)
        val shadowMainActivity = Shadows.shadowOf(mainActivity)
        val nextIntent = shadowMainActivity.peekNextStartedActivityForResult().intent
        Assert.assertEquals(nextIntent.action, (Intent.ACTION_VIEW))
        Assert.assertEquals(nextIntent.data.toString(), ("tel:0123456789"))
    }

    @Test
    fun testOpenIntentWebView() {
        SwrveIntentHelper.openIntentWebView(
            Uri.parse("www.google.com"),
            mainActivity,
            "some_referrer"
        )
        val shadowMainActivity = Shadows.shadowOf(mainActivity)
        val nextIntent = shadowMainActivity.peekNextStartedActivityForResult().intent
        Assert.assertEquals(nextIntent.action, (Intent.ACTION_VIEW))
        Assert.assertEquals(nextIntent.data.toString(), ("www.google.com"))
        val bundle = nextIntent.getBundleExtra(Browser.EXTRA_HEADERS)
        Assert.assertNotNull(bundle)
        Assert.assertEquals(bundle!!.getString("referrer"), ("some_referrer"))
    }

    @Test
    fun testOpenDeepLink() {
        SwrveIntentHelper.openDeepLink(mainActivity, "www.google.com", null)
        val shadowMainActivity = Shadows.shadowOf(mainActivity)
        val nextIntent = shadowMainActivity.peekNextStartedActivityForResult().intent
        Assert.assertEquals(nextIntent.action, (Intent.ACTION_VIEW))
        Assert.assertEquals(nextIntent.data.toString(), ("www.google.com"))
    }
}
