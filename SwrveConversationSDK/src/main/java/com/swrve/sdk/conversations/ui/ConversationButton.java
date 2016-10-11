package com.swrve.sdk.conversations.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;

import com.swrve.sdk.conversations.engine.model.ButtonControl;
import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

public class ConversationButton extends android.widget.Button implements IConversationControl {
    private ButtonControl model;
    private int textColor;
    private int textColorPressed;
    private int backgroundColor;
    private int backgroundColorPressed;
    private float borderRadius;

    public ConversationButton(Context context, ButtonControl model, int defStyle, int conversationVersion) {
        super(context, null, defStyle);
        if (model != null) {
            this.model = model;
            setText(model.getDescription());
        }
        this.borderRadius = SwrveConversationHelper.getRadiusInPixels(context, model.getStyle().getBorderRadius());
        initColors();
        initTextColorStates();
        initBackgroundColorStates();
        setLines(1);
        setEllipsize(TextUtils.TruncateAt.END);
        if (conversationVersion >= 4) { // text/font styling added in v4
            SwrveConversationHelper.setTypeface(this, model.getStyle());
            setTextSize(TypedValue.COMPLEX_UNIT_PX, model.getStyle().getTextSize());
            initAlignment();
        }
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
        float radii[] = {borderRadius, borderRadius, borderRadius, borderRadius, borderRadius, borderRadius, borderRadius, borderRadius};
        Drawable backgroundStates = new ColorDrawable();
        if (model.getStyle().isSolidStyle()) {
            Drawable backgroundDrawable = SwrveConversationHelper.createRoundedDrawable(backgroundColor, radii);
            Drawable backgroundDrawablePressed = SwrveConversationHelper.createRoundedDrawable(backgroundColorPressed, radii);
            backgroundStates = getStateListDrawable(backgroundDrawablePressed, backgroundDrawablePressed, backgroundDrawable);
        } else if (model.getStyle().isOutlineStyle()) {
            Drawable drawable = SwrveConversationHelper.createRoundedDrawableWithBorder(backgroundColor, textColor, radii);
            Drawable pressedDrawable = SwrveConversationHelper.createRoundedDrawableWithBorder(backgroundColorPressed, textColorPressed, radii);
            backgroundStates = getStateListDrawable(pressedDrawable, pressedDrawable, drawable);
        }
        setBackgroundForOs(backgroundStates);
    }

    private StateListDrawable getStateListDrawable(Drawable pressedDrawable, Drawable focusedDrawable, Drawable normalDrawable) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[] {android.R.attr.state_pressed}, pressedDrawable);
        states.addState(new int[]{android.R.attr.state_focused}, focusedDrawable);
        states.addState(new int[]{}, normalDrawable);
        return states;
    }

    private void initAlignment() {
        if (model.getStyle().getAlignment() == ConversationStyle.ALIGNMENT.LEFT) {
            setGravity(Gravity.CENTER | Gravity.LEFT);
        } else if (model.getStyle().getAlignment() == ConversationStyle.ALIGNMENT.CENTER) {
            setGravity(Gravity.CENTER | Gravity.CENTER);
        } else if (model.getStyle().getAlignment() == ConversationStyle.ALIGNMENT.RIGHT) {
            setGravity(Gravity.CENTER | Gravity.RIGHT);
        }
    }

    @Override
    public ButtonControl getModel() {
        return model;
    }

    @SuppressLint("NewApi")
    private void setBackgroundForOs(Drawable drawable) {
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

    public float getBorderRadius() {
        return borderRadius;
    }
}