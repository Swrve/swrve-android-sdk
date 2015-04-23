package com.swrve.sdk.conversations.engine.model;

import android.net.Uri;

import java.io.Serializable;
import java.util.HashMap;

public class ControlActions implements Serializable {
    public static final Object CALL_ACTION = "call";
    public static final Object VISIT_URL_ACTION = "visit";
    public static final Object DEEPLINK_ACTION = "deeplink";

    public static final String VISIT_URL_URI_KEY = "url";
    public static final String VISIT_URL_REFERER_KEY = "refer";
    public static final String DEEPLINK_URL_URI_KEY = "url";

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

    public boolean isDeepLink() {
        return actionItems.containsKey(DEEPLINK_ACTION);
    }

    public Uri getCallUri() {
        return Uri.parse("tel:" + actionItems.get(CALL_ACTION).toString());
    }

    public HashMap<String, String> getVisitDetails() {
        HashMap<String, String> visitUrlDetails = (HashMap<String, String>) actionItems.get(VISIT_URL_ACTION);
        return visitUrlDetails;
    }

    public HashMap<String, String> getDeepLinkDetails() {
        HashMap<String, String> deepLinkDetails = (HashMap<String, String>) actionItems.get(DEEPLINK_ACTION);
        return deepLinkDetails;
    }
}
