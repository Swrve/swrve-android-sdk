package com.swrve.sdk;

import android.app.Application;
import android.content.Context;

import com.swrve.sdk.config.SwrveConfig;

import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_CAMPAIGN_TYPE_PUSH;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_PLATFORM;
import static com.swrve.sdk.ISwrveCommon.GENERIC_EVENT_PAYLOAD_TRACKING_DATA;

import java.util.HashMap;
import java.util.Map;

public class SwrveSDK extends SwrveSDKBase {

    /**
     * Create a single Swrve SDK instance.
     *
     * @param application your application context
     * @param appId       your app id in the Swrve dashboard
     * @param apiKey      your app api_key in the Swrve dashboard
     * @return singleton SDK instance.
     */
    public static synchronized ISwrve createInstance(final Application application, final int appId, final String apiKey) {
        return createInstance(application, appId, apiKey, new SwrveConfig());
    }

    /**
     * Create a single Swrve SDK instance.
     *
     * @param application your application context
     * @param appId       your app id in the Swrve dashboard
     * @param apiKey      your app api_key in the Swrve dashboard
     * @param config      your SwrveConfig options
     * @return singleton SDK instance.
     */
    public static synchronized ISwrve createInstance(final Application application, final int appId, final String apiKey, final SwrveConfig config) {
        if (application == null) {
            SwrveHelper.logAndThrowException("Application is null");
        } else if (SwrveHelper.isNullOrEmpty(apiKey)) {
            SwrveHelper.logAndThrowException("Api key not specified");
        } else if (SwrveHelper.isInvalidAPIKey(apiKey)) {
            SwrveHelper.logAndThrowException("Api key should not start with secret-");
        }

        if (!SwrveHelper.sdkAvailable(config.getModelBlackList())) {
            instance = new SwrveEmpty(application, apiKey);
        }
        if (instance == null) {
            instance = new Swrve(application, appId, apiKey, config);
        }
        return (ISwrve) instance;
    }

    /**
     * Returns the Swrve configuration that was used to initialize the SDK.
     *
     * @return configuration used to context the SDK
     */
    public static SwrveConfig getConfig() {
        checkInstanceCreated();
        return (SwrveConfig) instance.getConfig();
    }

    /**
     * Set the silent push listener.
     *
     * @param silentPushListener silent push listener
     */
    public static void setSilentPushListener(SwrveSilentPushListener silentPushListener) {
        checkInstanceCreated();
        SwrveConfig config = getConfig();
        if (config != null) {
            config.setSilentPushListener(silentPushListener);
        }
    }

    /**
     * Called to send the push engaged event to Swrve.
     *
     * @param context android context
     * @param pushId  The push id for engagement
     * @deprecated Instead use sendPushEngagedEvent(Context context, String pushId, String trackingData, String platform)
     */
    @Deprecated
    public static void sendPushEngagedEvent(Context context, String pushId) {
        checkInstanceCreated();
        EventHelper.sendEngagedEvent(context, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, pushId, null);
    }

    /**
     * Called to send the push engaged event to Swrve.
     *
     * @param context      android context
     * @param pushId       The push id for engagement (the _p value from the push payload)
     * @param trackingData Additional tracking data to be sent with the event (the _td value from the push payload)
     * @param platform     Platform of the push notification (the _smp value from the push payload)
     */
    public static void sendPushEngagedEvent(Context context, String pushId, String trackingData, String platform) {
        checkInstanceCreated();
        Map<String, String> payload = new HashMap<>();
        if (SwrveHelper.isNotNullOrEmpty(trackingData)) {
            payload.put(GENERIC_EVENT_PAYLOAD_TRACKING_DATA, trackingData);
        }
        if (SwrveHelper.isNotNullOrEmpty(platform)) {
            payload.put(GENERIC_EVENT_PAYLOAD_PLATFORM, platform);
        }
        EventHelper.sendEngagedEvent(context, GENERIC_EVENT_CAMPAIGN_TYPE_PUSH, pushId, payload);
    }

    /**
     * Update the Huawei registrationId.
     *
     * @param registrationId Updated registration Id
     */
    public static void setRegistrationId(String registrationId) {
        checkInstanceCreated();
        ((ISwrve) instance).setRegistrationId(registrationId);
    }
}
