package com.swrve.sdk.conversations.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.RelativeLayout;

import com.swrve.sdk.conversations.R;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.ConversationInputChangedListener;
import com.swrve.sdk.conversations.engine.model.StarRating;
import com.swrve.sdk.conversations.engine.model.UserInputResult;
import com.swrve.sdk.conversations.engine.model.styles.ConversationColorStyle;
import com.swrve.sdk.conversations.engine.model.styles.ConversationStyle;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConversationRatingBar extends LinearLayout implements RatingBar.OnRatingBarChangeListener, IConversationInput {

    private StarRating model;
    private HtmlSnippetView htmlSnippetView;
    private RatingBar ratingBar;
    private ConversationInputChangedListener inputChangedListener;

    public ConversationRatingBar(Context context, StarRating model, File cacheDir) {
        super(context);
        this.model = model;
        setOrientation(LinearLayout.VERTICAL);
        setTag(model.getTag());
        initHtmlSnippetView(cacheDir);
        initRatingBar();
    }

    private void initHtmlSnippetView(File cacheDir) {
        Content content = new Content(model.getTag(), ConversationAtom.TYPE.CONTENT_HTML, model.getStyle(), model.getValue(), "");
        htmlSnippetView = new HtmlSnippetView(getContext(), content, cacheDir);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        htmlSnippetView.setLayoutParams(layoutParams);
        htmlSnippetView.setBackgroundColor(Color.TRANSPARENT);
        ConversationStyle conversationStyle = model.getStyle();
        ConversationColorStyle conversationStyleBg = conversationStyle.getBg();
        SwrveConversationHelper.setBackgroundDrawable(htmlSnippetView, conversationStyleBg.getPrimaryDrawable());
        addView(htmlSnippetView);
    }

    private void initRatingBar() {
        ratingBar = new RatingBar(getContext(), null, R.attr.conversationContentRatingBarStyle);
        ratingBar.setNumStars(5);
        ratingBar.setStepSize(0.01f); // set a tiny increment and do the rounding in onRatingChanged method.
        ratingBar.setOnRatingBarChangeListener(this);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        ratingBar.setLayoutParams(layoutParams);

        setStarColor(Color.parseColor(model.getStarColor()));

        RelativeLayout container = new RelativeLayout(getContext(), null, R.attr.conversationContentRatingBarContainerStyle);
        LayoutParams containerParams = new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);
        container.setLayoutParams(containerParams);
        container.addView(ratingBar);
        addView(container);
    }

    private void setStarColor(int color) {
        LayerDrawable stars = (LayerDrawable) ratingBar.getProgressDrawable();
        stars.getDrawable(0).setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        stars.getDrawable(1).setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        stars.getDrawable(2).setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        if(!fromUser) {
            return;
        }
        if (rating < 1.0f) { // Once a star value is selected it can never be set to less than one
            ratingBar.setRating(1.0f);
        } else {
            float rounded = (float) Math.ceil(rating);
            ratingBar.setRating(rounded);
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
