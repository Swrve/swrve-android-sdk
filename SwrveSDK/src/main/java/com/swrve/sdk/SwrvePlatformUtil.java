package com.swrve.sdk;

import com.swrve.sdk.localstorage.SwrveMultiLayerLocalStorage;

import org.json.JSONException;
import org.json.JSONObject;

interface SwrvePlatformUtil {

    void init(SwrveMultiLayerLocalStorage multiLayerLocalStorage, String userId, boolean automaticPushRegistration, boolean logAdvertisingId);

    void saveAndSendRegistrationId(SwrveMultiLayerLocalStorage multiLayerLocalStorage, String userId, String regId);

    void appendDeviceUpdate(JSONObject deviceUpdate) throws JSONException;

}
