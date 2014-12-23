package io.converser.android;

import android.os.Build;
import android.util.Log;

import com.google.ciogson.Gson;
import com.google.ciogson.JsonObject;
import com.google.ciogson.JsonParseException;
import com.google.ciogson.JsonSyntaxException;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import io.converser.android.engine.BuildConfig;
import io.converser.android.model.ConversationAtom;
import io.converser.android.model.ConversationDetail;
import io.converser.android.model.ConversationReply;
import io.converser.android.model.Conversations;
import io.converser.android.model.OptInOutRequest;
import io.converser.android.model.SubscribeRequest;

/**
 * a proxy object representing the converser.io API
 * <p/>
 * All calls are syncronous, the caller will need to handle any async stuff
 *
 * @author Jason Connery
 */
class ConverserApi {

    private static final String HTTP_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_JSON = "application/json;charset=UTF-8";
    private static final String API_HEADER_KEY = "X-CONVERSER-APP-ID";
    private static final String DEVICE_HEADER_KEY = "X-CONVERSER-DEVICE-ID";
    protected String baseUrl = "https://api.converser.io";
    protected String apiKey;
    protected String deviceToken;

    protected Gson gson;

    protected KeyStore trustStore;
    protected SSLContext sslCtx;

    public ConverserApi(String apiKey) {
        disableConnectionReuseIfNecessary();
        gson = GsonHelper.getConfiguredGson();
        this.apiKey = apiKey;
    }

    public ConverserApi(String apiKey, String baseUrl) {
        this(apiKey);
        this.baseUrl = baseUrl;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    private HttpURLConnection buildConnection(URL url) throws IOException {
        return buildConnection(url, true);
    }

    private HttpURLConnection buildConnection(URL url, boolean includeToken) {
        HttpURLConnection urlConnection = null;
        try {
            if (url.getProtocol().equalsIgnoreCase("https")) {
                urlConnection = (HttpsURLConnection) url.openConnection();
            } else {
                urlConnection = (HttpURLConnection) url.openConnection();
            }
        } catch (IOException e) {
            if (url.equals(null)) {
                Log.e("Converser API", "The Converser Engine(API) tried to open a Https connection to a url but an IO exception occured. This could be because the url variable was null");
            } else {
                Log.e("Converser API", "The Converser Engine(API) tried to open a Https connection to" + url.toString() + " but an IO exception occured.");
            }
            e.printStackTrace();
        }

        urlConnection.setRequestProperty(HTTP_CONTENT_TYPE, CONTENT_TYPE_JSON);
        urlConnection.addRequestProperty(API_HEADER_KEY, apiKey);

        if (includeToken && deviceToken != null) {
            urlConnection.addRequestProperty(DEVICE_HEADER_KEY, deviceToken);
        }
        return urlConnection;
    }

    public String subscribe(SubscribeRequest subRequest) throws ApiException {

        HttpURLConnection connection = null;
        try {
            URL url = new URL(baseUrl + "/subscribe");
            connection = buildConnection(url, false);

            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            String jsonPostData = gson.toJson(subRequest);

            if (BuildConfig.DEBUG) {
                Log.d(Constants.LOGTAG, "Sending -> " + jsonPostData);
            }

            OutputStream os = connection.getOutputStream();
            os.write(jsonPostData.getBytes("UTF-8"));
            os.flush();
            os.close();

            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() >= 200 && connection.getResponseCode() <= 299) {
                String responseData = StreamHelper.convertStreamToString(in).trim();

                if (BuildConfig.DEBUG) {
                    Log.d(Constants.LOGTAG, "Received <-" + responseData + "->");
                }
                return responseData;
            } else {
                throw new ApiException(connection);
            }

        } catch (MalformedURLException e) {
            Log.e(Constants.LOGTAG, "Error talking to the api", e);
        } catch (IOException e) {
            if (connection != null) {
                try {
                    Log.e(Constants.LOGTAG, connection.getResponseCode() + ":" + connection.getResponseMessage());
                    throw new ApiException(connection);
                } catch (IOException e1) {
                    Log.e(Constants.LOGTAG, e1.getClass().toString() + " :: " + e1.getMessage());
                    e1.printStackTrace();
                }
            } else {
                Log.e(Constants.LOGTAG, "Error talking to the api", e);
                throw new ApiException(e);
            }
        }

        return null;
    }

    public String sendOptInOutRequest(OptInOutRequest request) throws ApiException {
        HttpURLConnection connection = null;
        JsonObject jsonPostData = new JsonObject();
        try {
            URL url = new URL(baseUrl + "/optout");

            boolean choice = request.getChoice();

            connection = buildConnection(url, true);
            // WARN: it is very important that the above build connection sets
            // includes the device tokens
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            jsonPostData.addProperty("all", choice);

            if (BuildConfig.DEBUG) {
                Log.d(Constants.LOGTAG, "Sending Json -> " + jsonPostData);
            }

            OutputStream os = connection.getOutputStream();
            os.write(jsonPostData.toString().getBytes("UTF-8"));
            os.flush();
            os.close();

            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() >= 200 && connection.getResponseCode() <= 299) {
                String responseData = StreamHelper.convertStreamToString(in).trim();

                if (BuildConfig.DEBUG) {
                    Log.d(Constants.LOGTAG, "Received <-" + responseData + "->");
                }
                return responseData;
            } else {
                throw new ApiException(connection);
            }

        } catch (MalformedURLException e) {
            Log.e(Constants.LOGTAG, "Error talking to the api", e);
        } catch (IOException e) {
            if (connection != null) {
                try {
                    Log.e(Constants.LOGTAG, connection.getResponseCode() + ":" + connection.getResponseMessage());
                    throw new ApiException(connection.getResponseCode(), connection.getResponseMessage());
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            } else {
                Log.e(Constants.LOGTAG, "Error talking to the api", e);
                throw new ApiException(e);
            }
        }

        return null;

    }

    public Conversations getConversations() throws ApiException {

        HttpURLConnection connection = null;
        try {
            URL url = new URL(baseUrl + "/inbox");
            connection = buildConnection(url);

            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() >= 200 && connection.getResponseCode() <= 299) {

                String responseData = StreamHelper.convertStreamToString(connection.getInputStream());
                // Conversations conv = gson.fromJson( new InputStreamReader(
                // connection.getInputStream() ), Conversations.class);

                if (BuildConfig.DEBUG) {
                    Log.d(Constants.LOGTAG, "Received <-" + responseData + "->");
                }

                try {
                    return gson.fromJson(responseData, Conversations.class);
                } catch (JsonSyntaxException ex) {
                    Log.e(Constants.LOGTAG, "JsonSyntaxException :: " + ex.getMessage());
                } catch (JsonParseException ex) {
                    Log.e(Constants.LOGTAG, "JsonParseException :: " + ex.getMessage());
                }
            } else {
                throw new ApiException(connection);
            }

        } catch (MalformedURLException e) {
            Log.e(Constants.LOGTAG, "Error talking to the api", e);
        } catch (IOException e) {
            Log.e(Constants.LOGTAG, "Error talking to the api", e);
            throw new ApiException(e);
        }

        return null;
    }

    public ConversationDetail getConversationDetail(String ref) throws ApiException {
        try {

            URL url = new URL(baseUrl + "/conversation/" + ref);
            HttpURLConnection connection = buildConnection(url);

            connection.setDoInput(true);
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() >= 200 && connection.getResponseCode() <= 299) {
                int rCode = connection.getResponseCode();
                if (rCode == 204) {
                    // No content to be expected
                    // The conversation has most likely finished or is no longer available. Create an empty conversation detail to pass up to the Engine
                    ConversationDetail finishedConversationDetails = new ConversationDetail();
                    finishedConversationDetails.setName("");
                    finishedConversationDetails.setTitle("");
                    finishedConversationDetails.setSubtitle("");
                    finishedConversationDetails.setDescription("");
                    finishedConversationDetails.setControls(new ArrayList<ConversationAtom>());
                    finishedConversationDetails.setContent(new ArrayList<ConversationAtom>());
                    Log.i("ConverserAPI :: sendConversationReply", "204 recieved. Conversation may be finished");
                    return finishedConversationDetails; // This may be considered an error, might need
                    // to think of a better way to signal this
                } else {
                    String responseData = StreamHelper.convertStreamToString(connection.getInputStream());

                    if (BuildConfig.DEBUG) {
                        Log.d(Constants.LOGTAG, "Recv <-" + responseData);
                    }
                    String tag;
                    String msg;
                    try {
                        ConversationDetail cd = gson.fromJson(responseData, ConversationDetail.class);
                        return cd;

                    } catch (JsonSyntaxException ex) {
                        tag = "ConverserApi :: getConversationDetail";
                        msg = "JsonSyntaxException thrown.";
                        Log.e(tag, msg);
                        ex.printStackTrace();
                    } catch (JsonParseException ex) {
                        tag = "ConverserApi :: getConversationDetail";
                        msg = "JsonParseException thrown.";
                        Log.e(tag, msg);
                        ex.printStackTrace();
                    } catch (Exception ex) {
                        tag = "ConverserApi :: getConversationDetail";
                        msg = "Exception thrown.";
                        Log.e(tag, msg);
                        ex.printStackTrace();
                    }
                }
            } else {
                Log.e(Constants.LOGTAG, "Non OK status : " + connection.getResponseCode());
                throw new ApiException(connection);
            }

        } catch (MalformedURLException e) {
            Log.e(Constants.LOGTAG, "Error talking to the api", e);
        } catch (IOException e) {
            Log.e(Constants.LOGTAG, "Error talking to the api", e);
            throw new ApiException(e);
        }

        return null;
    }

    public boolean deleteFromInbox(String inboxref) throws ApiException {
        try {

            URL url = new URL(baseUrl + "/inbox/" + inboxref);
            HttpURLConnection connection = buildConnection(url);

            connection.setDoInput(true);
            connection.setRequestMethod("DELETE");

            if (connection.getResponseCode() >= 200 && connection.getResponseCode() <= 299) {
                return true;
            } else {
                Log.e(Constants.LOGTAG, "Non OK status : " + connection.getResponseCode());
                return false;
            }

        } catch (MalformedURLException e) {
            Log.e(Constants.LOGTAG, e.getClass().toString() + " :: " + e.getMessage());
        } catch (IOException e) {
            Log.e(Constants.LOGTAG, e.getClass().toString() + " :: " + e.getMessage());
            throw new ApiException(e);
        }

        return false;
    }

    public ConversationDetail sendConversationReply(String ref, ConversationReply conversationReply) throws ApiException {

        try {

            String jsonData = gson.toJson(conversationReply);

            URL url = new URL(baseUrl + "/conversation/" + ref);
            HttpURLConnection connection = buildConnection(url);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");

            connection.getOutputStream().write(jsonData.getBytes("UTF-8"));

            if (connection.getResponseCode() >= 200 && connection.getResponseCode() <= 299) {
                int rCode = connection.getResponseCode();
                if (rCode == 204) {
                    // No content to be expected
                    // The conversation has most likely finished or is no longer available. Create an empty conversation detail to pass up to the Engine
                    ConversationDetail finishedConversationDetails = new ConversationDetail();
                    finishedConversationDetails.setName("");
                    finishedConversationDetails.setTitle("");
                    finishedConversationDetails.setSubtitle("");
                    finishedConversationDetails.setDescription("");
                    finishedConversationDetails.setControls(new ArrayList<ConversationAtom>());
                    finishedConversationDetails.setContent(new ArrayList<ConversationAtom>());
                    Log.i("ConverserAPI :: sendConversationReply", "204 recieved. Conversation may be finished");
                    return finishedConversationDetails; // This may be considered an error, might need
                    // to think of a better way to signal this
                } else {
                    String responseData = StreamHelper.convertStreamToString(connection.getInputStream());

                    if (BuildConfig.DEBUG) {
                        Log.d(Constants.LOGTAG, "Recv <-" + responseData);
                    }

                    try {

                        ConversationDetail cd = gson.fromJson(responseData, ConversationDetail.class);
                        return cd;

                    } catch (JsonSyntaxException ex) {
                        Log.e(Constants.LOGTAG, ex.getClass().toString() + " :: " + ex.getMessage());
                    } catch (JsonParseException ex) {
                        Log.e(Constants.LOGTAG, ex.getClass().toString() + " :: " + ex.getMessage());
                    }
                }

            } else {
                Log.e(Constants.LOGTAG, "Non OK status : " + connection.getResponseCode());
                throw new ApiException(connection);
            }

        } catch (MalformedURLException e) {
            Log.e(Constants.LOGTAG, "Error talking to the api", e);
        } catch (IOException e) {
            Log.e(Constants.LOGTAG, "Error talking to the api", e);
            throw new ApiException(e);
        }

        return null;
    }

    protected void disableConnectionReuseIfNecessary() {
        // HTTP connection reuse which was buggy pre-froyo
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    protected void developmentTurnOffSLLChecks() {
        HttpsURLConnection.setDefaultHostnameVerifier(new AllowAllHostnameVerifier());
    }

    private static class UnsecureTrustManager implements X509TrustManager {

        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {

        }
    }

    private class MyTrustManager implements X509TrustManager {
        private X509TrustManager defaultTrustManager;
        private X509TrustManager localTrustManager;

        private X509Certificate[] acceptedIssuers;

        public MyTrustManager(KeyStore localKeyStore) {
            // init defaultTrustManager using the system defaults
            // init localTrustManager using localKeyStore
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException ce) {
                localTrustManager.checkServerTrusted(chain, authType);
            }
        }

        // ...
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

    }

}
