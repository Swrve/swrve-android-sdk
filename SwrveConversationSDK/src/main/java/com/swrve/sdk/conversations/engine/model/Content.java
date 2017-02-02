package com.swrve.sdk.conversations.engine.model;

import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Content extends ConversationAtom {
    public static final String YOUTUBE_VIDEO_ID_REGEX = "^https?://.*(?:youtu.be/|v/|u/\\w/|embed/|watch\\?v=)([^#&?]*).*$";

    protected String value;
    protected String height;

    public Content(String tag, TYPE type, ConversationStyle style, String value, String height) {
        super(tag, type, style);
        this.value = value;
        this.height = height;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getHeight() {
        if (height == null || Integer.parseInt(height) <= 0) {
            return "0";
        } else {
            return height;
        }
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getYoutubeVideoId(){
        String videoId = null;
        Pattern pattern = Pattern.compile(YOUTUBE_VIDEO_ID_REGEX, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(getValue());
        if (matcher.matches()){
            videoId = matcher.group(1);
        }
        return videoId;
    }
}
