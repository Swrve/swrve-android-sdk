package com.swrve.sdk;

import android.app.NotificationChannel;

public class SwrveNotificationConfig {

    private Class<?> activityClass;
    private int iconDrawableId;
    private int iconMaterialDrawableId;
    private NotificationChannel defaultNotificationChannel;
    private int largeIconDrawableId;
    private Integer accentColorResourceId;
    private SwrveNotificationCustomFilter notificationCustomFilter;

    private SwrveNotificationConfig(Builder builder) {
        this.activityClass = builder.activityClass;
        this.iconDrawableId = builder.iconDrawableId;
        this.iconMaterialDrawableId = builder.iconMaterialDrawableId;
        this.defaultNotificationChannel = builder.defaultNotificationChannel;
        this.largeIconDrawableId = builder.largeIconDrawableId;
        if (builder.accentColorResourceId != 0) {
            this.accentColorResourceId = builder.accentColorResourceId;
        }
        this.notificationCustomFilter = builder.notificationCustomFilter;
    }

    /**
     * Get the activity to open when the notification is engaged with.
     *
     * @return The Activity class
     */
    public Class<?> getActivityClass() {
        return activityClass;
    }

    /**
     * The notification icon drawable to be shown in the status bar for below api level 21
     *
     * @return The notification icon drawable id
     */
    public int getIconDrawableId() {
        return iconDrawableId;
    }

    /**
     * The notification icon drawable to be shown in the status bar for above api level 20
     *
     * @return The notification icon drawable id
     */
    public int getIconMaterialDrawableId() {
        return iconMaterialDrawableId;
    }

    /**
     * The default notification channel for which notifications should appear in.
     *
     * @return The default NotificationChannel
     */
    public NotificationChannel getDefaultNotificationChannel() {
        return defaultNotificationChannel;
    }

    /**
     * The icon to display if not configured by swrve dashboard
     *
     * @return drawable icon id
     */
    public int getLargeIconDrawableId() {
        return largeIconDrawableId;
    }

    /**
     * The default accent color to use in the notification
     *
     * @return color id
     */
    public Integer getAccentColorResourceId() {
        return accentColorResourceId;
    }

    /**
     * The notification filter used for modifying notifications before they are displayed.
     *
     * @return the notification custom filter
     */
    public SwrveNotificationCustomFilter getNotificationCustomFilter() {
        return notificationCustomFilter;
    }

    public static class Builder {

        private Class<?> activityClass;
        private int iconDrawableId;
        private int iconMaterialDrawableId;
        private NotificationChannel defaultNotificationChannel;
        private int largeIconDrawableId;
        private int accentColorResourceId;
        private SwrveNotificationCustomFilter notificationCustomFilter;

        /**
         * Builder constructor
         * @param iconDrawableId the notification icon drawable to be shown in the status bar for below api level 21
         * @param iconMaterialDrawableId the notification icon drawable to be shown in the status bar for above api level 20
         * @param defaultNotificationChannel Set the default notification channel used to display notifications. This is required if you target Android O (API 26) or higher.
         *                                   We recommend that the channel is created before setting it in our config. Our SDK will attempt to create it if it doesn't exist.
         */
        public Builder(int iconDrawableId, int iconMaterialDrawableId, NotificationChannel defaultNotificationChannel) {
            this.iconDrawableId = iconDrawableId;
            this.iconMaterialDrawableId = iconMaterialDrawableId;
            this.defaultNotificationChannel = defaultNotificationChannel;
        }

        /**
         * Set the activity class to open when the notification is engaged with.
         *
         * @param activityClass The Activity class
         * @return this builder
         */
        public Builder activityClass(Class<?> activityClass) {
            this.activityClass = activityClass;
            return this;
        }

        /**
         * Set the icon to display if not configured by swrve dashboard
         *
         * @param largeIconDrawableId drawable icon id
         * @return this builder
         */
        public Builder largeIconDrawableId(int largeIconDrawableId) {
            this.largeIconDrawableId = largeIconDrawableId;
            return this;
        }

        /**
         * Set the default accent color to use in the notification
         *
         * @param accentColorResourceId resource id for the color.
         * @return this builder
         */
        public Builder accentColorResourceId(int accentColorResourceId) {
            this.accentColorResourceId = accentColorResourceId;
            return this;
        }

        /**
         * Set the notification filter used for modifying remote notifications before they are displayed.
         * If filtering Geo Notifications, please use the SwrveGeoCustomFilter
         *
         * @param notificationCustomFilter the notification custom filter to apply
         * @return this builder
         */
        public Builder notificationCustomFilter(SwrveNotificationCustomFilter notificationCustomFilter) {
            this.notificationCustomFilter = notificationCustomFilter;
            return this;
        }

        public SwrveNotificationConfig build() {
            return new SwrveNotificationConfig(this);
        }
    }
}
