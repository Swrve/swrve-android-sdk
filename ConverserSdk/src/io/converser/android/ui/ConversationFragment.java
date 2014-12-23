package io.converser.android.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.ProgressDialog;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import io.converser.android.ConverserEngine;
import io.converser.android.R;
import io.converser.android.custom.CustomBehaviours;
import io.converser.android.model.ButtonControl;
import io.converser.android.model.CalendarInput;
import io.converser.android.model.Content;
import io.converser.android.model.ControlActions;
import io.converser.android.model.ControlBase;
import io.converser.android.model.ConversationAtom;
import io.converser.android.model.ConversationDetail;
import io.converser.android.model.ConversationReply;
import io.converser.android.model.DateChoice;
import io.converser.android.model.DateSaver;
import io.converser.android.model.InputBase;
import io.converser.android.model.MultiValueInput;
import io.converser.android.model.MultiValueLongInput;
import io.converser.android.model.NPSInput;
import io.converser.android.model.TextInput;

/**
 * (will eventually) display a conversation content, input, and choices as well as handling
 *
 * @author Jason Connery
 */
public class ConversationFragment extends Fragment implements OnClickListener {

    private static final String ARG_CONVERSATION_REF = "io.converser.conversationRef";

    private ViewGroup root;
    private LinearLayout contentLayout;
    private LinearLayout controlLayout;
    private String conversationRef;
    private ConversationDetail conversation;

    private ArrayList<ConverserInput> inputs = new ArrayList<ConverserInput>();

    private ConverserEngine converserEngine;

    private ProgressDialog progressDialog;

    public static ConversationFragment create(String ref) {
        ConversationFragment f = new ConversationFragment();

        Bundle arguments = new Bundle();
        arguments.putString(ARG_CONVERSATION_REF, ref);
        f.setArguments(arguments);
        return f;
    }

    @Override
    public void onPause() {
        converserEngine.cancelOperations();
        converserEngine = null;
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (converserEngine == null) {
            converserEngine = new ConverserEngine(getActivity().getApplicationContext());

        }

        if (getArguments().containsKey(ARG_CONVERSATION_REF)) {
            conversationRef = getArguments().getString(ARG_CONVERSATION_REF);

            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage(getActivity().getString(R.string.cio__refreshing));
            progressDialog.show();

            converserEngine.getConversationDetail(conversationRef, new ConverserEngine.Callback<ConversationDetail>() {
                @Override
                public void onSuccess(final ConversationDetail response) {
                    if (isDetached() || getActivity() == null) {
                        return;
                    }

                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            displayConversation(response);

                            if (progressDialog != null) {
                                try {
                                    progressDialog.dismiss();
                                } finally {
                                }
                            }
                        }

                    });
                }

                @Override
                public void onError(String error) {
                    Log.i("Conversation Fragment", "Error getting the next piece of conversation");
                    if (isDetached() || getActivity() == null) {
                        return;
                    }

                    if (progressDialog != null) {
                        try {
                            progressDialog.dismiss();
                        } finally {
                        }
                    }
                }
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.cio__conversation_fragment, container, false);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    public void displayConversation(ConversationDetail result) {
        LayoutInflater layoutInf = getLayoutInflater(null);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd MMM");
        this.conversation = result;
        if (inputs.size() > 0) {
            inputs.clear();
        }

        Activity activity = getActivity();

        activity.setTitle(result.getTitle());

        root = (ViewGroup) getView();

        if (root == null) {
            return;
        }

        contentLayout = (LinearLayout) root.findViewById(R.id.cio__content);
        controlLayout = (LinearLayout) root.findViewById(R.id.cio__controls);

        if (contentLayout.getChildCount() > 0) {
            contentLayout.removeAllViews();
        }

        if (controlLayout.getChildCount() > 0) {
            controlLayout.removeAllViews();
        }

        LayoutParams controlLp;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            controlLp = new LayoutParams(root.getLayoutParams());
        } else {
            controlLp = new LayoutParams(root.getLayoutParams().width, root.getLayoutParams().height);
        }

        LayoutParams contentLp;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            contentLp = new LayoutParams(root.getLayoutParams());
        } else {
            contentLp = new LayoutParams(root.getLayoutParams().width, root.getLayoutParams().height);

        }

        controlLp.height = LayoutParams.WRAP_CONTENT;
        controlLp.weight = 0;

        contentLp.weight = 1;

        TypedArray margins = getActivity().getTheme().obtainStyledAttributes(new int[]
                {R.attr.controlLayoutMargin});

        int controlLayoutMarginInPixels = margins.getDimensionPixelSize(0, 0);

        int buttonCount = 0;

        if (result.getControls().size() == 0) {

            Button ctrlButton = new Button(activity, null, (buttonCount == 0 ? R.attr.conversationControlSecondButtonStyle : R.attr.conversationControlSecondButtonStyle));// (buttonCount
            // ==
            // 1
            // ?
            // R.attr.conversationControlFirstButtonStyle
            // :
            // R.attr.conversationControlSecondButtonStyle)));
            ctrlButton.setText("Done");
            LayoutParams buttonLP;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                buttonLP = new LayoutParams(controlLp);
            } else {
                buttonLP = new LayoutParams(controlLp.width, controlLp.height);
            }
            buttonLP.weight = 1;
            buttonLP.leftMargin = controlLayoutMarginInPixels;
            buttonLP.rightMargin = controlLayoutMarginInPixels;
            buttonLP.topMargin = controlLayoutMarginInPixels;
            buttonLP.bottomMargin = controlLayoutMarginInPixels;

            ctrlButton.setLayoutParams(buttonLP);
            controlLayout.addView(ctrlButton);
            ctrlButton.setOnClickListener(new DoneButtonListener());

        }

        for (int i = 0; i < result.getControls().size(); i++) {
            ConversationAtom atom = result.getControls().get(i);

            boolean isFirst = (i == 0);
            boolean isLast = (i == result.getControls().size() - 1);

            if (atom instanceof ButtonControl) {
                // Fucked up use case
                // There are times when the layout or styles will need to change
                // based on the number of controls.
                // EG if there is one button, make it green. If there are 2
                // buttons, make the first red, and the second green
                int numControls = result.getControls().size();

                ButtonControl ctrl = (ButtonControl) atom;
                Button ctrlButton = null;

                if (isFirst) {
                    if (numControls == 1) {
                        // Button should be green
                        ctrlButton = new Button(activity, ctrl, R.attr.conversationControlSecondButtonStyle);
                    } else if (numControls == 2) {
                        // Button should be red
                        ctrlButton = new Button(activity, ctrl, R.attr.conversationControlFirstButtonStyle);
                    } else if (numControls > 2) {
                        // Button should be red
                        ctrlButton = new Button(activity, ctrl, R.attr.conversationControlFirstButtonStyle);
                    }
                } else if (!isFirst && !isLast) {
                    if (numControls == 1) {
                        // Button should be green
                        ctrlButton = new Button(activity, ctrl, R.attr.conversationControlSecondButtonStyle);
                    } else if (numControls == 2) {
                        // Button should be blue
                        ctrlButton = new Button(activity, ctrl, R.attr.conversationControlThirdButtonStyle);
                    } else if (numControls > 2) {
                        ctrlButton = new Button(activity, ctrl, R.attr.conversationControlThirdButtonStyle);
                    }
                    // If it is not the first button but is also not the last IE
                    // it is in the middle
                } else if (isLast) {
                    if (numControls == 1) {
                        ctrlButton = new Button(activity, ctrl, R.attr.conversationControlSecondButtonStyle);
                    } else if (numControls == 2) {
                        ctrlButton = new Button(activity, ctrl, R.attr.conversationControlSecondButtonStyle);
                    } else if (numControls > 2) {
                        ctrlButton = new Button(activity, ctrl, R.attr.conversationControlThirdButtonStyle);
                    }
                    ctrlButton = new Button(activity, ctrl, R.attr.conversationControlSecondButtonStyle);
                }

                buttonCount++;

                LayoutParams buttonLP;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    buttonLP = new LayoutParams(controlLp);
                } else {
                    buttonLP = new LayoutParams(controlLp.width, controlLp.height);
                }
                buttonLP.weight = 1;
                buttonLP.leftMargin = (isFirst ? controlLayoutMarginInPixels : controlLayoutMarginInPixels / 2);
                buttonLP.rightMargin = (isLast ? controlLayoutMarginInPixels : controlLayoutMarginInPixels / 2);
                buttonLP.topMargin = controlLayoutMarginInPixels;
                buttonLP.bottomMargin = controlLayoutMarginInPixels;

                ctrlButton.setLayoutParams(buttonLP);
                controlLayout.addView(ctrlButton);
                ctrlButton.setOnClickListener(this);

            } else if (atom instanceof DateChoice) {
                DatePickerButton btn = new DatePickerButton(activity, (DateChoice) atom);

                LayoutParams buttonLP;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    buttonLP = new LayoutParams(controlLp);
                } else {
                    buttonLP = new LayoutParams(controlLp.width, controlLp.height);
                }
                buttonLP.weight = 1;
                buttonLP.leftMargin = (isFirst ? controlLayoutMarginInPixels : controlLayoutMarginInPixels / 2);
                buttonLP.rightMargin = (isLast ? controlLayoutMarginInPixels : controlLayoutMarginInPixels / 2);
                buttonLP.topMargin = controlLayoutMarginInPixels;
                buttonLP.bottomMargin = controlLayoutMarginInPixels;

                btn.setLayoutParams(buttonLP);
                controlLayout.addView(btn);
                btn.setOnClickListener(this);

            } else if (atom instanceof DateSaver) {

            }

        }

        for (ConversationAtom content : result.getContent()) {
            if (content instanceof Content) {

                Content modelContent = (Content) content;

                if (modelContent.getType().toString().equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_IMAGE)) {
                    ImageView iv = new ImageView(activity, modelContent);
                    iv.setAdjustViewBounds(true);
                    iv.setScaleType(ScaleType.FIT_CENTER);
                    iv.setPadding(12, 12, 12, 12);

                    contentLayout.addView(iv);

                } else if (modelContent.getType().toString().equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_HTML)) {
                    LayoutParams tvLP;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        tvLP = new LayoutParams(controlLp);
                    } else {
                        tvLP = new LayoutParams(controlLp.width, controlLp.height);
                    }

                    tvLP.width = LayoutParams.MATCH_PARENT;
                    tvLP.height = LayoutParams.WRAP_CONTENT;

                    HtmlSnippetView view = new HtmlSnippetView(activity, modelContent);
                    view.setBackgroundColor(0);
                    view.setLayoutParams(tvLP);

                    contentLayout.addView(view);
                } else if (modelContent.getType().toString().equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_VIDEO)) {
                    LayoutParams tvLP;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        tvLP = new LayoutParams(controlLp);
                    } else {
                        tvLP = new LayoutParams(controlLp.width, controlLp.height);
                    }
                    tvLP.width = LayoutParams.MATCH_PARENT;
                    tvLP.height = LayoutParams.WRAP_CONTENT;

                    HtmlVideoView view = new HtmlVideoView(activity, modelContent);
                    view.setBackgroundColor(0);
                    view.setLayoutParams(tvLP);

                    contentLayout.addView(view);
                } else {
                    TextView tv = new TextView(activity, modelContent, R.attr.conversationTextContentDefaultStyle);

                    LayoutParams tvLP;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        tvLP = new LayoutParams(controlLp);
                    } else {
                        tvLP = new LayoutParams(controlLp.width, controlLp.height);
                    }
                    tv.setLayoutParams(tvLP);

                    contentLayout.addView(tv);
                }
            } else if (content instanceof InputBase) {
                if (content instanceof TextInput) {
                    // Do stuff for text
                    TextInput inputModel = (TextInput) content;

                    EditTextControl etc = (EditTextControl) getLayoutInflater(null).inflate(R.layout.cio__edittext_input, contentLayout, false);
                    etc.setModel(inputModel);

                    contentLayout.addView(etc);
                    inputs.add(etc);
                } else if (content instanceof MultiValueInput) {
                    MultiValueInputControl input = new MultiValueInputControl(activity, null, (MultiValueInput) content);

                    LayoutParams lp;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        lp = new LayoutParams(controlLp);
                    } else {
                        lp = new LayoutParams(controlLp.width, controlLp.height);
                    }
                    lp.width = LayoutParams.MATCH_PARENT;
                    lp.height = LayoutParams.WRAP_CONTENT;

                    input.setLayoutParams(lp);

                    contentLayout.addView(input);
                    inputs.add(input);
                } else if (content instanceof MultiValueLongInput) {
                    MultiValueLongInputControl input = MultiValueLongInputControl.inflate(activity, contentLayout, (MultiValueLongInput) content);

                    LayoutParams lp;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        lp = new LayoutParams(controlLp);
                    } else {
                        lp = new LayoutParams(controlLp.width, controlLp.height);
                    }
                    lp.width = LayoutParams.MATCH_PARENT;
                    lp.height = LayoutParams.WRAP_CONTENT;

                    input.setLayoutParams(lp);

                    contentLayout.addView(input);
                    inputs.add(input);
                } else if (content instanceof NPSInput) {
                    NPSlider slider = (NPSlider) getLayoutInflater(null).inflate(R.layout.cio__npslider, contentLayout, false);
                    slider.setModel((NPSInput) content);
                    contentLayout.addView(slider);
                    inputs.add(slider);
                } else if (content instanceof CalendarInput) {
                    CalendarInputControl cic = (CalendarInputControl) getLayoutInflater(null).inflate(R.layout.cio__calendar_input, contentLayout, false);
                    cic.setModel((CalendarInput) content);

                    contentLayout.addView(cic);
                    inputs.add(cic);
                }
            }
        }

        root.requestFocus();

    }

    @Override
    public void onClick(View v) {

        if (v instanceof ConverserControl) {
            // Ok, lets do this....
            if (v instanceof DatePickerButton) {
                final DateChoice dpModel = ((DatePickerButton) v).getModel();
                Calendar rightNow = Calendar.getInstance();
                DatePickerDialog dpd = new DatePickerDialog(getActivity(), new OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar targetTime = new GregorianCalendar(year, monthOfYear, dayOfMonth);
                        ConversationReply reply = new ConversationReply();
                        reply.getData().put(dpModel.getTag(), targetTime);
                        sendReply(dpModel, reply);
                    }
                }, rightNow.get(Calendar.YEAR) - 1900, rightNow.get(Calendar.MONTH), rightNow.get(Calendar.DAY_OF_MONTH));

                dpd.show();
            } else if (v instanceof Button) {
                ConversationReply reply = new ConversationReply();
                Button convButton = (Button) v;
                ButtonControl model = convButton.getModel();
                if (((ConverserControl) v).getModel().hasActions()) {
                    CustomBehaviours customBehaviours = new CustomBehaviours(this.getActivity(), this.getActivity().getApplicationContext());
                    ControlActions actions = ((ConverserControl) v).getModel().getActions();
                    if (actions.isCall()) {
                        sendReply(model, reply);
                        customBehaviours.openDialer(actions.getCallUri(), this.getActivity());
                    } else if (actions.isVisit()) {
                        HashMap<String, String> visitUriDetails = (HashMap<String, String>) actions.getVisitDetails();
                        String urlStr = visitUriDetails.get(ControlActions.VISIT_URL_URI_KEY);
                        String referrer = visitUriDetails.get(ControlActions.VISIT_URL_REFERER_KEY);
                        String ext = visitUriDetails.get(ControlActions.VISIT_URL_EXTERNAL_KEY);
                        Uri uri = Uri.parse(urlStr);

                        if (Boolean.parseBoolean(ext) == true) {
                            sendReply(model, reply);
                            customBehaviours.openIntentWebView(uri, this.getActivity(), referrer);
                        } else if (Boolean.parseBoolean(ext) == false) {
                            customBehaviours.openPopupWebView(uri, this.getActivity(), referrer, "Back to Conversation");
                        } else {

                        }
                    }
                } else {
                    // Send a simple reply
                    sendReply(model, reply);
                }

            } else {

            }

        }

    }

    /**
     * Kick off sending reply. The input tree will be traversed and responses gathered. If additional data needs to be included, include in the reply before passing in.
     *
     * @param control
     * @param reply
     */
    private void sendReply(ControlBase control, ConversationReply reply) {

        reply.setControl(control.getTag());

        ArrayList<String> validationErrors = new ArrayList<String>();

        // First, validate
        for (ConverserInput inputView : inputs) {
            String answer = inputView.validate();
            if (answer != null) {
                validationErrors.add(answer);
            }
        }

        if (validationErrors.size() > 0) {
            ValidationDialog vd = ValidationDialog.create("Please fill out all of the items on this page before continuing");
            vd.show(getFragmentManager(), "validation_dialog");

            return;
        }

        // For all the inputs , get their data

        for (ConverserInput inputView : inputs) {
            inputView.onReplyDataRequired(reply.getData());
        }

        converserEngine.replyToConversation(conversationRef, reply, new ConverserEngine.Callback<ConversationDetail>() {

            @Override
            public void onSuccess(final ConversationDetail response) {
                getActivity().runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (response != null) {
                            displayConversation(response);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        });
    }

    private class DoneButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            Activity act = getActivity();
            act.finish();
        }
    }
}