package com.swrve.sdk.converser.engine;

import com.google.ciogson.FieldNamingPolicy;
import com.google.ciogson.Gson;
import com.google.ciogson.GsonBuilder;
import com.swrve.sdk.converser.engine.deserialisers.ControlActionsDeserialiser;
import com.swrve.sdk.converser.engine.deserialisers.ConversationAtomDeserialiser;
import com.swrve.sdk.converser.engine.deserialisers.MultiValueItemDeserialiser;
import com.swrve.sdk.converser.engine.deserialisers.SliderValueDeserialiser;
import com.swrve.sdk.converser.engine.model.ControlActions;
import com.swrve.sdk.converser.engine.model.ConversationAtom;

import com.swrve.sdk.converser.engine.model.MultiValueInput.MultiValueItem;
import com.swrve.sdk.converser.engine.model.SliderInput.SliderValue;

public class GsonHelper {

    public static Gson getConfiguredGson() {
        GsonBuilder db = new GsonBuilder();

        db.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        db.setDateFormat("yyyy-MM-dd HH:mm:ss");

        db.registerTypeAdapter(ConversationAtom.class, new ConversationAtomDeserialiser());
        db.registerTypeAdapter(SliderValue.class, new SliderValueDeserialiser());
        db.registerTypeAdapter(ControlActions.class, new ControlActionsDeserialiser());
        db.registerTypeAdapter(MultiValueItem.class, new MultiValueItemDeserialiser());

        return db.create();
    }
}
