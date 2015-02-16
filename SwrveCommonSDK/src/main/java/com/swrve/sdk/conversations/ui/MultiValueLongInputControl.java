package com.swrve.sdk.conversations.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.swrve.sdk.common.R;
import com.swrve.sdk.conversations.engine.model.ChoiceInputItem;
import com.swrve.sdk.conversations.engine.model.ChoiceInputResponse;
import com.swrve.sdk.conversations.engine.model.UserInputResult;
import com.swrve.sdk.conversations.engine.model.MultiValueLongInput;
import com.swrve.sdk.conversations.engine.model.MultiValueLongInput.Item;
import com.swrve.sdk.conversations.engine.model.OnContentChangedListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MultiValueLongInputControl extends LinearLayout implements ConversationInput {

    private MultiValueLongInput model;
    private HashMap<String, ChoiceInputItem> responses = new HashMap<String, ChoiceInputItem>();
    private OnContentChangedListener onContentChangedListener;
    private ArrayList<Spinner> spinners;

    @SuppressLint("NewApi")
    public MultiValueLongInputControl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MultiValueLongInputControl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiValueLongInputControl(Context context) {
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
    public static MultiValueLongInputControl inflate(Context context, ViewGroup parentContainer, MultiValueLongInput model) {
        LayoutInflater layoutInf = LayoutInflater.from(context);
        MultiValueLongInputControl control = (MultiValueLongInputControl) layoutInf.inflate(R.layout.cio__multiinputlong, parentContainer, false);

        control.model = model;
        control.spinners = new ArrayList<Spinner>();

        int itemCount = control.model.getValues().size();
        for (int i = 0; i < itemCount; i++) {
            Item item = control.model.getValues().get(i);

            ViewGroup row = (ViewGroup) layoutInf.inflate(R.layout.cio__multiinputlong_row, control, false);

            android.widget.TextView header = (android.widget.TextView) row.findViewById(R.id.cio__MIV_Item_Header);
            Spinner selector = (Spinner) row.findViewById(R.id.cio__MIV_Item_Spinner);

            header.setText(item.getTitle());
            ArrayAdapter<ChoiceInputItem> adapter = new ArrayAdapter<ChoiceInputItem>(context, R.layout.cio__simple_spinner_item, item.getOptions());
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            // a 'Header' type option to make people choose one
            String pleaseSelect = "Please Select";
            ChoiceInputItem pleaseSelectChoice = new ChoiceInputItem("123-fake-id", pleaseSelect);
            ChoiceInputItem firstItem = (ChoiceInputItem) adapter.getItem(0);
            if (firstItem.getAnswerText().equalsIgnoreCase(pleaseSelect)){
                adapter.remove(adapter.getItem(0));
            }
            adapter.insert(pleaseSelectChoice, 0);
            selector.setAdapter(adapter);

            selector.setOnItemSelectedListener(control.createListener(item));
            control.spinners.add(selector);
            control.addView(row);
        }
        return control;
    }


    @Override
    public void onReplyDataRequired(Map<String, Object> dataMap) {
        for(String key: responses.keySet())
        {
            ChoiceInputItem response = responses.get(key);
            // Answer ID and Answer text are the same for both
            ChoiceInputResponse r = new ChoiceInputResponse();
            r.setQuestionID(key);
            r.setFragmentTag(model.getTag());
            r.setAnswerID(response.getAnswerID());
            r.setAnswerText(response.getAnswerText());
            dataMap.put(model.getTag() + "-" + key, r);
        }
    }

    @Override
    public String validate() {
        if (model.isOptional()) {
            return null;
        }
        if (responses.size() < model.getValues().size()) {
            return "Please choose an item";
        } else {
            return null;
        }
    }

    public void setUserInput(UserInputResult r){
        // Go through each of the spinners and find the answer which corresponds to the choice input by the user
        for(Spinner spinner : spinners){
            ChoiceInputResponse usersChoice = (ChoiceInputResponse) r.getResult();
            SpinnerAdapter adapter = spinner.getAdapter();
            for(int i = 0; i < adapter.getCount(); i++){
                ChoiceInputItem choiceOption = (ChoiceInputItem) adapter.getItem(i);
                if (choiceOption.getAnswerID().equalsIgnoreCase(usersChoice.getAnswerID())){
                    spinner.setSelection(i);
                }
            }
        }
    }


    private MILongItemListener createListener(Item item) {
        return new MILongItemListener(item);
    }

    public void setOnContentChangedListener(OnContentChangedListener l) {
        onContentChangedListener = l;
    }

    private class MILongItemListener implements OnItemSelectedListener {
        private Item item;

        public MILongItemListener(Item item) {
            this.item = item;
        }

        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int position,
                                   long id) {
            if (position == 0) {
                // This is the please select option. Ignore
                responses.remove(item.getTitle());
                return;
            } else {
                responses.put(item.getQuestionId(), item.getOptions().get(position));
            }
            if (onContentChangedListener != null) {
                onContentChangedListener.onContentChanged();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            responses.remove(item.getTitle());
        }
    }
}
