package com.swrve.sdk.conversations.engine.model.styles;

import android.graphics.Color;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class ConversationStyle implements Serializable {

    public static final String TYPE_OUTLINE = "outline";
    public static final String TYPE_SOLID = "solid";
    public static final String DEFAULT_LB_COLOR = "#B3000000";

    public enum ALIGNMENT {
        @SerializedName("left")
        LEFT,
        @SerializedName("right")
        RIGHT,
        @SerializedName("center")
        CENTER,
        @SerializedName("justified")
        JUSTIFIED
    }

    private int border_radius;
    private String type;
    private ConversationColorStyle bg;
    private ConversationColorStyle fg;
    private ConversationColorStyle lb = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, DEFAULT_LB_COLOR);
    private String font_file;
    private String font_family;
    private int text_size;
    private List<Integer> padding;
    private String line_space;
    private ALIGNMENT alignment;

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

    public String getFontFile() {
        return font_file;
    }

    public String getFontFamily() {
        return font_family;
    }

    public int getTextSize() {
        return text_size;
    }

    public List<Integer> getPadding() {
        return padding;
    }

    public String getLineSpace() {
        return line_space;
    }

    public ALIGNMENT getAlignment() {
        return alignment;
    }
}