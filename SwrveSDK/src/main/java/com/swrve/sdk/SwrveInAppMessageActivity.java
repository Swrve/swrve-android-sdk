package com.swrve.sdk;

import static com.swrve.sdk.messaging.SwrveActionType.PageLink;

import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveInAppMessageFragment;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveMessagePage;
import com.swrve.sdk.messaging.SwrveOrientation;

import java.util.LinkedList;
import java.util.Map;

public class SwrveInAppMessageActivity extends FragmentActivity {

    public static final String MESSAGE_ID_KEY = "message_id";
    public static final String SWRVE_PERSONALISATION_KEY = "message_personalization";
    public static final String SWRVE_AD_MESSAGE = "ad_message_key";

    protected InAppMessageHandler inAppMessageHandler;
    private SwrveBase sdk;
    protected ViewPager2 viewPager2;
    protected ScreenSlidePagerAdapter adapter;
    protected boolean isSwipeable;
    protected long currentPageIdNonSwipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inAppMessageHandler = new InAppMessageHandler(this, getIntent(), savedInstanceState);
        if (inAppMessageHandler.message == null) {
            finish();
            return;
        }

        this.sdk = (SwrveBase) SwrveSDK.getInstance();
        if (sdk == null) {
            finish();
            return;
        }

        setRequestedOrientation();

        SwrveConfigBase config = sdk.getConfig();
        // Add the status bar if configured that way
        if (!config.getInAppMessageConfig().isHideToolbar()) {
            setTheme(R.style.Theme_InAppMessageWithToolbar);
        }

        setContentView(R.layout.swrve_frag_iam);

        try {
            pageSetup();
        } catch (Exception e) {
            SwrveLogger.e("Exception setting up IAM page(s) ", e);
            finish();
        }

        if (savedInstanceState == null) {
            inAppMessageHandler.notifyOfImpression(inAppMessageHandler.format);
        }

        if (config.getInAppMessageConfig().getWindowListener() != null) {
            config.getInAppMessageConfig().getWindowListener().onCreate(getWindow()); // This must not be called before setContentView
        }
    }

    // If swipeable and multiple pages, use a viewpager, hide fragment frame
    // If not swipeable or is single page, hide viewpager and use fragment frame to show page(s)
    private void pageSetup() {

        long firstPageId = inAppMessageHandler.format.getFirstPageId();
        Map<Long, SwrveMessagePage> pages = inAppMessageHandler.format.getPages();
        SwrveMessagePage page = pages.get(firstPageId);
        LinkedList<Long> trunk = new LinkedList<>();
        if (pages.size() == 1 || page.getSwipeForward() == -1) {
            SwrveLogger.v("SwrveInAppMessageActivity: non swipe page flow");
            this.isSwipeable = false;
        } else {
            SwrveLogger.v("SwrveInAppMessageActivity: swipeable multi page flow. Traversing tree to get trunk and check for circular flows");
            this.isSwipeable = true;
            while (true) {
                trunk.add(page.getPageId());
                if (page.getSwipeForward() == -1) {
                    break;
                } else if (trunk.contains(page.getSwipeForward())) {
                    throw new IllegalArgumentException("SwrveInAppMessageActivity: Circular loops not supported in swipeable flow.");
                } else {
                    page = pages.get(page.getSwipeForward());
                }
            }
        }

        if (isSwipeable) {
            View singlePageFragment = findViewById(R.id.swrve_iam_frag_container);
            singlePageFragment.setVisibility(View.GONE);
            View multiPageFragment = findViewById(R.id.swrve_iam_pager);
            multiPageFragment.setVisibility(View.VISIBLE);

            viewPager2 = findViewById(R.id.swrve_iam_pager);
            viewPager2.setOffscreenPageLimit(pages.size()); // Page limit of 10 on the UI.
            adapter = new ScreenSlidePagerAdapter(this, trunk);
            viewPager2.setAdapter(adapter);
        } else {
            View singlePageFragment = findViewById(R.id.swrve_iam_frag_container);
            singlePageFragment.setVisibility(View.VISIBLE);
            View multiPageFragment = findViewById(R.id.swrve_iam_pager);
            multiPageFragment.setVisibility(View.GONE);
        }

        // The firstPageId is the first in the flow. The startingPageId could be different if the device has been rotated.
        long startingPageId = inAppMessageHandler.getStartingPageId();
        showPage(startingPageId);
    }

    private void setRequestedOrientation() {
        SwrveMessage message = inAppMessageHandler.message;
        SwrveMessageFormat format = inAppMessageHandler.format;
        if (message.getFormats().size() == 1) {
            try {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O && SwrveHelper.getTargetSdkVersion(this) >= 27) {
                    // Cannot call setRequestedOrientation with translucent attribute, otherwise "IllegalStateException: Only fullscreen activities can request orientation"
                    // https://github.com/Swrve/swrve-android-sdk/issues/271
                    // workaround is to not change orientation
                    SwrveLogger.w("SwrveInAppMessageActivity: Oreo bug with setRequestedOrientation so Message may appear in wrong orientation.");
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    if (format.getOrientation() == SwrveOrientation.Landscape) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
                    } else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
                    }
                } else {
                    if (format.getOrientation() == SwrveOrientation.Landscape) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                    } else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                    }
                }
            } catch (RuntimeException ex) {
                SwrveLogger.e("SwrveInAppMessageActivity: Bugs with setRequestedOrientation can happen: https://issuetracker.google.com/issues/68454482", ex);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        long currentPageId = isSwipeable ? adapter.trunk.get(viewPager2.getCurrentItem()) : currentPageIdNonSwipe;
        inAppMessageHandler.saveInstanceState(bundle, currentPageId);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (isSwipeable) {
            long currentPageId = adapter.trunk.get(viewPager2.getCurrentItem());
            inAppMessageHandler.backButtonClicked(currentPageId);
        } else {
            inAppMessageHandler.backButtonClicked(currentPageIdNonSwipe);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SwrveMessage message = inAppMessageHandler.message;
        if (message != null && message.getCampaign() != null) {
            message.getCampaign().messageDismissed();
        }
    }

    private void showPage(long pageId) {
        if (isSwipeable) {
            int index = adapter.trunk.indexOf(pageId);
            if (index == -1) {
                SwrveLogger.e("SwrveInAppMessageActivity: cannot show %s because it is not on the main swipeable trunk.", pageId);
                finish();
            } else {
                viewPager2.setCurrentItem(index, false); // false will remove animation and fragment will appear instantly.
            }
        } else {
            SwrveInAppMessageFragment fragment = SwrveInAppMessageFragment.newInstance(pageId);
            getSupportFragmentManager().beginTransaction().replace(R.id.swrve_iam_frag_container, fragment).commit();
            currentPageIdNonSwipe = pageId;
        }
    }

    public void sendPageViewEvent(long pageId) {
        inAppMessageHandler.sendPageViewEvent(pageId);
    }

    public void buttonClicked(SwrveButton button, String action, long pageId, String pageName) {
        try {
            inAppMessageHandler.buttonClicked(button, action, pageId, pageName);
            if (button.getActionType() == PageLink) {
                long pageToId = Long.parseLong(button.getAction());
                showPage(pageToId);
            } else {
                finish();
            }
        } catch (Exception e) {
            SwrveLogger.e("Error in IAM onClick button listener.", e);
        }
    }

    public SwrveMessage getSwrveMesage() {
        return inAppMessageHandler.message;
    }

    public SwrveMessageFormat getSwrveMessageFormat() {
        return inAppMessageHandler.format;
    }

    public Map<String, String> getInAppPersonalization() {
        return inAppMessageHandler.inAppPersonalization;
    }

    class ScreenSlidePagerAdapter extends FragmentStateAdapter {

        public final LinkedList<Long> trunk;

        ScreenSlidePagerAdapter(FragmentActivity fragmentActivity, LinkedList<Long> trunk) {
            super(fragmentActivity);
            this.trunk = trunk;
        }

        @Override
        public Fragment createFragment(int position) {
            long pageId = trunk.get(position);
            return SwrveInAppMessageFragment.newInstance(pageId);
        }

        @Override
        public int getItemCount() {
            return trunk.size();
        }
    }
}
