package com.swrve.sdk
import android.content.Context

/**
 * Listener invoked when a user has been disabled and the SDK
 * has stopped tracking for that user.
 */
fun interface SwrveUserDisabledListener {

    /**
     * Called when a user has been disabled.
     *
     * @param context Android context.
     * @param swrveUserId The Swrve-generated user identifier that has been disabled.
     * @param externalId The external user identifier associated with the user.
     *                   If no external ID is present, this value will be an empty string.
     */
    fun onUserDisabled(
        context: Context,
        swrveUserId: String,
        externalId: String
    )
}
