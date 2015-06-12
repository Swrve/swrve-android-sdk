package com.swrve.sdk.conversations.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.text.TextUtils;

import com.swrve.sdk.conversations.engine.model.ButtonControl;

public class ConversationButton extends android.widget.Button implements ConversationControl {
    private ButtonControl model;
    private int textColor;
    private int textColorPressed;
    private int backgroundColor;
    private int backgroundColorPressed;

    public ConversationButton(Context context, ButtonControl model, int defStyle) {
        super(context, null, defStyle);
        if (model != null) {
            this.model = model;
            setText(model.getDescription());
        }
        initColors();
        initTextColorStates();
        initBackgroundColorStates();
        setLines(1);
        setEllipsize(TextUtils.TruncateAt.END);
    }

    private void initColors() {
        textColor = model.getStyle().getTextColorInt();
        textColorPressed = lerpColor(textColor, (isLight(textColor)) ? Color.BLACK : Color.WHITE, 0.3f);
        backgroundColor = model.getStyle().getBgColorInt();
        backgroundColorPressed = lerpColor(backgroundColor, (isLight(backgroundColor)) ? Color.BLACK : Color.WHITE, 0.3f);
    }

    private void initTextColorStates() {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_pressed},
                new int[]{android.R.attr.state_focused},
                new int[]{}
        };
        int[] colors = new int[]{
                textColorPressed,
                textColorPressed,
                textColor
        };
        ColorStateList colorStateList = new ColorStateList(states, colors);
        setTextColor(colorStateList);
    }

    private void initBackgroundColorStates() {
        Drawable backgroundStates = new ColorDrawable();
        if (model.getStyle().isSolidStyle()) {
            GradientDrawable backgroundDrawable = new GradientDrawable();
            backgroundDrawable.setColor(backgroundColor);

            GradientDrawable backgroundDrawablePressed = new GradientDrawable();
            backgroundDrawablePressed.setColor(backgroundColorPressed);

            backgroundStates = getStateListDrawable(backgroundDrawablePressed, backgroundDrawablePressed, backgroundDrawable);
        } else if (model.getStyle().isOutlineStyle()) {
            Drawable drawable = getOutlineDrawable(textColor, backgroundColor);
            Drawable pressedDrawable = getOutlineDrawable(textColorPressed, backgroundColorPressed);
            backgroundStates = getStateListDrawable(pressedDrawable, pressedDrawable, drawable);
        }
        setBackgroundForOs(backgroundStates);
    }

    private Drawable getOutlineDrawable(int borderColor, int backgroundColor) {
        RectF inset = new RectF(6, 6, 6, 6);
        float radii[] = {1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f,1.0f};
        RoundRectShape rr = new RoundRectShape(radii, inset, radii);
        ShapeDrawable rectShapeDrawable = new ShapeDrawable(rr);
        Paint paint = rectShapeDrawable.getPaint();
        paint.setColor(borderColor);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(6);

        // Fill
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setColor(backgroundColor);

        Drawable[] drawables = new Drawable[2];
        drawables[1] = rectShapeDrawable;
        drawables[0] = gradientDrawable;
        return new LayerDrawable(drawables);
    }

    private StateListDrawable getStateListDrawable(Drawable pressedDrawable, Drawable focusedDrawable, Drawable normalDrawable) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_pressed}, pressedDrawable);
        states.addState(new int[] {android.R.attr.state_focused}, focusedDrawable);
        states.addState(new int[]{}, normalDrawable);
        return states;
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

    private int lerpColor(int color, int to, float amount) {
        if (color != Color.TRANSPARENT) {
            byte r = (byte) lerp(Color.red(color), Color.red(to), amount),
                    g = (byte) lerp(Color.green(color), Color.green(to), amount),
                    b = (byte) lerp(Color.blue(color), Color.blue(to), amount);
            return Color.rgb(r, g, b);
        }
        return color;
    }

    private float lerp(float start, float end, float amount) {
        return start + ((end - start) * amount);
    }

    private boolean isLight(int color) {
        return ((0.2126*(Color.red(color)/255f) + 0.7152*(Color.green(color)/255f) + 0.0722*(Color.blue(color)/255f)) > 0.5f);
    }
}