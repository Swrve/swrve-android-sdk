package io.converser.android.engine.model;

import android.net.Uri;

import java.util.HashMap;

public class ControlActions {

    public static final Object CALL_ACTION = "call";
    public static final Object VISIT_URL_ACTION = "visit";

    public static final String VISIT_URL_URI_KEY = "url";
    public static final String VISIT_URL_REFERER_KEY = "refer";
    public static final String VISIT_URL_EXTERNAL_KEY = "ext";

    private HashMap<String, Object> actionItems = new HashMap<String, Object>();

    public void includeAction(String name, Object value) {
        actionItems.put(name, value);
    }

    public boolean isCall() {
        return actionItems.containsKey(CALL_ACTION);
    }

    public boolean isVisit() {
        return actionItems.containsKey(VISIT_URL_ACTION);
    }

    public Uri getCallUri() {
        return Uri.parse("tel:" + actionItems.get(CALL_ACTION).toString());
    }

    public HashMap<String, String> getVisitDetails() {
        HashMap<String, String> visitUrlDetails = (HashMap<String, String>) actionItems.get(VISIT_URL_ACTION);
        return visitUrlDetails;
    }
}
