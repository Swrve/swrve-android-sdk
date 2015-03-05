package com.swrve.sdk.converser.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import com.swrve.sdk.converser.engine.model.ButtonControl;

public class ConversationButton extends android.widget.Button implements ConverserControl {
    private ButtonControl model;
    protected GradientDrawable gradientDrawable;


    public ConversationButton(Context context, ButtonControl model) {
        super(context, null);
        this.gradientDrawable = new GradientDrawable();
        if (model != null) {
            this.model = model;
            setText(model.getDescription());
        }
    }

    public ConversationButton(Context context, ButtonControl model, int defStyle) {
        super(context, null, defStyle);
        this.gradientDrawable = new GradientDrawable();
        if (model != null) {
            this.model = model;
            setText(model.getDescription());
        }
    }

    @Override
    public ButtonControl getModel() {
        return model;
    }

    @SuppressLint("NewApi")
    public void updateBackground(){
        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            setBackgroundDrawable(gradientDrawable); // Deprecated but still in use for now
        } else {
            setBackground(gradientDrawable); // Requires minimum api level 16
        }
    }

    public void setConversationButtonColor(int color){
        gradientDrawable.setColor(color);
        updateBackground();
    }

    public void setConversationButtonTextColor(int color){
        setTextColor(color);
        updateBackground();
    }

    public void setCurved(){
        gradientDrawable.setCornerRadius(10.0f);
        updateBackground();
    }

    public void setFlat(){
        gradientDrawable.setCornerRadius(0.0f);
        updateBackground();
    }
}
