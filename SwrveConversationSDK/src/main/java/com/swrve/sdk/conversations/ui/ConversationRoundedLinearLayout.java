package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class ConversationRoundedLinearLayout extends LinearLayout {

    private float radius;
    private final RectF rect = new RectF();
    private Path path = new Path();

    public ConversationRoundedLinearLayout(Context context) {
        super(context);
    }

    public ConversationRoundedLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        path.reset();
        rect.set(0, 0, w, h);
        path.addRoundRect(rect, radius, radius, Path.Direction.CW);
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
