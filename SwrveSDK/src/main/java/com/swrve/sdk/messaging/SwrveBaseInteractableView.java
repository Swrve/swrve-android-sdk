package com.swrve.sdk.messaging;

import android.content.Context;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public abstract class SwrveBaseInteractableView extends AppCompatImageView {

    public int clickColor;
    private SwrveMessageFocusListener messageFocusListener;
    private SwrveActionType type;

    public SwrveBaseInteractableView(Context context, SwrveActionType type, SwrveMessageFocusListener messageFocusListener, int inAppMessageClickColor) {
        super(context);
        this.type = type;
        this.messageFocusListener = messageFocusListener;
        this.clickColor = inAppMessageClickColor;
    }

    abstract String getAction();

    public SwrveActionType getType() {
        return type;
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
                scaleView(this, 1.0f, 1.2f);
            } else {
                scaleView(this, 1.2f, 1.0f);
            }
        }
    }

    private void scaleView(View v, float startScale, float endScale) {
        Animation anim = new ScaleAnimation(
                startScale, endScale,
                startScale, endScale,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 1f);
        anim.setFillAfter(true);
        anim.setDuration(100);
        v.startAnimation(anim);
    }
}
