package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.swrve.sdk.conversations.R;

public class ConversationRoundedLinearLayout extends LinearLayout {

    private int maxModalWidthPx;
    private float radius;
    private final RectF rect = new RectF();
    private Path path = new Path();

    public ConversationRoundedLinearLayout(Context context) {
        super(context);
        maxModalWidthPx = getResources().getDimensionPixelSize(R.dimen.swrve__conversation_max_modal_width);
    }

    public ConversationRoundedLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        maxModalWidthPx = getResources().getDimensionPixelSize(R.dimen.swrve__conversation_max_modal_width);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        path.reset();
        rect.set(0, 0, w, h);
        float radiusToApply = getWidth() >= maxModalWidthPx ? radius : 0;
        path.addRoundRect(rect, radiusToApply, radiusToApply, Path.Direction.CW);
        path.close();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int save = canvas.save();
        canvas.clipPath(path);
        super.dispatchDraw(canvas);
        canvas.restoreToCount(save);
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public float getRadius() {
        return radius;
    }
}
