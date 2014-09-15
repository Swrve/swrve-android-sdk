package com.swrve.sdk;

import android.app.Activity;

import com.swrve.sdk.runnable.UIThreadSwrveResourcesDiffRunnable;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * This helper class is used to run UI logic to handle
 * the result of Swrve::getUserResourcesDiff()
 *
 * Note: the Runnable uiWork will be called from the context of
 * the Activity.
 */
public class UIThreadSwrveUserResourcesDiffListener implements ISwrveUserResourcesDiffListener {

    private final WeakReference<Activity> context;
    private final UIThreadSwrveResourcesDiffRunnable uiWork;

    public UIThreadSwrveUserResourcesDiffListener(Activity context, UIThreadSwrveResourcesDiffRunnable uiWork) {
        this.context = new WeakReference<Activity>(context);
        this.uiWork = uiWork;
    }

    @Override
    public void onUserResourcesDiffSuccess(
            Map<String, Map<String, String>> oldResources,
            Map<String, Map<String, String>> newResources,
            String resourcesAsJSON) {
        Activity ctx = context.get();
        if (ctx != null && !ctx.isFinishing()) {
            uiWork.setData(oldResources, newResources, resourcesAsJSON);
            ctx.runOnUiThread(uiWork);
        }
    }

    @Override
    public void onUserResourcesDiffError(Exception exception) {
        Activity ctx = context.get();
        if (ctx != null && !ctx.isFinishing()) {
            uiWork.setException(exception);
            ctx.runOnUiThread(uiWork);
        }
    }
}
