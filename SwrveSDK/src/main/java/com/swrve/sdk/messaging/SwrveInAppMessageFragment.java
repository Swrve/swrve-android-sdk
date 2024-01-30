package com.swrve.sdk.messaging;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.swrve.sdk.SwrveInAppMessageActivity;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveSDK;
import com.swrve.sdk.config.SwrveConfigBase;

import java.util.Map;

public class SwrveInAppMessageFragment extends Fragment {

    private static final String PAGE_ID = "PAGE_ID";

    private long pageId;

    private GestureDetector gestureDetector;

    public static SwrveInAppMessageFragment newInstance(long pageId) {
        Bundle args = new Bundle();
        args.putLong(PAGE_ID, pageId);
        SwrveInAppMessageFragment f = new SwrveInAppMessageFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        SwrveMessageView rootView = null;
        try {
            SwrveInAppMessageActivity inAppMessageActivity = (SwrveInAppMessageActivity) getActivity();
            SwrveConfigBase config = SwrveSDK.getInstance().getConfig();
            SwrveMessage message = inAppMessageActivity.getSwrveMesage();
            SwrveMessageFormat format = inAppMessageActivity.getSwrveMessageFormat();
            Map<String, String> inAppPersonalization = inAppMessageActivity.getInAppPersonalization();
            pageId = getArguments().getLong(PAGE_ID);

            rootView = new SwrveMessageView(getContext(), config, message, format, inAppPersonalization, pageId, gestureDetector);
        } catch (SwrveMessageViewBuildException e) {
            SwrveLogger.e("Error in SwrveInAppMessageFragment while creating the SwrveMessageView", e);
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((SwrveInAppMessageActivity) getActivity()).sendPageViewEvent(pageId);
    }

    public void addGestureDetection(GestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;
    }
}
