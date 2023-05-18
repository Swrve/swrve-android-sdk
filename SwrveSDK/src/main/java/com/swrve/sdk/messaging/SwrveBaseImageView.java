package com.swrve.sdk.messaging;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.SwrveTextTemplating;
import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;

import java.io.File;
import java.util.Map;

public abstract class SwrveBaseImageView extends AppCompatImageView {

    public int clickColor;
    private SwrveMessageFocusListener messageFocusListener;

    public SwrveBaseImageView(Context context) {
        super(context);
    }

    public SwrveBaseImageView(Context context, SwrveMessageFocusListener messageFocusListener, int inAppMessageClickColor) {
        super(context);
        this.messageFocusListener = messageFocusListener;
        this.clickColor = inAppMessageClickColor;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                setColorFilter(clickColor); // Darkening effect when user taps on view.
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                clearColorFilter();
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (messageFocusListener != null) {
            messageFocusListener.onFocusChanged(this, gainFocus, direction, previouslyFocusedRect);
        } else {
            if (gainFocus) {
                SwrveHelper.scaleView(this, 1.0f, 1.2f);
            } else {
                SwrveHelper.scaleView(this, 1.2f, 1.0f);
            }
        }
    }

    protected void setContentDescription(SwrveWidget swrveWidget, Map<String, String> inAppPersonalization, String text) throws SwrveSDKTextTemplatingException {
        if (SwrveHelper.isNotNullOrEmpty(swrveWidget.getAccessibilityText())) {
            String personalizedAccessibilityText = SwrveTextTemplating.apply(swrveWidget.getAccessibilityText(), inAppPersonalization);
            setContentDescription(personalizedAccessibilityText);
        } else if (SwrveHelper.isNotNullOrEmpty(text)) {
            setContentDescription(text);
        }
    }

    protected void loadImage(SwrveImageFileInfo imageFileInfo) {
        if (imageFileInfo.isGif) {
            loadGlideImage(imageFileInfo.filePath);
        } else {
            setImageBitmap(imageFileInfo.image.getBitmap());
        }
    }

    private void loadGlideImage(String filePath) {
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
                .into(this);
    }
}
