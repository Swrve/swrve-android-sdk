package com.swrve.sdk.messaging;

import android.graphics.Color;
import android.graphics.Typeface;

import com.google.gson.annotations.SerializedName;

// Object used to define the text style of a SwrveTextView
public class SwrveTextViewStyle {

    public enum TextAlignment {
        Left, Right, Center;

        public static TextAlignment parse(String alignment) {
            if (alignment.equalsIgnoreCase("right")) {
                return TextAlignment.Right;
            } else if (alignment.equalsIgnoreCase("center")) {
                return TextAlignment.Center;
            } else {
                // Default
                return TextAlignment.Left;
            }
        }
    }

    public enum FONT_NATIVE_STYLE {
        @SerializedName("Normal")
        NORMAL,
        @SerializedName("Bold")
        BOLD,
        @SerializedName("Italic")
        ITALIC,
        @SerializedName("BoldItalic")
        BOLDITALIC;

        public static FONT_NATIVE_STYLE parse(String weight) {
            if (weight.equalsIgnoreCase("NORMAL")) {
                return FONT_NATIVE_STYLE.NORMAL;
            } else if (weight.equalsIgnoreCase("BOLD")) {
                return FONT_NATIVE_STYLE.BOLD;
            } else if (weight.equalsIgnoreCase("ITALIC")) {
                return FONT_NATIVE_STYLE.ITALIC;
            } else if (weight.equalsIgnoreCase("BOLDITALIC")) {
                return FONT_NATIVE_STYLE.BOLDITALIC;
            }
            else {
                // Default
                return FONT_NATIVE_STYLE.NORMAL;
            }
        }
    }

    private float fontSize;
    public boolean isScrollable;
    private int textBackgroundColor;
    private int textForegroundColor;
    private Typeface textTypeFace;
    private TextAlignment horizontalAlignment;
    private double lineHeight;
    private int topPadding;
    private int rightPadding;
    private int bottomPadding;
    private int leftPadding;


    private SwrveTextViewStyle(SwrveTextViewStyle.Builder builder) {
        this.fontSize = builder.fontSize;
        this.isScrollable = builder.isScrollable;
        this.textBackgroundColor = builder.textBackgroundColor;
        this.textForegroundColor = builder.textForegroundColor;
        this.textTypeFace = builder.textTypeFace;
        this.horizontalAlignment = builder.horizontalAlignment;
        this.lineHeight = builder.lineHeight;
        this.topPadding = builder.topPadding;
        this.rightPadding = builder.rightPadding;
        this.bottomPadding = builder.bottomPadding;
        this.leftPadding = builder.leftPadding;
    }

    public float getFontSize() {
        return fontSize;
    }

    public int getTextBackgroundColor() {
        return textBackgroundColor;
    }

    public int getTextForegroundColor() {
        return textForegroundColor;
    }

    public Typeface getTextTypeFace() {
        return textTypeFace;
    }

    public TextAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public double getLineHeight() {
        return lineHeight;
    }

    public int getTopPadding() {
        return topPadding;
    }

    public int getRightPadding() {
        return rightPadding;
    }

    public int getBottomPadding() {
        return bottomPadding;
    }

    public int getLeftPadding() {
        return leftPadding;
    }

    public static class Builder {
        private float fontSize = 0;
        private boolean isScrollable = true;
        private int textBackgroundColor = Color.TRANSPARENT;
        private int textForegroundColor = Color.BLACK;
        private Typeface textTypeFace = null;
        private TextAlignment horizontalAlignment = TextAlignment.Left;
        private double lineHeight = 0;
        private int topPadding = 0;
        private int rightPadding = 0;
        private int bottomPadding = 0;
        private int leftPadding = 0;

        public Builder() {
        }

        public SwrveTextViewStyle.Builder fontSize(float fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        public SwrveTextViewStyle.Builder isScrollable(boolean isScrollable) {
            this.isScrollable = isScrollable;
            return this;
        }

        public SwrveTextViewStyle.Builder textBackgroundColor(int textBackgroundColor) {
            this.textBackgroundColor = textBackgroundColor;
            return this;
        }

        public SwrveTextViewStyle.Builder textForegroundColor(int textForegroundColor) {
            this.textForegroundColor = textForegroundColor;
            return this;
        }

        public SwrveTextViewStyle.Builder textTypeFace(Typeface textTypeFace) {
            this.textTypeFace = textTypeFace;
            return this;
        }

        public SwrveTextViewStyle.Builder horizontalAlignment(TextAlignment horizontalAlignment) {
            this.horizontalAlignment = horizontalAlignment;
            return this;
        }

        public SwrveTextViewStyle.Builder lineHeight(double lineHeight) {
            this.lineHeight = lineHeight;
            return this;
        }

        public SwrveTextViewStyle.Builder topPadding(int topPadding) {
            this.topPadding = topPadding;
            return this;
        }

        public SwrveTextViewStyle.Builder rightPadding(int rightPadding) {
            this.rightPadding = rightPadding;
            return this;
        }

        public SwrveTextViewStyle.Builder bottomPadding(int bottomPadding) {
            this.bottomPadding = bottomPadding;
            return this;
        }

        public SwrveTextViewStyle.Builder leftPadding(int leftPadding) {
            this.leftPadding = leftPadding;
            return this;
        }

        public SwrveTextViewStyle build() {
            return new SwrveTextViewStyle(this);
        }
    }
}
