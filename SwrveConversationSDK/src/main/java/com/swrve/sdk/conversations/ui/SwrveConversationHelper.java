package com.swrve.sdk.conversations.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.view.View;
import android.widget.TextView;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.conversations.R;
import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

import java.io.File;

/**
 * Internal Swrve helper class for conversations.
 */
class SwrveConversationHelper {

    /**
     * Converts a percentage radius to a pixel float based from a fixed value.
     * @param context
     * @param borderRadiusPerCent
     * @return
     */
    public static float getRadiusInPixels(Context context, int borderRadiusPerCent) {
        float borderRadius;
        int[] attrs = {android.R.attr.minHeight};
        TypedArray ta = context.obtainStyledAttributes(R.style.cio__control_button, attrs);
        String height = ta.getString(0);
        height = height.contains("dip") ? height.substring(0, height.indexOf("dip")) : height;
        float maxRadius = SwrveHelper.convertDipToPixels(context, Float.parseFloat(height))/2; // maxRadius is height divide by two
        if (borderRadiusPerCent >= 100) {
            borderRadius = maxRadius;
        } else {
            borderRadius = ((borderRadiusPerCent * maxRadius) / 100f);
        }
        return borderRadius;
    }

    /**
     * Creates a Drawable filled with color and with rounded corners
     * @param color the color to fill in
     * @param radii Each corner in drawable has 2 radii (xradius, yradius), giving 8 values
     * @return A color filled drawable
     */
    public static Drawable createRoundedDrawable(int color, float radii[]) {
        if(radii.length != 8) {
            return null; // fail fast
        }
        RoundRectShape rr = new RoundRectShape(radii, null, radii);
        ShapeDrawable rectShapeDrawable = new ShapeDrawable(rr);
        Paint paint = rectShapeDrawable.getPaint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        return rectShapeDrawable;
    }

    /**
     * Creates a Drawable filled with color, and a border, and with rounded corners
     * @param color the color to fill in
     * @param borderColor the color of the border
     * @param radii Each corner in drawable has 2 radii (xradius, yradius), giving 8 values
     * @return A color filled drawable with border
     */
    public static Drawable createRoundedDrawableWithBorder(int color, int borderColor, float radii[]) {
        RectF inset = new RectF(6, 6, 6, 6);
        RoundRectShape rr = new RoundRectShape(radii, inset, radii);
        ShapeDrawable rectShapeDrawable = new ShapeDrawable(rr);
        Paint paint = rectShapeDrawable.getPaint();
        paint.setColor(borderColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(6);

        // Fill
        Drawable fillDrawable = SwrveConversationHelper.createRoundedDrawable(color, radii);

        Drawable[] drawables = new Drawable[2];
        drawables[1] = rectShapeDrawable;
        drawables[0] = fillDrawable;
        return new LayerDrawable(drawables);
    }

    /**
     * Sets the typeface for a given TextView using a custom font.
     * @param textView a Button/Option/Textview on which to configure the typeface
     * @param style Contains the typeface details.
     */
    public static void setTypeface(TextView textView, ConversationStyle style, File cacheDir) {
        if (SwrveHelper.isNotNullOrEmpty(style.getFontFile())) {
            File fontFile = new File(cacheDir, style.getFontFile());
            if (fontFile.exists()) {
                textView.setTypeface(Typeface.createFromFile(fontFile));
            }
        }
    }

    @SuppressLint("NewApi")
    public static void setBackgroundDrawable(View view, Drawable drawable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackgroundDrawable(drawable);
        } else {
            view.setBackground(drawable);
        }
    }
}
