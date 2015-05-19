package com.swrve.sdk.conversations.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;

import com.swrve.sdk.conversations.engine.model.ButtonControl;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.styles.SwrveAtomStyle;

public class ConversationButton extends android.widget.Button implements ConversationControl {
    private ButtonControl model;
    protected Drawable backgroundDrawable;
    protected int textColor, borderColor;


    public ConversationButton(Context context, ButtonControl model, int defStyle) {
        super(context, null, defStyle);
        if (model != null) {
            this.model = model;
            setText(model.getDescription());
        }

        SwrveAtomStyle style = model.getStyle();

        textColor = style.getTextColorInt();
        setTextColor(textColor);

        if (style.isSolidStyle()) {
            GradientDrawable gradientDrawable = new GradientDrawable();
            gradientDrawable.setColor(style.getBgColorInt());
            gradientDrawable.setCornerRadius(10.0f);
            backgroundDrawable = gradientDrawable;
        } else if (style.isOutlineStyle()) {
            borderColor = style.getTextColorInt();
            float outer = 10.0f;
            float inner = 5.0f;
            float[] outerR = new float[]{outer, outer, outer, outer, outer, outer, outer, outer};
            float[] innerR = new float[]{inner, inner, inner, inner, inner, inner, inner, inner};
            RectF inset = new RectF(6, 6, 6, 6);
            RoundRectShape rr = new RoundRectShape(outerR, inset, innerR);
            ShapeDrawable rectShapeDrawable = new ShapeDrawable(rr);
            Paint paint = rectShapeDrawable.getPaint();
            paint.setColor(borderColor);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(6);
            backgroundDrawable = rectShapeDrawable;
        } else {
            backgroundDrawable = null;
            // Return null. We want the activity to not be renderable since its an older version of conversations
        }
        setBackgroundForOs(backgroundDrawable);
    }

    @Override
    public ButtonControl getModel() {
        return model;
    }

    @SuppressLint("NewApi")
    public void setBackgroundForOs(Drawable drawable) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            setBackgroundDrawable(drawable); // Deprecated but still in use for now
        } else {
            setBackground(drawable); // Requires minimum api level 16
        }
    }
}
