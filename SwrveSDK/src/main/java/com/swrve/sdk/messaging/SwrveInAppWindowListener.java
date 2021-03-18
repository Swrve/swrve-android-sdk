package com.swrve.sdk.messaging;

import android.view.Window;

/**
 * A Window Listener for when an In App Message Activity is rendered. Use this to change default
 * Window settings such as going into immersive full screen mode.
 */
public interface SwrveInAppWindowListener {

    /**
     * This method is invoked when an In App Message Activity onCreate is finished and
     * setContentView is called.
     *
     * @param window The Window for the In App Message Activity
     */
    void onCreate(Window window);
}
