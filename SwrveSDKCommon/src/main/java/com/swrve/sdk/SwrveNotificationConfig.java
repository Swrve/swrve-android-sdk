package com.swrve.sdk;

public class SwrveNotificationConfig {

    private Class<?> activityClass;
    private int iconDrawableId;
    private int iconMaterialDrawableId;
    private int largeIconDrawableId;
    private int accentColorResourceId;
    private String notificationTitle;

    private SwrveNotificationConfig(Builder builder) {
        this.activityClass = builder.activityClass;
        this.iconDrawableId = builder.iconDrawableId;
        this.iconMaterialDrawableId = builder.iconMaterialDrawableId;
        this.largeIconDrawableId = builder.largeIconDrawableId;
        this.accentColorResourceId = builder.accentColorResourceId;
        this.notificationTitle = builder.notificationTitle;
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
    public int getAccentColorResourceId() {
        return accentColorResourceId;
    }

    /**
     * The default notification title to use if none defined by swrve dashboard
     *
     * @return the notification title text
     */
    public String getNotificationTitle() {
        return notificationTitle;
    }

    public static class Builder {

        private Class<?> activityClass;
        private int iconDrawableId;
        private int iconMaterialDrawableId;
        private int largeIconDrawableId;
        private int accentColorResourceId;
        private String notificationTitle;

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
         * Set the notification icon drawable to be shown in the status bar for below api level 21
         *
         * @param iconDrawableId The notification icon drawable id
         * @return this builder
         */
        public Builder iconDrawableId(int iconDrawableId) {
            this.iconDrawableId = iconDrawableId;
            return this;
        }

        /**
         * Set the notification icon drawable to be shown in the status bar for above api level 20
         *
         * @param iconMaterialDrawableId The notification icon drawable id
         * @return this builder
         */
        public Builder iconMaterialDrawableId(int iconMaterialDrawableId) {
            this.iconMaterialDrawableId = iconMaterialDrawableId;
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
         * @param accentColorResourceId color id
         * @return this builder
         */
        public Builder accentColorResourceId(int accentColorResourceId) {
            this.accentColorResourceId = accentColorResourceId;
            return this;
        }

        /**
         * Set the default notification title to use if none defined by swrve dashboard
         *
         * @param notificationTitle the notification title text
         * @return this builder
         */
        public Builder notificationTitle(String notificationTitle) {
            this.notificationTitle = notificationTitle;
            return this;
        }

        public SwrveNotificationConfig build() {
            return new SwrveNotificationConfig(this);
        }
    }
}
