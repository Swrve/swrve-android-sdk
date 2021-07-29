package com.swrve.sdk.messaging.view;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;

import com.swrve.sdk.messaging.SwrveActionType;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public abstract class SwrveBaseInteractableView extends AppCompatImageView {

    public int clickColor;
    public int focusColor;
    private SwrveActionType type;

    public SwrveBaseInteractableView(Context context, SwrveActionType type, int inAppMessageFocusColor, int inAppMessageClickColor) {
        super(context);
        this.type = type;
        this.focusColor = inAppMessageFocusColor;
        this.clickColor = inAppMessageClickColor;
    }

    public SwrveActionType getType() {
        return type;
    }

    /*
     * Darkening effect when user taps on view.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setColorFilter(clickColor);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                clearColorFilter();
                break;
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if(gainFocus) {
            clearColorFilter();
        } else {
            setColorFilter(focusColor);
        }
    }

    abstract String getAction();
}
