package io.converser.android.deserialisers;

import com.google.ciogson.JsonDeserializationContext;
import com.google.ciogson.JsonDeserializer;
import com.google.ciogson.JsonElement;
import com.google.ciogson.JsonObject;
import com.google.ciogson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Map;

import io.converser.android.model.MultiValueInput.MultiValueItem;

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
