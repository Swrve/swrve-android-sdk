/* 
 * SWRVE CONFIDENTIAL
 * 
 * (c) Copyright 2010-2014 Swrve New Media, Inc. and its licensors.
 * All Rights Reserved.
 * 
 * NOTICE: All information contained herein is and remains the property of Swrve
 * New Media, Inc or its licensors.  The intellectual property and technical
 * concepts contained herein are proprietary to Swrve New Media, Inc. or its
 * licensors and are protected by trade secret and/or copyright law.
 * Dissemination of this information or reproduction of this material is
 * strictly forbidden unless prior written permission is obtained from Swrve.
 */
package com.swrve.sdk.messaging.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.swrve.sdk.messaging.SwrveActionType;

/**
 * Android view representing a button.
 */
public class SwrveButtonView extends ImageView {
    private static int clickColor = Color.argb(100, 0, 0, 0);

    private SwrveActionType type;

    public SwrveButtonView(Context context, SwrveActionType type) {
        super(context);
        this.type = type;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }

    public SwrveActionType getType() {
        return type;
    }

    /**
     * Darkening effect when user taps on view.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setColorFilter(clickColor);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                clearColorFilter();
                break;
        }

        return super.onTouchEvent(event);
    }
}
