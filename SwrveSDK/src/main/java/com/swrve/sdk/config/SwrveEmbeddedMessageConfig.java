package com.swrve.sdk.config;

import com.swrve.sdk.messaging.SwrveEmbeddedListener;
import com.swrve.sdk.messaging.SwrveEmbeddedMessageListener;

public class SwrveEmbeddedMessageConfig {

    protected SwrveEmbeddedMessageListener embeddedMessageListener;
    protected SwrveEmbeddedListener embeddedListener;

    private SwrveEmbeddedMessageConfig(SwrveEmbeddedMessageConfig.Builder builder) {
        this.embeddedMessageListener = builder.embeddedMessageListener;
        this.embeddedListener = builder.embeddedListener;
    }

    /**
     * Custom listener which returns embeddedMessageListener
     * @deprecated Use SwrveEmbeddedListener
     * @return The custom listener
     */
    public SwrveEmbeddedMessageListener getEmbeddedMessageListener() {
        return embeddedMessageListener;
    }

    /**
     * Custom listener which returns embeddedListener
     *
     * @return The custom listener
     */
    public SwrveEmbeddedListener getEmbeddedListener() {
        return embeddedListener;
    }

    public static class Builder {
        private SwrveEmbeddedMessageListener embeddedMessageListener = null;
        private SwrveEmbeddedListener embeddedListener = null;

        /**
         * Builder constructor
         */
        public Builder() {
        }

        /**
         * listener to process embedded campaign data for custom rendering
         *
         * @param embeddedMessageListener The custom listener
         * @return this builder
         */
        public SwrveEmbeddedMessageConfig.Builder embeddedMessageListener(SwrveEmbeddedMessageListener embeddedMessageListener) {
            this.embeddedMessageListener = embeddedMessageListener;
            return this;
        }

        /**
         * listener to process embedded campaign data for custom rendering
         *
         * @param embeddedListener The custom listener
         * @return this builder
         */
        public SwrveEmbeddedMessageConfig.Builder embeddedListener(SwrveEmbeddedListener embeddedListener) {
            this.embeddedListener = embeddedListener;
            return this;
        }

        public SwrveEmbeddedMessageConfig build() {
            return new SwrveEmbeddedMessageConfig(this);
        }

    }

}
