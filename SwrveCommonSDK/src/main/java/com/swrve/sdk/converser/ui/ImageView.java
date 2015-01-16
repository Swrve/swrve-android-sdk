package com.swrve.sdk.converser.ui;

import android.content.Context;

import com.swrve.sdk.converser.engine.model.Content;
import com.swrve.sdk.converser.engine.model.ConversationAtom;

public class ImageView extends android.widget.ImageView implements ConverserContent {

    private Content model;

    public ImageView(Context context, Content model) {
        super(context);
    }

    @Override
    public ConversationAtom getModel() {
        return model;
    }

}
