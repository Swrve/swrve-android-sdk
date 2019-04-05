package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.swrve.sdk.SwrveBaseConversation;
import com.swrve.sdk.SwrveConversationEventHelper;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.conversations.engine.model.UserInputResult;

import java.util.HashMap;

public class ConversationActivity extends FragmentActivity {
    private static final String EXTRA_CONVERSATION_KEY = "conversation";
    private static final String EXTRA_ORIENTATION_KEY = "orientation";

    private SwrveBaseConversation localConversation;
    private ConversationFragment conversationFragment;

    public static boolean showConversation(Context context, SwrveBaseConversation conversation, SwrveOrientation orientation) {
        if (context == null) {
            SwrveLogger.e("Can't display ConversationActivity without a context.");
            return false;
        } else if (hasUnknownContentAtoms(conversation)) {
            SwrveLogger.e("This sdk cannot display Conversations with Unknown Atoms. Conversation.id:%s", conversation.getId());
            new SwrveConversationEventHelper().conversationEncounteredError(conversation, "UNKNOWN_ATOM", null);
            return false;
        }

        Intent intent = new Intent(context, ConversationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_CONVERSATION_KEY, conversation);
        intent.putExtra(EXTRA_ORIENTATION_KEY, orientation);
        context.startActivity(intent);

        return true;
    }

    protected static boolean hasUnknownContentAtoms(SwrveBaseConversation conversation) {
        boolean hasUnknownContentAtoms = false;
        for (ConversationPage conversationPage : conversation.getPages()) {
            for (ConversationAtom conversationAtom : conversationPage.getContent()) {
                switch (conversationAtom.getType()) {
                    case UNKNOWN:
                        hasUnknownContentAtoms = true;
                        break;
                }
            }
            if (hasUnknownContentAtoms) {
                break;
            }
        }
        return hasUnknownContentAtoms;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        SwrveOrientation orientation = SwrveOrientation.Both;
        if (intent != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                this.localConversation = (SwrveBaseConversation) extras.getSerializable(EXTRA_CONVERSATION_KEY);
                orientation = (SwrveOrientation) extras.getSerializable(EXTRA_ORIENTATION_KEY);
            }
        }

        try {
            if (localConversation != null) {
                conversationFragment = ConversationFragment.create(localConversation);
                conversationFragment.commitConversationFragment(getSupportFragmentManager());
            } else {
                SwrveLogger.e("Could not render ConversationActivity. No SwrveConversation was detected");
                this.finish();
            }
        } catch (Exception ge) {
            SwrveLogger.e("Could not render ConversationActivity.", ge);
            this.finish();
        }

        // Used by Unity too - Force the orientation based on the app configuration
        if (orientation != SwrveOrientation.Both) {
            try {
                if(Build.VERSION.SDK_INT == Build.VERSION_CODES.O && SwrveHelper.getTargetSdkVersion(this) >= 27) {
                    // Cannot call setRequestedOrientation with translucent attribute, otherwise "IllegalStateException: Only fullscreen activities can request orientation"
                    // https://github.com/Swrve/swrve-android-sdk/issues/271
                    // workaround is to not change orientation
                    SwrveLogger.w("Oreo bug with setRequestedOrientation so Conversation may appear in wrong orientation.");
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    if (orientation == SwrveOrientation.Landscape) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
                    } else if (orientation == SwrveOrientation.Portrait) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
                    }
                } else {
                    if (orientation == SwrveOrientation.Landscape) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    } else if (orientation == SwrveOrientation.Portrait) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                    }
                }
            } catch(RuntimeException ex) {
                SwrveLogger.e("Bugs with setRequestedOrientation can happen: https://issuetracker.google.com/issues/68454482", ex);
            }
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
            SwrveLogger.e("Could not call the ConversationFragments onBackPressed()", ne);
        }
        if (allowBackPress) {
            super.onBackPressed();
        }
    }

    public ConversationFragment getConversationFragment() {
        return conversationFragment;
    }
}
