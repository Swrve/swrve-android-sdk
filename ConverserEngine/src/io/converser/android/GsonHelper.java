package io.converser.android;

import com.google.ciogson.FieldNamingPolicy;
import com.google.ciogson.Gson;
import com.google.ciogson.GsonBuilder;

import io.converser.android.deserialisers.ControlActionsDeserialiser;
import io.converser.android.deserialisers.ConversationAtomDeserialiser;
import io.converser.android.deserialisers.MultiValueItemDeserialiser;
import io.converser.android.deserialisers.SliderValueDeserialiser;
import io.converser.android.model.ControlActions;
import io.converser.android.model.ConversationAtom;
import io.converser.android.model.MultiValueInput.MultiValueItem;
import io.converser.android.model.SliderInput.SliderValue;

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
