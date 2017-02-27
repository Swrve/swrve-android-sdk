package com.swrve.sdk.sample;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.swrve.sdk.ISwrve;
import com.swrve.sdk.SwrveBaseEmpty;
import com.swrve.sdk.SwrveEmpty;
import com.swrve.sdk.SwrveIAPRewards;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.SwrveSDKBase;
import com.swrve.sdk.config.SwrveConfig;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.net.URL;

/**
 * A utility to manage user identity with the Swrve SDK. This utility gives you the ability
 * to (a) initialize Swrve if you don't have a user ID yet (i.e. first session or before the
 * user has logged in), set the identity of the user once you have it and (c) change the
 * identity in case the user logs out an another logs in.
 */
public class SwrveIdentityUtils {

    /**
     * The config used to initialize the SDK.
     */
    protected SwrveConfig config = null;

    /**
     * The app_id of the Swrve environment
     */
    protected int app_id = -1;

    /**
     * The api_key of the Swrve environment
     */
    protected String api_key = null;

    /**
     * Call this method to initialize the Swrve SDK.  After you call this method you no longer
     * need to call SwrveSDK.createInstance.  You must call this method before you can call
     * changeUserID.
     */
    public void createSDKInstance(Application application, int app_id, String api_key, SwrveConfig config, String user_id)  {
        this.config = config;
        this.app_id = app_id;
        this.api_key = api_key;
        reinitializeSwrveSDK(application, null, user_id);
    }

    /**
     * Call this method to change the user ID referenced in the Swrve SDK. Calling this method
     * will do several things.  First it will attempt to send all events for the current user
     * to Swrve's servers.  If this fails the events will be saved to disk and be sent the next
     * time the Swrve SDK is initialized with the user ID.  Next, the Swrve SDK will be shut down
     * and it will detach from the current ActivityContext.  As a result, in-app messages or
     * conversations will be dismissed.  Next, the Swrve SDK will be re-initialized and it will
     * reattach itself to the activity provided.
     */
    public void changeUserID(Activity activity, String user_id) throws Exception {

        // Make sure users initialize the utility before they try to change the user ID.
        if(config == null || app_id == -1 || api_key == null ){
            throw new Exception("You must call initialize before you can change the user id.");
        }

        // You must provide an activity here because this method will destroy and
        // reinitialize the Swrve SDK.  When the SDK is re-initialize it must have
        // an activity context which is used to display in-app messages, send event
        // data, ect.
        if(activity == null) {
            throw new Exception("You must provide an activity while changing the user ID.");
        }

        reinitializeSwrveSDK(activity.getApplication(), activity, user_id);
    }

    /**
     * Utility method that does the heavy lifting, see changeUserID for a description of what
     * this method does.
     * @throws Exception
     */
    protected void reinitializeSwrveSDK(Application application, Activity activity, String user_id) {

        // Destroy old instance, if it exists
        if( SwrveSDK.getInstance() != null ) {

            // Session has ended for the current user
            SwrveSDK.sessionEnd();

            // Save events to disk.
            SwrveSDK.flushToDisk();

            // Try to send the events to Swrves servers.  If this fails the events won't be sent
            // until the SDK is initialized with the old user's ID again.
            SwrveSDK.sendQueuedEvents();

            // Shutdown attachement to activity lifecycle
            SwrveSDK.onPause();
            SwrveSDK.onDestroy(activity);

            // Shutdown the SDK
            SwrveSDK.shutdown();

            // Remove the singleton instance
            setStaticFieldToValue(SwrveSDKBase.class, "instance", null);
        }

        // Add the user id to the name of the local DB for the SDK.  This will 'namesapce'
        // all of the events raised and content to the user ID.
        this.config.setDbName(user_id + "swrve.db");
        this.config.setUserId(user_id);

        // Initialize the sdk
        if(user_id == null) {
            setStaticFieldToValue(SwrveSDKBase.class, "instance", new SwrveEmpty(application, api_key));
        } else {
            SwrveSDK.createInstance(application, app_id, api_key, config);
        }

        // Initialize the activity lifecycle
        if( activity != null ) {
            SwrveSDK.onCreate(activity);
            SwrveSDK.onResume(activity);
        }
    }

    /**
     * Utility method to reset the Swrve instance
     */
    private static void setStaticFieldToValue(Class clazz, String fieldName, Object value) {
        try {
            Field instance = clazz.getDeclaredField(fieldName);
            instance.setAccessible(true);
            instance.set(null, value);
        } catch (NoSuchFieldException e) {
            Log.e(SwrveIdentityUtils.class.getName(), "Could not set Swrve singleton instance to null", e);
        } catch (IllegalAccessException e) {
            Log.e(SwrveIdentityUtils.class.getName(), "Could not set Swrve singleton instance to null", e);
        }
    }

    /**
     * Empty Swrve class that does nothing.  This class is used if the user id provided is null.
     */
    protected class SwrveEmpty extends SwrveBaseEmpty<ISwrve, SwrveConfig> implements ISwrve {
        public SwrveEmpty(Context context, String apiKey) {
            super(context, apiKey);
        }

        public void iapPlay(String productId, double productPrice, String currency, String purchaseData, String dataSignature) {
        }

        public void iapPlay(String productId, double productPrice, String currency, SwrveIAPRewards rewards, String purchaseData, String dataSignature) {
        }

        @Deprecated
        public void processIntent(Intent intent) {
        }

        public void setRegistrationId(String registrationId) {
        }

        public void onTokenRefreshed(){
        }
    }
}
