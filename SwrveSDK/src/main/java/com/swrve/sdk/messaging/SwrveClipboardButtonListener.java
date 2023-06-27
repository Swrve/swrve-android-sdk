package com.swrve.sdk.messaging;

/**
 * Implement this interface to handle the feedback used by your app as a result
 * of an in-app copy-to-clipboard button.
 * Note: the methods in this interface will be invoked from the UI thread.
 * @deprecated Use SwrveMessageListener
 */
@Deprecated
public interface SwrveClipboardButtonListener {
    /**
     * This method is invoked when a clipboard button has been pressed on an in-app message.
     *
     * @param clipboardContents text contents that been copied to clipboard
     * @deprecated Use SwrveMessageListener
     */
    @Deprecated
    void onAction(String clipboardContents);

}
