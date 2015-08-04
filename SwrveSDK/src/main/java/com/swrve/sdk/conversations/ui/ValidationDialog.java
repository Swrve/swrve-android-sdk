package com.swrve.sdk.conversations.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;

import com.swrve.sdk.R;


public class ValidationDialog extends DialogFragment implements OnClickListener {

    public static ValidationDialog create() {
        ValidationDialog frag = new ValidationDialog();
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.swrve__validation_dialog, container);
        view.findViewById(R.id.swrve__btnDialogOk).setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }
}
