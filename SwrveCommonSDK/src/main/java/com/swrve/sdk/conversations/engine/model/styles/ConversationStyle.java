package com.swrve.sdk.conversations.engine.model.styles;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import java.io.Serializable;

public class ConversationStyle implements Serializable {
    public static final String TYPE_COLOR = "color", TYPE_TRANSPARENT = "transparent";
    private String type;
    private String value;

    public ConversationStyle(){ }

    public ConversationStyle(String type, String value){
        this.type = type;
        this.value = value;
    }

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }


    public boolean isTypeColor(){
        return TYPE_COLOR.equalsIgnoreCase(this.type);
    }

    public boolean isTypeTransparent(){
        return TYPE_TRANSPARENT.equalsIgnoreCase(this.type);
    }
}
