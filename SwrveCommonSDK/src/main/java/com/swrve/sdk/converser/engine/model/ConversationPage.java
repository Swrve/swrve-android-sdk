package com.swrve.sdk.converser.engine.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;
import com.swrve.sdk.converser.engine.GsonHelper;

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

    public static ConversationPage fromJson(JSONObject json) {
        return fromJSON(json.toString());
    }
}
