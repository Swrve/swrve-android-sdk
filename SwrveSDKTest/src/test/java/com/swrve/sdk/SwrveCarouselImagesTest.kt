package com.swrve.sdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SwrveCarouselImagesTest : SwrveBaseTest() {

    @Test
    fun testShapeFollowsTheFirstImage() {
        assertEquals(0.4f, SwrveCarouselImages.imageHeightRatio(0.4f))
        assertEquals(0.5f, SwrveCarouselImages.imageHeightRatio(0.5f))
        assertEquals(0.6f, SwrveCarouselImages.imageHeightRatio(0.6f))
    }

    @Test
    fun testShapeIsKeptBetweenThreeToOneAndThreeToTwo() {
        assertEquals(1f / 3f, SwrveCarouselImages.imageHeightRatio(0.1f))
        assertEquals(2f / 3f, SwrveCarouselImages.imageHeightRatio(1f))
        assertEquals(2f / 3f, SwrveCarouselImages.imageHeightRatio(1.5f))
    }

    @Test
    fun testShapeIsThreeToTwoWithoutAnImage() {
        assertEquals(2f / 3f, SwrveCarouselImages.imageHeightRatio(0f))
        assertEquals(2f / 3f, SwrveCarouselImages.imageHeightRatio(-1f))
        assertEquals(2f / 3f, SwrveCarouselImages.imageHeightRatio(Float.NaN))
    }

    @Test
    fun testImageOfTheSameShapeIsUnchanged() {
        val image = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        assertSame(image, SwrveCarouselImages.fill(image, 0.5f))
    }

    @Test
    fun testTallerImageIsCroppedFromTopAndBottom() {
        // red top quarter, green middle half, blue bottom quarter
        val image = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        for (y in 0 until 200) for (x in 0 until 100) {
            image.setPixel(x, y, if (y < 50) Color.RED else if (y < 150) Color.GREEN else Color.BLUE)
        }

        val filled = SwrveCarouselImages.fill(image, 0.5f)

        assertEquals(100, filled.width)
        assertEquals(50, filled.height)
        assertEquals(Color.GREEN, filled.getPixel(50, 0))
        assertEquals(Color.GREEN, filled.getPixel(50, 49))
    }

    @Test
    fun testWiderImageIsCroppedFromBothSides() {
        // red left quarter, green middle half, blue right quarter
        val image = Bitmap.createBitmap(400, 100, Bitmap.Config.ARGB_8888)
        for (y in 0 until 100) for (x in 0 until 400) {
            image.setPixel(x, y, if (x < 100) Color.RED else if (x < 300) Color.GREEN else Color.BLUE)
        }

        val filled = SwrveCarouselImages.fill(image, 0.5f)

        assertEquals(200, filled.width)
        assertEquals(100, filled.height)
        assertEquals(Color.GREEN, filled.getPixel(0, 50))
        assertEquals(Color.GREEN, filled.getPixel(199, 50))
    }

    @Test
    fun testPlaceholderTakesTheShapeWithATranslucentFillAndSymbol() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val placeholder = SwrveCarouselImages.placeholder(context, 0.5f)

        assertEquals(context.resources.displayMetrics.widthPixels, placeholder.width)
        assertEquals(placeholder.width / 2, placeholder.height)
        assertFillColour(placeholder.getPixel(0, 0))
        assertFillColour(placeholder.getPixel(placeholder.width - 1, placeholder.height - 1))
        // the icon's hills fill the lower middle of its frame
        val symbol = placeholder.getPixel(placeholder.width / 2, placeholder.height / 2 + placeholder.width / 50)
        assertTrue("no symbol drawn at the centre", Color.alpha(symbol) > Color.alpha(SwrveCarouselImages.PLACEHOLDER_FILL_COLOR))
    }

    @Test
    @Config(qualifiers = "w600dp-h900dp-xxhdpi") // 1800 pixels wide, as a large tablet
    fun testPlaceholderOnALargeScreenIsCapped() {
        val placeholder = SwrveCarouselImages.placeholder(ApplicationProvider.getApplicationContext(), 0.5f)

        assertEquals(SwrveCarouselImages.PLACEHOLDER_MAX_WIDTH, placeholder.width)
        assertEquals(SwrveCarouselImages.PLACEHOLDER_MAX_WIDTH / 2, placeholder.height)
    }

    // Translucent pixels are stored premultiplied, so the colour read back can be a few steps off
    private fun assertFillColour(pixel: Int) {
        val fill = SwrveCarouselImages.PLACEHOLDER_FILL_COLOR
        assertEquals(Color.alpha(fill), Color.alpha(pixel))
        for ((expected, actual) in listOf(Color.red(fill) to Color.red(pixel), Color.green(fill) to Color.green(pixel), Color.blue(fill) to Color.blue(pixel))) {
            assertTrue("colour $actual is not close to $expected", Math.abs(expected - actual) <= 8)
        }
    }
}
