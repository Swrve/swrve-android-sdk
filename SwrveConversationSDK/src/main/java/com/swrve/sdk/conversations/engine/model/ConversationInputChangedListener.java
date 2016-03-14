package com.swrve.sdk.conversations.engine.model;

import java.util.Map;

public interface ConversationInputChangedListener {
    void onContentChanged(Map<String, Object> contentChanged, ConversationAtom content);
}
