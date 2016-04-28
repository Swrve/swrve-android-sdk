package com.swrve.sdk.messaging;

/**
 * Implement this interface to listen for in-app message lifecycle events.
 */
public interface ISwrveMessageLifecycleListener {

    /**
     * Invoked when an in-app message dialog is shown.
     * @param message The message being shown
     */
    void onShowMessage(SwrveMessage message);

    /**
     * Invoked when an in-app message dialog is dismissed.
     * @param message The dismissed message
     */
    void onDismissMessage(SwrveMessage message);
}
