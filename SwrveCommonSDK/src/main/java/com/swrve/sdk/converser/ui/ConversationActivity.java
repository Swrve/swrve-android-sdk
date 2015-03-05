package com.swrve.sdk.converser.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;

import com.swrve.sdk.converser.SwrveConversation;
import com.swrve.sdk.converser.engine.model.ConversationPage;
import com.swrve.sdk.converser.engine.model.ConverserInputResult;

import java.util.ArrayList;
import java.util.HashMap;

public class ConversationActivity extends FragmentActivity {
    private static final String LOG_TAG = "ConversationActivity";
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
                commitConversationFragment();
            } else {
                Log.e("ConversationActivity", "Could not render ConversationActivity. No SwrveConversation was detected");
                this.finish();
            }
        } catch (Exception ge) {
            Log.e("ConversationActivity", "Could not render ConversationActivity.", ge);
            this.finish();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("page", conversationFragment.getPage());
        outState.putSerializable("userdata", conversationFragment.getUserInteractionData());
        outState.putSerializable("inputs", conversationFragment.getInputs());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        super.onRestoreInstanceState(savedState);
        if (savedState != null && localConversation !=null) {
            conversationFragment = ConversationFragment.create(localConversation);
            ConversationPage page = (ConversationPage) savedState.getSerializable("page");
            HashMap<String, ConverserInputResult> userData = (HashMap<String, ConverserInputResult>) savedState.getSerializable("userdata");
            ArrayList<ConverserInput> inputs = (ArrayList<ConverserInput>) savedState.getSerializable("inputs");

            if (page != null){
                conversationFragment.setPage(page);
            }
            if (userData != null){
                conversationFragment.setUserInteractionData(userData);
            }
            if (inputs != null){
                conversationFragment.setInputs(inputs);
            }
            commitConversationFragment();
        }
    }

    protected void commitConversationFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(android.R.id.content, conversationFragment, "conversation");
        ft.commit();
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