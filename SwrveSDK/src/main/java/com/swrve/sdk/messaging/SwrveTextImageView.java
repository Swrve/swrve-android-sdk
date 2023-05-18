package com.swrve.sdk.messaging;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveTextTemplating;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.exceptions.SwrveSDKTextTemplatingException;

import java.util.Map;

// Single line text view which generates a canvas image
public class SwrveTextImageView extends SwrveBaseImageView {

    public SwrveInAppMessageConfig inAppConfig;
    protected String text;

    public SwrveTextImageView(Context context, SwrveWidget swrveWidget, Map<String, String> inAppPersonalization,
                              SwrveInAppMessageConfig inAppConfig, int width, int height) throws SwrveSDKTextTemplatingException {
        super(context, inAppConfig.getMessageFocusListener(), inAppConfig.getClickColor());
        this.inAppConfig = inAppConfig;
        setText(swrveWidget, inAppPersonalization);
        setContentDescription(swrveWidget, inAppPersonalization, text); // the text must be personalized already

        Bitmap viewBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(viewBitmap);

        // Fill the entire canvas with this solid color
        canvas.drawColor(inAppConfig.getPersonalizedTextBackgroundColor());

        // Create a Paint object for the text
        Paint paintText = new Paint();

        // Set Typeface
        paintText.setTypeface(inAppConfig.getPersonalizedTextTypeface());

        // Set properties of the Paint used to draw on the canvas
        paintText.setColor(inAppConfig.getPersonalizedTextForegroundColor());

        // Calculate the text size needed to fill the available space
        fitTextSizeToImage(this.text, paintText, width, height);

        // Align text in the center and draw it
        Rect rect = new Rect();
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.getTextBounds(this.text, 0, this.text.length(), rect);
        float x = (width - rect.width()) / 2f - rect.left;
        float y = (height + rect.height()) / 2f - rect.bottom;
        canvas.drawText(this.text, x, y, paintText);

        // Set the image bitmap after we've generated it
        setImageBitmap(viewBitmap);
        setScaleType(ScaleType.FIT_XY);
    }

    private void setText(SwrveWidget swrveWidget, Map<String, String> inAppPersonalization) throws SwrveSDKTextTemplatingException {
        this.text = SwrveTextTemplating.apply(swrveWidget.getText(), inAppPersonalization);
    }

    private void fitTextSizeToImage(String text, Paint paint, int maxWidth, int maxHeight) {
        if (text == null || text.isEmpty() || paint == null) {
            return;
        }
        float textSizeToFitImage = SwrveHelper.getTextSizeToFitImage(paint, text, maxWidth, maxHeight);
        paint.setTextSize(textSizeToFitImage);
    }

    public String getText() {
        return text;
    }
}
