package com.swrve.sdk.messaging.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.messaging.ISwrveCustomButtonListener;
import com.swrve.sdk.messaging.ISwrveInstallButtonListener;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveButton;
import com.swrve.sdk.messaging.SwrveImage;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Android view representing a Swrve message with a given format.
 * It layouts its children around its center and supports show and dismiss animations.
 */
public class SwrveInnerMessageView extends RelativeLayout {
    protected static final String LOG_TAG = "SwrveMessagingSDK";

    // Message format chosen to display message
    protected SwrveMessageFormat format;
    // Install button listener
    protected ISwrveInstallButtonListener installButtonListener;
    // Custom button listener
    protected ISwrveCustomButtonListener customButtonListener;
    // Parent view
    protected SwrveMessageView parent;

    // Scale to fit in the device
    protected float scale;

    // Minimum sample size to use when loading images
    protected int minSampleSize = 1;

    // Bitmap cache
    protected Set<WeakReference<Bitmap>> bitmapCache;

    public SwrveInnerMessageView(Context context, SwrveMessageView parent, SwrveMessage message, SwrveMessageFormat format, ISwrveInstallButtonListener installButtonListener,
                                 ISwrveCustomButtonListener customButtonListener, int minSampleSize) throws SwrveMessageViewBuildException {
        super(context);
        this.parent = parent;
        this.format = format;
        this.installButtonListener = installButtonListener;
        this.customButtonListener = customButtonListener;
        // Sample size has to be a power of two or 1
        if (minSampleSize > 0 && (minSampleSize % 2) == 0) {
            this.minSampleSize = minSampleSize;
        }
        initializeLayout(context, message, format);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and
            // keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private static BitmapResult decodeSampledBitmapFromFile(String filePath, int reqWidth, int reqHeight, int minSampleSize) {
        try {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(filePath, options);

            int bitmapWidth = options.outWidth;
            int bitmapHeight = options.outHeight;

            // Calculate inSampleSize
            options.inSampleSize = Math.max(calculateInSampleSize(options, reqWidth,
                    reqHeight), minSampleSize);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            return new BitmapResult(BitmapFactory.decodeFile(filePath, options), bitmapWidth, bitmapHeight);
        } catch (OutOfMemoryError exp) {
            SwrveLogger.e(LOG_TAG, Log.getStackTraceString(exp));
        } catch (Exception exp) {
            SwrveLogger.e(LOG_TAG, Log.getStackTraceString(exp));
        }

        return null;
    }

    protected void initializeLayout(final Context context, final SwrveMessage message, final SwrveMessageFormat format) throws SwrveMessageViewBuildException {
        List<String> loadErrorReasons = new ArrayList<String>();
        try {
            // Create bitmap cache
            bitmapCache = new HashSet<WeakReference<Bitmap>>();

            // Get device screen metrics
            Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            int screenWidth = display.getWidth();
            int screenHeight = display.getHeight();

            // Construct layout
            scale = format.getScale();
            setMinimumWidth(format.getSize().x);
            setMinimumHeight(format.getSize().y);
            setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            for (final SwrveImage image : format.getImages()) {
                String filePath = message.getCacheDir().getAbsolutePath() + "/" + image.getFile();
                if(!SwrveHelper.hasFileAccess(filePath)) {
                    SwrveLogger.e(LOG_TAG, "Do not have read access to message asset for:" + filePath);
                    loadErrorReasons.add("Do not have read access to message asset for:" + filePath);
                    continue;
                }

                // Load image
                final BitmapResult backgroundImage = decodeSampledBitmapFromFile(filePath, screenWidth, screenHeight, minSampleSize);
                if (backgroundImage != null && backgroundImage.getBitmap() != null) {
                    Bitmap imageBitmap = backgroundImage.getBitmap();
                    SwrveImageView imageView = createSwrveImage(context);
                    bitmapCache.add(new WeakReference<Bitmap>(imageBitmap));
                    // Position
                    RelativeLayout.LayoutParams lparams = new RelativeLayout.LayoutParams(backgroundImage.getWidth(), backgroundImage.getHeight());
                    lparams.leftMargin = image.getPosition().x;
                    lparams.topMargin = image.getPosition().y;
                    lparams.width = backgroundImage.getWidth();
                    lparams.height = backgroundImage.getHeight();
                    imageView.setLayoutParams(lparams);
                    imageView.setImageBitmap(imageBitmap);
                    imageView.setScaleType(ScaleType.FIT_XY);
                    // Add to parent
                    addView(imageView);
                } else {
                    loadErrorReasons.add("Could not decode bitmap from file:" + filePath);
                    break;
                }
            }

            for (final SwrveButton button : format.getButtons()) {
                String filePath = message.getCacheDir().getAbsolutePath() + "/" + button.getImage();
                if(!SwrveHelper.hasFileAccess(filePath)) {
                    SwrveLogger.e(LOG_TAG, "Do not have read access to message asset for:" + filePath);
                    loadErrorReasons.add("Do not have read access to message asset for:" + filePath);
                    continue;
                }

                // Load image
                final BitmapResult backgroundImage = decodeSampledBitmapFromFile(filePath, screenWidth, screenHeight, minSampleSize);
                if (backgroundImage != null && backgroundImage.getBitmap() != null) {
                    Bitmap imageBitmap = backgroundImage.getBitmap();
                    SwrveButtonView buttonView = createSwrveButton(context, button.getActionType());
                    bitmapCache.add(new WeakReference<Bitmap>(imageBitmap));
                    // Position
                    RelativeLayout.LayoutParams lparams = new RelativeLayout.LayoutParams(backgroundImage.getWidth(), backgroundImage.getHeight());
                    lparams.leftMargin = button.getPosition().x;
                    lparams.topMargin = button.getPosition().y;
                    lparams.width = backgroundImage.getWidth();
                    lparams.height = backgroundImage.getHeight();
                    buttonView.setLayoutParams(lparams);
                    buttonView.setImageBitmap(imageBitmap);
                    buttonView.setScaleType(ScaleType.FIT_XY);
                    buttonView.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View buttonView) {
                            try {
                                message.getCampaignManager().buttonWasPressedByUser(button);
                                message.getCampaign().messageDismissed();
                                dismiss();

                                if (button.getActionType() == SwrveActionType.Install) {
                                    final String appInstallLink = message.getCampaignManager().getAppStoreURLForApp(button.getAppId());
                                    // in case the install link was not set correctly log issue and return early
                                    // without calling the install button listener not starting the install intent
                                    if (SwrveHelper.isNullOrEmpty(appInstallLink)) {
                                        SwrveLogger.e(LOG_TAG, "Could not launch install action as there was no app install link found. Please supply a valid app install link.");
                                        return;
                                    }
                                    boolean freeEvent = true;
                                    if (installButtonListener != null) {
                                        freeEvent = installButtonListener.onAction(appInstallLink);
                                    }
                                    if (freeEvent) {
                                        Context ctxt = buttonView.getContext();
                                        if (ctxt != null) {
                                            // Launch app store
                                            try {
                                                ctxt.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(appInstallLink)));
                                            } catch (android.content.ActivityNotFoundException anfe) {
                                                SwrveLogger.e(LOG_TAG, "Couldn't launch install action. No activity found for: " + appInstallLink, anfe);
                                            } catch (Exception exp) {
                                                SwrveLogger.e(LOG_TAG, "Couldn't launch install action for: " + appInstallLink, exp);
                                            }
                                        }
                                    }
                                } else if (button.getActionType() == SwrveActionType.Custom) {
                                    if (customButtonListener != null) {
                                        customButtonListener.onAction(button.getAction());
                                    } else {
                                        Context ctxt = buttonView.getContext();
                                        String buttonAction = button.getAction();
                                        // Parse action as an Uri
                                        try {
                                            ctxt.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(buttonAction)));
                                        } catch (Exception e) {
                                            SwrveLogger.e(LOG_TAG, "Couldn't launch default custom action: " + buttonAction, e);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                SwrveLogger.e(LOG_TAG, "Error in onClick handler.", e);
                            }
                        }
                    });
                    // Add to parent
                    addView(buttonView);
                } else {
                    loadErrorReasons.add("Could not decode bitmap from file:" + filePath);
                    break;
                }
            }
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Error while initializing SwrveMessageView layout", e);
            loadErrorReasons.add("Error while initializing SwrveMessageView layout:" + e.getMessage());
        } catch (OutOfMemoryError e) {
            SwrveLogger.e(LOG_TAG, "OutOfMemoryError while initializing SwrveMessageView layout", e);
            loadErrorReasons.add("OutOfMemoryError while initializing SwrveMessageView layout:" + e.getMessage());
        }

        if (loadErrorReasons.size() > 0) {
            Map<String, String> errorReasonPayload = new HashMap<String, String>();
            errorReasonPayload.put("reason", loadErrorReasons.toString());
            destroy();
            throw new SwrveMessageViewBuildException("There was an error creating the view caused by:\n" + loadErrorReasons.toString());
        }
    }

    private void dismiss() {
        parent.dismiss();
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
            SwrveLogger.e(LOG_TAG, "Error while onLayout in SwrveMessageView", e);
        }
    }

    protected SwrveImageView createSwrveImage(Context context) {
        return new SwrveImageView(context);
    }

    protected SwrveButtonView createSwrveButton(Context context, SwrveActionType type) {
        return new SwrveButtonView(context, type);
    }

    public void setInstallButtonListener(ISwrveInstallButtonListener installButtonListener) {
        this.installButtonListener = installButtonListener;
    }

    public void setCustomButtonListener(ISwrveCustomButtonListener customButtonListener) {
        this.customButtonListener = customButtonListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    public void destroy() {
        try {
            if (bitmapCache != null) {
                // Iterate through all the bitmaps to recycle them
                Iterator<WeakReference<Bitmap>> bitmapIt = bitmapCache.iterator();
                while (bitmapIt.hasNext()) {
                    WeakReference<Bitmap> weakBitmap = bitmapIt.next();
                    Bitmap b = weakBitmap.get();
                    if (b != null) {
                        if (!b.isRecycled()) {
                            b.recycle();
                        }
                        b = null;
                    }
                    weakBitmap = null;
                }

                bitmapCache.clear();
                bitmapCache = null;
            }
            System.gc();
        } catch (Exception exp) {
            SwrveLogger.e(LOG_TAG, Log.getStackTraceString(exp));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        destroy();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        destroy();
    }

    private static class BitmapResult {
        private Bitmap bitmap;
        private int width;
        private int height;

        public BitmapResult(Bitmap bitmap, int width, int height) {
            this.bitmap = bitmap;
            this.width = width;
            this.height = height;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}
