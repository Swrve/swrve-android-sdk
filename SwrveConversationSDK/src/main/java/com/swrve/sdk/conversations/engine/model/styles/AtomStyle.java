package com.swrve.sdk.conversations.engine.model.styles;

import android.graphics.Color;
import java.io.Serializable;

public class AtomStyle implements Serializable {
    public static final String TYPE_OUTLINE = "outline", TYPE_SOLID = "solid";

    public int border_radius;
    public String type;
    private BackgroundStyle bg;
    private ForegroundStyle fg;

    public AtomStyle() {
    }

    public int getBorderRadius() {
        return border_radius;
    }

    public BackgroundStyle getBg() {
        return bg;
    }

    public ForegroundStyle getFg() {
        return fg;
    }

    public void setBg(BackgroundStyle bg) {
        this.bg = bg;
    }

    public void setFg(ForegroundStyle fg) {
        this.fg = fg;
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