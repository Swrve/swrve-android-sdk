package com.swrve.sdk.rest;

import java.util.List;
import java.util.Map;

public class RESTResponseLog {

    public final int code;
    public final int eventsCount;
    public final int requestCount;
    // If there are repeated logs with same response code then only the last body, headers and time are logged.
    public final String body;
    public final String headers;
    public final long time;

    public RESTResponseLog(int code, int eventsCount, int requestCount, long time, String body, Map<String, List<String>> headers) {
        this.code = code;
        this.eventsCount = eventsCount;
        this.requestCount = requestCount;
        this.time = time;

        if (body != null && body.length() > 250) {
            this.body = body.substring(0, 250); // truncate
        } else {
            this.body = body;
        }

        if (headers != null && headers.toString().length() > 250) {
            this.headers = headers.toString().substring(0, 250);  // truncate
        } else {
            this.headers = headers.toString();
        }
    }
}