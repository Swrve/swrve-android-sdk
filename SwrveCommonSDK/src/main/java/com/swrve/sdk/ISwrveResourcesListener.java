package com.swrve.sdk;

/**
 * Implement this interface to be notified of changes to the resources of your app.
 */
public interface ISwrveResourcesListener {
    /**
     * This method is invoked when user resources in the SwrveResourceManager have been initially
     * loaded and each time user resources are updated.
     *
     * Note: this method will be invoked from a different thread than the main UI thread.
     */
    void onResourcesUpdated();
}
