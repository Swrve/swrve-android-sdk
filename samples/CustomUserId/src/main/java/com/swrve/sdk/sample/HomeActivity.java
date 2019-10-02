package com.swrve.sdk.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.swrve.sdk.SwrveSDK;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_home);

        TextView txtView = findViewById(R.id.txtUserMessage);
        txtView.setText("UserId:" + SwrveSDK.getUserId());
    }
}
