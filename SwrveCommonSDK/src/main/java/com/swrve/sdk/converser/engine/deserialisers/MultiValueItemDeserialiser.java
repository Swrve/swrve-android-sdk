package com.swrve.sdk.converser.engine.deserialisers;

import com.google.ciogson.JsonDeserializationContext;
import com.google.ciogson.JsonDeserializer;
import com.google.ciogson.JsonElement;
import com.google.ciogson.JsonObject;
import com.google.ciogson.JsonParseException;
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
