package com.swrve.sdk.converser.engine.model;

public class ConverserInputResult {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_CALENDAR = "calendar";
    public static final String TYPE_MULTI_CHOICE = "multi-choice";
    public static final String TYPE_SINGLE_CHOICE = "choice";
    public static final String TYPE_VIDEO_PLAY = "play";
    public static final String TYPE_NPS = "nps";
    public String type, conversationId,  pageTag, fragmentTag;
    public Object result;

    public String getType() {
        return type;
    }

    public String getPageTag() {
        return pageTag;
    }

    public String getResultAsString() {
        return result.toString();
    }

    public Object getResult() {
        return result;
    }

    public String getFragmentTag() {
        return fragmentTag;
    }

    public String getConversationId() {
        return conversationId;
    }

    public boolean isMultiChoice() {
        return type.equalsIgnoreCase(TYPE_MULTI_CHOICE);
    }

    public boolean isSingleChoice() {
        return type.equalsIgnoreCase(TYPE_SINGLE_CHOICE);
    }

    public boolean isTextInput() {
        return type.equalsIgnoreCase(TYPE_TEXT);
    }

    public boolean isNps() {
        return type.equalsIgnoreCase(TYPE_NPS);
    }
}
