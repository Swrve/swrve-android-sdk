package io.converser.android;

import android.content.Context;
import android.content.Intent;

import com.squareup.tape.ObjectQueue;
import com.squareup.tape.ObjectQueue.Listener;

public class QueueServiceListener implements Listener<Queueable> {

    private Context context;

    public QueueServiceListener(Context context) {
        this.context = context;
    }

    @Override
    public void onAdd(ObjectQueue<Queueable> queue, Queueable item) {

        context.startService(new Intent(context, ConverserQueueService.class));
    }

    @Override
    public void onRemove(ObjectQueue<Queueable> arg0) {

    }

}
