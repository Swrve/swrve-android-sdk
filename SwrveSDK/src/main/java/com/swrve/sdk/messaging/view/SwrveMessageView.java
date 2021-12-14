package com.swrve.sdk.messaging.view;

import android.app.Activity;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Configuration;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import com.swrve.sdk.QaUser;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveImageScaler;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveTextTemplating;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveImage;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;
import com.swrve.sdk.messaging.ui.SwrveInAppMessageActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Android view representing a Swrve message with a given format.
 * It layouts its children around its center and supports show and dismiss animations.
 */
public class SwrveMessageView extends RelativeLayout {
    // Activity that contains this view
    private final SwrveInAppMessageActivity activity;

    // Message format chosen to display message
    protected final SwrveMessageFormat format;

    // Scale to fit in the device
    protected float scale;

    // Minimum sample size to use when loading images
    protected int minSampleSize = 1;

    // In App Config
    protected SwrveInAppMessageConfig inAppConfig;

    protected Map<String, String> inAppPersonalization;

    public SwrveMessageView(SwrveInAppMessageActivity activity, SwrveMessage message,
                            SwrveMessageFormat format, int minSampleSize, SwrveInAppMessageConfig inAppConfig, Map<String, String> inAppPersonalization) throws SwrveMessageViewBuildException {
        super(activity);
        this.activity = activity;
        this.format = format;
        // Sample size has to be a power of two or 1
        if (minSampleSize > 0 && (minSampleSize % 2) == 0) {
            this.minSampleSize = minSampleSize;
        }

        this.inAppPersonalization = inAppPersonalization;
        this.inAppConfig = inAppConfig;
        initializeLayout(activity, message, format);
    }

    // Personalization is guarded by SwrveMessageTextTemplatingChecks
    protected void initializeLayout(final Context context, final SwrveMessage message, final SwrveMessageFormat format) throws SwrveMessageViewBuildException {
        List<String> loadErrorReasons = new ArrayList<>();
        try {

            // Get device screen metrics
            int screenWidth = SwrveHelper.getDisplayWidth(context);
            int screenHeight = SwrveHelper.getDisplayHeight(context);

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
            for (final SwrveImage image : format.getImages()) {

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
                    if (SwrveHelper.isNullOrEmpty(imageText)) {
                        imageView = new SwrveImageView(context);
                        imageView.setImageBitmap(backgroundImage.getBitmap());
                    } else {
                        // Need to render dynamic text
                        String personalizedText = SwrveTextTemplating.apply(imageText, this.inAppPersonalization);
                        // Add a default dismiss action although it won't be clickable as we set no setOnClickListener
                        imageView = new SwrveTextImageView(context, SwrveActionType.Dismiss, inAppConfig, personalizedText,
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

                    // Add to parent
                    addView(imageView);
                } else if (image.isMultiLine()) {

                    String imageText = image.getText();
                    if (SwrveHelper.isNullOrEmpty(imageText)) {
                        loadErrorReasons.add("Multi line text did not have any text present.");
                        break;
                    }

                    // Still need to personalize text
                    String text = SwrveTextTemplating.apply(imageText, this.inAppPersonalization);
                    text = text.replaceAll("\\n", "\n");
                    SwrveTextViewStyle textViewStyle = new SwrveTextViewStyle.Builder()
                            .fontSize(image.getFontSize())
                            .isScrollable(image.isScrollable())
                            .horizontalAlignment(image.getHorizontalAlignment())
                            /** using inappconfig for the rest until image includes it in V2 */
                            .textBackgroundColor(inAppConfig.getPersonalizedTextBackgroundColor())
                            .textForegroundColor(inAppConfig.getPersonalizedTextForegroundColor())
                            .textTypeFace(inAppConfig.getPersonalizedTextTypeface())
                            .build();

                    SwrveTextView textView = new SwrveTextView(context, text, textViewStyle, format.getCalibration());

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

            for (final SwrveButton button : format.getButtons()) {
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
                    if ((button.getActionType() == SwrveActionType.Custom || button.getActionType() == SwrveActionType.CopyToClipboard) && !SwrveHelper.isNullOrEmpty(personalizedButtonAction)) {
                        personalizedButtonAction = SwrveTextTemplating.apply(personalizedButtonAction, this.inAppPersonalization);
                    }

                    String buttonText = button.getText();
                    SwrveBaseInteractableView _buttonView;
                    if (SwrveHelper.isNullOrEmpty(buttonText)) {
                        _buttonView = new SwrveButtonView(context, button.getActionType(), inAppConfig.getFocusColor(), inAppConfig.getClickColor(), personalizedButtonAction);
                        _buttonView.setImageBitmap(backgroundImage.getBitmap());
                    } else {
                        // Need to render dynamic text
                        String personalizedText = SwrveTextTemplating.apply(buttonText, this.inAppPersonalization);
                        _buttonView = new SwrveTextImageView(context, button.getActionType(), inAppConfig, personalizedText,
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

                    buttonView.setOnClickListener(buttonView1 -> {
                        try {
                            dismiss();

                            if (button.getActionType() == SwrveActionType.Install) {
                                activity.notifyOfInstallButtonPress(button);
                            } else if (button.getActionType() == SwrveActionType.Custom) {
                                activity.notifyOfCustomButtonPress(button, buttonView.getAction());
                            } else if (button.getActionType() == SwrveActionType.CopyToClipboard) {
                                activity.notifyOfClipboardButtonPress(button, buttonView.getAction());
                            } else if (button.getActionType() == SwrveActionType.Dismiss) {
                                activity.notifyOfDismissButtonPress(button);
                            }
                        } catch (Exception e) {
                            SwrveLogger.e("Error in onClick handler.", e);
                        }
                    });
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
            SwrveLogger.e("Cannot resolve personalized asset: ", e);
            QaUser.assetFailedToDisplay(message.getCampaign().getId(), message.getId(), null, url, null, hasFallback, "Could not resolve url personalization");
        } catch (Exception e) {
            SwrveLogger.e("Cannot resolve personalized asset: ", e);
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    public SwrveMessageFormat getFormat() {
        return format;
    }
}
