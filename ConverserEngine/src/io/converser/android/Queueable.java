package io.converser.android;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Date;

import io.converser.android.engine.BuildConfig;
import io.converser.android.model.FeedbackRequest;

/**
 * All the info needed for a queueable, deferrable operation.
 *
 * @author Jason Connery
 */
class Queueable {

    protected static final int FAIL_COUNT_MAX = 30;
    private static final String REQUEST_TYPE_FEEDBACK = "feedback";
    private static Handler mainHandler = new Handler(Looper.getMainLooper());
    protected ApiException failureException;
    /* all the things we need to configure a ConverserEngine to recreate it exactly as the request at the time needs it */
    private String deviceId;
    private String apiKey;
    private String endpointUrl;
    private Date createdAt;
    private String requestType;
    private FeedbackRequest feedbackRequest;
    private int failureCount = 0;

    public Queueable() {

    }

    public Queueable(String apiEndpoint, String apiKey,
                     ConverserEngine converserEngine, String deviceId) {

        this.deviceId = deviceId;
        this.apiKey = apiKey;
        this.endpointUrl = apiEndpoint;
        this.createdAt = new Date();
    }

    public void setFeedbackRequest(FeedbackRequest request) {
        this.feedbackRequest = request;
        this.requestType = REQUEST_TYPE_FEEDBACK;
    }

    public void run(Context context, final Callback callback) {


        Thread apiCall = new Thread(new Runnable() {


            @Override
            public void run() {

                if (getFailureCount() > FAIL_COUNT_MAX) {
                    if (BuildConfig.DEBUG) {
                        Log.d(Constants.LOGTAG, "Queued Item has exceeded it's limit.");
                    }

                    //'fake' a success to remove from queue and continue
                    doSuccess(callback);
                    return;
                }
                ConverserApi api = new ConverserApi(apiKey, endpointUrl);
                api.setDeviceToken(deviceId);

                if (requestType.equalsIgnoreCase(REQUEST_TYPE_FEEDBACK)) {
                    try {

                        boolean result = api.sendFeedback(feedbackRequest);

                        if (result) {
                            doSuccess(callback);
                        } else {
                            doFail(callback);
                        }
                    } catch (ApiException e) {

                        if (e.getResponseCode() == 429) {
                            //This is a rate limit wall. Going to sleep for 10 seconds to try ease the pain
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e1) {
                                //Nothing to do here
                            }
                        }

                        Log.e(Constants.LOGTAG, "Queued Feedback failure due to exception!", e);
                        failureException = e;

                        doFail(callback);
                    }
                }
            }
        });

        apiCall.start();

    }

    protected void doSuccess(final Callback callback) {
        mainHandler.post(new Runnable() {

            @Override
            public void run() {
                callback.onSuccess(Queueable.this);
            }
        });
    }

    protected void doFail(final Callback callback) {
        mainHandler.post(new Runnable() {

            @Override
            public void run() {
                callback.onFailure(Queueable.this);
            }
        });
    }

    public boolean failedNetwork() {
        return true;
    }

    public void incrementFailCount() {

        failureCount++;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public interface Callback {
        void onSuccess(Queueable queueItem);

        void onFailure(Queueable queueItem);
    }

}
