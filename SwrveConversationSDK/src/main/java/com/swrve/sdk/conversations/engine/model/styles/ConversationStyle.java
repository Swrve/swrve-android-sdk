package com.swrve.sdk.conversations.engine.model.styles;

import android.graphics.Color;
import android.graphics.Typeface;

import com.google.gson.annotations.SerializedName;
import com.swrve.sdk.SwrveConversationConstants;
import com.swrve.sdk.SwrveHelper;

import java.io.Serializable;

public class ConversationStyle implements Serializable {

    private static final String TYPE_OUTLINE = "outline";
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

    public enum FONT_NATIVE_STYLE {
        @SerializedName("Normal")
        NORMAL,
        @SerializedName("Bold")
        BOLD,
        @SerializedName("Italic")
        ITALIC,
        @SerializedName("BoldItalic")
        BOLDITALIC
    }

    private int border_radius;
    private String type;
    private ConversationColorStyle bg;
    private ConversationColorStyle fg;
    private ConversationColorStyle lb = new ConversationColorStyle(ConversationColorStyle.TYPE_COLOR, DEFAULT_LB_COLOR);
    private String font_file;
    private String font_digest;
    private String font_family;
    private String font_postscript_name;
    private FONT_NATIVE_STYLE font_native_style;
    private int text_size;
    private ALIGNMENT alignment;

    private transient Typeface typeface; // not generated from json

    public ConversationStyle() { // empty constructor needed for gson
    }

    public ConversationStyle(int border_radius, String type, ConversationColorStyle bg, ConversationColorStyle fg, ConversationColorStyle lb){
        this.border_radius = border_radius;
        this.type = type;
        this.bg = bg;
        this.fg = fg;
        this.lb = lb;
    }

    public ConversationStyle(int border_radius, String type, ConversationColorStyle bg, ConversationColorStyle fg, ConversationColorStyle lb, String font_file,
                             String font_digest, String font_family, int text_size, ALIGNMENT alignment, FONT_NATIVE_STYLE font_native_style) {
        this.border_radius = border_radius;
        this.type = type;
        this.bg = bg;
        this.fg = fg;
        this.lb = lb;
        this.font_file = font_file;
        this.font_digest = font_digest;
        this.font_family = font_family;
        this.text_size = text_size;
        this.alignment = alignment;
        this.font_native_style = font_native_style;
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

    public void setFg(ConversationColorStyle fg) {
        this.fg = fg;
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

    public Typeface getTypeface() {
        return typeface;
    }

    public void setTypeface(Typeface typeface) {
        this.typeface = typeface;
    }

    public String getFontFile() {
        return font_file;
    }

    public String getFontDigest() {
        return font_digest;
    }

    public String getFontFamily() {
        return font_family;
    }

    public String getFontPostscriptName() {
        return font_postscript_name;
    }

    public FONT_NATIVE_STYLE getFontNativeStyle() {
        return font_native_style;
    }

    public int getTextSize() {
        return text_size;
    }

    public void setTextSize(int text_size) {
        this.text_size = text_size;
    }

    public ALIGNMENT getAlignment() {
        return alignment;
    }

    public boolean isSystemFont() {
        return SwrveHelper.isNotNullOrEmpty(font_file) && font_file.equals(SwrveConversationConstants.SYSTEM_FONT);
    }
}