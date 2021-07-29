package com.swrve.sdk.messaging.view;

import android.content.Context;
import android.view.MotionEvent;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * Android view representing a background image.
 */
public class SwrveImageView extends AppCompatImageView {

    public SwrveImageView(Context context) {
        super(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
}
