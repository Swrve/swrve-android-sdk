package com.swrve.sdk;

/**
 * Used internally to assure exceptions won't bubble up when executing a runnable in an executor.
 */
final class SwrveRunnables {

    public static Runnable withoutExceptions(final Runnable r) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    r.run();
                } catch (Exception exp) {
                    SwrveLogger.e("Error executing runnable: ", exp);
                }
            }
        };
    }
}
