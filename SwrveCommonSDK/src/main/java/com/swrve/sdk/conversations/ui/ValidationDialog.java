package com.swrve.sdk.conversations.ui;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.swrve.sdk.common.R;


public class ValidationDialog extends DialogFragment implements OnClickListener {
    private static final String ARGS_MESSAGE = "com.swrve.validation_message";

    public static ValidationDialog create(String message) {
        ValidationDialog frag = new ValidationDialog();

        Bundle args = new Bundle();
        args.putString(ARGS_MESSAGE, message);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cio__validation_dialog, container);

        android.widget.TextView msg = (android.widget.TextView) view.findViewById(R.id.cio__tvDialogContent);
        msg.setText(getArguments().getString(ARGS_MESSAGE));
        view.findViewById(R.id.cio__btnDialogOk).setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }
}
