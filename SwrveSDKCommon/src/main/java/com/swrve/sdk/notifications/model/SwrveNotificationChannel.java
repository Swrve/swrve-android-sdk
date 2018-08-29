package com.swrve.sdk.notifications.model;

import android.app.NotificationManager;

import com.google.gson.annotations.SerializedName;

public class SwrveNotificationChannel {

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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ImportanceLevel getImportance() {
        return importance;
    }

    public void setImportance(ImportanceLevel importance) {
        this.importance = importance;
    }

    public int getAndroidImportance() {
        return importance.androidImportance();
    }
}
