package io.converser.android.model;

import java.util.Date;

public class ConversationItem {

    private String status;
    private String subject;
    private String subtitle;
    private Date createdAt;
    private String conversationId;
    private String ref;
    private String conversationTrackerId;

    public String getStatus() {
        return status;
    }

    public String getSubject() {
        return subject;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getRef() {
        return ref;
    }

    public String getConversationTrackerId() {
        return conversationTrackerId;
    }


}
