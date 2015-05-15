package com.swrve.sdk.conversations.engine.model;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import java.io.Serializable;

public class ConversationStyle implements Serializable {
    public static final String TYPE_COLOR = "color", TYPE_TRANSPARENT = "transparent";
    private String type;
    private String value;

    public ConversationStyle(){ }

    public Drawable getPrimaryDrawable(){
        if (this.isTypeColor()){
            return new ColorDrawable(Color.parseColor(this.value));
        }else if(this.isTypeTransparent()){
            ColorDrawable c = new ColorDrawable(Color.parseColor("#ffffff"));
            c.setAlpha(0);
            return c;
        }else{
            // We want to rendering to fail. Better to render nothing than something incorrect
            return null;
        }
    }

    public boolean isTypeColor(){
        return TYPE_COLOR.equalsIgnoreCase(this.type);
    }

    public boolean isTypeTransparent(){
        return TYPE_TRANSPARENT.equalsIgnoreCase(this.type);
    }

    public class ForegroundStyle extends ConversationStyle {

    }

    public class BackgroundStyle extends ConversationStyle {

    }

    public class UnrecognizedSwrveStyleException extends Exception {
        public UnrecognizedSwrveStyleException(String message) {
            super(message);
        }
    }
}
