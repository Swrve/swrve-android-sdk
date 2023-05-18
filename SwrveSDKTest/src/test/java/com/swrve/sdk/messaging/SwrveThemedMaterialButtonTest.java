package com.swrve.sdk.messaging;

import static androidx.core.widget.TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE;
import static androidx.core.widget.TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.TypedValue;

import com.swrve.sdk.SwrveBaseTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class SwrveThemedMaterialButtonTest extends SwrveBaseTest {

    @Test
    public void testPersonalisation() throws Exception {

        Map<String, String> personalization = new HashMap<>();
        personalization.put("test_cp", "test_coupon");

        SwrveThemedMaterialButton buttonSpy = spy(new SwrveThemedMaterialButton(mActivity, com.google.android.material.R.attr.materialButtonOutlinedStyle));
        buttonSpy.swrveTextUtils = getMockSwrveTextUtils(Typeface.defaultFromStyle(Typeface.NORMAL), 100f);
        buttonSpy.calibration = getDummySwrveCalibration();
        buttonSpy.theme = getDummyTheme(15, false);
        buttonSpy.init("my_test_string", personalization);

        verify(buttonSpy, atLeastOnce()).setAction(personalization); // verify its called as part of init

        // call with valid data
        SwrveButton mockSwrveButton = mock(SwrveButton.class);
        doReturn("${test_cp}").when(mockSwrveButton).getAction();
        doReturn(SwrveActionType.Custom).when(mockSwrveButton).getActionType();
        buttonSpy.button = mockSwrveButton;

        buttonSpy.setAction(personalization);
        assertEquals("test_coupon", buttonSpy.getAction());
    }

    @Test
    public void testOnFocusChanged() throws Exception {

        SwrveThemedMaterialButton buttonSpy = spy(new SwrveThemedMaterialButton(mActivity, com.google.android.material.R.attr.materialButtonOutlinedStyle));
        buttonSpy.swrveTextUtils = getMockSwrveTextUtils(Typeface.defaultFromStyle(Typeface.NORMAL), 100f);
        buttonSpy.calibration = getDummySwrveCalibration();
        buttonSpy.theme = getDummyTheme(15, false);
        buttonSpy.init("my_test_string", null);
        buttonSpy.messageFocusListener = mock(SwrveMessageFocusListener.class);

        Rect rect = new Rect();
        buttonSpy.onFocusChanged(true, 100, rect);

        verify(buttonSpy.messageFocusListener, atLeastOnce()).onFocusChanged(buttonSpy, true, 100, rect);
    }

    @Test
    public void testCalibratedTextSize() throws Exception {

        SwrveThemedMaterialButton buttonSpy = spy(new SwrveThemedMaterialButton(mActivity, com.google.android.material.R.attr.materialButtonOutlinedStyle));
        buttonSpy.swrveTextUtils = getMockSwrveTextUtils(Typeface.defaultFromStyle(Typeface.NORMAL), 100f);
        buttonSpy.calibration = getDummySwrveCalibration();
        buttonSpy.theme = getDummyTheme(15, true);
        buttonSpy.init("my_test_string", null);

        assertEquals(75, buttonSpy.getTextSize(), 0.0); // -->  (15 / 20) * 100
    }

    @Test
    public void testCalibratedTextSizeLargerThanAutoSized() throws Exception {

        SwrveThemedMaterialButton buttonSpy = spy(new SwrveThemedMaterialButton(mActivity, com.google.android.material.R.attr.materialButtonOutlinedStyle));

        // setAutoSizeTextTypeWithDefaults will default to 14.
        // return a value that will set calibrated text to > than 14 to test logic flow, setting to 20 will return calibratedTextSizePX of 15
        buttonSpy.swrveTextUtils = getMockSwrveTextUtils(Typeface.defaultFromStyle(Typeface.NORMAL), 20f);
        buttonSpy.calibration = getDummySwrveCalibration();
        buttonSpy.theme = getDummyTheme(15, false);
        buttonSpy.init("my_test_string", null);

        //setAutoSizeTextTypeWithDefaults will default to 14
        assertEquals(14, buttonSpy.getTextSize(), 0.0);
        assertEquals("my_test_string", buttonSpy.getText().toString());
        assertEquals(buttonSpy.getAutoSizeTextType(), AUTO_SIZE_TEXT_TYPE_UNIFORM);

        //setAutoSizeTextTypeWithDefaults will default to 14. Add another GlobalLayoutListener and confirm we stick with autosized text
        final AtomicBoolean callback = new AtomicBoolean(false);
        buttonSpy.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            assertEquals(buttonSpy.getAutoSizeTextType(), AUTO_SIZE_TEXT_TYPE_UNIFORM);
            assertEquals(14, buttonSpy.getTextSize(), 0.0);
            assertEquals(buttonSpy.getTextSizeUnit(), TypedValue.COMPLEX_UNIT_SP);
            callback.set(true);
        });

        buttonSpy.getViewTreeObserver().dispatchOnGlobalLayout();
        await().untilTrue(callback);
    }

    @Test
    public void testCalibratedTextSizeSmallerThanAutoSized() throws Exception {

        SwrveThemedMaterialButton buttonSpy = spy(new SwrveThemedMaterialButton(mActivity, com.google.android.material.R.attr.materialButtonOutlinedStyle));

        // setAutoSizeTextTypeWithDefaults will default to 14.
        // return a value that will set calibrated text to < than 14 to test logic flow, setting to 12 will return calibratedTextSizePX of 9
        buttonSpy.swrveTextUtils = getMockSwrveTextUtils(Typeface.defaultFromStyle(Typeface.NORMAL), 12f);
        buttonSpy.calibration = getDummySwrveCalibration();
        buttonSpy.theme = getDummyTheme(15, false);
        buttonSpy.init("my_test_string", null);

        //setAutoSizeTextTypeWithDefaults will default to 14
        assertEquals(14, buttonSpy.getTextSize(), 0.0);
        assertEquals("my_test_string", buttonSpy.getText().toString());
        assertEquals(buttonSpy.getAutoSizeTextType(), AUTO_SIZE_TEXT_TYPE_UNIFORM);

        //setAutoSizeTextTypeWithDefaults will default to 14. Add another GlobalLayoutListener and confirm we stick with autosized text
        final AtomicBoolean callback = new AtomicBoolean(false);
        buttonSpy.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            assertEquals(buttonSpy.getAutoSizeTextType(), AUTO_SIZE_TEXT_TYPE_NONE); // this has reverted back
            assertEquals(9.0, buttonSpy.getTextSize(), 0.0); // using the calibrated size instead of autosize
            assertEquals(buttonSpy.getTextSizeUnit(), TypedValue.COMPLEX_UNIT_PX);
            callback.set(true);
        });

        buttonSpy.getViewTreeObserver().dispatchOnGlobalLayout();
        await().untilTrue(callback);
    }

    // Robolectric has limited support for testing Paint and Canvas, so mock the getScaledBaseFontSize method.
    private SwrveTextUtils getMockSwrveTextUtils(Typeface typeface, float scaledBaseFontSize) {
        SwrveTextUtils spySwrveTextUtils = spy(new SwrveTextUtils());
        doReturn(scaledBaseFontSize).when(spySwrveTextUtils).getScaledBaseFontSize(typeface, "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz", 5000, 500);
        return spySwrveTextUtils;
    }

    private SwrveCalibration getDummySwrveCalibration() throws JSONException {
// @formatter:off
        String json =
                "{" +
                        "\"width\": 5000," +
                        "\"height\": 500," +
                        "\"base_font_size\": 20," +
                        "\"text\": \"0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz\"" +
                        "}";
// @formatter:on
        JSONObject jsonObject = new JSONObject(json);
        return (new SwrveCalibration(jsonObject));
    }

    private SwrveButtonTheme getDummyTheme(int fontSize, boolean truncate) throws JSONException {
// @formatter:off
        String json =
                "{\n" +
                        "  \"font_size\": " + fontSize + ",\n" +
                        "  \"font_postscript_name\": \"\",\n" +
                        "  \"font_family\": \"\",\n" +
                        "  \"font_style\": \"Regular\",\n" +
                        "  \"font_native_style\": \"NORMAL\",\n" +
                        "  \"font_file\": \"_system_font_\",\n" +
                        "  \"font_digest\": \"\",\n" +
                        "  \"padding\": {\n" +
                        "    \"top\": 0,\n" +
                        "    \"right\": 0,\n" +
                        "    \"bottom\": 0,\n" +
                        "    \"left\": 0\n" +
                        "  },\n" +
                        "  \"font_color\": \"#FF000000\",\n" +
                        "  \"bg_image\": null,\n" +
                        "  \"bg_color\": \"#FFFF0000\",\n" +
                        "  \"border_width\": 4,\n" +
                        "  \"border_color\": \"#FF000000\",\n" +
                        "  \"corner_radius\": 40,\n" +
                        "  \"button_style_preset\": {\n" +
                        "    \"id\": \"121\",\n" +
                        "    \"version\": 0\n" +
                        "  },\n" +
                        "  \"truncate\": " + truncate + ",\n" +
                        "  \"pressed_state\": {\n" +
                        "    \"font_color\": \"#FFFF0000\",\n" +
                        "    \"bg_color\": \"#ffffd700\",\n" +
                        "    \"border_color\": \"#FFFF0000\",\n" +
                        "    \"bg_image\": null\n" +
                        "  },\n" +
                        "  \"h_align\": \"CENTER\"\n" +
                        "}\n";
// @formatter:on
        JSONObject jsonObject = new JSONObject(json);
        return new SwrveButtonTheme(jsonObject);
    }
}
