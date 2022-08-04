package com.swrve.sdk.messaging;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
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

        List<String> loadErrorReasons = new ArrayList<>();
        try {
            initializeLayout(loadErrorReasons);
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
    protected void initializeLayout(List<String> loadErrorReasons) throws SwrveSDKTextTemplatingException {

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

            // set fallback first
            String filePath = message.getCacheDir().getAbsolutePath() + "/" + image.getFile();
            boolean usingDynamic = false;
            // check if we can change filepath to personalized
            if (SwrveHelper.isNotNullOrEmpty(image.getDynamicImageUrl())) {
                String candidateAsset = resolveUrlPersonalization(image.getDynamicImageUrl(), message, SwrveHelper.isNotNullOrEmpty(image.getFile()));
                if (SwrveHelper.isNotNullOrEmpty(candidateAsset)) {
                    filePath = candidateAsset;
                    usingDynamic = true;
                }
            }

            boolean isGif = !image.isMultiLine() && isGif(filePath);
            if(isGif) {
                filePath = filePath + ".gif";
            }

            if (!SwrveHelper.hasFileAccess(filePath) && !image.isMultiLine()) {
                SwrveLogger.e("Do not have read access to message asset for:%s", filePath);
                loadErrorReasons.add("Do not have read access to message asset for:" + filePath);
                continue;
            }

            // Load background image
            final SwrveImageScaler.BitmapResult backgroundImage = SwrveImageScaler.decodeSampledBitmapFromFile(filePath, screenWidth, screenHeight, minSampleSize);
            if (backgroundImage != null && backgroundImage.getBitmap() != null && !image.isMultiLine()) {

                ImageView imageView;
                String imageText = image.getText();
                String personalizedText = null;
                if (SwrveHelper.isNullOrEmpty(imageText)) {
                    imageView = new SwrveImageView(getContext());
                    loadImage(imageView, filePath, backgroundImage.getBitmap(), isGif);
                } else {
                    // Need to render dynamic text
                    personalizedText = SwrveTextTemplating.apply(imageText, this.inAppPersonalization);
                    // Add a default dismiss action although it won't be clickable as we set no setOnClickListener
                    imageView = new SwrveTextImageView(getContext(), SwrveActionType.Dismiss, inAppConfig, personalizedText,
                            backgroundImage.getWidth(), backgroundImage.getHeight(), null);
                }
                // Position
                RelativeLayout.LayoutParams lparams = new RelativeLayout.LayoutParams(backgroundImage.getWidth(), backgroundImage.getHeight());
                lparams.leftMargin = image.getPosition().x;
                lparams.topMargin = image.getPosition().y;

                if (usingDynamic) {
                    lparams.width = image.getSize().x;
                    lparams.height = image.getSize().y;
                    imageView.setLayoutParams(lparams);
                    imageView.setScaleType(ScaleType.FIT_CENTER);
                    imageView.setAdjustViewBounds(true);
                } else {
                    lparams.width = backgroundImage.getWidth();
                    lparams.height = backgroundImage.getHeight();
                    imageView.setLayoutParams(lparams);
                    imageView.setScaleType(ScaleType.FIT_XY);
                }

                if (SwrveHelper.isNotNullOrEmpty(image.getAccessibilityText())) {
                    String personalizedAccessibilityText = SwrveTextTemplating.apply(image.getAccessibilityText(), this.inAppPersonalization);
                    imageView.setContentDescription(personalizedAccessibilityText);
                } else if (SwrveHelper.isNotNullOrEmpty(personalizedText)) {
                    imageView.setContentDescription(personalizedText);
                }

                // Add to parent
                addView(imageView);
            } else if (image.isMultiLine()) {

                String imageText = image.getText();
                if (SwrveHelper.isNullOrEmpty(imageText)) {
                    loadErrorReasons.add("Multi line text did not have any text present.");
                    break;
                }

                // Still need to personalize text
                String personalizedText = SwrveTextTemplating.apply(imageText, this.inAppPersonalization);
                personalizedText = personalizedText.replaceAll("\\n", "\n");

                Typeface typeface = image.getTypeface(inAppConfig.getPersonalizedTextTypeface());
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
            } else {
                loadErrorReasons.add("Could not decode bitmap from file:" + filePath);
                break;
            }
        }

        List<SwrveButton> buttons = page.getButtons();
        for (final SwrveButton button : buttons) {
            // set fallback first
            String filePath = message.getCacheDir().getAbsolutePath() + "/" + button.getImage();
            boolean usingDynamic = false;
            // check if we can change filepath to personalized
            if (SwrveHelper.isNotNullOrEmpty(button.getDynamicImageUrl())) {
                String candidateAsset = resolveUrlPersonalization(button.getDynamicImageUrl(), message, SwrveHelper.isNotNullOrEmpty(button.getImage()));
                if (SwrveHelper.isNotNullOrEmpty(candidateAsset)) {
                    filePath = candidateAsset;
                    usingDynamic = true;
                }
            }

            boolean isGif = isGif(filePath);
            if(isGif) {
                filePath = filePath + ".gif";
            }

            if (!SwrveHelper.hasFileAccess(filePath)) {
                SwrveLogger.e("Do not have read access to message asset for:%s", filePath);
                loadErrorReasons.add("Do not have read access to message asset for:" + filePath);
                continue;
            }

            // Load button image
            final SwrveImageScaler.BitmapResult backgroundImage = SwrveImageScaler.decodeSampledBitmapFromFile(filePath, screenWidth, screenHeight, minSampleSize);

            if (backgroundImage != null && backgroundImage.getBitmap() != null) {
                // Resolve templating in the button action
                String personalizedButtonAction = button.getAction();
                String personalizedText = null;
                if ((button.getActionType() == SwrveActionType.Custom || button.getActionType() == SwrveActionType.CopyToClipboard) && !SwrveHelper.isNullOrEmpty(personalizedButtonAction)) {
                    personalizedButtonAction = SwrveTextTemplating.apply(personalizedButtonAction, this.inAppPersonalization);
                }

                String buttonText = button.getText();
                SwrveBaseInteractableView _buttonView;
                if (SwrveHelper.isNullOrEmpty(buttonText)) {
                    _buttonView = new SwrveButtonView(getContext(), button.getActionType(), inAppConfig.getMessageFocusListener(), inAppConfig.getClickColor(), personalizedButtonAction);
                    loadImage(_buttonView, filePath, backgroundImage.getBitmap(), isGif);
                } else {
                    // Need to render dynamic text
                    personalizedText = SwrveTextTemplating.apply(buttonText, this.inAppPersonalization);
                    _buttonView = new SwrveTextImageView(getContext(), button.getActionType(), inAppConfig, personalizedText,
                            backgroundImage.getWidth(), backgroundImage.getHeight(), personalizedButtonAction);
                    _buttonView.setFocusable(true);
                }

                final SwrveBaseInteractableView buttonView = _buttonView;
                // Mark the buttonView tag with the name of the button as found on the swrve dashboard.
                // Used primarily for testing.
                buttonView.setTag(button.getName());
                // Position
                RelativeLayout.LayoutParams lparams = new RelativeLayout.LayoutParams(backgroundImage.getWidth(), backgroundImage.getHeight());
                lparams.leftMargin = button.getPosition().x;
                lparams.topMargin = button.getPosition().y;

                if (usingDynamic) {
                    lparams.width = button.getSize().x;
                    lparams.height = button.getSize().y;
                    buttonView.setLayoutParams(lparams);
                    buttonView.setScaleType(ScaleType.FIT_CENTER);
                    buttonView.setAdjustViewBounds(true);
                } else {
                    lparams.width = backgroundImage.getWidth();
                    lparams.height = backgroundImage.getHeight();
                    buttonView.setLayoutParams(lparams);
                    buttonView.setScaleType(ScaleType.FIT_XY);
                }

                buttonView.setOnClickListener(v -> {
                    SwrveInAppMessageActivity inAppMessageActivity = (SwrveInAppMessageActivity) getContext();
                    inAppMessageActivity.buttonClicked(button, buttonView.getAction(), page.getPageId(), getPage().getPageName());
                });

                if (SwrveHelper.isNotNullOrEmpty(button.getAccessibilityText())) {
                    String personalizedAccessibilityText = SwrveTextTemplating.apply(button.getAccessibilityText(), this.inAppPersonalization);
                    buttonView.setContentDescription(personalizedAccessibilityText);
                } else if (SwrveHelper.isNotNullOrEmpty(personalizedText)) {
                    buttonView.setContentDescription(personalizedText);
                }

                // Add to parent
                addView(buttonView);
                UiModeManager uiModeManager = (UiModeManager) getContext().getSystemService(Context.UI_MODE_SERVICE);
                if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
                    buttonView.requestFocus();
                }
            } else {
                loadErrorReasons.add("Could not decode bitmap from file:" + filePath);
                break;
            }
        }
    }

    private void dismiss() {
        Context ctx = getContext();
        if (ctx instanceof Activity) {
            ((Activity) ctx).finish();
        }
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

    private boolean isGif(String filePath) {
        File file = new File(filePath + ".gif");
        return file.canRead();
    }

    private void loadImage(ImageView imageView, String filePath, Bitmap bitmap, boolean isGif) {
        if (isGif) {
            loadGlideImage(imageView, filePath);
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }

    private void loadGlideImage(ImageView imageView, String filePath) {
        Glide.with(getContext())
                .asGif()
                .load(new File(filePath))
                .fitCenter()
                .listener(new RequestListener<GifDrawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<GifDrawable> target, boolean isFirstResource) {
                        SwrveLogger.e("SwrveSDK: Glide failed to load image.", e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                        SwrveLogger.v("SwrveSDK: Glide successfully loaded image");
                        return false;
                    }
                })
                .into(imageView);
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
