package com.swrve.sdk.runnable;

import com.swrve.sdk.ISwrveUserResourcesDiffListener;

import java.util.Map;

/**
 * This helper class is used to run UI logic to handle
 * the result of Swrve::getUserResourceDiffs()
 *
 * Note: the callback method onResourceDiffsSuccess will be called from the
 * same UI threat than the caller Activity.
 */
public abstract class UIThreadSwrveResourcesDiffRunnable extends UIThreadSwrveRunnable implements ISwrveUserResourcesDiffListener {
    private Map<String, Map<String, String>> oldResourcesValues;
    private Map<String, Map<String, String>> newResourcesValues;
    private String resourcesAsJSON;

    @Override
    public void run() {
        onUserResourcesDiffSuccess(oldResourcesValues, newResourcesValues, resourcesAsJSON);
    }

    public void setData(Map<String, Map<String, String>> oldResourcesValues, Map<String, Map<String, String>> newResourcesValues, String resourcesAsJSON) {
        this.oldResourcesValues = oldResourcesValues;
        this.newResourcesValues = newResourcesValues;
        this.resourcesAsJSON = resourcesAsJSON;
    }
}
