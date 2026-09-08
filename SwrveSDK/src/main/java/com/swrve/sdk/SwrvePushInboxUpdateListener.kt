package com.swrve.sdk

/**
 * Implement this interface to be notified of changes to the Push Inbox.
 */
fun interface SwrvePushInboxUpdateListener {

    /**
     * This method is invoked when Push Inbox Messages have been initially loaded and each time they are updated.
     *
     * Note: the thread this is invoked on is not guaranteed. It is the main UI thread when an
     * activity context is available, and the calling thread otherwise.
     */
    fun onMessagesUpdated()
}
