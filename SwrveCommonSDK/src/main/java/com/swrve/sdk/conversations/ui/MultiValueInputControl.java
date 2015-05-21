package com.swrve.sdk.conversations.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import com.swrve.sdk.common.R;
import com.swrve.sdk.conversations.engine.model.ChoiceInputItem;
import com.swrve.sdk.conversations.engine.model.ChoiceInputResponse;
import com.swrve.sdk.conversations.engine.model.MultiValueInput;
import com.swrve.sdk.conversations.engine.model.OnContentChangedListener;
import com.swrve.sdk.conversations.engine.model.UserInputResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

public class MultiValueInputControl extends LinearLayout implements Serializable, ConversationInput, OnCheckedChangeListener {
    private MultiValueInput model;
    private int selectedIndex = -1; // default to none selected
    private OnContentChangedListener onContentChangedListener;
    private TextView descLbl;
    private ArrayList<RadioButton> radioButtons;


    @SuppressLint("NewApi")
    public MultiValueInputControl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MultiValueInputControl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiValueInputControl(Context context) {
        super(context);
    }

    /**
     * inflates , but does not add to parent container. caller will need to add it
     *
     * @param context
     * @param parentContainer
     * @param model
     * @return
     */
    public static MultiValueInputControl inflate(Context context, ViewGroup parentContainer, MultiValueInput model) {
        LayoutInflater layoutInf = LayoutInflater.from(context);
        MultiValueInputControl control = (MultiValueInputControl) layoutInf.inflate(R.layout.cio__multiinput, parentContainer, false);
        control.descLbl = (android.widget.TextView) control.findViewById(R.id.cio__MIV_Header);
        control.descLbl.setText(model.getDescription());
        control.showHideError(model.hasError(), control, model);

        control.model = model;
        control.radioButtons = new ArrayList<RadioButton>();

        for (int i = 0; i < model.getValues().size(); i++) {
            RadioButton rb = new RadioButton(context);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            rb.setLayoutParams(lp);
            rb.setText(model.getValues().get(i).getAnswerText());
            rb.setChecked(i == control.selectedIndex);
            if (!control.isInEditMode()) {
                rb.setTag(R.string.cio__indexTag, i);
            }
            control.addView(rb);
            rb.setOnCheckedChangeListener(control);
            control.radioButtons.add(rb);
        }
        return control;
    }

    public void setTextColor(int colorInt){
        descLbl.setTextColor(colorInt);
    }

    public void setUserInput(UserInputResult userInput){
        ChoiceInputResponse choice = (ChoiceInputResponse) userInput.getResult();
        for(RadioButton rb : radioButtons){
            if (rb.getText().toString().equalsIgnoreCase(choice.getAnswerText()))
            {
                rb.setChecked(true);
            }
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
    public boolean isValid() {
        if (model.isOptional()) {
            return true;
        }

        if (this.selectedIndex < 0) {
            showHideError(true, this, model);
            return false;
        } else {
            showHideError(false, this, model);
            return true;
        }
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

        showHideError(false, this, model);
    }

    private void showHideError(boolean hasError, ViewGroup viewGroup, MultiValueInput model) {
        viewGroup.setBackgroundResource(hasError ? R.drawable.cio__error : 0);
        model.setError(hasError);
    }
}
