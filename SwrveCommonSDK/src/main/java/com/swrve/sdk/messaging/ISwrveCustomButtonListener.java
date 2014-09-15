package com.swrve.sdk.messaging;

/**
 * Implement this interface to handle custom deep-links in your app as result
 * of an in-app custom button.
 *
 * Note: the methods in this interface will be invoked from the UI thread.
 */
public interface ISwrveCustomButtonListener {
    /**
     * This method is invoked when a custom button has been pressed on an in-app message.
     *
     * @param customAction custom action of button that was pressed.
     */
    void onAction(String customAction);
}
