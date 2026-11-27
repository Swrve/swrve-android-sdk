package com.swrve.sdk

import android.os.Looper
import android.widget.FrameLayout
import com.swrve.sdk.messaging.SwrveInAppStoryView
import org.awaitility.Awaitility.await
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

// A story on page 2 of 3 with its progress bar half full: https://dashboard.swrve.com/apps/32032/campaigns/749307, as the
// server sent it for this screen, with a trigger and each page's page_duration added by hand
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SwrveInAppStoryScreenshotTest : SwrveBaseScreenshotTest() {

    @Test
    fun testStory() {
        // The bar's thread asks for redraws faster than native graphics can redraw an attached window, so the main looper
        // never idles. So the story moves on, and the bar stops, before the window is attached.
        val controller = startMessage(PHONE, "campaign_screenshot_story.json", "show_story_message",
            PAGE_1, PAGE_2, PAGE_3, FONT, BUTTON_IMAGE, BUTTON_IMAGE_PRESSED)
        val activity = controller.get()

        // Page 1 is very short and page 2 long, so the story moves on to page 2 and stays there
        await().pollInSameThread().atMost(Duration.ofSeconds(10)).until {
            shadowOf(Looper.getMainLooper()).idle() // the main looper can only be run from the test's thread
            activity.currentPageId == PAGE_2_ID
        }
        val container = activity.findViewById<FrameLayout>(R.id.swrve_iam_pager_container)
        val storyView = (0 until container.childCount).map { container.getChildAt(it) }.filterIsInstance<SwrveInAppStoryView>().single()
        // pauseAnimation interrupts the bar's thread without waiting for it, so wait for it to end, or it could still move
        // the bar after it's set below
        val animationThread = privateField("animationThread").get(storyView) as Thread?
        storyView.pauseAnimation()
        animationThread?.join(5000)
        assertFalse(animationThread?.isAlive ?: false)
        assertEquals(3, storyView.numberOfSegments)
        assertEquals(1, storyView.currentIndex)

        // How far the bar got depends on timing, so set it to halfway for the screenshot
        privateField("segmentProgress").setFloat(storyView, 50f)
        assertEquals(50, storyView.segmentProgress)

        controller.visible()
        assertNotNull(SwrveTestUtils.getSwrveMessageView(activity))
        activity.window.decorView.captureScreenshot()
    }

    // The story view has no setters for its progress, so the test reaches its fields directly
    private fun privateField(name: String) = SwrveInAppStoryView::class.java.getDeclaredField(name).apply { isAccessible = true }

    // The campaign's images, named as the CDN names them, by SHA-1, and its font
    private companion object {
        const val PAGE_2_ID = 917504L
        const val PAGE_1 = "5445f43c4913c2a99d236574d78aad40c340d6b9"
        const val PAGE_2 = "da8fc48b75d53687985bb347611110bd91ddb673"
        const val PAGE_3 = "f6cf17c0f36c135ecf11fd00da2a9485402eedd8"
        const val FONT = "535.32032.3d0b9eb17e645ac34f83c49c3f988372b242173a.ttf" // Pacifico. SIL Open Font License, see Pacifico-OFL.txt
        const val BUTTON_IMAGE = "ae22d321d98bf420693d766cc0d52aec70fdeff3"
        const val BUTTON_IMAGE_PRESSED = "b9223ad2346bc79d5d73b2921a7a241907c1fadb"
    }
}
