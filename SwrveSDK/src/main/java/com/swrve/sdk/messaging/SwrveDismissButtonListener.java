package com.swrve.sdk.messaging;

/**
 * Implement this interface to get notified of dismiss button clicks on an in-app message.
 * Use SwrveCustomButtonListener instead if you want to implement logic for your custom buttons.
 *
 * Note: the methods in this interface will be invoked from the UI thread.
 */
public interface SwrveDismissButtonListener {
    /**
     * This method is invoked when a dismiss button has been pressed on an in-app message or when the back button was pressed
     * and the message was closed.
     *
     * @param campaignSubject Campaign subject
     * @param buttonName Dismiss button name or null when the back button was pressed
     * @param campaignName The name of the campaign which is being dismissed
     */
    void onAction(String campaignSubject, String buttonName, String campaignName);
}
