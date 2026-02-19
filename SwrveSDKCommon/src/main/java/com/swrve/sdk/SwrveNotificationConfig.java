package com.swrve.sdk;

import android.app.NotificationChannel;

import java.util.List;

public class SwrveNotificationConfig {

    private final Class<?> activityClass;
    private final SwrveNotificationIntentListener notificationIntentListener;
    private final boolean useEngagementProxy;
    private final int iconMaterialDrawableId;
    private final NotificationChannel defaultNotificationChannel;
    private final int largeIconDrawableId;
    private final String accentColorHex;
    private final SwrveNotificationFilter notificationFilter;
    private final List<String> pushNotificationPermissionEvents;

    private SwrveNotificationConfig(Builder builder) {
        this.activityClass = builder.activityClass;
        this.notificationIntentListener = builder.notificationIntentListener;
        this.useEngagementProxy = builder.useEngagementProxy;
        this.iconMaterialDrawableId = builder.iconMaterialDrawableId;
        this.defaultNotificationChannel = builder.defaultNotificationChannel;
        this.largeIconDrawableId = builder.largeIconDrawableId;
        this.accentColorHex = builder.accentColorHex;
        this.notificationFilter = builder.notificationFilter;
        this.pushNotificationPermissionEvents = builder.pushNotificationPermissionEvents;
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
     * Get the notification intent listener to be called when the notification is engaged with.
     *
     * @return The notification intent listener
     */
    public SwrveNotificationIntentListener getNotificationIntentListener() {
        return notificationIntentListener;
    }

    /**
     * The useEngagementProxy configuration.
     * @return true to use the engagement proxy, false to not use it
     */
    public boolean useEngagementProxy() {
        return useEngagementProxy;
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
     * The default/fallback notification channel for which notifications should appear in.
     *
     * @return The default/fallback NotificationChannel
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
    public String getAccentColorHex() {
        return accentColorHex;
    }

    /**
     * The notification filter used for modifying notifications before they are displayed.
     *
     * @return the notification filter
     */
    public SwrveNotificationFilter getNotificationFilter() {
        return notificationFilter;
    }

    /**
     * The list of Swrve events that will attempt to trigger a notification permission request
     *
     * @return List of events
     */
    public List<String> getPushNotificationPermissionEvents() {
        return pushNotificationPermissionEvents;
    }

    public static class Builder {

        private Class<?> activityClass;
        private SwrveNotificationIntentListener notificationIntentListener;
        private boolean useEngagementProxy = true; // default to true, but we might consider removing this and proxy in next major version
        private final int iconMaterialDrawableId;
        private final NotificationChannel defaultNotificationChannel;
        private int largeIconDrawableId;
        private String accentColorHex;
        private SwrveNotificationFilter notificationFilter;
        private List<String> pushNotificationPermissionEvents;

        /**
         * Builder constructor
         *
         * @param iconMaterialDrawableId     the notification icon drawable to be shown in the status bar
         * @param defaultNotificationChannel Set the default notification (or fallback) channel used to display notifications if
         *                                   none is specified in the dashboard. If null, notifications will not be displayed.
         */
        public Builder(int iconMaterialDrawableId, NotificationChannel defaultNotificationChannel) {
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
         * Set the notification intent listener to be called when the notification is engaged with.
         * This is useful if you want to apply custom intent flags and extras. The Intent returned is
         * called with context.startActivity(intent). If set, this will override the activityClass
         * API and also the SwrveDeeplinkListener API. If not set, the SDK will apply default behavior.
         *
         * @param notificationIntentListener The notification intent listener
         * @return this builder
         */
        public Builder notificationIntentListener(SwrveNotificationIntentListener notificationIntentListener) {
            this.notificationIntentListener = notificationIntentListener;
            return this;
        }

        /**
         * Sets whether to use an engagement proxy activity for handling push notification engagements.
         * <p>
         * The engagement proxy is an invisible Activity that processes push notification intents and
         * tracks user engagement before routing the intent to the target Activity.  Using the engagement
         * proxy (the default behavior) ensures compatibility with various Android versions and devices,
         * and allows the SDK to manage necessary Activity flags.
         * <p>
         * If set to {@code false}, the SDK bypasses the engagement proxy and delivers the push notification
         * intent directly to your application's Activity.  In this case, you *must* override the
         * {@code android.app.Activity#onNewIntent(Intent)} method in your target Activity and call
         * {@code setIntent(intent)} to ensure proper handling of the notification intent. Failing to do
         * so will result in losing engagement events and potentially missed executions of
         * {@link com.swrve.sdk.SwrvePushNotificationListener}.
         * <p>
         * If the engagement opens a deeplink to an external app, the SDK will override this setting and use the engagement proxy.
         * <p>
         * Note: This setting is advanced not recommended for most use cases
         * @param useEngagementProxy {@code true} to use the engagement proxy (default), {@code false} to
         *                           bypass it and handle intents directly in your Activity.
         * @return This {@link Builder} instance for chaining.
         */
        public Builder useEngagementProxy(boolean useEngagementProxy) {
            this.useEngagementProxy = useEngagementProxy;
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
         * @param accentColorHex hex color string value for the color.
         * @return this builder
         */
        public Builder accentColorHex(String accentColorHex) {
            this.accentColorHex = accentColorHex;
            return this;
        }

        /**
         * Set the notification filter used for modifying remote notifications before they are displayed.
         * If filtering Geo Notifications, please use the SwrveGeoCustomFilter
         *
         * @param notificationFilter the notification filter to apply
         * @return this builder
         */
        public Builder notificationFilter(SwrveNotificationFilter notificationFilter) {
            this.notificationFilter = notificationFilter;
            return this;
        }

        /**
         * Set list of Swrve events that will attempt to trigger a notification permission request
         *
         * @param pushNotificationPermissionEvents List of events
         * @return this builder
         */
        public Builder pushNotificationPermissionEvents(List<String> pushNotificationPermissionEvents) {
            this.pushNotificationPermissionEvents = pushNotificationPermissionEvents;
            return this;
        }

        public SwrveNotificationConfig build() {
            return new SwrveNotificationConfig(this);
        }
    }
}
