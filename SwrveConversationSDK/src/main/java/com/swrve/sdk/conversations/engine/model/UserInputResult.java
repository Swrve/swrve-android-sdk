package com.swrve.sdk.conversations.engine.model;

import java.io.Serializable;

public class UserInputResult implements Serializable{
    public static final String TYPE_SINGLE_CHOICE = "choice";
    public static final String TYPE_VIDEO_PLAY = "play";
    public static final String TYPE_STAR_RATING = "star-rating";

    public String type, pageTag, fragmentTag;
    public int conversationId;
    public Object result;

    public String getType() {
        return type;
    }

    public String getPageTag() {
        return pageTag;
    }

    public Object getResult() {
        return result;
    }

    public String getFragmentTag() {
        return fragmentTag;
    }

    public int getConversationId() {
        return conversationId;
    }

    public boolean isSingleChoice() {
        return type.equalsIgnoreCase(TYPE_SINGLE_CHOICE);
    }

    public boolean isStarRating() {
        return type.equalsIgnoreCase(TYPE_STAR_RATING);
    }
}
