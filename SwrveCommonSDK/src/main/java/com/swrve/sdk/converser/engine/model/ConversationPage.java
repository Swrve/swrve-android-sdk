package com.swrve.sdk.converser.engine.model;

import com.google.gson.Gson;
import com.swrve.sdk.converser.engine.GsonHelper;

import org.json.JSONObject;

import java.util.ArrayList;

public class ConversationPage {


    private String tag;
    private String name;
    private String title;
    private String subtitle;
    private String description;
    private String theme;

    private ArrayList<ConversationAtom> content;
    private ArrayList<ConversationAtom> controls;

    // Note that name is actually TAG in the template.
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasName(String tag){
        return getName().equalsIgnoreCase(tag);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public void setControls(ArrayList<ConversationAtom> controls) {
        this.controls = controls;
    }

    public Boolean hasContentAndControls() {
        // If the conversation is missing controls and content, it cannot be rendered.
        return (!getControls().isEmpty() && !getContent().isEmpty());
    }


    public ConversationPage fromJSON(String json){
        Gson gson = GsonHelper.getConfiguredGson();
        return gson.fromJson(json, ConversationPage.class);
    }

    public ConversationPage fromJson(JSONObject json){
        return fromJSON(json.toString());
    }

}
