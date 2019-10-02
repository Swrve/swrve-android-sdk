package android.support.v4.app;

import com.swrve.sdk.SwrveLogger;

public abstract class SwrveJobIntentService extends JobIntentService {

    // Known bug in android causing a SecurityException so wrap this method in a try catch
    // https://github.com/Swrve/swrve-android-sdk/issues/282
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
