package com.swrve.sdk.conversations.ui;

import android.content.Context;

import com.swrve.sdk.conversations.engine.model.ButtonControl;

public class Button extends android.widget.Button implements ConversationControl {
    private ButtonControl model;

    public Button(Context context, ButtonControl model, int defStyle) {
        super(context, null, defStyle);

        if (model != null) {
            this.model = model;
            setText(model.getDescription());
        }
    }

    @Override
    public ButtonControl getModel() {
        return model;
    }
}
