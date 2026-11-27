package com.swrve.sdk.messaging

import android.view.GestureDetector
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.swrve.sdk.R
import com.swrve.sdk.Swrve
import com.swrve.sdk.SwrveBaseTest
import com.swrve.sdk.SwrveTestUtils
import com.swrve.sdk.config.SwrveConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SwrveMessageViewFitTest : SwrveBaseTest() {

    private lateinit var swrveSpy: Swrve

    @Before
    override fun setUp() {
        super.setUp()
        swrveSpy = SwrveTestUtils.createSpyInstance()
        SwrveTestUtils.initSwrve(swrveSpy, mActivity)
    }

    // The format in campaign_right_away.json is 320x240
    private fun createMessageView(gestureDetector: GestureDetector? = null): SwrveMessageView {
        SwrveTestUtils.loadCampaignsFromFile(mActivity!!, swrveSpy, "campaign_right_away.json", "1111111111111111111111111")
        val message = swrveSpy.getMessageForId(165)
        assertNotNull(message)
        return SwrveMessageView(ApplicationProvider.getApplicationContext(), SwrveConfig(), message, message.formats[0], null, 0, gestureDetector)
    }

    // campaign_access.json (format 1772x3150): element 3 is a template-sized video (1352x2002, centred), element 4 a
    // fill-screen one
    private fun createVideoMessageView(): SwrveMessageView {
        SwrveTestUtils.loadCampaignsFromFile(mActivity!!, swrveSpy, "campaign_access.json", "1111111111111111111111111", "3810f70fb3e5690ec0c04ac13f649322d12f0a4c")
        val message = swrveSpy.getMessageForId(582476)
        assertNotNull(message)
        val personalization = mapOf("test_1" to "TEST123", "user" to "Jose")
        return SwrveMessageView(mActivity!!, SwrveConfig(), message, message.formats[0], personalization, 0)
    }

    private fun layout(view: View, width: Int, height: Int) {
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
    }

    @Test
    fun testFitScaleIsOneWhenViewMatchesFormat() {
        val view = createMessageView()
        assertEquals(1f, view.getFitScale(320, 240), 0.0001f)
    }

    @Test
    fun testFitScaleTouchesTheNearestEdges() {
        val view = createMessageView()
        assertEquals(0.5f, view.getFitScale(160, 240), 0.0001f) // narrower than the format: fit the width
        assertEquals(2f, view.getFitScale(640, 480), 0.0001f) // larger in both: grow until the edges are reached
        assertEquals(1.5f, view.getFitScale(1000, 360), 0.0001f) // wider than the format: fit the height
    }

    @Test
    fun testFitScaleIgnoresAnUnmeasuredView() {
        val view = createMessageView()
        assertEquals(1f, view.getFitScale(0, 0), 0.0001f)
    }

    @Test
    fun testPageElementsLayoutIsFittedToCoverTheView() {
        for ((width, height, fit) in listOf(Triple(160, 120, 0.5f), Triple(640, 480, 2f), Triple(1000, 360, 1.5f))) {
            val view = createMessageView()
            layout(view, width, height)
            val elements = view.pageElementsLayout

            // Laid out at the view's size divided by the fit (rounded up, and by one more to centre exactly), centred, then
            // scaled by the fit about its centre
            assertEquals(fit, elements.scaleX, 0.0001f)
            assertEquals(fit, elements.scaleY, 0.0001f)
            assertEquals(width / fit, elements.width.toFloat(), 2f)
            assertEquals(height / fit, elements.height.toFloat(), 2f)
            assertEquals(width / 2f, (elements.left + elements.right) / 2f, 0.0001f)
            assertEquals(height / 2f, (elements.top + elements.bottom) / 2f, 0.0001f)
            assertFalse(elements.isPivotSet) // so the default pivot, its centre, is used
            // So once scaled it covers the view, by at most a layout pixel past each edge
            assertEquals(width.toFloat(), elements.width * elements.scaleX, 2 * fit)
            assertEquals(height.toFloat(), elements.height * elements.scaleY, 2 * fit)
            assertTrue(elements.width * elements.scaleX >= width - 0.01f)
            assertTrue(elements.height * elements.scaleY >= height - 0.01f)
        }
    }

    @Test
    fun testPageElementsLayoutCoversEveryEdgeAtUnevenFits() {
        // Sizes where the view divided by the fit isn't whole. Rounding it to the nearest pixel left the scaled layout
        // short of an edge by up to a pixel, showing a line of the background colour (a 1080x2364 folded Pixel Fold)
        for ((width, height) in listOf(1080 to 2364, 1079 to 2363, 2076 to 2152, 333 to 777, 641 to 479)) {
            val view = createMessageView()
            layout(view, width, height)
            val elements = view.pageElementsLayout

            // Centred exactly, so the default pivot is the view's centre
            assertEquals(width / 2f, elements.left + elements.width / 2f, 0.0001f)
            assertEquals(height / 2f, elements.top + elements.height / 2f, 0.0001f)
            // And once scaled it reaches every edge
            assertTrue(elements.width * elements.scaleX >= width - 0.01f)
            assertTrue(elements.height * elements.scaleY >= height - 0.01f)
        }
    }

    @Test
    fun testLayoutSizeToCover() {
        assertEquals(320, SwrveMessageView.layoutSizeToCover(160, 0.5f)) // an exact fit is left alone
        assertEquals(1382, SwrveMessageView.layoutSizeToCover(1080, 1080f / 1382f)) // an exact fit, even with float error
        assertEquals(1384, SwrveMessageView.layoutSizeToCover(1080, 1080f / 1383f)) // exactly 1383, but an odd overhang can't be centred
        assertEquals(702, SwrveMessageView.layoutSizeToCover(2364, 3.375f)) // 700.4 rounds up to 701, then 702 to centre
        assertEquals(3028, SwrveMessageView.layoutSizeToCover(2364, 1080f / 1383f)) // the folded Pixel Fold: 3027.2, up to 3028, not down to 3027
    }

    @Test
    fun testElementsKeepTheirTemplateLayoutAndOwnScale() {
        // At the format's own size the layout isn't scaled; note where each element sits relative to its centre
        val view = createMessageView()
        layout(view, 320, 240)
        val elements = view.pageElementsLayout
        assertTrue(elements.childCount > 0)
        val offsets = (0 until elements.childCount).map { i ->
            val child = elements.getChildAt(i)
            Pair((child.left + child.right) / 2f - elements.width / 2f, (child.top + child.bottom) / 2f - elements.height / 2f)
        }

        layout(view, 160, 120) // half the size of the format
        for (i in 0 until elements.childCount) {
            val child = elements.getChildAt(i)
            // Same place relative to the layout's centre: only the layout is scaled
            assertEquals(offsets[i].first, (child.left + child.right) / 2f - elements.width / 2f, 1f)
            assertEquals(offsets[i].second, (child.top + child.bottom) / 2f - elements.height / 2f, 1f)
            // Elements' own scale and pivot are left alone, so focus zoom animations work as before
            assertEquals(1f, child.scaleX, 0.0001f)
            assertEquals(1f, child.scaleY, 0.0001f)
            assertFalse(child.isPivotSet)
        }
    }

    @Test
    fun testGestureOverlayIsOutsidePageElementsLayoutAndNotScaled() {
        val gestureDetector = GestureDetector(mActivity, GestureDetector.SimpleOnGestureListener())
        val view = createMessageView(gestureDetector)
        layout(view, 160, 120) // half the size of the format

        // The overlay is added first, below the page elements layout, and keeps covering the whole view
        assertEquals(2, view.childCount)
        val overlay = view.getChildAt(0)
        assertSame(view.pageElementsLayout, view.getChildAt(1))
        assertEquals(1f, overlay.scaleX, 0.0001f)
        assertEquals(1f, overlay.scaleY, 0.0001f)
        assertEquals(0.5f, view.pageElementsLayout.scaleX, 0.0001f)
    }

    @Test
    fun testFillScreenVideoCoversTheViewAtAnyFit() {
        for ((width, height) in listOf(886 to 1575, 3544 to 6300, 1080 to 2400)) { // fit 0.5, 2 and ~0.61
            val view = createVideoMessageView()
            layout(view, width, height)
            val elements = view.pageElementsLayout
            val video = elements.getChildAt(4)
            assertTrue(video is SwrveVideoPlayerView)

            // Fills the elements layout, which is scaled to cover the view
            assertEquals(0, video.left)
            assertEquals(0, video.top)
            assertEquals(elements.width, video.width)
            assertEquals(elements.height, video.height)
            assertEquals(width.toFloat(), video.width * elements.scaleX, 1f)
            assertEquals(height.toFloat(), video.height * elements.scaleY, 1f)
        }
    }

    @Test
    fun testTemplateVideoKeepsItsPlaceAndSizeAtAnyFit() {
        for ((width, height) in listOf(886 to 1575, 3544 to 6300, 1080 to 2400)) { // fit 0.5, 2 and ~0.61
            val view = createVideoMessageView()
            layout(view, width, height)
            val elements = view.pageElementsLayout
            val video = elements.getChildAt(3)
            assertTrue(video is SwrveVideoPlayerView)
            val fit = view.getFitScale(width, height)

            // Template size and position (centred) inside the elements layout, with its own scale untouched
            assertEquals(1352, video.width)
            assertEquals(2002, video.height)
            assertEquals(elements.width / 2f, (video.left + video.right) / 2f, 1f)
            assertEquals(elements.height / 2f, (video.top + video.bottom) / 2f, 1f)
            assertEquals(1f, video.scaleX, 0.0001f)
            // So on screen it is the template size times the fit, centred in the view
            assertEquals(1352 * fit, video.width * elements.scaleX, 1f)
            assertEquals(2002 * fit, video.height * elements.scaleY, 1f)
        }
    }

    @Test
    fun testTemplateVideoUsesTextureViewWhereSurfaceViewsDoNotScale() {
        assertEquals(R.layout.swrve_video_player_fit_texture, SwrveMessageView.videoPlayerLayout(false, 23))
        assertEquals(R.layout.swrve_video_player_fit, SwrveMessageView.videoPlayerLayout(false, 24))
        assertEquals(R.layout.swrve_video_player_zoom, SwrveMessageView.videoPlayerLayout(true, 23))
        assertEquals(R.layout.swrve_video_player_zoom, SwrveMessageView.videoPlayerLayout(true, 37))
    }
}
