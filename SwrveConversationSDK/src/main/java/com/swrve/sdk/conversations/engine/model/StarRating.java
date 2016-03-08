package com.swrve.sdk.conversations.engine.model;

public class StarRating extends ConversationAtom {

    protected String value;
    protected String star_color;

    public String getValue() {
        return value;
    }

    public String getStarColor() {
        return star_color;
    }
}
