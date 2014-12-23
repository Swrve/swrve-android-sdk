package io.converser.android.model;

import java.util.ArrayList;

public class ConversationDetail {

    private String name;
    private String title;
    private String subtitle;
    private String description;
    private String conversationName;

    private ArrayList<ConversationAtom> content;
    private ArrayList<ConversationAtom> controls;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

}
