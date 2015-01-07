package com.swrve.sdk.converser.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.swrve.sdk.converser.SwrveConversation;

/**
 * (will eventually) display a conversation content, input, and choices as well as handling
 *
 * @author Jason Connery
 */
public class ConversationActivity extends FragmentActivity {
    /* TODO: STM : This is a complete hack to get things running. It will work but is by now means a good practice.
    The way ConversationActivities work is they accept a bundled conversation blob or reference and then use that to display the conversation.
    User interaction is recorded via a ConversationEngine or API. This will not work 100% with the SwrveConversations since they have multiple objects,
    event listeners and other objects and threaded factors which are not friendly to serialization.

     The alternative is to update the ConversationActivities and Ui particles to work inside a SwrveMessageView or alternative of it. This is a large unit of
     work however and may not be the best way to do things.

     As an intermediate solution, before the ConversationActivity is called, a static globalConversation variable is set.
     Since the serialization is a tricky beast, this is how we pass data to the Activity. This variable is then copied to a local variable which is treated the
     same as the aforementioned "bundled conversation".

     This is an intermediate fix until we can decide the best COA on how to pass a SwrveConversation into the ConversationActivity or alter the Activity's method of rendering information
     */

    public static SwrveConversation globalConversation;
    private SwrveConversation localConversation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.localConversation = globalConversation;

        try {
            if (localConversation != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ConversationFragment frag = ConversationFragment.create(localConversation);
                ft.replace(android.R.id.content, frag, "conversation");
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

}