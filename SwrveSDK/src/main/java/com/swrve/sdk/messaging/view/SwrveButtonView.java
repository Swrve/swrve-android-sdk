package com.swrve.sdk.messaging.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.swrve.sdk.messaging.SwrveActionType;

/**
 * Android view representing a button.
 */
public class SwrveButtonView extends ImageView {
    public int clickColor;
    public int focusColor;

    private SwrveActionType type;

    public SwrveButtonView(Context context, SwrveActionType type, int inAppMessageFocusColor, int inAppMessageClickColor) {
        super(context);
        this.type = type;
        this.focusColor = inAppMessageFocusColor;
        this.clickColor = inAppMessageClickColor;
        setFocusable(true);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }

    public SwrveActionType getType() {
        return type;
    }

    /**
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

}
