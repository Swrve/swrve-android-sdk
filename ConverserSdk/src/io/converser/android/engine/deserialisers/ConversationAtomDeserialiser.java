package io.converser.android.engine.deserialisers;

import com.google.ciogson.JsonDeserializationContext;
import com.google.ciogson.JsonDeserializer;
import com.google.ciogson.JsonElement;
import com.google.ciogson.JsonObject;
import com.google.ciogson.JsonParseException;

import java.lang.reflect.Type;

import io.converser.android.engine.model.ButtonControl;
import io.converser.android.engine.model.CalendarInput;
import io.converser.android.engine.model.Content;
import io.converser.android.engine.model.ConversationAtom;
import io.converser.android.engine.model.DateChoice;
import io.converser.android.engine.model.DateSaver;
import io.converser.android.engine.model.MultiValueInput;
import io.converser.android.engine.model.MultiValueLongInput;
import io.converser.android.engine.model.NPSInput;
import io.converser.android.engine.model.ReactionInput;
import io.converser.android.engine.model.SliderInput;
import io.converser.android.engine.model.TextInput;

public class ConversationAtomDeserialiser implements JsonDeserializer<ConversationAtom> {

    @Override
    public ConversationAtom deserialize(JsonElement element, Type type, JsonDeserializationContext jdcontext) throws JsonParseException {
        //What we should hopefully be able to do there is determine type of thingy by type string comparsion,
        //then once a more concrete type is determined, use it's deserialiser

        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            if (!obj.has("type")) {
                //No type? Must be a button
                return jdcontext.deserialize(obj, ButtonControl.class);
            }

            String caType = obj.get("type").getAsString();
            String caTag = (obj.has("tag") ? obj.get("tag").getAsString() : null);

           if (caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_AUDIO)
                    || caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_HTML)
                    || caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_IMAGE)
                    || caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_TEXT)
                    || caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_VIDEO)) {
                return jdcontext.deserialize(obj, Content.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTROL_DATESAVER)) {
                return jdcontext.deserialize(obj, DateSaver.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_CONTROL_DATECHOICE)) {
                return jdcontext.deserialize(obj, DateChoice.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_INPUT_TEXTINPUT)) {
                return jdcontext.deserialize(obj, TextInput.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_INPUT_MULTIVALUE)) {
                return jdcontext.deserialize(obj, MultiValueInput.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_INPUT_MULTIVALUELONG)) {
                return jdcontext.deserialize(obj, MultiValueLongInput.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_INPUT_REACTION)) {
                return jdcontext.deserialize(obj, ReactionInput.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_INPUT_SLIDER)) {
                return jdcontext.deserialize(obj, SliderInput.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_INPUT_NETPROMOTER)) {
                return jdcontext.deserialize(obj, NPSInput.class);
            } else if (caType.equalsIgnoreCase(ConversationAtom.TYPE_INPUT_CALENDAR_INPUT)) {
                return jdcontext.deserialize(obj, CalendarInput.class);
            } else {
                //All else has failed? just return a plain Atom, *shrug*
                //We should end up with types for everything, but this will probably get used a lot during initial dev

                return ConversationAtom.create(caTag, caType);
            }
        } else {
            //If it's not an object type, somethings gone wrong
            return null;
        }
    }

}
