package com.swrve.sdk.config;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.StateListDrawable;
import com.swrve.sdk.messaging.SwrveInAppWindowListener;
import com.swrve.sdk.messaging.SwrveMessageFocusListener;
import com.swrve.sdk.messaging.SwrveInAppMessageListener;
import com.swrve.sdk.messaging.SwrveMessagePersonalizationProvider;

public class SwrveInAppMessageConfig {

    private int defaultBackgroundColor;
    private int clickColor;
    private int personalizedTextBackgroundColor;
    private int personalizedTextForegroundColor;
    private Typeface personalizedTextTypeface;
    private long autoShowMessagesMaxDelay;

    private SwrveMessagePersonalizationProvider personalizationProvider;
    private SwrveInAppWindowListener windowListener;
    private SwrveMessageFocusListener messageFocusListener;
    private SwrveInAppMessageListener messageListener;
    private StateListDrawable storyDismissButton;

    private SwrveInAppMessageConfig(Builder builder) {
        this.defaultBackgroundColor = builder.defaultBackgroundColor;
        this.clickColor = builder.clickColor;
        this.personalizedTextBackgroundColor = builder.personalizedTextBackgroundColor;
        this.personalizedTextForegroundColor = builder.personalizedTextForegroundColor;
        this.personalizedTextTypeface = builder.personalizedTextTypeface;
        this.autoShowMessagesMaxDelay = builder.autoShowMessagesMaxDelay;
        this.personalizationProvider = builder.personalizationProvider;
        this.windowListener = builder.windowListener;
        this.messageFocusListener = builder.messageFocusListener;
        this.messageListener = builder.messagesListener;
        this.storyDismissButton = builder.storyDismissButton;
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

    /**
     * IAS dismiss button
     *
     * @return the list of drawables for button states
     */
    public StateListDrawable getStoryDismissButton() {
        return storyDismissButton;
    }

    public static class Builder {
        private int defaultBackgroundColor = Color.TRANSPARENT; // Default in-app background color used if none is specified in the template.
        private int clickColor = Color.argb(100, 0, 0, 0); // Default button click color for IAM
        private int personalizedTextBackgroundColor = Color.TRANSPARENT; // Default Background color for Personalized Text in IAMs
        private int personalizedTextForegroundColor = Color.BLACK; // Default Text Color for Personalized Text in IAMs
        private Typeface personalizedTextTypeface = null; // Default will use System Font
        private long autoShowMessagesMaxDelay = 5000; // Maximum delay for in-app messages to appear after initialization.

        protected SwrveMessagePersonalizationProvider personalizationProvider;
        protected SwrveInAppWindowListener windowListener;
        protected SwrveMessageFocusListener messageFocusListener;
        protected SwrveInAppMessageListener messagesListener;
        protected StateListDrawable storyDismissButton;

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

        /**
         * Button images to use for In-app story dismiss button
         *
         * @param storyDismissButton The StateList of drawables
         * @return this builder
         */
        public Builder storyDismissButton(StateListDrawable storyDismissButton) {
            this.storyDismissButton = storyDismissButton;
            return this;
        }

        public SwrveInAppMessageConfig build() {
            return new SwrveInAppMessageConfig(this);
        }
    }
}
