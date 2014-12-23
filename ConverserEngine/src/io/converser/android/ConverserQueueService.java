package io.converser.android;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;

import com.squareup.tape.FileObjectQueue;

import io.converser.android.engine.BuildConfig;

public class ConverserQueueService extends Service implements Queueable.Callback {

    private FileObjectQueue<Queueable> queue;
    private boolean running = false;
    private ConnectivityManager cm;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        this.queue = ConverserQueueFactory.getQueue(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        processNext();
        return START_STICKY;
    }

    private void processNext() {


        if (running) {
            return;
        }

        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            //Disable the broadcast receiver that alerts any time there's a network change
            ComponentName receiver = new ComponentName(this, NetworkStatusReceiver.class);

            PackageManager pm = getPackageManager();

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);

        } else {
            //Oh dear, we want ot process a queued thing, but can't.
            //Enable the receiver so we know when to try again

            if (BuildConfig.DEBUG) {
                Log.d(Constants.LOGTAG, "No active network, going to wait for network");

            }
            ComponentName receiver = new ComponentName(this, NetworkStatusReceiver.class);

            PackageManager pm = getPackageManager();

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);

            //Skip attempting to process a queue item. no point.
            return;
        }


        Queueable qItem = queue.peek();

        if (qItem != null) {
            running = true;

            qItem.run(this, this);
            return;
        } else {
            stopSelf();
        }


    }

    @Override
    public void onSuccess(Queueable queueItem) {

        queue.remove();
        running = false;
        processNext();
    }

    @Override
    public void onFailure(Queueable queueItem) {

        if (queueItem.failedNetwork()) {

            //Turn on the broadcast to know when to retry
            ComponentName receiver = new ComponentName(this, NetworkStatusReceiver.class);

            PackageManager pm = getPackageManager();

            pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
        }
        queue.remove();
        queueItem.incrementFailCount();
        running = false;
        queue.add(queueItem);
    }


}
