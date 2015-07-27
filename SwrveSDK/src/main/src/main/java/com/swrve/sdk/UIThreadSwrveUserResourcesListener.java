package com.swrve.sdk;

import android.app.Activity;

import com.swrve.sdk.runnable.UIThreadSwrveResourcesRunnable;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * This helper class is used to run UI logic to handle
 * the result of Swrve::getUserResources()
 *
 * Note: the Runnable uiWork will be called from the context of
 * the Activity.
 */
public class UIThreadSwrveUserResourcesListener implements ISwrveUserResourcesListener {

    private final WeakReference<Activity> context;
    private final UIThreadSwrveResourcesRunnable uiWork;

    public UIThreadSwrveUserResourcesListener(Activity context, UIThreadSwrveResourcesRunnable uiWork) {
        this.context = new WeakReference<Activity>(context);
        this.uiWork = uiWork;
    }

    public void onUserResourcesSuccess(Map<String, Map<String, String>> resources, String resourcesAsJSON) {
        Activity ctx = context.get();
        if (ctx != null && !ctx.isFinishing()) {
            uiWork.setData(resources, resourcesAsJSON);
            ctx.runOnUiThread(uiWork);
        }
    }

    @Override
    public void onUserResourcesError(Exception exception) {
        Activity ctx = context.get();
        if (ctx != null && !ctx.isFinishing()) {
            uiWork.setException(exception);
            ctx.runOnUiThread(uiWork);
        }
    }
}
