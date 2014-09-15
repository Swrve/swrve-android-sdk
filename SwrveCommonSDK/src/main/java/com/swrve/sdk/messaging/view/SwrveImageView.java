package com.swrve.sdk.messaging.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * Android view representing a background image.
 */
public class SwrveImageView extends ImageView {

    public SwrveImageView(Context context) {
        super(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
}
