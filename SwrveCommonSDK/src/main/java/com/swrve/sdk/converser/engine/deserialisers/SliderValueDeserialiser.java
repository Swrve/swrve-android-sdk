package com.swrve.sdk.converser.engine.deserialisers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.swrve.sdk.converser.engine.model.SliderInput.SliderValue;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * A Deserializer for SliderValue types
 * Converts a json frag like this :
 * <p/>
 * {"Poor" : 1}
 * <p/>
 * into an object. Assumes there's only one key and value pair
 *
 * @author Jason Connery
 */
public class SliderValueDeserialiser implements JsonDeserializer<SliderValue> {

    @Override
    public SliderValue deserialize(JsonElement element, Type type, JsonDeserializationContext jdcontext) throws JsonParseException {

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    String label = entry.getKey();
                    int value = entry.getValue().getAsInt();

                    return new SliderValue(label, value);
                }
            }

            return null;
        } else {
            return null;
        }
    }
}
