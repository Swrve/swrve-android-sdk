package com.swrve.sdk.conversations.engine;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.swrve.sdk.conversations.engine.deserialisers.ControlActionsDeserialiser;
import com.swrve.sdk.conversations.engine.deserialisers.ConversationAtomDeserialiser;
import com.swrve.sdk.conversations.engine.deserialisers.MultiValueItemDeserialiser;
import com.swrve.sdk.conversations.engine.model.ChoiceInputItem;
import com.swrve.sdk.conversations.engine.model.ControlActions;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;

public class GsonHelper {

    public static Gson getConfiguredGson() {
        GsonBuilder db = new GsonBuilder();

        db.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
        db.setDateFormat("yyyy-MM-dd HH:mm:ss");

        db.registerTypeAdapter(ConversationAtom.class, new ConversationAtomDeserialiser());
        db.registerTypeAdapter(ControlActions.class, new ControlActionsDeserialiser());
        db.registerTypeAdapter(ChoiceInputItem.class, new MultiValueItemDeserialiser());

        return db.create();
    }
}
