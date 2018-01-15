package com.swrve.sdk.sample;

import android.annotation.SuppressLint;
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

import com.swrve.sdk.SwrveLogger;

@RequiresApi(api = Build.VERSION_CODES.O)
public class GcmJobService extends JobService {

    private static final int JOB_ID = Integer.MAX_VALUE - 1234;

    @RequiresApi(api = Build.VERSION_CODES.O)
    static void scheduleJob(Context context, Bundle extras) {

        ComponentName jobComponentName = new ComponentName(context.getPackageName(), GcmJobService.class.getName());
        JobScheduler mJobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo existingInfo = mJobScheduler.getPendingJob(JOB_ID);
        if (existingInfo != null) {
            mJobScheduler.cancel(JOB_ID);
        }

        JobInfo.Builder jobBuilder = new JobInfo.Builder(JOB_ID, jobComponentName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setTransientExtras(extras);
        int result = mJobScheduler.schedule(jobBuilder.build());
        if (result != JobScheduler.RESULT_SUCCESS) {
            SwrveLogger.e("GcmJobService could not start job, error code %i:", result);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        asyncTask.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (asyncTask != null) {
            asyncTask.cancel(true);
        }
        return false;
    }

    @SuppressLint("StaticFieldLeak")
    private AsyncTask<JobParameters, Void, Void> asyncTask = new AsyncTask<JobParameters, Void, Void>() {
        private JobParameters params;

        @Override
        protected Void doInBackground(JobParameters... params) {
            this.params = params[0];

            Bundle extras = this.params.getTransientExtras();
            try {
                new GcmSwrvePushService().processNotification(extras);
            } catch (Exception ex) {
                SwrveLogger.e("GcmSwrvePushService exception (extras: %s): ", ex, extras);
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
