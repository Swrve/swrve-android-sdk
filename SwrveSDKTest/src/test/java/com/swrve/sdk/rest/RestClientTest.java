package com.swrve.sdk.rest;

import android.util.Log;

import com.swrve.sdk.SwrveBaseTest;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.config.SwrveConfig;
import com.swrve.sdk.rest.RESTClient;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class RestClientTest extends SwrveBaseTest {

    private MockWebServer server;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        server = new MockWebServer();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        server.shutdown();
    }

    @Test
    public void testForMetrics() throws Exception {
        RESTClient restClient = new RESTClient(60);
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
                } catch (Exception exp) {
                    Assert.fail("Shouldn't be an exception");
                }
            }

            @Override
            public void onException(Exception ex) {
                Assert.fail("Shouldn't be an exception");
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
                } catch (Exception exp) {
                    Assert.fail("Shouldn't be an exception");
                }
            }

            @Override
            public void onException(Exception ex) {
                Assert.fail("Shouldn't be an exception");
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
                } catch (Exception exp) {
                    Assert.fail("Shouldn't be an exception");
                }
            }

            @Override
            public void onException(Exception ex) {
                Assert.fail("Shouldn't be an exception");
            }
        };
        restClient.get(endPoint, restResponseListener3);
    }

    @Test
    public void testGETRequestWithGzipEncoding() throws Exception {
        String body = "{}";
        Dispatcher dispatcher = getDispatcher(body, 200, false);
        server.setDispatcher(dispatcher);
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
                Assert.fail("Shouldn't be an exception");
            }
        };
        IRESTResponseListener restResponseListenerSpy = Mockito.spy(restResponseListener);
        RESTClient restClient = new RESTClient(60);
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
                Assert.fail("Shouldn't be an exception");
            }
        };
        IRESTResponseListener restResponseListenerSpy = Mockito.spy(restResponseListener);
        RESTClient restClient = new RESTClient(60);
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
                        .addHeader("swrve-latency-metrics", swrveMetric)
                        .addHeader("Content-Type", "application/json; charset=utf-8");
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
