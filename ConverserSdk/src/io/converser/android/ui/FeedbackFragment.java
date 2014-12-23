package io.converser.android.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import io.converser.android.ConverserEngine;
import io.converser.android.R;

public class FeedbackFragment extends Fragment implements OnClickListener {

    private static final String ARGS_AREA_ARRAY = "io.converser.areas";
    private ConverserEngine engine;

    public static FeedbackFragment create(String[] areas) {
        FeedbackFragment f = new FeedbackFragment();

        Bundle args = new Bundle();
        args.putStringArray(ARGS_AREA_ARRAY, areas);

        f.setArguments(args);

        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cio__feedback_fragment, container, false);
    }


    @Override
    public void onResume() {
        super.onResume();

        if (engine == null) {
            engine = new ConverserEngine(getActivity().getApplicationContext());
        }
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();

        View doneButton = view.findViewById(R.id.cio__btnFeedbackDone);
        doneButton.setOnClickListener(this);

        String[] areas = getArguments().getStringArray(ARGS_AREA_ARRAY);


        Spinner areaSpinner = (Spinner) view.findViewById(R.id.cio__spinnerFeedbackArea);
        ArrayAdapter<String> areasAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, areas);
        areaSpinner.setAdapter(areasAdapter);
        areasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        areaSpinner.setSelection(0);


    }


    @Override
    public void onClick(View v) {

        View view = getView();
        if (view == null) {
            //We can't do anything if we can't access our views
            return;
        }

        if (v.getId() == R.id.cio__btnFeedbackDone) {
            //Gather around...
            ReactionInputControl ric = (ReactionInputControl) view.findViewById(R.id.cio__feedback_input);
            Spinner areaSpinner = (Spinner) view.findViewById(R.id.cio__spinnerFeedbackArea);
            android.widget.EditText feedbackText = (android.widget.EditText) view.findViewById(R.id.cio__etFeedbackText);

            int reaction = ric.getReaction();

            if (reaction < 1) {

                ValidationDialog vd = ValidationDialog.create("Please Select a value");
                vd.show(getFragmentManager(), "validation_dialog");

                return;
            }

            String area = areaSpinner.getSelectedItem().toString();
            String text = feedbackText.getText().toString();

            final Activity activity = getActivity();
            engine.queueFeedback(getActivity(), reaction, area, text);

            Toast t = Toast.makeText(activity, "Thank you for the feedback!", Toast.LENGTH_SHORT);
            t.show();

        }

    }


}
