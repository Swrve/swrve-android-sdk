package com.swrve.sdk.conversations.engine.model;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import com.google.gson.Gson;
import com.swrve.sdk.common.R;
import com.swrve.sdk.conversations.engine.GsonHelper;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;

public class ConversationPage implements Serializable {
    private String tag;
    private String title;

    private ArrayList<ConversationAtom> content;
    private ArrayList<ConversationAtom> controls;

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

    public ArrayList<ConversationAtom> getControls() {
        if (controls == null) {
            controls = new ArrayList<ConversationAtom>();
        }
        return controls;
    }

    public static ConversationPage fromJSON(String json) {
        Gson gson = GsonHelper.getConfiguredGson();
        return gson.fromJson(json, ConversationPage.class);
    }


    // Custom Colors and style paramaters
    public int getPrimaryButtonColor(Context c){
        return c.getResources().getColor(R.color.cio__control_primary_color);
    }

    public int getPrimaryButtonTextColor(Context c){
        return c.getResources().getColor(R.color.cio__control_primary_text_color);
    }

    public int getSecondaryButtonColor(Context c){
        return c.getResources().getColor(R.color.cio__control_secondary_color);
    }
    public int getSecondaryButtonTextColor(Context c){
        return c.getResources().getColor(R.color.cio__control_secondary_text_color);
    }

    public int getNeutralButtonColor(Context c){
        return c.getResources().getColor(R.color.cio__control_neutral_color);
    }
    public int getNeutralButtonTextColor(Context c){
        return c.getResources().getColor(R.color.cio__control_neutral_text_color);
    }

    public Drawable getContentBackgroundDrawable(Context c){
        return new ColorDrawable(c.getResources().getColor(R.color.cio__control_content_background_color));
    }

    public Drawable getControlTrayBackgroundDrawable(Context c){
        int colorInt = c.getResources().getColor(R.color.cio__control_tray_color);
        return new ColorDrawable(colorInt);
    }

    public static ConversationPage fromJson(JSONObject json) {
        return fromJSON(json.toString());
    }
}
