package com.swrve.sdk.messaging;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Implement this interface to handle focus callbacks in in-app messages.
 */
public interface SwrveMessageFocusListener {

    /**
     * This method is invoked when an button is focused in an in-app message. 
     * See {@link android.view.View#onFocusChanged(boolean, int, Rect)}
     *
     * @param view                  The view focused/unfocused. See View.onFocusChanged
     * @param gainFocus             True if the View has focus; false otherwise. See View.onFocusChanged
     * @param direction             The direction focus has moved. See View.onFocusChanged
     * @param previouslyFocusedRect The previously focused view. See View.onFocusChanged
     */
    void onFocusChanged(View view, boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect);
}
