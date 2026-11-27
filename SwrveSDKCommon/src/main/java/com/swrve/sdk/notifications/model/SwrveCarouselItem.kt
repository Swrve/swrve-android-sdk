package com.swrve.sdk.notifications.model

import com.google.gson.annotations.SerializedName

/**
 * A single item of the _sw.carousel array.
 */
class SwrveCarouselItem {
    var imageUrl: String? = null
    var title: String? = null
    var body: String? = null
    var action: String? = null

    @SerializedName("action_type")
    private var rawActionType: SwrveNotificationButton.ActionType? = null

    /**
     * Carousel items only handle OPEN_URL and OPEN_APP. Absent or any other value resolves to OPEN_APP.
     */
    val actionType: SwrveNotificationButton.ActionType
        get() = if (rawActionType == SwrveNotificationButton.ActionType.OPEN_URL) SwrveNotificationButton.ActionType.OPEN_URL else SwrveNotificationButton.ActionType.OPEN_APP

    /**
     * The URL a tap on this item opens, and that its events report: never the push's own deeplink.
     */
    val deeplink: String?
        get() = if (actionType == SwrveNotificationButton.ActionType.OPEN_URL && !action.isNullOrEmpty()) action else null
}
