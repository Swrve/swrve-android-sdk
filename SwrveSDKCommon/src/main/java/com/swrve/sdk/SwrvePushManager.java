package com.swrve.sdk;

import android.os.Bundle;

/**
 * Internal interface for processing messages either from remote push or local geo.
 * Processed differently depending on the platform eg: native vs unity
 */
public interface SwrvePushManager {
    void processMessage(final Bundle msg);
}
