package com.swrve.sdk.converser.ui;

import android.content.Context;

import com.swrve.sdk.converser.engine.model.Content;
import com.swrve.sdk.converser.engine.model.ConversationAtom;

public class ImageView extends android.widget.ImageView implements ConverserContent {
    private final Content model;

    public ImageView(Context context, Content model) {
        super(context); this.model = model;
    }

    @Override
    public ConversationAtom getModel() {
        return model;
    }

}
