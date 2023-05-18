package com.swrve.sdk.messaging;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.VisibleForTesting;

import com.swrve.sdk.QaUser;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveImageScaler;
import com.swrve.sdk.SwrveInAppMessageActivity;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveTextTemplating;
import com.swrve.sdk.config.SwrveConfigBase;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;

import java.io.File;
import java.util.ArrayList;
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
    private List<String> loadErrorReasons = new ArrayList<>();

    public SwrveMessageView(Context context, SwrveConfigBase config, SwrveMessage message, SwrveMessageFormat format, Map<String, String> inAppPersonalization, long pageId)
            throws SwrveMessageViewBuildException {
        super(context);
        this.message = message;
        this.format = format;
        this.inAppPersonalization = inAppPersonalization;
        if (format.getPages() != null && !format.getPages().containsKey(pageId)) {
            dismiss();
            return;
        }
        this.page = format.getPages().get(pageId);

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

    // Personalization is guarded by SwrveMessageTextTemplatingChecks
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

        List<SwrveImage> images = page.getImages();
        for (final SwrveImage image : images) {
            if (image.isMultiLine()) {
                addMultilineView(image);
            } else {
                addImageView(image, screenWidth, screenHeight);
            }
            if (loadErrorReasons.size() > 0) {
                break;
            }
        }

        if (loadErrorReasons.size() > 0) {
            return;
        }

        List<SwrveButton> buttons = page.getButtons();
        for (final SwrveButton button : buttons) {
            if (button.getTheme() != null) {
                addThemedButton(button);
            } else {
                addImageViewButton(button, screenWidth, screenHeight);
            }
            if (loadErrorReasons.size() > 0) {
                break;
            }
        }
    }

    private void addMultilineView(SwrveImage image) throws SwrveSDKTextTemplatingException {
        String imageText = image.getText();
        if (SwrveHelper.isNullOrEmpty(imageText)) {
            loadErrorReasons.add("Multi line text did not have any text present.");
            return;
        }

        // Still need to personalize text
        String personalizedText = SwrveTextTemplating.apply(imageText, this.inAppPersonalization);
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
            String personalizedAccessibilityText = SwrveTextTemplating.apply(image.getAccessibilityText(), this.inAppPersonalization);
            textView.setContentDescription(personalizedAccessibilityText);
        } else if (SwrveHelper.isNotNullOrEmpty(personalizedText)) {
            textView.setContentDescription(personalizedText);
        }

        // Position
        RelativeLayout.LayoutParams lparams = new RelativeLayout.LayoutParams(image.getSize().x, image.getSize().y);
        lparams.leftMargin = image.getPosition().x;
        lparams.topMargin = image.getPosition().y;
        textView.setLayoutParams(lparams);

        addView(textView);
    }

    private void addImageView(SwrveImage image, int screenWidth, int screenHeight) throws SwrveSDKTextTemplatingException {

        SwrveImageFileInfo imageFileInfo = getImageFileInfo(image, image.getFile(), screenWidth, screenHeight);
        if (imageFileInfo == null) {
            return;
        }

        final ImageView imageView;
        String imageText = image.getText();
        if (SwrveHelper.isNullOrEmpty(imageText)) {
            imageView = new SwrveImageView(getContext(), image, inAppPersonalization, imageFileInfo);
        } else {
            imageView = new SwrveTextImageView(getContext(), image, inAppPersonalization, inAppConfig, imageFileInfo.image.getWidth(), imageFileInfo.image.getHeight());
        }
        // Position and size
        RelativeLayout.LayoutParams lparams = getLayoutParams(image, imageFileInfo);
        imageView.setLayoutParams(lparams);

        // Add to parent
        addView(imageView);
    }

    private void addImageViewButton(SwrveButton button, int screenWidth, int screenHeight) throws SwrveSDKTextTemplatingException {

        SwrveImageFileInfo imageFileInfo = getImageFileInfo(button, button.getImage(), screenWidth, screenHeight);
        if (imageFileInfo == null) {
            return;
        }

        final View buttonView;
        final String buttonAction;
        if (SwrveHelper.isNullOrEmpty(button.getText())) {
            SwrveButtonView swrveButtonView = new SwrveButtonView(getContext(), button, inAppPersonalization, inAppConfig.getMessageFocusListener(), inAppConfig.getClickColor(), imageFileInfo);
            buttonView = swrveButtonView;
            buttonAction = swrveButtonView.getAction();
        } else {
            SwrveButtonTextImageView swrveButtonTextImageView = new SwrveButtonTextImageView(getContext(), button, inAppPersonalization, inAppConfig, imageFileInfo.image.getWidth(), imageFileInfo.image.getHeight());
            buttonView = swrveButtonTextImageView;
            buttonAction = swrveButtonTextImageView.getAction();
        }

        // Position and size
        RelativeLayout.LayoutParams lparams = getLayoutParams(button, imageFileInfo);
        buttonView.setLayoutParams(lparams);

        buttonView.setOnClickListener(v -> {
            SwrveInAppMessageActivity inAppMessageActivity = (SwrveInAppMessageActivity) getContext();
            inAppMessageActivity.buttonClicked(button, buttonAction, page.getPageId(), getPage().getPageName());
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
                button, inAppPersonalization, inAppConfig.getMessageFocusListener(), format.getCalibration(), message.getCacheDir().getAbsolutePath());
        // Position
        RelativeLayout.LayoutParams lparams = new RelativeLayout.LayoutParams(button.getSize().x, button.getSize().y);
        lparams.leftMargin = button.getPosition().x;
        lparams.topMargin = button.getPosition().y;
        lparams.width = button.getSize().x;
        lparams.height = button.getSize().y;
        buttonView.setLayoutParams(lparams);

        buttonView.setOnClickListener(v -> {
            SwrveInAppMessageActivity inAppMessageActivity = (SwrveInAppMessageActivity) getContext();
            inAppMessageActivity.buttonClicked(button, buttonView.getAction(), page.getPageId(), getPage().getPageName());
        });

        addView(buttonView);
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
            String personalizedUrl = SwrveTextTemplating.apply(url, this.inAppPersonalization);
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
}
