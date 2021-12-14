package com.swrve.sdk.messaging.view;

import android.graphics.Color;
import android.graphics.Typeface;

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

    private float fontSize;
    public boolean isScrollable;
    private int textBackgroundColor;
    private int textForegroundColor;
    private Typeface textTypeFace;
    private TextAlignment horizontalAlignment;

    private SwrveTextViewStyle(SwrveTextViewStyle.Builder builder) {
        this.fontSize = builder.fontSize;
        this.isScrollable = builder.isScrollable;
        this.textBackgroundColor = builder.textBackgroundColor;
        this.textForegroundColor = builder.textForegroundColor;
        this.textTypeFace = builder.textTypeFace;
        this.horizontalAlignment = builder.horizontalAlignment;
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

    public static class Builder {
        private float fontSize = 0;
        private boolean isScrollable = true;
        private int textBackgroundColor = Color.TRANSPARENT;
        private int textForegroundColor = Color.BLACK;
        private Typeface textTypeFace = null;
        private TextAlignment horizontalAlignment = TextAlignment.Left;

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

        public SwrveTextViewStyle build() {
            return new SwrveTextViewStyle(this);
        }
    }
}
