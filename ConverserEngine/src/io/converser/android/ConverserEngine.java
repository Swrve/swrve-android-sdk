package io.converser.android;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.converser.android.model.ConversationDetail;
import io.converser.android.model.ConversationItem;
import io.converser.android.model.ConversationReply;
import io.converser.android.model.Conversations;
import io.converser.android.model.SubscribeRequest;
import io.converser.android.model.SubscribeRequest.Device;

public class ConverserEngine {
    private static ConverserContext myContext = new ConverserContext();
    private static Handler MAINHANDLER = new Handler(Looper.getMainLooper());
    private ConverserApi api;
    private ExecutorService operationsService = null;

    /**
     * init(string) must be called before using this constructor;
     */
    public ConverserEngine(Context context) {
        if (myContext.apiEndpoint != null) {
            this.api = new ConverserApi(myContext.apiKey, myContext.apiEndpoint);
        } else {
            this.api = new ConverserApi(myContext.apiKey);
        }
        operationsService = Executors.newSingleThreadExecutor();

        if (myContext.deviceId != null) {
            this.api.setDeviceToken(myContext.deviceId);
        } else {
            Log.d(Constants.LOGTAG, "No device id after creating instance of engine");
        }
    }

    public static void init(Context context, String apiKey) {
        myContext.apiKey = apiKey;
    }

    public static void init(Context context, String apiKey, String endpoint) {
        myContext.apiKey = apiKey;
        myContext.apiEndpoint = endpoint;
    }

    /**
     * Gets the current device token that new Engines would use.
     *
     * @return
     */
    public static String getDeviceToken() {
        return myContext.deviceId;
    }

    public static boolean isSubscribed() {
        return myContext.deviceId != null;
    }

    public static void clearSubscriberInfo() {
        myContext.deviceId = null;
    }

    /**
     * Subscribe to the service
     * <p/>
     * If a generic identity is needed, see Installation.id
     *
     * @param identity
     * @param pushTokens pushTokens, or null
     * @param callback   , or null. The Callback type can be anything in this instance, it is not used.
     * @see io.converser.android.Installation.id
     */
    public void subscribe(Context applicationContext, final String identity, Device.Tokens pushTokens, final ConverserEngine.Callback<Void> callback) {
        if (myContext.deviceId != null) {
            if (callback != null) {
                MAINHANDLER.post(new CallbackSuccessRunner<Void>(callback, null));
            }
            return;
        }

        PackageManager pm = applicationContext.getPackageManager();
        PackageInfo pi = null;

        String appId = "NA";
        String version = "NA";

        try {
            appId = applicationContext.getPackageName();
            pi = pm.getPackageInfo(applicationContext.getPackageName(), 0);

            version = pi.versionName;

        } catch (NameNotFoundException e) {

        }

        final SubscribeRequest req = new SubscribeRequest(applicationContext, appId, version);
        req.getDevice().setTokens(pushTokens);

        operationsService.execute(new Runnable() {

            @Override
            public void run() {

                req.setIdentity(identity);
                String deviceToken;

                try {
                    deviceToken = api.subscribe(req);

                    if (deviceToken != null) {
                        api.setDeviceToken(deviceToken);
                        myContext.deviceId = deviceToken;

                        if (callback != null) {
                            MAINHANDLER.post(new CallbackSuccessRunner<Void>(callback, null));
                        }
                        return;
                    } else {
                        if (callback != null) {
                            MAINHANDLER.post(new CallbackErrorRunner(callback, "Error"));
                        }
                        return;
                    }

                } catch (ApiException e) {
                    Log.e(Constants.LOGTAG, "API Error ", e);
                    if (callback != null) {
                        MAINHANDLER.post(new CallbackErrorRunner(callback, e.getErrorText()));
                    }
                }


            }
        });
    }

    public void getConversations(final ConverserEngine.Callback<Conversations> callback) {

        operationsService.execute(new Runnable() {

            @Override
            public void run() {

                try {
                    Conversations convs = api.getConversations();

                    if (convs != null) {
                        /*
                    	 * If there are any expired convos, delete them.
                    	 * We are going to use an iterator for delete ability
                    	 */

                        Iterator<ConversationItem> iterator = convs.getItems().iterator();

                        MAINHANDLER.post(new CallbackSuccessRunner<Conversations>(callback, convs));
                    } else {
                        MAINHANDLER.post(new CallbackErrorRunner(callback, "Error"));
                    }
                } catch (ApiException apiex) {
                    MAINHANDLER.post(new CallbackErrorRunner(callback, apiex.getErrorText()));
                }

            }
        });
    }

    public void getConversationDetail(final String ref, final ConverserEngine.Callback<ConversationDetail> callback) {

        operationsService.execute(new Runnable() {

            @Override
            public void run() {

                try {
                    ConversationDetail conv = api.getConversationDetail(ref);

                    if (conv != null) {
                        MAINHANDLER.post(new CallbackSuccessRunner<ConversationDetail>(callback, conv));
                    } else {
                        MAINHANDLER.post(new CallbackErrorRunner(callback, "Error"));
                    }
                } catch (ApiException apiex) {
                    Log.e("ConverserEngine", "ApiException :: " + apiex.getErrorText());
                    MAINHANDLER.post(new CallbackErrorRunner(callback, apiex.getErrorText()));
                }

            }
        });
    }

    public void replyToConversation(final String ref, final ConversationReply reply, final ConverserEngine.Callback<ConversationDetail> callback) {

        operationsService.execute(new Runnable() {

            @Override
            public void run() {

                try {
                    ConversationDetail conv = api.sendConversationReply(ref, reply);

                    if (conv != null) {
                        MAINHANDLER.post(new CallbackSuccessRunner<ConversationDetail>(callback, conv));
                    } else {
                        MAINHANDLER.post(new CallbackErrorRunner(callback, "Error"));
                    }
                } catch (ApiException apiex) {
                    MAINHANDLER.post(new CallbackErrorRunner(callback, apiex.getErrorText()));
                }

            }
        });
    }

    /**
     * Cancel all operations pending.
     */
    public void cancelOperations() {
        operationsService.shutdownNow();
    }

    @Override
    protected void finalize() throws Throwable {
        operationsService.shutdownNow();
        super.finalize();
    }

    /**
     * The interface used to return the result of an operation
     *
     * @param <T>
     * @author Jason Connery
     */
    public interface Callback<T> {

        void onSuccess(T response);

        void onError(String error);

    }

    /**
     * Runs a callback via Runnable interface. Useful in conjunction with a handler to make the callback happen
     * on a different thread
     *
     * @param <T>
     * @author Jason Connery
     */
    private static class CallbackSuccessRunner<T> implements Runnable {
        private Callback<T> callback;
        private T response;

        public CallbackSuccessRunner(Callback<T> callback, T response) {
            this.callback = callback;
            this.response = response;
        }

        @Override
        public void run() {
            callback.onSuccess(response);
        }
    }

    /**
     * Runs a callback via Runnable interface. Useful in conjunction with a handler to make the callback happen
     * on a different thread
     *
     * @author Jason Connery
     */
    private static class CallbackErrorRunner implements Runnable {
        private Callback<?> callback;
        private String error;

        public CallbackErrorRunner(Callback<?> callback, String error) {
            this.callback = callback;
            this.error = error;
        }

        @Override
        public void run() {
            callback.onError(error);
        }
    }

    private static class ConverserContext {

        String apiKey = null;
        String deviceId = null;
        String apiEndpoint = null;

    }


}
