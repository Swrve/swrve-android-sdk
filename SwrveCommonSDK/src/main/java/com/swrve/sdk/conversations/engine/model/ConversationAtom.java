package com.swrve.sdk.conversations.engine.model;

import android.graphics.Color;


import com.swrve.sdk.conversations.engine.model.styles.BackgroundStyle;
import com.swrve.sdk.conversations.engine.model.styles.ForegroundStyle;
import com.swrve.sdk.conversations.engine.model.styles.SwrveAtomStyle;

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
    protected SwrveAtomStyle style;


    public static ConversationAtom create(String tag, String type) {
        BareConversationAtom bca = new BareConversationAtom();
        bca.tag = tag;
        bca.type = type;
        return bca;
    }

    public String getTag() {
        return tag;
    }

    public SwrveAtomStyle getStyle(){
        return this.style;
    }

    public String getType() {
        return type;
    }

    private static class BareConversationAtom extends ConversationAtom {
    }
}
