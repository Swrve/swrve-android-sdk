package com.swrve.sdk;

import org.junit.Assert;
import org.junit.Test;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class SwrveSSLSocketFactoryConfigTest {

    @Test
    public void testSSLSocketFactoryConfig() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{}, null);

        SSLSocketFactory events = context.getSocketFactory();
        SSLSocketFactory content = context.getSocketFactory();
        SSLSocketFactory identity = context.getSocketFactory();
        SSLSocketFactory contentCDN = context.getSocketFactory();

        SwrveSSLSocketFactoryConfig sslSocketFactoryConfig = new SwrveSSLSocketFactoryConfig(events, content, identity, contentCDN);

        Assert.assertEquals(events, sslSocketFactoryConfig.getFactory("api.swrve"));
        Assert.assertEquals(content, sslSocketFactoryConfig.getFactory("content.swrve"));
        Assert.assertEquals(identity, sslSocketFactoryConfig.getFactory("identity.swrve"));
        Assert.assertEquals(contentCDN, sslSocketFactoryConfig.getFactory("content-cdn.swrve"));
    }

}