package com.swrve.sdk

import org.awaitility.Awaitility.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anySet
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

class CampaignsUpdateListenerTest : SwrveBaseTest() {

    private lateinit var swrveSpy: Swrve

    @Before
    override fun setUp() {
        super.setUp()
        swrveSpy = SwrveTestUtils.createSpyInstance()
        SwrveCommon.setSwrveCommon(swrveSpy)
        doNothing().`when`(swrveSpy).sendEventsInBackground(any(), anyString(), any())
    }

    @Test
    fun testListenerHeldWeakly() {
        // Deliberately an object expression rather than a lambda: a non-capturing lambda compiles to
        // a singleton, so it would not model a listener the caller can stop referencing.
        val listener = object : SwrveCampaignsUpdateListener {
            override fun onCampaignsUpdated() { /* no-op */ }
        }
        swrveSpy.setCampaignsUpdateListener(listener)

        assertNotNull("listener should be wrapped in a weak reference", swrveSpy.campaignsUpdateListener)
        assertEquals("weak reference should resolve to the listener", listener, swrveSpy.campaignsUpdateListener.get())
    }

    @Test
    fun testListenerClearedWithNull() {
        swrveSpy.setCampaignsUpdateListener { /* no-op */ }
        swrveSpy.setCampaignsUpdateListener(null)

        assertNull("passing null should clear the listener", swrveSpy.campaignsUpdateListener)
    }

    @Test
    fun testCollectedListenerIsNotCalled() {
        var callCount = 0
        swrveSpy.setCampaignsUpdateListener { callCount++ }

        // Simulates the listener having been collected, which is what a caller keeping no strong reference would eventually see.
        swrveSpy.campaignsUpdateListener.clear()
        swrveSpy.invokeCampaignsUpdateListener()

        assertEquals("a collected listener should not be called", 0, callCount)
    }

    @Test
    fun testListenerIsInvoked() {
        var callCount = 0
        swrveSpy.setCampaignsUpdateListener { callCount++ }

        swrveSpy.invokeCampaignsUpdateListener()

        assertEquals(1, callCount)
    }

    @Test
    fun testNotifiedOnceOnFirstRefresh() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, "{}") // no campaigns section
        swrveSpy.campaignsAndResourcesInitialized = false
        swrveSpy.init(mActivity)

        // firstRefreshFinished owns this: it fires even though the response carried nothing.
        verify(swrveSpy, times(1)).invokeCampaignsUpdateListener()
    }

    @Test
    fun testNotifiedAfterAssetsOnFreshInstall() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)

        // Hold the asset callback so assets are still "downloading", as on a fresh install.
        // The queue crosses threads: written from downloadAssetsExecutor, read from the test.
        val pending = ConcurrentLinkedQueue<SwrveAssetsCompleteCallback>()
        doAnswer { invocation ->
            pending.add(invocation.getArgument(1)); null
        }.`when`(swrveSpy.swrveAssetsManager).downloadAssets(anySet(), any(SwrveAssetsCompleteCallback::class.java))
        swrveSpy.downloadAssetsExecutor = Executors.newSingleThreadExecutor()

        val campaignsResponseJson = "{\"campaigns\":" + SwrveTestUtils.getAssetAsText(mActivity!!, "campaign_autoshow.json") + "}"
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.campaignsAndResourcesInitialized = false
        swrveSpy.init(mActivity)

        // firstRefreshFinished reports the initial load, before the assets exist.
        await().untilAsserted { verify(swrveSpy, times(1)).invokeCampaignsUpdateListener() }

        // It runs off downloadAssetsExecutor, so wait for the asset request itself — otherwise there
        // is nothing to complete below and the final assertion times out.
        await().until { pending.isNotEmpty() }

        // The one that matters: without it a fresh install shows an empty Message Center.
        generateSequence { pending.poll() }.forEach { it.complete(HashSet(), true) }
        await().untilAsserted { verify(swrveSpy, times(2)).invokeCampaignsUpdateListener() }
    }

    @Test
    fun testNotifiedWhenCampaignsRemoved() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, "{\"campaigns\":{}}")
        swrveSpy.campaignsAndResourcesInitialized = true // suppress the startup notification
        swrveSpy.init(mActivity)

        // This path returns before the asset download, so the parse itself must report it.
        verify(swrveSpy, times(1)).invokeCampaignsUpdateListener()
        assertTrue(swrveSpy.campaigns.isEmpty())
    }

    @Test
    fun testNotNotifiedByCacheLoadOrUnchangedRefresh() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, "{}")
        swrveSpy.campaignsAndResourcesInitialized = true // suppress the startup notification
        swrveSpy.init(mActivity) // loads campaigns from cache

        // A cache load is not a campaigns change, so nothing should be reported.
        verify(swrveSpy, never()).invokeCampaignsUpdateListener()

        swrveSpy.refreshContent(null)
        verify(swrveSpy, never()).invokeCampaignsUpdateListener() // unchanged response, still nothing
    }

    @Test
    fun testNotifiedOnRealTimeUserPropertiesChange() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, "{}")
        swrveSpy.campaignsAndResourcesInitialized = true // suppress the startup notification
        swrveSpy.init(mActivity)

        completeAssetsImmediately()

        // Cache campaigns first: the RTUP branch reloads from cache and does nothing if empty.
        val campaignsResponseJson = "{\"campaigns\":" + SwrveTestUtils.getAssetAsText(mActivity!!, "campaign_autoshow.json") + "}"
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.refreshContent(null)
        await().untilAsserted { verify(swrveSpy, times(1)).invokeCampaignsUpdateListener() }

        // Campaign JSON is unchanged, but personalization gates which campaigns are listable.
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, "{\"real_time_user_properties\": {\"swrve.language\": \"en-IE\"}}")
        swrveSpy.refreshContent(null)

        await().untilAsserted { verify(swrveSpy, times(2)).invokeCampaignsUpdateListener() }
    }

    @Test
    fun testNotifiedOnChangedCampaigns() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, "{}")
        swrveSpy.campaignsAndResourcesInitialized = true // suppress the startup notification
        swrveSpy.init(mActivity)
        verify(swrveSpy, never()).invokeCampaignsUpdateListener()

        completeAssetsImmediately()

        val campaignsResponseJson = "{\"campaigns\":" + SwrveTestUtils.getAssetAsText(mActivity!!, "campaign_autoshow.json") + "}"
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.refreshContent(null)

        // Lands on downloadAssetsExecutor, which runSingleThreaded does not make synchronous.
        await().untilAsserted { verify(swrveSpy, times(1)).invokeCampaignsUpdateListener() }
    }

    @Test
    fun testNotifiedWhenAssetDownloadFailsSha1() {
        SwrveTestUtils.runSingleThreaded(swrveSpy)
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, "{}")
        swrveSpy.campaignsAndResourcesInitialized = true // suppress the startup notification
        swrveSpy.init(mActivity)

        // sha1Verified = false, as when the platform's SHA-1 self-test fails and no asset is downloaded.
        completeAssetsImmediately(sha1Verified = false)

        val campaignsResponseJson = "{\"campaigns\":" + SwrveTestUtils.getAssetAsText(mActivity!!, "campaign_autoshow.json") + "}"
        SwrveTestUtils.setRestClientWithGetResponse(swrveSpy, campaignsResponseJson)
        swrveSpy.refreshContent(null)

        // Still reported: the snapshot was replaced before the download ran, so the listable set can have
        // changed even though nothing landed. Suppressing this leaves an open Message Center stale.
        await().untilAsserted { verify(swrveSpy, times(1)).invokeCampaignsUpdateListener() }
    }

    /** Runs the asset completion callback inline, and replaces the executor the harness shuts down. */
    private fun completeAssetsImmediately(sha1Verified: Boolean = true) {
        doAnswer { invocation ->
            invocation.getArgument<SwrveAssetsCompleteCallback>(1).complete(HashSet(), sha1Verified); null
        }.`when`(swrveSpy.swrveAssetsManager).downloadAssets(anySet(), any(SwrveAssetsCompleteCallback::class.java))
        swrveSpy.downloadAssetsExecutor = Executors.newSingleThreadExecutor()
    }
}
