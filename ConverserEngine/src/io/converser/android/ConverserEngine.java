package io.converser.android;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.squareup.tape.FileObjectQueue;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.converser.android.model.ConversationDetail;
import io.converser.android.model.ConversationItem;
import io.converser.android.model.ConversationReply;
import io.converser.android.model.Conversations;
import io.converser.android.model.FeedbackRequest;
import io.converser.android.model.OptInOutRequest;
import io.converser.android.model.Qualifications;
import io.converser.android.model.SubscribeRequest;
import io.converser.android.model.SubscribeRequest.Device;
import io.converser.android.model.UpdateQualificationRequest;

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

        ConverserQueueFactory.init(context);
    }

    public static void init(Context context, String apiKey, String endpoint) {
        myContext.apiKey = apiKey;
        myContext.apiEndpoint = endpoint;


        ConverserQueueFactory.init(context);
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

    public void sendOptInOutRequest(boolean optIn) {
        // Engine prequisites first
        final OptInOutRequest req = new OptInOutRequest(false);
        req.setChoice(optIn);

        // Api call from here
        operationsService.execute(new Runnable() {

            @Override
            public void run() {

                try {
                    api.sendOptInOutRequest(req);

                } catch (ApiException e) {
                    Log.e(Constants.LOGTAG,
                            "API Error, could not send opt out request", e);
                }
            }
        });
    }

    public void sendOptInOutRequest(final OptInOutRequest req) {
        operationsService.execute(new Runnable() {

            @Override
            public void run() {

                try {
                    api.sendOptInOutRequest(req);

                } catch (ApiException e) {
                    Log.e(Constants.LOGTAG,
                            "API Error, could not send opt out request", e);
                }
            }
        });
    }

    /**
     * Send Feedback. Also, see the queueFeedback method for a more reliable alternative
     *
     * @param reaction
     * @param area
     * @param text
     * @param callback
     */
    public void sendFeedback(int reaction, String area, String text, final ConverserEngine.Callback<Boolean> callback) {

        final FeedbackRequest fr = new FeedbackRequest(reaction, area, text);

        operationsService.execute(new Runnable() {

            @Override
            public void run() {

                try {
                    boolean res = api.sendFeedback(fr);

                    if (res) {
                        MAINHANDLER.post(new CallbackSuccessRunner<Boolean>(callback, res));
                    } else {
                        MAINHANDLER.post(new CallbackErrorRunner(callback, "Error"));
                    }
                } catch (ApiException apiex) {
                    MAINHANDLER.post(new CallbackErrorRunner(callback, apiex.getErrorText()));
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

                        while (iterator.hasNext()) {
                            ConversationItem item = iterator.next();
                            if (item.getStatus().equalsIgnoreCase("expired")) {
                                try {
                                    api.deleteFromInbox(item.getRef());
                                    iterator.remove();
                                } catch (Exception ex) {
                                    Log.e(Constants.LOGTAG, "Error deleting item from inbox ", ex);
                                }
                            }
                        }

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

    public void deleteConversationFromInbox(final String inboxref, final ConverserEngine.Callback<Boolean> callback) {
        operationsService.execute(new Runnable() {

            @Override
            public void run() {

                try {
                    boolean res = api.deleteFromInbox(inboxref);
                    MAINHANDLER.post(new CallbackSuccessRunner<Boolean>(callback, res));
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
     * Update qualifications by adding and removing via updateRequest object.
     *
     * @param updateRequest
     * @param callback
     */
    public void updateQualifications(final UpdateQualificationRequest updateRequest, final ConverserEngine.Callback<Qualifications> callback) {
        operationsService.execute(new Runnable() {

            @Override
            public void run() {

                try {
                    Qualifications quals = api.sendData("/qualifications", updateRequest, Qualifications.class);

                    //If we got this far without an exception, we can consider it a success
                    MAINHANDLER.post(new CallbackSuccessRunner<Qualifications>(callback, quals));
                } catch (ApiException apiex) {
                    MAINHANDLER.post(new CallbackErrorRunner(callback, apiex.getErrorText()));
                }
            }
        });
    }

    public void getQualifications(final ConverserEngine.Callback<Qualifications> callback) {
        operationsService.execute(new Runnable() {

            @Override
            public void run() {

                try {
                    Qualifications quals = api.getData("/qualifications", null, Qualifications.class);

                    //If we got this far without an exception, we can consider it a success
                    MAINHANDLER.post(new CallbackSuccessRunner<Qualifications>(callback, quals));
                } catch (ApiException apiex) {
                    MAINHANDLER.post(new CallbackErrorRunner(callback, apiex.getErrorText()));
                }
            }
        });
    }

    /**
     * Send an arbitary data block to converser. The server must know what to do with it.
     * the object will be serialized before transmission
     * <p/>
     * If you dont want anything back, use Void  for K
     *
     * @param data
     * @param callback
     * @param classOfK , to help with serialization, need a class of k
     */
    public <K extends Object> void sendData(final String path, final Object data, final ConverserEngine.Callback<K> callback, final Class<K> classOfK) {
        operationsService.execute(new Runnable() {

            @Override
            public void run() {

                try {

                    K response = api.sendData(path, data, classOfK);

                	/* no null == fail, as maybe there's no response expected */
                    MAINHANDLER.post(new CallbackSuccessRunner<K>(callback, response));
                } catch (ApiException apiex) {
                    MAINHANDLER.post(new CallbackErrorRunner(callback, apiex.getErrorText()));
                }
            }
        });
    }

    public <K extends Object> void getData(final String path, final String optQueryString, final ConverserEngine.Callback<K> callback, final Class<K> classOfK) {
        operationsService.execute(new Runnable() {

            @Override
            public void run() {

                try {

                    K response = api.getData(path, optQueryString, classOfK);
                    MAINHANDLER.post(new CallbackSuccessRunner<K>(callback, response));
                } catch (ApiException apiex) {
                    MAINHANDLER.post(new CallbackErrorRunner(callback, apiex.getErrorText()));
                }
            }

        });
    }

    public void queueFeedback(Context context, int reaction, String area, String comment) {
        Queueable x = new Queueable(myContext.apiEndpoint, myContext.apiKey, this, myContext.deviceId);
        x.setFeedbackRequest(new FeedbackRequest(reaction, area, comment));

        FileObjectQueue<Queueable> queue = ConverserQueueFactory.getQueue(context);

        if (queue != null) {
            queue.add(x);
        }

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
     * @param <T>
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
