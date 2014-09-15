package com.swrve.sdk;

import java.util.Map;

/**
 * Implement this interface to handle the result of Swrve::getUserResourceDiffs().
 *
 * Note: the methods in this object will be invoked from a different thread
 * than the one used to call Swrve::getUserResourceDiffs().
 */
public interface ISwrveUserResourcesDiffListener {

    /**
     * This method is invoked asynchronously to return the request response of
     * the Swrve::getUserResourceDiffs().
     *
     * Note: this method is invoked from a different thread than the thread used
     * to call Swrve::getUserResourceDiffs().
     *
     * @param oldResourcesValues the old values of AB Tested resources represented as a map in
     *                           the form uid->(attribute_name->attribute_value).
     * @param newResourcesValues the new values of AB Tested resources represented as a map in
     *                           the form uid->(attribute_name->attribute_value).
     * @param resourcesAsJSON    the resources as JSON.
     */
    void onUserResourcesDiffSuccess(final Map<String, Map<String, String>> oldResourcesValues,
                                    final Map<String, Map<String, String>> newResourcesValues,
                                    final String resourcesAsJSON);

    /**
     * Called back on error.
     */
    public void onUserResourcesDiffError(Exception exception);
}
