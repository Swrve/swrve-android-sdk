package com.swrve.sdk.converser.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.swrve.sdk.common.R;
import com.swrve.sdk.converser.engine.model.ReactionInput;

import java.util.Map;


/**
 * A custom compound control to capture reactions , on a scale of 1-5
 *
 * @author Jason Connery
 */
public class ReactionInputControl extends LinearLayout implements ConverserInput, OnClickListener {


    private int mItemCount = 5;
    private int selectedIndex = -1; //default to none selected
    private ReactionInput model;

    public ReactionInputControl(Context context) {
        super(context);
        setupViews();
    }

    public ReactionInputControl(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupViews();
    }

    public ReactionInputControl(Context context, ReactionInput model) {
        super(context);
        setupViews();
        this.model = model;
    }


    public int getReaction() {
        return selectedIndex + 1;
    }

    private void setupViews() {

        removeAllViews();

        int padding = 2;

        if (!this.isInEditMode()) {
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.getDefaultDisplay().getMetrics(metrics);

            padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.0f, metrics);
        }

        for (int i = 0; i < mItemCount; i++) {
            android.widget.ImageButton item = new android.widget.ImageButton(getContext());
            LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);


            lp.leftMargin = padding;
            lp.rightMargin = padding;

            item.setLayoutParams(lp);
            item.setBackgroundDrawable(null);

            switch (i + 1) {
                case 1:
                    item.setImageResource(R.drawable.cio__emoticon_01);
                    break;
                case 2:
                    item.setImageResource(R.drawable.cio__emoticon_02);
                    break;
                case 3:
                    item.setImageResource(R.drawable.cio__emoticon_03);
                    break;
                case 4:
                    item.setImageResource(R.drawable.cio__emoticon_04);
                    break;
                case 5:
                    item.setImageResource(R.drawable.cio__emoticon_05);
            }


            if (i == selectedIndex) {
                item.setSelected(true);
            } else {
                item.setSelected(false);
            }

            if (!isInEditMode()) {
                item.setTag(R.string.cio__indexTag, i);
            }
            this.addView(item);
            item.setOnClickListener(this);
        }
    }

    @Override
    public void onClick(View v) {

        int index = (Integer) v.getTag(R.string.cio__indexTag);

        if (index != selectedIndex) {
            View currentSelected = getChildAt(selectedIndex);

            if (currentSelected != null) {
                currentSelected.setSelected(false);
            }

            v.setSelected(true);
            selectedIndex = index;
        }

    }

    @Override
    public void onReplyDataRequired(Map<String, Object> dataMap) {

        dataMap.put(model.getTag(), selectedIndex + 1);

    }

    @Override
    public String validate() {

        if (model.isOptional()) {
            return null;
        }

        if (selectedIndex >= 0) {
            return null;
        } else {
            return "Please choose a reaction";
        }
    }

}
