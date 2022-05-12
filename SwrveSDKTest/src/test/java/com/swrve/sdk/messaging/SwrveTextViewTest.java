package com.swrve.sdk.messaging;

import static androidx.core.widget.TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE;
import static androidx.core.widget.TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewTreeObserver;

import com.swrve.sdk.SwrveBaseTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicBoolean;

public class SwrveTextViewTest extends SwrveBaseTest {

    @Test
    public void testSwrveTextViewNullCalibration() {
        SwrveTextViewStyle textViewStyle = new SwrveTextViewStyle.Builder()
                .fontSize(15.0f)
                .isScrollable(true)
                .build();
        SwrveTextView textView = new SwrveTextView(mActivity, "my_test_string", textViewStyle, null);

        assertEquals(15.0, textView.getTextSize(), 0.0);
        assertEquals("my_test_string", textView.getText().toString());
        assertEquals(textView.getAutoSizeTextType(), AUTO_SIZE_TEXT_TYPE_NONE);
        assertEquals(textView.getTextSizeUnit(), TypedValue.COMPLEX_UNIT_SP);
    }

    @Test
    public void testSwrveTextViewAlignLeft() {
        SwrveTextViewStyle textViewStyle = new SwrveTextViewStyle.Builder()
                .horizontalAlignment(SwrveTextViewStyle.TextAlignment.Left)
                .build();
        SwrveTextView textView = Mockito.spy(new SwrveTextView(mActivity));
        textView.init("my_test_string", textViewStyle, null);

        Mockito.verify(textView).setGravity(Gravity.START);
    }

    @Test
    public void testSwrveTextViewAlignRight() {
        SwrveTextViewStyle textViewStyle = new SwrveTextViewStyle.Builder()
                .horizontalAlignment(SwrveTextViewStyle.TextAlignment.Right)
                .build();
        SwrveTextView textView = Mockito.spy(new SwrveTextView(mActivity));
        textView.init("my_test_string", textViewStyle, null);

        Mockito.verify(textView).setGravity(Gravity.END);
    }

    @Test
    public void testSwrveTextViewAlignCenter() {
        SwrveTextViewStyle textViewStyle = new SwrveTextViewStyle.Builder()
                .horizontalAlignment(SwrveTextViewStyle.TextAlignment.Center)
                .build();

        SwrveTextView textView = Mockito.spy(new SwrveTextView(mActivity));
        textView.init("my_test_string", textViewStyle, null);

        Mockito.verify(textView).setGravity(Gravity.CENTER_HORIZONTAL);
    }

    @Test
    public void testSwrveTextView() throws Exception {
        SwrveTextViewStyle textViewStyle = new SwrveTextViewStyle.Builder()
                .textBackgroundColor(Color.RED)
                .textForegroundColor(Color.BLUE)
                .isScrollable(true)
                .fontSize(15.0f)
                .horizontalAlignment(SwrveTextViewStyle.TextAlignment.Center)
                .build();

        SwrveTextView textView = Mockito.spy(new SwrveTextView(mActivity));

        // Robolectric has limited support for testing Paint and Canvas, so mock the following method. Execute a test in Firebase Test Lab for this instead
        doReturn(100.0f).when(textView).getScaledBaseFontSize("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz", 5000, 500);

        // now init
        textView.init("my_test_string", textViewStyle, getDummySwrveCalibration());

        assertEquals(75, textView.getTextSize(), 0.0); // -->  (15 / 20) * 100
        assertEquals("my_test_string", textView.getText().toString());
        assertEquals(textView.getAutoSizeTextType(), AUTO_SIZE_TEXT_TYPE_NONE);
        assertEquals(textView.getTextSizeUnit(), TypedValue.COMPLEX_UNIT_SP);

        assertFalse(textView.isScrollContainer());
        assertTrue(textView.isTextSelectable());
        assertTrue(textView.isFocusable());
        assertTrue(textView.getIncludeFontPadding());

        assertEquals(textView.getCurrentTextColor(), Color.BLUE);
        ColorDrawable cd = (ColorDrawable) textView.getBackground();
        int colorCode = cd.getColor();
        assertEquals(colorCode, Color.RED);
    }

    @Test
    public void testSwrveTextViewAutoSizeNoScroll() throws Exception {
        SwrveTextViewStyle textViewStyle = new SwrveTextViewStyle.Builder()
                .fontSize(15.0f)
                .isScrollable(false)
                .build();

        SwrveTextView textView = Mockito.spy(new SwrveTextView(mActivity));

        // Robolectric has limited support for testing Paint and Canvas, so mock the following method. Execute a test in Firebase Test Lab for this instead
        // setAutoSizeTextTypeWithDefaults will default to 14, as there is no layout in this test.
        // return a value that will set calibrated text to > than 14 to test logic flow, setting to 20 will return calibratedTextSizePX of 15
        doReturn(20.0f).when(textView).getScaledBaseFontSize("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz", 5000, 500);

        // now init
        textView.init("my_test_string", textViewStyle, getDummySwrveCalibration());

        //setAutoSizeTextTypeWithDefaults will default to 14
        assertEquals(14, textView.getTextSize(), 0.0);
        assertEquals("my_test_string", textView.getText().toString());
        assertEquals(textView.getAutoSizeTextType(), AUTO_SIZE_TEXT_TYPE_UNIFORM);

        //setAutoSizeTextTypeWithDefaults will default to 14, as there is no layout in this test.
        final AtomicBoolean callback = new AtomicBoolean(false);
        textView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // confirm we stick with fitting text
                assertEquals(textView.getAutoSizeTextType(), AUTO_SIZE_TEXT_TYPE_UNIFORM);
                assertEquals(14, textView.getTextSize(), 0.0);
                callback.set(true);
            }
        });

        textView.getViewTreeObserver().dispatchOnGlobalLayout();
        await().untilTrue(callback);
    }

    @Test
    public void testSwrveTextViewAutoSizeFitsNoScroll() throws Exception {
        SwrveTextViewStyle textViewStyle = new SwrveTextViewStyle.Builder()
                .fontSize(15.0f)
                .isScrollable(false)
                .build();

        SwrveTextView textView = Mockito.spy(new SwrveTextView(mActivity));

        // Robolectric has limited support for testing Paint and Canvas, so mock the following method. Execute a test in Firebase Test Lab for this instead
        // setAutoSizeTextTypeWithDefaults will default to 14, as there is no layout in this test.
        // return a value that will set calibrated text to < than 14 to test logic flow, setting to 12 will return calibratedTextSizePX of 9
        doReturn(12.0f).when(textView).getScaledBaseFontSize("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz", 5000, 500);

        // now init
        textView.init("my_test_string", textViewStyle, getDummySwrveCalibration());

        //setAutoSizeTextTypeWithDefaults will default to 14
        assertEquals(14, textView.getTextSize(), 0.0);
        assertEquals("my_test_string", textView.getText().toString());
        assertEquals(textView.getAutoSizeTextType(), AUTO_SIZE_TEXT_TYPE_UNIFORM);

        final AtomicBoolean callback = new AtomicBoolean(false);
        textView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // confirm we switch back to values as text already fits
                assertEquals(textView.getAutoSizeTextType(), AUTO_SIZE_TEXT_TYPE_NONE);
                assertEquals(9, textView.getTextSize(), 0.0);
                assertEquals(textView.getTextSizeUnit(), TypedValue.COMPLEX_UNIT_PX);
                callback.set(true);
            }
        });

        textView.getViewTreeObserver().dispatchOnGlobalLayout();
        await().untilTrue(callback);
    }

    @Test
    public void testSwrveTextViewIsMobileFalse() {
        SwrveTextViewStyle textViewStyle = new SwrveTextViewStyle.Builder()
                .isScrollable(true)
                .build();
        SwrveTextView textView = Mockito.spy(new SwrveTextView(mActivity));
        doReturn(false).when(textView).isMobile();
        textView.init("my_test_string", textViewStyle, null);
        Mockito.verify(textView).addListenerForResizing(0, textViewStyle.getLineHeight());
        Mockito.verify(textView, Mockito.times(0)).setTextSize(TypedValue.COMPLEX_UNIT_SP, 0);
    }

    @Test
    public void testSwrveTextViewIsMobileTrue() {
        SwrveTextViewStyle textViewStyle = new SwrveTextViewStyle.Builder()
                .isScrollable(true)
                .build();
        SwrveTextView textView = Mockito.spy(new SwrveTextView(mActivity));
        doReturn(true).when(textView).isMobile();
        textView.init("my_test_string", textViewStyle, null);
        Mockito.verify(textView).setTextSize(TypedValue.COMPLEX_UNIT_SP, 0);
        Mockito.verify(textView, Mockito.times(0)).addListenerForResizing(0, 1);
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
}
