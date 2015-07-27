package com.swrve.sdk.messaging.view;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.RelativeLayout;

import com.swrve.sdk.messaging.ISwrveCustomButtonListener;
import com.swrve.sdk.messaging.ISwrveInstallButtonListener;
import com.swrve.sdk.messaging.SwrveActionType;
import com.swrve.sdk.messaging.SwrveMessage;
import com.swrve.sdk.messaging.SwrveMessageFormat;

/**
 * Android view representing a Swrve message with a given format. It contains
 * the SwrveInnerMessageView that will render buttons and images specified
 * on the template.
 */
public class SwrveMessageView extends RelativeLayout {
    protected static final String LOG_TAG = "SwrveMessagingSDK";

    // Container dialog that will display the message
    protected Dialog containerDialog;
    // Message being displayed
    protected SwrveMessage message;
    // Message format chosen to display message
    protected SwrveMessageFormat format;
    // Indicates if it is the first time this message is shown. It will be
    // false if the orientation is changing and the same message is going to
    // be redisplayed.
    protected boolean firstTime = true;
    // Indicates if the message has yet to be drawn
    protected boolean firstDraw = true;

    // Reference to inner message view that will render buttons and images
    protected SwrveInnerMessageView innerMessageView;

    // Native android animation ID to launch when showing message
    protected int showAnimation;
    // Native android animation ID to launch when dismissing message
    protected int dismissAnimation;

    public SwrveMessageView(Context context) {
        super(context);
    }

    public SwrveMessageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwrveMessageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SwrveMessageView(Context context, SwrveMessage message, SwrveMessageFormat format, boolean firstTime, int rotation, int minSampleSize) throws SwrveMessageViewBuildException {
        this(context, message, format, null, null, firstTime, rotation, minSampleSize);
    }

    public SwrveMessageView(Context context, SwrveMessage message, SwrveMessageFormat format, ISwrveInstallButtonListener installButtonListener, ISwrveCustomButtonListener customButtonListener, boolean firstTime, int rotation, int minSampleSize) throws SwrveMessageViewBuildException {
        super(context);
        this.message = message;
        this.format = format;
        this.firstTime = firstTime;
        initializeLayout(context, message, format, installButtonListener, customButtonListener, rotation, minSampleSize);
    }

    protected void initializeLayout(final Context context, final SwrveMessage message, final SwrveMessageFormat format, ISwrveInstallButtonListener installButtonListener, ISwrveCustomButtonListener customButtonListener, int rotation, int minSampleSize) throws SwrveMessageViewBuildException {
        innerMessageView = new SwrveInnerMessageView(context, this, message, format, installButtonListener, customButtonListener, minSampleSize);
        setBackgroundColor(format.getBackgroundColor());
        setGravity(Gravity.CENTER);
        setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(innerMessageView);
        setClipChildren(false);

        if (rotation != 0) {
            // Flip internal message so that it can still fit in the screen
            RotateAnimation rotate = new RotateAnimation(0, rotation, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotate.setFillAfter(true);
            innerMessageView.startAnimation(rotate);
        }
    }

    /**
     * @return the related message format.
     */
    public SwrveMessageFormat getFormat() {
        return format;
    }

    /**
     * @return the show animation.
     */
    public int getShowAnimation() {
        return showAnimation;
    }

    /**
     * Set the message show animation.
     *
     * @param showAnimation
     */
    public void setShowAnimation(int showAnimation) {
        this.showAnimation = showAnimation;
    }

    /**
     * @return the dismiss animation.
     */
    public int getDismissAnimation() {
        return dismissAnimation;
    }

    /**
     * @param dismissAnimation dismiss animation.
     */
    public void setDismissAnimation(int dismissAnimation) {
        this.dismissAnimation = dismissAnimation;
    }

    protected SwrveImageView createSwrveImage(Context context) {
        return new SwrveImageView(context);
    }

    protected SwrveButtonView createSwrveButton(Context context, SwrveActionType type) {
        return new SwrveButtonView(context, type);
    }

    /**
     * Set the install button listener to process message element events.
     *
     * @param installButtonListener
     */
    public void setInstallButtonListener(ISwrveInstallButtonListener installButtonListener) {
        this.innerMessageView.setInstallButtonListener(installButtonListener);
    }

    /**
     * Set the custom button listener to process message element events.
     *
     * @param customButtonListener
     */
    public void setCustomButtonListener(ISwrveCustomButtonListener customButtonListener) {
        this.innerMessageView.setCustomButtonListener(customButtonListener);
    }

    /**
     * Starts the show animation if it has been specified. Note that you
     * will have to add the view to a parent before calling this function.
     */
    public void startAnimation() {
        try {
            if (this.showAnimation != 0) {
                Animation animation = AnimationUtils.loadAnimation(getContext(), this.showAnimation);
                animation.setStartOffset(0);
                startAnimation(animation);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while showing message", e);
        }
    }

    /**
     * Hide the message by removing it from its parent. If a dismiss animation has
     * been specified the message will be removed from the parent when the animation
     * finishes.
     * NOTE: The parent has to be an instance of ViewGroup or otherwise the view
     * will have to be removed manually.
     */
    public void dismiss() {
        try {
            if (this.dismissAnimation != 0) {
                // Animate the message
                Animation animation = AnimationUtils.loadAnimation(getContext(), this.dismissAnimation);
                animation.setStartOffset(0);
                animation.setAnimationListener(new AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation anim) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation anim) {
                    }

                    @Override
                    public void onAnimationEnd(Animation anim) {
                        dismissView();
                    }
                });
                startAnimation(animation);
            } else {
                dismissView();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while dismissing message", e);
        }
    }

    private void dismissView() {
        final ViewParent parent = getParent();
        if (containerDialog == null) {
            removeView(parent);
        } else {
            containerDialog.dismiss();
        }
    }

    protected void removeView(ViewParent parent) {
        if (parent != null) {
            ((ViewGroup) parent).removeView(SwrveMessageView.this);
        }
        destroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        try {
            if (firstTime && firstDraw) {
                firstDraw = false;
                message.getMessageController().messageWasShownToUser(format);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error while processing first impression", e);
        }
    }

    public void destroy() {
        if (innerMessageView != null) {
            innerMessageView.destroy();
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

    public void setContainerDialog(Dialog dialog) {
        this.containerDialog = dialog;
    }
}
