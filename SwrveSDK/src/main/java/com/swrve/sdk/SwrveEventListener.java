package com.swrve.sdk;

import android.content.Context;

import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.SwrveConversationListener;
import com.swrve.sdk.messaging.SwrveBaseMessage;
import com.swrve.sdk.messaging.SwrveEmbeddedMessage;
import com.swrve.sdk.messaging.SwrveEmbeddedMessageListener;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageListener;
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
    private final SwrveEmbeddedMessageListener embeddedMessageListener;

    public SwrveEventListener(SwrveBase<?, ?> sdk, SwrveMessageListener messageListener, SwrveConversationListener conversationListener, SwrveEmbeddedMessageListener embeddedMessageListener) {
        this.sdk = new WeakReference<>(sdk);
        this.messageListener = messageListener;
        this.conversationListener = conversationListener;
        this.embeddedMessageListener = embeddedMessageListener;
    }

    @Override
    public void onEvent(String eventName, Map<String, String> payload) {
        boolean conversationDisplayed = false;
        if (conversationListener != null && !SwrveHelper.isNullOrEmpty(eventName)) {
            SwrveBase<?, ?> sdkRef = sdk.get();
            if (sdkRef != null) {
                SwrveConversation conversation = sdkRef.getConversationForEvent(eventName, payload);
                if (conversation != null) {
                    conversationListener.onMessage(conversation);
                    conversationDisplayed = true;
                }
            }
        }
        if (conversationDisplayed) {
            QaUser.campaignTriggeredMessageNoDisplay(eventName, payload);
            return;
        }

        if ((messageListener != null || embeddedMessageListener != null) && !SwrveHelper.isNullOrEmpty(eventName)) {
            SwrveBase<?, ?> sdkRef = sdk.get();
            if (sdkRef != null) {
                SwrveOrientation deviceOrientation = SwrveOrientation.Both;
                Context ctx = sdkRef.getContext();
                if (ctx != null) {
                    deviceOrientation = SwrveOrientation.parse(ctx.getResources().getConfiguration().orientation);
                }

                // check message pool
                SwrveBaseMessage message = sdkRef.getBaseMessageForEvent(eventName, payload, deviceOrientation);
                if (message != null) {
                    // Save the last used payload to use inside the default message listener
                    sdkRef.lastEventPayloadUsed = payload;

                    if (message instanceof SwrveMessage) {

                        if (messageListener != null) {
                            messageListener.onMessage((SwrveMessage) message);
                        }
                    } else if (message instanceof SwrveEmbeddedMessage) {

                        if (embeddedMessageListener != null) {
                            Map<String, String> personalizationProperties = sdkRef.retrievePersonalizationProperties(payload, null);
                            embeddedMessageListener.onMessage(ctx, (SwrveEmbeddedMessage) message, personalizationProperties);
                        }
                    }

                    // Remove ref
                    sdkRef.lastEventPayloadUsed = null;

                }
            }
        }
    }
}
