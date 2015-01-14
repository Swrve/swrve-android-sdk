package com.swrve.sdk.converser.engine.deserialisers;

import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.swrve.sdk.converser.engine.model.ControlActions;

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
                    HashMap<String, String> visitUriDetails = new HashMap<String, String>();

                    String urlStr = "http://www.google.ie";
                    String refer = "http://converser.io";
                    String ext = "true";

                    if (jsonObject.has(ControlActions.VISIT_URL_URI_KEY)) {
                        urlStr = jsonObject.get(ControlActions.VISIT_URL_URI_KEY).getAsString().replaceAll("\\s", "");
                        urlStr = (urlStr.startsWith("http")) ? urlStr : ("http://" + urlStr);
                    }
                    if (jsonObject.has(ControlActions.VISIT_URL_REFERER_KEY)) {
                        refer = jsonObject.get(ControlActions.VISIT_URL_REFERER_KEY).getAsString().replaceAll("\\s", "");
                    }

                    if (jsonObject.has(ControlActions.VISIT_URL_EXTERNAL_KEY)) {
                        ext = jsonObject.get(ControlActions.VISIT_URL_EXTERNAL_KEY).getAsString().replaceAll("\\s", "");
                    }
                    // Validate the url. Sometimes, the : character of http:// will cause a space to occur
                    visitUriDetails.put(ControlActions.VISIT_URL_URI_KEY, urlStr);
                    visitUriDetails.put(ControlActions.VISIT_URL_REFERER_KEY, refer);
                    visitUriDetails.put(ControlActions.VISIT_URL_EXTERNAL_KEY, ext);
                    actions.includeAction(label, visitUriDetails);
                } else {
                    Log.e("ControlActionsDeserialiser", "Unrecognized Action in json");
                    Log.e("ControlActionsDeserialiser", "JSON :: " + entry.getValue().getAsJsonObject().toString());
                }
            }
            return actions;
        } else {
            return null;
        }
    }

}
