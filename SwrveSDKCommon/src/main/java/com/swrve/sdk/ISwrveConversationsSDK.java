package com.swrve.sdk;

import java.util.Map;

/**
 * Used internally to interface the SwrveSDK (native and Unity) with conversations
 */
interface ISwrveConversationsSDK {
    void queueConversationEvent(String viewEvent, String eventName, String page, String conversationId, Map<String, String> payload);
}
