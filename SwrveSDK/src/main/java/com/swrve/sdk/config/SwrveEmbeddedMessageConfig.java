package com.swrve.sdk.config;

import com.swrve.sdk.messaging.SwrveEmbeddedListener;

public class SwrveEmbeddedMessageConfig {

    protected SwrveEmbeddedListener embeddedListener;

    private SwrveEmbeddedMessageConfig(SwrveEmbeddedMessageConfig.Builder builder) {
        this.embeddedListener = builder.embeddedListener;
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
        private SwrveEmbeddedListener embeddedListener = null;

        /**
         * Builder constructor
         */
        public Builder() {
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
