package io.converser.android.engine.model;

public abstract class ConversationAtom {

    public static final String TYPE_CONTENT_TEXT = "text";
    public static final String TYPE_CONTENT_HTML = "html-fragment";
    public static final String TYPE_CONTENT_IMAGE = "image";
    public static final String TYPE_CONTENT_VIDEO = "video";
    public static final String TYPE_CONTENT_AUDIO = "audio";

    public static final String TYPE_CONTROL_DATESAVER = "date-saver";
    public static final String TYPE_CONTROL_DATECHOICE = "date-choice";
    public static final String TYPE_CONTROL_CALL = "call";
    public static final String TYPE_CONTROL_VISIT_URL = "visit";

    public static final String TYPE_ACTION_VISIT_URL = "url";
    public static final String TYPE_ACTION_VISIT_REFER = "refer";
    public static final String TYPE_ACTION_VISIT_EXT = "ext";

    public static final String TYPE_INPUT_TEXTINPUT = "text-input";
    public static final String TYPE_INPUT_SLIDER = "slider-input";
    public static final String TYPE_INPUT_MULTIVALUE = "multi-value-input";
    public static final String TYPE_INPUT_MULTIVALUELONG = "multi-value-long-input";
    public static final String TYPE_INPUT_REACTION = "reaction";
    public static final String TYPE_INPUT_VISUAL = "visual";
    public static final String TYPE_INPUT_AUDIO = "audio";
    public static final String TYPE_INPUT_SWITCH = "onoff";
    public static final String TYPE_INPUT_DATECHOOSER = "date-chooser";
    public static final String TYPE_INPUT_NETPROMOTER = "nps-input";
    public static final String TYPE_INPUT_CALENDAR_INPUT = "calendar-input";


    protected String tag;
    protected String type;

    public static ConversationAtom create(String tag, String type) {
        BareConversationAtom bca = new BareConversationAtom();
        bca.tag = tag;
        bca.type = type;

        return bca;
    }

    public String getTag() {
        return tag;
    }

    public String getType() {
        return type;
    }

    private static class BareConversationAtom extends ConversationAtom {

    }
}
