package com.swrve.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import static com.swrve.sdk.ISwrveCommon.SDK_PREFS_KEY_TRACKING_STATE;
import static com.swrve.sdk.ISwrveCommon.SDK_PREFS_NAME;

public enum SwrveTrackingState {
    UNKNOWN, STARTED, EVENT_SENDING_PAUSED, STOPPED;

    @Override
    public String toString() {
        switch (this) {
            case UNKNOWN:
                return "UNKNOWN";
            case STARTED:
                return "STARTED";
            case EVENT_SENDING_PAUSED:
                return "EVENT_SENDING_PAUSED";
            case STOPPED:
                return "STOPPED";
            default:
                return "UNKNOWN";
        }
    }

    static void saveTrackingState(Context context, SwrveTrackingState state) {
        if (state == EVENT_SENDING_PAUSED) {
            return;
        }
        SharedPreferences settings = context.getSharedPreferences(SDK_PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(SDK_PREFS_KEY_TRACKING_STATE, state.toString()).commit();
    }

    static SwrveTrackingState getTrackingState(Context context) {
        SharedPreferences settings = context.getSharedPreferences(SDK_PREFS_NAME, 0);
        String s = settings.getString(SDK_PREFS_KEY_TRACKING_STATE, null);
        return SwrveTrackingState.parse(s);
    }

    static SwrveTrackingState parse(String state) {
        SwrveTrackingState trackingState = UNKNOWN;
        if (SwrveHelper.isNullOrEmpty(state)) {
            trackingState = UNKNOWN; // UNKNOWN by default
        } else if (state.equalsIgnoreCase("STARTED")) {
            trackingState = STARTED;
        } else if (state.equalsIgnoreCase("EVENT_SENDING_PAUSED")) {
            trackingState = EVENT_SENDING_PAUSED;
        } else if (state.equalsIgnoreCase("STOPPED")) {
            trackingState = STOPPED;
        } else if (state.equalsIgnoreCase("UNKNOWN")) {
            trackingState = UNKNOWN;
        }
        return trackingState;
    }

}
