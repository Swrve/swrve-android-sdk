package io.converser.android.model;

import java.util.ArrayList;

public class Conversations {

    private ArrayList<ConversationItem> items;


    public ArrayList<ConversationItem> getItems() {
        if (items == null) {
            items = new ArrayList<ConversationItem>();
        }

        return items;
    }
}
