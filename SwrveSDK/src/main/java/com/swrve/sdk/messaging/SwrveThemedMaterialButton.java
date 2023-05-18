package com.swrve.sdk.messaging;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewTreeObserver;

import androidx.annotation.Nullable;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.button.MaterialButton;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveTextTemplating;
import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;

import java.io.File;
import java.util.Map;

public class SwrveThemedMaterialButton extends MaterialButton {

    private int[][] stateList = new int[][]{
            new int[]{android.R.attr.state_pressed},
            new int[]{android.R.attr.state_focused},
            new int[]{}
    };

    protected SwrveButton button;
    protected SwrveButtonTheme theme;
    protected SwrveMessageFocusListener messageFocusListener;
    protected SwrveCalibration calibration;
    private String cachePath;
    private String action;
    protected SwrveTextUtils swrveTextUtils = new SwrveTextUtils();

    // exposed for testing
    protected SwrveThemedMaterialButton(Context context, int defStyleAttr) {
        super(context, null, defStyleAttr);
    }

    public SwrveThemedMaterialButton(Context context, int defStyleAttr, SwrveButton button, Map<String, String> inAppPersonalization,
                                     SwrveMessageFocusListener messageFocusListener, SwrveCalibration calibration, String cachePath) throws SwrveSDKTextTemplatingException {
        super(context, null, defStyleAttr);

        this.button = button;
        this.theme = button.getTheme();
        this.messageFocusListener = messageFocusListener;
        this.calibration = calibration;
        this.cachePath = cachePath;

        init(button.getText(), inAppPersonalization);
    }

    protected void init(String text, Map<String, String> inAppPersonalization) throws SwrveSDKTextTemplatingException {

        text = SwrveTextTemplating.apply(text, inAppPersonalization);
        setText(text);
        setCornerRadius(theme.getCornerRadius());

        applyTextAlignment();
        applyFont();
        applyFontColor();
        applyBackground();
        applyBorder();
        applyPadding();
        applyTextSize(); // apply last so text, padding, typeface, etc all get applied first because they are relevant to resizing logic

        setContentDescription(text, inAppPersonalization);
        setAction(inAppPersonalization);

        removeRipple();
    }

    private void applyTextAlignment() {
        if (SwrveHelper.isNullOrEmpty(theme.getHAlign())) {
            return;
        }
        if (theme.getHAlign().equalsIgnoreCase("LEFT")) {
            setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        } else if (theme.getHAlign().equalsIgnoreCase("CENTER")) {
            setGravity(Gravity.CENTER);
        } else if (theme.getHAlign().equalsIgnoreCase("RIGHT")) {
            setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        }
    }

    private void applyFont() {
        Typeface typeface = SwrveTextUtils.getTypeface(theme.getFontFile(), theme.getFontNativeStyle());
        setTypeface(typeface);
    }

    private void applyFontColor() {
        int defaultColor = Color.parseColor(theme.getFontColor());
        int pressedColor = Color.parseColor(theme.getPressedState().getFontColor());
        int focusedColor = pressedColor;
        if (theme.getFocusedState() != null) {
            focusedColor = Color.parseColor(theme.getFocusedState().getFontColor());
        }
        int[] colors = new int[]{pressedColor, focusedColor, defaultColor};
        ColorStateList colorStateList = new ColorStateList(stateList, colors);
        setTextColor(colorStateList);
    }

    private void applyBackground() {
        if (SwrveHelper.isNotNullOrEmpty(theme.getBgColor())) {
            applyBackgroundColor();
        } else {
            applyBackgroundImage();
        }
    }

    private void applyBackgroundColor() {
        int defaultColor = Color.TRANSPARENT;
        if (SwrveHelper.isNotNullOrEmpty(theme.getBgColor())) {
            defaultColor = Color.parseColor(theme.getBgColor());
        }
        int pressedColor = Color.TRANSPARENT;
        if (SwrveHelper.isNotNullOrEmpty(theme.getPressedState().getBgColor())) {
            pressedColor = Color.parseColor(theme.getPressedState().getBgColor());
        }
        int focusedColor = pressedColor;
        if (theme.getFocusedState() != null && SwrveHelper.isNotNullOrEmpty(theme.getFocusedState().getBgColor())) {
            focusedColor = Color.parseColor(theme.getFocusedState().getBgColor());
        }
        int[] colors = new int[]{pressedColor, focusedColor, defaultColor};
        ColorStateList colorStateList = new ColorStateList(stateList, colors);
        setBackgroundTintList(colorStateList);
    }

    private void applyBackgroundImage() {

        Drawable bgDefault = Drawable.createFromPath(cachePath + File.separator + theme.getBgImage());
        Drawable bgPressed = Drawable.createFromPath(cachePath + File.separator + theme.getPressedState().getBgImage());
        Drawable bgFocused = bgPressed;
        if (theme.getFocusedState() != null) {
            bgFocused = Drawable.createFromPath(cachePath + File.separator + theme.getFocusedState().getBgImage());
        }

        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed}, bgPressed);
        states.addState(new int[]{android.R.attr.state_focused}, bgFocused);
        states.addState(new int[]{}, bgDefault);
        setBackgroundDrawable(states);

        setBackgroundTintMode(null); // this must be set if calling setBackgroundDrawable or setBackground See https://stackoverflow.com/questions/52673053/cant-use-androidbackground-with-button-from-the-new-material-components
    }

    private void applyBorder() {
        if (theme.getBorderWidth() == 0) {
            setStrokeWidth(0); // stroke width is 1 by default
            setStrokeColor(null);
            return;
        }
        setStrokeWidth(theme.getBorderWidth());

        int defaultColor = Color.TRANSPARENT;
        if (SwrveHelper.isNotNullOrEmpty(theme.getBorderColor())) {
            defaultColor = Color.parseColor(theme.getBorderColor());
        }
        int pressedColor = Color.TRANSPARENT;
        if (SwrveHelper.isNotNullOrEmpty(theme.getPressedState().getBorderColor())) {
            pressedColor = Color.parseColor(theme.getPressedState().getBorderColor());
        }
        int focusedColor = pressedColor;
        if (theme.getFocusedState() != null && SwrveHelper.isNotNullOrEmpty(theme.getFocusedState().getBorderColor())) {
            focusedColor = Color.parseColor(theme.getFocusedState().getBorderColor());
        }
        int[] colors = new int[]{pressedColor, focusedColor, defaultColor};
        ColorStateList colorStateList = new ColorStateList(stateList, colors);
        setStrokeColor(colorStateList);
    }

    private void applyPadding() {
        // set the inset to zero. If this isn't set, you can see extra padding in the layout bounds debugger.
        setInsetTop(0);
        setInsetBottom(0);

        setIncludeFontPadding(false);

        int leftPadding = theme.getBorderWidth() + theme.getLeftPadding();
        int topPadding = theme.getBorderWidth() + theme.getTopPadding();
        int rightPadding = theme.getBorderWidth() + theme.getRightPadding();
        int bottomPadding = theme.getBorderWidth() + theme.getBottomPadding();
        setPadding(leftPadding, topPadding, rightPadding, bottomPadding);
    }

    private void applyTextSize() {
        setAllCaps(false); // default is true
        setSingleLine();
        float calibratedTextSizePX = swrveTextUtils.getCalibratedTextSize(getTypeface(), theme.getFontSize(), calibration);
        float calibratedTextSizeDP = SwrveHelper.convertPixelsToDp(calibratedTextSizePX, getContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setLetterSpacing(0f);
        }
        if (theme.isTruncate()) {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, calibratedTextSizeDP);
            setEllipsize(TextUtils.TruncateAt.END); // Note: some custom font do not support Ellipse
            setHorizontallyScrolling(true);
        } else {
            setHorizontallyScrolling(false);
            // Set the text size to AutoSize UNIFORM so it resizes to the bounds but then check if the calibrated text size was smaller.
            // If it is smaller, then revert the AutoSize UNIFORM to AutoSize NONE and use the calibrated text size.
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this, 1, 200, 1, COMPLEX_UNIT_DIP);
            addListenerForResizing(calibratedTextSizePX);
        }
    }

    private void addListenerForResizing(float calibratedTextSizePX) {
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (calibratedTextSizePX < getTextSize()) {
                    TextViewCompat.setAutoSizeTextTypeWithDefaults(SwrveThemedMaterialButton.this, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, calibratedTextSizePX);
                }
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    private void setContentDescription(String text, Map<String, String> inAppPersonalization) throws SwrveSDKTextTemplatingException {
        if (button == null) {
            return;
        }
        String accessibilityText = button.getAccessibilityText();
        if (SwrveHelper.isNotNullOrEmpty(accessibilityText)) {
            String personalizedAccessibilityText = SwrveTextTemplating.apply(accessibilityText, inAppPersonalization);
            setContentDescription(personalizedAccessibilityText);
        } else if (SwrveHelper.isNotNullOrEmpty(text)) {
            setContentDescription(text);
        }
    }

    private void removeRipple() {
        // Ripple also adds a dark shade over the button when its in focus, so set to null to fix it.
        // However, setting it to null doesn't work on api 33 and makes the shade even darker, so
        // just apply the fix for below 32. This is possibly a bug in the material components library
        // so test this again when material library is updated.
        // See https://github.com/material-components/material-components-android/issues/3061
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            setRippleColor(null);
        }
    }

    protected void setAction(Map<String, String> inAppPersonalization) throws SwrveSDKTextTemplatingException {
        if (button == null) {
            return;
        }
        if ((button.getActionType() == SwrveActionType.Custom || button.getActionType() == SwrveActionType.CopyToClipboard) && !SwrveHelper.isNullOrEmpty(button.getAction())) {
            this.action = SwrveTextTemplating.apply(button.getAction(), inAppPersonalization);
        } else {
            this.action = button.getAction();
        }
    }

    protected String getAction() {
        return action;
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (messageFocusListener != null) {
            messageFocusListener.onFocusChanged(this, gainFocus, direction, previouslyFocusedRect);
        } else {
            if (gainFocus) {
                SwrveHelper.scaleView(this, 1.0f, 1.2f);
            } else {
                SwrveHelper.scaleView(this, 1.2f, 1.0f);
            }
        }
    }
}
