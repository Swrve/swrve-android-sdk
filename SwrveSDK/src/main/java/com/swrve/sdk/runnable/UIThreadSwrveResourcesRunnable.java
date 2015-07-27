package com.swrve.sdk.runnable;

import com.swrve.sdk.ISwrveUserResourcesListener;

import java.util.Map;

/**
 * This helper class is used to run UI logic to handle
 * the result of Swrve::getUserResources()
 *
 * Note: the callback method onResourcesSuccess will be called from the
 * same UI threat than the caller Activity.
 */
public abstract class UIThreadSwrveResourcesRunnable extends UIThreadSwrveRunnable implements ISwrveUserResourcesListener {
    private Map<String, Map<String, String>> resources;
    private String resourcesAsJSON;

    @Override
    public void run() {
        if (exception != null)
            onUserResourcesError(exception);
        else
            onUserResourcesSuccess(resources, resourcesAsJSON);
    }

    public void setData(Map<String, Map<String, String>> resources, String resourcesAsJSON) {
        this.resources = resources;
        this.resourcesAsJSON = resourcesAsJSON;
    }
}
