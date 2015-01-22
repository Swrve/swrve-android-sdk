package com.swrve.sdk.converser.ui;

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

import com.swrve.sdk.common.R;
import com.swrve.sdk.converser.engine.model.MultiValueLongInput;
import com.swrve.sdk.converser.engine.model.MultiValueLongInput.Item;
import com.swrve.sdk.converser.engine.model.OnContentChangedListener;

import java.util.HashMap;
import java.util.Map;

public class MultiValueLongInputControl extends LinearLayout implements ConverserInput {

    private MultiValueLongInput model;
    private HashMap<String, String> responses = new HashMap<String, String>();
    private OnContentChangedListener onContentChangedListener;

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

        int itemCount = control.model.getValues().size();
        for (int i = 0; i < itemCount; i++) {
            Item item = control.model.getValues().get(i);

            ViewGroup row = (ViewGroup) layoutInf.inflate(R.layout.cio__multiinputlong_row, control, false);

            android.widget.TextView header = (android.widget.TextView) row.findViewById(R.id.cio__MIV_Item_Header);
            Spinner selector = (Spinner) row.findViewById(R.id.cio__MIV_Item_Spinner);

            header.setText(item.getTitle());
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.cio__simple_spinner_item, item.getOptions());
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            //a 'Header' type option to make people choose one
            String pleaseSelect = "Please Select";
            adapter.remove(pleaseSelect); // On rotation, the adapter can sometimes hold onto older items. To fix this, we attempt to remove the item if it exists alright.
            adapter.insert(pleaseSelect, 0);
            selector.setAdapter(adapter);

            selector.setOnItemSelectedListener(control.createListener(item));
            control.addView(row);
        }

        return control;
    }

    @Override
    public void onReplyDataRequired(Map<String, Object> dataMap) {
        dataMap.put(model.getTag(), responses);
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
                responses.put(item.getTitle(), item.getOptions().get(position));
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
