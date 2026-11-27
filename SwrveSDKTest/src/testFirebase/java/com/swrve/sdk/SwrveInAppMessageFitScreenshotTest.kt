package com.swrve.sdk

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.swrve.sdk.messaging.SwrveOrientation
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

// Messages on screens of different shapes. The campaign has the proportions of a template for phones, foldables and
// tablets: a 1500x2325 background round a 1000x1550 safe zone, which fills screens from near-square to tall.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SwrveInAppMessageFitScreenshotTest : SwrveBaseScreenshotTest() {

    @Test
    fun testTallPortrait() {
        // Fit 1080/1000 = 1.08, so the background is 1620x2511, cropped on every side
        checkMessage("w540dp-h1182dp-port-xhdpi", 1080, 2364, SwrveOrientation.Portrait)
    }

    @Test
    fun testTallLandscape() {
        // Fit 1080/1000 = 1.08, so the background is 2511x1620, cropped on every side
        checkMessage("w1182dp-h540dp-land-xhdpi", 2364, 1080, SwrveOrientation.Landscape)
    }

    @Test
    fun testNearSquarePortrait() {
        // Fit 2152/1550 = 1.388, so the background is 2083x3228, cropped on every side
        checkMessage("w1038dp-h1076dp-port-xhdpi", 2076, 2152, SwrveOrientation.Portrait)
    }

    @Test
    fun testNearSquareLandscape() {
        // Fit 2152/1550 = 1.388, so the background is 3228x2083, cropped on every side
        checkMessage("w1076dp-h1038dp-land-xhdpi", 2152, 2076, SwrveOrientation.Landscape)
    }

    private fun checkMessage(qualifiers: String, width: Int, height: Int, orientation: SwrveOrientation) {
        val activity = startMessage(qualifiers, "campaign_fit.json", "show_fit_message",
            "bc5f1b888921145eb25b37b9bd1fd0cbd3b52a82", "c8891dc537d4b0f36ccb16a34d989047af5b8a6e").visible().get()
        val view = SwrveTestUtils.getSwrveMessageView(activity)
        assertNotNull(view)
        val window = activity.window.decorView
        assertEquals(width, window.width)
        assertEquals(height, window.height)

        // The safe zone touches two edges and isn't cropped
        assertEquals(orientation, view!!.format.orientation)
        val safeZoneWidth = view.format.size.x * view.pageElementsLayout.scaleX
        val safeZoneHeight = view.format.size.y * view.pageElementsLayout.scaleY
        assertTrue("safe zone $safeZoneWidth x $safeZoneHeight", safeZoneWidth <= width + 0.5f && safeZoneHeight <= height + 0.5f)
        assertTrue("safe zone $safeZoneWidth x $safeZoneHeight", abs(safeZoneWidth - width) < 1 || abs(safeZoneHeight - height) < 1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        window.draw(Canvas(bitmap))
        assertArrayEquals(intArrayOf(0, 0, 0, 0), transparentStrips(bitmap)) // the background reaches every edge
        bitmap.recycle()

        window.captureScreenshot()
    }

    // The transparent pixels in from each edge (left, top, right, bottom), where the app would show through, measured along
    // the lines through the centre
    private fun transparentStrips(bitmap: Bitmap): IntArray {
        val centreX = bitmap.width / 2
        val centreY = bitmap.height / 2
        fun count(range: IntProgression, pixel: (Int) -> Int) = range.takeWhile { Color.alpha(pixel(it)) == 0 }.count()
        return intArrayOf(
            count(0 until bitmap.width) { bitmap.getPixel(it, centreY) },
            count(0 until bitmap.height) { bitmap.getPixel(centreX, it) },
            count(bitmap.width - 1 downTo 0) { bitmap.getPixel(it, centreY) },
            count(bitmap.height - 1 downTo 0) { bitmap.getPixel(centreX, it) }
        )
    }
}
