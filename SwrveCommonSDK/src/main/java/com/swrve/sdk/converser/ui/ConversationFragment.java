package com.swrve.sdk.converser.ui;

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

import com.swrve.sdk.common.R;
import com.swrve.sdk.converser.engine.ConverserEngine;
import com.swrve.sdk.converser.engine.CustomBehaviours;
import com.swrve.sdk.converser.engine.model.ButtonControl;
import com.swrve.sdk.converser.engine.model.CalendarInput;
import com.swrve.sdk.converser.engine.model.Content;
import com.swrve.sdk.converser.engine.model.ControlActions;
import com.swrve.sdk.converser.engine.model.ControlBase;
import com.swrve.sdk.converser.engine.model.ConversationAtom;
import com.swrve.sdk.converser.engine.model.ConversationDetail;
import com.swrve.sdk.converser.engine.model.ConversationReply;
import com.swrve.sdk.converser.engine.model.DateChoice;
import com.swrve.sdk.converser.engine.model.DateSaver;
import com.swrve.sdk.converser.engine.model.InputBase;
import com.swrve.sdk.converser.engine.model.MultiValueInput;
import com.swrve.sdk.converser.engine.model.MultiValueLongInput;
import com.swrve.sdk.converser.engine.model.NPSInput;
import com.swrve.sdk.converser.engine.model.TextInput;
import com.swrve.sdk.converser.SwrveConversation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;


/**
 * (will eventually) display a conversationDetail content, input, and choices as well as handling
 *
 * @author Jason Connery
 */
public class ConversationFragment extends Fragment implements OnClickListener {

    private static final String ARG_CONVERSATION_REF = "io.converser.conversationRef";
    private static final String LOG_TAG = "ConversationFragment";

    private ViewGroup root;
    private LinearLayout contentLayout;
    private LinearLayout controlLayout;

    private ConversationDetail conversationDetail;
    private SwrveConversation swrveConversation;
    private ArrayList<ConverserInput> inputs = new ArrayList<ConverserInput>();


    public static ConversationFragment create(SwrveConversation swrveConversation) {
        ConversationFragment f = new ConversationFragment();
        // TODO: STM Beware of On resumes and other state held things.
        f.swrveConversation = swrveConversation;
        return f;
    }

    @Override
    public void onPause() {
        // TODO: STM save the conversations position so it can be resumed at a later date.
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        // TODO: STM Beware of On resumes and other state held things. This may need to pick up where it leaves off in a conversation at a later time
        // TODO: STM This onResume only respects getting the first page of the conversation, not where it left off.

        conversationDetail = swrveConversation.getfirstPage();
        displayConversation(conversationDetail);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cio__conversation_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    public void displayConversation(ConversationDetail conversationDetail) {
        LayoutInflater layoutInf = getLayoutInflater(null);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd MMM");
        this.conversationDetail = conversationDetail;
        if (inputs.size() > 0) {
            inputs.clear();
        }

        Activity activity = getActivity();

        activity.setTitle(conversationDetail.getTitle());

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

        if (conversationDetail.getControls().size() == 0) {

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

        for (int i = 0; i < conversationDetail.getControls().size(); i++) {
            ConversationAtom atom = conversationDetail.getControls().get(i);

            boolean isFirst = (i == 0);
            boolean isLast = (i == conversationDetail.getControls().size() - 1);

            if (atom instanceof ButtonControl) {
                // Fucked up use case
                // There are times when the layout or styles will need to change
                // based on the number of controls.
                // EG if there is one button, make it green. If there are 2
                // buttons, make the first red, and the second green
                int numControls = conversationDetail.getControls().size();

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

        for (ConversationAtom content : conversationDetail.getContent()) {
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
        // TODO: STM This onclick also has to respect the ConverserContent and send swrve events via that channel
        if (v instanceof ConverserControl) {
            // Ok, lets do this....
            if (v instanceof DatePickerButton) {
                final DateChoice dpModel = ((DatePickerButton) v).getModel();
                Calendar rightNow = Calendar.getInstance();
                DatePickerDialog dpd = new DatePickerDialog(getActivity(), new OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar targetTime = new GregorianCalendar(year, monthOfYear, dayOfMonth);
                        // TODO: STM Update this to send a SwrveEvent after the DateDialog has been filled in.
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
                        sendSwrveEvent("Swrve.Conversations.call");
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
                            sendSwrveEvent("Swrve.Conversations.link");
                            customBehaviours.openIntentWebView(uri, this.getActivity(), referrer);
                        } else if (Boolean.parseBoolean(ext) == false) {
                            sendSwrveEvent("Swrve.Conversations.link");
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

    private void sendSwrveEvent(String eventName){
        //  TODO: STM whats the best way to hook back into the controller/swrveConversation and send events
        Log.i(LOG_TAG, "Sending Event " + eventName);
        swrveConversation.getConversationController().event(eventName);
    }

    private void queueSwrveEvent(String eventName, HashMap<String, Object> payload){

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

        ConversationDetail nextPage = swrveConversation.getPageForControl(control);
        if(nextPage != null)
        {
            sendSwrveEvent("Swrve.Conversations.page");
            displayConversation(nextPage);
        }else
        {
            Log.e(LOG_TAG, "No more pages in this conversation. This is not normal and the conversation will end prematurely");
            sendSwrveEvent("Swrve.Conversations.error");
            getActivity().finish();
        }
    }

    private class DoneButtonListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Activity act = getActivity();
            sendSwrveEvent("Swrve.Conversation.done");
            act.finish();
        }
    }
}