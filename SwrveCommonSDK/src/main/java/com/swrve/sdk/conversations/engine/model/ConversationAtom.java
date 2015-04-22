package com.swrve.sdk.conversations.engine.model;

import java.io.Serializable;

public abstract class ConversationAtom implements Serializable {
    public static final String TYPE_CONTENT_TEXT = "text";
    public static final String TYPE_CONTENT_HTML = "html-fragment";
    public static final String TYPE_CONTENT_IMAGE = "image";
    public static final String TYPE_CONTENT_VIDEO = "video";
    public static final String TYPE_CONTENT_AUDIO = "audio";

    public static final String TYPE_INPUT_TEXTINPUT = "text-input";
    public static final String TYPE_INPUT_MULTIVALUE = "multi-value-input";
    public static final String TYPE_INPUT_MULTIVALUELONG = "multi-value-long-input";

    protected String tag;
    protected String type;
    protected String target;

    public static ConversationAtom create(String tag, String type, String target) {
        BareConversationAtom bca = new BareConversationAtom();
        bca.tag = tag;
        bca.type = type;
        bca.target = target;

        return bca;
    }

    public String getTag() {
        return tag;
    }

    public String getTarget() {
        return target;
    }

    public String getType() {
        return type;
    }

    private static class BareConversationAtom extends ConversationAtom {
    }
}
