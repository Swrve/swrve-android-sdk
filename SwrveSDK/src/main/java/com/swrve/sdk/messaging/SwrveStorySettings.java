package com.swrve.sdk.messaging;

import androidx.annotation.VisibleForTesting;

import org.json.JSONException;
import org.json.JSONObject;

public class SwrveStorySettings {

    public enum LastPageProgression {
        DISMISS, STOP, LOOP;

        public static LastPageProgression parse(String lastPageProgression) {
            if (lastPageProgression.equalsIgnoreCase("dismiss")) {
                return DISMISS;
            } else if (lastPageProgression.equalsIgnoreCase("stop")) {
                return STOP;
            } else if (lastPageProgression.equalsIgnoreCase("loop")) {
                return LOOP;
            }
            return STOP;
        }
    }

    private int pageDuration; // milliseconds
    private LastPageProgression lastPageProgression;
    private int topPadding;
    private int rightPadding;
    private int bottomPadding;
    private int leftPadding;
    private String barColor;
    private String barBgColor;
    private int barHeight;
    private int segmentGap;
    private boolean gesturesEnabled;
    private SwrveStoryDismissButton dismissButton;
    private long lastPageDismissId;
    private String lastPageDismissName;

    public SwrveStorySettings(JSONObject jsonObject) throws JSONException {

        if (jsonObject == null) {
            return;
        }

        this.pageDuration = jsonObject.getInt("page_duration");
        this.lastPageProgression = LastPageProgression.parse(jsonObject.getString("last_page_progression"));
        if (jsonObject.has("last_page_dismiss_id")) {
            this.lastPageDismissId = jsonObject.getLong("last_page_dismiss_id");
        }
        if (jsonObject.has("last_page_dismiss_name")) {
            this.lastPageDismissName = jsonObject.getString("last_page_dismiss_name");
        }

        JSONObject padding = jsonObject.getJSONObject("padding");
        this.topPadding = padding.getInt("top");
        this.bottomPadding = padding.getInt("bottom");
        this.rightPadding = padding.getInt("right");
        this.leftPadding = padding.getInt("left");

        JSONObject progressBar = jsonObject.getJSONObject("progress_bar");
        this.barColor = progressBar.getString("bar_color");
        this.barBgColor = progressBar.getString("bg_color");
        this.barHeight = progressBar.getInt("h");
        this.segmentGap = progressBar.getInt("segment_gap");

        // future proofing ability to disable gestures
        if (jsonObject.has("gestures_enabled") && !jsonObject.isNull("gestures_enabled")) {
            this.gesturesEnabled = jsonObject.getBoolean("gestures_enabled");
        } else {
            this.gesturesEnabled = true;
        }

        if (jsonObject.has("dismiss_button") && !jsonObject.isNull("dismiss_button")) {
            this.dismissButton = new SwrveStoryDismissButton(jsonObject.getJSONObject("dismiss_button"));
        }
    }

    public int getPageDuration() {
        return pageDuration;
    }

    public long getLastPageDismissId() {
        return lastPageDismissId;
    }
    public String getLastPageDismissName() {
        return lastPageDismissName;
    }
    public LastPageProgression getLastPageProgression() {
        return lastPageProgression;
    }
    @VisibleForTesting
    public void setLastPageProgression(LastPageProgression lastPageProgression) {
        this.lastPageProgression = lastPageProgression;
    }

    public int getTopPadding() {
        return topPadding;
    }

    public int getRightPadding() {
        return rightPadding;
    }

    public int getBottomPadding() {
        return bottomPadding;
    }

    public int getLeftPadding() {
        return leftPadding;
    }

    public String getBarColor() {
        return barColor;
    }

    public String getBarBgColor() {
        return barBgColor;
    }

    public int getBarHeight() {
        return barHeight;
    }

    public int getSegmentGap() {
        return segmentGap;
    }

    public boolean isGesturesEnabled() {
        return gesturesEnabled;
    }

    public SwrveStoryDismissButton getDismissButton() {
        return dismissButton;
    }
}
