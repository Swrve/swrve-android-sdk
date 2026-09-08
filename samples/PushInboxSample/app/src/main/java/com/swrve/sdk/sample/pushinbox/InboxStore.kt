package com.swrve.sdk.sample.pushinbox

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.swrve.sdk.SwrvePushInboxUpdateListener
import com.swrve.sdk.SwrveSDK

/**
 * Holds the inbox for the whole app.
 * The inbox belongs to the user rather than to any one screen, so it lives here and screens read from it.
 */
object InboxStore : SwrvePushInboxUpdateListener {

    var messages by mutableStateOf<List<InboxMessage>>(emptyList())
        private set

    var swrveUserId by mutableStateOf("")
        private set

    var externalUserId by mutableStateOf("")
        private set

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onMessagesUpdated() = reload()

    /**
     * Re-reads the SDK's local state — no network. Expired messages are already filtered out, so whatever comes back is what should be shown.
     */
    fun reload() {
        mainHandler.post {
            messages = SwrveSDK.getPushInboxMessages().map(InboxMessage::from)
            swrveUserId = SwrveSDK.getUserId()
            externalUserId = SwrveSDK.getExternalUserId() ?: ""
        }
    }
}
