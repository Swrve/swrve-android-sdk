package com.swrve.sdk.messaging;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.VisibleForTesting;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;

import com.swrve.sdk.QaUser;
import com.swrve.sdk.R;
import com.swrve.sdk.SwrveFreemarkerEvaluator;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveImageScaler;
import com.swrve.sdk.SwrveInAppMessageActivity;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveTextTemplating;
import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Android view representing a Swrve message with a given format.
 * It layouts its children around its center and supports show and dismiss animations.
 */
public class SwrveMessageView extends RelativeLayout {

    private final SwrveMessage message;
    private final SwrveMessageFormat format;
    private SwrveMessagePage page;
    private float scale;
    private int minSampleSize = 1; // Minimum sample size to use when loading images
    private SwrveInAppMessageConfig inAppConfig;
    private Map<String, String> inAppPersonalization;
    private boolean freemarkerEnabled;
    private boolean useLocalTimezone;
    private List<String> loadErrorReasons = new ArrayList<>();

    private WeakReference<GestureDetector> gestureDetector;
    
    // Store reference to video player for pause/play
    private SwrveVideoPlayerView videoPlayerView;

    public SwrveMessageView(Context context, SwrveConfigBase config, SwrveMessage message, SwrveMessageFormat format, Map<String, String> inAppPersonalization, long pageId)
            throws SwrveMessageViewBuildException {
        this(context, config, message, format, inAppPersonalization, pageId, null);
    }

    public SwrveMessageView(Context context, SwrveConfigBase config, SwrveMessage message, SwrveMessageFormat format, Map<String, String> inAppPersonalization, long pageId, GestureDetector gestureDetector)
            throws SwrveMessageViewBuildException {
        super(context);
        this.message = message;
        this.format = format;
        this.inAppPersonalization = inAppPersonalization;
        this.freemarkerEnabled = message.getCampaign().isFreemarkerEnabled();
        this.useLocalTimezone = message.getCampaign().useLocalTimezone();
        if (format.getPages() != null && !format.getPages().containsKey(pageId)) {
            dismiss();
            return;
        }
        this.page = format.getPages().get(pageId);
        if (gestureDetector != null) {
            this.gestureDetector = new WeakReference<>(gestureDetector);
        }

        // Sample size has to be a power of two or 1
        if (config.getMinSampleSize() > 0 && (config.getMinSampleSize() % 2) == 0) {
            this.minSampleSize = config.getMinSampleSize();
        }

        this.inAppConfig = config.getInAppMessageConfig();

        loadErrorReasons = new ArrayList<>();
        try {
            initializeLayout();
        } catch (Exception e) {
            SwrveLogger.e("Error while initializing SwrveMessageView layout", e);
            loadErrorReasons.add("Error while initializing SwrveMessageView layout:" + e.getMessage());
            // dismiss view as it may not be completely displayed.
            dismiss();
        } catch (OutOfMemoryError e) {
            SwrveLogger.e("OutOfMemoryError while initializing SwrveMessageView layout", e);
            loadErrorReasons.add("OutOfMemoryError while initializing SwrveMessageView layout:" + e.getMessage());
            // dismiss view as it may not be completely displayed.
            dismiss();
        }

        if (loadErrorReasons.size() > 0) {
            Map<String, String> errorReasonPayload = new HashMap<>();
            errorReasonPayload.put("reason", loadErrorReasons.toString());
            // dismiss what did successfully load as there was an error displaying the overall view
            dismiss();
            throw new SwrveMessageViewBuildException("There was an error creating the view caused by:\n" + loadErrorReasons.toString());
        }
    }

    // Personalization and visible_if expressions are pre-validated by SwrveMessageTextTemplatingChecks before display.
    protected void initializeLayout() throws SwrveSDKTextTemplatingException {

        // Get device screen metrics
        int screenWidth = SwrveHelper.getDisplayWidth(getContext());
        int screenHeight = SwrveHelper.getDisplayHeight(getContext());

        // Set background
        Integer backgroundColor = format.getBackgroundColor();
        if (backgroundColor == null) {
            backgroundColor = inAppConfig.getDefaultBackgroundColor();
        }
        setBackgroundColor(backgroundColor);

        // Construct layout
        scale = format.getScale();
        setMinimumWidth(format.getSize().x);
        setMinimumHeight(format.getSize().y);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        if (gestureDetector != null && gestureDetector.get() != null) {
            // Add gesture interceptor to bottom of the view stack by adding it first before buttons and images
            addGestureInterceptor(screenWidth, screenHeight);
        }

        List<SwrveWidget> pageElements = getPageElements();
        for (final SwrveWidget widget : pageElements) {
            if (!shouldRenderElement(widget)) {
                continue;
            }
            if (widget instanceof SwrveButton) {
                SwrveButton button = (SwrveButton) widget;
                if (button.getTheme() != null) {
                    addThemedButton(button);
                } else {
                    addImageViewButton(button, screenWidth, screenHeight);
                }
            } else if (widget instanceof SwrveImage) {
                SwrveImage image = (SwrveImage) widget;
                if (image.getSwrveVideoSettings() != null) {
                    addVideoView(image);
                } else if (image.isMultiLine()) {
                    addMultilineView(image);
                } else {
                    addImageView(image, screenWidth, screenHeight);
                }
            }
            if (loadErrorReasons.size() > 0) {
                break;
            }
        }
    }

    private boolean shouldRenderElement(SwrveWidget widget) throws SwrveSDKTextTemplatingException {
        String visibleIf = widget.getVisibleIf();
        if (visibleIf.isEmpty()) {
            return true;
        }
        try {
            String template = "<#if " + visibleIf + ">true<#else>false</#if>";
            String result = SwrveFreemarkerEvaluator.evaluate(template, inAppPersonalization != null ? inAppPersonalization : Collections.emptyMap(), this.useLocalTimezone);
            return "true".equals(result);
        } catch (Throwable e) {
            throw new SwrveSDKTextTemplatingException("visible_if evaluation failed for '" + visibleIf + "': " + e.getMessage(), e);
        }
    }

    private List<SwrveWidget> getPageElements() {
        // Combine page elements together (images and buttons)
        // Images must be added before buttons to preserve the order of the elements for backwards compatibility
        // If iam_z_index is set then it will be sorted by the iam_z_index
        List<SwrveWidget> pageElements = new ArrayList<>();
        pageElements.addAll(page.getImages());
        pageElements.addAll(page.getButtons());

        Collections.sort(pageElements, new Comparator<SwrveWidget>() {
            @Override
            public int compare(SwrveWidget widget1, SwrveWidget widget2) {
                return Integer.compare(widget1.getIamZIndex(), widget2.getIamZIndex());
            }
        });
        return pageElements;
    }

    private void addGestureInterceptor(int screenWidth, int screenHeight) {
        View interceptorOverlay = new View(getContext());
        interceptorOverlay.setClickable(true);
        interceptorOverlay.setFocusable(false);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(screenWidth, screenHeight);
        interceptorOverlay.setLayoutParams(params);
        interceptorOverlay.setOnTouchListener(getGestureOnTouchListener());
        addView(interceptorOverlay);
    }

    @SuppressLint("ClickableViewAccessibility")
    private View.OnTouchListener getGestureOnTouchListener() {
        return (view, motionEvent) -> {
            if (gestureDetector != null && gestureDetector.get() != null) {
                return gestureDetector.get().onTouchEvent(motionEvent);
            }
            return false;
        };
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addMultilineView(SwrveImage image) throws SwrveSDKTextTemplatingException {
        String imageText = image.getText();
        if (SwrveHelper.isNullOrEmpty(imageText)) {
            loadErrorReasons.add("Multi line text did not have any text present.");
            return;
        }

        // Still need to personalize text
        String personalizedText = SwrveTextTemplating.apply(imageText, this.inAppPersonalization, this.freemarkerEnabled, this.useLocalTimezone);
        personalizedText = personalizedText.replaceAll("\\n", "\n");

        String fontNativeStyle = image.getFontNativeStyle() == null ? null : image.getFontNativeStyle().toString();
        Typeface typeface = SwrveTextUtils.getTypeface(image.getFontFile(), fontNativeStyle);
        if (typeface == null) {
            typeface = inAppConfig.getPersonalizedTextTypeface();
        }
        int bgColor = image.getBackgroundColor(inAppConfig.getPersonalizedTextBackgroundColor());
        int fgColor = image.getForegroundColor(inAppConfig.getPersonalizedTextForegroundColor());

        SwrveTextViewStyle textViewStyle = new SwrveTextViewStyle.Builder()
                .fontSize(image.getFontSize())
                .isScrollable(image.isScrollable())
                .horizontalAlignment(image.getHorizontalAlignment())
                .textBackgroundColor(bgColor)
                .textForegroundColor(fgColor)
                .textTypeFace(typeface)
                .bottomPadding(image.getBottomPadding())
                .topPadding(image.getTopPadding())
                .leftPadding(image.getLeftPadding())
                .rightPadding(image.getRightPadding())
                .lineHeight(image.getLineHeight())
                .build();

        SwrveTextView textView = new SwrveTextView(getContext(), personalizedText, textViewStyle, format.getCalibration());

        if (SwrveHelper.isNotNullOrEmpty(image.getAccessibilityText())) {
            String personalizedAccessibilityText = SwrveTextTemplating.apply(image.getAccessibilityText(), this.inAppPersonalization, this.freemarkerEnabled, this.useLocalTimezone);
            textView.setContentDescription(personalizedAccessibilityText);
        } else if (SwrveHelper.isNotNullOrEmpty(personalizedText)) {
            textView.setContentDescription(personalizedText);
        }

        // Position
        RelativeLayout.LayoutParams lparams = new RelativeLayout.LayoutParams(image.getSize().x, image.getSize().y);
        lparams.leftMargin = image.getPosition().x;
        lparams.topMargin = image.getPosition().y;
        textView.setLayoutParams(lparams);

        if (gestureDetector != null && gestureDetector.get() != null) {
            textView.setOnTouchListener(getGestureOnTouchListener());
        }

        addView(textView);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addImageView(SwrveImage image, int screenWidth, int screenHeight) throws SwrveSDKTextTemplatingException {

        SwrveImageFileInfo imageFileInfo = getImageFileInfo(image, image.getFile(), screenWidth, screenHeight);
        if (imageFileInfo == null) {
            return;
        }

        final ImageView imageView;
        String imageText = image.getText();
        if (SwrveHelper.isNullOrEmpty(imageText)) {
            imageView = new SwrveImageView(getContext(), image, inAppPersonalization, imageFileInfo, freemarkerEnabled, useLocalTimezone);
        } else {
            imageView = new SwrveTextImageView(getContext(), image, inAppPersonalization, inAppConfig, imageFileInfo.image.getWidth(), imageFileInfo.image.getHeight(), freemarkerEnabled, useLocalTimezone);
        }
        // Position and size
        RelativeLayout.LayoutParams lparams = getLayoutParams(image, imageFileInfo);
        imageView.setLayoutParams(lparams);

        if (gestureDetector != null && gestureDetector.get() != null) {
            imageView.setOnTouchListener(getGestureOnTouchListener());
        }

        // Add to parent
        addView(imageView);
    }

    private void addImageViewButton(SwrveButton button, int screenWidth, int screenHeight) throws SwrveSDKTextTemplatingException {

        SwrveImageFileInfo imageFileInfo = getImageFileInfo(button, button.getImage(), screenWidth, screenHeight);
        if (imageFileInfo == null) {
            return;
        }

        final View buttonView;
        final String resolvedButtonAction;
        final String resolvedButtonText;
        if (SwrveHelper.isNullOrEmpty(button.getText())) {
            SwrveButtonView swrveButtonView = new SwrveButtonView(getContext(), button, inAppPersonalization, inAppConfig.getMessageFocusListener(), inAppConfig.getClickColor(), imageFileInfo, freemarkerEnabled, useLocalTimezone);
            buttonView = swrveButtonView;
            resolvedButtonAction = swrveButtonView.getAction();
            resolvedButtonText = button.getText();
        } else {
            SwrveButtonTextImageView swrveButtonTextImageView = new SwrveButtonTextImageView(getContext(), button, inAppPersonalization, inAppConfig, imageFileInfo.image.getWidth(), imageFileInfo.image.getHeight(), freemarkerEnabled, useLocalTimezone);
            buttonView = swrveButtonTextImageView;
            resolvedButtonAction = swrveButtonTextImageView.getAction();
            resolvedButtonText = swrveButtonTextImageView.getText();
        }

        // Position and size
        RelativeLayout.LayoutParams lparams = getLayoutParams(button, imageFileInfo);
        buttonView.setLayoutParams(lparams);

        buttonView.setOnClickListener(v -> {
            SwrveInAppMessageActivity inAppMessageActivity = (SwrveInAppMessageActivity) getContext();
            inAppMessageActivity.buttonClicked(button, resolvedButtonAction, resolvedButtonText, page.getPageId(), getPage().getPageName());
        });

        // Add to parent
        addView(buttonView);
        UiModeManager uiModeManager = (UiModeManager) getContext().getSystemService(Context.UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            buttonView.requestFocus();
        }
    }

    private void addThemedButton(SwrveButton button) throws SwrveSDKTextTemplatingException {
        SwrveThemedMaterialButton buttonView = new SwrveThemedMaterialButton(getContext(), com.google.android.material.R.attr.materialButtonOutlinedStyle,
                button, inAppPersonalization, inAppConfig.getMessageFocusListener(), format.getCalibration(), message.getCacheDir().getAbsolutePath(), freemarkerEnabled, useLocalTimezone);
        // Position
        RelativeLayout.LayoutParams lparams = new RelativeLayout.LayoutParams(button.getSize().x, button.getSize().y);
        lparams.leftMargin = button.getPosition().x;
        lparams.topMargin = button.getPosition().y;
        lparams.width = button.getSize().x;
        lparams.height = button.getSize().y;
        buttonView.setLayoutParams(lparams);

        buttonView.setOnClickListener(v -> {
            SwrveInAppMessageActivity inAppMessageActivity = (SwrveInAppMessageActivity) getContext();
            inAppMessageActivity.buttonClicked(button, buttonView.getAction(), buttonView.getText().toString(), page.getPageId(), getPage().getPageName());
        });

        addView(buttonView);
    }

    private void addVideoView(SwrveImage video) throws SwrveSDKTextTemplatingException {
        String resolvedUrl = SwrveTextTemplating.apply(video.getDynamicImageUrl(), this.inAppPersonalization, this.freemarkerEnabled, this.useLocalTimezone);
        String asset = SwrveHelper.sha1(resolvedUrl.getBytes());
        //Only support .mp4 video files
        String filePath = message.getCacheDir().getAbsolutePath() + "/" + asset + ".mp4";
        Uri videoUri = Uri.fromFile(new File(filePath));

        SwrveVideoSettings swrveVideoSettings = video.getSwrveVideoSettings();

        // if fillscreen, it zooms video, we will use texture view otherwise use surface view
        // texture view will prevents video from bleeding through the edges of the screen to next page.
        // see ref https://github.com/androidx/media/issues/1107
        int layoutId = swrveVideoSettings.getFillScreen() ? R.layout.swrve_video_player_zoom : R.layout.swrve_video_player_fit;
        videoPlayerView = (SwrveVideoPlayerView) LayoutInflater.from(getContext())
                .inflate(layoutId, this, false);
        videoPlayerView.setupPlayer(swrveVideoSettings, videoUri);


        if (SwrveHelper.isNotNullOrEmpty(video.getAccessibilityText())) {
            String personalizedAccessibilityText = SwrveTextTemplating.apply(video.getAccessibilityText(), this.inAppPersonalization, this.freemarkerEnabled, this.useLocalTimezone);
            videoPlayerView.setContentDescription(personalizedAccessibilityText);
        }

        if (swrveVideoSettings.getFillScreen()) {
            this.addOnLayoutChangeListener(new OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {

                    // Parent’s measured, edge-to-edge size
                    int w = right - left;
                    int h = bottom - top;

                    RelativeLayout.LayoutParams lp =
                            (RelativeLayout.LayoutParams) videoPlayerView.getLayoutParams();
                    lp.width = w;
                    lp.height = h;
                    lp.leftMargin = 0;
                    lp.topMargin = 0;
                    videoPlayerView.setLayoutParams(lp);

                    // Only need to do this once
                    SwrveMessageView.this.removeOnLayoutChangeListener(this);
                }
            });
        } else {
            int screenWidth = video.getSize().x;
            int screenHeight = video.getSize().y;
            RelativeLayout.LayoutParams lparams = new RelativeLayout.LayoutParams(screenWidth, screenHeight);
            lparams.leftMargin = video.getPosition().x;
            lparams.topMargin = video.getPosition().y;
            videoPlayerView.setLayoutParams(lparams);
        }

        videoPlayerView.setPlayerListener(new Player.Listener() {
            boolean hasStarted = false;

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying && !hasStarted) {
                    hasStarted = true;
                    SwrveInAppMessageActivity inAppMessageActivity = (SwrveInAppMessageActivity) getContext();
                    inAppMessageActivity.sendVideoEvent(page.getPageId(), video.getMediaId(), "video_started");
                }
            }

            @Override
            public void onPlaybackStateChanged(int state) {
                //if looping is enabled, we wont get to ended state, so we track ended in onPositionDiscontinuity
                if (state == Player.STATE_ENDED) {
                    SwrveInAppMessageActivity inAppMessageActivity = (SwrveInAppMessageActivity) getContext();
                    inAppMessageActivity.sendVideoEvent(page.getPageId(), video.getMediaId(), "video_ended");
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                SwrveLogger.e("ExoPlayer error: ", error);
            }

            @Override
            public void onPositionDiscontinuity(
                    Player.PositionInfo oldPosition,
                    Player.PositionInfo newPosition,
                    @Player.DiscontinuityReason int reason
            ) {
                // When looping, ExoPlayer jumps from end → start and triggers AUTO_TRANSITION
                if (swrveVideoSettings.getAutoPlay() && reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    SwrveInAppMessageActivity inAppMessageActivity = (SwrveInAppMessageActivity) getContext();
                    inAppMessageActivity.sendVideoEvent(page.getPageId(), video.getMediaId(), "video_ended");
                }
            }
        });

        addView(videoPlayerView);
    }

    private void dismiss() {
        Context ctx = getContext();
        if (ctx instanceof Activity) {
            ((Activity) ctx).finish();
        }
    }

    private SwrveImageFileInfo getImageFileInfo(SwrveWidget swrveWidget, String fileName, int screenWidth, int screenHeight) {
        // set fallback first
        String filePath = message.getCacheDir().getAbsolutePath() + "/" + fileName;
        boolean usingDynamic = false;
        // check if we can change filepath to personalized
        if (SwrveHelper.isNotNullOrEmpty(swrveWidget.getDynamicImageUrl())) {
            String candidateAsset = resolveUrlPersonalization(swrveWidget.getDynamicImageUrl(), message, SwrveHelper.isNotNullOrEmpty(fileName));
            if (SwrveHelper.isNotNullOrEmpty(candidateAsset)) {
                filePath = candidateAsset;
                usingDynamic = true;
            }
        }

        boolean isGif = isGif(filePath);
        if (isGif) {
            filePath = filePath + ".gif";
        }

        if (!SwrveHelper.hasFileAccess(filePath)) {
            SwrveLogger.e("Do not have read access to message asset for:%s", filePath);
            loadErrorReasons.add("Do not have read access to message asset for:" + filePath);
            return null;
        }

        // Load image
        final SwrveImageScaler.BitmapResult image = SwrveImageScaler.decodeSampledBitmapFromFile(filePath, screenWidth, screenHeight, minSampleSize);
        if (image == null || image.getBitmap() == null) {
            loadErrorReasons.add("Could not decode bitmap from file:" + filePath);
            return null;
        }

        return new SwrveImageFileInfo(filePath, usingDynamic, isGif, image);
    }

    private String resolveUrlPersonalization(String url, SwrveMessage message, boolean hasFallback) {

        if (SwrveHelper.isNullOrEmpty(url)) {
            SwrveLogger.i("cannot resolve url personalization");
            return null;
        }

        try {
            String personalizedUrl = SwrveTextTemplating.apply(url, this.inAppPersonalization, this.freemarkerEnabled, this.useLocalTimezone);
            if (SwrveHelper.isNotNullOrEmpty(personalizedUrl)) {
                // then there might be an asset saved, get the sha1 and check
                String candidateAsset = SwrveHelper.sha1(personalizedUrl.getBytes());
                String candidateFilePath = message.getCacheDir().getAbsolutePath() + "/" + candidateAsset;
                if (SwrveHelper.hasFileAccess(candidateFilePath)) {
                    return candidateFilePath;
                } else {
                    SwrveLogger.i("Personalized asset not found in cache: " + candidateAsset);
                    // Log the failed retrieval
                    QaUser.assetFailedToDisplay(message.getCampaign().getId(), message.getId(), candidateAsset, url, personalizedUrl, hasFallback, "Asset not found in cache");
                }
            }
        } catch (SwrveSDKTextTemplatingException e) {
            SwrveLogger.w("Cannot resolve personalized asset: %s", e.getMessage());
            QaUser.assetFailedToDisplay(message.getCampaign().getId(), message.getId(), null, url, null, hasFallback, "Could not resolve url personalization");
        } catch (Exception e) {
            SwrveLogger.w("Cannot resolve personalized asset: %s", e.getMessage());
        }

        return null;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        try {
            int count = getChildCount();
            int centerx = (int) (l + (r - l) / 2.0);
            int centery = (int) (t + (b - t) / 2.0);

            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() != GONE) {
                    RelativeLayout.LayoutParams st = (RelativeLayout.LayoutParams) child.getLayoutParams();
                    int cCenterX = st.width / 2;
                    int cCenterY = st.height / 2;

                    if (scale != 1f) {
                        child.layout((int) (scale * (st.leftMargin - cCenterX)) + centerx, (int) (scale * (st.topMargin - cCenterY)) + centery, (int) (scale * (st.leftMargin + cCenterX)) + centerx, (int) (scale * (st.topMargin + cCenterY)) + centery);
                    } else {
                        child.layout(st.leftMargin - cCenterX + centerx, st.topMargin - cCenterY + centery, st.leftMargin + cCenterX + centerx, st.topMargin + cCenterY + centery);
                    }
                }
            }
        } catch (Exception e) {
            SwrveLogger.e("Error while onLayout in SwrveMessageView", e);
        }
    }

    private RelativeLayout.LayoutParams getLayoutParams(SwrveWidget swrveWidget, SwrveImageFileInfo imageFileInfo) {
        RelativeLayout.LayoutParams lparams = new RelativeLayout.LayoutParams(imageFileInfo.image.getWidth(), imageFileInfo.image.getHeight());
        lparams.leftMargin = swrveWidget.getPosition().x;
        lparams.topMargin = swrveWidget.getPosition().y;
        if (imageFileInfo.usingDynamic) {
            lparams.width = swrveWidget.getSize().x;
            lparams.height = swrveWidget.getSize().y;
        } else {
            lparams.width = imageFileInfo.image.getWidth();
            lparams.height = imageFileInfo.image.getHeight();
        }
        return lparams;
    }

    private boolean isGif(String filePath) {
        File file = new File(filePath + ".gif");
        return file.canRead();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @VisibleForTesting
    public SwrveMessageFormat getFormat() {
        return format;
    }

    @VisibleForTesting
    public SwrveMessagePage getPage() {
        return page;
    }

    protected void autoPlayVideo() {
        if (videoPlayerView != null && videoPlayerView.getVideoPlayerSettings().getAutoPlay()) {
            videoPlayerView.play();
        }
    }

    protected void stopVideo() {
        if (videoPlayerView != null) {
            videoPlayerView.stop();
        }
    }

    protected void releaseVideo() {
        if (videoPlayerView != null) {
            videoPlayerView.release();
        }
    }
}
