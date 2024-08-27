package com.swrve.sdk

/**
 * Implement this interface to be notified of changes to the Push Inbox.
 */
interface SwrvePushInboxUpdateListener {

    /**
     * This method is invoked when Push Inbox Messages have been initially loaded and each time they are updated.
     *
     * Note: this method will be invoked from a different thread than the main UI thread.
     */
    fun onMessagesUpdated()
}
