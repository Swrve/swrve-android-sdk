package com.swrve.sdk.sample.minimalintegration.firebase;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.swrve.sdk.SwrveSDK;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = findViewById(R.id.btnRequestNotificationPermission);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            button.setVisibility(View.GONE);
        }
    }

    public void requestNotificationPermission(View view) {
        SwrveSDK.event("notification_permission_request");
    }
}
