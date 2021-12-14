package com.swrve.sdk.rest;

import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveSSLSocketFactoryConfig;
import com.swrve.sdk.SwrveTestUtils;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.shadows.ShadowLog;

import javax.net.ssl.SSLSocketFactory;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class RestClientTest extends SwrveBaseTest {

    private MockWebServer server;
    private static final int httpTimeout = 60000;

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        ShadowLog.stream = System.out;
        SwrveLogger.setLogLevel(1);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void testForMetrics() throws Exception {
        SSLSocketFactory socketFactory = SwrveTestUtils.setupLocalSllSocketFactory(server);
        SwrveSSLSocketFactoryConfig sslSocketFactoryConfig = SwrveTestUtils.mockCommonSocketFactory(socketFactory);

        RESTClient restClient = new RESTClient(httpTimeout, sslSocketFactoryConfig);
        restClient.metrics.clear();

        Dispatcher dispatcher = getDispatcherWithLastMetricsInBody();
        server.setDispatcher(dispatcher);
        server.start();
        String endPoint = server.url("/").toString();

        IRESTResponseListener restResponseListener1 = new IRESTResponseListener() {
            @Override
            public void onResponse(RESTResponse response) {
                try {
                    JSONObject responseJson = new JSONObject(response.responseBody);
                    String metric = responseJson.getString("last_metric");
                    assertNotNull(metric);
                    assertEquals(metric, "");
                } catch (Exception ex) {
                    SwrveLogger.e("RestClientTest: Shouldn't be an exception", ex);
                    Assert.fail("RestClientTest:Shouldn't be an exception:" + ex.getMessage());
                }
            }

            @Override
            public void onException(Exception ex) {
                SwrveLogger.e("RestClientTest: Shouldn't be an exception", ex);
                Assert.fail("RestClientTest:Shouldn't be an exception:" + ex.getMessage());
            }
        };
        restClient.get(endPoint, restResponseListener1);

        IRESTResponseListener restResponseListener2 = new IRESTResponseListener() {
            @Override
            public void onResponse(RESTResponse response) {
                try {
                    JSONObject responseJson = new JSONObject(response.responseBody);
                    String metric = responseJson.getString("last_metric");
                    assertNotNull(metric);
                    assertNotEquals(metric, "");
                    String url = metric.substring(metric.indexOf("=") + 1, metric.indexOf(",")) + "/";
                    assertEquals(endPoint, url);
                } catch (Exception ex) {
                    SwrveLogger.e("RestClientTest: Shouldn't be an exception", ex);
                    Assert.fail("RestClientTest:Shouldn't be an exception:" + ex.getMessage());
                }
            }

            @Override
            public void onException(Exception ex) {
                SwrveLogger.e("RestClientTest: Shouldn't be an exception", ex);
                Assert.fail("RestClientTest:Shouldn't be an exception:" + ex.getMessage());
            }
        };
        restClient.get(endPoint, restResponseListener2);

        IRESTResponseListener restResponseListener3 = new IRESTResponseListener() {
            @Override
            public void onResponse(RESTResponse response) {
                try {
                    JSONObject responseJson = new JSONObject(response.responseBody);
                    String metric = responseJson.getString("last_metric");
                    assertNotNull(metric);
                    assertNotEquals(metric, "");
                    String url = metric.substring(metric.indexOf("=") + 1, metric.indexOf(",")) + "/";
                    assertEquals(endPoint, url);
                } catch (Exception ex) {
                    SwrveLogger.e("RestClientTest: Shouldn't be an exception", ex);
                    Assert.fail("RestClientTest:Shouldn't be an exception:" + ex.getMessage());
                }
            }

            @Override
            public void onException(Exception ex) {
                SwrveLogger.e("RestClientTest: Shouldn't be an exception", ex);
                Assert.fail("RestClientTest:Shouldn't be an exception:" + ex.getMessage());
            }
        };
        restClient.get(endPoint, restResponseListener3);
    }

    @Test
    public void testGETRequestWithGzipEncoding() throws Exception {
        String body = "{}";
        Dispatcher dispatcher = getDispatcher(body, 200, false);
        server.setDispatcher(dispatcher);
        SSLSocketFactory socketFactory = SwrveTestUtils.setupLocalSllSocketFactory(server);
        SwrveSSLSocketFactoryConfig sslConfig = SwrveTestUtils.mockCommonSocketFactory(socketFactory);

        server.start();
        String endPoint = server.url("/").toString();

        IRESTResponseListener restResponseListener = new IRESTResponseListener() {
            @Override
            public void onResponse(RESTResponse response) {
                assertEquals(200, response.responseCode);
                assertEquals(body, response.responseBody);
            }

            @Override
            public void onException(Exception ex) {
                Assert.fail("RestClientTest:Shouldn't be an exception:" + ex.getMessage());
            }
        };
        IRESTResponseListener restResponseListenerSpy = Mockito.spy(restResponseListener);
        RESTClient restClient = new RESTClient(httpTimeout, sslConfig);
        restClient.get(endPoint, restResponseListenerSpy);

        Mockito.verify(restResponseListenerSpy, Mockito.atLeastOnce()).onResponse(Mockito.any(RESTResponse.class));
        Mockito.verify(restResponseListenerSpy, Mockito.never()).onException(Mockito.any(Exception.class));
    }

    @Test
    public void testGETRequestWithGzipEncodingForErrorStream() throws Exception {
        String body = "{" +
                "\"id\": \"xxxxx\"," +
                "\"message\": \"Invalid parameter: Invalid API key\"," +
                "\"status\": 403" +
                "}";
        Dispatcher dispatcher = getDispatcher(body, 403, true);
        server.setDispatcher(dispatcher);

        SSLSocketFactory socketFactory = SwrveTestUtils.setupLocalSllSocketFactory(server);
        SwrveSSLSocketFactoryConfig sslConfig = SwrveTestUtils.mockCommonSocketFactory(socketFactory);
        ;

        server.start();
        String endPoint = server.url("/").toString();

        IRESTResponseListener restResponseListener = new IRESTResponseListener() {
            @Override
            public void onResponse(RESTResponse response) {
                assertEquals(403, response.responseCode);
                assertEquals(body, response.responseBody);
            }

            @Override
            public void onException(Exception ex) {
                SwrveLogger.e("RestClientTest: Shouldn't be an exception", ex);
                Assert.fail("RestClientTest:Shouldn't be an exception:" + ex.getMessage());
            }
        };
        IRESTResponseListener restResponseListenerSpy = Mockito.spy(restResponseListener);
        RESTClient restClient = new RESTClient(httpTimeout, sslConfig);
        restClient.get(endPoint, restResponseListenerSpy);

        Mockito.verify(restResponseListenerSpy, Mockito.atLeastOnce()).onResponse(Mockito.any(RESTResponse.class));
        Mockito.verify(restResponseListenerSpy, Mockito.never()).onException(Mockito.any(Exception.class));
    }

    private Dispatcher getDispatcher(String body, int reponseCode, boolean gZipContentEncoding) {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String swrveMetric = request.getHeader("swrve-latency-metrics");
                MockResponse response = new MockResponse()
                        .setBody(body)
                        .setResponseCode(reponseCode)
                        .addHeader("Content-Type", "application/json; charset=utf-8");
                if (SwrveHelper.isNotNullOrEmpty(swrveMetric)) {
                    response.addHeader("swrve-latency-metrics", swrveMetric);
                }
                if (gZipContentEncoding) {
                    Buffer gzippedBody = gzip(body);
                    response = response
                            .addHeader("Content-Encoding", "gzip")
                            .setBody(gzippedBody);
                }
                return response;
            }
        };
    }

    private Dispatcher getDispatcherWithLastMetricsInBody() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String swrveMetric = request.getHeader("swrve-latency-metrics");
                swrveMetric = SwrveHelper.isNullOrEmpty(swrveMetric) ? "" : swrveMetric;
                String body = "{\"last_metric\" : \"" + swrveMetric + "\"}";
                MockResponse response = new MockResponse()
                        .setBody(body)
                        .setResponseCode(200)
                        .addHeader("Content-Type", "application/json; charset=utf-8");
                return response;
            }
        };
    }

    private Buffer gzip(String data) {
        Buffer result = new Buffer();
        BufferedSink sink = Okio.buffer(new GzipSink(result));
        try {
            sink.writeUtf8(data);
            sink.close();
        } catch (Exception e) {
            SwrveLogger.e("Exception gzip", e);
        }
        return result;
    }
}
