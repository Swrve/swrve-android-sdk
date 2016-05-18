package com.swrve.sdk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class TestHTTPServer extends NanoHTTPD {
    private final static String MIME_JSON = "application/json";
    private Response.IStatus defaultStatus = Response.Status.OK;
    private String defaultResponseBody = "";
    private final List<IHTTPSession> requests;

    public TestHTTPServer(int port) {
        super(port);
        requests = new ArrayList<IHTTPSession>();
    }

    public interface IRequestHandler {
        Response serve(String uri, Method method, Map<String, String> header, Map<String, String> params);
    }

    private final Map<String, IRequestHandler> handlers = new HashMap<String, IRequestHandler>();

    public void setHandler(String uriContains, IRequestHandler handler) {
        handlers.put(uriContains, handler);
    }

    public void removeHandler(String uriContains) {
        handlers.remove(uriContains);
    }

    public void clearHandlers() {
        handlers.clear();
    }

    @Override
    public Response serve(IHTTPSession session) {
        // Save request for tests
        requests.add(session);

        Response result = null;
        String uri = session.getUri();
        Iterator<String> it = handlers.keySet().iterator();
        while(it.hasNext() && result == null) {
            String key = it.next();
            if (uri.contains(key)) {
                result = handlers.get(key).serve(uri, session.getMethod(), session.getHeaders(), session.getParms());
            }
        }

        if (result == null) {
            result  = newFixedLengthResponse(defaultStatus, MIME_JSON, defaultResponseBody);
        }

        return result;
    }

    public void setDefaultResponse(Response.IStatus status, String responseBody) {
        defaultStatus = status;
        defaultResponseBody = responseBody;
    }
}
