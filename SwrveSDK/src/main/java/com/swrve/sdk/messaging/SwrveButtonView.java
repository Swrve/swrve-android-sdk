package com.swrve.sdk.messaging;

import android.content.Context;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveTextTemplating;
import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;

import java.util.Map;

/**
 * Android image view which can be interpreted as a button
 */
public class SwrveButtonView extends SwrveBaseImageView {

    private String action;
    private SwrveActionType type;

    public SwrveButtonView(Context context, SwrveButton swrveButton, Map<String, String> inAppPersonalization,
                           SwrveMessageFocusListener messageFocusListener, int inAppMessageClickColor,
                           SwrveImageFileInfo imageFileInfo) throws SwrveSDKTextTemplatingException {
        super(context, messageFocusListener, inAppMessageClickColor);
        setFocusable(true);
        setAction(swrveButton, inAppPersonalization);
        this.type = swrveButton.getActionType();
        setContentDescription(swrveButton, inAppPersonalization, null);

        if (imageFileInfo.usingDynamic) {
            setScaleType(ScaleType.FIT_CENTER);
            setAdjustViewBounds(true);
        } else {
            setScaleType(ScaleType.FIT_XY);
        }

        loadImage(imageFileInfo);
    }

    public String getAction() {
        return action;
    }

    public SwrveActionType getType() {
        return type;
    }

    private void setAction(SwrveButton button, Map<String, String> inAppPersonalization) throws SwrveSDKTextTemplatingException {
        if ((button.getActionType() == SwrveActionType.Custom || button.getActionType() == SwrveActionType.CopyToClipboard) && !SwrveHelper.isNullOrEmpty(button.getAction())) {
            this.action = SwrveTextTemplating.apply(button.getAction(), inAppPersonalization);
        } else {
            this.action = button.getAction();
        }
    }
}
