package com.swrve.sdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.swrve.swrvesdkcommon.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Gives every carousel item's picture the same shape, so the expanded notification keeps its height when the user
// moves between items.
object SwrveCarouselImages {

    // Height over width. No taller than 3:2, as Android crops a taller picture to fit its frame, so it would be
    // cropped twice
    const val MIN_IMAGE_HEIGHT_RATIO = 1f / 3f
    const val MAX_IMAGE_HEIGHT_RATIO = 2f / 3f
    const val DEFAULT_IMAGE_HEIGHT_RATIO = 2f / 3f

    // The placeholder is as wide as the screen, so the system scales it down to the notification's width rather than
    // up, which would blur the icon. Capped, as a large tablet's screen would make a needlessly big bitmap
    const val PLACEHOLDER_MAX_WIDTH = 1080
    // Translucent, so it reads on both light and dark notifications, as the bitmap can't follow the theme
    const val PLACEHOLDER_FILL_COLOR = 0x1F808080
    const val PLACEHOLDER_SYMBOL_COLOR = 0x66808080
    private const val SYMBOL_WIDTH_FRACTION = 0.1f

    @JvmStatic
    fun imageHeightRatio(firstImageRatio: Float): Float {
        if (!(firstImageRatio > 0)) { // also catches NaN
            return DEFAULT_IMAGE_HEIGHT_RATIO
        }
        return min(max(firstImageRatio, MIN_IMAGE_HEIGHT_RATIO), MAX_IMAGE_HEIGHT_RATIO)
    }

    // Keeps the middle, as Android does with a tall rich push picture, so the first item matches a rich push of the
    // same image
    @JvmStatic
    fun fill(image: Bitmap, imageHeightRatio: Float): Bitmap {
        val width = image.width
        val height = image.height
        val targetHeight = max(1, (width * imageHeightRatio).roundToInt())
        if (targetHeight <= height) {
            return if (targetHeight == height) image else Bitmap.createBitmap(image, 0, (height - targetHeight) / 2, width, targetHeight)
        }
        val targetWidth = max(1, min(width, (height / imageHeightRatio).roundToInt()))
        return Bitmap.createBitmap(image, (width - targetWidth) / 2, 0, targetWidth, height)
    }

    @JvmStatic
    fun placeholder(context: Context, imageHeightRatio: Float): Bitmap {
        val width = context.resources.displayMetrics.widthPixels.coerceIn(1, PLACEHOLDER_MAX_WIDTH)
        val height = max(1, (width * imageHeightRatio).roundToInt())
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(PLACEHOLDER_FILL_COLOR)
        ContextCompat.getDrawable(context, R.drawable.swrve_carousel_placeholder)?.mutate()?.let { icon ->
            val size = (width * SYMBOL_WIDTH_FRACTION).roundToInt()
            val left = (width - size) / 2
            val top = (height - size) / 2
            icon.setBounds(left, top, left + size, top + size)
            icon.setTint(PLACEHOLDER_SYMBOL_COLOR)
            icon.draw(canvas)
        }
        return bitmap
    }
}
