package com.swrve.sdk.demo;

import android.os.Bundle;
import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.swrve.sdk.SwrveSDK;

import java.util.ArrayList;
import java.util.List;

/*
 * Example of the Swrve SDK integrated in an Activity.
 *
 * It is VERY important to call the onCreate, onResume, onPause, onDestroy, onNewIntent, onLowMemory
 * methods for the SDK to work properly, at least in the Activities that are going to display
 * Swrve in-app messages, conversations or will be launched from a push notification
 */
public class MainActivity extends AppCompatActivity {

    private View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SwrveSDK.onCreate(this);

        root = getLayoutInflater().inflate(R.layout.activity_main, null);
        setContentView(root);
        // Adding Toolbar to Main screen
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Setting ViewPager for each Tabs
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
        setupViewPager(viewPager);
        // Set Tabs inside Toolbar
        TabLayout tabs = (TabLayout) findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
    }

    // Add Fragments to Tabs
    private void setupViewPager(ViewPager viewPager) {
        Adapter adapter = new Adapter(getSupportFragmentManager());
        adapter.addFragment(new ConfigContentFragment(), "Config");
        adapter.addFragment(new MessageCenterFragment(), "Message Center");
        viewPager.setAdapter(adapter);
    }

    static class Adapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public Adapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SwrveSDK.onResume(this);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        SwrveSDK.onLowMemory();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SwrveSDK.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        SwrveSDK.onNewIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SwrveSDK.onDestroy(this);
    }
}
