package com.swrve.sdk.converser.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

/**
 * (will eventually) display a conversation content, input, and choices as well as handling
 *
 * @author Jason Connery
 */
public class ConversationActivity extends FragmentActivity {

    public static final String EXTRA_CONVERSATION_REF = "io.converser.conversationRef";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().hasExtra(EXTRA_CONVERSATION_REF)) {

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ConversationFragment frag = ConversationFragment.create(getIntent().getStringExtra(EXTRA_CONVERSATION_REF));

            ft.replace(android.R.id.content, frag, "conversation");
            ft.commit();

        }
    }

}