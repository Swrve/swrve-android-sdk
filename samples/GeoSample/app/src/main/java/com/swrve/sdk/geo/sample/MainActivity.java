package com.swrve.sdk.geo.sample;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.swrve.sdk.geo.SwrveGeoSDK;

public class MainActivity extends AppCompatActivity {

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
        } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            btnStart.setVisibility(View.VISIBLE);
            btnStop.setVisibility(View.GONE);
            infoTextView.setText("Stopped");
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
}
