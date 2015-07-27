package com.swrve.sdk.messaging;

import com.swrve.sdk.messaging.view.SwrveDialog;

/**
 * Implement this interface to manage the in-app message dialogs created
 * by the SDK.
 */
public interface ISwrveDialogListener {
    /**
     * This method is invoked when a dialog message should be shown in your app.
     *
     * @param dialog in-app message dialog ready to be shown.
     */
    void onDialog(SwrveDialog dialog);
}
