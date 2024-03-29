package com.swrve.sdk;

import static com.swrve.sdk.messaging.SwrveActionType.PageLink;

import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveInAppMessageFragment;
import com.swrve.sdk.messaging.SwrveInAppStoryButton;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.SwrveMessagePage;
import com.swrve.sdk.messaging.SwrveOrientation;
import com.swrve.sdk.messaging.SwrveInAppStoryView;
import com.swrve.sdk.messaging.SwrveStoryDismissButton;
import com.swrve.sdk.messaging.SwrveStorySettings;

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
    private SwrveInAppStoryView storyView;
    private SwrveInAppStoryView.SwrveInAppStorySegmentListener storyViewListener;
    protected GestureDetector storyGestureDetector;

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

        addStoryView();

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

    @Override
    protected void onPause() {
        super.onPause();
        if (storyView != null) {
            storyView.pauseAnimation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (storyView != null) {
            storyView.resumeAnimation();
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
            if (storyGestureDetector != null) {
                fragment.addGestureDetection(storyGestureDetector);
            }
            getSupportFragmentManager().beginTransaction().replace(R.id.swrve_iam_frag_container, fragment).commit();
            currentPageIdNonSwipe = pageId;
            if (storyView != null) {
                storyView.startSegmentAtIndex(getSwrveMessageFormat().getIndexForPageId(currentPageIdNonSwipe));
            }
        }
    }

    public void sendPageViewEvent(long pageId) {
        inAppMessageHandler.sendPageViewEvent(pageId);
    }

    public void buttonClicked(SwrveButton button, String resolvedAction, String resolvedText, long pageId, String pageName) {
        try {
            inAppMessageHandler.buttonClicked(button, resolvedAction, resolvedText, pageId, pageName);
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

    private void addStoryView() {
        SwrveStorySettings storySettings = getSwrveMessageFormat().getStorySettings();
        if (storySettings == null) {
            return;
        }

        SwrveInAppStoryView.SwrveInAppStorySegmentListener segmentListener = getInAppStorySegmentListener();
        FrameLayout pagedMessageContainer = findViewById(R.id.swrve_iam_pager_container);

        storyView = new SwrveInAppStoryView(this, segmentListener, storySettings,
                inAppMessageHandler.format.getPages().size(), inAppMessageHandler.format.getPageDurations());

        pagedMessageContainer.addView(storyView);

        if (storySettings.isGesturesEnabled()) {
            InAppStoryGestureListener storyTapListener = new InAppStoryGestureListener(pagedMessageContainer);
            storyGestureDetector = new GestureDetector(this, storyTapListener);

            storyView.addOnLayoutChangeListener((view, left, top, right, bottom,
                                                 oldLeft, oldTop, oldRight, oldBottom) ->
                    storyTapListener.setTapAreaWidth((right - left) / 2));
        }

        if (storySettings.getDismissButton() != null) {
            Point dismissMargins = new Point(storySettings.getRightPadding(),
                    storySettings.getTopPadding() + storySettings.getBarHeight() + storySettings.getDismissButton().getMarginTop());

            SwrveInAppStoryButton dismissButton = new SwrveInAppStoryButton(this,
                    storySettings.getDismissButton(), dismissMargins,
                    sdk.getConfig().getInAppMessageConfig().getStoryDismissButton(),
                    sdk.getConfig().getInAppMessageConfig().getMessageFocusListener());

            pagedMessageContainer.addView(dismissButton);

            dismissButton.setOnClickListener((View view) -> {
                dismissStory();
            });
        }
    }

    private void dismissStory() {
        SwrveStoryDismissButton button = getSwrveMessageFormat().getStorySettings().getDismissButton();
        inAppMessageHandler.dismissMessage(currentPageIdNonSwipe, button.getButtonId(), button.getName());
        storyView.close();
        finish();
    }

    private SwrveInAppStoryView.SwrveInAppStorySegmentListener getInAppStorySegmentListener() {
        if (storyViewListener == null) {
            storyViewListener = new SwrveInAppStoryView.SwrveInAppStorySegmentListener() {
                @Override
                public void segmentFinished(int segmentIndex) {
                    runOnUiThread(() -> {
                        //Auto-progression uses order of pages as they are in the data array
                        handleStorySegmentChange(segmentIndex + 1);
                    });
                }
            };
        }
        return storyViewListener;
    }

    private void handleStorySegmentChange(int newSegmentIndex) {
        long nextPageId = getSwrveMessageFormat().getPageIdAtIndex(newSegmentIndex);
        if (nextPageId == 0) {
            handleLastPageProgression(getSwrveMessageFormat().getStorySettings());
        } else {
            showPage(nextPageId);
        }
    }

    private void handleLastPageProgression(SwrveStorySettings storySettings) {
        if (storySettings.getLastPageProgression() == SwrveStorySettings.LastPageProgression.DISMISS) {
            SwrveLogger.d("Last page progression is dismiss, so dismissing");
            inAppMessageHandler.dismissMessage(currentPageIdNonSwipe,
                    storySettings.getLastPageDismissId(),
                    storySettings.getLastPageDismissName());
            finish();
        } else if (storySettings.getLastPageProgression() == SwrveStorySettings.LastPageProgression.LOOP) {
            SwrveLogger.d("Last page progression is loop, so restarting the story");
            showPage(getSwrveMessageFormat().getFirstPageId());
        } else if (storySettings.getLastPageProgression() == SwrveStorySettings.LastPageProgression.STOP) {
            SwrveLogger.d("Last page progression is stop, so remain on last page");
        }
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

    private class InAppStoryGestureListener extends GestureDetector.SimpleOnGestureListener {
        private int tapAreaWidth;
        private View view;

        public InAppStoryGestureListener(View view) {
            this.view = view;
        }

        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
            if (storyViewListener != null) {
                if (e.getX() > (view.getWidth() - tapAreaWidth)) {
                    if (storyView.getCurrentIndex() < storyView.getNumberOfSegments() - 1) {
                        SwrveInAppMessageActivity.this.handleStorySegmentChange(storyView.getCurrentIndex() + 1);
                    }
                    return true;
                } else if (e.getX() < tapAreaWidth) {
                    if (storyView.getCurrentIndex() > 0) {
                        SwrveInAppMessageActivity.this.handleStorySegmentChange(storyView.getCurrentIndex() - 1);
                    }
                    return true;
                }
            }

            return super.onSingleTapConfirmed(e);
        }

        public void setTapAreaWidth(int width) {
            tapAreaWidth = width;
        }
    }
}
