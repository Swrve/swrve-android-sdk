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
     * It is important to handle each message action as this listener can be called multiple times eg for an Impression and Button actions such as Custom, Dismiss or Clipboard
     * 
     * {@code
     * SwrveInAppMessageConfig inAppConfig = new SwrveInAppMessageConfig.Builder()
     *       .messageListener((context, messageAction, messageDetails, selectedButton) -> {
     *
     *      switch (messageAction) {
     *           case Impression:
     *           break;
     *
     *           case Custom:
     *           // Note if there is a deeplink, we will call open url internally unless SwrveDeeplinkListener is implemented,
     *           // if it is, the deeplink url will be passed to the SwrveDeeplinkListener.
     *           break;
     *
     *           case Dismiss:
     *           break;
     *
     *           case CopyToClipboard:
     *           break;
     *
     *           default:
     *           break;
     *        }
     *
     *     }).build()
     * }
     * @param context        Application context
     * @param action         The listener action, see SwrveMessageAction for supported types
     * @param messageDetails Message details, see SwrveMessageDetails for meta data
     * @param selectedButton SelectedButton, see SwrveMessageButtonDetails for meta data, this will be null if message action is an Impression
     */
    void onAction(Context context, SwrveMessageAction action, SwrveMessageDetails messageDetails, SwrveMessageButtonDetails selectedButton);
}
