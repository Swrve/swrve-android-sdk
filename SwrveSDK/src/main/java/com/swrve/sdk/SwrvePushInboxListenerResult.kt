package com.swrve.sdk

/**
 * Represents the result of an operation related to Swrve push inbox listener.
 */
class SwrvePushInboxListenerResult(
    /**
     * Result code indicating the outcome of the operation.
     */
    val resultCode: ResultCode,

    /**
     * Error message providing additional details in case of failure.
     */
    val errorMessage: String,

    /**
     * HTTP response code associated with the operation, if applicable.
     */
    val httpResponseCode: Int
) {
    enum class ResultCode {
        /**
         * Success result code indicating the operation was successful.
         */
        SUCCESS,

        /**
         * Error result code indicating an unknown error occurred.
         */
        ERROR_UNKNOWN,

        /**
         * Error result code indicating an error occurred.
         */
        ERROR
    }
}
