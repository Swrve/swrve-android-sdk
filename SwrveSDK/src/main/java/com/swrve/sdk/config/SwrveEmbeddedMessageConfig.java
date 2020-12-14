package com.swrve.sdk.config;

import com.swrve.sdk.messaging.SwrveEmbeddedMessageListener;

public class SwrveEmbeddedMessageConfig {

    protected SwrveEmbeddedMessageListener embeddedMessageListener;

    private SwrveEmbeddedMessageConfig(SwrveEmbeddedMessageConfig.Builder builder) {
        this.embeddedMessageListener = builder.embeddedMessageListener;
    }

    /**
     * Custom listener which returns embeddedMessageListener
     *
     * @return The custom listener
     */
    public SwrveEmbeddedMessageListener getEmbeddedMessageListener() {
        return embeddedMessageListener;
    }

    public static class Builder {
        private SwrveEmbeddedMessageListener embeddedMessageListener = null;

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

        public SwrveEmbeddedMessageConfig build() {
            return new SwrveEmbeddedMessageConfig(this);
        }

    }

}
