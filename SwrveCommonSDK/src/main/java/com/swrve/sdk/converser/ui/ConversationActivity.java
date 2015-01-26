package com.swrve.sdk.converser.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.swrve.sdk.converser.SwrveConversation;

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
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                conversationFragment = ConversationFragment.create(localConversation);
                ft.replace(android.R.id.content, conversationFragment, "conversation");
                ft.commit();
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
    public void onBackPressed() {
        try {
            conversationFragment.onBackPressed();
        } catch (NullPointerException ne) {
            Log.e(LOG_TAG, "Could not call the ConversationFragments onBackPressed()", ne);
        }
        super.onBackPressed();
    }
}