package com.swrve.sdk.config;

import android.graphics.Color;
import android.graphics.Typeface;

import com.swrve.sdk.messaging.SwrveClipboardButtonListener;
import com.swrve.sdk.messaging.SwrveCustomButtonListener;
import com.swrve.sdk.messaging.SwrveDismissButtonListener;
import com.swrve.sdk.messaging.SwrveInstallButtonListener;
import com.swrve.sdk.messaging.SwrveMessagePersonalisationProvider;

public class SwrveInAppMessageConfig {

    private int defaultBackgroundColor;
    private int focusColor;
    private int clickColor;
    private boolean hideToolbar;
    private int personalisedTextBackgroundColor;
    private int personalisedTextForegroundColor;
    private Typeface personalisedTextTypeface;
    private long autoShowMessagesMaxDelay;

    protected SwrveInstallButtonListener installButtonListener;
    protected SwrveCustomButtonListener customButtonListener;
    protected SwrveDismissButtonListener dismissButtonListener;
    protected SwrveClipboardButtonListener clipboardButtonListener;
    protected SwrveMessagePersonalisationProvider personalisationProvider;

    private SwrveInAppMessageConfig(Builder builder) {
        this.defaultBackgroundColor = builder.defaultBackgroundColor;
        this.focusColor = builder.focusColor;
        this.clickColor = builder.clickColor;
        this.hideToolbar = builder.hideToolbar;
        this.personalisedTextBackgroundColor = builder.personalisedTextBackgroundColor;
        this.personalisedTextForegroundColor = builder.personalisedTextForegroundColor;
        this.personalisedTextTypeface = builder.personalisedTextTypeface;
        this.autoShowMessagesMaxDelay = builder.autoShowMessagesMaxDelay;
        this.installButtonListener = builder.installButtonListener;
        this.customButtonListener = builder.customButtonListener;
        this.dismissButtonListener = builder.dismissButtonListener;
        this.clipboardButtonListener = builder.clipboardButtonListener;
        this.personalisationProvider = builder.personalisationProvider;
    }

    /**
     * The in-app message default background color
     *
     * @return The in-app message default background color
     */
    public int getDefaultBackgroundColor() {
        return defaultBackgroundColor;
    }

    /**
     * The in-app message focus color
     *
     * @return The in-app message focus color
     */
    public int getFocusColor() {
        return focusColor;
    }

    /**
     * The in-app message button click color.
     *
     * @return The in-app message button click color
     */
    public int getClickColor() {
        return clickColor;
    }

    /**
     * @return Whether the SDK will hide the toolbar when displaying in-app messages.
     */
    public boolean isHideToolbar() {
        return hideToolbar;
    }

    /**
     * The in-app message text background color.
     *
     * @return The in-app message focus color
     */
    public int getPersonalisedTextBackgroundColor() {
        return personalisedTextBackgroundColor;
    }

    /**
     * The in-app message personalized text foreground color.
     *
     * @return The in-app message button click color
     */
    public int getPersonalisedTextForegroundColor() {
        return personalisedTextForegroundColor;
    }

    /**
     * The in-app message personalized text typeface.
     *
     * @return The in-app message personalized text typeface
     */
    public Typeface getPersonalisedTextTypeface() {
        return this.personalisedTextTypeface;
    }

    /**
     * Maximum delay for in-app messages to appear after initialization.
     *
     * @return maximum delay in milliseconds
     */
    public long getAutoShowMessagesMaxDelay() {
        return autoShowMessagesMaxDelay;
    }


    /**
     * Custom listener to process in-app message install button clicks.
     *
     * @return The custom listener
     */
    public SwrveInstallButtonListener getInstallButtonListener() {
        return installButtonListener;
    }

    /**
     * Custom listener to process in-app message custom button clicks.
     *
     * @return The custom listener
     */
    public SwrveCustomButtonListener getCustomButtonListener() {
        return customButtonListener;
    }

    /**
     * In-app button listener to get notified of in-app message dismiss button clicks.
     *
     * @return The custom listener
     */
    public SwrveDismissButtonListener getDismissButtonListener() {
        return dismissButtonListener;
    }

    /**
     * Custom listener to process in-app message copy-to-clipboard button clicks.
     *
     * @return The custom listener
     */
    public SwrveClipboardButtonListener getClipboardButtonListener() {
        return clipboardButtonListener;
    }

    /**
     * Custom personalisation provider for incoming In app messages.
     *
     * @return The personalisation provider
     */
    public SwrveMessagePersonalisationProvider getPersonalisationProvider() {
        return personalisationProvider;
    }

    public static class Builder {
        private int defaultBackgroundColor = Color.TRANSPARENT; // Default in-app background color used if none is specified in the template.
        private int focusColor = Color.argb(100, 0, 190, 152); // Default button focus color for IAM
        private int clickColor = Color.argb(100, 0, 0, 0); // Default button click color for IAM
        private boolean hideToolbar = true; // Hide the toolbar when displaying in-app messages.
        private int personalisedTextBackgroundColor = Color.TRANSPARENT; // Default Background color for Personalized Text in IAMs
        private int personalisedTextForegroundColor = Color.BLACK; // Default Text Color for Personalized Text in IAMs
        private Typeface personalisedTextTypeface = null; // Default will use System Font
        private long autoShowMessagesMaxDelay = 5000; // Maximum delay for in-app messages to appear after initialization.

        protected SwrveInstallButtonListener installButtonListener;
        protected SwrveCustomButtonListener customButtonListener;
        protected SwrveDismissButtonListener dismissButtonListener;
        protected SwrveClipboardButtonListener clipboardButtonListener;
        protected SwrveMessagePersonalisationProvider personalisationProvider;

        /**
         * Builder constructor
         */
        public Builder(){ }

        /**
         * Set the default in-app background color.
         *
         * @param defaultBackgroundColor Default in-app background color used if none is specified in the template
         * @return this builder
         */
        public Builder defaultBackgroundColor(int defaultBackgroundColor) {
            this.defaultBackgroundColor = defaultBackgroundColor;
            return this;
        }

        /**
         * Set the in-app message focus color. Used for in app message buttons on TV
         *
         * @param focusColor A color-int, eg: Color.argb(100, 0, 190, 152)
         * @return this builder
         */
        public Builder focusColor(int focusColor) {
            this.focusColor = focusColor;
            return this;
        }

        /**
         * Set the in-app message button click color. Used for in app message buttons on TV
         *
         * @param clickColor A color-int, eg: Color.argb(100, 0, 190, 152)
         * @return this builder
         */
        public Builder clickColor(int clickColor) {
            this.clickColor = clickColor;
            return this;
        }

        /**
         * Hide the toolbar when displaying in-app messages.
         *
         * @param hideToolbar true to hide the toolbar when displaying in-app messages\
         * @return this builder
         */
        public Builder hideToolbar(boolean hideToolbar) {
            this.hideToolbar = hideToolbar;
            return this;
        }


        /**
         * Set the in-app message personalized text background color. Used for the background of the text
         *
         * @param personalisedTextBackgroundColor A color-int, eg: Color.argb(100, 0, 190, 152)
         * @return this builder
         */
        public Builder personalisedTextBackgroundColor(int personalisedTextBackgroundColor) {
            this.personalisedTextBackgroundColor = personalisedTextBackgroundColor;
            return this;
        }

        /**
         * Set the in-app message personalized text color
         *
         * @param personalisedTextForegroundColor A color-int, eg: Color.argb(100, 0, 190, 152)
         * @return this builder
         */
        public Builder personalisedTextForegroundColor(int personalisedTextForegroundColor) {
            this.personalisedTextForegroundColor = personalisedTextForegroundColor;
            return this;
        }

        /**
         * Set the in-app message personalized text color.
         *
         * @param typeface A Font Object containing the default font you want for any IAM text
         * @return this builder
         */
        public Builder personalisedTextTypeface(Typeface typeface) {
            this.personalisedTextTypeface = typeface;
            return this;
        }

        /**
         * Maximum delay for in-app messages to appear after initialization.
         *
         * @param autoShowMessagesMaxDelay max delay
         * @return this builder
         */
        public Builder autoShowMessagesMaxDelay(long autoShowMessagesMaxDelay) {
            this.autoShowMessagesMaxDelay = autoShowMessagesMaxDelay;
            return this;
        }

        /**
         * Custom listener to process in-app message install button clicks.
         *
         * @param installButtonListener The custom listener
         * @return this builder
         */
        public Builder installButtonListener(SwrveInstallButtonListener installButtonListener) {
            this.installButtonListener = installButtonListener;
            return this;
        }

        /**
         * Custom listener to process in-app message custom button clicks.
         *
         * @param customButtonListener The custom listener
         * @return this builder
         */
        public Builder customButtonListener(SwrveCustomButtonListener customButtonListener) {
            this.customButtonListener = customButtonListener;
            return this;
        }

        /**
         * In-app button listener to get notified of in-app message dismiss button clicks.
         *
         * @param dismissButtonListener The in-app dismiss button listener
         * @return this builder
         */
        public Builder dismissButtonListener(SwrveDismissButtonListener dismissButtonListener) {
            this.dismissButtonListener = dismissButtonListener;
            return this;
        }

        /**
         * Custom listener to process in-app message copy-to-clipboard button clicks.
         *
         * @param clipboardButtonListener The clipboard listener
         * @return this builder
         */
        public Builder clipboardButtonListener(SwrveClipboardButtonListener clipboardButtonListener) {
            this.clipboardButtonListener = clipboardButtonListener;
            return this;
        }

        /**
         * Custom personalisation provider for incoming In App messages.
         *
         * @param personalisationProvider The personalisation provider
         * @return this builder
         */
        public Builder personalisationProvider(SwrveMessagePersonalisationProvider personalisationProvider) {
            this.personalisationProvider = personalisationProvider;
            return this;
        }

        public SwrveInAppMessageConfig build() {
            return new SwrveInAppMessageConfig(this);
        }
    }
}
