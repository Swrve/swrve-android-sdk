package com.swrve.sdk.conversations.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.swrve.sdk.conversations.SwrveConversation;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.conversations.engine.model.UserInputResult;

import java.util.HashMap;

public class ConversationActivity extends FragmentActivity {
    private static final String LOG_TAG = "SwrveSDK";
    private SwrveConversation localConversation;
    private ConversationFragment conversationFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                this.localConversation = (SwrveConversation) extras.getSerializable("conversation");
            }
        }

        try {
            if (localConversation != null) {
                conversationFragment = ConversationFragment.create(localConversation);
                conversationFragment.commitConversationFragment(getSupportFragmentManager());
            } else {
                Log.e(LOG_TAG, "Could not render ConversationActivity. No SwrveConversation was detected");
                this.finish();
            }
        } catch (Exception ge) {
            Log.e(LOG_TAG, "Could not render ConversationActivity.", ge);
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
        if (savedState != null && localConversation !=null) {
            conversationFragment = ConversationFragment.create(localConversation);
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
        try {
            conversationFragment.onBackPressed();
        } catch (NullPointerException ne) {
            Log.e(LOG_TAG, "Could not call the ConversationFragments onBackPressed()", ne);
        }
        super.onBackPressed();
    }
}