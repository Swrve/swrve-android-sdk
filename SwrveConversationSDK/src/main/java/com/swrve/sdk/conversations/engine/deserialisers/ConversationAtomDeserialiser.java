package com.swrve.sdk.conversations.engine.deserialisers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.MultiValueInput;
import com.swrve.sdk.conversations.engine.model.StarRating;

import java.lang.reflect.Type;

import static com.swrve.sdk.conversations.engine.model.ConversationAtom.TYPE.*;

public class ConversationAtomDeserialiser implements JsonDeserializer<ConversationAtom> {

    @Override
    public ConversationAtom deserialize(JsonElement element, Type type, JsonDeserializationContext jdContext) throws JsonParseException {

        ConversationAtom conversationAtom;
        JsonObject obj = element.getAsJsonObject();
        ConversationAtom.TYPE caType = parse(obj.get("type").getAsString());
        switch (caType) {
            case CONTENT_HTML:
            case CONTENT_IMAGE:
            case CONTENT_VIDEO:
            case CONTENT_SPACER:
                conversationAtom = jdContext.deserialize(obj, Content.class);
                break;
            case INPUT_MULTIVALUE:
                conversationAtom = jdContext.deserialize(obj, MultiValueInput.class);
                break;
            case INPUT_STARRATING:
                conversationAtom = jdContext.deserialize(obj, StarRating.class);
                break;
            case UNKNOWN:
            default:
                String caTag = (obj.has("tag") ? obj.get("tag").getAsString() : null);
                conversationAtom = ConversationAtom.create(caTag, caType);
        }
        return conversationAtom;
    }

    private ConversationAtom.TYPE parse(String type) {
        if (type.equalsIgnoreCase("html-fragment")) {
            return CONTENT_HTML;
        } else if (type.equalsIgnoreCase("image")) {
            return CONTENT_IMAGE;
        } else if (type.equalsIgnoreCase("video")) {
            return CONTENT_VIDEO;
        } else if (type.equalsIgnoreCase("spacer")) {
            return CONTENT_SPACER;
        } else if (type.equalsIgnoreCase("multi-value-input")) {
            return INPUT_MULTIVALUE;
        } else if (type.equalsIgnoreCase("star-rating")) {
            return INPUT_STARRATING;
        }
        return UNKNOWN;
    }
}
