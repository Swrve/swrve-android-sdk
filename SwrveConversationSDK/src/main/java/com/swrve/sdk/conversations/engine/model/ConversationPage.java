package com.swrve.sdk.conversations.engine.model;

import android.graphics.drawable.Drawable;

import com.google.gson.Gson;
import com.swrve.sdk.conversations.engine.GsonHelper;
import com.swrve.sdk.conversations.engine.model.styles.PageStyle;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class ConversationPage implements Serializable {
    private String tag;
    private String title;
    private PageStyle style;

    private ArrayList<ConversationAtom> content;
    private ArrayList<ButtonControl> controls;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public boolean hasTag(String tag) {
        return getTag().equalsIgnoreCase(tag);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ArrayList<ConversationAtom> getContent() {
        return content;
    }

    public void setContent(ArrayList<ConversationAtom> content) {
        this.content = content;
    }

    public ArrayList<ButtonControl> getControls() {
        if (controls == null) {
            controls = new ArrayList<ButtonControl>();
        }
        return controls;
    }

    public static ConversationPage fromJSON(String json) {
        Gson gson = GsonHelper.getConfiguredGson();
        return gson.fromJson(json, ConversationPage.class);
    }

    // Custom Colors and style paramaters
    public Drawable getBackground(){
        return getStyle().getBg().getPrimaryDrawable();
    }

    public PageStyle getStyle(){
        return this.style;
    }

    public static ConversationPage fromJson(JSONObject json) {
        return fromJSON(json.toString());
    }

}
