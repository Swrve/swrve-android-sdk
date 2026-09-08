package com.swrve.sdk

/**
 * Defines a listener interface for handling push inbox message apis in Swrve.
 */
fun interface SwrvePushInboxListener {

    /**
     * Called when an operation related to push inbox message is completed.
     *
     * @param messageId The id of the message that was the target of the operation.
     * @param result The result of the operation encapsulated in a {@link SwrvePushInboxListenerResult} object.
     *               This object contains information such as the result code, error message, and HTTP response code.
     */
    fun onComplete(messageId: Long, result: SwrvePushInboxListenerResult)
}
