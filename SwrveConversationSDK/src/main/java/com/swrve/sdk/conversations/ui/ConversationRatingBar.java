package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;

import com.swrve.sdk.conversations.R;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ConversationInputChangedListener;
import com.swrve.sdk.conversations.engine.model.StarRating;
import com.swrve.sdk.conversations.engine.model.UserInputResult;
import com.swrve.sdk.conversations.engine.model.styles.ConversationColorStyle;
import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

import java.util.HashMap;
import java.util.Map;

public class ConversationRatingBar extends LinearLayout implements RatingBar.OnRatingBarChangeListener, IConversationInput {

    private StarRating model;
    private HtmlSnippetView htmlSnippetView;
    private RatingBar ratingBar;
    private ConversationInputChangedListener inputChangedListener;

    public ConversationRatingBar(Context context, StarRating model) {
        super(context);
        this.model = model;
        setOrientation(LinearLayout.VERTICAL);
        setTag(model.getTag());
        initHtmlSnippetView();
        initRatingBar();
    }

    private void initHtmlSnippetView() {
        Content content = new Content();
        content.setValue(model.getValue());
        htmlSnippetView = new HtmlSnippetView(getContext(), content);
        htmlSnippetView.setTag(content.getTag());
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        htmlSnippetView.setLayoutParams(layoutParams);
        htmlSnippetView.setBackgroundColor(Color.TRANSPARENT);
        ConversationStyle conversationStyle = model.getStyle();
        ConversationColorStyle conversationStyleBg = conversationStyle.getBg();
        setBackgroundDrawable(htmlSnippetView, conversationStyleBg.getPrimaryDrawable());
        addView(htmlSnippetView);
    }

    private void initRatingBar() {
        ratingBar = new RatingBar(getContext(), null, R.attr.conversationContentRatingBarStyle);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(0.01f); // set a tiny increment and do the rounding in onRatingChanged method.
        ratingBar.setOnRatingBarChangeListener(this);

        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ratingBar.setLayoutParams(layoutParams);

        setStarColor(Color.parseColor(model.getStarColor()));

        RelativeLayout container = new RelativeLayout(getContext(), null, R.attr.conversationContentRatingBarContainerStyle);
        LayoutParams containerParams = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
        container.setLayoutParams(containerParams);
        container.addView(ratingBar);
        addView(container);
    }

    private void setBackgroundDrawable(HtmlSnippetView view, Drawable drawable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackgroundDrawable(drawable);
        } else {
            view.setBackground(drawable);
        }
    }

    private void setStarColor(int color) {
        LayerDrawable stars = (LayerDrawable) ratingBar.getProgressDrawable();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            stars.getDrawable(0).setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            stars.getDrawable(1).setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            stars.getDrawable(2).setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        } else {
            stars.getDrawable(0).setTint(color);
            stars.getDrawable(1).setTint(color);
            stars.getDrawable(2).setTint(color);
        }
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        if(!fromUser) {
            return;
        }
        if (rating < 1.0f) { // Once a star value is selected it can never be set to less than one
            ratingBar.setRating(1.0f);
        } else if (rating > 1.0f && rating <= 2.0f) {
            ratingBar.setRating(2.0f);
        } else if (rating > 2.0f && rating <= 3.0f) {
            ratingBar.setRating(3.0f);
        } else if (rating > 3.0f && rating <= 4.0f) {
            ratingBar.setRating(4.0f);
        } else if (rating > 4.0f && rating <= 5.0f) {
            ratingBar.setRating(5.0f);
        }

        if (inputChangedListener != null) {
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put(model.getTag(), ratingBar.getRating());
            inputChangedListener.onContentChanged(dataMap, model);
        }
    }

    @Override
    public void setUserInput(UserInputResult userInput) {
        if (userInput.getResult() != null && userInput.getResult() instanceof Float) {
            ratingBar.setRating((Float) userInput.getResult());
        }
    }

    public void setContentChangedListener(ConversationInputChangedListener inputChangedListener) {
        this.inputChangedListener = inputChangedListener;
    }

    public StarRating getModel() {
        return model;
    }

    public HtmlSnippetView getHtmlSnippetView() {
        return htmlSnippetView;
    }

    public RatingBar getRatingBar() {
        return ratingBar;
    }
}
