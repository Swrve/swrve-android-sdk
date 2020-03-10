package com.swrve.sdk.sample;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.swrve.sdk.SwrveSDK;

import java.util.UUID;

// Main activity of your app, where the user will log in etc
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If SDK is tracking move to home activity
        if (SwrveSDK.isStarted()) {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
            finish();
        } else {
            setContentView(R.layout.activity_main);
        }
    }

    public void startSwrveClick(View view) {
        // Simulate generating a custom user id
        UUID uuid = UUID.randomUUID();
        String customUserId = uuid.toString();

        // On success of your user id generation:
        SwrveSDK.start(this, customUserId);

        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }
}
