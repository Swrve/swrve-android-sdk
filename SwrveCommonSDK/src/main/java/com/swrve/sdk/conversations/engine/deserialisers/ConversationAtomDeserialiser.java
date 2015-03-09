package com.swrve.sdk.conversations.engine.deserialisers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.swrve.sdk.conversations.engine.model.ButtonControl;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.DateChoice;
import com.swrve.sdk.conversations.engine.model.DateSaver;
import com.swrve.sdk.conversations.engine.model.MultiValueInput;
import com.swrve.sdk.conversations.engine.model.MultiValueLongInput;
import com.swrve.sdk.conversations.engine.model.NPSInput;
import com.swrve.sdk.conversations.engine.model.ReactionInput;
import com.swrve.sdk.conversations.engine.model.SliderInput;
import com.swrve.sdk.conversations.engine.model.TextInput;

import java.lang.reflect.Type;

public class ConversationAtomDeserialiser implements JsonDeserializer<ConversationAtom> {

    @Override
    public ConversationAtom deserialize(JsonElement element, Type type, JsonDeserializationContext jdContext) throws JsonParseException {
        //What we should hopefully be able to do there is determine type of thingy by type string comparison,
        //then once a more concrete type is determined, use it's deserialiser

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            if (!obj.has("type")) {
                //No type? Must be a button
                return jdContext.deserialize(obj, ButtonControl.class);
            }

            String caType = obj.get("type").getAsString();
            String caTag = (obj.has("tag") ? obj.get("tag").getAsString() : null);
            String caTarget = (obj.has("target") ? obj.get("target").getAsString() : null);

            if (caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_AUDIO)
                    || caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_HTML)
                    || caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_IMAGE)
                    || caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_TEXT)
                    || caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_VIDEO)) {
                return jdContext.deserialize(obj, Content.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTROL_DATESAVER)) {
                return jdContext.deserialize(obj, DateSaver.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTROL_DATECHOICE)) {
                return jdContext.deserialize(obj, DateChoice.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_INPUT_TEXTINPUT)) {
                return jdContext.deserialize(obj, TextInput.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_INPUT_MULTIVALUE)) {
                return jdContext.deserialize(obj, MultiValueInput.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_INPUT_MULTIVALUELONG)) {
                return jdContext.deserialize(obj, MultiValueLongInput.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_INPUT_REACTION)) {
                return jdContext.deserialize(obj, ReactionInput.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_INPUT_SLIDER)) {
                return jdContext.deserialize(obj, SliderInput.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_INPUT_NETPROMOTER)) {
                return jdContext.deserialize(obj, NPSInput.class);
            } else {
                //All else has failed? just return a plain Atom, *shrug*
                //We should end up with types for everything, but this will probably get used a lot during initial dev

                return ConversationAtom.create(caTag, caType, caTarget);
            }
        } else {
            //If it's not an object type, somethings gone wrong
            return null;
        }
    }
}
