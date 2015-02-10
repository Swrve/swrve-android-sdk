package com.swrve.sdk.converser.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.swrve.sdk.common.R;
import com.swrve.sdk.converser.engine.model.ChoiceInputItem;
import com.swrve.sdk.converser.engine.model.ChoiceInputResponse;
import com.swrve.sdk.converser.engine.model.MultiValueInput;
import com.swrve.sdk.converser.engine.model.OnContentChangedListener;

import java.util.Map;

public class MultiValueInputControl extends LinearLayout implements ConverserInput, OnCheckedChangeListener {
    private MultiValueInput model;
    private int selectedIndex = -1; // default to none selected
    private OnContentChangedListener onContentChangedListener;

    public MultiValueInputControl(Context context, AttributeSet attrs, MultiValueInput model) {
        super(context, attrs);
        this.model = model;
        setOrientation(VERTICAL);
        setupViews();
    }

    private void setupViews() {
        removeAllViews();
        LayoutParams lbllp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        TextView descLbl = new TextView(getContext(), null, R.attr.conversationInputMultiValueDescriptionStyle);
        descLbl.setLayoutParams(lbllp);
        descLbl.setText(model.getDescription());
        addView(descLbl);

        for (int i = 0; i < model.getValues().size(); i++) {
            RadioButton rb = new RadioButton(getContext(), null, R.attr.conversationInputMultiValueItemStyle);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            rb.setLayoutParams(lp);
            rb.setText(model.getValues().get(i).getAnswerText());
            rb.setChecked(i == selectedIndex);
            if (!isInEditMode()) {
                rb.setTag(R.string.cio__indexTag, i);
            }
            this.addView(rb);
            rb.setOnCheckedChangeListener(this);
        }
    }

    @Override
    public void onReplyDataRequired(Map<String, Object> dataMap) {
        if (selectedIndex > -1) {
            ChoiceInputItem mv = model.getValues().get(selectedIndex);
            ChoiceInputResponse r = new ChoiceInputResponse();
            r.setQuestionID(model.getTag());
            r.setFragmentTag(model.getTag());
            r.setAnswerID(mv.getAnswerID());
            r.setAnswerText(mv.getAnswerText());
            dataMap.put(model.getTag(), r);
        }
    }

    @Override
    public String validate() {
        if (model.isOptional()) {
            return null;
        }

        if (this.selectedIndex < 0) {
            return "Please select an item";
        }

        return null;
    }

    public void setOnContentChangedListener(OnContentChangedListener l) {
        onContentChangedListener = l;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int index = (Integer) buttonView.getTag(R.string.cio__indexTag);

        if (selectedIndex > -1 && selectedIndex != index) {
            RadioButton oldChecked = (RadioButton) getChildAt(selectedIndex + 1);
            if (oldChecked.isChecked()) {
                oldChecked.setOnCheckedChangeListener(null); //not want to repeat myself
                oldChecked.setChecked(false);
                oldChecked.setOnCheckedChangeListener(this);
            }
        }

        if (!isChecked) {
            selectedIndex = -1;
        } else {
            selectedIndex = index;
        }
        if (onContentChangedListener != null) {
            onContentChangedListener.onContentChanged();
        }
    }
}
