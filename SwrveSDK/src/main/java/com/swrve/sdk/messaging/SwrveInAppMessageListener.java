package com.swrve.sdk.messaging;


import android.content.Context;

/**
 * Implement this interface to listen to IAM actions such as impressions or button clicks.
 * <p>
 * Note: the methods in this interface will be invoked from the UI thread.
 */
public interface SwrveInAppMessageListener {

    enum SwrveMessageAction {

        Dismiss,            // Cancel the message display
        Custom,             // Handle the custom action string associated with the button
        CopyToClipboard,    // Copy the contents of the action string associated to clipboard
        Impression;         // The in-app message was shown to the user
    }

    /**
     * This method is invoked for various actions and views that can happen in a in-app message.
     *
     * @param context        Application context
     * @param action         The listener action
     * @param messageDetails Message details
     * @param selectedButton SelectedButton, this can be null
     */
    void onAction(Context context, SwrveMessageAction action, SwrveMessageDetails messageDetails, SwrveMessageButtonDetails selectedButton);
}
