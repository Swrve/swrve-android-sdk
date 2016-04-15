package com.swrve.swrvesdkunityandroidbridge;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.swrve.sdk.SwrveSDK;

import java.net.URISyntaxException;

public class ConfigContentFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frag_config, null);
    }

    @Override
    public void onResume() {
        super.onResume();

        try {
            String eventsUrl = SwrveSDK.getConfig().getEventsUrl().toURI().toString();
            TextView textViewEventsUrl = (TextView) getView().findViewById(R.id.config_eventsUrl);
            textViewEventsUrl.setText(eventsUrl);
        } catch (URISyntaxException ex) {
            Log.e("Demo", "Error getting events url", ex);
        }

        try {
            String contentUrl = SwrveSDK.getConfig().getContentUrl().toURI().toString();
            TextView textViewContentUrl = (TextView) getView().findViewById(R.id.config_contentUrl);
            textViewContentUrl.setText(contentUrl);
        } catch (URISyntaxException ex) {
            Log.e("Demo", "Error getting content url", ex);
        }

        String apikey = SwrveSDK.getApiKey();
        TextView textViewApikey = (TextView) getView().findViewById(R.id.config_apikey);
        textViewApikey.setText(apikey);

        String userId = SwrveSDK.getUserId();
        TextView textViewUserId = (TextView) getView().findViewById(R.id.config_userid);
        textViewUserId.setText(userId);
    }
}
