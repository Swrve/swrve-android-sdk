package com.swrve.sdk

import android.net.Uri
import android.os.Looper
import android.view.MotionEvent
import androidx.media3.common.Player

import com.swrve.sdk.messaging.SwrveVideoPlayerView
import com.swrve.sdk.messaging.SwrveVideoSettings

import org.junit.After
import org.junit.Before
import org.junit.Test

import org.robolectric.Shadows

import org.junit.Assert.*
import org.robolectric.annotation.LooperMode

@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class SwrveVideoPlayerViewTest : SwrveBaseTest() {

    private lateinit var videoPlayerView: SwrveVideoPlayerView
    private lateinit var videoUri: Uri

    @Before
    override fun setUp() {
        super.setUp()
        videoUri = Uri.parse("https://cdn.example.com/test.mp4")
    }

    @After
    override fun tearDown() {
        runOnMainThread {
            if (::videoPlayerView.isInitialized) {
                videoPlayerView.release()
            }
        }
    }

    @Test
    fun testSetupPlayerInitializesExoPlayer() {
        val settings = SwrveVideoSettings(
            autoPlay = false,
            loop = false,
            fillScreen = false,
            showControls = false
        )

        runOnMainThread {
            videoPlayerView = inflateVideoView()
            videoPlayerView.setupPlayer(settings, videoUri)
        }

        assertNotNull(videoPlayerView.player)
        assertFalse(videoPlayerView.useController)
    }

    @Test
    fun testLoopingEnablesRepeatMode() {
        val settings = SwrveVideoSettings(
            autoPlay = false,
            loop = true,
            fillScreen = false,
            showControls = false
        )

        runOnMainThread {
            videoPlayerView = inflateVideoView()
            videoPlayerView.setupPlayer(settings, videoUri)
        }

        runOnMainThread {
            val exoPlayer = videoPlayerView.player as Player
            assertEquals(Player.REPEAT_MODE_ALL, exoPlayer.repeatMode)
        }
    }

    @Test
    fun testCleanupPlayerReleasesResources() {
        val settings = SwrveVideoSettings(
            autoPlay = false,
            loop = false,
            fillScreen = false,
            showControls = false
        )

        runOnMainThread {
            videoPlayerView = inflateVideoView()
            videoPlayerView.setupPlayer(settings, videoUri)
            videoPlayerView.release()
        }

        assertNull(videoPlayerView.player)
        assertNull(videoPlayerView.lastPlayerListener)
    }

    @Test
    fun testTouchEventNotConsumed() {
        runOnMainThread {
            videoPlayerView = inflateVideoView()
            val settings = SwrveVideoSettings(
                autoPlay = false,
                loop = false,
                fillScreen = false,
                showControls = false
            )
            videoPlayerView.setupPlayer(settings, videoUri)
            videoPlayerView.useController = false
            val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0f, 0f, 0)
            val result = videoPlayerView.onTouchEvent(event)
            assertFalse("Should not consume event when showControls is false", result)
            assertFalse("useController should remain false", videoPlayerView.useController)
        }
    }

    @Test
    fun testOnTouchEventConsumed() {
        runOnMainThread {
            videoPlayerView = inflateVideoView()
            val settings = SwrveVideoSettings(
                autoPlay = false,
                loop = false,
                fillScreen = false,
                showControls = true
            )
            videoPlayerView.setupPlayer(settings, videoUri)
            videoPlayerView.useController = false
            val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0f, 0f, 0)
            val result = videoPlayerView.onTouchEvent(event)
            assertTrue("Should consume event on first tap when showControls is true", result)
            assertTrue("useController should be true after tap", videoPlayerView.useController)
        }
    }

    private fun runOnMainThread(block: () -> Unit) {
        mActivity!!.runOnUiThread(block)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    private fun inflateVideoView(): SwrveVideoPlayerView {
        val inflater = mActivity!!.layoutInflater
        return inflater.inflate(R.layout.swrve_video_player_fit, null) as SwrveVideoPlayerView
    }
}
