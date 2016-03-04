package com.swrve.sdk.conversations.ui;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.swrve.sdk.conversations.ISwrveConversationSDKProvider;
import com.swrve.sdk.conversations.R;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.conversations.SwrveCommonConversation;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.conversations.engine.model.UserInputResult;

import java.util.HashMap;

public class ConversationActivity extends FragmentActivity {
    private static final String LOG_TAG = "SwrveSDK";
    private SwrveCommonConversation localConversation;
    private ConversationFragment conversationFragment;
    private static ISwrveConversationSDKProvider sdkProvider;

    public static void setSDKProvider(ISwrveConversationSDKProvider sdkProvider) {
        ConversationActivity.sdkProvider = sdkProvider;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                this.localConversation = (SwrveCommonConversation) extras.getSerializable("conversation");
            }
        }

        try {
            if (localConversation != null && sdkProvider != null) {
                conversationFragment = ConversationFragment.create(localConversation, sdkProvider.getInstance());
                conversationFragment.commitConversationFragment(getSupportFragmentManager());
                setOrientation();
            } else {
                SwrveLogger.e(LOG_TAG, "Could not render ConversationActivity. No SwrveConversation was detected");
                this.finish();
            }
        } catch (Exception ge) {
            SwrveLogger.e(LOG_TAG, "Could not render ConversationActivity.", ge);
            this.finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("page", conversationFragment.getPage());
        outState.putSerializable("userdata", conversationFragment.getUserInteractionData());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);
        if (savedState != null && localConversation != null && sdkProvider != null) {
            conversationFragment = ConversationFragment.create(localConversation, sdkProvider.getInstance());
            ConversationPage page = (ConversationPage) savedState.getSerializable("page");
            HashMap<String, UserInputResult> userData = (HashMap<String, UserInputResult>) savedState.getSerializable("userdata");

            if (page != null){
                conversationFragment.setPage(page);
            }
            if (userData != null){
                conversationFragment.setUserInteractionData(userData);
            }
            conversationFragment.commitConversationFragment(getSupportFragmentManager());
        }
    }

    @Override
    public void onBackPressed() {
        boolean allowBackPress = true;
        try {
            allowBackPress = conversationFragment.onBackPressed();
        } catch (NullPointerException ne) {
            SwrveLogger.e(LOG_TAG, "Could not call the ConversationFragments onBackPressed()", ne);
        }
        if (allowBackPress) {
            super.onBackPressed();
        }
    }

    private void setOrientation() {
        boolean isShortestWidthLessThan600 = !getResources().getBoolean(R.bool.swrve__is_sw600);
        if(isShortestWidthLessThan600) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
    }
}