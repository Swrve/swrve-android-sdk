package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.conversations.R;

public class ConversationRelativeLayout extends RelativeLayout {

    public ConversationRelativeLayout(Context context) {
        this(context, null, 0);
    }

    public ConversationRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConversationRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        View conversationLayoutModal = findViewById(R.id.swrve__conversation_modal);
        if (conversationLayoutModal == null) {
            SwrveLogger.e("ConversationRelativeLayout missing swrve__conversation_modal layout in xml.");
        } else {
            int maxModalWidthPx = getResources().getDimensionPixelSize(R.dimen.swrve__conversation_max_modal_width);
            // If the container width is more than the 365dp (max width swrve__conversation_max_modal_width )
            // then apply min padding top and bottom and let height wrap content
            if (this.getWidth() > maxModalWidthPx) {
                int topBottomPaddingPx = getResources().getDimensionPixelSize(R.dimen.swrve__conversation_min_modal_top_bottom_padding);
                LayoutParams lparams = (LayoutParams) conversationLayoutModal.getLayoutParams();
                lparams.bottomMargin = lparams.topMargin = topBottomPaddingPx;
                conversationLayoutModal.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            } else {
                LayoutParams lparams = (LayoutParams) conversationLayoutModal.getLayoutParams();
                lparams.bottomMargin = lparams.topMargin = 0;
                lparams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            }
        }
        super.onLayout(changed, l, t, r, b);
    }
}
