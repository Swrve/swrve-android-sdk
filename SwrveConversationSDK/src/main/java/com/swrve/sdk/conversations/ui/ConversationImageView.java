package com.swrve.sdk.conversations.ui;

import android.content.Context;

import androidx.appcompat.widget.AppCompatImageView;

public class ConversationImageView extends AppCompatImageView {
    public ConversationImageView(Context context) {
        super(context);
        setFocusable(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = width;
        int intrinsicWidth = getDrawable().getIntrinsicWidth();
        if (intrinsicWidth > 0) {
            height = width * getDrawable().getIntrinsicHeight() / intrinsicWidth;
        }
        setMeasuredDimension(width, height);
    }
}
