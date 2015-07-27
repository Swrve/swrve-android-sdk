package com.swrve.sdk.conversations;

/**
 * Implement this interface to handle the rendering of in-app conversations
 * completely from your app. You will have to render and manage these
 * conversations yourself.
 */
public interface ISwrveConversationListener {
    /**
     * This method is invoked when a conversation should be shown in your app.
     *
     * @param conversation conversation to be shown.
     */
    void onMessage(SwrveConversation conversation);
}
