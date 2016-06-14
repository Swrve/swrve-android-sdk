package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;

import com.swrve.sdk.conversations.engine.model.Content;

public class ConversationImageViewRounded extends ConversationImageView {

    private final RectF roundRect = new RectF();
    private final Paint maskPaint = new Paint();
    private final Paint zonePaint = new Paint();

    private float borderRadius;

    public ConversationImageViewRounded(Context context, Content model, float borderRadius, int pageBgColor) {
        super(context, model);
        this.borderRadius = borderRadius;

        maskPaint.setAntiAlias(true);
        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        zonePaint.setAntiAlias(true);
        zonePaint.setColor(pageBgColor);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int w = getWidth();
        int h = getHeight();
        roundRect.set(0, 0, w, h + borderRadius);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.saveLayer(roundRect, zonePaint, Canvas.ALL_SAVE_FLAG);
        canvas.drawRoundRect(roundRect, borderRadius, borderRadius, zonePaint);
        canvas.saveLayer(roundRect, maskPaint, Canvas.ALL_SAVE_FLAG);
        super.draw(canvas);
        canvas.restore();
    }

}
