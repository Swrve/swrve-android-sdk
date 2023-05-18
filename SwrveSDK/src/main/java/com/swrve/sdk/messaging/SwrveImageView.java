package com.swrve.sdk.messaging;

import android.content.Context;
import android.view.MotionEvent;

import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;

import java.util.Map;

/**
 * Android view representing a background image.
 */
public class SwrveImageView extends SwrveBaseImageView {

    public SwrveImageView(Context context, SwrveImage image, Map<String, String> inAppPersonalization,
                          SwrveImageFileInfo imageFileInfo) throws SwrveSDKTextTemplatingException {
        super(context);
        setContentDescription(image, inAppPersonalization, null);
        setFocusable(false);
        if (imageFileInfo.usingDynamic) {
            setScaleType(ScaleType.FIT_CENTER);
            setAdjustViewBounds(true);
        } else {
            setScaleType(ScaleType.FIT_XY);
        }

        loadImage(imageFileInfo);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
}
