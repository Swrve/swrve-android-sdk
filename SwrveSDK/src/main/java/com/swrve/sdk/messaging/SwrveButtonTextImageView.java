package com.swrve.sdk.messaging;

import android.content.Context;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveTextTemplating;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;

import java.util.Map;

public class SwrveButtonTextImageView extends SwrveTextImageView {

    private String action;

    private SwrveActionType type;

    public SwrveButtonTextImageView(Context context, SwrveButton swrveButton, Map<String, String> inAppPersonalization,
                                    SwrveInAppMessageConfig inAppConfig, int canvasWidth, int canvasHeight) throws SwrveSDKTextTemplatingException {
        super(context, swrveButton, inAppPersonalization, inAppConfig, canvasWidth, canvasHeight);
        setAction(swrveButton, inAppPersonalization);
        this.type = swrveButton.getActionType();
        setContentDescription(swrveButton, inAppPersonalization, text); // the text must be personalized already, which happens in superclass SwrveTextImageView
        setFocusable(true);
    }

    private void setAction(SwrveButton swrveButton, Map<String, String> inAppPersonalization) throws SwrveSDKTextTemplatingException {
        if ((swrveButton.getActionType() == SwrveActionType.Custom || swrveButton.getActionType() == SwrveActionType.CopyToClipboard) && !SwrveHelper.isNullOrEmpty(swrveButton.getAction())) {
            this.action = SwrveTextTemplating.apply(swrveButton.getAction(), inAppPersonalization);
        } else {
            this.action = swrveButton.getAction();
        }
    }

    protected String getAction() {
        return action;
    }

    public SwrveActionType getType() {
        return type;
    }
}
