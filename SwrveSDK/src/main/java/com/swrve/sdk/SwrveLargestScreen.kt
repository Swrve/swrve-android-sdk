package com.swrve.sdk

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.swrve.sdk.ISwrveCommon.CACHE_DEVICE_PROP_KEY
import com.swrve.sdk.ISwrveCommon.CACHE_LARGEST_SCREEN
import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage
import org.json.JSONException
import org.json.JSONObject

/**
 * Used internally. The largest of the app's screens seen on this device, in pixels (short side as width), kept across
 * launches. Content is requested for it, so in-app message images are big enough for any of the device's screens, such
 * as a foldable's.
 */
data class SwrveLargestScreen(val width: Int, val height: Int, val dpi: Float, val xdpi: Float, val ydpi: Float) {

    private val area: Long get() = width.toLong() * height

    /** Whether this screen replaces [other] as the largest. */
    fun isLargerThan(other: SwrveLargestScreen?): Boolean = other == null || area >= other.area * MIN_GROWTH

    private fun toJson(): String = JSONObject()
        .put("width", width)
        .put("height", height)
        .put("dpi", dpi.toDouble())
        .put("xdpi", xdpi.toDouble())
        .put("ydpi", ydpi.toDouble())
        .toString()

    companion object {
        // A screen replaces the stored one only if its area is larger by this much. A new size changes the content
        // request, so the content is downloaded in full again: not worth doing for a negligible gain
        private const val MIN_GROWTH = 1.02

        /**
         * Updates the stored largest screen with [screen], and returns the largest. Null if nothing is stored and
         * [screen] is null. Not synchronized: the caller holds a lock.
         */
        @JvmStatic
        fun update(storage: SwrveMultiLayerLocalStorage, screen: SwrveLargestScreen?): SwrveLargestScreen? {
            val largest = load(storage)
            if (screen == null || !screen.isLargerThan(largest)) {
                return largest
            }
            // Device level, so stored without a user id like the device id
            storage.setCacheEntry(CACHE_DEVICE_PROP_KEY, CACHE_LARGEST_SCREEN, screen.toJson())
            SwrveLogger.d("Swrve largest screen is now %dx%d (dpi %s)", screen.width, screen.height, screen.dpi)
            return screen
        }

        // The current screen: the whole display, not the app's window, which is smaller in split screen or a freeform window
        @JvmStatic
        @Suppress("DEPRECATION")
        fun measure(context: Context): SwrveLargestScreen? {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return null
            var width: Int
            var height: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = windowManager.maximumWindowMetrics.bounds
                width = bounds.width()
                height = bounds.height()
            } else {
                width = SwrveHelper.getDisplayWidth(context)
                height = SwrveHelper.getDisplayHeight(context)
            }
            if (width <= 0 || height <= 0) {
                return null
            }
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            var xdpi = metrics.xdpi
            var ydpi = metrics.ydpi
            // Always use portrait dimensions (i.e. width < height)
            if (width > height) {
                width = height.also { height = width }
                xdpi = ydpi.also { ydpi = xdpi }
            }
            return SwrveLargestScreen(width, height, metrics.densityDpi.toFloat(), xdpi, ydpi)
        }

        private fun load(storage: SwrveMultiLayerLocalStorage): SwrveLargestScreen? {
            val json = storage.getCacheEntry(CACHE_DEVICE_PROP_KEY, CACHE_LARGEST_SCREEN) ?: return null
            return try {
                val largest = JSONObject(json)
                SwrveLargestScreen(
                    largest.getInt("width"),
                    largest.getInt("height"),
                    largest.getDouble("dpi").toFloat(),
                    largest.getDouble("xdpi").toFloat(),
                    largest.getDouble("ydpi").toFloat()
                )
            } catch (e: JSONException) {
                SwrveLogger.e("Swrve largest screen could not be read, so is replaced by the current screen", e)
                null
            }
        }
    }
}
