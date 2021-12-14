package com.swrve.sdk.geo.sample;

import static com.swrve.sdk.geo.sample.SampleApplication.FOREGROUND_NOTIFICATION_ID;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.google.android.material.snackbar.Snackbar;
import com.swrve.sdk.geo.SwrveGeoSDK;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

    private TextView infoTextView;
    private Button btnStart;
    private Button btnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        infoTextView = findViewById(R.id.textviewInfo);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateView();
    }

    private void updateView() {
        boolean started = SwrveGeoSDK.isStarted(this);
        if (started) {
            btnStart.setVisibility(View.GONE);
            btnStop.setVisibility(View.VISIBLE);
            infoTextView.setText("Started");
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Button btnUpdateForegroundNotification = findViewById(R.id.btnUpdateForegroundNotification);
                btnUpdateForegroundNotification.setVisibility(View.VISIBLE);
            }
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btnStart.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.GONE);
            infoTextView.setText("Stopped");
            Button btnUpdateForegroundNotification = findViewById(R.id.btnUpdateForegroundNotification);
            btnUpdateForegroundNotification.setVisibility(View.INVISIBLE);
        }
    }

    public void btnStart(View v) {
        SwrveGeoSDK.start(this);
        updateView();
    }

    public void btnStop(View v) {
        SwrveGeoSDK.stop(this);
        updateView();
    }

    public void btnPrivacyPolicy(View v) {
        Snackbar.make(findViewById(android.R.id.content), "Show your privacy policy screen somewhere in your app.", Snackbar.LENGTH_SHORT).show();
    }

    public void btnForegroundNotification(View v) {
        String updatedContent = "This content was updated. See code. Time:" + simpleDateFormat.format(new Date());
        NotificationCompat.Builder builder = SampleApplication.getGeoForegroundNotification(this);
        builder.setContentText(updatedContent);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(updatedContent));

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(FOREGROUND_NOTIFICATION_ID, builder.build());

        Snackbar.make(findViewById(android.R.id.content), "Foreground notification content updated.", Snackbar.LENGTH_SHORT).show();
    }
}
