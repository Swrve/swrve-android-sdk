package com.swrve.sdk.messaging;

import android.graphics.Color;
import android.graphics.Point;

import com.swrve.sdk.SwrveHelper;
import com.swrve.sdk.SwrveLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * In-app message format with a given language, size and orientation.
 */
public class SwrveMessageFormat {

    private String name;
    private float scale;
    private Point size;
    private SwrveOrientation orientation;
    private Integer backgroundColor;
    private Map<Long, SwrveMessagePage> pages;
    private ArrayList<Long> pagesOrdered;
    private List<Integer> pageDurations;
    private SwrveMessage message;
    private SwrveCalibration calibration;
    private SwrveStorySettings storySettings;
    private long firstPageId;

    public SwrveMessageFormat(SwrveMessage message, JSONObject messageFormatData) throws JSONException {
        this.message = message;
        this.scale = 1f;
        this.name = messageFormatData.getString("name");
        JSONObject sizeData = messageFormatData.getJSONObject("size");
        this.size = new Point(sizeData.getJSONObject("w").getInt("value"), sizeData.getJSONObject("h").getInt("value"));
        SwrveLogger.i("Format name:%s size.x:%s size.y:%s scale:%s", name, size.x, size.y, scale);

        if (messageFormatData.has("orientation")) {
            this.orientation = SwrveOrientation.parse(messageFormatData.getString("orientation"));
        }

        if (messageFormatData.has("scale")) {
            this.scale = Float.parseFloat(messageFormatData.getString("scale"));
        }

        if (messageFormatData.has("color")) { // Background color
            String strColor = messageFormatData.getString("color");
            if (!SwrveHelper.isNullOrEmpty(strColor)) {
                this.backgroundColor = Color.parseColor("#" + strColor);
            }
        }

        this.pages = new HashMap<>();
        this.pagesOrdered = new ArrayList<>();
        this.pageDurations = new ArrayList<>();
        if (messageFormatData.has("pages")) {

            JSONArray jsonPages = messageFormatData.getJSONArray("pages");
            for (int i = 0; i < jsonPages.length(); i++) {
                JSONObject pageData = jsonPages.getJSONObject(i);
                SwrveMessagePage page = new SwrveMessagePage(message, pageData);
                pages.put(page.getPageId(), page);
                pagesOrdered.add(page.getPageId());
                if(page.getPageDuration() > 0) {
                    pageDurations.add(page.getPageDuration());
                }
                if (i == 0) {
                    firstPageId = page.getPageId(); // the first page is the first element in the array
                }
            }
        } else if (messageFormatData.has("buttons") && messageFormatData.has("images")) {
            // for backward compatibility, convert old IAM's into a single page Map
            SwrveMessagePage page = new SwrveMessagePage(message, messageFormatData);
            pages.put(0l, page);
            firstPageId = 0;
        }

        if (messageFormatData.has("calibration")) {
            this.calibration = new SwrveCalibration(messageFormatData.getJSONObject("calibration"));
        }

        if (messageFormatData.has("story_settings")) {
            this.storySettings = new SwrveStorySettings(messageFormatData.getJSONObject("story_settings"));
        }
    }

    public SwrveMessage getMessage() {
        return message;
    }

    public Map<Long, SwrveMessagePage> getPages() {
        return pages;
    }

    public Point getSize() {
        return size;
    }

    public SwrveOrientation getOrientation() {
        return orientation;
    }

    public float getScale() {
        return scale;
    }

    public Integer getBackgroundColor() {
        return backgroundColor;
    }

    public SwrveCalibration getCalibration() {
        return calibration;
    }

    public long getFirstPageId() {
        return firstPageId;
    }

    public long getPageIdAtIndex(int index) {
        if(pagesOrdered.size() > index){
            return pagesOrdered.get(index);
        }
        return 0l;
    }

    public int getIndexForPageId(long pageId) {
        return pagesOrdered.indexOf(pageId);
    }

    public String getName() {
        return name;
    }

    public SwrveStorySettings getStorySettings() {
        return storySettings;
    }

    public List<Integer> getPageDurations() {
        return pageDurations;
    }
}
