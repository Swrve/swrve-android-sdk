package com.swrve.sdk.converser.engine.deserialisers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.swrve.sdk.converser.engine.model.MultiValueInput.MultiValueItem;

import java.lang.reflect.Type;
import java.util.Map;

public class MultiValueItemDeserialiser implements JsonDeserializer<MultiValueItem> {

    @Override
    public MultiValueItem deserialize(JsonElement element, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String label = entry.getKey();
                String value = entry.getValue().getAsString();

                return new MultiValueItem(label, value);
            }

            return null;
        } else {
            return null;
        }
    }

}
