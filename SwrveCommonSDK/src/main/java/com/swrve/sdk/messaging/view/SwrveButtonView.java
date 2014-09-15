package com.swrve.sdk.messaging.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.swrve.sdk.messaging.SwrveActionType;

/**
 * Android view representing a button.
 */
public class SwrveButtonView extends ImageView {
    private static int clickColor = Color.argb(100, 0, 0, 0);

    private SwrveActionType type;

    public SwrveButtonView(Context context, SwrveActionType type) {
        super(context);
        this.type = type;
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
}
