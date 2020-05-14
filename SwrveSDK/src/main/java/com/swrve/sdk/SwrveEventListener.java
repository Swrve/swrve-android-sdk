package com.swrve.sdk;

import android.content.Context;

import com.swrve.sdk.conversations.SwrveConversationListener;
import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.messaging.SwrveMessageListener;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveOrientation;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Default event listener. Will display an in-app message if available.
 */
public class SwrveEventListener implements ISwrveEventListener {

    private final WeakReference<SwrveBase<?, ?>> sdk;
    private final SwrveMessageListener messageListener;
    private final SwrveConversationListener conversationListener;

    public SwrveEventListener(SwrveBase<?, ?> sdk, SwrveMessageListener messageListener, SwrveConversationListener conversationListener) {
        this.sdk = new WeakReference<>(sdk);
        this.messageListener = messageListener;
        this.conversationListener = conversationListener;
    }

    @Override
    public void onEvent(String eventName, Map<String, String> payload) {
        if (conversationListener != null && !SwrveHelper.isNullOrEmpty(eventName)) {
            SwrveBase<?, ?> sdkRef = sdk.get();
            if (sdkRef != null) {
                SwrveConversation conversation = sdkRef.getConversationForEvent(eventName, payload);
                if (conversation != null) {
                    conversationListener.onMessage(conversation);
                    return;
                }
            }
        }

        if (messageListener != null && !SwrveHelper.isNullOrEmpty(eventName)) {
            SwrveBase<?, ?> sdkRef = sdk.get();
            if (sdkRef != null) {
                SwrveOrientation deviceOrientation = SwrveOrientation.Both;
                Context ctx = sdkRef.getContext();
                if (ctx != null) {
                    deviceOrientation = SwrveOrientation.parse(ctx.getResources().getConfiguration().orientation);
                }
                SwrveMessage message = sdkRef.getMessageForEvent(eventName, payload, deviceOrientation);
                if (message != null) {
                    // Save the last used payload to use inside the default message listener
                    sdkRef.lastEventPayloadUsed = payload;
                    messageListener.onMessage(message);
                    // Remove ref
                    sdkRef.lastEventPayloadUsed = null;
                }
            }
        }
    }
}
