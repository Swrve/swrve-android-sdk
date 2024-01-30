package com.swrve.sdk.messaging;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.swrve.sdk.R;
import com.swrve.sdk.SwrveHelper;

import org.jetbrains.annotations.NotNull;

public class SwrveInAppStoryButton extends MaterialButton {

    protected SwrveMessageFocusListener messageFocusListener;

    public SwrveInAppStoryButton(@NotNull Context context,
                                 @NotNull SwrveStoryDismissButton settings,
                                 @NonNull Point margins,
                                 StateListDrawable drawables,
                                 SwrveMessageFocusListener messageFocusListener) {
        super(context);
        init(settings, margins, drawables, messageFocusListener);
    }

    private void init(@NotNull SwrveStoryDismissButton settings,
                      @NotNull Point margins,
                      StateListDrawable drawables,
                      SwrveMessageFocusListener messageFocusListener) {
        setFocusable(true);
        setClickable(true);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(settings.getSize(), settings.getSize());
        params.gravity = Gravity.RIGHT | Gravity.TOP;
        params.rightMargin = margins.x;
        params.topMargin = margins.y;
        setLayoutParams(params);

        setContentDescription(settings.getAccessibilityText());

        this.messageFocusListener = messageFocusListener;

        //Use the colours from button settings as tints on the background state images
        final int colorDismiss = Color.parseColor(settings.getColor());
        final int colorFocus = settings.getFocusedColor() == null ? colorDismiss : Color.parseColor(settings.getFocusedColor());
        final int colorPressed = Color.parseColor(settings.getPressedColor());

        if(drawables != null){
            setBackgroundDrawable(drawables);
        } else {
            Drawable buttonImage = getResources().getDrawable(R.drawable.swrve_x_xml);
            //Add the background image, including for focus state (to override material's default masking effect)
            StateListDrawable states = new StateListDrawable();
            states.addState(new int[]{android.R.attr.state_pressed}, buttonImage);
            states.addState(new int[]{android.R.attr.state_focused}, buttonImage);
            states.addState(new int[]{}, buttonImage);
            setBackgroundDrawable(states);
        }

        setBackgroundTintList(new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_pressed},
                        new int[]{android.R.attr.state_focused},
                        new int[]{}
                },
                new int[]{
                        colorPressed,
                        colorFocus,
                        colorDismiss
                }
        ));
        setBackgroundTintMode(PorterDuff.Mode.SRC_ATOP);

        setRippleColor(null);
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
