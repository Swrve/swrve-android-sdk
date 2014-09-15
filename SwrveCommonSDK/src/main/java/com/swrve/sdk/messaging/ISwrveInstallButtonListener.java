package com.swrve.sdk.messaging;

/**
 * Implement this interface to handle callbacks of install buttons
 * inside your in-app messages.
 *
 * Note: the methods in this interface will be invoked from the UI thread.
 */
public interface ISwrveInstallButtonListener {
    /**
     * This method is invoked when an install button has been pressed on an in-app message.
     *
     * @param appStoreLink app store install link.
     * @return boolean
     * returning false stops the normal flow of event processing
     * to enable custom logic. Return true otherwise.
     */
    boolean onAction(String appStoreLink);
}
