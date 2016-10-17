package com.swrve.sdk.conversations.engine.model;


import com.google.gson.annotations.SerializedName;
import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

import java.io.Serializable;

public abstract class ConversationAtom implements Serializable {

    public enum TYPE {
        @SerializedName("html-fragment")        CONTENT_HTML,
        @SerializedName("image")                CONTENT_IMAGE,
        @SerializedName("spacer")               CONTENT_SPACER,
        @SerializedName("video")                CONTENT_VIDEO,
        @SerializedName("multi-value-input")    INPUT_MULTIVALUE,
        @SerializedName("star-rating")          INPUT_STARRATING,
        UNKNOWN
    }

    protected String tag;
    protected TYPE type;
    protected ConversationStyle style;

    public static ConversationAtom create(String tag, TYPE type) {
        BareConversationAtom bca = new BareConversationAtom();
        bca.tag = tag;
        bca.type = type;
        return bca;
    }

    public String getTag() {
        return tag;
    }

    public ConversationStyle getStyle() {
        return this.style;
    }

    public TYPE getType() {
        return type;
    }

    private static class BareConversationAtom extends ConversationAtom {
    }
}
