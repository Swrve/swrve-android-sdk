package com.swrve.sdk.converser.ui;

import android.app.Activity;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.common.R;
import com.swrve.sdk.converser.engine.CustomBehaviours;
import com.swrve.sdk.converser.engine.model.ButtonControl;
import com.swrve.sdk.converser.engine.model.CalendarInput;
import com.swrve.sdk.converser.engine.model.Content;
import com.swrve.sdk.converser.engine.model.OnContentChangedListener;
import com.swrve.sdk.converser.engine.model.ControlActions;
import com.swrve.sdk.converser.engine.model.ControlBase;
import com.swrve.sdk.converser.engine.model.ConversationAtom;
import com.swrve.sdk.converser.engine.model.ConversationPage;
import com.swrve.sdk.converser.engine.model.ConversationReply;
import com.swrve.sdk.converser.engine.model.ConverserInputResult;
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
import java.util.HashMap;
import java.util.Map;


/**
 * (will eventually) display a page content, input, and choices as well as handling
 *
 * @author Jason Connery
 */
public class ConversationFragment extends Fragment implements OnClickListener {
    public static final String SWRVE_EVENT_NAVIGATION_START = "Swrve.Conversation.start";
    public static final String SWRVE_EVENT_NAVIGATION_DONE = "Swrve.Conversation.done";
    public static final String SWRVE_EVENT_NAVIGATION_ERROR = "Swrve.Conversations.error";
    public static final String SWRVE_EVENT_NAVIGATION_PAGE = "Swrve.Conversations.page";
    public static final String SWRVE_EVENT_NAVIGATION_CANCEL = "Swrve.Conversations.cancel";
    public static final String SWRVE_EVENT_ACTION_LINK = "Swrve.Conversations.link";
    public static final String SWRVE_EVENT_ACTION_CALL = "Swrve.Conversations.call";

    private static final String ARG_CONVERSATION_REF = "io.converser.conversationRef";


    private static final String LOG_TAG = "ConversationFragment";

    private ViewGroup root;
    private LinearLayout contentLayout;
    private LinearLayout controlLayout;

    private SwrveConversation swrveConversation;
    private ConversationPage page;
    private SwrveBase controller;
    private ArrayList<ConverserInput> inputs = new ArrayList<ConverserInput>();
    private HashMap<String, Map<String, Object>> userData = new HashMap<>();


    public static ConversationFragment create(SwrveConversation swrveConversation) {
        ConversationFragment f = new ConversationFragment();
        // TODO: STM Beware of OnResumes and other state held things.
        f.swrveConversation = swrveConversation;
        f.controller = swrveConversation.getConversationController();
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
        openFirstPage();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.cio__conversation_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void openFirstPage() {
        page = swrveConversation.getfirstPage();
        sendStartNavigationEvent();
        openConversationOnPage(page);
    }

    public void openConversationOnPage(ConversationPage conversationPage) {
        LayoutInflater layoutInf = getLayoutInflater(null);
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm, dd MMM");
        this.page = conversationPage;
        if (inputs.size() > 0) {
            inputs.clear();
        }

        Activity activity = getActivity();

        activity.setTitle(conversationPage.getTitle());

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

        if (conversationPage.getControls().size() == 0) {

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

        for (int i = 0; i < conversationPage.getControls().size(); i++) {
            ConversationAtom atom = conversationPage.getControls().get(i);

            boolean isFirst = (i == 0);
            boolean isLast = (i == conversationPage.getControls().size() - 1);

            if (atom instanceof ButtonControl) {
                // Fucked up use case
                // There are times when the layout or styles will need to change
                // based on the number of controls.
                // EG if there is one button, make it green. If there are 2
                // buttons, make the first red, and the second green
                int numControls = conversationPage.getControls().size();

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

        for (ConversationAtom content : conversationPage.getContent()) {
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

                    // Let the eventListener know that something has happened to the video
                    final HtmlVideoView cloneView = view;
                    view.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            // TODO: STM Due the fact that we render video in HTML, its very difficult to detect when a video has started/stopped  playing. For now all we can say is that the video was touched. Note that on click listeners behave strange with WebViews
                            stashVideoViewed(cloneView);
                            return false;
                        }
                    });
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
                    final TextInput inputModel = (TextInput) content;

                    EditTextControl etc = (EditTextControl) getLayoutInflater(null).inflate(R.layout.cio__edittext_input, contentLayout, false);
                    etc.setModel(inputModel);

                    // Store the result of the content for processing later
                    final EditTextControl etcCloneReference = etc;
                    etc.setOnContentChangedListener(new OnContentChangedListener() {
                        @Override
                        public void onContentChanged() {
                            Log.i("SHANEDEBUG", "CONTENT CHANGED");
                        }
                    });

                    contentLayout.addView(etc);
                    inputs.add(etc);
                } else if (content instanceof MultiValueInput) {
                    final MultiValueInputControl input = new MultiValueInputControl(activity, null, (MultiValueInput) content);

                    LayoutParams lp;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        lp = new LayoutParams(controlLp);
                    } else {
                        lp = new LayoutParams(controlLp.width, controlLp.height);
                    }
                    lp.width = LayoutParams.MATCH_PARENT;
                    lp.height = LayoutParams.WRAP_CONTENT;

                    input.setLayoutParams(lp);

                    // Store the result of the content for processing later
                    input.setOnContentChangedListener(new OnContentChangedListener() {
                        @Override
                        public void onContentChanged() {
                            ConverserInputResult result = new ConverserInputResult();
                            input.onReplyDataRequired(result);
                            stashMultiChoiceInputData(result);
                            Log.i("SHANEDEBUG", "CONTENT CHANGED");
                        }
                    });

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
                    input.setOnContentChangedListener(new OnContentChangedListener() {
                        @Override
                        public void onContentChanged() {
                            Log.i("SHANEDEBUG", "CONTENT CHANGED");
                        }
                    });

                    contentLayout.addView(input);
                    inputs.add(input);
                } else if (content instanceof NPSInput) {
                    NPSlider slider = (NPSlider) getLayoutInflater(null).inflate(R.layout.cio__npslider, contentLayout, false);
                    slider.setModel((NPSInput) content);
                    slider.setOnContentChangedListener(new OnContentChangedListener() {
                        @Override
                        public void onContentChanged() {
                            Log.i("SHANEDEBUG", "CONTENT CHANGED");
                        }
                    });
                    contentLayout.addView(slider);
                    inputs.add(slider);
                } else if (content instanceof CalendarInput) {
                    CalendarInputControl cic = (CalendarInputControl) getLayoutInflater(null).inflate(R.layout.cio__calendar_input, contentLayout, false);
                    cic.setModel((CalendarInput) content);
                    cic.setOnContentChangedListener(new OnContentChangedListener() {
                        @Override
                        public void onContentChanged() {
                            Log.i("SHANEDEBUG", "CONTENT CHANGED");
                        }
                    });

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

            // When a control is clicked, a navigation event or action event occurs. We then send all the queued SwrveEvents which have been queued for this page
            commitUserInputsToEvents();

            if (v instanceof Button) {
                ConversationReply reply = new ConversationReply();
                Button convButton = (Button) v;
                ButtonControl model = convButton.getModel();
                if (((ConverserControl) v).getModel().hasActions()) {
                    CustomBehaviours customBehaviours = new CustomBehaviours(this.getActivity(), this.getActivity().getApplicationContext());
                    ControlActions actions = ((ConverserControl) v).getModel().getActions();
                    if (actions.isCall()) {
                        sendReply(model, reply);
                        sendCallActionEvent();
                        customBehaviours.openDialer(actions.getCallUri(), this.getActivity());
                    } else if (actions.isVisit()) {
                        HashMap<String, String> visitUriDetails = (HashMap<String, String>) actions.getVisitDetails();
                        String urlStr = visitUriDetails.get(ControlActions.VISIT_URL_URI_KEY);
                        String referrer = visitUriDetails.get(ControlActions.VISIT_URL_REFERER_KEY);
                        String ext = visitUriDetails.get(ControlActions.VISIT_URL_EXTERNAL_KEY);
                        Uri uri = Uri.parse(urlStr);

                        if (Boolean.parseBoolean(ext) == true) {
                            sendReply(model, reply);
                            sendLinkActionEvent();
                            customBehaviours.openIntentWebView(uri, this.getActivity(), referrer);
                        } else if (Boolean.parseBoolean(ext) == false) {
                            sendLinkActionEvent();
                            customBehaviours.openPopupWebView(uri, this.getActivity(), referrer, "Back to Conversation");
                        } else {

                        }
                    }
                } else {
                    // There are no actions associated with Button. Send a normal reply
                    sendReply(model, reply);
                }
            } else {
                // Unknown button type was clicked
            }
        }
    }

    /**
     * Go through each of the recorded interactions the user has with the page and queue them as events
     */
    private void commitUserInputsToEvents() {
        //  TODO: STM whats the best way to hook back into the controller/swrveConversation and send events
        Log.i(LOG_TAG, "Sending all conversation events for page: " + page.getName());
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

        ConversationPage nextPage = swrveConversation.getPageForControl(control);
        if (nextPage != null) {
            sendTransitionPageEvent();
            openConversationOnPage(nextPage);
        } else {
            Log.e(LOG_TAG, "No more pages in this conversation. This is not normal and the conversation will end prematurely");
            sendErrorNavigationEvent(null, null);
            getActivity().finish();
        }
    }

    public void onBackPressed() {
        sendCancelNavigationEvent(null);
        commitUserInputsToEvents();
    }

    private class DoneButtonListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            Activity act = getActivity();
            sendDoneNavigationEvent();
            commitUserInputsToEvents();
            act.finish();
        }
    }

    // Events
    private void sendStartNavigationEvent() {
        if (controller != null) {
            controller.conversationWasStartedByUser(swrveConversation);
        }
    }

    private void sendDoneNavigationEvent() {
        if (controller != null) {
            controller.conversationWasFinishedByUser(swrveConversation, null);
        }
    }

    private void sendErrorNavigationEvent(ConversationReply reply, Exception e) {
        if (controller != null) {
            controller.conversationEncounteredError(swrveConversation, null);
        }
    }

    private void sendCancelNavigationEvent(ConversationReply reply) {
        if (controller != null) {
            controller.conversationWasCancelledByUser(swrveConversation, reply);
        }
    }

    private void sendTransitionPageEvent() {
        if (controller != null) {

        }
    }

    private void sendLinkActionEvent() {
        if (controller != null) {

        }
    }


    private void sendCallActionEvent() {
        if (controller != null) {

        }
    }

    // For each of the content portions we store data about them which is then committed at a later point
    private void stashVideoViewed(HtmlVideoView v) {
        if (controller != null) {
            // TODO: Is there any data we can record about clicked video views?
            ConversationAtom content = v.getModel();
            String key = content.getTag();
            userData.put(key, null);
        }
    }

    private void stashMultiChoiceInputData(ConverserInputResult data) {
        if (controller != null) {
            Log.i("SHANEDEBUG", data.toString());
        }
    }

    private void stashMultiChoiceLongInputData(ConverserInputResult data) {
        if (controller != null) {

        }
    }

    private void stashRatingInputData(ConverserInputResult data) {
        if (controller != null) {
            // TODO: STM Not yet implemented since we don't have a rating view
        }
    }

    private void stashCalendarInputData(ConverserInputResult data) {
        if (controller != null) {
            Log.i("SHANEDEBUG", data.toString());
        }
    }

    private void stashNPSInputData(ConverserInputResult data) {
        if (controller != null) {
            Log.i("SHANEDEBUG", data.toString());
        }
    }

    private void stashEditTextControlInputData(ConverserInputResult data) {
        if (controller != null) {
            Log.i("SHANEDEBUG", data.toString());
        }
    }
}