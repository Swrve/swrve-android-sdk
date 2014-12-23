package io.converser.android.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

public class FeedbackActivity extends FragmentActivity {

    public static final String EXTRA_FEEDBACK_AREAS = "io.converser.areas";

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        String[] areas = getIntent().getStringArrayExtra(EXTRA_FEEDBACK_AREAS);
        FeedbackFragment frag = FeedbackFragment.create(areas);
        ft.replace(android.R.id.content, frag);

        ft.commit();

    }


}
