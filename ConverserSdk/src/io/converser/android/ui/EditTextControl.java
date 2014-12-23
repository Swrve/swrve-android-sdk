package io.converser.android.ui;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Map;

import io.converser.android.R;
import io.converser.android.model.TextInput;


public class EditTextControl extends LinearLayout implements ConverserInput {

    private TextInput model;

    private TextView descriptionTextView;
    private EditText editText;

    public EditTextControl(Context context) {
        super(context);
    }

    public EditTextControl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextControl(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        descriptionTextView = (android.widget.TextView) findViewById(R.id.cio__editTextDesc);
        editText = (android.widget.EditText) findViewById(R.id.cio__editText);

    }

    public void setModel(TextInput model) {
        this.model = model;
        init();
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
