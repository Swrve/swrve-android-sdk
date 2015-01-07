package io.converser.android.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import io.converser.android.engine.model.ConversationItem;

public class ConversationListActivity extends FragmentActivity implements ConversationListFragment.OnConversationClickedListener {

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        ConversationListFragment frag = ConversationListFragment.create();
        ft.replace(android.R.id.content, frag);

        ft.commit();
    }

    @Override
    public void onConversationItemClicked(ConversationItem item) {

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        ConversationFragment frag = ConversationFragment.create(item.getConversationTrackerId());
        ft.addToBackStack("CONVDETAIL");
        ft.replace(android.R.id.content, frag, "CONVDETAIL");

        ft.commit();

    }


}
