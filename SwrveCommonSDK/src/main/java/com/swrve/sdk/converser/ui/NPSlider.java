package com.swrve.sdk.converser.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.swrve.sdk.common.R;
import com.swrve.sdk.converser.engine.model.NPSInput;
import com.swrve.sdk.converser.engine.model.OnContentChangedListener;

import java.util.Map;


public class NPSlider extends LinearLayout implements OnSeekBarChangeListener, ConverserInput {

    private static final int NOTLIKELY_LEVEL = 2;
    private static final int INBETWEEN_LEVEL = 9;

    private android.widget.TextView label;
    private LinearLayout display;
    private SeekBar seekbar;
    private NPSInput model;
    private OnContentChangedListener onContentChangedListener;

    @SuppressLint("NewApi")
    public NPSlider(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }

    public NPSlider(Context context) {
        super(context);

    }

    public NPSlider(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        this.label = (android.widget.TextView) findViewById(R.id.cio__npLabel);
        this.display = (LinearLayout) findViewById(R.id.cio__npDisplay);
        this.seekbar = (SeekBar) findViewById(R.id.cio__npSeekbar);

        seekbar.setOnSeekBarChangeListener(this);

        for (int i = 0; i < display.getChildCount(); i++) {
            View displaySegment = display.getChildAt(i);

            displaySegment.setOnClickListener(new DisplayClickListener(i));
        }

        if (Build.VERSION.SDK_INT < 14) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager(getContext()).getDefaultDisplay().getMetrics(metrics);
            // I think for pre-ICS I need to tweak the thumb offset. Esp for samsung devices
            seekbar.setThumbOffset((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 0, metrics));
        }
        // This is to fix a UI bug. When the NPS is displayed, its position is set but the text above is incorrect.
        // EG the NPS is at progress level 5 but the text says not likely. The NPS needs to detect a change in its variables in order to update the text
        seekbar.setProgress(6);
        seekbar.setProgress(5);

    }

    private WindowManager getWindowManager(Context context) {
        return (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        if (progress < NOTLIKELY_LEVEL) {
            label.setText(R.string.cio__npNotLikely);
        } else if (progress < INBETWEEN_LEVEL) {
            label.setText(R.string.cio__npInBetween);
        } else {
            label.setText(R.string.cio__npLikely);
        }

        for (int i = 0; i < 11; i++) {
            android.widget.ImageView displaySeg = (android.widget.ImageView) display.getChildAt(i);

            if (i > progress) {
                displaySeg.setImageResource(R.drawable.cio__cbg);
            } else {
                switch (i) {
                    case 0:
                        displaySeg.setImageResource(R.drawable.cio__c0);
                        break;
                    case 1:
                        displaySeg.setImageResource(R.drawable.cio__c1);
                        break;
                    case 2:
                        displaySeg.setImageResource(R.drawable.cio__c2);
                        break;
                    case 3:
                        displaySeg.setImageResource(R.drawable.cio__c3);
                        break;
                    case 4:
                        displaySeg.setImageResource(R.drawable.cio__c4);
                        break;
                    case 5:
                        displaySeg.setImageResource(R.drawable.cio__c5);
                        break;
                    case 6:
                        displaySeg.setImageResource(R.drawable.cio__c6);
                        break;
                    case 7:
                        displaySeg.setImageResource(R.drawable.cio__c7);
                        break;
                    case 8:
                        displaySeg.setImageResource(R.drawable.cio__c8);
                        break;
                    case 9:
                        displaySeg.setImageResource(R.drawable.cio__c9);
                        break;
                    case 10:
                        displaySeg.setImageResource(R.drawable.cio__c10);
                        break;
                }
            }
        }

        if(onContentChangedListener != null){
            onContentChangedListener.onContentChanged();
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onReplyDataRequired(Map<String, Object> dataMap) {
        dataMap.put(model.getTag(), seekbar.getProgress());
    }

    @Override
    public String validate() {
        if (model.isOptional()) {
            return null;
        }
        return null;
    }

    public void setModel(NPSInput content) {
        this.model = content;
    }

    public void setOnContentChangedListener(OnContentChangedListener l) {
        onContentChangedListener = l;
    }


    public class DisplayClickListener implements OnClickListener {

        private int index;

        public DisplayClickListener(int i) {
            this.index = i;
        }

        @Override
        public void onClick(View v) {
            seekbar.setProgress(index);
        }

    }

}
