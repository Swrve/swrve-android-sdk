package com.swrve.sdk.messaging;

import android.content.Context;

/**
 * Android image view which can be interpreted as a button
 */
public class SwrveButtonView extends SwrveBaseInteractableView {

    private String action;

    public SwrveButtonView(Context context, SwrveActionType type, int inAppMessageFocusColor, int inAppMessageClickColor, String action) {
        super(context, type, inAppMessageFocusColor, inAppMessageClickColor);
        setFocusable((true));
        this.action = action;
    }

    @Override
    public String getAction() {
        return action;
    }
}
