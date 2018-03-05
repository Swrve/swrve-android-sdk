package com.swrve.sdk.conversations.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Build;
import android.view.View;

import com.swrve.sdk.conversations.R;

/**
 * Internal Swrve helper class for conversations.
 */
class SwrveConversationHelper {

    /*
     * Converts a percentage radius to a pixel float based from a fixed value.
     */
    public static float getRadiusInPixels(Context context, int borderRadiusPerCent) {
        float borderRadius;
        int height = context.getResources().getDimensionPixelSize(R.dimen.swrve__conversation_control_height);
        float maxRadius = height / 2; // maxRadius is height divide by two
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

    @SuppressLint("NewApi")
    public static void setBackgroundDrawable(View view, Drawable drawable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackgroundDrawable(drawable);
        } else {
            view.setBackground(drawable);
        }
    }
}
