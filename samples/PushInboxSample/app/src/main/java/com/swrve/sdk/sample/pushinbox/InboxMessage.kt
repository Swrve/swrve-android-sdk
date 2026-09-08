package com.swrve.sdk.sample.pushinbox

import com.swrve.sdk.SwrvePushInboxMessage
import com.swrve.sdk.SwrvePushInboxMessageState
import org.json.JSONObject

/**
 * What one row and one detail screen show, copied out of the SDK's message object.
 *
 * Immutable on purpose. The SDK hands back a fresh list each time, but modelling the row as a plain data class means Compose can compare two lists and see that a message became read.
 */
data class InboxMessage(
    val messageId: Long,
    val subject: String?,
    val body: String,
    val thumbnailUrl: String?,
    val actionValue: String?,
    val sentDate: Long,
    val unread: Boolean,
    /** The payload as it arrived, for this sample's debug panel only. A real inbox has no use for it. */
    val rawCustomerJson: String
) {
    companion object {
        /**
         * `customerJson` is your payload, not the SDK's — it is passed through untouched, so parsing it is entirely up to you.
         */
        fun from(message: SwrvePushInboxMessage): InboxMessage {
            val json: JSONObject = message.customerJson
            return InboxMessage(
                messageId = message.messageId,
                subject = json.optNullableString("subject"),
                body = json.optNullableString("message").orEmpty(),
                thumbnailUrl = json.optNullableString("thumbnail"),
                actionValue = json.optNullableString("action_value"),
                sentDate = message.sentDate,
                unread = message.state == SwrvePushInboxMessageState.UNREAD,
                rawCustomerJson = runCatching { json.toString(2) }.getOrDefault(json.toString())
            )
        }

        private fun JSONObject.optNullableString(key: String): String? =
            if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }
    }
}
