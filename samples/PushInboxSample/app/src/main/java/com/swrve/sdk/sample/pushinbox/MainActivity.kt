package com.swrve.sdk.sample.pushinbox

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.swrve.sdk.SwrveIdentityResponse
import com.swrve.sdk.SwrvePushInboxListenerResult
import com.swrve.sdk.SwrveSDK

class MainActivity : ComponentActivity() {

    private val busy = mutableStateOf(false)

    private val selectedMessageId = mutableStateOf<Long?>(null)

    /** Hoisted out of the composable so it survives the detail screen replacing it. */
    private val selectedTab = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PushInboxSampleTheme {
                val messages = InboxStore.messages
                val selected = messages.firstOrNull { it.messageId == selectedMessageId.value }

                // A deleted message would leave the detail screen showing something that no longer exists.
                if (selectedMessageId.value != null && selected == null) {
                    selectedMessageId.value = null
                }

                // Back closes the detail screen before it leaves the app.
                BackHandler(enabled = selected != null) { selectedMessageId.value = null }

                // One Scaffold per screen. Nesting them applies the status bar inset twice.
                if (selected != null) {
                    MessageDetailScreen(
                        message = selected,
                        onBack = { selectedMessageId.value = null },
                        onFollowAction = { followAction(selected) }
                    )
                } else {
                    PushInboxApp(
                        messages = messages,
                        swrveUserId = InboxStore.swrveUserId,
                        externalUserId = InboxStore.externalUserId,
                        busy = busy.value,
                        onIdentify = ::identify,
                        onOpenMessage = ::openMessage,
                        onDelete = ::deleteMessage,
                        selectedTab = selectedTab.intValue,
                        onSelectTab = { selectedTab.intValue = it }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        InboxStore.reload()
    }

    /**
     * Inbox messages belong to a user, so the sample has to be that user before anything appears.
     * Identify with the external ID the campaign targeted and the SDK resolves to that Swrve user,
     * bringing their inbox down with the next content fetch.
     *
     * Kept to one field and one button deliberately — [IdentitySample] covers identify properly,
     * including what a failure leaves the SDK doing.
     */
    private fun identify(externalUserId: String) {
        busy.value = true
        SwrveSDK.identify(externalUserId, object : SwrveIdentityResponse {
            override fun onSuccess(status: String, swrveId: String) {
                Log.i(LOG_TAG, "identify success: status=$status swrveId=$swrveId")
                // identify runs on a background thread and calls back on it, so UI state has to be posted.
                runOnUiThread {
                    busy.value = false
                }
                // The new user's inbox arrives with the next content fetch and the update listener fires then. This just clears the previous user's messages in the meantime.
                InboxStore.reload()
            }

            override fun onError(responseCode: Int, errorMessage: String) {
                Log.e(LOG_TAG, "identify failed: $responseCode $errorMessage")
                runOnUiThread {
                    busy.value = false
                    toast("Identify failed: $errorMessage")
                }
                InboxStore.reload()
            }
        })
    }

    /**
     * Opening a message is where `read` belongs — distinct from `engage`, which is the user acting on
     * it. Keeping them apart is the whole reason there is a detail screen: collapse them onto one tap
     * and a message is read before you ever see it unread.
     *
     * Note the asymmetry between the two calls. `read` only produces an event when the server
     * confirms the state actually changed, so calling it on an already-read message succeeds
     * silently and sends nothing. `engage` always fires. That is easy to mistake for a bug.
     */
    private fun openMessage(message: InboxMessage) {
        selectedMessageId.value = message.messageId

        if (!message.unread) return

        SwrveSDK.readPushInboxMessage(message.messageId) { messageId, result ->
            logResult("read", messageId, result)
            // These calls need network, so failure is a real possibility. Re-reading either way keeps the UI honest: on failure the message stays unread.
            InboxStore.reload()
        }
    }

    /**
     * `engage` records that the user acted on the message. The deeplink is followed regardless of
     * whether the call succeeds — the user asked to go somewhere, and analytics failing is not a
     * reason to refuse. Nothing here validates the URL; if no app handles it, that surfaces as an error.
     */
    private fun followAction(message: InboxMessage) {
        val target = message.actionValue ?: return

        SwrveSDK.engagePushInboxMessage(message.messageId) { messageId, result ->
            logResult("engage", messageId, result)
        }

        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
        } catch (e: ActivityNotFoundException) {
            // Expected in this sample: the deeplinks in your campaigns point at your own app, not this one.
            Log.i(LOG_TAG, "Nothing on this device handles $target", e)
            toast("No app handles $target")
        }
    }

    private fun deleteMessage(message: InboxMessage) {
        SwrveSDK.deletePushInboxMessage(message.messageId) { messageId, result ->
            logResult("delete", messageId, result)
            InboxStore.reload()
        }
    }

    /** Like `read`, `delete` only sends an event when the server confirms the state changed — so worth logging the result rather than assuming. */
    private fun logResult(operation: String, messageId: Long, result: SwrvePushInboxListenerResult) {
        if (result.resultCode == SwrvePushInboxListenerResult.ResultCode.SUCCESS) {
            Log.i(LOG_TAG, "$operation $messageId succeeded")
        } else {
            Log.e(
                LOG_TAG,
                "$operation $messageId failed: ${result.resultCode} http=${result.httpResponseCode} ${result.errorMessage}"
            )
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()

    private companion object {
        const val LOG_TAG = "PushInboxSample"
    }
}
