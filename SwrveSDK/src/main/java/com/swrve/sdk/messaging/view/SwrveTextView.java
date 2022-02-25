package com.swrve.sdk.messaging.view;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.text.LineBreaker;
import android.os.Build;
import android.text.Layout;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewTreeObserver;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.widget.TextViewCompat;

import com.swrve.sdk.SwrveHelper;

public class SwrveTextView extends AppCompatTextView {

    // exposed for testing
    public SwrveTextView(Context context) {
        super(context);
    }

    public SwrveTextView(Context context, String text, SwrveTextViewStyle textViewStyle, SwrveCalibration calibration) {
        super(context);
        init(text, textViewStyle, calibration);
    }

    protected void init(String text, SwrveTextViewStyle textViewStyle, SwrveCalibration calibration) {
        setBackgroundColor(textViewStyle.getTextBackgroundColor());
        setTextColor(textViewStyle.getTextForegroundColor());

        setTextIsSelectable(true);
        setScrollContainer(false);
        setFocusable(true);
        setIncludeFontPadding(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
            setBreakStrategy(LineBreaker.BREAK_STRATEGY_SIMPLE);
        }

        setPadding(textViewStyle.getLeftPadding(), textViewStyle.getTopPadding(), textViewStyle.getRightPadding(), textViewStyle.getBottomPadding());
        setText(text);
        setTypeface(textViewStyle.getTextTypeFace());

        switch (textViewStyle.getHorizontalAlignment()) {
            case Left:
                setGravity(Gravity.START);
                break;
            case Right:
                setGravity(Gravity.END);
                break;
            case Center:
                setGravity(Gravity.CENTER_HORIZONTAL);
                break;
        }

        float calibratedTextSizePX = getCalibratedTextSize(textViewStyle.getFontSize(), calibration);
        //convert calibratedTextSizePX to DP so user preference font resizing will work below
        float calibratedTextSizeDP = SwrveHelper.convertPixelsToDp(calibratedTextSizePX, getContext());

        //for now we don't support scrolling textview on tv, due to focus issues.
        if (textViewStyle.isScrollable && isMobile()) {
            //use default SP unit, which will allow resizing based on user preferences
            setTextSize(TypedValue.COMPLEX_UNIT_SP, calibratedTextSizeDP);
            if (textViewStyle.getLineHeight() > 0) {
                setCalibratedLineSpacing((float) (getTextSize() * textViewStyle.getLineHeight()));
            }
        } else {
            if (textViewStyle.getLineHeight() > 0) {
                //use line height multiple when downscaling, as we can't calculate the line spacing.
                setLineSpacing(0, (float) (textViewStyle.getLineHeight()));
            }
            //auto size text to fit, we may switch back to calibratedTextSizePX if text actually fits inside container, without need for scaling
            //see global layout observer below
            TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this, 1, 200, 1, TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
            addListenerForResizing(calibratedTextSizePX, textViewStyle.getLineHeight());
        }
    }

    protected void addListenerForResizing(float calibratedTextSizePX, double lineHeightMultipler) {
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (calibratedTextSizePX < getTextSize()) {
                    TextViewCompat.setAutoSizeTextTypeWithDefaults(SwrveTextView.this, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE);
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, calibratedTextSizePX);
                    if (lineHeightMultipler > 0) {
                        setCalibratedLineSpacing((float) (calibratedTextSizePX * lineHeightMultipler));
                    }
                }
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    protected boolean isMobile() {
        return SwrveHelper.isMobile(getContext());
    }

    protected void setCalibratedLineSpacing(float lineHeight) {
        if (lineHeight <= 0) return;
        int fontHeight = getPaint().getFontMetricsInt(null);
        float linespacing = lineHeight - fontHeight;
        setLineSpacing(linespacing, 1);
    }

    protected float getCalibratedTextSize(float fontSize, SwrveCalibration calibration) {
        float scaledBaseFontSize = 1;
        int baseFontSize = 1;
        if (calibration != null) {
            scaledBaseFontSize = getScaledBaseFontSize(calibration.getText(), calibration.getWidth(), calibration.getHeight());
            baseFontSize = calibration.getBaseFontSize();
        }
        float scaledFontSize = (fontSize / baseFontSize) * scaledBaseFontSize;
        return scaledFontSize;
    }

    public float getScaledBaseFontSize(String text, int width, int height) {
        Paint paintText = new Paint();
        paintText.setTypeface(getTypeface());
        return SwrveHelper.getTextSizeToFitImage(paintText, text, width, height);
    }
}
