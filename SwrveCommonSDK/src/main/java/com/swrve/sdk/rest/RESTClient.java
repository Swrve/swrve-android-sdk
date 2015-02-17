package com.swrve.sdk.rest;

import com.swrve.sdk.SwrveHelper;

import org.apache.http.HttpStatus;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

/**
 * Used internally to implement an object capable to perform REST requests.
 */
public class RESTClient implements IRESTClient {

    private static final String CHARSET = "UTF-8";
    private static final int TIMEOUT_MS = 15000;
    private static final String COMMA_SEPARATOR = ", ", SEMICOLON_SEPARATOR = "; ";

    /**
     * Safeguarded against multiple writers
     * and on copy-and-clean when writing the headers.
     */
    private static List<String> metrics = new ArrayList<String>();

    public void get(String endpoint, IRESTResponseListener callback) {
        HttpURLConnection urlConnection = null;
        String responseBody = null;
        int responseCode = HttpStatus.SC_SERVICE_UNAVAILABLE;
        long connectTime = 0, responseHeaderTime = 0, responseBodyTime = 0;
        boolean isTimeout = false;

        InputStream wrapperIn = null;
        try {
            URL url = new URL(endpoint);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(TIMEOUT_MS);
            urlConnection.setConnectTimeout(TIMEOUT_MS);
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Accept-Charset", CHARSET);
            urlConnection.setRequestProperty("Connection", "close");
            urlConnection.setRequestProperty("Accept-Encoding", "gzip");
            urlConnection = addMetricsHeader(urlConnection);

            long start = System.nanoTime();
            urlConnection.connect();
            connectTime = milisecondsFrom(start);

            responseCode = urlConnection.getResponseCode();
            responseHeaderTime = milisecondsFrom(start);

            InputStream errorStream = urlConnection.getErrorStream();
            InputStream in = null;

            if (errorStream == null) {
                in = urlConnection.getInputStream();
                String encoding = urlConnection.getContentEncoding();

                if (encoding != null && encoding.toLowerCase().indexOf("gzip") != -1) {
                    in = new GZIPInputStream(in);
                } else {
                    in = new BufferedInputStream(in);
                }
            } else {
                in = new BufferedInputStream(errorStream);
            }
            wrapperIn = new SwrveFilterInputStream(in);
            responseBody = SwrveHelper.readStringFromInputStream(wrapperIn);

            responseBodyTime = milisecondsFrom(start);
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof SocketTimeoutException) {
                isTimeout = true;
            }
            if (callback != null) {
                callback.onException(e);
            }
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }

            if (wrapperIn != null) {
                try {
                    wrapperIn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            recordGetMetrics(endpoint, connectTime, responseHeaderTime, responseBodyTime, isTimeout);
        }
        if (callback != null) {
            RESTResponse response = new RESTResponse(responseCode, responseBody, urlConnection.getHeaderFields());
            callback.onResponse(response);
        }
    }

    private long milisecondsFrom(long timeInNanoseconds) {
        return TimeUnit.MILLISECONDS.convert(System.nanoTime() - timeInNanoseconds, TimeUnit.NANOSECONDS);
    }

    public void get(String endpoint, Map<String, String> params, IRESTResponseListener callback) throws UnsupportedEncodingException {
        get(endpoint + "?" + SwrveHelper.encodeParameters(params), callback);
    }

    public void post(String endpoint, String encodedBody, IRESTResponseListener callback) {
        post(endpoint, encodedBody, callback, "application/json");
    }

    public void post(String endpoint, String encodedBody, IRESTResponseListener callback, String contentType) {
        HttpURLConnection urlConnection = null;
        String responseBody = null;
        int responseCode = HttpStatus.SC_SERVICE_UNAVAILABLE;
        long connectTime = 0, requestBodyTime = 0, responseHeaderTime = 0, responseBodyTime = 0;
        boolean isTimeout = false;

        InputStream wrapperIn = null;
        try {
            byte[] bytes = encodedBody.getBytes("UTF-8");
            URL url = new URL(endpoint);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(TIMEOUT_MS);
            urlConnection.setConnectTimeout(TIMEOUT_MS);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", contentType);
            urlConnection.setRequestProperty("Accept-Charset", CHARSET);
            urlConnection.setRequestProperty("Connection", "close");
            urlConnection.setFixedLengthStreamingMode(bytes.length);
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);
            urlConnection = addMetricsHeader(urlConnection);

            long start = System.nanoTime();
            urlConnection.connect();
            connectTime = milisecondsFrom(start);

            OutputStream os = urlConnection.getOutputStream();
            os.write(bytes);
            os.close();
            requestBodyTime = milisecondsFrom(start);

            responseCode = urlConnection.getResponseCode();
            responseHeaderTime = milisecondsFrom(start);

            InputStream errorStream = urlConnection.getErrorStream();
            InputStream in = null;
            if (errorStream == null) {
                in = new BufferedInputStream(urlConnection.getInputStream());
            } else {
                in = new BufferedInputStream(errorStream);
            }
            wrapperIn = new SwrveFilterInputStream(in);
            responseBody = SwrveHelper.readStringFromInputStream(wrapperIn);

            responseBodyTime = milisecondsFrom(start);
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof SocketTimeoutException) {
                isTimeout = true;
            }
            if (callback != null) {
                callback.onException(e);
            }
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (wrapperIn != null) {
                try {
                    wrapperIn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            recordPostMetrics(endpoint, connectTime, requestBodyTime, responseHeaderTime, responseBodyTime, isTimeout);
        }
        if (callback != null) {
            RESTResponse response = new RESTResponse(responseCode, responseBody, urlConnection.getHeaderFields());
            callback.onResponse(response);
        }
    }

    /*
     * Print a sorted list with a custom separator
     */
    private String printList(List<String> list, String separarator) {
        StringBuffer buffer = new StringBuffer();
        int remains = list.size();
        for (String value : list) {
            buffer.append(value);
            if (--remains > 0) {
                buffer.append(separarator);
            }
        }
        return buffer.toString();
    }

    /*
     * Filter out the path and query parameter and keep the schema and authority (optionally port if exists)
     * e.g. http://api.swrve.com or https://staging-content.swrve.com:80
     */
    private String getUrlWithoutPathOrQuery(String url) throws MalformedURLException {
        URL fullUrl = new URL(url);
        return String.format("%s://%s", fullUrl.getProtocol(), fullUrl.getAuthority());
    }

    /*
     * Add a positive metric value to a list of metrics, formated with a template.
     *
     * If value of not positive, add error string and optionally set the metric to timeout value.
     *
     * If error, save the metrics early and return false.
     * Otherwise return true.
     */
    private boolean addMetric(List<String> metricList, long value, List<String> templates, String error, boolean timeout) {
        if (value > 0) {
            for (String metric : templates) {
                metricList.add(String.format(metric, value));
            }
        } else {
            metricList.add(error);
            if (timeout) {
                for (String metric : templates) {
                    metricList.add(String.format(metric, TIMEOUT_MS));
                }
            }
            synchronized (metrics) {
                metrics.add(printList(metricList, COMMA_SEPARATOR));
            }
            return false;
        }
        return true;
    }

    private void recordMetrics(boolean post, String url, long c, long sb, long rh, long rb, boolean timeout) {
        List<String> params = new ArrayList<String>();

        try {
            params.add(String.format("u=%s", getUrlWithoutPathOrQuery(url)));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        // Managing the list of metrics that need to be updated:
        // Keeping a list of the metric templates and clearing
        // it when the metrics have been written.
        // Using a list as there can be more than one
        // metric to be updated with the same value.
        List<String> templates = new ArrayList<String>();

        templates.add("c=%d");
        if (!addMetric(params, c, templates, "c_error=1", timeout)) return;
        templates.clear();

        // sh metric is never known, copy the value of next known
        templates.add("sh=%d");
        templates.add("sb=%d");

        // on post, the sb metric is known
        // add sb metric and break early on error
        // if sh metric is not known, copy the value of next known
        if (post) {
            if (!addMetric(params, sb, templates, "sb_error=1", timeout)) return;
            // if sh have been written, remove it
            templates.clear();
        }

        templates.add("rh=%d");
        if (!addMetric(params, rh, templates, "rh_error=1", timeout)) return;
        templates.clear();

        templates.add("rb=%d");
        if (!addMetric(params, rb, templates, "rb_error=1", timeout)) return;
        templates.clear();

        synchronized (metrics) {
            metrics.add(printList(params, COMMA_SEPARATOR));
        }
    }

    private void recordGetMetrics(String url, long c, long rh, long rb, boolean timeout) {
        recordMetrics(false, url, c, rh, rh, rb, timeout);
    }

    private void recordPostMetrics(String url, long c, long sb, long rh, long rb, boolean timeout) {
        recordMetrics(true, url, c, sb, rh, rb, timeout);
    }

    private HttpURLConnection addMetricsHeader(HttpURLConnection urlConnection) {
        List<String> metricList = new ArrayList<String>();
        synchronized (metrics) {
            metricList.addAll(metrics);
            metrics.clear();
        }
        if (!metricList.isEmpty()) {
            String metricsString = printList(metricList, SEMICOLON_SEPARATOR);
            urlConnection.addRequestProperty("Swrve-Latency-Metrics", metricsString);
        }
        return urlConnection;
    }

    public static void cleanMetrics() {
        synchronized (metrics) {
            metrics.clear();
        }
    }
}
