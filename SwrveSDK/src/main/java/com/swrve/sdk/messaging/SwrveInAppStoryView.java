package com.swrve.sdk.messaging;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.swrve.sdk.SwrveLogger;

import java.lang.ref.WeakReference;
import java.util.List;

public class SwrveInAppStoryView extends View {

    private int segments = 1;
    private int currentIndex = 0; //The index of the segment currently animating
    private float segmentProgress = 0; //0 - 100
    private int segmentGap;
    private int segmentDuration; //Milliseconds
    private int barHeight;
    private int colorBar;
    private int colorBarBackground;

    //State variables for the drawing of view
    private int roundingRadius = 0;
    private int segmentWidth = 0;
    private RectF drawArea;
    private Paint bgPaint, segPaint;

    private Thread animationThread = null;

    private List<Integer> segmentDurations;

    private WeakReference<SwrveInAppStorySegmentListener> segmentListener = new WeakReference<>(null);

    public interface SwrveInAppStorySegmentListener {
        void segmentFinished(int segmentIndex);
    }

    private class AnimationThread extends Thread {
        private int ANIMATION_INTERVAL = 15; //Milliseconds
        private long previousRunTime = System.currentTimeMillis();  //Timestamp of last progress bar update

        public void run() {
            //Could be resuming a partially-completed animation, so calculate remaining duration
            float durationRemaining = (float) getSegmentDuration() * ((100f - segmentProgress) / 100);
            while (!isInterrupted()) {
                int lastRunDuration = (int) (System.currentTimeMillis() - previousRunTime);
                float progressDiff = ((float) lastRunDuration / durationRemaining) * 100;
                segmentProgress += progressDiff;
                //Call invalidate to request onDraw, when UI thread is next free
                postInvalidate();
                previousRunTime = System.currentTimeMillis();
                if (segmentProgress >= 100f) {
                    segmentProgress = 100f;
                    stopAnimation();
                    synchronized (segmentListener) {
                        if (segmentListener.get() != null) {
                            segmentListener.get().segmentFinished(currentIndex);
                            if(isInterrupted()) break;
                        }
                    }
                } else {
                    try {
                        join(ANIMATION_INTERVAL);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        }
    }

    public SwrveInAppStoryView(@NonNull Context context,
                               @NonNull SwrveInAppStorySegmentListener listener,
                               @NonNull SwrveStorySettings settings,
                               int numberOfSegments,
                               List<Integer> pageDurations) {
        super(context);
        init(listener, settings, numberOfSegments, pageDurations);
    }

    protected void init(SwrveInAppStorySegmentListener listener,
                        SwrveStorySettings settings,
                        int numberOfSegments,
                        List<Integer> pageDurations) {
        setListener(listener);
        segments = numberOfSegments;
        segmentDurations = pageDurations;
        colorBar = Color.parseColor(settings.getBarColor());
        colorBarBackground = Color.parseColor(settings.getBarBgColor());
        if (settings.getPageDuration() > 0) {
            segmentDuration = settings.getPageDuration();
        }
        if (settings.getBarHeight() > 0) {
            barHeight = settings.getBarHeight();
        }
        if (settings.getSegmentGap() > 0) {
            segmentGap = settings.getSegmentGap();
        }

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        //Always anchored to the top
        params.gravity = Gravity.TOP;

        params.topMargin = settings.getTopPadding();
        params.leftMargin = settings.getLeftPadding();
        params.rightMargin = settings.getRightPadding();

        setLayoutParams(params);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(getMeasuredWidth(), barHeight);
    }

    @Override
    protected void onLayout(boolean changed,
                            int left,
                            int top,
                            int right,
                            int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            segmentWidth = (int) ((float) ((getMeasuredWidth() - (segmentGap * (segments - 1)))) / segments);
            roundingRadius = getMeasuredHeight() / 2;
            drawArea = new RectF(0, 0, getMeasuredWidth(), getMeasuredHeight());
            bgPaint = new Paint();
            bgPaint.setColor(Color.TRANSPARENT);
            bgPaint.setAntiAlias(true);
            segPaint = new Paint();
            segPaint.setColor(colorBar);
            segPaint.setAntiAlias(true);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //Background
        canvas.drawRoundRect(drawArea, roundingRadius, roundingRadius, bgPaint);

        drawSegments(canvas, drawArea);
    }

    private void drawSegments(Canvas canvas, RectF area) {
        segPaint.setColor(colorBar);
        int segmentXPosition = 0;
        for (int segmentIndex = 0; segmentIndex < segments; segmentIndex++) {
            segmentXPosition = (segmentWidth + segmentGap) * segmentIndex;
            RectF segmentArea = new RectF(segmentXPosition,
                    area.top,
                    segmentXPosition + segmentWidth,
                    area.bottom);
            if (segmentIndex == currentIndex) {
                //We are drawing the progressing segment bar
                int width = (int) ((segmentWidth / 100f) * segmentProgress);
                RectF progressArea = new RectF(segmentXPosition,
                        0,
                        segmentXPosition + width,
                        getMeasuredHeight());
                segPaint.setColor(colorBarBackground);
                canvas.drawRoundRect(segmentArea, roundingRadius, roundingRadius, segPaint);
                segPaint.setColor(colorBar);
                drawProgressBar(canvas, segmentArea, progressArea, roundingRadius, roundingRadius, segPaint);
            } else {
                if (segmentIndex < currentIndex) {
                    //Draw the segment as completed (100% progress)
                    segPaint.setColor(colorBar);
                } else {
                    segPaint.setColor(colorBarBackground);
                }
                canvas.drawRoundRect(segmentArea, roundingRadius, roundingRadius, segPaint);
            }
        }
    }

    private void drawProgressBar(Canvas canvas, RectF segment, RectF area, float radX, float radY, Paint paint ) {

        //Clip to match the segment background area
        Path path = new Path();
        path.arcTo(new RectF(segment.left, segment.top, segment.left + (radX * 2), segment.bottom), 90, 180, false);
        path.arcTo(new RectF(segment.right - radX * 2, segment.top, segment.right, segment.bottom), 270, 180, false);
        path.lineTo( radX, segment.bottom);
        path.close();
        canvas.save();
        canvas.clipPath(path);

        //Draw a bar with rounded end, adjusting it so it doesn't appear squashed
        if(area.width() < area.height()) {
            area.left -= area.height() - area.width();
        }
        canvas.drawRoundRect(area, radX, radY, paint);

        //Remove clipping
        canvas.restore();
    }

    public int getSegmentProgress() {
        return (int) segmentProgress;
    }

    public int getNumberOfSegments() {
        return segments;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getSegmentDuration() {
        if(segmentDurations != null && segmentDurations.size() > currentIndex ) {
            return segmentDurations.get(currentIndex);
        } else {
            return segmentDuration;
        }
    }

    private boolean isAnimationRunning() {
        return animationThread != null && !animationThread.isInterrupted();
    }

    public void startSegmentAtIndex(int segmentIndex) {
        stopAnimation();
        currentIndex = segmentIndex;
        segmentProgress = 0;
        startAnimation();
    }

    private void stopAnimation() {
        if (animationThread != null) {
            animationThread.interrupt();
            animationThread = null;
        }
    }

    private void startAnimation() {
        if (!isAnimationRunning()) {
            animationThread = new AnimationThread();
            animationThread.start();
        }
    }

    public void pauseAnimation() {
        stopAnimation();
    }

    public void resumeAnimation() {
        startAnimation();
    }

    @VisibleForTesting
    public void setListener(SwrveInAppStorySegmentListener listener) {
        this.segmentListener = new WeakReference<>(listener);
    }

    @Override
    protected void onDetachedFromWindow() {
        close();
        super.onDetachedFromWindow();
    }

    public void close() {
        stopAnimation();
        synchronized (segmentListener) {
            segmentListener.clear();
        }
    }
}
