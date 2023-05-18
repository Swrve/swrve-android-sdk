package com.swrve.sdk;

import javax.net.ssl.SSLSocketFactory;

public class SwrveSSLSocketFactoryConfig {

    private final SSLSocketFactory eventsSSLSocketFactory;
    private final SSLSocketFactory contentSSLSocketFactory;
    private final SSLSocketFactory identitySSLSocketFactory;
    private final SSLSocketFactory cdnSSLSocketFactory;

    public SwrveSSLSocketFactoryConfig(SSLSocketFactory eventsSSLSocketFactory, SSLSocketFactory contentSSLSocketFactory, SSLSocketFactory identitySSLSocketFactory, SSLSocketFactory cdnSSLSocketFactory) {
        this.eventsSSLSocketFactory = eventsSSLSocketFactory;
        this.contentSSLSocketFactory = contentSSLSocketFactory;
        this.identitySSLSocketFactory = identitySSLSocketFactory;
        this.cdnSSLSocketFactory = cdnSSLSocketFactory;
    }

    public SSLSocketFactory getFactory(String url) {
        if (url.contains("content-cdn.swrve") || url.contains("campaign-content.swrve")) {
            return getCdnSSLSocketFactory();
        } else if (url.contains("content.swrve")) {
            return getContentSSLSocketFactory();
        } else if (url.contains("api.swrve")) {
            return getEventsSSLSocketFactory();
        } else if (url.contains("identity.swrve")) {
            return getIdentitySSLSocketFactory();
        } else {
            return null;
        }
    }

    public SSLSocketFactory getEventsSSLSocketFactory() {
        return eventsSSLSocketFactory;
    }

    public SSLSocketFactory getContentSSLSocketFactory() {
        return contentSSLSocketFactory;
    }

    public SSLSocketFactory getIdentitySSLSocketFactory() {
        return identitySSLSocketFactory;
    }

    public SSLSocketFactory getCdnSSLSocketFactory() {
        return cdnSSLSocketFactory;
    }
}
