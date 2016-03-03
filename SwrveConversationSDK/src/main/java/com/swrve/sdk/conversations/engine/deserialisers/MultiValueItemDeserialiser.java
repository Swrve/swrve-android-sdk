package com.swrve.sdk.conversations.engine.deserialisers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.swrve.sdk.conversations.engine.model.ChoiceInputItem;

import java.lang.reflect.Type;

public class MultiValueItemDeserialiser implements JsonDeserializer<ChoiceInputItem> {

    @Override
    public ChoiceInputItem deserialize(JsonElement element, Type type, JsonDeserializationContext jdContext) throws JsonParseException {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            String answerID = obj.get("answer_id").getAsString();
            String answerText = obj.get("answer_text").getAsString();
            return new ChoiceInputItem(answerID, answerText);
        } else {
            return null;
        }
    }
}
