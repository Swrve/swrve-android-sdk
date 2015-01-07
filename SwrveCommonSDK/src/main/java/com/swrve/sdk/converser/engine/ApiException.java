package com.swrve.sdk.converser.engine;

import android.util.Log;

import com.swrve.sdk.common.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;


/**
 * Use getErrorText() for a use message.
 *
 * @author Jason Connery
 */
public class ApiException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = -1325365984433399102L;

    private int responseCode = -1;
    private String responseMessage = null;
    private String errorText = null;


    public ApiException(int responseCode, String responseMessage) {
        super();
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.errorText = responseMessage;
    }

    public ApiException(IOException ioex) {
        super(ioex);
        errorText = ioex.getMessage();
    }

    public ApiException(String message) {
        super(message);
        errorText = message;
    }

    /**
     * Create exception object from a connection, populating fields where possible
     *
     * @param connection
     */
    public ApiException(HttpURLConnection connection) {
        super();

        if (connection != null) {
            try {
                responseCode = connection.getResponseCode();
                responseMessage = connection.getResponseMessage();

                String contents = StreamHelper.convertStreamToString(connection.getErrorStream());

                if (BuildConfig.DEBUG) {
                    Log.d(Constants.LOGTAG, "Going to try parse error msg " + contents);
                }
                JSONObject obj = new JSONObject(contents);

                errorText = obj.optString("error_text");

            } catch (IOException e) {
                Log.e(Constants.LOGTAG, "IO:Error interperting error msg", e);
            } catch (JSONException e) {
                Log.e(Constants.LOGTAG, "JSON:Error interperting error msg", e);
            }
        }


    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public String getErrorText() {
        return errorText;
    }

    @Override
    public String toString() {
        return responseCode + " : " + responseMessage;
    }


}
