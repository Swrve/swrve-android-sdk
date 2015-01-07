package io.converser.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.Map;

import io.converser.android.R;
import io.converser.android.engine.model.MultiValueInput;
import io.converser.android.engine.model.MultiValueInput.MultiValueItem;

public class MultiValueInputControl extends LinearLayout implements ConverserInput, OnCheckedChangeListener {

    private MultiValueInput model;
    private int selectedIndex = -1; //default to none selected


    public MultiValueInputControl(Context context, AttributeSet attrs, MultiValueInput model) {

        super(context, attrs);
        this.model = model;
        setOrientation(VERTICAL);

        setupViews();
    }

    private void setupViews() {

        removeAllViews();
        LayoutParams lbllp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        android.widget.TextView descLbl = new TextView(getContext(), null, R.attr.conversationInputMultiValueDescriptionStyle);
        descLbl.setLayoutParams(lbllp);
        descLbl.setText(model.getDescription());
        addView(descLbl);

        for (int i = 0; i < model.getValues().size(); i++) {

            android.widget.RadioButton rb = new RadioButton(getContext(), null, R.attr.conversationInputMultiValueItemStyle);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            rb.setLayoutParams(lp);

            rb.setText(model.getValues().get(i).getName());

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
            MultiValueItem mv = model.getValues().get(selectedIndex);
            dataMap.put(model.getTag(), mv.getValue());
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

    }

}
