package com.swrve.sdk;

import java.io.File;
import java.util.Map;

/**
 * Used internally to interface the SwrveSDK (native and Unity) with conversations
 */
interface ISwrveConversationsSDK {
    File getCacheDir();

    void queueConversationEvent(String viewEvent,
                                String eventName, String page, String conversationId,
                                Map<String, String> payload);
}
