package com.swrve.sdk.model;

import android.app.NotificationManager;

import com.google.gson.annotations.SerializedName;

public class PushPayloadChannel {

    public enum ImportanceLevel {
        @SerializedName("default")
        DEFAULT,
        @SerializedName("high")
        HIGH,
        @SerializedName("low")
        LOW,
        @SerializedName("max")
        MAX,
        @SerializedName("min")
        MIN,
        @SerializedName("none")
        NONE;

        public int androidImportance() {
            switch (this) {
                case DEFAULT:
                    return NotificationManager.IMPORTANCE_DEFAULT;
                case HIGH:
                    return NotificationManager.IMPORTANCE_HIGH;
                case LOW:
                    return NotificationManager.IMPORTANCE_LOW;
                case MAX:
                    return NotificationManager.IMPORTANCE_MAX;
                case MIN:
                    return NotificationManager.IMPORTANCE_MIN;
                case NONE:
                    return NotificationManager.IMPORTANCE_NONE;
                default:
                    return NotificationManager.IMPORTANCE_DEFAULT;
            }
        }
    }

    private String id;

    private String name;

    private ImportanceLevel importance;

    /** getters **/

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ImportanceLevel getImportance() {
        return importance;
    }

    public int getAndroidImportance() {
        return importance.androidImportance();
    }
}
