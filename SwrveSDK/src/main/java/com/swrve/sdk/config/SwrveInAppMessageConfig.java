package com.swrve.sdk.config;

import android.graphics.Color;
import android.graphics.Typeface;

import com.swrve.sdk.messaging.SwrveClipboardButtonListener;
import com.swrve.sdk.messaging.SwrveCustomButtonListener;
import com.swrve.sdk.messaging.SwrveDismissButtonListener;
import com.swrve.sdk.messaging.SwrveInAppWindowListener;
import com.swrve.sdk.messaging.SwrveInstallButtonListener;
import com.swrve.sdk.messaging.SwrveMessageFocusListener;
import com.swrve.sdk.messaging.SwrveInAppMessageListener;
import com.swrve.sdk.messaging.SwrveMessagePersonalizationProvider;

public class SwrveInAppMessageConfig {

    private int defaultBackgroundColor;
    private int clickColor;
    private boolean hideToolbar;
    private int personalizedTextBackgroundColor;
    private int personalizedTextForegroundColor;
    private Typeface personalizedTextTypeface;
    private long autoShowMessagesMaxDelay;

    private SwrveInstallButtonListener installButtonListener;
    private SwrveCustomButtonListener customButtonListener;
    private SwrveDismissButtonListener dismissButtonListener;
    private SwrveClipboardButtonListener clipboardButtonListener;
    private SwrveMessagePersonalizationProvider personalizationProvider;
    private SwrveInAppWindowListener windowListener;
    private SwrveMessageFocusListener messageFocusListener;
    private SwrveInAppMessageListener messageListener;

    private SwrveInAppMessageConfig(Builder builder) {
        this.defaultBackgroundColor = builder.defaultBackgroundColor;
        this.clickColor = builder.clickColor;
        this.hideToolbar = builder.hideToolbar;
        this.personalizedTextBackgroundColor = builder.personalizedTextBackgroundColor;
        this.personalizedTextForegroundColor = builder.personalizedTextForegroundColor;
        this.personalizedTextTypeface = builder.personalizedTextTypeface;
        this.autoShowMessagesMaxDelay = builder.autoShowMessagesMaxDelay;
        this.installButtonListener = builder.installButtonListener;
        this.customButtonListener = builder.customButtonListener;
        this.dismissButtonListener = builder.dismissButtonListener;
        this.clipboardButtonListener = builder.clipboardButtonListener;
        this.personalizationProvider = builder.personalizationProvider;
        this.windowListener = builder.windowListener;
        this.messageFocusListener = builder.messageFocusListener;
        this.messageListener = builder.messagesListener;
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
     * @return The in-app message text background color
     */
    public int getPersonalizedTextBackgroundColor() {
        return personalizedTextBackgroundColor;
    }

    /**
     * The in-app message personalized text foreground color.
     *
     * @return The in-app message button click color
     */
    public int getPersonalizedTextForegroundColor() {
        return personalizedTextForegroundColor;
    }

    /**
     * The in-app message personalized text typeface.
     *
     * @return The in-app message personalized text typeface
     */
    public Typeface getPersonalizedTextTypeface() {
        return this.personalizedTextTypeface;
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
     * @deprecated Use SwrveMessageListener
     * @return The custom listener
     */
    @Deprecated
    public SwrveCustomButtonListener getCustomButtonListener() {
        return customButtonListener;
    }

    /**
     * In-app button listener to get notified of in-app message dismiss button clicks.
     * @deprecated Use SwrveMessageListener
     * @return The dismiss listener
     */
    @Deprecated
    public SwrveDismissButtonListener getDismissButtonListener() {
        return dismissButtonListener;
    }

    /**
     * Custom listener to process in-app message copy-to-clipboard button clicks.
     * @deprecated Use SwrveMessageListener
     * @return The custom listener
     */
    public SwrveClipboardButtonListener getClipboardButtonListener() {
        return clipboardButtonListener;
    }

    /**
     * Custom personalization provider for incoming In app messages.
     *
     * @return The personalization provider
     */
    public SwrveMessagePersonalizationProvider getPersonalizationProvider() {
        return personalizationProvider;
    }

    /**
     * In-app message listener
     *
     * @return The message listener
     */
    public SwrveInAppMessageListener getMessageListener() {
        return messageListener;
    }

    /**
     * Custom IAM window listener
     *
     * @return the custom listener
     */
    public SwrveInAppWindowListener getWindowListener() {
        return windowListener;
    }

    /**
     * IAM focus listener
     *
     * @return the focus listener
     */
    public SwrveMessageFocusListener getMessageFocusListener() {
        return messageFocusListener;
    }

    public static class Builder {
        private int defaultBackgroundColor = Color.TRANSPARENT; // Default in-app background color used if none is specified in the template.
        private int clickColor = Color.argb(100, 0, 0, 0); // Default button click color for IAM
        private boolean hideToolbar = true; // Hide the toolbar when displaying in-app messages.
        private int personalizedTextBackgroundColor = Color.TRANSPARENT; // Default Background color for Personalized Text in IAMs
        private int personalizedTextForegroundColor = Color.BLACK; // Default Text Color for Personalized Text in IAMs
        private Typeface personalizedTextTypeface = null; // Default will use System Font
        private long autoShowMessagesMaxDelay = 5000; // Maximum delay for in-app messages to appear after initialization.

        protected SwrveInstallButtonListener installButtonListener;
        protected SwrveCustomButtonListener customButtonListener;
        protected SwrveDismissButtonListener dismissButtonListener;
        protected SwrveClipboardButtonListener clipboardButtonListener;
        protected SwrveMessagePersonalizationProvider personalizationProvider;
        protected SwrveInAppWindowListener windowListener;
        protected SwrveMessageFocusListener messageFocusListener;
        protected SwrveInAppMessageListener messagesListener;

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
         * @param personalizedTextBackgroundColor A color-int, eg: Color.argb(100, 0, 190, 152)
         * @return this builder
         */
        public Builder personalizedTextBackgroundColor(int personalizedTextBackgroundColor) {
            this.personalizedTextBackgroundColor = personalizedTextBackgroundColor;
            return this;
        }

        /**
         * Set the in-app message personalized text color
         *
         * @param personalizedTextForegroundColor A color-int, eg: Color.argb(100, 0, 190, 152)
         * @return this builder
         */
        public Builder personalizedTextForegroundColor(int personalizedTextForegroundColor) {
            this.personalizedTextForegroundColor = personalizedTextForegroundColor;
            return this;
        }

        /**
         * Set the in-app message personalized text color.
         *
         * @param typeface A Font Object containing the default font you want for any IAM text
         * @return this builder
         */
        public Builder personalizedTextTypeface(Typeface typeface) {
            this.personalizedTextTypeface = typeface;
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
         * Custom personalization provider for incoming In App messages.
         *
         * @param personalizationProvider The personalization provider
         * @return this builder
         */
        public Builder personalizationProvider(SwrveMessagePersonalizationProvider personalizationProvider) {
            this.personalizationProvider = personalizationProvider;
            return this;
        }

        /**
         * Custom IAM window listener which is invoked after the Activity onCreate is finished and
         * setContentView is called.
         *
         * @param windowListener The custom window listener
         * @return this builder
         */
        public Builder windowListener(SwrveInAppWindowListener windowListener) {
            this.windowListener = windowListener;
            return this;
        }

        /**
         * IAM focus listener.
         *
         * @param messageFocusListener The focus listener
         * @return this builder
         */
        public Builder messageFocusListener(SwrveMessageFocusListener messageFocusListener) {
            this.messageFocusListener = messageFocusListener;
            return this;
        }

        /**
         * Message listener to process in-app message actions and views
         *
         * @param messageListener The Message listener
         * @return this builder
         */
        public Builder messageListener(SwrveInAppMessageListener messageListener) {
            this.messagesListener = messageListener;
            return this;
        }

        public SwrveInAppMessageConfig build() {
            return new SwrveInAppMessageConfig(this);
        }
    }
}
