package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.swrve.sdk.conversations.R;

public class ConversationRelativeLayout extends RelativeLayout {
    private final int maxWidthPx;
    private final int topBottomPaddingPx;
    private final int paddingForBorder1DpPx;

    public ConversationRelativeLayout(Context context) {
        this(context, null, 0);
    }

    public ConversationRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConversationRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        DisplayMetrics m = getResources().getDisplayMetrics();
        maxWidthPx = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 365, m);
        paddingForBorder1DpPx = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, m);
        topBottomPaddingPx = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, m) - paddingForBorder1DpPx;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // The first child is the container of the conversation (parent linear layout)
        if (getChildCount() > 0) {
            View conversationLayout = getChildAt(0);
            RelativeLayout.LayoutParams lparams = (LayoutParams) conversationLayout.getLayoutParams();
            // If the width is higher than the 365dp (max width) then apply some margin to
            // bottom and top, and a border
            if (this.getWidth() > maxWidthPx) {
                lparams.bottomMargin = lparams.topMargin = topBottomPaddingPx;
                conversationLayout.setPadding(paddingForBorder1DpPx, paddingForBorder1DpPx, paddingForBorder1DpPx, paddingForBorder1DpPx);
                conversationLayout.setBackgroundResource(R.drawable.conversation_border);
                conversationLayout.getLayoutParams().height= ViewGroup.LayoutParams.WRAP_CONTENT;
            } else {
                conversationLayout.getLayoutParams().height= ViewGroup.LayoutParams.MATCH_PARENT;
                conversationLayout.setPadding(0, 0, 0, 0);
            }
        }

        super.onLayout(changed, l, t, r, b);
    }
}
