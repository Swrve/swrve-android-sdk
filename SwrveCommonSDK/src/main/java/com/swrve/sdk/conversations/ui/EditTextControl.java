package com.swrve.sdk.conversations.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.swrve.sdk.common.R;
import com.swrve.sdk.conversations.engine.model.UserInputResult;
import com.swrve.sdk.conversations.engine.model.OnContentChangedListener;
import com.swrve.sdk.conversations.engine.model.TextInput;

import java.util.Map;


public class EditTextControl extends LinearLayout implements ConversationInput {
    private TextInput model;
    private TextView descriptionTextView;
    private EditText editText;
    private OnContentChangedListener onContentChangedListener;

    public EditTextControl(Context context) {
        super(context);
    }

    public EditTextControl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @SuppressLint("NewApi")
    public EditTextControl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        descriptionTextView = (TextView) findViewById(R.id.cio__editTextDesc);
        editText = (EditText) findViewById(R.id.cio__editText);

    }

    public void setModel(TextInput model) {
        this.model = model;
        init();
    }

    public void setUserInput(UserInputResult r) {
        editText.setText(r.getResultAsString());
    }

    public void setOnContentChangedListener(OnContentChangedListener l) {
        onContentChangedListener = l;
    }

    private void init() {
        descriptionTextView.setText(model.getDescription());
        editText.setHint(model.getPlaceholder());

        if (model.getLines() > 0) {
            editText.setLines(model.getLines());
        }

        if (model.getKeyboardType() != null) {
            if (model.getKeyboardType().equalsIgnoreCase(TextInput.KEYBOARD_EMAIL)) {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            } else if (model.getKeyboardType().equalsIgnoreCase(TextInput.KEYBOARD_NUMBER)) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
            } else if (model.getKeyboardType().equalsIgnoreCase(TextInput.KEYBOARD_PHONE)) {
                editText.setRawInputType(InputType.TYPE_CLASS_PHONE);
                editText.setInputType(InputType.TYPE_CLASS_PHONE);
            } else if (model.getKeyboardType().equalsIgnoreCase(TextInput.KEYBOARD_URL)) {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
            } else {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            }
        }

        LayoutParams lp = new LayoutParams(editText.getLayoutParams());
        if (model.getLines() < 1) {
            lp.height = LayoutParams.MATCH_PARENT;
        } else {
            lp.height = LayoutParams.WRAP_CONTENT;
        }
        lp.gravity = Gravity.TOP;
        editText.setLayoutParams(lp);
        editText.setGravity(Gravity.TOP);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (onContentChangedListener != null) {
                    onContentChangedListener.onContentChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    public void onReplyDataRequired(Map<String, Object> dataMap) {
        String myData = editText.getText().toString().trim();
        String myTag = model.getTag();
        dataMap.put(myTag, myData);
    }

    @Override
    public String validate() {
        if (model.isOptional()) {
            return null;
        }

        if (editText.getText().length() < 1) {
            return "Please input a value";
        } else {
            return null; //always ok. Maybe in the future we can detect if the input field is optional or must contain text
        }
    }
}
