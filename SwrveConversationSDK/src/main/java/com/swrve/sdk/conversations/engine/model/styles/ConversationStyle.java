package com.swrve.sdk.conversations.engine.model.styles;

import android.graphics.Color;
import java.io.Serializable;

public class ConversationStyle implements Serializable {

    public static final String TYPE_OUTLINE = "outline";
    public static final String TYPE_SOLID = "solid";
    public static final String DEFAULT_LB_COLOR = "#B3000000";

    private int border_radius;
    private String type;
    private ConversationColorStyle bg;
    private ConversationColorStyle fg;
    private ConversationColorStyle lb = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, DEFAULT_LB_COLOR);

    public ConversationStyle() { // empty constructor needed for gson
    }

    public ConversationStyle(int border_radius, String type, ConversationColorStyle bg, ConversationColorStyle fg, ConversationColorStyle lb){
        this.border_radius = border_radius;
        this.type = type;
        this.bg = bg;
        this.fg = fg;
        this.lb = lb;
    }

    public int getBorderRadius() {
        return border_radius;
    }

    public ConversationColorStyle getBg() {
        return bg;
    }

    public ConversationColorStyle getFg() {
        return fg;
    }

    public ConversationColorStyle getLb() {
        return lb;
    }

    public int getBgColorInt() {
        if (getBg().isTypeColor()){
            String colorHex = getBg().getValue();
            return Color.parseColor(colorHex);
        }else{
            return Color.TRANSPARENT;
        }
    }

    public int getTextColorInt() {
        String colorHex = getFg().getValue();
        return Color.parseColor(colorHex);
    }

    public boolean isSolidStyle(){
        return this.type.equalsIgnoreCase(TYPE_SOLID);
    }

    public boolean isOutlineStyle(){
        return this.type.equalsIgnoreCase(TYPE_OUTLINE);
    }
}