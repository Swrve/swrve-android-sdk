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
                          SwrveImageFileInfo imageFileInfo, boolean freemarkerEnabled, boolean useLocalTimezone) throws SwrveSDKTextTemplatingException {
        super(context);
        // Thread the campaign flags so accessibility-text templating on no-text background images is
        // consistent with the other widgets (previously this path always used freemarkerEnabled=false).
        this.freemarkerEnabled = freemarkerEnabled;
        this.useLocalTimezone = useLocalTimezone;
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
        return true; // Consume the event to avoid propagating to views below. Note that for Stories, the SwrveMessageView will handle the touch events.
    }
}
