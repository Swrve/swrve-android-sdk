package com.swrve.sdk;

import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.ui.ConversationActivity;
import com.swrve.sdk.messaging.SwrveBaseMessage;
import com.swrve.sdk.messaging.SwrveEmbeddedMessage;
import com.swrve.sdk.messaging.SwrveEmbeddedMessageListener;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveOrientation;

import java.util.Map;

/**
 * This class will trigger a swrve campaign if available.
 */
class SwrveEventListener implements ISwrveEventListener {

    private final SwrveBase<?, ?> sdk;
    private final SwrveEmbeddedMessageListener embeddedMessageListener;

    public SwrveEventListener(SwrveBase<?, ?> sdk, SwrveEmbeddedMessageListener embeddedMessageListener) {
        this.sdk = sdk;
        this.embeddedMessageListener = embeddedMessageListener;
    }

    @Override
    public void onEvent(String eventName, Map<String, String> payload) {
        if (SwrveHelper.isNullOrEmpty(eventName)) {
            return;
        }

        SwrveConversation conversation = sdk.getConversationForEvent(eventName, payload);
        if (conversation != null) {
            ConversationActivity.showConversation(sdk.getContext(), conversation, sdk.config.getOrientation());
            conversation.getCampaign().messageWasShownToUser();
            QaUser.campaignTriggeredMessageNoDisplay(eventName, payload);
            return;
        }

        SwrveOrientation deviceOrientation = SwrveOrientation.parse(sdk.getContext().getResources().getConfiguration().orientation);
        SwrveBaseMessage message = sdk.getBaseMessageForEvent(eventName, payload, deviceOrientation);
        if (message != null) {
            sdk.lastEventPayloadUsed = payload; // Save the last used payload for personalization
            if (message instanceof SwrveMessage) {
                sdk.displaySwrveMessage((SwrveMessage) message, null);
            } else if (message instanceof SwrveEmbeddedMessage && embeddedMessageListener != null) {
                Map<String, String> personalizationProperties = sdk.retrievePersonalizationProperties(payload, null);
                embeddedMessageListener.onMessage(sdk.getContext(), (SwrveEmbeddedMessage) message, personalizationProperties);
            }
            sdk.lastEventPayloadUsed = null; // Remove ref
        }
    }
}
