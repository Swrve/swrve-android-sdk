package com.swrve.sdk.messaging.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.swrve.sdk.config.SwrveInAppMessageConfig;
import com.swrve.sdk.messaging.SwrveActionType;

public class SwrvePersonalisedTextView extends SwrveBaseInteractableView {

    public int width;
    public int height;
    public SwrveInAppMessageConfig inAppConfig;
    public String text;
    public Bitmap viewBitmap;
    public String action;

    public SwrvePersonalisedTextView(Context context, SwrveActionType type, SwrveInAppMessageConfig inAppConfig, String text, int canvasWidth, int canvasHeight, String action) {
        super(context, type, inAppConfig.getFocusColor(), inAppConfig.getClickColor());
        this.inAppConfig = inAppConfig;
        this.text = text;
        this.width = canvasWidth;
        this.height = canvasHeight;
        this.action = action;

        this.viewBitmap = Bitmap.createBitmap(this.width, this.height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(viewBitmap);

        // Fill the entire canvas with this solid color.
        canvas.drawColor(inAppConfig.getPersonalisedTextBackgroundColor());

        // Create a Paint object for the text.
        Paint paintText = new Paint(Paint.FAKE_BOLD_TEXT_FLAG);

        // Set Typeface
        paintText.setTypeface(inAppConfig.getPersonalisedTextTypeface());

        // Set properties of the Paint used to draw on the canvas.
        paintText.setColor(inAppConfig.getPersonalisedTextForegroundColor());

        fitTextSizeToImage(this.text, paintText, this.width, this.height);

        paintText.getTextAlign();

        Rect rect = new Rect();
        canvas.getClipBounds(rect);

        int cHeight = rect.height();
        int cWidth = rect.width();
        paintText.setTextAlign(Paint.Align.LEFT);
        paintText.getTextBounds(this.text, 0, this.text.length(), rect);
        float x = cWidth / 2f - rect.width() / 2f - rect.left;
        float y = cHeight / 2f + rect.height() / 2f - rect.bottom;
        canvas.drawText(this.text, x, y, paintText);

        // we set the image bitmap after we've generated it.
        this.setImageBitmap(this.viewBitmap);
    }

    private float fitTextSizeToImage(String text, Paint paint, int maxWidth, int maxHeight) {
        if (text == null || text.isEmpty() || paint == null) return 0;
        Rect bound = new Rect();
        float size = 1.0f;
        float step= 1.0f;

        while (true) {
            paint.getTextBounds(text, 0, text.length(), bound);
            if(bound.width() == 0 && bound.height() == 0) return 0;

            if (bound.width() < maxWidth && bound.height() < maxHeight) {
                size += step;
                paint.setTextSize(size);
            } else {
                return size - step;
            }
        }
    }

    @Override
    public String getAction() {
        return action;
    }

    public String getText() {
        return text;
    }
}
