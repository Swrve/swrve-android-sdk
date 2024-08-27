package com.swrve.sdk

import org.json.JSONObject

/**
 * Represents a message in the Swrve push inbox.
 *
 * @constructor Creates an instance of SwrvePushInboxMessage with the given JSON object.
 * @param json The JSON object containing the message data.
 *
 * @property messageId The push campaign message id.
 * @property variantId The push campaign variant id.
 * @property endDate The expiry date of the push message. After this date, the message will no longer be returned from APIs.
 * @property sentDate The date the push notification was sent.
 * @property customerJson The JSON object containing customer-specific data.
 * @property state The state of the message (READ or UNREAD).
 */
class SwrvePushInboxMessage(json: JSONObject) {

    val messageId = json.getLong("message_id")
    val variantId = json.getLong("variant_id")
    val endDate = json.getLong("end_date")
    val sentDate = json.getLong("sent_date")
    val customerJson: JSONObject = json.getJSONObject("customer_json")
    var state: SwrvePushInboxMessageState

    init {
        val value = json.getString("state")
        state = if (value.equals("R", ignoreCase = true)) {
            SwrvePushInboxMessageState.READ
        } else if (value.equals("U", ignoreCase = true)) {
            SwrvePushInboxMessageState.UNREAD
        } else {
            throw IllegalArgumentException("Push Inbox Message invalid state value: $value")
        }
    }
}
