package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.swrve.sdk.SwrveBaseConversation;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.conversations.engine.model.UserInputResult;

import java.util.HashMap;

public class ConversationActivity extends FragmentActivity {
    private static final String EXTRA_CONVERSATION_KEY = "conversation";

    private static final String LOG_TAG = "SwrveSDK";
    private SwrveBaseConversation localConversation;
    private ConversationFragment conversationFragment;

    public static void showConversation(Context context, SwrveBaseConversation conversation) {

        if (context == null) {
            SwrveLogger.e(LOG_TAG, "Can't display ConversationActivity without a context.");
            return;
        }

        Intent intent = new Intent(context, ConversationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CONVERSATION_KEY, conversation);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                this.localConversation = (SwrveBaseConversation) extras.getSerializable(EXTRA_CONVERSATION_KEY);
            }
        }

        try {
            if (localConversation != null) {
                conversationFragment = ConversationFragment.create(localConversation);
                conversationFragment.commitConversationFragment(getSupportFragmentManager());
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
        if (savedState != null && localConversation != null) {
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

    public ConversationFragment getConversationFragment() {
        return conversationFragment;
    }
}