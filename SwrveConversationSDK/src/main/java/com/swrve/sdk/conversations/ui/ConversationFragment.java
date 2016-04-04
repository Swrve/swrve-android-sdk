package com.swrve.sdk.conversations.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.swrve.sdk.SwrveBaseConversation;
import com.swrve.sdk.SwrveConversationEventHelper;
import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveIntentHelper;
import com.swrve.sdk.SwrveLogger;
import com.swrve.sdk.conversations.R;
import com.swrve.sdk.conversations.engine.model.ButtonControl;
import com.swrve.sdk.conversations.engine.model.Content;
import com.swrve.sdk.conversations.engine.model.ControlActions;
import com.swrve.sdk.conversations.engine.model.ControlBase;
import com.swrve.sdk.conversations.engine.model.ConversationAtom;
import com.swrve.sdk.conversations.engine.model.ConversationInputChangedListener;
import com.swrve.sdk.conversations.engine.model.ConversationPage;
import com.swrve.sdk.conversations.engine.model.ConversationReply;
import com.swrve.sdk.conversations.engine.model.MultiValueInput;
import com.swrve.sdk.conversations.engine.model.StarRating;
import com.swrve.sdk.conversations.engine.model.UserInputResult;
import com.swrve.sdk.conversations.engine.model.styles.AtomStyle;
import com.swrve.sdk.conversations.engine.model.styles.BackgroundStyle;
import com.swrve.sdk.conversations.ui.video.WebVideoViewBase;
import com.swrve.sdk.conversations.ui.video.YoutubeVideoView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConversationFragment extends Fragment implements OnClickListener, ConversationInputChangedListener {
    private static final String LOG_TAG = "SwrveSDK";

    private ViewGroup root;
    private LinearLayout contentLayout;
    private LinearLayout controlLayout;
    private ConversationFullScreenVideoFrame fullScreenFrame;
    private LayoutParams controlLp;
    private SwrveBaseConversation swrveConversation;
    private ConversationPage page;
    private SwrveConversationEventHelper eventHelper;
    private HashMap<String, UserInputResult> userInteractionData;

    public ConversationPage getPage() {
        return page;
    }

    public void setPage(ConversationPage page) {
        this.page = page;
    }

    public HashMap<String, UserInputResult> getUserInteractionData() {
        return userInteractionData;
    }

    public void setUserInteractionData(HashMap<String, UserInputResult> userInteractionData) {
        this.userInteractionData = userInteractionData;
    }

    public static ConversationFragment create(SwrveBaseConversation swrveConversation) {
        ConversationFragment f = new ConversationFragment();
        f.swrveConversation = swrveConversation;
        f.eventHelper = new SwrveConversationEventHelper();
        return f;
    }

    public void commitConversationFragment(FragmentManager manager) {
        FragmentTransaction ft = manager.beginTransaction();
        ft.replace(android.R.id.content, this, "conversation");
        ft.commit();
    }

    @Override
    public void onResume() {
        super.onResume();
        userInteractionData = (userInteractionData == null) ? new HashMap<String, UserInputResult>() : userInteractionData;

        if (page != null) {
            View currentView = getView();
            openConversationOnPage(page);
            // Populate the page with existing inputs and answers
            for (String key : userInteractionData.keySet()) {
                UserInputResult userInput = userInteractionData.get(key);
                String fragmentTag = userInput.getFragmentTag();
                View inputView = currentView.findViewWithTag(fragmentTag);
                if (inputView instanceof IConversationInput) {
                    IConversationInput inputControl = (IConversationInput) inputView;
                    inputControl.setUserInput(userInput);
                }
            }
        } else {
            openFirstPage();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.swrve__conversation_fragment, container, false);
    }


    public void openFirstPage() {
        page = swrveConversation.getFirstPage();
        sendStartNavigationEvent(page.getTag());
        openConversationOnPage(page);
    }

    public void openConversationOnPage(ConversationPage conversationPage) {
        Activity activity = getActivity();
        if (!isAdded() || activity == null) {
            return;
        }

        root = (ViewGroup) getView();
        if (root == null) {
            return;
        }

        this.page = conversationPage;

        activity.setTitle(page.getTitle());
        try {
            initLayout();
            renderControls(activity);
            renderContent(activity);
        } catch (Exception e) {
            SwrveLogger.e(LOG_TAG, "Error rendering conversation page. Exiting conversation.", e);
            sendErrorNavigationEvent(page.getTag(), e);
            if (activity != null) {
                activity.finish();
            }
            return;
        }

        sendPageImpressionEvent(page.getTag());
        root.requestFocus();
    }

    protected int getSDKBuildVersion() {
        return Build.VERSION.SDK_INT;
    }

    private void initLayout() {
        contentLayout = (LinearLayout) root.findViewById(R.id.swrve__content);
        controlLayout = (LinearLayout) root.findViewById(R.id.swrve__controls);
        fullScreenFrame = (ConversationFullScreenVideoFrame) root.findViewById(R.id.swrve__full_screen);

        if (contentLayout.getChildCount() > 0) {
            contentLayout.removeAllViews();
        }

        if (controlLayout.getChildCount() > 0) {
            controlLayout.removeAllViews();
        }

        if (getSDKBuildVersion() >= Build.VERSION_CODES.KITKAT) {
            controlLp = new LayoutParams(root.getLayoutParams());
        } else {
            controlLp = new LayoutParams(root.getLayoutParams().width, root.getLayoutParams().height);
        }
        controlLp.height = LayoutParams.WRAP_CONTENT;

        // Set the background from whatever color the page object specifies as well as the control tray down the bottom
        setBackgroundDrawable(contentLayout, page.getBackground());
        setBackgroundDrawable(controlLayout, page.getBackground());
    }

    @SuppressLint("NewApi")
    private void renderControls(Activity activity) {
        TypedArray margins = activity.getTheme().obtainStyledAttributes(new int[]{R.attr.conversationControlLayoutMargin});
        int controlLayoutMarginInPixels = margins.getDimensionPixelSize(0, 0);

        int numControls = page.getControls().size();
        for (int i = 0; i < numControls; i++) {
            ButtonControl ctrl = page.getControls().get(i);
            ConversationButton ctrlConversationButton = new ConversationButton(activity, ctrl, R.attr.conversationControlButtonStyle);
            LayoutParams buttonLP;
            if (getSDKBuildVersion() >= Build.VERSION_CODES.KITKAT) {
                buttonLP = new LayoutParams(controlLp);
            } else {
                buttonLP = new LayoutParams(controlLp.width, controlLp.height);
            }
            buttonLP.weight = 1;
            buttonLP.leftMargin = controlLayoutMarginInPixels;
            buttonLP.rightMargin = controlLayoutMarginInPixels;
            buttonLP.topMargin = controlLayoutMarginInPixels;
            buttonLP.bottomMargin = controlLayoutMarginInPixels;

            ctrlConversationButton.setLayoutParams(buttonLP);
            controlLayout.addView(ctrlConversationButton);
            ctrlConversationButton.setOnClickListener(this);
        }
    }

    private void renderContent(Activity activity) {
        for (ConversationAtom content : page.getContent()) {
            AtomStyle atomStyle = content.getStyle();
            BackgroundStyle atomBg = atomStyle.getBg();

            if (content instanceof Content) {
                Content modelContent = (Content) content;
                String modelType = modelContent.getType().toString();
                if (modelType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_IMAGE)) {
                    ConversationImageView iv = new ConversationImageView(activity, modelContent);
                    String filePath = swrveConversation.getCacheDir().getAbsolutePath() + "/" + modelContent.getValue();
                    if(SwrveHelper.hasFileAccess(filePath)) {
                        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                        iv.setTag(content.getTag());
                        iv.setImageBitmap(bitmap);
                        iv.setAdjustViewBounds(true);
                        iv.setScaleType(ScaleType.FIT_CENTER);
                        setBackgroundDrawable(iv, atomBg.getPrimaryDrawable());
                        contentLayout.addView(iv);
                    } else {
                        SwrveLogger.e(LOG_TAG, "Could not render conversation asset image because there is no read access to:" + filePath);
                    }
                } else if (modelType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_HTML)) {
                    HtmlSnippetView view = new HtmlSnippetView(activity, modelContent);
                    view.setTag(content.getTag());
                    view.setLayoutParams(getContentLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    view.setBackgroundColor(Color.TRANSPARENT);
                    setBackgroundDrawable(view, atomBg.getPrimaryDrawable());
                    contentLayout.addView(view);
                } else if (modelType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_VIDEO)) {
                    YoutubeVideoView view = new YoutubeVideoView(activity, modelContent, fullScreenFrame);
                    view.setTag(content.getTag());
                    view.setBackgroundColor(Color.TRANSPARENT);
                    setBackgroundDrawable(view, atomBg.getPrimaryDrawable());
                    view.setLayoutParams(getContentLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    // Let the eventListener know that something has happened to the video
                    final YoutubeVideoView cloneView = view;
                    final String tag = content.getTag();
                    view.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            // Due the fact that we render video in HTML, its very difficult to detect when a video has started/stopped  playing. For now all we can say is that the video was touched. Note that on click listeners behave strange with WebViews
                            stashVideoViewed(page.getTag(), tag, cloneView);
                            return false;
                        }
                    });
                    contentLayout.addView(view);
                } else if (modelType.equalsIgnoreCase(ConversationAtom.TYPE_CONTENT_SPACER)) {
                    View view = new View(activity);
                    view.setTag(content.getTag());
                    view.setBackgroundColor(Color.TRANSPARENT);
                    setBackgroundDrawable(view, atomBg.getPrimaryDrawable());
                    int heightPixels = Integer.parseInt(((Content) content).getHeight());
                    view.setLayoutParams(getContentLayoutParams(LayoutParams.MATCH_PARENT, heightPixels));
                    contentLayout.addView(view);
                }
            } else if (content instanceof MultiValueInput) {
                MultiValueInputControl input = MultiValueInputControl.inflate(activity, contentLayout, (MultiValueInput) content);
                input.setLayoutParams(getContentLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                input.setTag(content.getTag());
                input.setContentChangedListener(this);
                setBackgroundDrawable(input, atomBg.getPrimaryDrawable());
                input.setTextColor(atomStyle.getTextColorInt());
                contentLayout.addView(input);
            } else if (content instanceof StarRating) {
                ConversationRatingBar conversationRatingBar = new ConversationRatingBar(activity, (StarRating)content);
                conversationRatingBar.setContentChangedListener(this);
                contentLayout.addView(conversationRatingBar);
            }
        }
    }

    @SuppressLint("NewApi")
    private LayoutParams getContentLayoutParams(int width, int height) {
        LayoutParams layoutParams;
        if (getSDKBuildVersion() >= Build.VERSION_CODES.KITKAT) {
            layoutParams = new LayoutParams(controlLp);
        } else {
            layoutParams = new LayoutParams(controlLp.width, controlLp.height);
        }
        layoutParams.width = width;
        layoutParams.height = height;
        return layoutParams;
    }

    @Override
    public void onClick(View v) {
        Activity activity = getActivity();
        if (!isAdded() || activity == null) {
            return;
        }

        if (v instanceof IConversationControl) {
            // When a control is clicked, a navigation event or action event occurs. We then send all the queued SwrveEvents which have been queued for this page
            commitUserInputsToEvents();

            if (v instanceof ConversationButton) {
                ConversationReply reply = new ConversationReply();
                ConversationButton convButton = (ConversationButton) v;
                ButtonControl model = convButton.getModel();
                if (((IConversationControl) v).getModel().hasActions()) {
                    ControlActions actions = ((IConversationControl) v).getModel().getActions();
                    if (actions.isCall()) {
                        sendReply(model, reply);
                        sendCallActionEvent(page.getTag(), model);
                        SwrveIntentHelper.openDialer(actions.getCallUri(), activity);
                    } else if (actions.isVisit()) {
                        HashMap<String, String> visitUriDetails = (HashMap<String, String>) actions.getVisitDetails();
                        String urlStr = visitUriDetails.get(ControlActions.VISIT_URL_URI_KEY);
                        String referrer = visitUriDetails.get(ControlActions.VISIT_URL_REFERER_KEY);
                        Uri uri = Uri.parse(urlStr);
                        sendReply(model, reply);
                        sendLinkVisitActionEvent(page.getTag(), model);
                        SwrveIntentHelper.openIntentWebView(uri, activity, referrer);
                    } else if (actions.isDeepLink()) {
                        HashMap<String, String> visitUriDetails = (HashMap<String, String>) actions.getDeepLinkDetails();
                        String urlStr = visitUriDetails.get(ControlActions.DEEPLINK_URL_URI_KEY);
                        sendReply(model, reply);
                        sendDeepLinkActionEvent(page.getTag(), model);
                        SwrveIntentHelper.openDeepLink(activity, urlStr);
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
    public void commitUserInputsToEvents() {
        SwrveLogger.i(LOG_TAG, "Commiting all stashed events");
        ArrayList<UserInputResult> userInputEvents = new ArrayList<UserInputResult>();
        for (String k : userInteractionData.keySet()) {
            UserInputResult r = userInteractionData.get(k);
            userInputEvents.add(r);
        }
        eventHelper.conversationEventsCommitedByUser(swrveConversation, userInputEvents);
        userInteractionData.clear(); // Remove all events stored locally so that they don't get resubmitted during another commit.
    }

    /**
     * Kick off sending reply. The input tree will be traversed and responses gathered. If additional data needs to be included, include in the reply before passing in.
     *
     * @param control
     * @param reply
     */
    private void sendReply(ControlBase control, ConversationReply reply) {
        reply.setControl(control.getTag());

        ConversationPage nextPage = swrveConversation.getPageForControl(control);
        if (nextPage != null) {
            sendTransitionPageEvent(page.getTag(), control.getTarget(), control.getTag());
            openConversationOnPage(nextPage);
        } else if (control.hasActions()) {
            SwrveLogger.i(LOG_TAG, "User has selected an Action. They are now finished the conversation");
            sendDoneNavigationEvent(page.getTag(), control.getTag());
            Activity activity = getActivity();
            if (isAdded() && activity != null) {
                activity.finish();
            }
        } else {
            SwrveLogger.i(LOG_TAG, "No more pages in this conversation");
            sendDoneNavigationEvent(page.getTag(), control.getTag()); // No exception. We just couldn't find a page attached to the control.
            Activity activity = getActivity();
            if (isAdded() && activity != null) {
                activity.finish();
            }
        }
    }

    public boolean onBackPressed() {
        if (fullScreenFrame.getVisibility() != View.GONE) {
            fullScreenFrame.disableFullScreen();
            return false;
        } else {
            sendCancelNavigationEvent(page.getTag());
            commitUserInputsToEvents();
        }
        return true;
    }

    // Events
    private void sendPageImpressionEvent(String pageTag) {
        eventHelper.conversationPageWasViewedByUser(swrveConversation, pageTag);
    }

    private void sendStartNavigationEvent(String startPageTag) {
        eventHelper.conversationWasStartedByUser(swrveConversation, startPageTag);
    }

    private void sendDoneNavigationEvent(String endPageTag, String endControlTag) {
        eventHelper.conversationWasFinishedByUser(swrveConversation, endPageTag, endControlTag);
    }

    private void sendCancelNavigationEvent(String currentPageTag) {
        eventHelper.conversationWasCancelledByUser(swrveConversation, currentPageTag);
    }

    private void sendErrorNavigationEvent(String currentPageTag, Exception e) {
        eventHelper.conversationEncounteredError(swrveConversation, currentPageTag, e);
    }

    private void sendTransitionPageEvent(String currentPageTag, String targetPageTag, String controlTag) {
        eventHelper.conversationTransitionedToOtherPage(swrveConversation, currentPageTag, targetPageTag, controlTag);
    }

    private void sendLinkVisitActionEvent(String currentPageTag, ConversationAtom control) {
        eventHelper.conversationLinkVisitActionCalledByUser(swrveConversation, currentPageTag, control.getTag());
    }

    private void sendDeepLinkActionEvent(String currentPageTag, ConversationAtom control) {
        eventHelper.conversationDeeplinkActionCalledByUser(swrveConversation, currentPageTag, control.getTag());
    }

    private void sendCallActionEvent(String currentPageTag, ConversationAtom control) {
        eventHelper.conversationCallActionCalledByUser(swrveConversation, currentPageTag, control.getTag());
    }

    // For each of the content portions we store data about them which is then committed at a later point
    private void stashVideoViewed(String pageTag, String fragmentTag, WebVideoViewBase v) {
        String key = pageTag + "-" + fragmentTag;
        String type = UserInputResult.TYPE_VIDEO_PLAY;
        UserInputResult result = new UserInputResult();
        result.type = type;
        result.conversationId = swrveConversation.getId();
        result.fragmentTag = fragmentTag;
        result.pageTag = pageTag;
        result.result = "";
        userInteractionData.put(key, result);
    }

    private void stashMultiChoiceInputData(String pageTag, String fragmentTag, Map<String, Object> data) {
        String key = pageTag + "-" + fragmentTag;
        String type = UserInputResult.TYPE_SINGLE_CHOICE;
        for (String k : data.keySet()) {
            UserInputResult result = new UserInputResult();
            result.type = type;
            result.conversationId = swrveConversation.getId();
            result.fragmentTag = fragmentTag;
            result.pageTag = pageTag;
            result.result = data.get(k);
            userInteractionData.put(key, result);
        }
    }

    private void stashStarRatingInputData(String pageTag, String contentTag, Map<String, Object> data) {
        String key = pageTag + "-" + contentTag;
        for (String k : data.keySet()) {
            UserInputResult result = new UserInputResult();
            result.type = UserInputResult.TYPE_STAR_RATING;
            result.conversationId = swrveConversation.getId();
            result.fragmentTag = contentTag;
            result.pageTag = pageTag;
            result.result = data.get(k);
            userInteractionData.put(key, result);
        }
    }

    @SuppressLint("NewApi")
    private void setBackgroundDrawable(View view, Drawable drawable) {
        if (getSDKBuildVersion() < Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackgroundDrawable(drawable);
        } else {
            view.setBackground(drawable);
        }
    }

    @Override
    public void onContentChanged(Map<String, Object> contentChanged, ConversationAtom content) {
        if(content instanceof MultiValueInput) {
            stashMultiChoiceInputData(page.getTag(), content.getTag(), contentChanged);
        } else if(content instanceof StarRating) {
            stashStarRatingInputData(page.getTag(), content.getTag(), contentChanged);
        }
    }
}
