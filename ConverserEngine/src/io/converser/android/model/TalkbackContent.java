package io.converser.android.model;

import java.util.Date;

public class TalkbackContent extends Content {

    private String role;
    private Date date;

    public String getRole() {
        return role;
    }

    public Date getDate() {
        return date;
    }

    public boolean isOperator() {
        return role.equalsIgnoreCase("operator");
    }

    public boolean isSubscriber() {
        return role.equalsIgnoreCase("subscriber");
    }

}
