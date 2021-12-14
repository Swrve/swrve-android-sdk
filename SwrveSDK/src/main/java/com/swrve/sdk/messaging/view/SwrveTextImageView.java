package com.swrve.sdk.messaging.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.messaging.SwrveActionType;

// Single line text view which generates a canvas image
public class SwrveTextImageView extends SwrveBaseInteractableView {

    private static final float TEST_FONT_SIZE = 200;

    public int width;
    public int height;
    public SwrveInAppMessageConfig inAppConfig;
    public String text;
    public Bitmap viewBitmap;
    public String action;

    public SwrveTextImageView(Context context, SwrveActionType type, SwrveInAppMessageConfig inAppConfig, String text, int canvasWidth, int canvasHeight, String action) {
        super(context, type, inAppConfig.getFocusColor(), inAppConfig.getClickColor());
        this.inAppConfig = inAppConfig;
        this.text = text;
        this.width = canvasWidth;
        this.height = canvasHeight;
        this.action = action;

        this.viewBitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888);
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
        fitTextSizeToImage(this.text, paintText, this.width, this.height);

        // Align text in the center and draw it
        Rect rect = new Rect();
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.getTextBounds(this.text, 0, this.text.length(), rect);
        float x = (width - rect.width()) / 2f - rect.left;
        float y = (height + rect.height()) / 2f - rect.bottom;
        canvas.drawText(this.text, x, y, paintText);

        // Set the image bitmap after we've generated it
        this.setImageBitmap(this.viewBitmap);
    }

    private void fitTextSizeToImage(String text, Paint paint, int maxWidth, int maxHeight) {
        if (text == null || text.isEmpty() || paint == null) {
            return;
        }
        float textSizeToFitImage = SwrveHelper.getTextSizeToFitImage(paint, text, maxWidth, maxHeight);
        paint.setTextSize(textSizeToFitImage);
    }

    @Override
    public String getAction() {
        return action;
    }

    public String getText() {
        return text;
    }
}
