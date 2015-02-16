package com.swrve.sdk.conversations.ui;

import android.content.Context;

import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;

public class ImageView extends android.widget.ImageView implements ConversationContent {
    private final Content model;

    public ImageView(Context context, Content model) {
        super(context); this.model = model;
    }

    @Override
    public ConversationAtom getModel() {
        return model;
    }

}
