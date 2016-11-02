package com.swrve.sdk.conversations.engine.model;

import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

public class StarRating extends ConversationAtom {

    protected String value;
    protected String star_color;

    public StarRating(String tag, TYPE type, ConversationStyle style, String value, String star_color) {
        super(tag, type, style);
        this.value = value;
        this.star_color = star_color;
    }

    public String getValue() {
        return value;
    }

    public String getStarColor() {
        return star_color;
    }
}
