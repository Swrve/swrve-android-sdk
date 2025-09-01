package com.swrve.sdk.messaging

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.util.AttributeSet
import android.view.MotionEvent

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class SwrveVideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : PlayerView(context, attrs, defStyleAttr) {

    private lateinit var exoPlayer: ExoPlayer
    lateinit var videoPlayerSettings: SwrveVideoSettings
    var lastPlayerListener: Player.Listener? = null
        private set

    init {
        setBackgroundColor(Color.TRANSPARENT)
        isFocusable = true
    }

    fun setupPlayer(videoSettings: SwrveVideoSettings, videoURL: Uri) {
        videoPlayerSettings = videoSettings
        exoPlayer = ExoPlayer.Builder(context.applicationContext)
            .build().apply {
                repeatMode =
                    if (videoSettings.loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
                setMediaItem(MediaItem.fromUri(videoURL))
                prepare()
            }

        player = exoPlayer
        useController = false
    }

    fun play() {
        if (::exoPlayer.isInitialized) {
            exoPlayer.playWhenReady = true
        }
    }

    fun stop() {
        if (::exoPlayer.isInitialized) {
            exoPlayer.playWhenReady = false
            exoPlayer.seekTo(0)
            useController = false
        }
    }

    fun setPlayerListener(listener: Player.Listener) {
        lastPlayerListener = listener
        exoPlayer.addListener(listener)
    }

    fun release() {
        if (::exoPlayer.isInitialized) {
            val playerToRelease = exoPlayer
            player = null
            lastPlayerListener = null
            if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
                playerToRelease.release()
            } else {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    playerToRelease.release()
                }
            }
        }
    }

    /*
     If video controls are not allowed (showControls == false), do not consume the event,
     so parent views (e.g., story progress gestures) can handle it.
     Otherwise ensure that video taps only consume events when controls are shown or being shown
    */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!videoPlayerSettings.showControls || event == null) {
            return false
        }
        if (!useController && (event?.action == MotionEvent.ACTION_UP || event?.action == MotionEvent.ACTION_DOWN)) {
            useController = true
        }
        return super.onTouchEvent(event)
    }

    fun getPlayWhenReady(): Boolean = if (::exoPlayer.isInitialized) exoPlayer.playWhenReady else false
}
