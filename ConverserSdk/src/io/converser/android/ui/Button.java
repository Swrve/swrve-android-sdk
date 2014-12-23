package io.converser.android.ui;

import android.content.Context;

import io.converser.android.model.ButtonControl;

public class Button extends android.widget.Button implements ConverserControl {

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
