package com.swrve.sdk.conversations.ui;

import android.content.Context;

import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;

public class ConversationImageView extends android.widget.ImageView implements IConversationContent {
    private final Content model;

    public ConversationImageView(Context context, Content model) {
        super(context);
        this.model = model;
    }

    @Override
    public ConversationAtom getModel() {
        return model;
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = width;
        int intrinsicWidth = getDrawable().getIntrinsicWidth();
        if(intrinsicWidth > 0 ) {
            height = width * getDrawable().getIntrinsicHeight() / intrinsicWidth;
        }
        setMeasuredDimension(width, height);
    }
}
