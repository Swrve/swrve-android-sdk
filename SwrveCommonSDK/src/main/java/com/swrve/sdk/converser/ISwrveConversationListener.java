package com.swrve.sdk.converser;

import com.swrve.sdk.converser.SwrveConversation;

/**
 * Implement this interface to handle the rendering of in-app conversations
 * completely from your app. You will have to render and manage these
 * conversations yourself.
 */
public interface ISwrveConversationListener {
    /**
     * This method is invoked when a conversation should be shown in your app.
     *
     * @param conversation   coonversation to be shown.
     * @param firstTime indicates if this message was already showing and the app
     *                  rotated.
     */
    void onMessage(SwrveConversation conversation, boolean firstTime);
}
