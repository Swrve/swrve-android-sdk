package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.conversations.R;

public class ConversationRelativeLayout extends RelativeLayout {

    private final int topBottomPaddingPx;
    private final int maxModalWidthPx;

    public ConversationRelativeLayout(Context context) {
        this(context, null, 0);
    }

    public ConversationRelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConversationRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFocusable(false);
        topBottomPaddingPx = getResources().getDimensionPixelSize(R.dimen.swrve__conversation_min_modal_top_bottom_padding);
        maxModalWidthPx = getResources().getDimensionPixelSize(R.dimen.swrve__conversation_max_modal_width);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Measure first to know how much space we will take
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        View conversationLayoutModal = findViewById(R.id.swrve__conversation_modal);
        if (conversationLayoutModal == null) {
            SwrveLogger.e("ConversationRelativeLayout missing swrve__conversation_modal layout in xml.");
        } else {
            boolean changedLayoutParams = false;
            // If the container width is more than the 365dp (max width swrve__conversation_max_modal_width)
            // then apply min padding top and bottom and let height wrap content
            LayoutParams lparams = (LayoutParams) conversationLayoutModal.getLayoutParams();
            if (this.getMeasuredWidth() > maxModalWidthPx) {
                if (lparams.bottomMargin != topBottomPaddingPx
                        || lparams.topMargin != topBottomPaddingPx
                        || lparams.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                    lparams.bottomMargin = lparams.topMargin = topBottomPaddingPx;
                    lparams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    changedLayoutParams = true;
                }
            } else {
                if (lparams.bottomMargin != 0
                        || lparams.topMargin != 0
                        || lparams.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                    lparams.bottomMargin = lparams.topMargin = 0;
                    lparams.height = ViewGroup.LayoutParams.MATCH_PARENT;
                    changedLayoutParams = true;
                }
            }

            if (changedLayoutParams) {
              conversationLayoutModal.setLayoutParams(lparams);
              // Measure again to layer properly layout after layout param changes
              super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        }
    }
}
