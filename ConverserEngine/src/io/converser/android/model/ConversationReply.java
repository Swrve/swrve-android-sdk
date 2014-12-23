package io.converser.android.model;

import java.util.HashMap;

public class ConversationReply {

    private String control;
    private HashMap<String, Object> data;

    public ConversationReply() {
        data = new HashMap<String, Object>();
    }

    /**
     * Convenience method for manually created TalkBack conversation screens that aren't generating a ConversationReply object dynamically. This will create the right structure for a single 'text' response to a 'reply' control.
     *
     * @param message
     * @return
     */
    public static ConversationReply createTalkbackReply(String message) {
        ConversationReply reply = new ConversationReply();

        reply.control = "reply";
        reply.data.put("text", message);


        return reply;
    }

    public String getControl() {
        return control;
    }

    public void setControl(String control) {
        this.control = control;
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public void setData(HashMap<String, Object> data) {
        this.data = data;
    }
}
