package com.swrve.sdk.conversations.engine.deserialisers;

import com.swrve.sdk.SwrveLogger;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.swrve.sdk.conversations.engine.model.ControlActions;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class ControlActionsDeserialiser implements JsonDeserializer<ControlActions> {

    @Override
    public ControlActions deserialize(JsonElement element, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {

        if (element.isJsonObject()) {
            ControlActions actions = new ControlActions();

            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String label = entry.getKey();
                if (label.equalsIgnoreCase(ControlActions.CALL_ACTION.toString())) {
                    String value = entry.getValue().getAsString();
                    actions.includeAction(label, value);
                } else if (label.equalsIgnoreCase(ControlActions.VISIT_URL_ACTION.toString())) {
                    JsonObject jsonObject = entry.getValue().getAsJsonObject();
                    HashMap<String, String> visitUriDetails = new HashMap<>();

                    String urlStr = "http://www.google.ie";
                    String refer = "http://swrve.com";
                    String ext = "true";

                    if (jsonObject.has(ControlActions.VISIT_URL_URI_KEY)) {
                        urlStr = jsonObject.get(ControlActions.VISIT_URL_URI_KEY).getAsString().replaceAll("\\s", "");
                        urlStr = (urlStr.startsWith("http")) ? urlStr : ("http://" + urlStr);
                    }
                    if (jsonObject.has(ControlActions.VISIT_URL_REFERER_KEY)) {
                        refer = jsonObject.get(ControlActions.VISIT_URL_REFERER_KEY).getAsString().replaceAll("\\s", "");
                    }
                    // Validate the url. Sometimes, the : character of http:// will cause a space to occur
                    visitUriDetails.put(ControlActions.VISIT_URL_URI_KEY, urlStr);
                    visitUriDetails.put(ControlActions.VISIT_URL_REFERER_KEY, refer);
                    actions.includeAction(label, visitUriDetails);
                } else if (label.equalsIgnoreCase(ControlActions.DEEPLINK_ACTION.toString())) {
                    JsonObject jsonObject = entry.getValue().getAsJsonObject();
                    HashMap<String, String> deeplinkURIDetails = new HashMap<>();

                    String urlStr = "twitter://"; // Default.

                    if (jsonObject.has(ControlActions.DEEPLINK_URL_URI_KEY)) {
                        urlStr = jsonObject.get(ControlActions.DEEPLINK_URL_URI_KEY).getAsString();
                    }

                    // Validate the url. Sometimes, the : character of http:// will cause a space to occur
                    deeplinkURIDetails.put(ControlActions.DEEPLINK_URL_URI_KEY, urlStr);
                    actions.includeAction(label, deeplinkURIDetails);
                }
                else {
                    SwrveLogger.e("Unrecognized Action in json");
                    SwrveLogger.e("JSON: %s", entry.getValue().getAsJsonObject().toString());
                }
            }
            return actions;
        } else {
            return null;
        }
    }
}
