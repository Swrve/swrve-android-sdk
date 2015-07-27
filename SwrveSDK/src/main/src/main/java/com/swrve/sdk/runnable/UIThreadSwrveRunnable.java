package com.swrve.sdk.runnable;

/**
 * Base class for helper utility to run callbacks on the main UI thread.
 */
public abstract class UIThreadSwrveRunnable implements Runnable {
    protected Exception exception;

    public void setException(Exception exception) {
        this.exception = exception;
    }
}
