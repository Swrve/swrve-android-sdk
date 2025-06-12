package com.swrve.sdk

/**
 * Defines a listener interface for handling completion of refreshContent API in Swrve.
 */
interface SwrveRefreshContentListener {

    /**
     * Called when an operation related to refreshContent API is completed.
     *
     * @param result The result of the operation encapsulated in a {@link SwrveRefreshContentListenerResult} object.
     *               This object contains information such as the result code, error message, and HTTP response code.
     */
    fun onComplete(result: SwrveRefreshContentListenerResult)
}
