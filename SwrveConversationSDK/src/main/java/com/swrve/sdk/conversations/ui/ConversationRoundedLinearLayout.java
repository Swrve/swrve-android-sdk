package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.conversations.R;

public class ConversationRoundedLinearLayout extends LinearLayout {

    private int maxModalWidthPx;
    private float radius;
    private final RectF rect = new RectF();
    private Path path = new Path();

    public ConversationRoundedLinearLayout(Context context) {
        super(context);
        init();
    }

    public ConversationRoundedLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        maxModalWidthPx = getResources().getDimensionPixelSize(R.dimen.swrve__conversation_max_modal_width);
        // clipPath is not supported on some API versions when hardware acceleration is used. Disable
        // for those.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        drawRoundedCorners(w, h);
    }

    private boolean clippingNotSupported = false;

    @Override
    protected void dispatchDraw(Canvas canvas) {
        boolean doClip = (radius > 0 && !clippingNotSupported);
        if (doClip) {
            // Clip the borders
            int save = canvas.save();
            try {
                canvas.clipPath(path);
            } catch (UnsupportedOperationException e) {
                SwrveLogger.e("Could not use clipPath", e);
                clippingNotSupported = true;
            }
            super.dispatchDraw(canvas);
            canvas.restoreToCount(save);
        } else {
            super.dispatchDraw(canvas);
        }
    }

    public void setRadius(float radius) {
        if(this.radius != radius) {
            this.radius = radius;
            drawRoundedCorners(getWidth(), getHeight());
        }
    }

    public float getRadius() {
        return radius;
    }

    private void drawRoundedCorners(int w, int h) {
        path.reset();
        rect.set(0, 0, w, h);
        float radiusToApply = getWidth() >= maxModalWidthPx ? radius : 0;
        path.addRoundRect(rect, radiusToApply, radiusToApply, Path.Direction.CW);
        path.close();
    }
}
