package com.swrve.sdk.conversations.engine.deserialisers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.swrve.sdk.conversations.engine.model.ButtonControl;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.MultiValueInput;
import com.swrve.sdk.conversations.engine.model.StarRating;

import java.lang.reflect.Type;

public class ConversationAtomDeserialiser implements JsonDeserializer<ConversationAtom> {

    @Override
    public ConversationAtom deserialize(JsonElement element, Type type, JsonDeserializationContext jdContext) throws JsonParseException {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            if (!obj.has("type")) {
                //No type? Must be a button
                return jdContext.deserialize(obj, ButtonControl.class);
            }

            String caType = obj.get("type").getAsString();
            String caTag = (obj.has("tag") ? obj.get("tag").getAsString() : null);

            if (caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_HTML)
                    || caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_IMAGE)
                    || caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_VIDEO)
                    || caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_SPACER)) {
                return jdContext.deserialize(obj, Content.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_INPUT_MULTIVALUE)) {
                return jdContext.deserialize(obj, MultiValueInput.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_INPUT_STARRATING)) {
                return jdContext.deserialize(obj, StarRating.class);
            } else {
                return ConversationAtom.create(caTag, caType);
            }
        } else {
            return null;
        }
    }
}
