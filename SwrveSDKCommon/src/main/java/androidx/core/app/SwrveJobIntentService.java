package androidx.core.app;

import com.swrve.sdk.SwrveLogger;

/**
 * Known bug in android causing a SecurityException so wrap dequeueWork method in a try catch
 * https://github.com/Swrve/swrve-android-sdk/issues/282
 */
public abstract class SwrveJobIntentService extends JobIntentService {

    @Override
    GenericWorkItem dequeueWork() {
        try {
            return super.dequeueWork();
        } catch (SecurityException e) {
            SwrveLogger.e("SwrveJobIntentService exception in dequeueWork. This is a known android O+ bug.", e);
            return null;
        }
    }
}
