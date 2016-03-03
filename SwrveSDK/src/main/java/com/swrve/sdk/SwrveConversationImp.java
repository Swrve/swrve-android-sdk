package com.swrve.sdk;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class SwrveConversationImp implements ISwrveConversationsSDK {
    private final String LOG_TAG = "SwrveConversationImp";
    private ISwrveBase<?, ?> swrve;

    public SwrveConversationImp(ISwrveBase<?, ?> swrve) {
        this.swrve = swrve;
    }

    @Override
    public void queueConversationEvent(
            String viewEvent,
            String eventName, String page, String conversationId,
            Map<String, String> payload)
    {
        if(payload == null) {
            payload = new HashMap<String, String>();
        }
        payload.put("event", eventName);
        payload.put("conversation", conversationId);
        payload.put("page", page);

        SwrveLogger.d(LOG_TAG, "Sending view conversation event: " + viewEvent);

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("name", viewEvent);
        ((SwrveBase)swrve).queueEvent("event", parameters, payload);
    }

    @Override
    public File getCacheDir() {
        return swrve.getCacheDir();
    }
}
