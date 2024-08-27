package com.swrve.sdk

/**
 * Represents the state of a message in the Swrve push inbox.
 *
 * The message can either be in a `READ` or `UNREAD` state.
 */
enum class SwrvePushInboxMessageState {
    /**
     * The message has been read.
     */
    READ,

    /**
     * The message has not been read.
     */
    UNREAD,

    /**
     * The message has been deleted.
     */
    DELETED
}
