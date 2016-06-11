package com.swrve.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import com.swrve.sdk.config.SwrveConfig;

import java.util.concurrent.TimeUnit;

public class SwrveObservable extends Swrve {

    protected final CountLatch numRunningTasks = new CountLatch(0);
    public boolean stopEventsFromBeingSent = false;

    public SwrveObservable(Context context, int appId, String apiKey, SwrveConfig config) {
        super(context, appId, apiKey, config);
    }

    public static SwrveObservable createInstance(Context context, int appId, String apiKey, SwrveConfig config) throws Exception {
        SwrveObservable instance = new SwrveObservable(context, appId, apiKey, config);
        SwrveTestUtils.setSingletonInstance(instance);
        return instance;
    }

    @Override
    protected boolean restClientExecutorExecute(final Runnable runnable) {
        numRunningTasks.countUp();
        boolean inserted = super.restClientExecutorExecute(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception ex) {
                    SwrveLogger.e(LOG_TAG, "Error SwrveTestHelper.", ex);
                }
                numRunningTasks.countDown();
            }
        });
        if (!inserted) {
            numRunningTasks.countDown();
        }
        return inserted;
    }

    @Override
    protected boolean storageExecutorExecute(final Runnable runnable) {
        numRunningTasks.countUp();
        boolean inserted = super.storageExecutorExecute(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception ex) {
                    SwrveLogger.e(LOG_TAG, "Error SwrveTestHelper.", ex);
                }
                numRunningTasks.countDown();
            }
        });
        if (!inserted) {
            numRunningTasks.countDown();
        }
        return inserted;
    }

    public boolean waitForJobs(int seconds) {
        boolean timeout = false;
        try {
            numRunningTasks.await(seconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            SwrveLogger.e(LOG_TAG, "Error SwrveTestHelper.", e);
            timeout = true;
        }
        return timeout;
    }

    @Override
    protected void _sendQueuedEvents() {
        if (!stopEventsFromBeingSent) {
            super._sendQueuedEvents();
        }
    }

    public void clearData() {
        // Clear database and cache
        if (cachedLocalStorage == null) {
            cachedLocalStorage = createCachedLocalStorage();
        }
        cachedLocalStorage.reset();
        // Clear shared preferences
        SharedPreferences prefs = getContext().getSharedPreferences(SDK_PREFS_NAME, 0);
        prefs.edit().clear().commit();
    }
}
