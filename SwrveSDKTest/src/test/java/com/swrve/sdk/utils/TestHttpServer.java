package com.swrve.sdk.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class TestHttpServer extends NanoHTTPD {
    private final static String MIME_JSON = "application/json";
    private Response.Status defaultResponseCode = Response.Status.OK;
    private String defaultResponseBody = "";

    public List<TestHttpPostRequest> postRequests;

    public TestHttpServer(int port) {
        super(port);
        postRequests = new ArrayList<TestHttpPostRequest>();
    }

    public interface ITestHttpServerHandler {
        NanoHTTPD.Response serve(String uri, Method method, Map<String, String> header, Map<String, String> params);
    }

    private final Map<String, ITestHttpServerHandler> handlers = new HashMap<String, ITestHttpServerHandler>();

    public void setHandler(String uriPrefix, ITestHttpServerHandler handler) {
        handlers.put(uriPrefix, handler);
    }

    public void removeHandler(String uriPrefix) {
        handlers.remove(uriPrefix);
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> files = new HashMap<String, String>();
        Method method = session.getMethod();
        if (Method.PUT.equals(method) || Method.POST.equals(method)) {
            try {
                session.parseBody(files);
            } catch (IOException ioe) {
                return new Response(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            } catch (ResponseException re) {
                return new Response(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
            }
        }

        Map<String, String> params = session.getParms();
        String uri = session.getUri();

        if (Method.POST.equals(method)) {
            TestHttpPostRequest request = new TestHttpPostRequest();
            request.body = session.getQueryParameterString();
            request.params = params;
            postRequests.add(request);
        }

        Response result = null;
        Iterator<String> it = handlers.keySet().iterator();
        while(it.hasNext() && result == null) {
            String key = it.next();
            if (uri.contains(key)) {
                result = handlers.get(key).serve(uri, method, session.getHeaders(), params);
            }
        }

        if (result == null) {
            result = new Response(defaultResponseCode, MIME_JSON, defaultResponseBody);
        }

        return result;
    }

    public void setDefaultResponse(Response.Status responseCode, String responseBody) {
        defaultResponseCode = responseCode;
        defaultResponseBody = responseBody;
    }

    public class TestHttpPostRequest {
        public String body;
        public Map<String, String> params;

        public JSONArray getEventsJSON() throws JSONException {
            JSONObject req = new JSONObject(body);
            if (req != null && req.has("data")) {
                return req.getJSONArray("data");
            }
            return null;
        }
    }
}
