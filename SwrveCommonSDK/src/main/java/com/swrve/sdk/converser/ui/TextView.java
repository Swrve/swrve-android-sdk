package com.swrve.sdk.converser.ui;

import android.content.Context;

import com.swrve.sdk.converser.engine.model.Content;
import com.swrve.sdk.converser.engine.model.ConversationAtom;

public class TextView extends android.widget.TextView implements ConverserContent {
    private Content model;

    public TextView(Context context, Content model) {
        super(context);
        init(model);
    }

    public TextView(Context context, Content model, int style) {
        super(context, null, style);
        init(model);
    }

    private void init(Content model) {
        this.model = model;
        if (model.getType().equals(ConversationAtom.TYPE_CONTENT_TEXT)) {
            setText(model.getValue());
        } else {
            setText(model.getValue());
        }
    }

    @Override
    public Content getModel() {
        return model;
    }
}
