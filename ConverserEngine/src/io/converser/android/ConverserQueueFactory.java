package io.converser.android;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.squareup.tape.FileObjectQueue;

import java.io.File;
import java.io.IOException;

public class ConverserQueueFactory {

    private static final String FILENAME = "converser_tape_queue";

    private static FileObjectQueue<Queueable> queue = null;

    public synchronized static FileObjectQueue<Queueable> getQueue(Context context) {

        if (queue == null) {
            File queueFile = new File(context.getFilesDir(), FILENAME);

            try {
                queue = new FileObjectQueue<Queueable>(queueFile, new QueueableConverter(GsonHelper.getConfiguredGson()));
                //This listener is static global, so going to ensure app context here to try prevent leaking
                queue.setListener(new QueueServiceListener(context.getApplicationContext()));
            } catch (IOException e) {

                Log.e(Constants.LOGTAG, "Error initing queue, can't continue", e);
            }
        }

        return queue;
    }

    public static void init(Context context) {
        //get the queue
        getQueue(context);

        //Fire off a request to the service, that'll process any pending items from last time
        context.startService(new Intent(context, ConverserQueueService.class));
    }
}
