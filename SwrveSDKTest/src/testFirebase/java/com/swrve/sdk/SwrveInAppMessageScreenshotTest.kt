package com.swrve.sdk

import android.graphics.Typeface
import android.text.TextUtils
import android.view.View
import com.swrve.sdk.messaging.SwrveButtonTextImageView
import com.swrve.sdk.messaging.SwrveButtonView
import com.swrve.sdk.messaging.SwrveImageView
import com.swrve.sdk.messaging.SwrveTextView
import com.swrve.sdk.messaging.SwrveThemedMaterialButton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.annotation.GraphicsMode

// The kinds of element a message is made of, one campaign each, made in the dashboard and shown on a tall phone. Each
// fixture is what the server sent for this screen, with a trigger added.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SwrveInAppMessageScreenshotTest : SwrveBaseScreenshotTest() {

    // https://dashboard.swrve.com/apps/32032/campaigns/749297: the background button, an image, two image buttons, and an
    // image button with text, which draws just its text, using its image for the size
    @Test
    fun testImages() {
        val elements = showElements("campaign_screenshot_images.json", "show_images_message",
            BACKGROUND, IMAGES_IMAGE, IMAGES_ICON_BUTTON, IMAGES_BUTTON, IMAGES_TEXT_BUTTON)
        assertEquals(listOf(SwrveButtonView::class, SwrveImageView::class, SwrveButtonView::class, SwrveButtonView::class,
            SwrveButtonTextImageView::class), elements.map { it::class })
        assertEquals("Dismiss text button", (elements[4] as SwrveButtonTextImageView).text)
        capture()
    }

    // https://dashboard.swrve.com/apps/32032/campaigns/749300: the background button and five multiline texts: a custom font,
    // a paragraph that doesn't scroll, the same with scrolling, centred, and right aligned
    @Test
    fun testText() {
        val elements = showElements("campaign_screenshot_text.json", "show_text_message", BACKGROUND, FONT)
        val (customFont, resize, scroll) = elements.drop(1).take(3).map { it as SwrveTextView }
        assertNotEquals(Typeface.DEFAULT, customFont.typeface)
        // Set to the same size: the text that doesn't scroll shrinks to fit, and the text that scrolls keeps its size and overflows
        assertTrue("${resize.textSize} < ${scroll.textSize}", resize.textSize < scroll.textSize)
        assertFalse(resize.canScrollVertically(1))
        assertTrue(scroll.canScrollVertically(1))
        capture()
    }

    // https://dashboard.swrve.com/apps/32032/campaigns/749305: the background button and six themed buttons: solid, outlined,
    // truncated, shrunk to fit, a custom font, and an image background with another image for pressed
    @Test
    fun testButtons() {
        val elements = showElements("campaign_screenshot_buttons.json", "show_buttons_message",
            BACKGROUND, FONT, BUTTON_IMAGE, BUTTON_IMAGE_PRESSED)
        val buttons = elements.drop(1).map { it as SwrveThemedMaterialButton }
        val (truncated, shrinks, customFont) = buttons.drop(2).take(3)
        // Set to the same size: a truncated label ends with an ellipsis, and the other shrinks to fit
        assertEquals(TextUtils.TruncateAt.END, truncated.ellipsize)
        assertNull(shrinks.ellipsize)
        assertTrue("${shrinks.textSize} < ${truncated.textSize}", shrinks.textSize < truncated.textSize)
        assertNotEquals(Typeface.DEFAULT, customFont.typeface)
        capture()
    }

    private lateinit var activity: SwrveInAppMessageActivity

    // Shows the campaign's message and returns its elements in the order the message view draws them
    private fun showElements(campaignFile: String, event: String, vararg assets: String): List<View> {
        activity = startMessage(PHONE, campaignFile, event, *assets).visible().get()
        val view = SwrveTestUtils.getSwrveMessageView(activity)
        assertNotNull(view)
        val layout = view!!.pageElementsLayout
        return (0 until layout.childCount).map { layout.getChildAt(it) }
    }

    private fun capture() = activity.window.decorView.captureScreenshot()

    // The campaigns' images, named as the CDN names them, by SHA-1, and their font
    private companion object {
        const val BACKGROUND = "23655c3b947ef1c20e3e4c0bdcb25894a304154f"
        const val FONT = "535.32032.3d0b9eb17e645ac34f83c49c3f988372b242173a.ttf" // Pacifico. SIL Open Font License, see Pacifico-OFL.txt
        const val IMAGES_IMAGE = "20ac590e0659bb24180e638926e8d6bebd0aa462"
        const val IMAGES_ICON_BUTTON = "f7143801f799389f3b7bade844e2c66082ee757e"
        const val IMAGES_BUTTON = "2ab2c5e05eb0f73d8da1609d968219287a1b7077"
        const val IMAGES_TEXT_BUTTON = "c315046f25f33965e02ef9853acda2438a021cc3"
        const val BUTTON_IMAGE = "ae22d321d98bf420693d766cc0d52aec70fdeff3"
        const val BUTTON_IMAGE_PRESSED = "b9223ad2346bc79d5d73b2921a7a241907c1fadb"
    }
}
