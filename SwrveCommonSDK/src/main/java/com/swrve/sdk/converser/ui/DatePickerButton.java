package com.swrve.sdk.converser.ui;

import android.content.Context;
import android.widget.Button;

import com.swrve.sdk.converser.engine.model.DateChoice;

public class DatePickerButton extends Button implements ConverserControl {

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
