package com.swrve.sdk.conversations.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.MotionEvent;

import com.swrve.sdk.conversations.engine.model.ButtonControl;
import com.swrve.sdk.conversations.engine.model.styles.AtomStyle;

public class ConversationButton extends android.widget.Button implements ConversationControl {
    private ButtonControl model;
    protected Drawable backgroundDrawable;
    protected int textColor;
    protected int backgroundColor;
    protected int clickTextColor;
    protected int clickBackgroundColor;

    public ConversationButton(Context context, ButtonControl model, int defStyle) {
        super(context, null, defStyle);
        if (model != null) {
            this.model = model;
            setText(model.getDescription());
        }

        AtomStyle style = model.getStyle();
        textColor = style.getTextColorInt();
        backgroundColor = style.getBgColorInt();
        clickTextColor = lerpColor(textColor, (isLight(textColor))? Color.BLACK : Color.WHITE, 0.3f);
        clickBackgroundColor = lerpColor(backgroundColor, (isLight(backgroundColor))? Color.BLACK : Color.WHITE, 0.3f);
        setColors(style, textColor, backgroundColor);
    }

    private void setColors(AtomStyle style, int textColor, int backgroundColor) {
        setTextColor(textColor);

        if (style.isSolidStyle()) {
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setColor(backgroundColor);
            backgroundDrawable = gradientDrawable;
        } else if (style.isOutlineStyle()) {
            // Border (same as text)
            RectF inset = new RectF(6, 6, 6, 6);
            RoundRectShape rr = new RoundRectShape(null, inset, null);
            ShapeDrawable rectShapeDrawable = new ShapeDrawable(rr);
            Paint paint = rectShapeDrawable.getPaint();
            paint.setColor(textColor);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(6);

            // Fill
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setColor(backgroundColor);

            Drawable[] drawables = new Drawable[2];
            drawables[1] = rectShapeDrawable;
            drawables[0] = gradientDrawable;
            backgroundDrawable = new LayerDrawable(drawables);
        } else {
            backgroundDrawable = null;
            // Return null. We want the activity to not be renderable since its an older version of conversations
        }
        setBackgroundForOs(backgroundDrawable);
    }

    @Override
    public ButtonControl getModel() {
        return model;
    }

    @SuppressLint("NewApi")
    public void setBackgroundForOs(Drawable drawable) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            setBackgroundDrawable(drawable); // Deprecated but still in use for now
        } else {
            setBackground(drawable); // Requires minimum api level 16
        }
    }

    public static int lerpColor(int color, int to, float amount) {
        if (color != Color.TRANSPARENT) {
            byte r = (byte) lerp(Color.red(color), Color.red(to), amount),
                    g = (byte) lerp(Color.green(color), Color.green(to), amount),
                    b = (byte) lerp(Color.blue(color), Color.blue(to), amount);
            return Color.rgb(r, g, b);
        }
        return color;
    }

    public static float lerp(float start, float end, float amount) {
        return start + ((end - start) * amount);
    }

    public static boolean isLight(int color) {
        return ((0.2126*(Color.red(color)/255f) + 0.7152*(Color.green(color)/255f) + 0.0722*(Color.blue(color)/255f)) > 0.5f);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        AtomStyle style = model.getStyle();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setColors(style, clickTextColor, clickBackgroundColor);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                setColors(style, textColor, backgroundColor);
                invalidate();
                break;
        }

        return super.onTouchEvent(event);
    }
}
