package com.swrve.sdk.converser.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import com.swrve.sdk.ISwrveBase;
import com.swrve.sdk.SwrveBase;
import com.swrve.sdk.SwrveSDKBase;
import com.swrve.sdk.common.R;
import com.swrve.sdk.converser.SwrveConversation;
import com.swrve.sdk.converser.engine.ActionBehaviours;
import com.swrve.sdk.converser.engine.model.ButtonControl;
import com.swrve.sdk.converser.engine.model.CalendarInput;
import com.swrve.sdk.converser.engine.model.Content;
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
import com.swrve.sdk.converser.engine.model.OnContentChangedListener;
import com.swrve.sdk.converser.engine.model.TextInput;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

public class ConversationFragment extends Fragment implements OnClickListener {
    private static final String LOG_TAG = "ConversationFragment";

    private ViewGroup root;
    private LinearLayout contentLayout;
    private LinearLayout controlLayout;

    private ValidationDialog validationDialog;
    private SwrveConversation swrveConversation;
    private ConversationPage page;
    private SwrveBase controller;
    private ArrayList<ConverserInput> inputs = new ArrayList<ConverserInput>();
    private HashMap<String, ConverserInputResult> userInteractionData = new HashMap<>();
    private boolean userInputValid = false;

    public static ConversationFragment create(SwrveConversation swrveConversation) {
        ConversationFragment f = new ConversationFragment();
        // TODO: STM Beware of OnResumes and other state held things.
        f.swrveConversation = swrveConversation;
        f.controller = (SwrveBase)SwrveSDKBase.getInstance();
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
        page = swrveConversation.getFirstPage();
        sendStartNavigationEvent(page.getTag());
        openConversationOnPage(page);
    }

    @SuppressLint("NewApi")
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
                    String filePath = swrveConversation.getCacheDir().getAbsolutePath() + "/" + modelContent.getValue();
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    iv.setImageBitmap(bitmap);
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
                    final String tag = content.getTag();
                    view.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            // TODO: STM Due the fact that we render video in HTML, its very difficult to detect when a video has started/stopped  playing. For now all we can say is that the video was touched. Note that on click listeners behave strange with WebViews
                            stashVideoViewed(page.getTag(), tag, cloneView);
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
                    TextInput inputModel = (TextInput) content;

                    EditTextControl etc = (EditTextControl) getLayoutInflater(null).inflate(R.layout.cio__edittext_input, contentLayout, false);
                    etc.setModel(inputModel);

                    // Store the result of the content for processing later
                    final EditTextControl etcReference = etc;
                    final String tag = content.getTag();
                    etc.setOnContentChangedListener(new OnContentChangedListener() {
                        @Override
                        public void onContentChanged() {
                            HashMap<String, Object> result = new HashMap<String, Object>();
                            etcReference.onReplyDataRequired(result);
                            stashEditTextControlInputData(page.getTag(), tag, result);
                        }
                    });

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

                    final MultiValueInputControl mvicReference = input;
                    final String tag = content.getTag();
                    // Store the result of the content for processing later
                    input.setOnContentChangedListener(new OnContentChangedListener() {
                        @Override
                        public void onContentChanged() {
                            HashMap<String, Object> result = new HashMap<String, Object>();
                            mvicReference.onReplyDataRequired(result);
                            stashMultiChoiceInputData(page.getTag(), tag, result);
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
                    final MultiValueLongInputControl mviclReference = input;
                    final String tag = content.getTag();
                    input.setOnContentChangedListener(new OnContentChangedListener() {
                        @Override
                        public void onContentChanged() {
                            HashMap<String, Object> result = new HashMap<String, Object>();
                            mviclReference.onReplyDataRequired(result);
                            stashMultiChoiceLongInputData(page.getTag(), tag, result);
                        }
                    });

                    contentLayout.addView(input);
                    inputs.add(input);
                } else if (content instanceof NPSInput) {
                    NPSlider slider = (NPSlider) getLayoutInflater(null).inflate(R.layout.cio__npslider, contentLayout, false);
                    slider.setModel((NPSInput) content);
                    final NPSlider sliderReference = slider;
                    final String tag = content.getTag();
                    slider.setOnContentChangedListener(new OnContentChangedListener() {
                        @Override
                        public void onContentChanged() {
                            HashMap<String, Object> result = new HashMap<String, Object>();
                            sliderReference.onReplyDataRequired(result);
                            stashNPSInputData(page.getTag(), tag, result);
                        }
                    });
                    contentLayout.addView(slider);
                    inputs.add(slider);
                } else if (content instanceof CalendarInput) {
                    CalendarInputControl cic = (CalendarInputControl) getLayoutInflater(null).inflate(R.layout.cio__calendar_input, contentLayout, false);
                    cic.setModel((CalendarInput) content);
                    final CalendarInputControl cicReference = cic;
                    cic.setOnContentChangedListener(new OnContentChangedListener() {
                        @Override
                        public void onContentChanged() {
                            HashMap<String, Object> result = new HashMap<String, Object>();
                            cicReference.onReplyDataRequired(result);
                            stashCalendarInputData(page.getTag(), cicReference.getModel().getTag(), result);
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
        if (v instanceof ConverserControl) {
            // Ok, lets do this....

            // When a control is clicked, a navigation event or action event occurs. We then send all the queued SwrveEvents which have been queued for this page
            commitUserInputsToEvents();

            if (v instanceof Button) {
                ConversationReply reply = new ConversationReply();
                Button convButton = (Button) v;
                ButtonControl model = convButton.getModel();
                if (((ConverserControl) v).getModel().hasActions()) {
                    ActionBehaviours behaviours = new ActionBehaviours(this.getActivity(), this.getActivity().getApplicationContext()) {
                    };
                    ControlActions actions = ((ConverserControl) v).getModel().getActions();
                    if (actions.isCall()) {
                        sendReply(model, reply);
                        sendCallActionEvent(page.getTag(), model);
                        behaviours.openDialer(actions.getCallUri(), this.getActivity());
                    } else if (actions.isVisit()) {
                        HashMap<String, String> visitUriDetails = (HashMap<String, String>) actions.getVisitDetails();
                        String urlStr = visitUriDetails.get(ControlActions.VISIT_URL_URI_KEY);
                        String referrer = visitUriDetails.get(ControlActions.VISIT_URL_REFERER_KEY);
                        String ext = visitUriDetails.get(ControlActions.VISIT_URL_EXTERNAL_KEY);
                        Uri uri = Uri.parse(urlStr);

                        if (Boolean.parseBoolean(ext) == true) {
                            sendReply(model, reply);
                            sendLinkActionEvent(page.getTag(), model);
                            behaviours.openIntentWebView(uri, this.getActivity(), referrer);
                        } else if (Boolean.parseBoolean(ext) == false) {
                            enforceValidations();
                            sendLinkActionEvent(page.getTag(), model);
                            behaviours.openPopupWebView(uri, this.getActivity(), referrer, "Back to Conversation");
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
        Log.i(LOG_TAG, "Commiting all stashed events");
        String currentPage = page.getTag();
        ArrayList<ConverserInputResult> userInputEvents = new ArrayList<>();
        for (String k : userInteractionData.keySet()) {
            ConverserInputResult r = userInteractionData.get(k);
            userInputEvents.add(r);
        }
        controller.conversationEventsCommitedByUser(swrveConversation, userInputEvents);
    }

    /**
     * Kick off sending reply. The input tree will be traversed and responses gathered. If additional data needs to be included, include in the reply before passing in.
     *
     * @param control
     * @param reply
     */
    private void sendReply(ControlBase control, ConversationReply reply) {

        reply.setControl(control.getTag());

        // For all the inputs , get their data
        for (ConverserInput inputView : inputs) {
            inputView.onReplyDataRequired(reply.getData());
        }

        ConversationPage nextPage = swrveConversation.getPageForControl(control);

        enforceValidations();

        if (nextPage != null) {
            if (isOkToProceed()) {
                sendTransitionPageEvent(page.getTag(), control.getTarget(), control.getTag());
                openConversationOnPage(nextPage);
            }
        }
        // If the button is an action event then it will leave the conversations
        else if (control.hasActions()) {
            Log.i(LOG_TAG, "User has selected an Action. They are now finished the conversation");
            if (isOkToProceed()) {
                sendDoneNavigationEvent(page.getTag());
                getActivity().finish();
            }
        } else {
            Log.e(LOG_TAG, "No more pages in this conversation. This is not normal and the conversation will end prematurely");
            if (isOkToProceed()) {
                sendErrorNavigationEvent(page.getTag(), null); // No exception. We just couldn't find a page
                getActivity().finish();
            }
        }
    }

    public void onBackPressed() {
        sendCancelNavigationEvent(page.getTag());
        commitUserInputsToEvents();
    }

    private void enforceValidations() {
        ArrayList<String> validationErrors = new ArrayList<String>();

        // First, validate
        for (ConverserInput inputView : inputs) {
            String answer = inputView.validate();
            if (answer != null) {
                validationErrors.add(answer);
            }
        }

        if (validationErrors.size() > 0) {
            userInputValid = false;
            validationDialog = ValidationDialog.create("Please fill out all of the items on this page before continuing");
            validationDialog.show(getFragmentManager(), "validation_dialog");
            return;
        } else {
            userInputValid = true;
            return;
        }
    }

    private class DoneButtonListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            enforceValidations();
            if (isOkToProceed()) {
                Activity act = getActivity();
                sendDoneNavigationEvent(page.getTag());
                commitUserInputsToEvents();
                act.finish();
            }
        }
    }

    private boolean isOkToProceed() {
        return userInputValid == true;
    }

    // Events
    private void sendStartNavigationEvent(String startPageTag) {
        if (controller != null) {
            controller.conversationWasStartedByUser(swrveConversation, startPageTag);
        }
    }

    private void sendDoneNavigationEvent(String endPageTag) {
        if (controller != null) {
            controller.conversationWasFinishedByUser(swrveConversation, endPageTag);
        }
    }

    private void sendCancelNavigationEvent(String currentPageTag) {
        if (controller != null) {
            controller.conversationWasCancelledByUser(swrveConversation, currentPageTag);
        }
    }

    private void sendErrorNavigationEvent(String currentPageTag, Exception e) {
        if (controller != null) {
            controller.conversationEncounteredError(swrveConversation, currentPageTag, e);
        }
    }

    private void sendTransitionPageEvent(String currentPageTag, String targetPageTag, String controlTag) {
        if (controller != null) {
            controller.conversationTransitionedToOtherPage(swrveConversation, currentPageTag, targetPageTag, controlTag);
        }
    }

    private void sendLinkActionEvent(String currentPageTag, ConversationAtom control) {
        if (controller != null) {
            controller.conversationLinkActionCalledByUser(swrveConversation, currentPageTag, control.getTag());
        }
    }


    private void sendCallActionEvent(String currentPageTag, ConversationAtom control) {
        if (controller != null) {
            controller.conversationCallActionCalledByUser(swrveConversation, currentPageTag, control.getTag());
        }
    }

    // For each of the content portions we store data about them which is then committed at a later point
    private void stashVideoViewed(String pageTag, String fragmentTag, HtmlVideoView v) {
        // TODO: Is there any data we can record about clicked video views?
        String key = pageTag + "-" + fragmentTag;
        String type = "play";
        ConverserInputResult result = new ConverserInputResult();
        result.type = type;
        result.result = "";
        userInteractionData.put(key, result);
    }

    private void stashMultiChoiceInputData(String pageTag, String fragmentTag, HashMap<String, Object> data) {
        String key = pageTag + "-" + fragmentTag;
        String type = "choice";
        for (String k : data.keySet()) {
            ConverserInputResult result = new ConverserInputResult();
            result.type = type;
            result.result = data.get(k);
            userInteractionData.put(key, result);
        }
    }

    private void stashMultiChoiceLongInputData(String pageTag, String fragmentTag, HashMap<String, Object> data) {
        String key = pageTag + "-" + fragmentTag;
        String type = "multi-choice";
        for (String k : data.keySet()) {
            ConverserInputResult result = new ConverserInputResult();
            result.type = type;
            result.result = data.get(k);
            userInteractionData.put(key, result);
        }
    }

    private void stashEditTextControlInputData(String pageTag, String fragmentTag, HashMap<String, Object> data) {
        String key = pageTag + "-" + fragmentTag;
        String type = "text";
        for (String k : data.keySet()) {
            ConverserInputResult result = new ConverserInputResult();
            result.type = type;
            result.result = data.get(k);
            userInteractionData.put(key, result);
        }
    }

    private void stashCalendarInputData(String pageTag, String fragmentTag, HashMap<String, Object> data) {
        String key = pageTag + "-" + fragmentTag;
        String type = "calendar";
        for (String k : data.keySet()) {
            ConverserInputResult result = new ConverserInputResult();
            result.type = type;
            result.result = data.get(k);
            userInteractionData.put(key, result);
        }
    }

    private void stashNPSInputData(String pageTag, String fragmentTag, HashMap<String, Object> data) {
        String key = pageTag + "-" + fragmentTag;
        String type = "nps";
        for (String k : data.keySet()) {
            ConverserInputResult result = new ConverserInputResult();
            result.type = type;
            result.result = data.get(k);
            userInteractionData.put(key, result);
        }
    }
}