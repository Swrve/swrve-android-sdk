package com.swrve.sdk;

import com.swrve.sdk.SwrveLogger;

/**
 * User internally to assure exceptions won't bubble up when executing a runnable
 * in an executor.
 */
final class SwrveRunnables {

    protected static final String LOG_TAG = "SwrveSDK";

    public static Runnable withoutExceptions(final Runnable r) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                } catch (Exception exp) {
                    SwrveLogger.e(LOG_TAG, "Error executing runnable: ", exp);
                }
            }
        };
    }
}
