package com.swrve.sdk.messaging;

import android.graphics.Paint;
import android.graphics.Typeface;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveSDK;

import java.io.File;

class SwrveTextUtils {

    protected static boolean isSystemFont(String fontFile) {
        return SwrveHelper.isNotNullOrEmpty(fontFile) && fontFile.equals("_system_font_");
    }

    protected static Typeface getTypeface(String fontFileString, String fontNativeStyle) {
        Typeface typeface = null;
        if (SwrveTextUtils.isSystemFont(fontFileString)) {
            if (fontNativeStyle.equalsIgnoreCase("Normal")) {
                typeface = Typeface.defaultFromStyle(Typeface.NORMAL);
            } else if (fontNativeStyle.equalsIgnoreCase("Bold")) {
                typeface = Typeface.defaultFromStyle(Typeface.BOLD);
            } else if (fontNativeStyle.equalsIgnoreCase("Italic")) {
                typeface = Typeface.defaultFromStyle(Typeface.ITALIC);
            } else if (fontNativeStyle.equalsIgnoreCase("BoldItalic")) {
                typeface = Typeface.defaultFromStyle(Typeface.BOLD_ITALIC);
            }
        } else if (SwrveHelper.isNotNullOrEmpty(fontFileString)) {
            File fontFile = new File(SwrveSDK.getInstance().getCacheDir(), fontFileString);
            if (fontFile.exists()) {
                typeface = Typeface.createFromFile(fontFile);
            }
        }
        return typeface;
    }

    protected float getCalibratedTextSize(Typeface typeface, float fontSize, SwrveCalibration calibration) {
        float scaledBaseFontSize = 1;
        int baseFontSize = 1;
        if (calibration != null) {
            scaledBaseFontSize = getScaledBaseFontSize(typeface, calibration.getText(), calibration.getWidth(), calibration.getHeight());
            baseFontSize = calibration.getBaseFontSize();
        }
        float scaledFontSize = (fontSize / baseFontSize) * scaledBaseFontSize;
        return scaledFontSize;
    }

    protected float getScaledBaseFontSize(Typeface typeface, String text, int width, int height) {
        Paint paintText = new Paint();
        paintText.setTypeface(typeface);
        return SwrveHelper.getTextSizeToFitImage(paintText, text, width, height);
    }
}
