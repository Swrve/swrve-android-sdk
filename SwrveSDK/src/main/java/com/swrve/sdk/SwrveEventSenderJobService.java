package com.swrve.sdk;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;

import java.util.ArrayList;

import static com.swrve.sdk.SwrveBackgroundEventSender.EXTRA_EVENTS;
import static com.swrve.sdk.SwrveBackgroundEventSender.EXTRA_USER_ID;

@RequiresApi(api = Build.VERSION_CODES.O)
public class SwrveEventSenderJobService extends JobService {

    private static final int JOB_ID = Integer.MAX_VALUE - 123;

    @RequiresApi(api = Build.VERSION_CODES.O)
    static void scheduleJob(Context context, String userId, ArrayList<String> events) {
        Bundle extras = new Bundle();
        extras.putString(EXTRA_USER_ID, userId);
        ComponentName jobComponentName = new ComponentName(context.getPackageName(), SwrveEventSenderJobService.class.getName());

        JobScheduler mJobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo existingInfo = mJobScheduler.getPendingJob(JOB_ID);
        if (existingInfo != null) {
            mJobScheduler.cancel(JOB_ID);
            ArrayList<String> existingEvents = existingInfo.getTransientExtras().getStringArrayList(EXTRA_EVENTS);
            if (existingEvents != null) {
                events.addAll(existingEvents);
            }
        }
        extras.putStringArrayList(EXTRA_EVENTS, events);

        JobInfo.Builder jobBuilder = new JobInfo.Builder(JOB_ID, jobComponentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setTransientExtras(extras);
        int result = mJobScheduler.schedule(jobBuilder.build());
        if (result != JobScheduler.RESULT_SUCCESS) {
            SwrveLogger.e("SwrveEventSenderJobService could not start event job, error code %i:", result);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        sendEventsTask.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (sendEventsTask != null) {
            sendEventsTask.cancel(true);
        }
        return false;
    }

    private AsyncTask<JobParameters, Void, Void> sendEventsTask = new AsyncTask<JobParameters, Void, Void>() {
        private JobParameters params;

        @Override
        protected Void doInBackground(JobParameters... params) {
            this.params = params[0];

            final Swrve swrve = (Swrve) SwrveSDKBase.getInstance();
            Bundle extras = this.params.getTransientExtras();

            try {
                SwrveBackgroundEventSender sender = new SwrveBackgroundEventSender(swrve, getApplicationContext());
                sender.handleSendEvents(extras);
            } catch (Exception ex) {
                SwrveLogger.e("SwrveEventSenderJobService exception (extras: %s): ", ex, extras);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            jobFinished(params, false);
        }
    };
}
