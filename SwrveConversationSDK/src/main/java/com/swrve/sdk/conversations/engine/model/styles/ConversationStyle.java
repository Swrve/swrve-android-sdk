package com.swrve.sdk.conversations.engine.model.styles;

import android.graphics.Color;
import java.io.Serializable;

public class ConversationStyle implements Serializable {

    public static final String TYPE_OUTLINE = "outline";
    public static final String TYPE_SOLID = "solid";
    public static final String DEFAULT_LB_COLOR = "#B3000000";

    public int border_radius;
    public String type;
    private ConversationColorStyle bg;
    private ConversationColorStyle fg;
    private ConversationColorStyle lb = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, DEFAULT_LB_COLOR);

    public ConversationStyle() {
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

    public void setBg(ConversationColorStyle bg) {
        this.bg = bg;
    }

    public void setFg(ConversationColorStyle fg) {
        this.fg = fg;
    }

    public void setLb(ConversationColorStyle lb) {
        this.lb = lb;
    } // todo remove this

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