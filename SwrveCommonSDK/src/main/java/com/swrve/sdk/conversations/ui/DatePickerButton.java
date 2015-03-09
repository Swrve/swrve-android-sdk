package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.widget.Button;

import com.swrve.sdk.conversations.engine.model.DateChoice;

public class DatePickerButton extends Button implements ConversationControl {
    private DateChoice model;

    public DatePickerButton(Context context, DateChoice choice) {
        super(context);

        this.model = choice;
    }

    @Override
    public DateChoice getModel() {
        return model;
    }
}
